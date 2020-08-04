package org.openmrs.module.htmlformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.PatientProgramAttribute;
import org.openmrs.Program;
import org.openmrs.api.context.Context;

public class HtmlFormEntryUtil2_2 extends HtmlFormEntryUtil {
	
	public static Log log = LogFactory.getLog(HtmlFormEntryUtil2_2.class);
	
	public static PatientProgramAttribute getProgramAttribute(String identifier) {
		PatientProgramAttribute pa = null;
		if (identifier != null) {
			try {
				identifier = identifier.trim();
				
				Integer.valueOf(identifier);
				pa = getProgramAttribute(identifier);
				
				if (pa != null) {
					return pa;
				}
			}
			catch (NumberFormatException e) {}
			
			if (isValidUuidFormat(identifier)) {
				pa = Context.getProgramWorkflowService().getPatientProgramAttributeByUuid(identifier);
				
				if (pa != null) {
					return pa;
				}
			}
		}
		return null;
		
	}
	
	/**
	 * Looks up a {@link PatientProgramAttribute} from the specified program by patientprogramId,uuid
	 * 
	 * @param identifier the programWorkflowStateId, uuid or the concept name to match against
	 * @param program
	 * @return <strong>Should</strong> return the patient program attribute with the matching id
	 *         <strong>Should</strong> return the state with the matching uuid <strong>Should</strong>
	 */
	public static PatientProgramAttribute getPatientProgramAttribute(String identifier, Program program) {
		
		if (identifier == null) {
			return null;
		}
		
		// try to fetch by id or uuid
		PatientProgramAttribute progrmAttribute = getProgramAttribute(identifier);
		
		if (progrmAttribute != null) {
			return progrmAttribute;
		}
		return null;
	}
	
}
