package org.openmrs.module.htmlformentry.element;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.tag.DrugOrderTag;
import org.openmrs.module.htmlformentry.tag.TagUtil;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.ConceptDropdownWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.HiddenFieldWidget;
import org.openmrs.module.htmlformentry.widget.MetadataDropdownWidget;
import org.openmrs.module.htmlformentry.widget.NumberFieldWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.RadioButtonsWidget;
import org.openmrs.module.htmlformentry.widget.TextFieldWidget;
import org.openmrs.module.htmlformentry.widget.Widget;

/**
 * Holds the widgets used to represent a specific drug order, and serves as both the
 * HtmlGeneratorElement and the FormSubmissionControllerAction for the drug order.
 */
public class DrugOrderSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	protected final Log log = LogFactory.getLog(DrugOrderSubmissionElement.class);

	public enum Action { NEW, EDIT, DISCONTINUE, DELETE }
	
	private final DrugOrderTag tag;

	private List<DrugOrder> existingOrdersInEncounter = new ArrayList<>();
	private DrugOrder orderAvailable;
	
	private RadioButtonsWidget actionWidget;
	
	private final ErrorWidget actionErrorWidget = new ErrorWidget();
	
	private Widget drugWidget;
	
	private final ErrorWidget drugErrorWidget = new ErrorWidget();
	
	private TextFieldWidget dosingInstructionsWidget;
	
	protected final ErrorWidget dosingInstructionsErrorWidget = new ErrorWidget();
	
	private Widget doseWidget;
	
	protected final ErrorWidget doseErrorWidget = new ErrorWidget();
	
	private Widget doseUnitsWidget;
	
	private final ErrorWidget doseUnitsErrorWidget = new ErrorWidget();
	
	private Widget routeWidget;
	
	private final ErrorWidget routeErrorWidget = new ErrorWidget();
	
	private Widget frequencyWidget;
	
	protected final ErrorWidget frequencyErrorWidget = new ErrorWidget();
	
	private Widget asNeededWidget;
	
	protected final ErrorWidget asNeededErrorWidget = new ErrorWidget();
	
	private DateWidget startDateWidget;
	
	private final ErrorWidget startDateErrorWidget = new ErrorWidget();
	
	private NumberFieldWidget durationWidget;
	
	private final ErrorWidget durationErrorWidget = new ErrorWidget();
	
	private Widget durationUnitsWidget;
	
	private final ErrorWidget durationUnitsErrorWidget = new ErrorWidget();
	
	private NumberFieldWidget quantityWidget;
	
	private final ErrorWidget quantityErrorWidget = new ErrorWidget();
	
	private Widget quantityUnitsWidget;
	
	private final ErrorWidget quantityUnitsErrorWidget = new ErrorWidget();
	
	private NumberFieldWidget numRefillsWidget;
	
	private final ErrorWidget numRefillsErrorWidget = new ErrorWidget();
	
	private TextFieldWidget instructionsWidget;
	
	private final ErrorWidget instructionsErrorWidget = new ErrorWidget();

	private DropdownWidget discontinuedReasonWidget;

	private ErrorWidget discontinuedReasonErrorWidget = new ErrorWidget();
	
	/**
	 * Instantiates a new Drug Order Submission Element, for the given Drug and Context
	 */
	public DrugOrderSubmissionElement(FormEntryContext context, DrugOrderTag tag) {

		this.tag = tag;
		populateExistingOrdersForDrug(context, tag);

		DrugOrder latestOrder = getLatestOrderForViewOrRevision();
		DrugOrder orderDefaults = null;
		DrugOrder discontinueDefaults = null;
		if (latestOrder != null) {
			if (latestOrder.getAction() == Order.Action.DISCONTINUE) {
				orderDefaults = (DrugOrder)latestOrder.getPreviousOrder();
				discontinueDefaults = latestOrder;
			}
			else {
				orderDefaults = latestOrder;
			}
		}
		if (orderDefaults == null) {
			orderDefaults = orderAvailable;
		}

		// Action Widget
		actionWidget = new RadioButtonsWidget();
		for (Action action : Action.values()) {
			actionWidget.addOption(new Option(action.name(), action.name(), false));
		}
		registerWidgets(context, actionWidget, actionErrorWidget);
		
		// Drug Widget
		drugWidget = new HiddenFieldWidget();
		drugWidget.setInitialValue(tag.getDrug().getId().toString());
		registerWidgets(context, drugWidget, drugErrorWidget);
		
		// Dose Widget
		doseWidget = new NumberFieldWidget(0d, 9999999d, true);
		doseWidget.setInitialValue(orderDefaults == null ? tag.getDefaultDose() : orderDefaults.getDose());
		registerWidgets(context, doseWidget, doseErrorWidget);
		
		// Dose Units Widget
		doseUnitsWidget = new ConceptDropdownWidget(Context.getOrderService().getDrugDosingUnits());
		doseUnitsWidget.setInitialValue(orderDefaults == null ? null : orderDefaults.getDoseUnits());
		registerWidgets(context, doseUnitsWidget, doseUnitsErrorWidget);
		
		// Route Widget
		routeWidget = new ConceptDropdownWidget(Context.getOrderService().getDrugRoutes());
		routeWidget.setInitialValue(orderDefaults == null ? null : orderDefaults.getRoute());
		registerWidgets(context, routeWidget, routeErrorWidget);
		
		// Frequency Widget
		frequencyWidget = new MetadataDropdownWidget(Context.getOrderService().getOrderFrequencies(false));
		frequencyWidget.setInitialValue(orderDefaults == null ? null : orderDefaults.getFrequency());
		registerWidgets(context, frequencyWidget, frequencyErrorWidget);
		
		// Dosing Instructions Widget
		dosingInstructionsWidget = new TextFieldWidget();
		dosingInstructionsWidget.setInitialValue(orderDefaults == null ? null : orderDefaults.getDosingInstructions());
		registerWidgets(context, dosingInstructionsWidget, dosingInstructionsErrorWidget);
		
		// As-Needed Widget
		asNeededWidget = new CheckboxWidget("true", translate(tag.getAsNeededLabel()));
		asNeededWidget.setInitialValue(orderDefaults == null ? null : orderDefaults.getAsNeeded());
		registerWidgets(context, asNeededWidget, asNeededErrorWidget);
		
		// Start Date Widget
		startDateWidget = new DateWidget();
		startDateWidget.setInitialValue(orderDefaults == null ? null : orderDefaults.getEffectiveStartDate());
		registerWidgets(context, startDateWidget, startDateErrorWidget);
		
		// Duration Widget
		durationWidget = new NumberFieldWidget(0d, 9999999d, false);
		durationWidget.setInitialValue(orderDefaults == null ? null : orderDefaults.getDuration());
		registerWidgets(context, durationWidget, durationErrorWidget);
		
		durationUnitsWidget = new ConceptDropdownWidget(Context.getOrderService().getDurationUnits());
		durationUnitsWidget.setInitialValue(orderDefaults == null ? null : orderDefaults.getDurationUnits());
		registerWidgets(context, durationUnitsWidget, durationUnitsErrorWidget);
		
		// Instructions Widget
		instructionsWidget = new TextFieldWidget();
		instructionsWidget.setInitialValue(orderDefaults == null ? null : orderDefaults.getInstructions());
		registerWidgets(context, instructionsWidget, instructionsErrorWidget);
		
		// Quantity Widget
		quantityWidget = new NumberFieldWidget(0d, 9999999d, true);
		quantityWidget.setInitialValue(orderDefaults == null ? null : orderDefaults.getQuantity());
		registerWidgets(context, quantityWidget, quantityErrorWidget);
		
		// Quantity Units Widget
		quantityUnitsWidget = new ConceptDropdownWidget(Context.getOrderService().getDrugDispensingUnits());
		quantityUnitsWidget.setInitialValue(orderDefaults == null ? null : orderDefaults.getQuantityUnits());
		registerWidgets(context, quantityUnitsWidget, quantityUnitsErrorWidget);
		
		// Number of Refills
		numRefillsWidget = new NumberFieldWidget(0d, 9999999d, false);
		numRefillsWidget.setInitialValue(orderDefaults == null ? null : orderDefaults.getNumRefills());
		registerWidgets(context, numRefillsWidget, numRefillsErrorWidget);

		// Discontinued Reason Widgets
		discontinuedReasonWidget = new ConceptDropdownWidget(tag.getDiscontinueAnswers());
		if (discontinueDefaults != null) {
			discontinuedReasonWidget.setInitialValue(discontinueDefaults.getOrderReason());
		}
		registerWidgets(context, discontinuedReasonWidget, discontinuedReasonErrorWidget);

		context.addFieldToActiveSection(tag.getDrugOrderField());
	}
	
	/**
	 * <strong>Should</strong> return HTML snippet
	 * 
	 * @see HtmlGeneratorElement#generateHtml(FormEntryContext)
	 */
	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder ret = new StringBuilder();
		startSpan(ret, "drugOrderField");
		append(ret, context, "action", null, actionWidget, actionErrorWidget);
		append(ret, context, "drug", tag.getDrugDisplayName(), drugWidget, drugErrorWidget);
		append(ret, context, "dose", "DrugOrder.dose", doseWidget, doseErrorWidget);
		append(ret, context, "doseUnits", null, doseUnitsWidget, doseUnitsErrorWidget);
		append(ret, context, "route", null, routeWidget, routeErrorWidget);
		append(ret, context, "frequency", null, frequencyWidget, frequencyErrorWidget);
		append(ret, context, "asNeeded", null, asNeededWidget, asNeededErrorWidget);
		append(ret, context, "dosingInstructions", null, dosingInstructionsWidget, dosingInstructionsErrorWidget);
		append(ret, context, "startDate", "general.dateStart", startDateWidget, startDateErrorWidget);
		append(ret, context, "duration", "htmlformentry.general.for", durationWidget, durationErrorWidget);
		append(ret, context, "durationUnits", null, durationUnitsWidget, durationUnitsErrorWidget);
		append(ret, context, "quantity", "DrugOrder.quantity", quantityWidget, quantityErrorWidget);
		append(ret, context, "quantityUnits", null, quantityUnitsWidget, quantityUnitsErrorWidget);
		append(ret, context, "instructions", tag.getInstructionsLabel(), instructionsWidget, instructionsErrorWidget);
		append(ret, context, "numRefills", "htmlformentry.drugOrder.numRefills", numRefillsWidget, numRefillsErrorWidget);
		append(ret, context, "discontinuedReason", "general.discontinuedReason", discontinuedReasonWidget, discontinuedReasonErrorWidget);
		endSpan(ret);
		return ret.toString();
	}
	
	/**
	 * For now, handle the following use cases: START: create new order STOP: stop order, create
	 * discontinue order EDIT: void / associate appropriate order and/or discontinue order
	 * 
	 * @see FormSubmissionControllerAction#handleSubmission(FormEntrySession, HttpServletRequest)
	 */
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest request) {
		Action action = widgetVal(actionWidget, session, request, Action.class);
		if (action == null) {
			log.trace("No action requested for drug: " + drugWidget.getValue(session.getContext(), request));
		} else {
			DrugOrder existingOrder = getLatestOrderForViewOrRevision();
			if (action == Action.NEW) {
				if (existingOrder != null) {
					throw new IllegalStateException("Cannot place a NEW order when existing Order is active for Drug");
				} else {
					handleNewOrder(session, request);
				}
			} else if (action == Action.EDIT) {
				if (existingOrder == null) {
					throw new IllegalStateException("Cannot REVISE order since existing Drug Order is not found");
				}
				handleEditOrder(session, request);
			} else if (action == Action.DISCONTINUE) {
				if (existingOrder == null) {
					throw new IllegalStateException("Cannot DISCONTINUE order since existing Drug Order is not found");
				}
				handleDiscontinueOrder(session, request);
			}
			else if (action == Action.DELETE) {
				DrugOrder orderToVoid = getLatestOrderForViewOrRevision();
				voidOrder(orderToVoid);
			}
			else {
				throw new IllegalStateException("Only START, EDIT, STOP, and DELETE actions are supported");
			}
		}
	}
	
	/**
	 * Responsible for creating and saving a new DrugOrder
	 */
	protected void handleNewOrder(FormEntrySession session, HttpServletRequest request) {
		DrugOrder drugOrder = populateDrugOrderFromRequest(session, request, new DrugOrder());
		log.debug("Adding new Drug Order for " + drugOrder.getDrug().getDisplayName());
		session.getSubmissionActions().getCurrentEncounter().addOrder(drugOrder);
	}
	
	/**
	 * Responsible for editing/revising an existing DrugOrder
	 */
	protected void handleEditOrder(FormEntrySession session, HttpServletRequest request) {
		// For now, don't try to get the REVISE functionality working.  Just void and recreate.
		DrugOrder previousOrder = getLatestOrderForViewOrRevision();
		voidOrder(previousOrder);
		handleNewOrder(session, request);
	}

	/**
	 * Responsible for stopping/discontinuing an existing DrugOrder
	 */
	protected void handleDiscontinueOrder(FormEntrySession session, HttpServletRequest request) {
		DrugOrder previousOrder = getLatestOrderForViewOrRevision();
		DrugOrder newOrder = populateDrugOrderFromRequest(session, request, previousOrder.cloneForDiscontinuing());
		newOrder.setOrderReason(widgetVal(discontinuedReasonWidget, session, request, Concept.class));
		log.debug("Discontinuing order " + newOrder.getDrug().getDisplayName());
		session.getSubmissionActions().getCurrentEncounter().addOrder(newOrder);
	}

	/**
	 * Responsible for voiding an Order
	 */
	protected void voidOrder(DrugOrder drugOrder) {
		drugOrder.setVoided(true);
		drugOrder.setVoidedBy(Context.getAuthenticatedUser());
		drugOrder.setDateVoided(new Date());
		drugOrder.setVoidReason("Voided by htmlformentry due to edited Order details");
		log.debug("Voided previous Drug Order for " + drugOrder.getDrug().getDisplayName());
	}

	protected DrugOrder populateDrugOrderFromRequest(FormEntrySession session, HttpServletRequest request, DrugOrder o) {
		o.setPatient(session.getPatient());
		o.setEncounter(session.getEncounter());
		o.setDrug(tag.getDrug());
		o.setDosingType(tag.getDosingInstructionsType());
		o.setCareSetting(tag.getCareSetting());
		o.setOrderType(HtmlFormEntryUtil.getDrugOrderType());
		o.setOrderer(HtmlFormEntryUtil.getOrdererFromEncounter(session.getEncounter()));
		o.setDose(widgetVal(doseWidget, session, request, Double.class));
		o.setDoseUnits(widgetVal(doseUnitsWidget, session, request, Concept.class));
		o.setRoute(widgetVal(routeWidget, session, request, Concept.class));
		o.setFrequency(widgetVal(frequencyWidget, session, request, OrderFrequency.class));
		o.setAsNeeded(widgetVal(asNeededWidget, session, request, Boolean.class, false));
		o.setDosingInstructions(widgetVal(dosingInstructionsWidget, session, request, String.class));
		o.setDuration(widgetVal(durationWidget, session, request, Integer.class));
		o.setDurationUnits(widgetVal(durationUnitsWidget, session, request, Concept.class));
		o.setQuantity(widgetVal(quantityWidget, session, request, Double.class));
		o.setQuantityUnits(widgetVal(quantityUnitsWidget, session, request, Concept.class));
		o.setInstructions(widgetVal(instructionsWidget, session, request, String.class));
		o.setNumRefills(widgetVal(numRefillsWidget, session, request, Integer.class));
		o.setVoided(false);

		// The dateActivated of an order must not be in the future or after the date of the associated encounter
		// This means we always need to set the dateActivated to the encounterDatetime
		Date encDate = session.getEncounter().getEncounterDatetime();
		o.setDateActivated(encDate);
		o.setUrgency(Order.Urgency.ROUTINE);

		// If the startDate indicated on the orderTag is after the encounterDatetime, then make this a future order
		Date startDate = widgetVal(startDateWidget, session, request, Date.class);
		if (startDate != null) {
			Date startDay = HtmlFormEntryUtil.startOfDay(startDate);
			Date encounterDay = HtmlFormEntryUtil.startOfDay(encDate);
			if (startDay.before(encounterDay)) {
				throw new IllegalStateException("Unable to start an order prior to the encounter date");
			}
			else if (startDay.after(encounterDay)) {
				o.setScheduledDate(startDate);
				o.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
			}
		}
		return o;
	}

	protected void populateExistingOrdersForDrug(FormEntryContext context, DrugOrderTag tag) {
		Encounter currentEnc = context.getExistingEncounter();
		List<Order> existingOrders = Context.getOrderService().getAllOrdersByPatient(context.getExistingPatient());
		if (existingOrders != null) {
			for (Order order : existingOrders) {
				if (order instanceof DrugOrder) {
					DrugOrder drugOrder = (DrugOrder) order;
					if (drugOrder.getDrug().equals(tag.getDrug())) {
						if (currentEnc != null && currentEnc.equals(order.getEncounter())) {
							existingOrdersInEncounter.add(drugOrder);
						} else {
							Date dateToCheck = (currentEnc == null ? new Date() : currentEnc.getEncounterDatetime());
							if (order.isActive(dateToCheck)) {
								orderAvailable = drugOrder;
							}
						}
					}
				}
			}
		}
	}

	protected DrugOrder getLatestOrderForViewOrRevision() {
		Set<Order> revisedOrders = new HashSet<>();
		for (Order drugOrder : getExistingOrdersInEncounter()) {
			while (drugOrder.getPreviousOrder() != null) {
				revisedOrders.add(drugOrder.getPreviousOrder());
				drugOrder = drugOrder.getPreviousOrder();
			}
		}
		List<DrugOrder> nonRevisedOrders = new ArrayList<>();
		for (DrugOrder drugOrder : getExistingOrdersInEncounter()) {
			if (!revisedOrders.contains(drugOrder)) {
				nonRevisedOrders.add(drugOrder);
			}
		}
		if (nonRevisedOrders.size() > 1) {
			DrugOrder latestEntered = null;
			for (DrugOrder drugOrder : nonRevisedOrders) {
				if (latestEntered == null || drugOrder.getDateCreated().after(latestEntered.getDateCreated())) {
					latestEntered = drugOrder;
				}
			}
			return latestEntered;
		}
		else if (nonRevisedOrders.size() == 1) {
			return nonRevisedOrders.get(0);
		}
		return null;
	}

	public List<DrugOrder> getExistingOrdersInEncounter() {
		return existingOrdersInEncounter;
	}

	protected void registerWidgets(FormEntryContext context, Widget fieldWidget, ErrorWidget errorWidget) {
		context.registerWidget(fieldWidget);
		context.registerErrorWidget(fieldWidget, errorWidget);
	}

	protected <T> T widgetVal(Widget w, FormEntrySession session, HttpServletRequest request, Class<T> type) {
		if (w != null) {
			Object o = w.getValue(session.getContext(), request);
			if (o != null) {
				if (o instanceof Date) {
					return (T) o;
				}
				return TagUtil.parseValue(o.toString(), type);
			}
		}
		return null;
	}

	protected <T> T widgetVal(Widget w, FormEntrySession session, HttpServletRequest request, Class<T> type, T defaultVal) {
		T val = widgetVal(w, session, request, type);
		if (val == null) {
			return defaultVal;
		}
		return val;
	}

	protected String translate(String code) {
		return Context.getMessageSourceService().getMessage(code);
	}

	protected String append(StringBuilder html, FormEntryContext ctx, String cssClass, String label, Widget w, Widget ew) {
		if (w != null) {
			if (label != null) {
				startSpan(html, cssClass + " fieldLabel");
				html.append(translate(label));
				endSpan(html);
			}
			startSpan(html, cssClass + " field");
			html.append(w.generateHtml(ctx));
			endSpan(html);
			if (ctx.getMode() != Mode.VIEW && ew != null) {
				html.append(ew.generateHtml(ctx));
			}
			html.append("\r\n");
		}
		return html.toString();
	}

	protected void startSpan(StringBuilder html, String cssClass) {
		html.append("<span class=\"").append(cssClass).append("\">");
	}

	protected void endSpan(StringBuilder html) {
		html.append("</span>\r\n");
	}

	/**
	 * <strong>Should</strong> return validation errors if any data is invalid
	 * 
	 * @see FormSubmissionControllerAction#validateSubmission(FormEntryContext, HttpServletRequest)
	 */
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		/*
		try {
			if (drugWidget != null && drugWidget.getValue(context, submission) != null
			        && ((String) drugWidget.getValue(context, submission)).equals("~"))
				throw new IllegalArgumentException("htmlformentry.error.cannotChooseADrugHeader");
		}
		catch (Exception ex) {
			ret.add(new FormSubmissionError(context.getFieldName(drugErrorWidget),
			        Context.getMessageSourceService().getMessage(ex.getMessage())));
		}
		//if no drug specified, then don't do anything.
		if (drugWidget != null && drugWidget.getValue(context, submission) != null
		        && !((String) drugWidget.getValue(context, submission)).trim().equals("")
		        && !((String) drugWidget.getValue(context, submission)).trim().equals("~")) {
			try {
				if (doseWidget != null) {
					Double dose = (Double) doseWidget.getValue(context, submission);
					if (dose == null)
						throw new Exception("htmlformentry.error.required");
					
					// min max
					if (validateDose) {
						String drugID = (String) drugWidget.getValue(context, submission);
						Drug drug = Context.getConceptService().getDrug(drugID);
						if ((drug.getMinimumDailyDose() != null && dose < drug.getMinimumDailyDose())
						        || (drug.getMaximumDailyDose() != null && dose > drug.getMaximumDailyDose())) {
							throw new IllegalArgumentException("htmlformentry.error.doseOutOfRange");
						}
					}
				}
			}
			catch (Exception ex) {
				ret.add(new FormSubmissionError(context.getFieldName(doseErrorWidget),
				        Context.getMessageSourceService().getMessage(ex.getMessage())));
			}
			try {
				if (startDateWidget != null) {
					Date dateCreated = startDateWidget.getValue(context, submission);
					if (dateCreated == null)
						throw new Exception("htmlformentry.error.required");
				}
			}
			catch (Exception ex) {
				ret.add(new FormSubmissionError(context.getFieldName(startDateErrorWidget),
				        Context.getMessageSourceService().getMessage(ex.getMessage())));
			}
			try {
				if (startDateWidget != null && discontinuedDateWidget != null) {
					Date startDate = startDateWidget.getValue(context, submission);
					Date endDate = discontinuedDateWidget.getValue(context, submission);
					if (startDate != null && endDate != null && startDate.getTime() > endDate.getTime())
						throw new Exception("htmlformentry.error.discontinuedDateBeforeStartDate");
				}
			}
			catch (Exception ex) {
				ret.add(new FormSubmissionError(context.getFieldName(discontinuedDateErrorWidget),
				        Context.getMessageSourceService().getMessage(ex.getMessage())));
			}
			try {
				if (discontinuedReasonWidget != null && discontinuedDateWidget != null) {
					String discReason = (String) discontinuedReasonWidget.getValue(context, submission);
					Date endDate = discontinuedDateWidget.getValue(context, submission);
					if (endDate == null && !StringUtils.isEmpty(discReason))
						throw new Exception("htmlformentry.error.discontinuedReasonEnteredWithoutDate");
				}
			}
			catch (Exception ex) {
				ret.add(new FormSubmissionError(context.getFieldName(discontinuedReasonErrorWidget),
				        Context.getMessageSourceService().getMessage(ex.getMessage())));
			}
			try {
				if (orderDurationWidget != null && orderDurationWidget.getValue(context, submission) != null) {
					String orderDurationVal = (String) orderDurationWidget.getValue(context, submission);
					if (!orderDurationVal.equals("")) {
						try {
							Integer.valueOf(orderDurationVal);
						}
						catch (Exception ex) {
							throw new Exception("htmlformentry.error.durationMustBeEmptyOrNumeric");
						}
					}
				}
			}
			catch (Exception ex) {
				ret.add(new FormSubmissionError(context.getFieldName(orderDurationErrorWidget),
				        Context.getMessageSourceService().getMessage(ex.getMessage())));
			}
		}
		*/
		return ret;
	}
}
