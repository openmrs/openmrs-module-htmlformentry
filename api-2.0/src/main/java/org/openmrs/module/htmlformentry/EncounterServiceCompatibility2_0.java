/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.htmlformentry;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.compatibility.EncounterServiceCompatibility;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.openmrs.parameter.EncounterSearchCriteriaBuilder;
import org.springframework.stereotype.Component;

@Component("htmlformentry.EncounterServiceCompatibility")
@OpenmrsProfile(openmrsPlatformVersion = "2.*")
public class EncounterServiceCompatibility2_0 implements EncounterServiceCompatibility {

	@Override
	public List<Encounter> getEncounters(Patient who, Location loc, Date fromDate, Date toDate,
			Collection<Form> enteredViaForms,
			Collection<EncounterType> encounterTypes,
			Collection<Provider> providers, Collection<VisitType> visitTypes,
			Collection<Visit> visits, boolean includeVoided) {
		
		EncounterSearchCriteria encounterSearchCriteria = new EncounterSearchCriteriaBuilder()
		.setPatient(who)
		.setLocation(loc)
		.setFromDate(fromDate)
		.setToDate(toDate)
		.setEnteredViaForms(enteredViaForms)
		.setEncounterTypes(encounterTypes)
		.setProviders(providers)
		.setVisitTypes(visitTypes)
		.setVisits(visits)
		.setIncludeVoided(includeVoided)
		.createEncounterSearchCriteria();
		
		return Context.getEncounterService().getEncounters(encounterSearchCriteria);
	}
}