package org.openmrs.module.htmlformentry.element;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.DosingInstructions;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.EncounterProvider;
import org.openmrs.Order;
import org.openmrs.OrderFrequency;
import org.openmrs.OrderType;
import org.openmrs.SimpleDosingInstructions;
import org.openmrs.api.APIException;
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

/**
 * Holds the widgets used to represent a specific drug order, and serves as both the
 * HtmlGeneratorElement and the FormSubmissionControllerAction for the drug order.
 */
public class DrugOrderSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction, GettingExistingOrder {
	
	protected final Log log = LogFactory.getLog(DrugOrderSubmissionElement.class);
	
	public static final String DRUG_ORDER_TYPE_UUID = "131168f4-15f5-102d-96e4-000c29c2a5d7";
	
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
	
	public static final String CONFIG_SHOW_DOSE = "hideDose";
	
	public static final String CONFIG_SHOW_DOSE_AND_FREQ = "hideDoseAndFrequency";
	
	public static final String FIELD_CHECKBOX = "checkbox";
	
	public static final String FIELD_DISCONTINUED_REASON = "discontinuedReasonConceptId";
	
	public static final String FIELD_DISCONTINUED_REASON_ANSWERS = "discontinueReasonAnswers";
	
	public static final String FIELD_DISCONTINUED_REASON_ANSWER_LABELS = "discontinueReasonAnswerLabels";
	
	public static final String FIELD_SHOW_ORDER_DURATION = "showOrderDuration";
	
	public static final String CONFIG_DEFAULT_DOSE = "defaultDose";
	
	private boolean validateDose = false;
	
	protected Widget drugWidget;
	
	private ErrorWidget drugErrorWidget;
	
	protected Widget doseWidget;
	
	protected ErrorWidget doseErrorWidget;
	
	protected DateWidget startDateWidget;
	
	private ErrorWidget startDateErrorWidget;
	
	protected DateWidget discontinuedDateWidget;
	
	private ErrorWidget discontinuedDateErrorWidget;
	
	protected DropdownWidget frequencyWidget;
	
	protected ErrorWidget frequencyErrorWidget;
	
	private DropdownWidget frequencyWeekWidget;
	
	private ErrorWidget frequencyWeekErrorWidget;
	
	private TextFieldWidget instructionsWidget;
	
	private ErrorWidget instructionsErrorWidget;
	
	private String instructionsLabel;
	
	private List<String> drugLabels;
	
	protected Boolean hideDose = false;
	
	protected Boolean hideDoseAndFrequency = false;
	
	private Boolean checkbox = false;
	
	protected DropdownWidget discontinuedReasonWidget;
	
	private ErrorWidget discontinuedReasonErrorWidget;
	
	private TextFieldWidget orderDurationWidget;
	
	private ErrorWidget orderDurationErrorWidget;
	
	private Double defaultDose;
	
	protected DrugOrder existingOrder;
	
	protected List<Drug> drugsUsedAsKey;
	
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
	
	public DrugOrderSubmissionElement(FormEntryContext context, Map<String, String> parameters) {
		ConceptService conceptService = Context.getConceptService();
		MessageSourceService mss = Context.getMessageSourceService();
		
		Boolean usingDurationField = false;
		String orderDurationStr = parameters.get(FIELD_SHOW_ORDER_DURATION);
		if (!StringUtils.isEmpty(orderDurationStr) && orderDurationStr.equals("true"))
			usingDurationField = true;
		
		String hideDoseStr = parameters.get(CONFIG_SHOW_DOSE);
		
		String hideDoseAndFreqStr = parameters.get(CONFIG_SHOW_DOSE_AND_FREQ);
		if (!StringUtils.isEmpty(hideDoseAndFreqStr) && hideDoseAndFreqStr.equals("true"))
			hideDoseAndFrequency = true;
		else if (!StringUtils.isEmpty(hideDoseStr) && hideDoseStr.equals("true"))
			hideDose = true;
		
		String checkboxStr = parameters.get(FIELD_CHECKBOX);
		if (checkboxStr != null && checkboxStr.equals("true"))
			checkbox = true;
		
		// check URL
		String drugNames = parameters.get(FIELD_DRUG_NAMES);
		if (drugNames == null || drugNames.length() < 1)
			throw new IllegalArgumentException(
			        "You must provide a valid drug name, or a valid ID or a valid UUID in " + parameters);
		
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
			if (drug == null) {
				drug = conceptService.getDrug(drugName.trim());
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
				drugListIndexPos++;
			} else if (drugName.length() > 0 && drugName.charAt(0) == '/' && drugName.charAt(drugName.length() - 1) == '/') {
				options.add(new Option("[ " + drugName.substring(1, drugName.length() - 1) + " ]", "~", false));
			} else {
				throw new IllegalArgumentException("No Drug found for drug name/id/uuid " + drugName);
			}
		}
		
		if (drugsUsedAsKey == null)
			throw new IllegalArgumentException(
			        "You must provide a valid drug name, or a valid ID or a valid UUID in " + parameters);
		
		// there need to be the same number of drugs as drug labels
		if (drugLabels != null && drugsUsedAsKey.size() != drugLabels.size())
			throw new IllegalArgumentException("There are a different number of drugLabels (" + drugLabels.size()
			        + ") than drugs (" + drugsUsedAsKey.size() + ").");
		
		// Register Drug Widget
		if (checkbox && drugsUsedAsKey.size() == 1) {
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
		
		if (!hideDoseAndFrequency && hideDose) {
			createFrequencyWidget(context, mss);
		} else if (!hideDoseAndFrequency) {
			// dose validation by drug is done in validateSubmission()
			doseWidget = new NumberFieldWidget(0d, 9999999d, true);
			//set default value (maybe temporarily)
			String defaultDoseStr = parameters.get(CONFIG_DEFAULT_DOSE);
			if (!StringUtils.isEmpty(defaultDoseStr)) {
				try {
					defaultDose = Double.valueOf(defaultDoseStr);
					doseWidget.setInitialValue(defaultDose);
				}
				catch (Exception ex) {
					throw new RuntimeException("optional attribute 'defaultDose' must be numeric or empty.");
				}
			}
			
			doseErrorWidget = new ErrorWidget();
			context.registerWidget(doseWidget);
			context.registerErrorWidget(doseWidget, doseErrorWidget);
			
			createFrequencyWidget(context, mss);
		}
		
		if (!usingDurationField) {
			discontinuedDateWidget = new DateWidget();
			discontinuedDateErrorWidget = new ErrorWidget();
			context.registerWidget(discontinuedDateWidget);
			context.registerErrorWidget(discontinuedDateWidget, discontinuedDateErrorWidget);
		}
		if (parameters.get(FIELD_DISCONTINUED_REASON) != null) {
			String discReasonConceptStr = (String) parameters.get(FIELD_DISCONTINUED_REASON);
			Concept discontineReasonConcept = HtmlFormEntryUtil.getConcept(discReasonConceptStr);
			if (discontineReasonConcept == null)
				throw new IllegalArgumentException(
				        "discontinuedReasonConceptId is not set to a valid conceptId or concept UUID");
			dof.setDiscontinuedReasonQuestion(discontineReasonConcept);
			
			discontinuedReasonWidget = new DropdownWidget();
			discontinuedReasonErrorWidget = new ErrorWidget();
			
			List<Option> discOptions = new ArrayList<Option>();
			discOptions.add(new Option("", "", false));
			
			if (parameters.get(FIELD_DISCONTINUED_REASON_ANSWERS) != null) {
				//setup a list of the reason concepts
				List<Concept> discReasons = new ArrayList<Concept>();
				String discAnswersString = (String) parameters.get(FIELD_DISCONTINUED_REASON_ANSWERS);
				String[] strDiscAnswers = discAnswersString.split(",");
				for (int i = 0; i < strDiscAnswers.length; i++) {
					String thisAnswer = strDiscAnswers[i];
					Concept answer = HtmlFormEntryUtil.getConcept(thisAnswer,
					    "discontinueReasonAnswers includes a value that is not a valid conceptId or concept UUID");
					discReasons.add(answer);
				}
				
				if (parameters.get(FIELD_DISCONTINUED_REASON_ANSWER_LABELS) != null) {
					// use the listed discontinueReasons, and use labels:
					String discLabelsString = parameters.get(FIELD_DISCONTINUED_REASON_ANSWER_LABELS);
					String[] strDiscAnswerLabels = discLabelsString.split(",");
					//a little validation:
					if (strDiscAnswerLabels.length != discReasons.size())
						throw new RuntimeException(
						        "discontinueReasonAnswers and discontinueReasonAnswerLabels must contain the same number of members.");
					for (int i = 0; i < strDiscAnswerLabels.length; i++) {
						discOptions.add(
						    new Option(strDiscAnswerLabels[i], discReasons.get(i).getConceptId().toString(), false));
						dof.addDiscontinuedReasonAnswer(
						    new ObsFieldAnswer(strDiscAnswerLabels[i].trim(), discReasons.get(i)));
					}
				} else {
					// use the listed discontinueReasons, and use their ConceptNames.
					for (Concept c : discReasons) {
						discOptions.add(
						    new Option(c.getName(Context.getLocale(), false).getName(), c.getConceptId().toString(), false));
						dof.addDiscontinuedReasonAnswer(new ObsFieldAnswer(c.getName(Context.getLocale()).getName(), c));
					}
				}
			} else {
				//just use the conceptAnswers
				for (ConceptAnswer ca : discontineReasonConcept.getAnswers()) {
					discOptions.add(new Option(ca.getAnswerConcept().getName(Context.getLocale()).getName(),
					        ca.getAnswerConcept().getConceptId().toString(), false));
					dof.addDiscontinuedReasonAnswer(new ObsFieldAnswer(
					        ca.getAnswerConcept().getName(Context.getLocale()).getName(), ca.getAnswerConcept()));
				}
			}
			if (discOptions.size() == 1)
				throw new IllegalArgumentException("discontinue reason Concept doesn't have any ConceptAnswers");
			
			discontinuedReasonWidget.setOptions(discOptions);
			context.registerWidget(discontinuedReasonWidget);
			context.registerErrorWidget(discontinuedReasonWidget, discontinuedReasonErrorWidget);
		}
		
		createAdditionalWidgets(context);
		
		if (context.getMode() != Mode.ENTER && context.getExistingOrders() != null) {
			// If VIEW, EDIT
			populateDrugOrderValuesFromDB(context, usingDurationField);
		}
		
		instructionsLabel = parameters.get(FIELD_INSTRUCTIONS_LABEL);
		if (instructionsLabel != null) {
			instructionsWidget = new TextFieldWidget();
			if (existingOrder != null) {
				instructionsWidget.setInitialValue(existingOrder.getInstructions());
			}
			instructionsErrorWidget = new ErrorWidget();
			context.registerWidget(instructionsWidget);
			context.registerErrorWidget(instructionsWidget, instructionsErrorWidget);
		}
		
		if (usingDurationField) {
			orderDurationWidget = new TextFieldWidget(4);
			if (existingOrder != null && existingOrder.getAutoExpireDate() != null) {
				//set duration from autoExpireDate in days
				Long autoDateMilis = existingOrder.getAutoExpireDate().getTime();
				Long startDateMilis = existingOrder.getEffectiveStartDate().getTime();
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
		context.addFieldToActiveSection(dof);
	}
	
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
		options.add(new Option(mss.getMessage("htmlformentry.drugOrder.dosingType.simple"),
		        SimpleDosingInstructions.class.getName(), true));
		//options.add(new Option(mss.getMessage("htmlformentry.drugOrder.dosingType.freetext"), FreeTextDosingInstructions.class.getName(), false));
		
		setupDropdownWidget(context, dosingTypeWidget, options);
	}
	
	private void createNumRefillsWidget(FormEntryContext context) {
		numRefillsWidget = new NumberFieldWidget(0d, 9999999d, false);
		context.registerWidget(numRefillsWidget);
		numRefillsErrorWidget = new ErrorWidget();
		context.registerErrorWidget(numRefillsWidget, numRefillsErrorWidget);
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
	
	/**
	 * Be aware it's called by the constructor.
	 *
	 * @param context
	 * @param usingDurationField
	 */
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
							DrugOrder discontinuationOrder = (DrugOrder) Context.getOrderService()
							        .getDiscontinuationOrder(drugOrder);
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
						
						if (!existingOrder.getAction().equals(Order.Action.DISCONTINUE)) {
							lastRevision = drugOrder;
						}
						
						startDateWidget.setInitialValue(lastRevision.getEffectiveStartDate());
						
						if (lastRevision.getRoute() != null) {
							routeWidget.setInitialValue(lastRevision.getRoute().getId());
						}
						
						if (lastRevision.getCareSetting() != null) {
							careSettingWidget.setInitialValue(lastRevision.getCareSetting().getId());
						}
						
						if (lastRevision.getDosingType() != null) {
							dosingTypeWidget.setInitialValue(lastRevision.getDosingType().toString());
						}
						
						if (lastRevision.getDose() != null) {
							doseWidget.setInitialValue(lastRevision.getDose());
						}
						
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
						
						if (lastRevision.getFrequency() != null) {
							frequencyWidget.setInitialValue(lastRevision.getFrequency().getConcept().getId());
						}
						
						if (!usingDurationField) {
							discontinuedDateWidget.setInitialValue(drugOrder.getDateStopped());
							Order discontinuationOrder = Context.getOrderService().getDiscontinuationOrder(drugOrder);
							if (discontinuedReasonWidget != null && discontinuationOrder != null) {
								Integer cId = discontinuationOrder.getOrderReason().getConceptId();
								discontinuedReasonWidget.setInitialValue(cId);
							}
						}
						break;
					}
					
				}
			}
		}
	}
	
	/**
	 * Be aware it's called by the constructor.
	 *
	 * @param context
	 * @param mss
	 */
	protected void createFrequencyWidget(FormEntryContext context, MessageSourceService mss) {
		frequencyWidget = new DropdownWidget();
		frequencyErrorWidget = new ErrorWidget();
		// fill frequency drop down lists (ENTER, EDIT)
		List<OrderFrequency> orderFrequencies = Context.getOrderService().getOrderFrequencies(false);
		
		List<Option> freqOptions = new ArrayList<Option>();
		if (context.getMode() != Mode.VIEW) {
			for (OrderFrequency orderFrequency : orderFrequencies) {
				freqOptions.add(
				    new Option(orderFrequency.getConcept().getName().getName(), orderFrequency.getId().toString(), false));
			}
			
			if (!orderFrequencies.isEmpty()) {
				frequencyWidget.setInitialValue(orderFrequencies.get(0).getId());
			}
		}
		frequencyWidget.setOptions(freqOptions);
		context.registerWidget(frequencyWidget);
		context.registerErrorWidget(frequencyWidget, frequencyErrorWidget);
	}
	
	/**
	 * Static helper method to parse frequency string <strong>Should</strong> return times per day which
	 * is part of frequency string
	 *
	 * @param frequency (format "x/d y d/w")
	 * @return x
	 */
	private static String parseFrequencyDays(String frequency) {
		String days = StringUtils.substringBefore(frequency, "/d");
		return days;
	}
	
	/**
	 * Static helper method to parse frequency string <strong>Should</strong> return number of days per
	 * weeks which is part of frequency string
	 *
	 * @param frequency (format "x/d y d/w")
	 * @return y
	 */
	private static String parseFrequencyWeek(String frequency) {
		String temp = StringUtils.substringAfter(frequency, "/d");
		String weeks = StringUtils.substringBefore(temp, "d/");
		return weeks;
	}
	
	/**
	 * <strong>Should</strong> return HTML snippet
	 *
	 * @see HtmlGeneratorElement#generateHtml(FormEntryContext)
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
		
		ret.append(generateHtmlForAdditionalWidgets(context));
		
		if (frequencyWidget != null) {
			ret.append(mss.getMessage("DrugOrder.frequency") + " ");
			ret.append(frequencyWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				ret.append(frequencyErrorWidget.generateHtml(context));
		}
		if (frequencyWeekWidget != null) {
			ret.append(" x ");
			ret.append(frequencyWeekWidget.generateHtml(context) + " ");
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
		if (orderDurationWidget != null) {
			ret.append(mss.getMessage("htmlformentry.general.for") + " ");
			ret.append(orderDurationWidget.generateHtml(context));
			ret.append(" " + mss.getMessage("htmlformentry.general.days") + " ");
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
		if (discontinuedReasonWidget != null) {
			ret.append(" | " + mss.getMessage("general.discontinuedReason") + " ");
			ret.append(discontinuedReasonWidget.generateHtml(context) + " ");
			if (context.getMode() != Mode.VIEW)
				ret.append(discontinuedReasonErrorWidget.generateHtml(context));
		}
		
		ret.append(generateHtmlForWidget(context, instructionsLabel + " ", instructionsWidget, instructionsErrorWidget));
		
		return ret.toString();
	}
	
	protected String generateHtmlForAdditionalWidgets(FormEntryContext context) {
		MessageSourceService mss = Context.getMessageSourceService();
		
		StringBuilder html = new StringBuilder();
		
		html.append(generateHtmlForWidget(context, mss.getMessage("htmlformentry.drugOrder.dosingType") + " ",
		    dosingTypeWidget, null));
		
		html.append(generateHtmlForWidget(context, mss.getMessage("DrugOrder.dose") + " ", doseWidget, doseErrorWidget));
		
		html.append(generateHtmlForWidget(context,
		    mss.getMessage("DrugOrder.dose") + " " + mss.getMessage("DrugOrder.units") + " ", doseUnitsWidget, null));
		
		html.append(
		    generateHtmlForWidget(context, mss.getMessage("DrugOrder.quantity") + " ", quantityWidget, quantityErrorWidget));
		
		html.append(generateHtmlForWidget(context,
		    mss.getMessage("DrugOrder.quantity") + " " + mss.getMessage("DrugOrder.units") + " ", quantityUnitsWidget,
		    null));
		
		html.append(generateHtmlForWidget(context, mss.getMessage("htmlformentry.drugOrder.duration") + " ", durationWidget,
		    durationErrorWidget));
		
		html.append(generateHtmlForWidget(context,
		    mss.getMessage("htmlformentry.drugOrder.duration") + " " + mss.getMessage("DrugOrder.units") + " ",
		    durationUnitsWidget, null));
		
		html.append(
		    generateHtmlForWidget(context, mss.getMessage("htmlformentry.drugOrder.route") + " ", routeWidget, null));
		
		html.append(generateHtmlForWidget(context, mss.getMessage("htmlformentry.drugOrder.careSetting") + " ",
		    careSettingWidget, null));
		
		html.append(generateHtmlForWidget(context, mss.getMessage("htmlformentry.drugOrder.numRefills") + " ",
		    numRefillsWidget, numRefillsErrorWidget));
		
		return html.toString();
	}
	
	public static String generateHtmlForWidget(FormEntryContext context, String label, Widget widget, Widget errorWidget) {
		StringBuilder html = new StringBuilder();
		if (widget != null) {
			if (label != null) {
				html.append(label);
			}
			html.append(widget.generateHtml(context) + " ");
			if (context.getMode() != Mode.VIEW && errorWidget != null)
				html.append(errorWidget.generateHtml(context));
		}
		return html.toString();
	}
	
	/**
	 * handleSubmission saves a drug order if in ENTER or EDIT-mode
	 *
	 * @see FormSubmissionControllerAction#handleSubmission(FormEntrySession, HttpServletRequest)
	 */
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		OrderTag orderTag = newOrderTag();
		
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
			
			if (!hideDoseAndFrequency && hideDose) {
				orderTag.frequency = (String) frequencyWidget.getValue(session.getContext(), submission);
				if (frequencyWeekWidget != null) {
					orderTag.frequency += "/d " + frequencyWeekWidget.getValue(session.getContext(), submission) + "d/w";
				}
			} else if (!hideDoseAndFrequency) {
				orderTag.dose = (Double) doseWidget.getValue(session.getContext(), submission);
				orderTag.frequency = (String) frequencyWidget.getValue(session.getContext(), submission);
				if (frequencyWeekWidget != null) {
					orderTag.frequency += "/d " + frequencyWeekWidget.getValue(session.getContext(), submission) + "d/w";
				}
			}
			
			populateOrderTag(orderTag, session, submission);
			
			if (session.getContext().getMode() == Mode.ENTER
			        || (session.getContext().getMode() == Mode.EDIT && existingOrder == null)) {
				enterOrder(session, orderTag);
			} else if (session.getContext().getMode() == Mode.EDIT) {
				editOrder(session, orderTag);
			}
		} else if (existingOrder != null) {
			voidOrder(session);
			
		}
	}
	
	protected OrderTag newOrderTag() {
		return new OrderTag();
	}
	
	protected void populateOrderTag(OrderTag oldOrderTag, FormEntrySession session, HttpServletRequest submission) {
		OrderTag orderTag = oldOrderTag;
		
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
		
		orderTag.quantity = (Double) quantityWidget.getValue(session.getContext(), submission);
		
		String quantityUnitsValue = (String) quantityUnitsWidget.getValue(session.getContext(), submission);
		if (quantityUnitsValue != null) {
			orderTag.quantityUnits = Context.getConceptService().getConcept(Integer.valueOf(quantityUnitsValue));
		}
		
		orderTag.duration = (Integer) durationWidget.getValue(session.getContext(), submission);
		
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
		
		orderTag.numRefills = (Integer) numRefillsWidget.getValue(session.getContext(), submission);
	}
	
	protected void voidOrder(FormEntrySession session) {
		//void order
		existingOrder.setVoided(true);
		existingOrder.setVoidedBy(Context.getAuthenticatedUser());
		existingOrder.setVoidReason("Drug De-selected in " + session.getForm().getName());
	}
	
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
		OrderType ot = Context.getOrderService().getOrderTypeByUuid(DRUG_ORDER_TYPE_UUID);
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
	
	/**
	 * <strong>Should</strong> return validation errors if doseWidget, startDateWidget or
	 * discontinuedDateWidget is invalid
	 *
	 * @see FormSubmissionControllerAction#validateSubmission(FormEntryContext, HttpServletRequest)
	 */
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
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
		
		return ret;
	}
	
	protected Date calculateAutoExpireDate(Date startDate, Integer orderDuration) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(startDate);
		cal.add(Calendar.DAY_OF_MONTH, orderDuration);
		return cal.getTime();
	}
	
	/* (non-Javadoc)
	 * @see org.openmrs.module.htmlformentry.element.GettingExistingOrder#getExistingOrder()
	 */
	@Override
	public DrugOrder getExistingOrder() {
		return existingOrder;
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
	
	public static DropdownWidget createCareSettingWidget(FormEntryContext context, boolean inpatientOnly) {
		DropdownWidget careSettingWidget = new DropdownWidget();
		List<CareSetting> careSettings = Context.getOrderService().getCareSettings(false);
		List<Option> options = new ArrayList<Option>();
		for (CareSetting careSetting : careSettings) {
			if (!inpatientOnly || careSetting.getCareSettingType().equals(CareSetting.CareSettingType.INPATIENT)) {
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
}
