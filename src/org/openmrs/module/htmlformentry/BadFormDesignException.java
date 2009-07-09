package org.openmrs.module.htmlformentry;

/**
 * Used to signal that the user is trying to design or submit a form that is
 * missing a required element.
 */
public class BadFormDesignException extends Exception {
    
    private static final long serialVersionUID = 1L;

    public BadFormDesignException() {
        super();
    }
    
    public BadFormDesignException(String message) {
        super(message);
    }
    
}
