package org.openmrs.module.htmlformentry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbunit.dataset.xml.XmlDataSet;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.test.BaseModuleContextSensitiveTest;

public class MyHtmlFormEntryTest extends BaseModuleContextSensitiveTest {

	protected final Log log = LogFactory.getLog(getClass());

	// This is for trunk
	protected static final String XML_DATASET_PACKAGE_PATH = "org/openmrs/module/htmlformentry/include/HtmlFormEntryTest-data.xml";

	// This is for sync
	// protected static final String XML_DATASET_PACKAGE_PATH =
	// "org/openmrs/module/htmlformentry/include/HtmlFormEntryTest-data.forsync.xml";

	protected static final String XML_DATASET_PACKAGE_PATH2 = "org/openmrs/module/htmlformentry/include/HtmlFormEntryTest-data2.xml";
	protected static final String HTML_FORM_PATH = "test/org/openmrs/module/htmlformentry/include/htmlForm.xml";
	protected static final String HTML_FORM2_PATH = "test/org/openmrs/module/htmlformentry/include/htmlForm2.xml";

	@Before
	public void setupDatabase() throws Exception {
		initializeInMemoryDatabase();
		authenticate();
	    executeDataSet(XML_DATASET_PACKAGE_PATH);
	    executeXmlDataSet(XML_DATASET_PACKAGE_PATH2);
	}

	public void executeXmlDataSet(String datasetFilename) throws Exception {
		File file = new File(datasetFilename);

		InputStream fileInInputStreamFormat = null;

		// try to load the file if its a straight up path to the file or
		// if its a classpath path to the file
		if (file.exists())
			fileInInputStreamFormat = new FileInputStream(datasetFilename);
		else {
			fileInInputStreamFormat = getClass().getClassLoader()
					.getResourceAsStream(datasetFilename);
			if (fileInInputStreamFormat == null)
				throw new FileNotFoundException("Unable to find '"
						+ datasetFilename + "' in the classpath");
		}

		XmlDataSet xmlDataSetToRun = null;
		try {
			xmlDataSetToRun = new XmlDataSet(fileInInputStreamFormat);
		} catch (Exception ex) {
			System.out.println("got here " + ex);
		} finally {
			fileInInputStreamFormat.close();
		}

		executeDataSet(xmlDataSetToRun);
	}

	@Test
	public void testGenerateHtmlFormFromXml() throws Exception {
		StringBuilder sb = new StringBuilder();

		sb.append("<htmlform>Symptomes:" + 
				"<newrepeat>"
				+ "<obs conceptId=\"1\" labelText=\"This is in  first repeat\"/>"
				+"<br/><obs conceptId=\"2\" rows=\"10\" cols=\"60\"  labelText=\"I've no idea wt is this\"/> "
				+ "</newrepeat>" 
				/*
				+ "\n\n<encounterDate default=\"today\"/> "
				+ "<encounterLocation/> " + "<encounterProvider/> "
				+ "<submit/> " 
				*/
				+ "</htmlform>");

		Patient demo = HtmlFormEntryUtil.getFakePerson();
		FormEntrySession session = new FormEntrySession(demo, sb.toString());
		System.out.println("<script src=\"jquery-1.4.2.js\" type=\"text/javascript\"></script>"
				+"<script src=\"htmlFormEntry.js\" type=\"text/javascript\"></script>\n" 
				+ session.getHtmlToDisplay());
	}

}
