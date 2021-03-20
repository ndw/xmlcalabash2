package com.xmlcalabash.util.xc

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable

class ElaboratedPipeline(config: XMLCalabashConfig) {
  private val builder = new SaxonTreeBuilder(config)
  private val openStack = mutable.Stack.empty[QName]
  private val inLibrary = false

  def startPipeline(tumble_id: String, stepName: String, stepType: Option[QName], version: Double): Unit = {
    startPipeline(tumble_id, stepName, stepType, version, None, None, None, None)
  }

  def startPipeline(tumble_id: String, stepName: String,
                    stepType: Option[QName],
                    version: Double,
                    psviRequired: Option[Boolean],
                    xpathVersion: Option[Double],
                    excludePrefixes: Option[String],
                    visibility: Option[String]): Unit = {
    if (!inLibrary) {
      builder.startDocument(None)
    }
    builder.addStartElement(XProcConstants.p_declare_step)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, stepName)
    builder.addNamespace("cx", XProcConstants.ns_cx)
    oattr(XProcConstants._type, stepType)
    oattr(XProcConstants._version, Some(version))
    oattr(XProcConstants._xpath_version, xpathVersion)
    oattr(XProcConstants._psvi_required, psviRequired)
    oattr(XProcConstants._exclude_inline_prefixes, excludePrefixes)
    oattr(XProcConstants._visibility, visibility)
    builder.startContent()
    openStack.push(XProcConstants.p_declare_step)
  }

  def endPipeline(): Option[XdmNode] = {
    builder.addEndElement()
    openStack.pop()

    if (inLibrary) {
      None
    } else {
      builder.endDocument()
      Some(builder.result)
    }
  }

  private def end(): Unit = {
    builder.addEndElement()
    openStack.pop()
  }

  def startInput(tumble_id: String, name: String, port: String): Unit = {
    val element = XProcConstants.p_input
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, name)
    builder.addAttribute(XProcConstants._port, port)
    builder.startContent()
    openStack.push(element)
  }

  def endInput(): Unit = {
    end()
  }

  def startOutput(tumble_id: String, name: String, port: String): Unit = {
    val element = XProcConstants.p_output
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, name)
    builder.addAttribute(XProcConstants._port, port)
    builder.startContent()
    openStack.push(element)
  }

  def endOutput(): Unit = {
    end()
  }

  def startWithOutput(tumble_id: String, name: String, port: String, sequence: Boolean): Unit = {
    val element = XProcConstants.p_with_output
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, name)
    builder.addAttribute(XProcConstants._port, port)
    builder.addAttribute(XProcConstants._sequence, sequence.toString)
    builder.startContent()
    openStack.push(element)
  }

  def endWithOutput(): Unit = {
    end()
  }

  def startWithInput(tumble_id: String, name: String, port: String): Unit = {
    val element = XProcConstants.p_with_input
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, name)
    builder.addAttribute(XProcConstants._port, port)
    builder.startContent()
    openStack.push(element)
  }

  def endWithInput(): Unit = {
    end()
  }

  def startPipe(tumble_id: String, step: String, port: String): Unit = {
    val element = XProcConstants.p_pipe
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._step, step)
    builder.addAttribute(XProcConstants._port, port)
    builder.startContent()
    openStack.push(element)
  }

  def endPipe(): Unit = {
    end()
  }

  def startNamePipe(tumble_id: String, step: String): Unit = {
    val element = new QName("p", XProcConstants.ns_p, "name-pipe")
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._step, step)
    builder.startContent()
    openStack.push(element)
  }

  def endNamePipe(): Unit = {
    end()
  }

  def startDocument(tumble_id: String, href: String): Unit = {
    val element = XProcConstants.p_inline
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._href, href)
    builder.startContent()
    openStack.push(element)
  }

  def endDocument(): Unit = {
    end()
  }

  def startInline(tumble_id: String, root: Option[QName]): Unit = {
    val element = XProcConstants.p_inline
    builder.addStartElement(element)
    tid(tumble_id)
    if (root.isDefined) {
      builder.addAttribute(new QName("root"), root.get.toString)
    }
    builder.startContent()
    openStack.push(element)
  }

  def endInline(): Unit = {
    end()
  }

  def startDocumentation(tumble_id: String, root: Option[QName]): Unit = {
    val element = XProcConstants.p_documentation
    builder.addStartElement(element)
    tid(tumble_id)
    if (root.isDefined) {
      builder.addAttribute(new QName("root"), root.get.toString)
    }
    builder.startContent()
    openStack.push(element)
  }

  def endDocumentation(): Unit = {
    end()
  }

  def startPipeInfo(tumble_id: String, root: Option[QName]): Unit = {
    val element = XProcConstants.p_pipeinfo
    builder.addStartElement(element)
    tid(tumble_id)
    if (root.isDefined) {
      builder.addAttribute(new QName("root"), root.get.toString)
    }
    builder.startContent()
    openStack.push(element)
  }

  def endPipeInfo(): Unit = {
    end()
  }

  def startVariable(tumble_id: String, stepName: String, name: QName): Unit = {
    val element = XProcConstants.p_variable
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, stepName)
    builder.addAttribute(new QName("varname"), name.toString)
    builder.startContent()
    openStack.push(element)
  }

  def endWithVariable(): Unit = {
    end()
  }

  def startOption(tumble_id: String, name: String, optname: QName): Unit = {
    val element = XProcConstants.p_option
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, name)
    builder.addAttribute(new QName("optname"), optname.toString)
    builder.startContent()
    openStack.push(element)
  }

  def endOption(): Unit = {
    end()
  }

  def startWithOption(tumble_id: String, name: String, optname: QName): Unit = {
    val element = XProcConstants.p_with_option
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, name)
    builder.addAttribute(new QName("optname"), optname.toString)
    builder.startContent()
    openStack.push(element)
  }

  def endWithOption(): Unit = {
    end()
  }

  def startAtomic(tumble_id: String,
                  stepName: String,
                  stepType: QName): Unit = {
    val element = stepType
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, stepName)
    builder.startContent()
    openStack.push(element)
  }

  def endAtomic(): Unit = {
    end()
  }

  def startViewport(tumble_id: String,
                    stepName: String,
                    pattern: String): Unit = {
    val element = XProcConstants.p_viewport
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, stepName)
    builder.addAttribute(XProcConstants._match, pattern)
    builder.startContent()
    openStack.push(element)
  }

  def endViewport(): Unit = {
    end()
  }

  def startForEach(tumble_id: String,
                   stepName: String): Unit = {
    val element = XProcConstants.p_for_each
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, stepName)
    builder.startContent()
    openStack.push(element)
  }

  def endForEach(): Unit = {
    end()
  }

  def startForUntil(tumble_id: String,
                    stepName: String): Unit = {
    val element = XProcConstants.cx_until
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, stepName)
    builder.startContent()
    openStack.push(element)
  }

  def endForUntil(): Unit = {
    end()
  }

  def startForWhile(tumble_id: String,
                    stepName: String): Unit = {
    val element = XProcConstants.cx_while
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, stepName)
    builder.startContent()
    openStack.push(element)
  }

  def endForWhile(): Unit = {
    end()
  }

  def startForLoop(tumble_id: String,
                    stepName: String): Unit = {
    val element = XProcConstants.cx_loop
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, stepName)
    builder.startContent()
    openStack.push(element)
  }

  def endForLoop(): Unit = {
    end()
  }

  def startChoose(tumble_id: String,
                  stepName: String): Unit = {
    val element = XProcConstants.p_choose
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, stepName)
    builder.startContent()
    openStack.push(element)
  }

  def endChoose(): Unit = {
    end()
  }

  def startWhen(tumble_id: String,
                stepName: String,
                test: String): Unit = {
    val element = XProcConstants.p_when
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, stepName)
    builder.addAttribute(XProcConstants._test, test)
    builder.startContent()
    openStack.push(element)
  }

  def endWhen(): Unit = {
    end()
  }

  def startGroup(tumble_id: String,
                   stepName: String): Unit = {
    val element = XProcConstants.p_group
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, stepName)
    builder.startContent()
    openStack.push(element)
  }

  def endGroup(): Unit = {
    end()
  }

  def startTry(tumble_id: String,
               stepName: String): Unit = {
    val element = XProcConstants.p_try
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, stepName)
    builder.startContent()
    openStack.push(element)
  }

  def endTry(): Unit = {
    end()
  }

  def startCatch(tumble_id: String,
                 stepName: String,
                 codes: Option[String]): Unit = {
    val element = XProcConstants.p_catch
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, stepName)
    oattr(XProcConstants._code, Some(codes))
    builder.startContent()
    openStack.push(element)
  }

  def endCatch(): Unit = {
    end()
  }

  def startFinally(tumble_id: String,
                    stepName: String): Unit = {
    val element = XProcConstants.p_catch
    builder.addStartElement(element)
    tid(tumble_id)
    builder.addAttribute(XProcConstants._name, stepName)
    builder.startContent()
    openStack.push(element)
  }

  def endFinally(): Unit = {
    end()
  }

  private def oattr(name: QName, value: Option[Any]): Unit = {
    if (value.isDefined) {
      builder.addAttribute(name, value.get.toString)
    }
  }

  private def tid(id: String): Unit = {
    val xid = s"ID_${id.substring(1)}"
    builder.addAttribute(XProcConstants.xml_id, xid)
  }
}
