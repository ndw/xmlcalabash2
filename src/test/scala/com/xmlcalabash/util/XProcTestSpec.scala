package com.xmlcalabash.util

/* I'm going to copy this file around from project to project for a bit, then I'm going to get
   frustrated at some point and make a separate project to hold it.
   /me takes an "I told you so" token, 18 Oct 2018.
 */

import java.io.File

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.testing.TestRunner
import org.scalatest.FunSpec

import scala.collection.mutable.ListBuffer

class XProcTestSpec extends FunSpec {
  protected val runtimeConfig: XMLCalabashConfig = XMLCalabashConfig.newInstance()
  protected val testFiles: ListBuffer[String] = ListBuffer.empty[String]

  protected def runtests(title: String, source: String): Unit = {
    describe(title) {
      val dir = new File(source)
      recurse(dir)

      testFiles foreach {
        case filename =>
          val pos = filename.indexOf("/tests/")
          val name = if (pos >= 0) {
            filename.substring(pos+7)
          } else {
            filename
          }
          it (s"test: $name") {
            test(filename)
          }
      }
    }
  }

  protected def test(fn: String) {
    val runner = new TestRunner(runtimeConfig, List(fn))
    val results = runner.run()
    for (result <- results) {
      if (!result.passed) {
        println("BANG")
      }
      assert(result.passed)
    }
  }

  protected def recurse(dir: File): Unit = {
    val fnregex = "^.*.xml".r

    if (dir.isDirectory) {
      for (file <- dir.listFiles()) {
        if (file.isDirectory) {
          recurse(file)
        } else {
          file.getName match {
            case fnregex() =>
              testFiles += file.getAbsolutePath
            case _ => Unit
          }
        }
      }
    } else {
      testFiles += dir.getAbsolutePath
    }
  }

}
