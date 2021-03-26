package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Node}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.messages.{XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.ValueParser
import com.xmlcalabash.runtime._
import com.xmlcalabash.runtime.params.XPathBindingParams
import com.xmlcalabash.util.xc.ElaboratedPipeline
import com.xmlcalabash.util.{S9Api, TypeUtils}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmMap, XdmValue}

import scala.collection.mutable

class WithOption(override val config: XMLCalabashConfig) extends NameBinding(config) {
  val typeUtils = new TypeUtils(config)

  def this(config: XMLCalabashConfig, name: QName) = {
    this(config)
    _name = name
  }

  override protected[model] def makeBindingsExplicit(): Unit = {
    super.makeBindingsExplicit()

    val env = environment()

    val bindings = mutable.HashSet.empty[QName]
    if (_avt.isDefined) {
      val avt = staticContext.parseAvt(_avt.get)
      bindings ++= staticContext.findVariableRefsInAvt(avt)
      if (bindings.isEmpty && parent.get.isInstanceOf[AtomicStep]) {
        val depends = staticContext.dependsOnContextAvt(avt)
        if (!depends) {
          val expr = new XProcVtExpression(staticContext, _avt.get, true)
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
        //val depends = collection || staticContext.dependsOnContextString(_select.get)
        val depends = true
        if (!depends) {
          val checkas = if (qnameKeys) {
            None
          } else {
            as
          }
          val opts = new XPathBindingParams(Map.empty[QName, XdmValue], collection)
          val expr = new XProcXPathExpression(staticContext, _select.get, checkas, allowedValues, opts)
          val msg = config.expressionEvaluator.value(expr, List(), inScopeStatics, Some(opts))

          if (qnameKeys) {
            msg.item match {
              case map: XdmMap =>
                staticValue = new XdmValueItemMessage(S9Api.forceQNameKeys(map.getUnderlyingValue, staticContext), XProcMetadata.XML, staticContext)
              case _ => throw new RuntimeException("qname map type didn't evaluate to a map")
            }
          } else {
            staticValue = new XdmValueItemMessage(msg.item, XProcMetadata.XML, staticContext)
          }
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

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
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

    val params = new XPathBindingParams(statics.toMap, collection)
    val init = if (_avt.isDefined) {
      val expr = staticContext.parseAvt(_avt.get)
      new XProcVtExpression(staticContext, expr, true)
    } else {
      val params = new XPathBindingParams(collection)
      new XProcXPathExpression(staticContext, _select.getOrElse("()"), as, _allowedValues, params)
    }
    val node = cnode.addOption(_name.getClarkName, init, params)
    _graphNode = Some(node)
  }


  override def graphEdges(runtime: XMLCalabashRuntime, parNode: Node): Unit = {
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
    if (staticValue.isEmpty) {
      xml.startWithOption(tumble_id, tumble_id, name)
      for (child <- rawChildren) {
        child.xdump(xml)
      }
      xml.endWithOption()
    }
  }

  override def toString: String = {
    if (tumble_id.startsWith("!syn")) {
      s"p:with-option $name"
    } else {
      s"p:with-option $name $tumble_id"
    }
  }

}
