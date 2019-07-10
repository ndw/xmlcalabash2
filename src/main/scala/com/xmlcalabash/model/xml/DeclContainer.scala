package com.xmlcalabash.model.xml

import com.xmlcalabash.config.StepSignature

trait DeclContainer {
  protected[model] def inScopeDeclarations: List[StepSignature]
  protected[model] def addDeclaration(decl: StepSignature)
}
