package org.openmrs.module.htmlformentry.element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.comparator.OptionComparator;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.TextFieldWidget;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.springframework.util.StringUtils;

/**
 * Holds the widget and submission logic for the {@code <personAttribute>} tag.
 *
 * <p>Supports three PersonAttributeType formats:
 * <ul>
 *   <li>{@code java.lang.String} – plain text input</li>
 *   <li>{@code org.openmrs.Concept} – dropdown whose answer concepts are resolved in priority order:
 *       (1) explicit {@code answerConceptIds} tag attribute (comma-separated IDs or UUIDs),
 *       (2) {@code PersonAttributeType.foreignKey} pointing to a concept set (set members used)
 *       or a question concept (concept answers used),
 *       (3) {@link BadFormDesignException} if neither is configured</li>
 *   <li>{@code org.openmrs.Location} – dropdown of locations, optionally filtered by {@code tags}</li>
 * </ul>
 *
 * <p>When multiple non-voided attributes of the requested type exist for the patient, the most
 * recently created one is used and a warning is logged.
 */
public class PersonAttributeSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {

	protected final Log log = LogFactory.getLog(getClass());

	/** The resolved attribute type. */
	private final PersonAttributeType attributeType;

	/** The widget used to render and submit the attribute value. */
	private Widget valueWidget;

	/** The error widget (rendered in ENTER / EDIT modes only). */
	private ErrorWidget errorWidget;

	/** The existing non-voided attribute (most recent if there are several), or null. */
	private PersonAttribute existingAttribute;

	public PersonAttributeSubmissionElement(FormEntryContext context, Map<String, String> parameters)
	        throws BadFormDesignException {

		// ---- 1. Resolve the PersonAttributeType ----------------------------------------
		String attributeTypeUuid = parameters.get("attributeType");
		if (!StringUtils.hasText(attributeTypeUuid)) {
			throw new BadFormDesignException("<personAttribute> tag requires an \"attributeType\" attribute");
		}
		attributeType = Context.getPersonService().getPersonAttributeTypeByUuid(attributeTypeUuid);
		if (attributeType == null) {
			throw new BadFormDesignException(
			        "<personAttribute> tag: PersonAttributeType not found for uuid=\"" + attributeTypeUuid + "\"");
		}

		// ---- 2. Find existing attribute on the patient ---------------------------------
		Patient patient = context.getExistingPatient();
		if (patient != null) {
			List<PersonAttribute> matching = new ArrayList<PersonAttribute>();
			for (PersonAttribute attr : patient.getActiveAttributes()) {
				if (attr.getAttributeType().equals(attributeType)) {
					matching.add(attr);
				}
			}
			if (matching.size() > 1) {
				log.warn("Patient " + patient.getPatientId() + " has " + matching.size()
				        + " non-voided PersonAttributes of type \"" + attributeType.getName()
				        + "\"; using most recent.");
				matching.sort(Comparator.comparing(PersonAttribute::getDateCreated).reversed());
			}
			existingAttribute = matching.isEmpty() ? null : matching.get(0);
		}

		// ---- 3. Build widget by format (all modes, including VIEW) --------------------
		errorWidget = new ErrorWidget();

		if (String.class.getName().equals(attributeType.getFormat())) {
			valueWidget = buildStringWidget(context);

		} else if (Concept.class.getName().equals(attributeType.getFormat())) {
			valueWidget = buildConceptWidget(context, parameters);

		} else if (Location.class.getName().equals(attributeType.getFormat())) {
			valueWidget = buildLocationWidget(context, parameters);

		} else {
			throw new BadFormDesignException("<personAttribute> tag: unsupported attribute format \"" + attributeType.getFormat()
			        + "\" for type \"" + attributeType.getName() + "\". Supported formats: "
			        + String.class.getName() + ", " + Concept.class.getName() + ", " + Location.class.getName());
		}

		context.registerWidget(valueWidget);
		context.registerErrorWidget(valueWidget, errorWidget);
	}

	// ---------------------------------------------------------------------------------
	// Widget builders
	// ---------------------------------------------------------------------------------

	private Widget buildStringWidget(FormEntryContext context) {
		TextFieldWidget w = new TextFieldWidget();
		if (existingAttribute != null) {
			w.setInitialValue(existingAttribute.getValue());
		}
		return w;
	}

	private Widget buildConceptWidget(FormEntryContext context, Map<String, String> parameters)
	        throws BadFormDesignException {

		DropdownWidget w = new DropdownWidget();
		w.addOption(new Option());

		for (Concept concept : resolveAnswerConcepts(parameters)) {
			w.addOption(new Option(concept.getName(Context.getLocale(), false).getName(), concept.getConceptId().toString(), false));
		}

		if (existingAttribute != null && StringUtils.hasText(existingAttribute.getValue())) {
			w.setInitialValue(existingAttribute.getValue());
		}

		return w;
	}

	private List<Concept> resolveAnswerConcepts(Map<String, String> parameters) throws BadFormDesignException {
		String answerConceptIds = parameters.get("answerConceptIds");
		if (StringUtils.hasText(answerConceptIds)) {
			return resolveConceptsFromIdList(answerConceptIds);
		}
		if (attributeType.getForeignKey() != null) {
			return resolveConceptsFromForeignKey(attributeType.getForeignKey());
		}
		throw new BadFormDesignException(
		    "<personAttribute> tag: PersonAttributeType \"" + attributeType.getName()
		    + "\" (format=" + Concept.class.getName() + ") requires either an \"answerConceptIds\" "
		    + "tag attribute or a foreignKey on the attribute type pointing to a concept set or question concept");
	}

	private List<Concept> resolveConceptsFromIdList(String answerConceptIds) throws BadFormDesignException {
		List<Concept> concepts = new ArrayList<>();
		for (String idOrUuid : answerConceptIds.split(",")) {
			String trimmed = idOrUuid.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			Concept concept = HtmlFormEntryUtil.getConcept(trimmed);
			if (concept == null) {
				throw new BadFormDesignException(
				    "<personAttribute> tag: cannot find concept for answerConceptIds value \"" + trimmed + "\"");
			}
			concepts.add(concept);
		}
		return concepts;
	}

	private List<Concept> resolveConceptsFromForeignKey(Integer foreignKey) throws BadFormDesignException {
		Concept fk = Context.getConceptService().getConcept(foreignKey);
		if (fk == null) {
			throw new BadFormDesignException(
			    "<personAttribute> tag: cannot find concept for foreignKey=" + foreignKey
			    + " on PersonAttributeType \"" + attributeType.getName() + "\"");
		}
		if (Boolean.TRUE.equals(fk.getSet())) {
			return fk.getSetMembers(false);
		}
		return fk.getAnswers(false).stream()
		         .map(ConceptAnswer::getAnswerConcept)
		         .collect(Collectors.toList());
	}

	private Widget buildLocationWidget(FormEntryContext context, Map<String, String> parameters)
	        throws BadFormDesignException {

		DropdownWidget w = new DropdownWidget();

		// blank / choose prompt
		w.addOption(new Option(Context.getMessageSourceService().getMessage("htmlformentry.chooseALocation"), "", false));

		// Build location list
		List<Location> locations;
		String tagsParam = parameters.get("tags");
		if (StringUtils.hasText(tagsParam)) {
			List<LocationTag> locationTags = new ArrayList<LocationTag>();
			for (String tagName : tagsParam.split(",")) {
				String trimmed = tagName.trim();
				if (trimmed.isEmpty()) {
					continue;
				}
				LocationTag tag = HtmlFormEntryUtil.getLocationTag(trimmed);
				if (tag == null) {
					throw new BadFormDesignException(
					        "<personAttribute> tag: cannot find location tag \"" + trimmed + "\"");
				}
				locationTags.add(tag);
			}
			locations = Context.getLocationService().getLocationsHavingAnyTag(locationTags);
		} else {
			locations = Context.getLocationService().getAllLocations(false);
		}

		// Build and sort options
		List<Option> locationOptions = new ArrayList<Option>();
		for (Location location : locations) {
			locationOptions.add(new Option(HtmlFormEntryUtil.format(location), location.getId().toString(), false));
		}
		Collections.sort(locationOptions, new OptionComparator());

		for (Option option : locationOptions) {
			w.addOption(option);
		}

		if (existingAttribute != null && StringUtils.hasText(existingAttribute.getValue())) {
			w.setInitialValue(existingAttribute.getValue());
		}

		return w;
	}

	// ---------------------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------------------

	/**
	 * Converts the raw submitted string from the widget into the canonical storage value for
	 * the attribute. For Concept and Location types, we look up the actual typed object and call
	 * its {@link org.openmrs.Attributable#serialize()} method so that core controls the format.
	 */
	private String toStorageValue(String rawValue) {
		if (!StringUtils.hasText(rawValue)) {
			return rawValue;
		}
		String format = attributeType.getFormat();
		if (Concept.class.getName().equals(format)) {
			Concept concept = Context.getConceptService().getConcept(Integer.parseInt(rawValue));
			return concept != null ? concept.serialize() : rawValue;
		} else if (Location.class.getName().equals(format)) {
			Location location = Context.getLocationService().getLocation(Integer.parseInt(rawValue));
			return location != null ? location.serialize() : rawValue;
		}
		return rawValue;
	}

	// ---------------------------------------------------------------------------------
	// HtmlGeneratorElement
	// ---------------------------------------------------------------------------------

	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder sb = new StringBuilder();
		sb.append(valueWidget.generateHtml(context));
		if (context.getMode() != Mode.VIEW) {
			sb.append(errorWidget.generateHtml(context));
		}
		return sb.toString();
	}

	// ---------------------------------------------------------------------------------
	// FormSubmissionControllerAction
	// ---------------------------------------------------------------------------------

	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		// No current validation
		return Collections.emptyList();
	}

	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		FormEntryContext context = session.getContext();
		Patient patient = session.getPatient();
		if (patient == null) {
			log.warn("<personAttribute> handleSubmission: patient is null, skipping");
			return;
		}

		Object rawValue = valueWidget.getValue(context, submission);
		String value = (rawValue == null) ? null : rawValue.toString().trim();

		if (existingAttribute != null) {
			if (!StringUtils.hasText(value)) {
				existingAttribute.setVoided(true);
				existingAttribute.setVoidedBy(Context.getAuthenticatedUser());
				existingAttribute.setVoidReason("Cleared via htmlformentry form submission");
				existingAttribute.setDateVoided(new Date());
			} else {
				existingAttribute.setValue(toStorageValue(value));
				existingAttribute.setChangedBy(Context.getAuthenticatedUser());
				existingAttribute.setDateChanged(new Date());
			}
		} else {
			if (StringUtils.hasText(value)) {
				PersonAttribute newAttribute = new PersonAttribute();
				newAttribute.setAttributeType(attributeType);
				newAttribute.setValue(toStorageValue(value));
				patient.addAttribute(newAttribute);
			}
		}

		// Signal that the patient needs to be persisted
		session.getSubmissionActions().setPatientUpdateRequired(true);
	}
}
