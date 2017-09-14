package com.xmlcalabash.runtime

import com.jafpl.exceptions.PipelineException
import com.jafpl.steps.PortSpecification
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}

import scala.collection.immutable

/** Useful default port binding specifications.
  *
  */
object XmlPortSpecification {
  /** Allow anything on any ports. */
  val ANY: XmlPortSpecification = new XmlPortSpecification(Map("*" -> "*"), Map("*" -> List("application/octet-stream")))

  /** Allow XML on any ports. */
  val ANYXML: XmlPortSpecification = new XmlPortSpecification(Map("*" -> "*"), Map("*" -> List("application/xml")))

  /** Allow no ports. */
  val NONE: XmlPortSpecification = new XmlPortSpecification(Map(), Map())

  /** Allow a single document of any sort on the `source` port. */
  val ANYSOURCE: XmlPortSpecification = new XmlPortSpecification(Map("source" -> "1"),
    Map("source" -> List("application/octet-stream")))

  /** Allow a single document of any sort on the `result` port. */
  val ANYRESULT: XmlPortSpecification = new XmlPortSpecification(Map("result" -> "1"),
    Map("result" -> List("application/octet-stream")))

  /** Allow a single XML document on the `source` port. */
  val XMLSOURCE: XmlPortSpecification = new XmlPortSpecification(Map("source" -> "1"),
    Map("source" -> List("application/xml")))

  /** Allow a single XML document on the `result` port. */
  val XMLRESULT: XmlPortSpecification = new XmlPortSpecification(Map("result" -> "1"),
    Map("result" -> List("application/xml")))

  /** Allow a sequence of zero or more documents of any sort on the `source` port. */
  val ANYSOURCESEQ: XmlPortSpecification = new XmlPortSpecification(Map("source" -> "*"),
    Map("source" -> List("application/octet-stream")))

  /** Allow a sequence of zero or more documents of any sort on the `result` port. */
  val ANYRESULTSEQ: XmlPortSpecification = new XmlPortSpecification(Map("result" -> "*"),
    Map("result" -> List("application/octet-stream")))

  /** Allow a sequence of zero or more XML documents on the `source` port. */
  val XMLSOURCESEQ: XmlPortSpecification = new XmlPortSpecification(Map("source" -> "*"),
    Map("source" -> List("application/xml")))

  /** Allow a sequence of zero or more XML documents on the `result` port. */
  val XMLRESULTSEQ: XmlPortSpecification = new XmlPortSpecification(Map("result" -> "*"),
    Map("result" -> List("application/xml")))
}

class XmlPortSpecification(spec: immutable.Map[String,String],
                           accept: immutable.Map[String, List[String]]) extends PortSpecification(spec) {
  for (port <- accept.keySet) {
    if (!spec.contains(port)) {
      throw new PipelineException("badport", "Cannot specify accept for a port that isn't specified", None)
    }
  }

  def accepts(port: String, contentType: String): Boolean = {
    if (spec.contains(port)) {
      if (accept.contains(port)) {
        val list = accept(port)
        if (list.contains("application/octet-stream")) {
          true
        } else {
          // FIXME: Handle the subtle cases like application/xml+rdf => application/xml
          list.contains(contentType)
        }
      } else {
        true
      }
    } else {
      false
    }
  }

}
