package dev.celestica.doc.erd.model

case class Entity(name: String, attributes: List[Attribute], relationships: List[Relationship] = List.empty)
