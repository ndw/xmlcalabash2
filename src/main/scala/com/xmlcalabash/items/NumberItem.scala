package com.xmlcalabash.items

/**
  * Created by ndw on 10/7/16.
  */
class NumberItem(data: Int) extends GenericItem  {
  override def contentType: String = "text/plain+xsd:string"

  def get = data

  override def toString: String = {
    data.toString
  }
}
