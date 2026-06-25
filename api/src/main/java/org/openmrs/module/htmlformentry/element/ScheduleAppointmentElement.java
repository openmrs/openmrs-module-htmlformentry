package org.openmrs.module.htmlformentry.element;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Obs;
import org.openmrs.Provider;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentKind;
import org.openmrs.module.appointments.model.AppointmentProvider;
import org.openmrs.module.appointments.model.AppointmentProviderResponse;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.model.Speciality;
import org.openmrs.module.appointments.service.AppointmentServiceDefinitionService;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.openmrs.module.appointments.service.SpecialityService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.tag.TagUtil;
import org.openmrs.module.htmlformentry.util.MatchMode;
import org.openmrs.module.htmlformentry.widget.DateTimeWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.ProviderAjaxAutoCompleteWidget;
import org.openmrs.module.htmlformentry.widget.TextFieldWidget;
import org.openmrs.module.htmlformentry.widget.TimeWidget;

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

	// Hidden field tracking "allday" vs "specific" radio state
	private TextFieldWidget allDayWidget;

	private TextFieldWidget durationWidget;

	private ErrorWidget locationErrorWidget;

	private ErrorWidget serviceErrorWidget;

	private ErrorWidget typeErrorWidget;

	private ErrorWidget startDateTimeErrorWidget;

	private ErrorWidget timeErrorWidget;

	private ErrorWidget durationErrorWidget;

	private ErrorWidget providerErrorWidget;

	// Config params
	private String appointmentUuidConceptMapping;

	private String tagControlId;

	// stored for duration lookup at submission time
	private List<AppointmentServiceDefinition> services = new ArrayList<>();

	// Appointment fetched in VIEW/EDIT mode constructor
	private Appointment existingAppointment;

	public ScheduleAppointmentElement(FormEntryContext context, Map<String, String> parameters) {
		appointmentUuidConceptMapping = parameters.get("appointmentUuidConcept");
		tagControlId = parameters.get("controlId");

		if (context.getMode() != Mode.ENTER) {
			// VIEW/EDIT: claim the UUID obs so HFE doesn't warn about an unmatched obs,
			// then fetch the appointment for display in generateHtml().
			if (appointmentUuidConceptMapping != null) {
				Concept uuidConcept = HtmlFormEntryUtil.getConcept(appointmentUuidConceptMapping);
				if (uuidConcept != null) {
					Obs uuidObs = StringUtils.isNotBlank(tagControlId)
					        ? context.getObsFromExistingObs(uuidConcept, tagControlId)
					        : (context.removeExistingObs(uuidConcept).stream().findFirst().orElse(null));
					if (uuidObs != null && uuidObs.getValueText() != null) {
						try {
							existingAppointment = Context.getService(AppointmentsService.class)
							        .getAppointmentByUuid(uuidObs.getValueText());
						}
						catch (Exception e) {
							// appointments module unavailable
						}
					}
				}
			}
			return;
		}

		// ENTER mode: set up all widgets
		optional = "true".equalsIgnoreCase(parameters.get("optional"));
		id = parameters.get("id");
		if (optional) {
			scheduleChoiceWidget = new TextFieldWidget();
			context.registerWidget(scheduleChoiceWidget);
		}

		// --- Location ---
		locationWidget = new DropdownWidget();
		locationErrorWidget = new ErrorWidget();
		locationWidget.addOption(new Option(Context.getMessageSourceService().getMessage(
		    "htmlformentry.scheduleAppointment.chooseLocation"), "", false));
		String locationTagName = parameters.getOrDefault("locationTag", DEFAULT_LOCATION_TAG);
		LocationTag locationTag = Context.getLocationService().getLocationTagByName(locationTagName);
		List<Location> locations = locationTag != null
		        ? Context.getLocationService().getLocationsByTag(locationTag)
		        : Context.getLocationService().getAllLocations(false);

		if ("true".equalsIgnoreCase(parameters.get("restrictToCurrentVisitLocation"))
		        && context.getVisit() != null) {
			locations = HtmlFormEntryUtil.removeLocationsNotEqualToOrDescendentOf(locations,
			    ((Visit) context.getVisit()).getLocation());
		}

		for (Location loc : locations) {
			locationWidget.addOption(new Option(loc.getName(), loc.getUuid(), false));
		}
		context.registerWidget(locationWidget);
		context.registerErrorWidget(locationWidget, locationErrorWidget);

		// --- Service ---
		serviceWidget = new DropdownWidget();
		serviceErrorWidget = new ErrorWidget();
		serviceWidget.addOption(new Option(Context.getMessageSourceService().getMessage(
		    "htmlformentry.scheduleAppointment.chooseService"), "", false));

		// Resolve speciality filter (also defines the order for the "specialityOrder" sort key)
		List<String> specialityFilterNames = TagUtil.parseListParameter(parameters, "specialities", String.class)
		        .stream().map(String::toLowerCase).map(String::trim).collect(Collectors.toList());
		List<Speciality> allSpecialities = Context.getService(SpecialityService.class).getAllSpecialities();
		List<String> specialityFilterUuids = specialityFilterNames.stream()
		        .map(s -> allSpecialities.stream()
		                .filter(sp -> sp.getName().equalsIgnoreCase(s) || sp.getUuid().equalsIgnoreCase(s))
		                .findFirst())
		        .filter(Optional::isPresent)
		        .map(Optional::get)
		        .map(Speciality::getUuid)
		        .collect(Collectors.toList());

		// Build comparator from sortBy list (e.g. sortBy="specialityOrder,serviceName")
		List<String> sortByKeys = TagUtil.parseListParameter(parameters, "sortServicesBy", String.class);
		if (sortByKeys.isEmpty()) {
			sortByKeys = Collections.singletonList("serviceName");
		}
		Comparator<AppointmentServiceDefinition> comparator = null;
		for (String key : sortByKeys) {
			Comparator<AppointmentServiceDefinition> next;
			if ("specialityOrder".equalsIgnoreCase(key)) {
				final List<String> uuids = specialityFilterUuids;
				next = Comparator.comparingInt((AppointmentServiceDefinition svc) -> {
					Speciality sp = svc.getSpeciality();
					return sp != null ? uuids.indexOf(sp.getUuid()) : -1;
				});
			} else {
				next = Comparator.comparing(AppointmentServiceDefinition::getName, String.CASE_INSENSITIVE_ORDER);
			}
			comparator = comparator == null ? next : comparator.thenComparing(next);
		}

		services = Context.getService(AppointmentServiceDefinitionService.class).getAllAppointmentServices(false);
		final Comparator<AppointmentServiceDefinition> finalComparator = comparator;
		services = services.stream()
		        .filter(svc -> specialityFilterUuids.isEmpty()
		                || (svc.getSpeciality() != null && specialityFilterUuids.contains(svc.getSpeciality().getUuid())))
		        .sorted(finalComparator)
		        .collect(Collectors.toList());

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
			typeWidget.addOption(new Option(Context.getMessageSourceService().getMessage(
			    "htmlformentry.scheduleAppointment.chooseType"), "", false));
		}
		for (AppointmentKind kind : allowedTypes) {
			typeWidget.addOption(new Option(kind.getValue(), kind.name(), false));
		}
		if (allowedTypes.size() == 1) {
			typeWidget.setInitialValue(allowedTypes.get(0).name());
		}
		context.registerWidget(typeWidget);
		context.registerErrorWidget(typeWidget, typeErrorWidget);

		// --- Date ---
		DateWidget dateWidget = new DateWidget();
		TimeWidget timeWidget = new TimeWidget();
		timeWidget.setHideSeconds(true);
		context.registerWidget(dateWidget);
		context.registerWidget(timeWidget);
		startDateTimeWidget = new DateTimeWidget(dateWidget, timeWidget);
		startDateTimeErrorWidget = new ErrorWidget();
		timeErrorWidget = new ErrorWidget();
		context.registerErrorWidget(startDateTimeWidget, startDateTimeErrorWidget);
		context.registerWidget(timeErrorWidget);

		// --- All-day toggle (hidden field; value set by JS radio buttons) ---
		allDayWidget = new TextFieldWidget();
		context.registerWidget(allDayWidget);

		// --- Duration ---
		durationWidget = new TextFieldWidget();
		durationErrorWidget = new ErrorWidget();
		context.registerWidget(durationWidget);
		context.registerErrorWidget(durationWidget, durationErrorWidget);

		// --- Provider ---
		providerWidget = new ProviderAjaxAutoCompleteWidget(MatchMode.ANYWHERE, null);
		providerErrorWidget = new ErrorWidget();
		context.registerWidget(providerWidget);
		context.registerErrorWidget(providerWidget, providerErrorWidget);

		// --- Note ---
		noteWidget = new TextFieldWidget();
		noteWidget.setTextArea(true);
		context.registerWidget(noteWidget);
	}

	@Override
	public String generateHtml(FormEntryContext context) {
		if (context.getMode() != Mode.ENTER) {
			return generateViewEditHtml();
		}

		StringBuilder sb = new StringBuilder();

		sb.append("<div class=\"schedule-appointment\"").append(id != null ? " id=\"" + id + "\"" : "").append(">");

		String fieldsWrapperId = null;
		if (optional) {
			String choiceFieldName = context.getFieldName(scheduleChoiceWidget);
			fieldsWrapperId = "schedule-appt-fields-" + choiceFieldName;
			sb.append("<p class=\"schedule-appointment-choice\">");
			sb.append("<label><input type=\"radio\" name=\"").append(choiceFieldName)
			        .append("\" value=\"yes\" checked")
			        .append(" onchange=\"document.getElementById('").append(fieldsWrapperId)
			        .append("').style.display='block'\"> ")
			        .append(msg("htmlformentry.scheduleAppointment.doSchedule")).append("</label> ");
			sb.append("<label><input type=\"radio\" name=\"").append(choiceFieldName)
			        .append("\" value=\"no\"")
			        .append(" onchange=\"document.getElementById('").append(fieldsWrapperId)
			        .append("').style.display='none'\"> ")
			        .append(msg("htmlformentry.scheduleAppointment.skip")).append("</label>");
			sb.append("</p>");
			sb.append("<div id=\"").append(fieldsWrapperId)
			        .append("\" class=\"schedule-appointment-fields\" style=\"display:block\">");
		}

		// Location
		sb.append(fieldRow("schedule-appointment-location",
		    msg("htmlformentry.scheduleAppointment.location"),
		    locationWidget.generateHtml(context) + locationErrorWidget.generateHtml(context), true));

		// Service
		sb.append(fieldRow("schedule-appointment-service",
		    msg("htmlformentry.scheduleAppointment.service"),
		    serviceWidget.generateHtml(context) + serviceErrorWidget.generateHtml(context), true));

		// Appointment Type (only when more than one option)
		if (typeWidget.getOptions().size() > 1) {
			sb.append(fieldRow("schedule-appointment-type",
			    msg("htmlformentry.scheduleAppointment.appointmentType"),
			    typeWidget.generateHtml(context) + typeErrorWidget.generateHtml(context), true));
		}

		// All-day / Specific-time toggle — shown BEFORE the date/time row
		String allDayFieldName = context.getFieldName(allDayWidget);
		String timeSpanId = "schedule-appt-time-" + allDayFieldName;
		String durationRowId = "schedule-appt-duration-" + allDayFieldName;
		sb.append("<p class=\"schedule-appointment-timing\">");
		sb.append("<label>").append(msg("htmlformentry.scheduleAppointment.timing"))
		        .append(" <span class=\"required\">*</span></label>");
		sb.append("<span style=\"display:flex;gap:1.5em;align-items:center\">");
		sb.append("<label style=\"display:inline-flex;align-items:center;gap:0.3em\"><input type=\"radio\" name=\"").append(allDayFieldName).append("-radio\" value=\"allday\"")
		        .append(" onchange=\"document.getElementById('").append(allDayFieldName).append("').value='allday';")
		        .append("document.getElementById('").append(timeSpanId).append("').style.display='none';")
		        .append("document.getElementById('").append(durationRowId).append("').style.display='none';\"> ")
		        .append(msg("htmlformentry.scheduleAppointment.allDay")).append("</label>");
		sb.append("<label style=\"display:inline-flex;align-items:center;gap:0.3em\"><input type=\"radio\" name=\"").append(allDayFieldName).append("-radio\" value=\"specific\" checked")
		        .append(" onchange=\"document.getElementById('").append(allDayFieldName).append("').value='specific';")
		        .append("document.getElementById('").append(timeSpanId).append("').style.display='inline-flex';")
		        .append("document.getElementById('").append(durationRowId).append("').style.display='block';\"> ")
		        .append(msg("htmlformentry.scheduleAppointment.specificTime")).append("</label>");
		sb.append("</span>");
		sb.append("<input type=\"hidden\" name=\"").append(allDayFieldName)
		        .append("\" id=\"").append(allDayFieldName).append("\" value=\"specific\"/>");
		sb.append("</p>");

		// Date + Time on one line; time portion shown/hidden by the toggle above
		sb.append("<p class=\"schedule-appointment-date-time\">");
		sb.append("<label>").append(msg("htmlformentry.scheduleAppointment.date"))
		        .append(" <span class=\"required\">*</span></label>");
		sb.append("<span style=\"display:inline-flex;align-items:center;gap:0.5em;flex-wrap:wrap\">");
		sb.append(startDateTimeWidget.getDateWidget().generateHtml(context));
		sb.append(startDateTimeErrorWidget.generateHtml(context));
		sb.append("<span id=\"").append(timeSpanId)
		        .append("\" style=\"display:inline-flex;align-items:center;gap:0.25em\">");
		sb.append(startDateTimeWidget.getTimeWidget().generateHtml(context));
		sb.append(timeErrorWidget.generateHtml(context));
		sb.append("</span>");
		sb.append("</span>");
		sb.append("</p>");

		// Duration (shown only for specific-time)
		String durFieldName = context.getFieldName(durationWidget);
		sb.append("<div id=\"").append(durationRowId).append("\">");
		sb.append(fieldRow("schedule-appointment-duration",
		    msg("htmlformentry.scheduleAppointment.duration"),
		    "<input type=\"number\" name=\"" + durFieldName + "\" id=\"" + durFieldName
		            + "\" min=\"1\" style=\"width:5em\"/>" + durationErrorWidget.generateHtml(context),
		    true));
		sb.append("</div>");

		// Provider
		sb.append(fieldRow("schedule-appointment-provider",
		    msg("htmlformentry.scheduleAppointment.provider"),
		    providerWidget.generateHtml(context) + providerErrorWidget.generateHtml(context), true));

		// Note
		sb.append(fieldRow("schedule-appointment-note",
		    msg("htmlformentry.scheduleAppointment.note"),
		    noteWidget.generateHtml(context)));

		if (optional) {
			sb.append("</div>");
		}

		sb.append("</div>");

		return sb.toString();
	}

	private String generateViewEditHtml() {
		if (existingAppointment == null) {
			if (appointmentUuidConceptMapping == null) {
				return "<p>" + msg("htmlformentry.scheduleAppointment.viewModeMessage") + "</p>";
			}
			return "<p class=\"schedule-appointment-none\">"
			        + msg("htmlformentry.scheduleAppointment.noAppointment") + "</p>";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<div class=\"schedule-appointment-view\">");

		Date start = existingAppointment.getStartDateTime();
		Date end = existingAppointment.getEndDateTime();
		boolean isAllDay = isAllDayAppointment(start, end);

		// Date
		String dateStr = Context.getDateFormat().format(start);
		sb.append(viewRow(msg("htmlformentry.scheduleAppointment.date"), dateStr));

		// Time (or "All day")
		if (isAllDay) {
			sb.append(viewRow(msg("htmlformentry.scheduleAppointment.appointmentTime"),
			    msg("htmlformentry.scheduleAppointment.allDay")));
		} else {
			Calendar cal = Calendar.getInstance();
			cal.setTime(start);
			String timeStr = String.format("%02d:%02d",
			    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
			sb.append(viewRow(msg("htmlformentry.scheduleAppointment.appointmentTime"), timeStr));
		}

		// Location
		if (existingAppointment.getLocation() != null) {
			sb.append(viewRow(msg("htmlformentry.scheduleAppointment.location"),
			    existingAppointment.getLocation().getName()));
		}

		// Service
		if (existingAppointment.getService() != null) {
			sb.append(viewRow(msg("htmlformentry.scheduleAppointment.service"),
			    existingAppointment.getService().getName()));
		}

		// Provider
		if (existingAppointment.getProviders() != null && !existingAppointment.getProviders().isEmpty()) {
			AppointmentProvider ap = existingAppointment.getProviders().iterator().next();
			if (ap.getProvider() != null) {
				sb.append(viewRow(msg("htmlformentry.scheduleAppointment.provider"),
				    ap.getProvider().getName()));
			}
		}

		// Notes
		if (StringUtils.isNotBlank(existingAppointment.getComments())) {
			sb.append(viewRow(msg("htmlformentry.scheduleAppointment.note"),
			    existingAppointment.getComments()));
		}

		// Status
		if (existingAppointment.getStatus() != null) {
			sb.append(viewRow(msg("htmlformentry.scheduleAppointment.status"),
			    existingAppointment.getStatus().name()));
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
			errors.add(new FormSubmissionError(context.getFieldName(locationErrorWidget),
			        msg("htmlformentry.scheduleAppointment.error.locationRequired")));
		}

		String serviceUuid = (String) serviceWidget.getValue(context, submission);
		if (serviceUuid == null || serviceUuid.isEmpty()) {
			errors.add(new FormSubmissionError(context.getFieldName(serviceErrorWidget),
			        msg("htmlformentry.scheduleAppointment.error.serviceRequired")));
		}

		if (typeWidget.getOptions().size() > 1) {
			String typeValue = (String) typeWidget.getValue(context, submission);
			if (typeValue == null || typeValue.isEmpty()) {
				errors.add(new FormSubmissionError(context.getFieldName(typeErrorWidget),
				        msg("htmlformentry.scheduleAppointment.error.typeRequired")));
			}
		}

		// Date is always required
		Date date = (Date) startDateTimeWidget.getDateWidget().getValue(context, submission);
		if (date == null) {
			errors.add(new FormSubmissionError(context.getFieldName(startDateTimeErrorWidget),
			        msg("htmlformentry.scheduleAppointment.error.dateRequired")));
		}

		// Time and duration required only for specific-time appointments
		String allDayValue = (String) allDayWidget.getValue(context, submission);
		boolean isAllDay = "allday".equals(allDayValue);
		if (!isAllDay) {
			Date time = (Date) startDateTimeWidget.getTimeWidget().getValue(context, submission);
			if (time == null) {
				errors.add(new FormSubmissionError(context.getFieldName(timeErrorWidget),
				        msg("htmlformentry.scheduleAppointment.error.timeRequired")));
			}

			String durStr = (String) durationWidget.getValue(context, submission);
			boolean validDuration = false;
			if (StringUtils.isNotBlank(durStr)) {
				try {
					validDuration = Integer.parseInt(durStr.trim()) > 0;
				}
				catch (NumberFormatException e) {
					// falls through to error
				}
			}
			if (!validDuration) {
				errors.add(new FormSubmissionError(context.getFieldName(durationErrorWidget),
				        msg("htmlformentry.scheduleAppointment.error.durationRequired")));
			}
		}

		Provider provider = (Provider) providerWidget.getValue(context, submission);
		if (provider == null) {
			errors.add(new FormSubmissionError(context.getFieldName(providerErrorWidget),
			        msg("htmlformentry.scheduleAppointment.error.providerRequired")));
		}

		return errors;
	}

	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		// EDIT mode is intentionally a no-op: the element does not update existing appointments.
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
		Date date = (Date) startDateTimeWidget.getDateWidget().getValue(context, submission);
		String allDayValue = (String) allDayWidget.getValue(context, submission);
		boolean isAllDay = "allday".equals(allDayValue);
		Provider selectedProvider = (Provider) providerWidget.getValue(context, submission);
		String note = (String) noteWidget.getValue(context, submission);

		AppointmentServiceDefinition service = services.stream()
		        .filter(s -> s.getUuid().equals(serviceUuid))
		        .findFirst()
		        .orElse(null);

		// Build start datetime
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		if (!isAllDay) {
			Date time = (Date) startDateTimeWidget.getTimeWidget().getValue(context, submission);
			if (time != null) {
				Calendar timeCal = Calendar.getInstance();
				timeCal.setTime(time);
				cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
				cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
			}
		} else {
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
		}
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date startDateTime = cal.getTime();

		// Build end datetime
		Date endDateTime;
		if (isAllDay) {
			Calendar endCal = Calendar.getInstance();
			endCal.setTime(date);
			endCal.set(Calendar.HOUR_OF_DAY, 23);
			endCal.set(Calendar.MINUTE, 59);
			endCal.set(Calendar.SECOND, 59);
			endCal.set(Calendar.MILLISECOND, 999);
			endDateTime = endCal.getTime();
		} else {
			Integer durationMins = null;
			String durStr = (String) durationWidget.getValue(context, submission);
			if (StringUtils.isNotBlank(durStr)) {
				try {
					durationMins = Integer.parseInt(durStr.trim());
				}
				catch (NumberFormatException e) {
					// ignore invalid input
				}
			}
			if (durationMins == null && service != null) {
				durationMins = service.getDurationMins();
			}
			endDateTime = durationMins != null
			        ? new Date(startDateTime.getTime() + (long) durationMins * 60 * 1000)
			        : null;
		}

		Appointment appointment = new Appointment();
		appointment.setPatient(session.getPatient());
		appointment.setLocation(Context.getLocationService().getLocationByUuid(locationUuid));
		appointment.setService(service);
		if (typeValue == null || typeValue.isEmpty()) {
			return;
		}
		appointment.setAppointmentKind(AppointmentKind.valueOf(typeValue));
		appointment.setStartDateTime(startDateTime);
		appointment.setEndDateTime(endDateTime);
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

		// Save the appointment UUID as a single obs so VIEW/EDIT mode can retrieve it
		if (appointmentUuidConceptMapping != null) {
			Concept uuidConcept = HtmlFormEntryUtil.getConcept(appointmentUuidConceptMapping);
			if (uuidConcept != null) {
				session.getSubmissionActions().createObs(uuidConcept, appointment.getUuid(),
				    session.getEncounter().getEncounterDatetime(), null, null, getControlFormPath(session));
			}
		}
	}

	private String getControlFormPath(FormEntrySession session) {
		if (StringUtils.isBlank(tagControlId)) {
			return null;
		}
		return session.generateControlFormPath(tagControlId, 0);
	}

	/** Returns true when the appointment spans an entire day (midnight-to-23:59). */
	private boolean isAllDayAppointment(Date start, Date end) {
		if (start == null) {
			return false;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(start);
		if (cal.get(Calendar.HOUR_OF_DAY) != 0 || cal.get(Calendar.MINUTE) != 0) {
			return false;
		}
		if (end == null) {
			return true;
		}
		cal.setTime(end);
		return cal.get(Calendar.HOUR_OF_DAY) == 23 && cal.get(Calendar.MINUTE) == 59;
	}

	private String msg(String key) {
		return Context.getMessageSourceService().getMessage(key);
	}

	private String fieldRow(String cssClass, String label, String widgetHtml) {
		return fieldRow(cssClass, label, widgetHtml, false);
	}

	private String fieldRow(String cssClass, String label, String widgetHtml, boolean required) {
		String labelHtml = required ? label + " <span class=\"required\">*</span>" : label;
		return "<p class=\"" + cssClass + "\"><label>" + labelHtml + "</label><span>" + widgetHtml + "</span></p>";
	}

	private String viewRow(String label, String value) {
		return "<p class=\"schedule-appointment-row\"><label>" + label + "</label><span class=\"value\">"
		        + StringEscapeUtils.escapeHtml(value) + "</span></p>";
	}

}
