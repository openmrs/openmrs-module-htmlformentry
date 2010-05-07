<%@ include file="/WEB-INF/template/include.jsp"%>

<%@ include file="/WEB-INF/template/header.jsp"%>

<style>
	tr,th {text-align:left; vertical-align:top;}
</style>

<openmrs:require privilege="Manage Forms" otherwise="/login.htm" redirect="/module/htmlformentry/htmlForm.list" />

<c:if test="${ not empty message }">
	<span style="color:red;">${message}</span>
</c:if>
<c:if test="${ not empty schema }">
	<a href="htmlFormFromFile.form?filePath=${filePath}">Preview Form</a>
	<br/>
	<h3>Form Schema Preview</h3>
	<b>${schema.name}</b>
	
	<c:forEach items="${schema.sections}" var="section">
		<div class="box">
			<div class="boxHeader" style="background-color:yellow; color:black; font-weight:bold;">${section.name}</div>
			<c:forEach items="${section.fields}" var="field">
				<c:choose>
					<c:when test="${field.class.name == 'org.openmrs.module.htmlformentry.schema.ObsGroup'}">
						<div class="box">
							<div class="boxHeader">${field.concept.displayString} (${field.concept.conceptId})</div>
							<c:forEach items="${field.children}" var="child">
								<table>
									<tr>
										<th>Concept:</th> 
										<td>
											${child.name} -&gt; 
											<a style="color:green;" target="_blank" href="${pageContext.request.contextPath}/dictionary/concept.htm?conceptId=${child.question.conceptId}">
												${child.question.displayString} (${child.question.conceptId})
											</a>
										</td>
										<c:choose>
											<c:when test="${fn:length(child.answers) <= 1}">
												<th>Answer:</th>
												<td>
													<c:choose>
														<c:when test="${fn:length(child.answers) == 1}">
															${child.answers[0].displayName} -&gt; 
															<a style="color:green;" target="_blank" href="${pageContext.request.contextPath}/dictionary/concept.htm?conceptId=${child.answers[0].concept.conceptId}">
																${child.answers[0].concept.displayString} (${child.answers[0].concept.conceptId})
															</a>
														</c:when>
														<c:otherwise>
															${child.question.datatype.name}
														</c:otherwise>
													</c:choose>
												</td>
											</c:when>
											<c:otherwise>
												<th>Answers:</th>
												<td>
													<c:forEach items="${child.answers}" var="ans">
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
				<br/>
			</c:forEach>
		</div>
	</c:forEach>
</c:if>

<%@ include file="/WEB-INF/template/footer.jsp"%>