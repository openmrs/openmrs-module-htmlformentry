package org.openmrs.module.htmlformentry.element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;
import org.openmrs.module.htmlformentry.schema.HtmlFormField;
import org.openmrs.module.htmlformentry.schema.ObsFieldAnswer;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

public class DrugOrderSubmissionElementTest extends BaseModuleContextSensitiveTest {
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_DATASET_PACKAGE_PATH = "org/openmrs/include/standardTestDataset.xml";
	
	protected static final String XML_DRUG_ORDER_ELEMENT_FILE = "drugOrderElement.xml";
	
	private FormEntryContext context;
	
	@Before
	public void setupDatabase() throws Exception {
		initializeInMemoryDatabase();
		authenticate();
		executeDataSet(XML_DATASET_PACKAGE_PATH);
		executeDataSet(XML_DATASET_PATH + XML_DRUG_ORDER_ELEMENT_FILE);
	}
	
	@Before
	public void setupContext() {
		if (context == null) {
			context = new FormEntryContext(Mode.ENTER);
		}
	}
	
	/**
	 * @see {@link
	 *      DrugOrderSubmissionElement#DrugOrderSubmissionElement(FormEntryContext,Map<QString;
	 *      QString;>)}
	 */
	@Test(expected = IllegalArgumentException.class)
	@Verifies(value = "should only instantiate object if required parameter FIELD_DRUG_NAMES is passed", method = "DrugOrderSubmissionElement(FormEntryContext,Map<QString;QString;>)")
	public void DrugOrderSubmissionElement_shouldOnlyInstantiateObjectIfRequiredParameterFIELD_DRUG_NAMESIsPassed()
	                                                                                                               throws Exception {
		Map<String, String> parameters;
		@SuppressWarnings("unused")
        DrugOrderSubmissionElement element;
		
		// test empty parameters map
		parameters = new HashMap<String, String>();
		element = new DrugOrderSubmissionElement(context, parameters);
		Assert.fail("IllegalArgumentException expected");
	}
	
	/**
	 * @see {@link
	 *      DrugOrderSubmissionElement#DrugOrderSubmissionElement(FormEntryContext,Map<QString;
	 *      QString;>)}
	 */
	@Test(expected = IllegalArgumentException.class)
	@Verifies(value = "should only instantiate object if required parameter FIELD_DRUG_NAMES is passed", method = "DrugOrderSubmissionElement(FormEntryContext,Map<QString;QString;>)")
	public void DrugOrderSubmissionElement_shouldInstantiateObject() throws Exception {
		Map<String, String> parameters;
		// test parameters map, at least one valid drug name
		parameters = new HashMap<String, String>();
		parameters.put(DrugOrderSubmissionElement.FIELD_DRUG_NAMES, "3, INVALIDDRUGNAME");
		
		Assert.fail("IllegalArgumentException expected");
	}
	
	/**
	 * @see {@link DrugOrderSubmissionElement#generateHtml(FormEntryContext)}
	 */
	@Test
	@Verifies(value = "should return HTML snippet", method = "generateHtml(FormEntryContext)")
	public void generateHtml_shouldReturnHTMLSnippet() throws Exception {
		Map<String, String> parameters;
		DrugOrderSubmissionElement element;
		
		parameters = new HashMap<String, String>();
		parameters.put(DrugOrderSubmissionElement.FIELD_DRUG_NAMES, "3");
		element = new DrugOrderSubmissionElement(context, parameters);
		String resultHTML = element.generateHtml(context);
		Assert.assertTrue(resultHTML.length() > 0);
		Assert.assertTrue(resultHTML.indexOf("<input") > 0);
		
		// VIEW mode
		context = new FormEntryContext(Mode.VIEW);
		element = new DrugOrderSubmissionElement(context, parameters);
		resultHTML = element.generateHtml(context);
		Assert.assertTrue(resultHTML.length() > 0);
		Assert.assertFalse(resultHTML.indexOf("<input") > 0);
	}
	
	/**
     * @see {@link
     *      DrugOrderSubmissionElement#DrugOrderSubmissionElement(FormEntryContext,Map<QString;
     *      QString;>)}
     */
    @Test
    @Verifies(value = "should return valid drugOrder schema elements", method = "DrugOrderSubmissionElement(FormEntryContext,Map<QString;QString;>)")
    public void DrugOrderSubmissionElement_shouldReturnCorrectSchema() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("drugNames", "Aspirin,Triomune-30");
        parameters.put("discontinuedReasonConceptId", "101");
        parameters.put("discontinueReasonAnswers", "102,103");
        parameters.put("discontinueReasonAnswerLabels", "DRUG1, DRUG2");
        new DrugOrderSubmissionElement(context, parameters);
        List<HtmlFormField> fields = context.getSchema().getAllFields();
        for (HtmlFormField field : fields){
            if (field instanceof DrugOrderField){
                DrugOrderField dof = (DrugOrderField) field;
                Assert.assertTrue(dof.getDrugOrderAnswers().size() == 2);
                Assert.assertTrue(dof.getDiscontinuedReasonQuestion().getConceptId().equals(101));
                Assert.assertTrue(dof.getDrugOrderAnswers().size() == 2);
                ObsFieldAnswer of = dof.getDiscontinuedReasonAnswers().get(0);
                Assert.assertTrue(of.getDisplayName().equals("DRUG1"));
                Assert.assertTrue(of.getConcept().getConceptId().equals(102));
                of = dof.getDiscontinuedReasonAnswers().get(1);
                Assert.assertTrue(of.getDisplayName().equals("DRUG2"));
                Assert.assertTrue(of.getConcept().getConceptId().equals(103));
            }
        }
    }
    
    /**
     * @see {@link
     *      DrugOrderSubmissionElement#DrugOrderSubmissionElement(FormEntryContext,Map<QString;
     *      QString;>)}
     */
    @Test
    @Verifies(value = "should return valid drugOrder schema elements when referenced by mapping", method = "DrugOrderSubmissionElement(FormEntryContext,Map<QString;QString;>)")
    public void DrugOrderSubmissionElementWhenReferencedByMapping_shouldReturnCorrectSchema() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("drugNames", "Aspirin,Triomune-30");
        parameters.put("discontinuedReasonConceptId", "SNOMED CT:7345693");
        parameters.put("discontinueReasonAnswers", "SNOMED CT:2332523,103");
        parameters.put("discontinueReasonAnswerLabels", "DRUG1, DRUG2");
        new DrugOrderSubmissionElement(context, parameters);
        List<HtmlFormField> fields = context.getSchema().getAllFields();
        for (HtmlFormField field : fields){
            if (field instanceof DrugOrderField){
                DrugOrderField dof = (DrugOrderField) field;
                Assert.assertTrue(dof.getDrugOrderAnswers().size() == 2);
                Assert.assertTrue(dof.getDiscontinuedReasonQuestion().getConceptId().equals(5497));
                ObsFieldAnswer of = dof.getDiscontinuedReasonAnswers().get(0);
                Assert.assertTrue(of.getDisplayName().equals("DRUG1"));
                Assert.assertTrue(of.getConcept().getConceptId().equals(5089));
                of = dof.getDiscontinuedReasonAnswers().get(1);
                Assert.assertTrue(of.getDisplayName().equals("DRUG2"));
                Assert.assertTrue(of.getConcept().getConceptId().equals(103));
            }
        }
    }
}
