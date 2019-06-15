package com.xmlcalabash.runtime

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, IOException, InputStream}
import java.lang
import java.net.URI

import com.jafpl.graph.Location
import com.jafpl.messages.{BindingMessage, ExceptionMessage, Message}
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.{BindingSpecification, DataConsumer, PortCardinality, Step}
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.{StepException, XProcException}
import com.xmlcalabash.messages.{AnyItemMessage, XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.util.{MediaType, TypeUtils}
import net.sf.saxon.s9api.{Axis, QName, XdmAtomicValue, XdmMap, XdmNode, XdmNodeKind, XdmValue}
import org.apache.http.util.ByteArrayBuffer
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class StepProxy(config: XMLCalabashRuntime, stepType: QName, step: StepExecutable, params: Option[ImplParams], staticContext: StaticContext) extends Step with XProcDataConsumer {
  private val typeUtils = new TypeUtils(config)
  private var _id: String = _
  private val openStreams = ListBuffer.empty[InputStream]
  private var p_message = Option.empty[String]
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var consumer: Option[DataConsumer] = None
  protected val bindings = mutable.HashSet.empty[QName]
  protected val bindingsMap = mutable.HashMap.empty[String, Message]
  protected var dynamicContext = new DynamicContext()
  protected var received = mutable.HashMap.empty[String,Long]
  protected var defaultSelect = mutable.HashMap.empty[String, XProcExpression]

  def nodeId: String = _id
  def nodeId_=(id: String): Unit = {
    if (_id == null) {
      _id = id
    } else {
      throw XProcException.xiRedefId(id, staticContext.location)
    }
  }

  def location: Option[Location] = staticContext.location
  def location_=(location: Location): Unit = {
    throw new RuntimeException("You can't assign the location")
  }

  def setDefaultSelect(port: String, select: XProcExpression): Unit = {
    defaultSelect.put(port, select)
  }

  // =============================================================================================

  override def toString: String = {
    val node = config.node(nodeId)
    if (node.isDefined) {
      node.get.toString
    } else {
      "proxy:" + step.toString
    }
  }

  override def inputSpec: XmlPortSpecification = {
    step match {
      case xstep: XmlStep => xstep.inputSpec
      case _ =>
        val portMap = mutable.HashMap.empty[String,PortCardinality]
        val typeMap = mutable.HashMap.empty[String,List[String]]
        for (key <- step.inputSpec.ports) {
          portMap.put(key, step.inputSpec.cardinality(key).getOrElse(PortCardinality.ZERO_OR_MORE))
          typeMap.put(key, List("application/octet-stream"))
        }
        new XmlPortSpecification(portMap.toMap, typeMap.toMap)
    }
  }

  override def outputSpec: XmlPortSpecification = {
    step match {
      case xstep: XmlStep => xstep.outputSpec
      case _ =>
        val portMap = mutable.HashMap.empty[String,PortCardinality]
        val typeMap = mutable.HashMap.empty[String,List[String]]
        for (key <- step.outputSpec.ports) {
          portMap.put(key, step.outputSpec.cardinality(key).getOrElse(PortCardinality.ZERO_OR_MORE))
          typeMap.put(key, List("application/octet-stream"))
        }
        new XmlPortSpecification(portMap.toMap, typeMap.toMap)
    }
  }
  override def bindingSpec: BindingSpecification = step.bindingSpec
  override def setConsumer(consumer: DataConsumer): Unit = {
    this.consumer = Some(consumer)
    step.setConsumer(this)
  }

  override def receiveBinding(bindmsg: BindingMessage): Unit = {
    val qname = if (bindmsg.name.startsWith("{")) {
      val clarkName = "\\{(.*)\\}(.*)".r
      val qname = bindmsg.name match {
        case clarkName(uri,name) => new QName(uri,name)
        case _ => throw XProcException.xiInvalidClarkName(staticContext.location, bindmsg.name)
      }
      qname
    } else {
      new QName("", bindmsg.name)
    }

    if ((step.signature.stepType.getNamespaceURI == XProcConstants.ns_p && qname == XProcConstants._message)
      || (step.signature.stepType.getNamespaceURI != XProcConstants.ns_p && qname == XProcConstants.p_message)) {
      p_message = Some(bindmsg.message.toString)
      return
    }

    bindings += qname
    bindingsMap.put(qname.getClarkName, bindmsg.message)

    val stepsig = step.signature
    if (stepsig.options.contains(qname)) {
      val optsig  = stepsig.option(qname, staticContext.location.get)
      val opttype: Option[QName] = if (optsig.declaredType.isDefined) {
        Some(new QName(XProcConstants.ns_xs, optsig.declaredType.get))
      } else {
        None
      }
      val occurrence = optsig.occurrence

      bindmsg.message match {
        case item: XdmValueItemMessage =>
          item.item match {
            case atomic: XdmAtomicValue =>
              if (false && occurrence.isDefined) {
                val seq = typeUtils.castSequenceAs(atomic, opttype, occurrence.get, item.context)
                step.receiveBinding(qname, seq, item.context)
              } else {
                val value = typeUtils.castAtomicAs(atomic, opttype, item.context)
                step.receiveBinding(qname, value, item.context)
              }
            case _ => Unit
              step.receiveBinding(qname, item.item, item.context)
          }
        case _ =>
          throw XProcException.xiInvalidMessage(staticContext.location, bindmsg.message)
      }
    }
  }

  override def initialize(config: RuntimeConfiguration): Unit = {
    config match {
      case saxon: XMLCalabashRuntime => Unit
      case _ => throw XProcException.xiNotXMLCalabash()
    }

    config match {
      case xcfg: XMLCalabashRuntime =>
        if (this.config.config != xcfg.config) {
          throw XProcException.xiDifferentXMLCalabash()
        }
      case _ => Unit
    }

    step.initialize(config, params)
  }

  override def run(): Unit = {
    if (p_message.isDefined) {
      println(p_message.get)
    }

    for (port <- inputSpec.ports) {
      if (!received.contains(port)) {
        inputSpec.checkInputCardinality(port, 0)
      }
    }

    for (port <- defaultSelect.keySet) {
      if (!received.contains(port)) {
        // If the input has a select, this is the context for that expression
        evalSelect(port, defaultSelect(port), None)
      }
    }

    for (qname <- step.signature.options) {
      if (!bindings.contains(qname)) {
        val optsig  = step.signature.option(qname, staticContext.location.get)
        val opttype: Option[QName] = if (optsig.declaredType.isDefined) {
          Some(new QName(XProcConstants.ns_xs, optsig.declaredType.get))
        } else {
          None
        }
        if (optsig.defaultValue.isDefined) {
          val value = typeUtils.castAtomicAs(new XdmAtomicValue(optsig.defaultValue.get), opttype, ExpressionContext.NONE)
          step.receiveBinding(qname, value, ExpressionContext.NONE)
        }
      }
    }

    try {
      DynamicContext.withContext(dynamicContext) { step.run(staticContext) }
    } finally {
      var thrown = Option.empty[Exception]
      for (stream <- openStreams) {
        try {
          stream.close()
        } catch {
          case ex: IOException => Unit
          case ex: Exception =>
            thrown = Some(ex)
        }
      }
      if (thrown.isDefined) {
        throw thrown.get
      }
    }
  }

  override def reset(): Unit = {
    step.reset()
    bindings.clear()
    bindingsMap.clear()
    received.clear()
  }

  override def abort(): Unit = {
    step.abort()
  }

  override def stop(): Unit = {
    step.stop()
  }

  override def receive(port: String, message: Message): Unit = {
    received.put(port, received.getOrElse(port, 1))

    inputSpec.checkInputCardinality(port, received(port))

    // Get exceptions out of the way
    message match {
      case msg: ExceptionMessage =>
        msg.item match {
          case ex: StepException =>
            if (ex.errors.isDefined) {
              step.receive(port, ex.errors.get, XProcMetadata.XML)
            } else {
              step.receive(port, msg.item, XProcMetadata.EXCEPTION)
            }
          case _ =>
            step.receive(port, msg.item, XProcMetadata.EXCEPTION)
        }
        return
      case _ => Unit
    }

    if (defaultSelect.contains(port)) {
      evalSelect(port, defaultSelect(port), Some(message))
    } else {
      message match {
        case msg: XdmValueItemMessage =>
          step.receive(port, msg.item, msg.metadata)
        case msg: AnyItemMessage =>
          step.receive(port, msg.shadow, msg.metadata)
        case _ =>
          throw XProcException.xiInvalidMessage(staticContext.location, message)
      }
    }
  }

  private def evalSelect(port: String, selectExpr: XProcExpression, message: Option[Message]): Unit = {
    val expr = config.expressionEvaluator.newInstance()
    val selected = if (message.isDefined) {
      expr.value(selectExpr, List(message.get), bindingsMap.toMap, None)
    } else {
      expr.value(selectExpr, List(), bindingsMap.toMap, None)
    }
    val iter = selected.item.iterator()
    while (iter.hasNext) {
      val item = iter.next()
      item match {
        case node: XdmNode =>
          if (node.getNodeKind == XdmNodeKind.ATTRIBUTE) {
            throw XProcException.xdInvalidSelection(selectExpr.toString, "an attribute", staticContext.location)
          }
          if (message.isDefined) {
            dynamicContext.addDocument(node, message.get)
          } else {
            // There's no message, so???
            // This only happens in the case of applying the default input to p:input
          }

        case _ => Unit
      }

      item match {
        case node: XdmNode =>
          val tree = new SaxonTreeBuilder(config)
          tree.startDocument(node.getBaseURI)
          tree.addSubtree(node)
          tree.endDocument()
          step.receive(port, tree.result, selected.metadata)
        case _ =>
          step.receive(port, item, selected.metadata)
      }
    }
  }

  // =======================================================================================

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    // Let's try to validate and normalize what just got sent out of the step.
    // If it claims to be XML, HTML, JSON, or text, we need to get it into an XDM.

    val contentType = metadata.contentType
    val sendMessage = contentType.classification match {
      case MediaType.XML => makeXmlMessage(item, metadata)
      case MediaType.HTML => makeHtmlMessage(item, metadata)
      case MediaType.JSON => makeJsonMessage(item, metadata)
      case MediaType.TEXT => makeTextMessage(item, metadata)
      case _ => makeBinaryMessage(item,metadata)
    }

    consumer.get.receive(port, sendMessage)
  }

  private def makeXmlMessage(item: Any, metadata: XProcMetadata): Message = {
    item match {
      case value: XdmNode =>
        assertXmlDocument(value)
        new XdmNodeItemMessage(value, metadata)
      case value: XdmMap =>
        new XdmValueItemMessage(value, metadata)
      case value: XdmAtomicValue =>
        val tree = new SaxonTreeBuilder(config)
        tree.startDocument(None)
        tree.addText(value.getStringValue)
        tree.endDocument()
        new XdmNodeItemMessage(tree.result, new XProcMetadata(MediaType.TEXT, metadata))
      case value: String =>
        val req = new DocumentRequest(metadata.baseURI.getOrElse(new URI("")), metadata.contentType)
        val result = config.documentManager.parse(req, new ByteArrayInputStream(value.getBytes("UTF-8")))
        makeXmlMessage(result.value, metadata)
      case bytes: Array[Byte] =>
        makeXmlMessage(new ByteArrayInputStream(bytes), metadata)
      case value: InputStream =>
        val req = new DocumentRequest(metadata.baseURI.getOrElse(new URI("")), metadata.contentType)
        val result = config.documentManager.parse(req, value)
        makeXmlMessage(result.value, metadata)
      case value: XdmValue =>
        throw XProcException.xiNotAnXmlDocument(None)
      case _ =>
        throw new RuntimeException(s"Cannot interpret $item as ${metadata.contentType}")
    }
  }

  private def makeHtmlMessage(item: Any, metadata: XProcMetadata): Message = {
    item match {
      case value: XdmNode =>
        assertXmlDocument(value)
        new XdmNodeItemMessage(value, metadata)
      case value: String =>
        val req = new DocumentRequest(metadata.baseURI.getOrElse(new URI("")), metadata.contentType)
        val result = config.documentManager.parse(req, new ByteArrayInputStream(value.getBytes("UTF-8")))
        makeHtmlMessage(result.value, metadata)
      case value: Array[Byte] =>
        makeHtmlMessage(new ByteArrayInputStream(value), metadata)
      case value: InputStream =>
        val req = new DocumentRequest(metadata.baseURI.getOrElse(new URI("")), metadata.contentType)
        val result = config.documentManager.parse(req, value)
        makeHtmlMessage(result.value, metadata)
      case value: XdmValue =>
        throw XProcException.xiNotAnXmlDocument(None)
      case _ =>
        throw new RuntimeException(s"Cannot interpret $item as ${metadata.contentType}")
    }
  }

  private def makeJsonMessage(item: Any, metadata: XProcMetadata): Message = {
    item match {
      case value: XdmNode =>
        throw XProcException.xiNotJSON(None)
      case value: XdmValue =>
        new XdmValueItemMessage(value, metadata)
      case value: String =>
        val req = new DocumentRequest(metadata.baseURI.getOrElse(new URI("")), metadata.contentType)
        val result = config.documentManager.parse(req, new ByteArrayInputStream(value.getBytes("UTF-8")))
        makeJsonMessage(result.value, metadata)
      case value: Array[Byte] =>
        makeJsonMessage(new ByteArrayInputStream(value), metadata)
      case value: InputStream =>
        val req = new DocumentRequest(metadata.baseURI.getOrElse(new URI("")), metadata.contentType)
        val result = config.documentManager.parse(req, value)
        makeJsonMessage(result.value, metadata)
      case _ =>
        throw new RuntimeException(s"Cannot interpret $item as ${metadata.contentType}")
    }
  }
  private def makeTextMessage(item: Any, metadata: XProcMetadata): Message = {
    item match {
      case value: XdmNode =>
        assertTextDocument(value)
        new XdmNodeItemMessage(value, metadata)
      case value: XdmValue =>
        value match {
          case atomic: XdmAtomicValue =>
            val t = atomic.getPrimitiveTypeName
            if (t == XProcConstants.xs_string || t == XProcConstants.xs_NCName || t == XProcConstants.xs_untypedAtomic
              || t == XProcConstants.xs_anyURI || t == XProcConstants.xs_NMTOKEN) {
              makeTextMessage(atomic.getStringValue, metadata)
            } else {
              throw XProcException.xiNotATextDocument(None)
            }
          case _ =>
            throw XProcException.xiNotATextDocument(None)
        }
      case value: String =>
        val tree = new SaxonTreeBuilder(config)
        tree.startDocument(metadata.baseURI)
        tree.addText(value)
        tree.endDocument()
        new XdmNodeItemMessage(tree.result, metadata)
      case value: Array[Byte] =>
        makeTextMessage(new ByteArrayInputStream(value), metadata)
      case stream: InputStream =>
        val bos = new ByteArrayOutputStream()
        var totBytes = 0L
        val pagesize = 4096
        val buffer = new ByteArrayBuffer(pagesize)
        val tmp = new Array[Byte](4096)
        var length = 0
        length = stream.read(tmp)
        while (length >= 0) {
          bos.write(tmp, 0, length)
          totBytes += length
          length = stream.read(tmp)
        }
        bos.close()
        stream.close()
        makeTextMessage(bos.toString("UTF-8"), metadata)
      case _ =>
        throw new RuntimeException(s"Cannot interpret $item as ${metadata.contentType}")
    }
  }

  private def makeBinaryMessage(item: Any, metadata: XProcMetadata): Message = {
    val tree = new SaxonTreeBuilder(config)
    tree.startDocument(metadata.baseURI)
    tree.endDocument()

    item match {
      case value: BinaryNode =>
        new AnyItemMessage(tree.result, value, metadata)
      case value: String =>
        val binary = new BinaryNode(config, value.getBytes("UTF-8"))
        new AnyItemMessage(tree.result, binary, metadata)
      case value: Array[Byte] =>
        makeBinaryMessage(new ByteArrayInputStream(value), metadata)
      case value: InputStream =>
        val binary = new BinaryNode(config, value)
        new AnyItemMessage(tree.result, binary, metadata)
      case _ =>
        throw XProcException.xiNotBinary(None)
    }
  }

  private def assertDocument(node: XdmNode): Unit = {
    if (node.getNodeKind != XdmNodeKind.DOCUMENT) {
      throw XProcException.xiNotADocument(None)
    }
  }

  private def assertTextDocument(node: XdmNode): Unit = {
    assertDocument(node)
    var count = 0
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next().asInstanceOf[XdmNode]
      if (count > 0 || child.getNodeKind != XdmNodeKind.TEXT) {
        throw XProcException.xiNotATextDocument(None)
      }
      count += 1
    }
  }

  private def assertXmlDocument(node: XdmNode): Unit = {
    assertDocument(node)

    // N.B. We don't assert that documents actually be well-formed XML.
    // This is on purpose; steps can produce any XdmNode tree.
    /*
    var count = 0
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next().asInstanceOf[XdmNode]
      child.getNodeKind match {
        case XdmNodeKind.ELEMENT => count += 1
        case XdmNodeKind.PROCESSING_INSTRUCTION => Unit
        case XdmNodeKind.COMMENT => Unit
        case XdmNodeKind.TEXT =>
          if (child.getStringValue.trim != "") {
            throw XProcException.xiNotAnXmlDocument(None)
          }
        case _ =>
          throw XProcException.xiNotAnXmlDocument(None)
      }
    }
    if (count != 1) {
      throw XProcException.xiNotAnXmlDocument(None)
    }
    */
  }
}
