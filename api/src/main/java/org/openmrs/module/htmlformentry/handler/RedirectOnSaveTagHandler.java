package org.openmrs.module.htmlformentry.handler;

import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.CustomFormSubmissionAction;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.w3c.dom.Node;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.util.Collection;

/**
 * Supports two usages:
 *
 * <redirectOnSave url="/modules/custom/displayForPrinting.form?patientId={{patient.id}}&encounterId={{encounter.id}}"/>
 *
 * <redirectOnSave script="groovy">
 *     import org.openmrs.api.context.Context;
 *     def customView = Context.getAdministrationService().getGlobalProperty("custom.view");
 *     return "custom.form?view=" + customView + "&amp;patientId={{patient.id}}";
 * </redirectOnSave>
 */
public class RedirectOnSaveTagHandler extends AbstractTagHandler implements FormSubmissionControllerAction, CustomFormSubmissionAction {

    private String urlTemplate;

    private String scriptType;
    private String script;

    @Override
    public boolean doStartTag(FormEntrySession session, PrintWriter out, Node parent, Node node) throws BadFormDesignException {
        urlTemplate = getAttribute(node, "url", null);
        if (urlTemplate == null) {
            scriptType = getAttribute(node, "script", null);
            script = node.getTextContent();
        }
        session.getSubmissionController().addAction(this);

        if (urlTemplate == null && scriptType == null) {
            throw new BadFormDesignException("<redirectOnSave> tag must specify url or script attribute");
        }
        return false; // skip children
    }

    @Override
    public void doEndTag(FormEntrySession session, PrintWriter out, Node parent, Node node) throws BadFormDesignException {
        // do nothing
    }

    @Override
    public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
        // this cannot fail validation
        return null;
    }

    @Override
    public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
        session.getSubmissionActions().addCustomFormSubmissionAction(this);
    }

    @Override
    public void applyAction(FormEntrySession session) {
        if (urlTemplate != null) {
            session.setAfterSaveUrlTemplate(urlTemplate);
        }
        else if (scriptType != null) {
            ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
            ScriptEngine scriptEngine = scriptEngineManager.getEngineByName(scriptType);
            scriptEngine.put("patient", session.getPatient());
            scriptEngine.put("encounter", session.getSubmissionActions().getCurrentEncounter());
            scriptEngine.put("formEntrySession", session);
            try {
                Object result = scriptEngine.eval(script);
                session.setAfterSaveUrlTemplate(result.toString());
            } catch (ScriptException ex) {
                throw new IllegalStateException("Exception while evaluating " + scriptType + " for <redirectOnSave>", ex);
            }
        }
    }

}
