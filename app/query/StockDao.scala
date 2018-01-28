package query

import java.time.LocalDate

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import domain.model.Stock

import scala.concurrent.{ExecutionContext, Future}

trait StockDao {
  def ytdStocks(date: LocalDate)(implicit ec: ExecutionContext, materializer: Materializer): Future[Source[Stock, NotUsed]]
}
