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

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.compatibility.DrugOrderCompatibility;
import org.springframework.stereotype.Component;

@Component("htmlformentry.DrugOrderCompatibility")
@OpenmrsProfile(openmrsPlatformVersion = "1.9.9 - 1.9.*")
public class DrugOrderCompatibility1_9 implements DrugOrderCompatibility {
	
	@Override
	public void setStartDate(DrugOrder drugOrder, Date startDate) {
		drugOrder.setStartDate(startDate);
	}
	
	@Override
	public Date getStartDate(DrugOrder drugOrder) {
		return drugOrder.getStartDate();
	}
	
	@Override
	public void setFrequency(DrugOrder drugOrder, String frequency) {
		drugOrder.setFrequency(frequency);
	}
	
	@Override
	public void setDoseUnits(DrugOrder drugOrder, String units) {
		drugOrder.setUnits(units);
	}
	
	@Override
	public void setRoute(DrugOrder drugOrder) {
		// NOTHING HERE
	}
	
	@Override
	public void discontinue(DrugOrder drugOrder, Date discontinueDate, String reason) {
		drugOrder.setDiscontinued(true);
		drugOrder.setDiscontinuedDate(discontinueDate);
		if (!StringUtils.isEmpty(reason)) {
			Concept c = HtmlFormEntryUtil.getConcept(reason);
			if (c != null) {
				drugOrder.setDiscontinuedReason(c);
			} else {
				drugOrder.setDiscontinuedReasonNonCoded(reason);
			}
		}
	}
}
