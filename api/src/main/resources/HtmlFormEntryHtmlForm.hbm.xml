<?xml version="1.0"?>
<!DOCTYPE hibernate-mapping PUBLIC
    "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
    "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd" >

<hibernate-mapping package="org.openmrs.module.htmlformentry">

	<class name="HtmlForm" table="htmlformentry_html_form">

		<id name="id" type="int" column="id" unsaved-value="0">
			<generator class="native" />
		</id>
	
		<many-to-one name="form" class="org.openmrs.Form" not-null="false" column="form_id" />

		<!-- for how I came up with length, see https://issues.openmrs.org/browse/HTML-668 -->
		<property name="xmlData" type="java.lang.String" column="xml_data" length="4194303" not-null="true"/>

		<!--  Standard Openmrs MetaData -->
		<many-to-one name="creator" class="org.openmrs.User" not-null="true" />
		
		<property name="dateCreated" type="java.util.Date" column="date_created" not-null="true"
			length="19" />
			
		<many-to-one name="changedBy" column="changed_by" class="org.openmrs.User" not-null="false" />
		
		<property name="dateChanged" type="java.util.Date" column="date_changed" not-null="false"
			length="19" />

		<property name="retired" type="boolean" not-null="true" />
						
		<property name="deprecatedName" type="java.lang.String" column="name" length="255" />

		<property name="uuid" type="string" length="38" not-null="true" />
				
		<property name="deprecatedDescription" type="java.lang.String" column="description" />
		
		<many-to-one name="retiredBy" class="org.openmrs.User" column="retired_by" />
	
		<property name="dateRetired" type="java.util.Date" column="date_retired" />
	
		<property name="retireReason" type="string" column="retire_reason" />

	</class>

</hibernate-mapping>
