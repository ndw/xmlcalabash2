package com.xmlcalabash.drivers

import java.io.{File, FileInputStream}

import com.xmlcalabash.parsers.StepConfigBuilder

object ScpDriver extends App {
  if (args.isEmpty) {
    println("Reading from resource file: xproc-steps.txt")
  } else {
    println("Reading from " + args.head)
  }

  val in = if (args.nonEmpty) {
    new FileInputStream(new File(args.head))
  } else {
    getClass.getResourceAsStream("/xproc-steps.txt")
  }

  val builder = new StepConfigBuilder()
  val sigs = if (args.isEmpty) {
    builder.parse(in)
  } else {
    builder.parse(in, args.head)
  }

  println(sigs)
}
