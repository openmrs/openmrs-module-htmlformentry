package org.openmrs.module.htmlformentry.substitution;

import java.util.Map;

import org.openmrs.OpenmrsObject;

/**
 * Defines a interface that is used by the HtmlFormSubstituionUtils.performSubstitutions
 * to perform different types of substitutions
 */
public interface Substituter {

	public String substitute(String value, Class<?> clazz, Map<OpenmrsObject,OpenmrsObject> substitutionMap);
	
}
