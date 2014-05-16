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
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.xml.sax.SAXException;
import net.sf.saxon.Configuration;
import net.sf.saxon.FeatureKeys;
import net.sf.saxon.dom.NodeOverNodeInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
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
 * defined in an ISO Schematron (ISO 19757-3) schema. The results are presented
 * in an SVRL report. The schema may incorporate abstract patterns and
 * inclusions (sch:include and xi:include elements).
 * 
 * @see <a
 *      href="http://standards.iso.org/ittf/PubliclyAvailableStandards/c040833_ISO_IEC_19757-3_2006(E).zip"
 *      >ISO 19757-3:2006</a>
 */
public class SchematronValidator {

    private static final Logger LOGR = Logger
            .getLogger(SchematronValidator.class.getPackage().getName());
    public static final String ISO_SCHEMATRON_SVRL_NS = "http://purl.oclc.org/dsdl/svrl";
    private static String INCLUDE_XSLT = "iso_dsdl_include.xsl";
    private static String ABSTRACT_EXPAND_XSLT = "iso_abstract_expand.xsl";
    private static String SVRL_REPORT_XSLT = "iso_svrl_xslt2.xsl";
    private Processor processor;
    private XsltTransformer validator;
    private int totalRuleViolations = 0;

    /**
     * Construct a validator for the given Schematron schema and phase (pattern
     * set).
     * 
     * @param schema
     *            The ISO Schematron schema to use for validation.
     * @param phase
     *            The active phase (rule subset). If {@code null} the default
     *            phase is used.
     * @throws Exception
     *             If any error occurs while attempting to read or preprocess
     *             the schema.
     */
    public SchematronValidator(Source schema, String phase) throws Exception {
        if (schema == null) {
            throw new IllegalArgumentException("No schema Source provided.");
        }
        processor = new Processor(false);
        processor.setConfigurationProperty(FeatureKeys.RECOVERY_POLICY,
                Configuration.RECOVER_SILENTLY);
        XsltExecutable xslt = compileSchema(schema, phase);
        this.validator = xslt.load();
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
    public ValidationErrorHandler validateSchema(Source schema)
            throws IOException {
        RelaxNGValidator rngValidator;
        try {
            rngValidator = new RelaxNGValidator(getClass().getResource(
                    "rnc/schematron-grammar.rnc"));
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
            this.validator.setParameter(new QName(paramName),
                    new XdmAtomicValue(paramValue));
        }
    }

    /**
     * Validates the specified source XML document and returns the results in an
     * SVRL report.
     * 
     * @param xmlSource
     *            The XML resource to validate.
     * @return A DOMResult object containing the validation results.
     */
    public DOMResult validate(Source xmlSource) {
        if (xmlSource == null) {
            throw new IllegalArgumentException("Nothing to validate.");
        }
        this.totalRuleViolations = 0;
        XdmDestination results = new XdmDestination();
        try {
            validator.setSource(xmlSource);
            validator.setDestination(results);
            validator.transform();
        } catch (SaxonApiException e1) {
            LOGR.warning(e1.getMessage());
        }
        this.totalRuleViolations = countRuleViolations(results);
        if (LOGR.isLoggable(Level.FINER)) {
            LOGR.log(Level.FINER, "{0} Schematron rule violations found",
                    totalRuleViolations);
            writeResultsToTempFile(results);
        }
        NodeInfo nodeInfo = results.getXdmNode().getUnderlyingNode();
        // wrap document node
        return new DOMResult(NodeOverNodeInfo.wrap(nodeInfo));
    }

    /**
     * Creates an immutable representation of a compiled stylesheet that will
     * generate an SVRL representation of the validation results when run
     * against an instance document.
     * 
     * @param schema
     *            A Source to read a Schematron schema.
     * @param phase
     *            The phase (patterns sets) to check.
     * @return A compiled stylesheet ready for execution.
     * @throws Exception
     *             If the schema cannot be compiled for any reason.
     */
    final XsltExecutable compileSchema(Source schema, String phase)
            throws Exception {
        XsltCompiler compiler = processor.newXsltCompiler();
        CatalogManager manager = new CatalogManager(
                "org/opengis/cite/validation/CatalogManager.properties");
        compiler.setURIResolver(new CatalogResolver(manager));
        XsltExecutable includeXslt = compiler.compile(new StreamSource(
                SchematronValidator.class.getResourceAsStream(INCLUDE_XSLT)));
        XsltExecutable abstractXslt = compiler.compile(new StreamSource(
                SchematronValidator.class
                        .getResourceAsStream(ABSTRACT_EXPAND_XSLT)));
        XsltExecutable svrlXslt = compiler
                .compile(new StreamSource(SchematronValidator.class
                        .getResourceAsStream(SVRL_REPORT_XSLT)));
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
        if (phase != null && !phase.equals("")) {
            stage3Transformer.setParameter(new QName("phase"),
                    new XdmAtomicValue(phase));
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
        XsltExecutable compiledStylesheet = compiler.compile(chainResult
                .getXdmNode().asSource());
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
            LOGR.log(Level.FINER, "Dumped Schematron results to {0}",
                    temp.getAbsolutePath());
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
            XPathExecutable exe = xpath
                    .compile("count(//svrl:failed-assert) + count(//svrl:successful-report)");
            XPathSelector selector = exe.load();
            selector.setContextItem(results.getXdmNode());
            totalCount = (XdmAtomicValue) selector.evaluateSingle();
        } catch (SaxonApiException e) {
            LOGR.warning(e.getMessage());
        }
        return Integer.parseInt(totalCount.getValue().toString());
    }
}
