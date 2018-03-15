package org.openmrs.module.htmlformentry.handler;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class ObsTagHandlerTest extends BaseModuleContextSensitiveTest {
	
	@Test
	public void validate_shouldWarnWhenObsIsNotAMemberOfParentObsgroup() throws Exception {
		String xml = "<htmlform>\n"
				+ "<obsgroup groupingConceptId=\"23\">\n"
				+ "<obs conceptId=\"4\" />\n"
				+ "</obsgroup>\n </htmlform>";
		Document document = HtmlFormEntryUtil.stringToDocument(xml);
		Node parentObsGroupNode = HtmlFormEntryUtil.findDescendant(document, "obsgroup");
		Assert.assertEquals("23", HtmlFormEntryUtil.getNodeAttribute(parentObsGroupNode, "groupingConceptId", null));
		Node obsNode = HtmlFormEntryUtil.findChild(parentObsGroupNode, "obs");
		TagAnalysis analysis = new ObsTagHandler().validate(obsNode);
		Assert.assertEquals(1, analysis.getWarnings().size());	
	}
	
	@Test
	public void validate_shouldPassWhenObsIsAMemberOfParentObsgroup() throws Exception {
		String xml = "<htmlform>\n"
				+ "<obsgroup groupingConceptId=\"23\">\n"
				+ "<obs conceptId=\"18\" />\n"
				+ "	</obsgroup>\n </htmlform>";
		
		Document document = HtmlFormEntryUtil.stringToDocument(xml);
		Node parentObsGroupNode = HtmlFormEntryUtil.findDescendant(document, "obsgroup");
		Assert.assertEquals("23", HtmlFormEntryUtil.getNodeAttribute(parentObsGroupNode, "groupingConceptId", null));
		
		Node obsNode = HtmlFormEntryUtil.findChild(parentObsGroupNode, "obs");
		TagAnalysis analysis = new ObsTagHandler().validate(obsNode);
		Assert.assertEquals(0, analysis.getWarnings().size());
	}
	
	@Test
	public void validate_shouldRejectXmlWhenEitherObsConceptIdOrConceptIdsIsMissing() throws Exception {
		String xml = "<htmlform><obs>TEST</obs></htmlform>";
		TagAnalysis analysis = validateObsTag(xml);
		Assert.assertEquals(1, analysis.getErrors().size());
	}
	
	@Test
	public void validate_shouldRejectXmlWhenBothObsConceptIdAndConceptIdsIsMissing() throws Exception {
		String xml = "<htmlform><obs conceptId=\"xx\" conceptIds=\"xx,yy\">TEST</obs></htmlform>";
		TagAnalysis analysis = validateObsTag(xml);
		Assert.assertEquals(1, analysis.getErrors().size());
	}
	
	@Test
	public void validate_shouldRejectXmlWhenObsConceptCannotBeFound() throws Exception {
		String xml = "<htmlform><obs conceptId=\"none\">TEST</obs></htmlform>";
		TagAnalysis analysis = validateObsTag(xml);
		Assert.assertEquals(1, analysis.getErrors().size());
	}
	
	@Test
	public void validate_shouldRejectXmlWhenOneOfObsConceptIdsCannotBeFound() throws Exception {
		String xml = "<htmlform><obs conceptIds=\"18,none\">TEST</obs></htmlform>";
		TagAnalysis analysis = validateObsTag(xml);
		Assert.assertEquals(1, analysis.getErrors().size());
	}
	
	@Test
	public void validate_shouldPassWhenObsHasValidConceptId() throws Exception {
		String xml = "<htmlform><obs conceptId=\"18\">TEST</obs></htmlform>";
		TagAnalysis analysis = validateObsTag(xml);
		Assert.assertEquals(0, analysis.getErrors().size());
	}
	
	@Test
	public void validate_shouldPassWhenObsHasValidConceptIds() throws Exception {
		String xml = "<htmlform><obs conceptIds=\"18,19\">TEST</obs></htmlform>";
		TagAnalysis analysis = validateObsTag(xml);
		Assert.assertEquals(0, analysis.getErrors().size());
	}

	@Test
	public void validate_shouldPassWhenObsHasValidConceptIdAndValidConceptAnswerId() throws Exception {
		String xml = "<htmlform><obs conceptId=\"21\" answerConceptId=\"7\">TEST</obs></htmlform>";
		TagAnalysis analysis = validateObsTag(xml);
		Assert.assertEquals(0, analysis.getWarnings().size());
	}

	@Test
	public void validate_shouldPassWhenObsHasValidConceptIdAndValidConceptAnswerIds() throws Exception {
		String xml = "<htmlform><obs conceptId=\"21\" answerConceptIds=\"7,8\">TEST</obs></htmlform>";
		TagAnalysis analysis = validateObsTag(xml);
		Assert.assertEquals(0, analysis.getWarnings().size());
	}

	@Test
	public void validate_shouldRejectWhenObsHasValidConceptIdAndInvalidConceptAnswerId() throws Exception {
		String xml = "<htmlform><obs conceptId=\"21\" answerConceptId=\"5\">TEST</obs></htmlform>";
		TagAnalysis analysis = validateObsTag(xml);
		Assert.assertEquals(1, analysis.getWarnings().size());
	}

	@Test
	public void validate_shouldRejectWhenObsHasValidConceptIdAndInvalidConceptAnswerIds() throws Exception {
		String xml = "<htmlform><obs conceptId=\"21\" answerConceptIds=\"5,6\">TEST</obs></htmlform>";
		TagAnalysis analysis = validateObsTag(xml);
		Assert.assertEquals(2, analysis.getWarnings().size());
	}

	@Test
	public void validate_shouldPassWhenObsHasValidConceptIdsAndValidConceptAnswerId() throws Exception {
		String xml = "<htmlform><obs conceptIds=\"21,21\" answerConceptId=\"7\">TEST</obs></htmlform>";
		TagAnalysis analysis = validateObsTag(xml);
		Assert.assertEquals(0, analysis.getWarnings().size());
	}

	@Test
	public void validate_shouldRejectWhenObsHasPartiallyValidConceptIdsAndPartiallyInvalidConceptAnswerId() throws Exception {
		String xml = "<htmlform><obs conceptIds=\"21,4\" answerConceptId=\"5\">TEST</obs></htmlform>";
		TagAnalysis analysis = validateObsTag(xml);
		Assert.assertEquals(1, analysis.getWarnings().size());
	}

	private TagAnalysis validateObsTag(String xml) throws Exception {
		Document document = HtmlFormEntryUtil.stringToDocument(xml);
		Node obsNode = HtmlFormEntryUtil.findDescendant(document, "obs");
		TagAnalysis analysis = new ObsTagHandler().validate(obsNode);
		return analysis;
	}
}
