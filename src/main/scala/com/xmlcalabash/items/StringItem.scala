package com.xmlcalabash.items

/**
  * Created by ndw on 10/3/16.
  */
class StringItem(data: String) extends GenericItem {
  override def contentType: String = "text/plain+xsd:string"
  def get = data

  override def toString: String = {
    "«" + data + "»"
  }
}
