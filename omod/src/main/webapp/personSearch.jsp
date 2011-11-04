<%@ include file="popupHeader.jsp" %>

<body>

<script type='text/javascript'>
var $j = jQuery.noConflict(); 
var ajaxCall;

function callSetParentValue(val, name, prefix)
{
	parent.setValue(val, name, prefix);
    return false;
}

function submitForm()
{
	if(document.getElementById("pSearch").value.length > 3)
	{
		processAjax();
	}
}

function processAjax()
{
	if(ajaxCall != undefined)
	{
		ajaxCall.abort();	
	}
	//show the loading sign
	//$j("#table").hide();
	$j('#loading').show();
	
	//start the ajax
	ajaxCall = $j.ajax({
		
		url: "personResultTable.list",	
		
		//GET method is used
		type: "POST",

		//pass the data			
		data: $j("#patientSearch").serialize(),		
		
		//Do not cache the page
		cache: false,
		
		//success
		success: function (html) {	
			$j("#table").html(html);
			$j("#loading").hide();
		//	$j("#table").show();
		}	
	});
}

$j(document).ready(function() {
	$j('#loading').hide();
});
</script>
			<openmrs:require privilege="View Patients" otherwise="/login.htm" redirect="/index.htm" />
			
			<div id="findPatient">
				<b class="boxHeader"><spring:message code="htmlformentry.personSearch"/></b>
					<form method="post" class="box" action="javascript:processAjax()" name="patientSearch" id="patientSearch">
						<span><spring:message code="htmlformentry.personSearchLabel"/></span>
						<input id="pSearch" name="pSearch" type="text" value="" style="width: 25em;" onkeyup="submitForm()">
						<c:if test="${not empty param.searchAttribute}">
							<input id="pAttribute" name="pAttribute" type="hidden" value="${param.searchAttribute}">
						</c:if>	
						<c:if test="${not empty param.attributeValue}">
							<input id="pAttributeValue" name="pAttributeValue" type="hidden" value="${param.attributeValue}">
						</c:if>	
						<c:if test="${not empty param.searchProgram}">
							<input id="pProgram" name="pProgram" type="hidden" value="${param.searchProgram}">
						</c:if>		
						<input id="prefix" name="prefix" type="hidden" value="${param.prefix}">
						<div class="loading" id="loading">loading...</div>
						<br></br>
						<div id="table">
				
					</div>	
				</div>
			</div>	
			</form>
	