package org.opengis.cite.validation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.xerces.util.XMLCatalogResolver;
import org.xml.sax.SAXException;

/**
 * Provides various convenience methods for compiling W3C XML schemas and
 * creating a thread-safe {@link Schema} object: an immutable in-memory
 * representation of one or more grammars spanning one or more target
 * namespaces.
 * 
 * An OASIS entity catalog is used to resolve schema references. Note that, in
 * general, the top-level schema entity is never passed to the
 * LSResourceResolver object set on a SchemaFactory. However an instance of this
 * class will use the catalog to resolve the given URI reference, thus enabling
 * redirection to a local copy of the schema. If a matching entry (system id or
 * namespace name) is not found, a regular URI connection to the resource should
 * be established.
 * 
 * @see <a
 *      href="https://www.oasis-open.org/committees/download.php/14809/xml-catalogs.html"
 *      >XML Catalogs, v1.1</a>
 */
public class XmlSchemaCompiler {

    private static final Logger LOGR = Logger.getLogger(XmlSchemaCompiler.class
            .getPackage().getName());
    private XMLCatalogResolver resolver;
    private ValidationErrorHandler errHandler;

    /**
     * Constructs and configures an XmlSchemaCompiler to use an OASIS entity
     * catalog to resolve schema references.
     * 
     * @param catalog
     *            An absolute URL specifying the location of an OASIS entity
     *            catalog.
     */
    public XmlSchemaCompiler(URL catalog) {
        this.resolver = new XMLCatalogResolver();
        if (null != catalog) {
            String[] catalogList = new String[] { catalog.toString() };
            resolver.setCatalogList(catalogList);
        }
        this.errHandler = new ValidationErrorHandler();
    }

    /**
     * Returns the specialized error handler used to collect all errors
     * encountered while parsing schemas. The error handler is reset before a
     * new Schema object is constructed.
     * 
     * @return A ValidationErrorHandler object recording the status of the last
     *         attempt to compile a schema.
     */
    public ValidationErrorHandler getErrorHandler() {
        return errHandler;
    }

    /**
     * Compiles W3C XML Schema resources obtained from the specified sequence of
     * URI references. The resulting schema contains components from the
     * specified sources. An attempt will be made to first resolve a schema URI
     * using the entity catalog; if that fails a normal connection to the
     * resource will be established to retrieve it.
     * 
     * @param schemaURIs
     *            A sequence (or array) of absolute URIs representing schema
     *            locations.
     * @return A thread-safe, composite Schema object or {@code null} if
     *         compilation failed for any reason.
     * @throws SAXException
     *             If an error occurs while attempting to read a schema.
     * @throws IOException
     *             If an error occurs while attempting to retrieve a schema.
     */
    public Schema compileXmlSchema(URI... schemaURIs) throws SAXException,
            IOException {
        List<Source> sources = new ArrayList<Source>();
        for (URI schemaURI : schemaURIs) {
            String catalogEntry = lookupSchemaByURI(schemaURI);
            if (null != catalogEntry) {
                LOGR.log(Level.FINE, "Found catalog entry for schema: {0}",
                        catalogEntry);
                schemaURI = URI.create(catalogEntry);
            }
            File schemaFile = dereferenceURI(schemaURI);
            Source source = new StreamSource(schemaFile);
            // use system identifier to resolve relative URIs.
            source.setSystemId(schemaURI.toString());
            LOGR.log(Level.FINE, "Adding schema source: {0}",
                    source.getSystemId());
            sources.add(source);
        }
        return compileXmlSchema(sources.toArray(new Source[sources.size()]));
    }

    /**
     * Compiles a sequence of W3C XML Schema resources that may be used to
     * perform grammar-based validation of an XML instance. The resulting schema
     * contains components from the specified sources. Check the error handler
     * for any problems that were detected; these might arise even if the schema
     * was successfully compiled.
     * 
     * @param xsdSources
     *            A sequence (or array) of Source objects used to read the input
     *            schemas.
     * @return A thread-safe, composite Schema object or {@code null} if
     *         compilation failed for any reason.
     * @throws SAXException
     *             If an error occurs while attempting to compile the schema
     *             (e.g. it's malformed).
     */
    public Schema compileXmlSchema(Source... xsdSources) throws SAXException {
        LOGR.log(Level.FINE, "Compiling {0} schema sources.", xsdSources.length);
        SchemaFactory xsdFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        errHandler.reset();
        xsdFactory.setErrorHandler(this.errHandler);
        xsdFactory.setResourceResolver(this.resolver);
        Schema schema = xsdFactory.newSchema(xsdSources);
        return schema;
    }

    /**
     * Looks up a (schema) URI in an entity catalog by system identifier or
     * namespace name.
     * 
     * @param uri
     *            An absolute URI referring to an XML Schema resource.
     * 
     * @return A String denoting the location of a local copy, or null if no
     *         matching entry exists.
     */
    String lookupSchemaByURI(URI uri) {
        String schemaRef = null;
        try {
            schemaRef = resolver.resolveSystem(uri.toString());
            if (null == schemaRef) {
                schemaRef = resolver.resolveURI(uri.toString());
            }
        } catch (IOException e) {
            LOGR.log(Level.WARNING, "Error reading schema catalog. {0}",
                    e.getCause());
        }
        return schemaRef;
    }

    /**
     * Dereferences the given URI and writes the resulting entity (XML Schema)
     * to a local file. If a communications link cannot be established within 5
     * s, a {@code java.net.SocketTimeoutException} is raised.
     * 
     * @param schemaURI
     *            An absolute URI reference.
     * @return A File object containing the referenced entity; it is located in
     *         the default temporary-file directory.
     * @throws IOException
     *             If the resource cannot be retrieved.
     */
    File dereferenceURI(URI schemaURI) throws IOException {
        if ((null == schemaURI) || !schemaURI.isAbsolute()) {
            throw new IllegalArgumentException(
                    "Schema reference is not an absolute URI: " + schemaURI);
        }
        if (schemaURI.getScheme().equalsIgnoreCase("file")) {
            return new File(schemaURI);
        }
        LOGR.log(Level.FINE, "Attempting to retrieve schema from {0}",
                schemaURI);
        Path destFilePath = Files.createTempFile("schema-", ".xsd");
        Charset utf8 = Charset.forName("UTF-8");
        URLConnection conn = schemaURI.toURL().openConnection();
        conn.setConnectTimeout(5000);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                conn.getInputStream(), utf8));
                BufferedWriter writer = Files.newBufferedWriter(destFilePath,
                        utf8)) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
            }
        }
        return destFilePath.toFile();
    }
}
