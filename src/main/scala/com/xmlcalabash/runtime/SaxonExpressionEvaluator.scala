package com.xmlcalabash.runtime

import java.net.URI

import com.jafpl.exceptions.PipelineException
import com.jafpl.runtime.ExpressionEvaluator
import net.sf.saxon.s9api.{QName, SaxonApiException, SaxonApiUncheckedException, XPathExecutable, XdmAtomicValue, XdmItem}
import net.sf.saxon.trans.XPathException

import scala.collection.mutable.ListBuffer
import scala.util.DynamicVariable

class SaxonExpressionEvaluator(runtime: SaxonRuntimeConfiguration) extends ExpressionEvaluator {
  private val _stepContext = new DynamicVariable[XmlStep](null)

  def withContext[T](context: XmlStep)(thunk: => T): T = _stepContext.withValue(context)(thunk)

  def stepContext(): Option[XmlStep] = Option(_stepContext.value)

  override def value(xpath: Any, context: List[Any], bindings: Map[String, Any]): Any = {
    var result = ListBuffer.empty[XdmItem]
    xpath match {
      case avtexpr: XProcAvtExpression =>
        var evalAvt = false
        for (part <- avtexpr.avt) {
          if (evalAvt) {
            val epart = computeValue(part, context, avtexpr.nsbindings, bindings, avtexpr.extensionFunctionsAllowed)
            for (item <- epart.asInstanceOf[ListBuffer[XdmItem]]) {
              result += item
            }
          } else {
            result += new XdmAtomicValue(part)
          }
          evalAvt = !evalAvt
        }
      case xpathexpr: XProcXPathExpression =>
        val epart = computeValue(xpathexpr.expr, context, xpathexpr.nsbindings, bindings, xpathexpr.extensionFunctionsAllowed)
        for (item <- epart.asInstanceOf[ListBuffer[XdmItem]]) {
          result += item
        }
      case str: String =>
        val epart = computeValue(str, context, Map.empty[String,String], bindings, false)
        for (item <- epart.asInstanceOf[ListBuffer[XdmItem]]) {
          result += item
        }
      case _ => throw new PipelineException("unexpected", "Unexpected type passed to value", None)
    }

    if (result.size == 1) {
      result.head
    } else {
      result.toList
    }
  }

  private def computeValue(xpath: Any,
                           context: List[Any],
                           nsbindings: Map[String,String],
                           bindings: Map[String, Any],
                           extensionsOk: Boolean): Any = {
    val results = ListBuffer.empty[XdmItem]
    val config = runtime.processor.getUnderlyingConfiguration

    // FIXME: Add a dynamic context object.

    if (context.size > 1) {
      throw new PipelineException("seq", "Sequence not allowed as context for expression", None)
    }

    try {
      val xcomp = runtime.processor.newXPathCompiler()
      val baseURI: URI = new URI("") // FIXME: get baseURI from dynamic context
      if (baseURI.toASCIIString != "") {
        xcomp.setBaseURI(baseURI)
      }

      if (extensionsOk) {
        throw new PipelineException("notimpl", "Extension functions aren't implemented yet", None)
      }

      for (varname <- bindings.keySet) {
        // FIXME: parse Clark names
        xcomp.declareVariable(new QName("", varname))
      }

      for ((prefix, uri) <- nsbindings) {
        xcomp.declareNamespace(prefix, uri)
      }

      var xexec: XPathExecutable = null // Yes, I know.
      try {
        xexec = xcomp.compile(xpath.toString)
      } catch {
        case sae: SaxonApiException =>
          sae.getCause match {
            case xpe: XPathException =>
              if (xpe.getMessage.contains("Undeclared variable")) {
                throw new PipelineException("undecl", xpe.getMessage, None)
              } else {
                throw sae
              }
            case _ => throw sae
          }
        case other: Throwable =>
          throw other
      }

      val selector = xexec.load()

      for ((varname, varvalue) <- bindings) {
        // FIXME: parse Clark names
        val avalue: XdmAtomicValue = new XdmAtomicValue(varvalue.toString) // FIXME: handle other types
        selector.setVariable(new QName("", varname), avalue)
      }

      if (context.nonEmpty) {
        context.head match {
          case item: XdmItem =>
            selector.setContextItem(item)
          case _ => throw new PipelineException("badcontext", "Expression context is not an XML item", None)
        }
      }

      try {
        val values = selector.iterator()
        while (values.hasNext) {
          results += values.next()
        }
      } catch {
        case saue: SaxonApiUncheckedException =>
          saue.getCause match {
            case xpe: XPathException =>
              if ((xpe.getErrorCodeNamespace == "http://www.w3.org/2005/xqt-errors")
                   && xpe.getErrorCodeLocalPart == "XPDY0002") {
                throw new PipelineException("nocontext","Expression refers to context when none is available: " + xpath, None)
              } else {
                throw saue
              }
            case _ => throw saue
          }
        case other: Throwable =>
          throw other
      }


    } catch {
      case sae: SaxonApiException => throw sae
      case other: Throwable => throw other

    }

    results
  }

  override def booleanValue(expr: Any, context: List[Any], bindings: Map[String, Any]): Boolean = {
    println("EVALBOO: false")
    false
  }
}
