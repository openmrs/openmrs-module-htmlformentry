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

import java.util.List;
import java.util.Set;

import org.openmrs.Drug;
import org.openmrs.OpenmrsObject;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.order.DrugOrderSupport;
import org.openmrs.order.RegimenSuggestion;
import org.openmrs.module.htmlformentry.compatibility.RegimenSuggestionCompatibility;
import org.openmrs.module.htmlformentry.handler.AttributeDescriptor;
import org.openmrs.order.DrugSuggestion;
import org.springframework.stereotype.Component;

@Component("htmlformentry.RegimenSuggestionCompatibility")
@OpenmrsProfile(openmrsPlatformVersion = "1.9.9 - 1.12.*")
public class RegimenSuggestionCompatibility1_9 implements RegimenSuggestionCompatibility {

	@Override
	public void AddDrugDependencies(String id, AttributeDescriptor attributeDescriptor, Set<OpenmrsObject> dependencies) {
		//RegimenSuggestion -- see global property 'dashboard.regimen.standardRegimens'
		if (RegimenSuggestion.class.equals(attributeDescriptor.getClazz())){
			List<RegimenSuggestion> stRegimens = DrugOrderSupport.getInstance().getStandardRegimens();
			if (stRegimens != null){
				ConceptService cs = Context.getConceptService();
				for (RegimenSuggestion rs : stRegimens){
					if (rs.getCodeName().equals(id) && rs.getDrugComponents() != null){
						for (DrugSuggestion ds : rs.getDrugComponents()){
							Drug drug = cs.getDrug(ds.getDrugId());
							if (drug == null)
								 drug = cs.getDrugByUuid(ds.getDrugId());
							if (drug != null)
								dependencies.add(drug);
						}
					}
				}
			}
		}
	}
}