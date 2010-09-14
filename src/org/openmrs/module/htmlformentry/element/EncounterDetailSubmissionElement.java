package org.openmrs.module.htmlformentry.element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Person;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.LocationWidget;
import org.openmrs.module.htmlformentry.widget.PersonWidget;
import org.openmrs.module.htmlformentry.widget.TimeWidget;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.util.StringUtils;

/**
 * Holds the widgets used to represent an Encounter details, and serves as both the HtmlGeneratorElement 
 * and the FormSubmissionControllerAction for Encounter details.
 */
public class EncounterDetailSubmissionElement implements HtmlGeneratorElement,
		FormSubmissionControllerAction {

	private DateWidget dateWidget;
	private ErrorWidget dateErrorWidget;
	private TimeWidget timeWidget;
	private ErrorWidget timeErrorWidget;
	private PersonWidget providerWidget;
	private ErrorWidget providerErrorWidget;
	private LocationWidget locationWidget;
	private ErrorWidget locationErrorWidget;

	public EncounterDetailSubmissionElement(FormEntryContext context,
			Map<String, Object> parameters) {
		if (Boolean.TRUE.equals(parameters.get("date"))) {
			dateWidget = new DateWidget();
			dateErrorWidget = new ErrorWidget();
			if (context.getExistingEncounter() != null) {
				dateWidget.setInitialValue(context.getExistingEncounter()
						.getEncounterDatetime());
			} else if (parameters.get("defaultDate") != null) {
				dateWidget.setInitialValue(parameters.get("defaultDate"));
			}
			if ("true".equals(parameters.get("showTime"))) {
				timeWidget = new TimeWidget();
				timeErrorWidget = new ErrorWidget();
				if (context.getExistingEncounter() != null) {
					timeWidget.setInitialValue(context.getExistingEncounter()
							.getEncounterDatetime());
				} else if (parameters.get("defaultDate") != null) {
					timeWidget.setInitialValue(parameters.get("defaultDate"));
				}
				context.registerWidget(timeWidget);
				context.registerErrorWidget(timeWidget, timeErrorWidget);
			}
			context.registerWidget(dateWidget);
			context.registerErrorWidget(dateWidget, dateErrorWidget);
		}
		if (Boolean.TRUE.equals(parameters.get("provider"))) {
			providerWidget = new PersonWidget();
			if (parameters.get("role") != null) {
				Role role = Context.getUserService().getRole(
						(String) parameters.get("role"));
				if (role == null)
					throw new RuntimeException("Cannot find role: "
							+ parameters.get("role"));
				Set<Person> options = new LinkedHashSet<Person>();
				for (User u : Context.getUserService().getUsersByRole(role)) {
					options.add(u.getPerson());
				}
				providerWidget.setOptions(new ArrayList<Person>(options));
			}
			providerErrorWidget = new ErrorWidget();
			if (context.getExistingEncounter() != null) {
				providerWidget.setInitialValue(context.getExistingEncounter()
						.getProvider());
			} else {
				String defParam = (String) parameters.get("default");
				if (StringUtils.hasText(defParam)) {
					Person defaultProvider = null;
					if ("currentuser".equalsIgnoreCase(defParam)) {
						defaultProvider = Context.getAuthenticatedUser().getPerson();
					} else {
						User providerByUsername = Context.getUserService().getUserByUsername(defParam); 
						if (providerByUsername != null) {
							defaultProvider = providerByUsername.getPerson();
						} else {
							try {
								defaultProvider = Context.getPersonService()
										.getPerson(Integer.parseInt(defParam));
							} catch (NumberFormatException ex) {
							}
						}
					}
					if (defaultProvider == null) {
						throw new IllegalArgumentException(
								"Invalid default provider specified for encounter: "
										+ defParam);
					}
					providerWidget.setInitialValue(defaultProvider);
				}
			}
			context.registerWidget(providerWidget);
			context.registerErrorWidget(providerWidget, providerErrorWidget);
		}
		if (Boolean.TRUE.equals(parameters.get("location"))) {
			locationWidget = new LocationWidget();
			if (parameters.get("order") != null) {
				List<Location> locations = new ArrayList<Location>();
				String[] temp = ((String) parameters.get("order")).split(",");
				for (String s : temp) {
					Location loc = HtmlFormEntryUtil.getLocation(s);
					if (loc == null)
						throw new RuntimeException("Cannot find location: "
								+ loc);
					locations.add(loc);
				}
				locationWidget.setOptions(locations);
			}
			locationErrorWidget = new ErrorWidget();
			if (context.getExistingEncounter() != null) {
				locationWidget.setInitialValue(context.getExistingEncounter()
						.getLocation());
			} else {
				String defaultLocId = (String) parameters.get("default");
				if (StringUtils.hasText(defaultLocId)) {
					Location defaultLoc = HtmlFormEntryUtil.getLocation(defaultLocId);
					locationWidget.setInitialValue(defaultLoc);
				}
			}
			context.registerWidget(locationWidget);
			context.registerErrorWidget(locationWidget, locationErrorWidget);
		}
	}

	public String generateHtml(FormEntryContext context) {
		StringBuilder ret = new StringBuilder();
		if (dateWidget != null) {
			ret.append(dateWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				ret.append(dateErrorWidget.generateHtml(context));
		}
		if (timeWidget != null) {
			ret.append("&nbsp;");
			ret.append(timeWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				ret.append(timeErrorWidget.generateHtml(context));
		}
		if (providerWidget != null) {
			ret.append(providerWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				ret.append(providerErrorWidget.generateHtml(context));
		}
		if (locationWidget != null) {
			ret.append(locationWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				ret.append(locationErrorWidget.generateHtml(context));
		}
		return ret.toString();
	}

	public Collection<FormSubmissionError> validateSubmission(
			FormEntryContext context, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		try {
			if (dateWidget != null) {
				Date date = (Date) dateWidget.getValue(context, submission);
				if (timeWidget != null) {
					Date time = (Date) timeWidget.getValue(context, submission);
					date = HtmlFormEntryUtil.combineDateAndTime(date, time);
				}
				if (date == null)
					throw new Exception("htmlformentry.error.required");
				if (OpenmrsUtil.compare((Date) date, new Date()) > 0)
					throw new Exception("htmlformentry.error.cannotBeInFuture");
			}
		} catch (Exception ex) {
			ret.add(new FormSubmissionError(context
					.getFieldName(dateErrorWidget), Context
					.getMessageSourceService().getMessage(ex.getMessage())));
		}

		try {
			if (providerWidget != null) {
				Object provider = providerWidget.getValue(context, submission);
				if (provider == null)
					throw new Exception("required");
			}
		} catch (Exception ex) {
			ret.add(new FormSubmissionError(context
					.getFieldName(providerErrorWidget), Context
					.getMessageSourceService().getMessage(ex.getMessage())));
		}
		try {
			if (locationWidget != null) {
				Object location = locationWidget.getValue(context, submission);
				if (location == null)
					throw new Exception("required");
			}
		} catch (Exception ex) {
			ret.add(new FormSubmissionError(context
					.getFieldName(locationErrorWidget), Context
					.getMessageSourceService().getMessage(ex.getMessage())));
		}
		return ret;
	}

	public void handleSubmission(FormEntrySession session,
			HttpServletRequest submission) {
		if (dateWidget != null) {
			Date date = (Date) dateWidget.getValue(session.getContext(),
					submission);
			session.getSubmissionActions().getCurrentEncounter()
					.setEncounterDatetime(date);
		}
		if (timeWidget != null) {
			Date time = (Date) timeWidget.getValue(session.getContext(),
					submission);
			Encounter e = session.getSubmissionActions().getCurrentEncounter();
			Date dateAndTime = HtmlFormEntryUtil.combineDateAndTime(e
					.getEncounterDatetime(), time);
			e.setEncounterDatetime(dateAndTime);
		}
		if (providerWidget != null) {
			Person person = (Person) providerWidget.getValue(session
					.getContext(), submission);
			session.getSubmissionActions().getCurrentEncounter().setProvider(
					person);
		}
		if (locationWidget != null) {
			Location location = (Location) locationWidget.getValue(session
					.getContext(), submission);
			session.getSubmissionActions().getCurrentEncounter().setLocation(
					location);
		}
	}
}
