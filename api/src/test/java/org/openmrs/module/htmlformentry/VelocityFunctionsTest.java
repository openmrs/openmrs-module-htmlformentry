package org.openmrs.module.htmlformentry;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;

public class VelocityFunctionsTest extends BaseModuleContextSensitiveTest {
	
	/**
	 * @see VelocityFunctions#earliestObs(Integer)
	 * @verifies return the first obs given the passed conceptId
	 */
	@Test
	public void earliestObs_shouldReturnTheFirstObsGivenThePassedConceptId() throws Exception {

		VelocityFunctions functions = setupFunctionsForPatient(7);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        
        Obs earliestWeight = functions.earliestObs(5089);
        Assert.assertEquals(50, earliestWeight.getValueNumeric().intValue());
        // this is a bit of a hack because for some reason the obsDatetime set for this obs in the standard test dataset changed between 1.7 and 1.8 
        Assert.assertTrue("Obs datetime not correct", (StringUtils.equals("2008-08-01", df.format(earliestWeight.getObsDatetime()))
        				|| StringUtils.equals("2008-07-01", df.format(earliestWeight.getObsDatetime()))));
  	}

	/**
	 * @see VelocityFunctions#latestObs(Integer)
	 * @verifies return the most recent obs given the passed conceptId
	 */
	@Test
	public void latestObs_shouldReturnTheMostRecentObsGivenThePassedConceptId() throws Exception {

		VelocityFunctions functions = setupFunctionsForPatient(7);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        
        Obs earliestWeight = functions.latestObs(5089);
        Assert.assertEquals(61, earliestWeight.getValueNumeric().intValue());
        Assert.assertEquals("2008-08-19", df.format(earliestWeight.getObsDatetime()));
	}
	
	/**
	 * @see VelocityFunctions@latestEncounter(EncounterType)
	 * @verifies return the most recent encounter if encounter type is null
	 */
	@Test
	public void latestEncounter_shouldReturnTheMostRecentEncounter() throws Exception {
		VelocityFunctions functions = setupFunctionsForPatient(7); 
        Assert.assertEquals(new Integer(5), functions.latestEncounter().getEncounterId());
	}
	
	/**
	 * @see VelocityFunctions@latestEncounter(EncounterType)
	 * @verifies return the most recent encounter if encounter type is null
	 */
	@Test
	public void latestEncounter_shouldReturnTheMostRecentEncounterByType() throws Exception {
		VelocityFunctions functions = setupFunctionsForPatient(7); 
		EncounterType type = Context.getEncounterService().getEncounterType(2);
        Assert.assertEquals(new Integer(3), functions.latestEncounter(type).getEncounterId());
	}
	
	/**
	 * @see VelocityFunctions@latestEncounter(EncounterType)
	 * @verifies return null if no matching encounter
	 */
	@Test
	public void latestEncounter_shouldReturnNullIfNoMatchingEncounter() throws Exception {
		VelocityFunctions functions = setupFunctionsForPatient(7); 
		EncounterType type = Context.getEncounterService().getEncounterType(6);
        Assert.assertNull(functions.latestEncounter(type));
	}
	
	/**
	 * @return a new VelocityFunctions instance for the given patientId
	 */
	private VelocityFunctions setupFunctionsForPatient(Integer patientId) throws Exception {
		HtmlForm htmlform = new HtmlForm();
		Form form = new Form();
		form.setEncounterType(new EncounterType(1));
        htmlform.setForm(form);
        htmlform.setDateChanged(new Date());
        htmlform.setXmlData("<htmlform></htmlform>");
        
        Patient p = new Patient(patientId);
        FormEntrySession session = new FormEntrySession(p, htmlform, null);
        return new VelocityFunctions(session);
	}
}
