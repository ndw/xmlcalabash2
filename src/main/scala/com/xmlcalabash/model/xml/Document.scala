package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.params.DocumentLoaderParams
import com.xmlcalabash.util.MediaType
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable

class Document(override val config: XMLCalabashConfig) extends DataSource(config) {
  private var _href = "UNINITIALIZED"
  private var _hrefAvt = List.empty[String]
  private var _contentType = Option.empty[MediaType]
  private var _documentProperties = Option.empty[String]
  private var _parameters = Option.empty[String]
  private var _context_provided = false

  def href: String = _href
  protected[model] def hrefAvt: List[String] = _hrefAvt
  protected[model] def href_=(href: String): Unit = {
    _href = href
    _hrefAvt = staticContext.parseAvt(_href)
  }
  def content_type: Option[MediaType] = _contentType
  def document_properties: Option[String] = _documentProperties
  def parameters: Option[String] = _parameters

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    if (!attributes.contains(XProcConstants._href)) {
      throw XProcException.xsMissingRequiredAttribute(XProcConstants._href, location)
    }

    _href = attr(XProcConstants._href).get
    _hrefAvt = staticContext.parseAvt(_href)
    _contentType = MediaType.parse(attributes.get(XProcConstants._content_type))
    _documentProperties = attr(XProcConstants._document_properties)
    _parameters = attr(XProcConstants._parameters)

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(environment: Environment): Unit = {
    // nop
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

    val bindings = mutable.HashSet.empty[QName]
    bindings ++= staticContext.findVariableRefsInAvt(_hrefAvt)
    bindings ++= staticContext.findVariableRefsInString(_documentProperties)
    bindings ++= staticContext.findVariableRefsInString(_parameters)

    for (ref <- bindings) {
      val binding = env.variable(ref)
      if (binding.isEmpty) {
        throw new RuntimeException("Reference to undefined variable")
      }
      if (!binding.get.static) {
        val pipe = new NamePipe(config, ref, binding.get.tumble_id, binding.get)
        addChild(pipe)
      }
    }
  }

  override protected[model] def normalizeToPipes(): Unit = {
    val context = staticContext.withStatics(inScopeStatics)
    val params = new DocumentLoaderParams(_hrefAvt, _contentType, _parameters, _documentProperties, _context_provided, staticContext)
    normalizeDataSourceToPipes(XProcConstants.cx_document_loader, params)
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startDocument(tumble_id, _href)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endDocument()
  }

  override def toString: String = {
    s"p:document $href"
  }
}
