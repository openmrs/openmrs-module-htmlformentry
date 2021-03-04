package org.openmrs.module.htmlformentry.element;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.Drug;
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
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.OrderWidget;
import org.openmrs.module.htmlformentry.widget.OrderWidgetConfig;
import org.openmrs.module.htmlformentry.widget.OrderWidgetValue;
import org.openmrs.util.OpenmrsUtil;

/**
 * Holds the widgets used to represent orders for a configured set of orderables (concepts, drugs),
 * and serves as both the HtmlGeneratorElement and the FormSubmissionControllerAction for the
 * associated orders.
 */
public class OrderSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	protected final Log log = LogFactory.getLog(OrderSubmissionElement.class);
	
	private OrderWidget orderWidget;
	
	private ErrorWidget orderErrorWidget;
	
	private List<Order> existingOrders;
	
	/**
	 * Instantiates a new Order Submission Element
	 */
	public OrderSubmissionElement(FormEntryContext context, OrderWidget orderWidget) {
		this.orderWidget = orderWidget;
		OrderWidgetConfig config = orderWidget.getWidgetConfig();
		orderWidget.setInitialValue(getOrders(context.getExistingPatient(), config));
		orderErrorWidget = new ErrorWidget();
		context.registerWidget(orderWidget);
		context.registerErrorWidget(orderWidget, orderErrorWidget);
		context.addFieldToActiveSection(config.getOrderField());
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
		html.append(orderWidget.generateHtml(context));
		if (context.getMode() != Mode.VIEW) {
			html.append(orderErrorWidget.generateHtml(context));
		}
		return html.toString();
	}
	
	/**
	 * Populate the list of existing orders that are associated with the context and the concepts
	 * configured in this tag
	 */
	protected void populateExistingOrders(FormEntryContext context) {
		existingOrders = new ArrayList<>();
		for (Concept c : orderWidget.getWidgetConfig().getConceptsAndDrugsConfigured().keySet()) {
			Order o = context.removeExistingOrder(c);
			while (o != null) {
				existingOrders.add(o);
				o = context.removeExistingOrder(c);
			}
		}
	}
	
	/**
	 * @see FormSubmissionControllerAction#validateSubmission(FormEntryContext, HttpServletRequest)
	 */
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext ctx, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<>();
		List<OrderWidgetValue> orderValues = orderWidget.getValue(ctx, submission);
		Map<Concept, List<Drug>> conceptsAndDrugs = orderWidget.getWidgetConfig().getConceptsAndDrugsConfigured();
		for (OrderWidgetValue v : orderValues) {
			Order newOrder = v.getNewOrder();
			if (newOrder != null) {
				String fs = v.getFieldSuffix();
				Order.Action action = newOrder.getAction();
				Order previousOrder = newOrder.getPreviousOrder();
				
				// If not a discontinue Order, then validate dosing
				if (action != Order.Action.DISCONTINUE) {
					
					handleRequiredField(ret, ctx, fs, "careSetting", newOrder.getCareSetting());
					handleRequiredField(ret, ctx, fs, "concept", newOrder.getConcept());
					
					if (newOrder.getUrgency() == Order.Urgency.ON_SCHEDULED_DATE) {
						handleRequiredField(ret, ctx, fs, "scheduledDate", newOrder.getScheduledDate());
					}
					
					if (newOrder instanceof DrugOrder) {
						DrugOrder newDrugOrder = (DrugOrder) newOrder;
						
						if (newDrugOrder.getDrug() != null && StringUtils.isNotBlank(newDrugOrder.getDrugNonCoded())) {
							String f = orderWidget.getFormErrorField(ctx, fs, "drugNonCoded");
							String errorCode = "htmlformentry.orders.specifyEitherDrugOrDrugNonCoded";
							ret.add(new FormSubmissionError(f, HtmlFormEntryUtil.translate(errorCode)));
						}
						
						if (newDrugOrder.getDrug() == null && StringUtils.isBlank(newDrugOrder.getDrugNonCoded())) {
							boolean isDrugRequired = getGlobalProperty("drugOrder.requireDrug", false);
							if (isDrugRequired) {
								List<Drug> drugsConfigured = conceptsAndDrugs.get(newDrugOrder.getConcept());
								if (drugsConfigured != null && !drugsConfigured.isEmpty()) {
									handleRequiredField(ret, ctx, fs, "drug", newDrugOrder.getDrug());
								} else {
									handleRequiredField(ret, ctx, fs, "drugNonCoded", newDrugOrder.getDrugNonCoded());
								}
							}
						}
						
						if (newDrugOrder.getDosingType() == SimpleDosingInstructions.class) {
							handleRequiredField(ret, ctx, fs, "dose", newDrugOrder.getDose());
							handleRequiredField(ret, ctx, fs, "doseUnits", newDrugOrder.getDoseUnits());
							handleRequiredField(ret, ctx, fs, "route", newDrugOrder.getRoute());
							handleRequiredField(ret, ctx, fs, "frequency", newDrugOrder.getFrequency());
						} else if (newDrugOrder.getDosingType() == FreeTextDosingInstructions.class) {
							String doseInstructions = newDrugOrder.getDosingInstructions();
							handleRequiredField(ret, ctx, fs, "dosingInstructions", doseInstructions);
						} else {
							String f = orderWidget.getFormErrorField(ctx, fs, "dosingType");
							String dosingTypeError = HtmlFormEntryUtil.translate("htmlformentry.orders.invalidDosingType");
							ret.add(new FormSubmissionError(f, dosingTypeError));
						}
						
						if (areQuantityFieldsRequired(newDrugOrder)) {
							handleRequiredField(ret, ctx, fs, "quantity", newDrugOrder.getQuantity());
							handleRequiredField(ret, ctx, fs, "quantityUnits", newDrugOrder.getQuantityUnits());
							handleRequiredField(ret, ctx, fs, "numRefills", newDrugOrder.getNumRefills());
						}
						
						if (newDrugOrder.getDuration() != null) {
							handleRequiredField(ret, ctx, fs, "durationUnits", newDrugOrder.getDurationUnits());
						}
						
						if (newDrugOrder.getQuantity() != null) {
							handleRequiredField(ret, ctx, fs, "quantityUnits", newDrugOrder.getQuantityUnits());
						}
					}
				}
				
				// If not a new Order, then this must have a previousOrder to operate on
				if (action != Order.Action.NEW) {
					String errorField = ctx.getFieldName(orderErrorWidget);
					if (previousOrder == null) {
						addError(ret, errorField, "htmlformentry.orders.previousOrderRequired");
					} else {
						if (!newOrder.hasSameOrderableAs(previousOrder) && BooleanUtils.isNotTrue(newOrder.getVoided())) {
							addError(ret, errorField, "htmlformentry.orders.drugChangedForRevision");
						}
						if (action == Order.Action.RENEW && newOrder instanceof DrugOrder) {
							if (dosingInstructionsChanged((DrugOrder) newOrder, (DrugOrder) previousOrder)) {
								addError(ret, errorField, "htmlformentry.orders.dosingChangedForRenew");
							}
						}
					}
				}
			}
		}
		return ret;
	}
	
	/**
	 * This is responsible for taking the passed OrderWidgetValue objects returned from the widget and
	 * issuing the appropriate void, NEW, RENEW, REVISE, DISCONTINUE orders, setting default values as
	 * needed
	 *
	 * @see FormSubmissionControllerAction#handleSubmission(FormEntrySession, HttpServletRequest)
	 */
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest request) {
		Encounter encounter = session.getSubmissionActions().getCurrentEncounter();
		for (OrderWidgetValue v : orderWidget.getValue(session.getContext(), request)) {
			
			Order newOrder = v.getNewOrder();
			Order previousOrder = v.getPreviousOrder();
			boolean createNew = (newOrder != null);
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
						if (newOrder.hasSameOrderableAs(previousOrder)) {
							if (newOrder.getDateActivated().equals(previousOrder.getDateActivated())) { // Same date activated
								voidPrevious = true;
								if (newOrder.getAction() == Order.Action.DISCONTINUE) {
									createNew = false;
								}
							}
						}
					}
				}
			}
			
			if (voidPrevious) {
				session.getSubmissionActions().getOrdersToVoid().add(previousOrder);
			}
			if (createNew) {
				session.getSubmissionActions().getOrdersToCreate().add(newOrder);
			}
		}
	}
	
	/**
	 * Retrieves the orders for the patient for the specified config
	 */
	public List<Order> getOrders(Patient patient, OrderWidgetConfig config) {
		List<Order> ret = new ArrayList<>();
		Set<Concept> configuredConcepts = config.getConceptsAndDrugsConfigured().keySet();
		if (patient != null && !configuredConcepts.isEmpty()) {
			ret = HtmlFormEntryUtil.getOrdersForPatient(patient, configuredConcepts);
		}
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
				String gpVal = getGlobalProperty(gpName, "true");
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
	
	protected <T> T getGlobalProperty(String name, T defaultValue) {
		return Context.getAdministrationService().getGlobalPropertyValue(name, defaultValue);
	}
	
	public void handleRequiredField(List<FormSubmissionError> ret, FormEntryContext ctx, String fieldSuffix, String prop,
	        Object val) {
		if (val == null || val.equals("")) {
			String field = orderWidget.getFormErrorField(ctx, fieldSuffix, prop);
			addError(ret, field, "htmlformentry.error.required");
		}
	}
	
	public void addError(List<FormSubmissionError> ret, String field, String messageCode) {
		ret.add(new FormSubmissionError(field, HtmlFormEntryUtil.translate(messageCode)));
	}
	
	public OrderWidget getOrderWidget() {
		return orderWidget;
	}
	
	public void setOrderWidget(OrderWidget orderWidget) {
		this.orderWidget = orderWidget;
	}
	
	public ErrorWidget getOrderErrorWidget() {
		return orderErrorWidget;
	}
	
	public void setOrderErrorWidget(ErrorWidget orderErrorWidget) {
		this.orderErrorWidget = orderErrorWidget;
	}
	
	public List<Order> getExistingOrders() {
		return existingOrders;
	}
}
