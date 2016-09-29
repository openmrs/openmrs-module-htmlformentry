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
package org.openmrs.module.htmlformentry;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.EncounterRole;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Integration test for the <encounterProviderAndRole/> tag 
 */
public class EncounterProviderAndRoleTagTest extends BaseModuleContextSensitiveTest {

	@Before
	public void setup() throws Exception {
		executeDataSet("org/openmrs/module/htmlformentry/include/encounterProviderAndRole.xml");
		new HtmlFormEntryActivator().started();
	}
	
	@Test
	public void encounterProviderAndRole_testPlainTag() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			protected String getXmlDatasetPath() {
				return "org/openmrs/module/htmlformentry/include/";
			}
			
			@Override
            public String getFormName() {
				return "plainEncounterProviderAndRoleTag";
			}
						
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "ProviderAndRole:", "ProviderAndRole:!!1"};
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("ProviderAndRole:"), "3"); // encounter role
				request.addParameter(widgets.get("ProviderAndRole:!!1"), "2"); // provider
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertLocation(2);
				Map<EncounterRole, Set<Provider>> byRoles = results.getEncounterCreated().getProvidersByRoles();
				Assert.assertEquals(1, byRoles.size());
				EncounterRole encRole = byRoles.keySet().iterator().next();
				Assert.assertEquals(Integer.valueOf(3), encRole.getEncounterRoleId());
				Assert.assertEquals(Integer.valueOf(2), byRoles.get(encRole).iterator().next().getProviderId());
			}
		}.run();
	}

    @Test
    public void encounterProviderAndRole_testTagWithRequiredAttribute() throws Exception {
        final Date date = new Date();
        new RegressionTestHelper() {

            @Override
            protected String getXmlDatasetPath() {
                return "org/openmrs/module/htmlformentry/include/";
            }

            @Override
            public String getFormName() {
                return "requiredEncounterProviderAndRoleTag";
            }

            @Override
            public String[] widgetLabels() {
                return new String[] { "Date:", "Location:", "ProviderAndRole:", "ProviderAndRole:!!1"};
            }

            @Override
            public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
                request.addParameter(widgets.get("Date:"), dateAsString(date));
                request.addParameter(widgets.get("Location:"), "2");
                request.addParameter(widgets.get("ProviderAndRole:"), "3"); // encounter role
                request.addParameter(widgets.get("ProviderAndRole:!!1"), "2"); // provider
            }

            @Override
            public void testResults(SubmissionResults results) {
                results.assertNoErrors();
                results.assertEncounterCreated();
                results.assertLocation(2);
                Map<EncounterRole, Set<Provider>> byRoles = results.getEncounterCreated().getProvidersByRoles();
                Assert.assertEquals(1, byRoles.size());
                EncounterRole encRole = byRoles.keySet().iterator().next();
                Assert.assertEquals(Integer.valueOf(3), encRole.getEncounterRoleId());
                Assert.assertEquals(Integer.valueOf(2), byRoles.get(encRole).iterator().next().getProviderId());
            }
        }.run();
    }

    @Test(expected = AssertionError.class)
    public void encounterProviderAndRole_testTagWithRequiredAttribute_shouldThrowExceptionIfNoProvider() throws Exception {
        final Date date = new Date();
        new RegressionTestHelper() {

            @Override
            protected String getXmlDatasetPath() {
                return "org/openmrs/module/htmlformentry/include/";
            }

            @Override
            public String getFormName() {
                return "requiredEncounterProviderAndRoleTag";
            }

            @Override
            public String[] widgetLabels() {
                return new String[] { "Date:", "Location:", "ProviderAndRole:", "ProviderAndRole:!!1"};
            }

            @Override
            public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
                request.addParameter(widgets.get("Date:"), dateAsString(date));
                request.addParameter(widgets.get("Location:"), "2");
                request.addParameter(widgets.get("ProviderAndRole:"), "3"); // encounter role
            }

            @Override
            public void testResults(SubmissionResults results) {
                results.assertNoErrors();
                results.assertEncounterCreated();
                results.assertLocation(2);
                Map<EncounterRole, Set<Provider>> byRoles = results.getEncounterCreated().getProvidersByRoles();
                Assert.assertEquals(1, byRoles.size());
                EncounterRole encRole = byRoles.keySet().iterator().next();
                Assert.assertEquals(Integer.valueOf(3), encRole.getEncounterRoleId());
                Assert.assertEquals(Integer.valueOf(2), byRoles.get(encRole).iterator().next().getProviderId());
            }
        }.run();
    }


    @Test
	public void encounterProviderAndRole_testDefaultValue() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			protected String getXmlDatasetPath() {
				return "org/openmrs/module/htmlformentry/include/";
			}
			
			@Override
            public String getFormName() {
				return "encounterProviderAndRoleTagWithDefault";
			}

			@Override
			public void testBlankFormHtml(String html) {
				Assert.assertTrue(html.contains("<option selected=\"true\" value=\"2\">"));
			};
		}.run();
	}
    @Test
    public void encounterProviderAndRole_WithMultipleDropdownsOnlySetsOneToDefault() throws Exception {
        new RegressionTestHelper() {

            @Override
            protected String getXmlDatasetPath() {
                return "org/openmrs/module/htmlformentry/include/";
            }

            @Override
            public String getFormName() {
                return "encounterProviderAndRoleTagWithMultipleAndDefault";
            }

            @Override
            public void testBlankFormHtml(String html) {
                Assert.assertTrue(html.contains("<option selected=\"true\" value=\"2\">"));
                Assert.assertTrue(html.contains("<option value=\"2\">"));
            };
        }.run();
    }

	
	@Test
	public void encounterProviderAndRole_testTagSpecifyingEncounterProviderTwiceWithDifferentRoles() throws Exception {
		final Date date = new Date();
		new RegressionTestHelper() {
			
			@Override
			protected String getXmlDatasetPath() {
				return "org/openmrs/module/htmlformentry/include/";
			}
			
			@Override
            public String getFormName() {
				return "specifyingEncounterRoleTwiceWithDifferentRoles";
			}
						
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Doctor:", "Nurse:" };
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(date));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Doctor:"), "2"); // Doctor Bob
				request.addParameter(widgets.get("Nurse:"), "1"); // Superuser
			}
					
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertLocation(2);
				Map<EncounterRole, Set<Provider>> byRoles = results.getEncounterCreated().getProvidersByRoles();
				Assert.assertEquals(2, byRoles.size());
				Set<Provider> doctors = byRoles.get(Context.getEncounterService().getEncounterRole(3));
				Set<Provider> nurses = byRoles.get(Context.getEncounterService().getEncounterRole(2));
				Assert.assertEquals(1, doctors.size());
				Assert.assertEquals(1, nurses.size());
				Assert.assertEquals(2, (int) doctors.iterator().next().getProviderId());
				Assert.assertEquals(1, (int) nurses.iterator().next().getProviderId());
			}
			
			@Override
			public boolean doViewEncounter() {
				return true;
			}
			
			@Override
			public void testViewingEncounter(Encounter encounter, String html) {
				TestUtil.assertFuzzyEquals("Date:" + Context.getDateFormat().format(date) + " Location:Xanadu Doctor:Doctor Bob, M.D. Nurse:Super User", html);
			}
			
		}.run();
	}

    @Test
    public void encounterProviderAndRole_testTagSpecifyingEncounterProviderTwiceWithSameRole() throws Exception {
        final Date date = new Date();
        new RegressionTestHelper() {

            @Override
            protected String getXmlDatasetPath() {
                return "org/openmrs/module/htmlformentry/include/";
            }

            @Override
            public String getFormName() {
                return "specifyingEncounterRoleTwiceWithSameRole";
            }

            @Override
            public String[] widgetLabels() {
                return new String[] { "Date:", "Location:", "Doctors:", "Doctors:!!1" };
            }

            @Override
            public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
                request.addParameter(widgets.get("Date:"), dateAsString(date));
                request.addParameter(widgets.get("Location:"), "2");
                request.addParameter(widgets.get("Doctors:"), "2"); // Doctor Bob
                request.addParameter(widgets.get("Doctors:!!1"), "1"); // Superuser  (hack to reference by widget id, but no label for this widget
            }

            @Override
            public void testResults(SubmissionResults results) {
                results.assertNoErrors();
                results.assertEncounterCreated();
                results.assertLocation(2);

                Map<EncounterRole, Set<Provider>> byRoles = results.getEncounterCreated().getProvidersByRoles();
                Assert.assertEquals(1, byRoles.size());

                Set<Provider> doctors = byRoles.get(Context.getEncounterService().getEncounterRole(3));
                Assert.assertEquals(2, doctors.size());

                // we can't guarantee the order of providers, but make sure both providers are now present
                Set<Integer> providerIds = new HashSet<Integer>();
                for (Provider doctor : doctors) {
                    providerIds.add(doctor.getId());
                }
                Assert.assertEquals(2, providerIds.size());
                Assert.assertTrue(providerIds.contains(1));
                Assert.assertTrue(providerIds.contains(2));
            }

            @Override
            public boolean doViewEncounter() {
                return true;
            }

            @Override
            public void testViewingEncounter(Encounter encounter, String html) {
                TestUtil.assertFuzzyContains("Doctor Bob", html);
                TestUtil.assertFuzzyContains("Super User", html);
            }

        }.run();
    }

    @Test
    public void encounterProviderAndRole_testSpecifyingASingleProviderForTagThatAcceptsTwo() throws Exception {
        final Date date = new Date();
        new RegressionTestHelper() {

            @Override
            protected String getXmlDatasetPath() {
                return "org/openmrs/module/htmlformentry/include/";
            }

            @Override
            public String getFormName() {
                return "specifyingEncounterRoleTwiceWithSameRole";
            }

            @Override
            public String[] widgetLabels() {
                return new String[] { "Date:", "Location:", "Doctors:" };
            }

            @Override
            public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
                request.addParameter(widgets.get("Date:"), dateAsString(date));
                request.addParameter(widgets.get("Location:"), "2");
                request.addParameter(widgets.get("Doctors:"), "2"); // Doctor Bob
            }

            @Override
            public void testResults(SubmissionResults results) {
                results.assertNoErrors();
                results.assertEncounterCreated();
                results.assertLocation(2);

                Map<EncounterRole, Set<Provider>> byRoles = results.getEncounterCreated().getProvidersByRoles();
                Assert.assertEquals(1, byRoles.size());

                Set<Provider> doctors = byRoles.get(Context.getEncounterService().getEncounterRole(3));
                Assert.assertEquals(1, doctors.size());
                Assert.assertEquals(new Integer(2), doctors.iterator().next().getId());
            }

            @Override
            public boolean doViewEncounter() {
                return true;
            }

            @Override
            public void testViewingEncounter(Encounter encounter, String html) {
                TestUtil.assertFuzzyContains("Doctor Bob", html);
                TestUtil.assertFuzzyDoesNotContain("Super User", html);
            }

        }.run();
    }

    @Test
    public void encounterProviderAndRole_testRemovingProviderFromEncounter() throws Exception {
        final Date date = new Date();
        new RegressionTestHelper() {

            @Override
            protected String getXmlDatasetPath() {
                return "org/openmrs/module/htmlformentry/include/";
            }

            @Override
            public String getFormName() {
                return "specifyingEncounterRoleTwiceWithSameRole";
            }

            @Override
            public String[] widgetLabels() {
                return new String[] { "Date:", "Location:", "Doctors:", "Doctors:!!1" };
            }

            @Override
            public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {

                // first set two providers
                request.addParameter(widgets.get("Date:"), dateAsString(date));
                request.addParameter(widgets.get("Location:"), "2");
                request.addParameter(widgets.get("Doctors:"), "2"); // Doctor Bob
                request.addParameter(widgets.get("Doctors:!!1"), "1"); // Superuser

            }

            @Override
            public boolean doEditEncounter() {
                return true;
            }

            @Override
            public String[] widgetLabelsForEdit() {
                return new String[] { "Date:", "Location:", "Doctors:", "Doctors:!!1" };
            }

            @Override
            public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
                // now, in the edit request, only set a single provider
                request.setParameter(widgets.get("Doctors:"), "1"); // Superuser
                request.setParameter(widgets.get("Doctors:!!1"), ""); // set the second doctor field blank
            }

            @Override
            public void testEditedResults(SubmissionResults results) {
                results.assertNoErrors();
                results.assertLocation(2);

                Map<EncounterRole, Set<Provider>> byRoles = results.getEncounterCreated().getProvidersByRoles();
                Assert.assertEquals(1, byRoles.size());

                Set<Provider> doctors = byRoles.get(Context.getEncounterService().getEncounterRole(3));
                Assert.assertEquals(1, doctors.size());

                Assert.assertEquals(1, doctors.size());
                Assert.assertEquals(new Integer(1), doctors.iterator().next().getId());
            }
        }.run();
    }

    // TODO: figure out why this tests is failing on bamboo and re-enable!

    @Test
    @Ignore
    public void encounterProviderAndRole_testWithProviderRoleAttribute() throws Exception {

        // load the provider role specific test dataset
        executeDataSet("org/openmrs/module/htmlformentry/include/providerRoles-dataset.xml");

        final Date date = new Date();
        new RegressionTestHelper() {

            @Override
            protected String getXmlDatasetPath() {
                return "org/openmrs/module/htmlformentry/include/";
            }

            @Override
            public String getFormName() {
                return "encounterProviderAndRoleTagWithProviderRolesAttribute";
            }

            @Override
            public void testBlankFormHtml(String html) {
                TestUtil.assertFuzzyContains("Mr. Horatio Test Hornblower", html);
                TestUtil.assertFuzzyContains("Johnny Test Doe", html);
                TestUtil.assertFuzzyContains("Collet Test Chebaskwony", html);

                TestUtil.assertFuzzyDoesNotContain("Anet Test Oloo", html);
            }

        }.run();
    }


}
