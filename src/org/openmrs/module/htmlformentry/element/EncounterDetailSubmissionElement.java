package org.openmrs.module.htmlformentry.element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Location;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.LocationWidget;
import org.openmrs.module.htmlformentry.widget.UserWidget;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.util.StringUtils;

public class EncounterDetailSubmissionElement implements HtmlGeneratorElement,
        FormSubmissionControllerAction {

    private DateWidget dateWidget;
    private ErrorWidget dateErrorWidget;
    private UserWidget providerWidget;
    private ErrorWidget providerErrorWidget;
    private LocationWidget locationWidget;
    private ErrorWidget locationErrorWidget;
    
    public EncounterDetailSubmissionElement(FormEntryContext context, Map<String, Object> parameters) {
        if (Boolean.TRUE.equals(parameters.get("date"))) {
            dateWidget = new DateWidget();
            dateErrorWidget = new ErrorWidget();
            if (context.getExistingEncounter() != null) {
                dateWidget.setInitialValue(context.getExistingEncounter().getEncounterDatetime());
            } else if (parameters.get("defaultDate") != null) {
                dateWidget.setInitialValue(parameters.get("defaultDate"));
            }
            context.registerWidget(dateWidget);
            context.registerErrorWidget(dateWidget, dateErrorWidget);
        }
        if (Boolean.TRUE.equals(parameters.get("provider"))) {
            providerWidget = new UserWidget();
            if (parameters.get("role") != null) {
                Role role = Context.getUserService().getRole((String) parameters.get("role"));
                if (role == null)
                    throw new RuntimeException("Cannot find role: " + parameters.get("role"));
                List<User> options = Context.getUserService().getUsersByRole(role);
                providerWidget.setOptions(options);
            }
            providerErrorWidget = new ErrorWidget();
            if (context.getExistingEncounter() != null) {
                providerWidget.setInitialValue(context.getExistingEncounter().getProvider());
            }
            else {
            	String defParam = (String)parameters.get("default");
            	if (StringUtils.hasText(defParam)) {
            		User defaultProvider = Context.getUserService().getUserByUsername(defParam);
            		if (defaultProvider == null) {
            			defaultProvider = Context.getUserService().getUser(Integer.parseInt(defParam));
            		}
            		if (defaultProvider == null) {
            			throw new IllegalArgumentException("Invalid default provider specified for encounter: " + defParam);
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
                    Location loc;
                    try {
                        loc = Context.getLocationService().getLocation(Integer.valueOf(s));
                    } catch (NumberFormatException ex) {
                        loc = Context.getLocationService().getLocation(s);
                    }
                    if (loc == null)
                        throw new RuntimeException("Cannot find location: " + loc);
                    locations.add(loc);
                }
                locationWidget.setOptions(locations);
            }
            locationErrorWidget = new ErrorWidget();
            if (context.getExistingEncounter() != null) {
                locationWidget.setInitialValue(context.getExistingEncounter().getLocation());
            }
            else {
            	String defaultLocId = (String)parameters.get("default");
            	if (StringUtils.hasText(defaultLocId)) {
            		Location defaultLoc = Context.getLocationService().getLocation(Integer.parseInt(defaultLocId));
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
            ret.append(dateErrorWidget.generateHtml(context));
        }
        if (providerWidget != null) {
            ret.append(providerWidget.generateHtml(context));
            ret.append(providerErrorWidget.generateHtml(context));
        }
        if (locationWidget != null) {
            ret.append(locationWidget.generateHtml(context));
            ret.append(locationErrorWidget.generateHtml(context));
        }
        return ret.toString();
    }

    public Collection<FormSubmissionError> validateSubmission(
            FormEntryContext context, HttpServletRequest submission) {
        List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
        try {
            if (dateWidget != null) {
                Object date = dateWidget.getValue(context, submission);
                if (date == null)
                    throw new Exception("htmlformentry.error.required");
                if (OpenmrsUtil.compare((Date) date, new Date()) > 0)
                    throw new Exception("htmlformentry.error.cannotBeInFuture");
            }
        } catch (Exception ex) {
            ret.add(new FormSubmissionError(context.getFieldName(dateErrorWidget), Context.getMessageSourceService().getMessage(ex.getMessage())));
        }
        try {
            if (providerWidget != null) {
                Object provider = providerWidget.getValue(context, submission);
                if (provider == null)
                    throw new Exception("required");
            }
        } catch (Exception ex) {
            ret.add(new FormSubmissionError(context.getFieldName(providerErrorWidget), Context.getMessageSourceService().getMessage(ex.getMessage())));
        }
        try {
            if (locationWidget != null) {
                Object location = locationWidget.getValue(context, submission);
                if (location == null)
                    throw new Exception("required");
            }
        } catch (Exception ex) {
            ret.add(new FormSubmissionError(context.getFieldName(locationErrorWidget), Context.getMessageSourceService().getMessage(ex.getMessage())));
        }
        return ret;
    }

    public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
        if (dateWidget != null) {
            Date date = (Date) dateWidget.getValue(session.getContext(), submission);
            session.getSubmissionActions().getCurrentEncounter().setEncounterDatetime(date);
        }
        if (providerWidget != null) {
            User user = (User) providerWidget.getValue(session.getContext(), submission);
            session.getSubmissionActions().getCurrentEncounter().setProvider(user);
        }
        if (locationWidget != null) {
            Location location = (Location) locationWidget.getValue(session.getContext(), submission);
            session.getSubmissionActions().getCurrentEncounter().setLocation(location);
        }
    }
    
}
