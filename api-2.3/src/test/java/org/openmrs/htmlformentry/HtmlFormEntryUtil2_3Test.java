package org.openmrs.htmlformentry;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.Obs;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil2_3;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_NAMESPACE;

@RunWith(PowerMockRunner.class)
public class HtmlFormEntryUtil2_3Test {
	
	@Test
	public void getControlId_shouldReturnControlId() {
		
		// Prepare parameters
		Obs observation = new Obs();
		observation.setFormField(FORM_NAMESPACE, "MyForm.1.0/my_condition_tag-0");

		// Test
		String controlId = HtmlFormEntryUtil2_3.getControlId(observation);
		
		// Validation
		Assert.assertEquals("my_condition_tag", controlId);
	}

	@Test
	public void getControlId_shouldReturnControlIdWithMoreThanOneDash() {

		// Prepare parameters
		Obs observation = new Obs();
		observation.setFormField(FORM_NAMESPACE, "MyForm.1.0/my-condition-tag-0");

		// Test
		String controlId = HtmlFormEntryUtil2_3.getControlId(observation);

		// Validation
		Assert.assertEquals("my-condition-tag", controlId);
	}
	
	@Test(expected = IllegalStateException.class)
	public void getControlId_shouldThrowIllegalStateException() {
		
		// Prepare parameters
		Obs observation = new Obs();
		
		// Test
		String controlId = HtmlFormEntryUtil2_3.getControlId(observation);
		
	}
}
