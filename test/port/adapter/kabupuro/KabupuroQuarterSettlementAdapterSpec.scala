package port.adapter.kabupuro

import java.time.{LocalDate, YearMonth}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import domain.model.{Market, QuarterSettlement}
import org.scalatest.{AsyncFreeSpec, BeforeAndAfterAll, FutureOutcome, Matchers}

import scala.collection.immutable

class KabupuroQuarterSettlementAdapterSpec extends AsyncFreeSpec with Matchers with BeforeAndAfterAll {
  implicit val system: ActorSystem = ActorSystem("test")
  implicit val materializer: Materializer = ActorMaterializer()
  val quarterSettlementAdapter = new KabupuroQuarterSettlementAdapter()

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  "KabupuroQuarterSettlementAdapter" - {

    "convertToQuarterSettlement" in {
      val flow: Flow[quarterSettlementAdapter.KabupuroWholeQuarterSettlement, QuarterSettlement, NotUsed] = quarterSettlementAdapter.convertToQuarterSettlement()

      val list = Seq(
        quarterSettlementAdapter.KabupuroWholeQuarterSettlement(
          "0001",
          YearMonth.of(2017, 3),
          1,
          LocalDate.of(2016, 4, 1),
          LocalDate.of(2016, 6, 30),
          Some(BigDecimal("1100")),
          Some(BigDecimal("1200")),
          Some(BigDecimal("1300")),
          Some(BigDecimal("1400")),
          LocalDate.of(2017, 3, 31)
        ),
        quarterSettlementAdapter.KabupuroWholeQuarterSettlement(
          "0001",
          YearMonth.of(2017, 3),
          2,
          LocalDate.of(2016, 7, 1),
          LocalDate.of(2016, 9, 30),
          Some(BigDecimal("2100")),
          Some(BigDecimal("2200")),
          Some(BigDecimal("2300")),
          Some(BigDecimal("2400")),
          LocalDate.of(2017, 3, 31)
        ),
        quarterSettlementAdapter.KabupuroWholeQuarterSettlement(
          "0001",
          YearMonth.of(2017, 3),
          3,
          LocalDate.of(2016, 10, 1),
          LocalDate.of(2016, 12, 31),
          Some(BigDecimal("3100")),
          Some(BigDecimal("3200")),
          Some(BigDecimal("3300")),
          Some(BigDecimal("3400")),
          LocalDate.of(2017, 3, 31)
        ),
        quarterSettlementAdapter.KabupuroWholeQuarterSettlement(
          "0001",
          YearMonth.of(2017, 3),
          4,
          LocalDate.of(2017, 1, 1),
          LocalDate.of(2017, 3, 31),
          Some(BigDecimal("4100")),
          Some(BigDecimal("4200")),
          Some(BigDecimal("4300")),
          Some(BigDecimal("4400")),
          LocalDate.of(2017, 3, 31)
        )
      ).to[immutable.Iterable]

      Source(list).via(flow).runWith(Sink.seq)
        .map(_ shouldBe Seq(
          QuarterSettlement(
            "TSE-0001",
            YearMonth.of(2017, 3),
            1,
            LocalDate.of(2016, 4, 1),
            LocalDate.of(2016, 6, 30),
            Some(BigDecimal("1100")),
            Some(BigDecimal("1200")),
            Some(BigDecimal("1300")),
            Some(BigDecimal("1400")),
            LocalDate.of(2017, 3, 31)
          ),
          QuarterSettlement(
            "TSE-0001",
            YearMonth.of(2017, 3),
            2,
            LocalDate.of(2016, 7, 1),
            LocalDate.of(2016, 9, 30),
            Some(BigDecimal("1000")),
            Some(BigDecimal("1000")),
            Some(BigDecimal("1000")),
            Some(BigDecimal("1000")),
            LocalDate.of(2017, 3, 31)
          ),
          QuarterSettlement(
            "TSE-0001",
            YearMonth.of(2017, 3),
            3,
            LocalDate.of(2016, 10, 1),
            LocalDate.of(2016, 12, 31),
            Some(BigDecimal("1000")),
            Some(BigDecimal("1000")),
            Some(BigDecimal("1000")),
            Some(BigDecimal("1000")),
            LocalDate.of(2017, 3, 31)
          ),
          QuarterSettlement(
            "TSE-0001",
            YearMonth.of(2017, 3),
            4,
            LocalDate.of(2017, 1, 1),
            LocalDate.of(2017, 3, 31),
            Some(BigDecimal("1000")),
            Some(BigDecimal("1000")),
            Some(BigDecimal("1000")),
            Some(BigDecimal("1000")),
            LocalDate.of(2017, 3, 31)
          )))
    }

    "findAsStream" ignore {
      val source = quarterSettlementAdapter.findAsStream(Market.Tokyo, LocalDate.of(2018, 1, 5))
      val count = source.runFold(0) { (acc, _) => acc + 1 }

      count.map(_ should be > 0)
    }
  }
}
