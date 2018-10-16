package com.xmlcalabash.model.xml

import java.net.URI

import com.jafpl.graph.{ContainerStart, Graph, Location, Node}
import com.xmlcalabash.config.{StepSignature, XMLCalabashConfig}
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.{UniqueId, ValueParser, XProcConstants}
import com.xmlcalabash.model.xml.containers.{Choose, Container, ForEach, Group, Try, Viewport, WithDocument, WithProperties}
import com.xmlcalabash.model.xml.datasource.{Document, Empty, Inline, Pipe}
import com.xmlcalabash.runtime.injection.{XProcPortInjectable, XProcStepInjectable}
import com.xmlcalabash.runtime.{ExpressionContext, ImplParams, NodeLocation, XProcExpression, XProcVtExpression, XmlStep}
import com.xmlcalabash.util.S9Api
import net.sf.saxon.expr.parser.XPathParser
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmNodeKind}
import net.sf.saxon.sxpath.IndependentContext
import net.sf.saxon.trans.XPathException
import net.sf.saxon.value.SequenceType
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class Artifact(val config: XMLCalabashConfig, val parent: Option[Artifact]) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected[xml] var id: Long = UniqueId.nextId
  protected[xml] val attributes = mutable.HashMap.empty[QName, String]
  protected[xml] val children: mutable.ListBuffer[Artifact] = mutable.ListBuffer.empty[Artifact]
  protected[xml] var inScopeNS = Map.empty[String,String]
  protected[xml] val subpiplineClasses = List(classOf[ForEach], classOf[Viewport],
    classOf[Choose], classOf[Group], classOf[Try], classOf[AtomicStep], classOf[Variable],
    classOf[WithProperties], classOf[WithDocument])
  protected[xml] val dataSourceClasses = List(classOf[Empty], classOf[Pipe],
    classOf[Document], classOf[Inline])
  protected[xml] var _label = Option.empty[String]
  protected[xml] var _graphNode = Option.empty[Node]
  protected[xml] var dump_attr = Option.empty[xml.MetaData]
  protected[xml] var _location = Option.empty[Location]
  protected[xml] var _baseURI = Option.empty[URI]
  protected[xml] var _nodeName: QName = XProcConstants.UNKNOWN
  protected[xml] var _xmlId = Option.empty[String]
  protected[xml] val _inputInjectables: ListBuffer[XProcPortInjectable] = ListBuffer.empty[XProcPortInjectable]
  protected[xml] val _outputInjectables: ListBuffer[XProcPortInjectable] = ListBuffer.empty[XProcPortInjectable]
  protected[xml] val _stepInjectables: ListBuffer[XProcStepInjectable] = ListBuffer.empty[XProcStepInjectable]

  def label: Option[String] = _label
  protected[xml] def label_=(label: String): Unit = {
    _label = Some(label)
  }

  def name: String = _label.getOrElse("_" + id)

  def location: Option[Location] = _location
  protected[xml] def location_=(loc: Location): Unit = {
    _location = Some(loc)
  }

  def baseURI: Option[URI] = _baseURI
  def xmlId: Option[String] = _xmlId
  def nodeName: QName = _nodeName

  override def toString: String = {
    "{step " + name + " " + super.toString + "}"
  }

  protected[xml] def inputInjectables: List[XProcPortInjectable] = _inputInjectables.toList
  protected[xml] def addInputInjectable(injectable: XProcPortInjectable): Unit = {
    _inputInjectables += injectable
  }

  protected[xml] def outputInjectables: List[XProcPortInjectable] = _outputInjectables.toList
  protected[xml] def addOutputInjectable(injectable: XProcPortInjectable): Unit = {
    _outputInjectables += injectable
  }

  protected[xml] def stepInjectables: List[XProcStepInjectable] = _stepInjectables.toList
  protected[xml] def addStepInjectable(injectable: XProcStepInjectable): Unit = {
    _stepInjectables += injectable
  }

  def stepDeclaration(stepType: QName): Option[DeclareStep] = {
    if (parent.isDefined) {
      parent.get.stepDeclaration(stepType)
    } else {
      None
    }
  }

  def stepSignature(stepType: QName): Option[StepSignature] = {
    if (parent.isDefined) {
      parent.get.stepSignature(stepType)
    } else {
      None
    }
  }

  protected[xml] def setLocation(node: XdmNode): Unit = {
    // What if the preceding node is a location PI?
    var pi = Option.empty[XdmNode]
    var found = false
    val iter = node.axisIterator(Axis.PRECEDING_SIBLING)
    while (iter.hasNext) {
      val pnode = iter.next().asInstanceOf[XdmNode]
      if (!found) {
        pnode.getNodeKind match {
          case XdmNodeKind.TEXT =>
            if (pnode.getStringValue.trim != "") {
              found = true
            }
          case XdmNodeKind.PROCESSING_INSTRUCTION =>
            if (pnode.getNodeName.getLocalName == "_xmlcalabash") {
              pi = Some(pnode)
              found = true
            }
          case _ => found = true
        }
      }
    }

    if (found && pi.isDefined) {
      println("OVERRIDE WITH " + pi)
    }

    _location = Some(new NodeLocation(node))
  }

  protected[xml] def parse(node: XdmNode): Unit = {
    _location = Some(new NodeLocation(node))
    _baseURI = Some(node.getBaseURI)
    _nodeName = node.getNodeName
    val ns = S9Api.inScopeNamespaces(node)

    // Parse attributes
    val aiter = node.axisIterator(Axis.ATTRIBUTE)
    while (aiter.hasNext) {
      val attr = aiter.next().asInstanceOf[XdmNode]
      attr.getNodeName match {
        case XProcConstants.xml_base => _baseURI = Some(new URI(attr.getStringValue))
        case XProcConstants.xml_id => _xmlId = Some(attr.getStringValue)
        case _ => attributes.put(attr.getNodeName, attr.getStringValue)
      }
    }

    var same = parent.isDefined
    if (parent.isDefined) {
      for (prefix <- parent.get.inScopeNS.keySet) {
        same = same && (ns.contains(prefix) && (ns(prefix) == parent.get.inScopeNS(prefix)))
      }
      for (prefix <- ns.keySet) {
        same = same && (parent.get.inScopeNS.contains(prefix) && (ns(prefix) == parent.get.inScopeNS(prefix)))
      }
    }
    if (same) {
      inScopeNS = parent.get.inScopeNS
    } else {
      inScopeNS = ns.toMap
    }
  }

  protected[xml] def parsePipeAttribute(pipe: String): Unit = {
    if (pipe.trim() == "") {
      addChild(new Pipe(config, this, None, None))
      return
    }

    for (spec <- pipe.split("\\s+")) {
      val pos = spec.indexOf("@")
      if (pos >= 0) {
        val port = spec.substring(0, pos)
        val step = spec.substring(pos + 1)
        val pipe = if (step == "") {
          new Pipe(config, this, None, Some(port))
        } else {
          if (port =="") {
            new Pipe(config, this, Some(step), None)
          } else {
            new Pipe(config, this, Some(step), Some(port))
          }
        }
        addChild(pipe)
      } else {
        val pipe = new Pipe(config, this, None, Some(spec))
        addChild(pipe)
      }
    }
  }

  protected[xml] def addChild(child: Artifact): Unit = {
    children += child
  }

  protected[xml] def insertChildBefore(node: Artifact, insert: Artifact): Unit = {
    var pos = -1
    var found = false
    while (!found && pos < children.length) {
      pos += 1
      found = (children(pos) == node)
    }
    if (!found) {
      throw XProcException.xiChildNotFound(node.location)
    }
    children.insert(pos, insert)

  }

  protected[xml] def insertChildAfter(node: Artifact, insert: Artifact): Unit = {
    var pos = -1
    var found = false
    while (!found && pos < children.length) {
      pos += 1
      found = (children(pos) == node)
    }
    if (!found) {
      throw XProcException.xiChildNotFound(node.location)
    }
    children.insert(pos+1, insert)
  }

  protected def patchPipe(fromName: String, fromPort: List[String], patchName: String, patchPort: String): Unit = {
    for (child <- children) {
      child.patchPipe(fromName, fromPort, patchName, patchPort)
    }
  }

  def lexicalBoolean(value: Option[String]): Option[Boolean] = {
    ValueParser.parseBoolean(value, location, true)
  }

  def lexicalQName(name: Option[String]): Option[QName] = {
    ValueParser.parseQName(name, inScopeNS, location)
  }

  def lexicalPrefixes(value: Option[String]): Map[String,String] = {
    if (value.isDefined) {
      val set = mutable.HashMap.empty[String,String]
      val prefixes = value.get.split("\\s+")
      if (prefixes.contains("#default")) {
        if (inScopeNS.contains("")) {
          set.put("", inScopeNS(""))
        } else {
          throw new ModelException(ExceptionCode.NOPREFIX, "", location)
        }
      }
      if (prefixes.contains("#all")) {
        for ((pfx,uri) <- inScopeNS) {
          set.put(pfx,uri)
        }
      }
      for (prefix <- prefixes) {
        if ((prefix == "#all") || (prefix == "#default")) {
          // nop
        } else {
          if (inScopeNS.contains(prefix)) {
            set.put(prefix, inScopeNS(prefix))
          } else {
            throw new ModelException(ExceptionCode.NOPREFIX, prefix, location)
          }
        }
      }
      set.toMap
    } else {
      Map.empty[String,String]
    }
  }

  def lexicalAvt(name: String, value: String): XProcVtExpression = {
    val avt = ValueParser.parseAvt(value)
    if (avt.isDefined) {
      val context = new ExpressionContext(_baseURI, inScopeNS, _location)
      new XProcVtExpression(context, avt.get, true)
    } else {
      throw new ModelException(ExceptionCode.BADAVT, List(name, value), location)
    }
  }

  def lexicalVariables(expr: String): Set[QName] = {
    ValueParser.findVariableRefsInString(config, inScopeNS, expr, location)
  }

  def staticValue(vref: QName): Option[XPathItemMessage] = {
    var msg: Option[XPathItemMessage] = None
    if (parent.isDefined) {
      var found = false
      for (child <- parent.get.relevantChildren) {
        found = found || (child == this)
        //println(s"$child: $found")
        if (!found) {
          child match {
            case vdef: Variable =>
              if (vdef.static && vdef.variableName == vref) {
                msg = vdef.staticValueMessage
              }
            case odef: OptionDecl =>
              if (odef.static && odef.optionName == vref) {
                msg = odef.staticValueMessage
              }
            case _ => Unit
          }
        }
      }
      if (msg.isDefined) {
        msg
      } else {
        parent.get.staticValue(vref)
      }
    } else {
      None
    }
  }

  def sequenceType(seqType: Option[String]): Option[SequenceType] = {
    if (seqType.isDefined) {
      try {
        val parser = new XPathParser
        parser.setLanguage(XPathParser.SEQUENCE_TYPE, 31)
        val ic = new IndependentContext(config.processor.getUnderlyingConfiguration)
        for ((prefix, uri) <- inScopeNS) {
          ic.declareNamespace(prefix, uri)
        }
        Some(parser.parseSequenceType(seqType.get, ic))
      } catch {
        case xpe: XPathException =>
          throw XProcException.xsInvalidSequenceType(seqType.get, xpe.getMessage, location)
        case t: Throwable =>
          throw t
      }
    } else {
      None
    }
  }

  def relevantChildren: List[Artifact] = {
    relevantChildren(children.toList)
  }

  def relevantChildren(children: List[Artifact]): List[Artifact] = {
    // FIXME: use a proper scala map
    val list = ListBuffer.empty[Artifact]
    for (node <- children) {
      node match {
        case e: PipeInfo => Unit
        case e: Documentation => Unit
        case _ => list += node
      }
    }
    list.toList
  }

  def inputPorts: List[String] = {
    val list = mutable.ListBuffer.empty[String]
    for (child <- children) {
      child match {
        case input: Input =>
          if (input.port.isDefined) {
            list += input.port.get
          }
        case _ => Unit
      }
    }
    list.toList
  }

  def inputs: List[Input] = {
    val list = mutable.ListBuffer.empty[Input]
    for (child <- children) {
      child match {
        case input: Input =>
          list += input
        case _ => Unit
      }
    }
    list.toList
  }

  def input(port: String): Option[Input] = {
    for (child <- children) {
      child match {
        case input: Input =>
          if (input.port.isDefined && input.port.get == port) {
            return Some(input)
          }
        case _ => Unit
      }
    }
    None
  }

  def primaryInput: Option[Input] = {
    for (child <- children) {
      child match {
        case input: Input =>
          if (input.primary.getOrElse(false)) {
            return Some(input)
          }
        case _ => Unit
      }
    }
    None
  }

  def outputPorts: List[String] = {
    val list = mutable.ListBuffer.empty[String]
    for (child <- children) {
      child match {
        case output: Output =>
          if (output.port.isDefined) {
            list += output.port.get
          }
        case _ => Unit
      }
    }
    list.toList
  }

  def outputs: List[Output] = {
    val list = mutable.ListBuffer.empty[Output]
    for (child <- children) {
      child match {
        case output: Output =>
          list += output
        case _ => Unit
      }
    }
    list.toList
  }

  def output(port: String): Option[Output] = {
    for (child <- children) {
      child match {
        case output: Output =>
          if (output.port.isDefined && output.port.get == port) {
            return Some(output)
          }
        case _ => Unit
      }
    }
    None
  }

  def primaryOutput: Option[Output] = {
    for (child <- children) {
      child match {
        case output: Output =>
          if (output.primary.getOrElse(false)) {
            return Some(output)
          }
        case _ => Unit
      }
    }
    None
  }

  def bindings: List[QName] = {
    val list = mutable.ListBuffer.empty[QName]
    for (child <- children) {
      child match {
        case opt: OptionDecl =>
          list += opt.optionName
        case _ => Unit
      }
    }
    list.toList
  }


  def bindingDeclaration(qname: QName): Option[OptionDecl] = {
    for (child <- children) {
      child match {
        case opt: OptionDecl =>
          if (opt.optionName == qname) {
            return Some(opt)
          }
        case _ => Unit
      }
    }
    None
  }

  def defaultLabel: String = {
    if (parent.isEmpty) {
      "!1"
    } else {
      var count = 0
      var found = false
      for (child <- parent.get.children) {
        if (!found) {
          child match {
            case step: PipelineStep => count += 1
            case _ => Unit
          }
        }
        found = found || (this == child)
      }
      parent.get.defaultLabel + "." + count
    }
  }

  def findStep(stepName: String): Option[Artifact] = {
    if (name == stepName) {
      Some(this)
    } else {
      var step = Option.empty[Artifact]
      for (child <- children) {
        if (step.isEmpty) {
          if (child.name == stepName) {
            step = Some(child)
          }
        }
      }
      if (step.isDefined) {
        step
      } else {
        if (parent.isDefined) {
          parent.get.findStep(stepName)
        } else {
          None
        }
      }
    }
  }

  def precedingSibling(): Option[Artifact] = {
    if (parent.isDefined) {
      var preceding = Option.empty[Artifact]
      for (child <- parent.get.children) {
        if (this == child) {
          return preceding
        }
        child match {
          case step: PipelineStep =>
            preceding = Some(child)
          case _ => Unit
        }
      }
      None
    } else {
      None
    }
  }

  def isAncestor(stepName: String): Boolean = {
    if (name == stepName) {
      true
    } else {
      if (parent.isDefined) {
        parent.get.isAncestor(stepName)
      } else {
        false
      }
    }
  }

  def defaultReadablePort: Option[IOPort] = {
    if (parent.isDefined) {
      val drpIsPrecedingSibling =
        this match {
          case step: PipelineStep => true
          case variable: Variable => true
          case _ => false
        }

      if (drpIsPrecedingSibling) {
        val ps = precedingSibling()
        if (ps.isDefined) {
          for (port <- ps.get.outputPorts) {
            val out = ps.get.output(port)
            if (out.get.primary.getOrElse(false)) {
              return out
            }
          }
        }
      }

      for (port <- parent.get.inputPorts) {
        val in = parent.get.input(port)
        if (in.get.primary.getOrElse(false)) {
          if (DrpRemap.map(in.get).isDefined) {
            return DrpRemap.map(in.get)
          } else {
            return in
          }
        }
      }

      None
    } else {
      None
    }
  }

  def findBinding(varname: QName): Option[Artifact] = {
    if (parent.isEmpty) {
      None
    } else {
      var binding = Option.empty[Artifact]
      for (child <- parent.get.children) {
        if ((child == this) && binding.isDefined) {
          return binding
        }

        child match {
          case varbind: Variable =>
            if (varbind.variableName == varname) {
              binding = Some(varbind)
            }
          case optdecl: OptionDecl =>
            if (optdecl.optionName == varname) {
              binding = Some(optdecl)
            }
          case art: Artifact =>
            if (art == this) {
              return parent.get.findBinding(varname)
            }
        }
      }
      throw new ModelException(ExceptionCode.INTERNAL, "Graph navigation error???", location)
    }
  }

  def findVariableRefs(expression: XProcExpression): Set[QName] = {
    ValueParser.findVariableRefs(config, expression, location)
  }

  def validate(): Boolean = {
    println(s"ERROR: $this doesn't override validate")
    false
  }

  def makePortsExplicit(): Boolean = {
    println(s"ERROR: $this doesn't override makePortsExplicit")
    false
  }

  def makePipesExplicit(): Boolean = {
    var valid = true
    for (child <- children) {
      child match {
        case pipe: Pipe =>
          if (pipe.step.isEmpty) {
            val drp = defaultReadablePort
            if (drp.isDefined) {
              pipe.step = drp.get.parent.get.name
            } else {
              throw new ModelException(ExceptionCode.NODRP, List(), location)
            }
          }
          if (pipe.port.isEmpty) {
            val step = findStep(pipe.step.get)
            if (step.isDefined) {
              step.get match {
                case c: Container =>
                  if (step.get.primaryInput.isDefined) {
                    pipe.port = step.get.primaryInput.get.port.get
                  } else {
                    throw new ModelException(ExceptionCode.NODRP, List(), location)
                  }
                case _ =>
                  if (step.get.primaryOutput.isDefined) {
                    pipe.port = step.get.primaryOutput.get.port.get
                  } else {
                    throw new ModelException(ExceptionCode.NODRP, List(), location)
                  }
              }
            } else {
              throw new ModelException(ExceptionCode.NODRP, List(), location)
            }
          }

        case _ => child.makePipesExplicit()
      }
    }
    valid
  }

  def makeBindingsExplicit(): Boolean = {
    println(s"ERROR: $this doesn't override makeBindingsExplicit")
    false
  }

  private[xml] def findPatchable(): List[Artifact] = {
    val list = ListBuffer.empty[Artifact]
    for (child <- children) {
      child match {
        case art: WithProperties =>
          list += art
        case _ => Unit
      }
      list ++= child.findPatchable()
    }
    list.toList
  }

  private[xml] def findInjectables(): List[Artifact] = {
    val list = ListBuffer.empty[Artifact]
    if (inputInjectables.nonEmpty || outputInjectables.nonEmpty || stepInjectables.nonEmpty) {
      list += this
    }

    for (child <- children) {
      list ++= child.findInjectables()
    }

    list.toList
  }

  def makeGraph(graph: Graph, parent: Node) {
    if (children.nonEmpty) {
      if (_graphNode.isDefined) {
        for (child <- children) {
          child.makeGraph(graph, _graphNode.get)
        }
      } else {
        println("cannot process children of " + this)
      }
    }
  }

  protected[xml] def graphChildren(graph: Graph, parent: ContainerStart) {
    for (child <- children) {
      child.makeGraph(graph, parent)
    }
  }

  def makeEdges(graph: Graph, parent: Node) {
    println(s"ERROR: $this doesn't override makeEdges")
  }

  protected[xml] def graphEdges(graph: Graph, parent: ContainerStart) {
    for (child <- children) {
      child match {
        case doc: Documentation => Unit
        case pipe: PipeInfo => Unit
        case _ =>
          child.makeEdges(graph, parent)
      }
    }
  }

  protected[xml] def dumpAttr(name: String, value: String): Unit = {
    val attr = new xml.UnprefixedAttribute(name, xml.Text(value), xml.Null)
    if (dump_attr.isDefined) {
      dump_attr = Some(dump_attr.get.append(attr))
    } else {
      dump_attr = Some(attr)
    }
  }

  protected[xml] def dumpAttr(name: String, value: Option[Any]): Unit = {
    if (value.isDefined) {
      dumpAttr(name, value.get.toString)
    }
  }

  protected[xml] def dumpAttr(properties: Map[QName, String]): Unit = {
    for ((prop,value) <- properties) {
      if (prop.getNamespaceURI == "") {
        dumpAttr(prop.getLocalName, value)
      } else {
        val attr = new xml.PrefixedAttribute(prop.getPrefix, prop.getLocalName, xml.Text(value), xml.Null)
        if (dump_attr.isDefined) {
          dump_attr = Some(dump_attr.get.append(attr))
        } else {
          dump_attr = Some(attr)
        }
      }
    }
  }

  protected[xml] def namespaceScope: xml.NamespaceBinding = {
    var bindings: xml.NamespaceBinding = xml.TopScope
    for ((prefix, uri) <- inScopeNS) {
      bindings = new xml.NamespaceBinding(prefix, uri, bindings)
    }
    bindings
  }

  def asXML: xml.Elem = {
    <ERROR artifact={ this.toString }/>
  }
}
