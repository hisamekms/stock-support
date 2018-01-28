package port.adapter.quandl

import java.time.LocalDate
import javax.inject.Inject

import com.jimmoores.quandl._
import com.jimmoores.quandl.classic.ClassicQuandlSession
import com.typesafe.config.Config
import domain.model.{DailyPrice, DailyPriceAdapter, Market}
import domain.model.ValueObjects.Code

import scala.collection.JavaConverters
import scala.concurrent.Future

object QuandlDailyPriceAdapter {
  def apply(config: Config): QuandlDailyPriceAdapter = new QuandlDailyPriceAdapter(config)
}

class QuandlDailyPriceAdapter @Inject()(config: Config) extends DailyPriceAdapter {

  private def session(): ClassicQuandlSession = ClassicQuandlSession.create(
    SessionOptions.Builder.withAuthToken(config.getString("quandl.token")).build)

  override def find(from: LocalDate, to: LocalDate, market: Market, code: Code): Future[Seq[DailyPrice]] = {

    val session = this.session()
    val tabularResult = session.getDataSet(
      DataSetRequest.Builder
        .of(s"${market.code}/$code")
        .withStartDate(org.threeten.bp.LocalDate.ofEpochDay(from.toEpochDay))
        .withEndDate(org.threeten.bp.LocalDate.ofEpochDay(to.toEpochDay))
        .build()
    )

    val result = JavaConverters.asScalaIterator(tabularResult.iterator)
    Future.successful(
      result.map { row =>
        DailyPrice(
          LocalDate.parse(row.getString("Date")),
          BigDecimal(row.getString("Open")),
          BigDecimal(row.getString("Close")),
          BigDecimal(row.getString("High")),
          BigDecimal(row.getString("Low")),
          BigDecimal(row.getString("Volume"))
        )
      }.toSeq
    )

  }
}
