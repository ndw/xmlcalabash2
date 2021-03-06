package com.xmlcalabash.exceptions

object ExceptionCode extends Enumeration {
  type ExceptionCode = Value
  val
      // Configuration exception codes
      CFGINCOMPLETE, MUSTBEABS, CLOSED,

      // Model exception codes
      INTERNAL,
      DUPINPUTSIG, DUPOUTPUTSIG, DUPOPTSIG, BADINPUTSIG, BADOUTPUTSIG, BADOPTSIG,
      NOTYPE, NOIMPL, IMPLNOTSTEP,
      BADTREENODE,
      BADBOOLEAN, NOPREFIX,
      ATTRREQ, BADATTR,
      BADATOMICATTR, BADATOMICINPUTPORT, BADATOMICOUTPUTPORT,
      BADCONTAINERATTR, DUPCONTAINERINPUTPORT, DUPCONTAINEROUTPUTPORT, DUPPRIMARYINPUT, DUPPRIMARYOUTPUT,
      NOCONTAINEROUTPUT,
      PORTATTRREQ,
      BADPIPE, BADSEQ, BADPRIMARY,
      NAMEATTRREQ, SELECTATTRREQ,
      BADPIPELINEROOT, INVALIDPIPELINE, NOTASTEP,
      BADSERPORT, BADSERSTANDALONE,
      BADAVT, NOBINDING,
      DUPOTHERWISE, MISSINGWHEN, BADCHOOSECHILD, DIFFPRIMARYINPUT, DIFFPRIMARYOUTPUT,
      TESTREQUIRED,
      DUPGROUP, DUPFINALLY, MISSINGGROUP, MISSINGCATCH, BADTRYCHILD,
      INVALIDNAME, NODRP, MIXEDPIPE, NOSTEP, NOPORT, NOPRIMARYINPUTPORT, DUPINPUTPORT
  = Value
}
