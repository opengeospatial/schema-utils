package org.opengis.cite.validation;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;

import org.apache.xerces.jaxp.validation.XSGrammarPoolContainer;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.grammars.XMLGrammarPool;
import org.apache.xerces.xni.grammars.XSGrammar;
import org.apache.xerces.xs.XSModel;

/**
 * Provides methods for building schema models that can be used to access and
 * query the post-schema-validation infoset (PSVI). It employs the XML Schema
 * API implemented by the Apache Xerces2 parser.
 * 
 * @see <a href="http://xerces.apache.org/xerces2-j/javadocs/xs/index.html"
 *      target="_blank">XML Schema API</a>
 */
public class XSModelBuilder {

    public XSModelBuilder() {
    }

    /**
     * Builds an XSModel instance from a given Schema object.
     * 
     * @param xsd
     *            A Schema representing an XML Schema resource.
     * @param targetNamespace
     *            The target namespace of a schema (an absolute URI).
     * @return An XSModel object that encapsulates the schema components.
     */
    public static XSModel buildXMLSchemaModel(Schema xsd, String targetNamespace) {
        if ((null == targetNamespace) || targetNamespace.isEmpty()) {
            throw new IllegalArgumentException("targetNamespace is required.");
        }
        Grammar[] grammars = null;
        if (XSGrammarPoolContainer.class.isAssignableFrom(xsd.getClass())) {
            XSGrammarPoolContainer poolContainer = XSGrammarPoolContainer.class
                    .cast(xsd);
            XMLGrammarPool pool = poolContainer.getGrammarPool();
            grammars = pool
                    .retrieveInitialGrammarSet(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        }
        XSModel xsModel = null;
        for (int i = 0; i < grammars.length; i++) {
            XSGrammar xsGrammar = (XSGrammar) grammars[i];
            if (xsGrammar.getGrammarDescription().getNamespace()
                    .equals(targetNamespace)) {
                // root grammar includes all imported schema components
                xsModel = xsGrammar.toXSModel();
                break;
            }
        }
        return xsModel;
    }
}
