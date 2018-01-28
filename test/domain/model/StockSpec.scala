package domain.model

import java.time.{LocalDate, YearMonth}

import org.scalatest.{AsyncFreeSpec, Matchers}

class StockSpec extends AsyncFreeSpec with Matchers {
  "Stock" - {
    val stock = Stock("1", "test", Market.Tokyo, "9999",
      Seq(
        DailyPrice(
          LocalDate.of(2017, 1, 1),
          BigDecimal("10"),
          BigDecimal("11"),
          BigDecimal("15"),
          BigDecimal("5"),
          BigDecimal("100"))
      )
    )

    "add a daily price" in {
      val s = stock.addDailyPrice(
        DailyPrice(
          LocalDate.of(2017, 1, 2),
          BigDecimal("10"),
          BigDecimal("11"),
          BigDecimal("15"),
          BigDecimal("5"),
          BigDecimal("200")))
      assert(s == Stock("1", "test", Market.Tokyo, "9999", Seq(
        DailyPrice(
          LocalDate.of(2017, 1, 1),
          BigDecimal("10"),
          BigDecimal("11"),
          BigDecimal("15"),
          BigDecimal("5"),
          BigDecimal("100")),
        DailyPrice(
          LocalDate.of(2017, 1, 2),
          BigDecimal("10"),
          BigDecimal("11"),
          BigDecimal("15"),
          BigDecimal("5"),
          BigDecimal("200"))),
        StockAnalysis(BigDecimal("15"), LocalDate.of(2017, 1, 2))
      ))
    }

    "add daily prices" in {
      val s = stock.addDailyPrices(Seq(
        DailyPrice(
          LocalDate.of(2017, 1, 2),
          BigDecimal("10"),
          BigDecimal("11"),
          BigDecimal("15"),
          BigDecimal("5"),
          BigDecimal("200")),
        DailyPrice(
          LocalDate.of(2017, 1, 3),
          BigDecimal("10"),
          BigDecimal("11"),
          BigDecimal("15"),
          BigDecimal("5"),
          BigDecimal("300")))
      )
      assert(s == Stock("1", "test", Market.Tokyo, "9999", Seq(
        DailyPrice(
          LocalDate.of(2017, 1, 1),
          BigDecimal("10"),
          BigDecimal("11"),
          BigDecimal("15"),
          BigDecimal("5"),
          BigDecimal("100")),
        DailyPrice(
          LocalDate.of(2017, 1, 2),
          BigDecimal("10"),
          BigDecimal("11"),
          BigDecimal("15"),
          BigDecimal("5"),
          BigDecimal("200")),
        DailyPrice(
          LocalDate.of(2017, 1, 3),
          BigDecimal("10"),
          BigDecimal("11"),
          BigDecimal("15"),
          BigDecimal("5"),
          BigDecimal("300"))),
        StockAnalysis(BigDecimal("15"), LocalDate.of(2017, 1, 3))
      ))
    }

    "update settlement list" in {
      val quarterSettlementList1 = Seq(
        QuarterSettlement(
          "1",
          YearMonth.of(2017, 3),
          1,
          LocalDate.of(2016, 4, 1),
          LocalDate.of(2016, 6, 30),
          Some(BigDecimal("1100")),
          Some(BigDecimal("1200")),
          Some(BigDecimal("1300")),
          Some(BigDecimal("1400")),
          LocalDate.of(2017, 3, 31)
        ),
        QuarterSettlement(
          "1",
          YearMonth.of(2017, 3),
          2,
          LocalDate.of(2016, 7, 1),
          LocalDate.of(2016, 9, 30),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          LocalDate.of(2017, 3, 31)
        ),
        QuarterSettlement(
          "1",
          YearMonth.of(2017, 3),
          3,
          LocalDate.of(2016, 10, 1),
          LocalDate.of(2016, 12, 31),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          LocalDate.of(2017, 3, 31)
        ),
        QuarterSettlement(
          "1",
          YearMonth.of(2017, 3),
          4,
          LocalDate.of(2017, 1, 1),
          LocalDate.of(2017, 3, 31),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          LocalDate.of(2017, 3, 31)
        )
      )

      val quarterSettlementList2 = Seq(
        QuarterSettlement(
          "1",
          YearMonth.of(2017, 3),
          1,
          LocalDate.of(2016, 4, 1),
          LocalDate.of(2016, 6, 30),
          Some(BigDecimal("2100")),
          Some(BigDecimal("2200")),
          Some(BigDecimal("2300")),
          Some(BigDecimal("2400")),
          LocalDate.of(2017, 4, 1)
        ),
        QuarterSettlement(
          "1",
          YearMonth.of(2018, 3),
          1,
          LocalDate.of(2017, 4, 1),
          LocalDate.of(2017, 6, 30),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          LocalDate.of(2017, 7, 31)
        )
      )

      val s1 = stock.update(quarterSettlementList1)

      s1.quarterSettlementList shouldBe quarterSettlementList1

      val s2 = s1.update(quarterSettlementList2)

      s2.quarterSettlementList shouldBe Seq(
        QuarterSettlement(
          "1",
          YearMonth.of(2017, 3),
          1,
          LocalDate.of(2016, 4, 1),
          LocalDate.of(2016, 6, 30),
          Some(BigDecimal("2100")),
          Some(BigDecimal("2200")),
          Some(BigDecimal("2300")),
          Some(BigDecimal("2400")),
          LocalDate.of(2017, 4, 1)
        ),
        QuarterSettlement(
          "1",
          YearMonth.of(2017, 3),
          2,
          LocalDate.of(2016, 7, 1),
          LocalDate.of(2016, 9, 30),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          LocalDate.of(2017, 3, 31)
        ),
        QuarterSettlement(
          "1",
          YearMonth.of(2017, 3),
          3,
          LocalDate.of(2016, 10, 1),
          LocalDate.of(2016, 12, 31),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          LocalDate.of(2017, 3, 31)
        ),
        QuarterSettlement(
          "1",
          YearMonth.of(2017, 3),
          4,
          LocalDate.of(2017, 1, 1),
          LocalDate.of(2017, 3, 31),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          LocalDate.of(2017, 3, 31)
        ),
        QuarterSettlement(
          "1",
          YearMonth.of(2018, 3),
          1,
          LocalDate.of(2017, 4, 1),
          LocalDate.of(2017, 6, 30),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          Some(BigDecimal("1000")),
          LocalDate.of(2017, 7, 31)
        )
      )
    }
  }
}
