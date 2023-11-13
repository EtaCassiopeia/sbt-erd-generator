package dev.celestica.sql

sealed trait SqlElement

case class View(name: String, select: SelectClause, from: FromClause) extends SqlElement

case class Table(name: String, alias: Option[String] = None) extends SqlElement

case class SelectClause(columns: Seq[ColumnAlias]) extends SqlElement

case class JoinClause(joinType: String, table: Table, on: String) extends SqlElement

case class FromClause(table: Table, joins: Seq[JoinClause]) extends SqlElement

case class ColumnAlias(expression: String, alias: String) extends SqlElement

//object View {
//  def toMermaid(view: View): String = {
//    val columns = view.select.columns.map(c => s"${c.alias}: ${c.expression}").mkString("\n    ")
//    val joins = view.from.joins.map(join => s"${join.table} <|.. ${view.from.table}: ${join.on}").mkString("\n    ")
//    s"""classDiagram
//         class ${view.name} {
//           $columns
//         }
//         ${view.name} --|> ${view.from.table}
//         $joins
//      """
//  }
//}
