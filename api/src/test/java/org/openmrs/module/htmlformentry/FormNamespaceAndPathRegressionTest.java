package org.openmrs.module.htmlformentry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.springframework.mock.web.MockHttpServletRequest;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FormNamespaceAndPathRegressionTest extends BaseHtmlFormEntryTest {
	
	@Before
	public void loadData() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.8.xml");
	}
	
	@Test
	public void testSingleObsFormWithControlIdSavesNamespaceAndPath() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsFormWithControlId";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Weight:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Weight:"), "70");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				Encounter encounter = results.getEncounterCreated();
				Assert.assertEquals(1, encounter.getObs().size());
				Obs obs = encounter.getObs().iterator().next();
				Assert.assertEquals("HtmlFormEntry", obs.getFormFieldNamespace());
				// note that "FakeForm and 2.0" are just hardcoded into the Regression Test Helper context setup
				Assert.assertEquals("FakeForm.2.0/weight-0", obs.getFormFieldPath());
			}
		}.run();
	}
	
	@Test
	public void testSingleObsFormWithoutControlIdShouldNotSaveNamespaceAndPath() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Weight:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Weight:"), "70");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				Encounter encounter = results.getEncounterCreated();
				Assert.assertEquals(1, encounter.getObs().size());
				Obs obs = encounter.getObs().iterator().next();
				Assert.assertNull(obs.getFormFieldNamespace());
				Assert.assertNull(obs.getFormFieldPath());
			}
		}.run();
	}
	
	@Test
	public void testSingleObsGroupWithoutControlIdShouldNotCreateNamespaceAndPath() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "singleObsGroupForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Weight:", "Allergy:", "Allergy Date:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Allergy:"), "Bee stings");
				request.addParameter(widgets.get("Allergy Date:"), dateAsString(date));
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				Encounter encounter = results.getEncounterCreated();
				Obs obs = encounter.getObsAtTopLevel(false).iterator().next();
				Assert.assertNull(obs.getFormFieldPath());
				Assert.assertEquals(2, obs.getGroupMembers().size());
				Iterator<Obs> i = obs.getGroupMembers().iterator();
				while (i.hasNext()) {
					Obs childObs = i.next();
					Assert.assertNull(childObs.getFormFieldPath());
				}
			}
		}.run();
	}
	
	@Test
	public void testDoubleObsGroupFormWithControlIdSavesNamespaceAndPath() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "doubleObsGroupFormWithControlId";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Allergy 1:", "Allergy 1 Date:", "Allergy 2:",
				        "Allergy 2 Date:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter(widgets.get("Allergy 1:"), "Bee stings");
				request.addParameter(widgets.get("Allergy 1 Date:"), dateAsString(date));
				request.addParameter(widgets.get("Allergy 2:"), "Lactose");
				request.addParameter(widgets.get("Allergy 2 Date:"), dateAsString(date));
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				Encounter encounter = results.getEncounterCreated();
				Assert.assertEquals(4, encounter.getObs().size());
				
				List<String> formFieldPaths = encounter.getObs().stream().map(Obs::getFormFieldPath)
				        .collect(Collectors.toList());
				
				// note that "FakeForm and 2.0" are just hardcoded into the Regression Test Helper context setup
				Assert.assertTrue(formFieldPaths.contains("FakeForm.2.0/allergy-1-0"));
				Assert.assertTrue(formFieldPaths.contains("FakeForm.2.0/allergy-2-0"));
				Assert.assertTrue(formFieldPaths.contains("FakeForm.2.0/allergy-date-1-0"));
				Assert.assertTrue(formFieldPaths.contains("FakeForm.2.0/allergy-date-2-0"));
				
			}
		}.run();
	}
	
	@Test
	public void testDoubleObsGroupFormWithControlIdCorrectlyReloadsObs() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "doubleObsGroupFormWithControlId";
			}
			
			@Override
			public Encounter getEncounterToView() throws Exception {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				Date date = Context.getDateFormat().parse("01/02/2003");
				e.setDateCreated(new Date());
				e.setEncounterType(Context.getEncounterService().getEncounterType(1));
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
				// first create the second allergies obs group
				Obs allergy2Parent = TestUtil.createObs(e, 70000, null, date);
				e.addObs(allergy2Parent);
				Obs allergy2 = TestUtil.createObs(e, 80000, "Lactose", date);
				allergy2.setFormField(null, "FakeForm.2.0/allergy-2-0");
				allergy2Parent.addGroupMember(allergy2);
				
				// then create the first allergies obs group
				Obs allergy1Parent = TestUtil.createObs(e, 70000, null, date);
				e.addObs(allergy1Parent);
				Obs allergy1 = TestUtil.createObs(e, 80000, "Bee stings", date);
				allergy1.setFormField(null, "FakeForm.2.0/allergy-1-0");
				allergy1Parent.addGroupMember(allergy1);
				
				e = Context.getEncounterService().saveEncounter(e);
				return e;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyContains("Allergy 1:Bee stings", html);
				TestUtil.assertFuzzyContains("Allergy 2:Lactose", html);
			}
		}.run();
	}
	
	@Test
	public void testDoubleObsGroupFormWithControlIdCorrectlyEditsObs() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "doubleObsGroupFormWithControlId";
			}
			
			@Override
			public Encounter getEncounterToEdit() {
				Encounter e = new Encounter();
				e.setPatient(getPatient());
				Date date;
				try {
					date = Context.getDateFormat().parse("01/02/2003");
				}
				catch (Exception ex) {
					throw new APIException();
				}
				
				e.setDateCreated(new Date());
				e.setEncounterType(Context.getEncounterService().getEncounterType(1));
				e.setEncounterDatetime(date);
				e.setLocation(Context.getLocationService().getLocation(2));
				e.addProvider(Context.getEncounterService().getEncounterRole(1),
				    Context.getProviderService().getProvider(1));
				
				// first create the second allergies obs group
				Obs allergy2Parent = TestUtil.createObs(e, 70000, null, date);
				e.addObs(allergy2Parent);
				Obs allergy2 = TestUtil.createObs(e, 80000, "Lactose", date);
				allergy2.setFormField(null, "FakeForm.2.0/allergy-2-0");
				allergy2Parent.addGroupMember(allergy2);
				
				// then create the first allergies obs group
				Obs allergy1Parent = TestUtil.createObs(e, 70000, null, date);
				e.addObs(allergy1Parent);
				Obs allergy1 = TestUtil.createObs(e, 80000, "Bee stings", date);
				allergy1.setFormField(null, "FakeForm.2.0/allergy-1-0");
				allergy1Parent.addGroupMember(allergy1);
				
				e = Context.getEncounterService().saveEncounter(e);
				return e;
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Allergy 1:", "Allergy 2:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.setParameter(widgets.get("Allergy 1:"), "Wasp stings");
				request.setParameter(widgets.get("Allergy 2:"), "Milk");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				results.assertNoErrors();
				Encounter encounter = results.getEncounterCreated();
				Assert.assertEquals(2, encounter.getObs().size());
				
				List<String> obsValues = encounter.getObs().stream().map(Obs::getValueText).collect(Collectors.toList());
				Assert.assertTrue(obsValues.contains("Wasp stings"));
				Assert.assertTrue(obsValues.contains("Milk"));
				
				List<String> formFieldPaths = encounter.getObs().stream().map(Obs::getFormFieldPath)
				        .collect(Collectors.toList());
				
				// testing that the form field paths were preserved, see https://openmrs.atlassian.net/browse/HTML-841
				// note that "FakeForm and 2.0" are just hardcoded into the Regression Test Helper context setup
				Assert.assertTrue(formFieldPaths.contains("FakeForm.2.0/allergy-1-0"));
				Assert.assertTrue(formFieldPaths.contains("FakeForm.2.0/allergy-2-0"));
			}
			
		}.run();
	}
	
	private BufferedImage createImage() {
		int width = 10;
		int height = 10;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		WritableRaster raster = image.getRaster();
		int[] colorArray = new int[3];
		int h = 255;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				if (i == 0 || j == 0 || i == width - 1 || j == height - 1
				        || (i > width / 3 && i < 2 * width / 3) && (j > height / 3 && j < 2 * height / 3)) {
					colorArray[0] = h;
					colorArray[1] = h;
					colorArray[2] = 0;
				} else {
					colorArray[0] = 0;
					colorArray[1] = 0;
					colorArray[2] = h;
				}
				raster.setPixel(i, j, colorArray);
			}
		}
		
		return image;
	}
}
