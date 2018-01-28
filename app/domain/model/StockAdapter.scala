package domain.model

import scala.concurrent.{ExecutionContext, Future}

trait StockAdapter {
  def find(market: Market)(implicit ec: ExecutionContext): Future[Seq[Stock]]
}
