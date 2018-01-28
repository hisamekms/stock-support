package port.adapter.quandl

import java.io.File
import java.nio.file.{Files, Path}
import java.util.zip.ZipFile
import javax.inject.Inject

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Framing, Sink}
import akka.util.ByteString
import com.google.common.io.{MoreFiles, RecursiveDeleteOption}
import com.typesafe.config.Config
import domain.model.{Market, Stock, StockAdapter}

import scala.concurrent.{ExecutionContext, Future}

object QuandlStockAdapter {
  def apply(config: Config)(implicit system: ActorSystem, materializer: Materializer): QuandlStockAdapter = new QuandlStockAdapter(config)
}

class QuandlStockAdapter @Inject()(config: Config)(implicit system: ActorSystem, materializer: Materializer) extends StockAdapter {

  import scala.collection.JavaConverters._

  def unzip(file: Path): Path = {
    val zipFile = new ZipFile(file.toFile)

    zipFile.entries().asScala.toSeq
      .filter(!_.isDirectory)
      .map { entry =>
        val unzipped = file.resolveSibling("unzipped")
        Files.copy(zipFile.getInputStream(entry), unzipped)
        unzipped
      }
      .head
  }

  override def find(market: Market)(implicit ec: ExecutionContext): Future[Seq[Stock]] = {
    val apiToken = config.getString("quandl.token")
    val uri = s"https://www.quandl.com/api/v3/databases/${market.code}/codes?api_key=$apiToken"
    val dir = Files.createTempDirectory("QuandlStockAdapter")

    val stocks = for {
      unzipped <- Http().singleRequest(HttpRequest(uri = uri))
        .flatMap { res =>
          res.headers.find(_.is("location")) match {
            case Some(location) => Http().singleRequest(HttpRequest(uri = location.value()))
            case _ => Future.successful(res)
          }
        }
        .flatMap { res =>
          val zip = dir.resolve("zip")
          res.entity.dataBytes.runWith(FileIO.toPath(zip)).map(_ => zip)
        }
        .map(unzip)
      stocks <- FileIO.fromPath(unzipped)
        .via(Framing.delimiter(ByteString("\n"), Int.MaxValue).map(_.utf8String))
        .map(line => (line, line.indexOf(",")))
        .map(t => Seq[String](t._1.substring(0, t._2), t._1.substring(t._2 + 1)))
        .map(cols => cols.head.split(s"/") :+ cols(1))
        .map(cols => Stock(
          s"${cols(0)}-${cols(1)}",
          cols(2).replaceAll("\"", ""),
          market,
          cols(1)
        ))
        .runWith(Sink.seq)
    } yield stocks

    stocks.onComplete(_ => MoreFiles.deleteRecursively(dir, RecursiveDeleteOption.ALLOW_INSECURE))

    stocks
  }
}
