package com.xmlcalabash.graph

import com.xmlcalabash.items.GenericItem
import com.xmlcalabash.messages.{CloseMessage, ItemMessage, RanMessage}
import com.xmlcalabash.runtime.{CompoundEnd, CompoundStart}

import scala.collection.mutable

/**
  * Created by ndw on 10/2/16.
  */
class LoopEnd(graph: Graph, name: Option[String], step: CompoundEnd) extends Node(graph, name, Some(step)) {
}
