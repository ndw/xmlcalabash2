package com.xmlcalabash.functions;

import com.xmlcalabash.config.XMLCalabashConfig;
import com.xmlcalabash.model.util.XProcConstants$;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.value.SequenceType;

public class SystemPropertyShim extends ExtensionFunctionDefinition {
    private static StructuredQName funcname = new StructuredQName("p", "http://www.w3.org/ns/xproc", "system-property");
    private XMLCalabashConfig runtime = null;

    private SystemPropertyShim() {
        // no one can call this
    }

    public SystemPropertyShim(XMLCalabashConfig runtime) {
        this.runtime = runtime;
    }

    @Override
    public StructuredQName getFunctionQName() {
        return funcname;
    }

    @Override
    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{SequenceType.SINGLE_STRING};
    }

    @Override
    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.SINGLE_ATOMIC;
    }

    @Override
    public ExtensionFunctionCall makeCallExpression() {
        return new DocPropsCall(this);
    }

    private class DocPropsCall extends ExtensionFunctionCall {
        private ExtensionFunctionDefinition xdef = null;
        private StaticContext staticContext = null;

        private DocPropsCall(ExtensionFunctionDefinition def) {
            xdef = def;
        }

        @Override
        public void supplyStaticContext(StaticContext context, int locationId, Expression[] arguments) {
            staticContext = context;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Sequence call(XPathContext context, Sequence[] arguments) {
            SystemProperty impl = new SystemProperty(runtime);
            return impl.call(staticContext, context, arguments);
        }
    }
}
