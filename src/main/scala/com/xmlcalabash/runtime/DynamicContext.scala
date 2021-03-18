package com.xmlcalabash.runtime

import java.net.URI

import com.jafpl.graph.{Location, LoopStart}
import com.jafpl.messages.Message
import com.xmlcalabash.model.xml.{Artifact, ForEach, ForLoop, ForUntil}
import net.sf.saxon.om.Item
import net.sf.saxon.s9api.{QName, XdmNode, XdmValue}

import scala.collection.mutable
import scala.util.DynamicVariable

object DynamicContext {
  private val _dynContext = new DynamicVariable[DynamicContext](null)
  def withContext[T](context: DynamicContext)(thunk: => T): T = _dynContext.withValue(context)(thunk)
  def dynContext: Option[DynamicContext] = Option(_dynContext.value)
}

class DynamicContext() {
  private var _artifact = Option.empty[Artifact]
  private var _iterationPosition = 1L
  private var _iterationSize = 1L
  private val _documents = mutable.HashMap.empty[Any,Message]
  private val _imessages = mutable.HashMap.empty[Message,Any]
  private val _messages = mutable.HashMap.empty[Message,XdmValue]
  private var _location = Option.empty[Location]
  private var _baseURI = Option.empty[URI]
  private var _injElapsed = Option.empty[Double]
  private var _injName = Option.empty[String]
  private var _injId = Option.empty[String]
  private var _injType = Option.empty[QName]

  def this(artifact: Option[Artifact]) {
    this()

    _artifact = artifact
    var found = false
    var p: Option[Artifact] = artifact
    while (!found && p.isDefined) {
      if (p.get.graphNode.isDefined) {
        p.get.graphNode.get match {
          case node: LoopStart =>
            found = true
            _iterationPosition = node.iterationPosition
            _iterationSize = node.iterationSize
          case _ => ()
        }
      }
      p = p.get.parent
    }
  }

  def artifact: Option[Artifact] = _artifact

  def iterationPosition: Long = _iterationPosition
  def iterationSize: Long = _iterationSize

  def message(document: Item[_ <: Item[_]]): Option[Message] = {
    _documents.get(document)
  }

  def document(message: Message): Option[XdmValue] = {
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

  def addDocument(doc: XdmValue, msg: Message): Unit = {
    _messages.put(msg, doc)

    doc match {
      case node: XdmNode =>
        _documents.put(node.getUnderlyingValue, msg)
        _imessages.put(msg, node.getUnderlyingValue)
      case _ =>
        _documents.put(doc, msg)
        _imessages.put(msg, doc)
    }
  }

  def addItem(item: Item[_ <: Item[_]], msg: Message): Unit = {
    _documents.put(item, msg)
    _imessages.put(msg, item)
  }
}
