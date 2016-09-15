package org.opengis.cite.validation;

import static org.junit.Assert.*;

import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class VerifySchematronValidator {

    private static final String TEST_RESOURCES = "src/test/resources/";
    private static final String SVRL_NS = "http://purl.oclc.org/dsdl/svrl";
    private static final String OWS_NS = "http://www.opengis.net/ows/1.1";
    private static DocumentBuilder docBuilder;

    public VerifySchematronValidator() {
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUpClass() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        docBuilder = dbf.newDocumentBuilder();
    }

    @Test
    public void buildValidatorWithoutSchemaShouldFail() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("No schema Source provided.");
        SchematronValidator iut = new SchematronValidator(null);
        assertNull(iut);
    }

    @Test
    public void buildValidator_goodSchema() throws Exception {
        Source schema = new StreamSource(getClass().getResourceAsStream("/sch/SoapFault.sch"));
        SchematronValidator iut = new SchematronValidator(schema);
        assertNotNull("Validator is null", iut);
    }

    @Test
    public void buildValidator_invalidSchema() throws Exception {
        thrown.expect(Exception.class);
        thrown.expectMessage("does not work with schemas using the query language qb");
        URL schemaURL = getClass().getResource("/sch/Invalid.sch");
        Source schema = new StreamSource(schemaURL.toString());
        SchematronValidator iut = new SchematronValidator(schema);
        assertNull("Validator is not null", iut);
    }

    @Test
    public void validateEmptySoapBody() throws Exception {
        Source schemaSource = new StreamSource(getClass().getResourceAsStream("/sch/SoapFault.sch"));
        SchematronValidator iut = new SchematronValidator(schemaSource);
        Source xmlSource = new StreamSource(getClass().getResourceAsStream("/Soap-EmptyBody.xml"));
        assertNotNull("XML source is null", xmlSource);
        DOMResult result = (DOMResult) iut.validate(xmlSource);
        Document doc = (Document) result.getNode();
        assertEquals("Unexpected number of reported violations.", 2, iut.getRuleViolationCount());
        assertEquals("Result doc node has unexpected [local name]", "schematron-output",
                doc.getDocumentElement().getLocalName());
    }

    @Test
    public void validateUsingSchemaInclusion() throws Exception {
        Source schemaSource = new StreamSource(new File(TEST_RESOURCES + "sch/inclusion.sch"));
        SchematronValidator iut = new SchematronValidator(schemaSource);
        Source xmlSource = new StreamSource(getClass().getResourceAsStream("/DocBook-Chapters.xml"));
        DOMResult result = (DOMResult) iut.validate(xmlSource);
        assertEquals("Unexpected number of reported violations.", 3, iut.getRuleViolationCount());
        Document doc = (Document) result.getNode();
        assertNotNull("Result document is null.", doc);
    }

    @Test
    public void validateUsingSchemaInclusion_xinclude() throws Exception {
        Source schemaSource = new StreamSource(new File(TEST_RESOURCES + "sch/inclusion-xinclude.sch"));
        SchematronValidator iut = new SchematronValidator(schemaSource);
        Source xmlSource = new StreamSource(getClass().getResourceAsStream("/DocBook-Chapters.xml"));
        DOMResult result = (DOMResult) iut.validate(xmlSource);
        assertEquals("Unexpected number of reported violations.", 3, iut.getRuleViolationCount());
        assertNotNull("Result document is null.", result.getNode());
    }

    @Test
    public void validateUsingAbstractTablePattern() throws Exception {
        Source schemaSource = new StreamSource(new File(TEST_RESOURCES + "sch/abstract-table.sch"));
        SchematronValidator iut = new SchematronValidator(schemaSource);
        Source xmlSource = new StreamSource(getClass().getResourceAsStream("/DocBook-Tables.xml"));
        DOMResult result = (DOMResult) iut.validate(xmlSource);
        assertEquals("Unexpected number of reported violations.", 1, iut.getRuleViolationCount());
        Document doc = (Document) result.getNode();
        assertNotNull("Result document is null.", doc);
    }

    @Test
    public void reuseValidator() throws Exception {
        Source schemaSource = new StreamSource(getClass().getResourceAsStream("/sch/SoapFault.sch"));
        SchematronValidator iut = new SchematronValidator(schemaSource);
        Source source1 = new StreamSource(getClass().getResourceAsStream("/Soap-EmptyBody.xml"));
        DOMResult result = (DOMResult) iut.validate(source1);
        assertNotNull("Result document is null.", result.getNode());
        assertEquals("Unexpected number of reported violations.", 2, iut.getRuleViolationCount());
        Source source2 = new StreamSource(getClass().getResourceAsStream("/Soap-Fault.xml"));
        result = (DOMResult) iut.validate(source2);
        assertNotNull("Result document is null.", result.getNode());
        assertEquals("Unexpected number of reported violations.", 0, iut.getRuleViolationCount());
    }

    @Test
    public void validateByPhase_EssentialCapabilitiesPhase() throws Exception {
        URL url = this.getClass().getResource("/sch/wfs2-capabilities.sch");
        Source schemaSource = new StreamSource(url.openStream(), url.toString());
        SchematronValidator iut = new SchematronValidator(schemaSource, "EssentialCapabilitiesPhase");
        Source xmlSource = new StreamSource(getClass().getResourceAsStream("/wfs-capabilities-incomplete.xml"));
        DOMResult result = (DOMResult) iut.validate(xmlSource);
        Document doc = (Document) result.getNode();
        assertNotNull("Result document is null.", doc);
        assertEquals("Unexpected number of rule violations.", 3, iut.getRuleViolationCount());

    }

    @Test
    public void validateByPhase_SimpleWFSPhase() throws Exception {
        URL url = this.getClass().getResource("/sch/wfs2-capabilities.sch");
        Source schemaSource = new StreamSource(url.openStream(), url.toString());
        SchematronValidator iut = new SchematronValidator(schemaSource, "SimpleWFSPhase");
        Source xmlSource = new StreamSource(getClass().getResourceAsStream("/wfs-capabilities-incomplete.xml"));
        DOMResult result = (DOMResult) iut.validate(xmlSource);
        Document doc = (Document) result.getNode();
        assertNotNull("Result document is null.", doc);
        assertEquals("Unexpected number of rule violations.", 18, iut.getRuleViolationCount());

    }

    @Test
    public void exceptionReport_MissingParamValue() throws Exception {
        URL url = this.getClass().getResource("/sch/ExceptionReport.sch");
        Source schemaSource = new StreamSource(url.openStream(), url.toString());
        SchematronValidator iut = new SchematronValidator(schemaSource, "MissingParameterValuePhase");
        Source xmlSource = new StreamSource(
                getClass().getResourceAsStream("/ExceptionReport-MissingParameterValue.xml"));
        DOMResult result = (DOMResult) iut.validate(xmlSource);
        Document doc = (Document) result.getNode();
        assertNotNull("Result document is null.", doc);
        assertEquals("Unexpected number of rule violations.", 3, iut.getRuleViolationCount());
    }

    @Test
    public void validationReportHasWarningFlag() throws Exception {
        URL url = this.getClass().getResource("/sch/gml-deprecated-3.2.1.sch");
        Source schemaSource = new StreamSource(url.openStream(), url.toString());
        SchematronValidator iut = new SchematronValidator(schemaSource, "#ALL");
        assertNotNull(iut);
        Source xmlSource = new StreamSource(getClass().getResourceAsStream("/SimpleFeature-1.xml"));
        DOMResult result = (DOMResult) iut.validate(xmlSource);
        Document doc = (Document) result.getNode();
        assertEquals("Unexpected number of constraint violations.", 1, iut.getRuleViolationCount());
        Element report = (Element) doc.getElementsByTagNameNS(SVRL_NS, "successful-report").item(0);
        assertEquals("Expected warning flag.", "warning", report.getAttribute("flag"));
    }

    @Test
    public void validateDocumentFragment() throws Exception {
        URL url = this.getClass().getResource("/sch/wfs2-capabilities.sch");
        Source schemaSource = new StreamSource(url.openStream(), url.toString());
        SchematronValidator iut = new SchematronValidator(schemaSource, "SimpleWFSPhase");
        Document doc = docBuilder.parse(getClass().getResourceAsStream("/wfs-capabilities-incomplete.xml"));
        Element opsMetadata = (Element) doc.getElementsByTagNameNS(OWS_NS, "OperationsMetadata").item(0);
        DOMSource domSource = new DOMSource(opsMetadata);
        DOMResult result = (DOMResult) iut.validate(domSource);
        Document report = (Document) result.getNode();
        assertNotNull("Report document is null.", report);
        assertEquals("Unexpected number of rule violations.", 18, iut.getRuleViolationCount());
    }

    @Test
    public void plainTextReport() throws Exception {
        Source schemaSource = new StreamSource(getClass().getResourceAsStream("/sch/SoapFault.sch"));
        SchematronValidator iut = new SchematronValidator(schemaSource, null);
        Source xmlSource = new StreamSource(getClass().getResourceAsStream("/Soap-EmptyBody.xml"));
        assertNotNull("XML source is null", xmlSource);
        Result result = iut.validate(xmlSource, false);
        assertTrue("Expected StreamResult", result instanceof StreamResult);
        assertEquals("Unexpected number of rule violations.", 2, iut.getRuleViolationCount());
    }

    void writeResult(Result result, OutputStream out) throws TransformerConfigurationException, TransformerException {
        Transformer idTransformer = TransformerFactory.newInstance().newTransformer();
        Properties outProps = new Properties();
        outProps.setProperty("encoding", "UTF-8");
        outProps.setProperty("indent", "yes");
        idTransformer.setOutputProperties(outProps);
        Source source = null;
        if (result instanceof DOMResult) {
            source = new DOMSource(DOMResult.class.cast(result).getNode());
            idTransformer.transform(source, new StreamResult(out));
        } else if (result instanceof StreamResult) {
            OutputStream os = StreamResult.class.cast(result).getOutputStream();
            System.out.print(os.toString());
        } else {
            throw new IllegalArgumentException("Unsupported Result type:" + result.getClass());
        }
    }
}
