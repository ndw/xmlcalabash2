package com.xmlcalabash.model.xml

import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable.ListBuffer

class Documentation(override val config: ParserConfiguration,
                    override val parent: Option[Artifact],
                    val content: List[XdmNode]) extends Artifact(config, parent) {
  override def validate(): Boolean = true

  override def asXML: xml.Elem = {
    dumpAttr(properties.toMap)
    val nodes = ListBuffer.empty[xml.Node]
    if (children.nonEmpty) {
      nodes += xml.Text("\n")
    }
    for (child <- children) {
      nodes += child.asXML
      nodes += xml.Text("\n")
    }
    new xml.Elem("p", "documentation", dump_attr.getOrElse(xml.Null),
      namespaceScope, false, nodes:_*)
  }

}
