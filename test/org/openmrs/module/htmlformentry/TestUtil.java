package org.openmrs.module.htmlformentry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.RegressionTestHelper.ObsValue;
import org.openmrs.util.Format;


public class TestUtil {

	public String loadXmlFromFile(String filename) throws Exception {
		InputStream fileInInputStreamFormat = null;
		
		// try to load the file if its a straight up path to the file or
		// if its a classpath path to the file
		if (new File(filename).exists()) {
			fileInInputStreamFormat = new FileInputStream(filename);
		} else {
			fileInInputStreamFormat = getClass().getClassLoader().getResourceAsStream(filename);
			if (fileInInputStreamFormat == null)
				throw new FileNotFoundException("Unable to find '" + filename + "' in the classpath");
		}
		StringBuilder sb = new StringBuilder();
		BufferedReader r = new BufferedReader(new InputStreamReader(fileInInputStreamFormat, Charset.forName("UTF-8")));
		while (true) {
			String line = r.readLine();
			if (line == null)
				break;
			sb.append(line).append("\n");
		}
		return sb.toString();
	}
	
	/**
	 * Tests whether the child obs of this group exactly match 'expected'
	 * 
	 * @param group
	 * @param expected
	 * @return
	 */
	public static boolean isMatchingObsGroup(Obs group, List<ObsValue> expected) {
		if (!group.isObsGrouping())
			return false;
		
		Set<Obs> children = group.getGroupMembers();
		if (children.size() != expected.size())
			return false;
		
		boolean[] alreadyUsed = new boolean[expected.size()];
		for (Obs child : children) {
			boolean foundMatch = false;
			for (int i = 0; i < expected.size(); ++i) {
				if (alreadyUsed[i])
					continue;
				if (expected.get(i).matches(child)) {
					foundMatch = true;
					alreadyUsed[i] = true;
					break;
				}
			}
			if (!foundMatch)
				return false;
		}
		return true;
	}
	
	public static String valueAsStringHelper(Object value) {
		if (value == null)
			return null;
		if (value instanceof Concept)
			return ((Concept) value).getName(Context.getLocale()).getName();
		else if (value instanceof Date)
			return Format.format((Date) value);
		else if (value instanceof Number)
			return "" + ((Number) value).doubleValue();
		else
			return value.toString();
	}
	
	/**
	 * Ignores white space. Ignores capitalization. Strips <span class="value">...</span>. Removes
	 * <span class="emptyValue">___</span>. Strips <htmlform>...</htmlform>.
	 */
	public static void assertFuzzyEquals(String expected, String actual) {
		if (expected == null && actual == null)
			return;
		if (expected == null || actual == null)
			Assert.fail(expected + " does not match " + actual);
		String test1 = stripTagsAndWhitespace(expected);
		String test2 = stripTagsAndWhitespace(actual);
		if (!test1.equals(test2)) {
			Assert.fail(expected + " does not match " + actual);
			//Assert.fail(test1 + " VERSUS " + test2);
		}
	}
	
	/**
	 * Tests whether the substring is contained in the actual string. Allows for inclusion of
	 * regular expressions in the substring. Ignores white space. Ignores capitalization. Strips
	 * <span class="value">...</span>. Removes <span class="emptyValue">___</span>. Strips
	 * <htmlform>...</htmlform>.
	 */
	public static void assertFuzzyContains(String substring, String actual) {
		if (substring == null) {
			return;
		}
		if (actual == null) {
			Assert.fail(substring + " is not contained in " + actual);
		}
		
		if (!Pattern.compile(stripTagsAndWhitespace(substring)).matcher(stripTagsAndWhitespace(actual)).find()) {
			Assert.fail(substring + " is not contained in " + actual);
		}
	}
	
	
	private static String stripTagsAndWhitespace(String string) {
		string = string.toLowerCase();
		string = string.replaceAll("<span class=\"value\">(.*)</span>", "$1");
		string = string.replaceAll("<span class=\"emptyvalue\">.*</span>", "");
		string = string.replaceAll("\\s", "");
		string = string.replaceAll("<htmlform>(.*)</htmlform>", "$1");
		return string;
	}
	
	/**
	 * 
	 * Adds an Obs to the specified encounter.  
	 * 
	 * @param encounter the encounter to add the obs to
	 * @param conceptId the concept id associated with the encounter
	 * @param value the value of the obs (can be numeric, text, datetime, or coded)
	 * @param date the date created of the obs
	 * @return
	 */
	public static Obs addObs(Encounter encounter, Integer conceptId, Object value, Date date) {
		Person person = encounter.getPatient();
		Concept concept = Context.getConceptService().getConcept(conceptId);
		Location location = encounter.getLocation();
		Obs obs = new Obs(person, concept, date, location);
		if (value != null) {
			if (value instanceof Number)
				obs.setValueNumeric(((Number) value).doubleValue());
			else if (value instanceof String)
				obs.setValueText((String) value);
			else if (value instanceof Date)
				obs.setValueDatetime((Date) value);
			else if (value instanceof Concept)
				obs.setValueCoded((Concept) value);
		}
		obs.setDateCreated(new Date());
		encounter.addObs(obs);
		
		return obs;
	}
	
	/**
	 * Creates an obsgroup and adds it to the specified encounter. The obsDetails arguements should be triplets of conceptId, concept
	 * value, and date created.
	 * 
	 * @param encounter
	 * @param groupingConceptId
	 * @param date
	 * @param obsDetails
	 * @return
	 */
	public static Obs addObsGroup(Encounter encounter, Integer groupingConceptId, Date date, Object... obsDetails) {
		Obs obsgroup = addObs(encounter, groupingConceptId, null, date);
		
		int i = 0;
		while (i < obsDetails.length) {
			obsgroup.addGroupMember(addObs(encounter, (Integer) obsDetails[i], obsDetails[i + 1], (Date) obsDetails[i + 2]));
			
			// skip to the next triple to the next triplet
			i = i + 3;
		}
		
		return obsgroup;
	}
	
}
