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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.api.OrderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.compatibility.DrugOrderCompatibility;
import org.springframework.stereotype.Component;

@Component("htmlformentry.DrugOrderCompatibility")
@OpenmrsProfile(openmrsPlatformVersion = "1.10.0 - 2.*")
public class DrugOrderCompatibility1_10 implements DrugOrderCompatibility {
	
	@Override
	public void setStartDate(DrugOrder drugOrder, Date startDate) {
		if (drugOrder.getEncounter() == null) {
			throw new IllegalStateException("You cannot set the start date on a Drug Order until you set the encounter");
		}
		Date encounterDate = drugOrder.getEncounter().getEncounterDatetime();
		drugOrder.setDateActivated(encounterDate);
		if (startDate.compareTo(encounterDate) > 0) {
			drugOrder.setScheduledDate(startDate);
			drugOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		} else if (startDate.compareTo(encounterDate) < 0) {
			throw new IllegalStateException("You cannot set the start date on a Drug Order prior to the encounter date");
		}
	}
	
	@Override
	public Date getStartDate(DrugOrder drugOrder) {
		return drugOrder.getEffectiveStartDate();
	}
	
	@Override
	public void setFrequency(DrugOrder drugOrder, String frequency) {
		OrderService os = Context.getOrderService();
		List<OrderFrequency> orderFrequencies = os.getOrderFrequencies(frequency, null, false, false);
		if (!orderFrequencies.isEmpty()) {
			drugOrder.setFrequency(orderFrequencies.get(0));
		}
	}
	
	@Override
	public void setDoseUnits(DrugOrder drugOrder, String units) {
		Concept c = HtmlFormEntryUtil.getConcept(units);
		drugOrder.setDoseUnits(c);
	}
	
	@Override
	public void setRoute(DrugOrder drugOrder) {
		Concept c = HtmlFormEntryUtil.getConcept("UNKNOWN");
		drugOrder.setRoute(c);
	}
	
	@Override
	public void discontinue(DrugOrder drugOrder, Date discontinueDate, String reason) {
		throw new RuntimeException("TODO:  Need to implement this method");
	}
	
	public int compareDates(Date d1, Date d2) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		return df.format(d1).compareTo(df.format(d2));
	}
}
