package com.xmlcalabash.graph

/**
  * Created by ndw on 10/3/16.
  */
private[graph] class Port(val node: Node, val name: String) {
  override def equals(that: Any): Boolean = {
    that match {
      case that: Port => that.canEqual(this) && this.hashCode == that.hashCode
      case _ => false
    }
  }

  def canEqual(a: Any) = a.isInstanceOf[Port]

  override def hashCode: Int = {
    val prime = 31
    (node.hashCode * prime) + name.hashCode
  }

  override def toString: String = {
    node.toString + "." + name
  }
}
