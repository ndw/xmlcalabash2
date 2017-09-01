package com.xmlcalabash.config

import net.sf.saxon.s9api.QName

import scala.collection.mutable.ListBuffer

class OptionSignature(val name: QName) {
  private var _required = true
  private var _declaredType = Option.empty[String]
  private var _tokenList: Option[ListBuffer[String]] = None
  private var _defaultValue = Option.empty[String]

  def this(name: QName, optType: String, required: Boolean) = {
    this(name)
    _declaredType = Some(optType)
    _required = required
  }

  def required: Boolean = _required
  def required_=(req: Boolean): Unit = {
    _required = req
  }

  def declaredType: Option[String] = _declaredType
  def declaredType_=(value: String): Unit = {
    _declaredType = Some(value)
  }

  def tokenList: Option[List[String]] = {
    if (_tokenList.isDefined) {
      Some(_tokenList.get.toList)
    } else {
      None
    }
  }
  def tokenList_=(list: List[String]): Unit = {
    _tokenList = Some(ListBuffer.empty[String] ++ list)
  }

  def defaultValue: Option[String] = _defaultValue
  def defaultValue_=(value: String): Unit = {
    _defaultValue = Some(value)
  }
}