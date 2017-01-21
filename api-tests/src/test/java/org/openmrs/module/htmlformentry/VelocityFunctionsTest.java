package org.openmrs.module.htmlformentry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Months;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.GlobalProperty;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class VelocityFunctionsTest extends BaseModuleContextSensitiveTest {

    private Integer ageInMonths;
    private Integer ageInDays;
	
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

    @Test
    public void earliestObs_shouldReturnTheFirstObsGivenThePassedConcepUuid() throws Exception {

        VelocityFunctions functions = setupFunctionsForPatient(7);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        Obs earliestWeight = functions.earliestObs("c607c80f-1ea9-4da3-bb88-6276ce8868dd");
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

    @Test
    public void latestObs_shouldReturnTheMostRecentObsGivenThePassedConceptUuid() throws Exception {

        VelocityFunctions functions = setupFunctionsForPatient(7);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        Obs earliestWeight = functions.latestObs("c607c80f-1ea9-4da3-bb88-6276ce8868dd");
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
		Encounter latestEncounter = functions.latestEncounter("2");
        Assert.assertEquals(new Integer(3), latestEncounter.getEncounterId());
	}
	
	/**
	 * @see VelocityFunctions@latestEncounter(EncounterType)
	 * @verifies return null if no matching encounter
	 */
	@Test
	public void latestEncounter_shouldReturnNullIfNoMatchingEncounter() throws Exception {
		VelocityFunctions functions = setupFunctionsForPatient(7);
        Assert.assertNull(functions.latestEncounter("6"));
	}
	
	/**
	 * @see VelocityFunctions@allEncounters(EncounterType)
	 * @verifies return all the encounters of the specified type
	 */
	public void allEncounters_shouldReturnAllTheEncountersOfTheSpecifiedType() throws Exception {
		VelocityFunctions functions = setupFunctionsForPatient(7);
		List<Encounter> allEncounters = functions.allEncounters("2");
		Assert.assertEquals(1, allEncounters.size());
	}

    /**
     *  @see  VelocityFunctions@patientAgeInMonths()
     *  @verifies return the ageInMonths accurately to the nearest month
     * @throws Exception
     */
    @Test
    public void patientAgeInMonths_shouldReturnAgeInMonthsAccurateToNearestMonth() throws Exception {
        VelocityFunctions functions = setupFunctionsForPatient(7);
        Assert.assertEquals(ageInMonths,functions.patientAgeInMonths());
    }

    /**
     *  @see  VelocityFunctions@patientAgeInDays()
     *  @verifies return the ageInDays accurately to the nearest date
     * @throws Exception
     */
    @Test
    public void patientAgeInDays_shouldReturnAgeInDaysAccuratelyToNearestDate() throws Exception {
        VelocityFunctions functions = setupFunctionsForPatient(7);
        Assert.assertEquals(ageInDays,functions.patientAgeInDays());
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
        String[] datePattern = {"yyyy.MM.dd"};
        p.setBirthdate(DateUtils.parseDate("1970.01.01",datePattern ));
        measureAgeInDaysAndMonths(new Date(), p.getBirthdate());
        FormEntrySession session = new FormEntrySession(p, htmlform, null);
        return new VelocityFunctions(session);
	}

    private void measureAgeInDaysAndMonths(Date dateChanged, Date birthdate) {
        ageInMonths = Months.monthsBetween
                (new DateTime(birthdate.getTime()).toDateMidnight(), new DateTime(dateChanged.getTime()).toDateMidnight()).getMonths();
        ageInDays = Days.daysBetween
                (new DateTime(birthdate.getTime()).toDateMidnight(), new DateTime(dateChanged.getTime()).toDateMidnight()).getDays();
    }

    /**
	 * @see VelocityFunctions@getConcept()
	 * @verifies return concept id of a concept code or uuid
	 */
	@Test
	public void getConcept_shouldReturnConceptWithGivenId() throws Exception {
		VelocityFunctions functions = setupFunctionsForPatient(7);
		Concept concept = Context.getConceptService ().getConcept (5089);
		Assert.assertEquals(concept.getDisplayString (), functions.getConcept ("5089").getDisplayString ());
	}

    @Test
    public void getObs_shouldReturnObsWithGivenConcept() throws Exception {
        Encounter encounter = Context.getEncounterService().getEncounter(4);
        VelocityFunctions functions = setupFunctionsForPatient(7);

        assertThat(functions.getObs(encounter, "5089").getValueNumeric(), is(55d));
        assertThat(functions.getObs(encounter, "3"), nullValue());
    }

    @Test
    public void allObs_shouldReturnObsWithGivenConcept() throws Exception {
        Encounter encounter = Context.getEncounterService().getEncounter(4);
        VelocityFunctions functions = setupFunctionsForPatient(7);

        List<Obs> allObs = functions.allObs(encounter, "5089");
        assertThat(allObs.size(), is(1));
        assertThat(allObs.get(0).getValueNumeric(), is(55d));
        assertThat(functions.allObs(encounter, "3").size(), is(0));
    }

    @Test
    public void testConstructLocale() throws Exception {
        VelocityFunctions functions = setupFunctionsForPatient(7);
        Locale en = functions.locale("en");
        assertThat(en, is(Locale.ENGLISH));
    }

    @Test
    public void globalProperty_shouldReturnGlobalPropertyValue() throws Exception {
        VelocityFunctions functions = setupFunctionsForPatient(7);
        setGlobalProperty("testGpName", "testGpValue");
        assertThat("testGpValue", is(functions.globalProperty("testGpName")));
    }

    @Test
    public void globalProperty_shouldReturnGlobalPropertyValueIfSet() throws Exception {
        VelocityFunctions functions = setupFunctionsForPatient(7);
        setGlobalProperty("testGpName", "testGpValue");
        assertThat("testGpValue", is(functions.globalProperty("testGpName", "")));
    }

    @Test
    public void globalProperty_shouldReturnDefaultValueIfGlobalPropertyNotSet() throws Exception {
        VelocityFunctions functions = setupFunctionsForPatient(7);
        setGlobalProperty("testGpName", "");
        assertThat("testGpDefault", is(functions.globalProperty("testGpName", "testGpDefault")));
    }

    private void setGlobalProperty(String name, String value) {
        GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(name);
        if (gp == null) {
            gp = new GlobalProperty(name);
        }
        gp.setPropertyValue(value);
        Context.getAdministrationService().saveGlobalProperty(gp);
    }
    
    @Test
	public void testLocation() throws Exception {
	    VelocityFunctions functions = setupFunctionsForPatient(7);
	    String locationIdIdentifier = new Integer(2).toString();
	    Assert.assertNotNull(functions.location(locationIdIdentifier));
	    Assert.assertEquals(locationIdIdentifier, functions.location(locationIdIdentifier).getId().toString());
	    
	    String locationUuidIdentifier = "8d6c993e-c2cc-11de-8d13-0010c6dffd0f";
	    Assert.assertNotNull(functions.location(locationUuidIdentifier));
	    Assert.assertEquals(locationUuidIdentifier, functions.location(locationUuidIdentifier).getUuid());
	    
	    String locationNameIdentifier = "Xanadu";
	    Assert.assertNotNull(functions.location(locationNameIdentifier));
	    Assert.assertEquals(locationNameIdentifier, functions.location(locationNameIdentifier).getName());
	    
	
    }
}
