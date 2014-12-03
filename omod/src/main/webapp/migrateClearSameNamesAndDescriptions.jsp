<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Forms" otherwise="/login.htm" redirect="/admin/forms/form.list" />

<%@ include file="/WEB-INF/template/header.jsp" %>
<%@ include file="localHeader.jsp" %>

<h2><spring:message code="htmlformentry.migrateNamesAndDescriptions" /></h2>

<div class="boxHeader">
	<spring:message code="htmlformentry.migrate.autoUpgrade" />
</div>
<div class="box">
	<form method="post" action="migrateNamesAndDescriptions.form">
		<input type="hidden" name="migration" value="clearNamesAndDescriptions"/>
		<spring:message code="htmlformentry.migrate.differentNameNotSupported" />
		<br/><br/>
		<spring:message code="htmlformentry.migrate.someNamesMatch" />
		<br/><br/>
		<b><i><spring:message code="htmlformentry.migrate.continueUpgrade" /></i></b>
		<ul>
			<c:forEach var="htmlForm" items="${sameName}">
				<li>
					<spring:message code="htmlformentry.migrate.sameName" />: ${htmlForm.name}
					<input type="hidden" name="clearName" value="${htmlForm.id}"/>
				</li>
			</c:forEach>
			<c:forEach var="htmlForm" items="${sameDescription}">
				<li>
					<spring:message code="htmlformentry.migrate.sameDescription" />: ${htmlForm.description}
					<input type="hidden" name="clearDescription" value="${htmlForm.id}"/>
				</li>
			</c:forEach>
		</ul>
		<input type="submit" value='<spring:message code="general.continue"/>'/>
	</form>
</div>

<%@ include file="/WEB-INF/template/footer.jsp"%>