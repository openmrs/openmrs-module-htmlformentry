<%@ include file="/WEB-INF/template/include.jsp"%>

<%@ include file="/WEB-INF/template/header.jsp"%>

<openmrs:require privilege="Manage Forms" otherwise="/login.htm" redirect="/module/htmlformentry/htmlForm.list" />

<h2><spring:message code="htmlformentry.HtmlForm.edit.title" /></h2>

<table>
	<c:forEach var="htmlForm" items="${command}">
		<tr>
		<!-- commenting these checkboxes out until we decide to use them -->
		<!-- <td><input type="checkbox" name="id" value="${ htmlForm.id }"/></td> -->
			<td><a href="htmlForm.form?id=${htmlForm.id}"><c:out value="${htmlForm.name}"/></a></td>
		</tr>
	</c:forEach>
	<c:if test="${ fn:length(command) == 0 }">
		<spring:message code="general.none" />
	</c:if>
</table>

<br/>
<a href="htmlForm.form"><spring:message code="general.add"/></a>

<%@ include file="/WEB-INF/template/footer.jsp"%>