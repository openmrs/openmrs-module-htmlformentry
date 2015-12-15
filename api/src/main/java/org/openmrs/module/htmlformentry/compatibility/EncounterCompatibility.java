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
package org.openmrs.module.htmlformentry.compatibility;

import java.util.Collection;

import org.openmrs.Encounter;
import org.openmrs.EncounterProvider;
import org.openmrs.EncounterRole;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;

public class EncounterCompatibility {

	public static Person getProvider(Encounter encounter) {
		if (encounter.getEncounterProviders() == null || encounter.getEncounterProviders().isEmpty()) {
			return null;
		} else {
			for (EncounterProvider encounterProvider : encounter.getEncounterProviders()) {
				// Return the first non-voided provider associated with a person in the list
				if (!encounterProvider.isVoided() && encounterProvider.getProvider().getPerson() != null) {
					return encounterProvider.getProvider().getPerson();
				}
			}
		}
		return null;
	}
	
	public static void setProvider(Encounter encounter, Person provider) {
		EncounterRole unknownRole = Context.getEncounterService().getEncounterRoleByUuid(
		    EncounterRole.UNKNOWN_ENCOUNTER_ROLE_UUID);
		if (unknownRole == null) {
			throw new IllegalStateException("No 'Unknown' encounter role with uuid "
			        + EncounterRole.UNKNOWN_ENCOUNTER_ROLE_UUID + ".");
		}
		Collection<Provider> providers = Context.getProviderService().getProvidersByPerson(provider);
		if (providers == null || providers.isEmpty()) {
			throw new IllegalArgumentException("No provider with personId " + provider.getPersonId());
		}
		encounter.setProvider(unknownRole, providers.iterator().next());
	}
}
