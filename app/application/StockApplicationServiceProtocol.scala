package application

import java.time.LocalDate

import akka.NotUsed
import akka.stream.scaladsl.Source
import domain.model.ValueObjects.Code
import domain.model.{Market, Stock}

object StockApplicationServiceProtocol {
  sealed trait StockServiceCommand
  sealed trait StockServiceResponse

  case class ImportStock(market: Market) extends StockServiceCommand

  case class ImportStockSuccess(stocks: Seq[Stock]) extends StockServiceResponse

  case class ImportStockFailure(ex: Throwable) extends StockServiceResponse

  case class ImportDailyPrices
  (
    from: LocalDate,
    to: LocalDate,
    market: Market,
    code: Option[Code]
  ) extends StockServiceCommand

  sealed trait ImportDailyPricesResponse extends StockServiceResponse

  case class ImportDailyPricesSuccess(stocks: Seq[Stock]) extends ImportDailyPricesResponse

  case class ImportDailyPricesFailure(ex: Throwable) extends ImportDailyPricesResponse

  case class ImportQuarterSettlementList(market: Market) extends StockServiceCommand

  sealed trait ImportQuarterSettlementListResponse extends StockServiceResponse

  case class ImportQuarterSettlementListSuccess(stocks: Seq[Stock]) extends ImportQuarterSettlementListResponse

  case class ImportQuarterSettlementListFailure(ex: Throwable) extends ImportQuarterSettlementListResponse

  case class GetRecommendedStocks(market: Market, date: LocalDate)

  sealed trait GetRecommendedStocksResponse

  case class GetRecommendedStocksSuccess(stocks: Source[Stock, NotUsed]) extends GetRecommendedStocksResponse

  case class GetRecommendedStocksFailure(ex: Throwable) extends GetRecommendedStocksResponse

}
