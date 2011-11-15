package org.openmrs.module.htmlformentry.substitution;

import java.util.Map;

import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;


/**
 * Performs Id to Uuid substitutions. Tests if the passed value is an id reference to an OpenmrsObject. If so,
 * returns the uuid of that object; otherwise, just returns the passed value.
 */
public class IdToUuidSubstituter implements Substituter {

	@Override
    @SuppressWarnings("unchecked")
    public String substitute(String value, Class<?> clazz, Map<OpenmrsObject, OpenmrsObject> substitutionMap) {
	   
		// if this is not a reference to an OpenMRS object, no substitution to perform
		if (!OpenmrsObject.class.isAssignableFrom(clazz)) {
			return value;
		}
    
		OpenmrsObject object = null;
    	
    	// if this appears to be an id, try to find the OpenmrsObject referenced by this id
    	if (value.matches("^\\d+$") && !HtmlFormEntryUtil.isValidUuidFormat(value)) {
    		object = Context.getService(HtmlFormEntryService.class).getItemById((Class<? extends OpenmrsObject>) clazz, Integer.valueOf(value));
    	} 
    	
    	// if we have found an object, return the uuid and use it for substitution, otherwise don't do any substitution 
    	if (object != null) {
    		return object.getUuid();
    	}
    	else {
    		return value;
    	}
		
		
	}

}
