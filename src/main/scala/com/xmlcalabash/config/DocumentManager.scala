package com.xmlcalabash.config

import org.xml.sax.InputSource

trait DocumentManager {
  def parse(request: DocumentRequest): DocumentResponse
  def parse(request: DocumentRequest, isource: InputSource): DocumentResponse
}
