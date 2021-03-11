package org.openmrs.module.htmlformentry.tester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.TestUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.schema.HtmlFormField;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

public class FormSessionTester {
	
	private HtmlForm form;
	
	private FormEntrySession formEntrySession;
	
	private String htmlToDisplay;
	
	private Map<String, String> startingFormValues;
	
	private Map<String, String> enteredFormValues = new HashMap<>();
	
	private FormSessionTester(Patient patient, Encounter encounter, Mode mode, HtmlForm form) {
		try {
			this.form = form;
			this.formEntrySession = new FormEntrySession(patient, encounter, mode, form, new MockHttpSession());
			this.htmlToDisplay = formEntrySession.getHtmlToDisplay();
			this.startingFormValues = TestUtil.getFormValues(htmlToDisplay);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Unable to construct form entry session", e);
		}
	}
	
	public static FormSessionTester startSession(Patient patient, Encounter encounter, Mode mode, HtmlForm form) {
		FormSessionTester tester = new FormSessionTester(patient, encounter, mode, form);
		return tester;
	}
	
	public FormSessionTester reopenForEditing(FormResultsTester resultsTester) {
		Patient patient = formEntrySession.getPatient();
		Encounter encounter = resultsTester.getEncounterCreated();
		FormSessionTester editTester = new FormSessionTester(patient, encounter, Mode.EDIT, form);
		for (String formField : enteredFormValues.keySet()) {
			editTester.setFormField(formField, enteredFormValues.get(formField));
		}
		return editTester;
	}
	
	public String getHtmlToDisplay() {
		return htmlToDisplay;
	}
	
	public FormEntrySession getFormEntrySession() {
		return formEntrySession;
	}
	
	public HtmlFormSchema getSchema() {
		return formEntrySession.getContext().getSchema();
	}
	
	public <T extends HtmlFormField> List<T> getFields(Class<T> type) {
		List<T> ret = new ArrayList<>();
		for (HtmlFormField f : getSchema().getAllFields()) {
			if (type.isAssignableFrom(f.getClass())) {
				ret.add((T) f);
			}
		}
		return ret;
	}
	
	public <T extends Widget> List<T> getWidgets(Class<T> type) {
		List<T> ret = new ArrayList<>();
		for (Widget widget : getFormEntrySession().getContext().getFieldNames().keySet()) {
			if (type.isAssignableFrom(widget.getClass())) {
				ret.add((T) widget);
			}
		}
		return ret;
	}
	
	public <T extends FormSubmissionControllerAction> List<T> getSubmissionAction(Class<T> type) {
		List<T> ret = new ArrayList<>();
		for (FormSubmissionControllerAction action : formEntrySession.getSubmissionController().getActions()) {
			if (type.isAssignableFrom(action.getClass())) {
				ret.add((T) action);
			}
		}
		return ret;
	}
	
	public FormSessionTester setEncounterFields(String encounterDate, String location, String providerPersonId) {
		setFieldWithLabel("Date:", encounterDate);
		setFieldWithLabel("Location:", location);
		setFieldWithLabel("Provider:", providerPersonId);
		return this;
	}
	
	public FormSessionTester setFormField(String fieldName, String value) {
		enteredFormValues.put(fieldName, value);
		return this;
	}
	
	public FormSessionTester setFieldWithLabel(String label, String value) {
		return setFieldWithLabel(label, 0, 0, value);
	}
	
	public FormSessionTester setFieldWithLabel(String label, int widgetsToSkip, int labelsToSkip, String value) {
		String fieldName = TestUtil.getFormFieldName(htmlToDisplay, label, widgetsToSkip, labelsToSkip);
		if (fieldName == null) {
			throw new IllegalArgumentException("Unable to find field with label: " + label);
		}
		setFormField(fieldName, value);
		return this;
	}
	
	public String getFormField(String label) {
		return TestUtil.getFormFieldName(htmlToDisplay, label, 0, 0);
	}
	
	public Map<String, String> getFormValues() {
		Map<String, String> m = new HashMap<>(startingFormValues);
		m.putAll(enteredFormValues);
		return m;
	}
	
	public FormResultsTester submitForm() {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.setSession(formEntrySession.getHttpSession());
		request.setParameters(getFormValues());
		FormResultsTester resultsTester = FormResultsTester.submitForm(formEntrySession, request);
		return resultsTester;
	}
	
	public FormSessionTester assertHtmlContains(String expected) {
		assertThat(htmlToDisplay, containsString(expected));
		return this;
	}
	
	public FormSessionTester assertHtmlDoesNotContain(String expected) {
		assertThat(htmlToDisplay, not(containsString(expected)));
		return this;
	}
	
	public FormSessionTester assertHtmlFuzzyContains(String expected) {
		TestUtil.assertFuzzyContains(expected, htmlToDisplay);
		return this;
	}
	
	public FormSessionTester assertHtmlFuzzyDoesNotContain(String expected) {
		TestUtil.assertFuzzyDoesNotContain(expected, htmlToDisplay);
		return this;
	}
	
	public FormSessionTester assertHtmlMatchesPattern(Pattern pattern) {
		assertThat(pattern.matcher(htmlToDisplay), is(true));
		return this;
	}
	
	public FormSessionTester assertStartingFormValue(String label, String value) {
		String fieldName = TestUtil.getFormFieldName(htmlToDisplay, label, 0, 0);
		String val = startingFormValues.get(fieldName);
		if (value == null) {
			assertThat(val, nullValue());
		} else {
			assertThat(val, is(value));
		}
		return this;
	}
	
	public FormSessionTester assertFormValue(String name, String value) {
		String val = getFormValues().get(name);
		if (value == null) {
			assertThat(val, nullValue());
		} else {
			assertThat(val, is(value));
		}
		return this;
	}
}
