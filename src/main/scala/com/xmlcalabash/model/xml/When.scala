package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmNode}

import scala.collection.mutable

class When(override val config: XMLCalabashConfig) extends ChooseBranch(config) {

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    _collection = attr(XProcConstants._collection)
    if (_collection.isDefined) {
      val coll = _collection.get
      if (List("1", "true", "yes").contains(coll)) {
        _collAvt = List("true")
      } else {
        if (List("0", "false", "no").contains(coll)) {
          _collAvt = List("false")
        } else {
          throw XProcException.xsBadTypeValue(coll, "literal boolean", location)
        }
      }
    }

    if (attributes.contains(XProcConstants._test)) {
      test = attr(XProcConstants._test).get
    } else {
      throw XProcException.xsMissingRequiredAttribute(XProcConstants._test, location)
    }

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeBindingsExplicit(): Unit = {
    super.makeBindingsExplicit()

    val env = environment()

    val bindings = mutable.HashSet.empty[QName]
    bindings ++= staticContext.findVariableRefsInAvt(_collAvt)

    for (ref <- bindings) {
      val binding = env.variable(ref)
      if (binding.isEmpty) {
        throw new RuntimeException("Reference to undefined variable")
      }
      if (!binding.get.static) {
        val pipe = new NamePipe(config, ref, binding.get.tumble_id, binding.get)
        addChild(pipe)
      }
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startWhen(tumble_id, stepName, _test)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endWhen()
  }

  override def toString: String = {
    s"p:when $stepName"
  }
}
