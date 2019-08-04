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

public class IterationSizeShim extends ExtensionFunctionDefinition {
    private static StructuredQName funcname = new StructuredQName("p", "http://www.w3.org/ns/xproc", "iteration-size");
    private XMLCalabashConfig runtime = null;

    private IterationSizeShim() {
        // no one can call this
    }

    public IterationSizeShim(XMLCalabashConfig runtime) {
        this.runtime = runtime;
    }

    @Override
    public StructuredQName getFunctionQName() {
        return funcname;
    }

    @Override
    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{};
    }

    @Override
    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.SINGLE_ATOMIC;
    }

    @Override
    public ExtensionFunctionCall makeCallExpression() {
        return new IterationSizeCall(this);
    }

    private class IterationSizeCall extends ExtensionFunctionCall {
        private ExtensionFunctionDefinition xdef = null;
        private StaticContext staticContext = null;

        private IterationSizeCall(ExtensionFunctionDefinition def) {
            xdef = def;
        }

        @Override
        public void supplyStaticContext(StaticContext context, int locationId, Expression[] arguments) {
            staticContext = context;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Sequence<?> call(XPathContext context, Sequence[] arguments) {
            IterationSize impl = new IterationSize(runtime);
            return impl.call(staticContext, context, arguments);
        }
    }
}
