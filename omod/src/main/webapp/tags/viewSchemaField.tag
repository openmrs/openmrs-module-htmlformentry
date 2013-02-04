<%@ include file="/WEB-INF/template/include.jsp" %>
<%@ taglib prefix="htmlformentryTag" tagdir="/WEB-INF/tags/module/htmlformentry" %>

<%@ attribute name="field" required="true" type="org.openmrs.module.htmlformentry.schema.HtmlFormField" %>

<c:choose>
	<c:when test="${field['class'].name == 'org.openmrs.module.htmlformentry.schema.ObsGroup'}">
		<div class="box">
			<div class="boxHeader">${field.concept.displayString} (${field.concept.conceptId})</div>
			<c:forEach items="${field.children}" var="child">
				<htmlformentryTag:viewSchemaField field="${child}"/>
			</c:forEach>
		</div>
	</c:when>
	<c:otherwise>
		<table>
			<tr>
				<th>Concept: </th>
				<td>${field.name} -&gt;
					<a style="color:green;" target="_blank" href="${pageContext.request.contextPath}/dictionary/concept.htm?conceptId=${field.question.conceptId}">
						${field.question.displayString} (${field.question.conceptId})
					</a>
				</td>
				<c:choose>
					<c:when test="${fn:length(field.answers) <= 1}">
						<th>Answer:</th>
						<td>
							<c:choose>
								<c:when test="${fn:length(field.answers) == 1}">
									${field.answers[0].displayName} -&gt;
									<a style="color:green;" target="_blank" href="${pageContext.request.contextPath}/dictionary/concept.htm?conceptId=${field.answers[0].concept.conceptId}">
										${field.answers[0].concept.displayString} (${field.answers[0].concept.conceptId})
									</a>
								</c:when>
								<c:otherwise>
									${field.question.datatype.name}
								</c:otherwise>
							</c:choose>
						</td>
					</c:when>
					<c:otherwise>
						<th>Answers:</th>
						<td>
							<c:forEach items="${field.answers}" var="ans">
								${ans.displayName} -&gt;
								<a style="color:green;" target="_blank" href="${pageContext.request.contextPath}/dictionary/concept.htm?conceptId=${ans.concept.conceptId}">
									${ans.concept.displayString} (${ans.concept.conceptId})
								</a>
								<br/>
							</c:forEach>
						</td>
					</c:otherwise>
				</c:choose>
			</tr>
		</table>
	</c:otherwise>
</c:choose>