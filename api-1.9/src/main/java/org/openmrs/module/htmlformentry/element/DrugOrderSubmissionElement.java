package org.openmrs.module.htmlformentry.element;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.util.OpenmrsConstants;

/**
 * Holds the widgets used to represent a specific drug order, and serves as both the
 * HtmlGeneratorElement and the FormSubmissionControllerAction for the drug order.
 */
public class DrugOrderSubmissionElement extends AbstractDrugOrderSubmissionElement {
	
	public DrugOrderSubmissionElement(FormEntryContext context, Map<String, String> parameters) {
		super(context, parameters);
	}
	
	@Override
	protected void createAdditionalWidgets(FormEntryContext context) {
	}
	
	/**
	 * Be aware it's called by the constructor.
	 * 
	 * @param context
	 * @param usingDurationField
	 */
	@Override
	protected void populateDrugOrderValuesFromDB(FormEntryContext context, Boolean usingDurationField) {
		for (Drug drug : drugsUsedAsKey) {
			if (context.getExistingOrders().containsKey(drug.getConcept())) {
				//this will return null if Order is not a DrugOrder even if matched by Concept
				DrugOrder drugOrder = (DrugOrder) context.removeExistingDrugOrder(drug);
				if (drugOrder != null) {
					existingOrder = drugOrder;
					if (drugWidget instanceof DropdownWidget) {
						drugWidget.setInitialValue(drugOrder.getDrug().getDrugId());
					} else {
						if (((CheckboxWidget) drugWidget).getValue().equals(drugOrder.getDrug().getDrugId().toString()))
							((CheckboxWidget) drugWidget).setInitialValue("CHECKED");
					}
					startDateWidget.setInitialValue(drugOrder.getStartDate());
					if (!hideDoseAndFrequency && hideDose) {
						frequencyWidget.setInitialValue(parseFrequencyDays(drugOrder.getFrequency()));
						frequencyWeekWidget.setInitialValue(parseFrequencyWeek(drugOrder.getFrequency()));
					} else if (!hideDoseAndFrequency) {
						doseWidget.setInitialValue(drugOrder.getDose());
						frequencyWidget.setInitialValue(parseFrequencyDays(drugOrder.getFrequency()));
						frequencyWeekWidget.setInitialValue(parseFrequencyWeek(drugOrder.getFrequency()));
					}
					if (!usingDurationField) {
						discontinuedDateWidget.setInitialValue(drugOrder.getDiscontinuedDate());
						if (discontinuedReasonWidget != null && drugOrder.getDiscontinuedReason() != null)
							discontinuedReasonWidget.setInitialValue(drugOrder.getDiscontinuedReason().getConceptId());
					}
					break;
				}
				
			}
		}
	}
	
	/**
	 * Static helper method to parse frequency string <strong>Should</strong> return times per day which
	 * is part of frequency string
	 *
	 * @param frequency (format "x/d y d/w")
	 * @return x
	 */
	private static String parseFrequencyDays(String frequency) {
		String days = StringUtils.substringBefore(frequency, "/d");
		return days;
	}
	
	/**
	 * Static helper method to parse frequency string <strong>Should</strong> return number of days per
	 * weeks which is part of frequency string
	 *
	 * @param frequency (format "x/d y d/w")
	 * @return y
	 */
	private static String parseFrequencyWeek(String frequency) {
		String temp = StringUtils.substringAfter(frequency, "/d");
		String weeks = StringUtils.substringBefore(temp, "d/");
		return weeks;
	}
	
	protected void editOrder(FormEntrySession session, OrderTag orderTag) {
		existingOrder.setDrug(orderTag.drug);
		existingOrder.setDose(orderTag.dose);
		existingOrder.setFrequency(orderTag.frequency);
		existingOrder.setStartDate(orderTag.startDate);
		if (orderTag.orderDuration != null)
			existingOrder.setAutoExpireDate(calculateAutoExpireDate(orderTag.startDate, orderTag.orderDuration));
		if (orderTag.discontinuedDate != null) {
			existingOrder.setDiscontinuedDate(orderTag.discontinuedDate);
			existingOrder.setDiscontinued(true);
		}
		if (!StringUtils.isEmpty(orderTag.discontinuedReasonStr))
			existingOrder.setDiscontinuedReason(HtmlFormEntryUtil.getConcept(orderTag.discontinuedReasonStr));
		
		existingOrder.setConcept(orderTag.drug.getConcept());
		if (!StringUtils.isEmpty(orderTag.instructions))
			existingOrder.setInstructions((String) orderTag.instructions);
		log.debug("modifying drug order, drugId is " + orderTag.drugId + " and startDate is " + orderTag.startDate);
		existingOrder = setDiscontinueDateFromAutoExpire(existingOrder);
		session.getSubmissionActions().getCurrentEncounter().setDateChanged(new Date());
	}
	
	protected void enterOrder(FormEntrySession session, OrderTag orderTag) {
		DrugOrder drugOrder = new DrugOrder();
		if (drugOrder.getDateCreated() == null)
			drugOrder.setDateCreated(new Date());
		if (drugOrder.getCreator() == null)
			drugOrder.setCreator(Context.getAuthenticatedUser());
		if (drugOrder.getUuid() == null)
			drugOrder.setUuid(UUID.randomUUID().toString());
		drugOrder.setDrug(orderTag.drug);
		drugOrder.setPatient(session.getPatient());
		drugOrder.setDose(orderTag.dose);
		drugOrder.setFrequency(orderTag.frequency);
		drugOrder.setStartDate(orderTag.startDate);
		//order duration:
		if (orderTag.orderDuration != null)
			drugOrder.setAutoExpireDate(calculateAutoExpireDate(orderTag.startDate, orderTag.orderDuration));
		drugOrder.setVoided(false);
		drugOrder.setDrug(orderTag.drug);
		drugOrder.setConcept(orderTag.drug.getConcept());
		drugOrder.setOrderType(Context.getOrderService().getOrderType(OpenmrsConstants.ORDERTYPE_DRUG));
		if (!StringUtils.isEmpty(orderTag.instructions))
			drugOrder.setInstructions((String) orderTag.instructions);
		if (orderTag.discontinuedDate != null) {
			drugOrder.setDiscontinuedDate(orderTag.discontinuedDate);
			drugOrder.setDiscontinued(true);
		}
		if (!StringUtils.isEmpty(orderTag.discontinuedReasonStr))
			drugOrder.setDiscontinuedReason(HtmlFormEntryUtil.getConcept(orderTag.discontinuedReasonStr));
		log.debug("adding new drug order, drugId is " + orderTag.drugId + " and startDate is " + orderTag.startDate);
		drugOrder = setDiscontinueDateFromAutoExpire(drugOrder);
		session.getSubmissionActions().getCurrentEncounter().addOrder(drugOrder);
	}
	
	private DrugOrder setDiscontinueDateFromAutoExpire(DrugOrder dor) {
		if (dor.getAutoExpireDate() != null) {
			Date today = new Date();
			if (dor.getAutoExpireDate().getTime() < today.getTime()) {
				dor.setDiscontinuedDate(dor.getAutoExpireDate());
				//TODO:  when discontinueReason is a String, set it.
			}
		}
		return dor;
	}
}
