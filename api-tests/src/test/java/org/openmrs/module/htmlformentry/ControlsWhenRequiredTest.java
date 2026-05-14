package org.openmrs.module.htmlformentry;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Date;
import java.util.Map;

/**
 * Tests that fields hidden by &lt;controls&gt;/&lt;when thenDisplay&gt; are exempt from required
 * validation, and that required validation still fires when the field is visible.
 */
public class ControlsWhenRequiredTest extends BaseHtmlFormEntryTest {

	@Before
	public void loadData() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.8.xml");
	}

	/**
	 * When the controlling obs value does not match the thenDisplay condition the section is hidden
	 * client-side and the JS populates hfeHiddenFields with the widget name.  The server should skip
	 * required validation for those fields.
	 */
	@Test
	public void requiredFieldHiddenByControls_shouldNotProduceValidationError() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "obsWithControlsAndRequired";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Allergy:", "Details:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				// Allergy is left blank — section #allergy-followup stays hidden.
				// Simulate what the JS sends: the hidden field's widget name in hfeHiddenFields.
				request.addParameter("hfeHiddenFields", widgets.get("Details:"));
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
			}
		}.run();
	}

	/**
	 * When the controlling obs value matches the thenDisplay condition the section is shown.  The
	 * required field inside it must be filled; leaving it empty should produce a validation error.
	 */
	@Test
	public void requiredFieldVisibleByControls_withNoValue_shouldProduceValidationError() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "obsWithControlsAndRequired";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Allergy:", "Details:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				// CATS (1002) triggers the thenDisplay — section is visible.
				request.addParameter(widgets.get("Allergy:"), "1002");
				// Details field is visible but intentionally left empty.
				// hfeHiddenFields is not set for the Details widget.
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertErrors(1);
				results.assertNoEncounterCreated();
			}
		}.run();
	}

	/**
	 * When the controlling obs value matches the thenDisplay condition and the required field is
	 * filled, the form should submit successfully and save both obs.
	 */
	@Test
	public void requiredFieldVisibleByControls_withValue_shouldSucceed() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {

			@Override
			public String getFormName() {
				return "obsWithControlsAndRequired";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Allergy:", "Details:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				// CATS (1002) triggers the thenDisplay — section is visible.
				request.addParameter(widgets.get("Allergy:"), "1002");
				// Required Details field is filled.
				request.addParameter(widgets.get("Details:"), "Follow-up details here");
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertObsCreated(1000, Context.getConceptService().getConcept(1002));
				results.assertObsCreated(60000, "Follow-up details here");
			}
		}.run();
	}
}
