package dev.celestica.doc.erd.model

case class Attribute(name: String, dataType: String, isPrimaryKey: Boolean = false, isNullable: Boolean)
