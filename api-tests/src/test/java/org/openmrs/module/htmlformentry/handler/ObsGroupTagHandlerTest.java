package org.openmrs.module.htmlformentry.handler;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class ObsGroupTagHandlerTest extends BaseModuleContextSensitiveTest {
	
	@Test
	public void validate_shouldWarnWhenObsgroupConceptIsNotASet() throws Exception {
		String xml = "<htmlform>\n"
				+ "<obsgroup groupingConceptId=\"10\">\n"
				+ "<obs conceptId=\"3\" />\n"
				+ "<obs conceptId=\"4\" />\n"
				+ "</obsgroup>\n"
				+ "</htmlform>";
		
		Document document = HtmlFormEntryUtil.stringToDocument(xml);
		Node obsGroupNode = HtmlFormEntryUtil.findDescendant(document, "obsgroup");
		TagAnalysis analysis = new ObsGroupTagHandler().validate(obsGroupNode);
		Assert.assertEquals(1, analysis.getWarnings().size());
	}
	
	@Test
	public void validate_shouldRejectXmlWhenObsgroupGroupingConceptIdIsMissing() throws Exception {
		String xml = "<htmlform><obsgroup>TEST</obsgroup></htmlform>";
		TagAnalysis analysis = validateObsGroupTag(xml);
		Assert.assertEquals(1, analysis.getErrors().size());
	}
	
	@Test
	public void validate_shouldRejectXmlWhenObsgroupConceptCannotBeFound() throws Exception {
		String xml = "<htmlform><obsgroup groupingConceptId=\"none\">TEST</obsgroup></htmlform>";
		TagAnalysis analysis = validateObsGroupTag(xml);
		Assert.assertEquals(1, analysis.getErrors().size());
	}
	
	@Test
	public void validate_shouldPassWhenObsGroupConceptIsASet() throws Exception {
		String xml = "<htmlfom><obsgroup groupingConceptId=\"23\"><obs conceptId=\"xx\" /></obsgroup></htmlfom>";
		TagAnalysis analysis = validateObsGroupTag(xml);
		Assert.assertEquals(0, analysis.getErrors().size());
	}
	
	private TagAnalysis validateObsGroupTag(String xml) throws Exception {
		Document document = HtmlFormEntryUtil.stringToDocument(xml);
		Node obsGroupNode = HtmlFormEntryUtil.findDescendant(document, "obsgroup");
		TagAnalysis analysis = new ObsGroupTagHandler().validate(obsGroupNode);
		return analysis;
	}
	
}
