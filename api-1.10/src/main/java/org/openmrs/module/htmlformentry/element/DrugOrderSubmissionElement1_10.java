package org.openmrs.module.htmlformentry.element;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openmrs.CareSetting;
import org.openmrs.CareSetting.CareSettingType;
import org.openmrs.Concept;
import org.openmrs.DosingInstructions;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.EncounterProvider;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.Order;
import org.openmrs.Order.Action;
import org.openmrs.OrderFrequency;
import org.openmrs.SimpleDosingInstructions;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.NumberFieldWidget;
import org.openmrs.module.htmlformentry.widget.Option;

public class DrugOrderSubmissionElement1_10 extends DrugOrderSubmissionElement {
	
	public static final String DRUG_ORDER_TYPE_UUID = "131168f4-15f5-102d-96e4-000c29c2a5d7";

	private DropdownWidget routeWidget;
	
	private DropdownWidget careSettingWidget;
	
	private DropdownWidget dosingTypeWidget;
	
	private DropdownWidget doseUnitsWidget;
	
	private NumberFieldWidget quantityWidget;
	
	private ErrorWidget quantityErrorWidget;
	
	private DropdownWidget quantityUnitsWidget;
	
	private NumberFieldWidget durationWidget;
	
	private ErrorWidget durationErrorWidget;
	
	private DropdownWidget durationUnitsWidget;

    private NumberFieldWidget numRefillsWidget;

    private ErrorWidget numRefillsErrorWidget;
	
	public DrugOrderSubmissionElement1_10(FormEntryContext context, Map<String, String> parameters) {
		super(context, parameters);
	}
	
	@Override
	protected void createAdditionalWidgets(FormEntryContext context) {
		createDosingTypeWidget(context);
		
		createDoseUnitsWidget(context);
		
		createQuantityWidget(context);
		
		createQuantityUnitsWidget(context);
		
		createDurationWidget(context);
		
		createDurationUnitsWidget(context);
		
		createRouteWidget(context);
		
		careSettingWidget = createCareSettingWidget(context, false);

        createNumRefillsWidget(context);
	}
	
	private void createDurationUnitsWidget(FormEntryContext context) {
		durationUnitsWidget = new DropdownWidget();
		List<Option> options = new ArrayList<Option>();
		List<Concept> durationUnits = Context.getOrderService().getDurationUnits();
		for (Concept concept : durationUnits) {
			options.add(new Option(concept.getName().getName(), concept.getId().toString(), false));
		}
		
		setupDropdownWidget(context, durationUnitsWidget, options);
	}
	
	private void createDurationWidget(FormEntryContext context) {
		durationWidget = new NumberFieldWidget(0d, 9999999d, true);
		context.registerWidget(durationWidget);
		
		durationErrorWidget = new ErrorWidget();
		context.registerErrorWidget(durationWidget, durationErrorWidget);
	}
	
	private void createQuantityUnitsWidget(FormEntryContext context) {
		quantityUnitsWidget = new DropdownWidget();
		
		List<Option> options = new ArrayList<Option>();
		List<Concept> drugDispensingUnits = Context.getOrderService().getDrugDispensingUnits();
		for (Concept concept : drugDispensingUnits) {
			options.add(new Option(concept.getName().getName(), concept.getId().toString(), false));
		}
		
		setupDropdownWidget(context, quantityUnitsWidget, options);
	}
	
	private void createQuantityWidget(FormEntryContext context) {
		quantityWidget = new NumberFieldWidget(0d, 9999999d, true);
		context.registerWidget(quantityWidget);
		
		quantityErrorWidget = new ErrorWidget();
		context.registerErrorWidget(quantityWidget, quantityErrorWidget);
	}
	
	private void createDosingTypeWidget(FormEntryContext context) {
		dosingTypeWidget = new DropdownWidget();
		
		List<Option> options = new ArrayList<Option>();
        MessageSourceService mss = Context.getMessageSourceService();
        options.add(new Option(mss.getMessage("htmlformentry.drugOrder.dosingType.simple"), SimpleDosingInstructions.class.getName(), true));
        //options.add(new Option(mss.getMessage("htmlformentry.drugOrder.dosingType.freetext"), FreeTextDosingInstructions.class.getName(), false));
		
		setupDropdownWidget(context, dosingTypeWidget, options);
	}

    private void createNumRefillsWidget(FormEntryContext context){
        numRefillsWidget = new NumberFieldWidget(0d, 9999999d, false);
        context.registerWidget(numRefillsWidget);
        numRefillsErrorWidget = new ErrorWidget();
        context.registerErrorWidget(numRefillsWidget, numRefillsErrorWidget);
    }
	
	@Override
	protected String generateHtmlForAdditionalWidgets(FormEntryContext context) {
		MessageSourceService mss = Context.getMessageSourceService();
		
		StringBuilder html = new StringBuilder();
		
		html.append(generateHtmlForWidget(context, mss.getMessage("htmlformentry.drugOrder.dosingType") + " ", dosingTypeWidget, null));
		
		html.append(generateHtmlForWidget(context, mss.getMessage("DrugOrder.dose") + " ", doseWidget, doseErrorWidget));
		
		html.append(generateHtmlForWidget(context, mss.getMessage("DrugOrder.dose") + " " + mss.getMessage("DrugOrder.units") + " ", doseUnitsWidget, null));
		
		html.append(generateHtmlForWidget(context, mss.getMessage("DrugOrder.quantity") + " ", quantityWidget, quantityErrorWidget));
		
		html.append(generateHtmlForWidget(context, mss.getMessage("DrugOrder.quantity") + " " + mss.getMessage("DrugOrder.units") + " ", quantityUnitsWidget,
		    null));
		
		html.append(generateHtmlForWidget(context, mss.getMessage("htmlformentry.drugOrder.duration") + " ", durationWidget, durationErrorWidget));
		
		html.append(generateHtmlForWidget(context, mss.getMessage("htmlformentry.drugOrder.duration") + " " + mss.getMessage("DrugOrder.units") + " ", durationUnitsWidget,
		    null));
		
		html.append(generateHtmlForWidget(context, mss.getMessage("htmlformentry.drugOrder.route") + " ", routeWidget, null));
		
		html.append(generateHtmlForWidget(context, mss.getMessage("htmlformentry.drugOrder.careSetting") + " ", careSettingWidget, null));

        html.append(generateHtmlForWidget(context, mss.getMessage("htmlformentry.drugOrder.numRefills") + " ", numRefillsWidget, numRefillsErrorWidget));

		return html.toString();
	}
	
	static void setupDropdownWidget(FormEntryContext context, DropdownWidget widget, List<Option> options) {
		if (context.getMode() != Mode.VIEW) {
			widget.setOptions(options);
			if (!options.isEmpty()) {
				widget.setInitialValue(options.get(0).getValue());
			}
		} else {
			widget.setOptions(new ArrayList<Option>());
		}
		context.registerWidget(widget);
	}
	
	private void createDoseUnitsWidget(FormEntryContext context) {
		doseUnitsWidget = new DropdownWidget();
		List<Concept> concepts = Context.getOrderService().getDrugDosingUnits();
		List<Option> options = new ArrayList<Option>();
		
		for (Concept concept : concepts) {
			options.add(new Option(concept.getName().getName(), concept.getId().toString(), false));
		}
		
		setupDropdownWidget(context, doseUnitsWidget, options);
	}
	
	public static DropdownWidget createCareSettingWidget(FormEntryContext context, boolean inpatientOnly) {
		DropdownWidget careSettingWidget = new DropdownWidget();
		List<CareSetting> careSettings = Context.getOrderService().getCareSettings(false);
		List<Option> options = new ArrayList<Option>();
		for (CareSetting careSetting : careSettings) {
			if (!inpatientOnly || careSetting.getCareSettingType().equals(CareSettingType.INPATIENT)) {
				options.add(new Option(careSetting.getName(), careSetting.getId().toString(), false));
			}
		}
		
		setupDropdownWidget(context, careSettingWidget, options);
		
		return careSettingWidget;
	}
	
	private void createRouteWidget(FormEntryContext context) {
		routeWidget = new DropdownWidget();
		List<Concept> drugRoutes = Context.getOrderService().getDrugRoutes();
		
		List<Option> options = new ArrayList<Option>();
		for (Concept route : drugRoutes) {
			options.add(new Option(route.getName().getName(), route.getId().toString(), false));
		}
		
		setupDropdownWidget(context, routeWidget, options);
	}
	
	@Override
	protected void createFrequencyWeekWidget(FormEntryContext context, MessageSourceService mss) {
		//It's not used in 1.10
	}
	
	@Override
	protected void createFrequencyWidget(FormEntryContext context, MessageSourceService mss) {
		frequencyWidget = new DropdownWidget();
		frequencyErrorWidget = new ErrorWidget();
		// fill frequency drop down lists (ENTER, EDIT)
		List<OrderFrequency> orderFrequencies = Context.getOrderService().getOrderFrequencies(false);
		
		List<Option> freqOptions = new ArrayList<Option>();
		if (context.getMode() != Mode.VIEW) {
			for (OrderFrequency orderFrequency : orderFrequencies) {
				freqOptions.add(new Option(orderFrequency.getConcept().getName().getName(), orderFrequency.getId()
				        .toString(), false));
			}
			
			if (!orderFrequencies.isEmpty()) {
				frequencyWidget.setInitialValue(orderFrequencies.get(0).getId());
			}
		}
		frequencyWidget.setOptions(freqOptions);
		context.registerWidget(frequencyWidget);
		context.registerErrorWidget(frequencyWidget, frequencyErrorWidget);
	}
	
	@Override
	protected void populateDrugOrderValuesFromDB(FormEntryContext context, Boolean usingDurationField) {
		// populate values drug order from database (VIEW, EDIT)
		if (context.getMode() != Mode.ENTER && context.getExistingOrders() != null) {
			for (Drug drug : drugsUsedAsKey) {
				if (context.getExistingOrders().containsKey(drug.getConcept())) {
					//this will return null if Order is not a DrugOrder even if matched by Concept
					DrugOrder drugOrder = (DrugOrder) context.removeExistingDrugOrder(drug);
					
					if (drugOrder != null) {
						//start from the first order for that drug
						while (drugOrder.getPreviousOrder() != null) {
							drugOrder = (DrugOrder) drugOrder.getPreviousOrder();
						}
						
						//get the latest revision or discontinuation order
						DrugOrder lastRevision = drugOrder;
						while (true) {
							DrugOrder revisedOrder = (DrugOrder) Context.getOrderService().getRevisionOrder(drugOrder);
							if (revisedOrder != null) {
								drugOrder = revisedOrder;
								lastRevision = revisedOrder;
								continue;
							}
							DrugOrder discontinuationOrder = (DrugOrder) Context.getOrderService().getDiscontinuationOrder(
							    drugOrder);
							if (discontinuationOrder != null) {
								drugOrder = discontinuationOrder;
								continue;
							}
							
							break;
						}
						
						existingOrder = drugOrder;
						if (drugWidget instanceof DropdownWidget) {
							drugWidget.setInitialValue(drugOrder.getDrug().getDrugId());
						} else {
							if (((CheckboxWidget) drugWidget).getValue().equals(drugOrder.getDrug().getDrugId().toString()))
								((CheckboxWidget) drugWidget).setInitialValue("CHECKED");
						}
						
						if (!existingOrder.getAction().equals(Action.DISCONTINUE)) {
							lastRevision = drugOrder;
						}
						
						startDateWidget.setInitialValue(lastRevision.getDateActivated());
						
						routeWidget.setInitialValue(lastRevision.getRoute().getId());
						
						careSettingWidget.setInitialValue(lastRevision.getCareSetting().getId());
						
						dosingTypeWidget.setInitialValue(lastRevision.getDosingType().toString());
						
						doseWidget.setInitialValue(lastRevision.getDose());

                        numRefillsWidget.setInitialValue(lastRevision.getNumRefills());
						
						if (lastRevision.getDoseUnits() != null) {
							doseUnitsWidget.setInitialValue(lastRevision.getDoseUnits().getId());
						}
						
						quantityWidget.setInitialValue(lastRevision.getQuantity());
						
						if (lastRevision.getQuantityUnits() != null) {
							quantityUnitsWidget.setInitialValue(lastRevision.getQuantityUnits().getId());
						}
						
						durationWidget.setInitialValue(lastRevision.getDuration());
						
						if (lastRevision.getDurationUnits() != null) {
							durationUnitsWidget.setInitialValue(lastRevision.getDurationUnits().getId());
						}
						
						frequencyWidget.setInitialValue(lastRevision.getFrequency().getConcept().getId());
						
						if (!usingDurationField) {
							discontinuedDateWidget.setInitialValue(drugOrder.getDateStopped());
							Order discontinuationOrder = Context.getOrderService().getDiscontinuationOrder(drugOrder);
							if (discontinuedReasonWidget != null && discontinuationOrder != null)
								discontinuedReasonWidget.setInitialValue(discontinuationOrder.getOrderReason()
								        .getConceptId());
						}
						break;
					}
					
				}
			}
		}
	}
	
	@Override
	protected void populateOrderTag(OrderTag oldOrderTag, FormEntrySession session, HttpServletRequest submission) {
		OrderTag1_10 orderTag = (OrderTag1_10) oldOrderTag;
		
		super.populateOrderTag(orderTag, session, submission);

        try {
            orderTag.dosingType = (Class<? extends DosingInstructions>)Context.loadClass((String) dosingTypeWidget.getValue(session.getContext(), submission));
        } catch (ClassNotFoundException e) {
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
	}
	
	@Override
	protected void enterOrder(FormEntrySession session, OrderTag oldOrderTag) {
		OrderTag1_10 orderTag = (OrderTag1_10) oldOrderTag;
		
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
		drugOrder.setDateActivated(orderTag.startDate);
        drugOrder.setNumRefills(orderTag.numRefills);
		//order duration:
		if (orderTag.orderDuration != null)
			drugOrder.setAutoExpireDate(calculateAutoExpireDate(orderTag.startDate, orderTag.orderDuration));
		drugOrder.setVoided(false);
		drugOrder.setOrderType(Context.getOrderService().getOrderTypeByUuid(DRUG_ORDER_TYPE_UUID));
		if (!StringUtils.isEmpty(orderTag.instructions))
			drugOrder.setInstructions((String) orderTag.instructions);
		
		DrugOrder discontinuationOrder = createDiscontinuationOrderIfNeeded(drugOrder, orderTag.discontinuedDate,
		    orderTag.discontinuedReasonStr);
		
		log.debug("adding new drug order, drugId is " + orderTag.drugId + " and startDate is " + orderTag.startDate);
		session.getSubmissionActions().getCurrentEncounter().addOrder(drugOrder);
		if (discontinuationOrder != null) {
			setOrderer(session, discontinuationOrder);
			session.getSubmissionActions().getCurrentEncounter().addOrder(discontinuationOrder);
		}
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
	
	@Override
	protected void editOrder(FormEntrySession session, OrderTag oldOrderTag) {
		OrderTag1_10 orderTag = (OrderTag1_10) oldOrderTag;
		DrugOrder discontinuationOrder = null;
		
		if (!existingOrder.getAction().equals(Action.DISCONTINUE)) {
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
	
	@Override
	protected OrderTag newOrderTag() {
		return new OrderTag1_10();
	}
	
	protected class OrderTag1_10 extends OrderTag {
		
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
}
