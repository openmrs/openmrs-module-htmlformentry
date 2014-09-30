package org.openmrs.module.htmlformentry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openmrs.Drug;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DirtiesContext
public class StandardRegimenElement1_10Test extends BaseModuleContextSensitiveTest {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	protected static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	protected static final String XML_REGRESSION_TEST_DATASET = "regressionTestDataSet";
	
	protected static final String XML_DRUG_ORDER_ELEMENT_DATASET = "drugOrderElementDataSet";
	
	protected static final String XML_HTML_FORM_ENTRY_REGIMEN_UTIL_TEST_DATASET = "RegimenUtils1_10Test.xml";
	
	@Before
	public void setupDatabase() throws Exception {
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_REGRESSION_TEST_DATASET));
		executeDataSet(XML_DATASET_PATH + new TestUtil().getTestDatasetFilename(XML_DRUG_ORDER_ELEMENT_DATASET));
	}

    private int orderNumberCounter = 1;

    @Before
    public void setUp() throws Exception {
        String xml = (new TestUtil()).loadXmlFromFile(XML_HTML_FORM_ENTRY_REGIMEN_UTIL_TEST_DATASET);

        // used to avoid lock timeout on GP#order.nextOrderNumberSeed
        AdministrationService administrationService = mock(AdministrationService.class);

        when(administrationService.getGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_STANDARD_DRUG_REGIMENS))
                .thenReturn(xml);

        when(administrationService.getGlobalProperty("")).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                return "" + orderNumberCounter++;
            }
        });

        ServiceContext.getInstance().setAdministrationService(administrationService);
    }

	@Test
	public void testStandardRegimenTag_shouldCreateAndEditStandardRegimen() throws Exception {
		new RegressionTestHelper() {

			final Date date = new Date();

			private Encounter encounter;

			@Override
			public String getFormName() {
				return "standardRegimenTestForm";
			}

			@Override
			public Patient getPatient() {
				return Context.getPatientService().getPatient(6);
			}

			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:" };

			}

			@Override
			public void testBlankFormHtml(String html) {
			}

			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				//w7 is the standardRegimen tag
				request.addParameter("w7", "all3");
				//start date
				request.addParameter("w9", dateAsString(date));
				//care setting
				request.addParameter("w15", "2");
			}

			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				encounter = results.getEncounterCreated();

				Drug drug2 = Context.getConceptService().getDrug(2);
				Drug drug3 = Context.getConceptService().getDrug(3);
				Drug drug11 = Context.getConceptService().getDrug(11);

				assertThat(
				    encounter.getOrders(),
				    containsInAnyOrder(
				        allOf(hasProperty("drug", is(drug2)), hasProperty("dose", is(1.0)),
				            hasProperty("dateActivated", is(ymdToDate(dateAsString(date))))),
				        allOf(hasProperty("drug", is(drug3)), hasProperty("dose", is(1.0)),
				            hasProperty("dateActivated", is(ymdToDate(dateAsString(date))))),
				        allOf(hasProperty("drug", is(drug11)), hasProperty("dose", is(1.0)),
				            hasProperty("dateActivated", is(ymdToDate(dateAsString(date)))))));
			}

			@Override
			public Encounter getEncounterToEdit() {
				return encounter;
			}

			@Override
			public void testEditFormHtml(String html) {
				System.out.println(html);
			}

			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				//w7 is the standardRegimen tag
				request.setParameter("w7", "drug2and3");
				//start date
				request.setParameter("w9", dateAsString(date));
				//care setting
				request.setParameter("w15", "2");
			}

			public void testEditedResults(SubmissionResults results) {
				Encounter editedEncounter = results.getEncounterCreated();

				Drug drug2 = Context.getConceptService().getDrug(2);
				Drug drug3 = Context.getConceptService().getDrug(3);

				Set<Order> orders = new HashSet<Order>(editedEncounter.getOrders());
				for (Iterator<Order> it = orders.iterator(); it.hasNext();) {
					Order order = it.next();
					if (order.isVoided()) {
						it.remove();
					}
				}

				assertThat(
				    orders,
				    containsInAnyOrder(
				        allOf(hasProperty("drug", is(drug2)), hasProperty("dose", is(1.0)),
				            hasProperty("dateActivated", is(ymdToDate(dateAsString(date))))),
				        allOf(hasProperty("drug", is(drug3)), hasProperty("dose", is(200.0)),
				            hasProperty("dateActivated", is(ymdToDate(dateAsString(date)))))));
			}
		}.run();
	}
}
