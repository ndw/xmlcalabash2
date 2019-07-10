package com.xmlcalabash.model.xml

import com.jafpl.graph.Node
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{XMLCalabashRuntime, XProcXPathExpression}
import net.sf.saxon.s9api.{QName, SequenceType, XdmAtomicValue, XdmNode}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class NameBinding(override val config: XMLCalabashConfig) extends Artifact(config) {
  protected var _name = new QName("UNINITIALIZED")
  protected var _declaredType = Option.empty[SequenceType]
  protected var _as = Option.empty[SequenceType]
  protected var _values = Option.empty[String]
  protected var _static = Option.empty[Boolean]
  protected var _required = Option.empty[Boolean]
  protected var _select = Option.empty[String]
  protected var _avt = Option.empty[String]
  protected var _visibility = Option.empty[String]
  protected var _allowedValues = Option.empty[List[XdmAtomicValue]]
  protected var _staticValue = Option.empty[XdmValueItemMessage]
  protected var collection = false

  protected var _href = Option.empty[String]
  protected var _pipe = Option.empty[String]

  def name: QName = _name
  def as: Option[SequenceType] = _as
  protected[model] def as_=(seq: SequenceType): Unit = {
    _as = Some(seq)
  }
  def declaredType: SequenceType = {
    if (_declaredType.isEmpty) {
      _declaredType = staticContext.parseSequenceType(Some("xs:string"))
    }
    _declaredType.get
  }
  protected[model] def declaredType_=(decltype: SequenceType): Unit = {
    _declaredType = Some(decltype)
  }
  def values: Option[String] = _values
  def required: Boolean = _required.getOrElse(false)
  def select: Option[String] = _select
  protected[model] def select_=(select: String): Unit = {
    _select = Some(select)
  }
  def avt: Option[String] = _avt
  protected[model] def avt_=(expr: String): Unit = {
    if (select.isDefined) {
      throw new RuntimeException("Cannot define AVT if select is present")
    }
    _avt = Some(expr)
  }
  def static: Boolean = _static.getOrElse(false)
  def visibility: String = _visibility.getOrElse("public")
  def allowedValues: Option[List[XdmAtomicValue]] = _allowedValues

  protected[xmlcalabash] def staticValue: Option[XdmValueItemMessage] = _staticValue
  protected[model] def staticValue_=(value: XdmValueItemMessage): Unit = {
    _staticValue = Some(value)
  }

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    if (attributes.contains(XProcConstants._name)) {
      _name = staticContext.parseQName(attr(XProcConstants._name).get)
    } else {
      throw XProcException.xsMissingRequiredAttribute(XProcConstants._name, location)
    }

    _as = staticContext.parseSequenceType(attr(XProcConstants._as))
    _values = attr(XProcConstants._values)
    _static = staticContext.parseBoolean(attr(XProcConstants._static))
    _required = staticContext.parseBoolean(attr(XProcConstants._required))
    _select = attr(XProcConstants._select)
    _visibility = attr(XProcConstants._visibility)
    collection = staticContext.parseBoolean(attr(XProcConstants._collection)).getOrElse(false)

    _href = attr(XProcConstants._href)
    _pipe = attr(XProcConstants._pipe)

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(environment: Environment): Unit = {
    if (_href.isDefined && _pipe.isDefined) {
      throw XProcException.xsPipeAndHref(location)
    }

    if (_href.isDefined && allChildren.nonEmpty) {
      throw XProcException.xsHrefAndOtherSources(location)
    }

    if (_pipe.isDefined && allChildren.nonEmpty) {
      throw XProcException.xsPipeAndOtherSources(location)
    }

    if (_href.isDefined) {
      val doc = new Document(config)
      doc.href = _href.get
      addChild(doc)
    }

    if (_pipe.isDefined) {
      var port = Option.empty[String]
      var step = Option.empty[String]
      if (_pipe.get.contains("@")) {
        val re = "(.*)@(.*)".r
        _pipe.get match {
          case re(pname, sname) =>
            if (pname != "") {
              port = Some(pname)
            }
            step = Some(sname)
        }
      } else {
        if (_pipe.get.trim() != "") {
          port = _pipe
        }
      }

      val pipe = new Pipe(config)
      if (step.isDefined) {
        pipe.step = step.get
      }
      if (port.isDefined) {
        pipe.port = port.get
      }
      addChild(pipe)
    }

    for (child <- allChildren) {
      child.makeStructureExplicit(environment)
    }
  }

  override protected[model] def makeBindingsExplicit(env: Environment, drp: Option[Port]): Unit = {
    super.makeBindingsExplicit(env, drp)

    val ds = ListBuffer.empty[DataSource]
    for (child <- allChildren) {
      child match {
        case source: DataSource =>
          if (static) {
            throw new RuntimeException("Statics cannot rely on the context")
          }
          ds += source
        case _ =>
          throw new RuntimeException(s"Unexpected child: $child")
      }
    }

    if (ds.isEmpty) {
      if (drp.isDefined && !static) {
        val winput = new WithInput(config)
        winput.port = "source"
        addChild(winput)
        val pipe = new Pipe(config)
        pipe.port = drp.get.port
        pipe.step = drp.get.step.stepName
        pipe.link = drp.get
        winput.addChild(pipe)
      }
    } else {
      removeChildren()
      val winput = new WithInput(config)
      winput.port = "source"
      addChild(winput)
      for (source <- ds) {
        winput.addChild(source)
      }
    }

    if (static) {
      val expr = new XProcXPathExpression(staticContext, select.get)
      val msg = config.expressionEvaluator.value(expr, List(), inScopeStatics, None)
      staticValue = msg
    }

    if (_select.isDefined) {
      val bindings = mutable.HashSet.empty[QName]
      bindings ++= staticContext.findVariableRefsInString(_select.get)
      if (bindings.isEmpty) {
        val depends = staticContext.dependsOnContextString(_select.get)
        // FIXME: if depends is false, we can resolve this statically
      } else {
        for (ref <- bindings) {
          val binding = env.variable(ref)
          if (binding.isEmpty) {
            throw new RuntimeException("Reference to undefined variable")
          }
          if (!binding.get.static) {
            val pipe = new NamePipe(config, ref, binding.get.tumble_id, binding.get)
            addChild(pipe)
          }
        }
      }
    }
  }

  override protected[model] def validateStructure(): Unit = {
    var hasEmpty = false
    var hasNonEmpty = false

    for (child <- allChildren) {
      child.validateStructure()
      child match {
        case winput: WithInput => Unit
        case npipe: NamePipe => Unit
        case _ =>
          throw new RuntimeException(s"Invalid content in $this")
      }
    }

    if (hasEmpty && hasNonEmpty) {
      throw XProcException.xsNoSiblingsOnEmpty(location)
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node) {
    for (child <- allChildren) {
      child.graphEdges(runtime, _graphNode.get)
    }
  }
}
