package dev.celestica.doc.erd

import dev.celestica.doc.erd.generator.MermaidFormatter
import dev.celestica.doc.erd.model.ERDModel
import dev.celestica.doc.erd.parser.SlickTableParser

import java.io.File
import java.io.PrintWriter
import scala.io.Source

object TestSlickTableParser {
  def main(args: Array[String]): Unit = {

    val formatter = new MermaidFormatter()

    val files = List(
      "/Users/mohsen/code/Improving/NCL/proxima-vacation-analytics/app/com/ncl/vacation/analytics/repositories/tables/DestinationHQSailItineraryTable.scala",
      "/Users/mohsen/code/Improving/NCL/proxima-vacation-analytics/app/com/ncl/vacation/analytics/repositories/tables/AccountingTransactionTable.scala"
    )

    val erdModel = files
      .map { file =>
        println(s"Processing file: $file")
        processFile(file)
      }
      .foldLeft(ERDModel(List.empty))((acc, model) => acc.copy(acc.entities ++ model.entities))

    println(erdModel)

    val mermaidOutput = formatter.format(erdModel)
    writeToFile(mermaidOutput, "target/erd.md")
  }

  private def processFile(str: String): ERDModel = {
    val source = Source.fromFile(str)
    val lines = source.getLines().mkString("\n")
    source.close()

    val parser = new SlickTableParser()
    val entities = parser.parse(lines)
    model.ERDModel(entities)
  }

  def writeToFile(content: String, filePath: String): Unit = {
    val file = new File(filePath)
    val pw = new PrintWriter(file)
    try
      pw.write(content)
    finally
      pw.close()
  }
}
