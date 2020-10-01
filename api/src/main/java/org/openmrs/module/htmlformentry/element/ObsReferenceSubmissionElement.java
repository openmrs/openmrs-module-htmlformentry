package org.openmrs.module.htmlformentry.element;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.schema.ObsField;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.NumberFieldWidget;
import org.openmrs.module.htmlformentry.widget.RadioButtonsWidget;
import org.openmrs.module.htmlformentry.widget.SingleOptionWidget;
import org.openmrs.module.htmlformentry.widget.TextFieldWidget;
import org.openmrs.module.htmlformentry.widget.Widget;

public class ObsReferenceSubmissionElement extends ObsSubmissionElement {
	
	private Widget referenceDisplayWidget = null;
	
	private Boolean allowOverride = false;
	
	private Obs referenceObs = null;
	
	private String overrideLabel = "Override";
	
	private String tooltipTemplate = "({{encounterType}} on {{encounterDate}})";
	
	public ObsReferenceSubmissionElement(FormEntryContext context, Map<String, String> parameters) {
		
		super(context, parameters);
		
		Date encounterDate = context.getExistingEncounter() != null ? context.getExistingEncounter().getEncounterDatetime()
		        : context.getDefaultEncounterDate();
		
		if (StringUtils.isNotEmpty(parameters.get("tooltipTemplate"))) {
			tooltipTemplate = parameters.get("tooltipTemplate");
		}
		
		if (StringUtils.isNotEmpty(parameters.get("allowOverride"))) {
			allowOverride = Boolean.parseBoolean(parameters.get("allowOverride"));
		}
		
		if (StringUtils.isNotEmpty(parameters.get("overrideLabel"))) {
			overrideLabel = parameters.get("overrideLabel");
		}
		
		String conceptId = parameters.get("conceptId");
		Concept concept = null;
		
		// TODO handle error cases
		if (StringUtils.isNotBlank(conceptId)) {
			concept = HtmlFormEntryUtil.getConcept(conceptId);
		}
		
		// TODO handle error cases
		if (concept != null && encounterDate != null) {
			
			// note that this may match obs from the existing encounter, but we don't worry about that, because if there's an existing match, we won't be using this functionality (because getInitialValue(valueWidget) should be null)
			// also note that if an answer concept is present (ie it's a checkbox) we match on that as well
			// note that we are passing in the value of "1" for
			List<Obs> obsList = Context.getObsService().getObservations(
			    Collections.singletonList((Person) context.getExistingPatient()), null, Collections.singletonList(concept),
			    getAnswerConcept() != null ? Collections.singletonList(getAnswerConcept()) : null, null, null, null, 1, null,
			    new DateTime(encounterDate).withTime(0, 0, 0, 0).toDate(),
			    new DateTime(encounterDate).withTime(23, 59, 59, 999).toDate(), false);
			
			if (!obsList.isEmpty()) {
				// we are restricting to a single answer, so we will only get one
				// also note that by default it sorts by obsDatetime descending so it should return the most recent
				referenceObs = obsList.get(0);
			}
		}
	}
	
	@Override
	protected ObsField instatiateObsField() {
		ObsField field = super.instatiateObsField();
		field.setObsReference(true);
		return field;
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		
		// break these out in separate methods for VIEW and ENTER/EDIT for clarify
		if (context.getMode().equals(FormEntryContext.Mode.VIEW)) {
			return generateHtmlViewMode(context);
		} else if (context.getMode().equals(FormEntryContext.Mode.ENTER)
		        || (context.getMode().equals(FormEntryContext.Mode.EDIT))) {
			return generateHtmlEnterAndEditMode(context);
		} else {
			// should never get here, but just in case a mode besides VIEW, ENTER, and EDIT is added in the future
			return super.generateHtml(context);
		}
	}
	
	private String generateHtmlViewMode(FormEntryContext context) {
		
		// if we have a reference value but no existing obs, set the value widget to the value of the existing obs
		if (getInitialValue(valueWidget) == null && referenceObs != null) {
			
			// in view mode we just set the initial value of the actual widget to the value
			if (context.getMode().equals(FormEntryContext.Mode.VIEW)) {
				if (this.valueWidget instanceof NumberFieldWidget) {
					this.valueWidget.setInitialValue(referenceObs.getValueNumeric());
				}
				
				if (this.valueWidget instanceof SingleOptionWidget) {
					this.valueWidget.setInitialValue(referenceObs.getValueCoded());
				}
				
				if (this.valueWidget instanceof TextFieldWidget) {
					this.valueWidget.setInitialValue(referenceObs.getValueText());
				}
				
				if (this.valueWidget instanceof DateWidget) {
					this.valueWidget.setInitialValue(referenceObs.getValueDatetime());
				}
				
				if (this.valueWidget instanceof CheckboxWidget) {
					this.valueWidget.setInitialValue(referenceObs.getValueCoded());
				}
			}
		}
		
		// have ObsSubmissionElement generate it's HTML
		return super.generateHtml(context);
	}
	
	private String generateHtmlEnterAndEditMode(FormEntryContext context) {
		
		// have ObsSubmissionElement generate it's HTML
		String html = super.generateHtml(context);
		
		// we are only modifying if there's no value for the obs and there is a reference value
		if (getInitialValue(valueWidget) == null && referenceObs != null) {
			
			String fieldName = context.getFieldName(this.valueWidget);
			
			String viewWidgetHtml = "";
			String tooltip = "";
			String editWidgetHtml = "";
			String overrideButtonHtml = "";
			String units = "";
			
			// create a new reference widget to display the reference value
			if (this.valueWidget instanceof NumberFieldWidget) {
				this.referenceDisplayWidget = ((NumberFieldWidget) this.valueWidget).clone();
				this.referenceDisplayWidget.setInitialValue(referenceObs.getValueNumeric());
			} else if (this.valueWidget instanceof DropdownWidget) {
				this.referenceDisplayWidget = ((DropdownWidget) this.valueWidget).clone();
				this.referenceDisplayWidget.setInitialValue(referenceObs.getValueCoded());
			} else if (this.valueWidget instanceof RadioButtonsWidget) {
				this.referenceDisplayWidget = ((RadioButtonsWidget) this.valueWidget).clone();
				this.referenceDisplayWidget.setInitialValue(referenceObs.getValueCoded());
			} else if (this.valueWidget instanceof TextFieldWidget) {
				this.referenceDisplayWidget = ((TextFieldWidget) this.valueWidget).clone();
				this.referenceDisplayWidget.setInitialValue(referenceObs.getValueText());
			} else if (this.valueWidget instanceof DateWidget) {
				this.referenceDisplayWidget = ((DateWidget) this.valueWidget).clone();
				this.referenceDisplayWidget.setInitialValue(referenceObs.getValueDatetime());
			}
			// TODO does this work for boolean checkboxes?
			else if (this.valueWidget instanceof CheckboxWidget) {
				this.referenceDisplayWidget = ((CheckboxWidget) this.valueWidget).clone();
				this.referenceDisplayWidget.setInitialValue(referenceObs.getValueCoded());
			}
			
			if (showUnits) {
				units = this.getUnits(context);
			}
			
			// generate the tool tip
			// TODO show the tooltip in view mode as well?
			// TODO this is pretty quick-and-dirty, and not fully localized; add better templating in the future?
			String value = referenceObs.getValueAsString(Context.getLocale());
			if (showUnits) {
				value = value + " " + units;
			}
			tooltip = StringUtils.replace(tooltipTemplate, "{{value}}", value);
			if (referenceObs.getEncounter() != null) {
				tooltip = StringUtils.replace(tooltip, "{{encounterType}}",
				    referenceObs.getEncounter() != null ? referenceObs.getEncounter().getEncounterType().getName() : "");
				tooltip = StringUtils.replace(tooltip, "{{encounterDate}}",
				    referenceObs.getEncounter() != null
				            ? dateFormat().format(referenceObs.getEncounter().getEncounterDatetime())
				            : "");
			}
			
			// create a new viewWidget from the reference display widget
			FormEntryContext mockViewContext = new FormEntryContext(FormEntryContext.Mode.VIEW); // bit of a hack, to force the widget to render in View mode
			viewWidgetHtml = "<span id=\"" + fieldName + "-reference-view\" title=\"" + tooltip + "\">"
			        + (referenceDisplayWidget != null ? referenceDisplayWidget.generateHtml(mockViewContext) : "")
			        + (StringUtils.isNotBlank(units) ? "<span class=\"" + unitsCssClass + "\">" + units + "</span> " : "")
			        + "</span>";
			
			// wrap the existing html generated by the ObsSubmissionElement in a span so we can show/hide it
			editWidgetHtml = "<span id=\"" + fieldName + "-reference-edit\" style=\"display:none\">" + html + "</span>";
			
			// unfortunately, impossible to "override" a Checkbox since it just creates a single obs if checked, and no obs if unchecked
			if (allowOverride) {
				if (!(this.valueWidget instanceof CheckboxWidget)) {
					overrideButtonHtml = "<button id=\"" + fieldName + "-toggle-button\" type=\"button\" onclick=\"jQuery('#"
					        + fieldName + "-reference-view').hide();jQuery('#" + fieldName
					        + "-reference-edit').show();jQuery('#" + fieldName + "-toggle-button').hide();\">"
					        + overrideLabel + "</button>";
				}
			}
			
			html = viewWidgetHtml + " " + editWidgetHtml + " " + overrideButtonHtml;
		}
		
		return html;
	}
	
	// utility method since getInitialValue not on Widget interface
	private Object getInitialValue(Widget widget) {
		if (widget instanceof NumberFieldWidget) {
			return ((NumberFieldWidget) this.valueWidget).getInitialValue();
		}
		
		if (widget instanceof SingleOptionWidget) {
			return ((SingleOptionWidget) this.valueWidget).getInitialValue();
		}
		
		if (widget instanceof TextFieldWidget) {
			return ((TextFieldWidget) this.valueWidget).getInitialValue();
		}
		
		if (widget instanceof DateWidget) {
			return ((DateWidget) this.valueWidget).getInitialValue();
		}
		
		if (widget instanceof CheckboxWidget) {
			return ((CheckboxWidget) this.valueWidget).getInitialValue();
		}
		
		// TODO also handle DateTime and Time widgets
		return null;
	}
	
	private SimpleDateFormat dateFormat() {
		String df = Context.getAdministrationService().getGlobalProperty(HtmlFormEntryConstants.GP_DATE_FORMAT);
		if (StringUtils.isNotEmpty(df)) {
			return new SimpleDateFormat(df, Context.getLocale());
		} else {
			return Context.getDateFormat();
		}
	}
}
