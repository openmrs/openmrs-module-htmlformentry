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
			You have HTML forms that need Name/Description migration
		</div>
		<div class="box">
			<form method="post">
				<input type="hidden" name="migration" value="namesAndDescriptions"/>
				Before version 1.7 of HTML Form Entry you were allowed to give an HTML Form
				a name and description that differed from those of its underlying Form. This
				is no longer supported.
				<br/>
				<br/>
				<b><i>For each of the following forms, pick which name and description you want
				to use going forwards:</i></b>
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
							Use the HTML Form name <i>${htmlForm.deprecatedName}</i>
							<br/>
							<input type="radio" name="name.${htmlForm.id}" value="form"/>
							Use the underlying Form name <i>${htmlForm.form.name}</i>
							<br/>
						</c:if>

						<c:if test="${htmlForm.deprecatedDescription != null}">
							<br/>
							<input type="radio" name="description.${htmlForm.id}" value="html"/>
							Use the HTML Form description:<br/>
							<i>${htmlForm.deprecatedDescription}</i>
							<br/>
							<input type="radio" name="description.${htmlForm.id}" value="form"/>
							Use the underlying Form description:<br/>
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