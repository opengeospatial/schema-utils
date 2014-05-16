package org.opengis.cite.validation;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;

import static junit.framework.Assert.*;

import org.apache.xerces.xs.XSModel;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xml.sax.SAXException;

public class VerifyXSModelBuilder {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private static URL entityCatalog;

    public VerifyXSModelBuilder() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        entityCatalog = VerifyXSModelBuilder.class
                .getResource("/entity-catalog.xml");
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.newDocumentBuilder();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void buildModelFromSchemaURI() throws SAXException, IOException {
        String NS_URI = "http://www.example.com/IPO";
        URI schemaURI = URI.create(NS_URI);
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        Schema schema = xsdCompiler.compileXmlSchema(schemaURI);
        XSModel model = XSModelBuilder.buildXMLSchemaModel(schema, NS_URI);
        // XML Schema namespace and target namespace
        assertEquals("Unexpected number of namespaces", 2, model
                .getNamespaces().size());
        assertNotNull("Element decl not found: comment",
                model.getElementDeclaration("comment", NS_URI));
    }
}
