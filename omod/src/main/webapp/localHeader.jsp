<ul id="menu">
	<li class="first">
		<a href="${pageContext.request.contextPath}/admin"><spring:message code="admin.title.short"/></a>
	</li>
	<li <c:if test='<%= request.getRequestURI().contains("htmlForms") %>'>class="active"</c:if>>
		<a href="${pageContext.request.contextPath}/module/htmlformentry/htmlForms.list">
			<spring:message code="htmlformentry.manage"/>
		</a>
	</li>
	<li <c:if test='<%= request.getRequestURI().contains("htmlFormFromFile") %>'>class="active"</c:if>>
		<a href="${pageContext.request.contextPath}/module/htmlformentry/htmlFormFromFile.form">
			<spring:message code="htmlformentry.preview"/>
		</a>
	</li>
</ul>