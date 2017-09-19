package com.xmlcalabash.runtime

import java.net.URI

import com.jafpl.graph.Location
import com.jafpl.messages.{ItemMessage, Message}
import com.xmlcalabash.messages.XPathItemMessage
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmNode}

import scala.collection.mutable
import scala.util.DynamicVariable

object DynamicContext {
  private val _dynContext = new DynamicVariable[DynamicContext](null)
  def withContext[T](context: DynamicContext)(thunk: => T): T = _dynContext.withValue(context)(thunk)
  def dynContext: Option[DynamicContext] = Option(_dynContext.value)

  def properties(doc: XdmNode): Option[Map[QName,XdmItem]] = {
    if (dynContext.isDefined && dynContext.get.message(doc.getUnderlyingNode).isDefined) {
      val msg = dynContext.get.message(doc.getUnderlyingNode).get
      msg match {
        case item: ItemMessage =>
          item.metadata match {
            case meta: XProcMetadata =>
              Some(meta.properties)
            case _ => None
          }
        case _ => None
      }
    } else {
      None
    }
  }
}

class DynamicContext {
  private var _iterationPosition = Option.empty[Long]
  private var _iterationSize = Option.empty[Long]
  private var _documents = mutable.HashMap.empty[NodeInfo,Message]
  private var _location = Option.empty[Location]
  private var _baseURI = Option.empty[URI]

  def iterationPosition: Option[Long] = _iterationPosition
  def iterationSize: Option[Long] = _iterationSize
  def message(doc: NodeInfo): Option[Message] = {
    _documents.get(doc)
  }
  def location: Option[Location] = _location
  def location_=(loc: Location): Unit = {
    _location = Some(loc)
  }

  def addDocument(doc: NodeInfo, msg: Message): Unit = {
    _documents.put(doc, msg)
  }
}
