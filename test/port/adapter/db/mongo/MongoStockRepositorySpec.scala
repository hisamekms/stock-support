package port.adapter.db.mongo

import java.time.{LocalDate, YearMonth, ZoneId}

import domain.model._
import org.scalatest.{FutureOutcome, fixture}
import port.adapter.db.mongo.MongoSupport.stocks
import reactivemongo.bson.{BSONArray, BSONDateTime, BSONDocument}

class MongoStockRepositorySpec extends fixture.AsyncFreeSpec {

  type FixtureParam = MongoStockRepository

  private def toBSONDateTime(y: Int, m: Int, d: Int) =
    BSONDateTime(LocalDate.of(y, m, d).atTime(0, 0).atZone(ZoneId.of("UTC")).toInstant.toEpochMilli)

  override def withFixture(test: OneArgAsyncTest): FutureOutcome = {
    val repository = new MongoStockRepository
    val f = (for {
      _ <- stocks.flatMap(_.drop(failIfNotFound = false))
      _ <- stocks.flatMap(_.insert(BSONDocument(
        "id" -> "1",
        "name" -> "insertedStock",
        "market" -> "TSE",
        "code" -> "9999",
        "deleted" -> false,
        "dailyPrices" -> BSONArray(
          BSONDocument(
            "date" -> toBSONDateTime(2017, 1, 1),
            "open" -> "10.0",
            "close" -> "10.1",
            "high" -> "10.5",
            "low" -> "0.5",
            "volume" -> "100"),
          BSONDocument(
            "date" -> toBSONDateTime(2017, 1, 2),
            "open" -> "10.1",
            "close" -> "10.2",
            "high" -> "10.6",
            "low" -> "0.6",
            "volume" -> "200")
        ),
        "analysis" -> BSONDocument(
          "ytd" -> "20",
          "ytdDate" -> toBSONDateTime(2016, 12, 1),
          "quarterSettlementAnalysisList" -> BSONArray()
        ),
        "quarterSettlementList" -> BSONArray(
          BSONDocument(
            "stockId" -> "1",
            "term" -> "2017-03",
            "ordinal" -> 1,
            "from" -> toBSONDateTime(2016, 4, 1),
            "to" -> toBSONDateTime(2016, 6, 30),
            "sales" -> "100",
            "operatingProfit" -> "101",
            "ordinaryProfit" -> "102",
            "profit" -> "103",
            "updatedAt" -> toBSONDateTime(2016, 7, 31)
          )
        )
      )))
    } yield null)
      .flatMap(_ => super.withFixture(test.toNoArgAsyncTest(repository)).toFuture)
    new FutureOutcome(f)
  }

  "MongoStockRepository" - {

    "find" in { repository =>
      repository.find("1").map { stockOpt =>
        assert(stockOpt.get == Stock(
          "1",
          "insertedStock",
          Market.Tokyo,
          "9999",
          Seq(
            DailyPrice(
              LocalDate.of(2017, 1, 1),
              BigDecimal("10.0"),
              BigDecimal("10.1"),
              BigDecimal("10.5"),
              BigDecimal("0.5"),
              BigDecimal("100")),
            DailyPrice(
              LocalDate.of(2017, 1, 2),
              BigDecimal("10.1"),
              BigDecimal("10.2"),
              BigDecimal("10.6"),
              BigDecimal("0.6"),
              BigDecimal("200"))
          ),
          StockAnalysis(BigDecimal("20"), LocalDate.of(2016, 12, 1)),
          Seq(
            QuarterSettlement(
              "1",
              YearMonth.of(2017, 3),
              1,
              LocalDate.of(2016, 4, 1),
              LocalDate.of(2016, 6, 30),
              Some(BigDecimal("100")),
              Some(BigDecimal("101")),
              Some(BigDecimal("102")),
              Some(BigDecimal("103")),
              LocalDate.of(2016, 7, 31)
            )
          )
        ))
      }
    }

    "Stockが保存されていない場合" - {
      "Stockが保存される" in { repository =>
        val stock = Stock("2", "testStock", Market.Tokyo, "1234", Seq())
        repository.save(stock).flatMap { s1 =>
          assert(s1 == stock)
          repository.find(stock.id).map { s2 =>
            assert(s2.get == stock)
          }
        }
      }
    }

    "Stockが保存されている場合" - {
      "Stockが更新される" in { repository =>
        val stock = Stock("1", "testStock", Market.Tokyo, "1234", Seq(
          DailyPrice(LocalDate.of(2017, 1, 1),
            BigDecimal("10.0"),
            BigDecimal("10.1"),
            BigDecimal("10.5"),
            BigDecimal("0.5"),
            BigDecimal("100"))
        ))
        repository.save(stock).flatMap { s1 =>
          assert(s1 == stock)
          repository.find(stock.id).map { s2 =>
            assert(s2.get == stock)
          }
        }
      }
    }
  }
}