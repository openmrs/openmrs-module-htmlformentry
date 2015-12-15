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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.openmrs.Drug;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.compatibility.DrugCompatibility;
import org.springframework.stereotype.Component;

@Component("htmlformentry.DrugCompatibility")
@OpenmrsProfile(openmrsPlatformVersion = "1.9.9 - 1.12.*")
public class DrugCompatibility1_9 implements DrugCompatibility {

	@Override
	public List<Map<String, Object>> simplify(List<Drug> drugs) {
		List<Map<String, Object>> simplified = new ArrayList<Map<String, Object>>();
        Locale locale = Context.getLocale();
        for (Drug drug : drugs) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", drug.getId());
            item.put("name", drug.getName());
            if (drug.getDosageForm() != null) {
                item.put("dosageForm", drug.getDosageForm().getName(locale).getName());
            }
            if (drug.getRoute() != null) {
                item.put("route", drug.getRoute().getName(locale).getName());
            }
            item.put("doseStrength", drug.getDoseStrength());
            item.put("units", drug.getUnits());
            item.put("combination", drug.getCombination());
            if (drug.getConcept() != null) {
                item.put("concept", drug.getConcept().getName(locale).getName());
            }
            simplified.add(item);
        }
        return simplified;
	}
}
