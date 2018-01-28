package domain.model

import java.time.{LocalDate, YearMonth}

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import domain.model.Stock.Rate
import domain.model.ValueObjects.{Code, StockId}

import scala.concurrent.{ExecutionContext, Future}

object Stock {
  type Rate = BigDecimal
}

object StockAnalysis {
  def empty(): StockAnalysis = StockAnalysis()

  def apply(ytd: BigDecimal, ytdDate: LocalDate): StockAnalysis = StockAnalysis(
    Some(ytd),
    Some(ytdDate)
  )
}
case class StockAnalysis
(
  ytd: Option[BigDecimal] = None,
  ytdDate: Option[LocalDate] = None,
  quarterSettlementAnalysisList: Seq[QuarterSettlementAnalysis] = Seq()
)

case class Stock
(
  id: StockId,
  name: String,
  market: Market,
  code: Code,
  dailyPrices: Seq[DailyPrice] = Seq(),
  analysis: StockAnalysis = StockAnalysis.empty(),
  quarterSettlementList: Seq[QuarterSettlement] = Seq(),
  deleted: Boolean = false
) {

  def update(s: Stock): Stock = copy(name = s.name)

  def delete(): Stock = copy(deleted = true)

  def update(quarterSettlementList: Seq[QuarterSettlement]): Stock = {
    val newQuarterSettlementList: Seq[QuarterSettlement] = (this.quarterSettlementList ++ quarterSettlementList)
      .groupBy(s => (s.term, s.ordinal))
      .values
      .map(_.sortBy(_.updatedAt.toEpochDay).reverse.head)
      .toSeq
      .sortBy(s => (s.term, s.ordinal))

    if (this.quarterSettlementList == newQuarterSettlementList) {
      return this
    }

    val quarterSettlementMap = newQuarterSettlementList.map(qs => (qs.term, qs.ordinal) -> qs).toMap
    val quarterSettlementAnalysisList = quarterSettlementList
      .filter(qs => quarterSettlementMap.contains((qs.term.minusYears(1), qs.ordinal)))
      .map(qs => QuarterSettlementAnalysis(qs, quarterSettlementMap((qs.term.minusYears(1), qs.ordinal))))

    copy(
      quarterSettlementList = newQuarterSettlementList,
      analysis = analysis.copy(
        quarterSettlementAnalysisList = quarterSettlementAnalysisList
      )
    )
  }

  def addDailyPrice(price: DailyPrice): Stock = addDailyPrices(Seq(price))

  def addDailyPrices(prices: Seq[DailyPrice]): Stock = {
    val newDailyPrices = dailyPrices ++ prices
    val mostRecentPrice = newDailyPrices.maxBy(_.date.toEpochDay)
    val ytd = this.ytd(newDailyPrices, mostRecentPrice.date)

    if (mostRecentPrice.high == ytd) {
      copy(
        dailyPrices = newDailyPrices,
        analysis = StockAnalysis(Some(mostRecentPrice.high), Some(mostRecentPrice.date))
      )
    } else copy(dailyPrices = newDailyPrices)
  }

  private def ytd(dailyPrices: Seq[DailyPrice], date: LocalDate): BigDecimal = {
    val thisYearAprilFirst = LocalDate.of(date.getYear, 4, 1)
    val thisYearJanuaryFirst = LocalDate.of(date.getYear, 1, 1)
    val lastYearJanuaryFirst = LocalDate.of(date.getYear - 1, 1, 1)

    val inRange = date match {
      case d if d.isAfter(thisYearAprilFirst) || d.isEqual(thisYearAprilFirst) => (dailyPrice: DailyPrice) =>
        dailyPrice.date.isAfter(thisYearJanuaryFirst) || dailyPrice.date.isEqual(thisYearJanuaryFirst)
      case _ => (dailyPrice: DailyPrice) =>
        dailyPrice.date.isAfter(lastYearJanuaryFirst) || dailyPrice.date.isEqual(lastYearJanuaryFirst)
    }

    dailyPrices
      .filter(inRange)
      .map(_.high)
      .max
  }
}

case class QuarterSettlement
(
  stockId: StockId,
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

object QuarterSettlementAnalysis {
  private def calc(v1: Option[BigDecimal], v2: Option[BigDecimal]): Option[Rate] = (v1, v2) match {
    case (Some(a: BigDecimal), Some(b: BigDecimal)) => Some(a / b)
    case _ => None
  }

  def apply(qs1: QuarterSettlement, qs2: QuarterSettlement): QuarterSettlementAnalysis = QuarterSettlementAnalysis(
    qs1.term,
    qs1.ordinal,
    calc(qs1.sales, qs2.sales),
    calc(qs1.operatingProfit, qs2.operatingProfit),
    calc(qs1.ordinaryProfit, qs2.ordinaryProfit),
    calc(qs1.profit, qs2.profit)
  )
}

case class QuarterSettlementAnalysis
(
  term: YearMonth,
  ordinal: Int,
  salesYoyRate: Option[Rate],
  operatingProfitYoyRate: Option[Rate],
  ordinaryProfitYoyRate: Option[Rate],
  profitYoyRate: Option[Rate]
)

case class DailyPrice
(
  date: LocalDate,
  open: BigDecimal,
  close: BigDecimal,
  high: BigDecimal,
  low: BigDecimal,
  volume: BigDecimal
)

trait StockRepository {
  def save(stocks: Seq[Stock])(implicit ec: ExecutionContext): Future[Seq[Stock]]

  def save(stock: Stock)(implicit ec: ExecutionContext): Future[Stock]

  def find(stockId: StockId)(implicit ec: ExecutionContext): Future[Option[Stock]]

  def find(market: Market, code: Code)(implicit ec: ExecutionContext): Future[Option[Stock]]

  def find(market: Market)(implicit ec: ExecutionContext): Future[Seq[Stock]]

  def findAsStream(market: Market)(implicit ec: ExecutionContext, materializer: Materializer): Future[Source[Stock, NotUsed]]
}