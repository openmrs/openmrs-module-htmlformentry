package org.openmrs.module.htmlformentry.element;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.service.AppointmentServiceDefinitionService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.tag.TagUtil;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Option;

public class AppointmentServiceObsElement implements HtmlGeneratorElement, FormSubmissionControllerAction {

	private Concept concept;

	private Obs existingObs;

	private DropdownWidget serviceWidget;

	private ErrorWidget errorWidget;

	public AppointmentServiceObsElement(FormEntryContext context, Map<String, String> parameters) {
		String conceptId = parameters.get("conceptId");
		if (conceptId == null) {
			throw new IllegalArgumentException("appointmentServiceObs requires a conceptId parameter");
		}
		concept = HtmlFormEntryUtil.getConcept(conceptId);
		if (concept == null) {
			throw new IllegalArgumentException("Could not find concept: " + conceptId);
		}

		existingObs = context.removeExistingObs(concept, (Concept) null);
		String existingUuid = existingObs != null ? existingObs.getValueText() : null;

		List<String> specialityFilter = TagUtil.parseListParameter(parameters, "specialities", String.class).stream()
		        .map(String::toLowerCase).map(String::trim).collect(Collectors.toList());

		List<AppointmentServiceDefinition> services = Context.getService(AppointmentServiceDefinitionService.class)
		        .getAllAppointmentServices(false);
		services.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

		if (!specialityFilter.isEmpty()) {
			services = services.stream()
			        .filter(s -> s.getSpeciality() != null
			                && specialityFilter.contains(s.getSpeciality().getName().toLowerCase().trim()))
			        .collect(Collectors.toList());
		}

		serviceWidget = new DropdownWidget();
		errorWidget = new ErrorWidget();

		serviceWidget.addOption(new Option("", "", false));
		for (AppointmentServiceDefinition svc : services) {
			serviceWidget.addOption(new Option(svc.getName(), svc.getUuid(), false));
		}
		if (existingUuid != null) {
			serviceWidget.setInitialValue(existingUuid);
		}

		context.registerWidget(serviceWidget);
		context.registerErrorWidget(serviceWidget, errorWidget);
	}

	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder sb = new StringBuilder();
		sb.append(serviceWidget.generateHtml(context));
		if (context.getMode() != Mode.VIEW) {
			sb.append("<span class='required'>*</span>");
			sb.append(errorWidget.generateHtml(context));
		}
		return sb.toString();
	}

	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		return Collections.emptyList();
	}

	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		if (session.getContext().getMode() == Mode.VIEW) {
			return;
		}

		FormEntryContext context = session.getContext();
		String value = (String) serviceWidget.getValue(context, submission);

		if (existingObs != null && context.getMode() == Mode.EDIT) {
			session.getSubmissionActions().modifyObs(existingObs, concept, value, null, null, null);
		} else if (value != null && !value.isEmpty()) {
			session.getSubmissionActions().createObs(concept, value, null, null, null);
		}
	}

}
