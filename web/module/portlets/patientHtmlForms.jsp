<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:hasPrivilege privilege="Form Entry">

<script type="text/javascript">
	
</script>

<table><tr valign="top"><td>

	<center><u><spring:message code="htmlformentry.patientDashboard.enterForm"/></u></center>
	<ol>
		<c:forEach var="hf" items="${ model.htmlForms }">
			<li>
				<a href="${ pageContext.request.contextPath }/module/htmlformentry/htmlFormEntry.form?personId=${ model.personId }&htmlFormId=${ hf.id }">
					${ hf.name }
				</a>
			</li>
		</c:forEach>
	</ol>

</td><td width="15px" style="border-right: 1px black solid">
</td><td style="padding-left: 15px">

	<center><u><spring:message code="htmlformentry.patientDashboard.existingForms"/></u></center><br/>
	<table>
		<tr>
			<th><spring:message code="Encounter.form"/></th>
			<th><spring:message code="Encounter.datetime"/></th>
			<th><spring:message code="Encounter.provider"/></th>
			<th><spring:message code="Encounter.location"/></th>
			<th><spring:message code="Encounter.enterer"/></th>
		</tr>	
		<openmrs:forEachEncounter encounters="${model.patientEncounters}" sortBy="encounterDatetime" descending="true" var="enc">
			<c:set var="htmlForm" value=""/>
			<c:forEach var="hf" items="${ model.htmlForms }">
				<c:if test="${ hf.form == enc.form }">
					<c:set var="htmlForm" value="${ hf }"/>
				</c:if>
			</c:forEach>

			<c:if test="${ not empty htmlForm }">
				<tr>
					<td>
						<a href="${ pageContext.request.contextPath }/module/htmlformentry/htmlFormEntry.form?mode=VIEW&personId=${ model.personId }&htmlFormId=${ htmlForm.id }&encounterId=${ enc.encounterId }">
							${ htmlForm.name }
						</a>
					</td>
					<td align="center">
						<openmrs:formatDate date="${enc.encounterDatetime}" type="small" />
					</td>
				 	<td align="center">${enc.provider.personName}</td>
				 	<td align="center">${enc.location.name}</td>
				 	<td align="center">${enc.creator.personName}</td>
				</tr>
			</c:if>		
		</openmrs:forEachEncounter>
	</table>

</td></tr></table>

</openmrs:hasPrivilege>