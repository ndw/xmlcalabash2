package com.xmlcalabash.model.xml.containers

import com.jafpl.graph.{Binding, ChooseStart, ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.{Artifact, DeclareStep, Documentation, OptionDecl, PipeInfo, Variable}
import com.xmlcalabash.runtime.{ExpressionContext, XProcExpression, XProcXPathExpression}

class When(override val config: XMLCalabash,
           override val parent: Option[Artifact]) extends Container(config, parent) {
  private var testExpr: XProcExpression = _

  override def validate(): Boolean = {
    var valid = true

    _name = attributes.get(XProcConstants._name)
    if (_name.isDefined) {
      label = _name.get
    } else {
      label = "when"
    }

    val test = attributes.get(XProcConstants._test)
    if (test.isDefined) {
      val context = new ExpressionContext(baseURI, inScopeNS, location)
      testExpr = new XProcXPathExpression(context, test.get)
    } else {
      throw new ModelException(ExceptionCode.TESTREQUIRED, List.empty[String], location)
    }

    for (key <- List(XProcConstants._name, XProcConstants._test)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    for (child <- relevantChildren()) {
      valid = valid && child.validate()
    }

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val node = parent match {
      case choose: ChooseStart =>
        choose.addWhen(testExpr, name)
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "When parent isn't a choose???", location)
    }
    graphNode = Some(node)

    for (child <- children) {
      child.makeGraph(graph, node)
    }
  }

  override def makeEdges(graph: Graph, parentNode: Node) {
    val drp = parent.get.defaultReadablePort
    if (drp.isDefined) {
      val gnode = if (drp.get.graphNode.isDefined) {
        drp.get.graphNode.get
      } else {
        drp.get.parent.get.graphNode.get
      }
      graph.addEdge(gnode, drp.get.port.get, graphNode.get, "condition")
    }

    for (output <- outputPorts) {
      graph.addEdge(graphNode.get, output, parentNode, output)
    }

    for (child <- children) {
      child match {
        case doc: Documentation => Unit
        case pipe: PipeInfo => Unit
        case _ =>
          child.makeEdges(graph, graphNode.get)
      }
    }

    val variableRefs = findVariableRefs(testExpr)
    for (ref <- variableRefs) {
      val bind = findBinding(ref)
      if (bind.isEmpty) {
        throw new ModelException(ExceptionCode.NOBINDING, ref.toString, location)
      }

      bind.get match {
        case declStep: DeclareStep =>
          var optDecl = Option.empty[OptionDecl]
          for (child <- declStep.children) {
            child match {
              case opt: OptionDecl =>
                if (opt.optionName == ref) {
                  optDecl = Some(opt)
                }
              case _ => Unit
            }
          }
          if (optDecl.isEmpty) {
            throw new ModelException(ExceptionCode.NOBINDING, ref.toString, location)
          }
          graph.addBindingEdge(optDecl.get.graphNode.get.asInstanceOf[Binding], graphNode.get)
        case varDecl: Variable =>
          graph.addBindingEdge(varDecl.graphNode.get.asInstanceOf[Binding], graphNode.get)
        case _ =>
          throw new ModelException(ExceptionCode.INTERNAL, s"Unexpected $ref binding: ${bind.get}", location)
      }
    }
  }
}

