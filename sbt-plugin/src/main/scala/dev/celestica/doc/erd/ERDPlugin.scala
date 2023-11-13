package dev.celestica.doc.erd

import dev.celestica.doc.erd.generator.MermaidFormatter
import dev.celestica.doc.erd.model.ERDModel
import dev.celestica.doc.erd.model.Entity
import dev.celestica.doc.erd.model.Relationship
import dev.celestica.doc.erd.model.Relationship
import dev.celestica.doc.erd.model.RelationshipType
import dev.celestica.doc.erd.model.View
import dev.celestica.doc.erd.parser.SlickTableParser
import sbt.*
import sbt.Keys.*

import scala.io.Source

object ERDPlugin extends AutoPlugin {

  object autoImport {
    val erdGen = taskKey[Unit]("Generate ERD diagram")
    val erdPublish = taskKey[Unit]("Publish ERD diagram to a third party application")
    val erdOutputFile = settingKey[File]("Output file for ERD diagram")
    val erdRelationships = settingKey[Seq[String]]("Defines relationships between entities")
    val erdViewFiles = settingKey[Map[String, Seq[String]]]("Specifies SQL files with specific views to include")
    // Add other settings for Confluence publishing
  }

  import autoImport.*

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    erdOutputFile := target.value / "erd-diagram.mmd",
    erdRelationships := Seq(),
    erdViewFiles := Map(),
    erdGen := {
      val outputFile = erdOutputFile.value
      val relationshipsConfig = erdRelationships.value
      val sourceFiles = (Compile / sources).value
      val relationships = Relationship.parseRelationshipConfig(relationshipsConfig)
      val viewConfig = erdViewFiles.value
      val views = viewConfig.toList.flatMap { case (filePath, viewNames) =>
        parseSpecificViewsFromSqlFile(filePath, viewNames)
      }
      val erdModel = generateERDModel(sourceFiles, relationships, views)
      val formatter = new MermaidFormatter()
      IO.write(outputFile, formatter.format(erdModel))
    },
    erdPublish := {
      // Implement logic to publish the ERD diagram
    }
  )

  private def generateERDModel(sourceFiles: Seq[File], relationships: Seq[Relationship], views: Seq[View]): ERDModel = {
    val parser = new SlickTableParser()
    val scalaSourceFiles = sourceFiles.filter(isStandardScalaFile)
    val allEntities = scalaSourceFiles.flatMap { file =>
      val sourceCode = Source.fromFile(file).getLines().mkString("\n")
      parser.parse(sourceCode)
    }.toList

    // Directly associate relationships with entities as defined
    val updatedEntities = allEntities.map { entity =>
      val entityRelationships = relationships.filter(rel => rel.entity1 == entity.name || rel.entity2 == entity.name)
      entity.copy(relationships = entityRelationships.toList)
    }

    ERDModel(updatedEntities, views.toList)
  }

  def parseSpecificViewsFromSqlFile(filePath: String, viewNames: Seq[String]): Seq[View] = {
    val fileContent = Source.fromFile(filePath).getLines().mkString("\n")
    View.extractViewsFromSql(fileContent, viewNames)
  }

  private def isStandardScalaFile(file: File): Boolean =
    file.getPath.endsWith(".scala") && !file.getPath.contains("/routes")
}
