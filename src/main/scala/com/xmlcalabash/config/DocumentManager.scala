package com.xmlcalabash.config

import com.xmlcalabash.util.ShadowValue
import net.sf.saxon.s9api.XdmNode
import org.xml.sax.InputSource

trait DocumentManager {
  def parse(request: DocumentRequest): DocumentResponse
  def parse(request: DocumentRequest, isource: InputSource): DocumentResponse

  def parseHtml(request: DocumentRequest): DocumentResponse
  def parseHtml(request: DocumentRequest, isource: InputSource): DocumentResponse
}
