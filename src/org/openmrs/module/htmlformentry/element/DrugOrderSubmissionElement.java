package org.openmrs.module.htmlformentry.element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.NumberFieldWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.openmrs.util.OpenmrsConstants;

/**
 * Holds the widgets used to represent a specific drug order, and serves as both the HtmlGeneratorElement 
 * and the FormSubmissionControllerAction for the drug order.
 */
public class DrugOrderSubmissionElement implements HtmlGeneratorElement,
		FormSubmissionControllerAction {
	
	protected final Log log = LogFactory.getLog(DrugOrderSubmissionElement.class);
	
	public static final String FIELD_DRUG = "drug";
	
	public static final String FIELD_DRUG_NAMES = "drugNames";
	
	public static final String FIELD_DOSE = "dose";
	
	public static final String FIELD_VALIDATE_DOSE = "validateDose";
	
	public static final String FIELD_UNITS = "units";
	
	public static final String FIELD_DISCONTINUED = "discontinued";
	
	public static final String FIELD_FREQUENCY = "frequency";
	
	public static final String FIELD_DRUG_ID = "drugID";
	
	public static final String FIELD_QUANTITY = "quantity";
	
	public static final String FIELD_DATE_CREATED = "date_created";
	
	public static final String FIELD_AUTO_EXPIRE_DATE = "autoExpireDate";
	
	private boolean validateDose = false;

	private DropdownWidget drugWidget;
	private ErrorWidget drugErrorWidget;
	private Widget doseWidget;
	private ErrorWidget doseErrorWidget;
	private DateWidget startDateWidget;
	private ErrorWidget startDateErrorWidget;
	private DateWidget discontinuedDateWidget;
	private ErrorWidget discontinuedDateErrorWidget;
	private DropdownWidget frequencyWidget;
	private ErrorWidget frequencyErrorWidget;
	private DropdownWidget frequencyWeekWidget;
	private ErrorWidget frequencyWeekErrorWidget;
	
	private DrugOrder existingOrder;
	private List<Drug> drugsUsedAsKey;


	public DrugOrderSubmissionElement(FormEntryContext context, Map<String, String> parameters) {
		ConceptService conceptService = Context.getConceptService();
		MessageSourceService mss = Context.getMessageSourceService();
		
		// check URL
		String drugNames = parameters.get(FIELD_DRUG_NAMES);		
		if (drugNames == null || drugNames.length() < 1)
			throw new IllegalArgumentException("You must provide a valid drug name, or a valid ID or a valid UUID in " + parameters);
		
		String fieldValidateDose = parameters.get(FIELD_VALIDATE_DOSE);	
		if (fieldValidateDose != null && fieldValidateDose.length() > 1)
			validateDose = Boolean.parseBoolean(fieldValidateDose);

		// Register widgets
		drugWidget = new DropdownWidget();
		drugErrorWidget = new ErrorWidget();

		// fill drop down with drug names from database
		List<Option> options = new ArrayList<Option>();
		options.add(new Option("", "", false));

		// drugNames is comma separated list which can contain ID, UUID or drugname
		StringTokenizer tokenizer = new StringTokenizer(drugNames, ",");
		while (tokenizer.hasMoreElements()) {
			String drugName = (String) tokenizer.nextElement();
			Drug drug = null;
			// pattern to match a uuid, i.e., five blocks of alphanumerics separated by hyphens
			if (Pattern.compile("\\w+-\\w+-\\w+-\\w+-\\w+").matcher(drugName.trim()).matches()) {
				drug = conceptService.getDrugByUuid(drugName.trim());
			} else {
				drug = conceptService.getDrugByNameOrId(drugName.trim());			
			}
			
			if (drug != null) {
				String displayText = drug.getName();
				if (drug.getUnits() != null && drug.getUnits().length() > 1) {
					displayText += " | " + mss.getMessage("DrugOrder.units") + ": " + drug.getUnits();
				}
				
				options.add(new Option(displayText, drug.getDrugId().toString(), false));
				if (drugsUsedAsKey == null) {
				    drugsUsedAsKey = new ArrayList<Drug>();
				}
				drugsUsedAsKey.add(drug);
			}

		}
		
		if (drugsUsedAsKey == null)
			throw new IllegalArgumentException("You must provide a valid drug name, or a valid ID or a valid UUID in " + parameters);

		drugWidget.setOptions(options);
		
		context.registerWidget(drugWidget);
		context.registerErrorWidget(drugWidget, drugErrorWidget);
		
		// dose validation by drug is done in validateSubmission() 
		doseWidget = new NumberFieldWidget(0d, 9999999d, true);
		doseErrorWidget = new ErrorWidget();
		context.registerWidget(doseWidget);
		context.registerErrorWidget(doseWidget, doseErrorWidget);
		
		startDateWidget = new DateWidget();
		startDateErrorWidget = new ErrorWidget();
		context.registerWidget(startDateWidget);
		context.registerErrorWidget(startDateWidget, startDateErrorWidget);
		
		frequencyWidget = new DropdownWidget();
		frequencyErrorWidget = new ErrorWidget();
		// fill frequency drop down lists (ENTER, EDIT)
		options = new ArrayList<Option>();
		if (context.getMode() != Mode.VIEW ) {
			for (int i = 1; i <= 10; i++) {
				options.add(new Option(i + "/" + mss.getMessage("DrugOrder.frequency.day"), String.valueOf(i), false));
			}
		}
		frequencyWidget.setOptions(options);
		context.registerWidget(frequencyWidget);
		context.registerErrorWidget(frequencyWidget, frequencyErrorWidget);
		
		frequencyWeekWidget = new DropdownWidget();
		frequencyWeekErrorWidget = new ErrorWidget();
		// fill frequency drop down lists (ENTER, EDIT)
		options = new ArrayList<Option>();
		if (context.getMode() != Mode.VIEW ) {
			for (int i = 7; i >= 1; i--) {
				options.add(new Option(i + " " + mss.getMessage("DrugOrder.frequency.days") + "/"  + mss.getMessage("DrugOrder.frequency.week") , String.valueOf(i), false));
            }			
		}
		frequencyWeekWidget.setOptions(options);
		context.registerWidget(frequencyWeekWidget);
		context.registerErrorWidget(frequencyWeekWidget, frequencyWeekErrorWidget);
		
		discontinuedDateWidget = new DateWidget();
		discontinuedDateErrorWidget = new ErrorWidget();
		context.registerWidget(discontinuedDateWidget);
		context.registerErrorWidget(discontinuedDateWidget, discontinuedDateErrorWidget);
		
		// populate values drug order from database (VIEW, EDIT)
		Map<Concept, List<Order>> existingOrders = context.getExistingOrders();
		if (context.getMode() != Mode.ENTER && existingOrders != null) {		
			for (Drug drug : drugsUsedAsKey) {
	            if (existingOrders.containsKey(drug.getConcept())) {
	    			    DrugOrder drugOrder = (DrugOrder) context.removeExistingDrugOrder(drug);
	    			    if (drugOrder != null){
    	    				existingOrder = drugOrder;
    	    				drugWidget.setInitialValue(drugOrder.getDrug().getDrugId());
    	    				doseWidget.setInitialValue(drugOrder.getDose());
    	    				startDateWidget.setInitialValue(drugOrder.getStartDate());
    	    				
    	    				frequencyWidget.setInitialValue(parseFrequencyDays(drugOrder.getFrequency()));
    	    				frequencyWeekWidget.setInitialValue(parseFrequencyWeek(drugOrder.getFrequency()));
    	    				
    	    				discontinuedDateWidget.setInitialValue(drugOrder.getDiscontinuedDate());
    	    				break;
	    			    }
	    				
	            }
            }	
		}
		
	}

	/**
	 * Static helper method to parse frequency string
	 * 
	 * @should return times per day which is part of frequency string
	 * @param frequency (format "x/d y d/w")
	 * @return x
	 */
	private static String parseFrequencyDays(String frequency) {
		String days = StringUtils.substringBefore(frequency, "/d");
		return days;
	}
	
	/**
	 * Static helper method to parse frequency string
	 * 
	 * @should return number of days per weeks which is part of frequency string
	 * @param frequency (format "x/d y d/w")
	 * @return y
	 */
	private static String parseFrequencyWeek(String frequency) {
		String temp = StringUtils.substringAfter(frequency, "/d");
		String weeks = StringUtils.substringBefore(temp, "d/");
		return weeks;
	}
	
	/**
	 * @should return HTML snippet
	 * @see org.openmrs.module.htmlformentry.element.HtmlGeneratorElement#generateHtml(org.openmrs.module.htmlformentry.FormEntryContext)
	 */
	public String generateHtml(FormEntryContext context) {
		StringBuilder ret = new StringBuilder();
		MessageSourceService mss = Context.getMessageSourceService();
		
		if (drugWidget != null) {
			ret.append(mss.getMessage("DrugOrder.drug") + " ");
			ret.append(drugWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				ret.append(drugErrorWidget.generateHtml(context));
		}
		if (doseWidget != null) {
			ret.append(mss.getMessage("DrugOrder.dose") + " ");
			ret.append(doseWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				ret.append(doseErrorWidget.generateHtml(context));
		}
		if (frequencyWidget != null) {
			ret.append(mss.getMessage("DrugOrder.frequency") + " ");
			ret.append(frequencyWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				ret.append(frequencyErrorWidget.generateHtml(context));
		}
		if (frequencyWeekWidget != null) {
			ret.append(" x ");
			ret.append(frequencyWeekWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				ret.append(frequencyWeekErrorWidget.generateHtml(context));
		}
		if (startDateWidget != null) {
			ret.append(mss.getMessage("general.dateStart") + " ");
			ret.append(startDateWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				ret.append(startDateErrorWidget.generateHtml(context));
		}
		if (discontinuedDateWidget != null) {
			ret.append(mss.getMessage("general.dateDiscontinued") + " ");
			ret.append(discontinuedDateWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				ret.append(discontinuedDateErrorWidget.generateHtml(context));
		}
		return ret.toString();
    }

	/**
	 * handleSubmission saves a drug order if in ENTER or EDIT-mode
	 *  
	 * @see org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction#handleSubmission(org.openmrs.module.htmlformentry.FormEntrySession, javax.servlet.http.HttpServletRequest)
	 */
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
    	String drugID =  drugWidget.getValue(session.getContext(), submission);
    	Double dose = (Double) doseWidget.getValue(session.getContext(), submission);
    	Date startDate =  startDateWidget.getValue(session.getContext(), submission);
    	Date discontinuedDate = discontinuedDateWidget.getValue(session.getContext(), submission);
    	String frequency = (String)frequencyWidget.getValue(session.getContext(), submission);
    	frequency += "/d " + frequencyWeekWidget.getValue(session.getContext(), submission) + "d/w";
    	
    	if (drugID != null && !drugID.equals("")){
        	Drug drug = Context.getConceptService().getDrug(Integer.valueOf(drugID));
        	if (session.getContext().getMode() == Mode.ENTER || (session.getContext().getMode() == Mode.EDIT && existingOrder == null)) {	   	
    	    	DrugOrder drugOrder = new DrugOrder();
    	    	if (drugOrder.getDateCreated() == null)
    	    	    drugOrder.setDateCreated(new Date());
    	    	if (drugOrder.getCreator() == null)
    	    	    drugOrder.setCreator(Context.getAuthenticatedUser());
    	    	if (drugOrder.getUuid() == null)
    	    	    drugOrder.setUuid(UUID.randomUUID().toString());
    	    	drugOrder.setDrug(drug);
    	    	drugOrder.setPatient(session.getPatient());
    	    	drugOrder.setDose(dose);
    	    	drugOrder.setStartDate(startDate);
    	    	drugOrder.setVoided(false);
    	    	drugOrder.setDrug(drug);
    	    	drugOrder.setFrequency(frequency);
    	    	drugOrder.setConcept(drug.getConcept());
    	    	drugOrder.setOrderType(Context.getOrderService().getOrderType(OpenmrsConstants.ORDERTYPE_DRUG));    	    	
    	    	if (discontinuedDate != null){
    	    	    drugOrder.setDiscontinuedDate(discontinuedDate);
    	    	    drugOrder.setDiscontinued(true);
    	    	}    
    			log.debug("add new drug order, drugId is " + drugID + " and dose is " + dose + " and frequency is " + frequency + " and startDate is " + startDate);
    
    			session.getSubmissionActions().getCurrentEncounter().addOrder(drugOrder);
    	    } else if (session.getContext().getMode() == Mode.EDIT) {
    	    	existingOrder.setDrug(drug);
    	    	existingOrder.setDose(dose);
    	    	existingOrder.setStartDate(startDate);
    	    	existingOrder.setFrequency(frequency);
    	    	if (discontinuedDate != null){
    	    	    existingOrder.setDiscontinuedDate(discontinuedDate);
    	    	    existingOrder.setDiscontinued(true);
                } 
    	    	existingOrder.setConcept(drug.getConcept());  	
    			log.debug("modify drug order, drugId is " + drugID + " and dose is " + dose + " and frequency is " + frequency + " and startDate is " + startDate);
    			session.getSubmissionActions().getCurrentEncounter().setDateChanged(new Date());
    		}
    	}
    }

	/**
	 * @should return validation errors if doseWidget, startDateWidget or discontinuedDateWidget is invalid
	 * @see org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction#validateSubmission(org.openmrs.module.htmlformentry.FormEntryContext, javax.servlet.http.HttpServletRequest)
	 */
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {

			List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
			
			//if no drug specified, then don't do anything.
			if (drugWidget != null && drugWidget.getValue(context, submission) != null && !drugWidget.getValue(context, submission).equals("")){
    			try {
    				if (doseWidget != null) {
    					Double dose = (Double) doseWidget.getValue(context, submission);
    					if (dose == null)
    						throw new Exception("htmlformentry.error.required");
    					
    					// min max
    					if (validateDose) {
    						String drugID = drugWidget.getValue(context, submission);
    						Drug drug = Context.getConceptService().getDrug(drugID);
    						if ((drug.getMinimumDailyDose() != null && dose < drug.getMinimumDailyDose()) || (drug.getMaximumDailyDose() != null && dose > drug.getMaximumDailyDose())) {
    							throw new IllegalArgumentException("htmlformentry.error.doseOutOfRange");
    						}							
    					}
    				}
    			} catch (Exception ex) {
    				ret.add(new FormSubmissionError(context
    						.getFieldName(doseErrorWidget), Context
    						.getMessageSourceService().getMessage(ex.getMessage())));
    			}
    			try {
    				if (startDateWidget != null) {
    					Date dateCreated = startDateWidget.getValue(context, submission);
    					if (dateCreated == null)
    						throw new Exception("htmlformentry.error.required");
    				}
    			} catch (Exception ex) {
    				ret.add(new FormSubmissionError(context
    						.getFieldName(startDateErrorWidget), Context
    						.getMessageSourceService().getMessage(ex.getMessage())));
    			}
    			try {
                    if (startDateWidget != null && discontinuedDateWidget != null) {
                        Date startDate = startDateWidget.getValue(context, submission);
                        Date endDate = discontinuedDateWidget.getValue(context, submission);
                        if (startDate != null && endDate != null 
                                && startDate.getTime() > endDate.getTime())
                            throw new Exception("htmlformentry.error.discontinuedDateBeforeStartDate");
                    }
                } catch (Exception ex) {
                    ret.add(new FormSubmissionError(context
                            .getFieldName(discontinuedDateErrorWidget), Context
                            .getMessageSourceService().getMessage(ex.getMessage())));
                }
			}
			
			return ret;
	    }
}
