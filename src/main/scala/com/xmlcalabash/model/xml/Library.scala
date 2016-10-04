package com.xmlcalabash.model.xml

import com.xmlcalabash.core.{XProcConstants, XProcEngine}
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.{QName, XdmNode}

/**
  * Created by ndw on 9/30/16.
  */
class Library(context: Option[XdmNode]) extends Artifact(context) {
  private var _version: Option[String] = None
  private var _xpathVersion: Option[String] = None
  private var _psviRequired: Option[Boolean] = None
  private var _content: Option[List[Artifact]] = None

  def version = _version
  def xpathVersion = _xpathVersion
  def psviRequired = _psviRequired
  def content = _content

  def version_=(value: Option[String]): Unit = {
    _version = value
  }

  def xpathVersion_=(value: Option[String]): Unit = {
    _xpathVersion = value
  }

  def psviRequired_=(value: Option[Boolean]): Unit = {
    _psviRequired = value
  }

  def addDeclaredStep(step: StepDeclaration): Unit = {
    if (step.stepType.isEmpty) {
      staticError("Steps declared in a library must have a type")
    }

    if (_content.isDefined) {
      _content.get.foreach {
        _ match {
          case stepDecl: StepDeclaration =>
            if (stepDecl.stepType.get == step.stepType.get) {
              staticError("Multiple declarations of stepe type: " + step.stepType.get.toString)
            }
        }
      }

      _content = Some(_content.get ::: List(step))
    } else {
      _content = Some(List(step))
    }
  }

  def addImport(anImport: Import): Unit = {
    if (_content.isDefined) {
      _content = Some(_content.get ::: List(anImport))
    } else {
      _content = Some(List(anImport))
    }
  }

  def dump(engine: XProcEngine): XdmNode = {
    val tree = new TreeWriter(engine)
    tree.startDocument(null)
    dump(tree)
    tree.getResult
  }

  override def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px("library"))
    if (_content.isDefined) {
      _content.get.foreach { _.dump(tree) }
    }
    tree.addEndElement()
  }
}


