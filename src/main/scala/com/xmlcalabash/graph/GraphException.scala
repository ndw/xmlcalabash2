package com.xmlcalabash.graph

import net.sf.saxon.s9api.QName

/**
  * Created by ndw on 10/8/16.
  */
class GraphException(val msg: String) extends RuntimeException {
  println(msg)
}
