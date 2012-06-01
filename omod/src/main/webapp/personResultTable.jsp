<%@ include file="/WEB-INF/template/include.jsp" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<script>
var $j = jQuery.noConflict();
$j('#personTable').load('load', function() {
	var dataTable = $j('#personTable').dataTable({
		"sPaginationType": "two_button",
		"bAutoWidth": true,
		"bFilter": false,
		"aaSorting": [[2,'asc']],
		"iDisplayLength": 15,
		"aoColumns": [
        	null,
        	null,
        	null,
        	null,
        	null,
        	null,
        	null
    	],
		"oLanguage": {
				"sLengthMenu": 'Show <select><option value="15">15</option><option value="30">30</option><option value="50">50</option><option value="100">100</option></select> entries',
				"sZeroRecords": '<spring:message code="htmlformentry.nopatientsfound"/>'
		}
	} );
	dataTable.fnDraw();
});

</script>
	
		<table cellspacing="0" cellpadding="2" id="personTable" style="width: 100%;">
					<thead>
						<tr>
							<th><spring:message code="Patient.identifier"/></th>
							<th><spring:message code="PersonName.givenName"/></th>
							<th><spring:message code="PersonName.familyName"/></th>
							<th><spring:message code="Person.age"/></th>
							<th><spring:message code="Person.gender"/></th>
							<th></th>
							<th><spring:message code="Person.birthdate"/></th>
						</tr>
					</thead>
					<tbody id="rows">
					
		<c:forEach var="collection" items="${people}" varStatus="rowStatus">
			
			<tr class="<c:choose><c:when test="${rowStatus.index % 2 == 0}">evenRow</c:when><c:otherwise>oddRow</c:otherwise></c:choose>">
				<c:set var="fullName" value="${collection.givenName} ${collection.familyName}" />  
				<td>
				<c:if test="${collection['class'].name eq 'org.openmrs.web.dwr.PatientListItem'}">
				<c:out value="${collection.identifier}"/>
				</c:if>
				</td>
				<td><a href='javascript:void(0)' onClick='callSetParentValue("${collection.personId}", "${fullName}", "${param.prefix}")'><c:out value="${collection.givenName}"/></a></td>
				<td><a href='javascript:void(0)' onClick='callSetParentValue("${collection.personId}", "${fullName}", "${param.prefix}")'><c:out value="${collection.familyName}"/></a></td>
				<td class="personAge">
				<c:choose><c:when test="${collection.age == 0}">&lt;1</c:when>
					<c:otherwise>
				<c:out value="${collection.age}"/>
					</c:otherwise>
				</c:choose></td>
				<td class="personGender">
					<c:if test="${collection.gender eq 'F'}"><img src="/openmrs/images/female.gif"></c:if>
					<c:if test="${collection.gender eq 'M'}"><img src="/openmrs/images/male.gif"></c:if>
				</td>
				<td>
					<c:if test="${collection.birthdateEstimated}">&asymp;</c:if>
				</td>		
				<td><fmt:formatDate value="${collection.birthdate}" pattern="dd/MM/yyyy"/></td>
			</tr>
		</c:forEach>
		</tbody>
					</table>
