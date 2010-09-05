package org.openmrs.module.htmlformentry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import org.junit.Assert;


public class HtmlFormEntryTestUtil {
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
	
}
