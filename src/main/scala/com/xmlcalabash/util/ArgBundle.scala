package com.xmlcalabash.util

import com.jafpl.messages.Message
import com.xmlcalabash.config.{XMLCalabashConfig, XMLCalabashDebugOptions}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.ValueParser
import com.xmlcalabash.runtime.{ExpressionContext, StaticContext, XProcMetadata, XProcXPathExpression}
import net.sf.saxon.lib.NamespaceConstant
import net.sf.saxon.s9api.{ItemTypeFactory, QName, XdmAtomicValue}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ArgBundle(xmlCalabash: XMLCalabashConfig) {
  private val itf = new ItemTypeFactory(xmlCalabash.processor)
  private val untypedAtomic = itf.getAtomicType(new QName(NamespaceConstant.SCHEMA, "xs:untypedAtomic"))

  private val _inputs = mutable.HashMap.empty[String, List[String]]
  private val _outputs = mutable.HashMap.empty[String, List[String]]
  private val _data = mutable.HashMap.empty[String, List[String]]
  private val _injectables = ListBuffer.empty[String]
  private val _nsbindings = mutable.HashMap.empty[String,String]
  private val _params = mutable.HashMap.empty[QName, XProcVarValue]
  private var _pipeline = Option.empty[String]
  private var _verbose = false
  private var _debugOptions = xmlCalabash.debugOptions

  def this(config: XMLCalabashConfig, args: List[String]) = {
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
      throw XProcException.xiArgBundleNoPipeline()
    }
  }

  def parse(args: List[String]): Unit = {
    // -iport=input | --input port=input
    // -oport=output | --output port=output
    // -bprefix=namespace | --bind prefix=namespace
    // -jinjectable | --inject injectable
    // --raw
    // -G | --graph output.xml
    // --graph-before output.xml
    // --norun
    // -D | --debug
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
          val scontext = new StaticContext()
          scontext.inScopeNS = _nsbindings.toMap
          val context = new ExpressionContext(scontext)
          val qname = ValueParser.parseQName(name, scontext)
          if (_params.contains(qname)) {
            throw XProcException.xiArgBundleRedefined(qname)
          }

          kind match {
            case "+" =>
              val node = xmlCalabash.parse(value, URIUtils.cwdAsURI)
              _params.put(qname, new XProcVarValue(node, context))
            case "?" =>
              val paramBind = mutable.HashMap.empty[String, Message]
              for ((qname, value) <- _params) {
                val clark = qname.getClarkName
                val msg = new XdmValueItemMessage(value.value, XProcMetadata.ANY, value.context)
                paramBind.put(clark, msg)
              }

              val expr = new XProcXPathExpression(context, value)
              val msg = xmlCalabash.expressionEvaluator.singletonValue(expr, List(), paramBind.toMap, None)
              val eval = msg.asInstanceOf[XdmValueItemMessage].item

              _params.put(qname, new XProcVarValue(eval, context))
            case null =>
              // Ordinary parameters are created as 'untypedAtomic' values so that numbers
              // can be treated as numbers, etc.
              val untypedValue = new XdmAtomicValue(value, untypedAtomic)
              _params.put(qname, new XProcVarValue(untypedValue, context))
            case _ =>
              throw XProcException.xiArgBundlePfxChar(kind)
          }
          pos += 1
        case longOptRegex(optname) =>
          try {
            optname match {
              case "verbose" => _verbose = true
              case "norun" => _debugOptions.norun = true
              case "debug" => _debugOptions.debug = true
              case "graph" => _debugOptions.dumpGraph = true
              case "graph-before" => _debugOptions.dumpOpenGraph = true
              case "dump-xml" => xmlCalabash.debugOptions.dumpXml = true
              case "inject" =>
                _injectables += args(pos+1)
                pos += 1
              case "input" =>
                val rest = args(pos + 1)
                val eqpos = rest.indexOf("=")
                if (eqpos > 0) {
                  val port = rest.substring(0, eqpos)
                  val value = rest.substring(eqpos+1)
                  parsePort(_inputs, s"$port=$value")
                } else {
                  throw XProcException.xiArgBundleCannotParseInput(s"--input $rest")
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
                  throw XProcException.xiArgBundleCannotParseInput(s"--output $rest")
                }
                pos += 1
              case "bind" =>
                val rest = args(pos + 1)
                val eqpos = rest.indexOf("=")
                if (eqpos > 0) {
                  val prefix = rest.substring(0, eqpos)
                  val uri = rest.substring(eqpos+1)
                  if (_nsbindings.contains(prefix)) {
                    throw XProcException.xiArgBundleRedefinedNamespace(prefix)
                  }
                  _nsbindings.put(prefix, uri)
                } else {
                  throw XProcException.xiArgBundleCannotParseNamespace(rest)
                }
                pos += 1
              case _ => throw XProcException.xiArgBundleUnexpectedOption(optname)
            }
          } catch {
            case iobe: IndexOutOfBoundsException =>
              throw XProcException.xiArgBundleIndexOOB(optname)
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
                  case 'D' => _debugOptions.debug = true
                  case 'G' => _debugOptions.dumpGraph = true
                  case 'i' =>
                    val rest = chars.substring(chpos + 1)
                    val eqpos = rest.indexOf("=")
                    if (eqpos > 0) {
                      val port = rest.substring(0, eqpos)
                      val value = rest.substring(eqpos+1)
                      parsePort(_inputs, s"$port=$value")
                    } else {
                      throw XProcException.xiArgBundleCannotParseInput(s"-i$rest")
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
                      throw XProcException.xiArgBundleCannotParseOutput(s"-o$rest")
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
                        throw XProcException.xiArgBundleRedefinedNamespace(prefix)
                      }
                      _nsbindings.put(prefix, uri)
                    } else {
                      throw XProcException.xiArgBundleCannotParseNamespace(rest)
                    }
                  case _ =>
                    throw XProcException.xiArgBundleUnexpectedOption(ch.toString)
                }
              }
              chpos += 1
            }
          } catch {
            case iobe: IndexOutOfBoundsException =>
              throw XProcException.xiArgBundleIndexOOB(optname)
            case t: Throwable => throw t
          }
          pos += 1
        case pipelineRegex(pfx,rest) =>
          if (_pipeline.isEmpty) {
            _pipeline = Some(pfx + rest)
          } else {
            throw XProcException.xiArgBundleMultiplePipelines(_pipeline.get, pfx+rest)
          }
          pos += 1
        case _ =>
          throw XProcException.xiArgBundleUnexpectedOption(opt)
      }
    }
  }

  def parsePort(ports: mutable.HashMap[String,List[String]], binding: String): Unit = {
    val pos = binding.indexOf("=")
    if (pos < 1) {
      throw XProcException.xiArgBundleInvalidPortSpec(binding)
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
