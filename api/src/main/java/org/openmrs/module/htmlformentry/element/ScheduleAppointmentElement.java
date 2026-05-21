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
import java.util.stream.Collectors;

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
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.widget.DateTimeWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.TimeWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.TextFieldWidget;

public class ScheduleAppointmentElement implements HtmlGeneratorElement, FormSubmissionControllerAction {

	private static final String DEFAULT_LOCATION_TAG = "Appointment Location";

	private boolean optional;

	private TextFieldWidget scheduleChoiceWidget;

	private DropdownWidget locationWidget;

	private DropdownWidget serviceWidget;

	private DropdownWidget typeWidget;

	private DateTimeWidget startDateTimeWidget;

	private DropdownWidget providerWidget;

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
		if (optional) {
			scheduleChoiceWidget = new TextFieldWidget();
			context.registerWidget(scheduleChoiceWidget);
		}

		// --- Location ---
		locationWidget = new DropdownWidget();
		locationErrorWidget = new ErrorWidget();
		locationWidget.addOption(new Option("", Context.getMessageSourceService().getMessage(
		    "htmlformentry.scheduleAppointment.chooseLocation", null, "Choose a location", Context.getLocale()), false));
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
		    "htmlformentry.scheduleAppointment.chooseService", null, "Choose a service", Context.getLocale()), false));
		services = Context.getService(AppointmentServiceDefinitionService.class).getAllAppointmentServices(false);
		services.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
		for (AppointmentServiceDefinition svc : services) {
			serviceWidget.addOption(new Option(svc.getName(), svc.getUuid(), false));
		}
		context.registerWidget(serviceWidget);
		context.registerErrorWidget(serviceWidget, serviceErrorWidget);

		// --- Appointment Type ---
		List<AppointmentKind> allowedTypes = parseAllowedTypes(parameters.get("type"));
		typeWidget = new DropdownWidget();
		typeErrorWidget = new ErrorWidget();
		if (allowedTypes.size() > 1) {
			typeWidget.addOption(new Option("", Context.getMessageSourceService().getMessage(
			    "htmlformentry.scheduleAppointment.chooseType", null, "Choose appointment type", Context.getLocale()),
			    false));
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
		providerWidget = new DropdownWidget();
		providerWidget.addOption(new Option("", Context.getMessageSourceService().getMessage(
		    "htmlformentry.scheduleAppointment.chooseProvider", null, "Choose a provider", Context.getLocale()), false));
		List<Provider> providers = Context.getProviderService().getAllProviders(false);
		providers.sort((a, b) -> HtmlFormEntryUtil.getProviderName(a).compareToIgnoreCase(HtmlFormEntryUtil.getProviderName(b)));
		for (Provider p : providers) {
			providerWidget.addOption(new Option(HtmlFormEntryUtil.getProviderName(p), p.getUuid(), false));
		}
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
			    "htmlformentry.scheduleAppointment.viewModeMessage", null,
			    "To view or manage appointments for this patient, use the Appointments app.",
			    Context.getLocale()) + "</p>";
		}

		StringBuilder sb = new StringBuilder();

		if (optional) {
			String fieldName = context.getFieldName(scheduleChoiceWidget);
			String divId = "schedule-appt-fields-" + fieldName;
			String scheduleLabel = Context.getMessageSourceService().getMessage(
			    "htmlformentry.scheduleAppointment.doSchedule", null, "Schedule appointment", Context.getLocale());
			String skipLabel = Context.getMessageSourceService().getMessage(
			    "htmlformentry.scheduleAppointment.skip", null, "Skip for now", Context.getLocale());
			sb.append("<p>");
			sb.append("<label><input type=\"radio\" name=\"").append(fieldName).append("\" value=\"yes\"")
			        .append(" onchange=\"document.getElementById('").append(divId).append("').style.display='block'\"> ")
			        .append(scheduleLabel).append("</label> ");
			sb.append("<label><input type=\"radio\" name=\"").append(fieldName).append("\" value=\"no\" checked")
			        .append(" onchange=\"document.getElementById('").append(divId).append("').style.display='none'\"> ")
			        .append(skipLabel).append("</label>");
			sb.append("</p>");
			sb.append("<div id=\"").append(divId).append("\" style=\"display:none\">");
		}

		sb.append(fieldRow(
		    Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.location", null, "Location",
		        Context.getLocale()),
		    locationWidget.generateHtml(context) + locationErrorWidget.generateHtml(context)));

		sb.append(fieldRow(
		    Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.service", null, "Service",
		        Context.getLocale()),
		    serviceWidget.generateHtml(context) + serviceErrorWidget.generateHtml(context)));

		if (typeWidget.getOptions().size() > 1) {
			sb.append(fieldRow(
			    Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.appointmentType", null,
			        "Appointment Type", Context.getLocale()),
			    typeWidget.generateHtml(context) + typeErrorWidget.generateHtml(context)));
		} else {
			sb.append(fieldRow(
			    Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.appointmentType", null,
			        "Appointment Type", Context.getLocale()),
			    "<span>" + typeWidget.getOptions().get(0).getLabel() + "</span>"));
		}

		sb.append(fieldRow(
		    Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.appointmentTime", null,
		        "Appointment Time", Context.getLocale()),
		    startDateTimeWidget.generateHtml(context) + startDateTimeErrorWidget.generateHtml(context)));

		sb.append(fieldRow(
		    Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.provider", null, "Provider",
		        Context.getLocale()),
		    providerWidget.generateHtml(context)));

		sb.append(fieldRow(
		    Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.note", null, "Note",
		        Context.getLocale()),
		    noteWidget.generateHtml(context)));

		if (optional) {
			sb.append("</div>");
		}

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
			        Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.error.locationRequired",
			            null, "Location is required", Context.getLocale())));
		}

		String serviceUuid = (String) serviceWidget.getValue(context, submission);
		if (serviceUuid == null || serviceUuid.isEmpty()) {
			errors.add(new FormSubmissionError(context.getFieldName(serviceWidget),
			        Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.error.serviceRequired",
			            null, "Service is required", Context.getLocale())));
		}

		if (typeWidget.getOptions().size() > 1) {
			String typeValue = (String) typeWidget.getValue(context, submission);
			if (typeValue == null || typeValue.isEmpty()) {
				errors.add(new FormSubmissionError(context.getFieldName(typeWidget),
				        Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.error.typeRequired",
				            null, "Appointment type is required", Context.getLocale())));
			}
		}

		Date startDateTime = (Date) startDateTimeWidget.getValue(context, submission);
		if (startDateTime == null) {
			errors.add(new FormSubmissionError(context.getFieldName(startDateTimeWidget),
			        Context.getMessageSourceService().getMessage("htmlformentry.scheduleAppointment.error.timeRequired", null,
			            "Appointment time is required", Context.getLocale())));
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
		String providerUuid = (String) providerWidget.getValue(context, submission);
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

		if (providerUuid != null && !providerUuid.isEmpty()) {
			Provider provider = Context.getProviderService().getProviderByUuid(providerUuid);
			if (provider != null) {
				AppointmentProvider appointmentProvider = new AppointmentProvider();
				appointmentProvider.setProvider(provider);
				appointmentProvider.setResponse(AppointmentProviderResponse.ACCEPTED);
				appointmentProvider.setAppointment(appointment);
				Set<AppointmentProvider> providers = new HashSet<>();
				providers.add(appointmentProvider);
				appointment.setProviders(providers);
			}
		}

		session.getSubmissionActions().addAppointmentToCreate(appointment);
	}

	private String fieldRow(String label, String widgetHtml) {
		return "<p><label>" + label + "</label><span>" + widgetHtml + "</span></p>";
	}

	/**
	 * Parses the optional {@code type} tag attribute into a list of allowed AppointmentKind values.
	 * Accepts a comma-separated list, optionally wrapped in brackets, e.g. {@code [scheduled,walkIn]}.
	 * Matching is case-insensitive. Returns all kinds when the attribute is absent or empty.
	 */
	private List<AppointmentKind> parseAllowedTypes(String typeParam) {
		if (typeParam == null || typeParam.trim().isEmpty()) {
			return Arrays.asList(AppointmentKind.values());
		}

		// strips out square brackets at the beginning or end of string, if exists
		String cleaned = typeParam.trim().replaceAll("^\\[|\\]$", "");
		return Arrays.stream(cleaned.split(","))
		        .map(String::trim)
		        .filter(s -> !s.isEmpty())
		        .map(s -> Arrays.stream(AppointmentKind.values())
		                .filter(k -> k.name().equalsIgnoreCase(s))
		                .findFirst()
		                .orElseThrow(() -> new IllegalArgumentException("Unknown appointment type: " + s)))
		        .collect(Collectors.toList());
	}
}
