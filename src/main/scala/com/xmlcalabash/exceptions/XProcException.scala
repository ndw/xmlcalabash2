package com.xmlcalabash.exceptions

import java.net.URI

import com.jafpl.graph.Location
import com.jafpl.messages.{Message, Metadata}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ExpressionContext, XProcExpression}
import net.sf.saxon.s9api.QName

object XProcException {
  def xiUnkExprType(location: Option[Location]) = internalError(1, location)
  def xiInvalidMessage(location: Option[Location], message: Message) = internalError(2, location, message)
  def xiBadBoundValue(location: Option[Location], value: Any) = internalError(3, location, value)
  def xiUnexpectedExprType(location: Option[Location], expr: Any) = internalError(4, location, expr)
  def xiSeqNotSupported(location: Option[Location], expr: XProcExpression) = internalError(5, location, expr)
  def xiInvalidClarkName(location: Option[Location], name: String) = internalError(6, location, name)
  def xiInvalidMetadata(location: Option[Location], metadata: Metadata) = internalError(7, location, metadata)
  def xiExtFunctionNotAllowed() = internalError(8, None)
  def xiInvalidAVT(location: Option[Location], expr: String) = internalError(9,location, expr)
  def xiParamsNotMap(location: Option[Location], props: Any) = internalError(10, location, props)
  def xiDocPropsUnavail(location: Option[Location], baseURI: URI) = internalError(11, location, baseURI)
  def xiDocPropsNotMap(location: Option[Location], props: Any) = internalError(12, location, props)
  def xiDocPropsKeyNotString(location: Option[Location], key: Any) = internalError(13, location, key)
  def xiDocPropsValueNotAtomic(location: Option[Location], key: Any) = internalError(14, location, key)
  def xiNotInInjectable() = internalError(15, None)

  private def internalError(code: Int, location: Option[Location]): XProcException = {
    internalError(code, location, List())
  }

  private def internalError(code: Int, location: Option[Location], args: Any): XProcException = {
    internalError(code, location, List(args))
  }

  private def internalError(code: Int, location: Option[Location], args: List[Any]): XProcException = {
    val qname = new QName("cx", XProcConstants.ns_cx, "XI%04d".format(code))
    new XProcException(qname, None, location, List.empty[String])
  }

  def dynamicError(code: Int): XProcException = {
    dynamicError(code, List.empty[String], None)
  }
  def dynamicError(code: Int, details: String): XProcException = {
    dynamicError(code, List(details), None)
  }
  def dynamicError(code: Int, details: List[String]): XProcException = {
    dynamicError(code, details, None)
  }
  def dynamicError(code: Int, location: Option[Location]): XProcException = {
    dynamicError(code, List.empty[String], location)
  }
  def dynamicError(code: Int, details: String, location: Option[Location]): XProcException = {
    dynamicError(code, List(details), location)
  }
  def dynamicError(code: Int, details: List[String], location: Option[Location]): XProcException = {
    val qname = new QName("err", XProcConstants.ns_err, "XD%04d".format(code))
    new XProcException(qname, None, location, details)
  }

  def staticError(code: Int): XProcException = {
    staticError(code, List.empty[String], None)
  }
  def staticError(code: Int, details: String): XProcException = {
    staticError(code, List(details), None)
  }
  def staticError(code: Int, details: List[String]): XProcException = {
    staticError(code, details, None)
  }
  def staticError(code: Int, location: Option[Location]): XProcException = {
    staticError(code, List.empty[String], location)
  }
  def staticError(code: Int, details: String, location: Option[Location]): XProcException = {
    staticError(code, List(details), location)
  }
  def staticError(code: Int, details: List[String], location: Option[Location]): XProcException = {
    val qname = new QName("err", XProcConstants.ns_err, "XS%04d".format(code))
    new XProcException(qname, None, location, details)
  }

  def stepError(code: Int): XProcException = {
    stepError(code, List.empty[String], None)
  }
  def stepError(code: Int, details: String): XProcException = {
    stepError(code, List(details), None)
  }
  def stepError(code: Int, details: List[String]): XProcException = {
    stepError(code, details, None)
  }
  def stepError(code: Int, location: Option[Location]): XProcException = {
    stepError(code, List.empty[String], location)
  }
  def stepError(code: Int, details: String, location: Option[Location]): XProcException = {
    stepError(code, List(details), location)
  }
  def stepError(code: Int, details: List[String], location: Option[Location]): XProcException = {
    val qname = new QName("err", XProcConstants.ns_err, "XC%04d".format(code))
    new XProcException(qname, None, location, details)
  }
}

class XProcException(val code: QName, val message: Option[String], val location: Option[Location], val details: List[String]) extends Throwable {
  def this(code: QName) {
    this(code, None, None, List.empty[String])
  }

  def this(code: QName, message: String) {
    this(code, Some(message), None, List.empty[String])
  }

  def this(code: QName, message: String, location: Location) {
    this(code, Some(message), Some(location), List.empty[String])
  }

  def this(code: QName, message: String, context: ExpressionContext) {
    this(code, Some(message), context.location, List.empty[String])
  }

  def this(code: QName, context: ExpressionContext) {
    this(code, None, context.location, List.empty[String])
  }

  override def toString: String = {
    var msg = "ERROR " + code

    if (location.isDefined) {
      msg += " " + location.get
    }

    if (message.isDefined) {
      msg += " " + message.get
    }

    msg
  }
}
