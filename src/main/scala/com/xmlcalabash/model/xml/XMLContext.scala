package com.xmlcalabash.model.xml

import java.net.URI

import com.jafpl.graph.Location
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.ValueParser
import com.xmlcalabash.runtime.StaticContext
import com.xmlcalabash.util.{MediaType, TypeUtils}
import net.sf.saxon.expr.parser.{ExpressionTool, XPathParser}
import net.sf.saxon.s9api.{ItemType, OccurrenceIndicator, QName, SaxonApiException, SequenceType, XdmAtomicValue}
import net.sf.saxon.sxpath.IndependentContext
import net.sf.saxon.trans.XPathException

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XMLContext(override val config: XMLCalabashConfig) extends StaticContext(config) {
  def this(config: XMLCalabashConfig, baseURI: Option[URI], ns: Map[String,String], location: Option[Location]) {
    this(config)
    _baseURI = baseURI
    _inScopeNS = ns
    _location = location
  }

  def parseBoolean(value: Option[String]): Option[Boolean] = {
    if (value.isDefined) {
      if (value.get == "true" || value.get == "false") {
        Some(value.get == "true")
      } else {
        throw XProcException.xsBadTypeValue(value.get, "boolean", location)
      }
    } else {
      None
    }
  }

  def parseQName(name: String): QName = {
    parseQName(Some(name)).get
  }

  def parseQName(name: Option[String]): Option[QName] = {
    if (name.isDefined) {
      val eqname = "^Q\\{(.*)\\}(\\S+)$".r
      name.get match {
        case eqname(uri,local) => Some(new QName(uri, local))
        case _ =>
          if (name.get.contains(":")) {
            val pos = name.get.indexOf(':')
            val prefix = name.get.substring(0, pos)
            val local = name.get.substring(pos+1)
            if (nsBindings.contains(prefix)) {
              Some(new QName(prefix, nsBindings(prefix), local))
            } else {
              throw XProcException.xdCannotResolveQName(name.get, location)
            }
          } else {
            Some(new QName("", name.get))
          }
      }
    } else {
      None
    }
  }

  def parseNCName(name: Option[String]): Option[String] = {
    if (name.isDefined) {
      try {
        val typeUtils = new TypeUtils(config)
        val ncname = typeUtils.castAtomicAs(XdmAtomicValue.makeAtomicValue(name.get), ItemType.NCNAME, null)
        Some(ncname.getStringValue)
      } catch {
        case sae: SaxonApiException =>
          throw XProcException.xsBadTypeValue(name.get, "NCName", location)
        case e: Exception =>
          throw e
      }
    } else {
      None
    }
  }

  def parseContentTypes(ctypes: Option[String]): List[MediaType] = {
    if (ctypes.isDefined) {
      try {
        MediaType.parseList(ctypes.get).toList
      } catch {
        case ex: XProcException =>
          if (ex.code == XProcException.xc0070) {
            // Map to the static error...
            throw XProcException.xsUnrecognizedContentType(ex.details.head.toString, ex.location)
          } else {
            throw ex
          }
      }
    } else {
      List.empty[MediaType]
    }
  }

  def parseSequenceType(seqType: Option[String]): Option[SequenceType] = {
    if (seqType.isDefined) {
      try {
        val parser = new XPathParser
        parser.setLanguage(XPathParser.SEQUENCE_TYPE, 31)
        val ic = new IndependentContext(config.processor.getUnderlyingConfiguration)
        for ((prefix, uri) <- nsBindings) {
          ic.declareNamespace(prefix, uri)
        }

        // Fracking heck, this returns an type.SequenceType. I can't work out
        // how to convert that into an s9api.SequenceType. Hacksville
        var s9apiSequenceType: SequenceType = null
        val stypere = "^([^*+?()]+)([*+?])?$".r
        seqType.get match {
          case stypere(typename, card) =>
            val itemtype = typename match {
              case "xs:string" => ItemType.STRING
              case "xs:integer" => ItemType.INTEGER
              case "xs:NCName" => ItemType.NCNAME
              case "xs:token" => ItemType.TOKEN
              case "xs:QName" => ItemType.QNAME
              case "xs:anyURI" => ItemType.ANY_URI
              case "xs:boolean" => ItemType.BOOLEAN
              case "xs:language" => ItemType.LANGUAGE
              case "xs:float" => ItemType.FLOAT
              case "xs:decimal" => ItemType.DECIMAL
              case _ =>
                throw XProcException.xsInvalidSequenceType(seqType.get, "Probably bad sequence type parsing", location)
            }
            val cardinality = if (card == null) {
              OccurrenceIndicator.ONE
            } else {
              card match {
                case "*" => OccurrenceIndicator.ZERO_OR_MORE
                case "?" => OccurrenceIndicator.ZERO_OR_ONE
                case "+" => OccurrenceIndicator.ONE_OR_MORE
                case _ =>
                  throw new RuntimeException(s"Unexpected cardinality $card")
              }
            }
            s9apiSequenceType = SequenceType.makeSequenceType(itemtype, cardinality)
          case _ =>
            if (seqType.get.startsWith("map(")) {
              s9apiSequenceType = SequenceType.makeSequenceType(ItemType.ANY_MAP, OccurrenceIndicator.ONE)
            } else {
              throw new RuntimeException(s"Unparsable sequence type: ${seqType.get}")
            }
        }
        Some(s9apiSequenceType)
      } catch {
        case xpe: XPathException =>
          throw XProcException.xsInvalidSequenceType(seqType.get, xpe.getMessage, location)
        case t: Throwable =>
          throw t
      }
    } else {
      None
    }
  }

  def parseAvt(value: String): List[String] = {
    val list = ListBuffer.empty[String]
    var state = StateChange.STRING
    var pos = 0
    var substr = ""

    while (pos < value.length) {
      val ch = value.substring(pos, pos + 1)
      val nextch = if (pos + 1 < value.length) {
        value.substring(pos + 1, pos + 2)
      } else {
        ""
      }
      ch match {
        case "{" =>
          if (nextch == "{") {
            pos += 2
            substr += "{"
          } else {
            state match {
              case StateChange.STRING =>
                list += substr
                substr = ""
                state = StateChange.EXPR
                pos += 1
              case StateChange.EXPR =>
                substr += "{"
                pos += 1
            }
          }
        case "}" =>
          if (nextch == "}") {
            pos += 2
            substr += "}"
          } else {
            state match {
              case StateChange.STRING =>
                throw new RuntimeException("Invalid AVT")
              case StateChange.EXPR =>
                if (list.isEmpty) {
                  list += ""
                }
                list += substr
                substr = ""
                state = StateChange.STRING
                pos += 1
            }
          }
        case _ =>
          substr += ch
          pos += 1
      }
    }

    if (state != StateChange.STRING) {
      throw new RuntimeException("Invalid AVT")
    }

    if (substr != "") {
      list += substr
    }

    list.toList
  }

  def findVariableRefsInAvt(list: List[String]): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    var avt = false
    for (substr <- list) {
      if (avt) {
        variableRefs ++= findVariableRefsInString(substr)
      }
      avt = !avt
    }

    variableRefs.toSet
  }

  def findVariableRefsInString(text: String): Set[QName] = {
    findVariableRefsInString(Some(text))
  }

  def findVariableRefsInString(text: Option[String]): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    if (text.isDefined) {
      val parser = config.expressionParser
      parser.parse(text.get)
      for (ref <- parser.variableRefs) {
        val qname = ValueParser.parseClarkName(ref)
        variableRefs += qname
      }
    }

    variableRefs.toSet
  }

  def dependsOnContextAvt(list: List[String]): Boolean = {
    var depends = false

    var avt = false
    for (substr <- list) {
      if (avt) {
        depends = depends || dependsOnContextString(substr)
      }
      avt = !avt
    }

    depends
  }

  def dependsOnContextString(expr: String): Boolean = {
    if (findVariableRefsInString(expr).isEmpty) {
      val xcomp = config.processor.newXPathCompiler()
      val xexec = xcomp.compile(expr)
      val xexpr = xexec.getUnderlyingExpression.getInternalExpression
      ExpressionTool.dependsOnFocus(xexpr)
    } else {
      true
    }
  }

  object StateChange {
    val STRING = 0
    val EXPR = 1
  }
}
