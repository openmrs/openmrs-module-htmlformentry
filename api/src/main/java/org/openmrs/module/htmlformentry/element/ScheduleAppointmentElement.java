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
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.comparator.OptionComparator;
import org.openmrs.module.htmlformentry.tag.TagUtil;
import org.openmrs.module.htmlformentry.util.MatchMode;
import org.openmrs.module.htmlformentry.widget.DateTimeWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.ProviderAjaxAutoCompleteWidget;
import org.openmrs.module.htmlformentry.widget.NumberFieldWidget;
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

	private NumberFieldWidget durationWidget;

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

	// Appointment fetched in VIEW/EDIT mode constructor
	private Appointment existingAppointment;

	public ScheduleAppointmentElement(FormEntryContext context, Map<String, String> parameters) throws BadFormDesignException {
		appointmentUuidConceptMapping = parameters.get("appointmentUuidConcept");
		tagControlId = parameters.get("controlId");

		if (appointmentUuidConceptMapping == null) {
			throw new BadFormDesignException("scheduleAppointment tag requires appointmentUuidConcept");
		}
		Concept uuidConcept = HtmlFormEntryUtil.getConcept(appointmentUuidConceptMapping);
		if (uuidConcept == null) {
			throw new BadFormDesignException("Could not find concept for appointmentUuidConcept mapping: "
			        + appointmentUuidConceptMapping);
		}

		if (context.getMode() != Mode.ENTER) {
			// VIEW/EDIT: claim the UUID obs so HFE doesn't warn about an unmatched obs,
			// then fetch the appointment for display in generateHtml().
			Obs uuidObs = StringUtils.isNotBlank(tagControlId)
			        ? context.getObsFromExistingObs(uuidConcept, tagControlId)
			        : context.removeExistingObs(uuidConcept, (Concept) null);
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
		else {
			// ENTER mode: set up all widgets
			optional = "true".equalsIgnoreCase(parameters.get("optional"));
			id = parameters.get("id");
			scheduleChoiceWidget = new TextFieldWidget();
			context.registerWidget(scheduleChoiceWidget);

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

			List<Option> locationOptions = new ArrayList<>();
			for (Location loc : locations) {
				locationOptions.add(new Option(HtmlFormEntryUtil.format(loc), loc.getUuid(), false));
			}
			Collections.sort(locationOptions, new OptionComparator());
			for (Option opt : locationOptions) {
				locationWidget.addOption(opt);
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
					.stream().map(String::trim).collect(Collectors.toList());
			List<Speciality> allSpecialities = Context.getService(SpecialityService.class).getAllSpecialities();
			List<String> specialityFilterUuids = new ArrayList<>();
			for (String filter : specialityFilterNames) {
				Speciality match = allSpecialities.stream()
						.filter(sp -> sp.getName().equalsIgnoreCase(filter) || sp.getUuid().equalsIgnoreCase(filter))
						.findFirst().orElse(null);
				if (match == null) {
					throw new BadFormDesignException("No appointment speciality found matching: " + filter);
				}
				specialityFilterUuids.add(match.getUuid());
			}

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

			List<AppointmentServiceDefinition> services = Context.getService(AppointmentServiceDefinitionService.class)
			        .getAllAppointmentServices(false);
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

			// --- Duration ---
			durationWidget = new NumberFieldWidget(1.0, null, false);
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
	}

	@Override
	public String generateHtml(FormEntryContext context) {
		if (context.getMode() != Mode.ENTER) {
			return generateViewEditHtml();
		}

		StringBuilder sb = new StringBuilder();

		sb.append("<style>");
		sb.append(".schedule-appointment p{margin:0.3em 0;}");
		sb.append(".schedule-appointment p>label{display:block;margin-bottom:0.2em;}");
		sb.append(".schedule-appointment .field-widget{display:flex;align-items:center;gap:0.25em;flex-wrap:wrap;}");
		sb.append(".schedule-appointment-choice label{display:inline-flex;align-items:center;gap:0.3em;cursor:pointer;margin-right:1em;}");
		sb.append(".schedule-appointment-date-time-widget{display:inline-flex;align-items:center;gap:0.5em;flex-wrap:wrap;}");
		sb.append(".schedule-appt-time{display:inline-flex;align-items:center;gap:0.25em;}");
		sb.append("</style>");

		sb.append("<div class=\"schedule-appointment\"").append(id != null ? " id=\"" + id + "\"" : "").append(">");

		if (optional) {
			String choiceFieldName = context.getFieldName(scheduleChoiceWidget);
			sb.append("<p class=\"schedule-appointment-choice\">");
			sb.append("<label><input type=\"radio\" name=\"").append(choiceFieldName)
			        .append("\" value=\"yes\" checked")
			        .append(" onchange=\"this.closest('.schedule-appointment').querySelector('.schedule-appointment-fields').style.display='block'\"> ")
			        .append(msg("htmlformentry.scheduleAppointment.doSchedule")).append("</label> ");
			sb.append("<label><input type=\"radio\" name=\"").append(choiceFieldName)
			        .append("\" value=\"no\"")
			        .append(" onchange=\"this.closest('.schedule-appointment').querySelector('.schedule-appointment-fields').style.display='none'\"> ")
			        .append(msg("htmlformentry.scheduleAppointment.doNotSchedule")).append("</label>");
			sb.append("</p>");
			sb.append("<div class=\"schedule-appointment-fields\" style=\"display:block\">");
		}

		// Location
		sb.append(fieldRow("schedule-appointment-location",
		    msg("htmlformentry.scheduleAppointment.location"),
		    locationWidget.generateHtml(context) + locationErrorWidget.generateHtml(context), true));

		// Service
		sb.append(fieldRow("schedule-appointment-service",
		    msg("htmlformentry.scheduleAppointment.service"),
		    serviceWidget.generateHtml(context) + serviceErrorWidget.generateHtml(context), true));

		// Appointment Type: dropdown when multiple options, hidden input when only one
		if (typeWidget.getOptions().size() > 1) {
			sb.append(fieldRow("schedule-appointment-type",
			    msg("htmlformentry.scheduleAppointment.appointmentType"),
			    typeWidget.generateHtml(context) + typeErrorWidget.generateHtml(context), true));
		} else {
			String typeFieldName = context.getFieldName(typeWidget);
			sb.append("<input type=\"hidden\" name=\"").append(typeFieldName)
			        .append("\" value=\"").append(typeWidget.getOptions().get(0).getValue()).append("\"/>");
		}

		// Date + Time
		sb.append("<p class=\"schedule-appointment-date-time\">");
		sb.append("<label>").append(msg("htmlformentry.scheduleAppointment.date"))
		        .append(" <span class=\"required\">*</span></label>");
		sb.append("<span class=\"field-widget schedule-appointment-date-time-widget\">");
		sb.append(startDateTimeWidget.getDateWidget().generateHtml(context));
		sb.append(startDateTimeErrorWidget.generateHtml(context));
		sb.append("<span class=\"schedule-appt-time\">");
		sb.append(startDateTimeWidget.getTimeWidget().generateHtml(context));
		sb.append(timeErrorWidget.generateHtml(context));
		sb.append("</span>");
		sb.append("</span>");
		sb.append("</p>");
		sb.append("<script>setupDateTimeValidation('")
			.append(context.getFieldName(startDateTimeWidget.getDateWidget())).append("', '")
			.append(context.getFieldName(startDateTimeWidget.getTimeWidget())).append("')</script>");

		// Duration
		sb.append(fieldRow("schedule-appointment-duration",
		    msg("htmlformentry.scheduleAppointment.duration"),
		    durationWidget.generateHtml(context) + durationErrorWidget.generateHtml(context),
		    true));

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
		sb.append(viewRow("schedule-appointment-date", msg("htmlformentry.scheduleAppointment.date"), dateStr));

		// Time (or "All day")
		if (isAllDay) {
			sb.append(viewRow("schedule-appointment-time", msg("htmlformentry.scheduleAppointment.appointmentTime"),
			    msg("htmlformentry.scheduleAppointment.allDay")));
		} else {
			Calendar cal = Calendar.getInstance();
			cal.setTime(start);
			String timeStr = String.format("%02d:%02d",
			    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
			sb.append(viewRow("schedule-appointment-time", msg("htmlformentry.scheduleAppointment.appointmentTime"),
			    timeStr));
		}

		// Location
		if (existingAppointment.getLocation() != null) {
			sb.append(viewRow("schedule-appointment-location", msg("htmlformentry.scheduleAppointment.location"),
			    HtmlFormEntryUtil.format(existingAppointment.getLocation())));
		}

		// Service
		if (existingAppointment.getService() != null) {
			sb.append(viewRow("schedule-appointment-service", msg("htmlformentry.scheduleAppointment.service"),
			    existingAppointment.getService().getName()));
		}

		// Provider
		if (existingAppointment.getProviders() != null && !existingAppointment.getProviders().isEmpty()) {
			AppointmentProvider ap = existingAppointment.getProviders().iterator().next();
			if (ap.getProvider() != null) {
				sb.append(viewRow("schedule-appointment-provider", msg("htmlformentry.scheduleAppointment.provider"),
				    ap.getProvider().getName()));
			}
		}

		// Notes
		if (StringUtils.isNotBlank(existingAppointment.getComments())) {
			sb.append(viewRow("schedule-appointment-note", msg("htmlformentry.scheduleAppointment.note"),
			    existingAppointment.getComments()));
		}

		// Status
		if (existingAppointment.getStatus() != null) {
			sb.append(viewRow("schedule-appointment-status", msg("htmlformentry.scheduleAppointment.status"),
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
		if (StringUtils.isBlank(locationUuid)) {
			errors.add(new FormSubmissionError(context.getFieldName(locationErrorWidget),
			        msg("htmlformentry.scheduleAppointment.error.locationRequired")));
		}

		String serviceUuid = (String) serviceWidget.getValue(context, submission);
		if (StringUtils.isBlank(serviceUuid)) {
			errors.add(new FormSubmissionError(context.getFieldName(serviceErrorWidget),
			        msg("htmlformentry.scheduleAppointment.error.serviceRequired")));
		}

		if (typeWidget.getOptions().size() > 1) {
			String typeValue = (String) typeWidget.getValue(context, submission);
			if (StringUtils.isBlank(typeValue)) {
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

		Date time = (Date) startDateTimeWidget.getTimeWidget().getValue(context, submission);
		if (time == null) {
			errors.add(new FormSubmissionError(context.getFieldName(timeErrorWidget),
			        msg("htmlformentry.scheduleAppointment.error.timeRequired")));
		}

		Number durNum = null;
		try {
			durNum = (Number) durationWidget.getValue(context, submission);
		}
		catch (IllegalArgumentException e) {
			// non-numeric input — falls through to error
		}
		if (durNum == null || durNum.intValue() < 1) {
			errors.add(new FormSubmissionError(context.getFieldName(durationErrorWidget),
			        msg("htmlformentry.scheduleAppointment.error.durationRequired")));
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
		String typeValue = (String) typeWidget.getValue(context, submission);
		Date date = (Date) startDateTimeWidget.getDateWidget().getValue(context, submission);
		Provider selectedProvider = (Provider) providerWidget.getValue(context, submission);
		String note = (String) noteWidget.getValue(context, submission);

		AppointmentServiceDefinition service = Context.getService(AppointmentServiceDefinitionService.class)
		        .getAppointmentServiceByUuid(serviceUuid);

		// Build start datetime
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		Date time = (Date) startDateTimeWidget.getTimeWidget().getValue(context, submission);
		if (time != null) {
			Calendar timeCal = Calendar.getInstance();
			timeCal.setTime(time);
			cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
			cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
		}
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date startDateTime = cal.getTime();

		// Build end datetime
		Integer durationMins = null;
		try {
			Number durNum = (Number) durationWidget.getValue(context, submission);
			if (durNum != null) {
				durationMins = durNum.intValue();
			}
		}
		catch (IllegalArgumentException e) {
			// non-numeric input — validation should have caught this
		}
		if (durationMins == null && service != null) {
			durationMins = service.getDurationMins();
		}
		Date endDateTime = durationMins != null
		        ? new Date(startDateTime.getTime() + (long) durationMins * 60 * 1000)
		        : null;

		Appointment appointment = new Appointment();
		appointment.setPatient(session.getPatient());
		appointment.setLocation(Context.getLocationService().getLocationByUuid(locationUuid));
		appointment.setService(service);
		appointment.setAppointmentKind(AppointmentKind.valueOf(typeValue));
		appointment.setStartDateTime(startDateTime);
		appointment.setEndDateTime(endDateTime);
		appointment.setComments(note);

		AppointmentProvider appointmentProvider = new AppointmentProvider();
		appointmentProvider.setProvider(selectedProvider);
		appointmentProvider.setResponse(AppointmentProviderResponse.ACCEPTED);
		appointmentProvider.setAppointment(appointment);
		Set<AppointmentProvider> providers = new HashSet<>();
		providers.add(appointmentProvider);
		appointment.setProviders(providers);

		session.getSubmissionActions().addAppointmentToCreate(appointment);

		// Save the appointment UUID as a single obs so VIEW/EDIT mode can retrieve it
		Concept uuidConcept = HtmlFormEntryUtil.getConcept(appointmentUuidConceptMapping);
		if (uuidConcept != null) {
			session.getSubmissionActions().createObs(uuidConcept, appointment.getUuid(),
				session.getEncounter().getEncounterDatetime(), null, null, getControlFormPath(session));
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
		return HtmlFormEntryUtil.translate(key);
	}

	private String fieldRow(String cssClass, String label, String widgetHtml) {
		return fieldRow(cssClass, label, widgetHtml, false);
	}

	private String fieldRow(String cssClass, String label, String widgetHtml, boolean required) {
		String labelHtml = required ? label + " <span class=\"required\">*</span>" : label;
		return "<p class=\"" + cssClass + "\"><label>" + labelHtml + "</label><span class=\"field-widget\">" + widgetHtml + "</span></p>";
	}

	private String viewRow(String cssClass, String label, String value) {
		return "<p class=\"schedule-appointment-row " + cssClass + "\"><label>" + label + "</label><span class=\"value\">"
		        + StringEscapeUtils.escapeHtml(value) + "</span></p>";
	}

}
