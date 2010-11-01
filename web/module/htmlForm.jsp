<%@ include file="/WEB-INF/template/include.jsp"%>

<openmrs:require privilege="Manage Forms" otherwise="/login.htm" redirect="/module/htmlformentry/htmlForm.list" />

<c:set var="DO_NOT_INCLUDE_JQUERY" value="true"/>

<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="localHeader.jsp" %>

<openmrs:htmlInclude file="/moduleResources/htmlformentry/jquery-ui-1.8.2.custom.css" />
<openmrs:htmlInclude file="/moduleResources/htmlformentry/jquery-1.4.2.min.js" />
<openmrs:htmlInclude file="/moduleResources/htmlformentry/jquery-ui-1.8.2.custom.min.js" />

<script>
	$j = jQuery.noConflict();
	
	function updateRetiredReasonDisplay(b) {
		if (b)
			$j('#retiredReasonRow').show(0);
		else
			$j('#retiredReasonRow').hide(0);
	}

	$j(document).ready(function() {
		updateRetiredReasonDisplay($j('#retired').checked);
		<c:choose>
			<c:when test="${empty htmlForm.id}">
				$j('.show-later').hide(0);
			</c:when>
			<c:otherwise>
				$j('.show-first').hide(0);
				$j('#toggle-all-fields').click(function() {
					$j('.show-first').toggle(0);
				});
			</c:otherwise>
		</c:choose>
	});
</script>

<h2>
	<c:choose>
		<c:when test="${empty htmlForm.id}">
			<spring:message code="htmlformentry.HtmlForm.create.title" />
		</c:when>
		<c:otherwise>
			<spring:message code="htmlformentry.HtmlForm.edit.title" />
		</c:otherwise>
	</c:choose>
</h2>

<openmrs:extensionPoint pointId="org.openmrs.module.htmlformentry.designer">
	<openmrs:portlet url="${extension.portletUrl}" moduleId="${extension.moduleId}" />
</openmrs:extensionPoint>

<spring:hasBindErrors name="htmlForm">
	<spring:message code="fix.error"/>
	<div class="error">
		<c:forEach items="${errors.allErrors}" var="error">
			<spring:message code="${error.code}" text="${error.code}"/><br/><!-- ${error} -->
		</c:forEach>
	</div>
	<br />
</spring:hasBindErrors>

<c:if test="${not empty htmlForm.id}">
	<a id="toggle-all-fields"><spring:message code="htmlformentry.toggleAllFields"/></a>
</c:if>

<form method="post">
	<table>
		<tr class="show-later">
			<td><spring:message code="general.id"/></td>
			<td>${htmlForm.id}</td>
		</tr>
		<tr>
			<td><spring:message code="general.name"/></td>
			<td>
				<spring:bind path="htmlForm.form.name">
					<input type="text" name="${status.expression}" value="${status.value}" size="35" />
					<c:if test="${status.errorMessage != ''}">
						<span class="error">${status.errorMessage}</span>
					</c:if>
				</spring:bind>
			</td>
		</tr>
		<tr class="show-first">
			<td valign="top"><spring:message code="general.description"/></td>
			<td valign="top">
				<spring:bind path="htmlForm.form.description">
					<textarea name="${status.expression}" rows="3" cols="40" type="_moz">${status.value}</textarea>
					<c:if test="${status.errorMessage != ''}">
						<span class="error">${status.errorMessage}</span>
					</c:if>
				</spring:bind>
			</td>
		</tr>
		<tr class="show-first">
			<td><spring:message code="Form.version"/></td>
			<td>
				<spring:bind path="htmlForm.form.version">
					<input type="text" name="${status.expression}" value="${status.value}" size="5" />
					<c:if test="${status.errorMessage != ''}">
						<span class="error">${status.errorMessage}</span>
					</c:if>
				</spring:bind>
			</td>
		</tr>
		<tr class="show-first">
			<td><spring:message code="Encounter.type"/></td>
			<td>
				<spring:bind path="htmlForm.form.encounterType">
					<select name="encounterType">
						<option value=""></option>
						<c:forEach items="${encounterTypes}" var="type">
							<option value="${type.encounterTypeId}" <c:if test="${type.encounterTypeId == status.value}">selected</c:if>>${type.name}</option>
						</c:forEach>
					</select>
					<c:if test="${status.errorMessage != ''}">
						<span class="error">${status.errorMessage}</span>
					</c:if>
				</spring:bind>
			</td>
		</tr>
		<tr class="show-later">
			<td><spring:message code="Form.published"/></td>
			<td>
				<spring:bind path="htmlForm.form.published">
					<input type="hidden" name="_${status.expression}">
					<input type="checkbox" name="${status.expression}" 
						   id="${status.expression}" 
						   <c:if test="${status.value == true}">checked</c:if> 
					/>
				</spring:bind>
			</td>
		</tr>
		<tr class="show-later">
			<td><spring:message code="general.retired"/></td>
			<td>
				<spring:bind path="htmlForm.form.retired">
					<input type="hidden" name="_${status.expression}">
					<input type="checkbox" name="${status.expression}" 
						   id="retired" 
						   <c:if test="${status.value == true}">checked</c:if>
						   onchange="updateRetiredReasonDisplay(this.checked)"
					/>
				</spring:bind>
				<span id="retiredReasonRow">
					<spring:message code="general.retiredReason"/>
					<spring:bind path="htmlForm.form.retireReason">
						<input type="text" name="${status.expression}" id="retiredReason" value="${status.value}" />
						<c:if test="${status.errorMessage != ''}">
							<span class="error">${status.errorMessage}</span>
						</c:if>
					</spring:bind>
				</span>
			</td>
		</tr>
		<c:if test="${htmlForm.form.retired}">
			<tr class="show-later">
				<td><spring:message code="general.retiredBy"/></td>
				<td>
					${htmlForm.form.retiredBy.personName} -
					<openmrs:formatDate date="${htmlForm.form.dateRetired}" type="long" />
				</td>
			</tr>
		</c:if>
		<tr class="show-later">
			<td><spring:message code="Form.formSchema"/>:</td>
			<td>
				<c:if test="${!empty htmlForm.xmlData}">
					<a target="_blank" href="htmlFormSchema.form?id=${htmlForm.id}"><spring:message code="general.view"/></a>
				</c:if>
			</td>
		</tr>
		<tr class="show-later">
			<td valign="top"><spring:message code="htmlformentry.HtmlForm.html"/>:</td>
			<td>
				<spring:bind path="htmlForm.xmlData">
					<textarea id="xmlData" rows="20" cols="80" name="${status.expression}"><c:out value="${status.value}" escapeXml="true"/></textarea>
					<c:if test="${status.errorMessage != ''}">
						<span class="error">${status.errorMessage}</span>
					</c:if>
				</spring:bind>
			</td>
		</tr>
		<tr>
			<td></td>
			<td>
				<input type="submit" value="<spring:message code='general.save'/>" />
			</td>
		</tr>
	</table>
</form>

<c:if test="${ not empty previewHtml }">
	
	<openmrs:htmlInclude file="/scripts/calendar/calendar.js" />
	<openmrs:htmlInclude file="/moduleResources/htmlformentry/htmlFormEntry.js" />
	<openmrs:htmlInclude file="/moduleResources/htmlformentry/htmlFormEntry.css" />
	<openmrs:htmlInclude file="/moduleResources/htmlformentry/jquery-ui-1.8.2.custom.css" />
	<openmrs:htmlInclude file="/moduleResources/htmlformentry/jquery-ui-1.8.2.custom.min.js" />
	<openmrs:htmlInclude file="/moduleResources/htmlformentry/jquery-1.4.2.min.js" />
	
	<br/>
	<br/>

	<hr/>
	<b><u>Preview</u></b><br/>
	<hr/>
	${ previewHtml }
</c:if>

<%--
		<tr>
			<th align="right"><spring:message code="general.createdBy"/>:</th>
			<td>
				${command.creator} ${command.dateCreated}
			</td>
		</tr>
		<tr>
			<th align="right"><spring:message code="general.changedBy"/>:</th>
			<td>
				${command.changedBy} ${command.dateChanged}
			</td>
		</tr>
--%>

<%@ include file="/WEB-INF/template/footer.jsp"%>