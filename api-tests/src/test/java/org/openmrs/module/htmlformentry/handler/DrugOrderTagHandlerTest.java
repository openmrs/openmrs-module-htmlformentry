package org.openmrs.module.htmlformentry.handler;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.htmlformentry.BaseHtmlFormEntryTest;

public class DrugOrderTagHandlerTest extends BaseHtmlFormEntryTest {
	
	private static final Log log = LogFactory.getLog(DrugOrderTagHandlerTest.class);
	
	@Before
	public void setupDatabase() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.1.xml");
	}
	
	@Test
	public void shouldEnableDrugConfiguration() throws Exception {
		/*
		TODO:
		
		Should be able to largely test that this parses XML correctly into schema
		
		- Should handle all legacy options correctly and not require any body tags
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
