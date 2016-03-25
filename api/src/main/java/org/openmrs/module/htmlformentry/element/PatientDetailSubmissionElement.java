/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.htmlformentry.element;

import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.api.IdentifierNotUniqueException;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.ValidationException;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.comparator.OptionComparator;
import org.openmrs.module.htmlformentry.widget.AddressWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.HiddenFieldWidget;
import org.openmrs.module.htmlformentry.widget.NameWidget;
import org.openmrs.module.htmlformentry.widget.NumberFieldWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.TextFieldWidget;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.validator.PatientIdentifierValidator;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

/**
 * Holds the widgets used to represent Patient Details, and serves as both the
 * HtmlGeneratorElement and the FormSubmissionControllerAction for Patient
 * Details.
 */
public class PatientDetailSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {

	public static final String FIELD_PERSON_NAME = "name";

	public static final String FIELD_BIRTH_DATE_OR_AGE = "birthDateOrAge";

	public static final String FIELD_BIRTH_DATE = "birthDate";

	public static final String FIELD_AGE = "age";

	public static final String FIELD_GENDER = "gender";

	public static final String FIELD_IDENTIFIER = "identifier";

	public static final String FIELD_IDENTIFIER_LOCATION = "identifierLocation";

	public static final String FIELD_ADDRESS = "address";

	private NameWidget nameWidget;
	private ErrorWidget nameErrorWidget;
	private DropdownWidget genderWidget;
	private ErrorWidget genderErrorWidget;
	private Widget ageWidget;
	private ErrorWidget ageErrorWidget;
	private Widget birthDateWidget;
	private ErrorWidget birthDateErrorWidget;
	private Widget identifierTypeWidget;
	private Widget identifierTypeValueWidget;
	private ErrorWidget identifierTypeValueErrorWidget;
	private Widget identifierLocationWidget;
	private ErrorWidget identifierLocationErrorWidget;
	private AddressWidget addressWidget;

	private boolean required;

	public PatientDetailSubmissionElement(FormEntryContext context, Map<String, String> attributes) {
		createElement(context, attributes);
	}

	public void createElement(FormEntryContext context, Map<String, String> attributes) {
		String field = attributes.get("field");
		Patient existingPatient = context.getExistingPatient();

		// Required attribute defaults to true if not specified
		required = ! "false".equalsIgnoreCase(attributes.get("required"));

		if (FIELD_PERSON_NAME.equalsIgnoreCase(field)) {
			nameWidget = new NameWidget();
			nameErrorWidget = new ErrorWidget();
			createWidgets(context, nameWidget, nameErrorWidget,
					existingPatient != null && existingPatient.getPersonName() != null ? existingPatient.getPersonName() : null);
		}
		else if (FIELD_GENDER.equalsIgnoreCase(field)) {
			MessageSourceService msg = Context.getMessageSourceService();
			genderWidget = new DropdownWidget();
			genderErrorWidget = new ErrorWidget();
			genderWidget.addOption(new Option(msg.getMessage("Patient.gender.male"), "M", false));
			genderWidget.addOption(new Option(msg.getMessage("Patient.gender.female"), "F", false));
			createWidgets(context, genderWidget, genderErrorWidget, existingPatient != null ? existingPatient.getGender() : null);
		}
		else if (FIELD_AGE.equalsIgnoreCase(field)) {
			ageWidget = new NumberFieldWidget(0d, 200d, false);
			ageErrorWidget = new ErrorWidget();
			createWidgets(context, ageWidget, ageErrorWidget, existingPatient != null ? existingPatient.getAge() : null);
		}
		else if (FIELD_BIRTH_DATE.equalsIgnoreCase(field)) {
			birthDateWidget = new DateWidget();
			birthDateErrorWidget = new ErrorWidget();
			createWidgets(context, birthDateWidget, birthDateErrorWidget, existingPatient != null ? existingPatient.getBirthdate() : null);
		}
		else if (FIELD_BIRTH_DATE_OR_AGE.equalsIgnoreCase(field)) {
			ageWidget = new NumberFieldWidget(0d, 200d, false);
			ageErrorWidget = new ErrorWidget();
			createWidgets(context, ageWidget, ageErrorWidget, existingPatient != null ? existingPatient.getAge() : null);

			birthDateWidget = new DateWidget();
			birthDateErrorWidget = new ErrorWidget();
			createWidgets(context, birthDateWidget, birthDateErrorWidget, existingPatient != null ? existingPatient.getBirthdate() : null);

		}
		else if (FIELD_IDENTIFIER.equalsIgnoreCase(field)) {

			PatientIdentifierType idType = HtmlFormEntryUtil.getPatientIdentifierType(attributes.get("identifierTypeId"));

			identifierTypeValueWidget = new TextFieldWidget();
			identifierTypeValueErrorWidget = new ErrorWidget();
			String initialValue = null;
			if (existingPatient != null) {
				if (idType == null) {
					if (existingPatient.getPatientIdentifier() != null) {
						initialValue = existingPatient.getPatientIdentifier().getIdentifier();
					}
				} else {
					if (existingPatient.getPatientIdentifier(idType) != null) {
						initialValue = existingPatient.getPatientIdentifier(idType).getIdentifier();
					}
				}
			}
			createWidgets(context, identifierTypeValueWidget, identifierTypeValueErrorWidget, initialValue);

			if (idType != null) {
				identifierTypeWidget = new HiddenFieldWidget();
				createWidgets(context, identifierTypeWidget, null, idType.getId().toString());
			}
			else {
				identifierTypeWidget = new DropdownWidget();
				List<PatientIdentifierType> patientIdentifierTypes = HtmlFormEntryUtil.getPatientIdentifierTypes();

				for (PatientIdentifierType patientIdentifierType : patientIdentifierTypes) {
					((DropdownWidget) identifierTypeWidget).addOption(new Option(patientIdentifierType.getName(), patientIdentifierType
							.getPatientIdentifierTypeId().toString(), false));
				}

				createWidgets(context, identifierTypeWidget, null,
						existingPatient != null && existingPatient.getPatientIdentifier() != null ? existingPatient.getPatientIdentifier()
								.getIdentifierType().getId() : null);
			}
		}
		else if (FIELD_IDENTIFIER_LOCATION.equalsIgnoreCase(field)) {
			identifierLocationWidget = new DropdownWidget();
			identifierLocationErrorWidget = new ErrorWidget();

            Location defaultLocation = existingPatient != null
					&& existingPatient.getPatientIdentifier() != null ? existingPatient.getPatientIdentifier().getLocation() : null;
			defaultLocation = defaultLocation == null ? context.getDefaultLocation() : defaultLocation;
            identifierLocationWidget.setInitialValue(defaultLocation);

            List<Option> locationOptions = new ArrayList<Option>();
            for(Location location:Context.getLocationService().getAllLocations()) {
                Option option = new Option(location.getName(), location.getId().toString(), location.equals(defaultLocation));
                locationOptions.add(option);
            }
            Collections.sort(locationOptions, new OptionComparator());

            // if initialValueIsSet=false, no initial/default location, hence this shows the 'select input' field as first option
            boolean initialValueIsSet = !(defaultLocation == null);
            ((DropdownWidget) identifierLocationWidget).addOption(new Option(Context.getMessageSourceService().getMessage("htmlformentry.chooseALocation"), "", !initialValueIsSet));
            if (!locationOptions.isEmpty()) {
                    for(Option option: locationOptions){
                    ((DropdownWidget) identifierLocationWidget).addOption(option);
                    }
            }
			createWidgets(context, identifierLocationWidget, identifierLocationErrorWidget, defaultLocation);
		}

		else if (FIELD_ADDRESS.equalsIgnoreCase(field)) {
			addressWidget = new AddressWidget();
			createWidgets(context, addressWidget, null, existingPatient != null ? existingPatient.getPersonAddress() : null);
		}
	}

	private void createWidgets(FormEntryContext context, Widget fieldWidget, ErrorWidget errorWidget, Object initialValue) {
		context.registerWidget(fieldWidget);
		if (errorWidget != null) {
			context.registerErrorWidget(fieldWidget, errorWidget);
		}
		if (initialValue != null && StringUtils.hasText(initialValue.toString())) {
			fieldWidget.setInitialValue(initialValue);
		}
	}

	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder sb = new StringBuilder();
		MessageSourceService mss = Context.getMessageSourceService();

		if (nameWidget != null) {
			sb.append(nameWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				sb.append(nameErrorWidget.generateHtml(context));
		}

		if (genderWidget != null) {
			sb.append(genderWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				sb.append(genderErrorWidget.generateHtml(context));
		}

		if (birthDateWidget != null) {
			if (ageWidget != null) {
				sb.append(mss.getMessage("Person.birthdate")).append(" ");
			}
			sb.append(birthDateWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				sb.append(birthDateErrorWidget.generateHtml(context));
		}

		if (ageWidget != null) {
			if (birthDateWidget != null) {
				sb.append(" ").append(mss.getMessage("Person.age.or")).append(" ");
			}
			sb.append(ageWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				sb.append(ageErrorWidget.generateHtml(context));
		}



		if (identifierTypeValueWidget != null) {
			sb.append(identifierTypeValueWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW) {
				// if value is required
				if (required) {
					sb.append("<span class='required'>*</span>");
				}

				sb.append(" ");
				sb.append(identifierTypeValueErrorWidget.generateHtml(context));
			}
		}

		if (identifierTypeWidget != null) {
			if (identifierTypeValueWidget instanceof DropdownWidget){
				sb.append(" ").append(mss.getMessage("PatientIdentifier.identifierType")).append(" ");
			}
			sb.append(identifierTypeWidget.generateHtml(context)).append(" ");
		}

		if (identifierLocationWidget != null) {
			sb.append(identifierLocationWidget.generateHtml(context));
			if (context.getMode() != Mode.VIEW)
				sb.append(identifierLocationErrorWidget.generateHtml(context));
		}

		if (addressWidget != null) {
			sb.append(addressWidget.generateHtml(context));
		}

		return sb.toString();
	}

	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest request) {

		Patient patient = (Patient) session.getSubmissionActions().getCurrentPerson();

		if (patient == null) {
			throw new RuntimeException("Programming exception: person shouldn't be null");
		}

		FormEntryContext context = session.getContext();

		if (nameWidget != null) {
			PersonName name = (PersonName) nameWidget.getValue(context, request);

			if (patient != null) {
				if (!name.isPreferred()) {
					PersonName currentPreferredName = context.getExistingPatient().getPersonName();
					if (currentPreferredName != null){
						currentPreferredName.setPreferred(false);
						currentPreferredName.setVoided(true);
					}
				}
			}

			// HACK: we need to set the date created and uuid here as a hack around a hibernate flushing issue (see saving the Patient in FormEntrySession applyActions())
			if (name.getDateCreated() == null) {
				name.setDateCreated(new Date());
			}
			if (name.getUuid() == null) {
				name.setUuid(UUID.randomUUID().toString());
			}


			name.setPreferred(true);
			patient.addName(name);
		}

		if (genderWidget != null) {
			String value = (String) genderWidget.getValue(context, request);
			patient.setGender(value);
		}

		if (ageWidget != null) {
			Double value = (Double) ageWidget.getValue(context, request);
			if (value != null)
				calculateBirthDate(patient, null, value);
		}

		if (birthDateWidget != null) {
			Date value = (Date) birthDateWidget.getValue(context, request);
			if (value != null) {
				calculateBirthDate(patient, value, null);
			}
		}

		if (identifierTypeValueWidget != null && identifierTypeWidget != null) {
			String identifier = (String) identifierTypeValueWidget.getValue(context, request);
			String identifierTypeString = (String) identifierTypeWidget.getValue(context, request);

			// if not required, no identifier type string may be specified, then do nothing
			if (StringUtils.hasText(identifierTypeString)) {
				PatientIdentifierType identifierType = getIdentifierType(identifierTypeString);
				// Look for an existing identifier of this type
				PatientIdentifier patientIdentifier = patient.getPatientIdentifier(identifierType);

				if (StringUtils.hasText(identifier)) {
					// No existing identifier of this type, so create new
					if (patientIdentifier == null) {
						patientIdentifier = new PatientIdentifier();
						patientIdentifier.setIdentifierType(identifierType);

						// HACK: we need to set the date created  and uuid here as a hack around a hibernate flushing issue (see saving the Patient in FormEntrySession applyActions())
						patientIdentifier.setDateChanged(new Date());
						patientIdentifier.setUuid(UUID.randomUUID().toString());

						// For 1.9+ onwards patients require a preferred identifier
						if (patient.getPatientId() == null) {
							patientIdentifier.setPreferred(true);
						}

						patient.addIdentifier(patientIdentifier);
					}

					if (!identifier.equals(patientIdentifier.getIdentifier()) || !identifierType.equals(patientIdentifier.getIdentifierType())) {
						validateIdentifier(identifierType.getId(), identifier, patient);
					}

					patientIdentifier.setIdentifier(identifier);
				} else if (patientIdentifier != null) {
					// If this field is not required, then we interpret a blank value as a request to avoid any existing identifier
					session.getSubmissionActions().getIdentifiersToVoid().add(patientIdentifier);
				}
			}
		}

		//
		// TODO current behavior always updates location of the preferred identifier rather than
		// a specific identifier type being edited by the identifier widget. But identifier location
		// widget isn't aware of the identifier type widget
		//
		if (identifierLocationWidget != null) {
			PatientIdentifier patientIdentifier = patient.getPatientIdentifier();
			if (patientIdentifier == null) {
				patientIdentifier = new PatientIdentifier();
				patient.addIdentifier(patientIdentifier);
			}

			Object locationString = identifierLocationWidget.getValue(context, request);
			Location location = (Location) HtmlFormEntryUtil.convertToType(locationString.toString().trim(), Location.class);
			patientIdentifier.setLocation(location);
			patientIdentifier.setPreferred(true);

		}

		if (addressWidget != null) {
			PersonAddress personAddress = (PersonAddress) addressWidget.getValue(context, request);
			if (context.getMode() == Mode.EDIT) {
				if (!personAddress.isPreferred()) {
					PersonAddress currentPreferredAddress = context.getExistingPatient().getPersonAddress();
					currentPreferredAddress.setPreferred(false);
					currentPreferredAddress.setVoided(true);
				}
			}
			personAddress.setPreferred(true);
			patient.addAddress(personAddress);
		}

        session.getSubmissionActions().setPatientUpdateRequired(true);
	}

	private void validateIdentifier(Integer identifierType, String identifier, Patient patient) {
		if (identifierType != null && identifier != null) {
			try {
				PatientIdentifier pi = new PatientIdentifier();

				pi.setPatient(patient);
				pi.setIdentifier(identifier);
				pi.setIdentifierType(getIdentifierType(identifierType.toString()));

				// note that this is a bit of a hack; we can't call the PatientIdentifierValidator.validateIdentifier(identifier) method
				// because it (as of 1.8) also tests to make sure that an identifier has a location associated with it; when validating an
				// individual identifier widget, there will be no location because the location is collected in another widget
				PatientIdentifierValidator.validateIdentifier(pi.getIdentifier(), pi.getIdentifierType());

				if (Context.getPatientService().isIdentifierInUseByAnotherPatient(pi)) {
					throw new IdentifierNotUniqueException(Context.getMessageSourceService().getMessage(
					    "PatientIdentifier.error.notUniqueWithParameter", new Object[] { pi.getIdentifier() },
					    Context.getLocale()));
				}
			}
			catch (Exception e) {
				throw new ValidationException(e.getMessage());
			}
		}
	}

	private PatientIdentifierType getIdentifierType(String id) {
		PatientIdentifierType patientIdentifierType = Context.getPatientService().getPatientIdentifierType(new Integer(id));
		if (patientIdentifierType == null) {
			throw new RuntimeException("Invalid identifierTypeId given " + id);
		}
		return patientIdentifierType;
	}

	@SuppressWarnings("unused")
    @Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest request) {
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();

		List<FormSubmissionError> ageOrBirthdDateErrorMessage = new ArrayList<FormSubmissionError>();

		validateMandatoryField(context, request, genderWidget, genderErrorWidget, ret);

		if (required) {
			validateMandatoryField(context, request, identifierTypeValueWidget, identifierTypeValueErrorWidget, ret);
		}

		validateMandatoryField(context, request, identifierLocationWidget, identifierLocationErrorWidget, ret);

		if(nameWidget != null){
			PersonName personName = nameWidget.getValue(context, request);
			if(!StringUtils.hasText(personName.getGivenName()) || !StringUtils.hasText(personName.getFamilyName())){
				ret.add(new FormSubmissionError(context.getFieldName(nameErrorWidget), Context.getMessageSourceService().getMessage("htmlformentry.error.name.required")));
			}
		}

		if (ageWidget != null && validateMandatoryField(context, request, ageWidget, ageErrorWidget, ageOrBirthdDateErrorMessage)) {
			try {
				Number value = (Number) ageWidget.getValue(context, request);
			}
			catch (Exception e) {
				ret.add(new FormSubmissionError(context.getFieldName(ageErrorWidget), e.getMessage()));
			}
		}

		if (birthDateWidget != null
				&& validateMandatoryField(context, request, birthDateWidget, birthDateErrorWidget, ageOrBirthdDateErrorMessage)) {
			try {
				if (birthDateWidget.getValue(context, request) != null
						&& OpenmrsUtil.compare((Date) birthDateWidget.getValue(context, request), new Date()) > 0) {
					ret.add(new FormSubmissionError(context.getFieldName(birthDateErrorWidget), Context.getMessageSourceService().getMessage(
							"htmlformentry.error.cannotBeInFuture")));
				}
			}
			catch (Exception e) {
				ret.add(new FormSubmissionError(context.getFieldName(birthDateErrorWidget), e.getMessage()));
			}
		}

		if (ageWidget != null && birthDateWidget != null) {
			if (ageOrBirthdDateErrorMessage.size() > 1) {
				ret.add(new FormSubmissionError(context.getFieldName(ageErrorWidget), Context.getMessageSourceService().getMessage(
						"Person.birthdate.required")));
			}
		}
		else {
			ret.addAll(ageOrBirthdDateErrorMessage);
		}

		if (identifierTypeWidget != null && identifierTypeValueWidget != null) {
			String identifierTypeId = (String) identifierTypeWidget.getValue(context, request);
			String identifierValue = (String) identifierTypeValueWidget.getValue(context, request);
			if(StringUtils.hasText(identifierValue)){
				try {
					validateIdentifier(Integer.valueOf(identifierTypeId), identifierValue, context.getExistingPatient());
				}
				catch (Exception e) {
					ret.add(new FormSubmissionError(context.getFieldName(identifierTypeValueErrorWidget), e.getMessage()));
				}
			}
		}

		return ret;
	}

	private boolean validateMandatoryField(FormEntryContext context, HttpServletRequest request, Widget widget, ErrorWidget errorWidget,
			List<FormSubmissionError> ret) {

		try {
			if (widget != null) {
				Object obj = widget.getValue(context, request);
				if (obj == null || (obj instanceof String && !StringUtils.hasText((String) obj))) {
					throw new Exception("htmlformentry.error.required");
				}
			}
		}
		catch (Exception ex) {
			ret.add(new FormSubmissionError(context.getFieldName(errorWidget), Context.getMessageSourceService().getMessage(ex.getMessage())));
			return false;
		}

		return true;
	}

	/**
	 * If there's a birthdate specified
	 */
	private void calculateBirthDate(Person person, Date date, Double age) {
		Date birthdate = null;
		boolean birthdateEstimated = false;
		if (date != null) {
				birthdate = date;
				//if you have a previous date that's marked as estimated and date does not change -->  keep it that way
				//if you have a previous date that's marked as estimated but date changes --> not estimated
				//if new --> not estimated
				//if existing and not estimated --> not estimated
				birthdateEstimated = person.getBirthdate() != null && person.getBirthdateEstimated() != null && person.getBirthdate().equals(date) ? person.getBirthdateEstimated() : false;
		}
		else if (age != null) {
			try {
				Double ageRemainder  = BigDecimal.valueOf(age).subtract(BigDecimal.valueOf(Math.floor(age))).doubleValue();
				if (ageRemainder.equals(Double.valueOf(0)))
					person.setBirthdateFromAge(age.intValue(), new Date()); //default to usual behavior from core
				else { //a decimal was entered
					Calendar c = Calendar.getInstance();
					c.setTime(new Date());
					c.add(Calendar.DAY_OF_MONTH, - Double.valueOf((ageRemainder * 365)).intValue()); //if patient is 2.2 years old, patient was 2.0 years 2.2 - (.2*365) days ago
					c.add(Calendar.YEAR, -1 * Double.valueOf(Math.floor(age)).intValue());
					birthdate = c.getTime();
				}
				birthdateEstimated = true;
			}
			catch (NumberFormatException e) {
				throw new RuntimeException("Error getting date from age", e);
			}
		} else {
			throw new IllegalArgumentException("You must provide either an age or a birthdate for this patient.");
		}
		if (birthdate != null)
			person.setBirthdate(birthdate);
		person.setBirthdateEstimated(birthdateEstimated);
	}
}
