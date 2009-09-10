package org.openmrs.module.htmlformentry.element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptNumeric;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.schema.ObsField;
import org.openmrs.module.htmlformentry.schema.ObsFieldAnswer;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.LocationWidget;
import org.openmrs.module.htmlformentry.widget.NumberFieldWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.RadioButtonsWidget;
import org.openmrs.module.htmlformentry.widget.SingleOptionWidget;
import org.openmrs.module.htmlformentry.widget.TextFieldWidget;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.openmrs.util.OpenmrsUtil;

/**
 *
 */
public class ObsSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {

    private Concept concept;
    private String valueLabel;
    private Widget valueWidget;
    private String dateLabel;
    private DateWidget dateWidget;
    private ErrorWidget errorWidget;
    private boolean allowFutureDates = false;
    private Concept answerConcept;
    private List<Concept> answers = new ArrayList<Concept>();
    private List<String> answerLabels = new ArrayList<String>();
    private String answerLabel;
    private Obs existingObs; // in edit mode, this allows submission to check whether the obs has been modified or not
    
    public ObsSubmissionElement(FormEntryContext context, Map<String, String> parameters) {
        try {
            String conceptId = parameters.get("conceptId");
            concept = Context.getConceptService().getConcept(Integer.valueOf(conceptId));
            if (concept == null)
                throw new NullPointerException();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot find concept in " + parameters);
        }
        if ("true".equals(parameters.get("allowFutureDates")))
            allowFutureDates = true;
        prepareWidgets(context, parameters);
    }

    private void prepareWidgets(FormEntryContext context, Map<String, String> parameters) {
    	String userLocaleStr = Context.getLocale().toString();
        try {
            answerConcept = Context.getConceptService().getConcept(Integer.valueOf(parameters.get("answerConceptId")));
        } catch (Exception ex) { }
        if (context.getCurrentObsGroupConcepts() != null && context.getCurrentObsGroupConcepts().size() > 0) {
            existingObs = context.getObsFromCurrentGroup(concept, answerConcept);
        } else {
            if (concept.getDatatype().isBoolean() && "checkbox".equals(parameters.get("style"))) {
                // since a checkbox has one value we need to look for an exact match for that value
                if ("false".equals(parameters.get("value"))) {
                    existingObs = context.removeExistingObs(concept, false);
                } else {
                    // if not 'false' we treat as 'true'
                    existingObs = context.removeExistingObs(concept, true);
                }
                
            } else {
                existingObs = context.removeExistingObs(concept, answerConcept);
            }
        }
        
        errorWidget = new ErrorWidget();
        context.registerWidget(errorWidget);
        
        if (parameters.containsKey("labelNameTag")) {
            if (parameters.get("labelNameTag").equals("default"))
                valueLabel = concept.getBestName(Context.getLocale()).getName();
            else
                throw new IllegalArgumentException("Name tags other than 'default' not yet implemented");
        } else if (parameters.containsKey("labelText")) {
            valueLabel = parameters.get("labelText");
        } else if (parameters.containsKey("labelCode")) {
            valueLabel = context.getTranslator().translate(userLocaleStr, parameters.get("labelCode"));
        } else {
            valueLabel = "";
        }
        if (concept.getDatatype().isNumeric()) {
            ConceptNumeric cn = Context.getConceptService().getConceptNumeric(concept.getConceptId());
            valueWidget = new NumberFieldWidget(cn);
            if (existingObs != null) {
                valueWidget.setInitialValue(existingObs.getValueNumeric());
            }
        } else if (concept.getDatatype().isText()) {
        	if ("location".equals(parameters.get("style"))) {
        		valueWidget = new LocationWidget();
        	}
        	else {
	            Integer rows = null;
	            Integer cols = null;
	            try {
	                rows = Integer.valueOf(parameters.get("rows"));
	            } catch (Exception ex) { }
	            try {
	                cols = Integer.valueOf(parameters.get("cols"));
	            } catch (Exception ex) { }
	            if (rows != null || cols != null || "textarea".equals(parameters.get("style"))) {
	                valueWidget = new TextFieldWidget(rows, cols);
	            } else {
	                Integer size = null;
	                try {
	                    size = Integer.valueOf(parameters.get("size"));
	                } catch (Exception ex) { }
	                valueWidget = new TextFieldWidget(size);
	            }
        	}
            if (existingObs != null) {
                Object value;
                if ("location".equals(parameters.get("style"))) {
                    value = Context.getLocationService().getLocation(Integer.valueOf(existingObs.getValueText()));
                } else {
                    value = existingObs.getValueText();
                }
                valueWidget.setInitialValue(value);
            }
        } else if (concept.getDatatype().isCoded()) {
            if (parameters.get("answerConceptIds") != null) {
                try {
                    for (StringTokenizer st = new StringTokenizer(parameters.get("answerConceptIds"), ", "); st.hasMoreTokens(); ) {
                        Integer conceptId = Integer.valueOf(st.nextToken());
                        Concept c = Context.getConceptService().getConcept(conceptId);
                        if (c == null)
                            throw new RuntimeException("Cannot find concept " + conceptId);
                        answers.add(c);
                    }
                } catch (Exception ex) {
                    throw new RuntimeException("Error in answer list for concept " + concept.getConceptId() + " (" + ex.toString() + "): " + answers);
                }
            }
            else if (parameters.get("answerClasses") != null) {
                try {
                    for (StringTokenizer st = new StringTokenizer(parameters.get("answerClasses"), ","); st.hasMoreTokens(); ) {
                    	String className = st.nextToken().trim();
                    	ConceptClass cc = Context.getConceptService().getConceptClassByName(className);
                        if (cc == null) {
                            throw new RuntimeException("Cannot find concept class " + className);
                        }
                    	answers.addAll(Context.getConceptService().getConceptsByClass(cc));
                    }
                    Collections.sort(answers, conceptNameComparator);
                } catch (Exception ex) {
                    throw new RuntimeException("Error in answer class list for concept " + concept.getConceptId() + " (" + ex.toString() + "): " + answers);
                }
            }
            if (parameters.get("answerLabels") != null) {
                answerLabels = Arrays.asList(parameters.get("answerLabels").split(","));
            }
            if (parameters.get("answerCodes") != null) {
            	String[] split = parameters.get("answerCodes").split(",");
            	for (String s : split) {
            		answerLabels.add(context.getTranslator().translate(userLocaleStr, s));
            	}
            }
            
            if (answerConcept != null) {
                // if there's also an answer concept specified, this is a single checkbox
                answerLabel = parameters.get("answerLabel");
                if (answerLabel == null) {
                    String answerCode = parameters.get("answerCode");
                    if (answerCode != null) {
                        answerLabel = context.getTranslator().translate(userLocaleStr, answerCode);
                    }
                    else {
                    	answerLabel = answerConcept.getBestName(Context.getLocale()).getName();
                    }
                }
                valueWidget = new CheckboxWidget(answerLabel, answerConcept.getConceptId().toString());
                if (existingObs != null) {
                    valueWidget.setInitialValue(existingObs.getValueCoded());
                }
            } else if ("true".equals(parameters.get("multiple"))) {
                // if this is a select-multi, we need a group of checkboxes
                throw new RuntimeException("Multi-select coded questions are not yet implemented");
            } else {
            	// If no answers are specified, use all available answers
            	if (answers == null || answers.isEmpty()) {
            		answers = new ArrayList<Concept>();
                    for (ConceptAnswer ca : concept.getAnswers(false)) {
                    	answers.add(ca.getAnswerConcept());
                    }
                    Collections.sort(answers, conceptNameComparator);
            	}
            	
            	// Show Radio Buttons if specified, otherwise default to Drop Down
            	boolean isRadio = "radio".equals(parameters.get("style"));
            	if (isRadio) {
            		valueWidget = new RadioButtonsWidget();
            	}
            	else {
            		valueWidget = new DropdownWidget();
            		((DropdownWidget) valueWidget).addOption(new Option());
            	}
                for (int i = 0; i < answers.size(); ++i) {
                	Concept c = answers.get(i);
                	String label = null;
                    if (answerLabels != null && i < answerLabels.size()) {
                        label = answerLabels.get(i);
                    } else {
                        label = c.getBestName(Context.getLocale()).getName();
                    }
                    ((SingleOptionWidget) valueWidget).addOption(new Option(label, c.getConceptId().toString(), false));
            	}
                if (existingObs != null) {
                    valueWidget.setInitialValue(existingObs.getValueCoded());
                }
            }
        } else if (concept.getDatatype().isBoolean()) {
        	String noStr = parameters.get("noLabel");
        	if (StringUtils.isEmpty(noStr)) {
        		noStr = context.getTranslator().translate(userLocaleStr, "general.no");
        	}
        	String yesStr = parameters.get("yesLabel");
        	if (StringUtils.isEmpty(yesStr)) {
        		yesStr = context.getTranslator().translate(userLocaleStr, "general.yes");
        	}
            if ("checkbox".equals(parameters.get("style"))) {
                valueWidget = new CheckboxWidget(valueLabel, parameters.get("value") != null ? parameters.get("value") : "true");
                valueLabel = "";
                if (existingObs != null) {
                    valueWidget.setInitialValue(existingObs.getValueAsBoolean());
                }
            } else if ("no_yes".equals(parameters.get("style"))) {
                valueWidget = new RadioButtonsWidget();
                ((RadioButtonsWidget) valueWidget).addOption(new Option(noStr, "false", false));
                ((RadioButtonsWidget) valueWidget).addOption(new Option(yesStr, "true", false));
                if (existingObs != null) {
                    valueWidget.setInitialValue(existingObs.getValueAsBoolean());
                }
            } else if ("yes_no".equals(parameters.get("style"))) {
                valueWidget = new RadioButtonsWidget();
                ((RadioButtonsWidget) valueWidget).addOption(new Option(yesStr, "true", false));
                ((RadioButtonsWidget) valueWidget).addOption(new Option(noStr, "false", false));
                if (existingObs != null) {
                    valueWidget.setInitialValue(existingObs.getValueAsBoolean());
                }
            } else if ("no_yes_dropdown".equals(parameters.get("style"))) {
                valueWidget = new DropdownWidget();
                ((DropdownWidget) valueWidget).addOption(new Option());
                ((DropdownWidget) valueWidget).addOption(new Option(noStr, "false", false));
                ((DropdownWidget) valueWidget).addOption(new Option(yesStr, "true", false));
                if (existingObs != null) {
                    valueWidget.setInitialValue(existingObs.getValueAsBoolean());
                }
            } else if ("yes_no_dropdown".equals(parameters.get("style"))) {
                valueWidget = new DropdownWidget();
                ((DropdownWidget) valueWidget).addOption(new Option());
                ((DropdownWidget) valueWidget).addOption(new Option(yesStr, "true", false));
                ((DropdownWidget) valueWidget).addOption(new Option(noStr, "false", false));
                if (existingObs != null) {
                    valueWidget.setInitialValue(existingObs.getValueAsBoolean());
                }
            } else {
                throw new RuntimeException("Boolean with style = " + parameters.get("style") + " not yet implemented (concept = " + concept.getConceptId() + ")");
            }
        } else if (concept.getDatatype().isDate()) {
            valueWidget = new DateWidget();
            if (existingObs != null)
                valueWidget.setInitialValue(existingObs.getValueDatetime());
        } else {
            throw new RuntimeException("Cannot handle datatype: " + concept.getDatatype().getName() + " (for concept " + concept.getConceptId() + ")");
        }
        context.registerWidget(valueWidget);
        context.registerErrorWidget(valueWidget, errorWidget);
        
        // if a date is requested, do that too
        if ("true".equals(parameters.get("showDate")) || parameters.containsKey("dateLabel")) {
            if (parameters.containsKey("dateLabel"))
                dateLabel = parameters.get("dateLabel");
            dateWidget = new DateWidget();
            context.registerWidget(dateWidget);
            context.registerErrorWidget(dateWidget, errorWidget);
            if (existingObs != null) {
                dateWidget.setInitialValue(existingObs.getObsDatetime());
            }
        }
        
        ObsField field = new ObsField();
        field.setName(valueLabel);
        field.setQuestion(concept);
        if (answerConcept != null) {
        	ObsFieldAnswer ans = new ObsFieldAnswer();
        	ans.setDisplayName(getAnswerLabel());
        	ans.setConcept(answerConcept);
        	field.setAnswers(Arrays.asList(ans));
        }
        else if (answers != null) {
        	for (int i=0; i<answers.size(); i++) {
        		ObsFieldAnswer ans = new ObsFieldAnswer();
        		ans.setConcept(answers.get(i));
        		if (i < answerLabels.size()) {
        			ans.setDisplayName(answerLabels.get(i));
        		}
            	field.getAnswers().add(ans);
        	}
        }
        if (context.getActiveObsGroup() != null) {
        	context.getActiveObsGroup().getChildren().add(field);
        }
        else {
        	context.getSchema().addField(field);
        }
    }
    
    public String generateHtml(FormEntryContext context) {
        StringBuilder ret = new StringBuilder();
        ret.append(valueLabel);
        if (!"".equals(valueLabel))
            ret.append(" ");
        ret.append(valueWidget.generateHtml(context));
        if (dateWidget != null) {
            ret.append(" ");
            if (dateLabel != null) {
                ret.append(dateLabel);
            }
            ret.append(dateWidget.generateHtml(context));
        }
        if (context.getMode() != Mode.VIEW) {
	        ret.append(" ");
	        ret.append(errorWidget.generateHtml(context));
        }
        return ret.toString();
    }

    public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
        List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
        Object value = null;
        Object date = null;

        try {
            value = valueWidget.getValue(context, submission);
        } catch (Exception ex) {
            ret.add(new FormSubmissionError(valueWidget, ex.getMessage()));
        }
        
        try {
            if (dateWidget != null)
                date = dateWidget.getValue(context, submission);
        } catch (Exception ex) {
            ret.add(new FormSubmissionError(dateWidget, ex.getMessage()));
        }
        
        if (value == null && date != null)
            ret.add(new FormSubmissionError(valueWidget, Context.getMessageSourceService().getMessage("htmlformentry.error.dateWithoutValue")));
        
        if (date != null && OpenmrsUtil.compare((Date) date, new Date()) > 0)
            ret.add(new FormSubmissionError(dateWidget, Context.getMessageSourceService().getMessage("htmlformentry.error.cannotBeInFuture")));
        
        if (value instanceof Date && !allowFutureDates && OpenmrsUtil.compare((Date) value, new Date()) > 0) {
            ret.add(new FormSubmissionError(valueWidget, Context.getMessageSourceService().getMessage("htmlformentry.error.cannotBeInFuture")));
        }

        return ret;
    }
    
    public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
        Object value = valueWidget.getValue(session.getContext(), submission);
        Date obsDatetime = null;
        if (dateWidget != null)
            obsDatetime = (Date) dateWidget.getValue(session.getContext(), submission);
        
        if (existingObs != null && session.getContext().getMode() == Mode.EDIT) {
            // call this regardless of whether the new value is null -- the modifyObs method is smart
            session.getSubmissionActions().modifyObs(existingObs, concept, value, obsDatetime);
        } else {
            if (value != null && !"".equals(value)) {
                session.getSubmissionActions().createObs(concept, value, obsDatetime);
            }
        }
    }
    
    private Comparator<Concept> conceptNameComparator = new Comparator<Concept>() {
	   	public int compare(Concept c1, Concept c2) {
	   		String n1 = c1.getBestName(Context.getLocale()).getName();
	   		String n2 = c2.getBestName(Context.getLocale()).getName();
	   		return n1.compareTo(n2);
	   	}
    };

	/**
	 * @return the concept
	 */
	public Concept getConcept() {
		return concept;
	}

	/**
	 * @return the answerConcept
	 */
	public Concept getAnswerConcept() {
		return answerConcept;
	}

	/**
	 * @return the conceptAnswers
	 */
	public List<Concept> getAnswers() {
		return answers;
	}

	/**
	 * @return the answerLabels
	 */
	public List<String> getAnswerLabels() {
		return answerLabels;
	}

	/**
	 * @return the answerLabel
	 */
	public String getAnswerLabel() {
		return answerLabel;
	}
}
