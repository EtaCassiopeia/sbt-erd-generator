package dev.celestica.doc.erd

import dev.celestica.sql.parser.mysql.MySqlLexer
import dev.celestica.sql.parser.mysql.MySqlParser
import dev.celestica.sql.parser.mysql.MySqlParserBaseListener
import org.antlr.v4.runtime.{CharStreams, CommonTokenStream}
import org.antlr.v4.runtime.tree.ParseTreeWalker

import org.antlr.v4.runtime.{CharStreams, CommonTokenStream}
import org.antlr.v4.runtime.tree.ParseTreeWalker

object TestAntlrParser extends App {
  val input = "CREATE VIEW testView AS SELECT * FROM table"

  val complex = """CREATE OR REPLACE VIEW VACATION_SUMMARY_AMT_INFO AS
                  |SELECT vi.VACATION_DISPLAY_ID                                              AS "VACATION_ID",
                  |       vi.CURRENCY                                                         AS "CURRENCY",
                  |       vi.GROSS_TOTAL_AMOUNT                                               AS "GROSS_TOTAL_AMOUNT",
                  |       vi.SAVINGS_AMOUNT                                                   AS "SAVINGS_TOTAL_AMOUNT",
                  |       (vi.GROSS_TOTAL_AMOUNT - vi.SAVINGS_AMOUNT)                         AS "INVOICE_TOTAL_AMOUNT",
                  |       IFNULL(p.AMOUNT, 0.0)                                               AS "AMOUNT_PAID",
                  |       IF(at.TRANSACTION_TYPE = 'REFUND', at.AMOUNT_PAID, 0.0)             AS "REFUNDS_AMOUNT",
                  |       (vi.GROSS_TOTAL_AMOUNT - vi.SAVINGS_AMOUNT - IFNULL(p.AMOUNT, 0.0)) AS "AMOUNT_DUE"
                  |FROM VACATION_INVOICES vi
                  |         LEFT JOIN PAYMENTS p ON vi.VACATION_ID = p.VACATION_ID
                  |    AND vi.CURRENCY = p.CURRENCY
                  |         LEFT JOIN ACOUNTING_TRANSACTIONS at ON p.TRANSACTION_ID = at.TRANSACTION_ID;""".stripMargin

  val charStream = CharStreams.fromString(complex)

  val lexer = new MySqlLexer(charStream)

  val tokens = new CommonTokenStream(lexer)

  val parser = new MySqlParser(tokens)

  val tree = parser.ddlStatement()

  val walker = new ParseTreeWalker
  val listener = new MySqlParserBaseListener()

  walker.walk(listener, tree)

  println(tree.toStringTree(parser))

  /*
  val listener = new ERDModelListener()
  ParseTreeWalker.DEFAULT.walk(listener, tree)

  val erdModel = listener.getErdModel
   */
}
