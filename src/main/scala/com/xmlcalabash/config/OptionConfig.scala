package com.xmlcalabash.config

import scala.collection.mutable.ListBuffer

class OptionConfig(val name: String) {
  private var _required = true
  private var _declaredType = Option.empty[String]
  private var _tokenList: Option[ListBuffer[String]] = None
  private var _defaultValue = Option.empty[String]

  def required: Boolean = _required
  protected[config] def required_=(req: Boolean): Unit = {
    _required = req
  }

  def declaredType: Option[String] = _declaredType
  protected[config] def declaredType_=(value: String): Unit = {
    _declaredType = Some(value)
  }

  def tokenList: Option[List[String]] = {
    if (_tokenList.isDefined) {
      Some(_tokenList.get.toList)
    } else {
      None
    }
  }
  protected[config] def tokenList_=(list: List[String]): Unit = {
    _tokenList = Some(ListBuffer.empty[String] ++ list)
  }

  def defaultValue: Option[String] = _defaultValue
  protected[config] def defaultValue_=(value: String): Unit = {
    _defaultValue = Some(value)
  }
}
