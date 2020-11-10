package org.openmrs.module.htmlformentry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.OpenmrsObject;
import org.openmrs.api.context.Context;
import org.openmrs.util.Format;

public class TestUtil {
	
	private static Log log = LogFactory.getLog(TestUtil.class);
	
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
	public static boolean isMatchingObsGroup(Obs group, List<TestObsValue> expected) {
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
		else if (value instanceof Drug) {
			return "" + ((Drug) value).getFullName(Context.getLocale());
		} else
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
		}
	}
	
	/**
	 * Tests whether the substring is contained in the actual string. Allows for inclusion of regular
	 * expressions in the substring. Ignores white space. Ignores capitalization. Strips
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
	
	/**
	 * Tests whether the substring is contained in the actual string. Allows for inclusion of regular
	 * expressions in the substring.
	 */
	public static void assertContains(String substring, String actual) {
		if (substring == null) {
			return;
		}
		if (actual == null) {
			Assert.fail(substring + " is not contained in " + actual);
		}
		
		if (!Pattern.compile(substring).matcher(actual).find()) {
			Assert.fail(substring + " is not contained in " + actual);
		}
	}
	
	/**
	 * Tests whether the substring is NOT contained in the actual string. Allows for inclusion of
	 * regular expressions in the substring. Ignores white space. Ignores capitalization. Strips
	 * <span class="value">...</span>. Removes <span class="emptyValue">___</span>. Strips
	 * <htmlform>...</htmlform>.
	 */
	public static void assertFuzzyDoesNotContain(String substring, String actual) {
		if (substring == null) {
			return;
		}
		if (actual == null) {
			return;
		}
		
		if (Pattern.compile(stripTagsAndWhitespace(substring)).matcher(stripTagsAndWhitespace(actual)).find()) {
			Assert.fail(substring + " found in  " + actual);
		}
	}
	
	public static void assertIdEquals(OpenmrsObject object, Integer expectedId) {
		if (expectedId == null) {
			Assert.assertNull(object);
		} else {
			Assert.assertNotNull(object);
			Assert.assertEquals(expectedId, object.getId());
		}
	}
	
	public static void assertFalseOrNull(Boolean booleanObj) {
		Assert.assertTrue(booleanObj == null || !booleanObj);
	}
	
	public static void assertDate(Date date, String expectedYmd) {
		assertDate(date, "yyyy-MM-dd", expectedYmd);
	}
	
	public static void assertDate(Date date, String format, String expectedValue) {
		if (expectedValue == null) {
			Assert.assertNull(date);
		} else {
			Assert.assertNotNull(date);
			Assert.assertEquals(expectedValue, new SimpleDateFormat(format).format(date));
		}
	}
	
	private static String stripTagsAndWhitespace(String string) {
		string = string.toLowerCase();
		string = string.replaceAll("<span class=\"value\">(.*)</span>", "$1");
		string = string.replaceAll("<span class=\"emptyvalue\">(.*)</span>", "$1");
		string = string.replaceAll("(?s)<div class=\"htmlform\">(.*)</div>", "$1");
		string = string.replaceAll("\\s", "");
		string = string.replaceAll("____", ""); //represents empty value in view mode
		return string;
	}
	
	/**
	 * Adds an Obs to the specified encounter.
	 * 
	 * @param encounter the encounter to add the obs to
	 * @param conceptId the concept id associated with the encounter
	 * @param value the value of the obs (can be numeric, text, datetime, or coded)
	 * @param date the date created of the obs
	 * @return
	 */
	public static Obs addObs(Encounter encounter, Integer conceptId, Object value, Date date) {
		Obs obs = createObs(encounter, conceptId, value, date);
		encounter.addObs(obs);
		return obs;
	}
	
	/**
	 * Creates an obsgroup and adds it to the specified encounter. The obsDetails arguements should be
	 * triplets of conceptId, concept value, and date created.
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
	
	/**
	 * Utility to give us a ready-to-save (without violating foreign-key constraints) Obs
	 * 
	 * @param encounter
	 * @param conceptId
	 * @param value
	 * @param date
	 * @return
	 */
	public static Obs createObs(Encounter encounter, Integer conceptId, Object value, Date date) {
		Obs obs = new Obs(encounter.getPatient(), Context.getConceptService().getConcept(conceptId), date,
		        encounter.getLocation());
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
		return obs;
	}
	
	/**
	 * Compares two dates, ignoring their time components
	 */
	public static Boolean dateEquals(Date date1, Date date2) {
		return clearTimeComponent(date1).equals(clearTimeComponent(date2));
	}
	
	public static String formatYmd(Date date) {
		return (date == null ? null : new SimpleDateFormat("yyyy-MM-dd").format(date));
	}
	
	public static Date parseYmd(String ymd) {
		if (StringUtils.isNotBlank(ymd)) {
			try {
				return new SimpleDateFormat("yyyy-MM-dd").parse(ymd);
			}
			catch (Exception e) {
				throw new IllegalArgumentException("Unable to parse from yyyy-MM-dd to Date: " + ymd, e);
			}
		}
		return null;
	}
	
	/**
	 * Given a Date object, returns a Date object for the same date but with the time component (hours,
	 * minutes, seconds & milliseconds) removed
	 */
	public static Date clearTimeComponent(Date date) {
		// Get Calendar object set to the date and time of the given Date object  
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		
		// Set time fields to zero  
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		return cal.getTime();
	}
	
	public static Map<String, String> getFormValues(String html) {
		Map<String, String> ret = new LinkedHashMap<>();
		
		// used for input and for select option
		Pattern forValue = Pattern.compile("value=\"(.*?)\"");
		
		// <input ... name="something" ...>
		{
			Pattern forInput = Pattern.compile("<input.*?name=\"(.*?)\".*?>");
			Matcher matcher = forInput.matcher(html);
			while (matcher.find()) {
				String element = matcher.group();
				if (element.contains("type=\"radio\"") && !element.contains("checked=\"true\"")) {
					// discards unchecked radios
					continue;
				}
				if (element.contains("type=\"checkbox\"") && !element.contains("checked=\"true\"")) {
					// discards unchecked checkboxes
					continue;
				}
				String name = matcher.group(1);
				Matcher lookForValue = forValue.matcher(element);
				if (lookForValue.find()) {
					String value = lookForValue.group(1);
					ret.put(name, value);
				}
			}
		}
		
		// <textarea ... name="something" ...>value</textarea>
		{
			Pattern forTextarea = Pattern.compile("<textarea.*?name=\"(.*?)\".*?>(.*?)</textarea>");
			Matcher matcher = forTextarea.matcher(html);
			while (matcher.find()) {
				String name = matcher.group(1);
				String value = matcher.group(2);
				ret.put(name, value);
			}
		}
		
		// <select ... name="something" ...>(options)</select> (DOTALL makes . match line terminator too)
		{
			String selectPattern = "<select.*?name=\"(.*?)\".*?>.*?(<option[^>]*selected[^>]*>).*?</select>";
			Pattern forSelect = Pattern.compile(selectPattern, Pattern.DOTALL);
			Matcher matcher = forSelect.matcher(html);
			while (matcher.find()) {
				String name = matcher.group(1);
				String selectedOption = matcher.group(2);
				Matcher lookForValue = forValue.matcher(selectedOption);
				if (lookForValue.find()) {
					String value = lookForValue.group(1);
					ret.put(name, value);
				} else {
					ret.put(name, "");
				}
			}
		}
		
		// setupDatePicker(jsDateFormat, jsLocale, displaySelector, '#something', '2012-01-30')
		{
			Pattern forDatePicker = Pattern.compile("setupDatePicker\\(.*?, .*?, .*?, '#(.+?)', '(.+?)'\\)");
			Matcher matcher = forDatePicker.matcher(html);
			while (matcher.find()) {
				String name = matcher.group(1);
				String value = matcher.group(2);
				ret.put(name, value);
			}
		}
		
		return ret;
	}
	
	/**
	 * Finds the name of the first widget found after the given label. I.e. the first name="w#". If
	 * widgetsToSkip is > 0, then it will skip over that number of widgets after the first instance of
	 * the label is found If labelsToSkip is > 0, then it will skip over that number of labels found
	 * before matching
	 */
	public static String getFormFieldName(String html, String label, int widgetsToSkip, int labelsToSkip) {
		String val = null;
		
		if (labelsToSkip > 0) {
			int startIndex = 0;
			for (int i = 0; i < labelsToSkip; i++) {
				startIndex = html.indexOf(label, startIndex) + label.length();
				html = html.substring(startIndex);
			}
		}
		
		int index = html.indexOf(label);
		if (index >= 0) {
			for (int i = 0; i < widgetsToSkip + 1; ++i) {
				index = html.indexOf("name=\"w", index);
				index = html.indexOf('"', index) + 1;
			}
			val = html.substring(index, html.indexOf('"', index + 1));
		}
		
		if (StringUtils.isBlank(val)) {
			log.warn("No widget found for " + label + " ");
		} else {
			log.trace(label + "->" + val);
		}
		
		return val;
	}
}
