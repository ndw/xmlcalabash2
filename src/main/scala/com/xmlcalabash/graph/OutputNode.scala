package com.xmlcalabash.graph

import com.xmlcalabash.items.GenericItem
import com.xmlcalabash.messages.ItemMessage

/**
  * Created by ndw on 10/2/16.
  */
class OutputNode(graph: Graph, name: Option[String]) extends Node(graph, name, None) {
  private val items = collection.mutable.ListBuffer.empty[GenericItem]
  private var done = false
  private var constructionOk = true

  override def addOutput(port: String, edge: Option[Edge]): Unit = {
    graph.engine.staticError(None, "Cannot connect to outputs of an OutputNode")
    constructionOk = false
  }

  override def valid: Boolean = {
    super.valid && constructionOk
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    items.synchronized {
      items.append(msg.item)
    }
  }

  override def run(): Unit = {
    done = true
  }

  def closed = done

  def read(): Option[GenericItem] = {
    items.synchronized {
      if (items.isEmpty) {
        None
      } else {
        Some(items.remove(0))
      }
    }
  }
}
