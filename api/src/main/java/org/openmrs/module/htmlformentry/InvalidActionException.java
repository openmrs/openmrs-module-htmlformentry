package org.openmrs.module.htmlformentry;

/**
 * Thrown when an attempt is made to add an invalid action submission actions stack
 */
public class InvalidActionException extends Exception {

    private static final long serialVersionUID = 1L;

	public InvalidActionException(String message) {
        super(message);
    }

}
