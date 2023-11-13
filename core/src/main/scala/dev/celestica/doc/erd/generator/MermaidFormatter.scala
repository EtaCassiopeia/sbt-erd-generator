package dev.celestica.doc.erd.generator

import dev.celestica.doc.erd.model.ERDModel
import dev.celestica.doc.erd.model.ERDModel
import dev.celestica.doc.erd.model.Relationship
import dev.celestica.doc.erd.model.Relationship
import dev.celestica.doc.erd.model.RelationshipType
import dev.celestica.doc.erd.model.RelationshipType

class MermaidFormatter extends OutputFormatter {

  override def format(erdModel: ERDModel): String = {
    val diagram = new StringBuilder
    diagram.append("erDiagram\n")

    // Format entities
    erdModel.entities.foreach { entity =>
      diagram.append(s"    ${entity.name} {\n")
      entity.attributes.foreach { attribute =>
        diagram.append(s"        ${attribute.dataType.toLowerCase} ${attribute.name}\n")
      }
      diagram.append("    }\n")
    }

    // Format views with columns
    erdModel.views.foreach { view =>
      diagram.append(s"    ${view.name} {\n")
      view.columns.foreach { column =>
        diagram.append(s"        ${column.dataType} ${column.name}\n")
      }
      diagram.append("    }\n")
    }

    // Format relationships, ensuring unique relationships
    val uniqueRelationships = getUniqueRelationships(
      erdModel.entities.flatMap(_.relationships) ++ erdModel.views.flatMap(_.relationships)
    )
    uniqueRelationships.foreach { relationship =>
      val relationSyntax = getRelationshipSyntax(relationship)
      diagram.append(s"    ${relationship.entity1} ${relationSyntax} ${relationship.entity2}\n")
    }

    diagram.toString
  }

  private def getUniqueRelationships(relationships: Seq[Relationship]): Seq[Relationship] =
    relationships.distinct

  private def getRelationshipSyntax(relationship: Relationship): String =
    relationship.relationshipType match {
      case RelationshipType.OneToOne => s"-- ${relationship.entity2} : ${relationship.label}"
      case RelationshipType.OneToMany => s"||--o{ ${relationship.entity2} : ${relationship.label}"
      case RelationshipType.ManyToOne => s"o--|| ${relationship.entity2} : ${relationship.label}"
      case RelationshipType.ManyToMany => s"--|{ ${relationship.entity2} : ${relationship.label}"
    }
}
