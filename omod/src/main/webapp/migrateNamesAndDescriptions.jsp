<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Forms" otherwise="/login.htm" redirect="/admin/forms/form.list" />

<%@ include file="/WEB-INF/template/header.jsp" %>
<%@ include file="localHeader.jsp" %>

<h2><spring:message code="htmlformentry.migrateNamesAndDescriptions" /></h2>

<c:choose>
	<c:when test="${fn:length(migrationNeeded) == 0}">
		Migration is complete!
	</c:when>
	<c:otherwise>
		<div class="boxHeader">
			<spring:message code="htmlformentry.migrate.manualUpgrade" />
		</div>
		<div class="box">
			<form method="post">
				<input type="hidden" name="migration" value="namesAndDescriptions"/>
				<spring:message code="htmlformentry.migrate.differentNameNoLongerSupported" />
				<br/>
				<br/>
				<b><i><spring:message code="htmlformentry.migrate.pickNames" />:</i></b>
				<c:forEach var="htmlForm" items="${migrationNeeded}" varStatus="status">
					<div class='${status.index % 2 == 0 ? "evenRow" : "oddRow"}'>
						<br/>
						For the
						<a href="htmlForm.form?id=${htmlForm.id}" target="preview">
							HTML Form
						</a>
						named <b>${htmlForm.deprecatedName}</b>
						<br/>
						with the underlying
						<a href="<openmrs:contextPath/>/admin/forms/formEdit.form?formId=${htmlForm.form.id}" target="preview">
							Form
						</a>
						named <b>${ htmlForm.form.name } (v${ htmlForm.form.version })</b>
						<br/>

						<c:if test="${htmlForm.deprecatedName != null}">
							<br/>
							<input type="radio" name="name.${htmlForm.id}" value="html"/>
							<spring:message code="htmlformentry.migrate.useHtmlFormName" /> <i>${htmlForm.deprecatedName}</i>
							<br/>
							<input type="radio" name="name.${htmlForm.id}" value="form"/>
							<spring:message code="htmlformentry.migrate.useUnderlyingFormName" /> <i>${htmlForm.form.name}</i>
							<br/>
						</c:if>

						<c:if test="${htmlForm.deprecatedDescription != null}">
							<br/>
							<input type="radio" name="description.${htmlForm.id}" value="html"/>
							<spring:message code="htmlformentry.migrate.useHtmlFormDescription" />:<br/>
							<i>${htmlForm.deprecatedDescription}</i>
							<br/>
							<input type="radio" name="description.${htmlForm.id}" value="form"/>
							<spring:message code="htmlformentry.migrate.useUnderlyingFormDescription" />:<br/>
							<i>${htmlForm.form.description}</i>
						</c:if>
						<br/>
					</div>
				</c:forEach>
				<input type="submit" value='<spring:message code="general.save"/>'/>
			</form>
		</div>
	</c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/template/footer.jsp"%>