package com.xmlcalabash.util

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.util.Urify.filesep

import java.net.URI
import scala.collection.mutable.ListBuffer

object Urify {
  private var _osname: String = Option(System.getProperty("os.name")).getOrElse("not-windows")
  private var _filesep: String = Option(System.getProperty("file.separator")).getOrElse("/")

  def osname: String = _osname
  def filesep: String = _filesep
  def windows: Boolean = osname.startsWith("Windows")

  def mockOS(name: String, sep: String): Unit = {
    _osname = name
    _filesep = sep
  }
}

class Urify(val filepath: String) {
  private var _path: String = _
  private var _driveLetter = Option.empty[String]
  private var _authority = Option.empty[String]
  private var _absolute = false
  private var _fixable = Option.empty[Boolean]
  private var _hierarchical = true
  private var _scheme = Option.empty[String]
  private var _fixedString = Option.empty[String]
  private var _uri = Option.empty[URI]

  if (filesep == "\\") {
    _path = filepath.replace("\\", "/")
  } else {
    _path = filepath
  }

  if (_path.toLowerCase().startsWith("file:/")) {
    _scheme = Some("file")
    _path = _path.substring(5)
    _fixable = Some(true)
    _absolute = true
    driveAndAuthority()
    _path = _path.replaceAll("^/+", "/")
  } else if (_path.toLowerCase().startsWith("file:")) {
    // It appears to be a relative file: URI, but might be file:c:/path
    _scheme = Some("file")
    _path = _path.substring(5)
    _fixable = Some(true)
    driveAndAuthority()
    _path = _path.replaceAll("^/+", "/")
  } else {
    if (_path.toLowerCase().matches("^[-a-z0-9_]+:.*$") && (!Urify.windows || _path.charAt(1) != ':')) {
      val pos = _path.indexOf(":")
      _scheme = Some(_path.substring(0, pos))
      _path = _path.substring(pos + 1)
      _absolute = _path.startsWith("/")
      _fixable = Some(false)

      if (List("http", "https", "ftp").contains(_scheme.get)) {
        // We know these are hierarchical
      } else {
        if (List("urn", "doi").contains(_scheme.get) || !filepath.contains("/")) {
          _hierarchical = false
          _absolute = true
        }
      }
    } else {
      driveAndAuthority()
      _path = _path.replaceAll("^/+", "/")
      _absolute = _path.startsWith("/")
    }
  }

  def this(uri: URI) = {
    this(uri.toString)
  }

  def this(copy: Urify) = {
    this(copy.filepath)
    _path = copy._path
    _driveLetter = copy._driveLetter
    _authority = copy._authority
    _fixable = copy._fixable
    _absolute = copy._absolute
    _hierarchical = copy._hierarchical
    _scheme = copy._scheme
  }

  private def driveAndAuthority(): Unit = {
    if (!Urify.windows) {
      return
    }

    if (_path.matches("^/*[a-zA-Z]:.*")) {
      val pos = _path.indexOf(':')
      _scheme = Some("file")
      _fixable = Some(true)
      _driveLetter = Some(_path.charAt(pos-1).toString)
      _path = _path.substring(pos+1)
      _absolute = path.startsWith("/")
    } else if (_path.startsWith("//") && !_path.startsWith("///")) {
      _scheme = Some("file")
      _fixable = Some(true)
      _path = _path.substring(2)
      val pos = _path.indexOf("/")
      if (pos >= 0) {
        _authority = Some(_path.substring(0, pos))
        _path = _path.substring(pos)
      } else {
        // ???
        _authority = Some(_path)
        _path = "/"
      }
      _absolute = true
    }
  }

  def path: String = _path
  def driveLetter: Option[String] = _driveLetter
  def authority: Option[String] = _authority
  def absolute: Boolean = _absolute
  def relative: Boolean = !_absolute
  def hierarchical: Boolean = _hierarchical
  def fixable: Boolean = _fixable.getOrElse(false)
  def mightBeFixable: Boolean = _fixable.isEmpty || _fixable.get
  def scheme: Option[String] = _scheme

  def discardDriveLetter(): Urify = {
    if (_driveLetter.isEmpty) {
      this
    } else {
      val repl = new Urify(this)
      repl._driveLetter = None
      repl
    }
  }

  private def fixup(path: String): String = {
    val fixed = path.replaceAll("^/+", "/")
      .replaceAll("\\?", "%3F")
      .replaceAll("#", "%23")
      .replaceAll(" ", "%20")

    val parts = fixed.split("/")
    var stack = ListBuffer.empty[String]
    for (part <- parts) {
      part match {
        case "." => ()
        case ".." =>
          if (stack.nonEmpty) {
            stack = stack.dropRight(1)
          }
        case _ =>
          stack += part
      }
    }
    if (fixed.endsWith("/")) {
      stack += ""
    }

    var newpath = stack.mkString("/")

    // unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
    var buf = new StringBuffer()
    var pos = newpath.indexOf("%")
    while (pos >= 0) {
      buf.append(newpath.substring(0, pos))
      newpath = newpath.substring(pos)
      if (newpath.length < 3) {
        pos = -1
      } else {
        val c1 = newpath.charAt(1).toLower
        val c2 = newpath.charAt(2).toLower
        if (((c1 >= '0' && c1 <= '9') || (c1 >= 'a' && c1 <= 'f'))
          && ((c2 >= '0' && c2 <= '9') || (c2 >= 'a' && c2 <= 'f'))) {
          val num = Integer.parseInt(s"${c1}${c2}", 16)
          if ((num >= 'A' && num <= 'Z') || (num >= 'a' && num <= 'z') || (num >= '0' && num <= '9')
            || (num == '-') || (num == '.') || (num == '_') || (num == '~')) {
            buf.append(num.toChar)
          } else {
            buf.append(newpath.substring(0, 3))
          }
          newpath = newpath.substring(3)
        } else {
          buf.append("%")
          newpath = newpath.substring(1)
        }

        pos = newpath.indexOf("%")
      }
    }
    buf.append(newpath)

    newpath = buf.toString
    buf = new StringBuffer()
    pos = 0
    while (pos < newpath.length) {
      val ch = newpath.charAt(pos)
      ch match {
        case '%' =>
          if (pos + 2 >= newpath.length) {
            buf.append("%25")
          } else {
            val c1 = newpath.charAt(pos + 1).toLower
            val c2 = newpath.charAt(pos + 2).toLower
            if (((c1 >= '0' && c1 <= '9') || (c1 >= 'a' && c1 <= 'f'))
              && ((c2 >= '0' && c2 <= '9') || (c2 >= 'a' && c2 <= 'f'))) {
              buf.append("%")
            } else {
              buf.append("%25")
            }
          }
        case _ =>
          buf.append(ch)
      }
      pos += 1
    }

    buf.toString
  }

  def makeFixable(): Urify = {
    if (fixable) {
      this
    } else {
      val fixable = new Urify(this)
      fixable._fixable = Some(true)
      fixable
    }
  }


  def makeAbsolute: Urify = {
    if (absolute) {
      this
    } else {
      var cwd = System.getProperty("user.dir")
      if (!cwd.endsWith(filesep)) {
        cwd += filesep
      }
      val base = new Urify(cwd).resolve(this.toString)
      if (base.relative) {
        throw XProcException.xdUrifyFailed(this.toString, cwd, None)
      }
      base
    }
  }

  def resolve(uri: URI): Urify = {
    resolve(uri.toString)
  }

  def resolve(uri: String): Urify = {
    var path = new Urify(uri)
    if (path.scheme.isDefined && path.absolute) {
      return path
    }

    var cwd = System.getProperty("user.dir")
    if (!cwd.endsWith(filesep)) {
      cwd += filesep
    }
    val cwduri = new Urify(s"file://${cwd}")

    val basepath = if (absolute) {
      if (scheme.isDefined) {
        this
      } else {
        cwduri.resolve(toString)
      }
    } else {
      val base = cwduri.resolve(this.toString)
      if (base.relative) {
        throw XProcException.xdUrifyFailed(this.toString, cwd, None)
      }
      base
    }

    if (path.relative && path.driveLetter.isDefined) {
      if (basepath.driveLetter.isEmpty || basepath.driveLetter.get != path.driveLetter.get) {
        throw XProcException.xdUrifyDifferentDrives(filepath, basepath.toString, None)
      }
      path = path.discardDriveLetter()
    }

    if (path.scheme.isDefined && basepath.scheme.isDefined && path.scheme.get != basepath.scheme.get) {
      throw XProcException.xdUrifyDifferentSchemes(filepath, basepath.toString, None)
    }

    if (basepath.scheme.isDefined && basepath.scheme.get == "file" && path.scheme.isEmpty) {
      new Urify(basepath.toURI.resolve(path.makeFixable().toURI))
    } else {
      new Urify(basepath.toURI.resolve(path.toURI))
    }
  }

  def toURI: URI = {
    if (_uri.isEmpty) {
      _uri = Some(new URI(toString))
    }
    _uri.get
  }

  private def combineParts(fpath: String): String = {
    val str = if (_driveLetter.isDefined) {
      s"file:///${_driveLetter.get}:${fpath}"
    } else if (_authority.isDefined) {
      s"file://${_authority.get}${fpath}"
    } else if (_scheme.isDefined) {
      if (_scheme.get == "file") {
        if (fpath.startsWith("/")) {
          s"${_scheme.get}://${fpath}"
        } else {
          s"${_scheme.get}:${fpath}"
        }
      } else {
        s"${_scheme.get}:${fpath}"
      }
    } else {
      s"${fpath}"
    }
    str
  }

  def toFixedString: String = {
    if (_fixedString.isEmpty) {
      combineParts(fixup(_path))
    } else {
      _fixedString.get
    }
  }

  override def toString: String = {
    if (_fixedString.isDefined) {
      return _fixedString.get
    }

    if (fixable) {
      _fixedString = Some(toFixedString)
      return _fixedString.get
    }

    combineParts(_path)
 }
}