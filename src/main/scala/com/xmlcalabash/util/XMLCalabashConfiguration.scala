package com.xmlcalabash.util

import java.io.File

import com.xmlcalabash.runtime.URIUtils
import net.sf.saxon.s9api.{Processor, XdmNode}
import org.slf4j.{Logger, LoggerFactory}

class XMLCalabashConfiguration {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def load(): Unit = {
    var fn = URIUtils.homeAsURI.toASCIIString + ".xmlcalabash"
    var cfg = new File(fn)
    if (cfg.exists()) {
      load(cfg)
    } else {
      fn = URIUtils.homeAsURI.toASCIIString + ".calabash"
      cfg = new File(fn)
      if (cfg.exists()) {
        load(cfg)
      }
    }

    fn = URIUtils.cwdAsURI.toASCIIString + ".xmlcalabash"
    cfg = new File(fn)
    if (cfg.exists()) {
      load(cfg)
    } else {
      var fn = URIUtils.cwdAsURI.toASCIIString + ".calabash"
      var cfg = new File(fn)
      if (cfg.exists()) {
        load(cfg)
      }
    }
  }

  def load(cfg: File): Unit = {
    val processor = new Processor(false) // explicitly our own because we don't know about schema awareness yet
    val builder = processor.newDocumentBuilder()
    builder.setDTDValidation(false)
    builder.setLineNumbering(true)
    val root = builder.build(cfg)
    parse(root)
  }

  private def parse(node: XdmNode): Unit = {
    logger.info("Parsing not yet implemented: " + node.getBaseURI)
  }
}
