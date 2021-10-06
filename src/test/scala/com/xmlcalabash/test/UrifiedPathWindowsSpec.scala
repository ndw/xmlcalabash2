package com.xmlcalabash.test

import com.xmlcalabash.util.UrifiedPath
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec

class UrifiedPathWindowsSpec extends AnyFlatSpec with BeforeAndAfter {
  private val OSNAME = "Windows"
  private val FILESEP = "\\"

  private var saveOsname = ""
  private var saveFilesep = ""

  before {
    saveOsname = UrifiedPath.osname
    saveFilesep = UrifiedPath.filesep
    UrifiedPath.mockOS(OSNAME, FILESEP)
  }

  after {
    UrifiedPath.mockOS(saveOsname, saveFilesep)
  }

  "http://example.com/path/file " should " parse" in {
    val path = new UrifiedPath("http://example.com/path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "http")
    assert(path.absolute)
    assert(!path.fixable)
    assert(path.toString == "http://example.com/path/file")
  }

  "http://user@example.com/path/file " should " parse" in {
    val path = new UrifiedPath("http://user@example.com/path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "http")
    assert(path.absolute)
    assert(!path.fixable)
    assert(path.toString == "http://user@example.com/path/file")
  }

  "http://user:pass@example.com/path/file " should " parse" in {
    val path = new UrifiedPath("http://user:pass@example.com/path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "http")
    assert(path.absolute)
    assert(!path.fixable)
    assert(path.toString == "http://user:pass@example.com/path/file")
  }

  "http://example.com/path/file#foo " should " parse" in {
    val path = new UrifiedPath("http://example.com/path/file#foo")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "http")
    assert(path.absolute)
    assert(!path.fixable)
    assert(path.toString == "http://example.com/path/file#foo")
  }

  "http://example.com/path/file?foo " should " parse" in {
    val path = new UrifiedPath("http://example.com/path/file?foo")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "http")
    assert(path.absolute)
    assert(!path.fixable)
    assert(path.toString == "http://example.com/path/file?foo")
  }

  "file:path/file " should " parse" in {
    val path = new UrifiedPath("file:path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "file")
    assert(path.relative)
    assert(path.fixable)
    assert(path.toString == "file:path/file")
  }

  "file:/path/file " should " parse" in {
    val path = new UrifiedPath("file:/path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "file")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "file:///path/file")
  }

  "file://path/file " should " parse" in {
    val path = new UrifiedPath("file://path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isDefined)
    assert(path.scheme.get == "file")
    assert(path.authority.get == "path")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "file://path/file")
  }

  "file:///path/file " should " parse" in {
    val path = new UrifiedPath("file:///path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "file")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "file:///path/file")
  }

  "file:////path/file " should " parse" in {
    val path = new UrifiedPath("file:////path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "file")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "file:///path/file")
  }

  "path/file " should " parse" in {
    val path = new UrifiedPath("path/file")
    assert(path.scheme.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.relative)
    assert(path.fixable)
    assert(path.toString == "path/file")
  }

  "/path/file " should " parse" in {
    val path = new UrifiedPath("/path/file")
    assert(path.scheme.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "/path/file")
  }

  "///path/file " should " parse" in {
    val path = new UrifiedPath("///path/file")
    assert(path.scheme.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "/path/file")
  }

  "////path/file " should " parse" in {
    val path = new UrifiedPath("////path/file")
    assert(path.scheme.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "/path/file")
  }

  "c:\\path\\file " should " parse" in {
    val path = new UrifiedPath("c:\\path\\file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isDefined)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "file")
    assert(path.driveLetter.get == "c")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString === "file:///c:/path/file")
  }

  "c:\\path\\file#foo " should " parse" in {
    val path = new UrifiedPath("c:\\path\\file#foo")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isDefined)
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "file")
    assert(path.driveLetter.get == "c")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString === "file:///c:/path/file%23foo")
  }

  "\\\\host\\path\\file " should " parse" in {
    val path = new UrifiedPath("\\\\host\\path\\file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isDefined)
    assert(path.scheme.get == "file")
    assert(path.authority.get == "host")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "file://host/path/file")
  }

  "\\\\host:port\\path\\file " should " parse" in {
    val path = new UrifiedPath("\\\\host:port\\path\\file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isDefined)
    assert(path.scheme.get == "file")
    assert(path.authority.get == "host:port")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "file://host:port/path/file")
  }

  "file:////c:/path/file " should " parse" in {
    val path = new UrifiedPath("file:////c:/path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isDefined)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.get == "c")
    assert(path.scheme.get == "file")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "file:///c:/path/file")
  }

  "file:///c:/path/file " should " parse" in {
    val path = new UrifiedPath("file:///c:/path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isDefined)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.get == "c")
    assert(path.scheme.get == "file")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "file:///c:/path/file")
  }

  "file://c:/path/file " should " parse" in {
    val path = new UrifiedPath("file://c:/path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isDefined)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.get == "c")
    assert(path.scheme.get == "file")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "file:///c:/path/file")
  }

  "file:/c:/path/file " should " parse" in {
    val path = new UrifiedPath("file:/c:/path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isDefined)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.get == "c")
    assert(path.scheme.get == "file")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "file:///c:/path/file")
  }

  "file:c:/path/file " should " parse" in {
    val path = new UrifiedPath("file:c:/path/file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isDefined)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.get == "c")
    assert(path.scheme.get == "file")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "file:///c:/path/file")
  }

  "C:\\Program Files (x86)\\acmeXProc\\bin " should " parse" in {
    val path = new UrifiedPath("C:\\Program Files (x86)\\acmeXProc\\bin")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isDefined)
    assert(path.authority.isEmpty)
    assert(path.driveLetter.get == "C")
    assert(path.scheme.get == "file")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "file:///C:/Program%20Files%20(x86)/acmeXProc/bin")
  }

  "file:///path\\file " should " parse" in {
    val path = new UrifiedPath("file:///path\\file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.absolute)
    assert(path.fixable)
    assert(path.scheme.get == "file")
    assert(path.toString == "file:///path/file")
  }
}
