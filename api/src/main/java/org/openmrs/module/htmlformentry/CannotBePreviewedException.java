package org.openmrs.module.htmlformentry;

/**
 * Indicates that something (velocity, logic, etc) cannot be displayed in preview mode.
 * The HTML Form Entry framework will pretty up this message and not show a stack trace.
 */
public class CannotBePreviewedException extends RuntimeException {

	public CannotBePreviewedException() {
		super();
	}
	
	public CannotBePreviewedException(String message) {
		super(message);
	}
	
}
