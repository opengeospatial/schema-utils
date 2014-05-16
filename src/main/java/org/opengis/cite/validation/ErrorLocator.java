package org.opengis.cite.validation;

/**
 * Provides information about the location of a validation error. Instances of
 * this class are immutable.
 */
public class ErrorLocator {

    /**
     * The line where the error is located.
     */
    private int lineNumber = -1;
    /**
     * The column where the error is located.
     */
    private int columnNumber = -1;
    /**
     * A pointer that identifies the invalid fragment of some representation.
     */
    private String pointer = null;

    public ErrorLocator(int line, int col, String ptr) {
        this.lineNumber = line;
        this.columnNumber = col;
        this.pointer = ptr;
    }

    /**
     * Returns the line number where the error is located.
     * 
     * @return A positive integer indicating the line number, or -1 if this is
     *         unavailable.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the column number where the error is located.
     * 
     * @return A positive integer indicating the column number, or -1 if this is
     *         unavailable.
     */
    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Returns a pointer specifying the location of the error in some
     * representation. For XML-based representations, the pointer conforms to
     * the W3C XPointer framework.
     * 
     * @return A String representing a pointer; the format depends on the media
     *         type.
     * 
     * @see <a href="http://www.w3.org/TR/xptr-framework/">XPointer
     *      Framework</a>
     */
    public String getPointer() {
        return pointer;
    }
}
