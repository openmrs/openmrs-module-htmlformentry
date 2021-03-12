package org.openmrs.module.htmlformentry.tester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.junit.Assert;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.EncounterProvider;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.SimpleDosingInstructions;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.TestObsValue;
import org.openmrs.module.htmlformentry.TestUtil;

public class FormResultsTester {
	
	private List<FormSubmissionError> validationErrors;
	
	private FormEntrySession formEntrySession;
	
	private Patient patient;
	
	private Encounter encounterCreated;
	
	private FormResultsTester(List<FormSubmissionError> validationErrors, FormEntrySession formEntrySession) {
		this.validationErrors = validationErrors;
		this.formEntrySession = formEntrySession;
		this.patient = formEntrySession.getPatient();
		this.encounterCreated = formEntrySession.getEncounter();
	}
	
	public static FormResultsTester submitForm(FormEntrySession fes, HttpServletRequest request) {
		fes.prepareForSubmit();
		FormSubmissionController fsc = fes.getSubmissionController();
		List<FormSubmissionError> validationErrors = fsc.validateSubmission(fes.getContext(), request);
		if (validationErrors.isEmpty()) {
			if (fes.getContext().getMode() == Mode.ENTER && fes.hasEncouterTag()) {
				List<Encounter> toCreate = fes.getSubmissionActions().getEncountersToCreate();
				if (toCreate == null || toCreate.size() == 0) {
					throw new IllegalArgumentException("This form is not going to create an encounter");
				}
			}
			try {
				fsc.handleFormSubmission(fes, request);
				Context.getService(HtmlFormEntryService.class).applyActions(fes);
			}
			catch (Exception e) {
				throw new IllegalArgumentException("Error submitting form", e);
			}
		}
		return new FormResultsTester(validationErrors, fes);
	}
	
	public FormResultsTester assertNoEncounterCreated() {
		Assert.assertNull(encounterCreated);
		return this;
	}
	
	public FormResultsTester assertEncounterCreated() {
		Assert.assertNotNull(encounterCreated);
		return this;
	}
	
	public void assertEncounterEdited() {
		Assert.assertNotNull("No encounter found", encounterCreated);
		Assert.assertNotNull("Encounter date changed not set on edit", encounterCreated.getDateChanged());
	}
	
	public void assertEncounterVoided() {
		Assert.assertTrue("Encounter not voided", encounterCreated.isVoided());
	}
	
	public void assertEncounterNotVoided() {
		Assert.assertFalse("Encounter voided", encounterCreated.isVoided());
	}
	
	public FormResultsTester assertNoErrors() {
		Assert.assertTrue("" + validationErrors, validationErrors == null || validationErrors.size() == 0);
		return this;
	}
	
	public void assertErrors() {
		Assert.assertTrue(validationErrors != null && validationErrors.size() > 0);
	}
	
	public void assertErrors(int numberOfErrors) {
		Assert.assertTrue(validationErrors != null && validationErrors.size() == numberOfErrors);
	}
	
	public void assertErrorMessage(String expectedMessage) {
		boolean found = false;
		if (validationErrors != null) {
			for (FormSubmissionError error : validationErrors) {
				if (error.getError().equals(HtmlFormEntryUtil.translate(expectedMessage))) {
					found = true;
				}
			}
		}
		Assert.assertTrue(found);
	}
	
	public void printErrors() {
		if (validationErrors == null || validationErrors.size() == 0) {
			System.out.println("No Errors");
		} else {
			for (FormSubmissionError error : validationErrors)
				System.out.println(error.getId() + " -> " + error.getError());
		}
	}
	
	public void print() {
		printErrors();
		printEncounterCreated();
	}
	
	public void printEncounterCreated() {
		if (encounterCreated == null) {
			System.out.println("No encounter created");
		} else {
			System.out.println("=== Encounter created ===");
			System.out.println(
			    "Created: " + encounterCreated.getDateCreated() + "  Edited: " + encounterCreated.getDateChanged());
			System.out.println("Date: " + encounterCreated.getEncounterDatetime());
			System.out.println("Location: " + encounterCreated.getLocation().getName());
			System.out.println("Provider: " + getProvider(encounterCreated).getPersonName());
			System.out.println("    (obs)");
			Collection<Obs> obs = encounterCreated.getAllObs(false);
			if (obs == null) {
				System.out.println("None");
			} else {
				for (Obs o : obs) {
					System.out.println(o.getConcept().getName() + " -> " + o.getValueAsString(Context.getLocale()));
				}
			}
		}
	}
	
	public List<FormSubmissionError> getValidationErrors() {
		return validationErrors;
	}
	
	public void setValidationErrors(List<FormSubmissionError> validationErrors) {
		this.validationErrors = validationErrors;
	}
	
	public Encounter getEncounterCreated() {
		return encounterCreated;
	}
	
	public void setEncounterCreated(Encounter encounterCreated) {
		this.encounterCreated = encounterCreated;
	}
	
	public Patient getPatient() {
		return patient;
	}
	
	public void setPatient(Patient patient) {
		this.patient = patient;
	}
	
	/**
	 * Fails if there is a patient (one was initially selected or one was created)
	 */
	public void assertNoPatient() {
		Assert.assertNull(patient);
	}
	
	/**
	 * Fails if there is no patient (none was initially selected and none was created), or the patient
	 * doesn't have a patientId assigned.
	 */
	public void assertPatient() {
		Assert.assertNotNull(patient);
		Assert.assertNotNull(patient.getPatientId());
	}
	
	/**
	 * Fails if there is no provider with an assigned id associated with the encounter
	 */
	public void assertProvider() {
		assertEncounterCreated();
		Assert.assertNotNull(getProvider(getEncounterCreated()));
		Assert.assertNotNull(getProvider(getEncounterCreated()).getPersonId());
	}
	
	public void assertNoProvider() {
		assertEncounterCreated();
		Assert.assertNull(getProvider(getEncounterCreated()));
	}
	
	/**
	 * Fails if there is no provider or if the provider id does not match the expected id
	 */
	public void assertProvider(Integer expectedProviderId) {
		assertProvider();
		Assert.assertEquals(expectedProviderId, getProvider(getEncounterCreated()).getPersonId());
	}
	
	/**
	 * Fails if there is no location with an assigned id associated with the encounter
	 */
	public void assertLocation() {
		assertEncounterCreated();
		Assert.assertNotNull(getEncounterCreated().getLocation());
		Assert.assertNotNull(getEncounterCreated().getLocation().getLocationId());
	}
	
	public void assertNoLocation() {
		assertEncounterCreated();
		Assert.assertNull(getEncounterCreated().getLocation());
	}
	
	/**
	 * Fails if there is no location or if the location id does not match the expected location id
	 */
	public void assertLocation(Integer expectedLocationId) {
		assertLocation();
		Assert.assertEquals(expectedLocationId, getEncounterCreated().getLocation().getLocationId());
	}
	
	public void assertEncounterType() {
		assertEncounterCreated();
		Assert.assertNotNull(getEncounterCreated().getEncounterType());
		Assert.assertNotNull(getEncounterCreated().getEncounterType().getEncounterTypeId());
	}
	
	public void assertEncounterType(Integer expectedEncounterTypeId) {
		assertEncounterType();
		Assert.assertEquals(expectedEncounterTypeId, getEncounterCreated().getEncounterType().getEncounterTypeId());
	}
	
	public void assertEncounterDatetime() {
		assertEncounterCreated();
		Assert.assertNotNull(getEncounterCreated().getEncounterDatetime());
	}
	
	public void assertEncounterDatetime(Date expectedEncounterDate) {
		assertEncounterDatetime();
		Assert.assertEquals(expectedEncounterDate, getEncounterCreated().getEncounterDatetime());
	}
	
	/**
	 * Fails if the number of obs in encounterCreated is not 'expected'
	 *
	 * @param expected
	 */
	public void assertObsCreatedCount(int expected) {
		int found = getObsCreatedCount();
		Assert.assertEquals("Expected to create " + expected + " obs but got " + found, expected, found);
	}
	
	/**
	 * Fails if the number of obs groups in encounterCreated is not 'expected'
	 *
	 * @param expected
	 */
	public void assertObsGroupCreatedCount(int expected) {
		int found = getObsGroupCreatedCount();
		Assert.assertEquals("Expected to create " + expected + " obs groups but got " + found, expected, found);
	}
	
	/**
	 * Fails if the number of obs leaves (i.e. obs that aren't groups) in encounterCreated is not
	 * 'expected'
	 *
	 * @param expected
	 */
	public void assertObsLeafCreatedCount(int expected) {
		int found = getObsLeafCreatedCount();
		Assert.assertEquals("Expected to create " + expected + " non-group obs but got " + found, expected, found);
	}
	
	/**
	 * Fails if any Obs with the given Concept Id doesn't have previous_version set and a matching
	 * voided obs exists in the encounter, or if no non-void obs with Concept Id is found
	 *
	 * @param conceptId
	 */
	public void assertPreviousVersion(int conceptId) {
		
		Assert.assertNotNull(encounterCreated);
		Collection<Obs> temp = encounterCreated.getAllObs(true);
		Assert.assertNotNull(temp);
		
		Obs found = null;
		
		for (Obs curObs : temp) {
			if (curObs.getConcept().getId() == conceptId && !curObs.getVoided()) {
				//just keep track that an non-void Obs
				//with the provided concept Id was found in the encounter
				
				found = curObs;
				Assert.assertNotNull(
				    "All Obs with provided Concept Id should have been modified and have a previous version set",
				    curObs.getPreviousVersion());
				
				Assert.assertTrue("Previous version should be in encounter", temp.contains(curObs.getPreviousVersion()));
				
				Assert.assertTrue("Previous version should be voided", curObs.getPreviousVersion().getVoided());
				
			}
		}
		
		Assert.assertNotNull("No Obs with provided Concept Id was found", found);
		
	}
	
	/**
	 * Fails if the Obs specified by the given Obs Id does not have previous_version set or if matching
	 * voided Obs does not exist in encounter
	 *
	 * @param obsId
	 */
	public void assertPreviousVersionById(int obsId) {
		
		Assert.assertNotNull(encounterCreated);
		Collection<Obs> temp = encounterCreated.getAllObs(true);
		Assert.assertNotNull(temp);
		
		Obs found = null;
		
		for (Obs curObs : temp) {
			if (curObs.getId() == obsId && !curObs.getVoided()) {
				found = curObs;
			}
		}
		
		Assert.assertNotNull("Obs with provided Obs id not found", found);
		Assert.assertNotNull("Obs with provided Obs id should have a previous version set", found.getPreviousVersion());
		Assert.assertTrue("Previous version should be in encounter", temp.contains(found.getPreviousVersion()));
		Assert.assertTrue("Previous version should be voided", found.getPreviousVersion().getVoided());
	}
	
	/**
	 * Fails if any Obs in the encounter does not have a previous_version set or a matching voided obs
	 * does not exist in encounter
	 */
	public void assertPreviousVersions() {
		
		Assert.assertNotNull("Encounter was not created", encounterCreated);
		Collection<Obs> temp = encounterCreated.getAllObs(true);
		Assert.assertNotNull("No observations were created", temp);
		
		for (Obs curObs : temp) {
			if (!curObs.getVoided()) {
				Assert.assertTrue("Previous version on new Obs Id#" + curObs.getId() + " should reference a voided Obs",
				    temp.contains(curObs.getPreviousVersion()));
			}
		}
	}
	
	/**
	 * @return the number of obs in encounterCreated (0 if no encounter was created)
	 */
	public int getObsCreatedCount() {
		if (encounterCreated == null)
			return 0;
		Collection<Obs> temp = encounterCreated.getAllObs();
		if (temp == null)
			return 0;
		return temp.size();
	}
	
	/**
	 * @return the number of obs groups in encounterCreated (0 if no encounter was created)
	 */
	public int getObsGroupCreatedCount() {
		if (encounterCreated == null)
			return 0;
		Collection<Obs> temp = encounterCreated.getAllObs();
		if (temp == null)
			return 0;
		int count = 0;
		for (Obs o : temp) {
			if (o.isObsGrouping())
				++count;
		}
		return count;
	}
	
	/**
	 * @return the number of non-group obs in encounterCreated (0 if no encounter was created)
	 */
	public int getObsLeafCreatedCount() {
		if (encounterCreated == null)
			return 0;
		Collection<Obs> temp = encounterCreated.getObs();
		if (temp == null)
			return 0;
		return temp.size();
	}
	
	private void assertObsExists(boolean lookForVoided, int conceptId, Object value) {
		// quick checks
		Assert.assertNotNull(encounterCreated);
		Collection<Obs> temp = encounterCreated.getAllObs(lookForVoided);
		Assert.assertNotNull(temp);
		
		String valueAsString = null;
		if (value instanceof Date) {
			valueAsString = formatObsValueDate((Date) value);
		} else {
			valueAsString = TestUtil.valueAsStringHelper(value);
		}
		for (Obs obs : temp) {
			if (lookForVoided && !obs.isVoided())
				continue;
			if (obs.getConcept().getConceptId() == conceptId) {
				if (valueAsString == null) {
					return;
				}
				String valueToCompare = obs.getValueAsString(Context.getLocale());
				if (valueAsString.equals(valueToCompare)) {
					return;
				}
				if (obs.getValueDatetime() != null) {
					if (valueAsString.equals(formatObsValueDate(obs.getValueDatetime()))) {
						return;
					}
					;
				}
			}
		}
		Assert.fail("Could not find obs with conceptId " + conceptId + " and value " + valueAsString);
	}
	
	/**
	 * Fails if encounterCreated doesn't have an obs with the given conceptId and value
	 *
	 * @param conceptId
	 * @param value may be null
	 */
	public void assertObsCreated(int conceptId, Object value) {
		assertObsExists(false, conceptId, value);
	}
	
	/**
	 * Fails if encounterCreated doesn't have a voided obs with the given conceptId and value
	 *
	 * @param conceptId
	 * @param value
	 */
	public void assertObsVoided(int conceptId, Object value) {
		assertObsExists(true, conceptId, value);
	}
	
	/**
	 * Fails if there isn't an obs group with these exact characteristics
	 *
	 * @param groupingConceptId the concept id of the grouping obs
	 * @param conceptIdsAndValues these parameters must be given in pairs, the first element of which is
	 *            the conceptId of a child obs (Integer) and the second element of which is the value of
	 *            the child obs
	 */
	public void assertObsGroupCreated(int groupingConceptId, Object... conceptIdsAndValues) {
		// quick checks
		Assert.assertNotNull(encounterCreated);
		Collection<Obs> temp = encounterCreated.getAllObs();
		Assert.assertNotNull(temp);
		
		List<TestObsValue> expected = new ArrayList<>();
		for (int i = 0; i < conceptIdsAndValues.length; i += 2) {
			int conceptId = (Integer) conceptIdsAndValues[i];
			Object value = conceptIdsAndValues[i + 1];
			expected.add(new TestObsValue(conceptId, value));
		}
		
		for (Obs o : temp) {
			if (o.getConcept().getConceptId() == groupingConceptId) {
				if (o.getValueCoded() != null || o.getValueComplex() != null || o.getValueDatetime() != null
				        || o.getValueDrug() != null || o.getValueNumeric() != null || o.getValueText() != null) {
					Assert.fail("Obs group with groupingConceptId " + groupingConceptId + " should has a non-null value");
				}
				if (TestUtil.isMatchingObsGroup(o, expected)) {
					return;
				}
			}
		}
		Assert.fail("Cannot find an obs group matching " + expected);
	}
	
	private String formatObsValueDate(Date date) {
		if (date == null) {
			return null;
		}
		return new SimpleDateFormat("yyyy-MM-dd").format(date);
	}
	
	// helper workaround method now that as of platform 2.x, Encounter does not have a getProvider method
	private Person getProvider(Encounter enc) {
		if (enc.getEncounterProviders() == null || enc.getEncounterProviders().isEmpty()) {
			return null;
		} else {
			for (EncounterProvider encounterProvider : enc.getEncounterProviders()) {
				// Return the first non-voided provider associated with a person in the list
				if (!encounterProvider.isVoided() && encounterProvider.getProvider().getPerson() != null) {
					return encounterProvider.getProvider().getPerson();
				}
			}
		}
		return null;
	}
	
	/**
	 * @return the number of orders created, or an empty list if none
	 */
	public List<Order> getOrdersCreated() {
		if (encounterCreated == null || encounterCreated.getOrders() == null) {
			return new ArrayList<>();
		}
		return new ArrayList<>(encounterCreated.getOrders());
	}
	
	/**
	 * Fails if the number of orders created does not match expected
	 *
	 * @param expected
	 */
	public FormResultsTester assertOrderCreatedCount(int expected) {
		assertThat(getOrdersCreated().size(), is(expected));
		return this;
	}
	
	/**
	 * Fails if the number of orders created does not match expected
	 *
	 * @param expected
	 */
	public FormResultsTester assertVoidedOrderCount(int expected) {
		int num = 0;
		for (Order o : getOrdersCreated()) {
			if (BooleanUtils.isTrue(o.getVoided())) {
				num++;
			}
		}
		assertThat(num, is(expected));
		return this;
	}
	
	/**
	 * Fails if the number of orders created does not match expected
	 *
	 * @param expected
	 */
	public FormResultsTester assertNonVoidedOrderCount(int expected) {
		int num = 0;
		for (Order o : getOrdersCreated()) {
			if (BooleanUtils.isFalse(o.getVoided())) {
				num++;
			}
		}
		assertThat(num, is(expected));
		return this;
	}
	
	/**
	 * Fails if there is no order created in the encounter with given concept and action
	 *
	 * @returns the matching order
	 */
	public Order assertOrder(Order.Action action, Integer conceptId) {
		List<Order> found = new ArrayList<>();
		for (Order o : getOrdersCreated()) {
			if (o.getAction() == action && o.getConcept().getId().equals(conceptId)) {
				if (BooleanUtils.isFalse(o.getVoided())) {
					found.add(o);
				}
			}
		}
		assertThat(found.size(), is(1));
		return found.get(0);
	}
	
	/**
	 * Fails if there is no order created in the encounter with given drug and action
	 * 
	 * @returns the matching order
	 */
	public DrugOrder assertDrugOrder(Order.Action action, Integer drugId) {
		List<DrugOrder> found = new ArrayList<>();
		for (Order o : getOrdersCreated()) {
			if (o instanceof DrugOrder) {
				DrugOrder drugOrder = (DrugOrder) o;
				if (drugOrder.getAction() == action && drugOrder.getDrug().getId().equals(drugId)) {
					if (BooleanUtils.isFalse(drugOrder.getVoided())) {
						found.add(drugOrder);
					}
				}
			}
		}
		assertThat(found.size(), is(1));
		return found.get(0);
	}
	
	public FormResultsTester assertSimpleDosing(DrugOrder drugOrder, Double dose, Integer units, Integer freq, Integer route,
	        Boolean asNeeded, String instructions) {
		assertThat(drugOrder.getDosingType(), is(SimpleDosingInstructions.class));
		assertThat(drugOrder.getDose(), is(dose));
		TestUtil.assertIdEquals(drugOrder.getDoseUnits(), units);
		TestUtil.assertIdEquals(drugOrder.getFrequency(), freq);
		TestUtil.assertIdEquals(drugOrder.getRoute(), route);
		assertThat(drugOrder.getAsNeeded(), is(asNeeded));
		assertThat(drugOrder.getInstructions(), is(instructions));
		return this;
	}
	
	public FormResultsTester assertSimpleDosingFieldsAreNull(DrugOrder drugOrder) {
		assertThat(drugOrder.getDose(), nullValue());
		assertThat(drugOrder.getDoseUnits(), nullValue());
		assertThat(drugOrder.getRoute(), nullValue());
		assertThat(drugOrder.getFrequency(), nullValue());
		assertThat(drugOrder.getInstructions(), nullValue());
		assertThat(BooleanUtils.isTrue(drugOrder.getAsNeeded()), is(false));
		return this;
	}
	
	public FormResultsTester assertFreeTextDosing(DrugOrder drugOrder, String expectedDosingInstructions) {
		assertThat(drugOrder.getDosingType(), is(FreeTextDosingInstructions.class));
		assertThat(drugOrder.getDosingInstructions(), is(expectedDosingInstructions));
		return this;
	}
	
	public FormResultsTester assertFreeTextDosingFieldsNull(DrugOrder drugOrder) {
		assertThat(drugOrder.getDosingInstructions(), nullValue());
		return this;
	}
	
	public FormResultsTester assertDuration(DrugOrder drugOrder, Double duration, Integer durationUnitsConceptId) {
		assertThat(drugOrder.getDuration(), is(duration));
		TestUtil.assertIdEquals(drugOrder.getDurationUnits(), durationUnitsConceptId);
		return this;
	}
	
	public FormResultsTester assertDurationFieldsNull(DrugOrder drugOrder) {
		assertThat(drugOrder.getDuration(), nullValue());
		assertThat(drugOrder.getDurationUnits(), nullValue());
		return this;
	}
	
	public FormResultsTester assertDispensing(DrugOrder drugOrder, Double qty, Integer qtyUnitsId, Integer numRefills) {
		assertThat(drugOrder.getQuantity(), is(qty));
		TestUtil.assertIdEquals(drugOrder.getQuantityUnits(), qtyUnitsId);
		assertThat(drugOrder.getNumRefills(), is(numRefills));
		return this;
	}
	
	public FormResultsTester assertDispensingFieldsNull(DrugOrder drugOrder) {
		assertThat(drugOrder.getQuantity(), nullValue());
		assertThat(drugOrder.getQuantityUnits(), nullValue());
		assertThat(drugOrder.getNumRefills(), nullValue());
		return this;
	}
}
