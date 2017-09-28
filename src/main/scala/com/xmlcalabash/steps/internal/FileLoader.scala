package com.xmlcalabash.steps.internal

import java.io.File
import java.net.{URI, URLConnection}
import java.nio.file.Files

import com.jafpl.exceptions.PipelineException
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{DynamicContext, ExpressionContext, SaxonExpressionEvaluator, XProcExpression, XProcMetadata, XProcXPathExpression, XmlPortSpecification}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmMap}

import scala.collection.mutable

class FileLoader(private val context: ExpressionContext,
                 private val docPropsExpr: Option[String]) extends DefaultStep {
  private var _href = ""
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
    val contentType = Option(URLConnection.guessContentTypeFromName(href.toASCIIString))

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

    // I'm not sure what to do here...
    try {
      val node = config.get.documentManager.parse(href)
      val ctype = contentType.getOrElse("application/xml")
      props.put(XProcConstants._base_uri, new XdmAtomicValue(node.getBaseURI))
      logger.debug(s"Loaded $href as $ctype")
      consumer.get.receive("result", new ItemMessage(node, new XProcMetadata(ctype, props.toMap)))
    } catch {
      case t: Throwable =>
        // What should the representation of non-XML data be?
        val file = new File(href)
        props.put(new QName("", "file-size"), new XdmAtomicValue(file.length()))
        props.put(XProcConstants._base_uri, new XdmAtomicValue(file.toURI))
        val bytes = Files.readAllBytes(new File(href).toPath)
        val ctype = contentType.getOrElse("application/octet-stream")
        logger.debug(s"Loaded ${href} as $ctype")
        consumer.get.receive("result", new ItemMessage(bytes, new XProcMetadata(ctype, props.toMap)))
    }
  }

  def xpathValue(expr: XProcExpression): XdmItem = {
    val eval = config.get.expressionEvaluator.asInstanceOf[SaxonExpressionEvaluator]
    val dynContext = new DynamicContext()
    val msg = eval.withContext(dynContext) { eval.singletonValue(expr, List.empty[Message], bindings.toMap, None) }
    msg.asInstanceOf[XPathItemMessage].item
  }
}
