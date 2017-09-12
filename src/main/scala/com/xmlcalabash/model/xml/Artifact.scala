package com.xmlcalabash.model.xml

import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.{ContainerStart, Graph, Location, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.{StringParsers, UniqueId}
import com.xmlcalabash.model.xml.containers.{Choose, ForEach, Group, Try, Viewport}
import com.xmlcalabash.model.xml.datasource.{Document, Empty, Inline, Pipe}
import com.xmlcalabash.runtime.{NodeLocation, XProcAvtExpression, XProcExpression, XProcXPathExpression}
import net.sf.saxon.s9api.{Axis, QName, XdmNode}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Artifact(val config: XMLCalabash, val parent: Option[Artifact]) {
  protected[xml] var id: Long = UniqueId.nextId
  protected[xml] val attributes = mutable.HashMap.empty[QName, String]
  protected[xml] val children: mutable.ListBuffer[Artifact] = mutable.ListBuffer.empty[Artifact]
  protected[xml] var inScopeNS = Map.empty[String,String]
  protected[xml] val subpiplineClasses = List(classOf[ForEach], classOf[Viewport],
    classOf[Choose], classOf[Group], classOf[Try], classOf[AtomicStep], classOf[Variable])
  protected[xml] val dataSourceClasses = List(classOf[Empty], classOf[Pipe],
    classOf[Document], classOf[Inline])
  protected[xml] var _label = Option.empty[String]
  protected[xml] var valid = true
  protected[xml] var graphNode = Option.empty[Node]
  protected[xml] var dump_attr = Option.empty[xml.MetaData]
  protected[xml] var _location = Option.empty[Location]

  def label: Option[String] = _label
  protected[xml] def label_=(label: String): Unit = {
    _label = Some(label)
  }

  def name: String = _label.getOrElse("_" + id)

  def location: Option[Location] = _location

  protected[xml] def parse(node: XdmNode): Unit = {
    _location = Some(new NodeLocation(node))
    // Parse namespaces
    val nsiter = node.axisIterator(Axis.NAMESPACE)
    val ns = mutable.HashMap.empty[String,String]
    while (nsiter.hasNext) {
      val attr = nsiter.next().asInstanceOf[XdmNode]
      val prefix = if (attr.getNodeName == null) {
        ""
      } else {
        attr.getNodeName.toString
      }
      val uri = attr.getStringValue
      ns.put(prefix, uri)
    }

    // Parse attributes
    val aiter = node.axisIterator(Axis.ATTRIBUTE)
    while (aiter.hasNext) {
      val attr = aiter.next().asInstanceOf[XdmNode]
      attributes.put(attr.getNodeName, attr.getStringValue)
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

  protected[xml] def addChild(child: Artifact): Unit = {
    child match {
      case ioport: IOPort =>
        var pos = 0
        var found = false
        while (pos < children.length && !found) {
          children(pos) match {
            case doc: Documentation => Unit
            case pipe: PipeInfo => Unit
            case port: IOPort => Unit
            case _ => found = true
          }
          if (!found) {
            pos += 1
          }
        }
        if (found) {
          children.insert(pos, child)
        } else {
          children += child
        }
      case _ =>
        children += child
    }
  }

  def lexicalBoolean(value: Option[String]): Option[Boolean] = {
    if (value.isDefined) {
      if (value.get == "true" || value.get == "false") {
        Some(value.get == "true")
      } else {
        throw new ModelException(ExceptionCode.BADBOOLEAN, value.get, location)
      }
    } else {
      None
    }
  }

  def lexicalQName(name: Option[String]): Option[QName] = {
    if (name.isDefined) {
      if (name.get.contains(":")) {
        val pos = name.get.indexOf(':')
        val prefix = name.get.substring(0, pos)
        val local = name.get.substring(pos+1)
        if (inScopeNS.contains(prefix)) {
          Some(new QName(prefix, inScopeNS(prefix), local))
        } else {
          throw new ModelException(ExceptionCode.NOPREFIX, prefix , location)
        }
      } else {
        Some(new QName("", name.get))
      }
    } else {
      None
    }
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

  def lexicalAvt(name: String, value: String): XProcAvtExpression = {
    val avt = StringParsers.parseAvt(value)
    if (avt.isDefined) {
      new XProcAvtExpression(inScopeNS, avt.get)
    } else {
      throw new ModelException(ExceptionCode.BADAVT, List(name, value), location)
    }
  }

  def relevantChildren(): List[Artifact] = {
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
          if (input.primary) {
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
          if (output.primary) {
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
            if (out.get.primary) {
              return out
            }
          }
        } else {
          for (port <- parent.get.inputPorts) {
            val in = parent.get.input(port)
            if (in.get.primary) {
              return in
            }
          }
        }
        None
      } else {
        None
      }
    } else {
      None
    }
  }

  def findBinding(varname: QName): Option[Artifact] = {
    if (parent.isEmpty) {
      Some(this)
    } else {
      var binding = Option.empty[Artifact]
      for (child <- parent.get.children) {
        child match {
          case varbind: Variable =>
            if (varbind.variableName == varname) {
              binding = Some(varbind)
            }
          case art: Artifact =>
            if (art == this) {
              if (binding.isDefined) {
                return binding
              } else {
                return parent.get.findBinding(varname)
              }
            }
        }
      }
      throw new ModelException(ExceptionCode.INTERNAL, "Graph navigation error???", location)
    }
  }

  def findVariableRefs(expression: XProcExpression): Set[QName] = {
    val variableRefs = mutable.HashSet.empty[QName]

    expression match {
      case expr: XProcXPathExpression =>
        val parser = config.expressionParser
        parser.parse(expr.expr)
        for (ref <- parser.variableRefs) {
          val qname = StringParsers.parseClarkName(ref)
          variableRefs += qname
        }
      case expr: XProcAvtExpression =>
        var avt = false
        for (subexpr <- expr.avt) {
          if (avt) {
            val parser = config.expressionParser
            parser.parse(subexpr)
            for (ref <- parser.variableRefs) {
              val qname = StringParsers.parseClarkName(ref)
              variableRefs += qname
            }
          }
          avt = !avt
        }
      case _ =>
        throw new PipelineException("notimpl", "unknown expression type!", location)
    }

    variableRefs.toSet
  }


  def validate(): Boolean = {
    println(s"ERROR: $this doesn't override validate")
    false
  }

  def makePortsExplicit(): Boolean = {
    println(s"ERROR: $this doesn't override makeBindingsExplicit")
    false
  }

  def makeBindingsExplicit(): Boolean = {
    println(s"ERROR: $this doesn't override makeBindingsExplicit")
    false
  }

  def makeGraph(graph: Graph, parent: Node) {
    if (children.nonEmpty) {
      if (graphNode.isDefined) {
        for (child <- children) {
          child.makeGraph(graph, graphNode.get)
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
