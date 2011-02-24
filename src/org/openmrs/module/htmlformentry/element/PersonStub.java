package org.openmrs.module.htmlformentry.element;

import org.openmrs.Person;
import org.openmrs.util.OpenmrsUtil;


public class PersonStub {

    private String familyName;
    private String givenName;
    private Integer personId;
    
    
    public PersonStub(){}
    
    public PersonStub(Integer personId){
        this.personId = personId;
    }
    public PersonStub(Person person){
        if (person != null){
            this.personId = person.getPersonId();
            this.givenName = person.getGivenName();
            this.familyName = person.getFamilyName();
        }
     }
    
    public Integer getPersonId() {
        return personId;
    }

    public void setPersonId(Integer personId) {
        this.personId = personId;
    } 
    public String getFamilyName() {
        return familyName;
    }
    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }
    public String getGivenName() {
        return givenName;
    }
    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }
    
    @Override
    public boolean equals(Object o){
        if (o != null && o instanceof PersonStub){
            PersonStub oOther = (PersonStub) o;
            if (OpenmrsUtil.nullSafeEquals(oOther.getPersonId(), this.getPersonId()))
                    return true;
        }
        return false;
    }
    
}
