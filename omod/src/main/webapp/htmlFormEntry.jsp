<%@ include file="/WEB-INF/template/include.jsp" %>

<c:set var="OPENMRS_DO_NOT_SHOW_PATIENT_SET" scope="request" value="true"/>
<c:set var="pageFragment" value="${param.pageFragment != null && param.pageFragment}"/>
<c:set var="inPopup" value="${pageFragment || (param.inPopup != null && param.inPopup)}"/>

<c:if test="${not pageFragment}">
    <c:set var="DO_NOT_INCLUDE_JQUERY" value="true"/>
	<c:choose>
		<c:when test="${inPopup}">
			<%@ include file="/WEB-INF/template/headerMinimal.jsp" %>
		</c:when>
		<c:otherwise>
			<%@ include file="/WEB-INF/template/header.jsp" %>
		</c:otherwise>
	</c:choose>

	<openmrs:htmlInclude file="/dwr/engine.js" />
	<openmrs:htmlInclude file="/dwr/util.js" />
	<openmrs:htmlInclude file="/dwr/interface/DWRHtmlFormEntryService.js" />
	<openmrs:htmlInclude file="/moduleResources/htmlformentry/htmlFormEntry.js" />
	<openmrs:htmlInclude file="/moduleResources/htmlformentry/htmlFormEntry.css" />
	<openmrs:htmlInclude file="/moduleResources/htmlformentry/jquery-ui-1.8.17.custom.css" />
	<openmrs:htmlInclude file="/moduleResources/htmlformentry/jquery-1.4.2.min.js" />
    <script type="text/javascript">
        $j = jQuery.noConflict();
    </script>
	<openmrs:htmlInclude file="/moduleResources/htmlformentry/jquery-ui-1.8.17.custom.min.js" />
</c:if>

<script type="text/javascript">
	var propertyAccessorInfo = new Array();
	
	// individual forms can define their own functions to execute before a form validation or submission by adding them to these lists
	// if any function returns false, no further functions are called and the validation or submission is cancelled
	var beforeValidation = new Array();     // a list of functions that will be executed before the validation of a form
	var beforeSubmit = new Array(); 

	// a list of functions that will be executed before the submission of a form
	$j(document).ready(function() {
		$j('#deleteButton').click(function() {
			
			// display a "deleting form" message
			$j('#confirmDeleteFormPopup').children("center").html('<spring:message code="htmlformentry.deletingForm"/>');
			
			// do the post that does the actual delete
			$j.post("<c:url value="/module/htmlformentry/deleteEncounter.form"/>", 
				{ 	encounterId: "${command.encounter.encounterId}", 
				    htmlFormId: "${command.htmlFormId}",
					returnUrl: "${command.returnUrlWithParameters}", 
					reason: $j('#deleteReason').val()
			 	}, 
			 	function(data) {
				 	var url = "${command.returnUrlWithParameters}";
				 	if (url == null || url == "") {
					 	url = "${pageContext.request.contextPath}/patientDashboard.form?patientId=${command.patient.patientId}";
				 	}
				 	window.parent.location.href = url;
			 	}
			 );
		});
		$j(':button.addConceptButton').click(function() {
			  var string=(this.id).replace("_button","");
		        var conceptValue=$j('#'+string+'_hid').attr('value')
		        if($j('#'+string).css('color')=='green'){
		        var	divId=string+"_div";
			 var clickNum = $j(this).data('clickNum');
			 if (!clickNum) {
				 clickNum = 0;
				 $j('#'+divId).data("count",clickNum);
				 $j(this).data('clickNum',++clickNum);
			 }
	        	 var spanid=string+'span_'+ $j('#'+divId).data("count");
	        	 var count= $j('#'+divId).data("count");
	        	 $j('#'+divId).data("count",++count);
	        	 $j('#'+string+'_hid').attr('value',$j('#'+divId).data("count"));
	        	 var hidId=spanid+'_hid';
	           var v='<span id="'+spanid+'"></br>'+$j('#'+string).val()+'<input id="'+hidId+'"  class="autoCompleteHidden" type="hidden" name="'+hidId+'" value="'+conceptValue+'">';
	                var q='<input id="'+spanid+'_button" type="button" value="remove" onClick="$j(\'#'+spanid+'\').remove();refresh()"></span>';
	                $j('#'+divId).append(v+q);
	                $j('#'+string).val('');
	        } 
	        });
	        

	});
	function refresh () {
		var flag=true;
		var string=((this.id).split("span",1))+"_hid";
		var divId=((this.id).split("span",1))+"_div";
		$j('#'+divId+' span').each(function(index) {
			$j('#'+divId).data("count",index+1);
	     	 $('#'+string).attr('value',$j('#'+divId).data("count"));
			flag=false;
		     var spanId=this.id;
		     var newSpanId=spanId.split('_',1)+'_'+index;
		     this.id=newSpanId;
		     $j('#'+spanId+'_hid').attr('id',newSpanId+'_hid');
		      $j('#'+spanId+'_button').removeAttr('onclick',null).unbind('click').attr('id',newSpanId+'_button').click(function() {
		 $j('#'+newSpanId).remove();refresh();
		 });
		     });
		if(flag)
		$j('#'+divId).data("count",0);
    	 $j('#'+string).attr('value',$j('#'+divId).data("count"));
		}
	var tryingToSubmit = false;
	
	function submitHtmlForm() {
	    if (!tryingToSubmit) {
	        tryingToSubmit = true;
	        DWRHtmlFormEntryService.checkIfLoggedIn(checkIfLoggedInAndErrorsCallback);
	    }
	}

	function findAndHighlightErrors(){
		/* see if there are error fields */
		var containError = false
		var ary = $j(".autoCompleteHidden");
		$j.each(ary,function(index, value){
			if(value.value == "ERROR"){
				if(!containError){
					alert("<spring:message code='htmlformentry.error.autoCompleteAnswerNotValid'/>");
					var id = value.id;
					id = id.substring(0,id.length-4);
					$j("#"+id).focus(); 					
				}
				containError=true;
			}
		});
		return containError;
	}

	/*
		It seems the logic of  showAuthenticateDialog and 
		findAndHighlightErrors should be in the same callback function.
		i.e. only authenticated user can see the error msg of
	*/
	function checkIfLoggedInAndErrorsCallback(isLoggedIn) {
		
		var state_beforeValidation=true;
		
		if (!isLoggedIn) {
			showAuthenticateDialog();
		}else{
			
			// first call any beforeValidation functions that may have been defined by the html form
			if (beforeValidation.length > 0){
				for (var i=0, l = beforeValidation.length; i < l; i++){
					if (state_beforeValidation){
						var fncn=beforeValidation[i];						
						state_beforeValidation=eval(fncn);
					}
					else{
						// forces the end of the loop
						i=l;
					}
				}
			}
			
			// only do the validation if all the beforeValidationk functions returned "true"
			if (state_beforeValidation){
				var anyErrors = findAndHighlightErrors();
			
        		if (anyErrors) {
            		tryingToSubmit = false;
            		return;
        		}else{
        			doSubmitHtmlForm();
        		}
			}
		}
	}

	function showAuthenticateDialog() {
		$j('#passwordPopup').show();
		tryingToSubmit = false;
	}

	function loginThenSubmitHtmlForm() {
		
		$j('#passwordPopup').hide();
		var username = $j('#passwordPopupUsername').val();
		var password = $j('#passwordPopupPassword').val();
		$j('#passwordPopupUsername').val('');
		$j('#passwordPopupPassword').val('');
		DWRHtmlFormEntryService.authenticate(username, password, submitHtmlForm); 
	}

	function doSubmitHtmlForm() {
		
		// first call any beforeSubmit functions that may have been defined by the form
		var state_beforeSubmit=true;
		if (beforeSubmit.length > 0){
			for (var i=0, l = beforeSubmit.length; i < l; i++){
				if (state_beforeSubmit){
					var fncn=beforeSubmit[i];						
					state_beforeSubmit=fncn();					
				}
				else{
					// forces the end of the loop
					i=l;
				}
			}
		}
		
		// only do the submit if all the beforeSubmit functions returned "true"
		if (state_beforeSubmit){
			var form = document.getElementById('htmlform');
			form.submit();			
		}
		tryingToSubmit = false;
	}

	function handleDeleteButton() {
		$j('#confirmDeleteFormPopup').show();
	}

	function cancelDeleteForm() {
		$j('#confirmDeleteFormPopup').hide();
	}
</script>

<div id="htmlFormEntryBanner">
	<spring:message var="backMessage" code="htmlformentry.goBack"/>
	<c:if test="${!inPopup && (command.context.mode == 'ENTER' || command.context.mode == 'EDIT')}">
		<spring:message var="backMessage" code="htmlformentry.discard"/>
	</c:if>
	<div style="float: left" id="discardAndPrintDiv">
		<c:if test="${!inPopup}">
			<span id="discardLinkSpan"><a href="<c:choose><c:when test="${not empty command.returnUrlWithParameters}">${command.returnUrlWithParameters}</c:when><c:otherwise>${pageContext.request.contextPath}/patientDashboard.form?patientId=${command.patient.patientId}</c:otherwise></c:choose>">${backMessage}</a></span> | 
		</c:if>
		<span id="printLinkSpan"><a href="javascript:window.print();"><spring:message code="htmlformentry.print"/></a></span> &nbsp;<br/>
	</div>
	<div style="float:right">
		<c:if test="${command.context.mode == 'VIEW'}">
			<c:if test="${!inPopup}">
				<openmrs:hasPrivilege privilege="Edit Encounters,Edit Observations">
					<c:url var="editUrl" value="/module/htmlformentry/htmlFormEntry.form">
						<c:forEach var="p" items="${param}">
							<c:if test="${p.key != 'mode'}">
								<c:param name="${p.key}" value="${p.value}"/>
							</c:if>
						</c:forEach>
						<c:param name="mode" value="EDIT"/>
					</c:url>
					<a href="${editUrl}"><spring:message code="general.edit"/></a> |
				</openmrs:hasPrivilege>
			</c:if>
			<openmrs:hasPrivilege privilege="Delete Encounters,Delete Observations">
				<a onClick="handleDeleteButton()"><spring:message code="general.delete"/></a>
				<div id="confirmDeleteFormPopup" style="position: absolute; z-axis: 1; right: 0px; background-color: #ffff00; border: 2px black solid; display: none; padding: 10px">
					<center>
						<spring:message code="htmlformentry.deleteReason"/>
						<br/>
						<textarea name="reason" id="deleteReason"></textarea>
						<br/><br/>
						<input type="button" value="<spring:message code="general.cancel"/>" onClick="cancelDeleteForm()"/>
						&nbsp;&nbsp;&nbsp;&nbsp;
						<input type="button" value="<spring:message code="general.delete"/>" id="deleteButton"/>
					</center>
				</div>
			</openmrs:hasPrivilege>
		</c:if>
	</div>
	<c:if test="${!inPopup}">
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
	</c:if>
</div>

<c:if test="${command.context.mode != 'VIEW'}">
	<spring:hasBindErrors name="command">
		<spring:message code="fix.error"/>
		<div class="error">
			<c:forEach items="${errors.allErrors}" var="error">
				<spring:message code="${error.code}" text="${error.code}"/><br/>
			</c:forEach>
		</div>
		<br />
	</spring:hasBindErrors>
</c:if>

<c:if test="${command.context.mode != 'VIEW'}">
	<form id="htmlform" method="post" onSubmit="submitHtmlForm(); return false;">
		<input type="hidden" name="personId" value="${ command.patient.personId }"/>
		<input type="hidden" name="htmlFormId" value="${ command.htmlFormId }"/>
		<input type="hidden" name="formModifiedTimestamp" value="${ command.formModifiedTimestamp }"/>
		<input type="hidden" name="encounterModifiedTimestamp" value="${ command.encounterModifiedTimestamp }"/>
		<c:if test="${ not empty command.encounter }">
			<input type="hidden" name="encounterId" value="${ command.encounter.encounterId }"/>
		</c:if>
		<input type="hidden" name="closeAfterSubmission" value="${param.closeAfterSubmission}"/>
</c:if>

<c:if test="${command.context.guessingInd == 'true'}">
	<div class="error">
		<spring:message code="htmlformentry.form.reconstruct.warning" />
	</div>
</c:if>
	
	${command.htmlToDisplay}
	
<c:if test="${command.context.mode != 'VIEW'}">
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
</c:if>

<c:if test="${not empty command.fieldAccessorJavascript}">
	<script type="text/javascript">
		${command.fieldAccessorJavascript}
	</script>
</c:if>
<c:if test="${not empty command.setLastSubmissionFieldsJavascript || not empty command.lastSubmissionErrorJavascript}"> 
	<script type="text/javascript">
		$j(document).ready( function() {
			${command.setLastSubmissionFieldsJavascript}
			${command.lastSubmissionErrorJavascript}
		});
	</script>
</c:if>

<c:if test="${!pageFragment}">
	<c:choose>
		<c:when test="${inPopup}">
			<%@ include file="/WEB-INF/template/footerMinimal.jsp" %>
		</c:when>
		<c:otherwise>
			<%@ include file="/WEB-INF/template/footer.jsp" %>
		</c:otherwise>
	</c:choose>
</c:if>