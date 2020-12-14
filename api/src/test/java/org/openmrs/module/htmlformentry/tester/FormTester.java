package org.openmrs.module.htmlformentry.tester;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.util.OpenmrsClassLoader;

public class FormTester {
	
	private static final String XML_DATASET_PATH = "org/openmrs/module/htmlformentry/include/";
	
	private HtmlForm form;
	
	private FormTester(HtmlForm form) {
		this.form = form;
	}
	
	public static FormTester buildForm(String xmlPath) {
		HtmlForm f = new HtmlForm();
		f.setForm(Context.getFormService().getForm(1));
		f.setName(f.getForm().getName());
		if (!xmlPath.startsWith(XML_DATASET_PATH)) {
			xmlPath = XML_DATASET_PATH + xmlPath;
		}
		try (InputStream is = OpenmrsClassLoader.getInstance().getResourceAsStream(xmlPath)) {
			String xmlData = IOUtils.toString(is, "UTF-8");
			f.setXmlData(xmlData);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Unable to load xml from resource: " + xmlPath, e);
		}
		return new FormTester(f);
	}
	
	public HtmlForm saveForm() {
		form = Context.getService(HtmlFormEntryService.class).saveHtmlForm(form);
		return form;
	}
	
	public FormSessionTester openForm(Patient patient, Mode mode) {
		return FormSessionTester.startSession(patient, null, mode, form);
	}
	
	public FormSessionTester openNewForm(Integer patientId) {
		return openForm(Context.getPatientService().getPatient(patientId), Mode.ENTER);
	}
	
	public FormSessionTester openExistingToView(Integer encounterId) {
		return openForm(Context.getEncounterService().getEncounter(encounterId), Mode.VIEW);
	}
	
	public FormSessionTester openExistingToEdit(Integer encounterId) {
		return openForm(Context.getEncounterService().getEncounter(encounterId), Mode.EDIT);
	}
	
	public FormSessionTester openForm(Encounter encounter, Mode mode) {
		return FormSessionTester.startSession(encounter.getPatient(), encounter, mode, form);
	}
}
