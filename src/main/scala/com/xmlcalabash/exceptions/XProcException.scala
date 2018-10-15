package com.xmlcalabash.exceptions

import java.net.URI

import com.jafpl.exceptions.JafplException
import com.jafpl.graph.Location
import com.jafpl.messages.{Message, Metadata}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.Artifact
import com.xmlcalabash.runtime.{ExpressionContext, XProcExpression}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmNode}

object XProcException {
  def xiUnkExprType(location: Option[Location]): XProcException = internalError(1, location)
  def xiInvalidMessage(location: Option[Location], message: Message): XProcException = internalError(2, location, message)
  def xiBadBoundValue(location: Option[Location], value: Any): XProcException = internalError(3, location, value)
  def xiUnexpectedExprType(location: Option[Location], expr: Any): XProcException = internalError(4, location, expr)
  def xiSeqNotSupported(location: Option[Location], expr: XProcExpression): XProcException = internalError(5, location, expr)
  def xiInvalidClarkName(location: Option[Location], name: String): XProcException = internalError(6, location, name)
  def xiInvalidMetadata(location: Option[Location], metadata: Metadata): XProcException = internalError(7, location, metadata)
  def xiExtFunctionNotAllowed(): XProcException = internalError(8, None)
  def xiInvalidAVT(location: Option[Location], expr: String): XProcException = internalError(9,location, expr)
  def xiParamsNotMap(location: Option[Location], props: Any): XProcException = internalError(10, location, props)
  def xiDocPropsUnavail(location: Option[Location], baseURI: URI): XProcException = internalError(11, location, baseURI)
  def xiDocPropsNotMap(location: Option[Location], props: Any): XProcException = internalError(12, location, props)
  def xiDocPropsKeyNotString(location: Option[Location], key: Any): XProcException = internalError(13, location, key)
  def xiDocPropsValueNotAtomic(location: Option[Location], key: Any): XProcException = internalError(14, location, key)
  def xiNotInInjectable(): XProcException = internalError(15, None)
  def xiNoBindingForPort(port: String): XProcException = internalError(16, None, port)
  def xiInjectMessageNodes(location: Option[Location]): XProcException = internalError(17, location)
  def xiInjectRedefPort(location: Option[Location]): XProcException = internalError(18, location)
  def xiChildNotFound(location: Option[Location]): XProcException = internalError(19, location)
  def xiBadPatch(node: Artifact, location: Option[Location]): XProcException = internalError(20, location, node)
  def xiBadPatchChild(node: Artifact, location: Option[Location]): XProcException = internalError(21, location, node)
  def xiBadMessage(message: Message, location: Option[Location]): XProcException = internalError(22, location, message)
  def xiInvalidPort(port: String, location: Option[Location]): XProcException = internalError(23, location, port)
  def xiInvalidPropertyValue(value: Any, location: Option[Location]): XProcException = internalError(24, location, value)
  def xiRedefId(id: String, location: Option[Location]): XProcException = internalError(25, location, id)
  def xiMergeBadRoot(root: QName, location: Option[Location]): XProcException = internalError(26, location, root)
  def xiMergeBadText(text: String, location: Option[Location]): XProcException = internalError(27, location, text)
  def xiMergeBadValue(value: Any, location: Option[Location]): XProcException = internalError(28, location, value)
  def xiMergeDup(key: QName, location: Option[Location]): XProcException = internalError(29, location, key)
  def xiMergeXsiTypeOnNode(location: Option[Location]): XProcException = internalError(30, location)
  def xiMergeBadAtomic(ptype: QName, location: Option[Location]): XProcException = internalError(31, location, ptype)
  def xiWrapItems(location: Option[Location]): XProcException = internalError(32, location)
  def xiWrapXML(location: Option[Location]): XProcException = internalError(33, location)
  def xiCastXML(value: Any, location: Option[Location]): XProcException = internalError(34, location, value)
  def xiMediaType(value: Any, location: Option[Location]): XProcException = internalError(35, location, value)
  def xiArgBundleNoPipeline(): XProcException = internalError(36, None)
  def xiArgBundleRedefined(name: QName): XProcException = internalError(37, None, name)
  def xiArgBundlePfxChar(str: String): XProcException = internalError(38, None, str)
  def xiArgBundleCannotParseInput(str: String): XProcException = internalError(39, None, str)
  def xiArgBundleCannotParseOutput(str: String): XProcException = internalError(40, None, str)
  def xiArgBundleRedefinedNamespace(pfx: String): XProcException = internalError(41, None, pfx)
  def xiArgBundleCannotParseNamespace(str: String): XProcException = internalError(42, None, str)
  def xiArgBundleUnexpectedOption(str: String): XProcException = internalError(43, None, str)
  def xiArgBundleIndexOOB(str: String): XProcException = internalError(44, None, str)
  def xiArgBundleMultiplePipelines(p1: String, p2: String): XProcException = internalError(45, None, List(p1, p2))
  def xiArgBundleInvalidPortSpec(spec: String): XProcException = internalError(46, None, spec)
  def xiNotAnXPathExpression(expr: Any, location: Option[Location]): XProcException = internalError(47, location, expr)
  def xiNotXMLCalabash(): XProcException = internalError(48, None)
  def xiDifferentXMLCalabash(): XProcException = internalError(49, None)
  def xiNodesNotAllowed(node: XdmNode): XProcException = internalError(50, None, node)
  def xiWrongImplParams(): XProcException = internalError(51, None)
  def xiNoSuchPortOnAccept(port: String): XProcException = internalError(52, None, List(port))
  def xiBadValueOnFileLoader(variable: String): XProcException = internalError(53, None, List(variable))

  def xdSequenceNotAllowedOnIf(location: Option[Location]): XProcException = dynamicError(5, location)
  def xdInputSequenceNotAllowed(port: String, location: Option[Location]): XProcException = dynamicError(6, port, location)
  def xdOutputSequenceNotAllowed(port: String, location: Option[Location]): XProcException = dynamicError(7, port, location)
  def xdSequenceNotAllowedAsContext(location: Option[Location]): XProcException = dynamicError(8, location)
  def xdContextItemSequence(location: Option[Location]): XProcException = dynamicError(8, location)
  def xdNotWFXML(href: String, message: String): XProcException = dynamicError(11, List(href, message))
  def xdInvalidSelection(expr: String, selected: String, location: Option[Location]): XProcException = dynamicError(16, List(expr,selected), location)
  def xdBadValue(value: String, vtype: String, message: String, location: Option[Location]): XProcException = dynamicError(19, List(value,vtype, message), location)
  def xdNotAuthorized(href: String, message: String): XProcException = dynamicError(21, List(href, message))
  def xdNotValidXML(href: String, message: String): XProcException = dynamicError(23, List(href, message))
  def xdConflictingNamespaceDeclarations(msg: String, location: Option[Location]): XProcException = dynamicError(34, msg, location)
  def xdBadMediaType(ctype: MediaType, allowed: List[MediaType]): XProcException = dynamicError(38, List(ctype, allowed))
  def xdMismatchedContentType(declType: MediaType, propType: MediaType, location: Option[Location]): XProcException = dynamicError(62, List(declType,propType), location)

  //def xsUnconnectedInputPort(step: String, port: String, location: Option[Location]): XProcException = staticError(3, List(step,port), location)
  def xsUnconnectedInputPort(step: String, port: String, location: Option[Location]): XProcException = staticError(3, List(step,port), location)
  def xsDupOptionname(location: Option[Location], name: String): XProcException = staticError(4, name, location)
  def xsUnconnectedOutputPort(step: String, port: String, location: Option[Location]): XProcException = staticError(6, List(step, port), location)
  def xsBadAttribute(name: QName, location: Option[Location]): XProcException = staticError(8, name, location)
  def xsDupPortName(port: String, location: Option[Location]): XProcException = staticError(11, port, location)
  def xsMissingRequiredOption(optName: QName, location: Option[Location]): XProcException = staticError(18, optName, location)
  def xsDupPrimaryPort(port: String, primaryPort: String, location: Option[Location]): XProcException = staticError(30, List(port, primaryPort), location)
  def xsUndeclaredOption(stepType: QName, optName: QName, location: Option[Location]): XProcException = staticError(31, List(stepType,optName), location)
  def xsUnconnectedPrimaryInputPort(step: String, port: String, location: Option[Location]): XProcException = staticError(32, List(step,port), location)
  def xsMissingRequiredAttribute(attName: QName, location: Option[Location]): XProcException = staticError(38, attName, location)
  def xsElementNotAllowed(location: Option[Location], element: QName, message: String): XProcException = staticError(44, List(element, message), location)
  def xsElementNotAllowed(location: Option[Location], element: QName): XProcException = staticError(44, List(element, "element is not allowed here"), location)

  def xsTextNotAllowed(location: Option[Location], text: String): XProcException = staticError(999, text, location)

  def xsBadTypeValue(name: String, reqdType: String): XProcException = staticError(77, List(name, reqdType), None)
  def xsDupWithOptionName(optName: QName, location: Option[Location]): XProcException = staticError(80, optName, location)
  def xsNoSiblingsOnEmpty(location: Option[Location]): XProcException = staticError(89, None, location)
  def xsNoSelectOnStaticOption(location: Option[Location]): XProcException = staticError(93, None, location)
  def xsNoSelectOnVariable(location: Option[Location]): XProcException = staticError(94, None, location)
  def xsInvalidSequenceType(seqType: String, errMsg: String, location: Option[Location]): XProcException = staticError(96, List(seqType, errMsg), location)

  def xcNotAnElement(pattern: String, nodeType: String, location: Option[Location]): XProcException = stepError(23, List(pattern, nodeType), location)

  private def internalError(code: Int, location: Option[Location]): XProcException = {
    internalError(code, location, List())
  }

  private def internalError(code: Int, location: Option[Location], args: Any): XProcException = {
    internalError(code, location, List(args))
  }

  private def internalError(code: Int, location: Option[Location], args: List[Any]): XProcException = {
    val qname = new QName("cx", XProcConstants.ns_cx, "XI%04d".format(code))
    new XProcException(qname, None, location, args)
  }

  def dynamicError(code: Int): XProcException = {
    dynamicError(code, List.empty[Any], None)
  }
  def dynamicError(code: Int, details: Any): XProcException = {
    dynamicError(code, List(details), None)
  }
  def dynamicError(code: Int, details: List[Any]): XProcException = {
    dynamicError(code, details, None)
  }
  def dynamicError(code: Int, location: Option[Location]): XProcException = {
    dynamicError(code, List.empty[Any], location)
  }
  def dynamicError(code: Int, details: Any, location: Option[Location]): XProcException = {
    dynamicError(code, List(details), location)
  }
  def dynamicError(code: Int, details: List[Any], location: Option[Location]): XProcException = {
    val qname = dynamicErrorCode(code)
    new XProcException(qname, None, location, details)
  }

  def dynamicErrorCode(code: Int): QName = {
    new QName("err", XProcConstants.ns_err, "XD%04d".format(code))
  }

  def staticError(code: Int): XProcException = {
    staticError(code, List.empty[Any], None)
  }
  def staticError(code: Int, details: Any): XProcException = {
    staticError(code, List(details), None)
  }
  def staticError(code: Int, details: List[Any]): XProcException = {
    staticError(code, details, None)
  }
  def staticError(code: Int, location: Option[Location]): XProcException = {
    staticError(code, List.empty[Any], location)
  }
  def staticError(code: Int, details: Any, location: Option[Location]): XProcException = {
    staticError(code, List(details), location)
  }
  def staticError(code: Int, details: List[Any], location: Option[Location]): XProcException = {
    val qname = staticErrorCode(code)
    new XProcException(qname, None, location, details)
  }

  def staticErrorCode(code: Int): QName = {
    new QName("err", XProcConstants.ns_err, "XS%04d".format(code))
  }

  def stepError(code: Int): XProcException = {
    stepError(code, List.empty[Any], None)
  }
  def stepError(code: Int, details: Any): XProcException = {
    stepError(code, List(details), None)
  }
  def stepError(code: Int, details: List[Any]): XProcException = {
    stepError(code, details, None)
  }
  def stepError(code: Int, location: Option[Location]): XProcException = {
    stepError(code, List.empty[Any], location)
  }
  def stepError(code: Int, details: Any, location: Option[Location]): XProcException = {
    stepError(code, List(details), location)
  }
  def stepError(code: Int, details: List[Any], location: Option[Location]): XProcException = {
    val qname = new QName("err", XProcConstants.ns_err, "XC%04d".format(code))
    new XProcException(qname, None, location, details)
  }

  def mapPipelineException(ex: Exception): Exception = {
    ex match {
      case jex: JafplException =>
        jex.code match {
          case JafplException.DUP_OPTION_NAME => XProcException.xsDupOptionname(jex.location, jex.details.head.asInstanceOf[String])
          // Port missing must be a cardinality error
          case JafplException.INPUT_PORT_MISSING => XProcException.xdInputSequenceNotAllowed(jex.details.head.asInstanceOf[String], jex.location)
          case JafplException.INPUT_CARDINALITY_ERROR => XProcException.xdInputSequenceNotAllowed(jex.details.head.asInstanceOf[String], jex.location)
          case JafplException.OUTPUT_CARDINALITY_ERROR => XProcException.xdOutputSequenceNotAllowed(jex.details.head.asInstanceOf[String], jex.location)
          case _ => jex
        }
      case _ => ex
    }
  }
}

class XProcException(val code: QName, val message: Option[String], val location: Option[Location], val details: List[Any]) extends Exception {
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

  override def getMessage: String = {
    message.getOrElse("")
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
