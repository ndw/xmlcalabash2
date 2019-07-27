package com.xmlcalabash.functions;

import com.xmlcalabash.config.XMLCalabashConfig;
import com.xmlcalabash.model.util.XProcConstants$;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.value.SequenceType;

public class InjNameShim extends ExtensionFunctionDefinition {
    private static XProcConstants$ xproc_constants = XProcConstants$.MODULE$;
    private static StructuredQName funcname = new StructuredQName("cx", xproc_constants.ns_cx(), "step-name");
    private XMLCalabashConfig runtime = null;

    private InjNameShim() {
        // no one can call this
    }

    public InjNameShim(XMLCalabashConfig runtime) {
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
        return SequenceType.SINGLE_STRING;
    }

    @Override
    public ExtensionFunctionCall makeCallExpression() {
        return new FunctionCall(this);
    }

    private class FunctionCall extends ExtensionFunctionCall {
        private ExtensionFunctionDefinition xdef = null;

        private FunctionCall(ExtensionFunctionDefinition def) {
            xdef = def;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Sequence<?> call(XPathContext context, Sequence[] arguments) {
            InjName impl = new InjName(runtime);
            return impl.call(context, arguments);
        }
    }
}
