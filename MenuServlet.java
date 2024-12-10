package com.eserv.platform;

import com.europa.exceptions.EuropaException;
import com.europa.exceptions.SecurityException;
import com.europa.log.LogManager;
import com.europa.security.Application;
import com.europa.security.ApplicationUsers;
import com.europa.security.SecurityManager;
import com.europa.security.User;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MenuServlet extends HttpServlet {
   Vector clientMenuAccessUserVector = null;
   Vector clientdetailAccessVector = null;
   String applicationId = null;
   boolean menuFlag;
   String employeeType = "";
   
   private List<MenuItem> overallMenuItems = new ArrayList<>(); 


   public void init(ServletConfig config) throws ServletException {
      super.init(config);
   }

   public void doGet(HttpServletRequest request, HttpServletResponse response) {
      LogManager.log("/servlets/MenuServlet.doGet()");
      String currentUser = request.getParameter("FWUserID");
      this.applicationId = System.getProperty("europa.application.id").toString();
      request.setAttribute("overallMenuItems", overallMenuItems) ; 
//      response.setContentType("text/html");
      LogManager.log("current User " + request.getQueryString());
      PrintWriter pw = null;

      try {
         pw = response.getWriter();
      } catch (IOException var7) {
         LogManager.log("Could not retrieve response stream.");
         return;
      }
      

      StringBuffer output = new StringBuffer();
      if (currentUser != null && !currentUser.equals("")) {
         User user = SecurityManager.getUser(currentUser);
         if (user.isInternalUser()) {
            this.employeeType = user.getEmployeeType();
         }

         output.append(this.writeLogoSection(user)).append(this.writeMenuHeader()).append(this.writeMenuEntries(user)).append(this.writeMenuFooter());
      }
    
      

      if (pw != null) {
//         pw.println(output.toString());
    	  try {
        	  request.getRequestDispatcher("/menu.jsp").forward(request, response);
          }catch (Exception e) {
    		// TODO: handle exception
        	  e.printStackTrace(); 
    	}
    	    
      }
      

   }

   public void doPost(HttpServletRequest request, HttpServletResponse response) {
      LogManager.log("/servlets/MenuServlet.doPost()");
      this.doGet(request, response);
   }

   private String writeMenuHeader() {
      LogManager.log("/servlets/MenuServlet.writeMenuHeader()");
      StringBuffer buffer = new StringBuffer();
      String platform = System.getProperty("europa.dashboard.platform");
      StringBuffer eServharley = new StringBuffer();
      eServharley.append("var xmlHttp; var redirect = false;function createAjaxHttpRequest(){\ttry\t{\t\txmlHttp=new XMLHttpRequest(); \t} catch (e)\t{  \t\ttry\t\t{ \t\t\txmlHttp=new ActiveXObject('Msxml2.XMLHTTP'); \t\t}\t\tcatch (e)\t\t{\t\t\ttry\t\t\t{ \t\t\t\txmlHttp=new ActiveXObject('Microsoft.XMLHTTP');\t\t\t}\t\t\tcatch (e)\t\t\t{\t\t\t\talert('Your browser does not support AJAX!');\t\t\t\treturn false;\t\t\t}\t\t}\t}}function handleStateChange(){\ttry\t {\t\tif(xmlHttp.readyState == 1)\t\t{\t\t}\t\tif(xmlHttp.readyState == 4)\t\t{\t\t\tvar output='false';\t\t\tif(xmlHttp.status == 200)\t\t\t{   \t\t\t\toutput= xmlHttp.responseText;\t\t\t\tif(output == 'false') \t\t\t\t{\t\t\t\t\tredirect = false; \t\t\t\t}\t\t\t\telse \t\t\t\t\tredirect = true;\t\t\t}\t\t\telse\t\t\t{\t\t\t\tredirect = false; \t\t\t}\t\t}\t}\tcatch(err)\t{\t}}function checkAppsStatus (url){\ttry{\t\tcreateAjaxHttpRequest();\t\txmlHttp.open('GET','/servlets/CheckApplicationStatusServlet?FWURL='+url,false);\t\txmlHttp.onreadystatechange = handleStateChange;\t\txmlHttp.send(null);\t}\tcatch(err)\t{\t\tredirect = false;\t}}");
      eServharley.append("function callIntegeration(url,userid,password){   if(redirect == true){document.loginform.action=url+'/servlets/ProxyServlet';document.loginform.FWUserId.value =userid;document.loginform.FWPassword.value=password;document.loginform.submit();}else { alert ('The remote application is currently inaccessible, please try again later.'); }}");
      buffer.append("<head>").append("<title>Navigation Menu</title><link rel=\"stylesheet\" href=\"/css/bootstrap.min.css\">\n" +
              "\t\t<link rel=\"stylesheet\" href=\"/css/styles.css\">").append("<script>").append("function openDashboard()").append("{").append("var location = \"" + platform + "/dashboard.html\";").append("var title = \"\";").append("window.open(location, title, \"toolbar=no,scrollbars=yes,resizable=yes,width=800,height=600\");").append("}").append(eServharley).append("function homeClick()\t{if (parent.body.isHome==1)\t{return false;}}").append("var message=\"\";").append("</script>").append("</head>").append("<body bgcolor=\"#ffffff\" text=\"#000000\" link=\"#0000ff\" vlink=\"#800080\" alink=\"#ff0000\">");
//      buffer.append("<div id=\"menudiv\" align=\"center\">");
//      buffer.append(" <table border=\"0\" cellspacing=\"2\" style=\"border-collapse: collapse\" bordercolor=\"#111111\" width=\"100%\" id=\"AutoNumber1\" cellpadding=\"2\"> ");
      
      return buffer.toString();
   }

   private String writeLogoSection(User user) {
      LogManager.log("/servlets/MenuServlet.writeLogoSection()");
      StringBuffer buffer = new StringBuffer();
      String imageName = null;
      Integer AppId = new Integer(System.getProperty("europa.application.id").toString());
      String imageLogo = "";
      if (AppId == 1) {
         imageLogo = "images\\eServ\\eserv_logo.jpg";
      } else if (AppId == 2) {
         imageLogo = "images\\Harley\\eServ1.jpg";
      }

      if (user.isInternalUser()) {
         imageName = imageLogo;
      } else if (user.isContractUser()) {
         imageName = imageLogo;
      } else {
         imageName = imageLogo;
      }

      LogManager.log("imageName:" + imageName);
      buffer.append("<html><div id=\"logodiv\" align=\"center\">").
      append("<img src=\"/").append(imageName).append("\" alt=\"\">").append("</div>");
      return buffer.toString();
   }

   private String writeMenuEntries(User user) {
      LogManager.log("/servlets/MenuServlet.writeMenuEntries()");
      StringBuffer buffer = new StringBuffer();
      String menu = null;
      String userName = user.toString();
      String notAllowedUsers = System.getProperty("europa.adminproject.notallowedusers");
      Vector notAllowed = new Vector();
      StringTokenizer st = new StringTokenizer(notAllowedUsers, ",");

      while(st.hasMoreTokens()) {
         notAllowed.add(st.nextElement().toString());
      }

      //LogManager.log("notAllowed Users:" + notAllowed);
      String clientMenuAccessUsers = System.getProperty("europa.clientdetailsblockusers");
      this.clientMenuAccessUserVector = new Vector();
      StringTokenizer clientMenuSt = new StringTokenizer(clientMenuAccessUsers, ",");

      while(clientMenuSt.hasMoreTokens()) {
         this.clientMenuAccessUserVector.add(clientMenuSt.nextElement().toString());
      }

      LogManager.log("Client menu Access:" + this.clientMenuAccessUserVector);
      String clientdetailAccessUsers = System.getProperty("europa.clientdetailsaccessusers");
      this.clientdetailAccessVector = new Vector();
      StringTokenizer clientMenuAccess = new StringTokenizer(clientdetailAccessUsers, ",");

      while(clientMenuAccess.hasMoreTokens()) {
         this.clientdetailAccessVector.add(clientMenuAccess.nextElement().toString());
      }

      LogManager.log("Client menu Access: 2" + this.clientdetailAccessVector);
      StringBuffer homeLink = new StringBuffer();
      homeLink.append("<tr><td>").append("<a href=\"/servlets/ProxyServlet?FWModule=home\" target=\"_parent\" id=\"Home\" onClick=\"return homeClick()\" >Home</a>").append("</td></tr>");
      overallMenuItems.add(new MenuItem("Home","/servlets/ProxyServlet?FWModule=home","Home","home"));
      String empId = user.getEmployeeId().toString();
      if (user.isInternalUser() && !this.is1099Employee()) {
         if (notAllowed.contains(userName)) {
            menu = this.generateAdminReportsMenu(user);
         } else {
            menu = this.generateInternalMenu(user);
         }
      } else if (!user.isContractUser() && !this.is1099Employee()) {
         menu = this.generateExternalMenu(user);
      } else {
         homeLink = new StringBuffer();
         menu = this.generateContractorMenu(user);
      }

      try {
         buffer.append(homeLink);
         buffer.append(menu).append(this.createPSFEEDMenu(user)).append(this.writeAppsMenu(user));
      } catch (EuropaException var15) {
         LogManager.log("Menu Creation exception:" + var15);
      }

      buffer.append("<tr><td>").append("<a href=\"/servlets/ProxyServlet?FWModule=changepassword\" target=\"body\" id=\"ChangePassword\" >Change Password</a>").append("</td></tr>");
      buffer.append("<tr><td>").append("<a href=\"/servlets/ProxyServlet?FWModule=logout\" target=\"_parent\" id=\"Logout\" >Log Out</a>").append("</td></tr>");

      return buffer.toString();
   }

   private String writeMenuFooter() {
      LogManager.log("/servlets/MenuServlet.writeMenuFooter()");
      StringBuffer buffer = new StringBuffer();
      buffer.append("</table></div>");
      buffer.append("</body>").append("</html>");
      return buffer.toString();
   }

   private Vector getUsersByFeaturePerms(String permName) {
      String accessUsers = System.getProperty(permName);
      Vector userVector = new Vector();
      StringTokenizer st = new StringTokenizer(accessUsers, ",");

      while(st.hasMoreTokens()) {
         userVector.add(st.nextElement().toString());
      }

      // LogManager.log(permName + " Users:" + userVector);
      return userVector;
   }

   private String generateInternalMenu(User user) {
      LogManager.log("/servlets/MenuServlet.generateInternalMenu()");
      StringBuffer buffer = new StringBuffer();
      String chagePwdAccessUsers = System.getProperty("europa.changepasswordaccessusers");
      Vector changePwdUserVector = new Vector();
      StringTokenizer changePwdSt = new StringTokenizer(chagePwdAccessUsers, ",");

      while(changePwdSt.hasMoreTokens()) {
         changePwdUserVector.add(changePwdSt.nextElement().toString());
      }

      // LogManager.log("Change Password Users:" + changePwdUserVector);
      String contractorAccess = System.getProperty("europa.contractors.access.users");
      Vector contractorAccessVector = new Vector();
      StringTokenizer contractorAccessSt = new StringTokenizer(contractorAccess, ",");

      while(contractorAccessSt.hasMoreTokens()) {
         contractorAccessVector.add(contractorAccessSt.nextElement().toString());
      }

      String activityCodesAccess = System.getProperty("europa.activitycodes.access.users");
      Vector activityCodesAccessVector = new Vector();
      StringTokenizer activityCodesAccessSt = new StringTokenizer(activityCodesAccess, ",");

      while(activityCodesAccessSt.hasMoreTokens()) {
         activityCodesAccessVector.add(activityCodesAccessSt.nextElement().toString());
      }

      Vector dashboardMenuVector = this.getUsersByFeaturePerms("europa.dashboard.menu");
      Vector projectStatusMenuVector = this.getUsersByFeaturePerms("europa.projectstatus.menu");
      Vector reportsMenuVector = this.getUsersByFeaturePerms("europa.reports.menu");
      Vector clientsMenuVector = this.getUsersByFeaturePerms("europa.clients.menu");
      Vector employeesMenuVector = this.getUsersByFeaturePerms("europa.employees.menu");
      Vector contractorsMenuVector = this.getUsersByFeaturePerms("europa.contractors.menu");
      Vector projectsMenuVector = this.getUsersByFeaturePerms("europa.projects.menu");
      Vector activityCodesMenuVector = this.getUsersByFeaturePerms("europa.activitycodes.menu");
      Vector registerMenuVector = this.getUsersByFeaturePerms("europa.register.accessuser");
      Vector unPostInvoiceMenuVector = this.getUsersByFeaturePerms("europa.unpost_invoice.accessuser");
      Vector lockTimeEntryMenuVector = this.getUsersByFeaturePerms("europa.lock_time_entry.accessuser");
      Vector billingMenuVector = this.getUsersByFeaturePerms("europa.billing.accessuser");
      String userName = user.toString();
      if (!dashboardMenuVector.contains(userName)) {
    	  overallMenuItems.add(new MenuItem("Dashboard", "javascript:openDashboard();", "Dashboard", "dashboard")) ;
    	  buffer.append("<tr><td >").append("<a href=\"javascript:openDashboard();\"  id=\"Dashboard\" >Dashboard</a>").append("</td></tr>");
      }

      if (!projectStatusMenuVector.contains(userName)) {
    	  overallMenuItems.add(new MenuItem("Project Status", "/servlets/ProxyServlet?FWModule=project_status", "ProjectStatus", "receipt_long")) ;
         buffer.append("<tr><td>").append("<a href=\"/servlets/ProxyServlet?FWModule=project_status\" target=\"body\" id=\"ProjectStatus\" >Project Status</a>").append("</td></tr>");
      }
      overallMenuItems.add(new MenuItem("Time Log", "/servlets/ProxyServlet?FWModule=timelogger", "Timelog", "insights")) ;
      buffer.append("<tr><td>").append("<a href=\"/servlets/ProxyServlet?FWModule=timelogger\" target=\"body\" id=\"TimeLog\" >Time Log</a>").append("</td></tr>");
      LogManager.log("applicationId:" + this.applicationId);
      if (!this.applicationId.equals("2")) {
         if (!reportsMenuVector.contains(userName)) {
        	 overallMenuItems.add(new MenuItem("Reports", "/servlets/ProxyServlet?FWModule=reportviewer", "Reports", "account_circle")) ;
            buffer.append("<tr><td>").append("<a href=\"/servlets/ProxyServlet?FWModule=reportviewer\" target=\"body\" id=\"Reports\" >Reports</a>").append("</td></tr>");
         }

         try {
            if (userName.equals("admin") || SecurityManager.isMember("admin", user) && billingMenuVector.contains(userName)) {
            	overallMenuItems.add(new MenuItem("Billing", "/servlets/ProxyServlet?FWModule=billing", "Billing", "payments")) ; 
               buffer.append("<tr><td>").append("<a href=\"/servlets/ProxyServlet?FWModule=billing\" target=\"body\" id=\"Billing\" >Billing</a>").append("</td></tr>");
            }

            if (userName.equals("admin") || SecurityManager.isMember("admin", user) && unPostInvoiceMenuVector.contains(userName)) {
            	overallMenuItems.add(new MenuItem("Unpost Invoice", "/servlets/ProxyServlet?FWModule=unpost_invoice", "UnpostInvoice", "receipt")) ; 
               buffer.append("<tr><td>").append("<a href=\"/servlets/ProxyServlet?FWModule=unpost_invoice\" target=\"body\" id=\"UnpostInvoice\" >Unpost Invoice</a>").append("</td></tr>");
            }
         } catch (SecurityException var28) {
            var28.printStackTrace();
         }
      }

      if (!clientsMenuVector.contains(userName) && !this.clientMenuAccessUserVector.contains(userName)) {
    	  overallMenuItems.add(new MenuItem("Clients", "/servlets/ProxyServlet?FWModule=admin_client", "Clients", "account_circle")) ;
         buffer.append("<tr><td>").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_client\" target=\"body\" id=\"Clients\" >Clients</a>").append("</td></tr>");
      }

      if (!employeesMenuVector.contains(userName)) {
    	  overallMenuItems.add(new MenuItem("Employees", "/servlets/ProxyServlet?FWModule=admin_employee", "Employees", "people")) ;
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_employee\" target=\"body\"  id=\"Employees\" >Employees</a>").append("</td></tr>");
      }

      if (!contractorsMenuVector.contains(userName) && (userName.equals("admin") || contractorAccessVector.contains(userName))) {
    	  overallMenuItems.add(new MenuItem("Contractors", "/servlets/ProxyServlet?FWModule=admin_contractor", "Contractors", "engineering")) ; 
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_contractor\" target=\"body\"  id=\"Contractors\" >Contractors</a>").append("</td></tr>");
      }

      if (!projectsMenuVector.contains(userName)) {
    	  overallMenuItems.add(new MenuItem("Projects", "/servlets/ProxyServlet?FWModule=admin_project", "Projects", "engineering")) ;
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_project\" target=\"body\"  id=\"Projects\" >Projects</a>").append("</td></tr>");
      }

      if (!activityCodesMenuVector.contains(userName) && activityCodesAccessVector.contains(userName)) {
    	  overallMenuItems.add(new MenuItem("Activity Codes", "/servlets/ProxyServlet?FWModule=admin_activitycodes", "Activitycode", "settings")) ;
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_activitycodes\" target=\"body\"  id=\"Activitycode\" >Activity Codes</a>").append("</td></tr>");
      }

      if (userName.equals("admin")) {
    	  overallMenuItems.add(new MenuItem("Activity Codes", "/servlets/ProxyServlet?FWModule=admin_activitycodes", "Activitycode", "settings")) ;
    	  overallMenuItems.add(new MenuItem("Locations", "/servlets/ProxyServlet?FWModule=departments", "ManageDepartment", "place")) ;
    	  overallMenuItems.add(new MenuItem("Work Locations", "/servlets/ProxyServlet?FWModule=psworklocations", "Managepsworklocation", "location_city")) ;
    	  overallMenuItems.add(new MenuItem("List of Holidays", "/servlets/ProxyServlet?FWModule=List_of_Holidays", "ListOfHolidays", "holiday_village")) ;
    	  
    	  
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_activitycodes\" target=\"body\"  id=\"Activitycode\" >Activity Codes</a>").append("</td></tr>");
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=departments\" target=\"body\"  id=\"ManageDepartment\" >Locations</a>").append("</td></tr>");
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=psworklocations\" target=\"body\"  id=\"Managepsworklocation\" >Work Locations</a>").append("</td></tr>");
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=List_of_Holidays\" target=\"body\"  id=\"ListOfHolidays\" >List of Holidays</a>").append("</td></tr>");
      }

      try {
         if (userName.equals("admin") || SecurityManager.isMember("admin", user) && lockTimeEntryMenuVector.contains(userName)) {
        	 overallMenuItems.add(new MenuItem("Lock Time Entry", "/servlets/ProxyServlet?FWModule=tl_HardLock", "LockTimeEntry", "lock_clock")) ; 
            buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=tl_HardLock\" target=\"body\"  id=\"LockTimeEntry\" >Lock Time Entry</a>").append("</td></tr>");
         }
      } catch (SecurityException var27) {
         var27.printStackTrace();
      }

      try {
         if (userName.equals("admin") || SecurityManager.isMember("admin", user) && registerMenuVector.contains(userName)) {
        	 overallMenuItems.add(new MenuItem("Register", "/servlets/ProxyServlet?FWModule=register", "Register", "how_to_reg")) ;
            buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=register\" target=\"body\"  id=\"Register\" >Register</a>").append("</td></tr>");
         }
      } catch (SecurityException var26) {
         var26.printStackTrace();
      }

      return buffer.toString();
   }

   private String generateAdminReportsMenu(User user) {
      StringBuffer buffer = new StringBuffer();
      String activityCodesAccess = System.getProperty("europa.activitycodes.access.users");
      Vector activityCodesAccessVector = new Vector();
      StringTokenizer activityCodesAccessSt = new StringTokenizer(activityCodesAccess, ",");

      while(activityCodesAccessSt.hasMoreTokens()) {
         activityCodesAccessVector.add(activityCodesAccessSt.nextElement().toString());
      }

      String contractorAccess = System.getProperty("europa.contractors.access.users");
      Vector contractorAccessVector = new Vector();
      StringTokenizer contractorAccessSt = new StringTokenizer(contractorAccess, ",");

      while(contractorAccessSt.hasMoreTokens()) {
         contractorAccessVector.add(contractorAccessSt.nextElement().toString());
      }
      overallMenuItems.add(new MenuItem("Dashboard", "javascript:openDashboard();", "Dashboard", "dashboard")) ;
      overallMenuItems.add(new MenuItem("Project Status", "/servlets/ProxyServlet?FWModule=project_status", "ProjectStatus", "receipt_long")) ;
      overallMenuItems.add(new MenuItem("Time Log", "/servlets/ProxyServlet?FWModule=timelogger", "Timelog", "insights")) ;
      
      
      buffer.append("<tr><td >").append("<a href=\"javascript:openDashboard();\"  id=\"Dashboard\" >Dashboard</a>").append("</td></tr>");
      buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=project_status\" target=\"body\"  id=\"ProjectStatus\" >Project Status</a>").append("</td></tr>");
      buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=timelogger\" target=\"body\"  id=\"Timelog\" >Time Log</a>").append("</td></tr>");
      LogManager.log("Report menu inside" + this.clientMenuAccessUserVector + "client detail vector:" + this.clientdetailAccessVector);
      LogManager.log("user is" + this.clientdetailAccessVector.contains(user.toString()));
      if (!this.clientMenuAccessUserVector.contains(user.toString()) && this.clientdetailAccessVector.contains(user.toString())) {
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_client\" target=\"body\"  id=\"Clients\" >Clients</a>").append("</td></tr>");
         overallMenuItems.add(new MenuItem("Clients", "/servlets/ProxyServlet?FWModule=admin_client", "Clients", "account_circle")) ;
      }

      if (!this.applicationId.equals("2")) {
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=reportviewer\" target=\"body\"  id=\"Reports\" >Reports</a>").append("</td></tr>");
         overallMenuItems.add(new MenuItem("Reports", "/servlets/ProxyServlet?FWModule=reportviewer", "Reports", "account_circle")) ;
      }
      overallMenuItems.add(new MenuItem("Projects", "/servlets/ProxyServlet?FWModule=admin_project", "Projects", "people")) ;
      overallMenuItems.add(new MenuItem("Employees", "/servlets/ProxyServlet?FWModule=admin_employee", "Employees", "people")) ;
      
      buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_project\" target=\"body\"  id=\"Projects\" >Projects</a>").append("</td></tr>");
      buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_employee\" target=\"body\"  id=\"Employees\" >Employees</a>").append("</td></tr>");
      if (contractorAccessVector.contains(user.toString())) {
    	  overallMenuItems.add(new MenuItem("Contractors", "/servlets/ProxyServlet?FWModule=admin_contractor", "Contractors", "group")) ;
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_contractor\" target=\"body\"  id=\"Contractors\" >Contractors</a>").append("</td></tr>");
      }

      if (activityCodesAccessVector.contains(user.toString())) {
    	  overallMenuItems.add(new MenuItem("Activity Codes", "/servlets/ProxyServlet?FWModule=admin_activitycodes", "Activitycode", "settings")) ;
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_activitycodes\" target=\"body\"  id=\"Activitycode\" >Activity Codes</a>").append("</td></tr>");
      }

      return buffer.toString();
   }

   private String generateContractorMenu(User user) {
      LogManager.log("/servlets/MenuServlet.generateInternalMenu()");
      StringBuffer buffer = new StringBuffer();
      overallMenuItems.add(new MenuItem("Time Log", "/servlets/ProxyServlet?FWModule=timelogger", "Timelog", "insights")) ;
      buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=timelogger\" target=\"body\"  id=\"Timelog\" >Time Log</a>").append("</td></tr>");
      if (!this.clientMenuAccessUserVector.contains(user.toString()) && this.clientdetailAccessVector.contains(user.toString())) {
    	  overallMenuItems.add(new MenuItem("Clients", "/servlets/ProxyServlet?FWModule=admin_client", "Clients", "account_circle")) ;
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_client\" target=\"body\"  id=\"Clients\" >Clients</a>").append("</td></tr>");
      }

      if (this.is1099Employee()) {
    	  overallMenuItems.add(new MenuItem("Employees", "/servlets/ProxyServlet?FWModule=admin_employee", "Employees", "people")) ;
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_employee\" target=\"body\"  id=\"Employees\" >Employees</a>").append("</td></tr>");
      }

      buffer.append("<tr><td >").append(this.generateActivityReportLink(user)).append("</td></tr>");
      return buffer.toString();
   }

   private String generateExternalMenu(User user) {
      LogManager.log("/servlets/MenuServlet.generateExternalMenu()");
      StringBuffer buffer = new StringBuffer();
      buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=project_status\" target=\"body\"  id=\"ProjectStatus\" >Project Status</a>").append("</td></tr>");
      overallMenuItems.add(new MenuItem("Project Status", "/servlets/ProxyServlet?FWModule=project_status", "ProjectStatus", "receipt_long")) ;
      if (!this.clientMenuAccessUserVector.contains(user.toString()) && this.clientdetailAccessVector.contains(user.toString())) {
    	  overallMenuItems.add(new MenuItem("Clients", "/servlets/ProxyServlet?FWModule=admin_client", "Clients", "account_circle")) ;
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_client\" target=\"body\"  id=\"Clients\" >Clients</a>").append("</td></tr>");
      }

      if (!this.applicationId.equals("2")) {
    	  overallMenuItems.add(new MenuItem("Reports", "/servlets/ProxyServlet?FWModule=reportviewer", "Reports", "account_circle")) ;
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=reportviewer\" target=\"body\"  id=\"Reports\" >Reports</a>").append("</td></tr>");
      }

      return buffer.toString();
   }

   private String generateActivityReportLink(User user) {
      LogManager.log("/servlets/MenuServlet.generateInternalMenu()");
      StringBuffer link = new StringBuffer();
      return link.toString();
   }

   private boolean is1099Employee() {
      boolean retVal = false;

      try {
         return this.employeeType.equals("1099");
      } catch (Exception var3) {
         return retVal;
      }
   }

   private String writeAppsMenu(User user) throws EuropaException {
      LogManager.log("writeAppsMenu():");
      StringBuffer buffer = new StringBuffer("");

      try {
         String AppId = System.getProperty("europa.application.id").toString();
         new Hashtable();
         Hashtable apps = SecurityManager.getApplication();
         Application currentApps = null;
         if (apps.containsKey(AppId)) {
            currentApps = (Application)apps.get(AppId);
         }

         Iterator it = apps.keySet().iterator();

         while(true) {
            Application Appln;
            ApplicationUsers temp;
            do {
               do {
                  do {
                     do {
                        if (!it.hasNext()) {
                           return buffer.toString();
                        }

                        Appln = (Application)apps.get(it.next());
                     } while(currentApps == null);
                  } while(Appln.getApplnId() == currentApps.getApplnId());

                  temp = SecurityManager.getApplnUser(user.getName(), String.valueOf(Appln.getApplnId()));
               } while(!this.applicationId.equals("1") && !this.applicationId.equals("2"));
            } while(temp == null && !user.toString().equals("admin"));

            buffer.append(this.createAppsMenu(Appln.getApplnName(), Appln.getApplnUrl(), user.getName(), user.getPlainTextPassword()));
         }
      } catch (Exception var9) {
         LogManager.log("writeAppsMenu Exception:" + var9);
         return buffer.toString();
      }
   }

   private StringBuffer createAppsMenu(String AppsMenu, String redirectedURL, String userId, String Plainpassword) throws EuropaException {
      LogManager.log("createAppsMenu():");
      StringBuffer buffer = new StringBuffer();
      buffer.append("<form name=\"loginform\" method=\"POST\" target=\"_top\">").append("<input type=\"hidden\" name=\"FWModule\" value=\"login\">").append("<input type=\"hidden\" name=\"FWCurrentPage\" value=\"0\">").append("<input type=\"hidden\" name=\"FWRetry\" value=\"false\">").append("<input type=\"hidden\" name=\"FWUserId\" value=\"\">").append("<input type=\"hidden\" name=\"FWPassword\" value=\"\">");
      buffer.append("<tr><td >").append("<a href=\"javascript:void(0);\"     id=\"AppIDLink\" onclick=\"checkAppsStatus ('" + redirectedURL + "');callIntegeration('" + redirectedURL + "','" + userId + "','" + Plainpassword + "');\" >").append(AppsMenu + "</a>").append("</td></tr>");
      buffer.append("</form>");
      return buffer;
   }

   private StringBuffer createPSFEEDMenu(User user) throws EuropaException {
      LogManager.log("createPSFEEDMenu():");
      StringBuffer buffer = new StringBuffer();
      String psfeedusers = System.getProperty("europa.PSFeedUsers");
      ArrayList psfeeduserslist = new ArrayList();
      StringTokenizer psfeedSt = new StringTokenizer(psfeedusers, ",");

      while(psfeedSt.hasMoreTokens()) {
         psfeeduserslist.add(psfeedSt.nextElement().toString());
      }

      LogManager.log("PSFeed Users =" + psfeeduserslist);
      LogManager.log("Login UserNames=" + user.getName());
      String userName = user.toString();
      if (!this.applicationId.equals("2") && (userName.equals("admin") || psfeeduserslist.contains(userName))) {
         LogManager.log("UserName contains vector ");
         overallMenuItems.add(new MenuItem("Changepoint", "/servlets/ProxyServlet?FWModule=ps_feed", "PsFeed", "control_point")) ;
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=ps_feed\" target=\"body\"  id=\"PsFeed\" >Changepoint</a>").append("</td></tr>");
      }

      return buffer;
   }

   private String generateInternalMenu_ASUser(User user) {
      LogManager.log("/servlets/MenuServlet.generateInternalMenu()");
      StringBuffer buffer = new StringBuffer();
      String contractorAccess = System.getProperty("europa.contractors.access.users");
      Vector contractorAccessVector = new Vector();
      StringTokenizer contractorAccessSt = new StringTokenizer(contractorAccess, ",");

      while(contractorAccessSt.hasMoreTokens()) {
         contractorAccessVector.add(contractorAccessSt.nextElement().toString());
      }

      String activityCodesAccess = System.getProperty("europa.activitycodes.access.users");
      Vector activityCodesAccessVector = new Vector();
      StringTokenizer activityCodesAccessSt = new StringTokenizer(activityCodesAccess, ",");

      while(activityCodesAccessSt.hasMoreTokens()) {
         activityCodesAccessVector.add(activityCodesAccessSt.nextElement().toString());
      }

      Vector dashboardMenuVector = this.getUsersByFeaturePerms("europa.dashboard.menu");
      Vector projectStatusMenuVector = this.getUsersByFeaturePerms("europa.projectstatus.menu");
      Vector reportsMenuVector = this.getUsersByFeaturePerms("europa.reports.menu");
      Vector clientsMenuVector = this.getUsersByFeaturePerms("europa.clients.menu");
      Vector employeesMenuVector = this.getUsersByFeaturePerms("europa.employees.menu");
      Vector contractorsMenuVector = this.getUsersByFeaturePerms("europa.contractors.menu");
      Vector projectsMenuVector = this.getUsersByFeaturePerms("europa.projects.menu");
      Vector activityCodesMenuVector = this.getUsersByFeaturePerms("europa.activitycodes.menu");
      String userName = user.toString();
      if (!dashboardMenuVector.contains(userName)) {
    	  overallMenuItems.add(new MenuItem("Dashboard", "javascript:openDashboard();", "Dashboard", "dashboard")) ;
         buffer.append("<tr><td >").append("<a href=\"javascript:openDashboard();\"  id=\"Dashboard\" >Dashboard</a>").append("</td></tr>");
      }

      if (!projectStatusMenuVector.contains(userName)) {
    	  overallMenuItems.add(new MenuItem("Project Status", "/servlets/ProxyServlet?FWModule=project_status", "ProjectStatus", "receipt_long")) ;
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=project_status\" target=\"body\"  id=\"ProjectStatus\" >Project Status</a>").append("</td></tr>");
      }
      overallMenuItems.add(new MenuItem("Time Log", "/servlets/ProxyServlet?FWModule=timelogger", "Timelog", "insights")) ;
      buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=timelogger\" target=\"body\"  id=\"Timelog\" >Time Log</a>").append("</td></tr>");
      if (!this.applicationId.equals("2") && !reportsMenuVector.contains(userName)) {
    	  overallMenuItems.add(new MenuItem("Reports", "/servlets/ProxyServlet?FWModule=reportviewer", "Reports", "account_circle")) ;
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=reportviewer\" target=\"body\"  id=\"Reports\" >Reports</a>").append("</td></tr>");
      }

      if (!clientsMenuVector.contains(userName) && !this.clientMenuAccessUserVector.contains(userName)) {
    	  overallMenuItems.add(new MenuItem("Clients", "/servlets/ProxyServlet?FWModule=admin_client", "Clients", "account_circle")) ;
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_client\" target=\"body\"  id=\"Clients\" >Clients</a>").append("</td></tr>");
      }

      if (!employeesMenuVector.contains(userName)) {
    	  overallMenuItems.add(new MenuItem("Employees", "/servlets/ProxyServlet?FWModule=admin_employee", "Employees", "people")) ;
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_employee\" target=\"body\"  id=\"Employees\" >Employees</a>").append("</td></tr>");
      }

      if (!contractorsMenuVector.contains(userName) && (userName.equals("admin") || contractorAccessVector.contains(userName))) {
    	  overallMenuItems.add(new MenuItem("Contractors", "/servlets/ProxyServlet?FWModule=admin_contractor", "Contractors", "engineering")) ; 
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_contractor\" target=\"body\"  id=\"Contractors\" >Contractors</a>").append("</td></tr>");
      }

      if (!projectsMenuVector.contains(userName)) {
    	  overallMenuItems.add(new MenuItem("Projects", "/servlets/ProxyServlet?FWModule=admin_project", "Projects", "people")) ;
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_project\" target=\"body\"  id=\"Projects\" >Projects</a>").append("</td></tr>");
      }

      if (!activityCodesMenuVector.contains(userName) && activityCodesAccessVector.contains(userName)) {
    	  overallMenuItems.add(new MenuItem("Activity Codes", "/servlets/ProxyServlet?FWModule=admin_activitycodes", "Activitycode", "settings")) ;
         buffer.append("<tr><td >").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_activitycodes\" target=\"body\"  id=\"Activitycode\" >Activity Codes</a>").append("</td></tr>");
      }

      return buffer.toString();
   }
   
   private static class MenuItem {
	    private final String label;
	    private final String url;
	    private final String id;
	    private final String iconName ;

	    public MenuItem(String label, String url, String id,String iconName) {
	        this.label = label;
	        this.url = url;
	        this.id = id;
	        this.iconName = iconName; 
	    }

	    public String getLabel() {
	        return label;
	    }

	    public String getUrl() {
	        return url;
	    }

	    public String getId() {
	        return id;
	    }
	    
	    public String getIconName() {
			return iconName;
		}
	}
}
