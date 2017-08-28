package com.xmlcalabash.steps

import com.xmlcalabash.runtime.{XmlMetadata, XmlPortSpecification}

import scala.collection.mutable

class Producer() extends DefaultStep {
  private val _items = mutable.ListBuffer.empty[String]

  def items: List[String] = _items.toList
  def items_=(values: List[String]): Unit = {
    _items.clear()
    for (item <- values) {
      _items += item
    }
  }

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def run(): Unit = {
    for (item <- items) {
      consumer.get.receive("result", item, new XmlMetadata("text/plain"))
    }
  }
}
