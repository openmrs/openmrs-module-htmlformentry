package org.openmrs.module.htmlformentry.element;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;
import org.openmrs.module.htmlformentry.widget.DrugOrderWidgetConfig;
import org.openmrs.module.htmlformentry.widget.DrugOrderWidgetValue;
import org.openmrs.module.htmlformentry.widget.DrugOrdersWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.util.ReflectionUtils;

/**
 * Holds the widgets used to represent drug orders for a configured set of drugs, and serves as both
 * the HtmlGeneratorElement and the FormSubmissionControllerAction for the associated drug orders.
 */
public class DrugOrdersSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	protected final Log log = LogFactory.getLog(DrugOrdersSubmissionElement.class);
	
	private DrugOrdersWidget drugOrdersWidget;
	
	private ErrorWidget drugOrdersErrorWidget;
	
	/**
	 * Instantiates a new Drug Order Submission Element, for the given Drug and Context
	 */
	public DrugOrdersSubmissionElement(FormEntryContext context, DrugOrdersWidget drugOrdersWidget) {
		this.drugOrdersWidget = drugOrdersWidget;
		DrugOrderField field = drugOrdersWidget.getDrugOrderField();
		drugOrdersWidget.setInitialValue(getDrugOrders(context.getExistingPatient(), field));
		drugOrdersErrorWidget = new ErrorWidget();
		context.registerWidget(drugOrdersWidget);
		context.registerErrorWidget(drugOrdersWidget, drugOrdersErrorWidget);
		context.addFieldToActiveSection(field);
	}
	
	/**
	 * <strong>Should</strong> return HTML snippet
	 * 
	 * @see HtmlGeneratorElement#generateHtml(FormEntryContext)
	 */
	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder html = new StringBuilder();
		html.append(drugOrdersWidget.generateHtml(context));
		if (context.getMode() != Mode.VIEW) {
			html.append(drugOrdersErrorWidget.generateHtml(context));
		}
		return html.toString();
	}
	
	/**
	 * @see FormSubmissionControllerAction#validateSubmission(FormEntryContext, HttpServletRequest)
	 */
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		List<DrugOrderWidgetValue> drugOrders = drugOrdersWidget.getValue(context, submission);
		for (DrugOrderWidgetValue v : drugOrders) {
			DrugOrder drugOrder = v.getNewDrugOrder();
			if (drugOrder != null) {
				Order.Action action = drugOrder.getAction();
				DrugOrder previousOrder = (DrugOrder) drugOrder.getPreviousOrder();
				if (action == Order.Action.NEW) {
					// TODO: Add any necessary validation for NEW orders here
				} else {
					// If not a new Order, then this must have a previousOrder to operate on
					if (previousOrder == null) {
						addError(ret, drugOrder, "htmlformentry.drugOrderError.previousOrderRequired");
					} else {
						if (!isSameDrug(drugOrder, previousOrder) && BooleanUtils.isNotTrue(drugOrder.getVoided())) {
							addError(ret, drugOrder, "htmlformentry.drugOrderError.drugChangedForRevision");
						}
						if (action == Order.Action.RENEW) {
							if (dosingInstructionsChanged(drugOrder, previousOrder)) {
								addError(ret, drugOrder, "htmlformentry.drugOrderError.dosingChangedForRenew");
							}
						}
					}
				}
			}
		}
		return ret;
	}
	
	/**
	 * For now, handle the following use cases: START: create new order STOP: stop order, create
	 * discontinue order EDIT: void / associate appropriate order and/or discontinue order
	 *
	 * @see FormSubmissionControllerAction#handleSubmission(FormEntrySession, HttpServletRequest)
	 */
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest request) {
		Encounter encounter = session.getSubmissionActions().getCurrentEncounter();
		DrugOrderWidgetConfig widgetConfig = drugOrdersWidget.getWidgetConfig();
		for (DrugOrderWidgetValue v : drugOrdersWidget.getValue(session.getContext(), request)) {
			
			if (v.isVoidPreviousOrder()) {
				DrugOrder previousOrder = v.getPreviousDrugOrder();
				previousOrder.setVoided(true);
				previousOrder.setDateVoided(new Date());
				previousOrder.setVoidReason("Voided by htmlformentry");
			}
			
			DrugOrder newOrder = v.getNewDrugOrder();
			if (newOrder != null) {
				Order.Action action = newOrder.getAction();
				
				// Set orderer if needed from encounter
				if (newOrder.getOrderer() == null) {
					newOrder.setOrderer(HtmlFormEntryUtil.getOrdererFromEncounter(encounter));
				}
				// Set dateActivated if needed from encounter
				if (newOrder.getDateActivated() == null) {
					String defaultDateActivated = widgetConfig.getTemplateConfig("dateActivated").get("value");
					if (StringUtils.isNotBlank(defaultDateActivated)) {
						if ("encounterDate".equalsIgnoreCase(defaultDateActivated)) {
							newOrder.setDateActivated(encounter.getEncounterDatetime());
						} else {
							throw new IllegalArgumentException("Unknown value for dateActivated: " + defaultDateActivated);
						}
					}
				}
				
				// Order Service does not allow a REVISE operation on a voided order, so ensure this is set to NEW
				if (v.isVoidPreviousOrder()) {
					if (action == Order.Action.REVISE) {
						newOrder.setAction(Order.Action.NEW);
					}
				}
				
				// Order Service does not support RENEW, so we have to try to do that here
				if (action == Order.Action.RENEW) {
					DrugOrder previousOrder = v.getPreviousDrugOrder();
					Field dateStoppedField = ReflectionUtils.findField(DrugOrder.class, "dateStopped");
					dateStoppedField.setAccessible(true);
					ReflectionUtils.setField(dateStoppedField, previousOrder, newOrder.getDateActivated());
				}
				
				encounter.addOrder(newOrder);
			}
		}
	}
	
	/**
	 * Removes all DrugOrders of the relevant Drug from existingOrders, and returns it.
	 *
	 * @return
	 */
	public Map<Drug, List<DrugOrder>> getDrugOrders(Patient patient, DrugOrderField drugOrderField) {
		Map<Drug, List<DrugOrder>> ret = new HashMap<>();
		if (patient != null && drugOrderField.getDrugOrderAnswers() != null) {
			Set<Drug> drugs = new HashSet<>();
			for (DrugOrderAnswer doa : drugOrderField.getDrugOrderAnswers()) {
				drugs.add(doa.getDrug());
			}
			ret = HtmlFormEntryUtil.getDrugOrdersForPatient(patient, drugs);
		}
		return ret;
	}
	
	protected boolean isSameDrug(DrugOrder current, DrugOrder previous) {
		boolean ret = true;
		ret = ret && OpenmrsUtil.nullSafeEquals(current.getDrug(), previous.getDrug());
		ret = ret && OpenmrsUtil.nullSafeEquals(current.getDrugNonCoded(), previous.getDrugNonCoded());
		return ret;
	}
	
	protected boolean dosingInstructionsChanged(DrugOrder current, DrugOrder previous) {
		boolean ret = false;
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getDosingType(), previous.getDosingType());
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getDosingInstructions(), previous.getDosingInstructions());
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getDose(), previous.getDose());
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getDoseUnits(), previous.getDoseUnits());
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getRoute(), previous.getRoute());
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getFrequency(), previous.getFrequency());
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getInstructions(), previous.getInstructions());
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getAsNeeded(), previous.getAsNeeded());
		ret = ret || !OpenmrsUtil.nullSafeEquals(current.getAsNeededCondition(), previous.getAsNeededCondition());
		return ret;
	}
	
	protected void addError(List<FormSubmissionError> errorList, DrugOrder drugOrder, String message) {
		String translatedMessage = Context.getMessageSourceService().getMessage(message);
		FormSubmissionError error = new FormSubmissionError(drugOrder.getDrug().getDisplayName(), translatedMessage);
		errorList.add(error);
	}
	
	public DrugOrdersWidget getDrugOrdersWidget() {
		return drugOrdersWidget;
	}
	
	public void setDrugOrdersWidget(DrugOrdersWidget drugOrdersWidget) {
		this.drugOrdersWidget = drugOrdersWidget;
	}
	
	public ErrorWidget getDrugOrdersErrorWidget() {
		return drugOrdersErrorWidget;
	}
	
	public void setDrugOrdersErrorWidget(ErrorWidget drugOrdersErrorWidget) {
		this.drugOrdersErrorWidget = drugOrdersErrorWidget;
	}
}
