package com.xmlcalabash.model.xml

import scala.collection.mutable

// OMG this is an ugly hack. DeclareStep introduces a new first step; that needs to
// be the drp for the rest of the pipeline. So this object handles remapping.
// There really must be a better way.

object DrpRemap {
  private val _remap = mutable.HashMap.empty[IOPort,IOPort]

  def remap(drp: IOPort, alt: IOPort): Unit = {
    _remap.put(drp, alt)
  }

  def map(drp: IOPort): Option[IOPort] = _remap.get(drp)
}
