package org.openmrs.module.htmlformentry;

public interface FormEntryContextFactory {
	
	FormEntryContext create(FormEntryContext.Mode mode);
	
}
