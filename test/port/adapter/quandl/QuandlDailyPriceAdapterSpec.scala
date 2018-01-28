package port.adapter.quandl

import java.time.LocalDate

import com.typesafe.config.ConfigFactory
import domain.model.Market
import org.scalatest.{AsyncFreeSpec, Matchers}


class QuandlDailyPriceAdapterSpec extends AsyncFreeSpec with Matchers {
  "QuandlDailyPriceAdapter" - {
    val quandlDailyPriceAdapter = QuandlDailyPriceAdapter(ConfigFactory.load)
    "find" in {
      quandlDailyPriceAdapter.find(
        LocalDate.of(2016, 8, 1),
        LocalDate.of(2016, 8, 2),
        Market.Tokyo,
        "1379"
      ).map { seq =>
        seq should have length 2
      }
    }
  }
}
