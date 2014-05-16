package org.opengis.cite.validation;

import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class VerifySchematronValidator {

	private static final String TEST_RESOURCES = "src/test/resources/";
	private static final String SVRL_NS = "http://purl.oclc.org/dsdl/svrl";

	public VerifySchematronValidator() {
	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void buildValidatorWithoutSchemaShouldFail() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("No schema Source provided.");
		SchematronValidator iut = new SchematronValidator(null, null);
		Assert.assertNull(iut);
	}

	@Test
	public void buildValidator_goodSchema() throws Exception {
		Source schema = new StreamSource(getClass().getResourceAsStream(
				"/sch/SoapFault.sch"));
		SchematronValidator iut = new SchematronValidator(schema, null);
		Assert.assertNotNull("Validator is null", iut);
	}

	@Test
	public void buildValidator_invalidSchema() throws Exception {
		thrown.expect(Exception.class);
		thrown.expectMessage("does not work with schemas using the query language qb");
		URL schemaURL = getClass().getResource("/sch/Invalid.sch");
		Source schema = new StreamSource(schemaURL.toString());
		SchematronValidator iut = new SchematronValidator(schema, null);
		Assert.assertNull("Validator is not null", iut);
	}

	@Test
	public void validateEmptySoapBody() throws Exception {
		Source schemaSource = new StreamSource(getClass().getResourceAsStream(
				"/sch/SoapFault.sch"));
		SchematronValidator iut = new SchematronValidator(schemaSource, null);
		Source xmlSource = new StreamSource(getClass().getResourceAsStream(
				"/Soap-EmptyBody.xml"));
		Assert.assertNotNull("XML source is null", xmlSource);
		DOMResult result = iut.validate(xmlSource);
		Assert.assertEquals("Unexpected number of reported violations.", 2,
				iut.getRuleViolationCount());
		Document doc = (Document) result.getNode();
		Assert.assertEquals("Result doc node has unexpected [local name]",
				"schematron-output", doc.getDocumentElement().getLocalName());
	}

	@Test
	public void validateUsingSchemaInclusion() throws Exception {
		Source schemaSource = new StreamSource(new File(TEST_RESOURCES
				+ "sch/inclusion.sch"));
		SchematronValidator iut = new SchematronValidator(schemaSource, null);
		Source xmlSource = new StreamSource(getClass().getResourceAsStream(
				"/DocBook-Chapters.xml"));
		DOMResult result = iut.validate(xmlSource);
		Assert.assertEquals("Unexpected number of reported violations.", 3,
				iut.getRuleViolationCount());
		Document doc = (Document) result.getNode();
		Assert.assertNotNull("Result document is null.", doc);
	}

	@Test
	public void validateUsingSchemaInclusion_xinclude() throws Exception {
		Source schemaSource = new StreamSource(new File(TEST_RESOURCES
				+ "sch/inclusion-xinclude.sch"));
		SchematronValidator iut = new SchematronValidator(schemaSource, null);
		Source xmlSource = new StreamSource(getClass().getResourceAsStream(
				"/DocBook-Chapters.xml"));
		DOMResult result = iut.validate(xmlSource);
		Assert.assertEquals("Unexpected number of reported violations.", 3,
				iut.getRuleViolationCount());
		Assert.assertNotNull("Result document is null.", result.getNode());
	}

	@Test
	public void validateUsingAbstractTablePattern() throws Exception {
		Source schemaSource = new StreamSource(new File(TEST_RESOURCES
				+ "sch/abstract-table.sch"));
		SchematronValidator iut = new SchematronValidator(schemaSource, null);
		Source xmlSource = new StreamSource(getClass().getResourceAsStream(
				"/DocBook-Tables.xml"));
		DOMResult result = iut.validate(xmlSource);
		Assert.assertEquals("Unexpected number of reported violations.", 1,
				iut.getRuleViolationCount());
		Document doc = (Document) result.getNode();
		Assert.assertNotNull("Result document is null.", doc);
	}

	@Test
	public void reuseValidator() throws Exception {
		Source schemaSource = new StreamSource(getClass().getResourceAsStream(
				"/sch/SoapFault.sch"));
		SchematronValidator iut = new SchematronValidator(schemaSource, null);
		Source source1 = new StreamSource(getClass().getResourceAsStream(
				"/Soap-EmptyBody.xml"));
		DOMResult result = iut.validate(source1);
		Assert.assertNotNull("Result document is null.", result.getNode());
		Assert.assertEquals("Unexpected number of reported violations.", 2,
				iut.getRuleViolationCount());
		Source source2 = new StreamSource(getClass().getResourceAsStream(
				"/Soap-Fault.xml"));
		result = iut.validate(source2);
		Assert.assertNotNull("Result document is null.", result.getNode());
		Assert.assertEquals("Unexpected number of reported violations.", 0,
				iut.getRuleViolationCount());
	}

	@Test
	public void validateByPhase_EssentialCapabilitiesPhase() throws Exception {
		URL url = this.getClass().getResource("/sch/wfs2-capabilities.sch");
		Source schemaSource = new StreamSource(url.openStream(), url.toString());
		SchematronValidator iut = new SchematronValidator(schemaSource,
				"EssentialCapabilitiesPhase");
		Source xmlSource = new StreamSource(getClass().getResourceAsStream(
				"/wfs-capabilities-incomplete.xml"));
		DOMResult result = iut.validate(xmlSource);
		Document doc = (Document) result.getNode();
		Assert.assertNotNull("Result document is null.", doc);
		Assert.assertEquals("Unexpected number of rule violations.", 3,
				iut.getRuleViolationCount());

	}

	@Test
	public void validateByPhase_SimpleWFSPhase() throws Exception {
		URL url = this.getClass().getResource("/sch/wfs2-capabilities.sch");
		Source schemaSource = new StreamSource(url.openStream(), url.toString());
		SchematronValidator iut = new SchematronValidator(schemaSource,
				"SimpleWFSPhase");
		Source xmlSource = new StreamSource(getClass().getResourceAsStream(
				"/wfs-capabilities-incomplete.xml"));
		DOMResult result = iut.validate(xmlSource);
		Document doc = (Document) result.getNode();
		Assert.assertNotNull("Result document is null.", doc);
		Assert.assertEquals("Unexpected number of rule violations.", 18,
				iut.getRuleViolationCount());

	}

	@Test
	public void exceptionReport_MissingParamValue() throws Exception {
		URL url = this.getClass().getResource("/sch/ExceptionReport.sch");
		Source schemaSource = new StreamSource(url.openStream(), url.toString());
		SchematronValidator iut = new SchematronValidator(schemaSource,
				"MissingParameterValuePhase");
		Source xmlSource = new StreamSource(getClass().getResourceAsStream(
				"/ExceptionReport-MissingParameterValue.xml"));
		DOMResult result = iut.validate(xmlSource);
		Document doc = (Document) result.getNode();
		Assert.assertNotNull("Result document is null.", doc);
		Assert.assertEquals("Unexpected number of rule violations.", 3,
				iut.getRuleViolationCount());
	}

	@Test
	public void validationReportHasWarningFlag() throws Exception {
		URL url = this.getClass().getResource("/sch/gml-deprecated-3.2.1.sch");
		Source schemaSource = new StreamSource(url.openStream(), url.toString());
		SchematronValidator iut = new SchematronValidator(schemaSource, "#ALL");
		Assert.assertNotNull(iut);
		Source xmlSource = new StreamSource(getClass().getResourceAsStream(
				"/SimpleFeature-1.xml"));
		DOMResult result = iut.validate(xmlSource);
		Document doc = (Document) result.getNode();
		Assert.assertEquals("Unexpected number of constraint violations.", 1,
				iut.getRuleViolationCount());
		Element report = (Element) doc.getElementsByTagNameNS(SVRL_NS,
				"successful-report").item(0);
		Assert.assertEquals("Expected warning flag.", "warning",
				report.getAttribute("flag"));
	}

	void writeNode(Node node, OutputStream out)
			throws TransformerConfigurationException, TransformerException {
		Transformer idTransformer = TransformerFactory.newInstance()
				.newTransformer();
		Properties outProps = new Properties();
		outProps.setProperty("encoding", "UTF-8");
		outProps.setProperty("indent", "yes");
		idTransformer.setOutputProperties(outProps);
		idTransformer.transform(new DOMSource(node), new StreamResult(out));
	}
}
