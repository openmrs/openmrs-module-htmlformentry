<%@ include file="/WEB-INF/template/include.jsp"%>

<%@ include file="/WEB-INF/template/header.jsp"%>

<openmrs:require privilege="<% OpenmrsConstants.PRIV_MANAGE_FORMS %>" otherwise="/login.htm" redirect="/module/htmlformentry/htmlForm.list" />

<h2><spring:message code="htmlformentry.HtmlForm.edit.title" /></h2>

<table>
	<c:forEach var="htmlForm" items="${command}">
		<tr>
			<td><input type="checkbox" name="id" value="${ htmlForm.id }"/></td>
			<td><a href="htmlForm.form?id=${htmlForm.id}">${htmlForm.name}</a></td>
		</tr>
	</c:forEach>
	<c:if test="${ fn:length(command) == 0 }">
		<spring:message code="general.none" />
	</c:if>
</table>

<br/>
<a href="htmlForm.form"><spring:message code="general.add"/></a>

<%@ include file="/WEB-INF/template/footer.jsp"%>