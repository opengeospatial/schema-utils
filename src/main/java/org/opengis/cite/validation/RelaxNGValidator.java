package org.opengis.cite.validation;

import com.thaiopensource.validation.Constants;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

/**
 * Validates an XML resource with respect to a RELAX NG (ISO 19757-2) schema.
 * The schema may be represented using either the XML or the compact syntax.
 * 
 * @see <a
 *      href="http://standards.iso.org/ittf/PubliclyAvailableStandards/c052348_ISO_IEC_19757-2_2008(E).zip"
 *      >ISO 19757-2:2008</a>
 */
public final class RelaxNGValidator {

    private static final Logger LOGR = Logger.getLogger(RelaxNGValidator.class
            .getPackage().getName());
    private static final String RELAXNG_COMPACT_FACTORY = "com.thaiopensource.relaxng.jaxp.CompactSyntaxSchemaFactory";
    private static final String RELAXNG_XML_FACTORY = "com.thaiopensource.relaxng.jaxp.XMLSyntaxSchemaFactory";
    private Validator validator;

    /**
     * Constructs a validator using the supplied schema reference and a default
     * resource resolver (which will suffice for commonly used URL protocols
     * such as the {@code file} and {@code http} schemes).
     * 
     * @param rngSchemaURL
     *            An absolute URL that refers to a RELAX NG grammar.
     * @throws IOException
     *             If the schema cannot be accessed.
     * @throws SAXException
     *             If parsing of the schema fails for any reason.
     */
    public RelaxNGValidator(URL rngSchemaURL) throws SAXException, IOException {
        this(rngSchemaURL, null);
    }

    /**
     * Constructs a validator using the supplied schema reference and resource
     * resolver. The resolver customizes resource resolution when parsing
     * schemas; resolving schema references in an entity catalog is a common use
     * case.
     * 
     * @param rngSchemaURL
     *            A URL that refers to a RELAX NG grammar.
     * @param resolver
     *            The resolver used to locate external resources (included
     *            grammars) while parsing schemas.
     * @throws SAXException
     *             If the schema cannot be accessed (e.g. URL resolution fails).
     * @throws IOException
     *             If parsing of the schema fails for any reason (e.g. it's not
     *             a valid Relax NG grammar).
     */
    public RelaxNGValidator(URL rngSchemaURL, LSResourceResolver resolver)
            throws SAXException, IOException {
        if (null == rngSchemaURL) {
            throw new IllegalArgumentException("No schema URL supplied.");
        }
        Schema schema = createRelaxNGSchema(rngSchemaURL, resolver);
        validator = schema.newValidator();
    }

    /**
     * Returns the error handler.
     * 
     * @return A ValidationErrorHandler that contains details about the most
     *         recent validation episode.
     */
    public ValidationErrorHandler getErrorHandler() {
        return (ValidationErrorHandler) validator.getErrorHandler();
    }

    /**
     * Validates the given XML resource against the schema known to this
     * validator. Constraint violations are accumulated by the error handler.
     * 
     * @param source
     *            The Source to be read. It must not be null.
     * @throws SAXException
     *             if a fatal error occurs while parsing the source.
     * @throws IOException
     *             if an error occurs while reading the source.
     */
    public void validate(Source source) throws SAXException, IOException {
        if (null == source) {
            throw new NullPointerException("source is null.");
        }
        if (DOMSource.class.isInstance(source)) {
            source = toStreamSource((DOMSource) source);
        }
        validator.setErrorHandler(new ValidationErrorHandler());
        validator.validate(source);
    }

    /**
     * Creates a Schema object from the supplied URL reference. A resource
     * resolver may be supplied if entity resolution must be customized (e.g.
     * using an OASIS entity catalog).
     * 
     * @param schemaURL
     *            A URL that specifies the location of a Relax NG grammar (XML
     *            or compact syntax).
     * @param resolver
     *            The resolver used to locate external resources while parsing
     *            schemas.
     * @return A Schema object, or <code>null</code> if one could not be
     *         constructed.
     * @throws IOException
     *             If the grammar cannot be accessed.
     * @throws SAXException
     *             If the grammar cannot be parsed.
     */
    Schema createRelaxNGSchema(URL schemaURL, LSResourceResolver resolver)
            throws SAXException, IOException {
        if (LOGR.isLoggable(Level.FINE)) {
            LOGR.fine("Attempting to create RELAX NG Schema object from schemaURL = "
                    + schemaURL.toString());
        }
        InputStream schemaStream = schemaURL.openStream();
        BufferedInputStream bufStream = new BufferedInputStream(schemaStream,
                8 * 1024);
        boolean isXMLSyntax = isXMLStream(bufStream);
        SchemaFactory schemaFactory;
        if (isXMLSyntax) {
            schemaFactory = SchemaFactory.newInstance(
                    Constants.RELAXNG_XML_URI, RELAXNG_XML_FACTORY, null);
        } else {
            schemaFactory = SchemaFactory.newInstance(
                    Constants.RELAXNG_COMPACT_URI, RELAXNG_COMPACT_FACTORY,
                    null);
        }
        if (null != resolver) {
            schemaFactory.setResourceResolver(resolver);
        }
        Schema schema = null;
        try {
            Source source = new StreamSource(bufStream);
            source.setSystemId(schemaURL.toURI().toString());
            schema = schemaFactory.newSchema(source);
        } catch (URISyntaxException e) {
            LOGR.log(Level.INFO, null, e);
        } finally {
            try {
                // also closes underlying stream
                bufStream.close();
            } catch (IOException iox) {
                LOGR.log(Level.INFO, null, iox);
            }
        }
        return schema;
    }

    /**
     * A utility method that attempts to determine if the given stream contains
     * XML content. The stream is always reset to the beginning so it can be
     * reused.
     * 
     * @param bufStream
     *            The BufferedInputStream to read.
     * @return true if the stream contains XML data; false otherwise.
     */
    boolean isXMLStream(BufferedInputStream bufStream) {
        bufStream.mark(8 * 1024);
        boolean isXML = true;
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
            XMLStreamReader reader = factory.createXMLStreamReader(bufStream);
            // Now in START_DOCUMENT state. Seek document element.
            reader.nextTag();
        } catch (XMLStreamException xse) {
            isXML = false;
        } finally {
            try {
                bufStream.reset();
            } catch (IOException x) {
                LOGR.log(Level.INFO, "Error resetting BufferedInputStream", x);
            }
        }
        return isXML;
    }

    /**
     * Converts a DOMSource object to a StreamSource. The underlying Jing
     * Validator implementation doesn't support DOMSource inputs.
     * 
     * @param domSource
     *            A DOMSource instance.
     * @return A StreamSource object for reading the content of the original DOM
     *         node.
     */
    static StreamSource toStreamSource(DOMSource domSource) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(baos);
        try {
            // Create identity transformer
            Transformer idt = TransformerFactory.newInstance().newTransformer();
            idt.transform(domSource, result);
        } catch (TransformerException tex) {
            LOGR.log(Level.WARNING, "Error serializing DOMSource.", tex);
        }
        return new StreamSource(new ByteArrayInputStream(baos.toByteArray()));
    }
}
