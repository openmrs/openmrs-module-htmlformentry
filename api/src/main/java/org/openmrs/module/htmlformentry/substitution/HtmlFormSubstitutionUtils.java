package org.openmrs.module.htmlformentry.substitution;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.handler.AttributeDescriptor;
import org.openmrs.module.htmlformentry.handler.TagHandler;

public class HtmlFormSubstitutionUtils {
	
	/**
	 * Replaces all the ids in a form with uuids. This method operates using the AttributeDescriptors
	 * property of TagHandlers; if you have have a new attribute that you want to configure for
	 * id-to-uuid substitution, add a descriptor for that attribute to the appropriate tag handler
	 * and set the class property of that descriptor to the appropriate class. Can currently handle
	 * ids for the following classes: Concept, Program, Person, PatientIdentifierType, Location, and
	 * Drug
	 */
	public static void replaceIdsWithUuids(HtmlForm form) {
		performSubstitution(form, new IdToUuidSubstituter(), null);
	}
	
	/**
	 * Replaces all the program name in a form with uuids. This method operates using the AttributeDescriptors
	 * property of TagHandlers; if you have have a new attribute that you want to configure for
	 * program-name-to-uuid substitution, add a descriptor for that attribute to the appropriate tag handler. 
	 */
	public static void replaceProgramNamesWithUuids(HtmlForm form) {
		performSubstitution(form, new ProgramNameToUuidSubstituter(), null);
	}
	
	/**
	 * Given an map of OpenmrsObjects to OpenmrsObjects, searches through the form for any references to 
	 * OpenmrsObjects in the key set, and replaces those references to the references to the corresponding
	 * OpenmrsObject in the value set. Used to handle any metadata replacement that may need to take place
	 * when sharing a form via Metadata Sharing
	 * @param form
	 * @param substitutionMap
	 */
	public static void replaceIncomingOpenmrsObjectsWithExistingOpenmrsObjects(HtmlForm form, Map<OpenmrsObject, OpenmrsObject> substitutionMap) {
		performSubstitution(form, new OpenmrsObjectSubstituter(), substitutionMap);
	}
	
	
	/**
	 * Reads through the content of a form and finds all values listed in any attributes defined in AttributeDescriptors
	 * Then used the passed Substituter to determine which values need to be substituted.
	 */
	private static void performSubstitution(HtmlForm form, Substituter substituter,  Map<OpenmrsObject, OpenmrsObject> substitutionMap) {
		
		// if the form is has no content, nothing to do
		if(StringUtils.isEmpty(form.getXmlData())) {
			return;
		}
		
		// get the tag handlers so we can gain access to the attribute descriptors
		Map<String, TagHandler> tagHandlers = Context.getService(HtmlFormEntryService.class).getHandlers();
		
		// loop through all the attribute descriptors for all the tags that have been registered
		for (String tagName : tagHandlers.keySet()) {
			HtmlFormEntryUtil.log.debug("Handling substitutions for tag " + tagName);
			
			if (tagHandlers.get(tagName).getAttributeDescriptors() != null) {
				for (AttributeDescriptor attributeDescriptor : tagHandlers.get(tagName).getAttributeDescriptors()) {
					// we only need to deal with descriptors that have an associated class
					if (attributeDescriptor.getClazz() != null) {
						// build the attribute string we are searching for
						// pattern matches <tagName .* attribute
						// to break down the regex in detail, ?: simply means that we don't want include this grouping in the groups that we backreference
						// the grouping itself is an "or", that matches either "\\s" (a single whitespace character) or
						// "\\s[^>]*\\s" (a single whitespace character plus 0 to n characters of any type but a >, followed by another single whitespace character)
						String pattern = "<" + tagName + "(?:\\s|\\s[^>]*\\s)" + attributeDescriptor.getName();
						HtmlFormEntryUtil.log.debug("substitution pattern: " + pattern);
						form.setXmlData(HtmlFormSubstitutionUtils.performSubstitutionHelper(form.getXmlData(), pattern,
						    attributeDescriptor.getClazz(), substituter, substitutionMap, true));
					}
				}
			}
		}
	}
	
	/**
	 * Helper method used by performSubstitution
	 */
	private static String performSubstitutionHelper(String formXmlData, String tagAndAttribute,
	                                                Class<?> clazz, Substituter substituter,
	                                                Map<OpenmrsObject, OpenmrsObject> substitutionMap, Boolean includeQuotes) {
		Pattern substitutionPattern;
		
		if (includeQuotes) {
			// pattern to find the specified attribute and pull out its values; regex matches any characters within quotes after an equals, i.e. ="a2-32" would match a232
			// we use () to break the match into three groups: 1) the characters up to the including the first quotes; 2) the characters in the quotes; and 3) then the trailing quote
			// (put a space before the attribute name so we don't get border= instead of order=)
			substitutionPattern = Pattern.compile("(" + tagAndAttribute + "=\")(.*?)(\")", Pattern.CASE_INSENSITIVE);
		} else {
			// the same pattern as above, but without the quotes (to handle the macro assignments),
			// and with a blank space at the end (which we need to account for when we do the replace)	
			substitutionPattern = Pattern.compile("(" + tagAndAttribute + "=)(.*?)(\\s)", Pattern.CASE_INSENSITIVE);
		}
		
		Matcher matcher = substitutionPattern.matcher(formXmlData);
		
		// lists to keep track of any "repeat" keys and macros we are going to have to substitute out as well
		Set<String> repeatKeysToReplace = new HashSet<String>();
		Set<String> macrosToReplace = new HashSet<String>();
		
		StringBuffer buffer = new StringBuffer();
		
		while (matcher.find()) {
			// split the group into the various ids
			String[] ids = matcher.group(2).split(",");
			
			StringBuffer idBuffer = new StringBuffer();
			// now loop through each id
			for (String id : ids) {
				
				// make the id substitution by calling the substituter's substitute method
				idBuffer.append(substituter.substitute(id, clazz, substitutionMap) + ",");
				
				// if this id is a repeat key (i.e., something in curly braces) we need to keep track of it so that we can perform key substitutions
				// pattern matches one or more characters of any type within curly braces
				Matcher repeatKeyMatcher = Pattern.compile("\\{(.+)\\}").matcher(id);
				if (repeatKeyMatcher.find()) {
					repeatKeysToReplace.add(repeatKeyMatcher.group(1));
				}
				
				// if this id is a macro reference (i.e, something that starts with a $) we need to keep track of it so that we can perform macro substitution
				Matcher macroMatcher = Pattern.compile("\\$(.+)").matcher(id);
				if (macroMatcher.find()) {
					macrosToReplace.add(macroMatcher.group(1));
				}
			}
			
			// trim off the trailing comma
			idBuffer.deleteCharAt(idBuffer.length() - 1);
			
			// now do the replacement
			
			// create the replacement string from the matched sequence, substituting out group(2) with the updated ids
			String replacementString = matcher.group(1) + idBuffer.toString() + matcher.group(3);
			
			// we need to escape any $ characters in the buffer or we run into errors with the appendReplacement method since 
			// the $ has a special meaning to that method
			replacementString = replacementString.replace("$", "\\$");
			
			// now append the replacement string to the buffer
			matcher.appendReplacement(buffer, replacementString);
		}
		
		// append the rest of htmlform
		matcher.appendTail(buffer);
		
		formXmlData = buffer.toString();
		
		// now handle any repeat keys we have discovered during this substitution
		for (String key : repeatKeysToReplace) {
			formXmlData = performSubstitutionHelper(formXmlData, key, clazz, substituter, substitutionMap, true);
		}
		
		// and now handle any macros we have discovered during this substitution
		for (String key : macrosToReplace) {
			formXmlData = performSubstitutionHelper(formXmlData, key, clazz, substituter, substitutionMap, false);
		}
		
		return formXmlData;
	}
}
