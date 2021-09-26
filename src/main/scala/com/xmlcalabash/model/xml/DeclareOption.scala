package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.runtime.params.XPathBindingParams
import com.xmlcalabash.runtime.{ExprParams, XMLCalabashRuntime, XProcXPathExpression, XProcXPathValue}
import com.xmlcalabash.util.XProcVarValue
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{QName, SaxonApiException, SequenceType}

class DeclareOption(override val config: XMLCalabashConfig) extends NameBinding(config) {
  private var _runtimeBindings = Map.empty[QName,XProcVarValue]

  override def toString: String = {
    s"p:option $name $tumble_id"
  }

  override def declaredType: SequenceType = {
    if (_as.isEmpty) {
      _declaredType = staticContext.parseSequenceType(Some("Q{http://www.w3.org/2001/XMLSchema}string"))
    } else {
      _declaredType = _as
    }
    _declaredType.get
  }

  override protected[model] def validateStructure(): Unit = {
    for (child <- allChildren) {
      child match {
        case _: WithInput => ()
        case _: NamePipe => ()
        case _ =>
          throw new RuntimeException(s"Invalid content in $this")
      }
    }
  }

  def runtimeBindings(bindings: Map[QName, XProcVarValue]): Unit = {
    _runtimeBindings = bindings
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    if (static) {
      // Statics have already been evaluated, they don't appear in the graph
      return
    }

    val container = this.parent.get
    val cnode = container._graphNode.get.asInstanceOf[ContainerStart]
    if (cnode.parent.nonEmpty) {
      throw new ModelException(ExceptionCode.INTERNAL, "Don't know what to do about opts here", location)
    }

    val params = new XPathBindingParams(collection)
    val init = if (_runtimeBindings.contains(name)) {
      new XProcXPathValue(staticContext, _runtimeBindings(name), as, _allowedValues, params)
    } else {
      val extext = _select.getOrElse("()")
      val expr = new XProcXPathExpression(staticContext, extext, as, _allowedValues, params)

      // Let's see if we think this is a syntactically valid expression (see bug #506 and test ab-option-024)
      // I have reservations about this...
      try {
        config.expressionEvaluator.value(expr, List(), Map(), Some(params))
      } catch {
        case ex: XProcException =>
          if (ex.code == XProcException.xs0107) {
            throw ex
          }
        case _: Throwable => ()
      }

      expr
    }

    val node = parent.asInstanceOf[ContainerStart].addOption(_name.getClarkName, init, xpathBindingParams(), topLevel = true)

    for (np <- _dependentNameBindings) {
      val binding = findInScopeOption(np.name)
      if (binding.isEmpty) {
        throw XProcException.xsNoBindingInExpression(np.name, location)
      }
      np.patchNode(binding.get.graphNode.get)
      np.graphEdges(runtime, node)
    }

    _graphNode = Some(node)
  }


  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startOption(tumble_id, tumble_id, name)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endOption()
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parNode: Node): Unit = {
    // nop; edges are *to* options
  }
}

