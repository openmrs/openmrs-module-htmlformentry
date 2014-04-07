package org.openmrs.module.htmlformentry;

import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.api.ConceptService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ObsGroupTagTest extends BaseModuleContextSensitiveTest {

    @Autowired @Qualifier("conceptService")
    ConceptService conceptService;

    @Test
    public void testEmptyObsGroupIsNotDisplayed() throws Exception {
        new RegressionTestHelper() {
            @Override
            public String getFormName() {
                return "obsGroupShowIfEmptyFalse";
            }

            @Override
            public void testViewingEncounter(Encounter encounter, String html) {
                assertFalse(html.contains("It is displayed."));
            }
        }.run();
    }

    @Test
    public void testObsGroupIsDisplayed() throws Exception {
        new RegressionTestHelper() {
            @Override
            public String getFormName() {
                return "obsGroupShowIfEmptyFalse";
            }

            @Override
            public Encounter getEncounterToView() throws Exception {
                Encounter e = new Encounter();
                e.setPatient(getPatient());
                e.setDateCreated(new Date());

                TestUtil.addObsGroup(e, 23, new Date(), 18, Boolean.TRUE, new Date());
                return e;
            }

            @Override
            public void testViewingEncounter(Encounter encounter, String html) {
                assertTrue(html.contains("It is displayed."));
            }
        }.run();
    }

}