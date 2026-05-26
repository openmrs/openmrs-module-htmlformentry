package org.openmrs.module.htmlformentry.element;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentKind;
import org.openmrs.module.appointments.model.AppointmentProvider;
import org.openmrs.module.appointments.model.AppointmentProviderResponse;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.service.AppointmentServiceDefinitionService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.tag.TagUtil;
import org.openmrs.module.htmlformentry.util.MatchMode;
import org.openmrs.module.htmlformentry.widget.DateTimeWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ProviderAjaxAutoCompleteWidget;
import org.openmrs.module.htmlformentry.widget.TimeWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.TextFieldWidget;

public class ScheduleAppointmentElement implements HtmlGeneratorElement, FormSubmissionControllerAction {

	private static final String DEFAULT_LOCATION_TAG = "Appointment Location";

	private boolean optional;

	private String id;

	private TextFieldWidget scheduleChoiceWidget;

	private DropdownWidget locationWidget;

	private DropdownWidget serviceWidget;

	private DropdownWidget typeWidget;

	private DateTimeWidget startDateTimeWidget;

	private ProviderAjaxAutoCompleteWidget providerWidget;

	private TextFieldWidget noteWidget;

	private ErrorWidget locationErrorWidget;

	private ErrorWidget serviceErrorWidget;

	private ErrorWidget typeErrorWidget;

	private ErrorWidget startDateTimeErrorWidget;

	// stored for duration lookup at submission time
	private List<AppointmentServiceDefinition> services = new ArrayList<>();

	public ScheduleAppointmentElement(FormEntryContext context, Map<String, String> parameters) {
		if (context.getMode() != Mode.ENTER) {
			return;
		}

		optional = "true".equalsIgnoreCase(parameters.get("optional"));
		id = parameters.get("id");
		if (optional) {
			scheduleChoiceWidget = new TextFieldWidget();
			context.registerWidget(scheduleChoiceWidget);
		}

		// --- Location ---
		locationWidget = new DropdownWidget();
		locationErrorWidget = new ErrorWidget();
		locationWidget.addOption(new Option("", Context.getMessageSourceService().getMessage(
		    "htmlformentry.scheduleAppointment.chooseLocation"), false));
		String locationTagName = parameters.getOrDefault("locationTag", DEFAULT_LOCATION_TAG);
		LocationTag locationTag = Context.getLocationService().getLocationTagByName(locationTagName);
		List<Location> locations = locationTag != null
		        ? Context.getLocationService().getLocationsByTag(locationTag)
		        : Context.getLocationService().getAllLocations(false);
		for (Location loc : locations) {
			locationWidget.addOption(new Option(loc.getName(), loc.getUuid(), false));
		}
		context.registerWidget(locationWidget);
		context.registerErrorWidget(locationWidget, locationErrorWidget);

		// --- Service ---
		serviceWidget = new DropdownWidget();
		serviceErrorWidget = new ErrorWidget();
		serviceWidget.addOption(new Option("", Context.getMessageSourceService().getMessage(
		    "htmlformentry.scheduleAppointment.chooseService"), false));
		services = Context.getService(AppointmentServiceDefinitionService.class).getAllAppointmentServices(false);
		services.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
		for (AppointmentServiceDefinition svc : services) {
			serviceWidget.addOption(new Option(svc.getName(), svc.getUuid(), false));
		}
		context.registerWidget(serviceWidget);
		context.registerErrorWidget(serviceWidget, serviceErrorWidget);

		// --- Appointment Type ---
		List<AppointmentKind> allowedTypes = TagUtil.parseListParameter(parameters, "type", AppointmentKind.class);
		if (allowedTypes.isEmpty()) {
			allowedTypes = Arrays.asList(AppointmentKind.values());
		}
		typeWidget = new DropdownWidget();
		typeErrorWidget = new ErrorWidget();
		if (allowedTypes.size() > 1) {
			typeWidget.addOption(new Option("", Context.getMessageSourceService().getMessage(
			    "htmlformentry.scheduleAppointment.chooseType"), false));
		}
		for (AppointmentKind kind : allowedTypes) {
			typeWidget.addOption(new Option(kind.getValue(), kind.name(), false));
		}
		if (allowedTypes.size() == 1) {
			typeWidget.setInitialValue(allowedTypes.get(0).name());
		}
		context.registerWidget(typeWidget);
		context.registerErrorWidget(typeWidget, typeErrorWidget);

		// --- Start Date/Time ---
		// DateTimeWidget wraps DateWidget and TimeWidget; all three need separate registration
		DateWidget dateWidget = new DateWidget();
		TimeWidget timeWidget = new TimeWidget();
		timeWidget.setHideSeconds(true);
		context.registerWidget(dateWidget);
		context.registerWidget(timeWidget);
		startDateTimeWidget = new DateTimeWidget(dateWidget, timeWidget);
		startDateTimeErrorWidget = new ErrorWidget();
		context.registerWidget(startDateTimeWidget);
		context.registerErrorWidget(startDateTimeWidget, startDateTimeErrorWidget);

		// --- Provider ---
		providerWidget = new ProviderAjaxAutoCompleteWidget(MatchMode.ANYWHERE, null);
		context.registerWidget(providerWidget);

		// --- Note ---
		noteWidget = new TextFieldWidget();
		noteWidget.setTextArea(true);
		context.registerWidget(noteWidget);
	}

	@Override
	public String generateHtml(FormEntryContext context) {
		if (context.getMode() != Mode.ENTER) {
			return "<p>" + Context.getMessageSourceService().getMessage(
			    "htmlformentry.scheduleAppointment.viewModeMessage") + "</p>";
		}

		StringBuilder sb = new StringBuilder();

		sb.append("<div class=\"schedule-appointment\"" + (id != null ? " id=\"" + id + "\"" : "") + ">");

		if (optional) {
			String fieldName = context.getFieldName(scheduleChoiceWidget);
			String divId = "schedule-appt-fields-" + fieldName;
			String scheduleLabel = Context.getMessageSourceService().getMessage(
			    "htmlformentry.scheduleAppointment.doSchedule");
			String skipLabel = Context.getMessageSourceService().getMessage(
			    "htmlformentry.scheduleAppointment.skip");
			sb.append("<p class=\"schedule-appointment-choice\">");
			sb.append("<label><input type=\"radio\" name=\"").append(fieldName).append("\" value=\"yes\"")
			        .append(" onchange=\"document.getElementById('").append(divId).append("').style.display='block'\"> ")
			        .append(scheduleLabel).append("</label> ");
			sb.append("<label><input type=\"radio\" name=\"").append(fieldName).append("\" value=\"no\" checked")
			        .append(" onchange=\"document.getElementById('").append(divId).append("').style.display='none'\"> ")
			        .append(skipLabel).append("</label>");
			sb.append("</p>");
			sb.append("<div id=\"").append(divId).append("\" class=\"schedule-appointment-fields\" style=\"display:none\">");
		}

		sb.append(fieldRow("schedule-appointment-location",
		    Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.location"),
		    locationWidget.generateHtml(context) + locationErrorWidget.generateHtml(context)));

		sb.append(fieldRow("schedule-appointment-service",
		    Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.service"),
		    serviceWidget.generateHtml(context) + serviceErrorWidget.generateHtml(context)));

		sb.append(fieldRow("schedule-appointment-type",
		    Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.appointmentType"),
		    typeWidget.generateHtml(context) + typeErrorWidget.generateHtml(context)));

		sb.append(fieldRow("schedule-appointment-time",
		    Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.appointmentTime"),
		    startDateTimeWidget.generateHtml(context) + startDateTimeErrorWidget.generateHtml(context)));

		sb.append(fieldRow("schedule-appointment-provider",
		    Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.provider"),
		    providerWidget.generateHtml(context)));

		sb.append(fieldRow("schedule-appointment-note",
		    Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.note"),
		    noteWidget.generateHtml(context)));

		if (optional) {
			sb.append("</div>");
		}

		sb.append("</div>");

		return sb.toString();
	}

	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		if (context.getMode() != Mode.ENTER) {
			return Collections.emptyList();
		}

		if (optional && !"yes".equals(scheduleChoiceWidget.getValue(context, submission))) {
			return Collections.emptyList();
		}

		List<FormSubmissionError> errors = new ArrayList<>();

		String locationUuid = (String) locationWidget.getValue(context, submission);
		if (locationUuid == null || locationUuid.isEmpty()) {
			errors.add(new FormSubmissionError(context.getFieldName(locationWidget),
			        Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.error.locationRequired")));
		}

		String serviceUuid = (String) serviceWidget.getValue(context, submission);
		if (serviceUuid == null || serviceUuid.isEmpty()) {
			errors.add(new FormSubmissionError(context.getFieldName(serviceWidget),
			        Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.error.serviceRequired")));
		}

		if (typeWidget.getOptions().size() > 1) {
			String typeValue = (String) typeWidget.getValue(context, submission);
			if (typeValue == null || typeValue.isEmpty()) {
				errors.add(new FormSubmissionError(context.getFieldName(typeWidget),
				        Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.error.typeRequired")));
			}
		}

		Date startDateTime = (Date) startDateTimeWidget.getValue(context, submission);
		if (startDateTime == null) {
			errors.add(new FormSubmissionError(context.getFieldName(startDateTimeWidget),
			        Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.error.timeRequired")));
		}

		return errors;
	}

	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		if (session.getContext().getMode() != Mode.ENTER) {
			return;
		}

		FormEntryContext context = session.getContext();

		if (optional && !"yes".equals(scheduleChoiceWidget.getValue(context, submission))) {
			return;
		}

		String locationUuid = (String) locationWidget.getValue(context, submission);
		String serviceUuid = (String) serviceWidget.getValue(context, submission);
		String typeValue = typeWidget.getOptions().size() == 1
		        ? typeWidget.getOptions().get(0).getValue()
		        : (String) typeWidget.getValue(context, submission);
		Date startDateTime = (Date) startDateTimeWidget.getValue(context, submission);
		Provider selectedProvider = (Provider) providerWidget.getValue(context, submission);
		String note = (String) noteWidget.getValue(context, submission);

		AppointmentServiceDefinition service = services.stream()
		        .filter(s -> s.getUuid().equals(serviceUuid))
		        .findFirst()
		        .orElse(null);

		Appointment appointment = new Appointment();
		appointment.setPatient(session.getPatient());
		appointment.setLocation(Context.getLocationService().getLocationByUuid(locationUuid));
		appointment.setService(service);
		appointment.setAppointmentKind(AppointmentKind.valueOf(typeValue));
		appointment.setStartDateTime(startDateTime);
		if (service != null && service.getDurationMins() != null) {
			appointment.setEndDateTime(new Date(startDateTime.getTime() + (long) service.getDurationMins() * 60 * 1000));
		}
		appointment.setComments(note);

		if (selectedProvider != null) {
			AppointmentProvider appointmentProvider = new AppointmentProvider();
			appointmentProvider.setProvider(selectedProvider);
			appointmentProvider.setResponse(AppointmentProviderResponse.ACCEPTED);
			appointmentProvider.setAppointment(appointment);
			Set<AppointmentProvider> providers = new HashSet<>();
			providers.add(appointmentProvider);
			appointment.setProviders(providers);
		}

		session.getSubmissionActions().addAppointmentToCreate(appointment);
	}

	private String fieldRow(String cssClass, String label, String widgetHtml) {
		return "<p class=\"" + cssClass + "\"><label>" + label + "</label><span>" + widgetHtml + "</span></p>";
	}

}
