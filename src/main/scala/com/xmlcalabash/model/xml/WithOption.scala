package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.messages.{XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.ValueParser
import com.xmlcalabash.runtime.params.XPathBindingParams
import com.xmlcalabash.runtime.{ExprParams, XMLCalabashRuntime, XProcMetadata, XProcVtExpression, XProcXPathExpression}
import com.xmlcalabash.util.TypeUtils
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmValue}

import scala.collection.mutable

class WithOption(override val config: XMLCalabashConfig) extends NameBinding(config) {
  val typeUtils = new TypeUtils(config)

  def this(config: XMLCalabashConfig, name: QName) {
    this(config)
    _name = name
  }

  override protected[model] def makeBindingsExplicit(env: Environment, drp: Option[Port]): Unit = {
    super.makeBindingsExplicit(env, drp)

    val bindings = mutable.HashSet.empty[QName]
    if (_avt.isDefined) {
      val avt = staticContext.parseAvt(_avt.get)
      bindings ++= staticContext.findVariableRefsInAvt(avt)
      if (bindings.isEmpty && parent.get.isInstanceOf[AtomicStep]) {
        val depends = staticContext.dependsOnContextAvt(avt)
        if (!depends) {
          val expr = new XProcVtExpression(staticContext, _avt.get)
          var msg = config.expressionEvaluator.value(expr, List(), inScopeStatics, None)
          // Ok, now we have a string value
          val avalue = msg.item.getUnderlyingValue.getStringValue
          var tvalue = typeUtils.castAtomicAs(XdmAtomicValue.makeAtomicValue(avalue), Some(declaredType), staticContext)
          if (as.isDefined) {
            tvalue = typeUtils.castAtomicAs(tvalue, as, staticContext)
          }
          msg = new XdmValueItemMessage(tvalue, XProcMetadata.XML, staticContext)
          staticValue = msg
        }
      }
    } else if (_select.isDefined) {
      bindings ++= staticContext.findVariableRefsInString(_select.get)
      if (bindings.isEmpty && parent.get.isInstanceOf[AtomicStep]) {
        val depends = staticContext.dependsOnContextString(_select.get)
        if (!depends) {
          val expr = new XProcXPathExpression(staticContext, _select.get)
          var msg = config.expressionEvaluator.value(expr, List(), inScopeStatics, None)
          // Ok, now we have a string value
          val avalue = msg.item.getUnderlyingValue.getStringValue
          var tvalue = typeUtils.castAtomicAs(XdmAtomicValue.makeAtomicValue(avalue), Some(declaredType), staticContext)
          if (as.isDefined) {
            tvalue = typeUtils.castAtomicAs(tvalue, as, staticContext)
          }
          msg = new XdmValueItemMessage(tvalue, XProcMetadata.XML, staticContext)
          staticValue = msg
        }
      }
    } else {
      throw new RuntimeException("With option has neither AVT nor select?")
    }

    var nonStaticBindings = false
    for (ref <- bindings) {
      val binding = env.variable(ref)
      if (binding.isEmpty) {
        throw new RuntimeException("Reference to undefined variable")
      }
      nonStaticBindings = nonStaticBindings || !binding.get.static
    }

    if (nonStaticBindings) {
      var winput = firstWithInput
      if (winput.isEmpty) {
        val input = new WithInput(config)
        input.port = "source"
        addChild(input)
        winput = Some(input)
      }
      for (ref <- bindings) {
        val binding = env.variable(ref).get
        if (!binding.static) {
          val pipe = new NamePipe(config, ref, binding.tumble_id, binding)
          winput.get.addChild(pipe)
        }
      }
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node) {
    if (staticValue.isDefined) {
      return
    }

    val container = this.parent.get.parent.get
    val cnode = container._graphNode.get.asInstanceOf[ContainerStart]
    val statics = mutable.HashMap.empty[QName, XdmValue]
    for ((name,smsg) <- inScopeStatics) {
      val qname = ValueParser.parseClarkName(name)
      smsg match {
        case msg: XdmNodeItemMessage =>
          statics.put(qname, msg.item)
        case msg: XdmValueItemMessage =>
          statics.put(qname, msg.item)
      }
    }

    val params = new XPathBindingParams(statics.toMap)
    val init = if (_avt.isDefined) {
      val expr = staticContext.parseAvt(_avt.get)
      new XProcVtExpression(staticContext, expr)
    } else {
      val params = new ExprParams(collection)
      new XProcXPathExpression(staticContext, _select.getOrElse("()"), as, _allowedValues, params)
    }
    val node = runtime.graph.addOption(_name.getClarkName, init, params)
    _graphNode = Some(node)
  }


  override def graphEdges(runtime: XMLCalabashRuntime, parNode: Node) {
    if (staticValue.isDefined) {
      return
    }

    val toNode = parNode
    val fromPort = "result"
    val fromNode = _graphNode.get
    val toPort = "#bindings"
    runtime.graph.addEdge(fromNode, fromPort, toNode, toPort)

    for (child <- allChildren) {
      child.graphEdges(runtime, _graphNode.get)
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startWithOption(tumble_id, tumble_id, name)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endWithOption()
  }

  override def toString: String = {
    if (tumble_id.startsWith("!syn")) {
      s"p:with-option $name"
    } else {
      s"p:with-option $name $tumble_id"
    }
  }

}
