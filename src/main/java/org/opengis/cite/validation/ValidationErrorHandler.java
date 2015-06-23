package org.opengis.cite.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMLocator;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * A SAX and DOM error handler that collects validation errors raised while
 * verifying the structure and content of XML entities.
 *
 */
public class ValidationErrorHandler implements ErrorHandler, DOMErrorHandler,
        Iterable<ValidationError> {

    /**
     * Temporary storage for serializing error information.
     */
    private StringBuffer buf;
    /**
     * An absolute URI identifying the relevant schema language, if any.
     */
    private String schemaLanguage;
    /**
     * Collection of reported validation errors.
     */
    private List<ValidationError> errors;

    public ValidationErrorHandler() {
        this.buf = new StringBuffer();
        this.errors = new ArrayList<ValidationError>();
    }

    /**
     * Indicates whether any validation errors have been detected.
     *
     * @return true if validation errors were detected; false otherwise.
     */
    public boolean errorsDetected() {
        return !errors.isEmpty();
    }

    /**
     * Reports the number of errors detected during a validation episode.
     *
     * @return the number of errors.
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Receives notification of a warning.
     *
     * @param spex a non-error condition reported by the parser
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    public void warning(SAXParseException spex) {

        addSAXError(ErrorSeverity.WARNING, spex);
    }

    /**
     * Receives notification of a recoverable error. Typically this indicates
     * that a validation constraint has been violated.
     *
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     *
     * @param spex a non-fatal error condition reported by the parser
     */
    public void error(SAXParseException spex) {

        addSAXError(ErrorSeverity.ERROR, spex);
    }

    /**
     * Receives notification of a non-recoverable error, such as non-XML
     * content, XML that is not well-formed, or an incorrect encoding
     * declaration.
     *
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     * @see <a href="http://www.w3.org/TR/xml/">XML 1.0 (Fourth Edition)</a>
     *
     * @param spex A fatal error condition reported by the parser.
     */
    public void fatalError(SAXParseException spex) {

        addSAXError(ErrorSeverity.CRITICAL, spex);
    }

    /**
     * Adds a validation error based on information provided by a reported
     * {@code SAXParseException}.
     *
     * @param severity The severity of the error.
     * @param spex The <code>SAXParseException</code> raised while validating
     * the XML source.
     */
    private void addSAXError(ErrorSeverity severity, SAXParseException spex) {

        ValidationError error = new ValidationError(severity,
                spex.getMessage(), spex.getLineNumber(),
                spex.getColumnNumber(), null);
        errors.add(error);
    }

    /**
     * Adds a validation error based on information provided by a reported
     * {@code DOMError}.
     *
     * @param severity The severity of the error.
     * @param err The <code>DOMError</code> raised while validating the XML
     * source document.
     */
    private void addDOMError(ErrorSeverity severity, String msg, DOMLocator loc) {
        ValidationError error = new ValidationError(severity, msg,
                loc.getLineNumber(), loc.getColumnNumber(), "#");
        errors.add(error);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.w3c.dom.DOMErrorHandler#handleError(org.w3c.dom.DOMError)
     */
    public boolean handleError(DOMError error) {
        switch (error.getSeverity()) {
            case DOMError.SEVERITY_WARNING:
                addDOMError(ErrorSeverity.WARNING, error.getMessage(),
                        error.getLocation());
                break;
            case DOMError.SEVERITY_ERROR:
                addDOMError(ErrorSeverity.ERROR, error.getMessage(),
                        error.getLocation());
                break;
            case DOMError.SEVERITY_FATAL_ERROR:
                addDOMError(ErrorSeverity.CRITICAL, error.getMessage(),
                        error.getLocation());
                break;
        }
        return (error.getSeverity() != DOMError.SEVERITY_FATAL_ERROR);
    }

    /**
     * Adds a general validation error.
     *
     * @param severity The severity of the error.
     * @param msg The error message.
     * @param locator An <code>ErrorLocator</code> instance that provides
     * information regarding the location of the error.
     */
    public void addError(ErrorSeverity severity, String msg,
            ErrorLocator locator) {
        addError(severity, msg, null, locator);
    }

    /**
     * Adds a general validation error with diagnostic information.
     *
     * @param severity The severity of the error.
     * @param msg The error message.
     * @param diag Supplementary diagnostic information about the error.
     * @param locator An <code>ErrorLocator</code> instance that provides
     * information regarding the location of the error.
     */
    public void addError(ErrorSeverity severity, String msg, String diag,
            ErrorLocator locator) {
        ValidationError error = new ValidationError(severity, msg, diag,
                locator);
        errors.add(error);
    }

    /**
     * Adds the given collection of validation errors to this handler.
     *
     * @param errors A collection of <code>ValidationError</code> objects, each
     * of which encapsulates information about some constraint violation (which
     * are not necessarily related).
     */
    public void addErrors(Collection<ValidationError> errors) {
        this.errors.addAll(errors);
    }

    /**
     * Gets the errors reported to this handler.
     *
     * @return A list containing error descriptions.
     */
    public List<ValidationError> getErrors() {
        return this.errors;
    }

    /**
     * Returns a concatenation of the summaries of all received errors.
     *
     * @return A consolidated error message.
     */
    public String toString() {

        buf.setLength(0);
        for (ValidationError err : errors) {
            buf.append(err.toString());
        }
        return buf.toString();
    }

    /**
     * <p>
     * Returns a simple XML representation of all received errors. The structure
     * is shown below:
     * </p>
     *
     * <pre>
     *   &lt;errors xmlns='http://www.galdosinc.com/arbitron'&gt;
     *     &lt;error /&gt; 1..*
     *   &lt;/errors&gt;
     * </pre>
     *
     * @return A String containing an XML summary (in no namespace) of all error
     * descriptions.
     */
    public String toXml() {

        buf.setLength(0);
        buf.append("\n<errors xmlns='");
        buf.append("http://cite.opengeospatial.org/").append("'");
        if (null != this.schemaLanguage) {
            buf.append(" schemaLanguage='").append(this.schemaLanguage);
            buf.append("'");
        }
        buf.append(">");
        for (ValidationError err : errors) {
            buf.append(err.toXml());
        }
        buf.append("\n</errors>");
        return buf.toString();
    }

    /**
     * Returns an iterator over the validation errors collected by this handler.
     *
     * @return a read-only error <code>Iterator</code> for this handler.
     */
    public Iterator<ValidationError> iterator() {
        return errors.iterator();
    }

    /**
     * Clears all errors and messages.
     */
    public void reset() {
        buf.setLength(0);
        errors.clear();
    }

    public String getSchemaLanguage() {
        return schemaLanguage;
    }

    public void setSchemaLanguage(String schemaLanguage) {
        this.schemaLanguage = schemaLanguage;
    }
}
