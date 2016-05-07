package org.openmrs.module.htmlformentry.action;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;

import java.util.*;

import static org.mockito.Mockito.mock;

public class RepeatControllerActionTest {
    private class TestRepeatControllerAction extends RepeatControllerAction {
        boolean beforeHandleSubmissionCalled;
        boolean afterHandleSubmissionCalled;
        boolean beforeValidateSubmissionCalled;
        boolean afterValidateSubmissionCalled;

        @Override
        public void beforeHandleSubmission(FormEntrySession session, HttpServletRequest submission) {
            beforeHandleSubmissionCalled = true;
        }

        @Override
        public void afterHandleSubmission(FormEntrySession session, HttpServletRequest submission) {
            afterHandleSubmissionCalled = true;
        }

        @Override
        public void beforeValidateSubmission(FormEntryContext context, HttpServletRequest submission) {
            beforeValidateSubmissionCalled = true;
        }

        @Override
        public void afterValidateSubmission(FormEntryContext context, HttpServletRequest submission) {
            afterValidateSubmissionCalled = true;
        }
    }

    private Set<Integer> validationCalledActionIds = new HashSet<Integer>();
    private Set<Integer> submissionCalledActionIds = new HashSet<Integer>();

    private class TestAction implements FormSubmissionControllerAction {
        public final int id;

        TestAction(int id) {
            this.id = id;
        }

        @Override
        public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
            validationCalledActionIds.add(id);
            List<FormSubmissionError> errors = new ArrayList<FormSubmissionError>();
            errors.add(new FormSubmissionError(String.valueOf(id), ""));

            return errors;
        }

        @Override
        public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
            submissionCalledActionIds.add(id);
        }
    }

    private FormEntrySession formEntrySession;
    private FormEntryContext formEntryContext;

    @Before
    public void setUp() {
        formEntrySession = mock(FormEntrySession.class);
        formEntryContext = mock(FormEntryContext.class);
    }

    @Test
    public void testBeforeAfterHandleSubmission_shouldCallBeforeAfterHandlers() {
        TestRepeatControllerAction action = new TestRepeatControllerAction();
        action.handleSubmission(formEntrySession, new MockHttpServletRequest());
        Assert.assertTrue(action.beforeHandleSubmissionCalled);
        Assert.assertTrue(action.afterHandleSubmissionCalled);
    }

    @Test
    public void testBeforeAfterValidateSubmission_shouldCallBeforeAfterHandlers() {
        TestRepeatControllerAction action = new TestRepeatControllerAction();
        action.validateSubmission(formEntryContext, new MockHttpServletRequest());
        Assert.assertTrue(action.beforeValidateSubmissionCalled);
        Assert.assertTrue(action.afterValidateSubmissionCalled);
    }

    @Test
    public void testValidateSubmission_shouldCallAllRegisteredActions() {
        TestRepeatControllerAction action = new TestRepeatControllerAction();

        final int NUM_ACTIONS = 5;
        for (int i = 0; i < NUM_ACTIONS; i++) {
            action.addAction(new TestAction(i));
        }

        Collection<FormSubmissionError> errors = action.validateSubmission(formEntryContext, new MockHttpServletRequest());

        // Validate all returned errors are merged together
        int i = 0;
        for (FormSubmissionError error : errors) {
            Assert.assertEquals(String.valueOf(i), error.getId());
            i++;
        }

        Assert.assertEquals(NUM_ACTIONS, errors.size());

        // Validate all validation functions are called
        Assert.assertEquals(NUM_ACTIONS, validationCalledActionIds.size());

        action.handleSubmission(formEntrySession, new MockHttpServletRequest());

        // Validate all submission functions are called
        Assert.assertEquals(NUM_ACTIONS, submissionCalledActionIds.size());
    }
}
