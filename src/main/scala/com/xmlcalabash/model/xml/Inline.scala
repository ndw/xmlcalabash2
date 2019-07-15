package com.xmlcalabash.model.xml

import com.jafpl.messages.Message
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.runtime.params.InlineLoaderParams
import com.xmlcalabash.runtime.XProcVtExpression
import com.xmlcalabash.util.xc.ElaboratedPipeline
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmNodeKind}

import scala.collection.mutable

class Inline(override val config: XMLCalabashConfig, val node: XdmNode, val implied: Boolean) extends DataSource(config) {
  def this(config: XMLCalabashConfig, node: XdmNode) {
    this(config, node, false)
  }

  private var _contentType = Option.empty[MediaType]
  private var _documentProperties = Option.empty[String]
  private var _encoding = Option.empty[String]
  private var _exclude_inline_prefixes = Option.empty[String]
  private var _context_provided = false
  private val nameBindings = mutable.HashSet.empty[QName]
  private val _statics = mutable.HashMap.empty[String, Message]

  if (node.getNodeKind != XdmNodeKind.DOCUMENT) {
    throw new RuntimeException("inline document must be a document")
  }

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    _contentType = MediaType.parse(attributes.get(XProcConstants._content_type))
    _documentProperties = attr(XProcConstants._document_properties)
    _encoding = attr(XProcConstants._encoding)
    _exclude_inline_prefixes = attr(XProcConstants._exclude_inline_prefixes)
  }

  override protected[model] def makeBindingsExplicit(env: Environment, drp: Option[Port]): Unit = {
    super.makeBindingsExplicit(env, drp)

    if (allChildren.isEmpty && drp.isDefined && !parent.get.isInstanceOf[DeclareInput]) {
      _context_provided = true
      val pipe = new Pipe(config)
      pipe.port = drp.get.port
      pipe.step = drp.get.step.stepName
      pipe.link = drp.get
      addChild(pipe)
    }

    val ctype = if (_contentType.isDefined) {
      _contentType.get
    } else {
      MediaType.XML
    }

    // FIXME: this isn't sufficient
    if (ctype.xmlContentType || ctype.htmlContentType) {
      findVariablesInTVT(node, true)
      for (ref <- nameBindings) {
        val binding = env.variable(ref)
        if (binding.isEmpty) {
          throw XProcException.xsNoBindingInExpression(ref, location)
        }
        if (binding.get.static) {
          _statics.put(ref.getClarkName, binding.get.staticValue.get)
        } else {
          val pipe = new NamePipe(config, ref, binding.get.tumble_id, binding.get)
          addChild(pipe)
        }
      }
    }
  }

  override protected[model] def makeStructureExplicit(environment: Environment): Unit = {
    // nop
  }

  override protected[model] def validateStructure(): Unit = {
    // nop
  }

  override protected[model] def normalizeToPipes(): Unit = {
    val context = staticContext.withStatics(inScopeStatics)
    val params = new InlineLoaderParams(node, _contentType, _documentProperties, _encoding, _exclude_inline_prefixes, expand_text, _context_provided, context)
    normalizeDataSourceToPipes(XProcConstants.cx_inline_loader, params)
  }

  private def findVariablesInTVT(node: XdmNode, expandText: Boolean): Unit = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        val iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next()
          findVariablesInTVT(child, expandText)
        }
      case XdmNodeKind.ELEMENT =>
        var newExpand = expandText
        var iter = node.axisIterator(Axis.ATTRIBUTE)
        while (iter.hasNext) {
          var discardAttribute = false
          val attr = iter.next()
          if (attr.getNodeName == XProcConstants.p_inline_expand_text) {
            if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
              throw XProcException.xsInlineExpandTextNotAllowed(location)
            }
            discardAttribute = true
            newExpand = attr.getStringValue == "true"
          }
          if (attr.getNodeName == XProcConstants._inline_expand_text) {
            if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
              discardAttribute = true
              newExpand = attr.getStringValue == "true"
            }
          }
          if (!discardAttribute) {
            if (expandText) {
              findVariablesInString(attr.getStringValue)
            }
          }
        }
        iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next()
          findVariablesInTVT(child, newExpand)
        }
      case XdmNodeKind.TEXT =>
        var str = node.getStringValue
        if (expandText && str.contains("{")) {
          findVariablesInNodes(str)
        }
      case _ => Unit
    }
  }

  private def findVariablesInString(text: String): Unit = {
    for (name <- ValueParser.findVariableRefsInString(config, text)) {
      nameBindings += name
    }
  }

  private def findVariablesInNodes(text: String): Unit = {
    val expr = staticContext.parseAvt(text)
    for (name <- ValueParser.findVariableRefsInAvt(config, expr)) {
      nameBindings += name
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    var root = Option.empty[QName]
    val iter = node.axisIterator(Axis.CHILD)
    while (root.isEmpty && iter.hasNext) {
      val item = iter.next()
      if (item.getNodeKind == XdmNodeKind.ELEMENT) {
        root = Some(item.getNodeName)
      }
    }

    xml.startInline(tumble_id, root)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endInline()
  }

  override def toString: String = {
    val root = S9Api.documentElement(node)
    if (root.isDefined) {
      s"p:inline <${root.get.getNodeName}> $tumble_id"
    } else {
      s"p:inline <> $tumble_id"
    }
  }
}