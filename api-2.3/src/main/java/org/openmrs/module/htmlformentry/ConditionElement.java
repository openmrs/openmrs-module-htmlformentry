package org.openmrs.module.htmlformentry;

import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_NAMESPACE;

import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.element.HtmlGeneratorElement;
import org.openmrs.module.htmlformentry.widget.*;

public class ConditionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	public static final String GLOBAL_PROPERTY_CONDITIONS_CRITERIA = "coreapps.conditionListClasses";
	
	private static final String DEFAULT_CONDITION_LIST_CONCEPT_CLASS_NAME = "Diagnosis";
	
	private Locale locale = Context.getLocale();

	private MessageSourceService mss;
	
	private boolean required;
	
	private String controlId;

	private Concept concept;

	private boolean showAdditionalDetails;

	// widgets
	private ConceptSearchAutocompleteWidget conditionSearchWidget;

	private TextFieldWidget additionalDetailsWidget;

	private RadioButtonsWidget conditionStatusesWidget;

	private ErrorWidget conditionSearchErrorWidget;
	
	private ErrorWidget conditionStatusesErrorWidget;

	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		FormEntryContext context = session.getContext();
		if (context.getMode() != Mode.VIEW) {
			Condition condition = bootstrap(context);

			// Handle condition concept
			CodedOrFreeText conditionConcept = new CodedOrFreeText();
			try {
				int conceptId = Integer.parseInt((String) conditionSearchWidget.getValue(session.getContext(), submission));
				conditionConcept.setCoded(Context.getConceptService().getConcept(conceptId));
			}
			catch (NumberFormatException e) {
				String nonCodedConcept = submission.getParameter(context.getFieldName(conditionSearchWidget));
				if (StringUtils.isBlank(nonCodedConcept) && !required) {
					// ignore silently
					return;
				}
				conditionConcept.setNonCoded(nonCodedConcept);
			}
			condition.setCondition(conditionConcept);

			// Handle Condition Clinical Status
			ConditionClinicalStatus status = getStatus(context, submission);
			condition.setClinicalStatus(status);

			// Handle Additional Details widget
			if (showAdditionalDetails) {
				condition.setAdditionalDetail(additionalDetailsWidget.getValue(context, submission));
			}

			// Handle Patient
			condition.setPatient(session.getPatient());

			// Handle Form field position
			condition.setFormField(FORM_NAMESPACE, session.generateControlFormPath(controlId, 0));

			// Only save the condition if no concept defined or is defined but has status
			if (concept == null || (concept != null && status != null)) {
				session.getEncounter().addCondition(condition);
			}
		}
	}
	
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		String condition = StringUtils.isNotBlank((String) conditionSearchWidget.getValue(context, submission))
		        ? (String) conditionSearchWidget.getValue(context, submission)
		        : submission.getParameter(context.getFieldName(conditionSearchWidget));
		ConditionClinicalStatus status = getStatus(context, submission);
		
		if (context.getMode() != Mode.VIEW) {
			
			if (StringUtils.isBlank(condition) && required) {
				ret.add(new FormSubmissionError(context.getFieldName(conditionSearchWidget),
				        Context.getMessageSourceService().getMessage("htmlformentry.conditionui.condition.required")));
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

		// Create wrapper id
		String wrapperDivId = "htmlformentry-condition-" + controlId;

		// Load messages service
		if (mss == null) {
			mss = Context.getMessageSourceService();
		}

		// Find condition for form position
		Condition existingCondition = initializeExistingCondition(context);

		// Generate html
		StringBuilder ret = new StringBuilder();
		ret.append("<div id=\"" + wrapperDivId + "\">");
		
		// Add condition search html
		ret.append(htmlForConditionSearchWidget(context, existingCondition));

		// Add additional details html
		if (showAdditionalDetails) {
			ret.append(htmlForAdditionalDetailsWidget(context, existingCondition));
		}

		// Add condition state html
		ret.append(htmlForConditionStatusesWidgets(context, existingCondition));

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
			ret = initializeExistingCondition(context);
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
	 * @return Condition - existing condition for form position
	 */
	private Condition initializeExistingCondition(FormEntryContext context) {
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
					return candidate;
				}
			}
		}
		// Return null if no candidate is found
		return null;
	}
	
	// public visibility for testing purposes only
	public String htmlForConditionSearchWidget(FormEntryContext context, Condition existingCondition) {
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
	
	private String htmlForConditionStatusesWidgets(FormEntryContext context, Condition existingCondition) {
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

	private String htmlForAdditionalDetailsWidget(FormEntryContext context, Condition existingCondition) {

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
