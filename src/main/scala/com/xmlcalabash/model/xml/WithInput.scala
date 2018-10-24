package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.datasource.{DataSource, Document, Empty, Inline}
import com.xmlcalabash.runtime.{ExpressionContext, XMLCalabashRuntime, XProcXPathExpression}

import scala.collection.mutable.ListBuffer

class WithInput(override val config: XMLCalabashRuntime,
                override val parent: Option[Artifact]) extends Input(config, parent) {

  protected[xml] def this(config: XMLCalabashRuntime, parent: Artifact, port: String) {
    this(config, Some(parent))
    _port = Some(port)
  }

  override def validate(): Boolean = {
    // Not super.validate() because Input has more attributes than WithInput
    var valid = true

    // Repeat what Artifact.validate() does.
    if (attributes.contains(XProcConstants._expand_text)) {
      expandText = lexicalBoolean(attributes.get(XProcConstants._expand_text)).get
    } else {
      if (parent.isDefined) {
        expandText = parent.get.expandText
      } else {
        expandText = true
      }
    }

    // The contents of p:inline are never parsed as Artifacts, so we can check this here
    if ((nodeName.getNamespaceURI == XProcConstants.ns_p && attributes.contains(XProcConstants._inline_expand_text))
      || (nodeName.getNamespaceURI != XProcConstants.ns_p && attributes.contains(XProcConstants.p_inline_expand_text))) {
      throw XProcException.xsInlineExpandTextNotAllowed(location)
    }

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
    var hasImplicit = false
    var hasExplicit = false
    for (child <- children) {
      child match {
        case ds: DataSource =>
          hasDataSources = true
          valid = valid && child.validate()
          child match {
            case inline: Inline =>
              hasImplicit = hasImplicit || inline.isImplicit
              hasExplicit = hasExplicit || !inline.isImplicit
              nonEmptyCount += 1
            case empty: Empty =>
              emptyCount += 1
              hasExplicit = true
            case _ =>
              nonEmptyCount += 1
              hasExplicit = true
          }
          if (hasImplicit && hasExplicit) {
            throw XProcException.xsElementNotAllowed(child.location, child.nodeName, "cannot mix implicit inlines with elements in the XProc namespace")
          }
        case d: Documentation =>
          hasExplicit = true
          if (hasImplicit) {
            throw XProcException.xsElementNotAllowed(child.location, child.nodeName, "cannot mix implicit inlines with elements in the XProc namespace")
          }
        case p: PipeInfo =>
          hasExplicit = true
          if (hasImplicit) {
            throw XProcException.xsElementNotAllowed(child.location, child.nodeName, "cannot mix implicit inlines with elements in the XProc namespace")
          }
        case _ => throw XProcException.xsElementNotAllowed(location, child.nodeName)
      }
    }

    if ((emptyCount > 0) && ((emptyCount != 1) || (nonEmptyCount != 0))) {
      throw XProcException.xsNoSiblingsOnEmpty(location)
    }

    if (href.isDefined) {
      if (hasDataSources) {
        throw XProcException.staticError(81, href.get, location)
      }
      hasDataSources = true

      for (uri <- href.get.split("\\s+")) {
        val ruri = baseURI.get.resolve(uri)
        val doc = new Document(config, this, ruri.toASCIIString)
        addChild(doc)
      }
    }

    if (pipe.isDefined) {
      if (hasDataSources) {
        throw XProcException.staticError(82, pipe.get, location)
      }
      parsePipeAttribute(pipe.get)
    }

    valid
  }

  override def defaultReadablePort: Option[IOPort] = {
    // From the point of view of a p:with-input, the DRP is not relative to "me",
    // it's relative to the step that contains me.
    if (parent.isDefined) {
      parent.get.defaultReadablePort
    } else {
      None
    }
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
