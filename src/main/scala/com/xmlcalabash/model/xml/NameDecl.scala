package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.bindings.{NamePipe, Pipe}
import com.xmlcalabash.xpath.XPathParser
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable

/**
  * Created by ndw on 10/7/16.
  */
abstract class NameDecl(node: Option[XdmNode], parent: Option[Artifact]) extends Artifact(node, parent) {
  protected var _nameRefs: Option[mutable.ListBuffer[QName]] = _
  protected var _funcRefs: Option[mutable.ListBuffer[QName]] = _
  protected var _declName: Option[QName] = _

  def declaredName: Option[QName] = _declName

  def nameRefs: List[QName] = {
    if (_nameRefs.isDefined) {
      _nameRefs.get.toList
    } else {
      List.empty[QName]
    }
  }

  def funcRefs: List[QName] = {
    if (_funcRefs.isDefined) {
      _funcRefs.get.toList
    } else {
      List.empty[QName]
    }
  }

  def inScopeNamespaces: Map[String, String] = {
    val bindings = mutable.HashMap.empty[String, String]
    var ctx: Option[Artifact] = Some(this)
    while (ctx.isDefined) {
      for (pfx <- ctx.get.nsbindings.keySet) {
        if (!bindings.contains(pfx)) {
          bindings.put(pfx, ctx.get.nsbindings(pfx))
        }
      }
      ctx = ctx.get.parent
    }
    bindings.toMap
  }

  override def addDefaultReadablePort(port: Option[InputOrOutput]): Unit = {
    _drp = port
    for (child <- _children) { child.addDefaultReadablePort(port) }
  }

  override def fixBindingsOnIO(): Unit = {
    var ctx: Option[XPathContext] = None

    for (child <- children) {
      child match {
        case x: XPathContext => ctx = Some(x)
        case _ => Unit
      }
    }

    if (ctx.isEmpty) {
      ctx = Some(new XPathContext(None, Some(this)))
      _children += ctx.get
    }

    if (bindings().isEmpty) {
      if (defaultReadablePort.isDefined) {
        val pipe = new Pipe(None, Some(ctx.get))
        pipe._drp = defaultReadablePort
        ctx.get.addChild(pipe)
      }
    }

    for (varname <- nameRefs) {
      val namedecl = parent.get.findNameDecl(varname, this)
      if (namedecl.isDefined) {
        val pipe = new NamePipe(varname, ctx.get)
        pipe.decl = namedecl.get
        ctx.get.addChild(pipe)
      } else {
        logger.info("Reference to unbound name: " + varname)
        _valid = false
      }
    }

    for (child <- _children) { child.fixBindingsOnIO() }
  }

  override private[model] def parseAttributes(node: XdmNode): Unit = {
    super.parseAttributes(node)

    val lexName = property(XProcConstants._name)
    if (lexName.isDefined) {
      _declName = Some(new QName(lexName.get.value, node))
    }

    _funcRefs = Some(mutable.ListBuffer.empty[QName])
    _nameRefs = Some(mutable.ListBuffer.empty[QName])

    val select = property(XProcConstants._select)
    if (select.isDefined) {
      val xpp = new XPathParser(select.get.value)
      if (xpp.errors) {
        _valid = false
        logger.info("Lexical error in XPath expression: " + select.get.value)
      }

      for (lexqname <- xpp.variableRefs()) {
        val qname = new QName(lexqname, node)
        _nameRefs.get += qname
      }

      for (lexqname <- xpp.functionRefs()) {
        val qname = new QName(lexqname, node)
        _funcRefs.get += qname
      }
    }
  }
}
