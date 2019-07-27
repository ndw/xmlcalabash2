package com.xmlcalabash.model.xml

import com.jafpl.graph.Node
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.params.SelectFilterParams
import com.xmlcalabash.runtime.{StaticContext, XMLCalabashRuntime}
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.functions.ConstantFunction.{False, True}
import net.sf.saxon.s9api.{QName, XdmNode}

class WithInput(override val config: XMLCalabashConfig) extends Port(config) {
  private var _exclude_inline_prefixes = List.empty[String]
  private var _context: StaticContext = _

  def exclude_inline_prefixes: List[String] = _exclude_inline_prefixes

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    if (attributes.contains(XProcConstants._port)) {
      _port = staticContext.parseNCName(attr(XProcConstants._port)).get
    }
    _context = new StaticContext(config, this, node)
    _select = attr(XProcConstants._select)

    _href = attr(XProcConstants._href)
    _pipe = attr(XProcConstants._pipe)

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeBindingsExplicit(): Unit = {
    super.makeBindingsExplicit()

    val env = environment()
    val drp = env.defaultReadablePort

    if (allChildren.isEmpty) {
      if (drp.isDefined) {
        val pipe = new Pipe(config)
        pipe.port = drp.get.port
        pipe.step = drp.get.step.stepName
        pipe.link = drp.get
        addChild(pipe)
      } else {
        // This is err:XS0032 unless it's a special case
        var raiseError = true

        // One of the special cases is, if this is an atomic step, and that
        // step is a user-defined pipeline and that pipeline has a default
        // input for this port, then this is not an error, the default input
        // should be used. UGH.
        parent.get match {
          case atomic: AtomicStep =>
            val sig = declaration(atomic.stepType)
            val decl = if (sig.isDefined) {
              sig.get.declaration
            } else {
              None
            }

            var default = false
            if (decl.isDefined) {
              for (input <- decl.get.children[DeclareInput]) {
                if (input.port == port) {
                  for (child <- input.allChildren) {
                    child match {
                      case inline: Inline => default = true
                      case doc: Document => default = true
                      case _ => Unit
                    }
                  }
                }
              }
            }

            if (default) {
              // This is a total hack; I really should send a magic message or something,
              // but this will do for the short term. Anyone who creates and sends a
              // <cx:use-default-input/> document by hand gets what they deserve.
              val tree = new SaxonTreeBuilder(config)
              tree.startDocument(None)
              tree.addStartElement(XProcConstants.cx_use_default_input)
              tree.startContent()
              tree.addEndElement()
              tree.endDocument()

              val inline = new Inline(config, tree.result, true)
              addChild(inline)
              raiseError = false
            }
          case _ => Unit
        }

        if (raiseError && synthetic) {
          // All the other special cases involve synthetic elements

          parent.get match {
            case choose: Choose => raiseError = false
            case when: When => raiseError = false
            case otherwise: Otherwise => raiseError = false
            case _ =>
              if (parent.get.parent.isDefined
                && parent.get.parent.get.synthetic
                && parent.get.parent.get.isInstanceOf[Otherwise]) {
                raiseError = false
              }
          }
        }

        if (raiseError) {
          if (primary) {
            throw XProcException.xsUnconnectedPrimaryInputPort(step.stepName, port, location)
          } else {
            throw XProcException.xsUnconnectedInputPort(step.stepName, port, location)
          }
        }
      }
    }
  }

  override protected[model] def addFilters(): Unit = {
    super.addFilters()
    if (select.isEmpty) {
      return
    }

    val context = staticContext.withStatics(inScopeStatics)
    val params = new SelectFilterParams(context, select.get)
    val filter = new AtomicStep(config, params)
    filter.stepType = XProcConstants.cx_select_filter

    val finput = new WithInput(config)
    finput.port = "source"
    finput.primary = true

    val foutput = new WithOutput(config)
    foutput.port = "result"
    foutput.primary = true

    filter.addChild(finput)
    filter.addChild(foutput)

    for (name <- staticContext.findVariableRefsInString(_select.get)) {
      var binding = _inScopeDynamics.get(name)
      if (binding.isDefined) {
        val npipe = new NamePipe(config, name,"???", binding.get)
        filter.addChild(npipe)
      } else {
        binding = _inScopeStatics.get(name.getClarkName)
        if (binding.isEmpty) {
          throw new RuntimeException(s"Reference to variable not in scope: $name")
        }
      }
    }

    val step = parent.get
    val container = step.parent.get.asInstanceOf[Container]
    container.addChild(filter, step)

    for (child <- allChildren) {
      child match {
        case pipe: Pipe =>
          finput.addChild(pipe)
      }
    }
    removeChildren()

    val pipe = new Pipe(config)
    pipe.step = filter.stepName
    pipe.port = "result"
    pipe.link = foutput
    addChild(pipe)
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parNode: Node) {
    for (child <- allChildren) {
      child.graphEdges(runtime, parNode)
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startWithInput(tumble_id, tumble_id, port)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endWithInput()
  }

  override def toString: String = {
    if (tumble_id.startsWith("!syn")) {
      s"p:with-input $port"
    } else {
      s"p:with-input $port $tumble_id"
    }
  }
}
