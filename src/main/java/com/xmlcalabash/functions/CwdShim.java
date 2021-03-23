package com.xmlcalabash.functions;

import com.xmlcalabash.config.XMLCalabashConfig;
import com.xmlcalabash.model.util.XProcConstants$;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.value.SequenceType;

public class CwdShim extends ExtensionFunctionDefinition {
    private static StructuredQName funcname = new StructuredQName("exf", "http://exproc.org/standard/functions", "cwd");
    private XMLCalabashConfig runtime = null;

    private CwdShim() {
        // no one can call this
    }

    public CwdShim(XMLCalabashConfig runtime) {
        this.runtime = runtime;
    }

    @Override
    public StructuredQName getFunctionQName() {
        return funcname;
    }

    @Override
    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{SequenceType.EMPTY_SEQUENCE};
    }

    @Override
    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.SINGLE_ATOMIC;
    }

    @Override
    public ExtensionFunctionCall makeCallExpression() {
        return new CwdCall(this);
    }

    private class CwdCall extends ExtensionFunctionCall {
        private ExtensionFunctionDefinition xdef = null;

        private CwdCall(ExtensionFunctionDefinition def) {
            xdef = def;
        }

        @Override
        public Sequence call(XPathContext context, Sequence[] arguments) {
            Cwd impl = new Cwd(runtime);
            return impl.call(context, arguments);
        }
    }
}
