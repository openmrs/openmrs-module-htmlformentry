package org.openmrs.module.htmlformentry.extender;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.impl.HtmlFormEntryServiceImpl;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class FormActionsExtenderTest {

    private HtmlFormEntryService htmlFormEntryService;

    private FormActionsExtender mockAfterFormSubmissionActionsExtender;

    private FormActionsExtender mockBeforeFormSubmissionActionsExtender;

    private FormEntrySession formEntrySession;

    @Before
    public void setup() {
        htmlFormEntryService = new HtmlFormEntryServiceImpl();

        formEntrySession = mock(FormEntrySession.class);

        // register a couple submission actions
        mockAfterFormSubmissionActionsExtender = mock(FormActionsExtender.class);
        mockBeforeFormSubmissionActionsExtender = mock(FormActionsExtender.class);

        when(mockAfterFormSubmissionActionsExtender.getType()).thenReturn(FormActionsExtenderType.AFTER_FORM_SUBMISSION_ACTIONS);
        when(mockBeforeFormSubmissionActionsExtender.getType()).thenReturn(FormActionsExtenderType.BEFORE_FORM_SUBMISSION_ACTIONS);

        htmlFormEntryService.addFormSubmissionActionsExtender("after", mockAfterFormSubmissionActionsExtender);
        htmlFormEntryService.addFormSubmissionActionsExtender("before", mockBeforeFormSubmissionActionsExtender);

    }

    @Test
    public void testFormActionExtendersCalled() throws Exception {
        htmlFormEntryService.applyActions(formEntrySession);

        InOrder inOrder = inOrder(mockBeforeFormSubmissionActionsExtender, formEntrySession, mockAfterFormSubmissionActionsExtender);

        inOrder.verify(mockBeforeFormSubmissionActionsExtender).applyActions(formEntrySession);
        inOrder.verify(formEntrySession).applyActions();
        inOrder.verify(mockAfterFormSubmissionActionsExtender).applyActions(formEntrySession);
    }



}
