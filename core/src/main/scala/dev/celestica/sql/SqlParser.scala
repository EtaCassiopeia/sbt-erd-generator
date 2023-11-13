package dev.celestica.sql

import fastparse._, NoWhitespace._

object SqlParser {
  private[sql] def keyword[_: P](k: String): P[Unit] = P(IgnoreCase(k) ~ !CharIn("a-zA-Z0-9_"))

  private[sql] def identifier[_: P]: P[String] = P(CharIn("a-zA-Z_") ~ CharIn("a-zA-Z0-9_").rep).!

  private def ws[_: P]: P[Unit] = P(CharIn(" \t\r\n").rep(1))

//  private def expression[_: P]: P[String] = P(CharsWhile(_ != ',').!).map(_.trim)
  private def expression[_: P]: P[String] = P(CharIn("a-zA-Z_") ~ CharIn("a-zA-Z0-9_").rep).!

  private def columnAlias[_: P]: P[ColumnAlias] =
    P(expression ~ (ws ~ keyword("AS") ~ ws ~ identifier).?).map {
      case (exp, Some(alias)) => ColumnAlias(exp, alias)
      case (exp, None) => ColumnAlias(exp, exp)
    }

  private def selectClause[_: P]: P[SelectClause] = P(
    keyword("SELECT").log ~ ws.log ~ (selectAll.log | columnAlias.rep(1, sep = P("," ~ ws.?)).log).map(SelectClause)
  )

  private def selectAll[_: P]: P[Seq[ColumnAlias]] = P("*").map(_ => Seq(ColumnAlias("*", "*")))

  private def joinType[_: P]: P[String] = P(
    StringInIgnoreCase("LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "OUTER JOIN", "JOIN")
  ).!

  private def tableWithAlias[_: P]: P[Table] = {
    val joinKeyword = P(StringInIgnoreCase("LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "OUTER JOIN", "JOIN").!)
    P(identifier ~ (ws ~ (!joinKeyword ~ identifier).?).?).map {
      case (name, Some(Some(alias))) => Table(name, Some(alias))
      case (name, _) => Table(name, None)
    }
  }

  private def joinCondition[_: P]: P[String] =
    P(ws ~ "ON" ~ ws ~ (CharsWhile(c => c != '\n' || c != ';') | End).!).map(_.trim)

  private def joinClause[_: P]: P[JoinClause] =
    P(joinType ~ ws ~ tableWithAlias ~ joinCondition).map { case (joinType, table, condition) =>
      JoinClause(joinType, table, condition)
    }

  private def fromClause[_: P]: P[FromClause] = P(
    keyword("FROM") ~ ws ~ tableWithAlias ~ joinClause
      .rep(sep = P(ws ~ keyword("AND") ~ ws))
      .?
      .map(_.getOrElse(Seq.empty))
  ).map { case (table, joins) =>
    FromClause(table, joins)
  }

  private def viewParser[_: P]: P[View] = P(
    keyword("CREATE").log ~ (ws ~ keyword("OR") ~ ws ~ keyword("REPLACE")).? ~ ws ~ keyword(
      "VIEW"
    ).log ~ ws ~ identifier.log ~ ws ~ keyword("AS").log ~ ws.log ~ selectClause.log ~ ws ~ fromClause.log
  ).map(View.tupled)

  def parseCreateViewStatement(stmt: String): Parsed[View] = {
    val result: Parsed[View] = parse(stmt, viewParser(_))

    result match {
      case Parsed.Success(value, index) => println(s"Successfully parsed SQL statement: $value")
      case Parsed.Failure(expected, failIndex, extra) =>
        println(s"Failed to parse SQL statement: $stmt : at [${stmt.substring(failIndex)}]")
        println(s"Expected: $expected")
        println(s"Fail index: $failIndex")
        println(s"Extra: ${extra.trace().longAggregateMsg}")
    }

    result
  }
}
