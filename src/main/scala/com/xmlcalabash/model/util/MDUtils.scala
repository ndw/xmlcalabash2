package com.xmlcalabash.model.util

import com.jafpl.messages.Metadata
import com.xmlcalabash.runtime.XProcMetadata

object MDUtils {
  def contentType(metadata: Metadata): String = {
    metadata match {
      case meta: XProcMetadata =>
        meta.contentType
      case _ =>
        if ((metadata == Metadata.STRING) || (metadata == Metadata.NUMBER) || (metadata == Metadata.BOOLEAN)) {
          "text/plain"
        } else {
          "application/octet-stream"
        }
    }
  }

  def textContentType(metadata: Metadata): Boolean = {
    metadata match {
      case meta: XProcMetadata =>
        meta.contentType.startsWith("text/")
      case _ =>
        (metadata == Metadata.STRING) || (metadata == Metadata.NUMBER) || (metadata == Metadata.BOOLEAN)
    }
  }

  def xmlContentType(metadata: Metadata): Boolean = {
    metadata match {
      case meta: XProcMetadata =>
        ValueParser.xmlContentType(meta.contentType)
      case _ =>
        false
    }
  }

  def jsonContentType(metadata: Metadata): Boolean = {
    metadata match {
      case meta: XProcMetadata =>
        ValueParser.jsonContentType(meta.contentType)
      case _ =>
        false
    }
  }

}
