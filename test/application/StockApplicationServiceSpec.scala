package application

import java.time.{LocalDate, YearMonth}

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.testkit.{ImplicitSender, TestKit}
import application.StockApplicationServiceProtocol._
import domain.model.ValueObjects.{Code, StockId}
import domain.model._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FreeSpecLike, Matchers}

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class StockApplicationServiceSpec extends TestKit(ActorSystem("test"))
  with ImplicitSender with FreeSpecLike with Matchers with MockFactory with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "StockApplicationService" - {
    "import stocks in a market" in {
      val stockAdapter = mock[StockAdapter]
      val dailyPriceAdapter = mock[DailyPriceAdapter]
      val stockRepository = mock[StockRepository]
      val quarterSettlementAdapter = mock[QuarterSettlementAdapter]
      val stockService = system.actorOf(StockApplicationService.props(stockAdapter, dailyPriceAdapter, quarterSettlementAdapter, stockRepository))

      val importedStocks = Seq(
        Stock("TSE-0001", "new a", Market.Tokyo, "0001"),
        Stock("TSE-0003", "new c", Market.Tokyo, "0003")
      )

      val savedStocks = Seq(
        Stock("TSE-0001", "old a", Market.Tokyo, "0001", Seq()),
        Stock("TSE-0002", "old b", Market.Tokyo, "0002", Seq())
      )

      val stocksToSave = Seq(
        Stock("TSE-0001", "new a", Market.Tokyo, "0001", Seq()),
        Stock("TSE-0002", "old b", Market.Tokyo, "0002", Seq(), deleted = true),
        Stock("TSE-0003", "new c", Market.Tokyo, "0003", Seq())
      )

      (stockRepository.find(_: Market)(_: ExecutionContext)).expects(where {
        (market: Market, _: ExecutionContext) => market == Market.Tokyo
      }).returning(Future.successful(savedStocks))

      (stockAdapter.find(_: Market)(_: ExecutionContext)).expects(where {
        (market: Market, _: ExecutionContext) => market == Market.Tokyo
      }).returning(Future.successful(importedStocks))

      (stockRepository.save(_: Seq[Stock])(_: ExecutionContext)).expects(where {
        (stocks: Seq[Stock], _: ExecutionContext) => stocks == stocksToSave
      }).returning(Future.successful(stocksToSave))

      stockService ! ImportStock(Market.Tokyo)

      expectMsg(ImportStockSuccess(stocksToSave))

    }


    "import daily prices of a stock" in {
      val stockAdapter = mock[StockAdapter]
      val dailyPriceAdapter = mock[DailyPriceAdapter]
      val stockRepository = mock[StockRepository]
      val quarterSettlementAdapter = mock[QuarterSettlementAdapter]
      val stockService = system.actorOf(StockApplicationService.props(stockAdapter, dailyPriceAdapter, quarterSettlementAdapter, stockRepository))

      val stock = Stock("test", "testStock", Market.Tokyo, "0001", Seq())
      val dailyPrices = Seq(
        DailyPrice(LocalDate.of(2017, 8, 1), BigDecimal("1"), BigDecimal("1"), BigDecimal("1"), BigDecimal("1"), BigDecimal("1")),
        DailyPrice(LocalDate.of(2017, 8, 2), BigDecimal("1"), BigDecimal("1"), BigDecimal("1"), BigDecimal("1"), BigDecimal("1"))
      )
      val returnedStock = Stock("test", "testStock", Market.Tokyo, "0001", dailyPrices, StockAnalysis(BigDecimal("1"), LocalDate.of(2017, 8, 2)))

      val from = LocalDate.of(2017, 8, 1)
      val to = LocalDate.of(2017, 8, 2)

      (stockRepository.find(_: Market, _: Code)(_: ExecutionContext)).expects(where {
        (market: Market, code: Code, _: ExecutionContext) => market == Market.Tokyo && code == "0001"
      }).returning(Future.successful(Some(stock)))

      (stockRepository.save(_: Stock)(_: ExecutionContext)).expects(where {
        (s: Stock, _: ExecutionContext) => s == returnedStock
      }).returning(Future.successful(returnedStock))

      (dailyPriceAdapter.find _).expects(where {
        (f: LocalDate, t: LocalDate, market: Market, code: Code) => f == from && t == to && market == Market.Tokyo && code == "0001"
      }).returning(Future.successful(dailyPrices))

      stockService ! ImportDailyPrices(
        from,
        to,
        Market.Tokyo,
        Some("0001"))

      expectMsg(ImportDailyPricesSuccess(Seq(returnedStock)))
    }

    "import daily prices of stocks in a market" in {
      val stockAdapter = mock[StockAdapter]
      val dailyPriceAdapter = mock[DailyPriceAdapter]
      val stockRepository = mock[StockRepository]
      val quarterSettlementAdapter = mock[QuarterSettlementAdapter]
      val stockService = system.actorOf(StockApplicationService.props(stockAdapter, dailyPriceAdapter, quarterSettlementAdapter, stockRepository))

      val stocks = Seq(
        Stock("test", "testStock", Market.Tokyo, "0001", Seq()),
        Stock("test", "testStock", Market.Tokyo, "0002", Seq()))
      val dailyPrices0 = Seq(
        DailyPrice(LocalDate.of(2017, 8, 1), BigDecimal("1"), BigDecimal("1"), BigDecimal("1"), BigDecimal("1"), BigDecimal("1")),
        DailyPrice(LocalDate.of(2017, 8, 2), BigDecimal("1"), BigDecimal("1"), BigDecimal("1"), BigDecimal("1"), BigDecimal("1"))
      )
      val dailyPrices1 = Seq(
        DailyPrice(LocalDate.of(2017, 8, 1), BigDecimal("2"), BigDecimal("1"), BigDecimal("1"), BigDecimal("1"), BigDecimal("1")),
        DailyPrice(LocalDate.of(2017, 8, 2), BigDecimal("2"), BigDecimal("1"), BigDecimal("1"), BigDecimal("1"), BigDecimal("1"))
      )
      val returnedStocks = Seq(
        Stock("test", "testStock", Market.Tokyo, "0001", dailyPrices0, StockAnalysis(BigDecimal("1"), LocalDate.of(2017, 8, 2))),
        Stock("test", "testStock", Market.Tokyo, "0002", dailyPrices1, StockAnalysis(BigDecimal("1"), LocalDate.of(2017, 8, 2)))
      )

      val from = LocalDate.of(2017, 8, 1)
      val to = LocalDate.of(2017, 8, 2)

      (stockRepository.find(_: Market)(_: ExecutionContext)).expects(where {
        (market: Market, _: ExecutionContext) => market == Market.Tokyo
      }).returning(Future.successful(stocks))

      (stockRepository.save(_: Stock)(_: ExecutionContext)).expects(where {
        (s: Stock, _: ExecutionContext) => s == returnedStocks(0)
      }).returning(Future.successful(returnedStocks(0)))

      (stockRepository.save(_: Stock)(_: ExecutionContext)).expects(where {
        (s: Stock, _: ExecutionContext) => s == returnedStocks(1)
      }).returning(Future.successful(returnedStocks(1)))

      (dailyPriceAdapter.find _).expects(where {
        (f: LocalDate, t: LocalDate, market: Market, code: Code) => f == from && t == to && market == Market.Tokyo && code == "0001"
      }).returning(Future.successful(dailyPrices0))

      (dailyPriceAdapter.find _).expects(where {
        (f: LocalDate, t: LocalDate, market: Market, code: Code) => f == from && t == to && market == Market.Tokyo && code == "0002"
      }).returning(Future.successful(dailyPrices1))

      stockService ! ImportDailyPrices(
        from,
        to,
        Market.Tokyo,
        None)

      expectMsg(ImportDailyPricesSuccess(returnedStocks))
    }

    "import quarter settlement list" in {
      val stockAdapter = mock[StockAdapter]
      val dailyPriceAdapter = mock[DailyPriceAdapter]
      val stockRepository = mock[StockRepository]
      val quarterSettlementAdapter = mock[QuarterSettlementAdapter]
      val stockService = system.actorOf(StockApplicationService.props(stockAdapter, dailyPriceAdapter, quarterSettlementAdapter, stockRepository))

      val stocks = Seq(
        Stock("TSE-0001", "testStock1", Market.Tokyo, "0001", quarterSettlementList = Seq()),
        Stock("TSE-0002", "testStock2", Market.Tokyo, "0002", quarterSettlementList = Seq(
          QuarterSettlement(
            "TSE-0002",
            YearMonth.of(2017, 3),
            1,
            LocalDate.of(2016, 4, 1),
            LocalDate.of(2016, 6, 30),
            Some(BigDecimal("1")),
            Some(BigDecimal("2")),
            Some(BigDecimal("3")),
            Some(BigDecimal("4")),
            LocalDate.of(2016, 7, 31)
          )
        )),
        Stock("TSE-0003", "testStock3", Market.Tokyo, "0003", quarterSettlementList = Seq()))

      val updatedStocks = Seq(
        Stock("TSE-0001", "testStock1", Market.Tokyo, "0001", quarterSettlementList = Seq(
          QuarterSettlement(
            "TSE-0001",
            YearMonth.of(2017, 3),
            1,
            LocalDate.of(2016, 4, 1),
            LocalDate.of(2016, 6, 30),
            Some(BigDecimal("1")),
            Some(BigDecimal("2")),
            Some(BigDecimal("3")),
            Some(BigDecimal("4")),
            LocalDate.of(2016, 7, 31)
          )
        )))

      val importQuarterSettlementList = Seq(
        QuarterSettlement(
          "TSE-0001",
          YearMonth.of(2017, 3),
          1,
          LocalDate.of(2016, 4, 1),
          LocalDate.of(2016, 6, 30),
          Some(BigDecimal("1")),
          Some(BigDecimal("2")),
          Some(BigDecimal("3")),
          Some(BigDecimal("4")),
          LocalDate.of(2016, 7, 31)
        ),
        QuarterSettlement(
          "TSE-0002",
          YearMonth.of(2017, 3),
          1,
          LocalDate.of(2016, 4, 1),
          LocalDate.of(2016, 6, 30),
          Some(BigDecimal("1")),
          Some(BigDecimal("2")),
          Some(BigDecimal("3")),
          Some(BigDecimal("4")),
          LocalDate.of(2016, 7, 31)
        ),
        QuarterSettlement(
          "TSE-0004",
          YearMonth.of(2017, 3),
          1,
          LocalDate.of(2016, 4, 1),
          LocalDate.of(2016, 6, 30),
          Some(BigDecimal("1")),
          Some(BigDecimal("2")),
          Some(BigDecimal("3")),
          Some(BigDecimal("4")),
          LocalDate.of(2016, 7, 31)
        )
      )

      (stockRepository.find(_: StockId)(_: ExecutionContext)).expects(where {
        (stockId: StockId, _: ExecutionContext) => stockId == stocks(0).id
      }).returning(Future.successful(Some(stocks(0))))

      (stockRepository.find(_: StockId)(_: ExecutionContext)).expects(where {
        (stockId: StockId, _: ExecutionContext) => stockId == stocks(1).id
      }).returning(Future.successful(Some(stocks(1))))

      (stockRepository.find(_: StockId)(_: ExecutionContext)).expects(where {
        (stockId: StockId, _: ExecutionContext) => stockId == "TSE-0004"
      }).returning(Future.successful(None))

      (quarterSettlementAdapter.findAsStream(_: Market, _: LocalDate)).expects(where {
        (market: Market, date: LocalDate) => market == Market.Tokyo && date == LocalDate.now()
      }).returning(Source(importQuarterSettlementList.to[immutable.Iterable]))

      (stockRepository.save(_: Seq[Stock])(_: ExecutionContext)).expects(where {
        (ss: Seq[Stock], _: ExecutionContext) => ss == updatedStocks
      }).returning(Future.successful(updatedStocks))

      stockService ! ImportQuarterSettlementList(Market.Tokyo)

      expectMsg(ImportQuarterSettlementListSuccess(updatedStocks))
    }
  }
}
