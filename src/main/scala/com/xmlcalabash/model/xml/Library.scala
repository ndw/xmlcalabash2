package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.s9api.{XdmNode, XdmNodeKind}

class Library(override val config: XMLCalabashConfig) extends DeclContainer(config) {

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    if (attributes.contains(XProcConstants._version)) {
      val vstr = attr(XProcConstants._version).get
      try {
        _version = Some(vstr.toDouble)
      } catch {
        case ex: NumberFormatException =>
          throw XProcException.xsBadVersion(vstr, location)
      }
      if (_version.get != 3.0) {
        throw XProcException.xsInvalidVersion(_version.get, location)
      }
    }
    if (_version.isEmpty) {
      if (node.getParent.getNodeKind == XdmNodeKind.DOCUMENT) {
        throw XProcException.xsVersionRequired(location)
      }
    }
  }

  override protected[model] def makeStructureExplicit(environment: Environment): Unit = {
    for (child <- allChildren) {
      child match {
        case decl: DeclareStep =>
          val newenvironment = environment.declareStep()
          decl.makeStructureExplicit(newenvironment)
        case variable: Variable =>
          variable.makeStructureExplicit(environment)
          environment.addVariable(variable)
        case function: DeclareFunction =>
          function.makeStructureExplicit(environment)
        case _ =>
          throw new RuntimeException(s"Invalid element: $child")
      }
    }
  }

  protected[model] def makeBindingsExplicit(env: Environment): Unit = {
    makeBindingsExplicit(env, None)
  }

  override protected[model] def makeBindingsExplicit(initialEnvironment: Environment, initialDrp: Option[Port]): Unit = {
    val containerEnvironment = configureContainerEnvironment(initialEnvironment)

    for (child <- allChildren) {
      child match {
        case decl: DeclareStep =>
          val newenvironment = containerEnvironment.declareStep()
          decl.makeBindingsExplicit(newenvironment, None)
        case variable: Variable =>
          if (!variable.static) {
            throw new RuntimeException("Variables in libraries must be static")
          }
          variable.makeBindingsExplicit(containerEnvironment, None)
          containerEnvironment.addVariable(variable)
        case function: DeclareFunction =>
          Unit
      }
    }
  }

  override protected[model] def validateStructure(): Unit = {
    for (child <- allChildren) {
      child.validateStructure()
      child match {
        case variable: Variable =>
          if (!variable.static) {
            throw new RuntimeException("Only static variables are allowed in a p:library")
          }
        case step: DeclareStep => Unit
        case function: DeclareFunction => Unit
        case _ =>
          throw new RuntimeException(s"Child not allowed: $child")
      }
    }
  }

  override def toString: String = {
    s"p:library $location $tumble_id"
  }
}