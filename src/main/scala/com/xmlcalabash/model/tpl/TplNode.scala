package com.xmlcalabash.model.tpl

import scala.collection.mutable

class TplNode(val name: String, val chars: Option[String]) {
  val args: mutable.ListBuffer[TplNode] = mutable.ListBuffer.empty[TplNode]
  var simplified: Boolean = false

  def this(name: String) {
    this(name, None)
  }

  def dump(): Unit = {
    dump("")
  }

  private def dump(indent: String): Unit = {
    if (chars.isDefined) {
      if (name == "TOKEN") {
        println(indent + "TOK: " + chars.get)
      } else {
        println(indent + name + ": " + chars.get)
      }
    } else {
      println(indent + name + " (" + args.length + ")")
    }
    for (arg <- args) {
      arg.dump(indent + "  ")
    }
  }

  def simplify(): TplNode = {
    // N.B. Should only be called on the root note

    var changed: Boolean = true
    var node = this
    while (changed) {
      node = node.simplifyTree().get
      changed = node.changedTree()
    }
    node
  }

  private def simplifyTree(): Option[TplNode] = {
    val simple = mutable.ListBuffer.empty[TplNode]
    for (arg <- args) {
      val sarg = arg.simplifyTree()
      if (sarg.isDefined) {
        simple += sarg.get
      } else {
        simplified = true
      }
      args.clear()
      for (arg <- simple) {
        args += arg
      }
    }

    name match {
      case "ARR" =>
        None
      case "TOKEN" =>
        None
      case "VarOrString" =>
        assert(args.length == 1)
        args.head.simplified = true
        Some(args.head)

      case "VarRef" =>
        assert(args.length == 1)
        args.head.simplified = true
        Some(args.head)

      case "OptionBindings" =>
        //args.head.simplified = true
        Some(this)

/*
      case "Expression" =>
        args.head.simplified = true
        Some(args.head)
      case "ValueExpr" =>
        if (args.length == 1) {
          args.head.simplified = true
          Some(args.head)
        } else {
          Some(this)
        }
      case "AdditiveExpr" =>
        if (args.length == 1) {
          args.head.simplified = true
          Some(args.head)
        } else {
          if (args.length > 3) {
            simplified = true
            val expr = new TplNode("AdditiveExpr")
            expr.args += args.head
            expr.args += args(1)
            expr.args += args(2)
            args.remove(0, 3)
            args.insert(0, expr)
          }
          Some(this)
        }
      case "MultiplicativeExpr" =>
        if (args.length == 1) {
          args.head.simplified = true
          Some(args.head)
        } else {
          if (args.length > 3) {
            simplified = true
            val expr = new TplNode("MultiplicativeExpr")
            expr.args += args.head
            expr.args += args(1)
            expr.args += args(2)
            args.remove(0, 3)
            args.insert(0, expr)
          }
          Some(this)
        }
      case "UnaryExpr" =>
        if (args.length == 1) {
          args.head.simplified = true
          Some(args.head)
        } else {
          if (args.length == 2
            && args.head.name == "TOKEN" && args.head.chars.get == "+") {
            args(1).simplified = true
            Some(args(1))
          } else {
            Some(this)
          }
        }
      case "ParenthesizedExpr" =>
        args(1).simplified = true
        Some(args(1))
      case "FunctionCall" =>
        if (chars.isEmpty) {
          val node = new TplNode(name, args.head.chars)
          var pos = 2
          while (pos < args.length) {
            node.args += args(pos)
            pos += 2
          }
          node.simplified = true
          Some(node)
        } else {
          Some(this)
        }
      case "VarRef" =>
        if (chars.isEmpty) {
          val node = new TplNode(name, args(1).chars)
          node.simplified = true
          Some(node)
        } else {
          Some(this)
        }
*/
      case "EOF" =>
        simplified = true
        None
      case _ =>
        Some(this)
    }
  }

  private def changedTree(): Boolean = {
    var changed = simplified
    simplified = false
    for (arg <- args) {
      changed = changed || arg.changedTree()
    }
    changed
  }
}
