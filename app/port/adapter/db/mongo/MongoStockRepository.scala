package port.adapter.db.mongo

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import domain.model.ValueObjects.{Code, StockId}
import domain.model._
import port.adapter.db.mongo.MongoSupport._
import reactivemongo.api.Cursor
import reactivemongo.bson.document

import scala.concurrent.{ExecutionContext, Future}

class MongoStockRepository extends StockRepository {

  private def byId(stockId: StockId) = document("id" -> stockId)

  override def save(stock: Stock)(implicit ec: ExecutionContext) =
    stocks.flatMap(_.update(byId(stock.id), stock, upsert = true).map(_ => stock))

  override def find(stockId: StockId)(implicit ec: ExecutionContext) = stocks.flatMap(_.find(byId(stockId)).one)

  override def find(market: Market, code: Code)(implicit ec: ExecutionContext): Future[Option[Stock]] =
    stocks.flatMap(_.find(document("market" -> market, "code" -> code)).one)

  override def find(market: Market)(implicit ec: ExecutionContext): Future[Seq[Stock]] =
    stocks.flatMap(_.find(document("market" -> market)).cursor[Stock]()
      .collect[Seq](-1, Cursor.FailOnError[Seq[Stock]]()))

  override def save(seq: Seq[Stock])(implicit ec: ExecutionContext): Future[Seq[Stock]] = Future.sequence(seq.map(this.save))

  override def findAsStream(market: Market)(implicit ec: ExecutionContext, materializer: Materializer): Source[Stock, NotUsed] = {
    import reactivemongo.akkastream.cursorProducer

    val f = stocks.map(_.find(document("market" -> market, "deleted" -> false)).cursor[Stock]().documentSource())

    Source.fromFutureSource(f).mapMaterializedValue(_ => NotUsed)
  }
}
