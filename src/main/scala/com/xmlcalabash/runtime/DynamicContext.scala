package com.xmlcalabash.runtime

import java.net.URI

import com.jafpl.graph.Location
import com.jafpl.messages.Message
import net.sf.saxon.om.Item
import net.sf.saxon.s9api.{QName, XdmNode}
import net.sf.saxon.value.StringValue

import scala.collection.mutable
import scala.util.DynamicVariable

object DynamicContext {
  private val _dynContext = new DynamicVariable[DynamicContext](null)
  def withContext[T](context: DynamicContext)(thunk: => T): T = _dynContext.withValue(context)(thunk)
  def dynContext: Option[DynamicContext] = Option(_dynContext.value)
}

class DynamicContext {
  private var _iterationPosition = Option.empty[Long]
  private var _iterationSize = Option.empty[Long]
  private val _documents = mutable.HashMap.empty[Item,Message]
  private val _imessages = mutable.HashMap.empty[Message,Item]
  private val _messages = mutable.HashMap.empty[Message,XdmNode]
  private var _location = Option.empty[Location]
  private var _baseURI = Option.empty[URI]
  private var _injElapsed = Option.empty[Double]
  private var _injName = Option.empty[String]
  private var _injId = Option.empty[String]
  private var _injType = Option.empty[QName]

  def iterationPosition: Option[Long] = _iterationPosition
  def iterationSize: Option[Long] = _iterationSize

  def message(document: Item): Option[Message] = {
    _documents.get(document)
  }

  def document(message: Message): Option[XdmNode] = {
    _messages.get(message)
  }

  def location: Option[Location] = _location
  def location_=(loc: Location): Unit = {
    _location = Some(loc)
  }

  def injElapsed: Option[Double] = _injElapsed
  def injElapsed_=(elapsed: Double): Unit = {
    _injElapsed = Some(elapsed)
  }

  def injName: Option[String] = _injName
  def injName_=(name: String): Unit = {
    _injName = Some(name)
  }

  def injId: Option[String] = _injId
  def injId_=(id: String): Unit = {
    _injId = Some(id)
  }

  def injType: Option[QName] = _injType
  def injType_=(stype: QName): Unit = {
    _injType = Some(stype)
  }

  def addDocument(doc: XdmNode, msg: Message): Unit = {
    _documents.put(doc.getUnderlyingNode, msg)
    _messages.put(msg, doc)
    _imessages.put(msg, doc.getUnderlyingNode)
  }

  def addItem(item: Item, msg: Message): Unit = {
    item match {
      case s: StringValue =>
        _documents.put(new FakeStringValue(s), msg)
      case _ =>
        _documents.put(item, msg)
    }

    _imessages.put(msg, item)
  }

  // WTF? Saxon's StringValue class throws an exception if you call .equals() on it.
  // So this is a wrapper that doesn't do that.
  private class FakeStringValue(str: StringValue) extends StringValue {
    override def equals(other: Any): Boolean = {
      other match {
        case s: StringValue =>
          str.toString == s.toString
        case _ =>
          false
      }
    }
  }
}
