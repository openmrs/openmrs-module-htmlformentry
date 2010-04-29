package org.openmrs.module.htmlformentry;

/**
 * Used for general Html Form Entry exceptions 
 */
public class FormEntryException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    public FormEntryException(String message) {
        super(message);
    }

}
