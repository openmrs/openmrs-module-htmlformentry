package org.openmrs.module.htmlformentry.util;

import org.openmrs.Provider;
import org.openmrs.module.htmlformentry.element.ProviderStub;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ProviderTransformer implements Transformer<Provider,ProviderStub> {
    @Override
    public Collection<ProviderStub> transform(Collection<Provider> collection, Predicate<Provider> predicate) {
        List<ProviderStub> providerStubs = new ArrayList<ProviderStub>();
        for(Provider p:collection) {
            if(predicate.test(p)) providerStubs.add(new ProviderStub(p));
        }
        return providerStubs;
    }
}
