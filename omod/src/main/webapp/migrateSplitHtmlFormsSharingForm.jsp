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
		<spring:message code="htmlformentry.migrate.sameCoreFormNotSupported" />
		<br/><br/>
		<spring:message code="htmlformentry.migrate.leaveOneDuplicateRest" />
		<br/><br/>
		<b><i><spring:message code="htmlformentry.migrate.pickUsedMostOften" /></i></b>
		<br/>
		<spring:message code="htmlformentry.migrate.existingEncounters" />
		<c:forEach var="group" items="${duplicateForms}" varStatus="status">
			<c:set var="underlying" value="${group.value[0].form}"/>
			<div class='${status.index % 2 == 0 ? "evenRow" : "oddRow"}'>
				<br/>
				<u>
					<spring:message code="htmlformentry.migrate.pickPrimary" />:
					${underlying.name} (v${underlying.version})
				</u>
				<br/>
				<c:forEach var="htmlForm" items="${group.value}">
					<input type="radio" name="group.${underlying.id}" value="${htmlForm.id}"/>
					${htmlForm.deprecatedName}
					<a href="htmlForm.form?id=${htmlForm.id}" target="preview">
						<spring:message code="htmlformentry.form.preview" />
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