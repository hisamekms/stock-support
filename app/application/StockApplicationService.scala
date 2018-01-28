package application

import java.time.LocalDate

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, Materializer}
import application.StockApplicationServiceProtocol._
import domain.model.ValueObjects.Code
import domain.model._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object StockApplicationService {
  def props(stockAdapter: StockAdapter, dailyPriceAdapter: DailyPriceAdapter, quarterSettlementAdapter: QuarterSettlementAdapter, stockRepository: StockRepository): Props =
    Props(classOf[StockApplicationService], stockAdapter, dailyPriceAdapter, quarterSettlementAdapter, stockRepository)
}

class StockApplicationService(stockAdapter: StockAdapter, dailyPriceAdapter: DailyPriceAdapter, quarterSettlementAdapter: QuarterSettlementAdapter, stockRepository: StockRepository)
  extends Actor with ActorLogging {

  implicit val ec: ExecutionContext = context.dispatcher
  implicit val materializer: Materializer = ActorMaterializer()

  override def receive: Receive = {
    case ImportStock(market: Market) =>
      val originalSender = sender

      val f: Future[(Map[Code, Stock], Map[Code, Stock])] = for {
        saved <- stockRepository.find(market)
        imported <- stockAdapter.find(market)
      } yield (Map(saved.map(s => s.code -> s): _*), Map(imported.map(s => s.code -> s): _*))

      f.map {
        case (saved, imported) =>
          val allKeys = saved.keySet ++ imported.keySet

          allKeys.map(key => (saved.get(key), imported.get(key))).map {
            case (Some(s), Some(i)) => Some(s.update(i))
            case (Some(s), None) => Some(s.delete())
            case (None, Some(i)) => Some(i)
            case _ => None
          }.filter(_.nonEmpty).map(_.get).toSeq
      }.flatMap(stockRepository.save)
        .onComplete {
          case Success(savedStocks) => originalSender ! ImportStockSuccess(savedStocks)
          case Failure(ex) => {
            log.error(ex, "unexpected error")
            originalSender ! ImportStockFailure(ex)
          }
        }
    case ImportDailyPrices(from, to, market, Some(code)) =>

      val originalSender = sender

      val savedStock: Future[Option[Stock]] = for {
        s1 <- stockRepository.find(market, code)
        dailyPrices <- dailyPriceAdapter.find(from, to, market, code)
        s2 <- s1 match {
          case Some(stock) => stockRepository.save(stock.addDailyPrices(dailyPrices)).map(Some(_))
          case _ => Future.successful(None)
        }
      } yield s2

      savedStock.onComplete {
        case Success(Some(s)) => originalSender ! ImportDailyPricesSuccess(Seq(s))
        case Success(None) => originalSender ! ImportDailyPricesSuccess(Seq())
        case Failure(ex) =>
          log.error(ex, "unexpected error")
          originalSender ! ImportDailyPricesFailure(ex)
      }

    case ImportDailyPrices(from, to, market, _) =>

      val originalSender = sender

      val savedStocks: Future[Seq[Stock]] = stockRepository.find(market).flatMap { stocks =>
        Future.sequence(
          stocks.map(stock =>
            Future.successful(stock).zip(dailyPriceAdapter.find(from, to, stock.market, stock.code))
              .flatMap { case (s, dailyPrices) => stockRepository.save(s.addDailyPrices(dailyPrices)) }
          )
        )
      }

      savedStocks.onComplete {
        case Success(stocks) => originalSender ! ImportDailyPricesSuccess(stocks)
        case Failure(ex) => {
          log.error(ex, "unexpected error")
          originalSender ! ImportDailyPricesFailure(ex)
        }
      }

    case ImportQuarterSettlementList(market) =>
      val originalSender = sender

      val stocksFuture = quarterSettlementAdapter.findAsStream(market, LocalDate.now())
        .groupBy(Int.MaxValue, _.stockId)
        .fold(Seq[QuarterSettlement]()) { (acc, v) => acc :+ v }
        .mapAsync(4) { seq => stockRepository.find(seq.head.stockId).map(_.map(stock => (stock, stock.update(seq)))) }
        .map {
          case Some((stock, updatedStock)) if stock != updatedStock => Some(updatedStock)
          case _ => None
        }
        .filter(_.nonEmpty)
        .map(_.get)
        .mergeSubstreams
        .runWith(Sink.seq)

      stocksFuture.flatMap(stockRepository.save).onComplete {
        case Success(stocks) => originalSender ! ImportQuarterSettlementListSuccess(stocks)
        case Failure(ex) => originalSender ! ImportQuarterSettlementListFailure(ex)
      }

    case GetYtdStocks(date) =>
  }
}
