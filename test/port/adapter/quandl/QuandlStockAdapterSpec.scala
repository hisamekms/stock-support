package port.adapter.quandl

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.ConfigFactory
import domain.model.Market
import org.scalatest.{AsyncFreeSpec, Matchers}

class QuandlStockAdapterSpec extends AsyncFreeSpec with Matchers  {

  implicit val system: ActorSystem = ActorSystem("test")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val stockAdapter: QuandlStockAdapter = QuandlStockAdapter(ConfigFactory.load)

  "QuandlStockAdapter" - {
    "find" in {
      stockAdapter.find(Market.Tokyo).map { stocks =>
        stocks.size should be > 0
      }
    }
  }
}
