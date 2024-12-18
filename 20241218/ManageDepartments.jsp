<%@page import="com.ltts.llc.dps.eserv.reports.util.*,java.sql.*,java.text.SimpleDateFormat,org.hibernate.criterion.*,org.hibernate.*,org.hibernate.Query,java.util.*,java.io.*"%>
<%@ page session="false"  import="com.ltts.llc.dps.eserv.europa.log.*, com.ltts.llc.dps.eserv.europa.session.SessionManager" %>
<%@ page import="com.ltts.llc.dps.eserv.perms.*,com.ltts.llc.dps.eserv.module.*" %>
<%@ page buffer="64kb"%>

<%@page import="com.ltts.llc.dps.eserv.europa.security.SecurityManager,com.ltts.llc.dps.eserv.perms.UserPermDisplayLogic"%>
<html>
   		<head>
    	<meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
	   	<META HTTP-EQUIV="Cache-Control" CONTENT="no-cache">
		<META HTTP-EQUIV="Pragma" CONTENT="no-cache">
		<META HTTP-EQUIV="Expires" CONTENT="0">
    	<title>Manage Locations (Revenue Center)</title>
<script type="text/javascript" src="/assets/js/SessionTimeOut.js"></script>
		<!-- Time Out Continue feature Start -->
	<script>
		var alerttimelimit = timeoutAlertDuration();
		setTimeout('sessionWarning()',alerttimelimit);
		function sessionWarning() {
		    var sFeatures="dialogHeight: " + 150 + "px; scroll=No";
	   		var myObject = new Object();
	   		myObject.CurrentsessionID = '<FIELD="FWSession">';
	        var retValue = window.showModalDialog("/jsp/RestoreSession.jsp",myObject,sFeatures);
	        if(retValue == 'tmedout'){
	        	document.ManageDepartment.action = "/servlets/ProxyServlet";
	        	document.ManageDepartment.submit();
		     }
	        setTimeout('sessionWarning()', alerttimelimit);
		}
	</script>
<!-- Time Out Continue feature End -->
		<STYLE type="text/css">

		table.contacts{ width: 840px;background-color: #fafafa;	border: 1px #000000 solid;border-spacing: 0.5px; font-family: Tahoma;}

		td.contactDept{ background-color: #lightgrey;border: 0.5px #000000 solid;font-family: Tahoma;font-weight: bolder;
				font-size: 14px;color: #000000;font-stretch: extra-condensed;Letter-spacing :1px; border: 1px solid #808080;}

		td.contact0{ border-bottom: 1px #C0C0C0 solid;text-align: left;	font-family: Tahoma;
				font-weight: normal;font-size: 10pt;padding-top: 4px;
				padding-bottom: 4px;padding-left: 8px;padding-right: 0px; background-color: #FFFFFF;border: 1px solid #808080;}
		td.contact1{ border-bottom: 1px #C0C0C0 solid;text-align: left;	font-family: Tahoma;
				font-weight: normal;font-size: 10pt;padding-top: 4px;
				padding-bottom: 4px;padding-left: 8px;padding-right: 0px; background-color: #fafafa;border-spacing: 0.5px;border: 1px solid #808080;}

		.style1 {font-family: Tahoma; font-size: 10pt; border: 1px solid #808080}

		.blankhead{background-color:DCDCDC;color: #DC143C;font-size: 10pt;}

		 </STYLE>

    	<script type="text/javascript">
    	var allAvailBillHours = new Array();
    	function loadAvailBillHours(){
        	var depArray=new Array();
   			//var depHrsArray ;
   			var depIdList=document.getElementById('DepIdList').value;//document.getElementById("FWDepartMentList").value;
   			depArray = depIdList.split(':');
			var addFlag=0;

			for(var i=0;i<depArray.length-1;i++)
  			{
				var hrs = document.getElementById('hrs'+depArray[i]).value;
				var depHrsArray = new Array();
				depHrsArray.push(depArray[i]);
				depHrsArray.push(hrs);
				allAvailBillHours.push(depHrsArray);
  			}
			//alert('allAvailBillHours'+allAvailBillHours.length);
    	}
    	var isAvailBillHrsChange = false;
    	function isAvailBillHoursChanged(curDepId){
        	var curDepHrs = document.getElementById('hrs'+curDepId).value;
        	for(var i=0;i<allAvailBillHours.length;i++)
  			{
      			var tempArray = new Array();
      			tempArray = allAvailBillHours[i];
      			if(tempArray[0] == curDepId)
      			{
      				if(tempArray[1] != curDepHrs){
          				//if(!confirm("Updating available billable hours will update all "+document.getElementById('txt'+curDepId).value+" employee's available billable hours except special users")){
					  if(!confirm("Updating available billable hours will update all "+document.getElementById(curDepId).value+" employee's available billable hours except special users")){
          					document.getElementById('hrs'+curDepId).value = tempArray[1];
							return false;
          				}else
						{
          					isAvailBillHrsChange = true;
						}
      				}
      			}
  			}
  			return true;
    	}

   		var globel;
		var curDepId;
    	function addDepartments()
   		{
    		clearErrorMsg();
   			document.getElementById("insertSpecial").style.display = 'block';

   			var depName=document.ManageDepartment.addDepText.value;
   			var code=document.ManageDepartment.addDepCode.value;
   			var hrsField = document.ManageDepartment.addAvailBillHrs;

   			depName=depName.replace(/^\s+|\s+$/g, '') ;
   			document.ManageDepartment.addDepText.value=depName;

   			if(isNaN(parseInt(code))==true ||code=='')
   				document.ManageDepartment.addDepCode.value='0';

   			if(depName=='' || textFieldValidation(depName)==0)
   	   			document.getElementById("insertSpecial").innerHTML='Please provide valid Location name. Special Character not allowed except hyphen';
			else if(codeValidation(code)==0)
				document.getElementById("insertSpecial").innerHTML='Please provide valid Item code';
   	   		else if(hrsField.value=='' || validateHours(hrsField)==0)
   	   	   		document.getElementById("insertSpecial").innerHTML='Please provide valid Available Billable Hours';
   	    	else if(depNameValidation(depName)==0)
				document.getElementById("insertSpecial").innerHTML='The Location Name already exist';
   	    	else if(depName!='' && hrsField.value!='' && textFieldValidation(depName)==1 && depNameValidation(depName)==1 && codeValidation(code)==1 && validateHours(hrsField)==1)
   			{
   	   			document.ManageDepartment.FWAction.value="Add";
   	   			document.ManageDepartment.action="/servlets/SaveDepartmentServlet";
   				document.ManageDepartment.submit();
   	   		}
   		}
   		 function UpdateLocation(depId)
		 {
   			if(isAvailBillHoursChanged(depId)){
   			globel=depId;
			clearErrorMsg();

			var found_it;
			for (var i=0; i<document.getElementsByName('rd'+depId).length; i++)  {
				if (document.getElementsByName('rd'+depId)[i].checked)  {
				    found_it = document.getElementsByName('rd'+depId)[i].value; //set found_it equal to checked button's value
   				}
			}

			// Delete Location, Only when not assigned for Employees / Projects
			if(parseInt(found_it)==2)
			{
				if(DeleteValidation(depId)==0){
					document.ManageDepartment.FWAction.value="Delete";
					document.ManageDepartment.FWDepID.value=depId;
					document.ManageDepartment.action="/servlets/SaveDepartmentServlet";
					document.ManageDepartment.submit();
				}else{
					document.getElementById("rem"+globel).style.display = 'none';
					document.getElementById("disp"+globel).innerHTML="You can not Delete. Location assigend for some of Employees/Projects.";
				}

			}// Update Location, rename not allowed when Employee / Projects assigned.
			else
			{
				//var depName=document.getElementById("txt"+depId).value;
				var depName=document.getElementById(depId).value;
				var code=document.getElementById("cd"+depId).value;
				var availBillHrs=document.getElementById("hrs"+depId).value;

				if(depName!='' && textFieldValidation(depName)==1){
					if(codeValidation(code)==1){
						if(availBillHrs!='' && validateHours(document.getElementById("hrs"+depId))==1){

							var saveStatus=document.getElementById("rd"+depId).value;
							var depList=document.getElementById("FWDepartMentList").value;

							depName=depName.replace(/^\s+|\s+$/g, '') ;
							//document.getElementById("txt"+depId).value=depName;
							document.getElementById(depId).value=depName;

							var uri = "/servlets/SaveDepartmentServlet";
							var action = "Update";
								var params="?FWAction="+action+"&FWDepID="+depId+"&DeptName="+depName+"&Status="+found_it+"&Code="+code
										+"&FWisAvailBillHrsChange="+isAvailBillHrsChange+"&availBillHrs="+availBillHrs+"&FWDepartMentList="+depList;
									//+"&availBillHrs="+availBillHrs+"&FWDepartMentList="+depList+"&FWisAvailBillHrsChange="+isAvailBillHrsChange;

							Ajaxcallfunction(uri+params);

						}
						else{
							document.getElementById("rem"+globel).style.display = 'none';
							document.getElementById("disp"+globel).innerHTML='Please provide valid Available Billable Hours';
						}
					}
					else{
						document.getElementById("rem"+globel).style.display = 'none';
						document.getElementById("disp"+globel).innerHTML='Please provide valid Item code';
					}
				}
				else{
					document.getElementById("rem"+globel).style.display = 'none';
					document.getElementById("disp"+globel).innerHTML='Please provide valid Location name. Special Character not allowed except hyphen';
				}
			}
   			}

 		}

   		function Ajaxcallfunction(url)
   		{
   			if(navigator.appName == "Microsoft Internet Explorer")
			{
	  			http = new ActiveXObject("Microsoft.XMLHTTP");
			}
			else
			{
			  http = new XMLHttpRequest();
			}
   			http.open("POST",url,true);
			http.onreadystatechange=stateChanged;
			http.send(null);
   		}

 		function stateChanged()
		{
			if (http.readyState==4){
				if(http.status==200){
			 		var resArr;
					resArr = http.responseText;
					try{
						document.getElementById("rem"+globel).style.display = 'none';
						document.getElementById("disp"+globel).innerHTML=resArr;
					}
					catch(err){
					}
			    }
			}
		}

   		function textFieldValidation(InString)
   		{
    		var RefString="abcdefghijklmnopqrstuvwxyz1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ.- ";

    		if(InString.length>0)
    		{
			for (Count=0; Count < InString.length; Count++)
    		{
		        TempChar= InString.substring (Count, Count+1);
        		if (RefString.indexOf (TempChar, 0)==-1)
	           		 return (0);
    		}
    		}else{    return (0); }

		    return (1);
   		}
   		function clearErrorMsg()
   		{
   			var resArr=new Array();

   			var depIdList=document.ManageDepartment.DepIdList.value;

   			resArr = depIdList.split(':');
   			var insert=parseInt(document.getElementById("InsertDep").value);
   			var delet=parseInt(document.getElementById("DeleteDep").value);
   			//alert('insert  :'+isNaN(insert)+':'+insert+' delet:'+(delet));

   			var Length=resArr.length-1;
  			try{
  				for(i=0;i<Length;i++)
  				{
		   			document.getElementById("rem"+resArr[i]).style.display = 'none';
					document.getElementById("disp"+resArr[i]).innerHTML='&nbsp;';
				}
				if(isNaN(insert)==false)
					if(insert==1)
						document.getElementById("insert").innerHTML='&nbsp;';
				if(isNaN(delet)==false)
					if(delet==1)
						document.getElementById("delete").innerHTML='&nbsp;';
				document.getElementById("insertSpecial").innerHTML='&nbsp;';
			}catch(err){
			}

   		}

   		function DeleteValidation(Depid)
   		{

			var resArr=new Array();

   			var depIdList=document.ManageDepartment.NotAssociated.value;

   			resArr = depIdList.split(':');

			var Length=resArr.length-1
			//alert(resArr)
   			for(i=0;i<Length;i++)
  			{
		   		if(resArr[i]==Depid)
		   		{
		   			return 0;
		   		}
			}
   			return 1;
   		}

   		function codeValidation(Code)
   		{
   			var c=0;
  			var RefString="1234567890";
   			for (Count=0; Count < Code.length; Count++)
  	  		{
    		    TempChar= Code.substring (Count, Count+1);

		    	if(TempChar==".")
			    	 c=c+1;
   		     	if (RefString.indexOf (TempChar, 0)==-1)
       		     return (0);
			    if(c>1)
			  return(0);
 	  		}
    	  	  return (1);
   		}
   		function depNameValidation(depName)
   		{
	   		var resArr=new Array();

   			var depIdList=document.getElementById("FWDepartMentList").value;
   			resArr = depIdList.split('#');
			var Length=resArr.length-1;
			var addFlag=0;

			for(i=0;i<Length;i++)
  			{
		   		var temVar=resArr[i].replace(/\s/g,"");
		   		var temDepName=depName.replace(/\s/g,"");

		   		if(temVar==temDepName)
		   		{
		   			return 0;
		   		}
			}
			return 1;
   		}

   		function validateHours( field )
   		{
   			var valid = "0123456789.";
   			var ok = "yes";
   			var temp;
   			var count = 0;
   			var isnan = isNaN(field.value);
   			if(isnan == true)
   	   			return 0;
   			for (var i=0; i<field.value.length; i++)
   			{
   				temp = "" + field.value.substring(i, i+1);
   				if (valid.indexOf(temp) == "-1")
   					ok = "no";
   				if( temp == '.' )
   					count++;
   			}

   			if( ok == "no" || count>1 )
   			{
   				field.focus();
   				field.select();
   				return 0;
   			}
   			return 1;
   		}

   		javascript:window.history.forward(1);
    	</script>
			<link rel="stylesheet" href="/assets/css/bootstrap.min.css">
    	</head>


<body onload="loadAvailBillHours();" style="padding-left: 25px;">
<form name="ManageDepartment" action="/servlets/SaveDepartmentServlet" method="post" class="form container">
	<fieldset class="border solid p-3">
<legend class="w-auto px-2 ">Manage Locations (Revenue Center)</legend>
	<div class="form-group row form-inline">
		<label for="addDepText" class="mr-2">*Location Name :</label>
		<input class="form-control" type="text" id="addDepText" name="addDepText" size="25" maxlength="49">
	</div>
	<div class="form-group row form-inline">
		<label for="addDepCode" class="mr-2">Item Code :</label>
		<input class="form-control" type="text" id="addDepCode" name="addDepCode" class="style1" size="9" maxlength="9">
	</div>
	<div class="form-group row form-inline">
		<label for="addDepCode" class="mr-2">*Avail Billable Hours :</label>
		<input class="form-control" type="text" id="addAvailBillHrs" name="addAvailBillHrs" class="style1" size="9" maxlength="5">
	</div>
	<div class="form-group container mb-3 d-flex align-items-center justify-content-start">
		<input type="button" class="btn btn-primary p-2 m-2" id="addDepButton" name="addDepButton" value="Add" size="9" onclick="addDepartments();" />
		<input type="button" class="btn btn-secondary p-2 m-2" id="ClearDepButton" name="ClearDepButton" value="Clear" size="9" onclick="document.ManageDepartment.addDepText.value='';document.ManageDepartment.addDepCode.value=''; document.ManageDepartment.addAvailBillHrs.value='';clearErrorMsg();">
	</div>
	</fieldset>



<div id="insertSpecial"  style="font-size: 10pt;font-family: Tahoma;color: Red">&nbsp; </div>
<div id ="remove"><br>&nbsp;</div>


		<%
			String inserDisplay ="0";
			try{
				inserDisplay = request.getParameter("display");
				if(inserDisplay.equals("1"))
				{
		%>
					<script type="text/javascript">
					document.getElementById("insertSpecial").style.display = 'none';
					</script>
					<div id="insert" style="font-size: 10pt;font-family: Tahoma;"><font color=red> Location Created Successfully</font></div>
		<%
				}
			}catch(Exception e){
			}

			String delete = "0";
			try{
				delete = request.getParameter("Delete");
				if(delete.equals("1"))
				{
		%>
					<script type="text/javascript">
					document.getElementById("insertSpecial").style.display = 'none';
					</script>
					<div id="delete" style="font-size: 10pt;font-family: Tahoma;"><font color=red> Location Deleted Successfully</font></div>
		<%		}
			}catch(Exception e){
			}
			ManageDepartmentDisplayLogic MdepartObj=new ManageDepartmentDisplayLogic();

			out.println(MdepartObj.buildAllDepartment());
		%>
		<input type="hidden" name="FWDepartMentList"  id="FWDepartMentList"  value="<%=MdepartObj.getFWDepartMentList()%>">
		<input type="hidden" name="DepIdList" id="DepIdList" value="<%=MdepartObj.getDepIdList()%>">
		<input type="hidden" name="NotAssociated" id="NotAssociated" value="<%=MdepartObj.getNotAssociated()%>">

		<input type="hidden" name="FWAction" id="FWAction" value="">

		<input type="hidden" name="FWDepID" id="FWDepID" value="">
		<input type="hidden" name="DeptName" id="DeptName" value="">
		<input type="hidden" name="saveStatus" id="saveStatus" value="">
		<input type="hidden" name="availBillHrs" id="availBillHrs" value="">

		<input type="hidden" name="InsertDep" id="InsertDep" value="<%=inserDisplay%>">
		<input type="hidden" name="DeleteDep" id="DeleteDep" value="<%=delete%>">


</form>
</body>
</html>
