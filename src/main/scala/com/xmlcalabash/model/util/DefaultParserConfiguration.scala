package com.xmlcalabash.model.util

import com.jafpl.steps.Step
import com.xmlcalabash.config.Signatures
import com.xmlcalabash.exceptions.ModelException
import com.xmlcalabash.parsers.{StepConfigBuilder, XPathParser}
import net.sf.saxon.s9api.QName
import org.slf4j.{Logger, LoggerFactory}

class DefaultParserConfiguration extends ParserConfiguration {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val _errorListener = new DefaultErrorListener()
  private val _signatures = new Signatures()

  private val builder = new StepConfigBuilder()
  private val sigs = builder.parse(getClass.getResourceAsStream("/xproc-steps.txt"))
  for (name <- sigs.stepTypes) {
    _signatures.addStep(sigs.step(name))
  }

  override def errorListener: ErrorListener = _errorListener

  override def stepSignatures: Signatures = _signatures

  override def stepImplementation(stepType: QName): Step = {
    if (!_signatures.stepTypes.contains(stepType)) {
      throw new ModelException("notype", s"Step type '$stepType' is unknown", None)
    }

    val implClass = _signatures.step(stepType).implementation
    if (implClass.isEmpty) {
      throw new ModelException("noimpl", s"Step type '$stepType' has no known implementation", None)
    }

    val klass = Class.forName(implClass.get).newInstance()
    klass match {
      case step: Step =>
        step
      case _ =>
        throw new ModelException("nostep", s"The implementation of '$stepType' is not a step", None)
    }
  }

  /** Enable trace events.
    *
    * The actors that run steps will emit log messages if the appropriate traces are enabled.
    * This method is called to determine if a particular trace is enabled.
    *
    * @param trace A trace event.
    * @return True, if that event should be considered enabled.
    */
  override def traceEnabled(trace: String): Boolean = {
    false
  }

  // FIXME: Should this be a factory, or should XPathParser be reusable?
  override def expressionParser: ExpressionParser = {
    new XPathParser(this)
  }
}
