package org.opengis.cite.validation;

/**
 * Enumerated type indicating the severity of a validation error.
 * 
 */
public enum ErrorSeverity {

    /**
     * Indicates a condition that is not strictly non-conforming but
     * nevertheless violates a convention, expectation, or a recommendation.
     */
    WARNING,
    /**
     * Indicates an assertion failure or the occurrence of some other
     * non-conforming condition.
     */
    ERROR,
    /**
     * Indicates that a process was abruptly terminated; data may have been lost
     * or corrupted.
     */
    CRITICAL;
}
