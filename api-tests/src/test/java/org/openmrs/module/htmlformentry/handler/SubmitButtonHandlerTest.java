package org.openmrs.module.htmlformentry.handler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class SubmitButtonHandlerTest {

    private FormEntrySession session;

    private FormEntryContext context;

    private FormSubmissionController formSubmissionController;

    private MessageSourceService messageSourceService;

    private SubmitButtonHandler submitButtonHandler = new SubmitButtonHandler();

    @Before
    public void setup() {

        session = mock(FormEntrySession.class);
        formSubmissionController = mock(FormSubmissionController.class);
        messageSourceService = mock(MessageSourceService.class);
        context = mock(FormEntryContext.class);

        when(session.getContext()).thenReturn(context);

        when(messageSourceService.getMessage("htmlformentry.saveChangesButton")).thenReturn("Save");
        when(messageSourceService.getMessage("htmlformentry.enterFormButton")).thenReturn("Enter");

        PowerMockito.mockStatic(Context.class);
        PowerMockito.when(Context.getMessageSourceService()).thenReturn(messageSourceService);

    }

    @Test
    public void getSubstitution_shouldGenerateProperHtmlInViewMode() {
        when(context.getMode()).thenReturn(FormEntryContext.Mode.VIEW);
        String html = submitButtonHandler.getSubstitution(session, formSubmissionController, new HashMap<String, String>());
        assertThat(html, is(""));  // nothing should be rendered in view mode
    }

    @Test
    public void getSubstitution_shouldGenerateProperHtmlInEnterMode() {
        when(context.getMode()).thenReturn(FormEntryContext.Mode.ENTER);
        String html = submitButtonHandler.getSubstitution(session, formSubmissionController, new HashMap<String, String>());
        assertThat(html, is("<input type=\"button\" class=\"submitButton\" value=\"Enter\" onClick=\"submitHtmlForm()\"/>"));
    }

    @Test
    public void getSubstitution_shouldGenerateProperHtmlInEditMode() {
        when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);
        String html = submitButtonHandler.getSubstitution(session, formSubmissionController, new HashMap<String, String>());
        assertThat(html, is("<input type=\"button\" class=\"submitButton\" value=\"Save\" onClick=\"submitHtmlForm()\"/>"));
    }

    @Test
    public void getSubstitution_shouldGenerateProperHtmlWithCustomSubmitLabel() {
        Map<String,String> params = new HashMap<String, String>();
        params.put("submitLabel", "Custom Button");

        when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);
        String html = submitButtonHandler.getSubstitution(session, formSubmissionController, params);
        assertThat(html, is("<input type=\"button\" class=\"submitButton\" value=\"Custom Button\" onClick=\"submitHtmlForm()\"/>"));
    }

    @Test
    public void getSubstitution_shouldGenerateProperHtmlWithCustomClassSpecifiedInSubmitClassAttribute() {
        Map<String,String> params = new HashMap<String, String>();
        params.put("submitClass", "custom-class");

        when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);
        String html = submitButtonHandler.getSubstitution(session, formSubmissionController, params);
        assertThat(html, is("<input type=\"button\" class=\"submitButton custom-class\" value=\"Save\" onClick=\"submitHtmlForm()\"/>"));
    }

    @Test
    public void getSubstitution_shouldGenerateProperHtmlWithCustomClassSpecifiedInClassAttribute() {
        Map<String,String> params = new HashMap<String, String>();
        params.put("class", "custom-class");

        when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);
        String html = submitButtonHandler.getSubstitution(session, formSubmissionController, params);
        assertThat(html, is("<input type=\"button\" class=\"submitButton custom-class\" value=\"Save\" onClick=\"submitHtmlForm()\"/>"));
    }

}
