package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException

import scala.collection.mutable.ListBuffer

class Port(override val config: XMLCalabashConfig) extends Artifact(config) {
  protected var _port = ""
  protected[xml] var _sequence = Option.empty[Boolean]
  protected[xml] var _primary = Option.empty[Boolean]
  protected[xml] var _select = Option.empty[String]

  protected var _href = Option.empty[String]
  protected var _pipe = Option.empty[String]

  def port: String = _port
  protected[model] def port_=(port: String): Unit = {
    _port = port
  }
  def sequence: Boolean = _sequence.getOrElse(false)
  protected[model] def sequence_=(seq: Boolean): Unit = {
    _sequence = Some(seq)
  }
  def primary: Boolean = _primary.getOrElse(false)
  protected[model] def primary_=(primary: Boolean): Unit = {
    _primary = Some(primary)
  }
  def select: Option[String] = _select

  def step: NamedArtifact = {
    if (parent.isDefined) {
      parent.get match {
        case art: NamedArtifact => art
        case _ => throw new RuntimeException("parent of port isn't a named artifact?")
      }
    } else {
      throw new RuntimeException("port has no parent?")
    }
  }

  def bindings: List[DataSource] = {
    val lb = ListBuffer.empty[DataSource]
    for (child <- allChildren) {
      child match {
        case ds: DataSource => lb += ds
        case _ => Unit
      }
    }
    lb.toList
  }

  override protected[model] def makeStructureExplicit(environment: Environment): Unit = {
    if (_href.isDefined && _pipe.isDefined) {
      throw XProcException.xsPipeAndHref(location)
    }

    if (_href.isDefined && allChildren.nonEmpty) {
      throw XProcException.xsHrefAndOtherSources(location)
    }

    if (_pipe.isDefined && allChildren.nonEmpty) {
      throw XProcException.xsPipeAndOtherSources(location)
    }

    if (_href.isDefined) {
      val doc = new Document(config)
      doc.href = staticContext.baseURI.get.resolve(_href.get).toASCIIString
      addChild(doc)
    }

    if (_pipe.isDefined) {
      for (shortcut <- _pipe.get.split("\\s+")) {
        var port = Option.empty[String]
        var step = Option.empty[String]
        if (shortcut.contains("@")) {
          val re = "(.*)@(.*)".r
          shortcut match {
            case re(pname, sname) =>
              if (pname != "") {
                port = Some(pname)
              }
              step = Some(sname)
          }
        } else {
          if (shortcut.trim() != "") {
            port = Some(shortcut)
          }
        }

        val pipe = new Pipe(config)
        if (step.isDefined) {
          pipe.step = step.get
        }
        if (port.isDefined) {
          pipe.port = port.get
        }
        addChild(pipe)
      }
    }

    for (child <- allChildren) {
      child.makeStructureExplicit(environment)
    }
  }

  override protected[model] def validateStructure(): Unit = {
    for (child <- allChildren) {
      child.validateStructure()
    }

    var empty = false
    var nonEmpty = false
    var pns = false
    var implinline = false
    for (child <- allChildren) {
      child match {
        case source: Document =>
          nonEmpty = true
          pns = true
        case source: Empty =>
          empty = true
          pns = true
        case source: Inline =>
          nonEmpty = true
          if (source.implied) {
            implinline = true
          } else {
            pns = true
          }
        case source: Pipe =>
          nonEmpty = true
          pns = true
        case source: NamePipe => Unit
        case _ => throw new RuntimeException(s"Unexpected port binding: $child")
      }
    }

    if (empty && nonEmpty) {
      throw XProcException.xsNoSiblingsOnEmpty(location)
    }

    if (pns && implinline) {
      throw XProcException.xsInvalidPipeline("Cannot combine implicit inlines with elements from the p: namespace", location)
    }
  }
}
