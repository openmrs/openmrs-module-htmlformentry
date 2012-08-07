package org.openmrs.module.htmlformentry.element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.schema.ObsField;
import org.openmrs.module.htmlformentry.schema.ObsFieldAnswer;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.RadioButtonsWidget;
import org.openmrs.module.htmlformentry.widget.SingleOptionWidget;
import org.openmrs.module.htmlformentry.widget.Widget;

@Deprecated
public class ObsConceptSelectSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
    
    private List<Concept> concepts = new ArrayList<Concept>(); //possible concepts
    private List<String> conceptLabels = new ArrayList<String>(); //the text to show for possible concepts
    private Concept concept; // the winner
    private Concept answerConcept; //the answerConcept
    private String valueLabel; //the conceptAnswer label
    private Widget valueWidget;
    private ErrorWidget errorWidget;
    private Obs existingObs; // in edit mode, this allows submission to check whether the obs has been modified or not
    private boolean required;

    public ObsConceptSelectSubmissionElement(FormEntryContext context,
            Map<String, String> parameters) {
        try {
            String answerConceptId = parameters.get("answerConceptId");
            answerConcept = HtmlFormEntryUtil.getConcept(answerConceptId);
            if (answerConcept == null)
                throw new NullPointerException();
        } catch (Exception ex) {
            throw new IllegalArgumentException("You must provide a valid answerConcept in "
                    + parameters);
        }
        String conceptIds = parameters.get("conceptIds");
        for (StringTokenizer st = new StringTokenizer(conceptIds, ","); st.hasMoreTokens(); ) {
            String s = st.nextToken().trim();
            Concept concept = HtmlFormEntryUtil.getConcept(s);
            if (concept == null)
                throw new IllegalArgumentException("Cannot find concept value " + s + " in conceptIds attribute value. Parameters: " + parameters);
            concepts.add(concept);
        }
        if (concepts.size() == 0)
            throw new IllegalArgumentException("You must provide some valid concept Ids for the conceptIds attribute. Parameters: " + parameters);
        if ("true".equals(parameters.get("required"))) {
            required = true;
        }
        prepareWidgets(context, parameters);
    }
    
    private void prepareWidgets(FormEntryContext context,Map<String, String> parameters) {
        String userLocaleStr = Context.getLocale().toString();
        //find existing obs, if necessary
        if (context.getCurrentObsGroupConcepts() != null && context.getCurrentObsGroupConcepts().size() > 0) {
            existingObs = context.getObsFromCurrentGroup(concept, answerConcept);
        } else {
            existingObs = context.removeExistingObs(concepts, answerConcept);
        }
        errorWidget = new ErrorWidget();
        context.registerWidget(errorWidget);
        //next, just setup all variables:
        if (parameters.containsKey("labelNameTag")) {
            if (parameters.get("labelNameTag").equals("default"))
                valueLabel = answerConcept.getBestName(Context.getLocale()).getName();
            else
                throw new IllegalArgumentException(
                        "Name tags other than 'default' not yet implemented");
        } else if (parameters.containsKey("labelText")) {
            valueLabel = parameters.get("labelText");
        } else if (parameters.containsKey("labelCode")) {
            valueLabel = context.getTranslator().translate(userLocaleStr,
                    parameters.get("labelCode"));
        } else {
            valueLabel = answerConcept.getBestName(Context.getLocale()).getName();
        }
        if (parameters.get("conceptLabels") != null) {
            conceptLabels = Arrays.asList(parameters.get("conceptLabels").split(","));
        }
        if ("radio".equals(parameters.get("style"))) {
            valueWidget = new RadioButtonsWidget();
        } else { // dropdown
            valueWidget = new DropdownWidget();
            ((DropdownWidget) valueWidget).addOption(new Option());
        }
        for (int i = 0; i < concepts.size(); ++i) {
            Concept c = concepts.get(i);
            String label = null;
            if (conceptLabels != null && i < conceptLabels.size()) {
                label = conceptLabels.get(i);
            } else {
                label = c.getBestName(Context.getLocale()).getName();
            }
            ((SingleOptionWidget) valueWidget).addOption(new Option(
                label, c.getConceptId().toString(), false));
        }
        if (existingObs != null) {
            valueWidget.setInitialValue(existingObs.getConcept());
        }
        context.registerWidget(valueWidget);
        context.registerErrorWidget(valueWidget, errorWidget);
        
        ObsField field = new ObsField();
        field.setName(valueLabel);
        //field.setQuestion(concept);
        if (answerConcept != null) {
            ObsFieldAnswer ans = new ObsFieldAnswer();
            ans.setDisplayName(getValueLabel());
            ans.setConcept(answerConcept);
            field.setAnswers(Arrays.asList(ans));
        }
        context.getSchema().addField(field);
    }
    
    @Override
    public String generateHtml(FormEntryContext context) {
        StringBuilder ret = new StringBuilder();
        ret.append(valueLabel);
        if (!"".equals(valueLabel))
            ret.append(" ");
        // if value is required
        ret.append(valueWidget.generateHtml(context));
        if (required) {
            ret.append("<span class='required'>*</span>");
        }
        
        if (context.getMode() != Mode.VIEW) {
            ret.append(" ");
            ret.append(errorWidget.generateHtml(context));
        }
        return ret.toString();
    }
    
    @Override
    public Collection<FormSubmissionError> validateSubmission(
            FormEntryContext context, HttpServletRequest submission) {
        List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
        Object value = null;
        try {
            value = valueWidget.getValue(context, submission);
        } catch (Exception ex) {
            ret.add(new FormSubmissionError(valueWidget, ex.getMessage()));
        }

        if (required) {
            if (value == null) {
                ret.add(new FormSubmissionError(valueWidget, Context.getMessageSourceService().getMessage("htmlformentry.error.required")));
            } else if (value instanceof String) {
                String valueStr = (String) value;
                if (StringUtils.isEmpty(valueStr)) {
                    ret.add(new FormSubmissionError(valueWidget, Context.getMessageSourceService().getMessage("htmlformentry.error.required")));
                }
            }
        }
        
        return ret;
    }
    
    @Override
    public void handleSubmission(FormEntrySession session,
            HttpServletRequest submission) {
        Object value = valueWidget.getValue(session.getContext(), submission);
        try {
            if (value instanceof Concept)
                concept = (Concept) value;
            else
                concept = (Concept) HtmlFormEntryUtil.convertToType(value.toString(), Concept.class);
        } catch (Exception ex){
            throw new RuntimeException("Unable to convert response to a concept!");
        }
        Date obsDatetime = null;
        String accessionNumberValue = null;
        if (existingObs != null && session.getContext().getMode() == Mode.EDIT) {
            //TODO:  we're going to need a new version of this that uses the conceptAnswer as the matching concept...
            session.getSubmissionActions().modifyObs(existingObs, concept,
                    answerConcept, obsDatetime, accessionNumberValue, null);
        } else {
            if (value != null && !"".equals(value) && concept != null) {
                session.getSubmissionActions().createObs(concept, answerConcept,
                        obsDatetime, accessionNumberValue, null);
            }
        }
    }

    public List<Concept> getConcepts() {
        return concepts;
    }

    public List<String> getConceptLabels() {
        return conceptLabels;
    }

    public Concept getConcept() {
        return concept;
    }

    public Concept getAnswerConcept() {
        return answerConcept;
    }

    public String getValueLabel() {
        return valueLabel;
    }

    public Widget getValueWidget() {
        return valueWidget;
    }

    public ErrorWidget getErrorWidget() {
        return errorWidget;
    }

    public Obs getExistingObs() {
        return existingObs;
    }

    public boolean isRequired() {
        return required;
    }
    
    
    
}
