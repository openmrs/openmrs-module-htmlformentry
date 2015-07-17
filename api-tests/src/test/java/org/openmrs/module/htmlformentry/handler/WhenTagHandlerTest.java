package org.openmrs.module.htmlformentry.handler;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class WhenTagHandlerTest {

	private FormSubmissionController submissionController;
	private FormEntrySession formEntrySession;
    private FormEntryContext formEntryContext;
    private HtmlFormSchema htmlFormSchema;
    private Map <String, String> attributes = new HashMap<String, String>();
    private WhenTagHandler handler = new WhenTagHandler();

	@Before
	public void setUp() throws Exception {

		htmlFormSchema = new HtmlFormSchema();

		formEntryContext = mock(FormEntryContext.class);
		when(formEntryContext.getSchema()).thenReturn(htmlFormSchema);

		formEntrySession = mock(FormEntrySession.class);
		when(formEntrySession.getContext()).thenReturn(formEntryContext);
		
		submissionController = mock(FormSubmissionController.class);
		when(formEntrySession.getSubmissionController()).thenReturn(submissionController);
		
		attributes.put("value", "true");
	}

	
	@Test
	public void testBooleanValue() {
		
		
		try {
			handler.getSubstitution(formEntrySession, submissionController, attributes);
		} catch (BadFormDesignException e) {
			fail("should not fail");
			e.printStackTrace();
		}
		
		
	}

}
