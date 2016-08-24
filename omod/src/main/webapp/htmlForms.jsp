<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Forms" otherwise="/login.htm" redirect="/admin/forms/form.list" />

<%@ include file="/WEB-INF/template/header.jsp" %>
<%@ include file="localHeader.jsp" %>

<h2><spring:message code="htmlformentry.manage" /></h2>

<a href="htmlForm.form"><spring:message code="htmlformentry.manage.add"/></a>

<br /><br />

<div class="boxHeader">
    <span style="float: right">
        <a href="#" id="showRetired" onClick="return toggleRowVisibilityForClass('formTable', 'voided');"><spring:message code="general.toggle.retired"/></a>
    </span>
	<b><spring:message code="htmlformentry.manage.header" /></b>
</div>

<div class="box">
	<table cellpadding="2" cellspacing="0" id="formTable" width="98%">
		<tr>
			<th> <spring:message code="general.name" /> </th>
			<th> <spring:message code="Form.version" /> </th>
			<th> <spring:message code="general.description" /> </th>
			<th> <spring:message code="Form.published" /> </th>
		</tr>
		<c:forEach var="form" items="${forms}" varStatus="status">
			<tr class='${status.index % 2 == 0 ? "evenRow" : "oddRow"} ${form.form.retired ? "voided" : ""}'>
				<td valign="top" style="white-space: nowrap"><a href="htmlForm.form?id=${form.id}"><c:out value="${form.form.name}"/></a></td>
				<td valign="top"><c:out value="${form.form.version}"/></td>
				<td valign="top"><c:out value="${form.form.description}"/></td>
				<td valign="top"><c:if test="${form.form.published == true}"><spring:message code="general.yes"/></c:if></td>
			</tr>
		</c:forEach>
	</table>
</div>
<script type="text/javascript">
	toggleRowVisibilityForClass("formTable", "voided");
</script>

<%@ include file="/WEB-INF/template/footer.jsp"%>