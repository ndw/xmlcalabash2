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

public class IterationPositionShim extends ExtensionFunctionDefinition {
    private static XProcConstants$ xproc_constants = XProcConstants$.MODULE$;
    private static StructuredQName funcname = new StructuredQName("p", xproc_constants.ns_p(), "iteration-position");
    private XMLCalabashConfig runtime = null;

    private IterationPositionShim() {
        // no one can call this
    }

    public IterationPositionShim(XMLCalabashConfig runtime) {
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
        return new IterationPositionCall(this);
    }

    private class IterationPositionCall extends ExtensionFunctionCall {
        private ExtensionFunctionDefinition xdef = null;
        private StaticContext staticContext = null;

        private IterationPositionCall(ExtensionFunctionDefinition def) {
            xdef = def;
        }

        @Override
        public void supplyStaticContext(StaticContext context, int locationId, Expression[] arguments) {
            staticContext = context;
        }

        @Override
        public Sequence<?> call(XPathContext context, Sequence[] arguments) {
            IterationPosition impl = new IterationPosition(runtime);
            return impl.call(staticContext, context, arguments);
        }
    }
}
