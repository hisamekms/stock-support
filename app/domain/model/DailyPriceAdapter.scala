package domain.model

import java.time.LocalDate

import domain.model.ValueObjects.Code

import scala.concurrent.Future

trait DailyPriceAdapter {
  def find(from: LocalDate, to: LocalDate, market: Market, code: Code): Future[Seq[DailyPrice]]
}
