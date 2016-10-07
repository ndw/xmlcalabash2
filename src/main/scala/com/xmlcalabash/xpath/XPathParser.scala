package com.xmlcalabash.xpath

/**
  * Created by ndw on 10/6/16.
  */
class XPathParser(expr: String) {
  val handler = new FindRefs()
  val parser = new CR_xpath_31_20151217(expr, handler)
  var _errors = false

  try {
    parser.parse_XPath()
  } catch {
    case e: Throwable =>
      _errors = true
  }

  def errors = _errors

  def variableRefs(): List[String] = {
    handler.variableRefs()
  }

  def functionRefs(): List[String] = {
    handler.functionRefs()
  }
}
