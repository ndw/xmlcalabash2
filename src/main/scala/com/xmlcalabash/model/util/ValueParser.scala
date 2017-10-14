package com.xmlcalabash.model.util

import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.Location
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.runtime.{XProcAvtExpression, XProcExpression, XProcXPathExpression}
import net.sf.saxon.s9api.{Axis, QName, XdmAtomicValue, XdmItem, XdmMap, XdmNode, XdmNodeKind, XdmValue}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object ValueParser {
  def parseAvt(value: String): Option[List[String]] = {
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
                return None
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
      None
    } else {
      if (substr != "") {
        list += substr
      }
      Some(list.toList)
    }
  }

  def findVariableRefsInString(config: XMLCalabash, inScopeNS: Map[String,String], text: String): Set[QName] = {
    val names = mutable.HashSet.empty[QName]

    val parser = config.expressionParser
    parser.parse(text)
    for (ref <- parser.variableRefs) {
      val qname = parseQName(ref, inScopeNS)
      names += qname
    }

    names.toSet
  }

  def parseClarkName(name: String): QName = {
    parseClarkName(name, None)
  }

  def parseClarkName(name: String, prefix: String): QName = {
    parseClarkName(name, Some(prefix))
  }

  private def parseClarkName(name: String, pfx: Option[String]): QName = {
    // FIXME: Better error handling for ClarkName parsing
    if (name.startsWith("{")) {
      val pos = name.indexOf("}")
      val uri = name.substring(1, pos)
      val local = name.substring(pos + 1)
      if (pfx.isDefined) {
        new QName(pfx.get, uri, local)
      } else {
        new QName(uri, local)
      }
    } else {
      new QName("", name)
    }
  }

  def parseQName(name: String, inScopeNS: Map[String,String]): QName = {
    if (name.contains(":")) {
      val pos = name.indexOf(':')
      val prefix = name.substring(0, pos)
      val local = name.substring(pos+1)
      if (inScopeNS.contains(prefix)) {
        new QName(prefix, inScopeNS(prefix), local)
      } else {
        throw new ModelException(ExceptionCode.NOPREFIX, prefix , None)
      }
    } else {
      new QName("", name)
    }
  }

  def parseBoolean(value: Option[String], location: Option[Location]): Option[Boolean] = {
    if (value.isDefined) {
      if (value.get == "true" || value.get == "false") {
        Some(value.get == "true")
      } else {
        throw new ModelException(ExceptionCode.BADBOOLEAN, value.get, location)
      }
    } else {
      None
    }
  }


  def parseParameters(value: XdmItem, nsBindings: Map[String,String], location: Option[Location]): Map[QName, XdmValue] = {
    val params = mutable.HashMap.empty[QName, XdmValue]

    value match {
      case map: XdmMap =>
        // Grovel through a Java Map
        val iter = map.keySet().iterator()
        while (iter.hasNext) {
          val key = iter.next()
          val value = map.get(key)

          val qname = ValueParser.parseQName(key.getStringValue, nsBindings)
          params.put(qname, value)
        }
      case _ =>
        throw XProcException.xiParamsNotMap(location, value)
    }

    params.toMap
  }

  def parseDocumentProperties(value: XdmItem, location: Option[Location]): Map[QName, XdmItem] = {
    val params = mutable.HashMap.empty[QName, XdmItem]

    value match {
      case map: XdmMap =>
        // Grovel through a Java Map
        val iter = map.keySet().iterator()
        while (iter.hasNext) {
          val key = iter.next()
          val value = map.get(key)

          val keytype = key.getTypeName
          val qkey = keytype match {
            case XProcConstants.xs_QName =>
              key.getQNameValue
            case XProcConstants.xs_string =>
              new QName("", key.getStringValue)
            case _ =>
              throw new RuntimeException("BANG2")
          }

          var count = 0
          val viter = value.iterator()
          while (viter.hasNext) {
            val item = viter.next()
            params.put(qkey, item)
            count += 1

            if (count > 1) {
              throw XProcException.xiDocPropsValueNotAtomic(location, item)
            }
          }
        }
      case _ =>
        throw XProcException.xiDocPropsNotMap(location, value)
    }

    params.toMap
  }

  def findVariableRefs(config: XMLCalabash, expression: XProcExpression, location: Option[Location]): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    expression match {
      case expr: XProcXPathExpression =>
        val parser = config.expressionParser
        parser.parse(expr.expr)
        for (ref <- parser.variableRefs) {
          val qname = ValueParser.parseClarkName(ref)
          variableRefs += qname
        }
      case expr: XProcAvtExpression =>
        var avt = false
        for (subexpr <- expr.avt) {
          if (avt) {
            val parser = config.expressionParser
            parser.parse(subexpr)
            for (ref <- parser.variableRefs) {
              val qname = ValueParser.parseClarkName(ref)
              variableRefs += qname
            }
          }
          avt = !avt
        }
      case _ =>
        throw XProcException.xiUnkExprType(location)
    }

    variableRefs.toSet
  }

  def findVariableRefs(config: XMLCalabash, node: XdmNode, expandText: Boolean, location: Option[Location]): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    node.getNodeKind match {
      case XdmNodeKind.ELEMENT =>
        var newExpand = expandText
        var iter = node.axisIterator(Axis.ATTRIBUTE)
        while (iter.hasNext) {
          val attr = iter.next().asInstanceOf[XdmNode]
          if (expandText) {
            variableRefs ++= ValueParser.findVariableRefsInAvt(config, attr.getStringValue, location)
          }
          if (attr.getNodeName == XProcConstants.p_expand_text) {
            newExpand = ValueParser.parseBoolean(Some(attr.getStringValue), location).get
          }
        }
        iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next().asInstanceOf[XdmNode]
          variableRefs ++= ValueParser.findVariableRefs(config, child, newExpand, location)
        }
      case XdmNodeKind.TEXT =>
        if (expandText) {
          variableRefs ++= ValueParser.findVariableRefsInAvt(config, node.getStringValue, location)
        }
      case _ => Unit
    }

    variableRefs.toSet
  }

  private def findVariableRefsInAvt(config: XMLCalabash, text: String, location: Option[Location]): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    val list = ValueParser.parseAvt(text)
    if (list.isEmpty) {
      throw new ModelException(ExceptionCode.BADAVT, List("TVT", text), location)
    }

    var avt = false
    for (substr <- list.get) {
      if (avt) {
        variableRefs ++= ValueParser.findVariableRefsInString(config, substr, location)
      }
      avt = !avt
    }

    variableRefs.toSet
  }

  def findVariableRefsInString(config: XMLCalabash, text: String, location: Option[Location]): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    val parser = config.expressionParser
    parser.parse(text)
    for (ref <- parser.variableRefs) {
      val qname = ValueParser.parseClarkName(ref)
      variableRefs += qname
    }

    variableRefs.toSet
  }

  def textContentType(contentType: String): Boolean = {
    contentType.startsWith("text/")
  }

  def xmlContentType(contentType: String): Boolean = {
    contentType.startsWith("application/xml") || contentType.startsWith("text/xml") || contentType.contains("+xml")
  }

  def jsonContentType(contentType: String): Boolean = {
    contentType.startsWith("application/json") || contentType.startsWith("text/json") || contentType.contains("+json")
  }

  def htmlContentType(contentType: String): Boolean = {
    contentType.startsWith("text/html")
  }

  object StateChange {
    val STRING = 0
    val EXPR = 1
  }
}
