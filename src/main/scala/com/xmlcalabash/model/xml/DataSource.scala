package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.runtime.ImplParams
import net.sf.saxon.s9api.QName

class DataSource(override val config: XMLCalabashConfig) extends Artifact(config) {

  override protected[model] def validateStructure(): Unit = {
    for (child <- allChildren) {
      child match {
        case _: Pipe => ()
        case _ =>
          throw new RuntimeException(s"Invalid content in $this")
      }
    }
  }

  protected[model] def normalizeDataSourceToPipes(stepType: QName, params: ImplParams): Unit = {
    if (parent.isDefined && parent.get.parent.isDefined) {
      parent.get.parent.get match {
        case binding: NameBinding =>
          if (binding.static) {
            return // this all has to be resolved statically
          }
        case _ => ()
      }
    }

    val env = environment()
    val loader = new AtomicStep(config, params, this)
    loader.stepType = stepType

    if (allChildren.nonEmpty) {
      val winput = new WithInput(config)
      winput.port = "source"
      loader.addChild(winput)
      for (child <- allChildren) {
        winput.addChild(child)
      }
      removeChildren()
    }

    val woutput = new WithOutput(config)
    woutput.port = "result"
    loader.addChild(woutput)

    val step = if (parent.get.isInstanceOf[WithInput]) {
      val p = parent.get.parent.get
      // Hack for inline inside with-option
      if (p.isInstanceOf[WithOption]) {
        p.parent.get
      } else {
        p
      }
    } else {
      parent.get
    }

    var stepTarget: Artifact = step
    var container = step.parent.get.asInstanceOf[Container]
    if (container.isInstanceOf[Choose]) {
      stepTarget = container
      container = container.parent.get.asInstanceOf[Container]
    }

    container.addChild(loader, stepTarget)

    val pipe = new Pipe(config)
    pipe.step = loader.stepName
    pipe.port = "result"
    pipe.link = woutput

    parent.get.replaceChild(pipe, this)
  }
}
