[![Build Status](https://travis-ci.org/openmrs/openmrs-module-htmlformentry.svg?branch=master)](https://travis-ci.org/openmrs/openmrs-module-htmlformentry)

HTML Form Entry
=========

Overview
--------

The HTML Form Entry allows anyone with basic HTML programming skills and knowledge of the
OpenMRS system to create forms. It is an alternative to the Infopath
FormEntry module in many, (but not all) cases.

The key focus of writing forms with this module is that you only have
to write HTML (with some special tags for things in the OpenMRS model)
and the module will automatically "just know" what to do when the user
clicks the submit button.

Currently, a form submission creates one encounter for one patient.

Requirements
----------
+ OpenMRS 2.1.0+

Instructions
---------

+ Download the module from the repository and install it.
+ Go to "Manage HTML Forms" under the administration page.
+ Create a new form there (click "New HTML Form").
+ Fill out the necessary information; including the Name, Description, Version, and select the Encounter Type from the list.  (When an HTML form is submitted, it will create this type of encounter.)
+ Save the form.  Then it will open the page for editing the HTML Form.
+ Customize the HTML form to your specifications. Recent versions of the HTML Form Entry Module include a basic form that can be customized. For additional documentation on the available HTML tags see the [HTML Reference][].


Global Properties
----------------

+ *htmlformentry.dateFormat*: (added in HFE 1.9) lets you specify a date format (as defined in [Java's SimpleDateFormat][]) that will be used to display all dates in HTML Forms. This will hold for entering new forms and viewing/editing existing ones. (For example set the global property to "dd-MMM-yyyy" for an unambiguous date format like 31-Jan-2012.)
+ *htmlformentry.showDateFormat*: (added in HFE 1.9) set to true if you want static text for the date format to be displayed, otherwise set to false. This text is displayed next to the date widgets as something like (dd/mm/yyyy)

Project Resources
---------

[Wiki page][]: You can find screenshots, example HTML code, and flowsheets on this page

[HTML Form Entry Module]: https://wiki.openmrs.org/display/docs/HTML+Form+Entry+Module
[View/download source code for htmlformentry]: https://github.com/OpenMRS/openmrs-module-htmlformentry
[Download the Htmlformentry module]: http://modules.openmrs.org/modules/view.jsp?module=htmlformentry
[HTML Reference]: http://archive.openmrs.org/wiki/HTML_Form_Entry_Module_HTML_Reference
[Wiki page]: https://wiki.openmrs.org/display/docs/HTML+Form+Entry+Module
[Java's SimpleDateFormat]: http://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html
