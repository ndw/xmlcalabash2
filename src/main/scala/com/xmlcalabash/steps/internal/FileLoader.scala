package com.xmlcalabash.steps.internal

import java.io.File
import java.net.{URI, URLConnection}
import java.nio.file.Files
import javax.xml.transform.dom.DOMSource

import com.jafpl.exceptions.PipelineException
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{DynamicContext, ExpressionContext, SaxonExpressionEvaluator, XProcExpression, XProcMetadata, XProcXPathExpression, XmlPortSpecification}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmMap, XdmValue}
import nu.validator.htmlparser.common.XmlViolationPolicy
import nu.validator.htmlparser.dom.HtmlDocumentBuilder
import org.xml.sax.InputSource

import scala.collection.mutable

class FileLoader(private val context: ExpressionContext,
                 private val docPropsExpr: Option[String]) extends DefaultStep {
  private var _href = ""
  private var _params = Option.empty[Map[QName,XdmValue]]
  private var docProps = Map.empty[QName, XdmItem]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receiveBinding(bindmsg: BindingMessage): Unit = {
    val variable = bindmsg.name

    var valueitem = Option.empty[XdmItem]
    bindmsg.message match {
      case itemmsg: ItemMessage =>
        itemmsg.item match {
          case item: XdmItem =>
            valueitem = Some(item)
          case _ => Unit
        }
      case _ => Unit
    }

    if (valueitem.isEmpty) {
      throw new PipelineException("badtype", s"binding for $variable must be an item", None)
    }

    variable match {
      case "href" =>
        _href = valueitem.get.getStringValue
      case "parameters" =>
        bindings.put(XProcConstants._parameters.getClarkName, bindmsg.message)
        _params = Some(ValueParser.parseParameters(valueitem.get, context.nsBindings, context.location))
      case _ =>
        logger.info("Ignoring unexpected option to p:document: " + variable)
    }
  }

  override def run(): Unit = {
    val href = if (context.baseURI.isDefined) {
      context.baseURI.get.resolve(_href)
    } else {
      new URI(_href)
    }

    // Using the filename sort of sucks, but it's what the OSes do at this point so...sigh
    // You can extend the set of known extensions by pointing the system property
    // `content.types.user.table` at your own mime types file. The default file to
    // start with is in $JAVA_HOME/lib/content-types.properties
    var contentType = Option(URLConnection.guessContentTypeFromName(href.toASCIIString)).getOrElse("application/xml")
    if (_params.isDefined && _params.get.contains(XProcConstants._content_type)) {
      contentType = _params.get(XProcConstants._content_type).toString
    }

    if (docPropsExpr.isDefined) {
      val expr = new XProcXPathExpression(context, docPropsExpr.get)
      val result = xpathValue(expr)
      docProps = result match {
        case map: XdmMap =>
          ValueParser.parseDocumentProperties(map, location)
        case _ =>
          throw new PipelineException("notmap", "The document-properties attribute must be a map", None)
      }
    }

    val props = mutable.HashMap.empty[QName, XdmItem]
    props ++= docProps

    if (props.contains(XProcConstants._content_type)) {
      contentType = props.get(XProcConstants._content_type).get.getStringValue
    }

    if (ValueParser.xmlContentType(contentType)) {
      val node = config.get.documentManager.parse(href)
      props.put(XProcConstants._base_uri, new XdmAtomicValue(node.getBaseURI))
      logger.debug(s"Loaded $href as $contentType")
      consumer.get.receive("result", new ItemMessage(node, new XProcMetadata(contentType, props.toMap)))
    } else if (ValueParser.jsonContentType(contentType)) {
      val expr = new XProcXPathExpression(context, s"json-doc('$href', $$parameters)")
      val json = config.get.expressionEvaluator.singletonValue(expr, List(), bindings.toMap, None)
      consumer.get.receive("result", new ItemMessage(json.item, new XProcMetadata(contentType, props.toMap)))
    } else if (ValueParser.htmlContentType(contentType)) {
      val node = config.get.documentManager.parseHtml(href)
      logger.debug(s"Loaded $href as $contentType")
      consumer.get.receive("result", new ItemMessage(node, new XProcMetadata(contentType, props.toMap)))
    } else {
      val file = new File(href)
      props.put(XProcConstants._content_length, new XdmAtomicValue(file.length()))
      props.put(XProcConstants._base_uri, new XdmAtomicValue(file.toURI))
      val bytes = Files.readAllBytes(new File(href).toPath)
      logger.debug(s"Loaded $href as $contentType")
      consumer.get.receive("result", new ItemMessage(bytes, new XProcMetadata(contentType, props.toMap)))
    }
  }

  def xpathValue(expr: XProcExpression): XdmValue = {
    val eval = config.get.expressionEvaluator
    val dynContext = new DynamicContext()
    val msg = eval.withContext(dynContext) { eval.singletonValue(expr, List.empty[Message], bindings.toMap, None) }
    msg.asInstanceOf[XPathItemMessage].item
  }
}
