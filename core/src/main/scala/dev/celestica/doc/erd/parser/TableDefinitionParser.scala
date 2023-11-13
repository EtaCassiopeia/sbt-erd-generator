package dev.celestica.doc.erd.parser

import dev.celestica.doc.erd.model.Entity

trait TableDefinitionParser {
  def parse(sourceCode: String): List[Entity]
}
