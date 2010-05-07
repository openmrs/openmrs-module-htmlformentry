<%@ include file="/WEB-INF/template/include.jsp"%>

<%@ include file="/WEB-INF/template/header.jsp"%>

<openmrs:require privilege="Manage Forms" otherwise="/login.htm" redirect="/module/htmlformentry/htmlForm.list" />

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