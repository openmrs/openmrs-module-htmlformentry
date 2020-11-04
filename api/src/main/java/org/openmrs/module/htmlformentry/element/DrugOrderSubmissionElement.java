package org.openmrs.module.htmlformentry.element;

import javax.servlet.http.HttpServletRequest;
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
import org.openmrs.module.htmlformentry.widget.DrugOrderWidget;
import org.openmrs.module.htmlformentry.widget.DrugOrderWidgetConfig;
import org.openmrs.module.htmlformentry.widget.DrugOrderWidgetValue;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
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
		DrugOrderField field = drugOrderWidget.getDrugOrderField();
		drugOrderWidget.setInitialValue(getDrugOrders(context.getExistingPatient(), field));
		drugOrderErrorWidget = new ErrorWidget();
		context.registerWidget(drugOrderWidget);
		context.registerErrorWidget(drugOrderWidget, drugOrderErrorWidget);
		context.addFieldToActiveSection(field);
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
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<>();
		List<DrugOrderWidgetValue> drugOrders = drugOrderWidget.getValue(context, submission);
		for (DrugOrderWidgetValue v : drugOrders) {
			DrugOrder drugOrder = v.getNewDrugOrder();
			if (drugOrder != null) {
				Order.Action action = drugOrder.getAction();
				DrugOrder previousOrder = (DrugOrder) drugOrder.getPreviousOrder();
				// If not a new Order, then this must have a previousOrder to operate on
				if (action != Order.Action.NEW) {
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
			boolean voidPrevious = v.isVoidPreviousOrder();
			
			// First we set some defaults on the new order
			
			if (newOrder != null) {
				// Set orderer if not specified to the provider on the encounter
				if (newOrder.getOrderer() == null) {
					newOrder.setOrderer(HtmlFormEntryUtil.getOrdererFromEncounter(encounter));
				}
				
				// By default, set the dateActivated to be the encounter date, if not specified
				if (newOrder.getDateActivated() == null) {
					Date dateActivated = encounter.getEncounterDatetime();
					String defaultDateActivated = widgetConfig.getAttributes("dateActivated").get("value");
					if (StringUtils.isNotBlank(defaultDateActivated)) {
						if ("entryDate".equalsIgnoreCase(defaultDateActivated)) {
							dateActivated = new Date();
						} else {
							throw new IllegalArgumentException("Unknown value for dateActivated: " + defaultDateActivated);
						}
					}
					newOrder.setDateActivated(dateActivated);
				}
				
				// Next, determine if this is a revision that should really be a void operation
				// We do this by determining if the new order is a revision within the same encounter, with same date activated
				if (!voidPrevious && previousOrder != null) {
					if (encounter.equals(previousOrder.getEncounter())) { // Same encounter
						if (newOrder.getDrug().equals(previousOrder.getDrug())) { // Same drug
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
		boolean ret = OpenmrsUtil.nullSafeEquals(current.getDrug(), previous.getDrug());
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
