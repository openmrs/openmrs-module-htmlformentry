package org.openmrs.module.htmlformentry;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.PersonName;
import org.openmrs.Provider;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.element.ProviderStub;
import org.openmrs.module.htmlformentry.util.MatchMode;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class HtmlFormEntryUtilTest extends BaseModuleContextSensitiveTest {

    @Before
    public void setup() throws Exception {
       executeDataSet("org/openmrs/module/htmlformentry/include/provider-dataset.xml");
    }

    @Test
    public void getFullNameWithFamilyNameFirst_shouldReturnProperSimpleName() {
        PersonName name = new PersonName();
        name.setGivenName("Mark");
        name.setFamilyName("Goodrich");
        assertThat(HtmlFormEntryUtil.getFullNameWithFamilyNameFirst(name), is("Goodrich, Mark"));
    }

    @Test
    public void getFullNameWithFamilyNameFirst_shouldReturnProperFullName() {
        PersonName name = new PersonName();
        name.setPrefix("Mr.");
        name.setGivenName("Mark");
        name.setMiddleName("Brutus");
        name.setFamilyNamePrefix("de");
        name.setFamilyName("Cameroon");
        name.setFamilyName2("Smith");
        name.setFamilyNameSuffix("Jr.");
        name.setDegree("Esq.");
        assertThat(HtmlFormEntryUtil.getFullNameWithFamilyNameFirst(name), is("de Cameroon Smith Jr., Mr. Mark Brutus Esq."));
    }

    @Test
    public void getFullNameWithFamilyNameFirst_shouldNotFailIfProviderDoesNotHaveFamilyName() {
        PersonName name = new PersonName();
        name.setGivenName("Mark");
        assertThat(HtmlFormEntryUtil.getFullNameWithFamilyNameFirst(name), is("Mark"));
    }

    @Test
    public void getFullNameWithFamilyNameFirst_shouldNotFailIfProviderHasNoName() {
        PersonName name = new PersonName();
        assertThat(HtmlFormEntryUtil.getFullNameWithFamilyNameFirst(name), is(""));
    }
    
	@Test
	public void getFullNameWithFamilyNameFirst_shouldReturnDeletedIfPersonNameIsNull() {
		assertThat(HtmlFormEntryUtil.getFullNameWithFamilyNameFirst(null), is("["
		        + Context.getMessageSourceService().getMessage("htmlformentry.unknownProviderName") + "]"));
	}

    /**
     * Tests for person names associated with a person object attached to the provider object.
     */
    @Test
    public  void getProviders_shouldReturnProviderStubsWhosePersonNamesStartWithAGivenString() {
        ProviderService providerService = Context.getProviderService();

        String search = "sura";
        List<Provider> providers = providerService.getProviders(search, null, null, null);
        List<ProviderStub> providerStubs =
                HtmlFormEntryUtil.getProviderStubs(providers,search, MatchMode.START);
        Provider provider = providerService.getProvider(13002);    //From provider-dataset.xml

        assertTrue(providerStubs.contains(new ProviderStub(provider)));
    }

    /**
     * This tests for names stored directly in provider object as name field.
     */
    @Test
    public void getProviders_shouldReturnProvidersStubsWhoseProviderNameStartWithAGivenString() {
        ProviderService providerService = Context.getProviderService();
        String search = "kisa";
        List<Provider> providers = providerService.getProviders(search,null,null,null);
        List<ProviderStub> providerStubs = HtmlFormEntryUtil.getProviderStubs(providers,search,MatchMode.START);
        Provider provider = providerService.getProvider(13010);          //From provider-datasets.xml

        assertTrue(providerStubs.contains(new ProviderStub(provider)));
    }

    /**
     * Tests for provider identifier stored in provider object
     */
    @Test
    public void getProviders_shouldReturnProviderStubsWhoseIdentifiersStartWithAGivenString() {
        ProviderService providerService = Context.getProviderService();
        String search = "5566-1";
        List<Provider> providers = providerService.getProviders(search,null,null,null);
        List<ProviderStub> providerStubs = HtmlFormEntryUtil.getProviderStubs(providers,search,MatchMode.START);
        Provider provider = providerService.getProvider(13002);

        assertTrue(providerStubs.contains(new ProviderStub(provider)));
    }

    /**
     * This tests for names stores as person names stored as names of associated person object.
     */
    @Test
    public void getProviders_shouldReturnProviderStubsWhosePersonNamesEndsWithAString() {
        ProviderService providerService = Context.getProviderService();
        String search = "rathne";
        List<Provider> providers = providerService.getProviders(search, null, null, null);
        List<ProviderStub> providerStubs =
                HtmlFormEntryUtil.getProviderStubs(providers,search,MatchMode.END);
        Provider provider = providerService.getProvider(13002);    //From provider-dataset.xml

        assertTrue(providerStubs.contains(new ProviderStub(provider)));
    }


    /**
     * Tests for names stored directly in provider object as a name field
     */
    @Test
    public void getProviders_shouldReturnProviderStubsWhoseProviderNamesEndWithAGivenString() {
        ProviderService providerService = Context.getProviderService();
        String search = "Sanga";
        List<Provider> providers = providerService.getProviders(search,null,null,null);
        List<ProviderStub> providerStubs = HtmlFormEntryUtil.getProviderStubs(providers,search,MatchMode.END);
        Provider provider = providerService.getProvider(13010);          //From provider-datasets.xml

        assertTrue(providerStubs.contains(new ProviderStub(provider)));
    }

    /**
     * Tests for provider identifier stored in provider table
     */
    @Test
    public void getProviders_shouldReturnProviderStubsWhoseIdentifierEndsWithAGivenString() {
        ProviderService providerService = Context.getProviderService();
        String search = "05-999";
        List<Provider> providers = providerService.getProviders(search,null,null,null);
        List<ProviderStub> providerStubs = HtmlFormEntryUtil.getProviderStubs(providers,search,MatchMode.END);
        Provider provider = providerService.getProvider(13010);

        assertTrue(providerStubs.contains(new ProviderStub(provider)));
    }

}
