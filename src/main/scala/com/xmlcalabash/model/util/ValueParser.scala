package com.xmlcalabash.model.util

import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.Location
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmMap, XdmValue}

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

  def parseDocumentProperties(value: XdmItem, location: Option[Location]): Map[String, XdmAtomicValue] = {
    val params = mutable.HashMap.empty[String, XdmAtomicValue]

    value match {
      case map: XdmMap =>
        // Grovel through a Java Map
        val iter = map.keySet().iterator()
        while (iter.hasNext) {
          val key = iter.next()
          val value = map.get(key)

          val strkey = key match {
            case atomic: XdmAtomicValue =>
              val itype = atomic.getTypeName
              if (itype != XProcConstants.xs_string) {
                throw XProcException.xiDocPropsKeyNotString(location, atomic)
              }
              atomic.getStringValue
            case _ =>
              throw XProcException.xiDocPropsKeyNotString(location, key)
          }

          var count = 0
          var strvalue = ""
          val viter = value.iterator()
          while (viter.hasNext) {
            val item = viter.next()
            item match {
              case atomic: XdmAtomicValue =>
              //val itype = atomic.getTypeName
              // FIXME: make sure some keys have the proper value (base-uri, etc.)
                params.put(key.asInstanceOf[XdmAtomicValue].getStringValue, atomic)
              case _ =>
                throw XProcException.xiDocPropsValueNotAtomic(location, item)
            }
            count += 1

            if (count > 1) {
              throw XProcException.xiDocPropsValueNotAtomic(location, item)
            }

            strvalue += item.getStringValue
          }
        }
      case _ =>
        throw XProcException.xiDocPropsNotMap(location, value)
    }

    params.toMap
  }

  object StateChange {
    val STRING = 0
    val EXPR = 1
  }
}
