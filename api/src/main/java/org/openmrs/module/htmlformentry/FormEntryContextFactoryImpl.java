package org.openmrs.module.htmlformentry;

import org.springframework.stereotype.Component;

@Component
public class FormEntryContextFactoryImpl implements FormEntryContextFactory {
	
	@Override
	public FormEntryContext create(FormEntryContext.Mode mode) {
		return new FormEntryContext(mode);
	}
}
