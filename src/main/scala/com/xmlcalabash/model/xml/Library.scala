package com.xmlcalabash.model.xml

import com.xmlcalabash.config.{StepSignature, XMLCalabashConfig}

import scala.collection.mutable.ListBuffer

class Library(override val config: XMLCalabashConfig) extends Artifact(config) with DeclContainer {
  private var _signatures = ListBuffer.empty[StepSignature]

  def inScopeDeclarations: List[StepSignature] = _signatures.toList

  override def addDeclaration(decl: StepSignature): Unit = {
    _signatures += decl
  }

  override protected[model] def makeStructureExplicit(environment: Environment): Unit = {
    for (child <- allChildren) {
      child match {
        case decl: DeclareStep =>
          val newenvironment = environment.declareStep()
          decl.makeStructureExplicit(newenvironment)
          _signatures += decl.signature
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

  override protected[model] def makeBindingsExplicit(environment: Environment, initialDrp: Option[Port]): Unit = {
    for (child <- allChildren) {
      child match {
        case decl: DeclareStep =>
          val newenvironment = environment.declareStep()
          decl.makeBindingsExplicit(newenvironment)
        case variable: Variable =>
          variable.makeBindingsExplicit(environment, None)
          environment.addVariable(variable)
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
        case option: DeclareOption =>
          if (!option.static) {
            throw new RuntimeException("Only static options are allowed in a p:library")
          }
        case step: DeclareStep => Unit
        case function: DeclareFunction => Unit
        case _ =>
          throw new RuntimeException(s"Child not allowed: $child")
      }
    }

    if (children.nonEmpty) {
      throw new RuntimeException("Invalid content in p:option")
    }
  }


}