package org.openmrs.module.htmlformentry;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.openmrs.module.htmlformentry.HtmlFormEntryConstants.FORM_NAMESPACE;
import static org.openmrs.module.htmlformentry.HtmlFormEntryUtil.getControlId;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DropdownWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Widget;

public class FormEntryContextTest {
	
	private FormEntryContext context;
	
	private Concept concept1;
	
	private Concept concept2;
	
	@Before
	public void setUp() {
		context = new FormEntryContext(FormEntryContext.Mode.ENTER);
		concept1 = createConceptMock(1);
		concept2 = createConceptMock(2);
	}
	
	@Test
	public void testGetWidgetByFieldName() {
		DropdownWidget widget = new DropdownWidget();
		context.registerWidget(widget);
		String fieldName = context.getFieldName(widget);
		Widget w = context.getWidgetByFieldName(fieldName);
		assertNotNull(w);
	}
	
	@Test
	public void testNullGetWidgetByFieldName() {
		Widget w = context.getWidgetByFieldName("test");
		assertNull(w);
	}
	
	@Test
	public void testGetErrorDivIds() {
		DateWidget startDateWidget;
		ErrorWidget startDateErrorWidget;
		startDateWidget = new DateWidget();
		startDateErrorWidget = new ErrorWidget();
		context.registerWidget(startDateWidget);
		context.registerErrorWidget(startDateWidget, startDateErrorWidget);
		assertNotNull(context.getErrorDivIds());
	}
	
	@Test
	public void shouldGetObsFromCurrentGroupByControlID() {
		
		// Prepare parameters
		String controlId = "child-obs-1-1";
		Obs parentObs1 = (createObsMock(concept1, "parent-obs-1"));
		parentObs1.addGroupMember(createObsMock(concept1, "child-obs-1-1"));
		parentObs1.addGroupMember(createObsMock(concept2, "child-obs-1-2"));
		parentObs1.addGroupMember(createObsMock(concept2, null));
		
		// Test
		context.setObsGroup(parentObs1);
		Obs obs = context.getObsFromCurrentGroup(controlId);
		
		// Validation
		Assert.assertEquals(getControlId(obs), controlId);
		Assert.assertEquals(obs.getConcept(), concept1);
	}
	
	@Test
	public void shouldNotFailGetObsFromCurrentGroupByControlIDWhenListIsEmpty() {
		
		// Prepare parameters
		String controlId = "child-obs-1-1";
		
		// Test
		Obs obs = context.getObsFromCurrentGroup(controlId);
		
		// Validation
		Assert.assertEquals(obs, null);
	}
	
	@Test
	public void shouldNoFailGetObsFromCurrentGroupByControlIdWhenControlIdDontExists() {
		
		// Prepare parameters
		String controlId = "NotExist";
		Obs parentObs1 = (createObsMock(concept1, "parent-obs-1"));
		parentObs1.addGroupMember(createObsMock(concept1, "child-obs-1-1"));
		parentObs1.addGroupMember(createObsMock(concept2, "child-obs-1-2"));
		parentObs1.addGroupMember(createObsMock(concept2, null));
		
		// Test
		context.setObsGroup(parentObs1);
		Obs obs = context.getObsFromCurrentGroup(controlId);
		
		// Validation
		Assert.assertEquals(obs, null);
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldFailGetObsFromCurrentGroupByControlIdWithMultipleMatching() {
		
		// Prepare parameters
		String controlId = "child-obs-1-1";
		Obs parentObs1 = (createObsMock(concept1, "parent-obs-1"));
		parentObs1.addGroupMember(createObsMock(concept1, "child-obs-1-1"));
		parentObs1.addGroupMember(createObsMock(concept2, "child-obs-1-1"));
		parentObs1.addGroupMember(createObsMock(concept2, null));
		
		// Test
		context.setObsGroup(parentObs1);
		context.getObsFromCurrentGroup(controlId);
	}
	
	@Test
	public void shouldGetObsFromExistingObsByConceptAndControlId() {
		
		// Prepare parameters
		context.setupExistingData(createEncounterMock(concept1, concept2));
		String controlId = "main-obs-1";
		
		// Test
		Obs obs = context.getObsFromExistingObs(concept1, controlId);
		
		// Validation
		Assert.assertEquals(getControlId(obs), controlId);
		Assert.assertEquals(obs.getConcept(), concept1);
	}
	
	@Test
	public void shouldNotFailGetObsFromExistingObsByConceptAndControlIdWhenListIsEmpty() {
		
		// Prepare parameters
		String controlId = "main-obs-1";
		
		// Test
		Obs obs = context.getObsFromExistingObs(concept1, controlId);
		
		// Validation
		Assert.assertEquals(obs, null);
	}
	
	@Test
	public void shouldNoFailGetObsFromExistingObsByConceptAndControlIdWhenControlIdDontExists() {
		
		// Prepare parameters
		context.setupExistingData(createEncounterMock(concept1, concept2));
		String controlId = "NotExist";
		
		// Test
		Obs obs = context.getObsFromExistingObs(concept1, controlId);
		
		// Validation
		Assert.assertEquals(obs, null);
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldFailGetObsFromExistingObsByConceptAndControlIdWithMultipleMatching() {
		
		// Prepare parameters
		context.setupExistingData(createEncounterMock(concept1, concept2));
		String controlId = "main-obs-3";
		
		// Test
		context.getObsFromExistingObs(concept1, controlId);
	}
	
	@Test
	public void shouldGetObsFromExistingObsByControlID() {
		
		// Prepare parameters
		context.setupExistingData(createEncounterMock(concept1, concept2));
		String controlId = "main-obs-1";
		
		// Test
		Obs obs = context.getObsFromExistingObs(controlId);
		
		// Validation
		Assert.assertEquals(getControlId(obs), controlId);
		Assert.assertEquals(obs.getConcept(), concept1);
	}
	
	@Test
	public void shouldNotFailGetObsFromExistingObsByControlIdWhenListIsEmpty() {
		
		// Prepare parameters
		String controlId = "main-obs-1";
		
		// Test
		Obs obs = context.getObsFromExistingObs(controlId);
		
		// Validation
		Assert.assertEquals(obs, null);
	}
	
	@Test
	public void shouldNoFailGetObsFromExistingObsByControlIDWhenControlIdDontExists() {
		
		// Prepare parameters
		context.setupExistingData(createEncounterMock(concept1, concept2));
		String controlId = "NotExist";
		
		// Test
		Obs obs = context.getObsFromExistingObs(controlId);
		
		// Validation
		Assert.assertEquals(obs, null);
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldFailGetObsFromExistingObsByControlIdWithMultipleMaching() {
		
		// Prepare parameters
		context.setupExistingData(createEncounterMock(concept1, concept2));
		String controlId = "main-obs-3";
		
		// Test
		context.getObsFromExistingObs(controlId);
	}
	
	private Concept createConceptMock(Integer id) {
		Concept concept = new Concept();
		concept.setId(id);
		concept.setUuid("uuid_" + id);
		return concept;
	}
	
	private Obs createObsMock(Concept concept, String controlId) {
		Obs obs = new Obs();
		obs.setConcept(concept);
		if (controlId != null)
			obs.setFormField(FORM_NAMESPACE, "MyForm.1.0/" + controlId + "-0");
		return obs;
	}
	
	private Encounter createEncounterMock(Concept concept1, Concept concept2) {
		Encounter encounter = new Encounter();
		Set<Obs> obsSet = new HashSet<>();
		
		obsSet.add(createObsMock(concept1, "main-obs-1"));
		obsSet.add(createObsMock(concept2, "main-obs-2"));
		obsSet.add(createObsMock(concept1, "main-obs-3"));
		obsSet.add(createObsMock(concept1, "main-obs-3"));
		
		Obs parentObs1 = (createObsMock(concept1, "parent-obs-1"));
		parentObs1.addGroupMember(createObsMock(concept1, "child-obs-1-1"));
		parentObs1.addGroupMember(createObsMock(concept2, "child-obs-1-2"));
		parentObs1.addGroupMember(createObsMock(concept2, null));
		obsSet.add(parentObs1);
		
		Obs parentObs2 = (createObsMock(concept2, "parent-obs-2"));
		parentObs2.addGroupMember(createObsMock(concept1, "child-obs-2-1"));
		parentObs2.addGroupMember(createObsMock(concept1, "child-obs-2-2"));
		parentObs2.addGroupMember(createObsMock(concept1, "child-obs-2-3"));
		obsSet.add(parentObs2);
		
		Obs parentObs3 = (createObsMock(concept1, null));
		parentObs3.addGroupMember(createObsMock(concept2, null));
		parentObs3.addGroupMember(createObsMock(concept1, null));
		obsSet.add(parentObs3);
		
		encounter.setObs(obsSet);
		return encounter;
	}
	
}
