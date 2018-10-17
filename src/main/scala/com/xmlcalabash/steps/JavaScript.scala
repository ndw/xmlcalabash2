package com.xmlcalabash.steps

import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.PortCardinality
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{ExpressionContext, ImplParams, StaticContext, XMLCalabashRuntime, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.TypeUtils
import javax.script.ScriptEngineManager
import net.sf.saxon.s9api.{QName, XdmNode, XdmValue}

class JavaScript extends DefaultXmlStep {
  private var typeUtils: TypeUtils = _
  private var parameters = Map.empty[QName, XdmValue]
  private val factory = new ScriptEngineManager()
  private val engine = factory.getEngineByName("nashorn")
  private var script = ""

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("script" -> PortCardinality.EXACTLY_ONE),
    Map("script" -> List("text/plain")))

  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULT

  override def initialize(config: RuntimeConfiguration, params: Option[ImplParams]): Unit = {
    super.initialize(config, params)
    typeUtils = new TypeUtils(config.asInstanceOf[XMLCalabashRuntime])
  }

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    if (port == "script") {
      item match {
        case node: XdmNode =>
          script = node.getStringValue
        case _ => Unit
      }
    }
  }

  override def receiveBinding(variable: QName, value: XdmValue, context: ExpressionContext): Unit = {
    if (variable == XProcConstants._parameters) {
      parameters = ValueParser.parseParameters(value, context.nsBindings, context.location)
    }
  }

  override def run(context: StaticContext): Unit = {
    for (key <- parameters.keySet) {
      engine.put(key.getLocalName, parameters(key))
    }

    val js = engine.eval(script)
    val xml = TypeUtils.castAsXml(js)

    consumer.get.receive("result", xml, new XProcMetadata(TypeUtils.mediaType(xml)))
  }
}
