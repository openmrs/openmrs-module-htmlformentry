package org.openmrs.module.htmlformentry.test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This class exists to facilitate updating a form in a database for testing from an xml file on the filesystem
 */
@Ignore
public class UpdateFormTest {

	String filePath = "src/test/resources/testForm.xml";
	Integer htmlFormId = 35;
	String dbUser = "root";
	String dbPass = "password";
	String dbUrl = "jdbc:mysql://localhost:3308/openmrs";
	String dbDriver = "com.mysql.jdbc.Driver";
	
	@Test
	public void updateForm() throws Exception {
		File f = new File("tmp.txt");
		File sourceFile = new File(filePath);
		String xml = FileUtils.readFileToString(sourceFile, "UTF-8");
		String q = "update htmlformentry_html_form set xml_data = ? where id = ?";
		Class.forName(dbDriver);
		try (Connection c = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
			try (PreparedStatement s = c.prepareStatement(q)) {
				s.setString(1, xml);
				s.setInt(2, htmlFormId);
				s.executeUpdate();
			}
		}
	}
}
