package com.xmlcalabash.model.xml

import com.xmlcalabash.core.{XProcConstants, XProcEngine}
import com.xmlcalabash.model.xml.bindings.{DocumentBinding, EmptyBinding, InlineBinding, PipeBinding}
import net.sf.saxon.s9api.{Axis, QName, XdmNode}
import com.xmlcalabash.model.xml.util.NodeUtils
import com.xmlcalabash.model.xml.util.RelevantNodes
import org.slf4j.LoggerFactory

import scala.collection.mutable.Stack

/**
  * Created by ndw on 10/1/16.
  */
class XProc11Parser(val engine: XProcEngine) {
  val logger = LoggerFactory.getLogger(this.getClass)
  val defaultReadableStepStack: Stack[Option[Step]] = Stack()

  def parse(document: XdmNode): Option[StepDeclaration] = {
    logger.debug("Parsing " + document.getBaseURI)

    val node = NodeUtils.getDocumentElement(document)
    var decl: Option[StepDeclaration] = None

    if (node.isEmpty) {
      engine.staticError(Some(document), "Cannot parse input as a pipeline")
    } else {
      if (XProcConstants.p_declare_step == node.get.getNodeName) {
        decl = readStepDeclaration(node.get)
      } else {
        engine.staticError(Some(document), "Cannot parse <%s> as a pipeline".format(node.get.getNodeName))
      }
    }

    decl
  }

  private def readStepDeclaration(node: XdmNode): Option[StepDeclaration] = {
    logger.debug("Reading: " + node.getNodeName)

    val decl = new StepDeclaration(Some(node))

    decl.version = Option(node.getAttributeValue(XProcConstants._version))

    var rest: List[XdmNode] = List()
    for (childitem <- RelevantNodes.filter(node, Axis.CHILD)) {
      val child = childitem.asInstanceOf[XdmNode]

      if (rest.nonEmpty) {
        rest = rest ::: List(child)
      } else {
        child.getNodeName match {
          case XProcConstants.p_input => readInputDeclaration(decl, child)
          case XProcConstants.p_output => readOutputDeclaration(decl, child)
          // FIXME: case XProcConstants.p_option => readOption(step, child)
          // FIXME: case XProcConstants.p_log => readLog(step, child)
          // FIXME: case XProcConstants.p_serialization => readSerialization(step, child)
          case _ => rest = rest ::: List(child)
        }
      }
    }

    // Make sure there's at most one input port declared as primary
    var input: Option[InputDeclaration] = None
    if (decl.inputs.isDefined) {
      for (in <- decl.inputs.get) {
        if (in.primary.isDefined && in.primary.get) {
          if (input.isDefined) {
            engine.staticError(Some(node), "Only one input port can be declared primary")
          } else {
            input = Some(in)
          }
        }
      }

      // If there's exactly one input and it's not explicitly secondary, then it's primary
      if (input.isEmpty && decl.inputs.get.size == 1) {
        input = Some(decl.inputs.get.head)
        if (input.get.primary.isEmpty) {
          input.get.primary = Some(true)
        } else {
          input = None
        }
      }
    }

    if (input.isDefined) {
      defaultReadableStepStack.push(Some(decl))
    } else {
      defaultReadableStepStack.push(None)
    }

    // Make sure there's at most one output port declared as primary
    var output: Option[OutputDeclaration] = None
    if (decl.outputs.isDefined) {
      for (out <- decl.outputs.get) {
        if (out.primary.isDefined && out.primary.get) {
          if (output.isDefined) {
            engine.staticError(Some(node), "Only one output port can be declared primary")
          } else {
            output = Some(out)
          }
        }
      }

      // If there's exactly one output and it's not explicitly secondary, then it's primary
      if (output.isEmpty && decl.outputs.get.size == 1) {
        output = Some(decl.outputs.get.head)
        if (output.get.primary.isEmpty) {
          output.get.primary = Some(true)
        } else {
          output = None
        }
      }
    }

    rest.foreach { readStepInstance(decl, _) }

    defaultReadableStepStack.pop()

    Some(decl)
  }

  private def readInputDeclaration(decl: StepDeclaration, node: XdmNode): Unit = {
    logger.debug("Reading: " + node.getNodeName)

    val port = Option(node.getAttributeValue(XProcConstants._port))
    if (port.isEmpty) {
      engine.staticError(Some(node), "Input declarations must have a port")
    } else {
      val input = new InputDeclaration(Some(node), port)
      input.sequence = booleanOption(node, XProcConstants._sequence)
      input.primary = booleanOption(node, XProcConstants._primary)
      val cTypes = Option(node.getAttributeValue(XProcConstants._content_types))
      if (cTypes.isDefined && !(cTypes.get.trim() == "")) {
        input.contentTypes = Some(cTypes.get.split("\\s+").toList)
      }
      input.select = Option(node.getAttributeValue(XProcConstants._select))

      var seenUnwrappedInline = false
      for (childitem <- RelevantNodes.filter(node, Axis.CHILD)) {
        val child = childitem.asInstanceOf[XdmNode]

        child.getNodeName match {
          case XProcConstants.p_empty => readEmptyBinding(input, child)
          case XProcConstants.p_document => readDocumentBinding(input, child)
          case XProcConstants.p_inline => readInlineBinding(input, child)
          case _ =>
            if (seenUnwrappedInline) {
              engine.staticError(Some(node), "Only one unwrapped inline may appear in an input declaration")
            } else {
              seenUnwrappedInline = true
              readInlineBinding(input, node)
            }
        }
      }

      decl.addInput(input)
    }
  }

  private def readOutputDeclaration(decl: StepDeclaration, node: XdmNode): Unit = {
    logger.debug("Reading: " + node.getNodeName)

    val port = Option(node.getAttributeValue(XProcConstants._port))
    if (port.isEmpty) {
      engine.staticError(Some(node), "Output declarations must have a port")
    } else {
      val output = new OutputDeclaration(Some(node), port)
      output.sequence = booleanOption(node, XProcConstants._sequence)
      output.primary = booleanOption(node, XProcConstants._primary)
      output.excludeInlinePrefixes = Option(node.getAttributeValue(XProcConstants._exclude_result_prefixes))
      for (childitem <- RelevantNodes.filter(node, Axis.CHILD)) {
        val child = childitem.asInstanceOf[XdmNode]

        child.getNodeName match {
          case XProcConstants.p_empty => readEmptyBinding(output, child)
          case XProcConstants.p_document => readDocumentBinding(output, child)
          case XProcConstants.p_inline => readInlineBinding(output, child)
          case XProcConstants.p_pipe => readPipeBinding(output, child)
          case _ => engine.staticError(Some(child), "Unexpected child of p:output")
        }
      }

      decl.addOutput(output)
    }
  }

  private def readStepInstance(decl: StepDeclaration, node: XdmNode): Unit = {
    logger.debug("Reading: " + node.getNodeName)

    var instance = new StepInstance(Some(node), node.getNodeName)
    instance.stepName = Option(node.getAttributeValue(XProcConstants._name))
    instance.defaultReadableStep = defaultReadableStepStack.top

    for (childitem <- RelevantNodes.filter(node, Axis.CHILD)) {
      val child = childitem.asInstanceOf[XdmNode]

      child.getNodeName match {
        case XProcConstants.p_input => readStepInput(instance, child)
        case _ => engine.staticError(Some(child), "Unexpected child in step: " + child.getNodeName)
      }
    }

    val stepDecl = engine.findDeclaration(node.getNodeName)
    if (stepDecl.isDefined) {
      if (!stepDecl.get.valid(instance)) {
        engine.staticError(Some(node), "Step instance does not conform to its declaration")
      } else {
        defaultReadableStepStack.pop()
        if (stepDecl.get.instanceDefaultReadablePort.isDefined) {
          defaultReadableStepStack.push(Some(instance))
        } else {
          defaultReadableStepStack.push(None)
        }
      }
    } else {
      engine.staticError(Some(node), "No declaration for " + node.getNodeName.toString)
    }

    decl.addSubpipeline(instance)
  }

  private def readStepInput(step: StepInstance, node: XdmNode): Unit = {
    logger.debug("Reading: " + node.getNodeName)

    val name = Option(node.getAttributeValue(XProcConstants._port))
    if (name.isEmpty) {
      engine.staticError(Some(node), "The port name must be specified")
    } else {
      val input = new Input(Some(node), name)

      val sugarStep = Option(node.getAttributeValue(XProcConstants._step))
      if (sugarStep.isDefined) {
        engine.staticError(Some(node), "The syntactic sugare of @step on p:input is not yet supported")
        /*
        val pipe = new PipeBinding(Some(node))
        pipe.stepName = sugarStep
        input.addBinding(pipe)
        */
      }

      var seenUnwrappedInline = false
      for (childitem <- RelevantNodes.filter(node, Axis.CHILD)) {
        val child = childitem.asInstanceOf[XdmNode]

        child.getNodeName match {
          case XProcConstants.p_inline => readInlineBinding(input, child)
          case XProcConstants.p_pipe => readPipeBinding(input, child)
          case _ =>
            if (seenUnwrappedInline) {
              engine.staticError(Some(node), "Only one unwrapped inline may appear in an input declaration")
            } else {
              seenUnwrappedInline = true
              readInlineBinding(input, node)
            }
        }
      }
      step.addInput(input)
    }
  }

  private def readInlineBinding(iodecl: IODeclaration, node: XdmNode): Unit = {
    logger.debug("Reading: " + node.getNodeName)

    var nodes: List[XdmNode] = List()
    for (childitem <- RelevantNodes.filter(node, Axis.CHILD)) {
      val child = childitem.asInstanceOf[XdmNode]
      nodes = nodes ::: List(child)
    }

    val inline = new InlineBinding(Some(node), NodeUtils.nodesToTree(engine, nodes))
    iodecl.addBinding(inline)
  }

  private def readPipeBinding(iodecl: IODeclaration, node: XdmNode): Unit = {
    logger.debug("Reading: " + node.getNodeName)

    var stepName = Option(node.getAttributeValue(XProcConstants._step))
    if (stepName.isEmpty && defaultReadableStepStack.top.isDefined) {
      stepName = defaultReadableStepStack.top.get.stepName
    }

    var portName = Option(node.getAttributeValue(XProcConstants._port))
    if (portName.isEmpty) {
      engine.staticError(Some(node), "Missing port isn't supported yet")
      portName = Some("FIXME")
    }

    val pipe = new PipeBinding(Some(node))
    pipe.stepName = stepName
    pipe.portName = portName
    iodecl.addBinding(pipe)
  }

  private def readDocumentBinding(iodecl: IODeclaration, node: XdmNode): Unit = {
    logger.debug("Reading: " + node.getNodeName)

    val href = Option(node.getAttributeValue(XProcConstants._href))
    if (href.isEmpty) {
      engine.staticError(Some(node), "p:document must have an href attribute")
    } else {
      val uri = node.getBaseURI.resolve(href.get)
      val doc = new DocumentBinding(Some(node), uri)
      iodecl.addBinding(doc)
    }
  }

  private def readEmptyBinding(iodecl: IODeclaration, node: XdmNode): Unit = {
    logger.debug("Reading: " + node.getNodeName)

    val empty = new EmptyBinding(Some(node))
    iodecl.addBinding(empty)
  }

  // ================================================================================================================

  private def booleanOption(node: XdmNode, attrName: QName): Option[Boolean] = {
    val value = Option(node.getAttributeValue(attrName))
    if (value.isEmpty) {
      None
    } else {
      if (value.get == "true") {
        Some(true)
      } else if (value.get == "false") {
        Some(false)
      } else {
        engine.staticError(Some(node), "Invalid boolean value for " + attrName.toString + ": " + value.get)
        None
      }
    }
  }
}
