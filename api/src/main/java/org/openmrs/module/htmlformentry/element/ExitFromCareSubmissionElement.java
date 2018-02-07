package org.openmrs.module.htmlformentry.element;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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
import org.openmrs.module.htmlformentry.widget.TextFieldWidget;
import org.openmrs.util.OpenmrsUtil;

/**
 * Holds the widgets used to record details of exiting a person from care center, and serves as both the
 * HtmlGeneratorElement and the FormSubmissionControllerAction for exit from care functionality
 */
public class ExitFromCareSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {

    private DateWidget dateWidget;
    private ErrorWidget dateErrorWidget;
    private DropdownWidget reasonForExitWidget;
    private ErrorWidget reasonForExitErrorWidget;
    private DropdownWidget causeOfDeathWidget;
    private ErrorWidget causeOfDeathErrorWidget;
    private TextFieldWidget otherReasonWidget;
    private ErrorWidget otherReasonErrorWidget;

    public ExitFromCareSubmissionElement(FormEntryContext context, Map<String, String> parameters) {
        createElement(context, parameters);
    }

    private void createElement(FormEntryContext context, Map<String, String> parameters) {

        Patient patient = context.getExistingPatient();

        dateWidget = new DateWidget();
        dateErrorWidget = new ErrorWidget();

        reasonForExitWidget = new DropdownWidget();
        reasonForExitErrorWidget = new ErrorWidget();

        causeOfDeathWidget = new DropdownWidget();
        causeOfDeathErrorWidget = new ErrorWidget();

        otherReasonWidget = new TextFieldWidget();
        otherReasonErrorWidget = new ErrorWidget();

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
        reasonForExitWidget.addOption(new Option(Context.getMessageSourceService().getMessage("htmlformentry.chooseAReasonForExit"), "", !initialValueIsSet));

        if (reasonExitConcept != null) {
            for (ConceptAnswer conceptAnswer : reasonExitConcept.getAnswers()) {
                Concept answerConcept = conceptAnswer.getAnswerConcept();
                Option answerOption = new Option(answerConcept.getDisplayString(), answerConcept.getId().toString(), answerConcept.equals(initialAnswer));
                reasonForExitWidget.addOption(answerOption);
            }
        }

        // set the cause for the death and reason for death fields if the reason for the patient's exit is, that the
        // patient has died
        String causeOfDeathConId = Context.getAdministrationService().getGlobalProperty("concept.causeOfDeath");
        Concept causeOfDeathConcept = Context.getConceptService().getConcept(causeOfDeathConId);
        List<Obs> obsDeath = Context.getObsService().getObservationsByPersonAndConcept(patient, causeOfDeathConcept);
        Concept initialCauseOfDeath = null;

        if (obsDeath != null && obsDeath.size() == 1) {
            initialCauseOfDeath = obsDeath.get(0).getValueCoded();
            causeOfDeathWidget.setInitialValue(initialCauseOfDeath.getDisplayString());
            if (obsDeath.get(0).getValueText() != null) {
                otherReasonWidget.setInitialValue(obsDeath.get(0).getValueText());
            }
        }

        // populating with cause of death answer options
        boolean causeOfDeathIsSet = !(causeOfDeathWidget.getInitialValue() == null);
        causeOfDeathWidget.addOption(new Option(Context.getMessageSourceService().getMessage("htmlformentry.chooseACauseToDeath"), "", !causeOfDeathIsSet));

        if (causeOfDeathConcept != null) {
            for (ConceptAnswer conceptAnswer : causeOfDeathConcept.getAnswers()) {
                Concept answerConcept = conceptAnswer.getAnswerConcept();
                Option answerOption = new Option(answerConcept.getDisplayString(), answerConcept.getId().toString(), answerConcept.equals(initialCauseOfDeath));
                causeOfDeathWidget.addOption(answerOption);
            }
        }

        context.registerWidget(dateWidget);
        context.registerErrorWidget(dateWidget, dateErrorWidget);
        context.registerWidget(reasonForExitWidget);
        context.registerErrorWidget(reasonForExitWidget, reasonForExitErrorWidget);
        context.registerWidget(causeOfDeathWidget);
        context.registerErrorWidget(causeOfDeathWidget, causeOfDeathErrorWidget);
        context.registerWidget(otherReasonWidget);
        context.registerErrorWidget(otherReasonWidget, otherReasonErrorWidget);


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
        Concept causeOfDeathAnswerConcept = null;

        Patient patient = context.getExistingPatient();
        String reasonConId = Context.getAdministrationService().getGlobalProperty("concept.reasonExitedCare");
        Concept reasonExitConcept = Context.getConceptService().getConcept(reasonConId);
        List<Obs> obsList = Context.getObsService().getObservationsByPersonAndConcept(patient, reasonExitConcept);

        String causeOfDeathConId = Context.getAdministrationService().getGlobalProperty("concept.causeOfDeath");
        Concept causeOfDeathConcept = Context.getConceptService().getConcept(causeOfDeathConId);
        List<Obs> obsDeath = Context.getObsService().getObservationsByPersonAndConcept(patient, causeOfDeathConcept);

        String patientDiedConId = Context.getAdministrationService().getGlobalProperty("concept.patientDied");
        Concept patientDiedConcept = Context.getConceptService().getConcept(patientDiedConId);
        String otherNonCodedConId = Context.getAdministrationService().getGlobalProperty("concept.otherNonCoded");
        Concept otherNonCodedConcept = Context.getConceptService().getConcept(otherNonCodedConId);

        if (dateWidget != null) {
            exitDate = dateWidget.getValue(context, submission);
            if (exitDate != null) {
                if (OpenmrsUtil.compare(exitDate, new Date()) > 0) {
                    ret.add(new FormSubmissionError(context.getFieldName(dateErrorWidget), Context.getMessageSourceService()
                            .getMessage("htmlformentry.error.cannotBeInFuture")));

                }
            }

            if (obsList != null && obsList.size() == 0) {
                if (exitDate == null) {
                    if (reasonForExitWidget != null &&
                            HtmlFormEntryUtil.convertToType(reasonForExitWidget.getValue(context, submission).toString().trim(), Concept.class) != null) {
                        ret.add(new FormSubmissionError(context.getFieldName(dateErrorWidget), Context.getMessageSourceService()
                                .getMessage("htmlformentry.error.required")));
                    }
                }
            }
        }

        if (reasonForExitWidget != null) {
            Object value = reasonForExitWidget.getValue(context, submission);
            exitReasonAnswerConcept = (Concept) HtmlFormEntryUtil.convertToType(value.toString().trim(), Concept.class);
            if (obsList != null && obsList.size() == 0) {
                if (exitReasonAnswerConcept == null) {
                    if (dateWidget != null && dateWidget.getValue(context, submission) != null) {
                        ret.add(new FormSubmissionError(context.getFieldName(reasonForExitErrorWidget), Context.getMessageSourceService()
                                .getMessage("htmlformentry.error.required")));
                    }
                } else if (exitReasonAnswerConcept != null && exitReasonAnswerConcept.getConceptId().equals(patientDiedConcept.getConceptId())) {
                    if (causeOfDeathWidget == null ||
                            HtmlFormEntryUtil.convertToType(causeOfDeathWidget.getValue(context, submission).toString().trim(), Concept.class) == null) {
                        ret.add(new FormSubmissionError(context.getFieldName(causeOfDeathErrorWidget), Context.getMessageSourceService()
                                .getMessage("htmlformentry.error.required")));
                    }
                } else if (exitReasonAnswerConcept != null && !exitReasonAnswerConcept.getConceptId().equals(patientDiedConcept.getConceptId())) {
                    if (causeOfDeathWidget != null &&
                            HtmlFormEntryUtil.convertToType(causeOfDeathWidget.getValue(context, submission).toString().trim(), Concept.class) != null) {
                        ret.add(new FormSubmissionError(context.getFieldName(causeOfDeathErrorWidget), Context.getMessageSourceService()
                                .getMessage("htmlformentry.error.cannotEnterAValue")));
                    }
                }
            }
        }

        if (causeOfDeathWidget != null) {
            Object value = causeOfDeathWidget.getValue(context, submission);
            causeOfDeathAnswerConcept = (Concept) HtmlFormEntryUtil.convertToType(value.toString().trim(), Concept.class);
            String valueString = otherReasonWidget.getValue(context, submission);

            if (obsDeath != null && obsDeath.size() == 0) {
                if (causeOfDeathAnswerConcept == null) {
                    if (otherReasonWidget != null && !valueString.equals("")) {
                        ret.add(new FormSubmissionError(context.getFieldName(otherReasonErrorWidget), Context.getMessageSourceService()
                                .getMessage("htmlformentry.error.cannotEnterAValue")));
                    }
                } else if (causeOfDeathAnswerConcept != null && causeOfDeathAnswerConcept.getConceptId().equals(otherNonCodedConcept.getConceptId())) {
                    if (otherReasonWidget == null || valueString.equals("")) {
                        ret.add(new FormSubmissionError(context.getFieldName(otherReasonErrorWidget), Context.getMessageSourceService()
                                .getMessage("htmlformentry.error.required")));
                    }
                } else if (causeOfDeathAnswerConcept != null && !causeOfDeathAnswerConcept.getConceptId().equals(otherNonCodedConcept.getConceptId())) {
                    if (otherReasonWidget != null && !valueString.equals("")) {
                        ret.add(new FormSubmissionError(context.getFieldName(otherReasonErrorWidget), Context.getMessageSourceService()
                                .getMessage("htmlformentry.error.cannotEnterAValue")));
                    }
                }
            }
        }

        // this validation is added to avoid user resetting the 'exit from care'
        if (obsList != null && obsList.size() == 1) {
            if (exitDate == null) {
                ret.add(new FormSubmissionError(context.getFieldName(dateErrorWidget), Context.getMessageSourceService()
                        .getMessage("htmlformentry.error.required")));
            } else if (exitReasonAnswerConcept == null) {
                ret.add(new FormSubmissionError(context.getFieldName(reasonForExitErrorWidget), Context.getMessageSourceService()
                        .getMessage("htmlformentry.error.required")));
            } else if (exitReasonAnswerConcept != null && exitReasonAnswerConcept.getConceptId().equals(patientDiedConcept.getConceptId())) {
                if (causeOfDeathWidget == null ||
                        HtmlFormEntryUtil.convertToType(causeOfDeathWidget.getValue(context, submission).toString().trim(), Concept.class) == null) {
                    ret.add(new FormSubmissionError(context.getFieldName(causeOfDeathErrorWidget), Context.getMessageSourceService()
                            .getMessage("htmlformentry.error.required")));
                }
            } else if (exitReasonAnswerConcept != null && !exitReasonAnswerConcept.getConceptId().equals(patientDiedConcept.getConceptId())) {
                if (causeOfDeathWidget != null &&
                        HtmlFormEntryUtil.convertToType(causeOfDeathWidget.getValue(context, submission).toString().trim(), Concept.class) != null) {
                    ret.add(new FormSubmissionError(context.getFieldName(causeOfDeathErrorWidget), Context.getMessageSourceService()
                            .getMessage("htmlformentry.error.cannotEnterAValue")));
                }
            }
        }

        // this validation is added to avoid user resetting the 'patient died' incident
        String valueString = otherReasonWidget.getValue(context, submission);

        if (obsDeath != null && obsDeath.size() == 1) {
            if (causeOfDeathAnswerConcept != null && causeOfDeathAnswerConcept.getConceptId().equals(otherNonCodedConcept.getConceptId())) {
                if (otherReasonWidget == null || valueString.equals("")) {
                    ret.add(new FormSubmissionError(context.getFieldName(otherReasonErrorWidget), Context.getMessageSourceService()
                            .getMessage("htmlformentry.error.required")));
                }
            } else if (causeOfDeathAnswerConcept != null && !causeOfDeathAnswerConcept.getConceptId().equals(otherNonCodedConcept.getConceptId())) {
                if (otherReasonWidget != null && !valueString.equals("")) {
                    ret.add(new FormSubmissionError(context.getFieldName(otherReasonErrorWidget), Context.getMessageSourceService()
                            .getMessage("htmlformentry.error.cannotEnterAValue")));
                }
            }
        }

        return ret;
    }

    @Override
    public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {

        Date date = null;
        Concept exitReasonConcept = null;
        Concept causeOfDeathConcept = null;
        String otherReason = null;

        if (dateWidget != null) {
            date = dateWidget.getValue(session.getContext(), submission);
        }
        if (reasonForExitWidget != null) {
            Object value = reasonForExitWidget.getValue(session.getContext(), submission);
            exitReasonConcept = (Concept) HtmlFormEntryUtil.convertToType(value.toString().trim(), Concept.class);
        }
        if (causeOfDeathWidget != null) {
            Object value = causeOfDeathWidget.getValue(session.getContext(), submission);
            causeOfDeathConcept = (Concept) HtmlFormEntryUtil.convertToType(value.toString().trim(), Concept.class);
        }
        if (otherReasonWidget != null) {
            otherReason = otherReasonWidget.getValue(session.getContext(), submission);
        }

        // only if user submits both date and reason we allow to exit from care, and this is done
        // to make sure a  user is able to submit a form with <exitfromcare> tag, without filling that
        // section, however if it is filled initially, user can't resubmit after changing the date and
        // reason fields to null back
        if (date != null && exitReasonConcept != null) {
            session.getSubmissionActions().exitFromCare(date, exitReasonConcept, causeOfDeathConcept, otherReason);
        }

    }

    @Override
    public String generateHtml(FormEntryContext context) {

        StringBuilder sb = new StringBuilder();

        Patient patient = context.getExistingPatient();

        String patientDiedConId = Context.getAdministrationService().getGlobalProperty("concept.patientDied");
        Concept patientDiedConcept = Context.getConceptService().getConcept(patientDiedConId);
        String otherNonCodedConId = Context.getAdministrationService().getGlobalProperty("concept.otherNonCoded");
        Concept otherNonCodedConcept = Context.getConceptService().getConcept(otherNonCodedConId);

        String reasonWidgetId = context.getFieldName(reasonForExitWidget);
        String causeWidgetId = context.getFieldName(causeOfDeathWidget);
        String otherTextWidgetId = context.getFieldName(otherReasonWidget);

        String reasonConId = Context.getAdministrationService().getGlobalProperty("concept.reasonExitedCare");
        Concept reasonExitConcept = Context.getConceptService().getConcept(reasonConId);
        List<Obs> obsList = Context.getObsService().getObservationsByPersonAndConcept(patient, reasonExitConcept);

        String causeOfDeathConId = Context.getAdministrationService().getGlobalProperty("concept.causeOfDeath");
        Concept causeOfDeathConcept = Context.getConceptService().getConcept(causeOfDeathConId);
        List<Obs> obsDeath = Context.getObsService().getObservationsByPersonAndConcept(patient, causeOfDeathConcept);


        if (reasonForExitWidget != null) {
            if (context.getMode() == FormEntryContext.Mode.VIEW) {
                if (obsList != null && obsList.size() == 1) {
                    sb.append(reasonForExitWidget.generateHtml(context));
                }
            } else {
                sb.append(reasonForExitWidget.generateHtml(context));
            }

            if (context.getMode() != FormEntryContext.Mode.VIEW) {
                sb.append(reasonForExitErrorWidget.generateHtml(context));
            }

            if (context.getMode() == FormEntryContext.Mode.EDIT) {
                sb.append("<script>");
                sb.append("jq(document).ready(function(){");
                sb.append("var rVal = jq(\"#" + reasonWidgetId + "\").val();");
                sb.append("if (rVal == \"" + patientDiedConId + "\")\n"
                        + " { jq(\"#" + causeWidgetId + "\").show();}\n"
                        + "if (rVal != \"" + patientDiedConId + "\")\n"
                        + " { jq(\"#" + causeWidgetId + "\").val(\"\"); jq(\"#" + causeWidgetId + "\").hide();"
                        + " jq(\"#" + otherTextWidgetId + "\").val(\"\"); jq(\"#" + otherTextWidgetId + "\").hide();}");

                sb.append("});");
                sb.append("</script>");
            }
        }

        // adding a jquery event handler to reasonForExitWidget
        sb.append("<script>");
        sb.append("jq(\"#" + reasonWidgetId + "\").change(function(){");
        sb.append("var reasonVal = jq(\"#" + reasonWidgetId + "\").val();");
        sb.append("if (reasonVal == \"" + patientDiedConId + "\")\n"
                + " { jq(\"#" + causeWidgetId + "\").show();}\n"
                + "if (reasonVal != \"" + patientDiedConId + "\")\n"
                + " { jq(\"#" + causeWidgetId + "\").val(\"\"); jq(\"#" + causeWidgetId + "\").hide();"
                + " jq(\"#" + otherTextWidgetId + "\").val(\"\"); jq(\"#" + otherTextWidgetId + "\").hide();}");
        sb.append("});");
        sb.append("</script>");

        // providing a blank space between the widgets
        sb.append("&nbsp;&nbsp;");

        if (dateWidget != null) {
            if (context.getMode() == FormEntryContext.Mode.VIEW) {
                if (obsList != null && obsList.size() == 1) {
                    sb.append(dateWidget.generateHtml(context));
                }
            } else {
                sb.append(dateWidget.generateHtml(context));
            }

            if (context.getMode() != FormEntryContext.Mode.VIEW)
                sb.append(dateErrorWidget.generateHtml(context));
        }

        sb.append("<br/>");
        sb.append("<br/>");

        if (causeOfDeathWidget != null) {

            if (context.getMode() == FormEntryContext.Mode.VIEW) {
                if (obsList != null && obsList.size() == 1) {
                    Obs obs = obsList.get(0);
                    if (obs.getValueCoded().getConceptId().equals(patientDiedConcept.getConceptId())) {
                        sb.append(causeOfDeathWidget.generateHtml(context));
                    }
                } else if (obsList != null && obsList.size() == 0) {
                    // this state may not be happen often, but in case that there is no exit from case
                    // observation, but however patient is dead, we are displaying cause of death then
                    if (obsDeath != null && obsDeath.size() == 1) {
                        sb.append(causeOfDeathWidget.generateHtml(context));
                    }
                }
            } else {
                sb.append(causeOfDeathWidget.generateHtml(context));
            }

            if (context.getMode() != FormEntryContext.Mode.VIEW) {
                sb.append(causeOfDeathErrorWidget.generateHtml(context));
            }

            // only show causeOfDeathWidget if there is an initial value, else hide it
            if (context.getMode() == FormEntryContext.Mode.ENTER) {
                sb.append("<script>");
                sb.append("jq(document).ready(function(){");
                sb.append(" if(jq(\"#" + causeWidgetId + "\").val() != \"\" && "
                        + "jq(\"#" + reasonWidgetId + "\").val() == \"" + patientDiedConId + "\"){\n");
                sb.append(" jq(\"#" + causeWidgetId + "\").show();}\n");
                sb.append(" if(jq(\"#" + causeWidgetId + "\").val() == \"\"){\n");
                sb.append(" jq(\"#" + causeWidgetId + "\").hide();}\n");
                sb.append("});");
                sb.append("</script>");
            }
            if (context.getMode() == FormEntryContext.Mode.EDIT) {
                sb.append("<script>");
                sb.append("jq(document).ready(function(){");
                sb.append("var cVal = jq(\"#" + causeWidgetId + "\").val();");
                sb.append("if (cVal == \"" + otherNonCodedConId + "\")\n"
                        + " { jq(\"#" + otherTextWidgetId + "\").show();}\n"
                        + "if (cVal != \"" + otherNonCodedConId + "\")\n"
                        + " { jq(\"#" + otherTextWidgetId + "\").val(\"\"); jq(\"#" + otherTextWidgetId + "\").hide();}");
                sb.append("});");
                sb.append("</script>");
            }
        }

        // adding a jquery event handler to causeOfDeathWidget
        sb.append("<script>");
        sb.append("jq(\"#" + causeWidgetId + "\").change(function(){");
        sb.append("var causeVal = jq(\"#" + causeWidgetId + "\").val();");
        sb.append("if (causeVal == \"" + otherNonCodedConId + "\")\n"
                + " { jq(\"#" + otherTextWidgetId + "\").show();}\n"
                + "if (causeVal != \"" + otherNonCodedConId + "\")\n"
                + " { jq(\"#" + otherTextWidgetId + "\").val(\"\"); jq(\"#" + otherTextWidgetId + "\").hide();}");
        sb.append("});");
        sb.append("</script>");

        // providing a blank space between the widgets
        sb.append("&nbsp;&nbsp;");

        if (otherReasonWidget != null) {
            if (context.getMode() == FormEntryContext.Mode.VIEW) {
                if (obsDeath != null && obsDeath.size() == 1) {
                    Obs obs = obsDeath.get(0);

                    if (obsList != null && obsList.size() == 0) {
                        if (obs.getValueCoded().getConceptId().equals(otherNonCodedConcept.getConceptId())) {
                            sb.append(otherReasonWidget.generateHtml(context));
                        }
                    } else if (obsList != null && obsList.size() == 1) {
                        Obs exitObs = obsList.get(0);
                        if (exitObs.getValueCoded().getConceptId().equals(patientDiedConcept.getConceptId())
                                && obs.getValueCoded().getConceptId().equals(otherNonCodedConcept.getConceptId())) {
                            sb.append(otherReasonWidget.generateHtml(context));
                        }
                    }
                }
            } else {
                sb.append(otherReasonWidget.generateHtml(context));
            }

            if (context.getMode() != FormEntryContext.Mode.VIEW) {
                sb.append(otherReasonErrorWidget.generateHtml(context));
            }

            // only show otherReasonWidget if there is an initial value, else hide it
            if (context.getMode() == FormEntryContext.Mode.ENTER) {
                sb.append("<script>");
                sb.append(" if(jq(\"#" + otherTextWidgetId + "\").val() != \"\" &&"
                        + " jq(\"#" + reasonWidgetId + "\").val() == \"" + patientDiedConId + "\"){\n");
                sb.append(" jq(\"#" + otherTextWidgetId + "\").show();}\n");
                sb.append(" if(jq(\"#" + otherTextWidgetId + "\").val() == \"\"){\n");
                sb.append(" jq(\"#" + otherTextWidgetId + "\").hide();}\n");
                sb.append("</script>");
            }
        }

        return sb.toString();
    }
}

