package com.xmlcalabash.model.xml

import com.sun.prism.image.ViewPort
import com.xmlcalabash.model.xml.containers.{Choose, ForEach, Group, Try, Viewport}
import com.xmlcalabash.model.xml.datasource.{Data, Document, Empty, Inline, Pipe}
import net.sf.saxon.s9api.{Axis, QName, XdmNode}

import scala.collection.mutable

class Artifact(val parent: Option[Artifact]) {
  protected[xml] val properties = mutable.HashMap.empty[QName,String]
  protected[xml] val children = mutable.ListBuffer.empty[Artifact]
  protected[xml] var inScopeNS = Map.empty[String,String]
  protected[xml] val subpiplineClasses = List(classOf[ForEach], classOf[Viewport],
    classOf[Choose], classOf[Group], classOf[Try], classOf[AtomicStep])
  protected[xml] val dataSourceClasses = List(classOf[Empty], classOf[Pipe],
    classOf[Document], classOf[Inline], classOf[Data])

  protected[xml] def parse(node: XdmNode): Unit = {
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
      case input: Input =>
        children.insert(0, child)
      case output: Output =>
        children.insert(0, child)
      case _ =>
        children += child
    }
  }

  def lexicalBoolean(value: Option[String]): Option[Boolean] = {
    if (value.isDefined) {
      if (value.get == "true" || value.get == "false") {
        Some(value.get == "true")
      } else {
        throw new XmlPipelineException("badboolean", s"Not a boolean: $value")
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
          throw new XmlPipelineException("badns", s"No in-scope namespace for prefix: $prefix")
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
          throw new XmlPipelineException("badns", s"No in-scope namespace for prefix: $prefix")
        }
      }
      set.toSet
    } else {
      Set()
    }
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

  def validate(): Boolean = {
    println(s"ERROR: $this doesn't override validate")
    false
  }

  def makeBindingsExplicit(): Boolean = {
    println(s"ERROR: $this doesn't override makeBindingsExplicit")
    false
  }

}
