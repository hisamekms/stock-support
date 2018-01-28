package domain.model

object ValueObjects {
  type StockId = String
  type Code = String

  def toStockId(market: Market, code: Code) = s"${market.code}-$code"
}