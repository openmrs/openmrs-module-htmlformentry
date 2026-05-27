package org.openmrs.module.htmlformentry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.context.Context;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Regression tests for the {@code <personAttribute>} tag.
 *
 * <p>Covers String, Concept, and Location attribute types in VIEW, ENTER, and EDIT modes, plus
 * warning behaviour when multiple non-voided attributes exist, and error cases.
 *
 * <p>Standard test data note:
 * <ul>
 *   <li>PersonAttributeType 2 ("Birthplace") – format {@code java.lang.String}</li>
 *   <li>PersonAttributeType 8 ("Civil Status") – format {@code org.openmrs.Concept}</li>
 *   <li>PersonAttributeType 19 ("Birthplace Location") – format {@code org.openmrs.Location}
 *       (added in RegressionTest-data-openmrs-2.8.xml)</li>
 *   <li>Patient 7 has NULL-valued Birthplace and Civil Status attributes; no Birthplace Location
 *       attribute. Used for ENTER/EDIT tests.</li>
 *   <li>Patient 2 has TWO non-voided Civil Status attributes (ids 21 and 22) pre-loaded in the
 *       test dataset; id=22 (value="1001"/PENICILLIN) is the most-recent one.</li>
 * </ul>
 */
public class PersonAttributeTagTest extends BaseHtmlFormEntryTest {

	/** PersonAttributeType 2 – Birthplace – format=java.lang.String */
	private static final String STRING_ATTR_TYPE_UUID = "54fc8400-1683-4d71-a1ac-98d40836ff7c";

	/** PersonAttributeType 8 – Civil Status – format=org.openmrs.Concept */
	private static final String CONCEPT_ATTR_TYPE_UUID = "a0f5521c-dbbd-4c10-81b2-1b7ab18330df";

	/** PersonAttributeType 19 – Birthplace Location – format=org.openmrs.Location */
	private static final String LOCATION_ATTR_TYPE_UUID = "a6e61b61-5f1a-4a19-8a4c-test00000001";

	/** Patient 7 has null-valued attributes and is safe to use for ENTER/EDIT tests. */
	private static final int PATIENT_ID = 7;

	@Before
	public void setUp() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.8.xml");
	}

	// =========================================================================
	// String attribute type tests
	// =========================================================================

	@Test
	public void shouldEnterStringPersonAttribute() throws Exception {
		new RegressionTestHelper() {

			@Override
			public Patient getPatient() {
				return Context.getPatientService().getPatient(PATIENT_ID);
			}

			@Override
			public String getFormName() {
				return "personAttributeStringForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Birthplace:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Birthplace:"), "Paris");
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				Patient patient = Context.getPatientService().getPatient(PATIENT_ID);
				PersonAttributeType type = Context.getPersonService().getPersonAttributeTypeByUuid(STRING_ATTR_TYPE_UUID);
				PersonAttribute attr = patient.getAttribute(type);
				assertNotNull("Expected a Birthplace attribute after ENTER", attr);
				assertEquals("Paris", attr.getValue());
			}
		}.run();
	}

	@Test
	public void shouldDisplayStringPersonAttributeInViewMode() throws Exception {
		new RegressionTestHelper() {

			@Override
			public Patient getPatient() {
				return Context.getPatientService().getPatient(PATIENT_ID);
			}

			@Override
			public String getFormName() {
				return "personAttributeStringForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Birthplace:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Birthplace:"), "Tokyo");
			}

			@Override
			public boolean doViewPatient() {
				return true;
			}

			@Override
			public void testViewingPatient(Patient patient, String html) {
				assertTrue("Expected VIEW HTML to contain the entered value 'Tokyo'", html.contains("Tokyo"));
			}
		}.run();
	}

	@Test
	public void shouldEditStringPersonAttribute() throws Exception {
		new RegressionTestHelper() {

			@Override
			public Patient getPatient() {
				return Context.getPatientService().getPatient(PATIENT_ID);
			}

			@Override
			public String getFormName() {
				return "personAttributeStringForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Birthplace:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Birthplace:"), "Paris");
			}

			@Override
			public boolean doEditEncounter() {
				return true;
			}

			@Override
			public String[] widgetLabelsForEdit() {
				return widgetLabels();
			}

			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// Use setParameter to replace the pre-populated value from the rendered EDIT form.
				request.setParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				request.setParameter(widgets.get("Birthplace:"), "Rome");
			}

			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				Patient patient = Context.getPatientService().getPatient(PATIENT_ID);
				PersonAttributeType type = Context.getPersonService().getPersonAttributeTypeByUuid(STRING_ATTR_TYPE_UUID);
				PersonAttribute attr = patient.getAttribute(type);
				assertNotNull("Expected a Birthplace attribute after EDIT", attr);
				assertEquals("Rome", attr.getValue());
			}
		}.run();
	}

	@Test
	public void shouldVoidStringPersonAttributeWhenBlankValueSubmitted() throws Exception {
		new RegressionTestHelper() {

			@Override
			public Patient getPatient() {
				return Context.getPatientService().getPatient(PATIENT_ID);
			}

			@Override
			public String getFormName() {
				return "personAttributeStringForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Birthplace:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Birthplace:"), "Paris");
			}

			@Override
			public boolean doEditEncounter() {
				return true;
			}

			@Override
			public String[] widgetLabelsForEdit() {
				return widgetLabels();
			}

			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// Use setParameter to replace the pre-populated value from the rendered EDIT form.
				request.setParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				// Submit blank value → should void the existing attribute
				request.setParameter(widgets.get("Birthplace:"), "");
			}

			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				Patient patient = Context.getPatientService().getPatient(PATIENT_ID);
				PersonAttributeType type = Context.getPersonService().getPersonAttributeTypeByUuid(STRING_ATTR_TYPE_UUID);
				PersonAttribute attr = patient.getAttribute(type);
				assertNull("Expected no active Birthplace attribute after blank submission (should be voided)", attr);
			}
		}.run();
	}

	// =========================================================================
	// Concept attribute type tests
	// =========================================================================

	@Test
	public void shouldEnterConceptPersonAttribute() throws Exception {
		new RegressionTestHelper() {

			@Override
			public Patient getPatient() {
				return Context.getPatientService().getPatient(PATIENT_ID);
			}

			@Override
			public String getFormName() {
				return "personAttributeConceptForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Civil Status:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				// Concept 1001 = PENICILLIN (from answerConceptIds in the form)
				request.addParameter(widgets.get("Civil Status:"), "1001");
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				Patient patient = Context.getPatientService().getPatient(PATIENT_ID);
				PersonAttributeType type = Context.getPersonService().getPersonAttributeTypeByUuid(CONCEPT_ATTR_TYPE_UUID);
				PersonAttribute attr = patient.getAttribute(type);
				assertNotNull("Expected a Civil Status attribute after ENTER", attr);
				assertEquals("Stored value should be the concept ID", "1001", attr.getValue());
			}
		}.run();
	}

	@Test
	public void shouldDisplayConceptPersonAttributeNameInViewMode() throws Exception {
		new RegressionTestHelper() {

			@Override
			public Patient getPatient() {
				return Context.getPatientService().getPatient(PATIENT_ID);
			}

			@Override
			public String getFormName() {
				return "personAttributeConceptForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Civil Status:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Civil Status:"), "1001");
			}

			@Override
			public boolean doViewPatient() {
				return true;
			}

			@Override
			public void testViewingPatient(Patient patient, String html) {
				// Concept 1001 is named "PENICILLIN" in test data
				assertTrue("Expected VIEW HTML to contain the concept name 'PENICILLIN'", html.contains("PENICILLIN"));
			}
		}.run();
	}

	@Test
	public void shouldEditConceptPersonAttribute() throws Exception {
		new RegressionTestHelper() {

			@Override
			public Patient getPatient() {
				return Context.getPatientService().getPatient(PATIENT_ID);
			}

			@Override
			public String getFormName() {
				return "personAttributeConceptForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Civil Status:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Civil Status:"), "1001");
			}

			@Override
			public boolean doEditEncounter() {
				return true;
			}

			@Override
			public String[] widgetLabelsForEdit() {
				return widgetLabels();
			}

			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// Use setParameter to replace the pre-populated value from the rendered EDIT form.
				request.setParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				request.setParameter(widgets.get("Civil Status:"), "1002");
			}

			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				Patient patient = Context.getPatientService().getPatient(PATIENT_ID);
				PersonAttributeType type = Context.getPersonService().getPersonAttributeTypeByUuid(CONCEPT_ATTR_TYPE_UUID);
				PersonAttribute attr = patient.getAttribute(type);
				assertNotNull("Expected a Civil Status attribute after EDIT", attr);
				assertEquals("1002", attr.getValue());
			}
		}.run();
	}

	// =========================================================================
	// Location attribute type tests
	// =========================================================================

	@Test
	public void shouldEnterLocationPersonAttribute() throws Exception {
		new RegressionTestHelper() {

			@Override
			public Patient getPatient() {
				return Context.getPatientService().getPatient(PATIENT_ID);
			}

			@Override
			public String getFormName() {
				return "personAttributeLocationForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Birthplace Location:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				// Location 1001 = Kigali
				request.addParameter(widgets.get("Birthplace Location:"), "1001");
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				Patient patient = Context.getPatientService().getPatient(PATIENT_ID);
				PersonAttributeType type = Context.getPersonService().getPersonAttributeTypeByUuid(LOCATION_ATTR_TYPE_UUID);
				PersonAttribute attr = patient.getAttribute(type);
				assertNotNull("Expected a Birthplace Location attribute after ENTER", attr);
				assertEquals("Stored value should be the location ID", "1001", attr.getValue());
			}
		}.run();
	}

	@Test
	public void shouldDisplayLocationPersonAttributeNameInViewMode() throws Exception {
		new RegressionTestHelper() {

			@Override
			public Patient getPatient() {
				return Context.getPatientService().getPatient(PATIENT_ID);
			}

			@Override
			public String getFormName() {
				return "personAttributeLocationForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Birthplace Location:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Birthplace Location:"), "1001");
			}

			@Override
			public boolean doViewPatient() {
				return true;
			}

			@Override
			public void testViewingPatient(Patient patient, String html) {
				// Location 1001 = Kigali
				assertTrue("Expected VIEW HTML to contain the location name 'Kigali'", html.contains("Kigali"));
			}
		}.run();
	}

	@Test
	public void shouldEditLocationPersonAttribute() throws Exception {
		new RegressionTestHelper() {

			@Override
			public Patient getPatient() {
				return Context.getPatientService().getPatient(PATIENT_ID);
			}

			@Override
			public String getFormName() {
				return "personAttributeLocationForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Birthplace Location:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Birthplace Location:"), "1001");
			}

			@Override
			public boolean doEditEncounter() {
				return true;
			}

			@Override
			public String[] widgetLabelsForEdit() {
				return widgetLabels();
			}

			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// Use setParameter to replace the pre-populated value from the rendered EDIT form.
				request.setParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				// Change to location 1002 = Mirebalais
				request.setParameter(widgets.get("Birthplace Location:"), "1002");
			}

			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				Patient patient = Context.getPatientService().getPatient(PATIENT_ID);
				PersonAttributeType type = Context.getPersonService().getPersonAttributeTypeByUuid(LOCATION_ATTR_TYPE_UUID);
				PersonAttribute attr = patient.getAttribute(type);
				assertNotNull("Expected a Birthplace Location attribute after EDIT", attr);
				assertEquals("1002", attr.getValue());
			}
		}.run();
	}

	/**
	 * When the {@code tags} attribute is specified on a Location-type personAttribute, only locations
	 * tagged with the specified tag(s) should appear in the dropdown. This form omits
	 * {@code <encounterLocation/>} to avoid its full location list contaminating the assertions.
	 */
	@Test
	public void shouldFilterLocationDropdownByLocationTag() throws Exception {
		new RegressionTestHelper() {

			@Override
			public Patient getPatient() {
				return Context.getPatientService().getPatient(PATIENT_ID);
			}

			@Override
			public String getFormName() {
				return "personAttributeLocationFilterOnlyForm";
			}

			@Override
			public void testBlankFormHtml(String html) {
				// Locations tagged with "Some Tag": 1001 = Kigali, 1002 = Mirebalais
				assertTrue("Expected Kigali (tagged 'Some Tag') in dropdown", html.contains("Kigali"));
				assertTrue("Expected Mirebalais (tagged 'Some Tag') in dropdown", html.contains("Mirebalais"));
				// Locations with ONLY "Another Tag": Boston (1004), Scituate (1005) must NOT appear
				assertFalse("Expected Boston (not tagged 'Some Tag') to be absent", html.contains("Boston"));
				assertFalse("Expected Scituate (not tagged 'Some Tag') to be absent", html.contains("Scituate"));
			}
		}.run();
	}

	// =========================================================================
	// Multiple non-voided attributes – warning and most-recent selection
	// =========================================================================

	/**
	 * When a patient has more than one non-voided attribute of the same type, the element should
	 * render successfully (logging a warning) and use the most recently created attribute.
	 *
	 * <p>Patient 2 has two Civil Status attributes pre-loaded in the test dataset:
	 * <ul>
	 *   <li>id=21: value="6", date_created=2020-01-01 (older)</li>
	 *   <li>id=22: value="1001" (PENICILLIN), date_created=2024-01-01 (more recent)</li>
	 * </ul>
	 */
	@Test
	public void shouldUseMostRecentAttributeWhenMultipleNonVoidedExistInViewMode() throws Exception {
		// Patient 2 already has two non-voided Civil Status attributes in the test dataset.
		// The most-recent one (id=22, value="1001"/PENICILLIN) should be displayed in VIEW mode.
		final Patient viewPatient = Context.getPatientService().getPatient(2);

		new RegressionTestHelper() {

			/** Use patient 7 for the required ENTER phase; patient 2 for VIEW. */
			@Override
			public Patient getPatient() {
				return Context.getPatientService().getPatient(PATIENT_ID);
			}

			@Override
			public Patient getPatientToView() {
				return viewPatient;
			}

			@Override
			public String getFormName() {
				return "personAttributeConceptForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Civil Status:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// A valid ENTER for patient 7; the Civil Status attribute is intentionally left blank
				// so patient 7's data is not affected.
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Civil Status:"), "");
			}

			@Override
			public void testViewingPatient(Patient patient, String html) {
				// The most-recent attribute (value "1001" / PENICILLIN) should be displayed.
				// The form must render without exception even with two active attributes.
				assertNotNull("Expected non-null HTML when multiple attributes exist", html);
				assertTrue("Expected most-recent attribute name 'PENICILLIN' in VIEW HTML",
				    html.contains("PENICILLIN"));
			}
		}.run();
	}

	/**
	 * When a patient has multiple non-voided attributes of the same type and the form is edited,
	 * the most-recently-created attribute should be the one updated.
	 *
	 * <p>Patient 2 has two Civil Status attributes pre-loaded in the test dataset:
	 * <ul>
	 *   <li>id=21: value="6", date_created=2020-01-01 (older)</li>
	 *   <li>id=22: value="1001" (PENICILLIN), date_created=2024-01-01 (more recent)</li>
	 * </ul>
	 */
	@Test
	public void shouldUseMostRecentAttributeWhenMultipleNonVoidedExistInEditMode() throws Exception {
		// Patient 2 already has two non-voided Civil Status attributes in the test dataset.
		final PersonAttributeType civilStatusType = Context.getPersonService().getPersonAttributeType(8);
		final Patient patient2 = Context.getPatientService().getPatient(2);

		new RegressionTestHelper() {

			/** Use patient 2 (with multiple attributes) for the whole test. */
			@Override
			public Patient getPatient() {
				return patient2;
			}

			@Override
			public String getFormName() {
				return "personAttributeConceptForm";
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Civil Status:" };
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// ENTER: update the most-recent attribute (value="1001") to "1002"
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Civil Status:"), "1002");
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				// Most-recent attribute should now be "1002"
				Patient patient = Context.getPatientService().getPatient(2);
				boolean found = patient.getActiveAttributes().stream()
				        .filter(a -> a.getAttributeType().equals(civilStatusType))
				        .anyMatch(a -> "1002".equals(a.getValue()));
				assertTrue("Expected the most-recent attribute to be updated to '1002'", found);
			}

			@Override
			public boolean doEditEncounter() {
				return true;
			}

			@Override
			public String[] widgetLabelsForEdit() {
				return widgetLabels();
			}

			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// EDIT: update the most-recent attribute (now "1002") to "1003".
				// Use setParameter to replace the pre-populated value from the rendered EDIT form.
				request.setParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.setParameter(widgets.get("Location:"), "2");
				request.setParameter(widgets.get("Provider:"), "502");
				request.setParameter(widgets.get("Civil Status:"), "1003");
			}

			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				Patient patient = Context.getPatientService().getPatient(2);
				boolean found = patient.getActiveAttributes().stream()
				        .filter(a -> a.getAttributeType().equals(civilStatusType))
				        .anyMatch(a -> "1003".equals(a.getValue()));
				assertTrue("Expected the most-recent attribute to be updated to '1003' after EDIT", found);
			}
		}.run();
	}

	// =========================================================================
	// Form design error cases
	// =========================================================================

	/**
	 * When an unknown {@code attributeType} UUID is provided, the framework renders a form-design
	 * error div rather than propagating the exception (this is the standard HFE behaviour for
	 * {@link BadFormDesignException} thrown from a tag handler).
	 */
	@Test
	public void shouldRenderFormDesignErrorForUnknownAttributeTypeUuid() throws Exception {
		String xml = "<htmlform><encounterDate/><encounterLocation/><encounterProvider/>"
		        + "<personAttribute attributeType=\"non-existent-uuid-000000\"/></htmlform>";
		Patient patient = Context.getPatientService().getPatient(PATIENT_ID);
		FormEntrySession session = new FormEntrySession(patient, xml, null);
		String html = session.getHtmlToDisplay();
		assertTrue("Expected form-design error to be rendered in HTML", html.contains("error"));
	}

	/**
	 * A Concept-type PersonAttributeType without the mandatory {@code answerConceptIds} attribute
	 * results in a rendered form-design error.
	 */
	@Test
	public void shouldRenderFormDesignErrorForConceptTypeWithoutAnswerConceptIds() throws Exception {
		// Civil Status (type 8) has format=org.openmrs.Concept; answerConceptIds is mandatory
		String xml = "<htmlform><encounterDate/><encounterLocation/><encounterProvider/>"
		        + "<personAttribute attributeType=\"" + CONCEPT_ATTR_TYPE_UUID + "\"/></htmlform>";
		Patient patient = Context.getPatientService().getPatient(PATIENT_ID);
		FormEntrySession session = new FormEntrySession(patient, xml, null);
		String html = session.getHtmlToDisplay();
		assertTrue("Expected form-design error to be rendered in HTML", html.contains("error"));
	}

	/**
	 * A {@code <personAttribute>} tag without any {@code attributeType} attribute results in a
	 * rendered form-design error.
	 */
	@Test
	public void shouldRenderFormDesignErrorWhenAttributeTypeAttributeIsMissing() throws Exception {
		String xml = "<htmlform><encounterDate/><encounterLocation/><encounterProvider/>"
		        + "<personAttribute/></htmlform>";
		Patient patient = Context.getPatientService().getPatient(PATIENT_ID);
		FormEntrySession session = new FormEntrySession(patient, xml, null);
		String html = session.getHtmlToDisplay();
		assertTrue("Expected form-design error to be rendered in HTML", html.contains("error"));
	}

	// =========================================================================
	// Helper
	// =========================================================================

	private static void assertFalse(String message, boolean condition) {
		assertTrue(message, !condition);
	}
}
