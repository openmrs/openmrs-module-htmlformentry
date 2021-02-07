package org.openmrs.module.htmlformentry.element;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.SimpleDosingInstructions;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.widget.DrugOrderWidget;
import org.openmrs.module.htmlformentry.widget.DrugOrderWidgetConfig;
import org.openmrs.module.htmlformentry.widget.DrugOrderWidgetValue;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.util.OpenmrsUtil;

/**
 * Holds the widgets used to represent drug orders for a configured set of drugs, and serves as both
 * the HtmlGeneratorElement and the FormSubmissionControllerAction for the associated drug orders.
 */
public class DrugOrderSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	protected final Log log = LogFactory.getLog(DrugOrderSubmissionElement.class);
	
	private DrugOrderWidget drugOrderWidget;
	
	private ErrorWidget drugOrderErrorWidget;
	
	private List<DrugOrder> existingOrders;
	
	/**
	 * Instantiates a new Drug Order Submission Element
	 */
	public DrugOrderSubmissionElement(FormEntryContext context, DrugOrderWidget drugOrderWidget) {
		this.drugOrderWidget = drugOrderWidget;
		DrugOrderWidgetConfig config = drugOrderWidget.getWidgetConfig();
		drugOrderWidget.setInitialValue(getDrugOrders(context.getExistingPatient(), config));
		drugOrderErrorWidget = new ErrorWidget();
		context.registerWidget(drugOrderWidget);
		context.registerErrorWidget(drugOrderWidget, drugOrderErrorWidget);
		context.addFieldToActiveSection(config.getDrugOrderField());
		populateExistingOrders(context);
	}
	
	/**
	 * <strong>Should</strong> return HTML snippet
	 *
	 * @see HtmlGeneratorElement#generateHtml(FormEntryContext)
	 */
	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder html = new StringBuilder();
		html.append(drugOrderWidget.generateHtml(context));
		if (context.getMode() != Mode.VIEW) {
			html.append(drugOrderErrorWidget.generateHtml(context));
		}
		return html.toString();
	}
	
	/**
	 * Populate the list of existing orders that are associated with the context and the drugs
	 * configured in this tag
	 */
	protected void populateExistingOrders(FormEntryContext context) {
		existingOrders = new ArrayList<>();
		for (DrugOrderAnswer doa : drugOrderWidget.getDrugOrderField().getDrugOrderAnswers()) {
			DrugOrder o = context.removeExistingDrugOrder(doa.getDrug());
			while (o != null) {
				existingOrders.add(o);
				o = context.removeExistingDrugOrder(doa.getDrug());
			}
		}
	}
	
	/**
	 * @see FormSubmissionControllerAction#validateSubmission(FormEntryContext, HttpServletRequest)
	 */
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext ctx, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<>();
		List<DrugOrderWidgetValue> drugOrders = drugOrderWidget.getValue(ctx, submission);
		for (DrugOrderWidgetValue v : drugOrders) {
			DrugOrder drugOrder = v.getNewDrugOrder();
			if (drugOrder != null) {
				String fs = v.getFieldSuffix();
				Order.Action action = drugOrder.getAction();
				DrugOrder previousOrder = (DrugOrder) drugOrder.getPreviousOrder();
				
				// If not a discontinue Order, then validate dosing
				if (action != Order.Action.DISCONTINUE) {
					
					handleRequiredField(ret, ctx, fs, "careSetting", drugOrder.getCareSetting());
					
					if (drugOrder.getDrug() != null && StringUtils.isNotBlank(drugOrder.getDrugNonCoded())) {
						String f = drugOrderWidget.getFormErrorField(ctx, fs, "drugNonCoded");
						String errorCode = "htmlformentry.drugOrder.specifyEitherDrugOrDrugNonCoded";
						ret.add(new FormSubmissionError(f, HtmlFormEntryUtil.translate(errorCode)));
					}
					
					if (drugOrder.getDosingType() == SimpleDosingInstructions.class) {
						handleRequiredField(ret, ctx, fs, "dose", drugOrder.getDose());
						handleRequiredField(ret, ctx, fs, "doseUnits", drugOrder.getDoseUnits());
						handleRequiredField(ret, ctx, fs, "route", drugOrder.getRoute());
						handleRequiredField(ret, ctx, fs, "frequency", drugOrder.getFrequency());
					} else if (drugOrder.getDosingType() == FreeTextDosingInstructions.class) {
						String doseInstructions = drugOrder.getDosingInstructions();
						handleRequiredField(ret, ctx, fs, "dosingInstructions", doseInstructions);
					} else {
						String f = drugOrderWidget.getFormErrorField(ctx, fs, "dosingType");
						String dosingTypeError = HtmlFormEntryUtil.translate("htmlformentry.drugOrder.invalidDosingType");
						ret.add(new FormSubmissionError(f, dosingTypeError));
					}
					
					if (areQuantityFieldsRequired(drugOrder)) {
						handleRequiredField(ret, ctx, fs, "quantity", drugOrder.getQuantity());
						handleRequiredField(ret, ctx, fs, "quantityUnits", drugOrder.getQuantityUnits());
						handleRequiredField(ret, ctx, fs, "numRefills", drugOrder.getNumRefills());
					}
					
					if (drugOrder.getUrgency() == Order.Urgency.ON_SCHEDULED_DATE) {
						handleRequiredField(ret, ctx, fs, "scheduledDate", drugOrder.getScheduledDate());
					}
					
					if (drugOrder.getDuration() != null) {
						handleRequiredField(ret, ctx, fs, "durationUnits", drugOrder.getDurationUnits());
					}
					
					if (drugOrder.getQuantity() != null) {
						handleRequiredField(ret, ctx, fs, "quantityUnits", drugOrder.getQuantityUnits());
					}
				}
				
				// If not a new Order, then this must have a previousOrder to operate on
				if (action != Order.Action.NEW) {
					String errorField = ctx.getFieldName(drugOrderErrorWidget);
					if (previousOrder == null) {
						addError(ret, errorField, "htmlformentry.drugOrderError.previousOrderRequired");
					} else {
						if (!isSameDrug(drugOrder, previousOrder) && BooleanUtils.isNotTrue(drugOrder.getVoided())) {
							addError(ret, errorField, "htmlformentry.drugOrderError.drugChangedForRevision");
						}
						if (action == Order.Action.RENEW) {
							if (dosingInstructionsChanged(drugOrder, previousOrder)) {
								addError(ret, errorField, "htmlformentry.drugOrderError.dosingChangedForRenew");
							}
						}
					}
				}
			}
		}
		return ret;
	}
	
	/**
	 * This is responsible for taking the passed DrugOrderWidgetValue objects returned from the widget
	 * and issuing the appropriate void, NEW, RENEW, REVISE, DISCONTINUE orders, setting default values
	 * as needed
	 *
	 * @see FormSubmissionControllerAction#handleSubmission(FormEntrySession, HttpServletRequest)
	 */
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest request) {
		Encounter encounter = session.getSubmissionActions().getCurrentEncounter();
		DrugOrderWidgetConfig widgetConfig = drugOrderWidget.getWidgetConfig();
		for (DrugOrderWidgetValue v : drugOrderWidget.getValue(session.getContext(), request)) {
			
			DrugOrder newOrder = v.getNewDrugOrder();
			DrugOrder previousOrder = v.getPreviousDrugOrder();
			boolean voidPrevious = false;
			
			// First we set some defaults on the new order
			
			if (newOrder != null) {
				// Set orderer if not specified to the provider on the encounter
				if (newOrder.getOrderer() == null) {
					newOrder.setOrderer(HtmlFormEntryUtil.getOrdererFromEncounter(encounter));
				}
				
				// By default, set the dateActivated to be the encounter date, if not specified
				if (newOrder.getDateActivated() == null) {
					newOrder.setDateActivated(encounter.getEncounterDatetime());
				}
				
				// Next, determine if this is a revision that should really be a void operation
				// We do this by determining if the new order is a revision within the same encounter, with same date activated
				if (previousOrder != null) {
					if (encounter.equals(previousOrder.getEncounter())) { // Same encounter
						if (isSameDrug(newOrder, previousOrder)) {
							if (newOrder.getDateActivated().equals(previousOrder.getDateActivated())) { // Same date activated
								voidPrevious = true;
							}
						}
					}
				}
			}
			
			if (voidPrevious) {
				session.getSubmissionActions().getOrdersToVoid().add(previousOrder);
			}
			if (newOrder != null) {
				session.getSubmissionActions().getOrdersToCreate().add(newOrder);
			}
		}
	}
	
	/**
	 * Retrieves the drug orders for the patient for the specified drugs
	 */
	public List<DrugOrder> getDrugOrders(Patient patient, DrugOrderWidgetConfig config) {
		List<DrugOrder> ret = new ArrayList<>();
		if (patient != null && config.getOrderPropertyOptions("concept") != null) {
			Set<Concept> concepts = new HashSet<>();
			for (Option o : config.getOrderPropertyOptions("concept")) {
				concepts.add(HtmlFormEntryUtil.getConcept(o.getValue()));
			}
			ret = HtmlFormEntryUtil.getDrugOrdersForPatient(patient, concepts);
		}
		return ret;
	}
	
	protected boolean isSameDrug(DrugOrder current, DrugOrder previous) {
		boolean ret = areEqual(current.getConcept(), previous.getConcept());
		ret = ret && areEqual(current.getDrug(), previous.getDrug());
		ret = ret && areEqual(current.getDrugNonCoded(), previous.getDrugNonCoded());
		return ret;
	}
	
	protected boolean dosingInstructionsChanged(DrugOrder current, DrugOrder previous) {
		boolean ret = false;
		ret = ret || !areEqual(current.getDosingType(), previous.getDosingType());
		ret = ret || !areEqual(current.getDosingInstructions(), previous.getDosingInstructions());
		ret = ret || !areEqual(current.getDose(), previous.getDose());
		ret = ret || !areEqual(current.getDoseUnits(), previous.getDoseUnits());
		ret = ret || !areEqual(current.getRoute(), previous.getRoute());
		ret = ret || !areEqual(current.getFrequency(), previous.getFrequency());
		ret = ret || !areEqual(current.getInstructions(), previous.getInstructions());
		ret = ret || !areEqual(current.getAsNeeded(), previous.getAsNeeded());
		ret = ret || !areEqual(current.getAsNeededCondition(), previous.getAsNeededCondition());
		return ret;
	}
	
	protected boolean areQuantityFieldsRequired(DrugOrder drugOrder) {
		if (drugOrder.getCareSetting() != null) {
			if (drugOrder.getCareSetting().getCareSettingType() == CareSetting.CareSettingType.OUTPATIENT) {
				String gpName = "drugOrder.requireOutpatientQuantity";
				String gpVal = Context.getAdministrationService().getGlobalProperty(gpName, "true");
				return "true".equalsIgnoreCase(gpVal);
			}
		}
		return false;
	}
	
	/**
	 * Convenience method to determine if two objects are equal. It considers null and "" to be equal.
	 */
	protected boolean areEqual(Object o1, Object o2) {
		o1 = ("".equals(o1) ? null : o1);
		o2 = ("".equals(o2) ? null : o2);
		return OpenmrsUtil.nullSafeEquals(o1, o2);
	}
	
	public void handleRequiredField(List<FormSubmissionError> ret, FormEntryContext ctx, String fieldSuffix, String prop,
	        Object val) {
		if (val == null || val.equals("")) {
			String field = drugOrderWidget.getFormErrorField(ctx, fieldSuffix, prop);
			addError(ret, field, "htmlformentry.error.required");
		}
	}
	
	public void addError(List<FormSubmissionError> ret, String field, String messageCode) {
		ret.add(new FormSubmissionError(field, HtmlFormEntryUtil.translate(messageCode)));
	}
	
	public DrugOrderWidget getDrugOrderWidget() {
		return drugOrderWidget;
	}
	
	public void setDrugOrderWidget(DrugOrderWidget drugOrderWidget) {
		this.drugOrderWidget = drugOrderWidget;
	}
	
	public ErrorWidget getDrugOrderErrorWidget() {
		return drugOrderErrorWidget;
	}
	
	public void setDrugOrderErrorWidget(ErrorWidget drugOrderErrorWidget) {
		this.drugOrderErrorWidget = drugOrderErrorWidget;
	}
	
	public List<DrugOrder> getExistingOrders() {
		return existingOrders;
	}
}
