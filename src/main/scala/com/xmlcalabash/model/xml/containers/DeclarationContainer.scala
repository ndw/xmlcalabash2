package com.xmlcalabash.model.xml.containers

import com.xmlcalabash.config.{StepSignature, XMLCalabash}
import com.xmlcalabash.model.xml.{Artifact, DeclareStep, Function, Library}
import net.sf.saxon.s9api.QName

import scala.collection.mutable.ListBuffer

class DeclarationContainer(override val config: XMLCalabash,
                           override val parent: Option[Artifact],
                           override val stepType: QName) extends Container(config, parent, stepType) {
  // FIXME: what about static variables referenced by declared steps?
  protected val _declaredSteps: ListBuffer[Artifact] = ListBuffer.empty[Artifact]
  protected var _declaredFunctions: ListBuffer[Function] = ListBuffer.empty[Function]

  def declaredSteps: List[Artifact] = _declaredSteps.toList
  def declaredFunctions: List[Function] = _declaredFunctions.toList
  def declarations: List[Artifact] = {
    val buf = ListBuffer.empty[Artifact]
    buf ++= _declaredSteps
    buf ++= _declaredFunctions
    buf.toList
  }

  override def stepDeclaration(stepType: QName): Option[DeclareStep] = {
    var stepDecl: Option[DeclareStep] = None
    for (art <- _declaredSteps) {
      art match {
        case decl: DeclareStep =>
          if (decl.declaredType.isDefined && decl.declaredType.get == stepType) {
            stepDecl = Some(decl)
          }
        case _ => Unit
      }
    }

    if (stepDecl.isEmpty && parent.isDefined) {
      stepDecl = parent.get match {
        case decl: DeclarationContainer =>
          decl.stepDeclaration(stepType)
        case _ => None
      }
    }

    stepDecl
  }

  override def stepSignature(stepType: QName): Option[StepSignature] = {
    var stepDecl = stepDeclaration(stepType)

    if (stepDecl.isDefined) {
      Some(stepDecl.get.signature)
    } else {
      if (config.signatures.stepTypes.contains(stepType)) {
        Some(config.signatures.step(stepType))
      } else {
        None
      }
    }
  }

  override def validate(): Boolean = {
    var valid = super.validate()

    for (art <- _declaredSteps) {
      valid = art.validate() && valid
    }
    for (art <- _declaredFunctions) {
      valid = art.validate() && valid
    }

    valid
  }

  override protected[xml] def addChild(child: Artifact): Unit = {
    child match {
      case decl: DeclareStep =>
        _declaredSteps += child
      case lib: Library =>
        _declaredSteps += child
      case func: Function =>
        _declaredFunctions += func
      case _ =>
        children += child
    }
  }
}
