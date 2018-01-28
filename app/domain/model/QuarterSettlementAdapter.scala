package domain.model

import java.time.LocalDate

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source

trait QuarterSettlementAdapter {
  def findAsStream(market: Market, date: LocalDate): Source[QuarterSettlement, NotUsed]
}
