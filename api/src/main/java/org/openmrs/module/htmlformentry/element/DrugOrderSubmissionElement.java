package org.openmrs.module.htmlformentry.element;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
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
public class DrugOrderSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction, GettingExistingOrder {
	
	protected final Log log = LogFactory.getLog(DrugOrderSubmissionElement.class);
	
	private final DrugOrderTag tag;
	
	private final DrugOrder existingOrder;
	
	private RadioButtonsWidget orderActionWidget;
	
	private final ErrorWidget orderActionErrorWidget = new ErrorWidget();
	
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
	
	/**
	 * Instantiates a new Drug Order Submission Element, for the given Drug and Context
	 */
	public DrugOrderSubmissionElement(FormEntryContext context, DrugOrderTag tag) {
		
		this.tag = tag;
		this.existingOrder = context.removeExistingDrugOrder(tag.getDrug());
		
		// Action Widget
		orderActionWidget = new RadioButtonsWidget();
		orderActionWidget.addOption(new Option(Order.Action.NEW.name(), Order.Action.NEW.name(), false));
		orderActionWidget.addOption(new Option(Order.Action.REVISE.name(), Order.Action.REVISE.name(), false));
		orderActionWidget.addOption(new Option(Order.Action.DISCONTINUE.name(), Order.Action.DISCONTINUE.name(), false));
		registerWidgets(context, orderActionWidget, orderActionErrorWidget);
		
		// Drug Widget
		drugWidget = new HiddenFieldWidget();
		drugWidget.setInitialValue(tag.getDrug().getId().toString());
		registerWidgets(context, drugWidget, drugErrorWidget);
		
		// Dose Widget
		doseWidget = new NumberFieldWidget(0d, 9999999d, true);
		doseWidget.setInitialValue(existingOrder == null ? tag.getDefaultDose() : existingOrder.getDose());
		registerWidgets(context, doseWidget, doseErrorWidget);
		
		// Dose Units Widget
		doseUnitsWidget = new ConceptDropdownWidget(Context.getOrderService().getDrugDosingUnits());
		doseUnitsWidget.setInitialValue(existingOrder == null ? null : existingOrder.getDoseUnits());
		registerWidgets(context, doseUnitsWidget, doseUnitsErrorWidget);
		
		// Route Widget
		routeWidget = new ConceptDropdownWidget(Context.getOrderService().getDrugRoutes());
		routeWidget.setInitialValue(existingOrder == null ? null : existingOrder.getRoute());
		registerWidgets(context, routeWidget, routeErrorWidget);
		
		// Frequency Widget
		frequencyWidget = new MetadataDropdownWidget(Context.getOrderService().getOrderFrequencies(false));
		frequencyWidget.setInitialValue(existingOrder == null ? null : existingOrder.getFrequency());
		registerWidgets(context, frequencyWidget, frequencyErrorWidget);
		
		// Dosing Instructions Widget
		dosingInstructionsWidget = new TextFieldWidget();
		dosingInstructionsWidget.setInitialValue(existingOrder == null ? null : existingOrder.getDosingInstructions());
		registerWidgets(context, dosingInstructionsWidget, dosingInstructionsErrorWidget);
		
		// As-Needed Widget
		asNeededWidget = new CheckboxWidget("true", translate(tag.getAsNeededLabel()));
		asNeededWidget.setInitialValue(existingOrder == null ? null : existingOrder.getAsNeeded());
		registerWidgets(context, asNeededWidget, asNeededErrorWidget);
		
		// Start Date Widget
		startDateWidget = new DateWidget();
		startDateWidget.setInitialValue(existingOrder == null ? null : existingOrder.getEffectiveStartDate());
		registerWidgets(context, startDateWidget, startDateErrorWidget);
		
		// Duration Widget
		durationWidget = new NumberFieldWidget(0d, 9999999d, false);
		durationWidget.setInitialValue(existingOrder == null ? null : existingOrder.getDuration());
		registerWidgets(context, durationWidget, durationErrorWidget);
		
		durationUnitsWidget = new ConceptDropdownWidget(Context.getOrderService().getDurationUnits());
		durationUnitsWidget.setInitialValue(existingOrder == null ? null : existingOrder.getDurationUnits());
		registerWidgets(context, durationUnitsWidget, durationUnitsErrorWidget);
		
		// Instructions Widget
		instructionsWidget = new TextFieldWidget();
		instructionsWidget.setInitialValue(existingOrder == null ? null : existingOrder.getInstructions());
		registerWidgets(context, instructionsWidget, instructionsErrorWidget);
		
		// Quantity Widget
		quantityWidget = new NumberFieldWidget(0d, 9999999d, true);
		quantityWidget.setInitialValue(existingOrder == null ? null : existingOrder.getQuantity());
		registerWidgets(context, quantityWidget, quantityErrorWidget);
		
		// Quantity Units Widget
		quantityUnitsWidget = new ConceptDropdownWidget(Context.getOrderService().getDrugDispensingUnits());
		quantityUnitsWidget.setInitialValue(existingOrder == null ? null : existingOrder.getQuantityUnits());
		registerWidgets(context, quantityUnitsWidget, quantityUnitsErrorWidget);
		
		// Number of Refills
		numRefillsWidget = new NumberFieldWidget(0d, 9999999d, false);
		numRefillsWidget.setInitialValue(existingOrder == null ? null : existingOrder.getNumRefills());
		registerWidgets(context, numRefillsWidget, numRefillsErrorWidget);
		
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
		append(ret, context, "orderAction", null, orderActionWidget, orderActionErrorWidget);
		append(ret, context, "drug", tag.getDrugDisplayName(), drugWidget, drugErrorWidget);
		append(ret, context, "dose", "DrugOrder.dose", doseWidget, doseErrorWidget);
		append(ret, context, "doseUnits", null, doseUnitsWidget, doseUnitsErrorWidget);
		append(ret, context, "route", null, routeWidget, routeErrorWidget);
		append(ret, context, "frequency", null, frequencyWidget, frequencyErrorWidget);
		append(ret, context, "dosingInstructions", null, dosingInstructionsWidget, dosingInstructionsErrorWidget);
		append(ret, context, "startDate", "general.dateStart", startDateWidget, startDateErrorWidget);
		append(ret, context, "duration", "htmlformentry.general.for", durationWidget, durationErrorWidget);
		append(ret, context, "durationUnits", null, durationUnitsWidget, durationUnitsErrorWidget);
		append(ret, context, "quantity", "DrugOrder.quantity", quantityWidget, quantityErrorWidget);
		append(ret, context, "quantityUnits", null, quantityUnitsWidget, quantityUnitsErrorWidget);
		append(ret, context, "instructions", tag.getInstructionsLabel(), instructionsWidget, instructionsErrorWidget);
		append(ret, context, "numRefills", "htmlformentry.drugOrder.numRefills", numRefillsWidget, numRefillsErrorWidget);
		endSpan(ret);
		return ret.toString();
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
	
	@Override
	public DrugOrder getExistingOrder() {
		return existingOrder;
	}
	
	/**
	 * For now, handle the following use cases: START: create new order STOP: stop order, create
	 * discontinue order EDIT: void / associate appropriate order and/or discontinue order
	 * 
	 * @see FormSubmissionControllerAction#handleSubmission(FormEntrySession, HttpServletRequest)
	 */
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest request) {
		Order.Action action = widgetVal(orderActionWidget, session, request, Order.Action.class);
		if (action == null) {
			log.trace("No action requested for drug: " + drugWidget.getValue(session.getContext(), request));
		} else {
			if (action == Order.Action.NEW) {
				if (existingOrder != null) {
					throw new IllegalStateException("Cannot place a NEW order when existing Order is active for Drug");
				} else {
					handleNewOrder(session, request);
				}
			} else if (action == Order.Action.REVISE) {
				if (existingOrder == null) {
					throw new IllegalStateException("Cannot REVISE order since existing Drug Order is not found");
				}
				handleReviseOrder(session, request);
			} else if (action == Order.Action.DISCONTINUE) {
				if (existingOrder == null) {
					throw new IllegalStateException("Cannot DISCONTINUE order since existing Drug Order is not found");
				}
				handleDiscontinueOrder(session, request);
			} else {
				throw new IllegalStateException("Only NEW, REVISE, and DISCONTINUE actions are supported");
			}
		}
	}
	
	/**
	 * Responsible for creating and saving a new DrugOrder
	 */
	protected void handleNewOrder(FormEntrySession session, HttpServletRequest request) {
		DrugOrder drugOrder = new DrugOrder();
		drugOrder.setPatient(session.getPatient());
		drugOrder.setEncounter(session.getEncounter());
		drugOrder.setDrug(tag.getDrug());
		drugOrder.setDosingType(tag.getDosingInstructionsType());
		drugOrder.setCareSetting(tag.getCareSetting());
		drugOrder.setOrderType(HtmlFormEntryUtil.getDrugOrderType());
		drugOrder.setOrderer(HtmlFormEntryUtil.getOrdererFromEncounter(session.getEncounter()));
		drugOrder.setDose(widgetVal(doseWidget, session, request, Double.class));
		drugOrder.setDoseUnits(widgetVal(doseUnitsWidget, session, request, Concept.class));
		drugOrder.setRoute(widgetVal(routeWidget, session, request, Concept.class));
		drugOrder.setFrequency(widgetVal(frequencyWidget, session, request, OrderFrequency.class));
		drugOrder.setDosingInstructions(widgetVal(dosingInstructionsWidget, session, request, String.class));
		drugOrder.setDuration(widgetVal(durationWidget, session, request, Integer.class));
		drugOrder.setDurationUnits(widgetVal(durationUnitsWidget, session, request, Concept.class));
		drugOrder.setQuantity(widgetVal(quantityWidget, session, request, Double.class));
		drugOrder.setQuantityUnits(widgetVal(quantityUnitsWidget, session, request, Concept.class));
		drugOrder.setInstructions(widgetVal(instructionsWidget, session, request, String.class));
		drugOrder.setNumRefills(widgetVal(numRefillsWidget, session, request, Integer.class));
		drugOrder.setVoided(false);
		
		// The dateActivated of an order must not be in the future or after the date of the associated encounter
		// This means we always need to set the dateActivated to the encounterDatetime
		Date encDate = session.getEncounter().getEncounterDatetime();
		drugOrder.setDateActivated(encDate);
		drugOrder.setUrgency(Order.Urgency.ROUTINE);
		
		// If the startDate indicated on the orderTag is after the encounterDatetime, then make this a future order
		Date startDate = widgetVal(startDateWidget, session, request, Date.class);
		if (startDate != null) {
			Date startDay = HtmlFormEntryUtil.startOfDay(startDate);
			Date encounterDay = HtmlFormEntryUtil.startOfDay(encDate);
			if (startDay.before(encounterDay)) {
				throw new IllegalStateException("Unable to start an order prior to the encounter date");
			} else if (startDay.after(encounterDay)) {
				drugOrder.setScheduledDate(startDate);
				drugOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
			}
		}
		
		log.debug("Adding new Drug Order for " + drugOrder.getDrug().getDisplayName());
		session.getSubmissionActions().getCurrentEncounter().addOrder(drugOrder);
	}
	
	/**
	 * Responsible for editing/revising an existing DrugOrder
	 */
	protected void handleReviseOrder(FormEntrySession session, HttpServletRequest request) {
		
	}
	
	/**
	 * Responsible for stopping/discontinuing an existing DrugOrder
	 */
	protected void handleDiscontinueOrder(FormEntrySession session, HttpServletRequest request) {
		
	}
	
	/*
	protected void editOrder(FormEntrySession session, OrderTag oldOrderTag) {
		OrderTag orderTag = (OrderTag) oldOrderTag;
		DrugOrder discontinuationOrder = null;
		
		if (!existingOrder.getAction().equals(Order.Action.DISCONTINUE)) {
			//Discontinued orders must not be changed except for discontinue date and reason
			DrugOrder revisedOrder = existingOrder.cloneForRevision();
			setOrderer(session, revisedOrder);
			revisedOrder.setDrug(orderTag.drug);
			revisedOrder.setConcept(orderTag.drug.getConcept());
			revisedOrder.setDosingType(orderTag.dosingType);
			revisedOrder.setDose(orderTag.dose);
			revisedOrder.setDoseUnits(orderTag.doseUnits);
			revisedOrder.setQuantity(orderTag.quantity);
			revisedOrder.setQuantityUnits(orderTag.quantityUnits);
			revisedOrder.setDuration(orderTag.duration);
			revisedOrder.setDurationUnits(orderTag.durationUnits);
			revisedOrder.setRoute(orderTag.route);
			revisedOrder.setCareSetting(Context.getOrderService().getCareSetting(orderTag.careSettingId));
			OrderFrequency orderFrequency = Context.getOrderService().getOrderFrequency(Integer.valueOf(orderTag.frequency));
			revisedOrder.setFrequency(orderFrequency);
			revisedOrder.setDateActivated(orderTag.startDate);
			revisedOrder.setNumRefills(orderTag.numRefills);
			if (orderTag.orderDuration != null)
				revisedOrder.setAutoExpireDate(calculateAutoExpireDate(orderTag.startDate, orderTag.orderDuration));
			if (!StringUtils.isEmpty(orderTag.instructions))
				revisedOrder.setInstructions((String) orderTag.instructions);
			
			log.debug("modifying drug order, drugId is " + orderTag.drugId + " and startDate is " + orderTag.startDate);
			session.getSubmissionActions().getCurrentEncounter().setDateChanged(new Date());
			session.getSubmissionActions().getCurrentEncounter().addOrder(revisedOrder);
			
			discontinuationOrder = createDiscontinuationOrderIfNeeded(revisedOrder, orderTag.discontinuedDate,
			    orderTag.discontinuedReasonStr);
		} else {
			Context.getOrderService().voidOrder(existingOrder, "Update discontinued date or reason");
			discontinuationOrder = existingOrder.cloneForRevision();
			discontinuationOrder.setDateActivated(orderTag.discontinuedDate);
			discontinuationOrder.setOrderReason(HtmlFormEntryUtil.getConcept(orderTag.discontinuedReasonStr));
		}
		
		if (discontinuationOrder != null) {
			session.getSubmissionActions().getCurrentEncounter().setDateChanged(new Date());
			setOrderer(session, discontinuationOrder);
			session.getSubmissionActions().getCurrentEncounter().addOrder(discontinuationOrder);
		}
	}
	
	private DrugOrder createDiscontinuationOrderIfNeeded(DrugOrder drugOrder, Date discontinuedDate,
	        String discontinuedReasonStr) {
		DrugOrder discontinuationOrder = null;
	
		if (discontinuedDate != null) {
			discontinuationOrder = drugOrder.cloneForDiscontinuing();
			discontinuationOrder.setDateActivated(discontinuedDate);
			if (!StringUtils.isEmpty(discontinuedReasonStr))
				discontinuationOrder.setOrderReason(HtmlFormEntryUtil.getConcept(discontinuedReasonStr));
		} else if (drugOrder.getAutoExpireDate() != null) {
			Date date = new Date();
			if (drugOrder.getAutoExpireDate().getTime() < date.getTime()) {
				drugOrder.setDateActivated(drugOrder.getAutoExpireDate());
				discontinuationOrder = drugOrder.cloneForDiscontinuing();
			}
		}
	
		return discontinuationOrder;
	}
	
	
	
	
	
	
	 */
	
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
	
	/*
	protected Date calculateAutoExpireDate(Date startDate, Integer orderDuration) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(startDate);
		cal.add(Calendar.DAY_OF_MONTH, orderDuration);
		return cal.getTime();
	}
	
	
	protected static class OrderTag {
		
		public String drugId;
		
		public Date startDate;
		
		public Integer orderDuration;
		
		public Date discontinuedDate;
		
		public String discontinuedReasonStr;
		
		public String instructions;
		
		public Drug drug;
		
		public Double dose;
		
		public String frequency;
		
		public Class<? extends DosingInstructions> dosingType;
		
		public Concept doseUnits;
		
		public Double quantity;
		
		public Concept quantityUnits;
		
		public Integer duration;
		
		public Concept durationUnits;
		
		public Concept route;
		
		public Integer careSettingId;
		
		public Integer numRefills;
	}
	
	*/
}
