package dev.celestica.doc.erd.model

object RelationshipType extends Enumeration {
  val OneToOne, OneToMany, ManyToOne, ManyToMany = Value
}

case class Relationship(
  entity1: String,
  entity2: String,
  relationshipType: RelationshipType.Value,
  label: String
)
object Relationship {

  /**
   * "Entity1 -- Entity2" // One-to-One "Entity1 --o Entity2" // One-to-Many "Entity1 o-- Entity2" // Many-to-One
   * "Entity1 --|{ Entity2" // Many-to-Many
   */
  def parseRelationshipConfig(config: Seq[String]): Seq[Relationship] =
    config.map { relConfig =>
      val pattern = "(\\w+) (\\|\\|--o\\{|o--\\|\\||--|--) (\\w+) : (\\w+)".r
      relConfig match {
        case pattern(entity1, relTypeSymbol, entity2, label) =>
          val relType = determineRelationshipType(relTypeSymbol)
          Relationship(entity1, entity2, relType, label)
        case _ => throw new IllegalArgumentException(s"Invalid relationship syntax: $relConfig")
      }
    }

  private def determineRelationshipType(relTypeSymbol: String): RelationshipType.Value =
    relTypeSymbol match {
      case "||--o{" => RelationshipType.OneToMany
      case "o--||" => RelationshipType.ManyToOne
      case "--" => RelationshipType.OneToOne
      case "--|{" => RelationshipType.ManyToMany
      case _ => throw new IllegalArgumentException(s"Unknown relationship type symbol: $relTypeSymbol")
    }
}
