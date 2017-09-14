package com.xmlcalabash.util

import com.jafpl.messages.Message
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.ValueParser
import com.xmlcalabash.runtime.{ExpressionContext, XProcMetadata, XProcXPathExpression}
import net.sf.saxon.lib.NamespaceConstant
import net.sf.saxon.s9api.{ItemTypeFactory, QName, XdmAtomicValue}

import scala.collection.mutable

class ArgBundle(xmlCalabash: XMLCalabash) {
  private val itf = new ItemTypeFactory(xmlCalabash.processor)
  private val untypedAtomic = itf.getAtomicType(new QName(NamespaceConstant.SCHEMA, "xs:untypedAtomic"))

  private val _inputs = mutable.HashMap.empty[String, List[String]]
  private val _outputs = mutable.HashMap.empty[String, List[String]]
  private val _data = mutable.HashMap.empty[String, List[String]]
  private val _nsbindings = mutable.HashMap.empty[String,String]
  private val _params = mutable.HashMap.empty[QName, XProcVarValue]
  private var _pipeline = Option.empty[String]
  private var _verbose = false

  def this(config: XMLCalabash, args: List[String]) = {
    this(config)
    parse(args)
  }

  def verbose: Boolean = _verbose
  def inputs: Map[String, List[String]] = _inputs.toMap
  def outputs: Map[String, List[String]] = _outputs.toMap
  def data: Map[String, List[String]] = _data.toMap
  def params: Map[QName, XProcVarValue] = _params.toMap
  def pipeline: String = {
    if (_pipeline.isDefined) {
      _pipeline.get
    } else {
      throw new RuntimeException("No pipeline specified")
    }
  }

  def parse(args: List[String]): Unit = {
    // -iport=input | --input port=input
    // -d[content-type@]port=data | --data [content-type@]port=data
    // -oport=output | --output port=output
    // -bprefix=namespace
    // param=string value
    // +param=file value
    // ?param=xpath expression value

    val longPortRegex   = "-((input)|(output)|(data))".r
    val shortPortRegex  = "-([iod])(\\S+)=(.*)".r
    val nsbindingRegex  = "-(b)(\\S+)=(.*)".r
    val paramRegex      = "([\\+\\?])?(\\S+)=(\\S+)".r
    val pipelineRegex   = "([^-])(.*)".r
    val otherOptRegex   = "-(.+)".r
    var pos = 0
    while (pos < args.length) {
      val opt = args(pos)
      opt match {
        case longPortRegex(kind) =>
          kind match {
            case "input" => parsePort(_inputs, args(pos+1))
            case "output" => parsePort(_inputs, args(pos+1))
            case "data" => parsePort(_data, args(pos+1))
          }
          pos += 2
        case shortPortRegex(kind, port, value) =>
          kind match {
            case "i" => parsePort(_inputs, s"$port=$value")
            case "o" => parsePort(_inputs, s"$port=$value")
            case "d" => parsePort(_data, s"$port=$value")
          }
          pos += 1
        case nsbindingRegex(prefix, uri) =>
          println(s"ns $prefix=$uri")
          if (_nsbindings.contains(prefix)) {
            throw new RuntimeException(s"Attempt to redefine namespace binding for $prefix")
          }
          _nsbindings.put(prefix, uri)
          pos += 1
        case paramRegex(kind, name, value) =>
          val context = new ExpressionContext(None, _nsbindings.toMap, None)
          val qname = ValueParser.parseQName(name, _nsbindings.toMap)
          if (_params.contains(qname)) {
            throw new RuntimeException("Attempt to redefine parameter: " + qname)
          }

          kind match {
            case "+" =>
              val node = xmlCalabash.parse(value, URIUtils.cwdAsURI)
              _params.put(qname, new XProcVarValue(node, context))
            case "?" =>
              val paramBind = mutable.HashMap.empty[String, Message]
              for ((qname, value) <- _params) {
                val clark = qname.getClarkName
                val msg = new XPathItemMessage(value.value, XProcMetadata.ANY, value.context)
                paramBind.put(clark, msg)
              }

              val expr = new XProcXPathExpression(context, value)
              val msg = xmlCalabash.expressionEvaluator.value(expr, List(), paramBind.toMap)
              val eval = msg.asInstanceOf[XPathItemMessage].item

              _params.put(qname, new XProcVarValue(eval, context))
            case null =>
              // Ordinary parameters are created as 'untypedAtomic' values so that numbers
              // can be treated as numbers, etc.
              val untypedValue = new XdmAtomicValue(value, untypedAtomic)
              _params.put(qname, new XProcVarValue(untypedValue, context))
            case _ =>
              throw new RuntimeException("Unexpected prefix character in parameter: " + kind)
          }
          pos += 1
        case otherOptRegex(chars) =>
          for (ch <- chars) {
            ch match {
              case 'v' => _verbose = true
              case _ =>
                throw new RuntimeException(s"Unrecognized option: $ch")
            }
          }
          pos += 1
        case pipelineRegex(pfx,rest) =>
          if (_pipeline.isEmpty) {
            _pipeline = Some(pfx + rest)
          } else {
            throw new RuntimeException(s"More than one pipeline specified: ${_pipeline.get}, ${pfx+rest}, ...")
          }
          pos += 1
        case _ =>
          throw new RuntimeException("Unexpected option: " + opt)
      }
    }
  }

  def parsePort(ports: mutable.HashMap[String,List[String]], binding: String): Unit = {
    val pos = binding.indexOf("=")
    if (pos < 1) {
      throw new RuntimeException("Invalid port specification: " + binding)
    }
    val port = binding.substring(0, pos)
    val fn = binding.substring(pos+1)

    if (ports.contains(port)) {
      ports.put(port, ports(port) ++ List(fn))
    } else {
      ports.put(port, List(fn))
    }
  }

}
