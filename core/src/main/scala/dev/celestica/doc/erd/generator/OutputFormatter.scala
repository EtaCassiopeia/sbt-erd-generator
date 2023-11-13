package dev.celestica.doc.erd.generator

import dev.celestica.doc.erd.model.ERDModel
import dev.celestica.doc.erd.model.ERDModel

trait OutputFormatter {
  def format(erdModel: ERDModel): String
}
