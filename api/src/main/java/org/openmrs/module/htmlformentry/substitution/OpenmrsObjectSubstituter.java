package org.openmrs.module.htmlformentry.substitution;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OpenmrsObject;
import org.openmrs.Role;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;


/**
 * Performs substitution of references to OpenmrsObjects.  Tests if the passed value is a reference to an OpenmrsObject. 
 * If so, checks to see if that OpenmrsObject is in the substitutionMap key set.  If so, returns a reference to the 
 * OpenmrsObject associated with that key in the map; otherwise, just returns the passed value.
 */
public class OpenmrsObjectSubstituter implements Substituter {
	
	@Override
    public String substitute(String value, Class<?> clazz, Map<OpenmrsObject, OpenmrsObject> substitutionMap) {
		
		// see if this value is a uuid referenced in the substitutionMap
		if (HtmlFormEntryUtil.isValidUuidFormat(value)) {
			String replacementValue = substituteUuid(value, substitutionMap);
			
			// if we've found a substitution, we are done
			if (!StringUtils.equals(value, replacementValue)) {
				return replacementValue;
			}
		}
		
		// see if this value is a name using the same method
		if (OpenmrsMetadata.class.isAssignableFrom(clazz)) {
			String replacementValue =  substituteName(value, clazz, substitutionMap);
			
			// if we've found a substitution, we are done
			if (!StringUtils.equals(value, replacementValue)) {
				return replacementValue;
			}
		}
		// handle the special cases of a role, which is references by the getRole instead of getName
		if (Role.class.equals(clazz)) {
			String replacementValue = substituteRoleName(value, substitutionMap);
			
			// if we've found a substitution, we are done
			if (!StringUtils.equals(value, replacementValue)) {
				return replacementValue;
			}
		}
		
		// otherwise, no substitution to perform, return the existing value
		return value;
		
	}
	
	/**
	 * Test to see if the uuid references an Object in the substitutionMap key, and then substitute
	 * out as necessary
	 */
	private String substituteUuid(String uuid, Map<OpenmrsObject, OpenmrsObject> substitutionMap) {
		for (OpenmrsObject incoming : substitutionMap.keySet()) {
			if (StringUtils.equals(incoming.getUuid(), uuid)) {
				String replacementUuid = substitutionMap.get(incoming).getUuid();
				if (HtmlFormEntryUtil.isValidUuidFormat(replacementUuid)) {
					return replacementUuid;
				} else {
					throw new IllegalArgumentException("UUID has an supported format " + "[" + replacementUuid + "]");
				}
			}
		}
		
		// if we didn't find a match, just return the existing value
		return uuid;
	}
	
	/**
	 * Test to see if the name references an Object in the substitutionMap key, and then substitute
	 * out as necessary
	 */
	private String substituteName(String name, Class<?> clazz, Map<OpenmrsObject, OpenmrsObject> substitutionMap) {
		for (OpenmrsObject incoming : substitutionMap.keySet()) {
			if (incoming.getClass().equals(clazz) && incoming instanceof OpenmrsMetadata
			        && StringUtils.equals(((OpenmrsMetadata) incoming).getName(), name)) {
				return ((OpenmrsMetadata) substitutionMap.get(incoming)).getName();
			}
		}
		
		// if we didn't find a match, just return the existing value
		return name;
	}
	
	/**
	 * Test to see if the role references a Role in the substitutionMap key, and then substitute out
	 * as necessary
	 */
	private String substituteRoleName(String role, Map<OpenmrsObject, OpenmrsObject> substitutionMap) {
		for (OpenmrsObject incoming : substitutionMap.keySet()) {
			if (incoming.getClass().equals(Role.class) 
			        && StringUtils.equals(((Role) incoming).getRole(), role)) {
				return ((Role) substitutionMap.get(incoming)).getRole();
			}
		}
		
		// if we didn't find a match, just return the existing value
		return role;
	}
}
