package org.opengis.cite.validation;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import net.sf.saxon.Configuration;
import net.sf.saxon.FeatureKeys;
import net.sf.saxon.dom.NodeOverNodeInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

/**
 * Verifies that the content of an XML resource satisfies the constraints
 * defined in an ISO Schematron (ISO 19757-3) schema. The schema may incorporate
 * abstract patterns and inclusions (sch:include and xi:include elements).
 *
 * @see <a href=
 *      "http://standards.iso.org/ittf/PubliclyAvailableStandards/c040833_ISO_IEC_19757-3_2006(E).zip"
 *      >ISO 19757-3:2006</a>
 */
public class SchematronValidator {

    private static final Logger LOGR = Logger.getLogger(SchematronValidator.class.getPackage().getName());
    public static final String ISO_SCHEMATRON_SVRL_NS = "http://purl.oclc.org/dsdl/svrl";
    private static final String INCLUDE_XSLT = "iso_dsdl_include.xsl";
    private static final String ABSTRACT_EXPAND_XSLT = "iso_abstract_expand.xsl";
    private static final String SVRL_REPORT_XSLT = "iso_svrl_xslt2.xsl";
    private Processor processor;
    private XsltTransformer transformer;
    private int totalRuleViolations = 0;

    /**
     * Constructs a validator for the given Schematron schema with the default
     * phase enabled. If no default phase is specified, then all patterns are
     * active. The results will be represented as a standard SVRL (Schematron
     * Validation Report Language) document.
     *
     * @param schema
     *            The Source that represents the schema.
     * @throws Exception
     *             If any error occurs while attempting to read or preprocess
     *             the schema.
     */
    public SchematronValidator(Source schema) throws Exception {
        this(schema, null);
    }

    /**
     * Constructs a validator for the given Schematron schema and phase (pattern
     * set).
     *
     * @param schema
     *            The Source that represents the schema.
     * @param phase
     *            The active phase; if null, the default phase is enabled (all
     *            patterns are active if no default is specified).
     * @throws Exception
     *             If any error occurs while attempting to read or preprocess
     *             the schema.
     */
    public SchematronValidator(Source schema, String phase) throws Exception {
        if (schema == null) {
            throw new IllegalArgumentException("No schema Source provided.");
        }
        processor = new Processor(false);
        processor.setConfigurationProperty(FeatureKeys.RECOVERY_POLICY, Configuration.RECOVER_SILENTLY);
        XsltExecutable xslt = compileSchema(schema, phase);
        this.transformer = xslt.load();
    }

    /**
     * Validates a Schematron schema against the official RELAX NG grammar (ISO
     * 19757-3, Annex A).
     *
     * @param schema
     *            A Source to obtain the schema from.
     * @return The ErrorHandler that received reported errors, if any.
     * @throws IOException
     *             If an error occurs while reading the schema.
     */
    public ValidationErrorHandler validateSchema(Source schema) throws IOException {
        RelaxNGValidator rngValidator;
        try {
            rngValidator = new RelaxNGValidator(getClass().getResource("rnc/schematron-grammar.rnc"));
            rngValidator.validate(schema);
        } catch (SAXException e) {
            throw new RuntimeException(e); // unlikely using bundled grammar
        }
        return rngValidator.getErrorHandler();
    }

    /**
     * Get the number of violations (failed assertions and successful reports)
     * for the validation episode.
     *
     * @return An {@code int} value equal to or greater than zero.
     */
    public int getRuleViolationCount() {
        return totalRuleViolations;
    }

    /**
     * Indicates the occurrence of any rule violations.
     *
     * @return {@code true} if any rule violations have been detected;
     *         {@code false} otherwise.
     */
    public boolean ruleViolationsDetected() {
        return (totalRuleViolations > 0 ? true : false);
    }

    /**
     * Sets parameters required to evaluate Schematron rules.
     *
     * @param params
     *            A {@literal Map<String,String>} object containing parameter
     *            names and values.
     */
    public void setParameters(Map<String, String> params) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String paramName = entry.getKey();
            String paramValue = entry.getValue();
            this.transformer.setParameter(new QName(paramName), new XdmAtomicValue(paramValue));
        }
    }

    /**
     * Validates the specified XML source document.
     *
     * @param xmlSource
     *            The XML resource to validate. A DOMSource must wrap a Document
     *            or an Element node.
     * @return A Result containing the validation results as a standard SVRL
     *         report.
     */
    public Result validate(Source xmlSource) {
        return validate(xmlSource, true);
    }

    /**
     * Validates the specified XML source document. The result is represented
     * either as an SVRL (Schematron Validation Report Language) report or as
     * plain text.
     *
     * @param xmlSource
     *            The XML resource to validate. A DOMSource must wrap a Document
     *            or an Element node.
     * @param svrlReport
     *            Produce an SVRL (XML) report; if false, the results are in
     *            plain text.
     * @return A Result (DOMResult or StreamResult) containing the validation
     *         results.
     */
    public Result validate(Source xmlSource, boolean svrlReport) {
        if (xmlSource == null) {
            throw new IllegalArgumentException("Nothing to validate.");
        }
        if (DOMSource.class.isInstance(xmlSource)) {
            // Saxon XsltTransformer will reject DOMSource wrapping an Element
            Node node = DOMSource.class.cast(xmlSource).getNode();
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Document doc = importElement((Element) node);
                xmlSource = new DOMSource(doc, xmlSource.getSystemId());
            }
        }
        this.totalRuleViolations = 0;
        XdmDestination results = new XdmDestination();
        try {
            transformer.setSource(xmlSource);
            transformer.setDestination(results);
            transformer.transform();
        } catch (SaxonApiException e1) {
            LOGR.warning(e1.getMessage());
        }
        this.totalRuleViolations = countRuleViolations(results);
        if (LOGR.isLoggable(Level.FINER)) {
            LOGR.log(Level.FINER, "{0} Schematron rule violations found", totalRuleViolations);
            writeResultsToTempFile(results);
        }
        NodeInfo nodeInfo = results.getXdmNode().getUnderlyingNode();
        Result result = null;
        if (svrlReport) {
            result = new DOMResult(NodeOverNodeInfo.wrap(nodeInfo));
        } else {
            result = generateTextResult(results.getXdmNode().asSource());
        }
        return result;
    }

    /**
     * Transforms a standard SVRL report to a plain text representation that
     * only includes rule violations (positive or negative assertion failures).
     * 
     * @param svrlSource
     *            The source for reading the SVRL report.
     * @return A StreamResult holding the plain text output, or null if the
     *         transformation failed.
     */
    StreamResult generateTextResult(Source svrlSource) {
        XsltCompiler compiler = processor.newXsltCompiler();
        StreamResult result = null;
        try {
            XsltExecutable exec = compiler.compile(new StreamSource(getClass().getResourceAsStream("svrl2text.xsl")));
            XsltTransformer transformer = exec.load();
            transformer.setSource(svrlSource);
            Serializer serializer = new Serializer();
            serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            serializer.setOutputStream(bos);
            transformer.setDestination(serializer);
            transformer.transform();
            result = new StreamResult(bos);
        } catch (SaxonApiException e) {
            LOGR.warning(e.getMessage());
        }
        return result;
    }

    /**
     * Creates an immutable representation of a compiled stylesheet that will
     * generate an SVRL representation of the validation results when run
     * against an instance document.
     *
     * @param schema
     *            A Source to read a Schematron schema.
     * @param phase
     *            The name of the active phase; if not specified, the default
     *            phase will be used (all patterns are active if no default is
     *            specified).
     * @return A compiled stylesheet ready for execution.
     * @throws Exception
     *             If the schema cannot be compiled for any reason.
     */
    final XsltExecutable compileSchema(Source schema, String phase) throws Exception {
        XsltCompiler compiler = processor.newXsltCompiler();
        CatalogManager manager = new CatalogManager("org/opengis/cite/validation/CatalogManager.properties");
        compiler.setURIResolver(new CatalogResolver(manager));
        XsltExecutable includeXslt = compiler
                .compile(new StreamSource(SchematronValidator.class.getResourceAsStream(INCLUDE_XSLT)));
        XsltExecutable abstractXslt = compiler
                .compile(new StreamSource(SchematronValidator.class.getResourceAsStream(ABSTRACT_EXPAND_XSLT)));
        XsltExecutable svrlXslt = compiler
                .compile(new StreamSource(SchematronValidator.class.getResourceAsStream(SVRL_REPORT_XSLT)));
        // Set up pre-processing chain to enable:
        // 1. Inclusions
        // 2. Abstract patterns
        // 3. SVRL report
        XsltTransformer stage1Transformer = includeXslt.load();
        XsltTransformer stage2Transformer = abstractXslt.load();
        XsltTransformer stage3Transformer = svrlXslt.load();
        stage1Transformer.setSource(schema);
        stage1Transformer.setDestination(stage2Transformer);
        stage2Transformer.setDestination(stage3Transformer);
        XdmDestination chainResult = new XdmDestination();
        stage3Transformer.setDestination(chainResult);
        if (null != phase && !phase.isEmpty()) {
            stage3Transformer.setParameter(new QName("phase"), new XdmAtomicValue(phase));
        }
        // redirect messages written to System.err by default message emitter
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream console = System.err;
        try {
            System.setErr(new PrintStream(baos));
            stage1Transformer.transform();
        } catch (SaxonApiException e) {
            throw new Exception(baos.toString() + e.getMessage(), e.getCause());
        } finally {
            System.setErr(console);
        }
        XsltExecutable compiledStylesheet = compiler.compile(chainResult.getXdmNode().asSource());
        return compiledStylesheet;
    }

    private void writeResultsToTempFile(XdmDestination xdmResult) {
        File temp = null;
        try {
            temp = File.createTempFile("SchematronValidator-dump-", ".xml");
            BufferedWriter out = new BufferedWriter(new FileWriter(temp));
            out.write(xdmResult.getXdmNode().toString());
            out.close();
        } catch (IOException e) {
            LOGR.warning(e.getMessage());
        }
        if (temp.exists()) {
            LOGR.log(Level.FINER, "Dumped Schematron results to {0}", temp.getAbsolutePath());
        }
    }

    /**
     * Counts all rule violations: failed asserts and successful reports).
     *
     * @param results
     *            The validation results (svrl:schematron-output).
     * @return An integer value.
     */
    private int countRuleViolations(XdmDestination results) {
        XPathCompiler xpath = processor.newXPathCompiler();
        xpath.declareNamespace("svrl", ISO_SCHEMATRON_SVRL_NS);
        XdmAtomicValue totalCount = null;
        try {
            XPathExecutable exe = xpath.compile("count(//svrl:failed-assert) + count(//svrl:successful-report)");
            XPathSelector selector = exe.load();
            selector.setContextItem(results.getXdmNode());
            totalCount = (XdmAtomicValue) selector.evaluateSingle();
        } catch (SaxonApiException e) {
            LOGR.warning(e.getMessage());
        }
        return Integer.parseInt(totalCount.getValue().toString());
    }

    /**
     * Creates a DOM Document with the given Element as the document element. A
     * deep copy of the element is imported--the source element is not altered.
     *
     * @param elem
     *            An Element node.
     * @return A Document node.
     */
    Document importElement(Element elem) {
        DocumentBuilder docBuilder = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            docBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            LOGR.log(Level.WARNING, null, ex);
        }
        Document newDoc = docBuilder.newDocument();
        Node newNode = newDoc.importNode(elem, true);
        newDoc.appendChild(newNode);
        return newDoc;
    }
}
