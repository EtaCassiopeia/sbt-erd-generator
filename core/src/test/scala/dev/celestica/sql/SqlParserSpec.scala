package dev.celestica.sql

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import fastparse._
import fastparse.MultiLineWhitespace._
import fastparse.Whitespace

import com.facebook.presto.sql.tree._
import com.facebook.presto.sql.parser._

import net.sf.jsqlparser.parser._

class SqlParserSpec extends AnyFlatSpec with Matchers {

  "SqlParser" should "correctly parse SQL keywords" in {
    val result = parse("CREATE", SqlParser.keyword("CREATE")(_))
    result shouldBe a[Parsed.Success[_]]
  }

  it should "correctly parse identifiers" in {
    val testCases = Seq("VACATION_SUMMARY_AMT_INFO", "testView", "test")

    testCases.foreach { testCase =>
      val result = parse(testCase, SqlParser.identifier(_))
      result shouldBe a[Parsed.Success[_]]
      result.get.value shouldBe testCase
    }
  }

  it should "parse CREATE VIEW statement start" in {
    val result = SqlParser.parseCreateViewStatement("CREATE VIEW testView AS SELECT * FROM table")
    result shouldBe a[Parsed.Success[_]]
  }

  it should "correctly parse column aliases in SELECT clause" in {
    val selectSql = "SELECT column1 AS alias1, column2 AS alias2"
    val result = SqlParser.parseCreateViewStatement(s"CREATE VIEW testView AS $selectSql FROM table")
    result.get.value.select.columns should contain allOf (ColumnAlias("column1", "alias1"), ColumnAlias(
      "column2",
      "alias2"
    ))
  }

  it should "correctly parse column aliases in SELECT clause event without spaces" in {
    val selectSql = "SELECT column1 AS alias1,column2 AS alias2"
    val result = SqlParser.parseCreateViewStatement(s"CREATE VIEW testView AS $selectSql FROM table")
    result.get.value.select.columns should contain allOf (ColumnAlias("column1", "alias1"), ColumnAlias(
      "column2",
      "alias2"
    ))
  }

  it should "parse FROM clause with a single table" in {
    val fromSql = "FROM table1"
    val result = SqlParser.parseCreateViewStatement(s"CREATE VIEW testView AS SELECT * $fromSql")
    result.get.value.from.table.name shouldBe "table1"
  }

  it should "parse JOIN clauses" in {
    val joinSql = "FROM table1 LEFT JOIN table2 ON table1.id = table2.id"
    val result = SqlParser.parseCreateViewStatement(s"CREATE VIEW testView AS SELECT * $joinSql")
    result.get.value.from.joins should contain(JoinClause("LEFT JOIN", Table("table2"), "table1.id = table2.id"))
  }

  it should "fail to parse invalid SQL" in {
    val invalidSql = "CREATE VEW testView AS SELECT * FROM table"
    val result = SqlParser.parseCreateViewStatement(invalidSql)
    result shouldBe a[Parsed.Failure]
  }

  it should "parse a complex CREATE VIEW statement with joins" in {
    val sqlStatement =
      """
      |CREATE OR REPLACE VIEW VACATION_SUMMARY_AMT_INFO AS
      |SELECT vi.VACATION_DISPLAY_ID AS "VACATION_ID",
      |       vi.CURRENCY AS "CURRENCY",
      |       vi.GROSS_TOTAL_AMOUNT AS "GROSS_TOTAL_AMOUNT",
      |       vi.SAVINGS_AMOUNT AS "SAVINGS_TOTAL_AMOUNT",
      |       (vi.GROSS_TOTAL_AMOUNT - vi.SAVINGS_AMOUNT) AS "INVOICE_TOTAL_AMOUNT",
      |       IFNULL(p.AMOUNT, 0.0) AS "AMOUNT_PAID",
      |       IF(at.TRANSACTION_TYPE = 'REFUND', at.AMOUNT_PAID, 0.0) AS "REFUNDS_AMOUNT",
      |       (vi.GROSS_TOTAL_AMOUNT - vi.SAVINGS_AMOUNT - IFNULL(p.AMOUNT, 0.0)) AS "AMOUNT_DUE"
      |FROM VACATION_INVOICES vi
      |     LEFT JOIN PAYMENTS p ON vi.VACATION_ID = p.VACATION_ID
      |AND vi.CURRENCY = p.CURRENCY
      |     LEFT JOIN ACOUNTING_TRANSACTIONS at ON p.TRANSACTION_ID = at.TRANSACTION_ID;
    """.stripMargin.trim

    val result = SqlParser.parseCreateViewStatement(sqlStatement)

    result match {
      case Parsed.Success(view, _) =>
        view.name shouldBe "VACATION_SUMMARY_AMT_INFO"
        view.select.columns should have size 8
        view.from.table shouldBe "VACATION_INVOICES"
        view.from.joins should have size 2
        view.from.joins.head.table shouldBe "PAYMENTS"
        view.from.joins(1).table shouldBe "ACOUNTING_TRANSACTIONS"

      case _ => fail("Parsing failed")
    }
  }
}
