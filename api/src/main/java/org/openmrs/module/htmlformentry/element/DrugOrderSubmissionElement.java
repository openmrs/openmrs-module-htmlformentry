package org.openmrs.module.htmlformentry.element;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.CareSetting;
import org.openmrs.DrugOrder;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.tag.DrugOrderTag;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.ConceptDropdownWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.HiddenFieldWidget;
import org.openmrs.module.htmlformentry.widget.MetadataDropdownWidget;
import org.openmrs.module.htmlformentry.widget.NumberFieldWidget;
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
	
	private final DrugOrderAnswer drugOrderAnswer;
	
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
	public DrugOrderSubmissionElement(FormEntryContext context, DrugOrderTag tag, DrugOrderAnswer drugOrderAnswer) {
		
		this.tag = tag;
		this.drugOrderAnswer = drugOrderAnswer;
		this.existingOrder = context.removeExistingDrugOrder(drugOrderAnswer.getDrug());
		
		// Drug Widget
		drugWidget = new HiddenFieldWidget();
		drugWidget.setInitialValue(drugOrderAnswer.getDrug().getId().toString());
		registerWidgets(context, drugWidget, drugErrorWidget);
		
		// Free Text Dosing Instructions
		if (tag.getDosingInstructionsType() == FreeTextDosingInstructions.class) {
			
			// Dosing Instructions Widget
			dosingInstructionsWidget = new TextFieldWidget();
			dosingInstructionsWidget.setInitialValue(existingOrder == null ? null : existingOrder.getDosingInstructions());
			registerWidgets(context, dosingInstructionsWidget, dosingInstructionsErrorWidget);
		}
		// Simple Dosing Instructions
		else {
			
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
			
			// As-Needed Widget
			asNeededWidget = new CheckboxWidget("true", translate(tag.getAsNeededLabel()));
			asNeededWidget.setInitialValue(existingOrder == null ? null : existingOrder.getAsNeeded());
			registerWidgets(context, asNeededWidget, asNeededErrorWidget);
		}
		
		// Start Date Widget
		startDateWidget = new DateWidget();
		startDateWidget.setInitialValue(existingOrder == null ? null : existingOrder.getEffectiveStartDate());
		registerWidgets(context, startDateWidget, startDateErrorWidget);
		
		// Duration Widget
		if (tag.isShowOrderDuration()) {
			durationWidget = new NumberFieldWidget(0d, 9999999d, true);
			durationWidget.setInitialValue(existingOrder == null ? null : existingOrder.getDuration());
			registerWidgets(context, durationWidget, durationErrorWidget);
			
			durationUnitsWidget = new ConceptDropdownWidget(Context.getOrderService().getDurationUnits());
			durationUnitsWidget.setInitialValue(existingOrder == null ? null : existingOrder.getDurationUnits());
			registerWidgets(context, durationUnitsWidget, durationUnitsErrorWidget);
		}
		
		// Instructions Widget
		if (tag.getInstructionsLabel() != null) {
			instructionsWidget = new TextFieldWidget();
			instructionsWidget.setInitialValue(existingOrder == null ? null : existingOrder.getInstructions());
			registerWidgets(context, instructionsWidget, instructionsErrorWidget);
		}
		
		// Outpatient Care Setting Fields
		if (tag.getCareSetting().getCareSettingType().equals(CareSetting.CareSettingType.OUTPATIENT)) {
			
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
		}
		
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
		
		startSpan(ret, "drugName");
		ret.append(drugOrderAnswer.getDisplayName());
		endSpan(ret);
		
		append(ret, context, "dose", "DrugOrder.dose", doseWidget, doseErrorWidget);
		append(ret, context, "doseUnits", null, doseUnitsWidget, doseUnitsErrorWidget);
		append(ret, context, "route", null, routeWidget, routeErrorWidget);
		append(ret, context, "frequency", null, frequencyWidget, frequencyErrorWidget);
		append(ret, context, "doseInstructions", null, dosingInstructionsWidget, dosingInstructionsErrorWidget);
		append(ret, context, "startDate", "general.dateStart", startDateWidget, startDateErrorWidget);
		append(ret, context, "duration", "htmlformentry.general.for", durationWidget, durationErrorWidget);
		append(ret, context, "durationUnits", null, durationUnitsWidget, durationUnitsErrorWidget);
		append(ret, context, "quantity", "DrugOrder.quantity", quantityWidget, quantityErrorWidget);
		append(ret, context, "quantityUnits", null, quantityUnitsWidget, quantityUnitsErrorWidget);
		append(ret, context, "instructions", tag.getInstructionsLabel(), instructionsWidget, instructionsErrorWidget);
		append(ret, context, "numRefills", "htmlformentry.drugOrder.numRefills", numRefillsWidget, numRefillsErrorWidget);
		
		return ret.toString();
	}
	
	protected void registerWidgets(FormEntryContext context, Widget fieldWidget, ErrorWidget errorWidget) {
		context.registerWidget(fieldWidget);
		context.registerErrorWidget(fieldWidget, errorWidget);
	}
	
	protected String translate(String code) {
		return Context.getMessageSourceService().getMessage(code);
	}
	
	protected String append(StringBuilder html, FormEntryContext ctx, String cssClass, String label, Widget w, Widget ew) {
		if (w != null) {
			log.debug("Appending html for widget with cssClass: " + cssClass + " and label " + label);
			if (label != null) {
				startSpan(html, cssClass + " fieldLabel");
				html.append(translate(label));
				endSpan(html);
			}
			startSpan(html, cssClass + " field");
			html.append(w.generateHtml(ctx));
			endSpan(html);
			if (ctx.getMode() != Mode.VIEW && ew != null) {
				startSpan(html, cssClass + " fieldError");
				html.append(ew.generateHtml(ctx));
				endSpan(html);
			}
			
		}
		return html.toString();
	}
	
	protected void startSpan(StringBuilder html, String cssClass) {
		html.append("<span class=\"" + cssClass + "\">");
	}
	
	protected void endSpan(StringBuilder html) {
		html.append("</span>");
	}
	
	@Override
	public DrugOrder getExistingOrder() {
		return existingOrder;
	}
	
	/**
	 * handleSubmission saves a drug order if in ENTER or EDIT-mode
	 * 
	 * @see FormSubmissionControllerAction#handleSubmission(FormEntrySession, HttpServletRequest)
	 */
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		/*
		OrderTag orderTag = new OrderTag();
		
		if (drugWidget.getValue(session.getContext(), submission) != null)
			orderTag.drugId = ((String) drugWidget.getValue(session.getContext(), submission));
		orderTag.startDate = startDateWidget.getValue(session.getContext(), submission);
		if (orderDurationWidget != null) {
			String orderDurationStr = (String) orderDurationWidget.getValue(session.getContext(), submission);
			try {
				orderTag.orderDuration = Integer.valueOf(orderDurationStr);
			}
			catch (Exception ex) {
				//pass
			}
		}
		if (discontinuedDateWidget != null) {
			orderTag.discontinuedDate = discontinuedDateWidget.getValue(session.getContext(), submission);
		}
		if (discontinuedReasonWidget != null) {
			orderTag.discontinuedReasonStr = (String) discontinuedReasonWidget.getValue(session.getContext(), submission);
		}
		if (instructionsWidget != null)
			orderTag.instructions = (String) instructionsWidget.getValue(session.getContext(), submission);
		if (!StringUtils.isEmpty(orderTag.drugId) && !orderTag.drugId.equals("~")) {
			orderTag.drug = Context.getConceptService().getDrug(Integer.valueOf(orderTag.drugId));
			if (defaultDose != null) {
				orderTag.dose = defaultDose;
			}
		
		try {
			orderTag.dosingType = (Class<? extends DosingInstructions>) Context
			        .loadClass((String) dosingTypeWidget.getValue(session.getContext(), submission));
		}
		catch (ClassNotFoundException e) {
			throw new APIException(e);
		}
		
		String doseUnitsValue = (String) doseUnitsWidget.getValue(session.getContext(), submission);
		if (doseUnitsValue != null) {
			orderTag.doseUnits = Context.getConceptService().getConcept(Integer.valueOf(doseUnitsValue));
		}
		
		orderTag.quantity = quantityWidget.getValue(session.getContext(), submission);
		
		String quantityUnitsValue = (String) quantityUnitsWidget.getValue(session.getContext(), submission);
		if (quantityUnitsValue != null) {
			orderTag.quantityUnits = Context.getConceptService().getConcept(Integer.valueOf(quantityUnitsValue));
		}
		
		Double drugOrderDuration = durationWidget.getValue(session.getContext(), submission);
		if (drugOrderDuration != null) {
			orderTag.duration = drugOrderDuration.intValue();
		}
		
		String durationUnitsValue = (String) durationUnitsWidget.getValue(session.getContext(), submission);
		if (durationUnitsValue != null) {
			orderTag.durationUnits = Context.getConceptService().getConcept(Integer.valueOf(durationUnitsValue));
		}
		
		String careSettingValue = (String) careSettingWidget.getValue(session.getContext(), submission);
		if (careSettingValue != null) {
			orderTag.careSettingId = Integer.valueOf(Integer.valueOf(careSettingValue));
		}
		
		String routeValue = (String) routeWidget.getValue(session.getContext(), submission);
		if (routeValue != null) {
			orderTag.route = Context.getConceptService().getConcept(Integer.valueOf(routeValue));
		}
		
		Double refillsValue = numRefillsWidget.getValue(session.getContext(), submission);
		if (refillsValue != null) {
			orderTag.numRefills = refillsValue.intValue();
		}
		
		if (session.getContext().getMode() == Mode.ENTER
		|| (session.getContext().getMode() == Mode.EDIT && existingOrder == null)) {
				enterOrder(session, orderTag);
			} else if (session.getContext().getMode() == Mode.EDIT) {
				editOrder(session, orderTag);
			}
		} else if (existingOrder != null) {
			//void order
			existingOrder.setVoided(true);
			existingOrder.setVoidedBy(Context.getAuthenticatedUser());
			existingOrder.setVoidReason("Drug De-selected in " + session.getForm().getName());
		
		}
		
		 */
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
	
	protected void enterOrder(FormEntrySession session, OrderTag orderTag) {
		DrugOrder drugOrder = new DrugOrder();
		setOrderer(session, drugOrder);
	
		drugOrder.setDrug(orderTag.drug);
		drugOrder.setConcept(orderTag.drug.getConcept());
		drugOrder.setPatient(session.getPatient());
		drugOrder.setDosingType(orderTag.dosingType);
		drugOrder.setDose(orderTag.dose);
		drugOrder.setDoseUnits(orderTag.doseUnits);
		drugOrder.setQuantity(orderTag.quantity);
		drugOrder.setQuantityUnits(orderTag.quantityUnits);
		drugOrder.setDuration(orderTag.duration);
		drugOrder.setDurationUnits(orderTag.durationUnits);
		drugOrder.setRoute(orderTag.route);
		drugOrder.setCareSetting(Context.getOrderService().getCareSetting(orderTag.careSettingId));
		OrderFrequency orderFrequency = Context.getOrderService().getOrderFrequency(Integer.valueOf(orderTag.frequency));
		drugOrder.setFrequency(orderFrequency);
	
		// The dateActivated of an order must not be in the future or after the date of the associated encounter
		// This means we always need to set the dateActivated to the encounterDatetime
		Date encDate = session.getEncounter().getEncounterDatetime();
		drugOrder.setDateActivated(encDate);
	
		// If the startDate indicated on the orderTag is after the encounterDatetime, then make this a future order
		drugOrder.setUrgency(Order.Urgency.ROUTINE);
		if (orderTag.startDate != null) {
			if (HtmlFormEntryUtil.startOfDay(orderTag.startDate).after(HtmlFormEntryUtil.startOfDay(encDate))) {
				drugOrder.setScheduledDate(orderTag.startDate);
				drugOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
			}
		}
	
		drugOrder.setNumRefills(orderTag.numRefills);
		//order duration:
		if (orderTag.orderDuration != null) {
			drugOrder.setAutoExpireDate(calculateAutoExpireDate(orderTag.startDate, orderTag.orderDuration));
		}
		drugOrder.setVoided(false);
		setOrderType(session, drugOrder);
		if (!StringUtils.isEmpty(orderTag.instructions)) {
			drugOrder.setInstructions(orderTag.instructions);
		}
		DrugOrder discontinuationOrder = createDiscontinuationOrderIfNeeded(drugOrder, orderTag.discontinuedDate,
		    orderTag.discontinuedReasonStr);
	
		log.debug("adding new drug order, drugId is " + orderTag.drugId + " and startDate is " + orderTag.startDate);
		session.getSubmissionActions().getCurrentEncounter().addOrder(drugOrder);
		if (discontinuationOrder != null) {
			setOrderer(session, discontinuationOrder);
			session.getSubmissionActions().getCurrentEncounter().addOrder(discontinuationOrder);
		}
	}
	
	private void setOrderType(FormEntrySession session, DrugOrder drugOrder) {
		OrderType ot = Context.getOrderService().getOrderTypeByUuid(OrderType.DRUG_ORDER_TYPE_UUID);
		// TODO: Handle cases where an implementation might have multiple order types for Drug Order and want to choose
		if (ot == null) {
			for (OrderType orderType : Context.getOrderService().getOrderTypes(false)) {
				if (orderType.getJavaClass() == DrugOrder.class) {
					ot = orderType;
				}
			}
		}
		drugOrder.setOrderType(ot);
	}
	
	private void setOrderer(FormEntrySession session, DrugOrder drugOrder) {
		if (drugOrder.getUuid() == null)
			drugOrder.setUuid(UUID.randomUUID().toString());
		
		Set<EncounterProvider> encounterProviders = session.getSubmissionActions().getCurrentEncounter()
		        .getEncounterProviders();
		for (EncounterProvider encounterProvider : encounterProviders) {
			if (!encounterProvider.isVoided()) {
				drugOrder.setOrderer(encounterProvider.getProvider());
			}
		}
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
