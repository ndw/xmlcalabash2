package com.xmlcalabash.util

/**
  * Created by ndw on 10/1/16.
  */
object UniqueId {
  private var theNextId: Long = 0
  def nextId = {
    val id = theNextId
    theNextId = theNextId + 1
    id
  }
}
