package org.openmrs.module.htmlformentry;

import static org.hamcrest.CoreMatchers.is;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

public class ComplexObsTagTest extends BaseHtmlFormEntryTest {
	
	private File file;
	
	@Before
	public void setup() throws Exception {
		executeDataSet("org/openmrs/api/include/ObsServiceTest-complex.xml");
		
		file = File.createTempFile("test-image", ".png");
		FileUtils.copyURLToFile(
		    getClass().getClassLoader().getResource("org/openmrs/module/htmlformentry/include/test-image.png"), file);
	}
	
	@Test
	public void shouldSaveComplexObsWithImageHandler() throws Exception {
		
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "complexObsForm";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Upload:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "1");
				
				String ulWidgetHtmlName = widgets.get("Upload:");
				((MockMultipartHttpServletRequest) request).addFile(
				    new MockFileInputStreamMultipartFile(ulWidgetHtmlName, "test-image.png", "image/png", file));
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				Encounter e = results.getEncounterCreated();
				Assert.assertNotNull(e);
				
				List<Obs> obs = new ArrayList<Obs>(e.getAllObs());
				Assert.assertThat(obs.size(), is(1));
				
				Obs o = obs.get(0);
				String valueComplex = o.getValueComplex();
				Assert.assertTrue(valueComplex.startsWith("png image |test-image"));
				Assert.assertTrue(valueComplex.endsWith(".png"));
			}
			
		}.run();
	}
	
}
