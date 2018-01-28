package port.adapter.db.mongo

import java.time._

import domain.model._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONHandler, BSONReader, BSONString, BSONValue, BSONWriter, Macros}

import scala.concurrent.{ExecutionContext, Future}

object MongoSupport {
  val mongoUri = "mongodb://localhost:27017/mydb?authMode=scram-sha1"

  // Connect to the database: Must be done only once per application
  val driver = MongoDriver()
  val parsedUri = MongoConnection.parseURI(mongoUri)
  val connection = parsedUri.map(driver.connection(_))

  // Database and collections: Get references
  val futureConnection = Future.fromTry(connection)
  def db(implicit ec: ExecutionContext): Future[DefaultDB] = futureConnection.flatMap(_.database("stock-support"))
  def stocks(implicit ec: ExecutionContext): Future[BSONCollection] = db.map(_.collection("stocks"))

  def toBSONDateTime(date: LocalDate) = BSONDateTime(date.atTime(0, 0).atZone(ZoneId.of("UTC")).toInstant.toEpochMilli)

  implicit object MarketWriter extends BSONWriter[Market, BSONString] {
    override def write(market: Market): BSONString = BSONString(market.code)
  }

  implicit object MarketReader extends BSONReader[BSONValue, Market] {
    override def read(bson: BSONValue): Market = bson match {
      case BSONString(v) => Market.fromCode(v)
    }
  }

  implicit object BigDecimalWriter extends BSONWriter[BigDecimal, BSONString] {
    def write(value: BigDecimal): BSONString = BSONString(value.bigDecimal.toPlainString)
  }

  implicit object BigDecimalReader extends BSONReader[BSONValue, BigDecimal] {
    override def read(bson: BSONValue): BigDecimal = bson match {
      case BSONString(v) => BigDecimal(v)
    }
  }

  implicit object LocalDateWriter extends BSONWriter[LocalDate, BSONDateTime] {
    override def write(value: LocalDate): BSONDateTime = toBSONDateTime(value)
  }

  implicit object LocalDateReader extends BSONReader[BSONValue, LocalDate] {
    override def read(bson: BSONValue): LocalDate = bson match {
      case BSONDateTime(v) => ZonedDateTime.ofInstant(Instant.ofEpochMilli(v), ZoneId.of("UTC")).toLocalDate
    }
  }

  implicit object YearMonthWriter extends BSONWriter[YearMonth, BSONString] {
    override def write(value: YearMonth): BSONString = BSONString(value.toString)
  }

  implicit object YearMonthReader extends BSONReader[BSONString, YearMonth] {
    override def read(bson: BSONString): YearMonth = bson match {
      case BSONString(v) => YearMonth.parse(v)
    }
  }

  implicit def dailyPriceHandler: BSONHandler[BSONDocument, DailyPrice] = Macros.handler[DailyPrice]

  implicit def analysisHandler: BSONHandler[BSONDocument, StockAnalysis] = Macros.handler[StockAnalysis]

  implicit def quarterSettlementHandler: BSONHandler[BSONDocument, QuarterSettlement] = Macros.handler[QuarterSettlement]

  implicit def quarterSettlementAnalysisHandler: BSONHandler[BSONDocument, QuarterSettlementAnalysis] = Macros.handler[QuarterSettlementAnalysis]

  implicit def stockWriter: BSONDocumentWriter[Stock] = Macros.writer[Stock]

  implicit def stockReader: BSONDocumentReader[Stock] = Macros.reader[Stock]

}
