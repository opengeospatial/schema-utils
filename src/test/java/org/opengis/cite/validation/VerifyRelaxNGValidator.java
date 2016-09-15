package org.opengis.cite.validation;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.xerces.util.XMLCatalogResolver;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class VerifyRelaxNGValidator {

    private static final String TEST_RESOURCES = "src/test/resources/";
    private static Document doc;

    public VerifyRelaxNGValidator() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        doc = docBuilder.parse(new File(TEST_RESOURCES
                + "holiday-missingDay.xml"));
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void buildValidatorWithoutSchemaShouldFail() throws SAXException,
            IOException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("No schema URL supplied.");
        RelaxNGValidator iut = new RelaxNGValidator(null);
        Assert.assertNull(iut);
    }

    @Test
    public void buildValidatorUsingXmlGrammar() throws SAXException,
            IOException {
        URL schemaRef = getClass().getResource("/relax/publicHoliday.rng");
        RelaxNGValidator iut = new RelaxNGValidator(schemaRef);
        Assert.assertNotNull("Validator is null", iut);
    }

    @Test
    public void buildValidatorUsingCompactGrammar() throws SAXException,
            IOException {
        URL schemaRef = getClass().getResource("/relax/svrl.rnc");
        RelaxNGValidator iut = new RelaxNGValidator(schemaRef);
        Assert.assertNotNull("Validator is null", iut);
    }

    @Test
    public void validateHoliday_missingDay() throws Exception {
        URL schemaRef = getClass().getResource("/relax/publicHoliday.rng");
        RelaxNGValidator rngValidator = new RelaxNGValidator(schemaRef);
        rngValidator.validate(new StreamSource(new File(TEST_RESOURCES
                + "holiday-missingDay.xml")));
        ValidationErrorHandler err = rngValidator.getErrorHandler();
        Assert.assertEquals("Unexpected number of errors.", 1,
                err.getErrorCount());
    }

    @Test
    public void validateDOMSource() throws Exception {
        URL schemaRef = getClass().getResource("/relax/publicHoliday.rng");
        RelaxNGValidator rngValidator = new RelaxNGValidator(schemaRef);
        rngValidator.validate(new DOMSource(doc));
        ValidationErrorHandler err = rngValidator.getErrorHandler();
        Assert.assertEquals("Unexpected number of errors.", 1,
                err.getErrorCount());
    }

    @Test
    public void convertDOMSourceToStreamSource() {
        StreamSource src = RelaxNGValidator.toStreamSource(new DOMSource(doc));
        Assert.assertNotNull(src);
    }

    @Test
    public void validateHoliday_ok() throws Exception {
        URL schemaRef = getClass().getResource("/relax/publicHoliday.rng");
        RelaxNGValidator rngValidator = new RelaxNGValidator(schemaRef);
        rngValidator.validate(new StreamSource(new File(TEST_RESOURCES
                + "holiday.xml")));
        ValidationErrorHandler err = rngValidator.getErrorHandler();
        Assert.assertEquals("Unexpected number of errors.", 0,
                err.getErrorCount());
    }

    @Test
    public void validateSVRLReport() throws Exception {
        URL schemaRef = getClass().getResource("/relax/svrl.rnc");
        RelaxNGValidator iut = new RelaxNGValidator(schemaRef);
        iut.validate(new StreamSource(new File(TEST_RESOURCES + "svrl.xml")));
        ValidationErrorHandler err = iut.getErrorHandler();
        Assert.assertEquals("Unexpected number of errors.", 2,
                err.getErrorCount());
        String expectedErrMessage = "attribute \"document\" not allowed";
        String errMessages = err.toString();
        Assert.assertTrue("Expected error message to contain: "
                + expectedErrMessage, errMessages.contains(expectedErrMessage));
    }

    @Test(expected = SAXException.class)
    public void createValidatorUsingInvalidGrammar() throws SAXException,
            IOException {
        URL schemaRef = getClass().getResource("/sch/SoapFault.sch");
        RelaxNGValidator iut = new RelaxNGValidator(schemaRef);
        Assert.assertNull(iut);
    }

    @Test
    public void validateAgainstMergedCompactGrammar() throws SAXException,
            IOException {
        URL schemaRef = getClass().getResource("/relax/doc.rnc");
        RelaxNGValidator iut = new RelaxNGValidator(schemaRef);
        Assert.assertNotNull(iut);
        iut.validate(new StreamSource(new File(TEST_RESOURCES + "doc.xml")));
        ValidationErrorHandler err = iut.getErrorHandler();
        Assert.assertEquals("Unexpected number of errors.", 1,
                err.getErrorCount());
    }

    @Test
    public void createValidatorWithResolver() throws SAXException, IOException {
        URL catalog = getClass().getResource("/entity-catalog.xml");
        String[] catalogList = new String[] { catalog.toString() };
        XMLCatalogResolver resolver = new XMLCatalogResolver(catalogList);
        URL schemaURL = getClass().getResource("/relax/omcd2.rnc");
        RelaxNGValidator iut = new RelaxNGValidator(schemaURL, resolver);
        Assert.assertNotNull("Failed to construct validator.", iut);
    }

}
