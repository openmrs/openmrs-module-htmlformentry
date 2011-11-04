<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Forms" otherwise="/login.htm" redirect="/admin/forms/form.list" />

<%@ include file="/WEB-INF/template/header.jsp" %>
<%@ include file="localHeader.jsp" %>

<h2><spring:message code="htmlformentry.migrateNamesAndDescriptions" /></h2>

<div class="boxHeader">
	You have HTML Forms that share underlying Forms
</div>
<div class="box">
	<form method="post">
		<input type="hidden" name="migration" value="duplicateForms"/>
		Since version 1.7 of HTML Form Entry, we do not support having multiple HTML Forms
		for the same underlying core Form. You have some of these that must be migrated.
		<br/><br/>
		For each of the following groups of HTML Forms that share an underlying Form, we
		will leave one HTML Form as-is, and duplicate the underlying Form for all the rest
		of the HTML Forms. 
		<br/><br/>
		<b><i>Pick the one form in each group that is used most often.</i></b>
		<br/>
		(All existing encounters created from any of the forms in the group will now open with
		the primary form you specify now. Newly-created encounters will open with the form
		that creates them.)
		<c:forEach var="group" items="${duplicateForms}" varStatus="status">
			<c:set var="underlying" value="${group.value[0].form}"/>
			<div class='${status.index % 2 == 0 ? "evenRow" : "oddRow"}'>
				<br/>
				<u>
					Pick the primary HTML Form for this underlying form:
					${underlying.name} (v${underlying.version})
				</u>
				<br/>
				<c:forEach var="htmlForm" items="${group.value}">
					<input type="radio" name="group.${underlying.id}" value="${htmlForm.id}"/>
					${htmlForm.deprecatedName}
					<a href="htmlForm.form?id=${htmlForm.id}" target="preview">
						Preview
					</a>
					<br/>
				</c:forEach>
				<br/>
			</div>
		</c:forEach>
		<input type="submit" value='<spring:message code="general.save"/>'/>
	</form>
</div>			

<%@ include file="/WEB-INF/template/footer.jsp"%>