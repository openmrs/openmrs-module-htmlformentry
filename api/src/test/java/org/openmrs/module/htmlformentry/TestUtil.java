package org.openmrs.module.htmlformentry;

import org.junit.Assert;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.RegressionTestHelper.ObsValue;
import org.openmrs.util.Format;
import org.openmrs.util.OpenmrsUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;


public class TestUtil {

	public static final String TEST_DATASETS_PROPERTIES_FILE = "test-datasets.properties";
	
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
	 * Determines the name of the proper test dataset based on what version of OpenMRS we are testing against
	 */
    public String getTestDatasetFilename(String testDatasetName) throws Exception {
		
		InputStream propertiesFileStream = null;
		
		// try to load the file if its a straight up path to the file or
		// if its a classpath path to the file
		if (new File(TEST_DATASETS_PROPERTIES_FILE).exists()) {
			propertiesFileStream = new FileInputStream(TEST_DATASETS_PROPERTIES_FILE);
		} else {
			propertiesFileStream = getClass().getClassLoader().getResourceAsStream(TEST_DATASETS_PROPERTIES_FILE);
			if (propertiesFileStream == null)
				throw new FileNotFoundException("Unable to find '" + TEST_DATASETS_PROPERTIES_FILE + "' in the classpath");
		}
  
		Properties props = new Properties();
		
		OpenmrsUtil.loadProperties(props, propertiesFileStream);

		if (props.getProperty(testDatasetName) == null) {
			throw new Exception ("Test dataset named " + testDatasetName + " not found in properties file");
		}
		
		return props.getProperty(testDatasetName);
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
	
	/**
	 * Tests whether the substring is contained in the actual string. Allows for inclusion of
	 * regular expressions in the substring.
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
	    Obs obs = createObs(encounter, conceptId, value, date);
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
	
	/**
	 * 
	 * Utility to give us a ready-to-save (without violating foreign-key constraints) Obs
	 * 
	 * @param encounter
	 * @param conceptId
	 * @param value
	 * @param date
	 * @return
	 */
	public static Obs createObs(Encounter encounter, Integer conceptId, Object value, Date date){
	    Obs obs = new Obs(encounter.getPatient(), Context.getConceptService().getConcept(conceptId), date, encounter.getLocation());
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
	
	
	/**
	 * Given a Date object, returns a Date object for the same date but with the time component (hours, minutes, seconds & milliseconds) removed
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
	
}
