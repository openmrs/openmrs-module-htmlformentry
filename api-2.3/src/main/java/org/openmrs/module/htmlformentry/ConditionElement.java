package org.openmrs.module.htmlformentry;

import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_NAMESPACE;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openmrs.CodedOrFreeText;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.Condition;
import org.openmrs.ConditionClinicalStatus;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.element.HtmlGeneratorElement;
import org.openmrs.module.htmlformentry.widget.ConceptSearchAutocompleteWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.RadioButtonsWidget;
import org.openmrs.module.htmlformentry.widget.WidgetFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class ConditionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	public static final String GLOBAL_PROPERTY_CONDITIONS_CRITERIA = "coreapps.conditionListClasses";
	
	private static final String DEFAULT_CONDITION_LIST_CONCEPT_CLASS_NAME = "Diagnosis";

	private MessageSourceService mss;
	
	private boolean required;
	
	private String controlId;
	
	private Condition existingCondition;
	
	private Concept concept;
	
	private boolean showAdditionalDetails;

	// widgets
	private ConceptSearchAutocompleteWidget conditionSearchWidget;
	
	private DateWidget onSetDateWidget;
	
	private DateWidget endDateWidget;
	
	private TextFieldWidget additionalDetailsWidget;

	private RadioButtonsWidget conditionStatusesWidget;
	
	private ErrorWidget endDateErrorWidget;
	
	private ErrorWidget conditionSearchErrorWidget;
	
	private ErrorWidget conditionStatusesErrorWidget;
	
	private String wrapperDivId;
	
	private String endDatePickerWrapperId;
	
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		FormEntryContext context = session.getContext();
		Condition condition = bootstrap(context);

		CodedOrFreeText conditionConcept = new CodedOrFreeText();

		try {
			int conceptId = Integer.parseInt((String) conditionSearchWidget.getValue(session.getContext(), submission));
			conditionConcept.setCoded(Context.getConceptService().getConcept(conceptId));
		}
		catch (NumberFormatException e) {
			String nonCodedConcept = submission.getParameter(context.getFieldName(conditionSearchWidget));
			conditionConcept.setNonCoded(nonCodedConcept);
		}
		condition.setCondition(conditionConcept);

		ConditionClinicalStatus status = getStatus(context, submission);
		condition.setClinicalStatus(status);

		if (showAdditionalDetails) {
			condition.setAdditionalDetail(additionalDetailsWidget.getValue(context, submission));
		}

		condition.setOnsetDate(onSetDateWidget.getValue(context, submission));

		if (status != ConditionClinicalStatus.ACTIVE) {
			condition.setEndDate(endDateWidget.getValue(context, submission));
		}

		condition.setPatient(session.getPatient());

		condition.setFormField(FORM_NAMESPACE, session.generateControlFormPath(controlId, 0));

		if (concept != null && status == null && !required) {
			return;
		}

		session.getEncounter().addCondition(condition);
	}
	
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<>();
		Date givenOnsetDate = onSetDateWidget.getValue(context, submission);
		Date givenEndDate = endDateWidget.getValue(context, submission);
		String condition = StringUtils.isNotBlank((String) conditionSearchWidget.getValue(context, submission))
		        ? (String) conditionSearchWidget.getValue(context, submission)
		        : submission.getParameter(context.getFieldName(conditionSearchWidget));
		ConditionClinicalStatus status = getStatus(context, submission);
		
		if (context.getMode() != Mode.VIEW) {
			
			if (StringUtils.isBlank(condition) && required) {
				ret.add(new FormSubmissionError(context.getFieldName(conditionSearchWidget),
				        Context.getMessageSourceService().getMessage("htmlformentry.conditionui.condition.required")));
			}
			if (givenOnsetDate != null && givenEndDate != null) {
				if (givenOnsetDate.after(givenEndDate)) {
					ret.add(new FormSubmissionError(context.getFieldName(endDateWidget), Context.getMessageSourceService()
					        .getMessage("htmlformentry.conditionui.endDate.before.onsetDate.error")));
				}
			}
			if (status == null && required) {
				ret.add(new FormSubmissionError(context.getFieldName(conditionStatusesWidget),
				        Context.getMessageSourceService().getMessage("htmlformentry.conditionui.status.required")));
			}
		}
		return ret;
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		wrapperDivId = "htmlformentry-condition-" + controlId;
		endDatePickerWrapperId = "condition-end-date-" + controlId;
		if (mss == null) {
			mss = Context.getMessageSourceService();
		}
		initializeExistingCondition(context);
		StringBuilder ret = new StringBuilder();
		ret.append("<div id=\"" + wrapperDivId + "\">");
		// Show condition search
		ret.append(htmlForConditionSearchWidget(context));

		// Show additional details
		if (showAdditionalDetails) {
			ret.append(htmlForAdditionalDetailsWidget(context));
		}

		// Show condition state
		ret.append(htmlForConditionStatusesWidgets(context));

		// Show condition dates
		ret.append(htmlForConditionDatesWidget(context));

		ret.append("</div>");
		return ret.toString();
	}
	
	/**
	 * Bootstraps a new condition instance.
	 * <p>
	 * While in edit or view mode, it returns the existing condition
	 *
	 * @param context - the current FormEntryContext
	 * @return condition - the condition to edit or fill
	 */
	private Condition bootstrap(FormEntryContext context) {
		Condition ret = null;
		if (context.getMode() != Mode.ENTER) {
			if (existingCondition == null) {
				initializeExistingCondition(context);
			}
			ret = existingCondition;
		}
		if (ret == null) {
			ret = new Condition();
		}
		return ret;
	}
	
	/**
	 * Looks up the existing condition from the encounter to be edited or viewed.
	 * <p>
	 * It uses the {@code formFieldPath} to map the widget on the form to the target condition
	 *
	 * @param context - the current FormEntryContext
	 */
	private void initializeExistingCondition(FormEntryContext context) {
		if (context.getMode() != Mode.ENTER) {
			Set<Condition> conditions = context.getExistingEncounter().getConditions();
			for (Condition candidate : conditions) {
				
				// Get candidate control id
				String candidateControlId = HtmlFormEntryUtil2_3.getControlId(candidate);
				if (candidateControlId == null) {
					throw new IllegalStateException(
					        "A form recordable object was found to have no form namespace and path set, its control id in the form could not be determined.");
				}
				
				// Verify if it is a valid candidate for the condition
				if (StringUtils.equals(candidateControlId, controlId)) {
					this.existingCondition = candidate;
					return;
				}
			}
		}
	}
	
	// public visibility for testing purposes only
	public String htmlForConditionSearchWidget(FormEntryContext context) {
		String freeTextVal = null;
		String conditionLabel = mss.getMessage("htmlformentry.conditionui.condition.label");
		// use value set for GP 'coreapps.conditionListClasses' as concept source
		// a comma delimited list of concept class names or uuids is the expected value
		String conditionClassNames = Context.getAdministrationService()
		        .getGlobalProperty(GLOBAL_PROPERTY_CONDITIONS_CRITERIA);
		List<ConceptClass> allowedConceptClasses = new ArrayList<>();
		if (StringUtils.isNotBlank(conditionClassNames)) {
			for (String className : conditionClassNames.split(",")) {
				if (StringUtils.isNotBlank(className)) {
					// lookup by name
					ConceptClass conceptClass = Context.getConceptService().getConceptClassByName(className);
					if (conceptClass == null) {
						// lookup by uuid
						conceptClass = Context.getConceptService().getConceptClassByUuid(className);
					}
					if (conceptClass == null) {
						throw new IllegalArgumentException("Could not find ConceptClass identified by: " + className);
					}
					allowedConceptClasses.add(conceptClass);
				}
			}
		} else {
			// if no value was found for GP 'coreapps.conditionListClasses', use the concept class 'Diagnosis'
			ConceptClass conceptClass = Context.getConceptService()
			        .getConceptClassByName(DEFAULT_CONDITION_LIST_CONCEPT_CLASS_NAME);
			allowedConceptClasses.add(conceptClass);
		}
		conditionSearchWidget = new ConceptSearchAutocompleteWidget(null, allowedConceptClasses);
		String conditionNameTextInputId = context.registerWidget(conditionSearchWidget);
		conditionSearchErrorWidget = new ErrorWidget();
		
		if (concept == null) {
			if (existingCondition != null && context.getMode() != Mode.ENTER) {
				CodedOrFreeText codedOrFreeText = existingCondition.getCondition();
				if (codedOrFreeText.getCoded() != null) {
					conditionSearchWidget.setInitialValue(codedOrFreeText.getCoded());
				} else {
					freeTextVal = codedOrFreeText.getNonCoded();
				}
			}
		} else {
			// If exist concept define it as default value
			conditionSearchWidget.setInitialValue(concept);
		}
		
		context.registerErrorWidget(conditionSearchWidget, conditionSearchErrorWidget);

		StringBuilder ret = new StringBuilder();

		// Create wrapper id
		String searchWidgetWrapperId = "condition-" + controlId;
		ret.append("<div id=\"" + searchWidgetWrapperId + "\">");

		if (context.getMode() == Mode.VIEW) {
			// append label
			ret.append(conditionLabel + ": ");
		}
		// if the existing condition value is free text, initialise it as the default value
		if (StringUtils.isNotBlank(freeTextVal)) {
			if (context.getMode() == Mode.VIEW) {
				return ret.append(WidgetFactory.displayValue(freeTextVal)).toString();
			} else {
				String rawMarkup = conditionSearchWidget.generateHtml(context);
				String[] inputElements = rawMarkup.split(">", 2);
				for (String element : inputElements) {
					StringBuilder sb = new StringBuilder(element);
					// since we used '>' as the delimiter, it was removed by the splitter from the original string
					// if so, let's append it to avoid generating broken HTML
					if (!element.endsWith(">")) {
						sb.append(">");
					}
					int closingTagIndex = sb.indexOf("/>");
					sb.insert(closingTagIndex, " value=\"" + freeTextVal + "\"");
					ret.append(sb);
				}
			}
		} else {
			ret.append(conditionSearchWidget.generateHtml(context));
		}
		if (context.getMode() != Mode.VIEW) {
			ret.append(conditionSearchErrorWidget.generateHtml(context));
			ret.append("\n<script>jq('#" + conditionNameTextInputId + "').attr('placeholder',");
			ret.append(" '" + conditionLabel + "');\n");
			ret.append(" jq('#" + conditionNameTextInputId + "').css('min-width', '46.4%');\n");
			
			// Mark search box as read only if it has a concept
			if (concept != null) {
				ret.append("jq('#" + conditionNameTextInputId + "').attr(\"readonly\", true)\n");
			}
			
			// Add support for non-coded concept values.
			// This a hack to let the autocomplete widget accept values that aren't part of the concept list.
			ret.append("jq('#" + conditionNameTextInputId + "').blur(function(e){\n");
			ret.append("     var valueAttr = jq('#" + conditionNameTextInputId + "_hid" + "').attr('value');\n");
			ret.append("     if(valueAttr === \"ERROR\"){\n");
			ret.append("        jq('#" + conditionNameTextInputId + "_hid" + "').attr('value', '');\n");
			ret.append("     }\n");
			ret.append("});\n");
			ret.append("</script>\n");
		}
		ret.append("</div>");
		return ret.toString();
	}
	
	private String htmlForConditionStatusesWidgets(FormEntryContext context) {
		Option initialStatus = null;
		Option active = new Option(mss.getMessage("htmlformentry.conditionui.active.label"), "active", false);
		Option inactive = new Option(mss.getMessage("htmlformentry.conditionui.inactive.label"), "inactive", false);
		Option historyOf = new Option(mss.getMessage("htmlformentry.conditionui.historyOf.label"), "history-of", false);
		if (existingCondition != null && context.getMode() != Mode.ENTER) {
			ConditionClinicalStatus status = existingCondition.getClinicalStatus();
			if (status != null) {
				if (status.equals(ConditionClinicalStatus.ACTIVE)) {
					active.setSelected(true);
					initialStatus = active;
				} else if (status.equals(ConditionClinicalStatus.INACTIVE)) {
					inactive.setSelected(true);
					initialStatus = inactive;
				} else if (status.equals(ConditionClinicalStatus.HISTORY_OF)) {
					historyOf.setSelected(true);
					initialStatus = historyOf;
				}
			}
		}
		conditionStatusesWidget = new RadioButtonsWidget();
		conditionStatusesErrorWidget = new ErrorWidget();
		conditionStatusesWidget.addOption(active);
		conditionStatusesWidget.addOption(inactive);
		conditionStatusesWidget.addOption(historyOf);
		
		String radioGroupName = context.registerWidget(conditionStatusesWidget);
		context.registerErrorWidget(conditionStatusesWidget, conditionStatusesErrorWidget);
		
		StringBuilder sb = new StringBuilder();
		final String conditionStatusDivId = "condition-status-" + controlId;
		sb.append("<div id=\"" + conditionStatusDivId + "\">");
		if (context.getMode() != Mode.VIEW) {
			sb.append(conditionStatusesErrorWidget.generateHtml(context));
			sb.append(conditionStatusesWidget.generateHtml(context));
			sb.append("<script>");
			sb.append("jq(\"input[name='" + radioGroupName + "']\").change(function(e){\n"
			        + "    if($(this).val() == 'active') {\n" + "		document.getElementById('" + endDatePickerWrapperId
			        + "').style.visibility=\"hidden\"; \n" + "    } else {\n" + "		document.getElementById('"
			        + endDatePickerWrapperId + "').style.visibility=\"visible\";\n" + "    }\n" + "\n" + "});");
			sb.append("</script>");
			sb.append("<style>");
			sb.append("#" + conditionStatusDivId + " input {\n" + "    display: inline;\n" + "    float: none;\n" + "}\n"
			        + "#" + conditionStatusDivId + " label {\n" + "    display: inline;\n" + "}");
			sb.append("#" + conditionStatusDivId + " {\n" + "   padding:10px 0px;\n" + " }");
			sb.append("</style>");
		} else {
			if (initialStatus != null) {
				sb.append(mss.getMessage("htmlformentry.conditionui.status.label") + ": "
				        + WidgetFactory.displayValue(initialStatus.getValue()));
			} else {
				sb.append(mss.getMessage("htmlformentry.conditionui.status.label") + ": "
				        + WidgetFactory.displayDefaultEmptyValue());
			}
		}
		sb.append("</div>");
		return sb.toString();
	}
	
	private String htmlForConditionDatesWidget(FormEntryContext context) {
		onSetDateWidget = new DateWidget();
		endDateWidget = new DateWidget();
		String onsetDateLabel = mss.getMessage("htmlformentry.conditionui.onsetdate.label");
		String endDateLabel = mss.getMessage("htmlformentry.conditionui.endDate.label");
		
		if (existingCondition != null && context.getMode() != Mode.ENTER) {
			Date initialOnsetDate = existingCondition.getOnsetDate();
			Date initialEndDate = existingCondition.getEndDate();
			
			if (initialOnsetDate != null) {
				onSetDateWidget.setInitialValue(initialOnsetDate);
			}
			if (initialEndDate != null) {
				endDateWidget.setInitialValue(initialEndDate);
			}
		}
		String onsetDateTextInputId = context.registerWidget(onSetDateWidget) + "-display";
		endDateErrorWidget = new ErrorWidget();
		String endDateTextInputId = context.registerWidget(endDateWidget) + "-display";
		context.registerErrorWidget(endDateWidget, endDateErrorWidget);
		
		StringBuilder ret = new StringBuilder();
		ret.append("<ul>");
		ret.append("<li>");
		if (context.getMode() == Mode.VIEW) {
			// if in view mode, append label
			ret.append(onsetDateLabel + ": ");
		}
		ret.append(onSetDateWidget.generateHtml(context));
		ret.append("</li> <li>");
		ret.append("<span id=\"" + endDatePickerWrapperId + "\">");
		if (context.getMode() == Mode.VIEW) {
			// if in view mode, append label
			ret.append(endDateLabel + ": ");
		}
		ret.append(endDateWidget.generateHtml(context));
		ret.append("</span>");
		ret.append("</li>");
		if (context.getMode() != Mode.VIEW) {
			ret.append(endDateErrorWidget.generateHtml(context));
		}
		ret.append("</ul> <br/>");
		ret.append("<script> jq('#" + onsetDateTextInputId + "').attr('placeholder',");
		ret.append(" '" + onsetDateLabel + "');");
		ret.append("jq('#" + endDateTextInputId + "').attr('placeholder',");
		ret.append(" '" + endDateLabel + "');");
		ret.append("</script>");
		ret.append("<style>");
		ret.append("#" + wrapperDivId + " li {\n" + "	width:30%;\n" + "   float: left;\n" + "}");
		ret.append("#" + wrapperDivId + " ul {\n" + "	display:flow-root;\n" + "   width:85%" + "}");
		ret.append("</style>");
		return ret.toString();
	}

	private String htmlForAdditionalDetailsWidget(FormEntryContext context) {

		// Create wrapper id
		String additionalDetailsWrapperId = "condition-additional-details-" + controlId;

		// Register widget
		setAdditionalDetailsWidget(new TextFieldWidget());
		context.registerWidget(getAdditionalDetailsWidget());

		// Fill value for Edit/View
		if (context.getMode() != Mode.ENTER && existingCondition != null) {
			getAdditionalDetailsWidget().setInitialValue(existingCondition.getAdditionalDetail());
		}

		// Generate html
		StringBuilder ret = new StringBuilder();
		ret.append("<div id=\"" + additionalDetailsWrapperId + "\">");
		ret.append("<label>" + mss.getMessage("htmlformentry.conditionui.additionalDetails.label") + "</label>");
		ret.append(getAdditionalDetailsWidget().generateHtml(context));
		ret.append("</div>");

		return ret.toString();
	}

	private ConditionClinicalStatus getStatus(FormEntryContext context, HttpServletRequest request) {
		if (conditionStatusesWidget == null) {
			return null;
		}
		Object status = conditionStatusesWidget.getValue(context, request);
		if (status != null) {
			if (((String) status).equals("active")) {
				return ConditionClinicalStatus.ACTIVE;
			}
			if (((String) status).equals("inactive")) {
				return ConditionClinicalStatus.INACTIVE;
			}
			if (((String) status).equals("history-of")) {
				return ConditionClinicalStatus.HISTORY_OF;
			}
		}
		return null;
	}
	
	public void setConditionSearchWidget(ConceptSearchAutocompleteWidget conditionSearchWidget) {
		this.conditionSearchWidget = conditionSearchWidget;
	}
	
	public ConceptSearchAutocompleteWidget getConditionSearchWidget() {
		return conditionSearchWidget;
	}
	
	public void setOnSetDateWidget(DateWidget onSetDateWidget) {
		this.onSetDateWidget = onSetDateWidget;
	}
	
	public DateWidget getOnSetDateWidget() {
		return onSetDateWidget;
	}
	
	public void setEndDateWidget(DateWidget endDateWidget) {
		this.endDateWidget = endDateWidget;
	}
	
	public DateWidget getEndDateWidget() {
		return endDateWidget;
	}
	
	public TextFieldWidget getAdditionalDetailsWidget() {
		return additionalDetailsWidget;
	}

	public void setAdditionalDetailsWidget(TextFieldWidget additionalDetailsWidget) {
		this.additionalDetailsWidget = additionalDetailsWidget;
	}

	public void setConditionStatusesWidget(RadioButtonsWidget conditionStatusesWidget) {
		this.conditionStatusesWidget = conditionStatusesWidget;
	}
	
	public RadioButtonsWidget getConditionStatusesWidget() {
		return conditionStatusesWidget;
	}
	
	public void setRequired(boolean required) {
		this.required = required;
	}
	
	public String getControlId() {
		return controlId;
	}
	
	public void setControlId(String controlId) {
		this.controlId = controlId;
	}
	
	public Condition getExistingCondition() {
		return existingCondition;
	}
	
	public void setExistingCondition(Condition existingCondition) {
		this.existingCondition = existingCondition;
	}
	
	public Concept getConcept() {
		return concept;
	}
	
	public void setConcept(Concept concept) {
		this.concept = concept;
	}
	
	public boolean isShowAdditionalDetails() {
		return showAdditionalDetails;
	}

	public void setShowAdditionalDetails(boolean showAdditionalDetails) {
		this.showAdditionalDetails = showAdditionalDetails;
	}

	// available for testing purposes only
	public void setMessageSourceService(MessageSourceService mms) {
		this.mss = mms;
	}
	
}
