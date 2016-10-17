package com.xmlcalabash.runtime

import java.net.URI

import com.jafpl.items.GenericItem
import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.{DefaultStep, StepController}
import com.xmlcalabash.core.{XProcConstants, XProcEngine, XProcException}
import com.xmlcalabash.items.{StringItem, XPathDataModelItem}
import net.sf.saxon.s9api._

/**
  * Created by ndw on 10/3/16.
  */
class XPathExpression(engine: XProcEngine, nsbindings: Map[String,String], expr: String, option: Option[QName]) extends DefaultStep {
  private var vars = collection.mutable.HashMap.empty[QName, XdmValue]
  private var context: Option[GenericItem] = None
  private var assignedValue: Option[XdmValue] = None

  def this(engine: XProcEngine, nsbindings: Map[String,String], expr: String) {
    this(engine, nsbindings, expr, None)
  }

  override def setup(ctrl: StepController,
                     inputs: List[String],
                     outputs: List[String]): Unit = {
    super.setup(ctrl, inputs, outputs)
    if (outputPorts.isEmpty || (outputPorts.size == 1 && outputPorts.contains("result"))) {
      // nop
    } else {
      throw new XProcException("Misconfigured XPath expression step")
    }
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    super.receive(port, msg)
    if (port == "source") {
      if (context.isDefined) {
        throw new XProcException("Sequence on xpath context")
      } else {
        context = Some(msg.item)
      }
    } else {
      var name = parseClarkName(port)
      if (option.isDefined && (name == option.get)) {
        msg.item match {
          case item: StringItem => assignedValue = Some(getUntypedAtomic(engine.processor, item.get))
          case item: XPathDataModelItem => assignedValue = Some(item.value)
          case _ => throw new XProcException("Unexpected value passed to expression")
        }
      }

      msg.item match {
        case item: StringItem => vars.put(parseClarkName(port), getUntypedAtomic(engine.processor, item.get))
        case item: XPathDataModelItem => vars.put(parseClarkName(port), item.value)
        case _ => throw new XProcException("Unexpected value passed to expression")
      }
    }
  }

  override def run(): Unit = {
    if (assignedValue.isDefined) {
      controller.send("result", new XPathDataModelItem(assignedValue.get, nsbindings))
      return
    }

    val manager = new ContentTypeManager()

    val xcomp = engine.processor.newXPathCompiler()
    xcomp.setLanguageVersion("3.1")

    val baseURI = URI.create("http://example.com/")

    if (baseURI.toASCIIString != "") {
      xcomp.setBaseURI(baseURI)
    }

    for (name <- vars.keySet) {
      xcomp.declareVariable(name)
    }

    for (prefix <- nsbindings.keySet) {
      xcomp.declareNamespace(prefix, nsbindings(prefix))
    }

    val xexec = xcomp.compile(expr)
    val selector = xexec.load()

    for (name <- vars.keySet) {
      val value = vars(name)
      selector.setVariable(name, vars(name))
    }

    if (context.isDefined) {
      val node = manager.convertToXdmNode(context.get)
      if (node.isDefined) {
        selector.setContextItem(node.get)
      }
    }

    val iterator = selector.iterator()
    while (iterator.hasNext) {
      val next = iterator.next()
      controller.send("result", new XPathDataModelItem(next, nsbindings))
    }
  }

  def parseClarkName(name: String): QName = {
    if (name.startsWith("{}")) {
      new QName("", name.substring(2))
    } else {
      val pos = name.indexOf("}")
      val uri = name.substring(1, pos-1)
      val localName = name.substring(pos+1)
      new QName(uri, localName)
    }
  }

  def getUntypedAtomic(proc: Processor, str: String): XdmValue = {
    val itf = new ItemTypeFactory(proc)
    val untypedAtomic = itf.getAtomicType(XProcConstants.xs_untypedAtomic)
    new XdmAtomicValue(str, untypedAtomic)
  }
}
