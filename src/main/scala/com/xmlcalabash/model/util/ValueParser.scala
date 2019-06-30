package com.xmlcalabash.model.util

import com.jafpl.graph.Location
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.runtime.{StaticContext, XMLCalabashRuntime, XProcExpression, XProcVtExpression, XProcXPathExpression}
import net.sf.saxon.s9api.{Axis, QName, XdmItem, XdmMap, XdmNode, XdmNodeKind, XdmValue}

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

  def findVariableRefsInString(config: XMLCalabashRuntime, text: String, staticContext: StaticContext): Set[QName] = {
    val names = mutable.HashSet.empty[QName]

    val parser = config.expressionParser
    parser.parse(text)
    for (ref <- parser.variableRefs) {
      val qname = parseQName(ref, staticContext)
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

  def parseQName(name: String, staticContext: StaticContext): QName = {
    parseQName(Some(name), staticContext).get
  }

  def parseQName(name: Option[String], staticContext: StaticContext): Option[QName] = {
    if (name.isDefined) {
      val eqname = "^Q\\{(.*)\\}(\\S+)$".r
      name.get match {
        case eqname(uri,local) => Some(new QName(uri, local))
        case _ =>
          if (name.get.contains(":")) {
            val pos = name.get.indexOf(':')
            val prefix = name.get.substring(0, pos)
            val local = name.get.substring(pos+1)
            if (staticContext.inScopeNS.contains(prefix)) {
              Some(new QName(prefix, staticContext.inScopeNS(prefix), local))
            } else {
              throw XProcException.dynamicError(15, name.get, staticContext.location)
            }
          } else {
            Some(new QName("", name.get))
          }
      }
    } else {
      None
    }
  }

  def parseBoolean(value: Option[String], location: Option[Location]): Option[Boolean] = {
    parseBoolean(value, location, false)
  }

  def parseBoolean(value: Option[String], location: Option[Location], static: Boolean): Option[Boolean] = {
    if (value.isDefined) {
      if (value.get == "true" || value.get == "false") {
        Some(value.get == "true")
      } else {
        if (static) {
          throw XProcException.xsBadTypeValue(value.get, "boolean", location)
        } else {
          throw XProcException.xdBadValue(value.get, "boolean", location)
        }
      }
    } else {
      None
    }
  }


  def parseParameters(value: XdmValue, staticContext: StaticContext): Map[QName, XdmValue] = {
    val params = mutable.HashMap.empty[QName, XdmValue]

    value match {
      case map: XdmMap =>
        // Grovel through a Java Map
        val iter = map.keySet().iterator()
        while (iter.hasNext) {
          val key = iter.next()
          val value = map.get(key)

          val qname = ValueParser.parseQName(key.getStringValue, staticContext)
          params.put(qname, value)
        }
      case _ =>
        throw XProcException.xiParamsNotMap(staticContext.location, value)
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
              throw XProcException.dynamicError(45, List(key, "string or QName"), location)
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

  def findVariableRefs(config: XMLCalabashRuntime, expression: XProcExpression, location: Option[Location]): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    expression match {
      case expr: XProcXPathExpression =>
        val parser = config.expressionParser
        parser.parse(expr.expr)
        for (ref <- parser.variableRefs) {
          val qname = ValueParser.parseClarkName(ref)
          variableRefs += qname
        }
      case expr: XProcVtExpression =>
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

  def findVariableRefs(config: XMLCalabashRuntime, node: XdmNode, expandText: Boolean, location: Option[Location]): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    node.getNodeKind match {
      case XdmNodeKind.ELEMENT =>
        var newExpand = expandText
        var iter = node.axisIterator(Axis.ATTRIBUTE)
        while (iter.hasNext) {
          val attr = iter.next().asInstanceOf[XdmNode]
          if (expandText) {
            variableRefs ++= ValueParser.findVariableRefsInTvt(config, attr.getStringValue, location)
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
          variableRefs ++= ValueParser.findVariableRefsInTvt(config, node.getStringValue, location)
        }
      case _ => Unit
    }

    variableRefs.toSet
  }

  private def findVariableRefsInTvt(config: XMLCalabashRuntime, text: String, location: Option[Location]): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    val list = ValueParser.parseAvt(text)
    if (list.isEmpty) {
      throw new ModelException(ExceptionCode.BADAVT, List("TVT", text), location)
    }

    findVariableRefsInAvt(config, list.get, location)
  }

  def findVariableRefsInAvt(config: XMLCalabashRuntime, list: List[String], location: Option[Location]): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    var avt = false
    for (substr <- list) {
      if (avt) {
        variableRefs ++= ValueParser.findVariableRefsInString(config, substr, location)
      }
      avt = !avt
    }

    variableRefs.toSet
  }

  def findVariableRefsInString(config: XMLCalabashRuntime, text: String, location: Option[Location]): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    val parser = config.expressionParser
    parser.parse(text)
    for (ref <- parser.variableRefs) {
      val qname = ValueParser.parseClarkName(ref)
      variableRefs += qname
    }

    variableRefs.toSet
  }

  object StateChange {
    val STRING = 0
    val EXPR = 1
  }
}
