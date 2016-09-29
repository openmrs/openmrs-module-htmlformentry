package org.openmrs.module.htmlformentry;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.providermanagement.ProviderRole;
import org.openmrs.module.providermanagement.api.ProviderManagementService;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@Ignore
public class HtmlFormEntryUtilComponentTest extends BaseModuleContextSensitiveTest {

    // TODO: figure out why these tests are failing on bamboo and re-enable!

    @Before
    public void setup() throws Exception {
        executeDataSet("org/openmrs/module/htmlformentry/include/providerRoles-dataset.xml");
    }

    @Test
    @Ignore
    public void getProviderRole_shouldGetProviderRoleById() {
        Object providerRole = HtmlFormEntryUtil.getProviderRole("1002");
        assertThat(((ProviderRole) providerRole).getName(), is("Binome supervisor"));
    }

    @Test
    @Ignore
    public void getProviderRole_shouldGetProviderRoleByUuid() {
        Object providerRole = HtmlFormEntryUtil.getProviderRole("ea7f523f-27ce-4bb2-86d6-6d1d05312bd5");
        assertThat(((ProviderRole) providerRole).getName(), is("Binome supervisor"));
    }

    @Test
    @Ignore
    public void getProviderRole_shouldReturnNullIfBogusId() {
        Object providerRole = HtmlFormEntryUtil.getProviderRole("some bogus text");
        assertNull(providerRole);
    }

    @Test
    @Ignore
    public void getProviderRole_shouldReturnNullIfBlank() {
        Object providerRole = HtmlFormEntryUtil.getProviderRole("");
        assertNull(providerRole);
    }

    @Test
    @Ignore
    public void getProviders_shouldReturnProvidersForSingleRole() {
        ProviderRole providerRole = Context.getService(ProviderManagementService.class).getProviderRole(1002);
        List<Provider> providers = HtmlFormEntryUtil.getProviders(Collections.singletonList(providerRole));
        assertThat(providers.size(), is(1));
        assertThat(providers.get(0).getId(), is(1006));
    }

    @Test
    @Ignore
    public void getProviders_shouldReturnProvidersForMultipleRole() {

        List<ProviderRole> providerRoles = new ArrayList<ProviderRole>();
        providerRoles.add(Context.getService(ProviderManagementService.class).getProviderRole(1001));
        providerRoles.add(Context.getService(ProviderManagementService.class).getProviderRole(1002));

        List<Provider> providers = HtmlFormEntryUtil.getProviders(providerRoles);
        assertThat(providers.size(), is(4));

        List<Integer> resultProviderIds = Arrays.asList(1003,1004,1005, 1006);

        for (Provider provider : providers) {
            assertTrue(resultProviderIds.contains(provider.getId()));
        }
    }

    @Test
    @Ignore
    public void getProviders_shouldReturnEmptyListIfNoMatches() {
        ProviderRole providerRole = Context.getService(ProviderManagementService.class).getProviderRole(1004);
        List<Provider> providers = HtmlFormEntryUtil.getProviders(Collections.singletonList(providerRole));
        assertThat(providers.size(), is(0));
    }

    @Test
    @Ignore
    public void getProviders_shouldReturnEmptyListIfPassedNull() {
        List<Provider> providers = HtmlFormEntryUtil.getProviders(null);
        assertThat(providers.size(), is(0));
    }

    @Test
    @Ignore
    public void getProviders_shouldReturnEmptyListIfPassedEmptyList() {
        List<Provider> providers = HtmlFormEntryUtil.getProviders(new ArrayList<ProviderRole>());
        assertThat(providers.size(), is(0));
    }

}
