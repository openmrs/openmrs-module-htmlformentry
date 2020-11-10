package org.openmrs.module.htmlformentry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.Role;
import org.openmrs.api.context.Context;
import org.openmrs.test.Verifies;

public class HtmlFormEntryGeneratorTest extends BaseHtmlFormEntryTest {
	
	private Patient patient = null;
	
	private String pageCss = "<style>.pageHeader {overflow: hidden;border: 1px solid #ccc;border-bottom: 5px solid #66a3ff;background-color: #f1f1f1;}.pageHeader button {border-radius: 15px 15px 0px 0px;background-color: lightblue;border-top: 5px solid #f1f1f1;float: left;outline: none;cursor: pointer;padding: 14px 16px;transition: 0.3s;}.pageHeader button.active {background-color: #66a3ff;}.pageHeader button:hover {background-color: #ddd;}.content {display: none;overflow: hidden;padding: 10px;}.next {float: right;}.previous {float: left;}</style>";
	
	private String pageJs1 = "<script src=\"/openmrs/moduleResources/htmlformentry/jquery-3.4.1.min.js?v=2.3.0\" type=\"text/javascript\"></script>";
	
	private String jsNoConflict = "<script type=\"text/javascript\">var jQ3 = $.noConflict(true);</script>";
	
	private String pageJs2 = "<script type=\"text/javascript\">jQ3(document).ready(function (){var visibleContent = 0;function showItems() {jQ3(\'.listItems:eq(\' + visibleContent + \')\').addClass(\'active\');jQ3(\'.content:eq(\' + visibleContent + \')\').show();};showItems();jQ3(\'.pageHeader button\').click(function (event) {index = jQ3(this).index();jQ3(\'.listItems\').removeClass(\'active\');jQ3(\'.listItems:eq(\' + index + \')\').addClass(\'active\');jQ3(\'.content\').hide();visibleContent = index;showItems();});next = function () {jQ3(\'.listItems\').removeClass(\'active\');jQ3(\'.content\').hide();visibleContent = visibleContent + 1;showItems();};previous = function () {jQ3(\'.listItems\').removeClass(\'active\');jQ3(\'.content\').hide();visibleContent = visibleContent - 1;showItems();}});</script>";
	
	private String pageBodyContent = "<div class=\"pageHeader\"><button class=\"listItems\">firstTab</button><button class=\"listItems\">secondTab</button></div><div class=\"content\">Nothing Here<div><button class=\"next\" onclick=\"next();\">Next</button></div></div><div class=\"content\">Something here<div><button class=\"previous\" onclick=\"previous();\">Previous</button></div></div>";
	
	@Before
	public void setupDatabase() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
		patient = Context.getPatientService().getPatient(2);
	}
	
	@Test
	@Verifies(value = "should return correct xml after apply excludeIf tag", method = "applyExcludes(FormEntrySession,String)")
	public void applyExcludes_shouldReturnCorrectXmlAfterApplyExcludeIfTag() throws Exception {
		String htmlform = "<htmlform><excludeIf velocityTest=\"$patient.gender == 'F'\">This shows a logic test for a woman</excludeIf><excludeIf velocityTest=\"$patient.gender == 'M'\">This shows a logic test for a man</excludeIf></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertEquals("<div class=\"htmlform\">This shows a logic test for a woman</div>", session.getHtmlToDisplay());
	}
	
	@Test
	@Verifies(value = "should return correct xml after apply include tag", method = "applyIncludes(FormEntrySession,String)")
	public void applyIncludes_shouldReturnCorrectXmlAfterApplyIncludeTag() throws Exception {
		String htmlform = "<htmlform><includeIf velocityTest=\"$patient.gender == 'F'\">This shows a logic test for a woman</includeIf><includeIf velocityTest=\"$patient.gender == 'M'\">This shows a logic test for a man</includeIf></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertEquals("<div class=\"htmlform\">This shows a logic test for a man</div>", session.getHtmlToDisplay());
	}
	
	@Test
	@Verifies(value = "should return correct xml after applying <repeat> tag", method = "applyRepeats(String)")
	public void applyRepeats_shouldReturnCorrectValueAfterApplyRepeatTag() throws Exception {
		
		String htmlform = "<htmlform><repeat><template><obs conceptId=\"4300\" answerConceptId=\"{concept}\" answerLabel=\"{effect}\"/></template><render concept=\"4301\" effect=\"Stroke\"/>"
		        + "<render concept=\"4302\" effect=\"Other Non-coded\"/></repeat></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		String testText = "<input type=\"checkbox\" id=\"w2\" name=\"w2\" value=\"4301\"/><label for=\"w2\">Stroke</label>"
		        + "<input type=\"hidden\" name=\"_w2\"/> <span class=\"error field-error\" style=\"display: none\" id=\"w1\"></span>"
		        + "<input type=\"checkbox\" id=\"w4\" name=\"w4\" value=\"4302\"/><label for=\"w4\">Other Non-coded</label>"
		        + "<input type=\"hidden\" name=\"_w4\"/>";
		Assert.assertTrue(session.getHtmlToDisplay().contains(testText));
		
	}
	
	@Test
	@Verifies(value = "should return correct xml after applying <repeat with=''> tag", method = "applyRepeats(String)")
	public void applyRepeats_shouldReturnCorrectValueAfterApplyRepeatWithTag() throws Exception {
		
		// note that we throw in some random spaces here to make sure that we handle them correctly
		String htmlform = "<htmlform><repeat with=\" [ '4301','STROKE' ], ['4302', 'OTHER NON-CODED' ]\"><obs conceptId=\"4300\" answerConceptId=\"{0}\" answerLabel=\"{1}\" style=\"checkbox\" />"
		        + "</repeat></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		String testText = "<input type=\"checkbox\" id=\"w2\" name=\"w2\" value=\"4301\"/><label for=\"w2\">STROKE</label>"
		        + "<input type=\"hidden\" name=\"_w2\"/> <span class=\"error field-error\" style=\"display: none\" id=\"w1\"></span>"
		        + "<input type=\"checkbox\" id=\"w4\" name=\"w4\" value=\"4302\"/><label for=\"w4\">OTHER NON-CODED</label>"
		        + "<input type=\"hidden\" name=\"_w4\"/>";
		Assert.assertTrue(session.getHtmlToDisplay().contains(testText));
		
	}
	
	@Test
	@Verifies(value = "should return correct xml after applying <repeat with=''> tag with tabs in between", method = "applyRepeats(String)")
	public void applyRepeats_shouldReturnCorrectValueAfterApplyRepeatWithTagWithTab() throws Exception {
		
		// note that we throw in some random spaces here to make sure that we handle them correctly
		String htmlform = "<htmlform><repeat    with=\" [ '4301','STROKE' ], ['4302', 'OTHER NON-CODED' ]\"><obs conceptId=\"4300\" answerConceptId=\"{0}\" answerLabel=\"{1}\" style=\"checkbox\" />"
		        + "</repeat></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		String testText = "<input type=\"checkbox\" id=\"w2\" name=\"w2\" value=\"4301\"/><label for=\"w2\">STROKE</label>"
		        + "<input type=\"hidden\" name=\"_w2\"/> <span class=\"error field-error\" style=\"display: none\" id=\"w1\"></span>"
		        + "<input type=\"checkbox\" id=\"w4\" name=\"w4\" value=\"4302\"/><label for=\"w4\">OTHER NON-CODED</label>"
		        + "<input type=\"hidden\" name=\"_w4\"/>";
		Assert.assertTrue(session.getHtmlToDisplay().contains(testText));
	}
	
	@Test
	@Verifies(value = "should return correct xml after applying <repeat with=''> tag with new line in between", method = "applyRepeats(String)")
	public void applyRepeats_shouldReturnCorrectValueAfterApplyRepeatWithTagWithNewLine() throws Exception {
		
		// note that we throw in some random spaces here to make sure that we handle them correctly
		String htmlform = "<htmlform><repeat\n with=\" [ '4301','STROKE' ], ['4302', 'OTHER NON-CODED' ]\"><obs conceptId=\"4300\" answerConceptId=\"{0}\" answerLabel=\"{1}\" style=\"checkbox\" />"
		        + "</repeat></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		String testText = "<input type=\"checkbox\" id=\"w2\" name=\"w2\" value=\"4301\"/><label for=\"w2\">STROKE</label>"
		        + "<input type=\"hidden\" name=\"_w2\"/> <span class=\"error field-error\" style=\"display: none\" id=\"w1\"></span>"
		        + "<input type=\"checkbox\" id=\"w4\" name=\"w4\" value=\"4302\"/><label for=\"w4\">OTHER NON-CODED</label>"
		        + "<input type=\"hidden\" name=\"_w4\"/>";
		Assert.assertTrue(session.getHtmlToDisplay().contains(testText));
	}
	
	@Test
	@Verifies(value = "should return correct xml after applying multiple <repeat with=''> tag", method = "applyRepeats(String)")
	public void applyRepeats_shouldReturnCorrectVaueAfterApplyingMultipleRepeatWithTag() throws Exception {
		
		String htmlform = "<htmlform><repeat with=\"['4301','STROKE'],['4302','OTHER NON-CODED']\"><obs conceptId=\"4300\" answerConceptId=\"{0}\" answerLabel=\"{1}\" style=\"checkbox\" />"
		        + "</repeat><repeat with=\"['4302','CANCER'],['4301','FLU']\"><obs conceptId=\"4300\" answerConceptId=\"{0}\" answerLabel=\"{1}\" style=\"checkbox\" />"
		        + "</repeat></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		String testText = "<input type=\"checkbox\" id=\"w2\" name=\"w2\" value=\"4301\"/><label for=\"w2\">STROKE</label>"
		        + "<input type=\"hidden\" name=\"_w2\"/> <span class=\"error field-error\" style=\"display: none\" id=\"w1\"></span>"
		        + "<input type=\"checkbox\" id=\"w4\" name=\"w4\" value=\"4302\"/><label for=\"w4\">OTHER NON-CODED</label>"
		        + "<input type=\"hidden\" name=\"_w4\"/> <span class=\"error field-error\" style=\"display: none\" id=\"w3\"></span>"
		        + "<input type=\"checkbox\" id=\"w6\" name=\"w6\" value=\"4302\"/><label for=\"w6\">CANCER</label>"
		        + "<input type=\"hidden\" name=\"_w6\"/> <span class=\"error field-error\" style=\"display: none\" id=\"w5\"></span>"
		        + "<input type=\"checkbox\" id=\"w8\" name=\"w8\" value=\"4301\"/><label for=\"w8\">FLU</label>"
		        + "<input type=\"hidden\" name=\"_w8\"/>";
		
		Assert.assertTrue(session.getHtmlToDisplay().contains(testText));
		
	}
	
	/**
	 * @see {@link HtmlFormEntryGenerator#applyRepeats(String)}
	 * @throws Exception
	 */
	@Test
	@Verifies(value = "should return correct xml with <repeat with> tag after <repeat> tag", method = "applyRepeats(String)")
	public void applyTemplates_shouldReturnCorrectXmlRepeathWithTagAfterRepeatTag() throws Exception {
		
		/* verifies correct html when there is '<repeat with=""> tag after <repeat> tag together*/
		String htmlform = "<htmlform><repeat with=\"['4301','STROKE'],['4302','OTHER NON-CODED']\"><obs conceptId=\"4300\" answerConceptId=\"{0}\" answerLabel=\"{1}\" style=\"checkbox\" />"
		        + "</repeat><repeat><template><obs conceptId=\"4300\" answerConceptId=\"{concept}\" answerLabel=\"{effect}\"/></template><render concept=\"4301\" effect=\"Stroke\"/>"
		        + "<render concept=\"4302\" effect=\"Other Non-coded\"/></repeat></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		String testText = "<input type=\"checkbox\" id=\"w2\" name=\"w2\" value=\"4301\"/><label for=\"w2\">STROKE</label>"
		        + "<input type=\"hidden\" name=\"_w2\"/> <span class=\"error field-error\" style=\"display: none\" id=\"w1\"></span>"
		        + "<input type=\"checkbox\" id=\"w4\" name=\"w4\" value=\"4302\"/><label for=\"w4\">OTHER NON-CODED</label>"
		        + "<input type=\"hidden\" name=\"_w4\"/> <span class=\"error field-error\" style=\"display: none\" id=\"w3\"></span>"
		        + "<input type=\"checkbox\" id=\"w6\" name=\"w6\" value=\"4301\"/><label for=\"w6\">Stroke</label>"
		        + "<input type=\"hidden\" name=\"_w6\"/> <span class=\"error field-error\" style=\"display: none\" id=\"w5\"></span>"
		        + "<input type=\"checkbox\" id=\"w8\" name=\"w8\" value=\"4302\"/><label for=\"w8\">Other Non-coded</label>"
		        + "<input type=\"hidden\" name=\"_w8\"/>";
		Assert.assertTrue(session.getHtmlToDisplay().contains(testText));
		
	}
	
	/**
	 * @see {@link HtmlFormEntryGenerator#applyRepeats(String)}
	 * @throws Exception
	 */
	@Test
	@Verifies(value = "should return correct xml with <repeat with> tag before <repeat> tag", method = "applyRepeats(String)")
	public void applyTemplates_shouldReturnCorrectXmlRepeatWithTagBeforeRepeatTag() throws Exception {
		
		/*verifies correct html when there is <repeat> tag after '<repeat with=""> tag together*/
		String htmlform = "<htmlform><repeat><template><obs conceptId=\"4300\" answerConceptId=\"{concept}\" answerLabel=\"{effect}\"/></template><render concept=\"4301\" effect=\"Stroke\"/>"
		        + "<render concept=\"4302\" effect=\"Other Non-coded\"/></repeat><repeat with=\"['4301','STROKE'],['4302','OTHER NON-CODED']\"><obs conceptId=\"4300\" answerConceptId=\"{0}\" answerLabel=\"{1}\" style=\"checkbox\" />"
		        + "</repeat></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		String testText = "<input type=\"checkbox\" id=\"w2\" name=\"w2\" value=\"4301\"/><label for=\"w2\">Stroke</label>"
		        + "<input type=\"hidden\" name=\"_w2\"/> <span class=\"error field-error\" style=\"display: none\" id=\"w1\"></span>"
		        + "<input type=\"checkbox\" id=\"w4\" name=\"w4\" value=\"4302\"/><label for=\"w4\">Other Non-coded</label>"
		        + "<input type=\"hidden\" name=\"_w4\"/> <span class=\"error field-error\" style=\"display: none\" id=\"w3\"></span>"
		        + "<input type=\"checkbox\" id=\"w6\" name=\"w6\" value=\"4301\"/><label for=\"w6\">STROKE</label>"
		        + "<input type=\"hidden\" name=\"_w6\"/> <span class=\"error field-error\" style=\"display: none\" id=\"w5\"></span>"
		        + "<input type=\"checkbox\" id=\"w8\" name=\"w8\" value=\"4302\"/><label for=\"w8\">OTHER NON-CODED</label>"
		        + "<input type=\"hidden\" name=\"_w8\"/>";
		Assert.assertTrue(session.getHtmlToDisplay().contains(testText));
		
	}
	
	/**
	 * @see {@link HtmlFormEntryGenerator#applyRoleRestrictions(String)}
	 * @throws Exception
	 */
	@Test
	@Verifies(value = "should return correct xml after apply restrictByRole tag", method = "applyRoleRestrictions(FormEntrySession,String)")
	public void applyRoleRestrictions_shouldReturnCorrectXmlAfterApplyRestrictByRoleTag() throws Exception {
		
		/* check the restriction with a single role in include/exclude */
		String htmlform = "<htmlform><restrictByRole include=\"System Developer\">This is shown to admin as as content is included</restrictByRole><restrictByRole include=\"Data Manager\">This is not shown to admin as it doesn't contain this role</restrictByRole></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertEquals("<div class=\"htmlform\">This is shown to admin as as content is included</div>",
		    session.getHtmlToDisplay());
		
		String htmlform2 = "<htmlform><restrictByRole exclude=\"System Developer\">This is not shown to admin as content is excluded</restrictByRole><restrictByRole exclude=\"Data Manager\">This is shown to admin as content is not excluded</restrictByRole></htmlform>";
		FormEntrySession session2 = new FormEntrySession(patient, htmlform2, null);
		Assert.assertEquals("<div class=\"htmlform\">This is shown to admin as content is not excluded</div>",
		    session2.getHtmlToDisplay());
		
		/* check the restriction with multiple roles in include/exclude */
		String htmlform3 = "<htmlform><restrictByRole include=\"System Developer,Data Manager\">This is shown to admin with multiple roles in include field</restrictByRole><restrictByRole include=\"Provider,Data Manager\">This is not shown to admin with multiple roles in include field</restrictByRole></htmlform>";
		FormEntrySession session3 = new FormEntrySession(patient, htmlform3, null);
		Assert.assertEquals("<div class=\"htmlform\">This is shown to admin with multiple roles in include field</div>",
		    session3.getHtmlToDisplay());
		
		String htmlform4 = "<htmlform><restrictByRole exclude=\"System Developer,Data Manager\">This is not shown to admin with multiple roles in exclude field</restrictByRole><restrictByRole exclude=\"Provider,Data Manager\">This is shown to admin with multiple roles in exclude field</restrictByRole></htmlform>";
		FormEntrySession session4 = new FormEntrySession(patient, htmlform4, null);
		Assert.assertEquals("<div class=\"htmlform\">This is shown to admin with multiple roles in exclude field</div>",
		    session4.getHtmlToDisplay());
		
		/* check the restriction for a single user with multiple roles */
		Context.getAuthenticatedUser().addRole(new Role("Test Role", "A temporary role for the test"));
		
		String htmlform5 = "<htmlform><restrictByRole include=\"System Developer,Test Role\">This is shown to admin with multiple roles to single user in include field</restrictByRole><restrictByRole include=\"Provider,Data Manager\">This is not shown to admin with multiple roles to single user in include field</restrictByRole></htmlform>";
		FormEntrySession session5 = new FormEntrySession(patient, htmlform5, null);
		Assert.assertEquals(
		    "<div class=\"htmlform\">This is shown to admin with multiple roles to single user in include field</div>",
		    session5.getHtmlToDisplay());
		
		String htmlform6 = "<htmlform><restrictByRole exclude=\"System Developer,Test Role\">This is not shown to admin with multiple roles to single user in exclude field</restrictByRole><restrictByRole exclude=\"Provider,Data Manager\">This is shown to admin with multiple roles to single user in exclude field</restrictByRole></htmlform>";
		FormEntrySession session6 = new FormEntrySession(patient, htmlform6, null);
		Assert.assertEquals(
		    "<div class=\"htmlform\">This is shown to admin with multiple roles to single user in exclude field</div>",
		    session6.getHtmlToDisplay());
		
	}
	
	/**
	 * @see HtmlFormEntryGenerator#wrapInDiv(String)
	 * @verifies remove htmlform tag and wrap form in div
	 */
	@Test
	public void wrapInDiv_shouldRemoveHtmlformTagAndWrapFormInDiv() throws Exception {
		String htmlform = "<htmlform>\rsomeContent</htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		TestUtil.assertFuzzyContains("(?s)<div class=\"htmlform\">(.*)someContent</div>", session.getHtmlToDisplay());
	}
	
	/**
	 * @see {@link HtmlFormEntryGenerator#stripComments(String)}
	 * @verifies filters out all the comments in the input string
	 */
	@Test
	public void stripComments_shouldStripOutComments() throws Exception {
		String htmlform = "<htmlform><section><!--<repeat><template></template><render/></repeat>--><repeat><template></template><render/></repeat></section></htmlform>";
		HtmlFormEntryGenerator htmlFormEntryGenerator = new HtmlFormEntryGenerator();
		String returnedXml = htmlFormEntryGenerator.stripComments(htmlform);
		
		Assert.assertEquals("<htmlform><section><repeat><template></template><render/></repeat></section></htmlform>",
		    returnedXml);
	}
	
	/**
	 * @see {@link HtmlFormEntryGenerator#stripComments(String)}
	 */
	@Test
	@Verifies(value = "should return correct xml to display after filtering out comments", method = "stripComments(String)")
	public void stripComments_shouldReturnCorrectHtmlAfterFilteringOutComments() throws Exception {
		String htmlform = "<htmlform><section><!--some comment that should not be displayed--></section></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		Assert.assertEquals("<div class=\"htmlform\"><div class=\"section\"></div></div>", session.getHtmlToDisplay());
	}
	
	/**
	 * @see {@link HtmlFormEntryGenerator#substituteCharacterCodesWithAsciiCodes(String)}
	 * @verifies replaces all hardcoded special characters with ascii codes in the input string
	 */
	@Test
	public void substituteCharacterCodesWithAsciiCodes_shouldSubstituteSpecialCharacters() throws Exception {
		String htmlform = "<htmlform><h1>Testing&nbsp;Replacement</h1></htmlform>";
		HtmlFormEntryGenerator htmlFormEntryGenerator = new HtmlFormEntryGenerator();
		String returnedXml = htmlFormEntryGenerator.substituteCharacterCodesWithAsciiCodes(htmlform);
		Assert.assertEquals("<htmlform><h1>Testing&#160;Replacement</h1></htmlform>", returnedXml);
	}
	
	@Test
	@Verifies(value = "should close br tags", method = "doStartTag(FormEntrySession,PrintWriter,Node,Node)")
	public void doStartTag_shouldCloseBrTags() throws Exception {
		String htmlform = "<htmlform><section><span></span><br/><h1></h1></section></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		String html = session.getHtmlToDisplay();
		Assert.assertTrue("<br/> should be closed with one tag", html.contains("<br/>"));
		Assert.assertTrue("<span> and other tags can be open", html.contains("<span></span>"));
		Assert.assertTrue("<h1> and other tags can be open", html.contains("<h1></h1>"));
	}
	
	/**
	 * Similar to doStartTag test, but with capitalized BR tags.
	 */
	@Test
	@Verifies(value = "should skip br tags", method = "doEndTag(FormEntrySession,PrintWriter,Node,Node)")
	public void doEndTag_shouldCloseBrTags() throws Exception {
		String htmlform = "<htmlform><section><span></span><BR/><h1></h1></section></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		String html = session.getHtmlToDisplay();
		Assert.assertTrue("<BR/> should be closed with one tag", html.contains("<BR/>"));
		Assert.assertTrue("<span> and other tags can be open", html.contains("<span></span>"));
		Assert.assertTrue("<h1> and other tags can be open", html.contains("<h1></h1>"));
	}
	
	@Test
	public void applyMacros_shouldApplyMacrosDefinedAsTextContent() throws Exception {
		String htmlform = "<htmlform><macros>color=blue\nshape=circle</macros>$color $shape</htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		String html = session.getHtmlToDisplay();
		Assert.assertEquals("<div class=\"htmlform\">blue circle</div>", html);
	}
	
	@Test
	public void applyMacros_shouldApplyMacrosDefinedAsChildNodes() throws Exception {
		String htmlform = "<htmlform><macros><macro key=\"color\" value=\"blue\"/><macro key=\"shape\" value=\"circle\"/></macros>$color $shape</htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		String html = session.getHtmlToDisplay();
		Assert.assertEquals("<div class=\"htmlform\">blue circle</div>", html);
	}
	
	@Test
	public void applyMacros_shouldApplyMacrosDefinedAsChildNodesWithExpressions() throws Exception {
		String htmlform = "<htmlform><macros><macro key=\"loc\" expression=\"fn.locale('fr_FR').getDisplayName()\"/></macros>$loc</htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform, null);
		String html = session.getHtmlToDisplay();
		Assert.assertEquals("<div class=\"htmlform\">French (France)</div>", html);
	}
	
	@Test
	public void processPages_shouldGenerateCorrectPagesIfPageTagIfUsed() throws Exception {
		String pageHtmlform = "<htmlform><page title=\"firstTab\">Nothing Here</page><page title=\"secondTab\">Something here</page></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, pageHtmlform, null);
		String html = session.getHtmlToDisplay();
		Assert.assertEquals(
		    removeWhiteSpaces(
		        "<div class=\"htmlform\">" + pageCss + pageJs1 + jsNoConflict + pageJs2 + pageBodyContent + "</div>"),
		    removeWhiteSpaces(html));
	}
	
	@Test
	public void processPages_shouldAllowScriptToBeRunBeforeAndAfterPageTagIfUsed() throws Exception {
		String pageHtmlform = "<htmlform><script type=\"text/javascript\"></script><page title=\"firstTab\">Nothing Here</page><page title=\"secondTab\">Something here</page><script type=\"text/javascript\"></script></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, pageHtmlform, null);
		String html = session.getHtmlToDisplay();
		Assert.assertEquals(
		    removeWhiteSpaces("<div class=\"htmlform\"><script type=\"text/javascript\"></script>" + pageCss + pageJs1
		            + jsNoConflict + pageJs2 + pageBodyContent + "<script type=\"text/javascript\"></script></div>"),
		    removeWhiteSpaces(html));
	}
	
	@Test
	public void processPages_shouldAllowStyleToBeRunBeforeAndAfterPageTagIfUsed() throws Exception {
		String pageHtmlform = "<htmlform><style></style><page title=\"firstTab\">Nothing Here</page><page title=\"secondTab\">Something here</page><style ></style></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, pageHtmlform, null);
		String html = session.getHtmlToDisplay();
		Assert.assertEquals(removeWhiteSpaces("<div class=\"htmlform\"><style></style>" + pageCss + pageJs1 + jsNoConflict
		        + pageJs2 + pageBodyContent + "<style ></style></div>"),
		    removeWhiteSpaces(html));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void processPages_shouldThrowErrorIfTagIsIncludedOutsideThePageTagIfUsed() throws Exception {
		String pageHtmlform = "<htmlform><section></section><page title=\"firstTab\">Nothing Here</page><page title=\"secondTab\">Something here</page><style ></style></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, pageHtmlform, null);
		session.getHtmlToDisplay();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void processPages_shouldThrowErrorIfTextIsIncludedOutsideThePageTagIfUsed() throws Exception {
		String pageHtmlform = "<htmlform><page title=\"firstTab\">Nothing Here</page>This is outside the Page Tag<page title=\"secondTab\">Something here</page><style ></style></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, pageHtmlform, null);
		session.getHtmlToDisplay();
	}
	
	String removeWhiteSpaces(String input) {
		return input.replaceAll("\\s+", "");
	}
}
