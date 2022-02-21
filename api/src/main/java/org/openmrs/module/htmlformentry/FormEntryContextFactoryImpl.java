package org.openmrs.module.htmlformentry;

import org.openmrs.annotation.OpenmrsProfile;
import org.springframework.stereotype.Component;

@Component
@OpenmrsProfile(openmrsPlatformVersion = "1.* - 2.2.*")
public class FormEntryContextFactoryImpl implements FormEntryContextFactory {
	
	@Override
	public FormEntryContext create(FormEntryContext.Mode mode) {
		return new FormEntryContext(mode);
	}
}
