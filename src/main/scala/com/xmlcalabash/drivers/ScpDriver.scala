package com.xmlcalabash.drivers

import com.xmlcalabash.config.{StepConfigBuilder, StepConfigParser}

import scala.collection.mutable.ListBuffer
import scala.io.Source

object ScpDriver extends App {
  if (args.isEmpty) {
    println("Reading from resource file: xproc-steps.txt")
  } else {
    println("Reading from " + args.head)
  }

  val cfgOffsets = ListBuffer.empty[Int]
  var cfgOffset: Int = 0
  var text = ""
  val bufferedSource = if (args.isEmpty) {
    Source.fromInputStream(getClass.getResourceAsStream("/xproc-steps.txt"))
  } else {
    Source.fromFile(args.head)
  }
  for (line <- bufferedSource.getLines) {
    cfgOffsets += cfgOffset
    cfgOffset += (line.length + 1)
    text += line + "\n"
  }
  bufferedSource.close

  val tbuilder = new StepConfigBuilder()
  val parser = new StepConfigParser(text, tbuilder)

  try {
    parser.parse
    println(tbuilder.steps.size)
  } catch {
    case err: StepConfigParser.ParseException =>
      println(s"ERROR ${err.begin} ${LineColumn(err.begin)} $err")
    case err: com.xmlcalabash.exceptions.ParseException =>
      println(s"ERROR ${err.begin} ${LineColumn(err.begin)} $err")
  }

  private def LineColumn(pos: Int): (Int,Int) = {
    // Scan forward until we pass the position or we run out of lines
    var line0 = 0
    while (cfgOffsets.length > line0 && cfgOffsets(line0) < pos) {
      line0 += 1
    }

    // If we've passed the position, back up one line
    if (cfgOffsets(line0) > pos) {
      line0 -= 1
    }

    // The column is the position on this line
    val col0 = pos - cfgOffsets(line0)

    // Return the line/column starting with 1 not 0
    (line0+1,col0)
  }

}
