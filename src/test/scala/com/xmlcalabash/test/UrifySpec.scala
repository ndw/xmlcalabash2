package com.xmlcalabash.test

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.util.{URIUtils, UrifiedPath}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec

import java.net.URI

class UrifySpec extends AnyFlatSpec with BeforeAndAfter {
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

  "#B " should " resolve against an absolute URI" in {
    val answer = URIUtils.urify("#B", "https://wiki.acme.com/fr/categories.html")
    assert(answer == URI.create("https://wiki.acme.com/fr/%23B"))
  }

  "index.html " should " resolve against an absolute URI" in {
    val answer = URIUtils.urify("index.html", "https://wiki.acme.com/fr/categories.html")
    assert(answer == URI.create("https://wiki.acme.com/fr/index.html"))
  }

  "/en/index.html " should " resolve against an absolute URI" in {
    val answer = URIUtils.urify("/en/index.html", "https://wiki.acme.com/fr/categories.html")
    assert(answer == URI.create("https://wiki.acme.com/en/index.html"))
  }

  "//www.acme.com/lib/acme.js " should " resolve against an absolute URI" in {
    val answer = URIUtils.urify("//www.acme.com/lib/acme.js", "https://wiki.acme.com/fr/categories.html")
    assert(answer == URI.create("https://wiki.acme.com/www.acme.com/lib/acme.js"))
  }

  "http://example.com/absolute/ " should " resolve against an absolute URI" in {
    val answer = URIUtils.urify("http://example.com/absolute/", "https://wiki.acme.com/fr/categories.html")
    assert(answer == URI.create("http://example.com/absolute/"))
  }

  "https://example.com/absolute/ " should " resolve against an absolute URI" in {
    val answer = URIUtils.urify("https://example.com/absolute/", "https://wiki.acme.com/fr/categories.html")
    assert(answer == URI.create("https://example.com/absolute/"))
  }

  "https://example.com/absolute/ " should " resolve against a relative URI" in {
    val answer = URIUtils.urify("https://example.com/absolute/", "relative-uri")
    assert(answer == URI.create("https://example.com/absolute/"))
  }

  "file:path " should " throw an exception if resolved against a base URI with a different scheme" in {
    assertThrows[XProcException] {
      URIUtils.urify("file:path", "http://example.com/")
    }
  }

  "file:path " should " throw an exception if resolved against a relative URI" in {
    assertThrows[XProcException] {
      URIUtils.urify("file:path", "file:otherpath")
    }
  }

  "urn:a:b:c " should " resolve against an absolute URI" in {
    val answer = URIUtils.urify("urn:a:b:c", "https://wiki.acme.com/fr/categories.html")
    assert(answer == URI.create("urn:a:b:c"))
  }

  "urn:a:b:c " should " resolve against an relative URI" in {
    val answer = URIUtils.urify("urn:a:b:c", "segment")
    assert(answer == URI.create("urn:a:b:c"))
  }


}
