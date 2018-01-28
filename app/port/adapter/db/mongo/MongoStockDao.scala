package port.adapter.db.mongo

import java.time.LocalDate

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import domain.model.Stock
import query.StockDao

import scala.concurrent.{ExecutionContext, Future}
import port.adapter.db.mongo.MongoSupport._
import reactivemongo.bson.document

class MongoStockDao extends StockDao {

  override def ytdStocks(date: LocalDate)(implicit ec: ExecutionContext, materializer: Materializer): Future[Source[Stock, NotUsed]] = {
    import reactivemongo.akkastream.cursorProducer

    val query = document("analysis" -> document("ytdDate" -> toBSONDateTime(date)))
    stocks.map(_.find(query).cursor[Stock]().documentSource().mapMaterializedValue(_ => NotUsed))
  }

}
