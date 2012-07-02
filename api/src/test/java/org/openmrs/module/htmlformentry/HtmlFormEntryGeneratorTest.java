package org.openmrs.module.htmlformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.logic.util.LogicUtil;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

public class HtmlFormEntryGeneratorTest extends BaseModuleContextSensitiveTest {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_HTML_FORM_ENTRY_TEST_DATASET = "htmlFormEntryTestDataSet";
	
	private Patient patient = null;
	
	@Before
	public void setupDatabase() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_HTML_FORM_ENTRY_TEST_DATASET));
		patient = Context.getPatientService().getPatient(2);
	}
	
	/**
	 * @see {@link HtmlFormEntryGenerator#applyExcludes(FormEntrySession,String)}
	 */
	@Test
	@Verifies(value = "should return correct xml after apply excludeIf tag", method = "applyExcludes(FormEntrySession,String)")
	public void applyExcludes_shouldReturnCorrectXmlAfterApplyExcludeIfTag() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><excludeIf logicTest=\"GENDER = F\">This shows a logic test for a woman</excludeIf><excludeIf logicTest=\"GENDER = M\">This shows a logic test for a man</excludeIf></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertEquals("<div class=\"htmlform\">This shows a logic test for a woman</div>", session.getHtmlToDisplay());
	}
	
	/**
	 * @see {@link HtmlFormEntryGenerator#applyIncludes(FormEntrySession,String)}
	 */
	@Test
	@Verifies(value = "should return correct xml after apply include tag", method = "applyIncludes(FormEntrySession,String)")
	public void applyIncludes_shouldReturnCorrectXmlAfterApplyIncludeTag() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><includeIf logicTest=\"GENDER = F\">This shows a logic test for a woman</includeIf><includeIf logicTest=\"GENDER = M\">This shows a logic test for a man</includeIf></htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertEquals("<div class=\"htmlform\">This shows a logic test for a man</div>", session.getHtmlToDisplay());
	}
	
	/**
	 * @see {@link HtmlFormEntryGenerator#getTestStr(String)}
	 */
	@Test
	@Verifies(value = "should extract the correct expression from teststr", method = "getTestStr(String)")
	public void getTestStr_shouldExtractTheCorrectExpressionFromTeststr() throws Exception {
		String includeStr = "logicTest = \"GENDER = F\">";// <includeIf test =
		// "FEMALE">
		Assert.assertEquals("GENDER = F", HtmlFormEntryGenerator.getTestStr(includeStr));
		includeStr = "velocityTest = \"$patient.gender == 'F'\">";
		Assert.assertEquals("$patient.gender == 'F'", HtmlFormEntryGenerator.getTestStr(includeStr));
	}
	
	/**
	 * @see {@link HtmlFormEntryGenerator#processIncludeLogic(FormEntrySession,String)}
	 */
	@Test
	@Verifies(value = "should return a correct boolean value for logic test string", method = "processIncludeLogic(FormEntrySession,String)")
	public void processIncludeLogic_shouldReturnACorrectBooleanValueForLogicTestString() throws Exception {
		LogicUtil.registerDefaultRules();
		String htmlform = "<htmlform><includeIf logicTest=\"GENDER = F\">This shows a logic test for a woman</includeIf><includeIf logicTest=\"GENDER = M\">This shows a logic test for a man</includeIf></htmlform>";
		String testStr = "logicTest=\"GENDER = M\">";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		Assert.assertEquals(true, HtmlFormEntryGenerator.processIncludeLogic(session, testStr));
	}
	
	/**
	 * @see HtmlFormEntryGenerator#wrapInDiv(String)
	 * @verifies remove htmlform tag and wrap form in div
	 */
	@Test
	public void wrapInDiv_shouldRemoveHtmlformTagAndWrapFormInDiv() throws Exception {
		String htmlform = "<htmlform>\rsomeContent</htmlform>";
		FormEntrySession session = new FormEntrySession(patient, htmlform);
		TestUtil.assertFuzzyContains("(?s)<div class=\"htmlform\">(.*)someContent</div>", session.getHtmlToDisplay());
	}
	
	  /**
     * @see {@link HtmlFormEntryGenerator#stripComments(String)}
     * @verifies  filters out all the comments in the input string
     */
    @Test
    public void stripComments_shouldStripOutComments() throws Exception {
        LogicUtil.registerDefaultRules();
        String htmlform = "<htmlform><section><!--<repeat><template></template><render/></repeat>--><repeat><template></template><render/></repeat></section></htmlform>";
        HtmlFormEntryGenerator htmlFormEntryGenerator = new HtmlFormEntryGenerator();
        String returnedXml = htmlFormEntryGenerator.stripComments(htmlform);

        Assert.assertEquals("<htmlform><section><repeat><template></template><render/></repeat></section></htmlform>", returnedXml);
    }

    /**
     * @see {@link HtmlFormEntryGenerator#stripComments(String)}
     */
    @Test
    @Verifies(value = "should return correct xml to display after filtering out comments", method = "stripComments(String)")
    public void stripComments_shouldReturnCorrectHtmlAfterFilteringOutComments() throws Exception {
        LogicUtil.registerDefaultRules();
        String htmlform = "<htmlform><section><!--some comment that should not be displayed--></section></htmlform>";
        FormEntrySession session = new FormEntrySession(patient, htmlform);
        Assert.assertEquals("<div class=\"htmlform\"><div class=\"section\"></div></div>", session.getHtmlToDisplay());
    }
    
    /**
     * @see {@link HtmlFormEntryGenerator#doStartTag(FormEntrySession,PrintWriter,Node,Node)}
     * @see {@link HtmlFormEntryGenerator#doEndTag(FormEntrySession,PrintWriter,Node,Node)}
     */
    @Test
    @Verifies(value = "should close br tags", method = "doStartTag(FormEntrySession,PrintWriter,Node,Node)")
    public void doStartTag_shouldCloseBrTags() throws Exception {
        LogicUtil.registerDefaultRules();
        String htmlform = "<htmlform><section><span></span><br/><h1></h1></section></htmlform>";
        FormEntrySession session = new FormEntrySession(patient, htmlform);
        String html = session.getHtmlToDisplay();
        Assert.assertTrue("<br/> should be closed with one tag", html.contains("<br/>"));
        Assert.assertTrue("<span> and other tags can be open", html.contains("<span></span>"));
        Assert.assertTrue("<h1> and other tags can be open", html.contains("<h1></h1>"));
    }
    
    /**
     * Similar to doStartTag test, but with capitalized BR tags.
     * 
     * @see {@link HtmlFormEntryGenerator#doStartTag(FormEntrySession,PrintWriter,Node,Node)}
     * @see {@link HtmlFormEntryGenerator#doEndTag(FormEntrySession,PrintWriter,Node,Node)}
     */
    @Test
    @Verifies(value = "should skip br tags", method = "doEndTag(FormEntrySession,PrintWriter,Node,Node)")
    public void doEndTag_shouldCloseBrTags() throws Exception {
        LogicUtil.registerDefaultRules();
        String htmlform = "<htmlform><section><span></span><BR/><h1></h1></section></htmlform>";
        FormEntrySession session = new FormEntrySession(patient, htmlform);
        String html = session.getHtmlToDisplay();
        Assert.assertTrue("<BR/> should be closed with one tag", html.contains("<BR/>"));
        Assert.assertTrue("<span> and other tags can be open", html.contains("<span></span>"));
        Assert.assertTrue("<h1> and other tags can be open", html.contains("<h1></h1>"));
    }
    
}
