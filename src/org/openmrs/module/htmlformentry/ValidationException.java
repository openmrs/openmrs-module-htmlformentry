package org.openmrs.module.htmlformentry;

/**
 * Used to signal that the user provided invalid input
 */
public class ValidationException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public ValidationException() {
        super();
    }
    
    public ValidationException(String message) {
        super(message);
    }
    
}
