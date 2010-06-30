package org.openmrs.module.htmlformentry.handler;

import java.util.Map;

import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.element.ObsSubmissionElement;

/**
 * Handles the {@code <obs>} tag
 */
public class ObsTagHandler extends SubstitutionTagHandler {

    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions, Map<String, String> parameters) {
        ObsSubmissionElement element = new ObsSubmissionElement(session.getContext(), parameters);
        /*if it =0, then we are just generating the templates, no actions should be taken*/
        if(session.getContext().getNewrepeatTimesSeqVal()!=0){
        	session.getSubmissionController().addAction(element);
        }
        
        /*we are in multiple, not  generating the templates, but in edit/enter sumbit */
    	if(session.getContext().getRequest()!=null 
    			&& session.getContext().getRequest().getParameter("kCount1")!=null
    			&& session.getContext().getActiveRptGroup()!=null){
    		Object value = element.getValueWidget().getValue(session.getContext(), session.getContext().getRequest());
    		
    		/*we mark this multiple as not null */
    		if (value != null && !"".equals(value)) 
    			session.getContext().getActiveRptGroup().getIsallobsnulllist().set(session.getContext().getNewrepeatTimesSeqVal()-1, false);
    	}
		
        return element.generateHtml(session.getContext());
    }

}
