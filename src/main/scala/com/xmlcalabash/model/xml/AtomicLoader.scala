package com.xmlcalabash.model.xml

import com.jafpl.graph.Node
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.runtime.{ImplParams, XMLCalabashRuntime}

class AtomicLoader(override val config: XMLCalabashConfig, params: Option[ImplParams]) extends AtomicStep(config, params) {

  def this(config: XMLCalabashConfig, params: ImplParams, context: Artifact) = {
    this(config, Some(params))
    _inScopeStatics = context._inScopeStatics
    _inScopeDynamics = context._inScopeDynamics
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    super.graphEdges(runtime, parent)

    for ((name, np) <- _inScopeDynamics) {
      val binding = findInScopeOption(name)
      val pipe = new NamePipe(config, name, this.tumble_id, binding)
      pipe.graphEdges(runtime, _graphNode.get)
    }
  }
}
