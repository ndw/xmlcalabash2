package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.runtime.{ExprParams, XMLCalabashRuntime, XProcExpression, XProcXPathExpression, XProcXPathValue}
import com.xmlcalabash.util.XProcVarValue
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{QName, SequenceType}

class DeclareOption(override val config: XMLCalabashConfig) extends NameBinding(config) {
  private var _runtimeBindings = Map.empty[QName,XProcVarValue]

  override def toString: String = {
    s"p:option $name $tumble_id"
  }

  override def declaredType: SequenceType = {
    if (_as.isEmpty) {
      _declaredType = staticContext.parseSequenceType(Some("xs:string"))
    } else {
      _declaredType = _as
    }
    _declaredType.get
  }

  override protected[model] def validateStructure(): Unit = {
    for (child <- allChildren) {
      child match {
        case _: WithInput => Unit
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

    val params = new ExprParams(collection)
    var init = if (_runtimeBindings.contains(name)) {
      new XProcXPathValue(staticContext, _runtimeBindings(name), as, _allowedValues, params)
    } else {
      new XProcXPathExpression(staticContext, _select.getOrElse("()"), as, _allowedValues, params)
    }

    val node = runtime.graph.addOption(_name.getClarkName, init, xpathBindingParams())
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

