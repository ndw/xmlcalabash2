package com.xmlcalabash.model.xml

import com.jafpl.graph.CompoundStart
import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.bindings.Pipe
import net.sf.saxon.s9api.{QName, XdmNode}

/**
  * Created by ndw on 10/4/16.
  */
class CompoundStep(node: Option[XdmNode], parent: Option[Artifact]) extends Step(node, parent) {
  var compoundStart: CompoundStart = _

  override def makeInputsOutputsExplicit(): Unit = {
    var icount = 0
    var ocount = 0
    var input: Option[Input] = None
    var output: Option[Output] = None
    for (child <- _children) {
      child match {
        case i: Input =>
          icount += 1
          if (icount == 1) {
            input = Some(i)
          } else {
            input = None
          }
        case o: Output =>
          ocount += 1
          if (ocount == 1) {
            output = Some(o)
          } else {
            output = None
          }
        case _ => Unit
      }
    }

    if (input.isDefined) {
      val prim = input.get.property(XProcConstants._primary)
      if (prim.isEmpty) {
        input.get.setProperty(XProcConstants._primary, "true")
      }
    }

    if (output.isDefined) {
      val prim = output.get.property(XProcConstants._primary)
      if (prim.isEmpty) {
        output.get.setProperty(XProcConstants._primary, "true")
      }
    }

    for (child <- _children) {
      child.makeInputsOutputsExplicit()
    }

    // Figure out of the last step in the pipeline has a primary output port
    output = None
    val last = children.last
    for (child <- last.children) {
      child match {
        case o: Output =>
          if (o.primary) {
            output = Some(o)
          }
        case _ => Unit
      }
    }

    // If the last step has a primary output port...
    if (output.isDefined) {
      // If the the compound step has no output, add a default output port
      if (ocount == 0 && output.isDefined) {
        val newChildren = collection.mutable.ListBuffer.empty[Artifact]
        val newOutput = new Output(None, Some(this))
        newOutput.setProperty(XProcConstants._port, "result")
        newOutput.setProperty(XProcConstants._primary, "true")

        newChildren += newOutput
        newChildren ++= _children
        _children.clear()
        _children ++= newChildren
      }

      // Make the primary output port of the last step the default readable port
      // for all outputs. (This will only matter if the binding gets defaulted.)
      for (child <- children) {
        child match {
          case o: Output =>
            o._drp = output
          case _ => Unit
        }
      }
    }
  }

  override def addDefaultReadablePort(port: Option[InputOrOutput]): Unit = {
    _drp = port
    for (child <- _children) {
      child match {
        case input: Input => input.addDefaultReadablePort(port)
        case opt: OptionDecl => opt.addDefaultReadablePort(port)
        case ctx: XPathContext => ctx.addDefaultReadablePort(port)
        case _ => Unit
      }
    }

    var drp: Option[InputOrOutput] = primaryInputPort
    for (child <- _children) {
      child match {
        case step: AtomicStep =>
          step.addDefaultReadablePort(drp)
          drp = step.primaryOutputPort
        case step: CompoundStep =>
          step.addDefaultReadablePort(drp)
          drp = step.primaryOutputPort
        case variable: Variable => variable.addDefaultReadablePort(drp)
        case _ => Unit
      }
    }
  }

  override def findInScopeStep(name: String): Option[Step] = {
    var step: Option[Step] = None

    val myName = property(XProcConstants._name)
    if (myName.isDefined && myName.get.value == name) {
      step = Some(this)
    } else {
      for (child <- _children) {
        child match {
          case s: Step =>
            val stepName = s.property(XProcConstants._name)
            if (stepName.isDefined && stepName.get.value == name) {
              step = Some(s)
            }
          case _ => Unit
        }
      }
    }

    if (step.isEmpty && parent.isDefined) {
      step = parent.get.asInstanceOf[Step].findInScopeStep(name)
    }

    step
  }

  override def findNameDecl(varname: QName, ref: Artifact): Option[NameDecl] = {
    var found: Option[NameDecl] = None

    for (child <- children) {
      if (child == ref) {
        if (found.isDefined) {
          return found
        } else {
          if (parent.isDefined) {
            return parent.get.findNameDecl(varname, this)
          } else {
            return None
          }
        }
      }

      child match {
        case v: NameDecl =>
          if (v.declaredName.get == varname) {
            found = Some(v)
          }
        case _ => Unit
      }
    }

    found
  }

  override def hoistOptions(): Unit = {
    val newch = collection.mutable.ListBuffer.empty[Artifact]

    for (child <- children) {
      child match {
        case atomic: AtomicStep =>
          val hoisted = atomic.extractOptions()
          for (opt <- hoisted) {
            val optName = opt.property(XProcConstants._name).get.value

            val expr = new ExprStep(this)
            expr.addProperty(XProcConstants._name, optName)
            expr.addProperty(XProcConstants._select, opt.property(XProcConstants._select).get.value)
            val out = new Output(None, Some(expr))
            out.addProperty(XProcConstants._port, "result")
            expr.addChild(out)

            val in = new Input(None, Some(opt))
            in.addProperty(XProcConstants._port, "$" + optName)
            val pipe = new Pipe(None, Some(in))
            pipe._port = Some(out)
            in.addChild(pipe)
            atomic.addChild(in)

            newch += expr
          }
          newch += child
        case _ =>
          newch += child
      }
    }

    _children.clear()
    _children ++= newch
  }
}
