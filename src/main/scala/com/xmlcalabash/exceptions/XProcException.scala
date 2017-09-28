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

  private def internalError(code: Int, location: Option[Location]): XProcException = {
    internalError(code, location, List())
  }

  private def internalError(code: Int, location: Option[Location], args: Any): XProcException = {
    internalError(code, location, List(args))
  }

  private def internalError(code: Int, location: Option[Location], args: List[Any]): XProcException = {
    val qname = new QName("cx", XProcConstants.ns_cx, "XI%04d".format(code))
    new XProcException(qname, None, location)
  }

  def dynamicError(code: Int): XProcException = {
    dynamicError(code, None)
  }
  def dynamicError(code: Int, location: Option[Location]): XProcException = {
    val qname = new QName("err", XProcConstants.ns_err, "XD%04d".format(code))
    new XProcException(qname)
  }

  def staticError(code: Int): XProcException = {
    staticError(code, None)
  }
  def staticError(code: Int, location: Option[Location]): XProcException = {
    val qname = new QName("err", XProcConstants.ns_err, "XS%04d".format(code))
    new XProcException(qname)
  }
  def staticError(code: Int, location: Option[Location], details: List[String]): XProcException = {
    val qname = new QName("err", XProcConstants.ns_err, "XS%04d".format(code))
    new XProcException(qname)
  }
}

class XProcException(val code: QName, val message: Option[String], val location: Option[Location]) extends Throwable {
  def this(code: QName) {
    this(code, None, None)
  }

  def this(code: QName, message: String) {
    this(code, Some(message), None)
  }

  def this(code: QName, message: String, location: Location) {
    this(code, Some(message), Some(location))
  }

  def this(code: QName, message: String, context: ExpressionContext) {
    this(code, Some(message), context.location)
  }

  def this(code: QName, context: ExpressionContext) {
    this(code, None, context.location)
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
