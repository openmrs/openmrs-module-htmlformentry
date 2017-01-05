package org.openmrs.module.htmlformentry.element;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

public class PatientDetailSubmissionElementTest extends BaseModuleContextSensitiveTest {

	@Test
	public void testPatientIdentifierValidation() throws Exception {
		// <patient field="identifier" identifierTypeId="1" />

		FormEntryContext context = new FormEntryContext(FormEntryContext.Mode.ENTER);

		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("field", "identifier");
		attributes.put("identifierTypeId", "1");
		PatientDetailSubmissionElement element = new PatientDetailSubmissionElement(context, attributes);

		String html = element.generateHtml(context);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("w1", "invalid-identifier");
		request.addParameter("w3", "1"); // this is the hidden input for identifier type

		Collection<FormSubmissionError> errors = element.validateSubmission(context, request);
		assertThat(errors.size(), is(1));
		assertThat(errors.iterator().next().getId(), is("w2")); // the error widget for w1
		assertThat(errors.iterator().next().getError(), is("PatientIdentifier.error.unallowedIdentifier"));

		request.setParameter("w1", "1-1");
		errors = element.validateSubmission(context, request);
		assertThat(errors.size(), is(1));
		assertThat(errors.iterator().next().getId(), is("w2")); // the error widget for w1
		assertThat(errors.iterator().next().getError(), is("PatientIdentifier.error.checkDigitWithParameter"));

		request.setParameter("w1", "1-8"); // TODO make sure this is a valid identifier with check digit
		errors = element.validateSubmission(context, request);
		assertThat(errors.size(), is(0));
	}

}