package com.xmlcalabash.model.xml

import com.xmlcalabash.model.xml.containers.Container
import net.sf.saxon.NonDelegatingURIResolver

class Output(override val parent: Option[Artifact]) extends Artifact(parent) {
  private var _port: Option[String] = None
  private var _sequence: Option[Boolean] = None
  private var _primary: Option[Boolean] = None
  private var valid = true

  protected[xml] def this(parent: Artifact, port: String, primary: Boolean, sequence: Boolean) {
    this(Some(parent))
    _port = Some(port)
    _primary = Some(primary)
    _sequence = Some(sequence)
  }

  def port: Option[String] = _port
  def primary: Boolean = _primary.getOrElse(false)
  def primary_=(setPrimary: Boolean): Unit = {
    _primary = Some(setPrimary)
  }

  override def validate(): Boolean = {
    _port = properties.get(XProcConstants._port)
    _sequence = lexicalBoolean(properties.get(XProcConstants._sequence))
    _primary = lexicalBoolean(properties.get(XProcConstants._primary))

    for (key <- List(XProcConstants._port, XProcConstants._sequence, XProcConstants._primary)) {
      if (properties.contains(key)) {
        properties.remove(key)
      }
    }

    if (_port.isEmpty) {
      throw new XmlPipelineException("portreq", "Port is required")
    }

    if (properties.nonEmpty) {
      val key = properties.keySet.head
      throw new XmlPipelineException("badopt", s"Unexpected attribute: ${key.getLocalName}")
    }

    if (parent.isDefined && parent.get.isInstanceOf[Container]) {
      for (child <- children) {
        if (dataSourceClasses.contains(child.getClass)) {
          valid = valid && child.validate()
        } else {
          throw new XmlPipelineException("badelem", s"Unexpected element: ${child}")
        }
      }
    } else {
      if (children.nonEmpty) {
        throw new XmlPipelineException("badelem", s"Unexpected element: ${children.head}")
      }
    }

    valid
  }
}
