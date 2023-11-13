package dev.celestica.doc.erd.model

case class ViewColumn(name: String, dataType: String)

case class View(name: String, columns: List[ViewColumn], relationships: List[Relationship])

object View {
  def extractViewsFromSql(fileContent: String, viewNames: Seq[String]): Seq[View] = {
    val viewPattern = "(?is)CREATE OR REPLACE VIEW (\\w+) AS (.*?)(?=CREATE OR REPLACE VIEW|$)".r
    viewPattern
      .findAllIn(fileContent)
      .matchData
      .collect {
        case m if viewNames.contains(m.group(1)) =>
          val viewName = m.group(1)
          println(s"Extracting view $viewName")
          val viewDefinition = m.group(2).trim
          val columns = extractColumnsFromViewDefinition(viewDefinition)
          println(s"Columns: $columns")
          val relationships = extractRelationshipsFromViewDefinition(viewDefinition, viewName)
          println(s"Relationships: $relationships")
          View(viewName, columns, relationships)
      }
      .toSeq
  }

  private def extractColumnsFromViewDefinition(viewDefinition: String): List[ViewColumn] = {
    val columnPattern = "(?i)SELECT (.+) FROM".r
    columnPattern.findFirstMatchIn(viewDefinition) match {
      case Some(m) =>
        m.group(1)
          .split(",")
          .map(_.trim.split("\\s+").last)
          .filter(_.nonEmpty)
          .map { columnName =>
            ViewColumn(
              columnName,
              "unknown"
            ) // Data type is set as 'unknown' due to complexity in inferring it from SQL
          }
          .toList
      case None => List.empty
    }
  }

  private def extractRelationshipsFromViewDefinition(viewDefinition: String, viewName: String): List[Relationship] = {
    val fromPattern = "(?is)FROM\\s+(\\w+).*".r
    val joinPattern = "(?is)JOIN\\s+(\\w+).*".r
    val fromTable = fromPattern.findFirstMatchIn(viewDefinition).map(_.group(1)).toList
    val joinedTables = joinPattern.findAllMatchIn(viewDefinition).map(_.group(1)).toList
    (fromTable ++ joinedTables).distinct.map(table =>
      Relationship(viewName, table, RelationshipType.OneToMany, "provides data for")
    )
  }
}
