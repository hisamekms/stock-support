package port.adapter.kabupuro

import java.nio.file.{Files, Path}
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, YearMonth, ZoneId}
import java.util.Date
import javax.inject.Inject

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Flow, Source}
import com.google.common.io.{MoreFiles, RecursiveDeleteOption}
import domain.model.ValueObjects.{Code, toStockId}
import domain.model.{Market, QuarterSettlement, QuarterSettlementAdapter}
import org.apache.poi.hssf.usermodel.{HSSFFormulaEvaluator, HSSFWorkbook}
import org.apache.poi.ss.usermodel._
import play.api.Logger

import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class KabupuroQuarterSettlementAdapter @Inject()(implicit system: ActorSystem, materializer: Materializer) extends QuarterSettlementAdapter {

  implicit val ec: ExecutionContext = system.dispatcher

  private[kabupuro] case class KabupuroWholeQuarterSettlement
  (
    code: Code,
    term: YearMonth,
    ordinal: Int,
    from: LocalDate,
    to: LocalDate,
    sales: Option[BigDecimal],
    operatingProfit: Option[BigDecimal],
    ordinaryProfit: Option[BigDecimal],
    profit: Option[BigDecimal],
    updatedAt: LocalDate
  )

  private[kabupuro] def excelSource(file: Path): Source[KabupuroWholeQuarterSettlement, NotUsed] = {

    // xlsxしか対応していない
    //    val workbook: Workbook = StreamingReader.builder()
    //      .rowCacheSize(100)
    //      .bufferSize(4096)
    //      .open(file.toFile)

    val workbook = WorkbookFactory.create(file.toFile)

    val dataFormatter = new DataFormatter()
    val evaluator = new HSSFFormulaEvaluator(workbook.asInstanceOf[HSSFWorkbook])

    val stringValue = (cell: Cell) => {
      evaluator.evaluate(cell)
      val s = dataFormatter.formatCellValue(cell, evaluator)
      s
    }

    val toLocalDate = (date: Date) => date.toInstant.atZone(ZoneId.systemDefault()).toLocalDate

    val toBigDecimal = (cell: Cell) =>
      stringValue(cell).replaceAll(",", "") match {
        case s if s.isEmpty =>
          Logger.warn(s"row:${cell.getRowIndex} col:${cell.getColumnIndex} empty")
          None
        case s => Some(BigDecimal(s))
      }

    Source.fromIterator[Row](() => workbook.getSheetAt(0).iterator().asScala)
      .drop(1)
      .filter(row => row.getCell(3).getStringCellValue == "連結")
      .map(row => KabupuroWholeQuarterSettlement(
        stringValue(row.getCell(0)),
        YearMonth.parse(row.getCell(4).getStringCellValue, DateTimeFormatter.ofPattern("yyyy年M月期")),
        row.getCell(5).getStringCellValue match {
          case "第1四半期" => 1
          case "第2四半期" => 2
          case "第3四半期" => 3
          case "通期" => 4
        },
        toLocalDate(row.getCell(6).getDateCellValue),
        toLocalDate(row.getCell(7).getDateCellValue),
        toBigDecimal(row.getCell(9)),
        toBigDecimal(row.getCell(10)),
        toBigDecimal(row.getCell(11)),
        toBigDecimal(row.getCell(12)),
        toLocalDate(row.getCell(21).getDateCellValue)
      ))
      .watchTermination() { (m, f) =>
        f.onComplete { _ =>
          try workbook.close() catch {
            case _: Throwable =>
          }
        }
        m
      }
  }

  private def calc(a: Option[BigDecimal], b: Option[BigDecimal]): Option[BigDecimal] = (a, b) match {
    case (Some(v1: BigDecimal), Some(v2: BigDecimal)) => Some(v1 - v2)
    case _ => None
  }


  private def toQuarterSettlement(s1: KabupuroWholeQuarterSettlement, s2: KabupuroWholeQuarterSettlement): QuarterSettlement = QuarterSettlement(
    toStockId(Market.Tokyo, s1.code),
    s1.term,
    s1.ordinal,
    s1.from,
    s1.to,
    calc(s1.sales, s2.sales),
    calc(s1.operatingProfit, s2.operatingProfit),
    calc(s1.ordinaryProfit, s2.ordinaryProfit),
    calc(s1.profit, s2.profit),
    s1.updatedAt
  )

  private def toQuarterSettlement(s1: KabupuroWholeQuarterSettlement): QuarterSettlement = QuarterSettlement(
    toStockId(Market.Tokyo, s1.code),
    s1.term,
    s1.ordinal,
    s1.from,
    s1.to,
    s1.sales,
    s1.operatingProfit,
    s1.ordinaryProfit,
    s1.profit,
    s1.updatedAt
  )

  private[kabupuro] def convertToQuarterSettlement(): Flow[KabupuroWholeQuarterSettlement, QuarterSettlement, NotUsed] =
    Flow.apply[KabupuroWholeQuarterSettlement]
      .groupBy(Int.MaxValue, s => (s.code, s.term))
      .fold(Map[(Int), KabupuroWholeQuarterSettlement]()) { (map, s) => map + (s.ordinal -> s) }
      .filter(_.size == 4)
      .mapConcat(map => Seq(
        toQuarterSettlement(map(1)),
        toQuarterSettlement(map(2), map(1)),
        toQuarterSettlement(map(3), map(2)),
        toQuarterSettlement(map(4), map(3))
      ).to[immutable.Iterable])
      .mergeSubstreams

  private def fromExcel(file: Path): Source[QuarterSettlement, NotUsed] = excelSource(file) via convertToQuarterSettlement()

  override def findAsStream(market: Market, date: LocalDate): Source[QuarterSettlement, NotUsed] = {

    require(market == Market.Tokyo)

    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    val uri = s"http://ke.kabupro.jp/down/${date.format(formatter)}f.xls"
    val dir = Files.createTempDirectory("KabupuroQuarterSettlementAdapter")
    val file = dir.resolve("settlement.xls")

    val futureSource: Future[Source[QuarterSettlement, NotUsed]] = Http().singleRequest(HttpRequest(uri = uri))
      .flatMap { res =>
        res.entity
          .withSizeLimit(Long.MaxValue)
          .dataBytes.runWith(FileIO.toPath(file))
      }
      .map(_ => fromExcel(file))

    val source = Source.fromFutureSource(futureSource).mapMaterializedValue { mat =>
      mat.onComplete(_ => MoreFiles.deleteRecursively(dir, RecursiveDeleteOption.ALLOW_INSECURE))
      NotUsed
    }

    source
  }
}

