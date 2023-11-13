package dev.celestica.sql.antlr

import dev.celestica.doc.erd.model.ERDModel
import dev.celestica.doc.erd.model.View
import dev.celestica.doc.erd.model.ViewColumn
import dev.celestica.sql.parser.mysql.MySqlParserBaseListener
import dev.celestica.sql.parser.mysql.MySqlParser

class ERDModelListener extends MySqlParserBaseListener {
  var erdModel: ERDModel = ERDModel(List.empty, List.empty)
  var currentView: Option[View] = None

  override def enterCreateView(ctx: MySqlParser.CreateViewContext): Unit = {
    val viewName = ctx.fullId.getText
    currentView = Some(View(viewName, List.empty, List.empty))
  }

//  override def enterSelectElement(ctx: MySqlParser.SelectElementContext): Unit = {
//    val columnName = ctx.toString() // .fullColumnName.getText
//    val dataType = "String" // Placeholder, actual logic to determine dataType needed
//    currentView = currentView.map(v => v.copy(columns = v.columns :+ ViewColumn(columnName, dataType)))
//  }

  override def exitDdlStatement(ctx: MySqlParser.DdlStatementContext): Unit = {
    currentView.foreach(v => erdModel = erdModel.copy(views = erdModel.views :+ v))
    currentView = None
  }

  def getErdModel: ERDModel = erdModel
}