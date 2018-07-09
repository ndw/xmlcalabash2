package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.datasource.{Document, Empty, Pipe}
import com.xmlcalabash.runtime.{ExpressionContext, XProcXPathExpression}

import scala.collection.mutable.ListBuffer

class WithInput(override val config: XMLCalabash,
                override val parent: Option[Artifact]) extends Input(config, parent) {

  protected[xml] def this(config: XMLCalabash, parent: Artifact, port: String) {
    this(config, Some(parent))
    _port = Some(port)
  }

  override def validate(): Boolean = {
    // Not super.validate() because Input has more attributes than WithInput
    var valid = true

    _sequence = None
    _primary = None

    _port = attributes.get(XProcConstants._port)

    _select = attributes.get(XProcConstants._select)
    if (_select.isDefined) {
      val context = new ExpressionContext(baseURI, inScopeNS, location)
      _expression = Some(new XProcXPathExpression(context, _select.get))
    }

    val href = attributes.get(XProcConstants._href)
    val pipe = attributes.get(XProcConstants._pipe)

    for (key <- List(XProcConstants._port, XProcConstants._select, XProcConstants._pipe, XProcConstants._href)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    var hasDataSources = false
    var emptyCount = 0
    var nonEmptyCount = 0
    for (child <- children) {
      if (dataSourceClasses.contains(child.getClass)) {
        hasDataSources = true
        valid = valid && child.validate()
        child match {
          case empty: Empty => emptyCount += 1
          case _ => nonEmptyCount += 1
        }
      } else {
        throw new ModelException(ExceptionCode.BADCHILD, child.toString, location)
      }
    }

    if ((emptyCount > 0) && ((emptyCount != 1) || (nonEmptyCount != 0))) {
      throw new ModelException(ExceptionCode.EMPTYNOTALONE, "", location)
    }

    if (href.isDefined) {
      if (hasDataSources) {
        throw XProcException.staticError(81, href.get, location)
      }
      hasDataSources = true

      for (uri <- href.get.split("\\s+")) {
        val doc = new Document(config, this, uri)
        addChild(doc)
      }
    }

    if (pipe.isDefined) {
      if (hasDataSources) {
        throw XProcException.staticError(82, pipe.get, location)
      }
      for (spec <- pipe.get.split("\\s+")) {
        val pos = spec.indexOf("@")
        if (pos > 0) {
          val step = spec.substring(0, pos)
          val port = spec.substring(pos + 1)
          val pipe = if (step == "") {
            new Pipe(config, this, None, Some(port))
          } else {
            new Pipe(config, this, Some(step), Some(port))
          }
          addChild(pipe)
        } else {
          val pipe = new Pipe(config, this, spec)
          addChild(pipe)
        }
      }
    }

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    // Process the children in the context of our parent
    for (child <- children) {
      child.makeGraph(graph, parent)
    }
  }

  override def asXML: xml.Elem = {
    dumpAttr("port", _port)
    dumpAttr("id", id.toString)

    val nodes = ListBuffer.empty[xml.Node]
    if (children.nonEmpty) {
      nodes += xml.Text("\n")
    }
    for (child <- children) {
      nodes += child.asXML
      nodes += xml.Text("\n")
    }
    new xml.Elem("p", "with-input", dump_attr.getOrElse(xml.Null),
      namespaceScope, false, nodes:_*)
  }
}
