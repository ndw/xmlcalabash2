package com.xmlcalabash.test

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.util.Urify
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec

class UrifyNonWindowsSpec extends AnyFlatSpec with BeforeAndAfter {
  private val OSNAME = "MacOs"
  private val FILESEP = "/"
  private val CWD = "/home/johndoe/"

  private var saveOsname = ""
  private var saveFilesep = ""
  private var saveCwd = ""

  before {
    saveOsname = Urify.osname
    saveFilesep = Urify.filesep
    saveCwd = Urify.cwd
    Urify.mockOS(OSNAME, FILESEP, Some(CWD))
  }

  after {
    Urify.mockOS(saveOsname, saveFilesep, Some(saveCwd))
  }

  "http://example.com/path/file " should " parse" in {
    val path = new Urify("http://example.com/path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "http")
    assert(path.absolute)
    assert(!path.fixable)
    assert(path.toString == "http://example.com/path/file")
  }

  "http://user@example.com/path/file " should " parse" in {
    val path = new Urify("http://user@example.com/path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "http")
    assert(path.absolute)
    assert(!path.fixable)
    assert(path.toString == "http://user@example.com/path/file")
  }

  "http://user:pass@example.com/path/file " should " parse" in {
    val path = new Urify("http://user:pass@example.com/path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "http")
    assert(path.absolute)
    assert(!path.fixable)
    assert(path.toString == "http://user:pass@example.com/path/file")
  }

  "http://example.com/path/file#foo " should " parse" in {
    val path = new Urify("http://example.com/path/file#foo")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "http")
    assert(path.absolute)
    assert(!path.fixable)
    assert(path.toString == "http://example.com/path/file#foo")
  }

  "http://example.com/path/file?foo " should " parse" in {
    val path = new Urify("http://example.com/path/file?foo")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "http")
    assert(path.absolute)
    assert(!path.fixable)
    assert(path.toString == "http://example.com/path/file?foo")
  }

  "file:path/file " should " parse" in {
    val path = new Urify("file:path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "file")
    assert(path.relative)
    assert(path.fixable)
    assert(path.toString == "file:path/file")
  }

  "file:/path/file " should " parse" in {
    val path = new Urify("file:/path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "file")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "file:///path/file")
  }

  "file://path/file " should " parse" in {
    val path = new Urify("file://path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "file")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "file:///path/file")
  }

  "file:///path/file " should " parse" in {
    val path = new Urify("file:///path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "file")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "file:///path/file")
  }

  "file:////path/file " should " parse" in {
    val path = new Urify("file:////path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "file")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "file:///path/file")
  }

  "path/file " should " parse" in {
    val path = new Urify("path/file")
    assert(path.scheme.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.relative)
    assert(path.mightBeFixable)
    assert(path.toString == "path/file")
  }

  "#B " should " parse" in {
    val path = new Urify("#B")
    assert(path.scheme.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.relative)
    assert(path.mightBeFixable)
    assert(path.toString == "#B")
  }

  "/path/file " should " parse" in {
    val path = new Urify("/path/file")
    assert(path.scheme.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.absolute)
    assert(path.mightBeFixable)
    assert(path.toString == "/path/file")
  }

  "//path/file " should " parse" in {
    val path = new Urify("//path/file")
    assert(path.scheme.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.absolute)
    assert(path.mightBeFixable)
    assert(path.toString == "/path/file")
  }

  "///path/file " should " parse" in {
    val path = new Urify("///path/file")
    assert(path.scheme.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.absolute)
    assert(path.mightBeFixable)
    assert(path.toString == "/path/file")
  }

  "////path/file " should " parse" in {
    val path = new Urify("////path/file")
    assert(path.scheme.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.absolute)
    assert(path.mightBeFixable)
    assert(path.toString == "/path/file")
  }

  "/Users/John Doe/Document Settings/path/file " should " parse" in {
    val path = new Urify("/Users/John Doe/Document Settings/path/file")
    assert(path.scheme.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.absolute)
    assert(path.mightBeFixable)
    assert(path.toFixedString == "/Users/John%20Doe/Document%20Settings/path/file")
    assert(path.toString == "/Users/John Doe/Document Settings/path/file")
  }

  "/Users/John%?#Madison%20Doe/path#foo/bar% " should " parse" in {
    val path = new Urify("/Users/John%?#Madison%20Doe/path#foo/bar%")
    assert(path.scheme.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.absolute)
    assert(path.mightBeFixable)
    assert(path.toFixedString == "/Users/John%25%3F%23Madison%20Doe/path%23foo/bar%25")
    assert(path.toString == "/Users/John%?#Madison%20Doe/path#foo/bar%")
  }

  "file:///path\\file " should " parse" in {
    val path = new Urify("file:///path\\file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.absolute)
    assert(path.fixable)
    assert(path.scheme.get == "file")
    assert(path.toFixedString == "file:///path\\file")
    assert(path.toString == "file:///path\\file")
  }

  "#B " should " resolve against an absolute HTTP URI" in {
    val answer = new Urify("https://wiki.acme.com/fr/categories.html").resolve("#B")
    assert(answer.toString == "https://wiki.acme.com/fr/categories.html#B")
  }

  "index.html " should " resolve against an absolute URI" in {
    val answer = new Urify("https://wiki.acme.com/fr/categories.html").resolve("index.html")
    assert(answer.toString == "https://wiki.acme.com/fr/index.html")
  }

  "/en/index.html " should " resolve against an absolute URI" in {
    val answer = new Urify("https://wiki.acme.com/fr/categories.html").resolve("/en/index.html")
    assert(answer.toString == "https://wiki.acme.com/en/index.html")
  }

  "//www.acme.com/lib/acme.js " should " resolve against an absolute URI" in {
    val answer = new Urify("https://wiki.acme.com/fr/categories.html").resolve("//www.acme.com/lib/acme.js")
    assert(answer.toString == "https://wiki.acme.com/www.acme.com/lib/acme.js")
  }

  "http://example.com/absolute/ " should " resolve against an absolute URI" in {
    val answer = new Urify("https://wiki.acme.com/fr/categories.html").resolve("http://example.com/absolute/")
    assert(answer.toString == "http://example.com/absolute/")
  }

  "https://example.com/absolute/ " should " resolve against an absolute URI" in {
    val answer = new Urify("https://wiki.acme.com/fr/categories.html").resolve("https://example.com/absolute/")
    assert(answer.toString == "https://example.com/absolute/")
  }

  "https://example.com/absolute/ " should " resolve against a relative URI" in {
    val answer = new Urify("relative-uri").resolve("https://example.com/absolute/")
    assert(answer.toString == "https://example.com/absolute/")
  }

  "file:path " should " throw an exception if resolved against a base URI with a different scheme" in {
    assertThrows[XProcException] {
      new Urify("http://example.com/").resolve("file:path")
    }
  }

  "file:path " should " throw an exception if resolved against a relative URI" in {
    assertThrows[XProcException] {
      new Urify("file:otherpath").resolve("file:path")
    }
  }

  "urn:a:b:c " should " resolve against an absolute URI" in {
    val answer = new Urify("https://wiki.acme.com/fr/categories.html").resolve("urn:a:b:c")
    assert(answer.toString == "urn:a:b:c")
  }

  "urn:a:b:c " should " resolve against an relative URI" in {
    val answer = new Urify("segment").resolve("urn:a:b:c")
    assert(answer.toString == "urn:a:b:c")
  }

  "#B " should " resolve against an absolute file URI" in {
    val answer = new Urify("file:///path/to/file.txt").resolve("#B")
    assert(answer.toString == "file:///path/to/%23B")
  }

  "#B " should " resolve against relative path URI" in {
    val answer = new Urify("path/to/file.txt").resolve("#B")
    assert(answer.toString == "file:///home/johndoe/path/to/%23B")
  }

  "#B " should " resolve against an absolute path URI" in {
    val answer = new Urify("//path/to/file.txt").resolve("#B")
    assert(answer.toString == "file:///path/to/%23B")
  }

  "index.html " should " resolve against an absolute file URI" in {
    val answer = new Urify("file:///path/to/file.txt").resolve("index.html")
    assert(answer.toString == "file:///path/to/index.html")
  }

  "/en/index.html " should " resolve against an absolute file URI" in {
    val answer = new Urify("file:///path/to/file.txt").resolve("/en/index.html")
    assert(answer.toString == "file:///en/index.html")
  }

  "//www.acme.com/lib/acme.js " should " resolve against an absolute file URI" in {
    val answer = new Urify("file:///path/to/file.txt").resolve("//www.acme.com/lib/acme.js")
    assert(answer.toString == "file:///www.acme.com/lib/acme.js")
  }
}
