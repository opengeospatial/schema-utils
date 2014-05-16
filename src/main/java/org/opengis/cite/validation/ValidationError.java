package org.opengis.cite.validation;

/**
 * Provides information pertaining to a validation error. Instances of this
 * class are immutable.
 * 
 */
public class ValidationError {

    /**
     * A brief description of the error.
     */
    private String message;
    /**
     * Supplementary diagnostic information about the error.
     */
    private String diagnostics;
    /**
     * The severity level.
     */
    private final ErrorSeverity severity;
    /**
     * Error location information.
     */
    private ErrorLocator locator;

    /**
     * Constructs an immutable validation error object from SAX or DOM error
     * information.
     * 
     * @param severity
     *            The severity level (WARNING, ERROR, CRITICAL).
     * @param message
     *            A brief description of the error.
     * @param lineNum
     *            The line number where the error occurs (-1 if unknown).
     * @param columnNum
     *            The column number where the error occurs (-1 if unknown).
     * @param pointer
     *            A fragment identifier (pointer) that locates the error in the
     *            invalid representation.
     */
    public ValidationError(ErrorSeverity severity, String message, int lineNum,
            int columnNum, String pointer) {
        this(severity, message, null, new ErrorLocator(lineNum, columnNum,
                pointer));
    }

    /**
     * Constructs an immutable validation error object from SAX or DOM error
     * information.
     * 
     * @param severity
     *            The severity level (WARNING, ERROR, CRITICAL).
     * @param message
     *            A brief description of the error.
     * @param diagnostics
     *            Supplementary diagnostic information about the error.
     * @param lineNum
     *            The line number where the error occurs (-1 if unknown).
     * @param columnNum
     *            The column number where the error occurs (-1 if unknown).
     * @param pointer
     *            A fragment identifier (pointer) that locates the error in the
     *            invalid representation.
     */
    public ValidationError(ErrorSeverity severity, String message,
            String diagnostics, int lineNum, int columnNum, String pointer) {
        this(severity, message, diagnostics, new ErrorLocator(lineNum,
                columnNum, pointer));
    }

    /**
     * Constructs an immutable validation error.
     * 
     * @param severity
     *            The severity level (WARNING, ERROR, CRITICAL).
     * @param message
     *            A brief description of the error.
     * @param diagnostics
     *            Supplementary diagnostic information about the error.
     * @param locator
     *            Provides information about the location of the error.
     */
    public ValidationError(ErrorSeverity severity, String message,
            String diagnostics, ErrorLocator locator) {
        if ((null == message) || (message.length() == 0)) {
            message = "No information available.";
        }
        this.severity = severity;
        this.message = message;
        this.diagnostics = diagnostics;
        this.locator = locator;
    }

    /**
     * Returns the message describing this error.
     * 
     * @return the error description.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns additional diagnostic information about this error.
     * 
     * @return supplementary details about this error.
     */
    public String getDiagnostics() {
        return diagnostics;
    }

    /**
     * Returns the severity of this error.
     * 
     * @return The severity value for this error.
     */
    public ErrorSeverity getSeverity() {
        return severity;
    }

    /**
     * Returns the line number where the error is located.
     * 
     * @return An integer value greater than zero, or -1 if no value is
     *         available;
     */
    public int getLineNumber() {
        return locator.getLineNumber();
    }

    /**
     * Returns the column number where the error is located.
     * 
     * @return An integer value greater than zero, or -1 if no value is
     *         available.
     */
    public int getColumnNumber() {
        return locator.getColumnNumber();
    }

    /**
     * Returns a fragment identifier that identifies the invalid part of some
     * resource representation. For XML-based representations, such pointers are
     * based on the W3C XPointer framework.
     * 
     * @return A String that conforms to the applicable pointer syntax.
     */
    public String getPointer() {
        return locator.getPointer();
    }

    /**
     * <p>
     * Returns an XML representation of the validation error. The structure is
     * as shown in the example below:
     * </p>
     * 
     * <pre>
     *   &lt;error&gt;
     *     &lt;severity&gt;ERROR&lt;/severity&gt;
     *     &lt;message&gt;Error details&lt;/message&gt;
     *     &lt;diagnosticInfo&gt;Error details&lt;/diagnosticInfo&gt;
     *     &lt;location&gt;
     *       &lt;lineNumber&gt;5&lt;/lineNumber&gt;
     *       &lt;columnNumber&gt;4&lt;/columnNumber&gt;
     *       &lt;pointer&gt;xmlns(tns=http://www.example.org)xpointer(//tns:foo[1])
     *     &lt;/pointer&gt;
     *     &lt;/location&gt;
     *   &lt;/error&gt;
     * </pre>
     * 
     * @return A string containing XML elements in no namespace.
     */
    public String toXml() {
        StringBuilder xmlInfo = new StringBuilder("\n<error>");
        xmlInfo.append("\n  <severity>").append(this.severity)
                .append("</severity>");
        xmlInfo.append("\n  <message>").append(escapeXmlString(this.message))
                .append("</message>");
        if (null != this.diagnostics) {
            xmlInfo.append("\n  <diagnosticInfo>")
                    .append(escapeXmlString(this.diagnostics))
                    .append("</diagnosticInfo>");
        }
        if (null != this.locator) {
            xmlInfo.append("\n  <location>");
            if (getLineNumber() > 0) {
                xmlInfo.append("\n    <lineNumber>").append(getLineNumber())
                        .append("</lineNumber>");
            }
            if (getColumnNumber() > 0) {
                xmlInfo.append("\n    <columnNumber>")
                        .append(getColumnNumber()).append("</columnNumber>");
            }
            if (null != getPointer()) {
                xmlInfo.append("\n    <pointer>").append(getPointer())
                        .append("</pointer>");
            }
            xmlInfo.append("\n  </location>");
        }
        xmlInfo.append("\n</error>");
        return xmlInfo.toString();
    }

    /**
     * Returns information about the error.
     * 
     * @return A string containing error details.
     */
    public String toString() {
        StringBuilder errorInfo = new StringBuilder();
        errorInfo.append("\nSeverity: ").append(this.severity);
        errorInfo.append("\nMessage: ").append(this.message);
        if (null != this.diagnostics) {
            errorInfo.append("\nDiagnostic info: ").append(this.diagnostics);
        }
        if (null != this.locator) {
            errorInfo.append("\nLocation: ");
            if (getLineNumber() > 0) {
                errorInfo.append(" line=").append(getLineNumber());
            }
            if (getColumnNumber() > 0) {
                errorInfo.append(" column=").append(getColumnNumber());
            }
            if (null != getPointer()) {
                errorInfo.append(" pointer=").append(getPointer());
            }
        }
        return errorInfo.toString();
    }

    /**
     * Escapes characters that cause the message to be invalid XML (namely
     * greater than and less than characters if both are present).
     * 
     * @param str
     *            the string to escape
     * @return the escaped string
     */
    private String escapeXmlString(String str) {
        // Escape characters that will cause the XML to be invalid
        if (str.indexOf("<") != -1 && str.indexOf(">") != -1) {
            str = str.replaceAll("<", "&lt;");
            str = str.replaceAll(">", "&gt;");
        }
        return str;
    }
}
