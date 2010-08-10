<%@ include file="/WEB-INF/template/include.jsp"%>

<%@ include file="/WEB-INF/template/header.jsp"%>

<openmrs:require privilege="Manage Forms" otherwise="/login.htm" redirect="/module/htmlformentry/htmlForm.list" />

<h2><spring:message code="htmlformentry.HtmlForm.edit.title" /></h2>

<openmrs:extensionPoint pointId="org.openmrs.module.htmlformentry.designer">
	<openmrs:portlet url="${extension.portletUrl}" moduleId="${extension.moduleId}" />
</openmrs:extensionPoint>

<spring:hasBindErrors name="command">
	<spring:message code="fix.error"/>
	<div class="error">
		<c:forEach items="${errors.allErrors}" var="error">
			<spring:message code="${error.code}" text="${error.code}"/><br/><!-- ${error} -->
		</c:forEach>
	</div>
	<br />
</spring:hasBindErrors>

<form method="post">
	<table>
		<tr>
			<th align="right"><spring:message code="general.id"/>:</th>
			<td>${command.id}</td>
		</tr>
		<tr>
			<th align="right"><spring:message code="htmlformentry.HtmlForm.form"/>:</th>
			<td>
				<spring:bind path="command.form">
					<select name="${status.expression}">
						<option value=""></option>
						<openmrs:forEachRecord name="form">
							<option value="${record.formId}" <c:if test="${ status.value == record.formId }"> selected="true"</c:if> >
								${record.name} v${record.version} (${record.formId})
							</option>
						</openmrs:forEachRecord>
					</select>
					<c:if test="${status.errorMessage != ''}">
						<span class="error">${status.errorMessage}</span>
					</c:if>
				</spring:bind>
			</td>
		</tr>
		<tr>
			<th align="right"><spring:message code="htmlformentry.HtmlForm.name"/>:</th>
			<td>
				<spring:bind path="command.name">
					<input type="text" name="${status.expression}" value="${status.value}"/>
					<c:if test="${status.errorMessage != ''}">
						<span class="error">${status.errorMessage}</span>
					</c:if>
				</spring:bind>
			</td>
		</tr>
		<tr>
			<th align="right"><spring:message code="general.retired" />:</th>
			<td>
				<spring:bind path="command.retired">
					<input type="hidden" name="_${status.expression}">
					<input type="checkbox" name="${status.expression}" value="true"
						<c:if test="${status.value == true}">checked</c:if> />
					<c:if test="${status.errorMessage != ''}">
						<span class="error">${status.errorMessage}</span>
					</c:if>
				</spring:bind>
			</td>
		</tr>
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
		<tr>
			<th align="right"><spring:message code="Form.formSchema"/>:</th>
			<td>
				<c:if test="${!empty command.xmlData}">
					<a target="_blank" href="htmlFormSchema.form?id=${command.id}"><spring:message code="general.view"/></a>
				</c:if>
			</td>
		</tr>
		<tr>
			<th valign="top" align="right"><spring:message code="htmlformentry.HtmlForm.html"/>:</th>
			<td>
				<spring:bind path="command.xmlData">
					<!-- added id="xmlData" to assure compatibility with HtmlFormDesigner module -->
					<textarea id="xmlData" rows="20" cols="80" name="${status.expression}"><c:out value="${status.value}" escapeXml="true"/></textarea>
					<c:if test="${status.errorMessage != ''}">
						<span class="error">${status.errorMessage}</span>
					</c:if>
				</spring:bind>
			</td>
		</tr>
		<tr>
			<th></th>
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


<%@ include file="/WEB-INF/template/footer.jsp"%>