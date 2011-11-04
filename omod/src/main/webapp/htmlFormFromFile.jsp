<%@ include file="/WEB-INF/template/include.jsp"%>

<openmrs:require privilege="Manage Forms" otherwise="/login.htm" redirect="/module/htmlformentry/htmlForm.list" />

<c:set var="DO_NOT_INCLUDE_JQUERY" value="true"/>

<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="localHeader.jsp" %>

<openmrs:htmlInclude file="/moduleResources/htmlformentry/jquery-ui-1.8.2.custom.css" />
<openmrs:htmlInclude file="/moduleResources/htmlformentry/jquery-1.4.2.min.js" />
<openmrs:htmlInclude file="/moduleResources/htmlformentry/jquery-ui-1.8.2.custom.min.js" />


<form method="get">
	<b>
		Preview HTML Form located at: <input type="text" name="filePath" value="${filePath}" size="40" />
		<input type="submit" value="Preview"/> 
		<c:if test="${ not empty previewHtml }">
			<input type="button" value="View Schema" onclick="document.location.href='htmlFormSchema.form?filePath=${filePath}';"/>
		</c:if>
	</b>
</form>
<br/>

<c:if test="${ not empty message }">
	<span style="color:red;">${message}</span>
</c:if>
<c:if test="${ not empty previewHtml }">
	
	<openmrs:htmlInclude file="/scripts/calendar/calendar.js" />
	<openmrs:htmlInclude file="/moduleResources/htmlformentry/htmlFormEntry.js" />
	<openmrs:htmlInclude file="/moduleResources/htmlformentry/htmlFormEntry.css" />

	${ previewHtml }
</c:if>

<%@ include file="/WEB-INF/template/footer.jsp"%>