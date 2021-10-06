package com.xmlcalabash.test

import com.xmlcalabash.util.UrifiedPath
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec

class UrifiedPathSpec extends AnyFlatSpec with BeforeAndAfter {
  private val OSNAME = "MacOs"
  private val FILESEP = "/"

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
    assert(path.authority.isEmpty)
    assert(path.scheme.get == "file")
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "file:///path/file")
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

  "//path/file " should " parse" in {
    val path = new UrifiedPath("//path/file")
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

  "/Users/John Doe/Document Settings/path/file " should " parse" in {
    val path = new UrifiedPath("/Users/John Doe/Document Settings/path/file")
    assert(path.scheme.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "/Users/John%20Doe/Document%20Settings/path/file")
  }

  "/Users/John%?#Madison%20Doe/path#foo/bar% " should " parse" in {
    val path = new UrifiedPath("/Users/John%?#Madison%20Doe/path#foo/bar%")
    assert(path.scheme.isEmpty)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.absolute)
    assert(path.fixable)
    assert(path.toString == "/Users/John%25%3F%23Madison%20Doe/path%23foo/bar%25")
  }

  "file:///path\\file " should " parse" in {
    val path = new UrifiedPath("file:///path\\file")
    assert(path.scheme.isDefined)
    assert(path.driveLetter.isEmpty)
    assert(path.authority.isEmpty)
    assert(path.absolute)
    assert(path.fixable)
    assert(path.scheme.get == "file")
    assert(path.toString == "file:///path\\file")
  }
}
