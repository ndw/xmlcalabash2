package com.xmlcalabash.drivers

import java.net.URI

import com.jafpl.exceptions.{JafplException, JafplLoopDetected}
import com.jafpl.graph.{Binding, Node}
import com.xmlcalabash.config.{DocumentRequest, XMLCalabashConfig}
import com.xmlcalabash.exceptions.{ModelException, ParseException, StepException, XProcException}
import com.xmlcalabash.model.xml.Parser
import com.xmlcalabash.runtime.{PrintingConsumer, XProcMetadata}
import com.xmlcalabash.util.{ArgBundle, URIUtils}
import net.sf.saxon.s9api.QName

object Main extends App {
  type OptionMap = Map[Symbol, Any]

  val config = XMLCalabashConfig.newInstance()
  val options = new ArgBundle(config, args.toList)

  var errored = false
  try {
    config.debugOptions.injectables = options.injectables

    val parser = new Parser(config)
    val pipeline = parser.loadDeclareStep(new URI(options.pipeline))

    //pipeline.dump()

    config.debugOptions.dumpGraph(pipeline)

    val runtime = pipeline.runtime()

    if (config.debugOptions.norun) {
      System.exit(0)
    }

    for (port <- options.inputs.keySet) {
      for (filename <- options.inputs(port)) {
        val href = URIUtils.cwdAsURI.resolve(filename)
        val request = new DocumentRequest(href)
        val response = config.documentManager.parse(request)
        runtime.input(port, response.value, new XProcMetadata(response.contentType))
      }
    }

    for (port <- runtime.outputs) {
      val serOpt = runtime.serializationOptions(port)
      val pc = if (options.outputs.contains(port)) {
        new PrintingConsumer(runtime, serOpt, options.outputs(port))
      } else {
        new PrintingConsumer(runtime, serOpt)
      }
      runtime.output(port, pc)
    }

    runtime.run()
  } catch {
    case ex: Exception =>
      errored = true

      if (config.debugOptions.debug) {
        ex.printStackTrace()
      }

      val mappedex = XProcException.mapPipelineException(ex)

      mappedex match {
        case model: ModelException =>
          println(model)
        case parse: ParseException =>
          println(parse)
        case jafpl: JafplLoopDetected =>
          println("Loop detected:")
          var first = true
          var pnode = Option.empty[Node]
          for (node <- jafpl.nodes) {
            val prefix = if (first) {
              "An output from"
            } else {
              "flows to"
            }

            node match {
              case bind: Binding =>
                val bnode = bind.bindingFor
                pnode = Some(bnode)

                val bnodeLabel = bnode.userLabel.getOrElse(bnode.label)
                val bindLabel = bind.userLabel.getOrElse(bind.label)

                println(s"\tis the context item for $bnodeLabel/$bindLabel; $bnodeLabel")
                if (bnode.location.isDefined) {
                  println(s"\t\t${bnode.location.get}")
                }
              case _ =>
                if (pnode.isEmpty || pnode.get != node) {
                  println(s"\t$prefix ${node.userLabel.getOrElse(node.label)}")
                  if (node.location.isDefined) {
                    println(s"\t\t${node.location.get}")
                  }
                  pnode = Some(node)
                }
            }

            first = false
          }
        case jafpl: JafplException =>
          println(jafpl)
        case xproc: XProcException =>
          val code = xproc.code
          val message = if (xproc.message.isDefined) {
            xproc.message.get
          } else {
            code match {
              case qname: QName =>
                config.errorExplanation.message(qname, xproc.variant, xproc.details)
              case _ =>
                s"Configuration error: code ($code) is not a QName"
            }
          }
          if (xproc.location.isDefined) {
            println(s"ERROR ${xproc.location.get} $code $message")
          } else {
            println(s"ERROR $code $message")
          }

          if (options.verbose && code.isInstanceOf[QName]) {
            val explanation = config.errorExplanation.explanation(code.asInstanceOf[QName], xproc.variant)
            if (explanation != "") {
              println(explanation)
            }
          }
        case step: StepException =>
          val code = step.code
          val message = if (step.message.isDefined) {
            step.message.get
          } else {
            config.errorExplanation.message(code, 1)
          }
          println(s"ERROR $code $message")

          if (options.verbose) {
            val explanation = config.errorExplanation.explanation(code, 1)
            if (explanation != "") {
              println(explanation)
            }
          }

        case _ =>
          println("Caught unexpected error: " + ex)
      }
  }

  if (errored) {
    System.exit(1)
  }
}
