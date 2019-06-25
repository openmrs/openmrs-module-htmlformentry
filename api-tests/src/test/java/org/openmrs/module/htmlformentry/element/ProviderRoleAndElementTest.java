package org.openmrs.module.htmlformentry.element;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.Provider;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

//@Ignore
public class ProviderRoleAndElementTest extends BaseModuleContextSensitiveTest {

    // TODO: figure out why these tests are failing on bamboo and re-enable!

    @Before
    public void setup() throws Exception {
        executeDataSet("org/openmrs/module/htmlformentry/include/providerRoles-dataset.xml");
    }

    @Test
    //@Ignore
    public void getProviderList_shouldReturnAllProvidersIfNoRoleSpecified() throws Exception {

        FormEntryContext context = new FormEntryContext(FormEntryContext.Mode.EDIT);
        Map<String, String> params = new HashMap<String,String>();
        List<Provider> providerList = new ProviderAndRoleElement(context, params).getTag().getProviders();

        // there are 4 providers in the provider roles dataset and one in the standard test dataset
        assertThat(providerList.size(), is(5));
    }

    @Test
    //@Ignore
    public void getProviderList_shouldReturnProvidersForASingleProviderRole() throws Exception {

        FormEntryContext context = new FormEntryContext(FormEntryContext.Mode.EDIT);
        Map<String, String> params = new HashMap<String,String>();
        params.put("providerRoles", "ea7f523f-27ce-4bb2-86d6-6d1d05312bd5");

        List<Provider> providerList = new ProviderAndRoleElement(context, params).getTag().getProviders();
        assertThat(providerList.size(), is(1));
        assertThat(providerList.get(0).getId(), is(1006));

    }

    @Test
    //@Ignore
    public void getProviderList_shouldReturnProvidersForMultipleProviderRole() throws Exception {

        FormEntryContext context = new FormEntryContext(FormEntryContext.Mode.EDIT);
        Map<String, String> params = new HashMap<String,String>();
        params.put("providerRoles","ea7f523f-27ce-4bb2-86d6-6d1d05312bd5 , 1001");

        List<Provider> providerList = new ProviderAndRoleElement(context, params).getTag().getProviders();
        assertThat(providerList.size(), is(4));

        List<Integer> resultProviderIds = Arrays.asList(1003, 1004, 1005, 1006);

        for (Provider provider : providerList) {
            assertTrue(resultProviderIds.contains(provider.getId()));
        }

    }

    @Test(expected = BadFormDesignException.class)
    //@Ignore
    public void getProviderList_shouldFailIfInvalidId() throws Exception {

        FormEntryContext context = new FormEntryContext(FormEntryContext.Mode.EDIT);
        Map<String, String> params = new HashMap<String,String>();
        params.put("providerRoles", "ea7f523f-27ce-42bd5");

        List<Provider> providerList = new ProviderAndRoleElement(context, params).getTag().getProviders();
    }

}
