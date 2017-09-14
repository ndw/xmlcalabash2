package com.xmlcalabash.runtime

import com.jafpl.graph.Location
import com.jafpl.messages.Message
import net.sf.saxon.om.NodeInfo

import scala.collection.mutable

class DynamicContext {
  private var _iterationPosition = Option.empty[Long]
  private var _iterationSize = Option.empty[Long]
  private var _documents = mutable.HashMap.empty[NodeInfo,Message]
  private var _location = Option.empty[Location]

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
