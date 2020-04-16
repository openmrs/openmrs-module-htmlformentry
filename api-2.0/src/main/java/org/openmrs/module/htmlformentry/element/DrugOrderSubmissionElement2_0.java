package org.openmrs.module.htmlformentry.element;

import java.util.Date;
import java.util.Map;
import org.openmrs.DrugOrder;
import org.openmrs.module.htmlformentry.FormEntryContext;

public class DrugOrderSubmissionElement2_0 extends DrugOrderSubmissionElement1_10 {	
	
	public DrugOrderSubmissionElement2_0(FormEntryContext context, Map<String, String> parameters) {
		super(context, parameters);
	}
	
	@Override
	protected Date getDrugOrderStartDate(DrugOrder dod) {
		return dod.getDateActivated();
	}
	
	@Override
	protected void setOrderTagDose(OrderTag orderTag) {
		orderTag.dose = orderTag.drug.getMinimumDailyDose();
	}
}