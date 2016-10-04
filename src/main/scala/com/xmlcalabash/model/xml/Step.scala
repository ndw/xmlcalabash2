package com.xmlcalabash.model.xml

import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/1/16.
  */
abstract class Step(context: Option[XdmNode]) extends Artifact(context) {
  private var _stepName: Option[String] = None
  private var _psviRequired: Option[Boolean] = None
  private var _xpathVersion: Option[String] = None
  private var _version: Option[String] = None

  stepName = Some("!" + id.toString())

  def stepName = _stepName
  def psviRequired = _psviRequired
  def xpathVersion = _xpathVersion
  def version = _version

  def stepName_=(value: Option[String]): Unit = {
    // FIXME: check that it's an NCName
    _stepName = value
  }

  def psviRequired_=(value: Option[Boolean]): Unit = {
    _psviRequired = value
  }

  def xpathVersion_=(value: Option[String]): Unit = {
    _xpathVersion = value
  }

  def version_=(value: Option[String]): Unit = {
    _version = value
  }
}
