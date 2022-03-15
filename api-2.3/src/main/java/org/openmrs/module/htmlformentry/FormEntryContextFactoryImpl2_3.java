package org.openmrs.module.htmlformentry;

import org.openmrs.annotation.OpenmrsProfile;
import org.springframework.stereotype.Component;

@Component("formEntryContextFactoryImpl")
@OpenmrsProfile(openmrsPlatformVersion = "2.3.* - 2.*")
public class FormEntryContextFactoryImpl2_3 implements FormEntryContextFactory {
	
	@Override
	public FormEntryContext create(FormEntryContext.Mode mode) {
		return new FormEntryContext2_3(mode);
	}
}
