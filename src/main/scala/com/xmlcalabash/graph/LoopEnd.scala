package com.xmlcalabash.graph

import com.xmlcalabash.items.GenericItem
import com.xmlcalabash.messages.{CloseMessage, ItemMessage, RanMessage}

import scala.collection.mutable

/**
  * Created by ndw on 10/2/16.
  */
class LoopEnd(graph: Graph, name: Option[String]) extends Node(graph, name, None) {
}
