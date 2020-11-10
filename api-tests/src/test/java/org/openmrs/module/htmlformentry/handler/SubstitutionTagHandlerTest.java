package org.openmrs.module.htmlformentry.handler;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.junit.Test;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;

public class SubstitutionTagHandlerTest {
	
	@Test
	public void testParseBoolean() throws Exception {
		SubstitutionTagHandler handler = new SubstitutionTagHandler() {
			
			@Override
			protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
			        Map<String, String> parameters) throws BadFormDesignException {
				return "";
			}
		};
		
		assertThat(handler.parseBooleanAttribute(null, false), is(false));
		assertThat(handler.parseBooleanAttribute("", false), is(false));
		assertThat(handler.parseBooleanAttribute("true", false), is(true));
		assertThat(handler.parseBooleanAttribute("True", false), is(true));
		assertThat(handler.parseBooleanAttribute("false", false), is(false));
		assertThat(handler.parseBooleanAttribute("FALSE", false), is(false));
		
		assertThat(handler.parseBooleanAttribute(null, true), is(true));
		assertThat(handler.parseBooleanAttribute("", true), is(true));
		assertThat(handler.parseBooleanAttribute("true", false), is(true));
		assertThat(handler.parseBooleanAttribute("false", false), is(false));
	}
	
}
