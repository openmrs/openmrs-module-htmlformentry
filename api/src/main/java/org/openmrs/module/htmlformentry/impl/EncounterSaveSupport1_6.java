package org.openmrs.module.htmlformentry.impl;

import org.openmrs.Encounter;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.api.EncounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@OpenmrsProfile(openmrsVersion = "[1.7.5 - 1.9.*]")
public class EncounterSaveSupport1_6 implements EncounterSaveSupport {
	
	@Autowired
	@Qualifier("encounterService")
	EncounterService encounterService;
	
	@Override
    public void saveEncounter(Encounter encounter) {
		encounterService.saveEncounter(encounter);
	}
}
