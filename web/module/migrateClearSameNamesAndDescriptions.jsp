<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Forms" otherwise="/login.htm" redirect="/admin/forms/form.list" />

<%@ include file="/WEB-INF/template/header.jsp" %>
<%@ include file="localHeader.jsp" %>

<h2><spring:message code="htmlformentry.migrateNamesAndDescriptions" /></h2>

<div class="boxHeader">
	Some HTML Forms can be automatically upgraded
</div>
<div class="box">
	<form method="post" action="migrateNamesAndDescriptions.form">
		<input type="hidden" name="migration" value="clearNamesAndDescriptions"/>
		Since version 1.7 of HTML Form Entry, we do not support letting an HTML Form have
		a different name or description from its underlying core Form.
		<br/><br/>
		You have some forms whose name and/or description exactly match those in their
		underlying core Form.
		<br/><br/>
		<b><i>Continue to automatically upgrade the following:</i></b>
		<ul>
			<c:forEach var="htmlForm" items="${sameName}">
				<li>
					Same name: ${htmlForm.name}
					<input type="hidden" name="clearName" value="${htmlForm.id}"/>
				</li>
			</c:forEach>
			<c:forEach var="htmlForm" items="${sameDescription}">
				<li>
					Same description: ${htmlForm.description}
					<input type="hidden" name="clearDescription" value="${htmlForm.id}"/>
				</li>
			</c:forEach>
		</ul>
		<input type="submit" value='<spring:message code="general.continue"/>'/>
	</form>
</div>

<%@ include file="/WEB-INF/template/footer.jsp"%>