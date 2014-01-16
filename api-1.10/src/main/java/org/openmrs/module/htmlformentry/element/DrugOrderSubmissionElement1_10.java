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
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.EncounterProvider;
import org.openmrs.Order;
import org.openmrs.Order.Action;
import org.openmrs.OrderFrequency;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Option;


public class DrugOrderSubmissionElement1_10 extends DrugOrderSubmissionElement {
	
	public DrugOrderSubmissionElement1_10(FormEntryContext context, Map<String, String> parameters) {
	    super(context, parameters);
    }
	
	@Override
	protected void createAdditionalWidgets(FormEntryContext context) {
		createDoseUnitsWidget(context);
	    
	    createRouteWidget(context);
	    
	    createCareSettingWidget(context);
	}

	private void createDoseUnitsWidget(FormEntryContext context) {
	    doseUnitsWidget = new DropdownWidget();
	    List<Concept> concepts = Context.getOrderService().getDrugDosingUnits();
	    List<Option> options = new ArrayList<Option>();
	    if (context.getMode() != Mode.VIEW) {
		    for (Concept concept : concepts) {
		        options.add(new Option(concept.getName().getName(), concept.getId().toString(), false));
	        }
		    
		    if (!concepts.isEmpty()) {
		    	doseUnitsWidget.setInitialValue(concepts.get(0).getId());
		    }
	    }
	    doseUnitsWidget.setOptions(options);
	    context.registerWidget(doseUnitsWidget);
    }

	private void createCareSettingWidget(FormEntryContext context) {
	    careSettingWidget = new DropdownWidget();
	    List<CareSetting> careSettings = Context.getOrderService().getCareSettings(false);
	    
	    List<Option> options = new ArrayList<Option>();
	    if (context.getMode() != Mode.VIEW) {
		    for (CareSetting careSetting : careSettings) {
		        options.add(new Option(careSetting.getName(), careSetting.getId().toString(), false));
	        }
		    
		    if (!careSettings.isEmpty()) {
		    	careSettingWidget.setInitialValue(careSettings.get(0).getId());
		    }
	    }
	    careSettingWidget.setOptions(options);
	    context.registerWidget(careSettingWidget);
    }

	private void createRouteWidget(FormEntryContext context) {
	    routeWidget = new DropdownWidget();
	    List<Concept> drugRoutes = Context.getOrderService().getDrugRoutes();
	    
	    List<Option> options = new ArrayList<Option>();
	    if (context.getMode() != Mode.VIEW) {
	    	for (Concept route : drugRoutes) {
	            options.add(new Option(route.getName().getName(), route.getId().toString(), false));
            }
	    	
	    	if (!drugRoutes.isEmpty()) {
	    		routeWidget.setInitialValue(drugRoutes.get(0).getId());
	    	}
	    }
	    routeWidget.setOptions(options);
	    context.registerWidget(routeWidget);
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
		if (context.getMode() != Mode.VIEW ) {
    		for (OrderFrequency orderFrequency : orderFrequencies) {
				freqOptions.add(new Option(orderFrequency.getConcept().getName().getName(), orderFrequency.getId().toString(), false));
			}
    		frequencyWidget.setInitialValue(orderFrequencies.get(0).getId());
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
							DrugOrder discontinuationOrder = (DrugOrder) Context.getOrderService().getDiscontinuationOrder(drugOrder);
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
						
						if (existingOrder.getAction().equals(Action.DISCONTINUE)) {
							//existingOrder is discontinued
							startDateWidget.setInitialValue(lastRevision.getStartDate());
							
							routeWidget.setInitialValue(lastRevision.getRoute().getId());
							
							careSettingWidget.setInitialValue(lastRevision.getCareSetting().getId());
							if (!hideDoseAndFrequency) {
								doseWidget.setInitialValue(lastRevision.getDose());
								
								doseUnitsWidget.setInitialValue(lastRevision.getDoseUnits().getId());
								
								frequencyWidget.setInitialValue(lastRevision.getFrequency().getConcept().getId());
							}
						} else {
							startDateWidget.setInitialValue(drugOrder.getStartDate());
							
							routeWidget.setInitialValue(drugOrder.getRoute().getId());
							
							careSettingWidget.setInitialValue(drugOrder.getCareSetting().getId());
							if (!hideDoseAndFrequency) {
								doseWidget.setInitialValue(drugOrder.getDose());
								
								doseUnitsWidget.setInitialValue(drugOrder.getDoseUnits().getId());
								
								frequencyWidget.setInitialValue(drugOrder.getFrequency().getConcept().getId());
							}
						}
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
	protected void populateOrderTag(OrderTag orderTag, FormEntrySession session, HttpServletRequest submission) {
		super.populateOrderTag(orderTag, session, submission);
		
		//1.10.x only
	    if (doseUnitsWidget != null) {
	    	String value = (String) doseUnitsWidget.getValue(session.getContext(), submission);
	    	if (value != null) {
	    		orderTag.doseUnits = Context.getConceptService().getConcept(Integer.valueOf(value));
	    	}
	    }
	    if (careSettingWidget != null) {
	    	String value = (String) careSettingWidget.getValue(session.getContext(), submission);
	    	if (value != null) {
	    		orderTag.careSettingId = Integer.valueOf(Integer.valueOf(value));
	    	}
	    }
	    if (routeWidget != null) {
	    	String value = (String) routeWidget.getValue(session.getContext(), submission);
	    	if (value != null) {
	    		orderTag.route = Context.getConceptService().getConcept(Integer.valueOf(value));
	    	}
	    }
	}
	
	@Override
	protected void enterOrder(FormEntrySession session, OrderTag orderTag) {
		DrugOrder drugOrder = new DrugOrder();
    	if (drugOrder.getDateCreated() == null)
    	    drugOrder.setDateCreated(new Date());
    	if (drugOrder.getCreator() == null)
    	    drugOrder.setCreator(Context.getAuthenticatedUser());
    	setOrderer(session, drugOrder);
    	
    	drugOrder.setDrug(orderTag.drug);
    	drugOrder.setPatient(session.getPatient());
    	drugOrder.setDose(orderTag.dose);
    	drugOrder.setDoseUnits(orderTag.doseUnits);
    	drugOrder.setRoute(orderTag.route);
    	drugOrder.setCareSetting(Context.getOrderService().getCareSetting(orderTag.careSettingId));
    	OrderFrequency orderFrequency = Context.getOrderService().getOrderFrequency(Integer.valueOf(orderTag.frequency));
    	drugOrder.setFrequency(orderFrequency);
    	drugOrder.setStartDate(orderTag.startDate);
    	//order duration:
    	if (orderTag.orderDuration != null)
    	    drugOrder.setAutoExpireDate(calculateAutoExpireDate(orderTag.startDate, orderTag.orderDuration));
    	drugOrder.setVoided(false);
    	drugOrder.setDrug(orderTag.drug);
    	drugOrder.setConcept(orderTag.drug.getConcept());
    	drugOrder.setOrderType(Context.getOrderService().getOrderTypeByName("Drug order")); 
    	if (!StringUtils.isEmpty(orderTag.instructions))
    	    drugOrder.setInstructions((String) orderTag.instructions);
    	
    	DrugOrder discontinuationOrder = createDiscontinuationOrderIfNeeded(
				drugOrder, orderTag.discontinuedDate, orderTag.discontinuedReasonStr);
    	
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
    	
    	Set<EncounterProvider> encounterProviders = session.getSubmissionActions().getCurrentEncounter().getEncounterProviders();
    	for (EncounterProvider encounterProvider : encounterProviders) {
	        if (!encounterProvider.isVoided()) {
	        	drugOrder.setOrderer(encounterProvider.getProvider());
	        }
        }
    }
	
	@Override
	protected void editOrder(FormEntrySession session, OrderTag orderTag) {
		DrugOrder discontinuationOrder = null;
		
		if (!existingOrder.getAction().equals(Action.DISCONTINUE)) {
			//Discontinued orders must not be changed except for discontinue date and reason
			DrugOrder revisedOrder = existingOrder.cloneForRevision();
			setOrderer(session, revisedOrder);
			revisedOrder.setDrug(orderTag.drug);
			revisedOrder.setDose(orderTag.dose);
			revisedOrder.setDoseUnits(orderTag.doseUnits);
			revisedOrder.setRoute(orderTag.route);
			revisedOrder.setCareSetting(Context.getOrderService().getCareSetting(orderTag.careSettingId));
	    	OrderFrequency orderFrequency = Context.getOrderService().getOrderFrequency(Integer.valueOf(orderTag.frequency));
	    	revisedOrder.setFrequency(orderFrequency);
	    	revisedOrder.setStartDate(orderTag.startDate);
	    	if (orderTag.orderDuration != null)
	    		revisedOrder.setAutoExpireDate(calculateAutoExpireDate(orderTag.startDate, orderTag.orderDuration));
	    	revisedOrder.setConcept(orderTag.drug.getConcept());  	
	    	if (!StringUtils.isEmpty(orderTag.instructions))
	    		revisedOrder.setInstructions((String) orderTag.instructions);
	    	
			log.debug("modifying drug order, drugId is " + orderTag.drugId + " and startDate is " + orderTag.startDate);
			session.getSubmissionActions().getCurrentEncounter().setDateChanged(new Date());
			session.getSubmissionActions().getCurrentEncounter().addOrder(revisedOrder);
			
			discontinuationOrder = createDiscontinuationOrderIfNeeded(revisedOrder, orderTag.discontinuedDate, orderTag.discontinuedReasonStr);
		} else {
			Context.getOrderService().voidOrder(existingOrder, "Update discontinued date or reason");
			discontinuationOrder = existingOrder.cloneForRevision();
			discontinuationOrder.setStartDate(orderTag.discontinuedDate);
			discontinuationOrder.setOrderReason(HtmlFormEntryUtil.getConcept(orderTag.discontinuedReasonStr));
		}
		
		if (discontinuationOrder != null) {
			session.getSubmissionActions().getCurrentEncounter().setDateChanged(new Date());
			setOrderer(session, discontinuationOrder);
			session.getSubmissionActions().getCurrentEncounter().addOrder(discontinuationOrder);
		}
	}
		
	private DrugOrder createDiscontinuationOrderIfNeeded(DrugOrder drugOrder,
			Date discontinuedDate, String discontinuedReasonStr) {
		DrugOrder discontinuationOrder = null;
		
		if (discontinuedDate != null){
			discontinuationOrder = drugOrder.cloneForDiscontinuing();
			discontinuationOrder.setStartDate(discontinuedDate);
			if (!StringUtils.isEmpty(discontinuedReasonStr))
				discontinuationOrder.setOrderReason(HtmlFormEntryUtil.getConcept(discontinuedReasonStr));
		} else if (drugOrder.getAutoExpireDate() != null) {
			Date date = new Date();
			if (drugOrder.getAutoExpireDate().getTime() < date.getTime()) {
				drugOrder.setStartDate(drugOrder.getAutoExpireDate());
				discontinuationOrder = drugOrder.cloneForDiscontinuing();
			}
		}
		
		return discontinuationOrder;
	}

}
