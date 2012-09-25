<%@ include file="/WEB-INF/template/include.jsp"%>

<openmrs:require privilege="Manage Forms" otherwise="/login.htm" redirect="/module/htmlformentry/htmlForm.list" />

<c:set var="DO_NOT_INCLUDE_JQUERY" value="true"/>

<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="localHeader.jsp" %>

<openmrs:htmlInclude file="/moduleResources/htmlformentry/jquery-ui-1.8.17.custom.css" />
<openmrs:htmlInclude file="/moduleResources/htmlformentry/jquery-1.4.2.min.js" />
<openmrs:htmlInclude file="/moduleResources/htmlformentry/jquery-ui-1.8.17.custom.min.js" />
<openmrs:htmlInclude file="/dwr/util.js" />

<br />
<spring:message code="htmlformentry.previewFile.message" />
<br />
<form method="get">
	<b>
		Located at: <input type="text" name="filePath" <c:if test="${isFileUpload == false}"> value="${filePath}"</c:if> size="40" />
		<input type="submit" value="Preview"/>
		<input type="hidden" name="isFileUpload" value="false" />
	</b>
</form>

<b><spring:message code="htmlformentry.or" /></b>
<br />

<spring:message code="htmlformentry.previewFile.upload" />
<br/>
<form method="POST" enctype="multipart/form-data">
	<b>
		<input type="file" name="htmlFormFile" size="40" />
		<input type="submit" value='Preview' />
		<input type="hidden" name="isFileUpload" value="true" />
	</b>
</form>
<br/>

<c:if test="${ not empty message }">
	<span style="color:red;">${message}</span>
	<br/>
</c:if>

<c:if test="${ not empty previewHtml }">
	<input type="button" value="View Schema" onclick="document.location.href='htmlFormSchema.form?filePath=${filePath}';"/>
	<br/><br/>
</c:if>
<hr/>

<c:if test="${ not empty previewHtml }">

	<script>
		var propertyAccessorInfo = new Array();
		var beforeValidation = new Array();     // a list of functions that will be executed before the validation of a form
		var beforeSubmit = new Array(); 		// a list of functions that will be executed before the submission of a form
	</script>
	
	<openmrs:htmlInclude file="/scripts/calendar/calendar.js" />
	<openmrs:htmlInclude file="/moduleResources/htmlformentry/htmlFormEntry.js" />
	<openmrs:htmlInclude file="/moduleResources/htmlformentry/htmlFormEntry.css" />

	${ previewHtml }
</c:if>

<%@ include file="/WEB-INF/template/footer.jsp"%>