package org.openmrs.module.htmlformentry.export;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Form;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.HtmlFormExporter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class is responsible for exporting htmlforms in a manner suitable for later importing back
 * in using initializer. A primary use case would be to support an implementation that has many
 * htmlforms in their database and wants to transition to start maintaining these via initializer
 */
public class InitializerExportUtil {
	
	protected final static Log log = LogFactory.getLog(InitializerExportUtil.class);
	
	/**
	 * This method creates a zip archive of all htmlforms in the system, with normalized file names, and
	 * with the xml data altered to support initializer by adding appropriate attributes to the htmlform
	 * tag. If useSubstitution is set to true, this will also attempt to convert primary key ids to
	 * uuids wherever possible. This has a side effect of importing and exporting the xml data from an
	 * xml Document, which may result in some xml differences (eg. order of attributes in tags, etc)
	 */
	public static void writeAllHtmlFormsAsZip(boolean useSubstitutions, OutputStream out) {
		log.info("Exporting all htmlforms for Initializer");
		try {
			Set<String> filesAdded = new HashSet<>();
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
				try (ZipOutputStream zipFile = new ZipOutputStream(baos)) {
					for (HtmlForm form : Context.getService(HtmlFormEntryService.class).getAllHtmlForms()) {
						log.info("Adding form: " + form.getName());
						String content = getXmlForInitializer(form, useSubstitutions);
						String fileName = getFileNameForForm(form);
						if (filesAdded.contains(fileName)) {
							fileName += "-" + form.getId();
						}
						ZipEntry zipEntry = new ZipEntry(fileName + ".xml");
						zipFile.putNextEntry(zipEntry);
						zipFile.write(content.getBytes("UTF-8"));
						filesAdded.add(fileName);
					}
					log.info("Added " + filesAdded.size() + " files to zip archive");
				}
				log.info("Writing zip");
				IOUtils.write(baos.toByteArray(), out);
			}
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to export all htmlforms for initializer", e);
		}
	}
	
	/**
	 * This method returns the xml data for an htmlform, with the xml data altered to support
	 * initializer by adding appropriate attributes to the htmlform tag. If useSubstitution is set to
	 * true, this will also attempt to convert primary key ids to uuids wherever possible. This has a
	 * side effect of importing and exporting the xml data from an xml Document, which may result in
	 * some xml differences (eg. order of attributes in tags, etc)
	 */
	public static String getXmlForInitializer(HtmlForm htmlForm, boolean useSubstitutions) throws Exception {
		
		String xml = htmlForm.getXmlData();
		Map<String, String> attrs = getAttributesExpectedForHtmlForm(htmlForm);
		
		// If useSubstitutions is indicated, use the HtmlFormExporter to replace primary keys wherever possible
		if (useSubstitutions) {
			HtmlFormExporter exporter = new HtmlFormExporter(htmlForm);
			htmlForm = exporter.export(true, true, true, true);
			xml = htmlForm.getXmlData();
			
			// Next, ensure that the appropriate attributes are included in the htmlform tag
			Document doc = HtmlFormEntryUtil.stringToDocument(xml);
			Element htmlFormElement = (Element) HtmlFormEntryUtil.findChild(doc, "htmlform");
			for (String attName : attrs.keySet()) {
				htmlFormElement.setAttribute(attName, attrs.get(attName));
			}
			xml = HtmlFormEntryUtil.documentToString(doc);
		}
		// If useSubstitutions is false, then avoid manipulating the document if possible, and just modify the htmlform tag
		else {
			// (<htmlform) - Matches any tag starting with <htmlform
			// | - Starting with this means either nothing or what comes to the right
			// \s+(?:.*?)="(?:.*?)" - Whitespace, followed by attributeName="attributeValue"
			// (>) - the closing tag
			// This will match both an empty <htmlform> tag and also one with existing attributes
			String htmlFormTagPatternStr = "(<htmlform)(|\\s+(?:.*?)=\"(?:.*?)\")(>)";
			Pattern htmlFormTagPattern = Pattern.compile(htmlFormTagPatternStr, Pattern.CASE_INSENSITIVE);
			Matcher htmlFormTagMatcher = htmlFormTagPattern.matcher(xml);
			String toReplace = null;
			String replaceWith = null;
			while (htmlFormTagMatcher.find()) {
				toReplace = htmlFormTagMatcher.group(0); // This should be the entire tag
				String tagStart = htmlFormTagMatcher.group(1); // This should be <htmlform
				String tagAttributes = htmlFormTagMatcher.group(2); // this should be any existing attributes
				String tagEnd = htmlFormTagMatcher.group(3); // This should be >
				
				// Tag attributes should come from the form metadata.  Don't override these if the form specifies otherwise.
				// But if new attributes exist, include these as-is
				if (StringUtils.isNotBlank(tagAttributes)) {
					// This matches a pattern of attName1="attValue1" attName2="attValue2" ...
					Pattern attributePattern = Pattern.compile("\\s+(.*?)=\"(.*?)\"", Pattern.CASE_INSENSITIVE);
					Matcher attributeMatcher = attributePattern.matcher(tagAttributes);
					while (attributeMatcher.find()) {
						String attName = attributeMatcher.group(1);
						String attVal = attributeMatcher.group(2);
						if (!attrs.containsKey(attName)) {
							attrs.put(attName, attVal);
						}
					}
				}
				
				StringBuilder newTag = new StringBuilder();
				newTag.append(tagStart);
				for (String attr : attrs.keySet()) {
					newTag.append(" \n\t");
					newTag.append(attr).append("=\"");
					newTag.append(StringEscapeUtils.escapeXml(attrs.get(attr)));
					newTag.append("\"");
				}
				newTag.append(System.getProperty("line.separator")).append(tagEnd);
				replaceWith = newTag.toString();
			}
			
			if (toReplace != null && replaceWith != null) {
				log.debug("Replacing " + toReplace + " with " + replaceWith);
				xml = xml.replace(toReplace, replaceWith);
			}
		}
		
		return xml;
	}
	
	/**
	 * This method aims to create a name for a file given an htmlform It essentially just converts the
	 * form name to lower case and replaces white-space and special characters where possible
	 */
	public static String getFileNameForForm(HtmlForm form) {
		String name = form.getName();
		name = name.toLowerCase();
		name = name.replace(" ", "-");
		name = name.replace("'", "");
		name = name.replace("\"", "");
		name = name.replace("/", "-");
		name = name.replace(",", "-");
		name = name.replace(":", "-");
		name = name.replace("(", "-");
		name = name.replace(")", "-");
		return name;
	}
	
	/**
	 * This method gets the attributes that one would expect to find on an htmlform that is consumed by
	 * initializer
	 */
	public static Map<String, String> getAttributesExpectedForHtmlForm(HtmlForm htmlForm) {
		Map<String, String> ret = new LinkedHashMap<>();
		Form f = htmlForm.getForm();
		ret.put("htmlformUuid", htmlForm.getUuid());
		ret.put("formUuid", f.getUuid());
		ret.put("formName", f.getName());
		if (f.getDescription() != null) {
			ret.put("formDescription", f.getDescription());
		}
		if (f.getVersion() != null) {
			ret.put("formVersion", f.getVersion());
		}
		if (f.getEncounterType() != null) {
			ret.put("formEncounterType", f.getEncounterType().getUuid());
		}
		if (f.getPublished() != null) {
			ret.put("formPublished", f.getPublished().toString());
		}
		if (f.getRetired() != null) {
			ret.put("formRetired", f.getRetired().toString());
		}
		return ret;
	}
}
