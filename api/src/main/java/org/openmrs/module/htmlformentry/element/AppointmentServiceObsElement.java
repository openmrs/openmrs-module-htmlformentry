package org.openmrs.module.htmlformentry.element;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.service.AppointmentServiceDefinitionService;
import org.openmrs.module.htmlformentry.BadFormDesignException;
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

	DropdownWidget serviceWidget;

	private ErrorWidget errorWidget;

	private String tagControlId;

	public AppointmentServiceObsElement(FormEntryContext context, Map<String, String> parameters) throws BadFormDesignException{
		String conceptId = parameters.get("conceptId");
		if (conceptId == null) {
			throw new BadFormDesignException("appointmentServiceObs requires a conceptId parameter");
		}
		concept = HtmlFormEntryUtil.getConcept(conceptId);
		if (concept == null) {
			throw new BadFormDesignException("Could not find concept: " + conceptId);
		}

		if (parameters.containsKey("controlId")) {
			tagControlId = parameters.get("controlId");
		}

		boolean insideObsGroup = context.getCurrentObsGroupConcepts() != null
		        && context.getCurrentObsGroupConcepts().size() > 0;
		if (StringUtils.isNotBlank(tagControlId)) {
			if (insideObsGroup) {
				existingObs = context.getObsFromCurrentGroup(tagControlId);
			} else {
				existingObs = context.getObsFromExistingObs(concept, tagControlId);
			}
		} else {
			if (insideObsGroup) {
				existingObs = context.getObsFromCurrentGroup(concept, (Concept) null);
			} else {
				existingObs = context.removeExistingObs(concept, (Concept) null);
			}
		}
		String existingValue = existingObs != null ? existingObs.getValueText() : null;

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
			serviceWidget.addOption(new Option(svc.getName(), svc.getId() + " - " + svc.getName(), false));
		}
		if (existingValue != null) {
			serviceWidget.setInitialValue(existingValue);
		}

		context.registerWidget(serviceWidget);
		context.registerErrorWidget(serviceWidget, errorWidget);
	}

	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder sb = new StringBuilder();
		sb.append(serviceWidget.generateHtml(context));
		if (context.getMode() != Mode.VIEW) {
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
			session.getSubmissionActions().modifyObs(existingObs, concept, value, null, null, null, getControlFormPath(session));
		} else if (value != null && !value.isEmpty()) {
			session.getSubmissionActions().createObs(concept, value, null, null, null, getControlFormPath(session));
		}
	}

	private String getControlFormPath(FormEntrySession session) {
		if (StringUtils.isBlank(tagControlId)) {
			return null;
		}
		return session.generateControlFormPath(tagControlId, 0);
	}

}
