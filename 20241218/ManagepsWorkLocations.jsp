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
    	<title>Manage PS Locations</title>
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
	        	document.ManagePSworkLocations.action = "/servlets/ProxyServlet";
	        	document.ManagePSworkLocations.submit();
		     } 
	        setTimeout('sessionWarning()', alerttimelimit);  
		}	
	</script>
<!-- Time Out Continue feature End -->

		<STYLE type="text/css">
@import url('/assets/css/bootstrap.min.css');


			table.contacts{ width: 740px;background-color: #fafafa;	border: 1px #000000 solid;border-spacing: 0.5px; font-family: Tahoma;}
				
		td.contactDept{ background-color: #lightgrey;border: 0.5px #000000 solid;font-family: Tahoma;font-weight: bolder;
				font-size: 15px;color: #000000;font-stretch: extra-condensed;Letter-spacing :1.2px; border: 1px solid #808080;}
				
		td.contact0{ border-bottom: 1px #C0C0C0 solid;text-align: left;	font-family: Tahoma;
				font-weight: normal;font-size: 9pt;padding-top: 3px;
				padding-bottom: 3px;padding-left: 6px;padding-right: 4px; background-color: #FFFFFF;border: 1px solid #808080;}
		td.contact1{ border-bottom: 1px #C0C0C0 solid;text-align: left;	font-family: Tahoma;
				font-weight: normal;font-size: 9pt;padding-top: 3px;
				padding-bottom: 3px;padding-left: 6px;padding-right: 6px; background-color: #fafafa;border-spacing: 0.5px;border: 1px solid #808080;}
		
		.style1 {font-family: Tahoma; font-size: 10pt; border: 1px solid #808080;}

		.blankhead{background-color:DCDCDC;color: #DC143C;font-size: 10pt;}
		
		 </STYLE>
		
<script type="text/javascript">

var globel;
var curDepId;

var globalLocNameArray=new Array();
var globalLocCodeArray=new Array();

function addLocations()
	{
	clearErrorMsg();
		document.getElementById("insertSpecial").style.display = 'block';
	
		var locName=document.ManagePSworkLocations.addLocText.value;
		var code=document.ManagePSworkLocations.addLocCode.value;
		var psLocation=document.ManagePSworkLocations.addPsLocation.value;
		locName=locName.replace(/^\s+|\s+$/g, '') ;
		code=code.replace(/^\s+|\s+$/g, '') ;
		psLocation=psLocation.replace(/^\s+|\s+$/g, '') ;
		document.ManagePSworkLocations.addLocText.value=locName;
		document.ManagePSworkLocations.addPsLocation.value=psLocation;
		if(locName=='' || textFieldValidation(locName)==0)
		{
  			document.getElementById("insertSpecial").innerHTML='Please provide valid Location name. Special Character not allowed except hyphen';
  		}
  		else if(code=='' || codeValidation(code)==0){
		document.getElementById("insertSpecial").innerHTML='Please provide valid PS Location code'; 
  		}
  		else if(psLocation=='' || textFieldValidation(psLocation)==0){
  			document.getElementById("insertSpecial").innerHTML='Please provide valid CP Location'; 
  	  		}  
   		else if(locationNameValidation(locName)==0){
		document.getElementById("insertSpecial").innerHTML='The Location Name already exist';
   		}
   		else if(locationCodeValidation(code)==0){
   		document.getElementById("insertSpecial").innerHTML='The PS Location code already exist';
   		}
   	else if(locName!='' && textFieldValidation(locName)==1 && locationNameValidation(locName)==1 && codeValidation(code)==1 && code!='')
		{
  			document.ManagePSworkLocations.FWAction.value="Add";
  			document.ManagePSworkLocations.action="/servlets/SavePSworkLocationServlet";
			document.ManagePSworkLocations.submit();
  		}   	    	
	}	

function updateLocation(locId)
{		
		clearErrorMsg();
		globel=locId;

			//var LocationName=document.getElementById("txt"+locId).value;						
			var LocationName=document.getElementById(locId).value;						
			var LocationCode=document.getElementById("cd"+locId).value;
			var psLocation=document.getElementById("cp"+locId).value;
			LocationName=LocationName.replace(/^\s+|\s+$/g, '') ;
			LocationCode=LocationCode.replace(/^\s+|\s+$/g, '') ;
			psLocation=psLocation.replace(/^\s+|\s+$/g, '') ;
			
			if(LocationName!=''){
				if(textFieldValidation(LocationName)==1 ){	
					if(textFieldValidation(psLocation)==1 ){				
						if(locationUpdateCodeValidation(LocationCode,locId)){
							if(locationUpdateNameValidation(LocationName,locId)){						
							if(codeValidation(LocationCode)==1 && LocationCode!=''){							
								var locList=globalLocNameArray;							
								LocationName=LocationName.replace(/^\s+|\s+$/g, '') ;	
								LocationCode=LocationCode.replace(/^\s+|\s+$/g, '') ;
								psLocation=psLocation.replace(/^\s+|\s+$/g, '') ;
								//document.getElementById("txt"+locId).value=LocationName;	
								document.getElementById(locId).value=LocationName;	
								document.getElementById("cd"+locId).value=LocationCode;
								document.getElementById("cp"+locId).value=psLocation;		
								var uri = "/servlets/SavePSworkLocationServlet";
								var action = "Update";						
								var params="?FWAction="+action+"&FWLocID="+locId+"&LocName="+LocationName+"&Code="+LocationCode+"&psLocation="+psLocation
								+"&FWPSLocationList="+locList;  
 							Ajaxcallfunction(uri+params);
							}else{
							 document.getElementById("rem"+globel).style.display = 'none';
						 	document.getElementById("disp"+globel).innerHTML='Please provide valid PS Location code';
				    		}
				    		}else{
							document.getElementById("rem"+globel).style.display = 'none';
							document.getElementById("disp"+globel).innerHTML='Please provide another Location name, its already exists';
							}
						}else{
						document.getElementById("rem"+globel).style.display = 'none';
						document.getElementById("disp"+globel).innerHTML='Please provide another PS Location code, its already exists';
						
					}
					}else{
						document.getElementById("rem"+globel).style.display = 'none';
						document.getElementById("disp"+globel).innerHTML='Please provide valid CP Location Code. Special Characters not allowed, except dot and hyphen';
					}
					}else{
					document.getElementById("rem"+globel).style.display = 'none';
					document.getElementById("disp"+globel).innerHTML='Please provide valid Location name. Special Character not allowed except comma,hypen,(,)';
			 		}
			}else{
				document.getElementById("rem"+globel).style.display = 'none';
				document.getElementById("disp"+globel).innerHTML='Please provide valid Location name';
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
	 		var responseArr;    
	 		responseArr = http.responseText; 
			var resArr=new Array();
			resArr=responseArr.split("#");
			var locId=resArr[1];
			locId=locId.replace(/^\s+|\s+$/g, '');
			if(resArr[0]=="Location Updated successfully.")
			{
				reloadList(locId);
				reloadListCode(locId);	
			}
			
			try{						
				document.getElementById("rem"+globel).style.display = 'none';
				document.getElementById("disp"+globel).innerHTML=resArr[0];
			}
			catch(err){
			}
	    }                	 
	}							
}	 
		
	function textFieldValidation(InString)
	{
	var RefString="abcdefghijklmnopqrstuvwxyz1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ.-(), ";
	
	if(InString.length>0)
	{
	for (Count=0; Count < InString.length; Count++) 
	{
        TempChar= InString.substring (Count, Count+1);
		if (RefString.indexOf (TempChar, 0)==-1)  
       		 return (0) 
	}
	}else{    return (0) }

    return (1)
	}
	
	function clearErrorMsg()
	{
		var resArr=new Array();
		
		var LocIdList=document.ManagePSworkLocations.PSLocIdList.value;
		
		resArr = LocIdList.split(':');
		var insert=parseInt(document.getElementById("InsertLoc").value);
		var delet=parseInt(document.getElementById("DeleteLoc").value);
							
		var Length=resArr.length-1;
		try{
			for(i=0;i<Length;i++)
			{
   			document.getElementById("rem"+resArr[i]).style.display = 'none';
			document.getElementById("disp"+resArr[i]).innerHTML='&nbsp;'
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
	
	function codeValidation(Code)
	{
		var RefString="abcdefghijklmnopqrstuvwxyz1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ.-(), ";
		
		if(Code.length>0)
		{
		for (Count=0; Count < Code.length; Count++) 
		{
	        TempChar= Code.substring (Count, Count+1);
			if (RefString.indexOf (TempChar, 0)==-1)  
	       		 return (0) 
		}
		}else{    return (0) }

	    return (1)
  	}
	
	function locationNameValidation(locName)
	{
		var resArr=new Array();				   			
		var LocIdList=globalLocNameArray;
		resArr = LocIdList.split('#');
		var Length=resArr.length-1;			
		var addFlag=0;
	
	for(i=0;i<Length;i++)
		{
   		var temVar=resArr[i].replace(/\s/g,"");
   		var temLocName=locName.replace(/\s/g,"");
   		var NameArray = temVar.split('_');   		
   		if(NameArray[1]==temLocName)
   		{	   			
   			return 0;		   			
   		}
	}			
	return 1;
	}

	function locationCodeValidation(locationCode)
	{
		var resArr=new Array();
		var LocCodes=globalLocCodeArray;
		resArr = LocCodes.split(':');
		var Length=resArr.length-1;			
		var addFlag=0;
	for(i=0;i<Length;i++)
	{
   		var temVar=resArr[i].replace(/\s/g,"");
   		var temLocName=locationCode.replace(/\s/g,"");
   		var codeArray = temVar.split('_'); 
   		if(codeArray[1]==temLocName)
   		{	  
   						
   			return 0;		   			
   		}
	}		
		
			return 1;
	}
	
	function locationUpdateCodeValidation(locationCode,LocID)
	{
		var resArr=new Array();
		var codeIdArray=new Array();
		codeIdArray=globalLocCodeArray.split(':');
		var tempArray=new Array();
		var indexOfCodeArray;
		var count=0;
		for(var i=0;i<codeIdArray.length-1;i++)
		{
			var str=codeIdArray[i];
			var localArr=new Array();
			localArr=str.split('_');			
			var temVar=localArr[1].replace(/\s/g,"");
			var temLocCode=locationCode.replace(/\s/g,"");
			if(temVar==temLocCode)
			{
				if(LocID != localArr[0] )
				{
					return false;
				}
			}
			
		}	
		
		return true;			
	} 
	
	function locationUpdateNameValidation(locationName,LocID)
	{
		var resArr=new Array();
		var nameIdArray=new Array();
		nameIdArray=globalLocNameArray.split('#');
		var tempArray=new Array();
		var indexOfnameArray;
		var count=0;
		for(var i=0;i<nameIdArray.length-1;i++)
		{
			var str=nameIdArray[i];
			var localArr=new Array();
			localArr=str.split('_');	
			var locName =localArr[1];		
			var temVar=locName.replace(/\s/g,"");
			var temLocName=locationName.replace(/\s/g,"");
			
			if(temVar==temLocName)
			{
				if(LocID != localArr[0] )
				{
				return false;	
				}
			}
			
		}	
		return true;
			
	}

	function reloadList(locId)
	{
		var nameIdArray=new Array();
		nameIdArray=globalLocNameArray.split('#');
		var tempArray=new Array();
		var indexOfnameArray;
		for(var i=0;i<nameIdArray.length-1;i++)
		{
			var str=nameIdArray[i];
			var localArr=new Array();
			localArr=str.split('_');
			
			var locationName=localArr[1];
			var locationId=localArr[0];
				if(locationId==locId)
			{
				indexOfnameArray=i;
				break;
			}
	}
		//var name=document.getElementById("txt"+locId).value;
		var name=document.getElementById(locId).value;
		var newVar=locId+"_"+name;
		var oldValue=nameIdArray[indexOfnameArray];
		nameIdArray[indexOfnameArray]=newVar;
		var newValue=nameIdArray[indexOfnameArray];
		globalLocNameArray=globalLocNameArray.replace(oldValue,newValue);
	}

	function reloadListCode(locId)
	{
		var codeIdArray=new Array();
		codeIdArray=globalLocCodeArray.split(':');
		var tempArray=new Array();
		var indexOfcodeArray;
		for(var i=0;i<codeIdArray.length-1;i++)
		{
			var str=codeIdArray[i];
			var localArr=new Array();
			localArr=str.split('_');
			
			var locationCode=localArr[1];
			var locationId=localArr[0];
			
			if(locationId==locId)
			{
				indexOfcodeArray=i;
				break;
			}
		}
		var code=document.getElementById("cd"+locId).value;
		var newVar=locId+"_"+code;
		var oldValue=codeIdArray[indexOfcodeArray];
		codeIdArray[indexOfcodeArray]=newVar;
		var newValue=codeIdArray[indexOfcodeArray];
		globalLocCodeArray=globalLocCodeArray.replace(oldValue,newValue);
	}
	
	function storeValues()
	{
	globalLocNameArray=document.getElementById('FWPSLocationList').value;
    globalLocCodeArray=document.getElementById('LocCodeList').value;
	}
	
			

	javascript:window.history.forward(1);
</script>
			<link rel="stylesheet" href="/assets/css/bootstrap.min.css">
		</head>

<body onload="storeValues();" style="padding-left: 30px;">
<form name="ManagePSworkLocations" action="/servlets/SavePSworkLocationServlet" method="post" class="form container">
	<fieldset class="border solid p-3">
		<legend class="w-auto px-2 ">Manage Work Locations</legend>

		<div class="form-group row form-inline">
			<label for="addLocText" class="mr-2">*Location Name :</label>
			<input class="form-control" type="text" id="addLocText" name="addLocText" size="25" maxlength="49">
		</div>
		<div class="form-group row form-inline">
			<label for="addLocCode" class="mr-2">*PS Location code :</label>
			<input class="form-control form-control-sm" type="text" id="addLocCode" name="addLocCode"  size="9" maxlength="9">
		</div>
		<div class="form-group row form-inline">
			<label for="addPsLocation" class="mr-2">*CP Location Code :</label>
			<input class="form-control" type="text" id="addPsLocation" name="addPsLocation"  size="25" maxlength="59">
		</div>
		<div class="form-group container mb-3 d-flex align-items-center justify-content-start">
			<input type="button" class="btn btn-primary p-2 m-2" id="addLocationButton" name="addLocationButton" value="Add" size="9" onclick="addLocations();document.ManagePSworkLocations.addLocCode.value='';"" />
			<input type="button" class="btn btn-secondary p-2 m-2" id="ClearLocButton" name="ClearLocButton" class="style1" value="Clear" size="9" onclick="document.ManagePSworkLocations.addLocText.value='';document.ManagePSworkLocations.addLocCode.value='';document.ManagePSworkLocations.addPsLocation.value='';clearErrorMsg();">
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
			ManagePSworkLocationDisplayLogic MpsLocObj=new ManagePSworkLocationDisplayLogic();
	
			out.println(MpsLocObj.buildAllLocations());
		%>
	
<input type="hidden" name="FWPSLocationList"  id="FWPSLocationList"  value="<%=MpsLocObj.getPSLocationsList()%>">
<input type="hidden" name="PSLocIdList" id="PSLocIdList" value="<%=MpsLocObj.getLocIdList()%>">
<input type="hidden" name="LocCodeList" id="LocCodeList" value="<%=MpsLocObj.getPSLocationsCode()%>">


<input type="hidden" name="FWAction" id="FWAction" value="">
<input type="hidden" name="FWLocID" id="FWLocID" value="">
<input type="hidden" name="LocName" id="LocName" value="">		
<input type="hidden" name="InsertLoc" id="InsertLoc" value="null" >
<input type="hidden" name="DeleteLoc" id="DeleteLoc" value="null" >

</left>
</form>
</body>    
</html>
