package com.xmlcalabash.steps.internal

import java.net.{URI, URLConnection}

import com.jafpl.messages.{BindingMessage, JoinGateMessage, Message}
import com.jafpl.steps.PortCardinality
import com.xmlcalabash.config.{DocumentRequest, XProcTypes}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{AnyItemMessage, XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{BinaryNode, DynamicContext, ExpressionContext, XProcExpression, XProcMetadata, XProcXPathExpression, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.s9api.{QName, XdmItem, XdmMap, XdmNode, XdmValue}

class FileLoader(private val context: ExpressionContext,
                 private val declContentType: Option[MediaType],
                 private val docPropsExpr: Option[String]) extends DefaultStep {
  private var _href = ""
  private var _params = Option.empty[XProcTypes.Parameters]
  private var docProps = Map.empty[QName, XdmItem]
  private var latch = false

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("latch"->PortCardinality.ZERO_OR_MORE),
    Map("latch"->List("application/octet-stream")))
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def receive(port: String, message: Message): Unit = {
    message match {
      case msg: JoinGateMessage => Unit
      case _ => latch = true
    }
  }

  override def receiveBinding(bindmsg: BindingMessage): Unit = {
    val variable = bindmsg.name

    var valueitem = Option.empty[XdmItem]
    bindmsg.message match {
      case msg: XdmValueItemMessage =>
        msg.item match {
          case item: XdmItem =>
            valueitem = Some(item)
          case _ => Unit
        }
      case _ => Unit
    }

    if (valueitem.isEmpty) {
      throw XProcException.xiBadValueOnFileLoader(variable.toString)
    }

    variable match {
      case "href" =>
        _href = valueitem.get.getStringValue
      case "parameters" =>
        bindings.put(XProcConstants._parameters.getClarkName, bindmsg.message)
        _params = Some(ValueParser.parseParameters(valueitem.get, context.staticContext))
      case _ =>
        logger.info("Ignoring unexpected option to p:document: " + variable)
    }
  }

  override def run(): Unit = {
    if (latch) {
      return
    }

    val href = if (context.baseURI.isDefined) {
      context.baseURI.get.resolve(_href)
    } else {
      new URI(_href)
    }

    var params = Map.empty[QName, XdmValue]
    if (_params.isDefined) {
      for ((key,value) <- _params.get) {
        params += (key -> value)
      }
    }

    if (docPropsExpr.isDefined) {
      val expr = new XProcXPathExpression(context, docPropsExpr.get)
      val result = xpathValue(expr)
      docProps = result match {
        case map: XdmMap =>
          ValueParser.parseDocumentProperties(map, location)
        case _ =>
          throw XProcException.xsBadTypeValue("document-properties", "map", location)
      }
    }

    val props = Map.empty[QName, XdmValue] ++ docProps

    // Using the filename sort of sucks, but it's what the OSes do at this point so...sigh
    // You can extend the set of known extensions by pointing the system property
    // `content.types.user.table` at your own mime types file. The default file to
    // start with is in $JAVA_HOME/lib/content-types.properties
    val map = URLConnection.getFileNameMap
    var contentTypeString = Option(URLConnection.guessContentTypeFromName(href.toASCIIString)).getOrElse("application/xml")

    var propContentType = if (docProps.contains(XProcConstants._content_type)) {
      Some(MediaType.parse(docProps.get(XProcConstants._content_type).toString))
    } else {
      None
    }

    val contentType = if (propContentType.isDefined) {
      if (declContentType.isDefined) {
        if (!declContentType.get.matches(propContentType.get)) {
          throw XProcException.xdMismatchedContentType(declContentType.get, propContentType.get, location)
        }
      }
      propContentType.get
    } else {
      if (declContentType.isDefined) {
        declContentType.get
      } else {
        MediaType.parse(contentTypeString)
      }
    }

    val dtdValidate = if (params.contains(XProcConstants._dtd_validate)) {
      if (params(XProcConstants._dtd_validate).size > 1) {
        throw new IllegalArgumentException("dtd validate parameter is not a singleton")
      } else {
        params(XProcConstants._dtd_validate).itemAt(0).getStringValue == "true"
      }
    } else {
      false
    }

    val request = new DocumentRequest(href, Some(contentType), location, dtdValidate)
    request.params = params
    request.docprops = props

    val result = config.get.documentManager.parse(request)
    val metadata = new XProcMetadata(result.contentType, props ++ result.props)

    if (result.shadow.isDefined) {
      val binary = new BinaryNode(config.get, result.shadow.get)
      consumer.get.receive("result", new AnyItemMessage(S9Api.emptyDocument(config.get), binary, metadata))
    } else {
      result.value match {
        case node: XdmNode =>
          consumer.get.receive("result", new XdmNodeItemMessage(node, metadata))
        case _ =>
          consumer.get.receive("result", new XdmValueItemMessage(result.value, metadata))
      }
    }
  }

  def xpathValue(expr: XProcExpression): XdmValue = {
    val eval = config.get.expressionEvaluator
    val dynContext = new DynamicContext()
    val msg = eval.withContext(dynContext) { eval.singletonValue(expr, List.empty[Message], bindings.toMap) }
    msg.asInstanceOf[XdmValueItemMessage].item
  }
}
