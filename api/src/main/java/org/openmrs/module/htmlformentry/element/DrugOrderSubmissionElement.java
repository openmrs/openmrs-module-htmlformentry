package org.openmrs.module.htmlformentry.element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.schema.DrugOrderAnswer;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;
import org.openmrs.module.htmlformentry.schema.ObsFieldAnswer;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.NumberFieldWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.TextFieldWidget;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.openmrs.util.OpenmrsConstants;

/**
 * Holds the widgets used to represent a specific drug order, and serves as both the HtmlGeneratorElement 
 * and the FormSubmissionControllerAction for the drug order.
 */
public class DrugOrderSubmissionElement implements HtmlGeneratorElement,
		FormSubmissionControllerAction {
	
	protected final Log log = LogFactory.getLog(DrugOrderSubmissionElement.class);
	
	public static final String FIELD_DRUG_NAMES = "drugNames";
	
	public static final String FIELD_DOSE = "dose";
	
	public static final String FIELD_VALIDATE_DOSE = "validateDose";
	
	public static final String FIELD_UNITS = "units";
	
	public static final String FIELD_DISCONTINUED = "discontinued";
	
	public static final String FIELD_FREQUENCY = "frequency";
	
	public static final String FIELD_QUANTITY = "quantity";
	
	public static final String FIELD_DATE_CREATED = "date_created";
	
	public static final String FIELD_INSTRUCTIONS_LABEL = "instructionsLabel";
	
	public static final String FIELD_DRUG_LABELS = "drugLabels";
	
	public static final String CONFIG_SHOW_DOSE_AND_FREQ = "hideDoseAndFrequency";
	
	public static final String FIELD_CHECKBOX = "checkbox";
	
	public static final String FIELD_DISCONTINUED_REASON="discontinuedReasonConceptId";
	
	public static final String FIELD_DISCONTINUED_REASON_ANSWERS="discontinueReasonAnswers";
	
	public static final String FIELD_DISCONTINUED_REASON_ANSWER_LABELS="discontinueReasonAnswerLabels";
	
	public static final String FIELD_SHOW_ORDER_DURATION = "showOrderDuration";
	
	public static final String CONFIG_DEFAULT_DOSE = "defaultDose";
	
	private boolean validateDose = false;

	private Widget drugWidget;
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
	private TextFieldWidget instructionsWidget;
	private ErrorWidget instructionsErrorWidget;
	private String instructionsLabel;
	private List<String> drugLabels;
	private Boolean hideDoseAndFrequency = false;
	private Boolean checkbox = false;
	private DropdownWidget discontinuedReasonWidget;
    private ErrorWidget discontinuedReasonErrorWidget;
    private TextFieldWidget orderDurationWidget;
    private ErrorWidget orderDurationErrorWidget;
    private Double defaultDose;
	
	private DrugOrder existingOrder;
	private List<Drug> drugsUsedAsKey;


	public DrugOrderSubmissionElement(FormEntryContext context, Map<String, String> parameters) {
		ConceptService conceptService = Context.getConceptService();
		MessageSourceService mss = Context.getMessageSourceService();
		
		Boolean usingDurationField = false;
		String orderDurationStr = parameters.get(FIELD_SHOW_ORDER_DURATION);
		if (!StringUtils.isEmpty(orderDurationStr) && orderDurationStr.equals("true"))
		    usingDurationField = true;
		String hideDoseAndFreqStr = parameters.get(CONFIG_SHOW_DOSE_AND_FREQ);
		if (!StringUtils.isEmpty(hideDoseAndFreqStr) && hideDoseAndFreqStr.equals("true"))
		    hideDoseAndFrequency = true;
		
		String checkboxStr = parameters.get(FIELD_CHECKBOX);
        if (checkboxStr != null && checkboxStr.equals("true"))
            checkbox = true;  
		
		// check URL
		String drugNames = parameters.get(FIELD_DRUG_NAMES);		
		if (drugNames == null || drugNames.length() < 1)
			throw new IllegalArgumentException("You must provide a valid drug name, or a valid ID or a valid UUID in " + parameters);
		
		String fieldValidateDose = parameters.get(FIELD_VALIDATE_DOSE);	
		if (fieldValidateDose != null && fieldValidateDose.length() > 1)
			validateDose = Boolean.parseBoolean(fieldValidateDose);
		
        if (parameters.get(FIELD_DRUG_LABELS) != null) {
            drugLabels = Arrays.asList(parameters.get(FIELD_DRUG_LABELS).split(","));
        }

		// fill drop down with drug names from database
		List<Option> options = new ArrayList<Option>();
		options.add(new Option("", "", false));

		// drugNames is comma separated list which can contain ID, UUID or drugname
		StringTokenizer tokenizer = new StringTokenizer(drugNames, ",");
		int drugListIndexPos = 0;
        String displayText = "";
        DrugOrderField dof = new DrugOrderField();
		while (tokenizer.hasMoreElements()) {
			String drugName = (String) tokenizer.nextElement();
			Drug drug = null;
			
			// see if this is a uuid
			if (HtmlFormEntryUtil.isValidUuidFormat(drugName.trim())) {
				drug = conceptService.getDrugByUuid(drugName.trim());
			} 
			
			// if we didn't find by id, find by uuid or name			
			if (drug == null){
				drug = conceptService.getDrugByNameOrId(drugName.trim());			
			}
			
			if (drug != null) {
			    displayText = drug.getName();
			    if (drugLabels != null) {
			        displayText = drugLabels.get(drugListIndexPos);
			    }
				options.add(new Option(displayText, drug.getDrugId().toString(), false));
				if (drugsUsedAsKey == null) {
				    drugsUsedAsKey = new ArrayList<Drug>();
				}
				drugsUsedAsKey.add(drug);
				DrugOrderAnswer doa = new DrugOrderAnswer(drug, displayText);
				dof.addDrugOrderAnswer(doa);
				drugListIndexPos ++;
			} else if (drugName.length() > 0 && drugName.charAt(0) == '/' && drugName.charAt(drugName.length()-1) == '/'){
			    options.add(new Option("[ " + drugName.substring(1,drugName.length()-1) + " ]", "~", false));
			} else {
			    throw new IllegalArgumentException("No Drug found for drug name/id/uuid " + drugName);
			}
		}
		
		if (drugsUsedAsKey == null)
			throw new IllegalArgumentException("You must provide a valid drug name, or a valid ID or a valid UUID in " + parameters);

	      // there need to be the same number of drugs as drug labels
        if (drugLabels != null && drugsUsedAsKey.size() != drugLabels.size())
            throw new IllegalArgumentException("There are a different number of drugLabels (" + drugLabels.size() + ") than drugs (" + drugsUsedAsKey.size() + ").");

        
        // Register Drug Widget
        if (checkbox && drugsUsedAsKey.size() == 1){
            CheckboxWidget cb = new CheckboxWidget();
            cb.setLabel(displayText);
            cb.setValue(drugsUsedAsKey.get(0).getDrugId().toString());
            drugWidget = cb;
        } else {
            DropdownWidget dw = new DropdownWidget();
            dw.setOptions(options);
            drugWidget = dw;
        }
        context.registerWidget(drugWidget);
        drugErrorWidget = new ErrorWidget();
        context.registerErrorWidget(drugWidget, drugErrorWidget);
        
		//start date
		startDateWidget = new DateWidget();
        startDateErrorWidget = new ErrorWidget();
        context.registerWidget(startDateWidget);
        context.registerErrorWidget(startDateWidget, startDateErrorWidget);

        if (!hideDoseAndFrequency){
    		// dose validation by drug is done in validateSubmission() 
    		doseWidget = new NumberFieldWidget(0d, 9999999d, true);
    		//set default value (maybe temporarily)
    		String defaultDoseStr = parameters.get(CONFIG_DEFAULT_DOSE);
    		if (!StringUtils.isEmpty(defaultDoseStr)){
    		    try {
    		        defaultDose = Double.valueOf(defaultDoseStr);
    		        doseWidget.setInitialValue(defaultDose);
    		    } catch (Exception ex){
    		            throw new RuntimeException("optional attribute 'defaultDose' must be numeric or empty.");
    		    }        
    		}
    		    
    		doseErrorWidget = new ErrorWidget();
    		context.registerWidget(doseWidget);
    		context.registerErrorWidget(doseWidget, doseErrorWidget);
    		
    		frequencyWidget = new DropdownWidget();
    		frequencyErrorWidget = new ErrorWidget();
    		// fill frequency drop down lists (ENTER, EDIT)
    		List<Option> freqOptions = new ArrayList<Option>();
    		if (context.getMode() != Mode.VIEW ) {
    			for (int i = 1; i <= 10; i++) {
    			    freqOptions.add(new Option(i + "/" + mss.getMessage("DrugOrder.frequency.day"), String.valueOf(i), false));
    			}
    		}
    		frequencyWidget.setOptions(freqOptions);
    		context.registerWidget(frequencyWidget);
    		context.registerErrorWidget(frequencyWidget, frequencyErrorWidget);
    		
    		frequencyWeekWidget = new DropdownWidget();
    		frequencyWeekErrorWidget = new ErrorWidget();
    		// fill frequency drop down lists (ENTER, EDIT)
    		List<Option> weekOptions = new ArrayList<Option>();
    		if (context.getMode() != Mode.VIEW ) {
    			for (int i = 7; i >= 1; i--) {
    			    weekOptions.add(new Option(i + " " + mss.getMessage("DrugOrder.frequency.days") + "/"  + mss.getMessage("DrugOrder.frequency.week") , String.valueOf(i), false));
                }			
    		}
    		frequencyWeekWidget.setOptions(weekOptions);
    		context.registerWidget(frequencyWeekWidget);
    		context.registerErrorWidget(frequencyWeekWidget, frequencyWeekErrorWidget);
        }

        if (!usingDurationField){
    		discontinuedDateWidget = new DateWidget();
    		discontinuedDateErrorWidget = new ErrorWidget();
    		context.registerWidget(discontinuedDateWidget);
    		context.registerErrorWidget(discontinuedDateWidget, discontinuedDateErrorWidget);
        }
		if (parameters.get(FIELD_DISCONTINUED_REASON) != null){
		    String discReasonConceptStr = (String) parameters.get(FIELD_DISCONTINUED_REASON);
		    Concept discontineReasonConcept = HtmlFormEntryUtil.getConcept(discReasonConceptStr);
		    if (discontineReasonConcept == null)
		        throw new IllegalArgumentException("discontinuedReasonConceptId is not set to a valid conceptId or concept UUID");
		    dof.setDiscontinuedReasonQuestion(discontineReasonConcept);
		    
		    discontinuedReasonWidget = new DropdownWidget();
		    discontinuedReasonErrorWidget = new ErrorWidget();
		    
		    List<Option> discOptions = new ArrayList<Option>();
		    discOptions.add(new Option("", "", false));
		    
		    if (parameters.get(FIELD_DISCONTINUED_REASON_ANSWERS) != null){
		        //setup a list of the reason concepts
		        List<Concept> discReasons = new ArrayList<Concept>();
		        String discAnswersString = (String) parameters.get(FIELD_DISCONTINUED_REASON_ANSWERS);
		        String[] strDiscAnswers = discAnswersString.split(",");
		        for (int i = 0; i < strDiscAnswers.length; i++){
		            String thisAnswer = strDiscAnswers[i];
		            Concept answer = HtmlFormEntryUtil.getConcept(thisAnswer, "discontinueReasonAnswers includes a value that is not a valid conceptId or concept UUID");
	                discReasons.add(answer);
		        }
		       
		        if (parameters.get(FIELD_DISCONTINUED_REASON_ANSWER_LABELS) != null){
		            // use the listed discontinueReasons, and use labels:
		            String discLabelsString = parameters.get(FIELD_DISCONTINUED_REASON_ANSWER_LABELS);
		            String[] strDiscAnswerLabels = discLabelsString.split(",");
		            //a little validation:
		            if (strDiscAnswerLabels.length != discReasons.size())
		                throw new RuntimeException("discontinueReasonAnswers and discontinueReasonAnswerLabels must contain the same number of members.");
		            for (int i = 0; i < strDiscAnswerLabels.length; i ++ ){
		                discOptions.add(new Option( strDiscAnswerLabels[i], discReasons.get(i).getConceptId().toString(),false));  
		                dof.addDiscontinuedReasonAnswer(new ObsFieldAnswer(strDiscAnswerLabels[i].trim(), discReasons.get(i)));
		            }
		        } else {
		            // use the listed discontinueReasons, and use their ConceptNames.
    		        for (Concept c: discReasons){
    		            discOptions.add(new Option( c.getBestName(Context.getLocale()).getName(), c.getConceptId().toString(),false));
    		            dof.addDiscontinuedReasonAnswer(new ObsFieldAnswer(c.getBestName(Context.getLocale()).getName(), c));
    		        }
		        }
		    } else {
		        //just use the conceptAnswers
    		    for (ConceptAnswer ca : discontineReasonConcept.getAnswers()){
    		        discOptions.add(new Option( ca.getAnswerConcept().getBestName(Context.getLocale()).getName(), ca.getAnswerConcept().getConceptId().toString(),false));
    		        dof.addDiscontinuedReasonAnswer(new ObsFieldAnswer(ca.getAnswerConcept().getBestName(Context.getLocale()).getName(), ca.getAnswerConcept()));
    		    }
		    }
		    if (discOptions.size() == 1)
		        throw new IllegalArgumentException("discontinue reason Concept doesn't have any ConceptAnswers");
		    
		    discontinuedReasonWidget.setOptions(discOptions);
		    context.registerWidget(discontinuedReasonWidget);
	        context.registerErrorWidget(discontinuedReasonWidget, discontinuedReasonErrorWidget);
		}
		// populate values drug order from database (VIEW, EDIT)
		if (context.getMode() != Mode.ENTER && context.getExistingOrders() != null) {		
			for (Drug drug : drugsUsedAsKey) {
	            if (context.getExistingOrders().containsKey(drug.getConcept())) {
	            		//this will return null if Order is not a DrugOrder even if matched by Concept
	    			    DrugOrder drugOrder = (DrugOrder) context.removeExistingDrugOrder(drug);
	    			    if (drugOrder != null){
    	    				existingOrder = drugOrder;
    	    				if (drugWidget instanceof DropdownWidget){
    	    				    drugWidget.setInitialValue(drugOrder.getDrug().getDrugId());
    	    				} else {
    	    				    if (((CheckboxWidget) drugWidget).getValue().equals(drugOrder.getDrug().getDrugId().toString()))
    	    				        ((CheckboxWidget) drugWidget).setInitialValue("CHECKED");
    	    				}
    	    				startDateWidget.setInitialValue(drugOrder.getStartDate());
    	    				if (!hideDoseAndFrequency){
    	    				    doseWidget.setInitialValue(drugOrder.getDose());
    	    				    frequencyWidget.setInitialValue(parseFrequencyDays(drugOrder.getFrequency()));
    	    				    frequencyWeekWidget.setInitialValue(parseFrequencyWeek(drugOrder.getFrequency()));
    	    				}
    	    				if (!usingDurationField){
    	    				    discontinuedDateWidget.setInitialValue(drugOrder.getDiscontinuedDate());
    	    				    if (discontinuedReasonWidget != null && drugOrder.getDiscontinuedReason() != null)
    	    				        discontinuedReasonWidget.setInitialValue(drugOrder.getDiscontinuedReason().getConceptId());
    	    				}
    	    				break;
	    			    }
	    				
	            }
            }	
		}
		
        instructionsLabel = parameters.get(FIELD_INSTRUCTIONS_LABEL);
        if (instructionsLabel != null){
            instructionsWidget = new TextFieldWidget();
            if (existingOrder != null){
                instructionsWidget.setInitialValue(existingOrder.getInstructions());  
            }    
            instructionsErrorWidget = new ErrorWidget();
            context.registerWidget(instructionsWidget);
            context.registerErrorWidget(instructionsWidget, instructionsErrorWidget);
        }  
        
        if (usingDurationField){
            orderDurationWidget = new TextFieldWidget(4);
            if (existingOrder != null && existingOrder.getAutoExpireDate() != null){
                //set duration from autoExpireDate in days
                Long autoDateMilis = existingOrder.getAutoExpireDate().getTime(); 
                Long startDateMilis = existingOrder.getStartDate().getTime();
                Long diffInMSec = autoDateMilis - startDateMilis;
                // Find date difference in days 
                // (24 hours 60 minutes 60 seconds 1000 millisecond)
                Long diffOfDays = diffInMSec / (24 * 60 * 60 * 1000);
                orderDurationWidget.setInitialValue(String.valueOf(diffOfDays.intValue()));
            }
            orderDurationErrorWidget = new ErrorWidget();
            context.registerWidget(orderDurationWidget);
            context.registerErrorWidget(orderDurationWidget, orderDurationErrorWidget);
        }
        context.getSchema().addField(dof);
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
	@Override
    public String generateHtml(FormEntryContext context) {
		StringBuilder ret = new StringBuilder();
		MessageSourceService mss = Context.getMessageSourceService();
		
		if (drugWidget != null) {
		    if (drugWidget instanceof CheckboxWidget == false)
		        ret.append(mss.getMessage("DrugOrder.drug") + " ");
			ret.append(drugWidget.generateHtml(context) + " ");
			if (context.getMode() != Mode.VIEW)
				ret.append(drugErrorWidget.generateHtml(context));
			ret.append(" | ");
		}
		if (doseWidget != null) {
			ret.append(mss.getMessage("DrugOrder.dose") + " ");
			ret.append(doseWidget.generateHtml(context)  + " ");
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
			ret.append(frequencyWeekWidget.generateHtml(context)  + " ");
			if (context.getMode() != Mode.VIEW)
				ret.append(frequencyWeekErrorWidget.generateHtml(context));
		}
		if (startDateWidget != null) {
			ret.append(" | ");
			ret.append(mss.getMessage("general.dateStart") + " ");
			ret.append(startDateWidget.generateHtml(context) + " ");
			if (context.getMode() != Mode.VIEW)
				ret.append(startDateErrorWidget.generateHtml(context));
		}
		if (orderDurationWidget != null){
		    ret.append(mss.getMessage("htmlformentry.general.for") + " ");
		    ret.append(orderDurationWidget.generateHtml(context));
		    ret.append(" " + mss.getMessage("htmlformentry.general.days") +  " ");
		    if (context.getMode() != Mode.VIEW)
                ret.append(orderDurationErrorWidget.generateHtml(context));
		}
		//duration and discontinuedDate are now mutually exclusive
		if (discontinuedDateWidget != null) {
			ret.append(mss.getMessage("general.dateDiscontinued") + " ");
			ret.append(discontinuedDateWidget.generateHtml(context) + " ");
			if (context.getMode() != Mode.VIEW)
				ret.append(discontinuedDateErrorWidget.generateHtml(context));
		}
		if (discontinuedReasonWidget != null){
		    ret.append(" | " + mss.getMessage("general.discontinuedReason") + " ");
            ret.append(discontinuedReasonWidget.generateHtml(context) + " ");
            if (context.getMode() != Mode.VIEW)
                ret.append(discontinuedReasonErrorWidget.generateHtml(context));
        }
		if (instructionsWidget != null){
		    ret.append(instructionsLabel + " ");
		    ret.append(instructionsWidget.generateHtml(context) + " ");
		    if (context.getMode() != Mode.VIEW)
                ret.append(instructionsErrorWidget.generateHtml(context));
		}
		
		return ret.toString();
    }

	/**
	 * handleSubmission saves a drug order if in ENTER or EDIT-mode
	 *  
	 * @see org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction#handleSubmission(org.openmrs.module.htmlformentry.FormEntrySession, javax.servlet.http.HttpServletRequest)
	 */
	@Override
    public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
	    String drugId = null;
	    if (drugWidget.getValue(session.getContext(), submission) != null)
	            drugId = ((String) drugWidget.getValue(session.getContext(), submission));
    	Date startDate =  startDateWidget.getValue(session.getContext(), submission);
    	Integer orderDuration = null;
    	if (orderDurationWidget != null){
    	   String orderDurationStr = (String) orderDurationWidget.getValue(session.getContext(), submission);
    	   try {
    	       orderDuration = Integer.valueOf(orderDurationStr);
    	   } catch (Exception ex){
    	       //pass
    	   }
    	}
    	Date discontinuedDate = null;
    	if (discontinuedDateWidget != null){
    	    discontinuedDate = discontinuedDateWidget.getValue(session.getContext(), submission);
    	}    
    	String discontinuedReasonStr = null;
    	if (discontinuedReasonWidget != null){
    	    discontinuedReasonStr = (String) discontinuedReasonWidget.getValue(session.getContext(), submission);
    	}
    	String instructions = null;
    	if (instructionsWidget != null)
    	    instructions = (String) instructionsWidget.getValue(session.getContext(), submission);
    	if (!StringUtils.isEmpty(drugId) && !drugId.equals("~")){
        	Drug drug = Context.getConceptService().getDrug(Integer.valueOf(drugId));
        	Double dose;
        	if (defaultDose == null)
        	    dose = drug.getDoseStrength();
        	else
        	    dose = defaultDose;
        	String frequency = null;
        	if (!hideDoseAndFrequency){
                dose = (Double) doseWidget.getValue(session.getContext(), submission);
                frequency = (String) frequencyWidget.getValue(session.getContext(), submission);
                frequency += "/d " + frequencyWeekWidget.getValue(session.getContext(), submission) + "d/w";
            }
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
    	    	drugOrder.setFrequency(frequency);
    	    	drugOrder.setStartDate(startDate);
    	    	//order duration:
    	    	if (orderDuration != null)
    	    	    drugOrder.setAutoExpireDate(calculateAutoExpireDate(startDate, orderDuration));
    	    	drugOrder.setVoided(false);
    	    	drugOrder.setDrug(drug);
    	    	drugOrder.setConcept(drug.getConcept());
    	    	drugOrder.setOrderType(Context.getOrderService().getOrderType(OpenmrsConstants.ORDERTYPE_DRUG)); 
    	    	if (!StringUtils.isEmpty(instructions))
    	    	    drugOrder.setInstructions((String) instructions);
    	    	if (discontinuedDate != null){
    	    	    drugOrder.setDiscontinuedDate(discontinuedDate);
    	    	    drugOrder.setDiscontinued(true);
    	    	}    
    	    	if (!StringUtils.isEmpty(discontinuedReasonStr))
    	    	    drugOrder.setDiscontinuedReason(HtmlFormEntryUtil.getConcept(discontinuedReasonStr));
    			log.debug("adding new drug order, drugId is " + drugId + " and startDate is " + startDate);
    			drugOrder = setDiscontinueDateFromAutoExpire(drugOrder);
    			session.getSubmissionActions().getCurrentEncounter().addOrder(drugOrder);
    	    } else if (session.getContext().getMode() == Mode.EDIT) {
    	    	existingOrder.setDrug(drug);
    	    	existingOrder.setDose(dose);
    	    	existingOrder.setFrequency(frequency);
    	    	existingOrder.setStartDate(startDate);
    	    	if (orderDuration != null)
    	    	    existingOrder.setAutoExpireDate(calculateAutoExpireDate(startDate, orderDuration));
    	    	if (discontinuedDate != null){
    	    	    existingOrder.setDiscontinuedDate(discontinuedDate);
    	    	    existingOrder.setDiscontinued(true);
                } 
    	    	if (!StringUtils.isEmpty(discontinuedReasonStr))
                    existingOrder.setDiscontinuedReason(HtmlFormEntryUtil.getConcept(discontinuedReasonStr));
                
    	    	existingOrder.setConcept(drug.getConcept());  	
    	    	if (!StringUtils.isEmpty(instructions))
    	    	    existingOrder.setInstructions((String) instructions);
    			log.debug("modifying drug order, drugId is " + drugId + " and startDate is " + startDate);
    			existingOrder = setDiscontinueDateFromAutoExpire(existingOrder);
    			session.getSubmissionActions().getCurrentEncounter().setDateChanged(new Date());
    		}
    	} else if (existingOrder != null){
    	     //void order 
    	     existingOrder.setVoided(true);
    	     existingOrder.setVoidedBy(Context.getAuthenticatedUser());
    	     existingOrder.setVoidReason("Drug De-selected in " + session.getForm().getName());
    	    
    	}
    }

	/**
	 * @should return validation errors if doseWidget, startDateWidget or discontinuedDateWidget is invalid
	 * @see org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction#validateSubmission(org.openmrs.module.htmlformentry.FormEntryContext, javax.servlet.http.HttpServletRequest)
	 */
	@Override
    public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {

			List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
			try {
			if (drugWidget != null && drugWidget.getValue(context, submission) != null && ((String) drugWidget.getValue(context, submission)).equals("~"))
			    throw new IllegalArgumentException("htmlformentry.error.cannotChooseADrugHeader");
			} catch (Exception ex){
			    ret.add(new FormSubmissionError(context
                        .getFieldName(drugErrorWidget), Context
                        .getMessageSourceService().getMessage(ex.getMessage())));
			}
			//if no drug specified, then don't do anything.
			if (drugWidget != null && drugWidget.getValue(context, submission) != null && !((String) drugWidget.getValue(context, submission)).trim().equals("") && !((String) drugWidget.getValue(context, submission)).trim().equals("~")){
    			try {
    				if (doseWidget != null) {
    					Double dose = (Double) doseWidget.getValue(context, submission);
    					if (dose == null)
    						throw new Exception("htmlformentry.error.required");
    					
    					// min max
    					if (validateDose) {
    						String drugID = (String) drugWidget.getValue(context, submission);
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
                try {
                    if (discontinuedReasonWidget != null && discontinuedDateWidget != null) {
                        String discReason = (String) discontinuedReasonWidget.getValue(context, submission);
                        Date endDate = discontinuedDateWidget.getValue(context, submission);
                        if (endDate == null && !StringUtils.isEmpty(discReason))
                            throw new Exception("htmlformentry.error.discontinuedReasonEnteredWithoutDate");
                    }
                } catch (Exception ex) {
                    ret.add(new FormSubmissionError(context
                            .getFieldName(discontinuedReasonErrorWidget), Context
                            .getMessageSourceService().getMessage(ex.getMessage())));
                }
                try {
                    if (orderDurationWidget != null && orderDurationWidget.getValue(context, submission) != null) {
                        String orderDurationVal = (String) orderDurationWidget.getValue(context, submission);
                        if (!orderDurationVal.equals("")){
                            try {
                                Integer.valueOf(orderDurationVal);
                            } catch (Exception ex){
                                throw new Exception("htmlformentry.error.durationMustBeEmptyOrNumeric");
                            }
                        }    
                    }
                } catch (Exception ex) {
                    ret.add(new FormSubmissionError(context
                            .getFieldName(orderDurationErrorWidget), Context
                            .getMessageSourceService().getMessage(ex.getMessage())));
                }
			}
			
			return ret;
	    }
	
	private Date calculateAutoExpireDate(Date startDate, Integer orderDuration){
	    Calendar cal = Calendar.getInstance();
	    cal.setTime(startDate);
	    cal.add(Calendar.DAY_OF_MONTH, orderDuration);
	    return cal.getTime();
	}
	
	
	private DrugOrder setDiscontinueDateFromAutoExpire(DrugOrder dor){
	    if (dor.getAutoExpireDate() != null){
	        Date today = new Date();
	        if (dor.getAutoExpireDate().getTime() < today.getTime()){
	            dor.setDiscontinuedDate(dor.getAutoExpireDate());
	            //TODO:  when discontinueReason is a String, set it.
	        }    
	    }
	    return dor;
	}

	public DrugOrder getExistingOrder() {
		return existingOrder;
	}
	
	
}
