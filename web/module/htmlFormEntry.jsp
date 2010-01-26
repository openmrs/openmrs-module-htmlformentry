<%@ include file="/WEB-INF/template/include.jsp" %>

<c:set var="OPENMRS_DO_NOT_SHOW_PATIENT_SET" scope="request" value="true"/>
<c:set var="inPopup" value="${param.inPopup != null && param.inPopup}"/>

<c:choose>
	<c:when test="${inPopup}">
		<%@ include file="/WEB-INF/template/headerMinimal.jsp" %>
	</c:when>
	<c:otherwise>
		<%@ include file="/WEB-INF/template/header.jsp" %>
	</c:otherwise>
</c:choose>

<openmrs:htmlInclude file="/scripts/calendar/calendar.js" />
<openmrs:htmlInclude file="/dwr/engine.js" />
<openmrs:htmlInclude file="/dwr/util.js" />
<openmrs:htmlInclude file="/dwr/interface/DWRHtmlFormEntryService.js" />
<openmrs:htmlInclude file="/moduleResources/htmlformentry/htmlFormEntry.js" />
<openmrs:htmlInclude file="/moduleResources/htmlformentry/htmlFormEntry.css" />
<openmrs:htmlInclude file="/scripts/dojoConfig.js"/>
<openmrs:htmlInclude file="/scripts/dojo/dojo.js"/>

<script type="text/javascript">
	var tryingToSubmit = false;
	
	function submitHtmlForm() {
		if (!tryingToSubmit) {
			tryingToSubmit = true;
			DWRHtmlFormEntryService.checkIfLoggedIn(checkIfLoggedInCallback);
		}
	}

	function checkIfLoggedInCallback(isLoggedIn) {
		if (isLoggedIn) {
			doSubmitHtmlForm();
		} else {
			showAuthenticateDialog();
		}
	}

	function showAuthenticateDialog() {
		showDiv('passwordPopup');
		tryingToSubmit = false;
	}

	function loginThenSubmitHtmlForm() {
		hideDiv('passwordPopup');
		var username = DWRUtil.getValue('passwordPopupUsername');
		var password = DWRUtil.getValue('passwordPopupPassword');
		DWRUtil.setValue('passwordPopupUsername', '');
		DWRUtil.setValue('passwordPopupPassword', '');
		DWRHtmlFormEntryService.authenticate(username, password, submitHtmlForm); 
	}

	function doSubmitHtmlForm() {
		var form = document.getElementById('htmlform');
		form.submit();
		tryingToSubmit = false;
	}

	function handleDeleteButton() {
		showDiv('confirmDeleteFormPopup');
	}

	function cancelDeleteForm() {
		hideDiv('confirmDeleteFormPopup');
	}
</script>

<c:if test="${!inPopup}">
	<div id="htmlFormEntryBanner">
		<spring:message var="backMessage" code="htmlformentry.goBack"/>
		<c:if test="${command.context.mode == 'ENTER' || command.context.mode == 'EDIT'}">
			<spring:message var="backMessage" code="htmlformentry.discard"/>
		</c:if>
		<div style="float: left">
			<a href="<c:choose><c:when test="${not empty command.returnUrlWithParameters}">${command.returnUrlWithParameters}</c:when><c:otherwise>javascript:back();</c:otherwise></c:choose>">${backMessage}</a>
			| <a href= "javascript:window.print();"><spring:message code="htmlformentry.print"/></a><br/>
		</div>
		<div style="float:right">
			<c:if test="${command.context.mode == 'VIEW'}">
				<c:url var="editUrl" value="htmlFormEntry.form">
					<c:forEach var="p" items="${param}">
						<c:if test="${p.key != 'mode'}">
							<c:param name="${p.key}" value="${p.value}"/>
						</c:if>
					</c:forEach>
					<c:param name="mode" value="EDIT"/>
				</c:url>
				<a href="${editUrl}"><spring:message code="general.edit"/></a>
				|
				<a onClick="handleDeleteButton()"><spring:message code="general.delete"/></a>
				<div id="confirmDeleteFormPopup" style="position: absolute; z-axis: 1; right: 0px; background-color: #ffff00; border: 2px black solid; display: none; padding: 10px">
					<form method="post" action="deleteEncounter.form">
						<input type="hidden" name="encounterId" value="${command.encounter.encounterId}"/>
						<input type="hidden" name="returnUrl" value="${command.returnUrlWithParameters}"/>
						<center>
							<spring:message code="htmlformentry.deleteReason"/>
							<br/>
							<textarea name="reason"></textarea>
							<br/><br/>
							<input type="button" value="<spring:message code="general.cancel"/>" onClick="cancelDeleteForm()"/>
							&nbsp;&nbsp;&nbsp;&nbsp;
							<input type="submit" value="<spring:message code="general.delete"/>"/>
						</center>	
				</div> 
			</c:if>
		</div>
		<b>
			${command.patient.personName} |
			<c:choose>
				<c:when test="${not empty command.form}">
					${command.form.name} (${command.form.encounterType.name})
				</c:when>
				<c:otherwise>
					<c:if test="${not empty command.encounter}">
						${command.encounter.form.name} (${command.encounter.encounterType.name})
					</c:if>
				</c:otherwise> 
			</c:choose>
			
			|
			<c:if test="${not empty command.encounter}">
				<openmrs:formatDate date="${command.encounter.encounterDatetime}"/> | ${command.encounter.location.name} 
			</c:if>
			<c:if test="${empty command.encounter}">
				<spring:message code="htmlformentry.newForm"/>
			</c:if>
		</b>
	</div>
</c:if>

<spring:hasBindErrors name="command">
	<spring:message code="fix.error"/>
	<div class="error">
		<c:forEach items="${errors.allErrors}" var="error">
			<spring:message code="${error.code}" text="${error.code}"/><br/>
		</c:forEach>
	</div>
	<br />
</spring:hasBindErrors>

<form id="htmlform" method="post">
	<input type="hidden" name="personId" value="${ command.patient.personId }"/>
	<input type="hidden" name="htmlFormId" value="${ command.htmlFormId }"/>
	<input type="hidden" name="formModifiedTimestamp" value="${ command.formModifiedTimestamp }"/>
	<input type="hidden" name="encounterModifiedTimestamp" value="${ command.encounterModifiedTimestamp }"/>
	<c:if test="${ not empty command.encounter }">
		<input type="hidden" name="encounterId" value="${ command.encounter.encounterId }"/>
	</c:if>
	<input type="hidden" name="closeAfterSubmission" value="${param.closeAfterSubmission}"/>
	
	${command.htmlToDisplay}
	
	<div id="passwordPopup" style="position: absolute; z-axis: 1; bottom: 25px; background-color: #ffff00; border: 2px black solid; display: none; padding: 10px">
		<center>
			<table>
				<tr>
					<td colspan="2"><b><spring:message code="htmlformentry.loginAgainMessage"/></b></td>
				</tr>
				<tr>
					<td align="right"><b>Username:</b></td>
					<td><input type="text" id="passwordPopupUsername"/></td>
				</tr>
				<tr>
					<td align="right"><b>Password:</b></td>
					<td><input type="password" id="passwordPopupPassword"/></td>
				</tr>
				<tr>
					<td colspan="2" align="center"><input type="button" value="Submit" onClick="loginThenSubmitHtmlForm()"/></td>
				</tr>
			</table>
		</center>
	</div>
</form>

<script type="text/javascript">
	dojo.addOnLoad( function() {
		${command.setLastSubmissionFieldsJavascript}
		${command.lastSubmissionErrorJavascript}
	});
</script>

<c:choose>
	<c:when test="${inPopup}">
		<%@ include file="/WEB-INF/template/footerMinimal.jsp" %>
	</c:when>
	<c:otherwise>
		<%@ include file="/WEB-INF/template/footer.jsp" %>
	</c:otherwise>
</c:choose>
