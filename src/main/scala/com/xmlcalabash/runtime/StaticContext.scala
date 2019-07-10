package com.xmlcalabash.runtime

import java.net.URI

import com.jafpl.graph.Location
import com.jafpl.messages.Message
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.model.xml.NameBinding
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

class StaticContext(val config: XMLCalabashConfig) {
  protected var _baseURI: Option[URI] = None
  protected var _inScopeNS = Map.empty[String,String]
  protected var _location: Option[Location] = None
  protected var _statics = Map.empty[String,Message]

  def this(runtime: XMLCalabashRuntime) {
    this(runtime.config)
  }

  def this(context: StaticContext) {
    this(context.config)
    _baseURI = context._baseURI
    _inScopeNS = context._inScopeNS
    _location = context._location
    _statics = context._statics
  }

  def this(config: XMLCalabashConfig, node: XdmNode) {
    this(config)
    _baseURI = Option(node.getBaseURI)
    _inScopeNS = S9Api.inScopeNamespaces(node)
    _location = Some(new NodeLocation(node))
  }

  def baseURI: Option[URI] = _baseURI
  protected[xmlcalabash] def baseURI_=(uri: URI): Unit = {
    _baseURI = Some(uri)
  }

  def nsBindings: Map[String,String] = _inScopeNS
  protected[xmlcalabash] def nsBindings_=(bindings: Map[String,String]): Unit = {
    _inScopeNS = bindings
  }

  def location: Option[Location] = _location
  protected[xmlcalabash] def location_=(loc: Location): Unit = {
    _location = Some(loc)
  }

  def statics: Map[String,Message] = _statics

  def withStatics(statics: Map[String,Message]): StaticContext = {
    val context = new StaticContext(this)
    context._statics = statics
    context
  }

  def withStatics(bindings: List[NameBinding]): StaticContext = {
    val statics = mutable.HashMap.empty[String, Message]
    for (bind <- bindings) {
      statics.put(bind.name.getClarkName, bind.staticValue.get)
    }
    withStatics(statics.toMap)
  }
}
