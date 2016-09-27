package org.openmrs.module.htmlformentry.element;

import org.openmrs.Person;
import org.openmrs.Provider;
import org.springframework.util.StringUtils;

public class ProviderStub extends ValueStub {
    private Integer providerId;
    private String identifier;
    private String name;
    private String uuid;

    public ProviderStub(Provider provider) {
        if(provider != null) {
            setId(provider.getProviderId());
            this.providerId = provider.getProviderId();
            this.identifier = provider.getIdentifier();
            if (StringUtils.hasLength(provider.getName())) {
                this.name = provider.getName();
            } else {
                //Use person names if they exist.
                Person p = provider.getPerson();
                if (p != null) {
                    this.name = p.getGivenName() + " " + p.getFamilyName() + ", " + p.getMiddleName();
                }
            }
            this.uuid = provider.getUuid();
        }
    }

    public Integer getProviderId() {
        return providerId;
    }

    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getDisplayValue() {
        return name + (StringUtils.hasLength(identifier)? " ("+identifier+")" : "");
    }

    @Override
    public boolean equals(Object o) {
        if(o != null && o instanceof ProviderStub){
            ProviderStub other = (ProviderStub)o;
            return this.getProviderId().intValue() == other.getProviderId().intValue();
        }
        return false;
    }
}
