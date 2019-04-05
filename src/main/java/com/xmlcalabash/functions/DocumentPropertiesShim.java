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

public class DocumentPropertiesShim extends ExtensionFunctionDefinition {
    private static XProcConstants$ xproc_constants = XProcConstants$.MODULE$;
    private static StructuredQName funcname = new StructuredQName("p", xproc_constants.ns_p(), "document-properties");
    private XMLCalabashConfig runtime = null;

    private DocumentPropertiesShim() {
        // no one can call this
    }

    public DocumentPropertiesShim(XMLCalabashConfig runtime) {
        this.runtime = runtime;
    }

    @Override
    public StructuredQName getFunctionQName() {
        return funcname;
    }

    @Override
    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{SequenceType.SINGLE_ITEM};
    }

    @Override
    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.SINGLE_ITEM;
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
        public Sequence<?> call(XPathContext context, Sequence[] arguments) {
            DocumentProperties impl = new DocumentProperties(runtime);
            return impl.call(staticContext, context, arguments);
        }
    }
}
