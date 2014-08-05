package org.openmrs.module.htmlformentry.handler;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.CustomFormSubmissionAction;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Map;

/**
 * Usage example: <postSubmissionAction class="org.openmrs.module.xyz.DecideWhereToRedirect"/>
 */
public class PostSubmissionActionTagHandler extends SubstitutionTagHandler implements FormSubmissionControllerAction {

    private String className;

    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions, Map<String, String> parameters) throws BadFormDesignException {
        className = parameters.get("class");
        if (StringUtils.isEmpty(className)) {
            throw new BadFormDesignException("<postSubmissionAction/> tag requires a 'class' attribute");
        }
        session.getSubmissionController().addAction(this);
        return "";
    }

    @Override
    public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
        // this can never fail validation
        return null;
    }

    @Override
    public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
        try {
            Class<?> actionClass = Context.loadClass(className);
            CustomFormSubmissionAction actionInstance = (CustomFormSubmissionAction) actionClass.newInstance();
            session.getSubmissionActions().addCustomFormSubmissionAction(actionInstance);
        } catch (Exception ex) {
            throw new IllegalStateException("Error loading/instantiating post submission action class " + className, ex);
        }
    }

}
