package com.xmlcalabash.testers

import java.io.File

import com.xmlcalabash.config.XMLCalabash
import org.scalatest.FunSpec

import scala.collection.mutable.ListBuffer

class XProcTestSpec extends FunSpec {
  private val fnregex = "^.*.xml".r

  protected val runtimeConfig: XMLCalabash = XMLCalabash.newInstance()
  protected var error: Option[String] = Option.empty[String]
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
    val runner = new TestRunner(runtimeConfig, fn)
    try {
      error = runner.run()
    } catch {
      case t: Throwable =>
        println(t)
        throw t
    }
    if (error.isDefined) {
      println(error.toString)
    }
    assert(error.isEmpty)
  }

  protected def recurse(dir: File): Unit = {
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
