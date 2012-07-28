package org.openmrs.module.htmlformentry.element;


import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.util.OpenmrsUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Holds the widgets used to record details of exiting a person from care center, and serves as both the
 * HtmlGeneratorElement and the FormSubmissionControllerAction for exit from care functionality
 */
public class ExitFromCareSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {

    private DateWidget dateWidget;
    private ErrorWidget dateErrorWidget;
    private DropdownWidget reasonForExitWidget;
    private ErrorWidget reasonForExitErrorWidget;

    public ExitFromCareSubmissionElement(FormEntryContext context, Map<String, String> parameters) {
        createElement(context, parameters);
    }

    private void createElement(FormEntryContext context, Map<String, String> parameters) {

        Patient patient = context.getExistingPatient();

        dateWidget = new DateWidget();
        dateErrorWidget = new ErrorWidget();

        reasonForExitWidget = new DropdownWidget();
        reasonForExitErrorWidget = new ErrorWidget();

        // setting the initial values
        String conceptId = Context.getAdministrationService().getGlobalProperty("concept.reasonExitedCare");
        Concept reasonExitConcept = Context.getConceptService().getConcept(conceptId);
        Concept initialAnswer = null;
        List<Obs> obsList = Context.getObsService().getObservationsByPersonAndConcept(patient, reasonExitConcept);
        if (obsList != null && obsList.size() == 1) {
            dateWidget.setInitialValue(obsList.get(0).getObsDatetime());
            initialAnswer = obsList.get(0).getValueCoded();
            reasonForExitWidget.setInitialValue(initialAnswer.getDisplayString());
        }

        // populating with exit reason answer options
        boolean initialValueIsSet = !(reasonForExitWidget.getInitialValue() == null);
        reasonForExitWidget.addOption(new Option("Select Reason for Exit", "", !initialValueIsSet));

        if (reasonExitConcept != null) {
            for (ConceptAnswer conceptAnswer : reasonExitConcept.getAnswers()) {
                Concept answerConcept = conceptAnswer.getAnswerConcept();
                Option answerOption = new Option(answerConcept.getDisplayString(), answerConcept.getId().toString(), answerConcept.equals(initialAnswer));
                reasonForExitWidget.addOption(answerOption);
            }
        }
        context.registerWidget(dateWidget);
        context.registerErrorWidget(dateWidget, dateErrorWidget);
        context.registerWidget(reasonForExitWidget);
        context.registerErrorWidget(reasonForExitWidget, reasonForExitErrorWidget);


    }

    @Override
    /**
     * @should not allow to edit and submit if either date or reason is null
     * @should allow to submit a form if exit from care section is initially not filled
     */
    public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
        List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
        Date exitDate = null;
        Concept exitReasonAnswerConcept = null;

        Patient patient = context.getExistingPatient();
        String conId = Context.getAdministrationService().getGlobalProperty("concept.reasonExitedCare");
        Concept reasonExitConcept = Context.getConceptService().getConcept(conId);
        List<Obs> obsList = Context.getObsService().getObservationsByPersonAndConcept(patient, reasonExitConcept);

        try {
            if (dateWidget != null) {
                exitDate = dateWidget.getValue(context, submission);
                if (exitDate != null){
                    if (OpenmrsUtil.compare(exitDate, new Date()) > 0) {
                        throw new Exception("htmlformentry.error.cannotBeInFuture");
                    }
                }

                if (obsList != null && obsList.size() == 0) {
                    if (exitDate == null) {
                        if (reasonForExitWidget != null &&
                                HtmlFormEntryUtil.convertToType(reasonForExitWidget.getValue(context, submission).toString().trim(), Concept.class) != null) {
                            throw new Exception("htmlformentry.error.required");
                        }
                    }
                }
            }

        } catch (Exception ex) {
            ret.add(new FormSubmissionError(context.getFieldName(dateErrorWidget), Context.getMessageSourceService()
                    .getMessage(ex.getMessage())));
        }

        try {
            if (reasonForExitWidget != null) {
                Object value = reasonForExitWidget.getValue(context, submission);
                exitReasonAnswerConcept = (Concept) HtmlFormEntryUtil.convertToType(value.toString().trim(), Concept.class);
                if (obsList!= null && obsList.size() == 0){
                    if (exitReasonAnswerConcept == null) {
                        if (dateWidget != null && dateWidget.getValue(context, submission) != null) {
                        throw new Exception("htmlformentry.error.required");
                        }
                    }
                }

            }
        } catch (Exception ex) {
            ret.add(new FormSubmissionError(context.getFieldName(reasonForExitErrorWidget), Context.getMessageSourceService()
                    .getMessage(ex.getMessage())));
        }

        // this validation is added to avoid user resetting the 'exit from care'
        try {
            if (obsList != null && obsList.size() == 1) {
                if (exitDate == null || exitReasonAnswerConcept == null) {
                    throw new Exception("htmlformentry.error.required");
                }
            }
        } catch (Exception ex) {

            if (exitDate == null) {
                ret.add(new FormSubmissionError(context.getFieldName(dateErrorWidget), Context.getMessageSourceService()
                        .getMessage(ex.getMessage())));
            } else if (exitReasonAnswerConcept == null) {
                ret.add(new FormSubmissionError(context.getFieldName(reasonForExitErrorWidget), Context.getMessageSourceService()
                        .getMessage(ex.getMessage())));
            }
        }
        return ret;
    }

    @Override
    public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {

        Date date = null;
        Concept exitReasonConcept = null;
        if (dateWidget != null) {
            date = dateWidget.getValue(session.getContext(), submission);
        }
        if (reasonForExitWidget != null) {
            Object value = reasonForExitWidget.getValue(session.getContext(), submission);
            exitReasonConcept = (Concept) HtmlFormEntryUtil.convertToType(value.toString().trim(), Concept.class);
        }

        // only if user submits both date and reason we allow to exit from care, and this is done
        // to make sure a  user is able to submit a form with <exitfromcare> tag, without filling that
        // section, however if it is filled initially, user can't resubmit after changing the date and
        // reason fields to null back
        if (date != null && exitReasonConcept != null){
           session.getSubmissionActions().exitFromCare(date, exitReasonConcept);
        }

    }

    @Override
    public String generateHtml(FormEntryContext context) {

        StringBuilder sb = new StringBuilder();

        if (reasonForExitWidget != null) {
            sb.append(reasonForExitWidget.generateHtml(context));
            if (context.getMode() != FormEntryContext.Mode.VIEW)
                sb.append(reasonForExitErrorWidget.generateHtml(context));
        }

        // providing a blank space between the widgets
        sb.append("&nbsp;&nbsp;");

        if (dateWidget != null) {
            sb.append(dateWidget.generateHtml(context));
            if (context.getMode() != FormEntryContext.Mode.VIEW)
                sb.append(dateErrorWidget.generateHtml(context));
        }

        return sb.toString();
    }
}
