package org.openmrs.module.htmlformentry.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.htmlformentry.BaseHtmlFormEntryTest;
import org.openmrs.module.htmlformentry.schema.DrugOrderField;
import org.openmrs.module.htmlformentry.tester.FormSessionTester;
import org.openmrs.module.htmlformentry.tester.FormTester;

public class DrugOrderTagHandlerTest extends BaseHtmlFormEntryTest {
	
	private static final Log log = LogFactory.getLog(DrugOrderTagHandlerTest.class);
	
	@Before
	public void setupDatabase() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
	}

	@Test
	public void shouldSupportLegacyDrugAttributes() throws Exception {
		FormTester formTester = FormTester.buildForm("drugOrderTestFormLegacyNamesAndLabels.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(6);
		List<DrugOrderField> drugOrderFields = formSessionTester.getFields(DrugOrderField.class);
		assertThat(drugOrderFields.size(), is(1));
		DrugOrderField field = drugOrderFields.get(0);
		assertThat(field.getDrugOrderAnswers().size(), is(2));
		assertThat(field.getDrugOrderAnswers().get(0).getDrug().getDrugId(), is(2));
		assertThat(field.getDrugOrderAnswers().get(0).getDisplayName(), is("TRIOMUNE 30 Label"));
		assertThat(field.getDrugOrderAnswers().get(1).getDrug().getDrugId(), is(3));
		assertThat(field.getDrugOrderAnswers().get(1).getDisplayName(), is("Aspirin Label"));
	}

	@Test
	public void shouldSupportLegacyDiscontinueReasonAttribute() throws Exception {
		FormTester formTester = FormTester.buildForm("drugOrderTestFormLegacyDiscontinueReason.xml");
		FormSessionTester formSessionTester = formTester.openNewForm(6);
		List<DrugOrderField> drugOrderFields = formSessionTester.getFields(DrugOrderField.class);
		assertThat(drugOrderFields.size(), is(2));

		DrugOrderField field1 = drugOrderFields.get(0);
		assertThat(field1.getDrugOrderAnswers().size(), is(1));
		assertThat(field1.getDiscontinuedReasonQuestion().getConceptId(), is(555));
		assertThat(field1.getDiscontinuedReasonAnswers().size(), is(2));
		assertThat(field1.getDiscontinuedReasonAnswers().get(0).getConcept().getConceptId(), is(556));
		assertThat(field1.getDiscontinuedReasonAnswers().get(0).getDisplayName(), is("TOXICITY"));
		assertThat(field1.getDiscontinuedReasonAnswers().get(1).getConcept().getConceptId(), is(557));
		assertThat(field1.getDiscontinuedReasonAnswers().get(1).getDisplayName(), is("TRANSFERRED OUT"));

		DrugOrderField field2 = drugOrderFields.get(1);
		assertThat(field2.getDrugOrderAnswers().size(), is(1));
		assertThat(field2.getDiscontinuedReasonQuestion().getConceptId(), is(555));
		assertThat(field2.getDiscontinuedReasonAnswers().size(), is(2));
		assertThat(field2.getDiscontinuedReasonAnswers().get(0).getConcept().getConceptId(), is(16));
		assertThat(field2.getDiscontinuedReasonAnswers().get(0).getDisplayName(), is("Died Label"));
		assertThat(field2.getDiscontinuedReasonAnswers().get(1).getConcept().getConceptId(), is(22));
		assertThat(field2.getDiscontinuedReasonAnswers().get(1).getDisplayName(), is("Unknown Label"));
	}
	
	@Test
	public void shouldEnableDrugConfiguration() throws Exception {
		/*
		TODO:
		
		Should be able to largely test that this parses XML correctly into schema

		- Should parse various XML configurations into appropriate DrugOrderWidgetConfig objects
		- handle orderProperty, orderTemplate, various attributes, nested html in the template
		- should populate the DrugOrderField object appropriately with any configured metadata options
		- Should construct a DrugOrderWidget
		- Should construct a DrugOrderSubmissionElement and add it to the Session
		- Should render the html from the DrugOrderSubmissionElement
		- for each orderProperty
		- should require a name (BadFormDesignException)
		- should take in attributes
		- should take in options, each must have a value attribute specified (BadFormDesignException)
		- value should be able to be an id, uuid, sometimes a mapping
		- each option can have an optional label
		- each label should be either rendered as is, or translated if it is a message code
		- should get either all options, or the configured set of options, appropriate for the property
		- urgency should default to ROUTINE
		- enum should have a translated label
		- order type should default to HtmlFormEntryUtil.drugOrderType()
		- BadFormDesignException if any of doseUnits, route, durationUnits, quantityUnits are not among allowed concepts
		- Should add all of the metadata to the schema and populate DrugOrderField correctly
		
		*/
		
		/*
		TODO: Here, or in another test:
		
		AttributeDescriptor stuff and MDS / substitutions / dependencies:
		- DrugOrderTagHandler.createAttributeDescriptors.  Support Substitution and Dependencies for drugOrder tag
		 */
	}
}
