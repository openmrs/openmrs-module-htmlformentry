package org.openmrs.module.htmlformentry.element;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.util.OpenmrsUtil;


/**
 * A "Stub" version of a person added for performance reasons when dealing with large numbers of persons
 */
public class PersonStub extends ValueStub {

    private String familyName;
    private String givenName;
    private String middleName;
    private String familyName2;
    
    
    public PersonStub(){}
    
    public PersonStub(Integer personId){
        this.setId(personId);
    }
    public PersonStub(Person person){
        if (person != null){
            this.setId(person.getPersonId());
            
            PersonName name = person.getPersonName();
            
            if (name != null) {
            	this.givenName = name.getGivenName();
            	this.middleName = name.getMiddleName();
            	this.familyName = name.getFamilyName(); 
            	this.familyName2 = name.getFamilyName2();
            }
        }
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
    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getFamilyName2() {
        return familyName2;
    }

    public void setFamilyName2(String familyName2) {
        this.familyName2 = familyName2;
    }
    
    /**
     * 
     * @see org.openmrs.module.htmlformentry.element.ValueStub#getDisplayValue()
     * NOTE: setResultTransformer(Transformers.aliasToBean(PersonStub.class)) isn't compatible with the 
     * mysql CONCAT function for some reason, so I wasn't able to build the display value in the Hibernate SQLQuery itself
     */
    @Override
    public String getDisplayValue(){
        String displayValue = (StringUtils.isNotBlank(this.getGivenName()) ? this.getGivenName() + " " : "") + (StringUtils.isNotBlank(this.getMiddleName()) ? this.getMiddleName() + " " : "") +
        						(StringUtils.isNotBlank(this.getFamilyName()) ? this.getFamilyName() + " " : "") + (StringUtils.isNotBlank(this.getFamilyName2()) ? this.getFamilyName2() : "");
          
        // remove the trailing space
        displayValue = displayValue.replaceAll("\\s$", "");
        
        return displayValue;
    }
    
    @Override
    public String toString() {
    	return getDisplayValue();
    }
    
    @Override
    public boolean equals(Object o){
        if (o != null && o instanceof PersonStub){
            PersonStub oOther = (PersonStub) o;
            if (OpenmrsUtil.nullSafeEquals(oOther.getId(), this.getId()))
                    return true;
        }
        return false;
    }
}
