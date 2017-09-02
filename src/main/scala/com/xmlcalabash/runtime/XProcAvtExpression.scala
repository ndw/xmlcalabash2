package com.xmlcalabash.runtime

import scala.collection.mutable.ListBuffer

class XProcAvtExpression(override val nsbindings: Map[String,String], val avt:List[String])
  extends XProcExpression(nsbindings) {

  override def toString: String = {
    var str = ""
    var isavt = false
    for (item <- avt) {
      if (isavt) {
        str += "{" + item + "}"
      } else {
        str += item
      }
      isavt = !isavt
    }
    str
  }
}
