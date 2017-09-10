package com.xmlcalabash.runtime

import com.jafpl.messages.Message
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

class DynamicContext {
  private var _iterationPosition = Option.empty[Long]
  private var _iterationSize = Option.empty[Long]
  private var _documents = mutable.HashMap.empty[NodeInfo,Message]

  def iterationPosition: Option[Long] = _iterationPosition
  def iterationSize: Option[Long] = _iterationSize
  def message(doc: NodeInfo): Option[Message] = {
    _documents.get(doc)
  }

  def addDocument(doc: NodeInfo, msg: Message): Unit = {
    _documents.put(doc, msg)
  }
}
