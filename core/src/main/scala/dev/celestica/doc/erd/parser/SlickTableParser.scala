package dev.celestica.doc.erd.parser

import dev.celestica.doc.erd.model.Attribute
import dev.celestica.doc.erd.model.Attribute
import dev.celestica.doc.erd.model.Entity

import scala.collection.mutable.ListBuffer
import scala.meta._

class SlickTableParser extends TableDefinitionParser {

  override def parse(sourceCode: String): List[Entity] = {
    val parsedCode = sourceCode.parse[Source].get
    val entities = ListBuffer[Entity]()

    parsedCode.traverse {
      case t: Defn.Trait => processDefn(t, entities)
      case c: Defn.Class => processDefn(c, entities)
      case _ => // Ignore other cases
    }

    entities.toList
  }

  private def processDefn(defn: Defn, entities: ListBuffer[Entity]): Unit =
    defn match {
      case t: Defn.Trait => t.templ.stats.foreach(processScalaTree(_, entities))
      case c: Defn.Class => c.templ.stats.foreach(processScalaTree(_, entities))
      case _ => // Ignore other cases
    }

  private def processScalaTree(tree: Tree, entities: ListBuffer[Entity]): Unit =
    tree match {
      case cls: Defn.Class if isSlickTableClass(cls) =>
        println(s"Found Slick Table Class: ${cls.name.value}")
        val entity = extractEntityFromTree(cls)
        entities += entity
      case _ => // Ignore other cases
    }

  private def isSlickTableClass(cls: Defn.Class): Boolean =
    cls.templ.inits.exists {
      case Init.After_4_6_0(Type.Name("Table"), _, _) |
      Init.After_4_6_0(Type.Apply.After_4_6_0(Type.Name("Table"), _), _, _) =>
        println("Found a Slick table class")
        true
      case _ => false
    }

  private def extractEntityFromTree(cls: Defn.Class): Entity = {
    val tableName = extractTableName(cls)
    val attributes = extractAttributes(cls)

    Entity(tableName, attributes)
  }

  private def extractTableName(cls: Defn.Class): String =
    // Extract the constructor and find the table name argument
    cls.ctor.paramss.flatten
      .collectFirst { case param"tag: Tag" =>
        extractTableNameFromConstructor(cls)
      }
      .getOrElse("Unknown")

  private def extractTableNameFromConstructor(cls: Defn.Class): String =
    // Find the constructor call to Table and extract the table name
    cls.templ.inits
      .collectFirst { case Init(_, _, List(List(_, Lit(litTableName: String)))) =>
        litTableName
      }
      .getOrElse("Unknown")

  private def extractAttributes(cls: Defn.Class): List[Attribute] = {
    // First, identify any composite primary keys
    val compositePrimaryKeys = identifyCompositePrimaryKeys(cls)

    cls.templ.stats.flatMap {
      case method: Decl.Def if method.decltpe.toString.startsWith("Rep") =>
        Some(parseAttribute(method, compositePrimaryKeys))
      case defn: Defn.Def if defn.body.toString.startsWith("column") =>
        Some(parseAttributeFromDef(defn, compositePrimaryKeys))
      case _ => None
    }
  }

  private def identifyCompositePrimaryKeys(cls: Defn.Class): Set[String] =
    cls.templ.stats
      .collect {
        case defn: Defn.Def if defn.body.toString.startsWith("primaryKey") =>
          extractColumnNamesFromPrimaryKey(defn)
      }
      .flatten
      .toSet

  private def extractColumnNamesFromPrimaryKey(defn: Defn.Def): Set[String] =
    defn.body match {
      case q"primaryKey($name, $tuple)" =>
        extractNamesFromTuple(tuple)
      case _ =>
        Set.empty[String]
    }

  private def extractNamesFromTuple(tuple: Term): Set[String] =
    tuple match {
      case q"(..$elems)" =>
        elems.collect { case Term.Name(name) =>
          name
        }.toSet
      case _ =>
        Set.empty[String]
    }

  private def parseAttribute(method: Decl.Def, compositePrimaryKeys: Set[String]): Attribute = {
    val attributeName = method.name.value
    val (dataType, isNullable) = parseType(method.decltpe)
    val isPrimaryKey = compositePrimaryKeys.contains(attributeName)

    Attribute(attributeName, dataType, isNullable, isPrimaryKey)
  }

  private def parseAttributeFromDef(defn: Defn.Def, compositePrimaryKeys: Set[String]): Attribute = {
    val attributeName = defn.name.value
    val (dataType, isNullable) = extractDataTypeFromColumnDefinition(defn.body)
    val isPrimaryKey = isPrimaryKeyColumn(defn) || compositePrimaryKeys.contains(attributeName)

    Attribute(attributeName, dataType, isPrimaryKey, isNullable)
  }

  private def isPrimaryKeyColumn(defn: Defn.Def): Boolean =
    defn.body.toString.contains("PrimaryKey") || defn.body.toString.startsWith("primaryKey")

  private def parseType(tpe: Type): (String, Boolean) =
    tpe match {
      case Type.Name(name) => (name, false)
      // TODO: handle enum types, e.g. TransactionTypeEnum
      case Type.Apply(Type.Name("Option"), Type.Name(innerType) :: Nil) => (innerType, true)
      case _ => ("Unknown", false)
    }

  private def extractDataTypeFromColumnDefinition(body: Term): (String, Boolean) =
    // Extract the Scala data type and check for Option
    body match {
      case q"column[$tpe](..$args)" => parseType(tpe)
      case _ => ("Unknown", false)
    }
}
