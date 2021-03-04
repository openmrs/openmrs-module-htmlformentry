package org.openmrs.module.htmlformentry.element;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateMidnight;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Person;
import org.openmrs.Role;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryConstants;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.MetadataMappingResolver;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.comparator.OptionComparator;
import org.openmrs.module.htmlformentry.compatibility.EncounterCompatibility;
import org.openmrs.module.htmlformentry.widget.AutocompleteWidget;
import org.openmrs.module.htmlformentry.widget.CheckboxWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.EncounterTypeWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.SingleOptionWidget;
import org.openmrs.module.htmlformentry.widget.TimeWidget;
import org.openmrs.module.htmlformentry.widget.ToggleWidget;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.util.RoleConstants;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds the widgets used to represent an Encounter details, and serves as both the
 * HtmlGeneratorElement and the FormSubmissionControllerAction for Encounter details.
 */
public class EncounterDetailSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	private String id;
	
	private DateWidget dateWidget;
	
	private ErrorWidget dateErrorWidget;
	
	private TimeWidget timeWidget;
	
	private ErrorWidget timeErrorWidget;
	
	private SingleOptionWidget providerWidget;
	
	private ErrorWidget providerErrorWidget;
	
	private SingleOptionWidget locationWidget;
	
	private ErrorWidget locationErrorWidget;
	
	private ToggleWidget toggleWidget;
	
	private CheckboxWidget voidWidget;
	
	private ErrorWidget voidErrorWidget;
	
	private EncounterTypeWidget encounterTypeWidget;
	
	private ErrorWidget encounterTypeErrorWidget;
	
	boolean locationRequired = true;
	
	boolean providerRequired = true;
	
	private MetadataMappingResolver getMetadataMappingResolver() {
		return Context.getRegisteredComponent("metadataMappingResolver", MetadataMappingResolver.class);
	}
	
	/**
	 * Construct a new EncounterDetailSubmissionElement
	 *
	 * @param context
	 * @param parameters <strong>Should</strong> display 'Enter' option if 'type' is set to Autocomplete
	 */
	public EncounterDetailSubmissionElement(FormEntryContext context, Map<String, Object> parameters) {
		
		// Register Date and Time widgets, if appropriate
		if (Boolean.TRUE.equals(parameters.get("date"))) {
			
			dateWidget = new DateWidget();
			dateErrorWidget = new ErrorWidget();
			
			if (context.getExistingEncounter() != null) {
				dateWidget.setInitialValue(context.getExistingEncounter().getEncounterDatetime());
			} else if (context.getDefaultEncounterDate() != null) {
				dateWidget.setInitialValue(context.getDefaultEncounterDate());
			} else if (parameters.get("defaultDate") != null) {
				dateWidget.setInitialValue(parameters.get("defaultDate"));
			}
			
			if (parameters.get("disallowMultipleEncountersOnDate") != null
			        && StringUtils.hasText((String) parameters.get("disallowMultipleEncountersOnDate"))) {
				dateWidget.setOnChangeFunction(
				    "existingEncounterOnDate(this, '" + parameters.get("disallowMultipleEncountersOnDate") + "') ");
			}
			
			if ("true".equals(parameters.get("showTime"))) {
				timeWidget = new TimeWidget();
				timeErrorWidget = new ErrorWidget();
				if (context.getExistingEncounter() != null) {
					timeWidget.setInitialValue(context.getExistingEncounter().getEncounterDatetime());
				} else if (parameters.get("defaultDate") != null) {
					timeWidget.setInitialValue(parameters.get("defaultDate"));
				}
				if ("true".equals((parameters.get("hideSeconds")))) {
					timeWidget.setHideSeconds(true);
				}
				context.registerWidget(timeWidget);
				context.registerErrorWidget(timeWidget, timeErrorWidget);
			}
			context.registerWidget(dateWidget);
			context.registerErrorWidget(dateWidget, dateErrorWidget);
			
			if ("hidden".equals(parameters.get("widget"))) {
				dateWidget.setHidden(true);
				if (timeWidget != null) {
					timeWidget.setHidden(true);
				}
			}
		}
		
		// Register Provider widgets, if appropriate
		if (Boolean.TRUE.equals(parameters.get("provider"))) {
			
			if ("autocomplete".equals(parameters.get("type"))) {
				providerWidget = new AutocompleteWidget(Person.class);
			} else {
				providerWidget = new DropdownWidget();
			}
			providerErrorWidget = new ErrorWidget();
			
			List<Option> providerOptions = new ArrayList<Option>();
			// If specific persons are specified, display only those persons in order
			String personsParam = (String) parameters.get("persons");
			if (personsParam != null) {
				for (String s : personsParam.split(",")) {
					Person p = HtmlFormEntryUtil.getPerson(s);
					if (p == null) {
						throw new RuntimeException("Cannot find Person: " + s);
					}
					String label = StringEscapeUtils.escapeHtml(p.getPersonName().getFullName());
					providerOptions.add(new Option(label, p.getId().toString(), false));
				}
				removeNonProviders(providerOptions);
			}
			
			// Only if specific person ids are not passed in do we get by user Role
			if (providerOptions.isEmpty()) {
				
				List<PersonStub> users = new ArrayList<PersonStub>();
				List<Option> providerUsers = new ArrayList<Option>();
				
				// If the "role" attribute is passed in, limit to users with this role
				String roleParam = (String) parameters.get("role");
				if (roleParam != null) {
					Role role = Context.getUserService().getRoleByUuid(roleParam);
					if (role == null) {
						MetadataMappingResolver metadataMappingResolver = getMetadataMappingResolver();
						role = metadataMappingResolver.getMetadataItem(Role.class, roleParam);
						if (role == null) {
							role = Context.getUserService().getRole((String) roleParam);
						}
						
						if (role == null) {
							throw new RuntimeException("Cannot find role: " + roleParam);
						} else {
							users = Context.getService(HtmlFormEntryService.class).getUsersAsPersonStubs(role.getRole());
						}
					}
				}
				// Otherwise, use default options
				else {
					users = getAllProvidersThatArePersonsAsPersonStubs();
				}
				
				for (PersonStub personStub : users) {
					String optionLabel = StringEscapeUtils.escapeHtml(personStub.toString());
					Option option = new Option(optionLabel, personStub.getId().toString(), false);
					providerUsers.add(option);
				}
				providerOptions.addAll(providerUsers);
			}
			
			Collections.sort(providerOptions, new OptionComparator());
			
			if (("autocomplete").equals(parameters.get("type"))) {
				providerWidget.addOption(new Option());
			} else {
				String emptyLabel = Context.getMessageSourceService().getMessage("htmlformentry.chooseAProvider");
				providerWidget.addOption(new Option(emptyLabel, "", false));
			}
			if (!providerOptions.isEmpty()) {
				for (Option option : providerOptions) {
					providerWidget.addOption(option);
				}
			}
			
			// Configure the initial value of the widget with any provider value from an existing encounter
			Person initialProviderValue = null;
			if (context.getExistingEncounter() != null) {
				initialProviderValue = EncounterCompatibility.getProvider(context.getExistingEncounter());
			}
			// If there is no existing encounter, then set the initial value with configured defaults, if they match valid options
			else {
				Person defaultProvider = null;
				String defParam = (String) parameters.get("default");
				if (StringUtils.hasText(defParam)) {
					if ("currentuser".equalsIgnoreCase(defParam)) {
						defaultProvider = Context.getAuthenticatedUser().getPerson();
					} else {
						defaultProvider = HtmlFormEntryUtil.getPerson(defParam);
					}
					if (defaultProvider == null) {
						throw new IllegalArgumentException("Invalid default provider specified for encounter: " + defParam);
					}
				}
				if (defaultProvider != null && providerWidget.getOptions() != null) {
					for (Option o : providerWidget.getOptions()) {
						if (OpenmrsUtil.nullSafeEquals(defaultProvider.getId().toString(), o.getValue())) {
							initialProviderValue = defaultProvider;
						}
					}
				}
			}
			
			if (initialProviderValue != null) {
				providerWidget.setInitialValue(new PersonStub(initialProviderValue));
			}
			
			// Set the selected provider option appropriately
			if (providerWidget.getOptions() != null) {
				String selectedVal = initialProviderValue == null ? "" : initialProviderValue.getId().toString();
				for (Option o : providerWidget.getOptions()) {
					o.setSelected(OpenmrsUtil.nullSafeEquals(selectedVal, o.getValue()));
				}
			}
			
			if (parameters.containsKey("required")) {
				providerRequired = Boolean.valueOf((String) parameters.get("required"));
			}
			context.registerWidget(providerWidget);
			context.registerErrorWidget(providerWidget, providerErrorWidget);
		}
		
		if (Boolean.TRUE.equals(parameters.get("encounterType"))) {
			encounterTypeWidget = new EncounterTypeWidget();
			encounterTypeErrorWidget = new ErrorWidget();
			if (parameters.get("types") != null) {
				List<EncounterType> encounterTypes = new ArrayList<EncounterType>();
				String[] temp = ((String) parameters.get("types")).split(",");
				for (String s : temp) {
					EncounterType type = HtmlFormEntryUtil.getEncounterType(s);
					if (type == null) {
						throw new RuntimeException("Cannot find encounter type: " + s);
					}
					encounterTypes.add(type);
				}
				
				encounterTypeWidget.setOptions(encounterTypes);
			}
			// Set default values
			
			EncounterType defaultEncounterType = null;
			if (context.getExistingEncounter() != null) {
				defaultEncounterType = context.getExistingEncounter().getEncounterType();
			} else {
				String defaultTypeId = (String) parameters.get("default");
				if (StringUtils.hasText(defaultTypeId)) {
					defaultEncounterType = HtmlFormEntryUtil.getEncounterType(defaultTypeId);
				}
			}
			
			encounterTypeWidget.setInitialValue(defaultEncounterType);
			context.registerWidget(encounterTypeWidget);
			context.registerErrorWidget(encounterTypeWidget, encounterTypeErrorWidget);
		}
		
		// Register Location widgets, if appropriate
		if (Boolean.TRUE.equals(parameters.get("location"))) {
			
			locationErrorWidget = new ErrorWidget();
			List<Location> locations = new ArrayList<Location>();
			List<Option> locationOptions = new ArrayList<Option>();
			
			if ("autocomplete".equals(parameters.get("type"))) {
				locationWidget = new AutocompleteWidget(Location.class);
			} else {
				locationWidget = new DropdownWidget();
			}
			
			if (parameters.get("tags") != null && parameters.get("orders") != null) {
				throw new RuntimeException(
				        "Using both \"order\" and \"tags\" attribute in an encounterLocation tag is not currently supported");
			}
			if (parameters.get("restrictToSupportedVisitLocations") != null) {
				Set<Location> allVisitLocations = new HashSet<Location>();
				Set<Location> traversedLocations = new HashSet<Location>();
				Set<Location> allVisitsAndChildLocations = new HashSet<Location>();
				if ("true".equalsIgnoreCase(parameters.get("restrictToSupportedVisitLocations").toString())) {
					LocationTag visitLocationTag = HtmlFormEntryUtil.getLocationTag("Visit Location");
					List<Location> visitLocations = Context.getLocationService().getLocationsByTag(visitLocationTag);
					allVisitLocations.addAll(visitLocations);
					allVisitsAndChildLocations = getAllVisitsAndChildLocations(allVisitLocations, traversedLocations);
					locations.addAll(allVisitsAndChildLocations);
					
				} else if (!"false".equalsIgnoreCase(parameters.get("restrictToSupportedVisitLocations").toString())) {
					
					throw new RuntimeException("Invalid value for restrictToSupportedVisitLocations,use true or false");
				}
				
			}
			
			// if the "tags" attribute has been specified, load all the locations referenced by tag
			if (parameters.get("tags") != null) {
				
				List<LocationTag> tags = new ArrayList<LocationTag>();
				String temp[] = ((String) parameters.get("tags")).split(",");
				for (String s : temp) {
					
					LocationTag tag = HtmlFormEntryUtil.getLocationTag(s);
					if (tag == null) {
						throw new RuntimeException("Cannot find tag: " + tag);
					}
					tags.add(tag);
				}
				locations.addAll(Context.getLocationService().getLocationsHavingAnyTag(tags));
			}
			// If the "order" attribute is passed in, limit to the specified locations in order
			else if (parameters.get("order") != null) {
				
				String[] temp = ((String) parameters.get("order")).split(",");
				for (String s : temp) {
					Location loc = HtmlFormEntryUtil.getLocation(s, context);
					if (loc == null) {
						throw new RuntimeException("Cannot find location: " + loc);
					}
					locations.add(loc);
				}
				
			}
			
			// if no locations have been specified by the order attribute, use all non-retired locations
			if (locations.isEmpty()) {
				locations = Context.getLocationService().getAllLocations(false);
			}
			
			// restrict to visit locations if necessary
			if ((parameters.get("restrictToCurrentVisitLocation") != null
			        && "true".equalsIgnoreCase(parameters.get("restrictToCurrentVisitLocation").toString())
			        || "true".equalsIgnoreCase(Context.getAdministrationService().getGlobalProperty(
			            HtmlFormEntryConstants.GP_RESTRICT_ENCOUNTER_LOCATION_TO_CURRENT_VISIT_LOCATION)))
			        && context.getVisit() != null) {
				locations = removeLocationsNotEqualToOrDescendentOf(locations, ((Visit) context.getVisit()).getLocation());
			}
			
			// now create the actual location options
			for (Location location : locations) {
				String label = HtmlFormEntryUtil.format(location);
				Option option = new Option(label, location.getId().toString(), false);
				locationOptions.add(option);
			}
			
			// sort options (if a specific order hasn't been specified
			if (parameters.get("order") == null) {
				Collections.sort(locationOptions, new OptionComparator());
			}
			
			if ("autocomplete".equals(parameters.get("type"))) {
				locationWidget.addOption(new Option());
				if (!locationOptions.isEmpty()) {
					locationWidget.setOptions(locationOptions);
				}
			} else {
				boolean initialValueIsSet = !(locationWidget.getInitialValue() == null);
				String emptyLabel = Context.getMessageSourceService().getMessage("htmlformentry.chooseALocation");
				locationWidget.addOption(new Option(emptyLabel, "", !initialValueIsSet));
				if (!locationOptions.isEmpty()) {
					for (Option option : locationOptions) {
						locationWidget.addOption(option);
					}
				}
			}
			
			// Configure the initial value of the widget with any location value from an existing encounter
			Location initialValue = null;
			if (context.getExistingEncounter() != null) {
				initialValue = context.getExistingEncounter().getLocation();
			}
			// If there is no existing encounter, then set the initial value with configured defaults, if they match valid options
			else {
				Location defaultLocation = null;
				String defaultLocId = (String) parameters.get("default");
				if (StringUtils.hasText(defaultLocId)) {
					defaultLocation = HtmlFormEntryUtil.getLocation(defaultLocId, context);
				}
				defaultLocation = defaultLocation == null ? context.getDefaultLocation() : defaultLocation;
				if (defaultLocation != null && locationWidget.getOptions() != null) {
					for (Option o : locationWidget.getOptions()) {
						if (OpenmrsUtil.nullSafeEquals(defaultLocation.getId().toString(), o.getValue())) {
							initialValue = defaultLocation;
						}
					}
				}
			}
			
			// Set the initial value of the widget based on the above
			locationWidget.setInitialValue(initialValue);
			
			// Set the selected location option appropriately
			if (locationWidget.getOptions() != null) {
				String selectedVal = initialValue == null ? "" : initialValue.getId().toString();
				for (Option o : locationWidget.getOptions()) {
					o.setSelected(OpenmrsUtil.nullSafeEquals(selectedVal, o.getValue()));
				}
			}
			
			if (parameters.containsKey("required")) {
				locationRequired = Boolean.valueOf((String) parameters.get("required"));
			}
			context.registerWidget(locationWidget);
			context.registerErrorWidget(locationWidget, locationErrorWidget);
		}
		
		if (Boolean.TRUE.equals(parameters.get("showVoidEncounter")) && context.getMode() == Mode.EDIT) { //only show void option if the encounter already exists.  And VIEW implies not voided.
			if (parameters.get("toggle") != null) {
				ToggleWidget toggleWidget = new ToggleWidget((String) parameters.get("toggle"));
				voidWidget = new CheckboxWidget(" " + Context.getMessageSourceService().getMessage("general.voided"),
				        (context.getExistingEncounter() != null && context.getExistingEncounter().isVoided().equals(true))
				                ? "true"
				                : "false",
				        toggleWidget.getTargetId(), toggleWidget.isToggleDim());
			} else {
				voidWidget = new CheckboxWidget();
			}
			voidWidget.setLabel(" " + Context.getMessageSourceService().getMessage("general.voided"));
			voidErrorWidget = new ErrorWidget();
			if (context.getExistingEncounter() != null && context.getExistingEncounter().isVoided().equals(true))
				voidWidget.setInitialValue("true");
			context.registerWidget(voidWidget);
			context.registerErrorWidget(voidWidget, voidErrorWidget);
		}
		
		// set the id, if it has been specified
		if (parameters.get("id") != null) {
			id = (String) parameters.get("id");
		}
		
	}
	
	/**
	 * This method exists to allow us to quickly support providers as introduce in OpenMRS 1.9.x,
	 * without having to branch the module. We should remove this method when do a proper
	 * implementation.
	 *
	 * @param persons
	 */
	private void removeNonProviders(List<Option> persons) {
		Set<Integer> legalPersonIds = getAllProviderPersonIds();
		for (Iterator<Option> i = persons.iterator(); i.hasNext();) {
			Option candidate = i.next();
			if (!legalPersonIds.contains(Integer.parseInt(candidate.getValue())))
				i.remove();
		}
	}
	
	/**
	 * This method exists to allow us to quickly support providers as introduce in OpenMRS 1.9.x,
	 * without having to branch the module. We should remove this method when do a proper
	 * implementation.
	 *
	 * @return all providers that are attached to persons
	 */
	private List<Object> getAllProvidersThatArePersons() {
		try {
			Object providerService = Context.getService(Context.loadClass("org.openmrs.api.ProviderService"));
			Method getProvidersMethod = providerService.getClass().getMethod("getAllProviders");
			@SuppressWarnings("rawtypes")
			List allProviders = (List) getProvidersMethod.invoke(providerService);
			List<Object> ret = new ArrayList<Object>();
			for (Object provider : allProviders) {
				Person person = (Person) PropertyUtils.getProperty(provider, "person");
				if (person != null)
					ret.add(provider);
			}
			return ret;
		}
		catch (Exception ex) {
			throw new RuntimeException("Programming error in HTML Form Entry module. This method should be safe!", ex);
		}
	}
	
	/**
	 * This method exists to allow us to quickly support providers as introduce in OpenMRS 1.9.x,
	 * without having to branch the module. We should remove this method when do a proper
	 * implementation.
	 *
	 * @return person stubs for all providers that are attached to persons
	 */
	private List<PersonStub> getAllProvidersThatArePersonsAsPersonStubs() {
		try {
			List<PersonStub> ret = new ArrayList<PersonStub>();
			for (Object provider : getAllProvidersThatArePersons()) {
				Person person = (Person) PropertyUtils.getProperty(provider, "person");
				ret.add(new PersonStub(person));
			}
			return ret;
		}
		catch (Exception ex) {
			throw new RuntimeException("Programming error in HTML Form Entry module. This method should be safe!", ex);
		}
	}
	
	/**
	 * This method exists to allow us to quickly support providers as introduce in OpenMRS 1.9.x,
	 * without having to branch the module. We should remove this method when do a proper
	 * implementation.
	 *
	 * @return personIds of all providers that are attached to persons
	 */
	private Set<Integer> getAllProviderPersonIds() {
		try {
			Set<Integer> ret = new HashSet<Integer>();
			for (Object candidate : getAllProvidersThatArePersons()) {
				Person person = (Person) PropertyUtils.getProperty(candidate, "person");
				if (person != null)
					ret.add(person.getPersonId());
			}
			return ret;
		}
		catch (Exception ex) {
			throw new RuntimeException("Programming error in HTML Form Entry module. This method should be safe!", ex);
		}
	}
	
	/**
	 * Iterates through the "locations" list and removed all that are not equal to, or a descendent of,
	 * the "testLocation"
	 *
	 * @param locations
	 * @param testLocation
	 * @return
	 */
	private List<Location> removeLocationsNotEqualToOrDescendentOf(List<Location> locations, Location testLocation) {
		
		if (testLocation == null) {
			return locations;
		}
		
		Iterator<Location> i = locations.iterator();
		
		while (i.hasNext()) {
			if (!isLocationEqualToOrDescendentOf(i.next(), testLocation)) {
				i.remove();
			}
		}
		
		return locations;
	}
	
	/**
	 * Returns true/false whether the given location is equal to, or a descendent of, "testLocation"
	 *
	 * @param location
	 * @param testLocation
	 * @return
	 */
	private Boolean isLocationEqualToOrDescendentOf(Location location, Location testLocation) {
		if (location == null) {
			return false;
		} else if (location.equals(testLocation)) {
			return true;
		} else {
			return isLocationEqualToOrDescendentOf(location.getParentLocation(), testLocation);
		}
	}
	
	/**
	 * @see HtmlGeneratorElement#generateHtml(FormEntryContext)
	 */
	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder ret = new StringBuilder();
		
		if (id != null || !context.getMode().equals(Mode.VIEW)) {
			// wrap the whole encounter element in a span tag so that we access property values via javascript
			// note that if this element ever handles multiple widgets, the names of the provider and location accessors will need unique names
			if (dateWidget != null) {
				id = id != null ? id : "encounterDate";
				context.registerPropertyAccessorInfo(id + ".value", context.getFieldNameIfRegistered(dateWidget),
				    "dateFieldGetterFunction", null, "dateSetterFunction");
				context.registerPropertyAccessorInfo(id + ".error", context.getFieldNameIfRegistered(dateErrorWidget), null,
				    null, null);
			} else if (providerWidget != null) {
				id = id != null ? id : "encounterProvider";
				context.registerPropertyAccessorInfo(id + ".value", context.getFieldNameIfRegistered(providerWidget), null,
				    getGetterFunction(providerWidget), getSetterFunction(providerWidget));
				context.registerPropertyAccessorInfo(id + ".error", context.getFieldNameIfRegistered(providerErrorWidget),
				    null, null, null);
			} else if (locationWidget != null) {
				id = id != null ? id : "encounterLocation";
				context.registerPropertyAccessorInfo(id + ".value", context.getFieldNameIfRegistered(locationWidget), null,
				    getGetterFunction(locationWidget), getSetterFunction(locationWidget));
				context.registerPropertyAccessorInfo(id + ".error", context.getFieldNameIfRegistered(locationErrorWidget),
				    null, null, null);
			} else if (encounterTypeWidget != null) {
				id = id != null ? id : "encounterType";
				context.registerPropertyAccessorInfo(id + ".value", context.getFieldNameIfRegistered(encounterTypeWidget),
				    null, null, null);
				context.registerPropertyAccessorInfo(id + ".error",
				    context.getFieldNameIfRegistered(encounterTypeErrorWidget), null, null, null);
			}
			ret.append("<span id='" + id + "'>");
		}
		
		if (dateWidget != null) {
			ret.append(dateWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				ret.append(dateErrorWidget.generateHtml(context));
		}
		if (timeWidget != null) {
			if (!timeWidget.isHidden() || (context.getMode() == Mode.VIEW)) {
				ret.append("&#160;");
			}
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
		if (encounterTypeWidget != null) {
			ret.append(encounterTypeWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				ret.append(encounterTypeErrorWidget.generateHtml(context));
		}
		if (voidWidget != null) {
			if (context.getMode() == Mode.EDIT) //only show void option if the encounter already exists.
				ret.append(voidWidget.generateHtml(context));
		}
		
		// close out the span if we have an id tag
		if (id != null) {
			ret.append("</span>");
		}
		
		return ret.toString();
	}
	
	/**
	 * selects the correct setter function for provider/location widgets according to its type
	 *
	 * @param widget- dropdown or autocomplete
	 * @return
	 */
	private String getGetterFunction(Widget widget) {
		if (widget == null) {
			return null;
		}
		if (widget instanceof AutocompleteWidget) {
			return "autocompleteGetterFunction";
		}
		return null;
	}
	
	/**
	 * selects the correct getter function for provider/location widgets according to its type
	 *
	 * @param widget - dropdown or autocomplete
	 * @return
	 */
	private String getSetterFunction(Widget widget) {
		if (widget == null) {
			return null;
		}
		if (widget instanceof AutocompleteWidget) {
			return "autocompleteSetterFunction";
		}
		return null;
	}
	
	/**
	 * @see FormSubmissionControllerAction#validateSubmission(FormEntryContext, HttpServletRequest)
	 */
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		
		try {
			if (dateWidget != null) {
				Date date = (Date) dateWidget.getValue(context, submission);
				if (timeWidget != null) {
					Date time = (Date) timeWidget.getValue(context, submission);
					String timeZone = timeWidget.getTimeZone(context, submission);
					date = HtmlFormEntryUtil.combineDateAndTime(date, time, timeZone);
				}
				if (date == null)
					throw new Exception("htmlformentry.error.required");
				if (OpenmrsUtil.compare((Date) date, new Date()) > 0)
					throw new Exception("htmlformentry.error.cannotBeInFuture");
			}
		}
		catch (Exception ex) {
			ret.add(new FormSubmissionError(context.getFieldName(dateErrorWidget),
			        Context.getMessageSourceService().getMessage(ex.getMessage())));
		}
		
		try {
			if (providerWidget != null) {
				Object value = providerWidget.getValue(context, submission);
				if (value == null) {
					if (providerRequired) {
						throw new Exception("required");
					}
				} else {
					Person provider = (Person) convertValueToProvider(value);
					if (provider == null) {
						if (providerRequired) {
							throw new Exception("required");
						}
					}
				}
			}
		}
		catch (Exception ex) {
			ret.add(new FormSubmissionError(context.getFieldName(providerErrorWidget),
			        Context.getMessageSourceService().getMessage(ex.getMessage())));
		}
		
		try {
			if (locationWidget != null) {
				Object value = locationWidget.getValue(context, submission);
				if (value == null) {
					if (locationRequired) {
						throw new Exception("required");
					}
				} else {
					Location location = (Location) HtmlFormEntryUtil.convertToType(value.toString().trim(), Location.class);
					if (location == null) {
						log.debug("Location Required " + locationRequired);
						if (locationRequired) {
							
							throw new Exception("required");
						}
					}
				}
			}
		}
		catch (Exception ex) {
			ret.add(new FormSubmissionError(context.getFieldName(locationErrorWidget),
			        Context.getMessageSourceService().getMessage(ex.getMessage())));
		}
		try {
			if (encounterTypeWidget != null) {
				Object encounterType = encounterTypeWidget.getValue(context, submission);
				System.out.println("the location widget initial value " + encounterTypeWidget.getClass() + " actual value "
				        + encounterType);
				if (encounterType == null)
					throw new Exception("required");
			}
		}
		catch (Exception ex) {
			ret.add(new FormSubmissionError(context.getFieldName(encounterTypeErrorWidget),
			        Context.getMessageSourceService().getMessage(ex.getMessage())));
		}
		return ret;
	}
	
	/**
	 * Gets provider id and obtains the Provider from it
	 *
	 * @param value - provider id
	 * @return the Provider object of corresponding id
	 */
	private Object convertValueToProvider(Object value) {
		String val = (String) value;
		if (StringUtils.hasText(val)) {
			return HtmlFormEntryUtil.convertToType(val.trim(), Person.class);
		}
		return null;
	}
	
	/**
	 * @see FormSubmissionControllerAction#handleSubmission(FormEntrySession, HttpServletRequest)
	 */
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		if (dateWidget != null) {
			Date date = (Date) dateWidget.getValue(session.getContext(), submission);
			Date previousDate = session.getSubmissionActions().getCurrentEncounter().getEncounterDatetime();
			
			if (previousDate == null) {
				session.getSubmissionActions().getCurrentEncounter().setEncounterDatetime(date);
			}
			
			else {
				// we don't want to lose any time information just because we edited it with a form that only collects date,
				// so we only update the date if the date has a time component or the actual date has changed
				if (hasTimeComponent(date) || !stripTimeComponent(date).equals(stripTimeComponent(previousDate))) {
					session.getContext().setPreviousEncounterDate(
					    new Date(session.getSubmissionActions().getCurrentEncounter().getEncounterDatetime().getTime()));
					session.getSubmissionActions().getCurrentEncounter().setEncounterDatetime(date);
				}
			}
		}
		if (timeWidget != null) {
			Date time = (Date) timeWidget.getValue(session.getContext(), submission);
			Encounter e = session.getSubmissionActions().getCurrentEncounter();
			String timeZone = timeWidget.getTimeZone(session.getContext(), submission);
			Date dateAndTime = HtmlFormEntryUtil.combineDateAndTime(e.getEncounterDatetime(), time, timeZone);
			e.setEncounterDatetime(dateAndTime);
		}
		if (providerWidget != null) {
			Object value = providerWidget.getValue(session.getContext(), submission);
			if (value != null) {
				Person person = (Person) convertValueToProvider(value);
				EncounterCompatibility.setProvider(session.getSubmissionActions().getCurrentEncounter(), person);
			}
		}
		if (locationWidget != null) {
			Object value = locationWidget.getValue(session.getContext(), submission);
			if (value != null) {
				Location location = (Location) HtmlFormEntryUtil.convertToType(value.toString().trim(), Location.class);
				session.getSubmissionActions().getCurrentEncounter().setLocation(location);
			}
		}
		if (encounterTypeWidget != null) {
			EncounterType encounterType = (EncounterType) encounterTypeWidget.getValue(session.getContext(), submission);
			session.getSubmissionActions().getCurrentEncounter().setEncounterType(encounterType);
		}
		if (voidWidget != null) {
			if ("true".equals(voidWidget.getValue(session.getContext(), submission))) {
				session.setVoidEncounter(true);
			} else if ("false".equals(voidWidget.getValue(session.getContext(), submission))) {
				//nothing..  the session.voidEncounter property will be false, and the encounter will be un-voided if already voided
				//otherwise, nothing will happen.  99% of the time the encounter won't be voided to begin with.
			}
		}
	}
	
	private boolean hasTimeComponent(Date date) {
		return !(new DateMidnight(date).toDate().equals(date));
	}
	
	private DateMidnight stripTimeComponent(Date date) {
		return new DateMidnight(date);
	}
	
	public static Set<Location> getAllVisitsAndChildLocations(Set<Location> visitLocations,
	        Set<Location> traversedLocations) {
		
		Set<Location> locations = new HashSet<Location>();
		
		for (Location visitLocation : visitLocations) {
			if (!traversedLocations.contains(visitLocation)) {
				locations.add(visitLocation);
				traversedLocations.add(visitLocation);
				
				if (visitLocation.getChildLocations() != null) {
					locations.addAll(getAllVisitsAndChildLocations(visitLocation.getChildLocations(), traversedLocations));
					
				}
			}
			
		}
		return locations;
	}
	
}
