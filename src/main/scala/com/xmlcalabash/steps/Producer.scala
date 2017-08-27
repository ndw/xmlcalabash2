package com.xmlcalabash.steps

import com.jafpl.steps.PortSpecification

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

  override def inputSpec: PortSpecification = PortSpecification.NONE
  override def outputSpec: PortSpecification = PortSpecification.RESULTSEQ

  override def run(): Unit = {
    for (item <- items) {
      consumer.get.send("result", item)
    }
  }
}
