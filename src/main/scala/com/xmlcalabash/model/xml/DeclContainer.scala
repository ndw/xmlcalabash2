package com.xmlcalabash.model.xml

import com.xmlcalabash.config.{StepSignature, XMLCalabashConfig}
import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DeclContainer(override val config: XMLCalabashConfig) extends Container(config) {
  protected var _psvi_required = Option.empty[Boolean]
  protected var _xpath_version = Option.empty[Double]
  protected var _exclude_inline_prefixes = Option.empty[String]
  protected var _version = Option.empty[Double]

  protected var _signatures: ListBuffer[StepSignature] = ListBuffer.empty[StepSignature]
  private var processed = mutable.HashSet.empty[DeclContainer]

  def inScopeDeclarations: List[StepSignature] = _signatures.toList

  def addSignature(sig: StepSignature): Unit = {
    if (sig.stepType.isEmpty) {
      return
    }

    val dsig = declaration(sig.stepType.get)
    if (dsig.isDefined) {
      if (dsig.get eq sig) {
        return // they're the same signature
      } else {
        throw XProcException.xsDupStepType(sig.stepType.get, location)
      }
    }
    _signatures += sig
  }

  def addSignatures(sigs: List[StepSignature]): Unit = {
    for (sig <- sigs) {
      addSignature(sig)
    }
  }

  protected[model] def loadImports(): Unit = {
    val imap = collection.mutable.HashMap.empty[Artifact,Artifact]

    for (child <- allChildren) {
      child match {
        case decl: DeclareStep =>
          decl.parseDeclarationSignature()
          decl.loadImports()
        case pimport: Import =>
          val icont = pimport.loadImports()
          imap.put(pimport, icont)
        case _ => Unit
      }
    }

    for ((pimport, icont) <- imap) {
      replaceChild(icont, pimport)
    }
  }

  protected[model] def updateInScopeSteps(): Unit = {
    val buf = ListBuffer.empty[DeclContainer]

    for (child <- allChildren) {
      child match {
        case decl: DeclareStep =>
          if (!processed.contains(decl)) {
            processed += decl
            decl.updateInScopeSteps()
          }
          if (decl.signature.isEmpty) {
            decl.parseDeclarationSignature()
          }
          addSignature(decl.signature.get)
        case library: Library =>
          if (!processed.contains(library)) {
            processed += library
            library.updateInScopeSteps()
          }
          addSignatures(library._signatures.toList)
          buf += library
        case _ => Unit
      }
    }

    for (library <- buf) {
      removeChild(library)
    }
  }

  override def declaration(stepType: QName): Option[StepSignature] = {
    var found = Option.empty[StepSignature]

    for (sig <- _signatures) {
      if (sig.stepType.isDefined && sig.stepType.get == stepType) {
        found = Some(sig)
      }
    }

    if (found.isEmpty) {
      if (parent.isDefined) {
        found = parent.get.declaration(stepType)
      } else {
        if (config.builtinSteps.isDefined) {
          for (sig <- config.builtinSteps.get.inScopeDeclarations) {
            if (sig.stepType.isDefined && sig.stepType.get == stepType) {
              found = Some(sig)
            }
          }
        }
      }
    }

    found
  }
}
