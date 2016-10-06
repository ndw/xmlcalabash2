package com.xmlcalabash.model.xml

import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class Namespace(val prefix: String, val uri: String) extends Artifact(None, None) {
  override def equals(that: Any): Boolean = {
    that match {
      case that: Namespace => that.canEqual(this) && this.hashCode == that.hashCode
      case _ => false
    }
  }

  def canEqual(a: Any) = a.isInstanceOf[Namespace]

  override def hashCode: Int = {
    val prime = 31
    (prefix.hashCode * prime) + uri.hashCode
  }
}
