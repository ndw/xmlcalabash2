package com.xmlcalabash.util

import com.jafpl.messages.Message
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.ValueParser
import com.xmlcalabash.runtime.{ExpressionContext, XProcMetadata, XProcXPathExpression}
import net.sf.saxon.lib.NamespaceConstant
import net.sf.saxon.s9api.{ItemTypeFactory, QName, XdmAtomicValue}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ArgBundle(xmlCalabash: XMLCalabash) {
  private val itf = new ItemTypeFactory(xmlCalabash.processor)
  private val untypedAtomic = itf.getAtomicType(new QName(NamespaceConstant.SCHEMA, "xs:untypedAtomic"))

  private val _inputs = mutable.HashMap.empty[String, List[String]]
  private val _outputs = mutable.HashMap.empty[String, List[String]]
  private val _data = mutable.HashMap.empty[String, List[String]]
  private val _injectables = ListBuffer.empty[String]
  private val _nsbindings = mutable.HashMap.empty[String,String]
  private val _params = mutable.HashMap.empty[QName, XProcVarValue]
  private var _pipeline = Option.empty[String]
  private var _graph = Option.empty[String]
  private var _graphBefore = Option.empty[String]
  private var _dumpXml = Option.empty[String]
  private var _raw = false
  private var _verbose = false
  private var _norun = false

  def this(config: XMLCalabash, args: List[String]) = {
    this(config)
    parse(args)
  }

  def verbose: Boolean = _verbose
  def inputs: Map[String, List[String]] = _inputs.toMap
  def outputs: Map[String, List[String]] = _outputs.toMap
  def injectables: List[String] = _injectables.toList
  def inScopeNamespaces: Map[String,String] = _nsbindings.toMap
  def data: Map[String, List[String]] = _data.toMap
  def params: Map[QName, XProcVarValue] = _params.toMap
  def pipeline: String = {
    if (_pipeline.isDefined) {
      _pipeline.get
    } else {
      throw new RuntimeException("No pipeline specified")
    }
  }

  def graph: Option[String] = _graph
  def graphBefore: Option[String] = _graphBefore
  def raw: Boolean = _raw
  def dumpXML: Option[String] = _dumpXml
  def norun: Boolean = _norun

  def parse(args: List[String]): Unit = {
    // -iport=input | --input port=input
    // -oport=output | --output port=output
    // -bprefix=namespace | --bind prefix=namespace
    // -jinjectable | --inject injectable
    // --raw
    // -G|--graph output.xml
    // --graph-before output.xml
    // --norun
    // param=string value
    // +param=file value
    // ?param=xpath expression value

    val longPortRegex   = "--((input)|(output))".r
    val paramRegex      = "([\\+\\?])?([^-]\\S+)=(\\S+)".r
    val pipelineRegex   = "([^-])(.*)".r
    val shortOptRegex   = "-(.+)".r
    val longOptRegex    = "--(.+)".r
    var pos = 0
    while (pos < args.length) {
      val opt = args(pos)
      opt match {
        case longPortRegex(kind) =>
          kind match {
            case "input" => parsePort(_inputs, args(pos+1))
            case "output" => parsePort(_outputs, args(pos+1))
          }
          pos += 2
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
              val msg = xmlCalabash.expressionEvaluator.singletonValue(expr, List(), paramBind.toMap, None)
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
        case longOptRegex(optname) =>
          try {
            optname match {
              case "verbose" => _verbose = true
              case "raw" => _raw = true
              case "norun" => _norun = true
              case "graph" =>
                _graph = Some(args(pos + 1))
                pos += 1
              case "graph-before" =>
                _graphBefore = Some(args(pos + 1))
                pos += 1
              case "dump-xml" =>
                _dumpXml = Some(args(pos + 1))
                pos += 1
              case "inject" =>
                _injectables += args(pos+1)
                pos += 1
              /*
              case "input" =>
                val rest = args(pos + 1)
                val eqpos = rest.indexOf("=")
                if (eqpos > 0) {
                  val port = rest.substring(0, eqpos)
                  val value = rest.substring(eqpos+1)
                  parsePort(_inputs, s"$port=$value")
                } else {
                  throw new RuntimeException(s"Cannot parse option --input $rest")
                }
                pos += 1
              case "output" =>
                val rest = args(pos + 1)
                val eqpos = rest.indexOf("=")
                if (eqpos > 0) {
                  val port = rest.substring(0, eqpos)
                  val value = rest.substring(eqpos+1)
                  parsePort(_outputs, s"$port=$value")
                } else {
                  throw new RuntimeException(s"Cannot parse option --output $rest")
                }
                pos += 1
              */
              case "bind" =>
                val rest = args(pos + 1)
                val eqpos = rest.indexOf("=")
                if (eqpos > 0) {
                  val prefix = rest.substring(0, eqpos)
                  val uri = rest.substring(eqpos+1)
                  if (_nsbindings.contains(prefix)) {
                    throw new RuntimeException(s"Attempt to redefine namespace binding for $prefix")
                  }
                  _nsbindings.put(prefix, uri)
                } else {
                  throw new RuntimeException(s"Cannot parse option --input $rest")
                }
                pos += 1
              case _ => throw new RuntimeException(s"Unexpected option --$optname")
            }
          } catch {
            case iobe: IndexOutOfBoundsException =>
              throw new RuntimeException(s"--$optname must be followed by a filename")
            case t: Throwable => throw t
          }
          pos += 1
        case shortOptRegex(chars) =>
          var optname = ""
          try {
            var skip = false
            var chpos = 0
            for (ch <- chars) {
              optname = ch.toString
              if (!skip) {
                ch match {
                  case 'v' => _verbose = true
                  case 'i' =>
                    val rest = chars.substring(chpos + 1)
                    val eqpos = rest.indexOf("=")
                    if (eqpos > 0) {
                      val port = rest.substring(0, eqpos)
                      val value = rest.substring(eqpos+1)
                      parsePort(_inputs, s"$port=$value")
                    } else {
                      throw new RuntimeException(s"Cannot parse option -i$rest")
                    }
                    skip = true

                  case 'o' =>
                    val rest = chars.substring(chpos + 1)
                    val eqpos = rest.indexOf("=")
                    if (eqpos > 0) {
                      val port = rest.substring(0, eqpos)
                      val value = rest.substring(eqpos+1)
                      parsePort(_outputs, s"$port=$value")
                    } else {
                      throw new RuntimeException(s"Cannot parse option -o$rest")
                    }
                    skip = true

                  case 'j' =>
                    val rest = chars.substring(chpos + 1)
                    _injectables += rest
                    skip = true

                  case 'b' =>
                    var rest = ""
                    if (chpos + 1 == chars.length) {
                      rest = args(pos + 1)
                      pos += 1
                    } else {
                      rest = chars.substring(chpos + 1)
                      skip = true
                    }
                    val eqpos = rest.indexOf("=")
                    if (eqpos > 0) {
                      val prefix = rest.substring(0, eqpos)
                      val uri = rest.substring(eqpos+1)
                      if (_nsbindings.contains(prefix)) {
                        throw new RuntimeException(s"Attempt to redefine namespace binding for $prefix")
                      }
                      _nsbindings.put(prefix, uri)
                    } else {
                      throw new RuntimeException(s"Cannot parse option -o$rest")
                    }
                  case 'G' =>
                    if (chpos + 1 == chars.length) {
                      _graph = Some(args(pos + 1))
                      pos += 1
                    } else {
                      _graph = Some(chars.substring(chpos + 1))
                      skip = true
                    }
                  case _ =>
                    throw new RuntimeException(s"Unrecognized option: $ch")
                }
              }
              chpos += 1
            }
          } catch {
            case iobe: IndexOutOfBoundsException =>
              throw new RuntimeException(s"--$optname must be followed by a filename")
            case t: Throwable => throw t
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
