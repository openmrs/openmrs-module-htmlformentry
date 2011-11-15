package org.openmrs.module.htmlformentry.substitution;

import java.util.Map;

import org.openmrs.OpenmrsObject;
import org.openmrs.Program;
import org.openmrs.api.context.Context;

/**
 * Performs Program Name to Uuid substitutions. Tests if the passed value is a name reference to a Program. If so,
 * returns the uuid of that object; otherwise, just returns the passed value.
 */
public class ProgramNameToUuidSubstituter implements Substituter {

    @Override
    public String substitute(String value, Class<?> clazz, Map<OpenmrsObject, OpenmrsObject> substitutionMap) {
		// if this is not a reference to an program, no substitution to perform
		if (!Program.class.isAssignableFrom(clazz)) {
			return value;
		}
		
		// see if this is a name reference to a program
		Program program = Context.getProgramWorkflowService().getProgramByName(value);
		
		// if we've found a match, return the uuid, otherwise do nothing
		if (program != null) {
			return program.getUuid();
		}
		else {
			return value;
		}
    }
	
}
