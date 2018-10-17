package com.xmlcalabash.config

import java.io.{File, FileOutputStream, PrintStream, PrintWriter}

import com.jafpl.graph.Graph
import com.xmlcalabash.model.xml.DeclareStep

import scala.collection.mutable.ListBuffer

class XMLCalabashDebugOptions {
  private val _injectables = ListBuffer.empty[String]
  private var _dumpXMLFilename: Option[String] = None
  private var _dumpOpenGraphFilename: Option[String] = None
  private var _dumpGraphFilename: Option[String] = None
  private var _dumpRawFilename: Option[String] = None

  def injectables: List[String] = _injectables.toList
  def injectables_=(list: List[String]): Unit = {
    _injectables.clear()
    _injectables ++= list
  }

  def dumpXmlFilename: Option[String] = _dumpXMLFilename
  def dumpXmlFilename_=(filename: String): Unit = {
    _dumpXMLFilename = Some(filename)
  }

  def dumpOpenGraphFilename: Option[String] = _dumpOpenGraphFilename
  def dumpOpenGraphFilename_=(filename: String): Unit = {
    _dumpOpenGraphFilename = Some(filename)
  }

  def dumpGraphFilename: Option[String] = _dumpGraphFilename
  def dumpGraphFilename_=(filename: String): Unit = {
    _dumpGraphFilename = Some(filename)
  }

  def dumpRawFilename: Option[String] = _dumpRawFilename
  def dumpRawFilename_=(filename: String): Unit = {
    _dumpRawFilename = Some(filename)
  }

  // ===========================================================================================

  def dumpGraph(graph: Graph): Unit = {
    if (dumpGraphFilename.isDefined) {
      val pw = new PrintWriter(new File(dumpGraphFilename.get))
      pw.write(graph.asXML.toString)
      pw.close()
    }
  }

  def dumpOpenGraph(graph: Graph): Unit = {
    if (dumpOpenGraphFilename.isDefined) {
      val pw = new PrintWriter(new File(dumpOpenGraphFilename.get))
      pw.write(graph.asXML.toString)
      pw.close()
    }
  }

  def dumpXml(pipeline: DeclareStep): Unit = {
    if (dumpXmlFilename.isDefined) {
      val pw = new PrintWriter(new File(dumpXmlFilename.get))
      pw.write(pipeline.asXML.toString)
      pw.close()
    }
  }

  def dumpRaw(graph: Graph): Unit = {
    if (dumpRawFilename.isDefined) {
      val stdout = new FileOutputStream(new File(dumpRawFilename.get))
      val psout = new PrintStream(stdout)
      Console.withOut(psout) {
        graph.dump()
      }
    }
  }
}
