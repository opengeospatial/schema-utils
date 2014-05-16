package org.opengis.cite.validation;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class VerifyXmlSchemaCompiler {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private static final String TEST_RESOURCES = "src/test/resources/";
    private static DocumentBuilder docBuilder;
    private static URL entityCatalog;

    public VerifyXmlSchemaCompiler() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        entityCatalog = VerifyXmlSchemaCompiler.class
                .getResource("/entity-catalog.xml");
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        docBuilder = dbf.newDocumentBuilder();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void schemaFromFileURI() throws SAXException, IOException {
        File schemaFile = new File(TEST_RESOURCES + "xsd/ipo.xsd");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        Schema schema = xsdCompiler.compileXmlSchema(schemaFile.toURI());
        assertNotNull("Schema is null", schema);
        ValidationErrorHandler err = xsdCompiler.getErrorHandler();
        System.out.println(err.toString());
        assertFalse("Compilation errors were reported.", err.errorsDetected());

    }

    @Test
    public void incompleteSchema() throws SAXException, IOException {
        File schemaFile = new File(TEST_RESOURCES
                + "xsd/shiporder-incomplete.xsd");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        Schema schema = xsdCompiler.compileXmlSchema(schemaFile.toURI());
        assertNotNull("Schema is null", schema);
        ValidationErrorHandler err = xsdCompiler.getErrorHandler();
        assertEquals("Unexpected number of compilation errors", 1,
                err.getErrorCount());
    }

    @Test
    public void invalidSchema() throws SAXException, IOException {
        File schemaFile = new File(TEST_RESOURCES + "xsd/shiporder-invalid.xsd");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        Schema schema = xsdCompiler.compileXmlSchema(schemaFile.toURI());
        assertNotNull("Schema is null", schema);
        ValidationErrorHandler err = xsdCompiler.getErrorHandler();
        assertEquals("Unexpected number of compilation errors", 2,
                err.getErrorCount());
    }

    @Test
    public void malformedXMLSchema() throws SAXException, IOException {
        thrown.expect(SAXException.class);
        thrown.expectMessage("must be terminated by the matching end-tag");
        File schemaFile = new File(TEST_RESOURCES + "xsd/malformed.xsd");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        Schema schema = xsdCompiler.compileXmlSchema(schemaFile.toURI());
        assertNull("Schema should be null (malformed).", schema);
    }

    @Test
    public void notXMLSchema() throws SAXException, IOException {
        thrown.expect(SAXException.class);
        thrown.expectMessage("Content is not allowed in prolog");
        File schemaFile = new File(TEST_RESOURCES + "relax/svrl.rnc");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        Schema schema = xsdCompiler.compileXmlSchema(schemaFile.toURI());
        assertNull("Schema should be null (malformed).", schema);
    }

    @Test
    public void schemaInCatalog_systemId() throws SAXException, IOException {
        URI schemaURI = URI.create("http://www.example.net/shiporder.xsd");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        Schema schema = xsdCompiler.compileXmlSchema(schemaURI);
        assertNotNull("Schema is null", schema);
    }

    @Test
    public void schemaInCatalog_namespaceURI() throws SAXException, IOException {
        URI schemaURI = URI.create("http://www.example.com/IPO");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        Schema schema = xsdCompiler.compileXmlSchema(schemaURI);
        assertNotNull("Schema is null", schema);
        ValidationErrorHandler err = xsdCompiler.getErrorHandler();
        assertFalse("Compilation errors were reported.", err.errorsDetected());
    }

    @Test
    public void schemaFromDOMSource() throws SAXException, IOException {
        File schemaFile = new File(TEST_RESOURCES + "xsd/ipo.xsd");
        Document doc = docBuilder.parse(schemaFile);
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        Schema schema = xsdCompiler.compileXmlSchema(new DOMSource(doc, doc
                .getDocumentURI()));
        assertNotNull("Schema is null", schema);
        ValidationErrorHandler err = xsdCompiler.getErrorHandler();
        assertFalse("Compilation errors were reported.", err.errorsDetected());
    }

    @Test
    @Ignore("Passes, but avoid establishing a network connection")
    public void dereferenceURI_XLink11Schema() throws IOException {
        URI schemaURI = URI.create("http://www.w3.org/1999/xlink.xsd");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        File schemaFile = xsdCompiler.dereferenceURI(schemaURI);
        assertTrue("schemaFile does not exist.", schemaFile.exists());
        assertTrue("schemaFile is empty.", schemaFile.length() > 0);
        schemaFile.delete();
    }

    @Test
    @Ignore("Passes, but avoid establishing a network connection")
    public void compileSchema_includeHasRelativeURI() throws SAXException,
            IOException {
        URI schemaURI = URI
                .create("http://www.oracle.com/webfolder/technetwork/jsc/xml/ns/javaee/javaee_7.xsd");
        XmlSchemaCompiler xsdCompiler = new XmlSchemaCompiler(entityCatalog);
        Schema schema = xsdCompiler.compileXmlSchema(schemaURI);
        assertNotNull("Schema is null", schema);
        ValidationErrorHandler err = xsdCompiler.getErrorHandler();
        assertFalse("Compilation errors were reported.", err.errorsDetected());
    }
}
