package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Graph, Location, Node}
import com.xmlcalabash.exceptions.ModelException
import com.xmlcalabash.model.util.{ParserConfiguration, UniqueId}
import com.xmlcalabash.model.xml.containers.{Choose, ForEach, Group, Try, Viewport}
import com.xmlcalabash.model.xml.datasource.{Data, Document, Empty, Inline, Pipe}
import com.xmlcalabash.runtime.NodeLocation
import net.sf.saxon.s9api.{Axis, QName, XdmNode}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Artifact(val config: ParserConfiguration, val parent: Option[Artifact]) {
  protected[xml] var id: Long = UniqueId.nextId
  protected[xml] val properties = mutable.HashMap.empty[QName,String]
  protected[xml] val children: mutable.ListBuffer[Artifact] = mutable.ListBuffer.empty[Artifact]
  protected[xml] var inScopeNS = Map.empty[String,String]
  protected[xml] val subpiplineClasses = List(classOf[ForEach], classOf[Viewport],
    classOf[Choose], classOf[Group], classOf[Try], classOf[AtomicStep], classOf[Variable])
  protected[xml] val dataSourceClasses = List(classOf[Empty], classOf[Pipe],
    classOf[Document], classOf[Inline], classOf[Data])
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
    val aiter = node.axisIterator(Axis.ATTRIBUTE)
    while (aiter.hasNext) {
      val attr = aiter.next().asInstanceOf[XdmNode]
      properties.put(attr.getNodeName, attr.getStringValue)
    }

    // Parse attributes
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
        throw new ModelException("badboolean", s"Not a boolean: $value", location)
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
          throw new ModelException("badns", s"No in-scope namespace for prefix: $prefix", location)
        }
      } else {
        Some(new QName("", name.get))
      }
    } else {
      None
    }
  }

  def lexicalPrefixes(value: Option[String]): Set[String] = {
    if (value.isDefined) {
      val set = mutable.HashSet.empty[String]
      val prefixes = value.get.split("\\s+")
      for (prefix <- prefixes) {
        if (inScopeNS.contains(prefix)) {
          set += prefix
        } else {
          throw new ModelException("badns", s"No in-scope namespace for prefix: $prefix", location)
        }
      }
      set.toSet
    } else {
      Set()
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

  def defaultReadablePort(): Option[IOPort] = {
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
      throw new ModelException("missed", "Graph navigation error???", location)
    }
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
    for (child <- children) {
      child.makeGraph(graph, parent)
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
