package com.xmlcalabash.test

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.testers.XProcTestSpec
import com.xmlcalabash.util.{ArgBundle, ValueUtils}
import net.sf.saxon.s9api.QName
import org.scalatest.FlatSpec

class ArgBundleSpec extends FlatSpec {
  private val config = XMLCalabash.newInstance()

  "Parsing nothing" should "fail" in {
    val bundle = new ArgBundle(config)
    var pass = false
    try {
      bundle.parse(List())
      val pipeline = bundle.pipeline
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "Parsing pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.pipeline == "pipe.xpl")
  }

  // -i | --input

  "Parsing --input" should "fail" in {
    val bundle = new ArgBundle(config)
    val args = "--input".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "Parsing -i" should "fail" in {
    val bundle = new ArgBundle(config)
    val args = "-i".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "Parsing -isource=doc.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "-isource=doc.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.inputs("source").length == 1)
    assert(bundle.inputs("source").head == "doc.xml")
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing -isource=doc1.xml -isource=doc2.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "-isource=doc1.xml -isource=doc2.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.inputs("source").length == 2)
    assert(bundle.inputs("source").head == "doc1.xml")
    assert(bundle.inputs("source")(1) == "doc2.xml")
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing -visource=doc.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "-visource=doc.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.inputs("source").length == 1)
    assert(bundle.inputs("source").head == "doc.xml")
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing -i source=doc.xml pipeline" should "fail" in {
    val bundle = new ArgBundle(config)
    val args = "-i source=doc.xml pipe.xpl".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "Parsing --input source=doc.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "--input source=doc.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.inputs("source").length == 1)
    assert(bundle.inputs("source").head == "doc.xml")
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing -v --input source=doc.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "-v --input source=doc.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.inputs("source").length == 1)
    assert(bundle.inputs("source").head == "doc.xml")
    assert(bundle.verbose)
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing --input source=doc1.xml --input source=doc2.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "--input source=doc1.xml --input source=doc2.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.inputs("source").length == 2)
    assert(bundle.inputs("source").head == "doc1.xml")
    assert(bundle.inputs("source")(1) == "doc2.xml")
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing --input doc.xml pipeline" should "fail" in {
    val bundle = new ArgBundle(config)
    val args = "--input doc.xml pipe.xpl".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  // -o | --output

  "Parsing --output" should "fail" in {
    val bundle = new ArgBundle(config)
    val args = "--output".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "Parsing -o" should "fail" in {
    val bundle = new ArgBundle(config)
    val args = "-o".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "Parsing -oresult=doc.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "-oresult=doc.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.outputs("result").length == 1)
    assert(bundle.outputs("result").head == "doc.xml")
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing -oresult=doc1.xml -oresult=doc2.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "-oresult=doc1.xml -oresult=doc2.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.outputs("result").length == 2)
    assert(bundle.outputs("result").head == "doc1.xml")
    assert(bundle.outputs("result")(1) == "doc2.xml")
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing -voresult=doc.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "-voresult=doc.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.outputs("result").length == 1)
    assert(bundle.outputs("result").head == "doc.xml")
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing -o result=doc.xml pipeline" should "fail" in {
    val bundle = new ArgBundle(config)
    val args = "-o result=doc.xml pipe.xpl".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "Parsing --output result=doc.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "--output result=doc.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.outputs("result").length == 1)
    assert(bundle.outputs("result").head == "doc.xml")
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing -v --output result=doc.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "-v --output result=doc.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.outputs("result").length == 1)
    assert(bundle.outputs("result").head == "doc.xml")
    assert(bundle.verbose)
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing --output result=doc1.xml --output result=doc2.xml pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "--output result=doc1.xml --output result=doc2.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.outputs("result").length == 2)
    assert(bundle.outputs("result").head == "doc1.xml")
    assert(bundle.outputs("result")(1) == "doc2.xml")
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing --output doc.xml pipeline" should "fail" in {
    val bundle = new ArgBundle(config)
    val args = "--output doc.xml pipe.xpl".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  // --raw

  "Parsing --raw" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "--raw".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.raw)
  }

  // --norun

  "Parsing --norun" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "--norun".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.norun)
  }

  // -G | --graph

  "Parsing --graph" should "fail" in {
    val bundle = new ArgBundle(config)
    val args = "--graph".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "Parsing -G" should "fail" in {
    val bundle = new ArgBundle(config)
    val args = "-G".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "Parsing -Gname pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "-Gpipe.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.graph.isDefined && (bundle.graph.get == "pipe.xml"))
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing -vGname pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "-vGpipe.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.graph.isDefined && (bundle.graph.get == "pipe.xml"))
    assert(bundle.verbose)
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing -G name pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "-G pipe.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.graph.isDefined && (bundle.graph.get == "pipe.xml"))
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing --graph name pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "--graph pipe.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.graph.isDefined && (bundle.graph.get == "pipe.xml"))
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing -v --graph name pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "-v --graph pipe.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.graph.isDefined && (bundle.graph.get == "pipe.xml"))
    assert(bundle.verbose)
    assert(bundle.pipeline == "pipe.xpl")
  }

  // --graph-before

  "Parsing --graph-before" should "fail" in {
    val bundle = new ArgBundle(config)
    val args = "--graph-before".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "Parsing --graph-before name pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "--graph-before pipe.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.graphBefore.isDefined && (bundle.graphBefore.get == "pipe.xml"))
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing -v --graph-before name pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "-v --graph-before pipe.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.graphBefore.isDefined && (bundle.graphBefore.get == "pipe.xml"))
    assert(bundle.verbose)
    assert(bundle.pipeline == "pipe.xpl")
  }

  // --dump-xml

  "Parsing --dump-xml" should "fail" in {
    val bundle = new ArgBundle(config)
    val args = "--dump-xml".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  "Parsing --dump-xml name pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "--dump-xml pipe.xml pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(bundle.dumpXML.isDefined && (bundle.dumpXML.get == "pipe.xml"))
    assert(bundle.pipeline == "pipe.xpl")
  }

  // param=value

  "Parsing foo=bar pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "foo=bar pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(ValueUtils.singletonStringValue(bundle.params(new QName("", "foo")).value, None) == "bar")
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing foo=2 pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "foo=2 pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(ValueUtils.singletonStringValue(bundle.params(new QName("", "foo")).value, None) == "2")
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing -bex=foo ex:foo=bar pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "-bex=foo ex:foo=bar pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(ValueUtils.singletonStringValue(bundle.params(new QName("foo", "foo")).value, None) == "bar")
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing --bind ex=foo ex:foo=bar pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "--bind ex=foo ex:foo=bar pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(ValueUtils.singletonStringValue(bundle.params(new QName("foo", "foo")).value, None) == "bar")
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing ex:foo=bar pipeline" should "fail" in {
    val bundle = new ArgBundle(config)
    val args = "ex:foo=bar pipe.xpl".split("\\s+")
    var pass = false
    try {
      bundle.parse(args.toList)
    } catch {
      case _: Throwable => pass = true
    }
    assert(pass)
  }

  // ?param=value

  "Parsing ?foo=3+4 pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "?foo=3+4 pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(ValueUtils.singletonStringValue(bundle.params(new QName("", "foo")).value, None) == "7")
    assert(bundle.pipeline == "pipe.xpl")
  }

  "Parsing ?foo=3+4 ?bar=3+$foo pipeline" should "succeed" in {
    val bundle = new ArgBundle(config)
    val args = "?foo=3+4 ?bar=3+$foo pipe.xpl".split("\\s+")
    bundle.parse(args.toList)
    assert(ValueUtils.singletonStringValue(bundle.params(new QName("", "foo")).value, None) == "7")
    assert(ValueUtils.singletonStringValue(bundle.params(new QName("", "bar")).value, None) == "10")
    assert(bundle.pipeline == "pipe.xpl")
  }

}
