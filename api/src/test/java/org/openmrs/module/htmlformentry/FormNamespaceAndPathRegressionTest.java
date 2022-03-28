package org.openmrs.module.htmlformentry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.springframework.mock.web.MockHttpServletRequest;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

public class FormNamespaceAndPathRegressionTest extends BaseHtmlFormEntryTest {
	
	@Before
	public void loadData() throws Exception {
		executeVersionedDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.3.xml");
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
