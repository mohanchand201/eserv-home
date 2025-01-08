package com.ltts.llc.dps.eserv.module;

import com.ltts.llc.dps.eserv.dao.EmployeeDAO;
import com.ltts.llc.dps.eserv.platform.SyncManager;
import com.ltts.llc.dps.eserv.reportgenerator.JasperserverConnector;
import com.ltts.llc.dps.eserv.europa.exceptions.EuropaException;
import com.ltts.llc.dps.eserv.europa.exceptions.InvalidDataFormatException;
import com.ltts.llc.dps.eserv.europa.log.LogManager;
import com.ltts.llc.dps.eserv.europa.logic.AbstractBusinessLogic;
import com.ltts.llc.dps.eserv.europa.logic.DataLogic;
import com.ltts.llc.dps.eserv.europa.logic.DisplayLogic;
import com.ltts.llc.dps.eserv.europa.logic.PreparedSqlStatement;
import com.ltts.llc.dps.eserv.europa.logic.SqlStatement;
import com.ltts.llc.dps.eserv.europa.security.SecurityManager;
import com.ltts.llc.dps.eserv.europa.security.User;
import  com.ltts.llc.dps.eserv.europa.session.Session;
import  com.ltts.llc.dps.eserv.europa.session.SessionManager;
import java.io.File;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import oracle.sql.BLOB;
import oracle.sql.CLOB;

public class AdminEmployeeBusinessLogic extends AbstractBusinessLogic {
   Vector notAllowed = null;
   Vector degree = null;
   ArrayList insustryList = null;
   Vector hrmEditUsers = null;
   Vector hrm_ViewAccess = null;
   Vector hrmHelmetAwardEditUsers = null;
   Vector hrmMasterFullAccessUsers = null;
   Vector hrmWagesPermsFullAccessUsers = null;
   Vector hrmSearchScreenFullAccessUsers = null;
   private Vector oldHelmetResults = null;
   private Vector hrmDataEntryUsers = null;
   private boolean spotBonusInsertion = true;

   public void init(String sessionId) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.init()");
      boolean empFlag = false;
      this.notAllowed = new Vector();
      Session session = SessionManager.findSession(sessionId);
      session.setAttribute("FWSession", sessionId);
      this.setDisplayLabel(sessionId);
      if (!session.getId().equals(sessionId)) {
         session.setAttribute("FWCurrentPage", "10");
      }

      LogManager.log("......................." + session.getAttribute("FWFromTimelogger"));
      Date dt = new Date();
      DisplayLogic displayLogic = this.module.getDisplayLogic();
      LogManager.log("Retreived DisplayLogic " + displayLogic);
      String pagePrefix = System.getProperty("europa.module.repository") + File.separator + this.module.getName() + File.separator;
      String allowedusers = System.getProperty("europa.employees.notallowedusers");
      session.setAttribute("FWHostIP", System.getProperty("europa.app.host"));
      session.setAttribute("FWApplicationHost", System.getProperty("europa.app.host"));
      StringTokenizer st = new StringTokenizer(allowedusers, ",");

      while(st.hasMoreTokens()) {
         this.notAllowed.add(st.nextElement().toString());
      }

      String hrm_ViewAccessUsers = System.getProperty("europa.HrmInfo.viewAccess");
      this.hrm_ViewAccess = new Vector();
      StringTokenizer hrm_View = new StringTokenizer(hrm_ViewAccessUsers, ",");

      String hrmDEUsers;
      while(hrm_View.hasMoreTokens()) {
         hrmDEUsers = hrm_View.nextToken().toString();
         this.hrm_ViewAccess.add(hrmDEUsers);
      }

      LogManager.log("......................." + session.getAttribute("FWFromTimelogger"));
      LogManager.log("......................." + session.getAttribute("FWEmployeeID"));
      this.hrmDataEntryUsers = new Vector();
      hrmDEUsers = System.getProperty("europa.email.configs.HRM_DE");
      StringTokenizer hrmDE = new StringTokenizer(hrmDEUsers, ",");

      while(hrmDE.hasMoreTokens()) {
         this.hrmDataEntryUsers.add(hrmDE.nextElement().toString());
      }

      String retryString = (String)session.getAttribute("FWRetry");
      LogManager.log("Retry = " + retryString);
      if (retryString != null) {
         boolean retry = new Boolean(retryString);
         if (retry) {
            LogManager.log("Resetting session");
            session.reset();
         }
      }

      String userId = session.getUser().getEmployeeId().toString();
      LogManager.log("UserId " + userId);
      session.setAttribute("FWUserId", userId);
      session.setAttribute("FWCurrSession", sessionId);
      LogManager.log("fwsubmit:" + session.getAttribute("FWSubmit") + "............" + sessionId);
      String codeAccessButton = (String)session.getAttribute("FWCodeAccessButton");
      if (codeAccessButton != null) {
         session.setAttribute("FWCurrentPage", "3");
         session.removeAttribute("FWSubmit");
         session.removeAttribute("FWCodeAccessButton");
      }

      String privacyDetails = (String)session.getAttribute("FWPrivacyDetails");
      if (privacyDetails != null) {
         session.setAttribute("FWCurrentPage", "4");
         session.removeAttribute("FWSubmit");
         session.removeAttribute("FWPrivacyDetails");
      }

      String HRMToolInfo = (String)session.getAttribute("FWHRMToolInfo");
      if (HRMToolInfo != null) {
         session.setAttribute("FWCurrentPage", "5");
         session.removeAttribute("FWSubmit");
         session.removeAttribute("FWHRMToolInfo");
      }

      String evaluationUpload = (String)session.getAttribute("FWEvaluationUpload");
      if (evaluationUpload != null) {
         session.setAttribute("FWCurrentPage", "6");
         session.removeAttribute("FWSubmit");
         session.removeAttribute("FWEvaluationUpload");
      }

      String fromTimeLogger = (String)session.getAttribute("FWFromTimelogger");
      LogManager.log("From Timelogger" + fromTimeLogger);
      String employeeId;
      if (fromTimeLogger != null && !fromTimeLogger.equals("")) {
         session.setAttribute("FWCurrentPage", "5");
         LogManager.log("comming to Access Denied Area");
         employeeId = "";
         if (session.getAttribute("FWEmployeeID") != null) {
            if (session.getAttribute("FWEmployeeID").toString().equals(System.getProperty("europa.su.email").trim().toString())) {
               employeeId = "82";
            } else {
               User tempUser = SecurityManager.getUser(session.getAttribute("FWEmployeeID").toString());
               employeeId = tempUser.getEmployeeId().toString();
            }

            session.setAttribute("FWEmployeeID", employeeId);
         }

         session.setAttribute("FWExtension", "");
         session.removeAttribute("FWSubmit");
         session.removeAttribute("FWRetain");
         session.removeAttribute("FWFromTimelogger");
      }

      employeeId = (String)session.getAttribute("FWResumeUpload");
      if (employeeId != null) {
         session.setAttribute("FWCurrentPage", "7");
         session.removeAttribute("FWSubmit");
         session.removeAttribute("FWResumeUpload");
      }

      String isGoBack = (String)session.getAttribute("FWGoBack");
      if (isGoBack != null) {
         session.setAttribute("FWCurrentPage", "5");
         session.removeAttribute("FWSubmit");
         session.removeAttribute("FWGoBack");
         session.removeAttribute("FWArch");
      }

      String isGoBack1 = (String)session.getAttribute("FWGoBack1");
      if (isGoBack1 != null) {
         session.setAttribute("FWCurrentPage", "4");
         int presentYear = Integer.parseInt(String.valueOf(dt.getYear())) + 1900;
         session.setAttribute("FWYearSelected", String.valueOf(presentYear));
         session.removeAttribute("FWSubmit");
         session.removeAttribute("FWGoBack1");
      }

      LogManager.log("Show all employees:" + session.getAttribute("FWShowAllEmp"));
      if (session.getAttribute("FWShowAllEmp") != null) {
         session.setAttribute("FWCurrentPage", "0");
      }

      LogManager.log("FwHrm TOol:" + session.getAttribute("FWHRMToolInfo"));
      String currentPage = (String)session.getAttribute("FWCurrentPage");
      LogManager.log("Current Page = " + currentPage);
      String submitString = (String)session.getAttribute("FWSubmit");
      LogManager.log("Submit String = " + submitString);
      String page = null;
      int a = 0;
      LogManager.log("Year selected is...................." + dt.getYear());
      if (session.getAttribute("FWYearSelected") == null) {
         a = Integer.parseInt(String.valueOf(dt.getYear())) + 1900;
         session.setAttribute("FWYearSelected", String.valueOf(a));
      }

      String hrmInfoEditUsers = System.getProperty("europa.HrmInfo.fullAccess");
      this.hrmEditUsers = new Vector();
      StringTokenizer hrmSt = new StringTokenizer(hrmInfoEditUsers, ",");

      String hrmWagesFullAccessUsers;
      while(hrmSt.hasMoreTokens()) {
         hrmWagesFullAccessUsers = hrmSt.nextToken().toString();
         this.hrmEditUsers.add(hrmWagesFullAccessUsers);
      }

      hrmWagesFullAccessUsers = System.getProperty("europa.hrmWagesPermsScreen.fullaccess");
      this.hrmWagesPermsFullAccessUsers = new Vector();
      StringTokenizer hrmWagesPermsToken = new StringTokenizer(hrmWagesFullAccessUsers, ",");

      String hrmSrchScreenFullAccessUsers;
      while(hrmWagesPermsToken.hasMoreTokens()) {
         hrmSrchScreenFullAccessUsers = hrmWagesPermsToken.nextToken();
         this.hrmWagesPermsFullAccessUsers.add(hrmSrchScreenFullAccessUsers);
      }

      LogManager.log("Test name" + System.getProperty("europa.hrmSearchScreen.fullaccess"));
      hrmSrchScreenFullAccessUsers = System.getProperty("europa.hrmSearchScreen.fullaccess");
      this.hrmSearchScreenFullAccessUsers = new Vector();
      StringTokenizer hrmSrchUsersToken = new StringTokenizer(hrmSrchScreenFullAccessUsers, ",");

      String hrmMstrFullAccessUsers;
      while(hrmSrchUsersToken.hasMoreTokens()) {
         hrmMstrFullAccessUsers = hrmSrchUsersToken.nextToken();
         this.hrmSearchScreenFullAccessUsers.add(hrmMstrFullAccessUsers);
      }

      session.removeAttribute("hrmSearchScreenFullAccessUsers");
      session.setAttribute("hrmSearchScreenFullAccessUsers", this.hrmSearchScreenFullAccessUsers);
      hrmMstrFullAccessUsers = System.getProperty("europa.hrmMasterScreen.fullaccess");
      this.hrmMasterFullAccessUsers = new Vector();
      StringTokenizer hrmSearchFullUsersToken = new StringTokenizer(hrmMstrFullAccessUsers, ",");

      String hrmHelmetAwEditUsers;
      while(hrmSearchFullUsersToken.hasMoreTokens()) {
         hrmHelmetAwEditUsers = hrmSearchFullUsersToken.nextToken().toString();
         this.hrmMasterFullAccessUsers.add(hrmHelmetAwEditUsers);
      }

      hrmHelmetAwEditUsers = System.getProperty("europa.HrmInf.HelmetAwards.EditUsers");
      this.hrmHelmetAwardEditUsers = new Vector();
      StringTokenizer hrmHAEditUsers = new StringTokenizer(hrmHelmetAwEditUsers, ",");

      String queryName;
      while(hrmHAEditUsers.hasMoreTokens()) {
         queryName = hrmHAEditUsers.nextToken().toString();
         this.hrmHelmetAwardEditUsers.add(queryName);
      }

      try {
         if (SecurityManager.getUser(session.getAttribute("FWEmail").toString()).toString().equals(session.getUser().toString()) && !SecurityManager.isMember("admin", session.getUser())) {
            session.setAttribute("FWBlockBankAndFlexTimeStart", "<!--");
            session.setAttribute("FWBlockBankAndFlexTimeEnd", "-->");
         } else {
            session.setAttribute("FWBlockBankAndFlexTimeStart", "");
            session.setAttribute("FWBlockBankAndFlexTimeEnd", "");
         }
      } catch (Exception var59) {
         session.setAttribute("FWBlockBankAndFlexTimeStart", "<!--");
         session.setAttribute("FWBlockBankAndFlexTimeEnd", "-->");
      }

      LogManager.log("Full Access Permission" + this.hrmSearchScreenFullAccessUsers);

      try {
         if (SecurityManager.isMember("People Manager", session.getUser()) && SecurityManager.isMember("TopLineManager", session.getUser()) && !this.hrmSearchScreenFullAccessUsers.contains(session.getUser().toString())) {
            session.setAttribute("FWPeopleManagerIVPPermission", session.getUser().getEmployeeId().toString());
            session.setAttribute("FWResumeIVPermStart", "<!--");
            session.setAttribute("FWResumeIVPermEnd", "-->");
         } else {
            session.setAttribute("FWPeopleManagerIVPPermission", "0");
            session.setAttribute("FWResumeIVPermStart", "");
            session.setAttribute("FWResumeIVPermEnd", "");
         }
      } catch (Exception var58) {
      }

      boolean success;
      if (submitString == null) {
         if (currentPage != null && !currentPage.equals("0")) {
            String empFullAccessUsers;
            StringTokenizer st2;
            String empSecureInfoUsers;
            String empSelectedAccessId;
            String employeeViewUsers;
            StringTokenizer st1;
            if (currentPage.equals("1")) {
               this.clearEmpInfo(sessionId);
               LogManager.log("comming to one");
               Vector eViewUser = new Vector();
               success = this.buildNewEmpPage(sessionId);
               LogManager.log("Here Comming success:" + success);
               empSelectedAccessId = System.getProperty("europa.employeeviewpermittedusers");
               LogManager.log("EmployeeViewUsers:" + empSelectedAccessId);
               st1 = new StringTokenizer(empSelectedAccessId, ",");

               while(st1.hasMoreTokens()) {
                  employeeViewUsers = st1.nextToken().toString();
                  eViewUser.add(employeeViewUsers);
               }

               LogManager.log("eViewUser:" + eViewUser);
               employeeViewUsers = System.getProperty("europa.employeeactivitycodes.editaccess");
               st1 = new StringTokenizer(employeeViewUsers, ",");

               while(st1.hasMoreTokens()) {
                  empFullAccessUsers = st1.nextToken().toString();
                  eViewUser.add(empFullAccessUsers);
               }

               empFullAccessUsers = System.getProperty("europa.employeesecureinfousers.viewaccess");
               st2 = new StringTokenizer(empFullAccessUsers, ",");

               while(st2.hasMoreTokens()) {
                  empSecureInfoUsers = st2.nextToken().toString();
                  eViewUser.add(empSecureInfoUsers);
               }

               if (session.getAttribute("FWEmployeeID") != null && session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString())) {
                  empFlag = true;
               }

               session.removeAttribute("FWGoToSecure");
               LogManager.log("emp Flag" + empFlag);
               if ((!success || this.notAllowed.contains(session.getUser().toString()) || !SecurityManager.isMember("admin", session.getUser())) && !empFlag && !eViewUser.contains(session.getUser().toString())) {
                  page = System.getProperty("europa.module.repository") + File.separator + "common" + File.separator + "illegal_access.html";
               } else {
                  page = pagePrefix + "employee_edit.html";
               }
            } else if (currentPage.equals("2")) {
               LogManager.log("comming to the sequence 2");
               success = this.buildEditEmpPage(sessionId);
               if (session.getAttribute("FWisCompanyFunctionDisplayed") != null && session.getAttribute("FWisCompanyFunctionDisplayed").toString().equals("false")) {
                  session.setAttribute("FWisDisabled", "true");
               }

               session.removeAttribute("FWGoToSecure");
               String empAccessId = session.getUser().getEmployeeId().toString();
               empSelectedAccessId = session.getAttribute("FWEmployeeID").toString();
               LogManager.log("EmpID from Session:" + session.getAttribute("FWEmployeeID") + "AccessID" + empAccessId);
               Vector eViewUser = new Vector();
               employeeViewUsers = System.getProperty("europa.employeeviewpermittedusers");
               LogManager.log("EmployeeViewUsers:" + employeeViewUsers);
               st1 = new StringTokenizer(employeeViewUsers, ",");

               while(st1.hasMoreTokens()) {
                  empFullAccessUsers = st1.nextToken().toString();
                  eViewUser.add(empFullAccessUsers);
               }

               LogManager.log("eViewUser:" + eViewUser);
               empFullAccessUsers = System.getProperty("europa.employeeactivitycodes.editaccess");
               st2 = new StringTokenizer(empFullAccessUsers, ",");

               while(st2.hasMoreTokens()) {
                  empSecureInfoUsers = st2.nextToken().toString();
                  eViewUser.add(empSecureInfoUsers);
               }

               empSecureInfoUsers = System.getProperty("europa.employeesecureinfousers.viewaccess");
               StringTokenizer st3 = new StringTokenizer(empSecureInfoUsers, ",");

               while(st3.hasMoreTokens()) {
                  String username = st3.nextToken().toString();
                  eViewUser.add(username);
               }

               if (session.getAttribute("FWEmployeeID") != null && empAccessId.equals(empSelectedAccessId)) {
                  empFlag = true;
               }

               LogManager.log("Hrm View access:" + this.hrm_ViewAccess);
               if (this.hrm_ViewAccess.contains(session.getUser().toString()) && !this.hrmEditUsers.contains(session.getUser().toString())) {
                  LogManager.log("comming to if");
                  this.showHrmButton(sessionId, true);

                  try {
                     if (SecurityManager.getUser(session.getAttribute("FWEmail").toString()).toString().equals(session.getUser().toString()) && this.hrmHelmetAwardEditUsers.contains(session.getUser().toString())) {
                        session.setAttribute("NormalUserViewStart", "<!--");
                        session.setAttribute("NormalUserViewEnd", "-->");
                        session.removeAttribute("HelmetEditUsersStart");
                        session.removeAttribute("HelmetEditUsersEnd");
                     } else {
                        session.setAttribute("FWSubmitButtonStart", "<!--");
                        session.setAttribute("FWSubmitButtonEnd", "-->");
                        session.setAttribute("HelmetEditUsersStart", "<!--");
                        session.setAttribute("HelmetEditUsersEnd", "-->");
                        session.removeAttribute("NormalUserViewStart");
                        session.removeAttribute("NormalUserViewEnd");
                     }
                  } catch (Exception var57) {
                     session.setAttribute("FWSubmitButtonStart", "<!--");
                     session.setAttribute("FWSubmitButtonEnd", "-->");
                  }
               } else {
                  if (this.isPeopleManagerUsers(sessionId, session.getAttribute("FWEmployeeID").toString()) || this.isTopLineManagerUsers(sessionId, session.getAttribute("FWEmployeeID").toString())) {
                     session.setAttribute("FWSubmitButtonStart", "<!--");
                     session.setAttribute("FWSubmitButtonEnd", "-->");
                  }

                  LogManager.log("comming to else");
               }

               LogManager.log("Check 56");
               if ((!success || this.notAllowed.contains(session.getUser().toString()) || !SecurityManager.isMember("admin", session.getUser())) && !empFlag && !eViewUser.contains(session.getUser().toString()) && !this.hrmEditUsers.contains(session.getUser().toString()) && !this.hrmDataEntryUsers.contains(session.getUser().toString()) && (!SecurityManager.isMember("People Manager", session.getUser()) || !this.isPeopleManagerUsers(sessionId, session.getAttribute("FWEmployeeID").toString())) && (!SecurityManager.isMember("TopLineManager", session.getUser()) || !this.isTopLineManagerUsers(sessionId, session.getAttribute("FWEmployeeID").toString()))) {
                  page = System.getProperty("europa.module.repository") + File.separator + "common" + File.separator + "illegal_access.html";
               } else {
                  if (session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString()) && !SecurityManager.isMember("admin", session.getUser())) {
                     this.showEmployeeData(sessionId, false);
                     session.setAttribute("FWNonCompanyfunctions", "True");
                     session.setAttribute("FWNoncompanyfunMode", "disabled");
                  } else if (!SecurityManager.isMember("admin", session.getUser()) && !session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString())) {
                     session.setAttribute("FWEmployeeViewStart", "<!--");
                     session.setAttribute("FWEmployeeViewEnd", "-->");
                  } else {
                     session.setAttribute("FWEmployeeViewStart", "");
                     session.setAttribute("FWEmployeeViewEnd", "");
                     if (!SecurityManager.isMember("admin", session.getUser()) && session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString())) {
                        session.setAttribute("FWNonCompanyfunctions", "True");
                        session.setAttribute("FWNoncompanyfunMode", "disabled");
                     }
                  }

                  if (empSelectedAccessId.equals("82")) {
                     session.setAttribute("FWEmployeeViewStart", "<!--");
                     session.setAttribute("FWEmployeeViewEnd", "-->");
                     session.setAttribute("FWShowEmpSecureInfoStart", "<!--");
                     session.setAttribute("FWShowEmpSecureInfoEnd", "-->");
                     session.setAttribute("FWShowHRMInfoStart", "<!--");
                     session.setAttribute("FWShowHRMInfoEnd", "-->");
                  }

                  if (this.isPeopleManagerUsers(sessionId, session.getAttribute("FWEmployeeID").toString()) || this.isTopLineManagerUsers(sessionId, session.getAttribute("FWEmployeeID").toString())) {
                     session.setAttribute("FWShowHRMInfoStart", "");
                     session.setAttribute("FWShowHRMInfoEnd", "");
                  }

                  page = pagePrefix + "employee_edit.html";
               }
            } else if (currentPage.equals("3")) {
               this.buildCodeAccessPage(sessionId);
               page = pagePrefix + "codeAccess.html";
            } else if (currentPage.equals("4")) {
               if (session.getAttribute("EditMode").toString().equals("Insert")) {
                  LogManager.log("step1");
                  queryName = session.getUser().getEmployeeId().toString();
                  session.setAttribute("FWUserId", queryName);
                  if (SecurityManager.isMember("admin", session.getUser())) {
                     session.removeAttribute("FWSecureInfoEditStart");
                     session.removeAttribute("FWSecureInfoEditEnd");
                  } else {
                     session.setAttribute("FWSecureInfoEditStart", "<!--");
                     session.setAttribute("FWSecureInfoEditEnd", "-->");
                  }
               } else {
                  LogManager.log("step2");
                  if (session.getUser().getEmployeeId().toString().equals("82")) {
                     session.removeAttribute("FWPrevDetails");
                     session.setAttribute("FWPrevDetails", this.empSalaryAudit(sessionId));
                     session.removeAttribute("FWSecureInfoEditStart");
                     session.removeAttribute("FWSecureInfoEditEnd");
                  } else {
                     session.setAttribute("FWSecureInfoEditStart", "<!--");
                     session.setAttribute("FWSecureInfoEditEnd", "-->");
                  }
               }

               if (session.getAttribute("EditMode").toString().equals("Insert")) {
                  LogManager.log("step3");
                  session.setAttribute("FWBackButtonStart", "<!--");
                  session.setAttribute("FWBackButtonEnd", "-->");
               } else {
                  LogManager.log("step4");
                  session.removeAttribute("FWBackButtonStart");
                  session.removeAttribute("FWBackButtonEnd");
               }

               queryName = System.getProperty("europa.employees.secureinfo.editaccess");
               ArrayList secureInfoEditUsers = new ArrayList();
               StringTokenizer secureInfoEditUserTokens = new StringTokenizer(queryName, ",");

               while(secureInfoEditUserTokens.hasMoreTokens()) {
                  String username = secureInfoEditUserTokens.nextToken().toString();
                  secureInfoEditUsers.add(username);
               }

               if (secureInfoEditUsers.contains(session.getUser().toString())) {
                  session.removeAttribute("FWSecureInfoEditStart");
                  session.removeAttribute("FWSecureInfoEditEnd");
               }

               try {
                  if (session.getAttribute("FWEmail") != null && !session.getAttribute("FWEmail").equals("")) {
                     if (session.getAttribute("FWEmployeeID").toString().equals("82")) {
                        LogManager.log("admin is comming");
                        this.showallHrmData(sessionId, true);
                     } else {
                        try {
                           if (this.hrmDataEntryUsers.contains(session.getUser().toString())) {
                              this.showallHrmData(sessionId, false);
                              this.spotBonusInsertion = false;
                           } else {
                              this.showallHrmData(sessionId, true);
                              this.spotBonusInsertion = true;
                           }
                        } catch (Exception var55) {
                           this.showallHrmData(sessionId, false);
                           this.spotBonusInsertion = false;
                        }
                     }
                  }
               } catch (Exception var56) {
               }

               this.DataFeedFunction(session.getAttribute("EditMode").toString(), sessionId);
               session.setAttribute("FWSpotBounsAudit", this.spotBonusCommisionAudit(sessionId));
               page = pagePrefix + "employeePrivacyDetails.html";
               Date secureInfoCurrentDate = new Date();
               LogManager.log("secureInfoCurrentDate  :" + secureInfoCurrentDate);
               SimpleDateFormat secureInfoCurrentDateFormatter = new SimpleDateFormat("MM/dd/yy");
               String secureInfoSysDate = secureInfoCurrentDateFormatter.format(secureInfoCurrentDate);
               session.setAttribute("FWSecureInfoSysDate", secureInfoSysDate);
               LogManager.log("FWSecureInfoSysDate  :" + secureInfoSysDate);
            } else {
               int presentYear;
               if (!currentPage.equals("5")) {
                  if (currentPage.equals("6")) {
                     try {
                        this.buildEvaluationParameters(sessionId);
                     } catch (Exception var51) {
                     }

                     session.setAttribute("sessionVal", sessionId);
                     page = pagePrefix + "evaluationupload_edit.html";
                  } else if (currentPage.equals("7")) {
                     try {
                        session.setAttribute("FWMessageNotUploadedStart", "<!--");
                        session.setAttribute("FWMessageNotUploadedEnd", "-->");
                        session.setAttribute("FWFileNotFoundStart", "<!--");
                        session.setAttribute("FWFileNotFoundEnd", "-->");
                        LogManager.log("file uploading:----" + session.getAttribute("FWFileNotFound"));
                        this.buildResumeParameters(sessionId);
                        if (session.getAttribute("FWNotUploaded").toString().equals("1")) {
                           session.setAttribute("FWMessageNotUploadedStart", "");
                           session.setAttribute("FWMessageNotUploadedEnd", "");
                        } else {
                           session.setAttribute("FWMessageNotUploadedStart", "<!--");
                           session.setAttribute("FWMessageNotUploadedEnd", "-->");
                        }

                        if (session.getAttribute("FWFileNotFound") != null) {
                           if (session.getAttribute("FWFileNotFound").toString().equals("1")) {
                              session.setAttribute("FWFileNotFoundStart", "");
                              session.setAttribute("FWFileNotFoundEnd", "");
                              session.removeAttribute("FWNotUploaded");
                           } else {
                              session.setAttribute("FWFileNotFoundStart", "<!--");
                              session.setAttribute("FWFileNotFoundEnd", "-->");
                           }
                        } else {
                           session.setAttribute("FWFileNotFoundStart", "<!--");
                           session.setAttribute("FWFileNotFoundEnd", "-->");
                        }
                     } catch (Exception var50) {
                        session.setAttribute("FWMessageNotFoundStart", "<!--");
                        session.setAttribute("FWMessageNotFoundEnd", "-->");
                     }

                     page = pagePrefix + "resume_upload.html";
                  } else if (currentPage.equals("8")) {
                     LogManager.log("comming to the sequence 8");

                     try {
                        session.setAttribute("FWOldWageHistory", "");
                        if (session.getAttribute("FWYearSelected") != null) {
                           presentYear = Integer.parseInt(String.valueOf(dt.getYear())) + 1900;
                           LogManager.log("year Selected Is:" + session.getAttribute("FWYearSelected"));
                           session.getAttribute("FWYearSelected").equals(String.valueOf(presentYear));
                        }

                        if (session.getAttribute("FWArch").toString().equals("1")) {
                           queryName = "SelectYears";
                           session.setAttribute("FWYearsList", this.buildYearList(sessionId, queryName, session.getAttribute("FWYearSelected").toString()));
                           session.setAttribute("FWOldWageHistory", this.empSalaryAudit(sessionId));
                        } else if (session.getAttribute("FWArch").toString().equals("2")) {
                           queryName = "SelectYears_SpotBonus";
                           session.setAttribute("FWYearsList", this.buildYearList(sessionId, queryName, session.getAttribute("FWYearSelected").toString()));
                           session.setAttribute("FWOldWageHistory", this.spotBonusCommisionAudit(sessionId));
                        } else if (session.getAttribute("FWArch").toString().equals("3")) {
                           queryName = "selectEvaluationYearList";
                           session.setAttribute("FWYearsList", this.buildYearList(sessionId, queryName, session.getAttribute("FWYearSelected").toString()));
                           session.setAttribute("FWOldWageHistory", this.buildEvaluationParameters(sessionId));
                        } else if (session.getAttribute("FWArch").toString().equals("4")) {
                           queryName = "SelectHelmetDetailYears";
                           session.setAttribute("FWYearsList", this.buildYearList(sessionId, queryName, session.getAttribute("FWYearSelected").toString()));
                           session.setAttribute("FWOldWageHistory", this.buildHelmetAwardsDetails(sessionId));
                        } else if (session.getAttribute("FWArch").toString().equals("5")) {
                           queryName = "SelectTuitionArchive";
                           session.setAttribute("FWYearsList", this.buildYearList(sessionId, queryName, session.getAttribute("FWYearSelected").toString()));
                           session.setAttribute("FWOldWageHistory", this.buildTuitionAndReimbursementDetails(sessionId));
                        } else if (session.getAttribute("FWArch").toString().equals("6")) {
                           try {
                              queryName = "SelectIvpAreaArchive";
                              session.setAttribute("FWYearsList", this.buildYearList(sessionId, queryName, session.getAttribute("FWYearSelected").toString()));
                              session.setAttribute("FWOldWageHistory", this.buildIvpAreaDetails(sessionId, true));
                           } catch (Exception var48) {
                              LogManager.log("IVP Exception :" + var48);
                           }
                        } else if (session.getAttribute("FWArch").toString().equals("7")) {
                           queryName = "SelectFlextimeArchive";
                           session.setAttribute("FWYearsList", this.buildYearList(sessionId, queryName, session.getAttribute("FWYearSelected").toString()));
                           if (session.getAttribute("FWYearsList").toString().length() > 0) {
                              session.setAttribute("FWOldWageHistory", this.buildFlexTimeHours(sessionId));
                           } else {
                              session.setAttribute("FWOldWageHistory", this.BuildNoArchiveData(session.getAttribute("FWArch").toString()));
                           }
                        } else if (session.getAttribute("FWArch").toString().equals("8")) {
                           queryName = "SelectBanktimeArchive";
                           session.setAttribute("FWYearsList", this.buildYearList(sessionId, queryName, session.getAttribute("FWYearSelected").toString()));
                           LogManager.log("Length of  year List" + session.getAttribute("FWYearsList").toString().length());
                           if (session.getAttribute("FWYearsList").toString().length() > 0) {
                              session.setAttribute("FWYearLength", "1");
                              session.setAttribute("FWOldWageHistory", this.buildBankTime(sessionId));
                           } else {
                              session.setAttribute("FWYearLength", "0");
                              session.setAttribute("FWOldWageHistory", this.BuildNoArchiveData(session.getAttribute("FWArch").toString()));
                           }
                        }
                     } catch (Exception var49) {
                        LogManager.log("Exception Raised @ 8 Page:" + var49);
                     }

                     page = pagePrefix + "employeeArchiveData.html";
                  } else if (currentPage.equals("12")) {
                     page = pagePrefix + "unique_alert";
                  } else if (currentPage.equals("10")) {
                     page = pagePrefix + "server_notresp.html";
                  } else {
                     page = pagePrefix + "error.html";
                  }
               } else {
                  LogManager.log("comming to the sequence 5");
                  presentYear = Integer.parseInt(String.valueOf(dt.getYear())) + 1900;
                  session.removeAttribute("FWOldWageHistory");
                  session.removeAttribute("FWYearsList");
                  session.setAttribute("FWYearSelected", String.valueOf(presentYear));
                  if (fromTimeLogger != null) {
                     this.buildEditEmpPage(sessionId);
                  }

                  if (session.getAttribute("FWDelSno") != null) {
                     this.deleteSelectedEntry(sessionId);
                  }

                  if (session.getAttribute("FWDelSno_Tuition") != null) {
                     this.deletedSelectedTutionEntry(sessionId);
                  }

                  if (session.getAttribute("FWEvaluation_Sno") != null) {
                     this.deleteSelectEvaluation(sessionId);
                  }

                  if (session.getAttribute("FWHelmet_Sno") != null) {
                     this.deleteSelectHelmetAwards(sessionId);
                  }

                  LogManager.log("FWEmail is....................." + session.getAttribute("FWEmail"));
                  this.buildHRMInfoPage(sessionId);
                  if (session.getAttribute("EditMode").toString().equals("Insert")) {
                     session.setAttribute("FWBackButtonStart", "<!--");
                     session.setAttribute("FWBackButtonEnd", "-->");
                  } else {
                     session.removeAttribute("FWBackButtonStart");
                     session.removeAttribute("FWBackButtonEnd");
                  }

                  if (!SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") && !this.hrmEditUsers.contains(session.getUser().toString()) && !this.hrmDataEntryUsers.contains(session.getUser().toString())) {
                     if (session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString())) {
                        session.setAttribute("FWShowDelete", "true");
                     } else {
                        session.setAttribute("FWShowDelete", "false");
                     }

                     if (this.hrmHelmetAwardEditUsers.contains(session.getUser().toString())) {
                        try {
                           if (SecurityManager.getUser(session.getAttribute("FWEmail").toString()).toString().equals(session.getUser().toString())) {
                              session.setAttribute("FWDisplayHelmet", "0");
                           } else {
                              session.setAttribute("FWDisplayHelmet", "1");
                           }
                        } catch (Exception var54) {
                           session.setAttribute("FWDisplayHelmet", "1");
                        }
                     }

                     LogManager.log(" Else Part  ");
                     if (!this.hrmHelmetAwardEditUsers.contains(session.getUser().toString()) && !session.getUser().getEmployeeId().toString().equals("82")) {
                        session.setAttribute("HelmetEditUsersStart", "<!--");
                        session.setAttribute("HelmetEditUsersEnd", "-->");
                     } else {
                        session.setAttribute("HelmetEditUsersStart", "");
                        session.setAttribute("HelmetEditUsersEnd", "");
                        session.removeAttribute("FWSubmitButtonStart");
                        session.removeAttribute("FWSubmitButtonEnd");
                        session.setAttribute("NormalUserViewStart", "<!--");
                        session.setAttribute("NormalUserViewEnd", "-->");

                        try {
                           if (!SecurityManager.getUser(session.getAttribute("FWEmail").toString()).toString().equals(session.getUser().toString())) {
                              session.setAttribute("NormalUserViewStart", "<!--");
                              session.setAttribute("NormalUserViewEnd", "-->");
                              session.setAttribute("FWHelmetAddTableStart", " <!-- ");
                              session.setAttribute("FWHelmetAddTableEnd", " --> ");
                           }
                        } catch (Exception var53) {
                        }
                     }

                     if (!this.hrm_ViewAccess.contains(session.getUser().toString()) && !SecurityManager.isMember("People Manager", session.getUser()) && !SecurityManager.isMember("TopLineManager", session.getUser())) {
                        session.setAttribute("FWDisplayHelmet", "0");
                     } else {
                        try {
                           if (SecurityManager.getUser(session.getAttribute("FWEmail").toString()).toString().equals(session.getUser().toString())) {
                              session.setAttribute("FWDisplayHelmet", "0");
                              session.removeAttribute("FWSubmitButtonStart");
                              session.removeAttribute("FWSubmitButtonEnd");
                           } else {
                              session.setAttribute("FWDisplayHelmet", "1");
                           }
                        } catch (Exception var52) {
                           session.setAttribute("FWDisplayHelmet", "1");
                        }
                     }

                     page = pagePrefix + "hrminfo_user.html";
                  } else {
                     LogManager.log("Comming to 5th seq");
                     if ((this.hrmHelmetAwardEditUsers.contains(session.getUser().toString()) || SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") || this.hrmEditUsers.contains(session.getUser().toString()) || this.hrmDataEntryUsers.contains(session.getUser().toString())) && !session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString())) {
                        session.removeAttribute("HelmetEditUsersStart");
                        session.removeAttribute("HelmetEditUsersEnd");
                        session.removeAttribute("FWSubmitButtonStart");
                        session.removeAttribute("FWSubmitButtonEnd");
                        LogManager.log("Comming to 5th seq if");
                     } else {
                        LogManager.log("Comming to 5th seq else");
                        session.setAttribute("HelmetEditUsersStart", "<!--");
                        session.setAttribute("HelmetEditUsersEnd", "-->");
                        if (session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString())) {
                           session.removeAttribute("HelmetEditUsersStart");
                           session.removeAttribute("HelmetEditUsersEnd");
                           session.setAttribute("FWHelmetAddTableStart", " <!-- ");
                           session.setAttribute("FWHelmetAddTableEnd", " --> ");
                        }
                     }

                     page = pagePrefix + "hrminfo_edit.html";
                  }
               }
            }
         } else {
            if (session.getAttribute("FWShowAllEmp") != null) {
               session.removeAttribute("searchIds");
               session.removeAttribute("FWSearchIds");
               session.removeAttribute("FWShowAllEmp");
               if (session.getAttribute("searchIds") != null) {
                  this.showAllEmployeesButton(sessionId, true);
               } else {
                  this.showAllEmployeesButton(sessionId, false);
               }

               LogManager.log("Comming and removing the session");
            } else if (session.getAttribute("searchIds") == null && session.getAttribute("FWSearchIds") == null) {
               this.showAllEmployeesButton(sessionId, false);
            } else {
               this.showAllEmployeesButton(sessionId, true);
            }

            session.setAttribute("FWCurrentPage", "0");
            this.buildEmpTablePage(sessionId);
            queryName = System.getProperty("europa.application.id").toString();
            LogManager.log("applicationID" + queryName);
            if ((SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") || this.hrmWagesPermsFullAccessUsers.contains(session.getUser().toString())) && !queryName.equals("2")) {
               this.showHrmWagesPermissionButton(sessionId, true);
            } else {
               this.showHrmWagesPermissionButton(sessionId, false);
            }

            if ((SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") || this.hrmSearchScreenFullAccessUsers.contains(session.getUser().toString())) && queryName.equals("2") && !SecurityManager.isMember("People Manager", session.getUser()) && SecurityManager.isMember("TopLineManager", session.getUser())) {
               this.showEmployeeSearchButton(sessionId, false);
            } else {
               this.showEmployeeSearchButton(sessionId, true);
            }

            if ((SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") || this.hrmMasterFullAccessUsers.contains(session.getUser().toString())) && !queryName.equals("2")) {
               this.showHRMMasterScreenButton(sessionId, true);
            } else {
               this.showHRMMasterScreenButton(sessionId, false);
            }

            if (queryName.equals("2")) {
               session.setAttribute("FWShowHrmRunReportStart", "<!--");
               session.setAttribute("FWShowHrmRunReportEnd", "-->");
            } else {
               session.setAttribute("FWShowHrmRunReportStart", " ");
               session.setAttribute("FWShowHrmRunReportEnd", " ");
            }

            if (SecurityManager.getUser(session.getUser().toString()).toString().equals("admin")) {
               session.setAttribute("FWShowAccountJobTitleStart", "");
               session.setAttribute("FWShowAccountJobTitleEnd", "");
            } else {
               session.setAttribute("FWShowAccountJobTitleStart", "<!--");
               session.setAttribute("FWShowAccountJobTitleEnd", "-->");
            }

            page = pagePrefix + "employee_list.html";
         }

         displayLogic.showPage(sessionId, page);
      } else {
         success = this.validateFields(sessionId);
         LogManager.log("Line 169 " + success);
         if (success) {
            success = this.performTransaction(sessionId);
            LogManager.log("Perform option:" + success);
            if (success) {
               session.removeAttribute("FWNotUniqueId");
               session.setAttribute("Redirect", "true");
               LogManager.log("comming here completion of Employee Screen:" + session.getAttribute("FWCurrentPage") + session.getAttribute("FWCurrentPage").equals("2"));
               if (session.getAttribute("FWCurrentPage").equals("1")) {
                  session.removeAttribute("FWSubmit");
                  session.setAttribute("Destination", "admin_employee&FWCurrentPage=4");
               } else if (session.getAttribute("FWCurrentPage").equals("4")) {
                  session.removeAttribute("FWSubmit");
                  session.setAttribute("Destination", "admin_employee&FWCurrentPage=2");
               } else if (session.getAttribute("FWCurrentPage").equals("3")) {
                  session.removeAttribute("FWSubmit");
                  session.setAttribute("Destination", "admin_employee&FWCurrentPage=2");
               } else if (session.getAttribute("FWCurrentPage").equals("5")) {
                  session.removeAttribute("FWSubmit");
                  session.setAttribute("Destination", "admin_employee&FWCurrentPage=2");
               } else {
                  session.setAttribute("Destination", "admin_employee&FWRetry=true");
               }
            } else {
               LogManager.log("Perform false : " + currentPage);
               if (currentPage.equals("4")) {
                  session.setAttribute("FWError", "1");
                  page = pagePrefix + "employeePrivacyDetails.html";
                  displayLogic.showPage(sessionId, page);
                  session.removeAttribute("FWError");
               } else if (!currentPage.equals("1") && !currentPage.equals("2")) {
                  LogManager.log("duplicate 2");
                  session.setAttribute("Destination", "admin_employee&FWRetry=true");
               } else if (session.getAttribute("NotRegister") != null && session.getAttribute("NotRegister").toString().equals("true")) {
                  session.setAttribute("Destination", "admin_employee&FWCurrentPage=9");
                  page = pagePrefix + "employee_NonRegister.html";
                  displayLogic.showPage(sessionId, page);
               } else if (session.getAttribute("FWSessionworng") != null && session.getAttribute("FWSessionworng").toString().equals("true")) {
                  LogManager.log("duplicate 0");
                  session.setAttribute("Destination", "admin_employee&FWCurrentPage=10");
                  page = pagePrefix + "server_notresp.html";
                  displayLogic.showPage(sessionId, page);
               } else {
                  session.setAttribute("Redirect", "true");
                  session.removeAttribute("FWSubmit");
                  session.setAttribute("FWDuplicateEmp", "true");
                  if (currentPage.equals("1")) {
                     LogManager.log("duplicate Insert Mode");
                     session.setAttribute("Destination", "admin_employee&FWRetry=false&FWCurrentPage=1");
                  } else {
                     LogManager.log("duplicate Update Mode");
                     session.setAttribute("Destination", "admin_employee&FWRetry=false&FWCurrentPage=2");
                  }
               }
            }
         } else {
            this.showErrors(sessionId);
         }
      }

   }

   public boolean performTransaction(String sessionId) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.performTransaction()");
      Session session = SessionManager.findSession(sessionId);
      if (!session.getId().equals(sessionId)) {
         session.setAttribute("FWSessionworng", "true");
         return false;
      } else {
         SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
         String currentPage = (String)session.getAttribute("FWCurrentPage");
         int action = Integer.parseInt(currentPage);
         DataLogic dataLogic = this.module.getDataLogic();
         Connection conn = dataLogic.openConnection();
         JasperserverConnector jsc = new JasperserverConnector();
         SqlStatement sql = null;
         SqlStatement sqlvalid = null;
         Vector results = null;
         PreparedSqlStatement psActivityCodeData = new PreparedSqlStatement();
         PreparedStatement pquery1 = null;
         Vector serchResults = null;
         StringTokenizer st = null;
         this.UpdateTerminationonJasper(sessionId);
         String ApplicationID = System.getProperty("europa.application.id").toString();
         String jasperEmail = (String)session.getAttribute("FWOldEmail");
         LogManager.log("Email....." + jasperEmail);
         SimpleDateFormat parser12 = new SimpleDateFormat("MM/dd/yy");

         try {
            session.setAttribute("FWLastUpdated", String.valueOf(formater.format(parser12.parse(session.getAttribute("FWHireDate").toString()))));
         } catch (Exception var646) {
         }

         try {
            session.setAttribute("FWCostEffectiveFrom", String.valueOf(formater.format(parser12.parse(session.getAttribute("FWHireDate").toString()))));
         } catch (Exception var645) {
         }

         session.setAttribute("FWDuplicateEmp", "false");
         session.setAttribute("FWEffectiveStartDate", "<!--");
         session.setAttribute("FWEffectiveEndDate", "-->");
         LogManager.log("New Hire Date1 :" + session.getAttribute("FWHireDate"));
         session.setAttribute("FWInitHireDate", session.getAttribute("FWHireDate").toString());
         CallableStatement cs;
         String isRehire;
         String isChk;
         String isEngAdmin;
         String isPowerSystem;
         CLOB newClob;
         String isSE;
         String isVer;
         boolean sSEQueryExecute;
         String mode;
         String isConDirect;
         String isPL;
         String isAnv;
         String isElv;
         String isMfv;
         label11025:
         switch (action) {
            case 1:
            case 2:
               try {
                  String isSpecialEmployee;
                  try {
                     isSpecialEmployee = (String)session.getAttribute("FWHireDate");
                     if (isSpecialEmployee != null && !isSpecialEmployee.equals("")) {
                        session.setAttribute("FWHireDate", formater.format(this.DateFormatFunction(sessionId, isSpecialEmployee)));
                     }

                     isRehire = (String)session.getAttribute("FWTermDate");
                     if (isRehire != null && !isRehire.equals("")) {
                        session.setAttribute("FWTermDate", formater.format(this.DateFormatFunction(sessionId, isRehire)));
                     }

                     isConDirect = (String)session.getAttribute("FWEffectiveFrom");
                     if (isConDirect != null && !isConDirect.equals("")) {
                        session.setAttribute("FWEffectiveFrom", formater.format(this.DateFormatFunction(sessionId, isConDirect)));
                     }

                     isPL = (String)session.getAttribute("FWRehireDate");
                     if (isPL != null && !isPL.equals("")) {
                        session.setAttribute("FWRehireDate", formater.format(this.DateFormatFunction(sessionId, isPL)));
                     }

                     isChk = (String)session.getAttribute("FWOldTermDate");
                     if (isChk != null && !isChk.equals("")) {
                        session.setAttribute("FWOldTermDate", formater.format(this.DateFormatFunction(sessionId, isChk)));
                     }

                     isEngAdmin = (String)session.getAttribute("FWConDirectHireDate");
                     if (isEngAdmin != null && !isEngAdmin.equals("")) {
                        session.setAttribute("FWConDirectHireDate", formater.format(this.DateFormatFunction(sessionId, isEngAdmin)));
                     }

                     isPowerSystem = (String)session.getAttribute("FWServiceDate");
                     if (isPowerSystem != null && !isPowerSystem.equals("")) {
                        session.setAttribute("FWServiceDate", formater.format(this.DateFormatFunction(sessionId, isPowerSystem)));
                     }
                  } catch (ParseException var655) {
                     LogManager.log(var655.toString());
                     throw new InvalidDataFormatException(var655.toString());
                  }

                  LogManager.log("New Hire Date2 :" + session.getAttribute("FWHireDate"));
                  isSpecialEmployee = (String)session.getAttribute("FWisSpecialEmployee");
                  if (isSpecialEmployee != null && isSpecialEmployee.equals("on")) {
                     session.setAttribute("FWisSpecialEmployee", "1");
                  } else {
                     session.setAttribute("FWisSpecialEmployee", "0");
                  }

                  isRehire = (String)session.getAttribute("FWIsRehire");
                  if (isRehire != null && isRehire.equals("on")) {
                     session.setAttribute("FWIsRehire", "1");
                  } else {
                     session.setAttribute("FWIsRehire", "0");
                  }

                  isConDirect = (String)session.getAttribute("FWIsConDirect");
                  if (isConDirect != null && isConDirect.equals("on")) {
                     session.setAttribute("FWIsConDirect", "1");
                  } else {
                     session.setAttribute("FWIsConDirect", "0");
                  }

                  isPL = (String)session.getAttribute("FWisProjectLeader");
                  LogManager.log("isPM=" + isPL);
                  if (isPL != null) {
                     if (isPL.equals("on")) {
                        session.setAttribute("FWisPL", "checked");
                        session.setAttribute("FWisProjectLeader", "1");
                     } else {
                        session.setAttribute("FWisPL", " ");
                        session.setAttribute("FWisProjectLeader", "0");
                     }
                  } else {
                     session.setAttribute("FWisPL", " ");
                     session.setAttribute("FWisProjectLeader", "0");
                  }

                  isChk = (String)session.getAttribute("FWisChecker");
                  LogManager.log("isPM=" + isPL);
                  if (isChk != null) {
                     if (isChk.equals("on")) {
                        session.setAttribute("FWisChk", "checked");
                        session.setAttribute("FWisChecker", "1");
                     } else {
                        session.setAttribute("FWisChk", " ");
                        session.setAttribute("FWisChecker", "0");
                     }
                  } else {
                     session.setAttribute("FWisChk", " ");
                     session.setAttribute("FWisChecker", "0");
                  }

                  isEngAdmin = (String)session.getAttribute("FWisEngAdmin");
                  LogManager.log("isEngAdmin=" + isEngAdmin);
                  if (isEngAdmin != null) {
                     if (isEngAdmin.equals("on")) {
                        session.setAttribute("FWisEAdmn", "checked");
                        session.setAttribute("FWisEngAdmin", "1");
                     } else {
                        session.setAttribute("FWisEAdmn", " ");
                        session.setAttribute("FWisEngAdmin", "0");
                     }
                  } else {
                     session.setAttribute("FWisEAdmn", " ");
                     session.setAttribute("FWisEngAdmin", "0");
                  }

                  isPowerSystem = (String)session.getAttribute("FWisPowerSystem");
                  LogManager.log("isPowerSystem=" + isPowerSystem);
                  if (isPowerSystem != null) {
                     if (isPowerSystem.equals("on")) {
                        session.setAttribute("FWisPSystem", "checked");
                        session.setAttribute("FWisPowerSystem", "1");
                     } else {
                        session.setAttribute("FWisPSystem", " ");
                        session.setAttribute("FWisPowerSystem", "0");
                     }
                  } else {
                     session.setAttribute("FWisPSystem", " ");
                     session.setAttribute("FWisPowerSystem", "0");
                  }

                  String isLeadEng = (String)session.getAttribute("FWisLeadEng");
                  LogManager.log("isLeadEng=" + isLeadEng);
                  if (isLeadEng != null) {
                     if (isLeadEng.equals("on")) {
                        session.setAttribute("FWisLEng", "checked");
                        session.setAttribute("FWisLeadEng", "1");
                     } else {
                        session.setAttribute("FWisLEng", " ");
                        session.setAttribute("FWisLeadEng", "0");
                     }
                  } else {
                     session.setAttribute("FWisLEng", " ");
                     session.setAttribute("FWisLeadEng", "0");
                  }

                  String isEngManager = (String)session.getAttribute("FWisEngManager");
                  LogManager.log("isEngManager=" + isEngManager);
                  if (isEngManager != null) {
                     if (isEngManager.equals("on")) {
                        session.setAttribute("FWisEMgr", "checked");
                        session.setAttribute("FWisEngManager", "1");
                     } else {
                        session.setAttribute("FWisEMgr", " ");
                        session.setAttribute("FWisEngManager", "0");
                     }
                  } else {
                     session.setAttribute("FWisEMgr", " ");
                     session.setAttribute("FWisEngManager", "0");
                  }

                  String isContractCostAssigned = (String)session.getAttribute("FWContractorcost");
                  LogManager.log("isContractCostAssigned=" + isContractCostAssigned);
                  LogManager.log("FWConcost=" + (String)session.getAttribute("FWConcost"));
                  if (isContractCostAssigned != null) {
                     if (isContractCostAssigned.equals("on")) {
                        session.setAttribute("FWContractorcostvalue", "1");
                     } else {
                        session.setAttribute("FWContractorcostvalue", "0");
                     }
                  } else if (session.getAttribute("FWConcost").equals("checked")) {
                     session.setAttribute("FWContractorcostvalue", "1");
                  } else {
                     session.setAttribute("FWContractorcostvalue", "0");
                  }

                  String isPeopleManager = (String)session.getAttribute("FWisPeopleManager");
                  LogManager.log("isPeopleManager=" + isEngAdmin);
                  if (isPeopleManager != null) {
                     if (isPeopleManager.equals("on")) {
                        session.setAttribute("FWisPManager", "checked");
                        session.setAttribute("FWisPeopleManager", "1");
                     } else {
                        session.setAttribute("FWisPManager", " ");
                        session.setAttribute("FWisPeopleManager", "0");
                     }
                  } else {
                     session.setAttribute("FWisPManager", " ");
                     session.setAttribute("FWisPeopleManager", "0");
                  }

                  LogManager.log("People manager list :" + (String)session.getAttribute("FWPeopleManagerList"));
                  if (session.getAttribute("FWPeopleManagerList") != null) {
                     session.setAttribute("FWPeopleManagerID", (String)session.getAttribute("FWPeopleManagerList"));
                  }

                  if (session.getAttribute("FWTopLineManagerIdList") != null) {
                     session.setAttribute("FWTopLineManagerId", (String)session.getAttribute("FWTopLineManagerIdList"));
                  }

                  String isAdminGroup = (String)session.getAttribute("FWisAdminGroup");
                  LogManager.log("FWisAdminGroup=" + isAdminGroup);
                  if (isAdminGroup != null) {
                     if (isAdminGroup.equals("on")) {
                        session.setAttribute("FWisAdminGP", "checked");
                        session.setAttribute("FWisAdminGroup1", "1");
                     } else {
                        session.setAttribute("FWisAdminGP", " ");
                        session.setAttribute("FWisAdminGroup1", "0");
                     }
                  } else {
                     session.setAttribute("FWisAdminGP", " ");
                     session.setAttribute("FWisAdminGroup1", "0");
                  }

                  String isAM = (String)session.getAttribute("FWisAccountManager");
                  LogManager.log("isAM=" + isAM);
                  if (isAM != null) {
                     if (isAM.equals("on")) {
                        session.setAttribute("FWisAM", "checked");
                        session.setAttribute("FWisAccountManager", "1");
                     } else {
                        session.setAttribute("FWisAM", " ");
                        session.setAttribute("FWisAccountManager", "0");
                     }
                  } else {
                     session.setAttribute("FWisAM", " ");
                     session.setAttribute("FWisAccountManager", "0");
                  }

                  String isCompEast = (String)session.getAttribute("FWisDesignAndAnalysCompEast");
                  LogManager.log("isCOMPEAST1111111111:" + isCompEast);
                  if (isCompEast != null) {
                     if (isCompEast.equals("on")) {
                        session.setAttribute("FWisDesignAndAnalysCompEast", "1");
                     } else {
                        session.setAttribute("FWisDesignAndAnalysCompEast", "0");
                     }
                  } else {
                     session.setAttribute("FWisCOMPEAST", " ");
                     session.setAttribute("FWisDesignAndAnalysCompEast", "0");
                  }

                  String isCompWest = (String)session.getAttribute("FWisDesignAndAnalysCompWest");
                  LogManager.log("isCOMPWEST1111111:" + isCompWest);
                  if (isCompWest != null) {
                     if (isCompWest.equalsIgnoreCase("on")) {
                        session.setAttribute("FWisDesignAndAnalysCompWest", "1");
                     } else {
                        session.setAttribute("FWisDesignAndAnalysCompWest", "0");
                     }
                  } else {
                     session.setAttribute("FWisDesignAndAnalysCompWest", "0");
                  }

                  String topLineManager = (String)session.getAttribute("FWisTopLineManager");
                  LogManager.log("topLineManager:" + topLineManager);
                  if (topLineManager != null) {
                     if (topLineManager.equals("on")) {
                        session.setAttribute("FWisTLManager", "checked");
                        session.setAttribute("FWisTopLineManager", "1");
                        LogManager.log("Hiiiiiiiiiii");
                     } else {
                        session.setAttribute("FWisTLManager", " ");
                        session.setAttribute("FWisTopLineManager", "0");
                     }
                  } else {
                     session.setAttribute("FWisTLManager", " ");
                     session.setAttribute("FWisTopLineManager", "0");
                  }

                  isSE = (String)session.getAttribute("FWisSalesEngineer");
                  LogManager.log("isSE=" + isSE);
                  if (isSE != null) {
                     if (isSE.equals("on")) {
                        session.setAttribute("FWisSE", "checked");
                        session.setAttribute("FWisSalesEngineer", "1");
                     } else {
                        session.setAttribute("FWisSE", " ");
                        session.setAttribute("FWisSalesEngineer", "0");
                     }
                  } else {
                     session.setAttribute("FWisSE", " ");
                     session.setAttribute("FWisSalesEngineer", "0");
                  }

                  String isDR = (String)session.getAttribute("FWisDirectRep");
                  LogManager.log("isDR=" + isDR);
                  if (isDR != null) {
                     if (isDR.equals("on")) {
                        session.setAttribute("FWisDR", "checked");
                        session.setAttribute("FWisDirectRep", "1");
                     } else {
                        session.setAttribute("FWisDR", " ");
                        session.setAttribute("FWisDirectRep", "0");
                     }
                  } else {
                     session.setAttribute("FWisDR", " ");
                     session.setAttribute("FWisDirectRep", "0");
                  }

                  String isSSE = (String)session.getAttribute("FWisSrSalesEngineer");
                  LogManager.log("isSSE=" + isSSE);
                  isVer = (String)session.getAttribute("FWisVertical");
                  LogManager.log("isVer=" + isVer);
                  if (isVer != null) {
                     if (isVer.equals("on")) {
                        session.setAttribute("FWisVer", "checked");
                        session.setAttribute("FWisVertical", "1");
                     } else {
                        session.setAttribute("FWisVer", " ");
                        session.setAttribute("FWisVertical", "0");
                     }
                  } else {
                     session.setAttribute("FWisVer", " ");
                     session.setAttribute("FWisVertical", "0");
                  }

                  String isResourceManager = (String)session.getAttribute("FWisResourceManager");
                  SqlStatement sqlstmt = null;
                  boolean resourceManagerFlag = false;
                  if (isResourceManager != null && isResourceManager.equals("on")) {
                     resourceManagerFlag = true;
                     session.setAttribute("FWisResourceManager", "1");
                  } else {
                     session.setAttribute("FWisResourceManager", "0");
                  }

                  String isOffshoreManager = (String)session.getAttribute("FWisOffshoreManager");
                  if (isOffshoreManager != null && isOffshoreManager.equals("on")) {
                     session.setAttribute("FWisOffShoerMgr", "checked");
                     session.setAttribute("FWisOffshoreManager", "1");
                  } else {
                     session.setAttribute("FWisOffShoerMgr", " ");
                     session.setAttribute("FWisOffshoreManager", "0");
                  }

                  String isElectricalSystemsIntegration = (String)session.getAttribute("FWisElectricalSystemsIntegration");
                  if (isElectricalSystemsIntegration != null && isElectricalSystemsIntegration.equals("on")) {
                     session.setAttribute("FWisElectricalSysIntegration", "checked");
                     session.setAttribute("FWisElectricalSystemsIntegration", "1");
                  } else {
                     session.setAttribute("FWisElectricalSysIntegration", " ");
                     session.setAttribute("FWisElectricalSystemsIntegration", "0");
                  }

                  if (session.getAttribute("FWEmployeeID") == null) {
                     session.setAttribute("FWEmployeeID", "0");
                  }

                  String modeOfScr = (String)session.getAttribute("EditMode");
                  Vector sSEserchResults = null;
                  if (!modeOfScr.equals("Insert")) {
                     sqlvalid = dataLogic.buildSQLString(sessionId, "SelectJpRow");
                     dataLogic.startTransaction(conn);

                     try {
                        serchResults = dataLogic.execute(conn, sqlvalid);
                     } catch (Exception var644) {
                     }

                     sqlvalid = dataLogic.buildSQLString(sessionId, "SelectSSERow");
                     dataLogic.startTransaction(conn);

                     try {
                        sSEserchResults = dataLogic.execute(conn, sqlvalid);
                     } catch (Exception var643) {
                     }
                  }

                  if (modeOfScr.equals("Insert")) {
                     session.setAttribute("FWTotHelmets", "0");
                     session.setAttribute("FWHelmetAmount", "0");
                     sqlvalid = dataLogic.buildSQLString(sessionId, "InsertMaster_HelmetAwards");
                     dataLogic.startTransaction(conn);

                     try {
                        sSEserchResults = dataLogic.execute(conn, sqlvalid);
                     } catch (Exception var642) {
                     }

                     sqlvalid = dataLogic.buildSQLString(sessionId, "InsertFlextimeEntry");
                     dataLogic.startTransaction(conn);

                     try {
                        sSEserchResults = dataLogic.execute(conn, sqlvalid);
                     } catch (Exception var641) {
                     }
                  }

                  String jpMode = "";
                  String sSEMode = "";
                  boolean qryExecute = true;
                  sSEQueryExecute = true;
                  String isJpr = (String)session.getAttribute("FWisJrPartner");
                  isAnv = (String)session.getAttribute("FWAnalysisVertical");
                  isElv = (String)session.getAttribute("FWElecVertical");
                  isMfv = (String)session.getAttribute("FWManFactVertical");
                  LogManager.log("isJounier Partner=" + isJpr);
                  String tempGroupUser;
                  if (isJpr != null) {
                     if (isJpr.equals("on")) {
                        session.setAttribute("FWisJpr", "checked");
                        session.setAttribute("FWisJrPartner", "1");
                        mode = (String)session.getAttribute("FWisAutoAllocation");
                        if (session.getAttribute("FWisAutoAllocation") != null) {
                           LogManager.log("isAutoAllocation IS:" + mode);
                           if (mode.equals("on")) {
                              session.setAttribute("FWisAutoAllocation1", "checked");
                              session.setAttribute("FWisAutoAllocation", "1");
                           }
                        } else {
                           session.setAttribute("FWisAutoAllocation", "0");
                        }

                        LogManager.log("Commint upto here" + session.getAttribute("FWisAutoAllocation"));
                        LogManager.log("log of allocated" + session.getAttribute("FWJpAllocatedHours"));
                        if (session.getAttribute("FWJpAllocatedHours") == null) {
                           session.setAttribute("FWJpAllocatedHours", "0");
                        }

                        tempGroupUser = (String)session.getAttribute("FWEmployeeID");
                        LogManager.log("EmployeeID:" + tempGroupUser);
                        if (!modeOfScr.equals("Insert")) {
                           if (serchResults.isEmpty()) {
                              jpMode = "Insert";
                           } else {
                              jpMode = "Update";
                           }
                        }

                        LogManager.log("isAutoAllocation IS:" + mode);
                     } else {
                        if (!modeOfScr.equals("Insert")) {
                           if (serchResults.isEmpty()) {
                              qryExecute = false;
                           } else {
                              jpMode = "Delete";
                           }
                        }

                        session.setAttribute("FWisJpr", " ");
                        session.setAttribute("FWisJrPartner", "0");
                     }
                  } else {
                     if (!modeOfScr.equals("Insert")) {
                        if (serchResults.isEmpty()) {
                           qryExecute = false;
                        } else {
                           jpMode = "Delete";
                        }
                     }

                     session.setAttribute("FWisJpr", " ");
                     session.setAttribute("FWisJrPartner", "0");
                  }

                  LogManager.log("isSSE Partner=" + isSSE);
                  LogManager.log("Serch Results:" + sSEserchResults);
                  if (isSSE != null) {
                     if (isSSE.equals("on")) {
                        session.setAttribute("FWisSSE", "checked");
                        session.setAttribute("FWisSrSalesEngineer", "1");
                        mode = (String)session.getAttribute("FWisSSEAutoAllocation");
                        if (session.getAttribute("FWisSSEAutoAllocation") != null) {
                           LogManager.log("FWisSSEAutoAllocation IS:" + mode);
                           if (mode.equals("on")) {
                              session.setAttribute("FWisSSEAutoAllocation1", "checked");
                              session.setAttribute("FWisSSEAutoAllocation", "1");
                           }
                        } else {
                           session.setAttribute("FWisSSEAutoAllocation", "0");
                        }

                        LogManager.log("Commint upto here" + session.getAttribute("FWisSSEAutoAllocation"));
                        LogManager.log("log of allocated" + session.getAttribute("FWSSEAllocatedHours"));
                        if (session.getAttribute("FWSSEAllocatedHours") == null) {
                           session.setAttribute("FWSSEAllocatedHours", "0");
                        }

                        tempGroupUser = (String)session.getAttribute("FWEmployeeID");
                        LogManager.log("EmployeeID:" + tempGroupUser);
                        if (!modeOfScr.equals("Insert")) {
                           if (sSEserchResults.isEmpty()) {
                              sSEMode = "Insert";
                           } else {
                              sSEMode = "Update";
                           }
                        }

                        LogManager.log("isAutoAllocation IS:" + mode);
                     } else {
                        if (!modeOfScr.equals("Insert")) {
                           if (sSEserchResults.isEmpty()) {
                              sSEQueryExecute = false;
                           } else {
                              sSEMode = "Delete";
                           }
                        }

                        session.setAttribute("FWisSSE", " ");
                        session.setAttribute("FWisSrSalesEngineer", "0");
                     }
                  } else {
                     if (!modeOfScr.equals("Insert")) {
                        if (sSEserchResults.isEmpty()) {
                           sSEQueryExecute = false;
                        } else {
                           sSEMode = "Delete";
                        }
                     }

                     session.setAttribute("FWisSSE", " ");
                     session.setAttribute("FWisSrSalesEngineer", "0");
                  }

                  if (isAnv != null) {
                     if (isAnv.equals("on")) {
                        session.setAttribute("FWisAnv", "checked");
                        session.setAttribute("FWAnalysisVertical", "1");
                     } else {
                        session.setAttribute("FWisAnv", " ");
                        session.setAttribute("FWAnalysisVertical", "0");
                     }
                  } else {
                     session.setAttribute("FWisAnv", " ");
                     session.setAttribute("FWAnalysisVertical", "0");
                  }

                  if (isElv != null) {
                     if (isElv.equals("on")) {
                        session.setAttribute("FWisElv", "checked");
                        session.setAttribute("FWElecVertical", "1");
                     } else {
                        session.setAttribute("FWisElv", " ");
                        session.setAttribute("FWElecVertical", "0");
                     }
                  } else {
                     session.setAttribute("FWisElv", " ");
                     session.setAttribute("FWElecVertical", "0");
                  }

                  if (isMfv != null) {
                     if (isMfv.equals("on")) {
                        session.setAttribute("FWisMfv", "checked");
                        session.setAttribute("FWManFactVertical", "1");
                     } else {
                        session.setAttribute("FWisMfv", " ");
                        session.setAttribute("FWManFactVertical", "0");
                     }
                  } else {
                     session.setAttribute("FWisMfv", " ");
                     session.setAttribute("FWManFactVertical", "0");
                  }

                  mode = (String)session.getAttribute("EditMode");
                  if (mode.equals("Insert")) {
                     session.setAttribute("FWDataFeedValue", "1");
                  }


                  User currentUser = null;
                  User checkUser = null;
                  boolean userValid = true;
                  boolean isemailchange = false;
                  LogManager.log("performTrans");
                  userValid = this.Checkvalidemail(sessionId, sqlvalid, dataLogic, conn, mode);
                  if (!userValid) {
                     return false;
                  }

                  String tempSSE;
                  String tempVER;
                  String tempJpr;
                  String tempAnv;
                  String tempElv;
                  String queryName;
                  try {
                     String OldFirstName;
                     String OldLastName;
                     String tempSE;
                     if (mode.equals("Insert")) {
                        Vector empidVect = null;
                        SqlStatement getEmpId = null;
                        OldFirstName = "";

                        try {
                           OldFirstName = session.getAttribute("FWEmail").toString();
                           checkUser = SecurityManager.getUser(OldFirstName);
                        } catch (Exception var640) {
                        }

                        if (checkUser == null && userValid) {
                           OldLastName = mode + "Employee";
                           sql = dataLogic.buildSQLString(sessionId, OldLastName);
                           dataLogic.startTransaction(conn);
                           dataLogic.execute(conn, sql);
                           getEmpId = dataLogic.buildSQLString(sessionId, "getEmpID");
                           dataLogic.startTransaction(conn);

                           try {
                              empidVect = dataLogic.execute(conn, getEmpId);
                           } catch (Exception var639) {
                           }

                           if (!empidVect.isEmpty()) {
                              Vector rows = (Vector)empidVect.get(0);
                              int empID = Integer.parseInt(rows.get(0).toString());
                              session.setAttribute("FWEmployeeID", String.valueOf(empID));
                           }

                           if (session.getAttribute("FWJobTitleID") != null) {
                              int jobTitleId = Integer.parseInt(session.getAttribute("FWJobTitleID").toString());
                              ModuleUtils.UpdateFixedBidProjectContractAmount(conn, session.getUser().getEmployeeId(), sessionId);
                              EmployeeDAO obj = new EmployeeDAO();
                              obj.updateScopedGPP(conn, jobTitleId);
                           }

                           if (session.getAttribute("FWSSEAllocatedHours") == null) {
                              session.setAttribute("FWSSEAllocatedHours", "0");
                           }

                           if (session.getAttribute("FWJpAllocatedHours") == null) {
                              session.setAttribute("FWJpAllocatedHours", "0");
                           }

                           if (session.getAttribute("FWisAutoAllocation") == null) {
                              session.setAttribute("FWisAutoAllocation", "0");
                           }

                           if (session.getAttribute("FWisSSEAutoAllocation") == null) {
                              session.setAttribute("FWisSSEAutoAllocation", "0");
                           }

                           queryName = "InsertJuniorPartners";
                           tempSE = "InsertSrSalesEngineers";
                           LogManager.log("Query Name:" + queryName + "  " + tempSE);
                           if (isJpr != null && qryExecute) {
                              sqlvalid = dataLogic.buildSQLString(sessionId, queryName);
                              dataLogic.startTransaction(conn);

                              try {
                                 dataLogic.execute(conn, sqlvalid);
                              } catch (Exception var638) {
                                 LogManager.log("Exececption while executing JP thrown" + var638);
                              }
                           }

                           if (isSSE != null && sSEQueryExecute) {
                              sqlvalid = dataLogic.buildSQLString(sessionId, tempSE);
                              dataLogic.startTransaction(conn);

                              try {
                                 dataLogic.execute(conn, sqlvalid);
                              } catch (Exception var637) {
                                 LogManager.log("Exececption while executing SSE thrown" + var637);
                              }
                           }
                        }
                     }

                     if (mode.equals("Update")) {
                        String NewFirstName = session.getAttribute("FWFirstName").toString();
                        String NewLastName = session.getAttribute("FWLastName").toString();
                        OldFirstName = session.getAttribute("FWOldFirstName").toString();
                        OldLastName = session.getAttribute("FWOldLastName").toString();
                        queryName = session.getAttribute("FWisProjectLeader").toString();
                        tempSE = session.getAttribute("FWisSalesEngineer").toString();
                        tempSSE = session.getAttribute("FWisSrSalesEngineer").toString();
                        tempVER = session.getAttribute("FWisVertical").toString();
                        tempJpr = session.getAttribute("FWisJrPartner").toString();
                        tempAnv = session.getAttribute("FWAnalysisVertical").toString();
                        tempElv = session.getAttribute("FWElecVertical").toString();
                        String tempMfv = session.getAttribute("FWManFactVertical").toString();
                        String tempChk = session.getAttribute("FWisChecker").toString();
                        String tempEAdmin = session.getAttribute("FWisEngAdmin").toString();
                        String tempPowerSystem = session.getAttribute("FWisPowerSystem").toString();
                        String tempLeadEng = session.getAttribute("FWisLeadEng").toString();
                        String tempEngManager = session.getAttribute("FWisEngManager").toString();
                        String tempBdv = session.getAttribute("FWisAccountManager").toString();
                        String userId = (String)session.getAttribute("FWEmail");
                        String oldEmail = (String)session.getAttribute("FWOldEmail");
                        String tempPM = (String)session.getAttribute("FWisPeopleManager");
                        String tempAdminGP = (String)session.getAttribute("FWisAdminGroup1");
                        String engManager = (String)session.getAttribute("FWEngineeringManager");
                        String mDACompEast = (String)session.getAttribute("FWisDesignAndAnalysCompEast");
                        String mDACompWest = (String)session.getAttribute("FWisDesignAndAnalysCompWest");
                        String tlManager = (String)session.getAttribute("FWisTopLineManager");
                        String tempResourceManager = (String)session.getAttribute("FWisResourceManager");
                        String tempOffshoreManager = (String)session.getAttribute("FWisOffshoreManager");
                        String tempElectricalSystemsIntegration = (String)session.getAttribute("FWisElectricalSystemsIntegration");
                        LogManager.log("engManager" + engManager);
                        LogManager.log("tempAdminGP :" + tempAdminGP);
                        boolean upFlag = false;
                        if (jpMode == "") {
                           jpMode = "Update";
                        }

                        if (sSEMode == "") {
                           sSEMode = "Update";
                        }

                        queryName = jpMode + "JuniorPartners";
                        String sSEQueryName = sSEMode + "SrSalesEngineers";
                        if (session.getAttribute("FWisCompanyFunctionDisplayed") != null && session.getAttribute("FWisCompanyFunctionDisplayed").toString().equals("true")) {
                           if (qryExecute) {
                              sqlvalid = dataLogic.buildSQLString(sessionId, queryName);
                              dataLogic.startTransaction(conn);

                              try {
                                 dataLogic.execute(conn, sqlvalid);
                              } catch (Exception var636) {
                                 LogManager.log("Exececption while executing JP thrown" + var636);
                              }
                           }

                           if (sSEQueryExecute) {
                              sqlvalid = dataLogic.buildSQLString(sessionId, sSEQueryName);
                              dataLogic.startTransaction(conn);

                              try {
                                 dataLogic.execute(conn, sqlvalid);
                              } catch (Exception var635) {
                                 LogManager.log("Exececption while executing SSE thrown" + var635);
                              }
                           }
                        }

                        LogManager.log("oldemail:" + oldEmail);
                        LogManager.log("newemail:" + userId);
                        User tmpGroupUser = SecurityManager.getUser(oldEmail);
                        currentUser = SecurityManager.getUser(userId);
                        LogManager.log("oldemail user:" + tmpGroupUser);
                        LogManager.log("currentUser:" + currentUser);
                        if (oldEmail.equals(userId)) {
                           upFlag = true;
                        }

                        boolean NameFlag = false;
                        if (NewFirstName.equals(OldFirstName) && NewLastName.equals(OldLastName)) {
                           NameFlag = true;
                        }

                        boolean flag1 = false;
                        if (currentUser != null && !upFlag) {
                           session.setAttribute("NotRegister", "false");
                           return false;
                        }

                        flag1 = true;
                        LogManager.log("User Name:" + tmpGroupUser);
                        LogManager.log("Admin:" + session.getAttribute("FWEmployeeID").toString());
                        if (!session.getAttribute("FWEmployeeID").toString().equals("82")) {
                           if (flag1) {
                              String sqlName1;
                              if (!NameFlag || !upFlag) {
                                 sqlName1 = "UpdateUserEmail";
                                 sql = dataLogic.buildSQLString(sessionId, sqlName1);
                                 LogManager.log("Query Executed" + sql);
                                 isemailchange = true;

                                 try {
                                    dataLogic.execute(conn, sql);
                                 } catch (Exception var634) {
                                    LogManager.log("exception while updating users table.", (Throwable)var634);
                                    dataLogic.rollbackTransaction(conn);
                                    NameFlag = true;
                                    upFlag = true;
                                 }

                                 dataLogic.commitTransaction(conn);
                                 LogManager.log("tempgroup user:" + tmpGroupUser);
                                 if (tmpGroupUser != null) {
                                    SecurityManager.removeUser(tmpGroupUser, false);
                                 }

                                 SecurityManager.loadUsers();
                                 SecurityManager.loadGroups();
                                 SecurityManager.loadAcls();
                                 LogManager.log("Test Lower" + System.getProperty("europa.hrmSearchScreen.fullaccess"));
                                 LogManager.log("completed upto here");
                              }

                              sqlName1 = "";
                              if (mode.equals("Update")) {
                                 if (session.getAttribute("FWisCompanyFunctionDisplayed") != null) {
                                    if (!session.getAttribute("FWisCompanyFunctionDisplayed").toString().equals("true") || session.getAttribute("FWNonCompanyfunctions") != null && session.getAttribute("FWNonCompanyfunctions").toString().equals("True")) {
                                       sqlName1 = mode + "EmployeeNonCompanyFunctions";
                                    } else {
                                       sqlName1 = mode + "Employee";
                                    }
                                 }
                              } else {
                                 sqlName1 = mode + "Employee";
                              }

                              sql = dataLogic.buildSQLString(sessionId, sqlName1);

                              try {
                                 dataLogic.execute(conn, sql);
                              } catch (Exception var633) {
                                 isemailchange = false;
                                 LogManager.log("exception: ", (Throwable)var633);
                              }

                              dataLogic.commitTransaction(conn);
                              dataLogic.startTransaction(conn);
                              if (session.getAttribute("FWOldJobTitleID") != null && session.getAttribute("FWJobTitleID") != null) {
                                 int oldJobTitleId = Integer.parseInt(session.getAttribute("FWOldJobTitleID").toString());
                                 int jobTitleId = Integer.parseInt(session.getAttribute("FWJobTitleID").toString());
                                 if (oldJobTitleId != jobTitleId) {
                                    ModuleUtils.UpdateFixedBidProjectContractAmount(conn, session.getUser().getEmployeeId(), sessionId);
                                    EmployeeDAO obj = new EmployeeDAO();
                                    obj.updateScopedGPP(conn, oldJobTitleId);
                                    obj.updateScopedGPP(conn, jobTitleId);
                                 }

                                 session.removeAttribute("FWOldJobTitleID");
                              }

                              if (session.getAttribute("FWPreTermDateValue") != null && session.getAttribute("FWTermDate") != null && session.getAttribute("FWJobTitleID") != null) {
                                 String preTermDate = session.getAttribute("FWPreTermDateValue").toString();
                                 String termDate = session.getAttribute("FWTermDate").toString();
                                 int jobTitleId = Integer.parseInt(session.getAttribute("FWJobTitleID").toString());
                                 if (!preTermDate.equals(termDate)) {
                                    ModuleUtils.UpdateFixedBidProjectContractAmount(conn, session.getUser().getEmployeeId(), sessionId);
                                    EmployeeDAO obj = new EmployeeDAO();
                                    obj.updateScopedGPP(conn, jobTitleId);
                                 }

                                 session.removeAttribute("FWPreTermDateValue");
                              }
                           }

                           LogManager.log("Employee Modifications");
                           this.CheckEmpinfodifferences(sessionId);
                        }

                        if (isemailchange || !NameFlag || !upFlag) {
                           SyncManager.SyncEmployeedetails(oldEmail, session.getAttribute("FWEmail").toString(), NewLastName, NewFirstName, ApplicationID);
                           SecurityManager.loadAppplicationUsers();
                           User usr = SecurityManager.getUser(session.getAttribute("FWEmail").toString());
                           if (usr != null) {
                              jsc.changeJSLastnameFirstnameEmail(usr);
                           }
                        }

                        if (tmpGroupUser != null && session.getAttribute("FWisCompanyFunctionDisplayed").toString().equals("true")) {
                           if (queryName.equals("1")) {
                              SecurityManager.addMember("projectleader", tmpGroupUser);
                           } else {
                              SecurityManager.removeMember("projectleader", tmpGroupUser);
                           }

                           if (tempSE.equals("1")) {
                              SecurityManager.addMember("salesengineer", tmpGroupUser);
                           } else {
                              SecurityManager.removeMember("salesengineer", tmpGroupUser);
                           }

                           if (tempSSE.equals("1")) {
                              SecurityManager.addMember("srsalesengineer", tmpGroupUser);
                           } else {
                              SecurityManager.removeMember("srsalesengineer", tmpGroupUser);
                           }

                           if (tempVER.equals("1")) {
                              SecurityManager.addMember("vertical", tmpGroupUser);
                           } else {
                              SecurityManager.removeMember("vertical", tmpGroupUser);
                           }

                           if (tempJpr.equals("1")) {
                              LogManager.log("jrpartner addding to group");
                              SecurityManager.addMember("jrpartner", tmpGroupUser);
                           } else {
                              LogManager.log("jrpartner removing from group");
                              SecurityManager.removeMember("jrpartner", tmpGroupUser);
                           }

                           if (tempResourceManager.equals("1")) {
                              LogManager.log("portfolioMgr addding to group");
                              SecurityManager.addMember("portfolioMgr", tmpGroupUser);
                           } else {
                              LogManager.log("portfolioMgr removing from group");
                              SecurityManager.removeMember("portfolioMgr", tmpGroupUser);
                           }

                           if (tempAnv.equals("1")) {
                              SecurityManager.addMember("analysisvertical", tmpGroupUser);
                           } else {
                              SecurityManager.removeMember("analysisvertical", tmpGroupUser);
                           }

                           if (tempElv.equals("1")) {
                              SecurityManager.addMember("electricalvertical", tmpGroupUser);
                           } else {
                              SecurityManager.removeMember("electricalvertical", tmpGroupUser);
                           }

                           if (tempMfv.equals("1")) {
                              SecurityManager.addMember("manufacturingvertical", tmpGroupUser);
                           } else {
                              SecurityManager.removeMember("manufacturingvertical", tmpGroupUser);
                           }

                           if (tempChk.equals("1")) {
                              SecurityManager.addMember("Checkers", tmpGroupUser);
                           } else {
                              SecurityManager.removeMember("Checkers", tmpGroupUser);
                           }

                           if (tempEAdmin.equals("1")) {
                              SecurityManager.addMember("Eng Admin", tmpGroupUser);
                           } else {
                              SecurityManager.removeMember("Eng Admin", tmpGroupUser);
                           }

                           if (tempBdv.equals("1")) {
                              SecurityManager.addMember("accountexec", tmpGroupUser);
                           } else {
                              SecurityManager.removeMember("accountexec", tmpGroupUser);
                           }

                           if (tempPM.equals("1")) {
                              SecurityManager.addMember("People Manager", tmpGroupUser);
                           } else {
                              SecurityManager.removeMember("People Manager", tmpGroupUser);
                           }

                           if (SecurityManager.getUser(session.getUser().toString()).toString().equals("admin")) {
                              if (tempAdminGP.equals("1")) {
                                 SecurityManager.addMember("admin", tmpGroupUser);
                              } else {
                                 SecurityManager.removeMember("admin", tmpGroupUser);
                              }
                           }

                           if (tempPowerSystem.equals("1")) {
                              SecurityManager.addMember("powersystem", tmpGroupUser);
                           } else {
                              SecurityManager.removeMember("powersystem", tmpGroupUser);
                           }

                           if (tempLeadEng.equals("1")) {
                              SecurityManager.addMember("leadengineer", tmpGroupUser);
                           } else {
                              SecurityManager.removeMember("leadengineer", tmpGroupUser);
                           }

                           if (tempEngManager.equals("1")) {
                              SecurityManager.addMember("engmanager", tmpGroupUser);
                           } else {
                              SecurityManager.removeMember("engmanager", tmpGroupUser);
                           }

                           if (mDACompEast.equals("1")) {
                              SecurityManager.addMember("DesignAndAnalysisCompEast", tmpGroupUser);
                           } else {
                              SecurityManager.removeMember("DesignAndAnalysisCompEast", tmpGroupUser);
                           }

                           if (mDACompWest.equals("1")) {
                              SecurityManager.addMember("DesignAndAnalysisCompWest", tmpGroupUser);
                           } else {
                              SecurityManager.removeMember("DesignAndAnalysisCompWest", tmpGroupUser);
                           }

                           LogManager.log("Came here Aris");
                           if (tlManager.equals("1")) {
                              SecurityManager.addMember("TopLineManager", tmpGroupUser);
                              LogManager.log("TopLineManager Added");
                           } else {
                              SecurityManager.removeMember("TopLineManager", tmpGroupUser);
                              LogManager.log("TopLineManager Removed");
                           }

                           LogManager.log("tempOffshoreManager " + tempOffshoreManager);
                           if (tempOffshoreManager.equals("1")) {
                              SecurityManager.addMember("OffshoreManager", tmpGroupUser);
                              LogManager.log("OffshoreManager Added");
                           } else {
                              SecurityManager.removeMember("OffshoreManager", tmpGroupUser);
                              LogManager.log("OffshoreManager Removed");
                           }

                           LogManager.log("tempElectricalSystemsIntegration " + tempElectricalSystemsIntegration);
                           if (tempElectricalSystemsIntegration.equals("1")) {
                              SecurityManager.addMember("ElectricalSystemsIntegration", tmpGroupUser);
                              LogManager.log("ElectricalSystemsIntegration Added");
                           } else {
                              SecurityManager.removeMember("ElectricalSystemsIntegration", tmpGroupUser);
                              LogManager.log("ElectricalSystemsIntegration Removed");
                           }
                        }
                     }
                  } catch (EuropaException var656) {
                     dataLogic.rollbackTransaction(conn);
                     throw var656;
                  }

                  User tuser = SecurityManager.getUser(session.getAttribute("FWEmail").toString());
                  boolean menuChange = false;
                  ArrayList limitedMenuPayRollDept = new ArrayList();
                  ArrayList excludereportmenupeyrollDept = new ArrayList();
                  queryName = System.getProperty("europa.module.employees.ExcludeReportsFromLimitedMenuPayRollDept");
                  StringTokenizer strTok = new StringTokenizer(queryName, ",");

                  while(strTok.hasMoreTokens()) {
                     excludereportmenupeyrollDept.add(strTok.nextElement().toString());
                  }

                  tempSSE = (String)session.getAttribute("FWPayRollDeptID");
                  tempVER = "";
                  if (mode.equals("Update")) {
                     tempVER = (String)session.getAttribute("FWOldPayRollDeptId");
                  }

                  Vector v;
                  if ((!mode.equals("Update") || tuser == null || !SecurityManager.isMember("admin", tuser)) && (limitedMenuPayRollDept.contains(tempSSE) || !limitedMenuPayRollDept.contains(tempVER))) {
                     if (limitedMenuPayRollDept.contains(tempSSE) && (mode.equals("Update") && tuser != null && !SecurityManager.isMember("admin", tuser) || tuser == null)) {
                        if (excludereportmenupeyrollDept.contains(tempSSE)) {
                           session.setAttribute("FWPermID", "0");
                        } else {
                           session.setAttribute("FWPermID", "48");
                        }

                        if (mode.equals("Insert")) {
                           new Vector();
                           sqlstmt = dataLogic.buildSQLString(sessionId, "getEmpID");
                           dataLogic.startTransaction(conn);

                           try {
                              v = dataLogic.execute(conn, sqlstmt);
                           } catch (EuropaException var631) {
                              dataLogic.rollbackTransaction(conn);
                              throw var631;
                           }

                           dataLogic.commitTransaction(conn);
                           if (v != null && v.size() > 0) {
                              Vector temp = (Vector)v.get(0);
                              tempElv = temp.get(0).toString();
                              session.setAttribute("FWEmployeeID", tempElv);
                           }
                        }

                        new Vector();
                        int menucount = 0;
                        sqlstmt = dataLogic.buildSQLString(sessionId, "getRevokeMenuAccessCount");
                        dataLogic.startTransaction(conn);

                        try {
                           v = dataLogic.execute(conn, sqlstmt);
                        } catch (EuropaException var630) {
                           dataLogic.rollbackTransaction(conn);
                           throw var630;
                        }

                        dataLogic.commitTransaction(conn);
                        if (v != null && v.size() > 0) {
                           Vector temp = (Vector)v.get(0);
                           menucount = Integer.parseInt(temp.get(0).toString());
                        }

                        if (menucount == 0) {
                           sqlstmt = dataLogic.buildSQLString(sessionId, "insertMenuRevokeAccess");
                           dataLogic.startTransaction(conn);

                           try {
                              dataLogic.execute(conn, sqlstmt);
                           } catch (EuropaException var629) {
                              dataLogic.rollbackTransaction(conn);
                              throw var629;
                           }

                           dataLogic.commitTransaction(conn);
                           menuChange = true;
                        }
                     }
                  } else {
                     if (excludereportmenupeyrollDept.contains(tempSSE)) {
                        session.setAttribute("FWPermID", "0");
                     } else {
                        session.setAttribute("FWPermID", "48");
                     }

                     sqlstmt = dataLogic.buildSQLString(sessionId, "deleteMenuRevokeAccess");
                     dataLogic.startTransaction(conn);

                     try {
                        dataLogic.execute(conn, sqlstmt);
                     } catch (EuropaException var632) {
                        dataLogic.rollbackTransaction(conn);
                        throw var632;
                     }

                     dataLogic.commitTransaction(conn);
                     menuChange = true;
                  }

                  dataLogic.commitTransaction(conn);
                  session.removeAttribute("FWPermID");
                  if (SecurityManager.getUser(session.getUser().toString()).toString().equals("admin")) {
                     tempJpr = (String)session.getAttribute("FWisAdminGroup1");
                     tempAnv = (String)session.getAttribute("FWOldisAdminGroup");
                     if (mode.equals("Update") && !tempJpr.equals(tempAnv)) {
                        sqlstmt = dataLogic.buildSQLString(sessionId, "DeleteDefaultProjectModulePerms");
                        dataLogic.startTransaction(conn);

                        try {
                           dataLogic.execute(conn, sqlstmt);
                        } catch (EuropaException var628) {
                           dataLogic.rollbackTransaction(conn);
                           throw var628;
                        }

                        dataLogic.commitTransaction(conn);
                        if (tempJpr.equals("1")) {
                           sqlstmt = dataLogic.buildSQLString(sessionId, "InsertDefaultProjectModulePerms");
                           dataLogic.startTransaction(conn);

                           try {
                              dataLogic.execute(conn, sqlstmt);
                           } catch (EuropaException var627) {
                              dataLogic.rollbackTransaction(conn);
                              throw var627;
                           }

                           dataLogic.commitTransaction(conn);
                        }

                        menuChange = true;
                     }
                  }

                  if (resourceManagerFlag) {
                     new Vector();
                     sqlstmt = dataLogic.buildSQLString(sessionId, "SelectIsResourceManager");
                     dataLogic.startTransaction(conn);

                     try {
                        v = dataLogic.execute(conn, sqlstmt);
                     } catch (EuropaException var626) {
                        dataLogic.rollbackTransaction(conn);
                        throw var626;
                     }

                     if (v.size() == 0) {
                        sqlstmt = dataLogic.buildSQLString(sessionId, "InsertResourceManager");
                        dataLogic.startTransaction(conn);

                        try {
                           dataLogic.execute(conn, sqlstmt);
                        } catch (EuropaException var625) {
                           dataLogic.rollbackTransaction(conn);
                           throw var625;
                        }

                        dataLogic.commitTransaction(conn);
                     }
                  }

                  if (isemailchange || menuChange) {
                     SecurityManager.loadAllEmailConfig();
                  }
               } finally {
                  if (conn != null) {
                     try {
                        conn.close();
                     } catch (SQLException var595) {
                        LogManager.log("Unable to Close connection.", (Throwable)var595);
                     }

                     conn = null;
                  }

               }

               if (conn != null) {
                  try {
                     conn.close();
                  } catch (SQLException var624) {
                     LogManager.log("Unable to Close connection.", (Throwable)var624);
                  }

                  conn = null;
               }
               break;
            case 3:
               cs = null;

               try {
                  LogManager.log("Comming to case 3");
                  isRehire = "";
                  if (session.getAttribute("FWCodes").toString() != null) {
                     isRehire = "," + session.getAttribute("FWCodes").toString();
                  }

                  try {
                     cs = conn.prepareCall("{call SP_AUDIT_EACT(?,?,?)}");
                     cs.setString(1, session.getAttribute("FWEmployeeID").toString());
                     cs.setString(2, isRehire);
                     cs.setString(3, session.getUser().getEmployeeId().toString());
                     cs.execute();
                  } catch (Exception var623) {
                     LogManager.log("Exception raised: SP", (Throwable)var623);
                  }

                  sql = dataLogic.buildSQLString(sessionId, "DeleteEmployeeActivity");
                  dataLogic.startTransaction(conn);

                  try {
                     dataLogic.execute(conn, sql);
                  } catch (EuropaException var622) {
                     dataLogic.rollbackTransaction(conn);
                     throw var622;
                  }

                  dataLogic.commitTransaction(conn);
                  isConDirect = (String)session.getAttribute("FWCodes");
                  LogManager.log("FwCodes are:" + isConDirect);
                  if (isConDirect != null && !isConDirect.equals("undefined")) {
                     for(st = new StringTokenizer(isConDirect, ","); st.hasMoreTokens(); dataLogic.commitTransaction(conn)) {
                        isPL = st.nextToken();
                        int index1 = isPL.indexOf("-");
                        isEngAdmin = isPL.substring(0, index1);
                        isPowerSystem = isPL.substring(index1 + 1);
                        LogManager.log("Line 547 " + isEngAdmin + "|" + isPowerSystem);
                        session.setAttribute("FWActCode", isEngAdmin.trim());
                        session.setAttribute("FWSubCode", isPowerSystem.trim());
                        sql = dataLogic.buildSQLString(sessionId, "SelectActivityCodeID");
                        dataLogic.startTransaction(conn);

                        try {
                           results = dataLogic.execute(conn, sql);
                        } catch (EuropaException var621) {
                           dataLogic.rollbackTransaction(conn);
                           throw var621;
                        }

                        dataLogic.commitTransaction(conn);
                        newClob = null;
                        Integer activityCodeID = null;
                        Date dateCode = null;

                        for(int i = 0; i < results.size(); ++i) {
                           Vector row = (Vector)results.get(i);
                           Integer newActivityCodeID = (Integer)row.get(0);
                           Date newDateCode = (Date)row.get(1);
                           if (dateCode == null) {
                              LogManager.log("dateCode is Null.  Setting to newDateCode");
                              activityCodeID = newActivityCodeID;
                              dateCode = newDateCode;
                           } else {
                              LogManager.log("dateCode is not Null.");
                              if (newDateCode.after(dateCode)) {
                                 LogManager.log("newDateCode is newer.  Setting ActID and dateCode");
                                 activityCodeID = newActivityCodeID;
                                 dateCode = newDateCode;
                              }
                           }
                        }

                        session.setAttribute("FWActID", activityCodeID.toString());
                        sql = dataLogic.buildSQLString(sessionId, "InsertEmployeeActivity");
                        dataLogic.startTransaction(conn);

                        try {
                           dataLogic.execute(conn, sql);
                        } catch (EuropaException var620) {
                           dataLogic.rollbackTransaction(conn);
                           throw var620;
                        }
                     }
                  }
                  break;
               } finally {
                  if (cs != null) {
                     try {
                        cs.close();
                     } catch (SQLException var594) {
                        LogManager.log("Unable to Close statement.", (Throwable)var594);
                     }
                  }

                  if (conn != null) {
                     try {
                        conn.close();
                     } catch (SQLException var593) {
                        LogManager.log("Unable to Close connection.", (Throwable)var593);
                     }

                     conn = null;
                  }

               }
            case 4:
               cs = null;

               try {
                  try {
                     SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy");
                     new SimpleDateFormat("yyyy-MM-dd");
                     isPL = (String)session.getAttribute("EditMode");
                     LogManager.log("Comming to Master Sp Location" + isPL);
                     isChk = (String)session.getAttribute("FWContractorcost");
                     if (session.getAttribute("FWContractorcost") != null) {
                        if (isChk.equals("on")) {
                           session.setAttribute("FWContractorcostvalue", "1");
                        } else {
                           session.setAttribute("FWContractorcostvalue", "0");
                        }
                     } else {
                        session.setAttribute("FWContractorcostvalue", "0");
                     }

                     double oldSalaryPeriod = 0.0;
                     double oldSalaryAmount = 0.0;
                     double oldBurdenFactor = 0.0;
                     double oldEmpTimeId = 0.0;
                     Date oldDate = null;
                     double oldEmpCost = 0.0;
                     isSE = null;
                     Date CostDate = null;

                     try {
                        oldSalaryPeriod = Double.parseDouble(session.getAttribute("FWOldSalaryPeriodId").toString());
                        oldSalaryAmount = Double.parseDouble(session.getAttribute("FWOldSalaryAmount").toString());
                        oldBurdenFactor = Double.parseDouble(session.getAttribute("FWOldBurdenFactor").toString());
                        oldEmpTimeId = Double.parseDouble(session.getAttribute("FWOldEmployeeTime").toString());
                        oldDate = new Date(session.getAttribute("FWSecureInfoSysDate").toString());
                        oldEmpCost = Double.parseDouble(session.getAttribute("FWOld_EmployeeCost").toString());
                        isSE = session.getAttribute("FWOldEmployeeCostDate").toString();
                        String[] a = isSE.split("-");
                        isVer = a[0].substring(2);
                        CostDate = new Date(a[1] + "/" + a[2] + "/" + isVer);
                     } catch (Exception var619) {
                     }

                     double newSalaryPeriod = Double.parseDouble(session.getAttribute("FWSalaryPeriodID").toString());
                     double newSalaryAmount = Double.parseDouble(session.getAttribute("FWSalaryAmount").toString());
                     double newBurdenFactor = Double.parseDouble(session.getAttribute("FWBurdenFactor").toString());
                     double newEmpTimeId = Double.parseDouble(session.getAttribute("FWEmpWorkType").toString());
                     Date newDate = new Date(session.getAttribute("FWSecureInfoDateEntered").toString());
                     double newEmpCost = Double.parseDouble(session.getAttribute("FWEmployeeCost").toString());
                     Date newCostDate = new Date(session.getAttribute("FWCostEffFromformatted").toString());
                     LogManager.log("FWCostEffFromformatted =" + newCostDate);
                     LogManager.log("oldSalaryPeriod" + oldSalaryPeriod);
                     LogManager.log("oldSalaryAmount" + oldSalaryAmount);
                     LogManager.log("oldBurdenFactor" + oldBurdenFactor);
                     sSEQueryExecute = Boolean.valueOf(session.getAttribute("FWInsertAudit").toString());
                     int cols1;
                     if ((oldSalaryPeriod != newSalaryPeriod || oldSalaryAmount != newSalaryAmount || oldBurdenFactor != newBurdenFactor || oldEmpTimeId != newEmpTimeId) && (oldSalaryPeriod != 0.0 || oldSalaryAmount != 0.0 || oldBurdenFactor != 0.0) && sSEQueryExecute) {
                        try {
                           LogManager.log("SP going get Executed  :" + oldDate.equals(newDate));
                           Date FWSecureInfoDate = null;
                           if (session.getAttribute("FWEffectiveDateFlag").toString().equals("0")) {
                              FWSecureInfoDate = new Date(session.getAttribute("FWDumLastUpdated").toString());
                           } else {
                              FWSecureInfoDate = new Date(session.getAttribute("FWEffectiveDateFlag").toString());
                           }

                           cs = conn.prepareCall("{call SP_AUDIT_EMPSAL_MASTERINSERT(?,?,?)}");
                           cs.setString(1, session.getUser().getEmployeeId().toString());
                           cs.setString(2, session.getAttribute("FWEmployeeID").toString());
                           cs.setDate(3, new java.sql.Date(FWSecureInfoDate.getTime()));
                           cs.execute();
                        } catch (Exception var618) {
                           LogManager.log("Exception Raised when Executing Master Sp", (Throwable)var618);
                        }

                        cols1 = 0;
                        int cols2 = 0;
                        int cols3 = 0;
                        int cols4 = 0;
                        if (oldSalaryPeriod != newSalaryPeriod) {
                           cols1 = 1;
                        }

                        if (oldSalaryAmount != newSalaryAmount) {
                           cols2 = 1;
                        }

                        if (oldBurdenFactor != newBurdenFactor) {
                           cols3 = 1;
                        }

                        if (oldEmpTimeId != newEmpTimeId) {
                           cols4 = 1;
                        }

                        LogManager.log("Extend:" + newEmpTimeId + "Extend 1:" + oldEmpTimeId);

                        try {
                           cs = conn.prepareCall("{call SP_AUDIT_EMPSAL(?,?,?,?,?,?,?,?,?,?,?,?,?)}");
                           cs.setString(1, String.valueOf(newSalaryPeriod));
                           cs.setString(2, String.valueOf(newSalaryAmount));
                           cs.setString(3, String.valueOf(newBurdenFactor));
                           cs.setString(4, String.valueOf(newEmpTimeId));
                           cs.setString(5, String.valueOf(oldSalaryPeriod));
                           cs.setString(6, String.valueOf(oldSalaryAmount));
                           cs.setString(7, String.valueOf(oldBurdenFactor));
                           cs.setString(8, String.valueOf(oldEmpTimeId));
                           cs.setString(9, String.valueOf(cols1));
                           cs.setString(10, String.valueOf(cols2));
                           cs.setString(11, String.valueOf(cols3));
                           cs.setString(12, String.valueOf(cols4));
                           cs.setString(13, String.valueOf(session.getUser().getEmployeeId()));
                           cs.execute();
                        } catch (Exception var617) {
                           LogManager.log("Exception Raised when Executing Detail Sp", (Throwable)var617);
                        }
                     }

                     if (session.getAttribute("FWEmpIdWage") == null || session.getAttribute("FWEmpIdWage").toString().trim().equals("")) {
                        session.setAttribute("FWEmpIdWage", "");
                     }

                     sql = dataLogic.buildSQLString(sessionId, "UpdateEmloyeePrivateInfo");
                     dataLogic.startTransaction(conn);

                     try {
                        dataLogic.execute(conn, sql);
                     } catch (EuropaException var652) {
                        dataLogic.rollbackTransaction(conn);
                        session.setAttribute("FWError", "yes");
                        return false;
                     }

                     dataLogic.commitTransaction(conn);
                     if (session.getAttribute("FWJobTitleID") != null) {
                        cols1 = Integer.parseInt(session.getAttribute("FWJobTitleID").toString());
                        ModuleUtils.UpdateFixedBidProjectContractAmount(conn, session.getUser().getEmployeeId(), sessionId);
                        EmployeeDAO dao = new EmployeeDAO();
                        dao.updateScopedGPP(conn, cols1);
                     }

                     dataLogic.commitTransaction(conn);
                     session.removeAttribute("FWDataFeed");
                     session.removeAttribute("FWDataFeedValue");
                     boolean insertFlag1 = false;
                     isAnv = (String)session.getAttribute("OldFWSpotBonuses");
                     isElv = (String)session.getAttribute("OldFWSpotBonusesDate");
                     isMfv = (String)session.getAttribute("FWSpotBonuses");
                     mode = (String)session.getAttribute("FWSpotBonusesDate");
                     if (this.spotBonusInsertion) {
                        if (isAnv != null && isElv != null) {
                           if (isAnv.equals(isMfv) && isElv.equals(mode)) {
                              insertFlag1 = false;
                           } else if (Double.parseDouble(isMfv) > 0.0) {
                              insertFlag1 = true;
                           } else {
                              insertFlag1 = false;
                           }
                        } else if (Double.parseDouble(isMfv) > 0.0) {
                           insertFlag1 = true;
                        } else {
                           insertFlag1 = false;
                        }

                        if (insertFlag1) {
                           sqlvalid = dataLogic.buildSQLString(sessionId, "InsertBonusCommisionDetails");
                           dataLogic.startTransaction(conn);

                           try {
                              dataLogic.execute(conn, sqlvalid);
                           } catch (EuropaException var616) {
                              dataLogic.rollbackTransaction(conn);
                              throw var616;
                           }

                           dataLogic.commitTransaction(conn);
                        }
                     }

                     if (oldEmpCost == newEmpCost && CostDate.equals(newCostDate)) {
                        LogManager.log("inside if");
                     } else {
                        LogManager.log("inside else");
                        if (CostDate.before(newCostDate)) {
                           session.setAttribute("FWUserID", session.getUser().getEmployeeId().toString());
                           session.setAttribute("FWOldEmployeeCost", String.valueOf(oldEmpCost));
                           session.setAttribute("FWOldEffectiveCostDate", formatter.format(CostDate));
                           sqlvalid = dataLogic.buildSQLString(sessionId, "FlushJunkAudits");
                           LogManager.log("sqlvalid = " + sqlvalid);
                           dataLogic.startTransaction(conn);

                           try {
                              dataLogic.execute(conn, sqlvalid);
                           } catch (EuropaException var615) {
                              dataLogic.rollbackTransaction(conn);
                              throw var615;
                           }

                           dataLogic.commitTransaction(conn);
                           sqlvalid = dataLogic.buildSQLString(sessionId, "InsertEmployeeCostAudit");
                           LogManager.log("sqlvalid = " + sqlvalid);
                           dataLogic.startTransaction(conn);

                           try {
                              dataLogic.execute(conn, sqlvalid);
                           } catch (EuropaException var614) {
                              dataLogic.rollbackTransaction(conn);
                              throw var614;
                           }

                           dataLogic.commitTransaction(conn);
                        } else {
                           session.setAttribute("FWOldEffectiveCostDate", formatter.format(newCostDate));
                           sqlvalid = dataLogic.buildSQLString(sessionId, "FlushJunkAudits");
                           LogManager.log("sqlvalid = " + sqlvalid);
                           dataLogic.startTransaction(conn);

                           try {
                              dataLogic.execute(conn, sqlvalid);
                           } catch (EuropaException var613) {
                              dataLogic.rollbackTransaction(conn);
                              throw var613;
                           }

                           dataLogic.commitTransaction(conn);
                        }
                     }
                  } catch (Exception var653) {
                     LogManager.log("Exception here is =" + var653);
                  }
                  break;
               } finally {
                  if (cs != null) {
                     try {
                        cs.close();
                     } catch (SQLException var597) {
                        LogManager.log("Unable to Close statement.", (Throwable)var597);
                     }
                  }

                  if (conn != null) {
                     try {
                        conn.close();
                     } catch (SQLException var596) {
                        LogManager.log("Unable to Close connection.", (Throwable)var596);
                     }

                     conn = null;
                  }

               }
            case 5:
               try {
                  LogManager.log("Inserting & Updating Employee information in HRMInfo");
                  if ((this.hrmHelmetAwardEditUsers.contains(session.getUser().toString()) && !session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString()) || SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") || this.hrmEditUsers.contains(session.getUser().toString()) || this.hrmDataEntryUsers.contains(session.getUser().toString())) && !session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString())) {
                     this.helmetAwardsTransaction(sessionId, conn);
                  }

                  this.tutionAndReimbursement(sessionId, conn);

                  try {
                     this.flexTimeTransaction(sessionId, conn);
                     this.bankTimeTransaction(sessionId, conn);
                  } catch (Exception var612) {
                     LogManager.log("Exception while executing bank time", (Throwable)var612);
                  }

                  sql = dataLogic.buildSQLString(sessionId, "DeleteHRMDegrees");
                  dataLogic.startTransaction(conn);

                  try {
                     dataLogic.execute(conn, sql);
                  } catch (EuropaException var611) {
                     dataLogic.rollbackTransaction(conn);
                     throw var611;
                  }

                  dataLogic.commitTransaction(conn);
                  LogManager.log("FwCodes are:" + session.getAttribute("FWCodes"));
                  isRehire = (String)session.getAttribute("FWCodes");
                  LogManager.log("Selected FwCodes are:" + isRehire);
                  if (isRehire != null) {
                     for(st = new StringTokenizer(isRehire, ","); st.hasMoreTokens(); dataLogic.commitTransaction(conn)) {
                        isConDirect = st.nextToken();
                        LogManager.log("String Token:" + isConDirect);
                        session.setAttribute("FWDegreeId", isConDirect);
                        sql = dataLogic.buildSQLString(sessionId, "InsertEmployeeHRMDegrees");
                        dataLogic.startTransaction(conn);

                        try {
                           dataLogic.execute(conn, sql);
                        } catch (EuropaException var610) {
                           dataLogic.rollbackTransaction(conn);
                           throw var610;
                        }
                     }
                  }

                  sql = dataLogic.buildSQLString(sessionId, "DeleteHRMCertifications");
                  dataLogic.startTransaction(conn);

                  try {
                     dataLogic.execute(conn, sql);
                  } catch (EuropaException var609) {
                     dataLogic.rollbackTransaction(conn);
                     throw var609;
                  }

                  dataLogic.commitTransaction(conn);
                  LogManager.log("FWCertificate are:" + session.getAttribute("FWCertificate"));
                  isConDirect = (String)session.getAttribute("FWCertificate");
                  LogManager.log("Selected FWCertificate are:" + isConDirect);
                  if (isConDirect != null) {
                     for(st = new StringTokenizer(isConDirect, ","); st.hasMoreTokens(); dataLogic.commitTransaction(conn)) {
                        isPL = st.nextToken();
                        LogManager.log("String Token:" + isPL);
                        session.setAttribute("FWCertificateId", isPL);
                        sql = dataLogic.buildSQLString(sessionId, "InsertEmployeeHRMCertifications");
                        dataLogic.startTransaction(conn);

                        try {
                           dataLogic.execute(conn, sql);
                        } catch (EuropaException var608) {
                           dataLogic.rollbackTransaction(conn);
                           throw var608;
                        }
                     }
                  }

                  isPL = (String)session.getAttribute("EditMode");
                  if (session.getAttribute("FWRelocate").toString().equals("Yes")) {
                     session.setAttribute("FWRelocate", "1");
                  } else {
                     session.setAttribute("FWRelocate", "0");
                  }

                  if (isPL.equals("Insert") || isPL.equals("Update")) {
                     LogManager.log("This is Updation ---------");
                     this.updateIvpArea(sessionId, conn);
                     sqlvalid = dataLogic.buildSQLString(sessionId, "UpdateHRMInfo");
                     dataLogic.startTransaction(conn);

                     try {
                        dataLogic.execute(conn, sqlvalid);
                     } catch (EuropaException var607) {
                        dataLogic.rollbackTransaction(conn);
                        throw var607;
                     }

                     dataLogic.commitTransaction(conn);
                  }

                  sql = dataLogic.buildSQLString(sessionId, "UpdateEvaluationDate");
                  Vector paramVector = new Vector();
                  pquery1 = psActivityCodeData.createPreparedStatement(conn, sql.toString());
                  int ivalue = 0;

                  try {
                     ivalue = Integer.parseInt(session.getAttribute("FWiValue").toString());
                  } catch (Exception var606) {
                     LogManager.log("exception: ", (Throwable)var606);
                  }

                  LogManager.log("XOssfjaflk;adjfdkals;fjdalsfjlasd;fj first" + ivalue);
                  dataLogic.startTransaction(conn);

                  try {
                     int i = 0;

                     while(true) {
                        if (i >= ivalue) {
                           break label11025;
                        }

                        paramVector.clear();
                        LogManager.log("Cmg:" + session.getAttribute("FWDate" + String.valueOf(i)).toString());
                        LogManager.log("Cmg 2 :" + session.getAttribute("FWEvaluSno" + String.valueOf(i)).toString());
                        paramVector.add(0, session.getAttribute("FWDate" + String.valueOf(i)).toString());
                        paramVector.add(1, session.getAttribute("FWEvaluSno" + String.valueOf(i)).toString());
                        paramVector.add(2, session.getAttribute("FWEmployeeID"));
                        LogManager.log("Paramter Vector:" + paramVector);
                        psActivityCodeData.executePreparedStatement(pquery1, paramVector);
                        ++i;
                     }
                  } catch (Exception var649) {
                     LogManager.log("Exception e: ", (Throwable)var649);
                     break;
                  }
               } finally {
                  if (pquery1 != null) {
                     try {
                        pquery1.close();
                     } catch (SQLException var592) {
                        LogManager.log("Unable to Close statement.", (Throwable)var592);
                     }
                  }

                  if (conn != null) {
                     try {
                        conn.close();
                     } catch (SQLException var591) {
                        LogManager.log("Unable to Close connection.", (Throwable)var591);
                     }

                     conn = null;
                  }

               }
            case 6:
               try {
                  sqlvalid = dataLogic.buildSQLString(sessionId, "IvpAreaEmployeeCount");
                  dataLogic.startTransaction(conn);

                  try {
                     serchResults = dataLogic.execute(conn, sqlvalid);
                  } catch (EuropaException var605) {
                     dataLogic.rollbackTransaction(conn);
                     throw var605;
                  }

                  LogManager.log("Serarch Results " + serchResults);
                  dataLogic.commitTransaction(conn);
                  isRehire = "InsertIvpArea";
                  Connection connClob = dataLogic.specificConnection();
                  PreparedStatement pstmtClobInsert = null;

                  try {
                     connClob.setAutoCommit(false);
                     isChk = "";
                     isEngAdmin = "";
                     isPowerSystem = "";
                     if (session.getAttribute("FWTrainingScholarship") == null) {
                        isChk = "";
                     } else {
                        isChk = session.getAttribute("FWTrainingScholarship").toString();
                     }

                     if (session.getAttribute("FWBonusEmail") == null) {
                        isEngAdmin = "";
                     } else {
                        isEngAdmin = session.getAttribute("FWBonusEmail").toString();
                     }

                     if (session.getAttribute("FWGoals") == null) {
                        isPowerSystem = "";
                     } else {
                        isPowerSystem = session.getAttribute("FWGoals").toString();
                     }

                     sqlvalid = dataLogic.buildSQLString(sessionId, isRehire);
                     pstmtClobInsert = connClob.prepareStatement(sqlvalid.toString());
                     newClob = CLOB.createTemporary(connClob, false, 10);
                     newClob.putString(1L, isChk);
                     pstmtClobInsert.setClob(1, newClob);
                     CLOB newClob1 = CLOB.createTemporary(connClob, false, 10);
                     newClob1.putString(1L, isEngAdmin);
                     pstmtClobInsert.setClob(2, newClob1);
                     CLOB newClob2 = CLOB.createTemporary(connClob, false, 10);
                     newClob2.putString(1L, isPowerSystem);
                     pstmtClobInsert.setClob(3, newClob2);
                     pstmtClobInsert.executeUpdate();
                     dataLogic.commitTransaction(connClob);
                  } catch (Exception var604) {
                     LogManager.log("Exception e:", (Throwable)var604);
                  } finally {
                     if (pstmtClobInsert != null) {
                        try {
                           pstmtClobInsert.close();
                        } catch (SQLException var590) {
                           LogManager.log("Unable to Close statement.", (Throwable)var590);
                        }
                     }

                     if (connClob != null) {
                        try {
                           connClob.close();
                        } catch (SQLException var589) {
                           LogManager.log("Unable to Close connection.", (Throwable)var589);
                        }

                        connClob = null;
                     }

                  }

                  if ((this.hrmHelmetAwardEditUsers.contains(session.getUser().toString()) && !session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString()) || SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") || this.hrmEditUsers.contains(session.getUser().toString()) || this.hrmDataEntryUsers.contains(session.getUser().toString())) && !session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString())) {
                     this.helmetAwardsTransaction(sessionId, conn);
                  }

                  if (SecurityManager.getUser(session.getAttribute("FWEmail").toString()).toString().equals(session.getUser().toString())) {
                     if (session.getAttribute("FWRelocate").toString().equals("Yes")) {
                        session.setAttribute("FWRelocate", "1");
                     } else {
                        session.setAttribute("FWRelocate", "0");
                     }

                     sql = dataLogic.buildSQLString(sessionId, "DeleteHRMDegrees");
                     dataLogic.startTransaction(conn);

                     try {
                        dataLogic.execute(conn, sql);
                     } catch (EuropaException var603) {
                        dataLogic.rollbackTransaction(conn);
                        throw var603;
                     }

                     dataLogic.commitTransaction(conn);
                     LogManager.log("FwCodes are:" + session.getAttribute("FWCodes"));
                     isChk = (String)session.getAttribute("FWCodes");
                     LogManager.log("Selected FwCodes are:" + isChk);
                     if (isChk != null) {
                        for(st = new StringTokenizer(isChk, ","); st.hasMoreTokens(); dataLogic.commitTransaction(conn)) {
                           isEngAdmin = st.nextToken();
                           LogManager.log("String Token:" + isEngAdmin);
                           session.setAttribute("FWDegreeId", isEngAdmin);
                           sql = dataLogic.buildSQLString(sessionId, "InsertEmployeeHRMDegrees");
                           dataLogic.startTransaction(conn);

                           try {
                              dataLogic.execute(conn, sql);
                           } catch (EuropaException var602) {
                              dataLogic.rollbackTransaction(conn);
                              throw var602;
                           }
                        }
                     }

                     sql = dataLogic.buildSQLString(sessionId, "DeleteHRMCertifications");
                     dataLogic.startTransaction(conn);

                     try {
                        dataLogic.execute(conn, sql);
                     } catch (EuropaException var601) {
                        dataLogic.rollbackTransaction(conn);
                        throw var601;
                     }

                     dataLogic.commitTransaction(conn);
                     LogManager.log("FWCertificate are:" + session.getAttribute("FWCertificate"));
                     isEngAdmin = (String)session.getAttribute("FWCertificate");
                     LogManager.log("Selected FWCertificate are:" + isEngAdmin);
                     if (isEngAdmin != null) {
                        for(st = new StringTokenizer(isEngAdmin, ","); st.hasMoreTokens(); dataLogic.commitTransaction(conn)) {
                           isPowerSystem = st.nextToken();
                           LogManager.log("String Token:" + isPowerSystem);
                           session.setAttribute("FWCertificateId", isPowerSystem);
                           sql = dataLogic.buildSQLString(sessionId, "InsertEmployeeHRMCertifications");
                           dataLogic.startTransaction(conn);

                           try {
                              dataLogic.execute(conn, sql);
                           } catch (EuropaException var600) {
                              dataLogic.rollbackTransaction(conn);
                              throw var600;
                           }
                        }
                     }

                     sqlvalid = dataLogic.buildSQLString(sessionId, "UpdateHRMInfo_NormalUser");
                     dataLogic.startTransaction(conn);

                     try {
                        dataLogic.execute(conn, sqlvalid);
                     } catch (EuropaException var599) {
                        dataLogic.rollbackTransaction(conn);
                        throw var599;
                     }

                     dataLogic.commitTransaction(conn);
                  }
               } finally {
                  if (conn != null) {
                     try {
                        conn.close();
                     } catch (SQLException var588) {
                        LogManager.log("Unable to Close connection.", (Throwable)var588);
                     }

                     conn = null;
                  }

               }
            case 7:
            default:
               break;
            case 8:
               LogManager.log("comming to the case 8");
               isRehire = (String)session.getAttribute("FWYearSelected");
               if (isRehire != null) {
                  session.setAttribute("FWYearsList", this.buildYearList(sessionId, "SelectYears", session.getAttribute("FWYearSelected").toString()));
                  session.setAttribute("FWOldWageHistory", this.empSalaryAudit(sessionId));
               }
         }

         LogManager.log("Updated HRM informations are Completed.It's going to Close the Connection.");

         try {
            if (conn != null && !conn.isClosed()) {
               conn.close();
               conn = null;
            }
         } catch (Exception var598) {
            LogManager.log("Unable to Close connection.", (Throwable)var598);
         }

         return true;
      }
   }

   private void showCodeAccessButton(String sessionId, boolean showButton) {
      LogManager.log("AdminEmployeeBusinessLogic.showCodeAccessButton()");
      Session session = SessionManager.findSession(sessionId);
      if (!showButton) {
         LogManager.log("Button on");
         session.setAttribute("FWShowCodeAccessButtonStart", "<!--");
         session.setAttribute("FWShowCodeAccessButtonEnd", "-->");
      } else {
         LogManager.log("Button off");
         session.removeAttribute("FWShowCodeAccessButtonStart");
         session.removeAttribute("FWShowCodeAccessButtonEnd");
      }

   }

   private void showAllEmployeesButton(String sessionId, boolean showButton) {
      LogManager.log("AdminEmployeeBusinessLogic.showAllEmployeesButton()");
      Session session = SessionManager.findSession(sessionId);
      if (!showButton) {
         LogManager.log("Button on");
         session.setAttribute("FWShowAllEmployeesStart", "<!--");
         session.setAttribute("FWShowAllEmployeesEnd", "-->");
      } else {
         LogManager.log("Button off");
         session.removeAttribute("FWShowAllEmployeesStart");
         session.removeAttribute("FWShowAllEmployeesEnd");
      }

   }

   private void showallHrmData(String sessionId, boolean show) {
      LogManager.log("AdminEmployeeBusinessLogic.showallHrmData()");
      Session session = SessionManager.findSession(sessionId);
      if (!show) {
         LogManager.log("shown");
         session.setAttribute("FWShowAllHrmDataStart", "<!--");
         session.setAttribute("FWShowAllHrmDataEnd", "-->");
      } else {
         LogManager.log("not shown");
         session.removeAttribute("FWShowAllHrmDataStart");
         session.removeAttribute("FWShowAllHrmDataEnd");
      }

   }

   private String buildEvaluationParameters(String sessionId) {
      LogManager.log("AdminEmployeeBusinessLogic.buildEvaluationParameters");
      Session session = SessionManager.findSession(sessionId);
      session.setAttribute("FWEmployeeId", session.getAttribute("FWEmployeeID").toString());
      DataLogic dataLogic = this.module.getDataLogic();
      SqlStatement sql = null;
      Vector results = null;
      Connection conn = null;

      try {
         conn = dataLogic.openConnection();
         sql = dataLogic.buildSQLString(sessionId, "SelectEvaluationFiles");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var25) {
            dataLogic.rollbackTransaction(conn);
            throw var25;
         }

         dataLogic.commitTransaction(conn);
      } catch (Exception var26) {
         LogManager.log("exception: ", (Throwable)var26);
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var24) {
               LogManager.log("Unable to Close connection.", (Throwable)var24);
            }

            conn = null;
         }

      }

      SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy");
      SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
      StringBuffer sb = new StringBuffer();
      String date = "";

      try {
         if (!results.isEmpty()) {
            String empid = session.getAttribute("FWEmployeeID").toString();
            if (!session.getAttribute("FWCurrentPage").equals("5")) {
               sb.append("<table bgcolor=\"#C4C8FB\" border=\"1\" width=\"100%\" cellpadding=\"5\" bordercolor=\"#808080\" style=\"border-collapse: collapse\"><tr><td width=\"93%\" align=\"left\" colspan=\"4\"><p align=\"center\"><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Evaluation History</b></font></td></tr><tr bgcolor = \"#606BFD\" ><td width=\"24%\" align=\"left\" ><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Date</b></font></td><td width=\"68%\" align=\"left\" ><p align=\"center\"><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>File Name</b></font></td><td width=\"18%\" align=\"center\" ><b><font size=\"2\">Type</font></b></td><td width= \"18%\" align=\"left\" >&nbsp;</td></tr>");
            } else if (!SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") && !this.hrmEditUsers.contains(session.getUser().toString()) && !this.hrmDataEntryUsers.contains(session.getUser().toString())) {
               sb.append("<table bgcolor=\"#C4C8FB\" border=\"0\" width=\"100%\" cellspacing=\"0\" cellpadding=\"5\" bordercolor=\"#FFFFFF\"><tr><td><font size= \"2\"><a href=\"/servlets/ProxyServlet?FWModule=admin_employee&FWCurrentPage=8&FWArch=3\">Archive</a></font></td></tr></table>");
               sb.append("<table border=\"1\" bgcolor=\"#C4C8FB\" width=\"100%\" cellpadding=\"5\" bordercolor=\"#808080\" style=\"border-collapse: collapse\"><tr><td width=\"93%\" align=\"left\" colspan=\"4\"><p align=\"center\"><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Evaluation History</b></font></td></tr><tr bgcolor = \"#606BFD\" ><td width=\"24%\" align=\"left\" ><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Date</b></font></td><td width=\"68%\" align=\"left\" ><p align=\"center\"><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>File Name</b></font></td><td width=\"18%\" align=\"center\" ><b><font size=\"2\">Type</font></b></td><td width= \"18%\" align=\"left\" >&nbsp;</td></tr>");
            } else {
               sb.append("<table bgcolor=\"#C4C8FB\" border=\"0\" width=\"100%\" cellspacing=\"0\" cellpadding=\"5\" bordercolor=\"#FFFFFF\"><tr><td><font face=\"Tahoma\"><span style=\"font-size: 9pt\"><input type = button value = \"Evaluation Upload\" name = \"FWEvaluationUpload\" style=\"font-family: Tahoma; font-size: 10pt; border: 1px solid #808080\" onclick = \"evaluationSubmit();\"></span></font></td><td><font size= \"2\"><a href=\"/servlets/ProxyServlet?FWModule=admin_employee&FWCurrentPage=8&FWArch=3\">Archive</a></font></td></tr></table>");
               sb.append("<table border=\"1\" bgcolor=\"#C4C8FB\" width=\"100%\" cellpadding=\"5\" bordercolor=\"#808080\" style=\"border-collapse: collapse\"><tr><td width=\"93%\" align=\"left\" colspan=\"4\"><p align=\"center\"><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Evaluation History</b></font></td></tr><tr bgcolor = \"#606BFD\" ><td width=\"24%\" align=\"left\" ><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Date</b></font></td><td width=\"68%\" align=\"left\" ><p align=\"center\"><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>File Name</b></font></td><td width=\"18%\" align=\"center\" ><b><font size=\"2\">Type</font></b></td><td width= \"18%\" align=\"left\" >&nbsp;</td></tr>");
            }

            for(int i = 0; i < results.size(); ++i) {
               Vector row = (Vector)results.get(i);
               String fileName = (String)row.get(0);
               date = row.get(1).toString();
               String sno = row.get(2).toString();
               String evType = row.get(3).toString();
               if (evType.equals("0")) {
                  evType = "PIP";
               } else if (evType.equals("1")) {
                  evType = "Warning";
               } else if (evType.equals("2")) {
                  evType = "Verbal";
               }

               LogManager.log("Date is Comming here aa:" + date);
               if (!session.getAttribute("FWCurrentPage").equals("5")) {
                  if (!SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") && !this.hrmEditUsers.contains(session.getUser().toString()) && !this.hrmDataEntryUsers.contains(session.getUser().toString())) {
                     sb.append("<tr  ><td width=\"24%\" align=\"left\" style=\"font-size: 10pt; border: 1px solid #808080\"><font face=\"Tahoma\"><span style=\"font-size: 9pt\">" + formatter.format(parser.parse(date)) + "</span></font></td><td width=\"68%\" align=\"left\">" + "<A href = /servlet/Download?product_name=" + URLEncoder.encode(fileName) + "&EmployeeID=" + empid + "&SessionId=" + sessionId + ">" + "<font face=\"Tahoma\" style=\"font-size: 9pt\">" + fileName + "</font></td>" + "<td width=\"18%\" align=\"center\" >" + "<b><font size=\"2\">" + evType + "</font></b></td>" + "</tr>");
                  } else {
                     sb.append("<tr  ><td width=\"24%\" align=\"left\" style=\"font-size: 10pt; border: 1px solid #808080\"><font face=\"Tahoma\"><span style=\"font-size: 9pt\">" + formatter.format(parser.parse(date)) + "</span></font></td><td width=\"68%\" align=\"left\">" + "<A href = /servlet/Download?product_name=" + URLEncoder.encode(fileName) + "&EmployeeID=" + empid + "&SessionId=" + sessionId + ">" + "<font face=\"Tahoma\" style=\"font-size: 9pt\">" + fileName + "</font></td>" + "<td width=\"18%\" align=\"center\" >" + "<b><font size=\"2\">" + evType + "</font></b></td>" + "<input type = hidden value = \"" + sno + "\" name = \"FWEvaluSno" + i + "\"></tr>");
                  }
               } else if (!SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") && !this.hrmEditUsers.contains(session.getUser().toString()) && !this.hrmDataEntryUsers.contains(session.getUser().toString())) {
                  sb.append("<tr ><td width=\"24%\" align=\"left\" style=\"font-size: 10pt; border: 1px solid #808080\"><font face=\"Tahoma\"><span style=\"font-size: 9pt\">" + formatter.format(parser.parse(date)) + "</span></font></td><td width=\"68%\" align=\"left\">" + "<A href = /servlet/Download?product_name=" + URLEncoder.encode(fileName) + "&EmployeeID=" + empid + "&SessionId=" + sessionId + ">" + "<font face=\"Tahoma\" style=\"font-size: 9pt\">" + fileName + "</font></td>" + "<td width=\"18%\" align=\"center\" >" + "<b><font size=\"2\">" + evType + "</font></b></td>" + "<td></td>" + "<input type = hidden value = \"" + sno + "\" name = \"FWEvaluSno" + i + "\"></tr>");
               } else {
                  sb.append("<tr  ><td width=\"24%\" align=\"left\" style=\"font-size: 10pt; border: 1px solid #808080\"><font face=\"Tahoma\"><span style=\"font-size: 9pt\"><input type= text value =\"" + formatter.format(parser.parse(date)) + "\"size=\"11\"  name=\"FWDate" + i + "\" style=\"font-family: Tahoma; font-size: 10pt; border: 1px solid #808080\"></span></font></td><td width=\"68%\" align=\"left\">" + "<A href = /servlet/Download?product_name=" + URLEncoder.encode(fileName) + "&EmployeeID=" + empid + "&SessionId=" + sessionId + ">" + "<font face=\"Tahoma\" style=\"font-size: 9pt\">" + fileName + "</font></td>" + "<td width=\"18%\" align=\"center\" >" + "<b><font size=\"2\">" + evType + "</font></b></td>" + "<td width= \"8%\" align=\"center\" ><a href=\"/servlets/ProxyServlet?FWModule=admin_employee&FWEvaluation_Sno=" + sno + "\" onclick = \"return (confirm('Are you sure?'))\" ><img src = \"/assets/images/recyclebin_hrm.gif\" border =0 ></img></a> </td>" + "<input type = hidden value = \"" + sno + "\" name = \"FWEvaluSno" + i + "\"></tr>");
               }
            }

            sb.append("</table>");
            session.setAttribute("FWiValue", String.valueOf(results.size()));
            sb.append("<input type = hidden name = \"FWCountOfDateTextBox\" value = \"" + results.size() + "\">");
         } else {
            if (!session.getAttribute("FWCurrentPage").equals("5")) {
               sb.append("<table bgcolor=\"#C4C8FB\" border=\"1\" width=\"100%\" cellpadding=\"5\" bordercolor=\"#808080\" style=\"border-collapse: collapse\"><tr><td width=\"93%\" align=\"left\" colspan=\"2\"><p align=\"left\"><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>No Evaluation Files Attached.</b></font></td></tr>");
            } else if (!SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") && !this.hrmEditUsers.contains(session.getUser().toString()) && !this.hrmDataEntryUsers.contains(session.getUser().toString())) {
               sb.append("<table bgcolor=\"#C4C8FB\" border=\"0\" width=\"100%\" cellspacing=\"0\" cellpadding=\"5\" bordercolor=\"#FFFFFF\"><tr><td><font size= \"2\"><a href=\"/servlets/ProxyServlet?FWModule=admin_employee&FWCurrentPage=8&FWArch=3\">Archive</a></font></td></tr></table>");
               sb.append("<table  bgcolor=\"#C4C8FB\" border=\"1\" width=\"100%\" cellpadding=\"5\" bordercolor=\"#808080\" style=\"border-collapse: collapse\"><tr><td width=\"93%\" align=\"left\" colspan=\"2\"><p align=\"center\"><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>No Evaluation Files Attached.</b></font></td></tr>");
            } else {
               sb.append("<table  bgcolor=\"#C4C8FB\" border=\"0\" width=\"100%\" cellspacing=\"0\" cellpadding=\"5\" bordercolor=\"#FFFFFF\"><tr><td><font face=\"Tahoma\"><span style=\"font-size: 9pt\"><input type = button value = \"Evaluation Upload\" name = \"FWEvaluationUpload\" style=\"font-family: Tahoma; font-size: 10pt; border: 1px solid #808080\" onclick = \"evaluationSubmit();\"> </span></font></td><td><font size= \"2\"><a href=\"/servlets/ProxyServlet?FWModule=admin_employee&FWCurrentPage=8&FWArch=3\">Archive</a></font></td></tr></table>");
               sb.append("<table bgcolor=\"#C4C8FB\" border=\"1\" width=\"100%\" cellpadding=\"5\" bordercolor=\"#808080\" style=\"border-collapse: collapse\"><tr><td width=\"93%\" align=\"left\" colspan=\"2\"><p align=\"center\"><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>No Evaluation Files Attached.</b></font></td></tr>");
            }

            sb.append("</table>");
         }
      } catch (Exception var28) {
         LogManager.log("Date is:", (Throwable)var28);
      }

      session.setAttribute("FWHost", System.getProperty("europa.app.host"));
      LogManager.log("Session Date Value:" + session.getAttribute("FWDate0"));
      return sb.toString();
   }

   private String buildResumeParameters(String sessionId) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.buildResumeParameters");
      Session session = SessionManager.findSession(sessionId);
      DataLogic dataLogic = this.module.getDataLogic();
      SqlStatement sql = null;
      Vector results = null;
      Connection conn = null;
      session.setAttribute("FWSessionVal", sessionId);

      try {
         conn = dataLogic.openConnection();
         sql = dataLogic.buildSQLString(sessionId, "SelectPlaiTextClob");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var29) {
            dataLogic.rollbackTransaction(conn);
            throw var29;
         }

         dataLogic.commitTransaction(conn);
      } catch (Exception var30) {
         LogManager.log("FileName exception:", (Throwable)var30);
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var28) {
               LogManager.log("Unable to Close connection.", (Throwable)var28);
            }

            conn = null;
         }

      }

      StringBuffer sb = new StringBuffer();
      String fileName = "";
      if (!results.isEmpty()) {
         Vector row = (Vector)results.get(0);
         String text_clob1 = (String)row.get(0);
         String text_clob2 = (String)row.get(1);
         String text_clob3 = (String)row.get(2);
         String text_clob4 = (String)row.get(3);
         String text_clob5 = (String)row.get(4);
         String text_clob6 = (String)row.get(5);
         String text_clob7 = (String)row.get(6);
         String text_clob8 = (String)row.get(7);
         String text_clob9 = (String)row.get(8);
         String text_clob10 = (String)row.get(9);
         String text_clob11 = (String)row.get(10);
         fileName = (String)row.get(12);
         String text_clob = text_clob1 + text_clob2 + text_clob3 + text_clob4 + text_clob5 + text_clob6 + text_clob7 + text_clob8 + text_clob9 + text_clob10 + text_clob11;
         session.setAttribute("FWTextAeraContent", text_clob);
      } else {
         session.setAttribute("FWTextAeraContent", "");
      }

      sb.append("<A href = /servlet/DownloadResume?product_name=" + URLEncoder.encode(fileName) + "&EmployeeID=" + session.getAttribute("FWEmployeeID").toString() + "&SessionId=" + sessionId + ">" + fileName + "</a>");
      session.setAttribute("FWHost", System.getProperty("europa.app.host"));
      return sb.toString();
   }

   private void showEmpSecureInfo(String sessionId, boolean showButton) {
      LogManager.log("AdminEmployeeBusinessLogic.showEmpSecureInfo()");
      Session session = SessionManager.findSession(sessionId);
      if (!showButton) {
         LogManager.log("Button on");
         session.setAttribute("FWShowEmpSecureInfoStart", "<!--");
         session.setAttribute("FWShowEmpSecureInfoEnd", "-->");
      } else {
         LogManager.log("Button off");
         session.removeAttribute("FWShowEmpSecureInfoStart");
         session.removeAttribute("FWShowEmpSecureInfoEnd");
      }

   }

   private void showHRMInfo(String sessionId, boolean showButton) {
      LogManager.log("AdminEmployeeBusinessLogic.showHRMInfo()");
      Session session = SessionManager.findSession(sessionId);
      if (!showButton) {
         LogManager.log("Button on");
         session.setAttribute("FWShowHRMInfoStart", "<!--");
         session.setAttribute("FWShowHRMInfoEnd", "-->");
      } else {
         LogManager.log("Button off");
         session.removeAttribute("FWShowHRMInfoStart");
         session.removeAttribute("FWShowHRMInfoEnd");
      }

   }

   private void buildCodeAccessPage(String sessionId) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.buildCodeAccessPage()");
      Session session = SessionManager.findSession(sessionId);
      String empFirstName = (String)session.getAttribute("FWFirstName");
      String empLastName = (String)session.getAttribute("FWLastName");
      session.setAttribute("FWFirstName", empFirstName);
      session.setAttribute("FWLastName", empLastName);
      TreeSet availableCodes = new TreeSet();
      TreeSet grantedCodes = new TreeSet();
      DataLogic dataLogic = this.module.getDataLogic();
      Connection conn = dataLogic.openConnection();

      try {
         SqlStatement sql = null;
         Vector results = null;
         sql = dataLogic.buildSQLString(sessionId, "SelectEmployeeActivity");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var63) {
            dataLogic.rollbackTransaction(conn);
            throw var63;
         }

         dataLogic.commitTransaction(conn);
         LogManager.log("Line 259 " + results.size());
         int sizeOfResult = results.size();
         Vector resultsTot;
         if (sizeOfResult > 0) {
            for(int i = 0; i < results.size(); ++i) {
               resultsTot = (Vector)results.get(i);
               String desc = resultsTot.get(0).toString();
               LogManager.log("LineNo. 264" + desc);
               grantedCodes.add(desc);
            }
         }

         SqlStatement sqlTot = null;
         resultsTot = null;
         sqlTot = dataLogic.buildSQLString(sessionId, "SelectTotalActivity");
         dataLogic.startTransaction(conn);

         try {
            resultsTot = dataLogic.execute(conn, sqlTot);
         } catch (EuropaException var62) {
            dataLogic.rollbackTransaction(conn);
            throw var62;
         }

         dataLogic.commitTransaction(conn);

         Vector sqlstmt;
         Integer timeOff;
         for(int j = 0; j < resultsTot.size(); ++j) {
            sqlstmt = (Vector)resultsTot.get(j);
            timeOff = (Integer)sqlstmt.get(0);
            String desc = sqlstmt.get(1).toString();
            String actDet = sqlstmt.get(2).toString();
            LogManager.log("LineNo. 299 " + timeOff + " " + desc);
            availableCodes.add(actDet);
         }

         Iterator itr = grantedCodes.iterator();

         while(itr.hasNext()) {
            availableCodes.remove(itr.next());
         }

         sqlstmt = null;
         timeOff = null;
         SqlStatement sqlstmt1 = dataLogic.buildSQLString(sessionId, "SelectTimeoffHours");
         dataLogic.startTransaction(conn);

         Vector timeOff1;
         try {
            timeOff1 = dataLogic.execute(conn, sqlstmt1);
         } catch (EuropaException var61) {
            dataLogic.rollbackTransaction(conn);
            throw var61;
         }

         dataLogic.commitTransaction(conn);
         double compTime = 0.0;
         double vacation = 0.0;
         double pbaTime = 0.0;
         double holidayFloating = 0.0;

         for(int i = 0; i < timeOff1.size(); ++i) {
            Vector temp = (Vector)timeOff1.get(i);
            int actid = Integer.parseInt(temp.get(0).toString());
            if (actid == 1055) {
               vacation = Double.parseDouble(temp.get(1).toString());
            } else if (actid == 1315) {
               pbaTime = Double.parseDouble(temp.get(1).toString());
            } else if (actid == 1515) {
               compTime = Double.parseDouble(temp.get(1).toString());
            } else if (actid == 2235) {
               holidayFloating = Double.parseDouble(temp.get(1).toString());
            }
         }

         boolean excludeFlag = false;
         double stbal_CompTime = 0.0;
         double stbal_Vacation = 0.0;
         double stbal_PbaTime = 0.0;
         double stbal_HolidayFloating = 0.0;
         String payrollDeptId = "";
         sqlstmt1 = null;
         Vector timeOffBal = null;
         sqlstmt1 = dataLogic.buildSQLString(sessionId, "SelectTimeOffStartBalance");
         dataLogic.startTransaction(conn);

         try {
            timeOffBal = dataLogic.execute(conn, sqlstmt1);
         } catch (EuropaException var60) {
            dataLogic.rollbackTransaction(conn);
            throw var60;
         }

         dataLogic.commitTransaction(conn);

         for(int i = 0; i < timeOffBal.size(); ++i) {
            Vector temp = (Vector)timeOffBal.get(i);
            stbal_Vacation = Double.parseDouble(temp.get(0).toString());
            stbal_PbaTime = Double.parseDouble(temp.get(1).toString());
            stbal_CompTime = Double.parseDouble(temp.get(2).toString());
            stbal_HolidayFloating = Double.parseDouble(temp.get(4).toString());
            payrollDeptId = temp.get(3).toString();
         }

         String excludedpayrolldepts = System.getProperty("europa.module.employees.ExcludedTimeOffPayRollDept");
         StringTokenizer sb = new StringTokenizer(excludedpayrolldepts, ",");

         while(sb.hasMoreTokens()) {
            String excludeid = sb.nextToken();
            if (excludeid.equalsIgnoreCase(payrollDeptId)) {
               excludeFlag = true;
            }
         }

         StringBuffer availCodes = new StringBuffer();
         StringBuffer grantCodes = new StringBuffer();
         String tempKey = null;
         Iterator availCodesIterator = availableCodes.iterator();
         String fontColor;
         if (availCodesIterator != null) {
            for(; availCodesIterator.hasNext(); availCodes.append("<option value=\"").append(tempKey).append("\" " + fontColor + " >").append(tempKey).append("\n")) {
               tempKey = (String)availCodesIterator.next();
               fontColor = "";
               if (!excludeFlag) {
                  if (tempKey.equalsIgnoreCase("ADMN-Vacation")) {
                     if (vacation >= stbal_Vacation) {
                        fontColor = "style=\"color: Red\"";
                     }
                  } else if (tempKey.equalsIgnoreCase("ADMN-PBA")) {
                     if (pbaTime >= stbal_PbaTime) {
                        fontColor = "style=\"color: Red\"";
                     }
                  } else if (tempKey.equalsIgnoreCase("ADMN-Comp Time") && compTime >= stbal_CompTime) {
                     fontColor = "style=\"color: Red\"";
                  } else if (tempKey.equalsIgnoreCase("ADMN-Holiday-Floating") && holidayFloating >= stbal_HolidayFloating) {
                     fontColor = "style=\"color: Red\"";
                  }
               }
            }
         }

         Iterator grantCodesIterator = grantedCodes.iterator();
         if (grantCodesIterator != null) {
            for(; grantCodesIterator.hasNext(); grantCodes.append("<option value=\"").append(tempKey).append("\" " + fontColor + " >").append(tempKey).append("</option>\n")) {
               tempKey = (String)grantCodesIterator.next();
               fontColor = "";
               if (!excludeFlag) {
                  if (tempKey.equalsIgnoreCase("ADMN-Vacation")) {
                     if (vacation >= stbal_Vacation) {
                        fontColor = "style=\"color: Red\"";
                     }
                  } else if (tempKey.equalsIgnoreCase("ADMN-PBA")) {
                     if (pbaTime >= stbal_PbaTime) {
                        fontColor = "style=\"color: Red\"";
                     }
                  } else if (tempKey.equalsIgnoreCase("ADMN-Comp Time") && compTime >= stbal_CompTime) {
                     fontColor = "style=\"color: Red\"";
                  } else if (tempKey.equalsIgnoreCase("ADMN-Holiday-Floating") && holidayFloating >= stbal_HolidayFloating) {
                     fontColor = "style=\"color: Red\"";
                  }
               }
            }
         }

         Vector eViewUser = new Vector();
         String projectViewUsers = System.getProperty("europa.employeeviewpermittedusers");
         LogManager.log("EmployeeViewUsers:" + projectViewUsers);
         StringTokenizer st1 = new StringTokenizer(projectViewUsers, ",");

         String empViewUsersActivitycodes;
         while(st1.hasMoreTokens()) {
            empViewUsersActivitycodes = st1.nextToken().toString();
            eViewUser.add(empViewUsersActivitycodes);
         }

         LogManager.log("eViewUser:" + eViewUser);
         empViewUsersActivitycodes = System.getProperty("europa.employeeactivitycodes.editaccess");
         LogManager.log("EmployeeViewUsers:" + empViewUsersActivitycodes);
         StringTokenizer st2 = new StringTokenizer(empViewUsersActivitycodes, ",");
         Vector empEditUsers = new Vector();

         while(st2.hasMoreTokens()) {
            String employeeName = st2.nextToken().toString();
            empEditUsers.add(employeeName);
         }

         if (eViewUser.contains(session.getUser().toString())) {
            session.setAttribute("FWAvailableCodes", availCodes.toString());
            session.setAttribute("FWGrantedCodes", grantCodes.toString());
            session.setAttribute("FWEmployeeCodeViewStart", "<!--");
            session.setAttribute("FWEmployeeCodeViewEnd", "-->");
         } else {
            session.setAttribute("FWAvailableCodes", availCodes.toString());
            session.setAttribute("FWGrantedCodes", grantCodes.toString());
            session.setAttribute("FWEmployeeCodeViewStart", "");
            session.setAttribute("FWEmployeeCodeViewEnd", "");
         }

         if (SecurityManager.isMember("admin", session.getUser())) {
            if (SecurityManager.getUser(session.getUser().toString()).toString().equals("admin")) {
               session.setAttribute("FWEmployeeCodeViewStart", "");
               session.setAttribute("FWEmployeeCodeViewEnd", "");
            } else {
               session.setAttribute("FWEmployeeCodeViewStart", "<!--");
               session.setAttribute("FWEmployeeCodeViewEnd", "-->");
            }
         }

         if (empEditUsers.contains(session.getUser().toString())) {
            session.setAttribute("FWAvailableCodes", availCodes.toString());
            session.setAttribute("FWGrantedCodes", grantCodes.toString());
            session.setAttribute("FWEmployeeCodeViewStart", "");
            session.setAttribute("FWEmployeeCodeViewEnd", "");
         }
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var59) {
               LogManager.log("Unable to Close connection.", (Throwable)var59);
            }

            conn = null;
         }

      }

   }

   private void buildHRMInfoPage(String sessionId) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.buildHRMInfoPage()");
      Session session = SessionManager.findSession(sessionId);
      String empFirstName = (String)session.getAttribute("FWFirstName");
      String empLastName = (String)session.getAttribute("FWLastName");
      String empSalaryAmount = (String)session.getAttribute("FWSalaryAmount");
      SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy");
      SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
      DecimalFormat currency = new DecimalFormat("#,##0.00");
      DataLogic dataLogic = this.module.getDataLogic();
      Connection conn = dataLogic.openConnection();
      SqlStatement sql = null;
      Vector results = null;
      StringBuffer sb = new StringBuffer();

      try {
         String PROJECTLEADERLABEL = session.getAttribute("FW_PROJECTLEADER_GROUPLABEL").toString();
         String SRSALESENGINEERLABEL = session.getAttribute("FW_SRSALESENGINEER_GROUPLABEL").toString();
         String SALESENGINEERLABEL = session.getAttribute("FW_SALESENGINEER_GROUPLABEL").toString();
         String VERTICALLABEL = session.getAttribute("FW_VERTICAL_GROUPLABEL").toString();
         String JRPARTNERLABEL = session.getAttribute("FW_JRPARTNER_GROUPLABEL").toString();
         String DIRECTREPLABEL = session.getAttribute("FW_DIRECTREP_GROUPLABEL").toString();
         String ANALYSISVERTICALLABEL = session.getAttribute("FW_ANALYSISVERTICAL_GROUPLABEL").toString();
         String ELECTRICALVERTICALLABEL = session.getAttribute("FW_ELECTRICALVERTICAL_GROUPLABEL").toString();
         String MANUFACTURINGVERTICALLABEL = session.getAttribute("FW_MANUFACTURINGVERTICAL_GROUPLABEL").toString();
         String CHECKERSLABEL = session.getAttribute("FW_CHECKERS_GROUPLABEL").toString();
         String ENGADMINLABEL = session.getAttribute("FW_ENGADMIN_GROUPLABEL").toString();
         String ACCOUNTEXECLABEL = session.getAttribute("FW_ACCOUNTEXEC_GROUPLABEL").toString();
         String ADMINLABEL = session.getAttribute("FW_ADMIN_GROUPLABEL").toString();
         String PEOPLEMANAGERLABEL = session.getAttribute("FW_PEOPLEMANAGER_GROUPLABEL").toString();
         String POWERSYSTEM_GROUPLABEL = session.getAttribute("FW_POWERSYSTEM_GROUPLABEL").toString();
         String ENGMANAGER_GROUPLABEL = session.getAttribute("FW_ENGMANAGER_GROUPLABEL").toString();
         String LEADENGINEER_GROUPLABEL = session.getAttribute("FW_LEADENGINEER_GROUPLABEL").toString();
         String IsTOPLINEMANAGER_GROUPLABEL = session.getAttribute("FW_IsTOPLINEMANAGER_GROUPLABEL").toString();
         String TOPLINEMANAGERID_GROUPLABEL = session.getAttribute("FW_TOPLINEMANAGERID_GROUPLABEL").toString();
         String ANALYSISVERTICALEASTLABEL = session.getAttribute("FW_ANALYSISVERTICALEAST_GROUPLABEL").toString();
         String ANALYSISVERTICALWESTLABEL = session.getAttribute("FW_ANALYSISVERTICALWEST_GROUPLABEL").toString();
         String RESOURCEMANAGERLABEL = session.getAttribute("FW_RESOURCEMANAGER_GROUPLABEL").toString();
         String OffShoreManagerLabel = session.getAttribute("FW_OFFSHOREMANAGER_GROUPLABEL").toString();
         String ElectricalSystemsIntegrationLabel = session.getAttribute("FW_ELECTRICALSYSTEMINTEGRATION_GROUPLABEL").toString();
         String chkPL = (String)session.getAttribute("FWisPL");
         if (!chkPL.equals("checked") && !chkPL.equals("Project Leader")) {
            session.setAttribute("FWisPL", " ");
         } else {
            session.setAttribute("FWisPL", "Project Leader");
            sb.append(PROJECTLEADERLABEL + ",");
         }

         String chkSE = (String)session.getAttribute("FWisSE");
         if (!chkSE.equals("checked") && !chkSE.equals("Sales Engineer")) {
            session.setAttribute("FWisSE", " ");
         } else {
            session.setAttribute("FWisSE", "Sales Engineer");
            sb.append(SALESENGINEERLABEL + ",");
         }

         String isSSE = (String)session.getAttribute("FWisSSE");
         if (!isSSE.equals("checked") && !isSSE.equals("Senior Sales Engineer")) {
            session.setAttribute("FWisSSE", " ");
         } else {
            session.setAttribute("FWisSSE", "Senior Sales Engineer");
            sb.append(SRSALESENGINEERLABEL + ",");
         }

         String chkDR = (String)session.getAttribute("FWisDR");
         if (!chkDR.equals("checked") && !chkDR.equals("Direct Rep")) {
            session.setAttribute("FWisDR", " ");
         } else {
            session.setAttribute("FWisDR", "Direct Rep");
            sb.append(DIRECTREPLABEL + ",");
         }

         String chkVer = (String)session.getAttribute("FWisVer");
         if (!chkVer.equals("checked") && !chkVer.equals("Vertical Manager")) {
            session.setAttribute("FWisVer", " ");
         } else {
            session.setAttribute("FWisVer", "Vertical Manager");
            sb.append(VERTICALLABEL + ",");
         }

         String chkAM = (String)session.getAttribute("FWisAM");
         if (!chkAM.equals("checked") && !chkAM.equals("Business Devpt")) {
            session.setAttribute("FWisAM", " ");
         } else {
            session.setAttribute("FWisAM", "Business Devpt");
            sb.append(ACCOUNTEXECLABEL + ",");
         }

         String chkAnv = (String)session.getAttribute("FWisAnv");
         if (!chkAnv.equals("checked") && !chkAnv.equals("Analysis Vertical")) {
            session.setAttribute("FWisAnv", " ");
         } else {
            session.setAttribute("FWisAnv", "Analysis Vertical");
            sb.append(ANALYSISVERTICALLABEL + ",");
         }

         String chkElv = (String)session.getAttribute("FWisElv");
         if (!chkElv.equals("checked") && !chkElv.equals("Electrical Vertical")) {
            session.setAttribute("FWisElv", " ");
         } else {
            session.setAttribute("FWisElv", "Electrical Vertical");
            sb.append(ELECTRICALVERTICALLABEL + ",");
         }

         String chkMfv = (String)session.getAttribute("FWisMfv");
         if (!chkMfv.equals("checked") && !chkMfv.equals("Manufactring Vertical")) {
            session.setAttribute("FWisMfv", " ");
         } else {
            session.setAttribute("FWisMfv", "Manufactring Vertical");
            sb.append(MANUFACTURINGVERTICALLABEL + ",");
         }

         String isEngAdmin = (String)session.getAttribute("FWisEAdmn");
         if (!isEngAdmin.equals("checked") && !isEngAdmin.equals("Engineering Admin")) {
            session.setAttribute("FWisEAdmn", " ");
         } else {
            session.setAttribute("FWisEAdmn", "Engineering Admin");
            sb.append(ENGADMINLABEL + ",");
         }

         String isChecker = (String)session.getAttribute("FWisChk");
         if (!isChecker.equals("checked") && !isChecker.equals("Checker")) {
            session.setAttribute("isChecker", " ");
         } else {
            session.setAttribute("FWisChk", "Checker");
            sb.append(CHECKERSLABEL + ",");
         }

         String isJp = (String)session.getAttribute("FWisJpr");
         if (!isJp.equals("checked") && !isJp.equals("Checker")) {
            session.setAttribute("FWisJpr", " ");
         } else {
            session.setAttribute("FWisJpr", "Checker");
            sb.append(JRPARTNERLABEL + ",");
         }

         String isPM = (String)session.getAttribute("FWisPManager");
         LogManager.log(" isPM" + isPM);
         if (!isPM.equals("checked") && !isPM.equals("Prople Manager")) {
            session.setAttribute("FWisPManager", " ");
         } else {
            session.setAttribute("FWisPManager", "Prople Manager");
            sb.append(PEOPLEMANAGERLABEL + ",");
         }

         String isAdminGP = (String)session.getAttribute("FWisAdminGP");
         if (!isAdminGP.equalsIgnoreCase("checked") && !isAdminGP.equalsIgnoreCase("Admin Group")) {
            session.setAttribute("FWisAdminGP", " ");
         } else {
            session.setAttribute("FWisAdminGP", "Admin Group");
            sb.append(ADMINLABEL + ",");
         }

         String isPSystem = (String)session.getAttribute("FWisPSystem");
         LogManager.log(" isPSystem" + isPSystem);
         if (!isPSystem.equals("checked") && !isPSystem.equals("Power System")) {
            session.setAttribute("FWisPSystem", " ");
         } else {
            session.setAttribute("FWisPSystem", "Power System");
            sb.append(POWERSYSTEM_GROUPLABEL + ",");
         }

         String isOffShoreMgr = (String)session.getAttribute("FWisOffShoerMgr");
         LogManager.log(" FWisOffShoerMgr" + isOffShoreMgr);
         if (!isOffShoreMgr.equals("checked") && !isOffShoreMgr.equals("offShore Manager")) {
            session.setAttribute("FWisOffShoerMgr", " ");
         } else {
            session.setAttribute("FWisOffShoerMgr", "offShore Manager");
            sb.append(OffShoreManagerLabel + ",");
         }

         String isElectricalSysIntegrationMgr = (String)session.getAttribute("FWisElectricalSysIntegration");
         LogManager.log(" FWisElectricalSysIntegration" + isElectricalSysIntegrationMgr);
         if (!isElectricalSysIntegrationMgr.equals("checked") && !isElectricalSysIntegrationMgr.equals("Electrical Systems Integration")) {
            session.setAttribute("FWisElectricalSysIntegration", " ");
         } else {
            session.setAttribute("FWisElectricalSysIntegration", "Electrical Systems Integration");
            sb.append(OffShoreManagerLabel + ",");
         }

         String isLEng = (String)session.getAttribute("FWisLEng");
         LogManager.log(" isLEng" + isLEng);
         if (!isLEng.equals("checked") && !isLEng.equals("Lead Engineer")) {
            session.setAttribute("FWisLEng", " ");
         } else {
            session.setAttribute("FWisLEng", "Lead Engineer");
            sb.append(LEADENGINEER_GROUPLABEL + ",");
         }

         String isEMgr = (String)session.getAttribute("FWisEMgr");
         LogManager.log(" isEMgr" + isEMgr);
         if (!isEMgr.equals("checked") && !isEMgr.equals("Engineering Manager")) {
            session.setAttribute("FWisEMgr", " ");
         } else {
            session.setAttribute("FWisEMgr", "Engineering Manager");
            sb.append(ENGMANAGER_GROUPLABEL + ",");
         }

         LogManager.log("comming to logmanger");
         sql = dataLogic.buildSQLString(sessionId, "SelectSpotBonusAudit");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var71) {
            dataLogic.rollbackTransaction(conn);
            throw var71;
         }

         try {
            if (!results.isEmpty()) {
               Vector row = (Vector)results.get(0);
               String amount = row.get(0).toString();
               String date = row.get(1).toString();
               LogManager.log("date is:" + date + "-------" + parser.parse(date) + "----" + formatter.format(parser.parse(date)));
               session.setAttribute("FWSpotBonuses", amount);
               session.setAttribute("FWSpotBonusesDate", String.valueOf(formatter.format(parser.parse(date))));
               session.setAttribute("OldFWSpotBonuses", amount);
               session.setAttribute("OldFWSpotBonusesDate", String.valueOf(formatter.format(parser.parse(date))));
            }
         } catch (Exception var70) {
            LogManager.log("exception: ", (Throwable)var70);
         }

         this.editHRMInfoPage(sessionId);
         session.setAttribute("FWFirstName", empFirstName);
         session.setAttribute("FWLastName", empLastName);
         session.setAttribute("FWSalaryAmount", currency.format(Double.parseDouble(empSalaryAmount.replaceAll(",", ""))));

         try {
            if (sb.charAt(sb.length() - 1) == ',') {
               sb.deleteCharAt(sb.length() - 1);
            }
         } catch (Exception var69) {
            LogManager.log("exception: ", (Throwable)var69);
         }

         session.setAttribute("FWPositionDetails", sb.toString());
         String str = this.empSalaryAudit(sessionId);
         session.setAttribute("FWWageHistory", str);
         session.setAttribute("FWHelmetAudit", this.buildHelmetAwardsDetails(sessionId));
         session.setAttribute("FWTutionAmoutHistory", this.buildTuitionAndReimbursementDetails(sessionId));
         session.setAttribute("FWFlexTime", this.buildFlexTimeHours(sessionId));
         session.setAttribute("FWBankTime", this.buildBankTime(sessionId));
         LogManager.log("Calling EditHRm Function");
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var68) {
               LogManager.log("Unable to Close connection.", (Throwable)var68);
            }

            conn = null;
         }

      }

   }

   private void editHRMInfoPage(String sessionId) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.editHRMInformation()");
      Session session = SessionManager.findSession(sessionId);
      DataLogic dataLogic = this.module.getDataLogic();
      Connection conn = dataLogic.specificConnection();
      SqlStatement sql = null;
      Vector results = null;

      try {
         sql = dataLogic.buildSQLString(sessionId, "SelectHRMInfo");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var46) {
            dataLogic.rollbackTransaction(conn);
            throw var46;
         }

         dataLogic.commitTransaction(conn);
         DecimalFormat decimal = new DecimalFormat("#,##0.00");
         if (results.size() > 1) {
            throw new EuropaException("Duplicate EmployeeID");
         }

         Vector employee = (Vector)results.get(0);
         String empCollege = employee.get(0).toString();
         String empRelocate = employee.get(1).toString();
         int hrmJpid = Integer.parseInt(employee.get(3).toString());
         String priorExp = (String)employee.get(4);
         BLOB blob = (BLOB)employee.get(5);

         try {
            if (blob.length() > 0L) {
               session.setAttribute("FWResumeLink", this.buildResumeParameters(sessionId));
            } else {
               session.setAttribute("FWResumeLink", "");
            }
         } catch (Exception var45) {
            LogManager.log("exception: ", (Throwable)var45);
         }

         session.setAttribute("FWPExp", priorExp);
         String ivpAreaHtmlString = this.buildIvpAreaDetails(sessionId, false);
         session.setAttribute("FWIvpAreaDisplay", ivpAreaHtmlString);
         String empRelocateDescription = employee.get(2).toString();
         session.setAttribute("FWHrmJpId", String.valueOf(hrmJpid));
         session.setAttribute("FWCollege", empCollege);
         if (empRelocate.equals("1")) {
            session.setAttribute("FWYesRelocate", "checked");
            session.setAttribute("FWNoRelocate", "");
         } else {
            session.setAttribute("FWNoRelocate", "checked");
            session.setAttribute("FWYesRelocate", "");
         }

         if (empRelocate.equals("1")) {
            session.setAttribute("FWWillingtoRelocate", "Yes");
         } else {
            session.setAttribute("FWWillingtoRelocate", "No");
         }

         session.setAttribute("FWRelocateDescription", empRelocateDescription);

         String empTermDate;
         String years;
         String termedYear;
         String tYear;
         int termedYearexp;
         String termedDayExp;
         String termedMonExp;
         try {
            empTermDate = (String)session.getAttribute("FWHireDate");
            years = "";
            termedYear = "";
            tYear = "";
            termedYearexp = Integer.parseInt(empTermDate.substring(6, 8));
            termedDayExp = empTermDate.substring(3, 5);
            termedMonExp = empTermDate.substring(0, 2);
            if (termedYearexp >= 80) {
               years = "19" + termedYearexp;
               termedYear = termedMonExp + "/" + termedDayExp + "/" + years;
            } else {
               LogManager.log("else condition");
               if (termedYearexp <= 9) {
                  tYear = "200" + String.valueOf(termedYearexp);
               } else {
                  tYear = "20" + String.valueOf(termedYearexp);
               }

               termedYear = termedMonExp + "/" + termedDayExp + "/" + tYear;
            }

            LogManager.log("Hiredate" + termedYear);
            LogManager.log("YEar" + termedYearexp + termedMonExp + termedDayExp);
            LogManager.log("Hiredate in Exp " + termedYear);
            session.setAttribute("FWHireDateEXp", termedYear);
            session.setAttribute("FWEmpTermDate", "");
         } catch (Exception var44) {
            LogManager.log("Employee creation Exception e:", (Throwable)var44);
         }

         try {
            empTermDate = (String)session.getAttribute("FWTermDate");
            years = "";
            termedYear = "";
            tYear = "";
            termedYearexp = Integer.parseInt(empTermDate.substring(6, 8));
            termedDayExp = empTermDate.substring(3, 5);
            termedMonExp = empTermDate.substring(0, 2);
            if (termedYearexp >= 80) {
               years = "19" + termedYearexp;
               termedYear = termedMonExp + "/" + termedDayExp + "/" + years;
            } else {
               if (termedYearexp <= 9) {
                  tYear = "200" + String.valueOf(termedYearexp);
               } else {
                  tYear = "20" + String.valueOf(termedYearexp);
               }

               termedYear = termedMonExp + "/" + termedDayExp + "/" + tYear;
            }

            session.removeAttribute("FWEmpTermDate");
            Date terminationDate = new Date(termedYear);
            Date currDate = new Date();
            if (terminationDate.after(currDate)) {
               try {
                  SimpleDateFormat termDateFormatter = new SimpleDateFormat("MM/dd/yyyy");
                  String sysDate = termDateFormatter.format(currDate);
                  LogManager.log("If TermDate > currentDate then use sysdate to calculate Exp. sysDate = :" + sysDate);
                  session.setAttribute("FWEmpTermDate", sysDate);
               } catch (Exception var42) {
                  LogManager.log("Exception in Simple Date Formatter of editHRMInfo() :", (Throwable)var42);
               }
            } else {
               session.setAttribute("FWEmpTermDate", termedYear);
            }
         } catch (Exception var43) {
            LogManager.log("Exception in converting the TermDate year to YYYY in editHRMInfo() :", (Throwable)var43);
         }

         sql = dataLogic.buildSQLString(sessionId, "SelectYearsOfExperiance");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var41) {
            dataLogic.rollbackTransaction(conn);
            throw var41;
         }

         Vector empExperiance = (Vector)results.get(0);
         years = empExperiance.get(1).toString();
         termedYear = empExperiance.get(2).toString();
         StringBuffer sb = new StringBuffer();
         sb.append(years).append(".").append(termedYear);
         LogManager.log("Display experiance:" + sb);
         session.setAttribute("FWActualExp", sb.toString());

         try {
            double actualExp = Double.parseDouble(session.getAttribute("FWActualExp").toString());
            double prExp = Double.parseDouble(session.getAttribute("FWPExp").toString());
            double totExp = actualExp + prExp;
            session.setAttribute("FWTotExp", String.valueOf(decimal.format(totExp)));
         } catch (Exception var40) {
            LogManager.log("Exception in Experiance Field:", (Throwable)var40);
         }

         this.listBoxHRMInfoPage(sessionId);
         session.setAttribute("FWEvaluationFiles", this.buildEvaluationParameters(sessionId));
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var39) {
               LogManager.log("Unable to Close connection.", (Throwable)var39);
            }

            conn = null;
         }

      }

   }

   private void listBoxHRMInfoPage(String sessionId) throws EuropaException {
      Session session = SessionManager.findSession(sessionId);
      DataLogic dataLogic = this.module.getDataLogic();
      Connection conn = dataLogic.openConnection();
      SqlStatement sql = null;
      Vector results = null;

      try {
         sql = dataLogic.buildSQLString(sessionId, "SelectWorkLocAndDept");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var45) {
            dataLogic.rollbackTransaction(conn);
            throw var45;
         }

         int workLocId = 0;
         int deptid = 0;
         if (!results.isEmpty()) {
            Vector row = (Vector)results.get(0);
            workLocId = Integer.parseInt(row.get(0).toString());
            deptid = Integer.parseInt(row.get(1).toString());
            String deptName = row.get(2).toString();
            session.setAttribute("FWHrmDepartmentName", deptName);
         }

         LogManager.log("Work Loc Id Dept Id:" + workLocId + " " + deptid);
         sql = dataLogic.buildSQLString(sessionId, "SelectHRMWorkLocations");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var44) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Error in" + var44);
            throw var44;
         }

         LogManager.log("Error");
         LogManager.log("" + SecurityManager.getUser(session.getUser().toString()).toString().equals("admin"));
         LogManager.log("" + this.hrmEditUsers.contains(session.getUser().toString()));
         LogManager.log("" + this.hrmDataEntryUsers.contains(session.getUser().toString()));
         LogManager.log("" + session.getAttribute("FWEmail"));
         String sqlGrand;
         String data;
         String avl;
         String resultsAvl;
         if (!SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") && !this.hrmEditUsers.contains(session.getUser().toString()) && !this.hrmDataEntryUsers.contains(session.getUser().toString()) && !SecurityManager.getUser(session.getAttribute("FWEmail").toString()).toString().equals(session.getUser().toString())) {
            LogManager.log("Error else");

            for(int i = 0; i < results.size(); ++i) {
               Vector row = (Vector)results.get(i);
               sqlGrand = row.get(0).toString();
               data = row.get(1).toString();
               avl = row.get(2).toString();
               resultsAvl = Integer.toString(workLocId);
               if (sqlGrand.equals(resultsAvl)) {
                  session.setAttribute("FWHRMWorkLocationList", data);
                  session.setAttribute("FWSelectState", avl);
                  break;
               }

               session.setAttribute("FWHRMWorkLocationList", "");
               session.setAttribute("FWSelectState", "");
            }
         } else {
            LogManager.log("Error if");
            session.setAttribute("FWHRMWorkLocationList", this.buildList(sessionId, results, workLocId, "FWHRMWorkLocationID"));
         }

         LogManager.log("Session Values in FWHRMWorkLocation:" + session.getAttribute("FWHRMWorkLocationList"));
         sql = dataLogic.buildSQLString(sessionId, "SelectHRMDepartments");
         dataLogic.startTransaction(conn);

         try {
            dataLogic.execute(conn, sql);
         } catch (EuropaException var43) {
            dataLogic.rollbackTransaction(conn);
            throw var43;
         }

         StringBuffer availDegrees = new StringBuffer();
         StringBuffer grantedDegrees = new StringBuffer();
         data = null;
         SqlStatement sqlGrand1 = dataLogic.buildSQLString(sessionId, "SelectHRMAssociateDegrees");
         dataLogic.startTransaction(conn);

         Vector resultsGrand;
         try {
            resultsGrand = dataLogic.execute(conn, sqlGrand1);
         } catch (EuropaException var42) {
            dataLogic.rollbackTransaction(conn);
            throw var42;
         }

         dataLogic.commitTransaction(conn);
         this.degree = new Vector();

         Vector resultsAvlV;
         for(int j = 0; j < resultsGrand.size(); ++j) {
            resultsAvlV = (Vector)resultsGrand.get(j);
            Integer degreeID = (Integer)resultsAvlV.get(0);
            this.degree.add(String.valueOf(degreeID));
            String degreeName = resultsAvlV.get(1).toString();
            LogManager.log(" Granted HRMDegrees " + degreeID + " " + degreeName);
            if (!SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") && !this.hrmEditUsers.contains(session.getUser().toString()) && !this.hrmDataEntryUsers.contains(session.getUser().toString()) && !SecurityManager.getUser(session.getAttribute("FWEmail").toString()).toString().equals(session.getUser().toString())) {
               grantedDegrees.append("<tr><td colspan=\"4\"><p align=\"left\"><font face=\"Tahoma\"><span style=\"font-size: 9pt\">").append(degreeName).append("</td></tr>");
            } else {
               grantedDegrees.append("<option value=\"").append(degreeID).append("\">").append(degreeName).append("\n");
            }
         }

         resultsAvlV = null;
         SqlStatement sqlAvl = dataLogic.buildSQLString(sessionId, "SelectHRMDegrees");
         dataLogic.startTransaction(conn);

         try {
            resultsAvlV = dataLogic.execute(conn, sqlAvl);
         } catch (EuropaException var41) {
            dataLogic.rollbackTransaction(conn);
            throw var41;
         }

         dataLogic.commitTransaction(conn);

         String degreeName;
         for(int j = 0; j < resultsAvlV.size(); ++j) {
            Vector row = (Vector)resultsAvlV.get(j);
            Integer degreeID = (Integer)row.get(0);
            degreeName = row.get(1).toString();
            LogManager.log(" Avilable HRMDegrees " + degreeID + " " + degreeName + "Vector " + this.degree);
            if (this.degree.contains(String.valueOf(degreeID))) {
               LogManager.log("Comming to If condition");
            } else {
               availDegrees.append("<option value=\"").append(degreeID).append("\">").append(degreeName).append("\n");
            }
         }

         session.setAttribute("FWavailableDegreesList", availDegrees.toString());
         session.setAttribute("FWAssociateDegreeList", grantedDegrees.toString());
         Vector certObtained = new Vector();
         StringBuffer availCertificate = new StringBuffer();
         StringBuffer grantedCertificate = new StringBuffer();
         degreeName = null;
         Vector resultsGranted = null;
         SqlStatement sqlGranted = dataLogic.buildSQLString(sessionId, "SelectHRMAssociateCertifications");
         dataLogic.startTransaction(conn);

         try {
            resultsGranted = dataLogic.execute(conn, sqlGranted);
         } catch (EuropaException var40) {
            dataLogic.rollbackTransaction(conn);
            throw var40;
         }

         dataLogic.commitTransaction(conn);
         LogManager.log("Granted :" + resultsGranted.size());

         Vector resultsCer;
         for(int j = 0; j < resultsGranted.size(); ++j) {
            resultsCer = (Vector)resultsGranted.get(j);
            Integer certificateID = (Integer)resultsCer.get(0);
            certObtained.add(String.valueOf(certificateID));
            String certificateName = resultsCer.get(1).toString();
            LogManager.log(" Granted HRMDegrees 2 " + certificateID + " " + certificateName);
            if (!SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") && !this.hrmEditUsers.contains(session.getUser().toString()) && !this.hrmDataEntryUsers.contains(session.getUser().toString()) && !SecurityManager.getUser(session.getAttribute("FWEmail").toString()).toString().equals(session.getUser().toString())) {
               grantedCertificate.append("<tr><td colspan=\"4\"><p align=\"left\"><font face=\"Tahoma\"><span style=\"font-size: 9pt\"> ").append(certificateName).append("</td></tr>");
            } else {
               grantedCertificate.append("<option value=\"").append(certificateID).append("\">").append(certificateName).append("\n");
            }
         }

         SqlStatement sqlCer = null;
         resultsCer = null;
         sqlCer = dataLogic.buildSQLString(sessionId, "SelectHRMCertifications");
         dataLogic.startTransaction(conn);

         try {
            resultsCer = dataLogic.execute(conn, sqlCer);
         } catch (EuropaException var39) {
            dataLogic.rollbackTransaction(conn);
            throw var39;
         }

         dataLogic.commitTransaction(conn);

         for(int j = 0; j < resultsCer.size(); ++j) {
            Vector row = (Vector)resultsCer.get(j);
            Integer certificateID = (Integer)row.get(0);
            String certificateName = row.get(1).toString();
            LogManager.log(" Avilable HRMDegrees  2" + certificateID + " " + certificateName);
            if (certObtained.contains(String.valueOf(certificateID))) {
               LogManager.log("Comming to If condition");
            } else {
               availCertificate.append("<option value=\"").append(certificateID).append("\">").append(certificateName).append("\n");
            }
         }

         session.setAttribute("FWavailableCertificateList", availCertificate.toString());
         session.setAttribute("FWAssociateCertificateList", grantedCertificate.toString());
         session.setAttribute("FWassociatedIndustries", this.generateIndustryExpList(sessionId, "SelectAssociatedIndustries", "0"));
         session.setAttribute("FWavailableIndustries", this.generateIndustryExpList(sessionId, "SelectIndustries", "1"));
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var38) {
               LogManager.log("Unable to Close connection.", (Throwable)var38);
            }

            conn = null;
         }

      }
   }

   public String generateIndustryExpList(String sessionId, String query, String flag) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.generateIndustryExpList()");
      DataLogic dataLogic = this.module.getDataLogic();
      Connection conn = dataLogic.openConnection();
      SqlStatement sql = null;
      Vector results = null;
      StringBuffer buffer = new StringBuffer();
      sql = dataLogic.buildSQLString(sessionId, query);
      LogManager.log("Query is :" + sql);
      dataLogic.startTransaction(conn);

      try {
         results = dataLogic.execute(conn, sql);
         LogManager.log("Results is:" + results);
         dataLogic.commitTransaction(conn);
      } catch (EuropaException var20) {
         dataLogic.rollbackTransaction(conn);
         throw var20;
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var19) {
               LogManager.log("Unable to Close connection.", (Throwable)var19);
            }

            conn = null;
         }

      }

      if (!flag.equals("1")) {
         this.insustryList = new ArrayList();
      } else {
         LogManager.log("list of industry:" + this.insustryList);
      }

      String dupCategory = "";

      for(int j = 0; j < results.size(); ++j) {
         Vector row = (Vector)results.get(j);
         Integer industryId = (Integer)row.get(0);
         String industryName = row.get(1).toString();
         String category = row.get(2).toString();
         LogManager.log("category" + category + "Duplicate Category" + dupCategory);
         if (!flag.equals("1")) {
            if (!category.equals(dupCategory)) {
               buffer.append("<optGroup label =" + category + ">");
               LogManager.log("comming one");
            }

            buffer.append("<option value=\"").append(industryId).append("\">").append(industryName).append("\n");
            this.insustryList.add(String.valueOf(industryId));
         } else if (this.insustryList.contains(String.valueOf(industryId))) {
            LogManager.log("Comming to If condition");
            if (!category.equals(dupCategory)) {
               buffer.append("<optGroup label =" + category + ">");
               LogManager.log("comming two");
            }
         } else {
            if (!category.equals(dupCategory)) {
               buffer.append("<optGroup label =" + category + ">");
               LogManager.log("comming three");
            }

            LogManager.log("Comming to else condition" + industryId);
            buffer.append("<option value=\"").append(industryId).append("\">").append(industryName).append("\n");
         }

         dupCategory = category;
      }

      return buffer.toString();
   }

   private void buildEmpTablePage(String sessionId) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.buildEmpTablePage()");
      Session session = SessionManager.findSession(sessionId);
      DataLogic dataLogic = this.module.getDataLogic();
      SqlStatement sql = null;
      Vector results = null;
      if (!SecurityManager.isMember("admin", session.getUser()) && !this.hrmEditUsers.contains(session.getUser().toString()) && !this.hrmDataEntryUsers.contains(session.getUser().toString())) {
         this.showNewEmployeeButton(sessionId, false);
         this.showHrmButton(sessionId, false);
      } else if (!SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") && !this.hrmEditUsers.contains(session.getUser().toString())) {
         this.showNewEmployeeButton(sessionId, false);
         this.showHrmButton(sessionId, false);
      } else if (SecurityManager.getUser(session.getUser().toString()).toString().equals("admin")) {
         this.showNewEmployeeButton(sessionId, true);
         this.showHrmButton(sessionId, true);
      } else {
         this.showNewEmployeeButton(sessionId, false);
         this.showHrmButton(sessionId, true);
      }

      if (SecurityManager.isMember("admin", session.getUser()) && !this.notAllowed.contains(session.getUser().toString())) {
         this.showNewEmployeeButton(sessionId, true);
      }

      LogManager.log("buildEmpTablePage");
      LogManager.log("Comming to the session sessionId" + session.getAttribute("sessionIds"));

      try {
         LogManager.log("Log manager Session." + session.getAttribute("FWSearchIds"));
         if (session.getAttribute("FWSearchIds") != null) {
            session.setAttribute("searchIds", session.getAttribute("FWSearchIds").toString());
         }

         if (session.getAttribute("searchIds") != null) {
            session.setAttribute("FWExtension", " Where EmployeeID in (" + session.getAttribute("searchIds").toString() + ")");
         } else {
            session.setAttribute("FWExtension", "");
            if (session.getAttribute("searchIds") != null) {
               session.setAttribute("FWExtension", " Where EmployeeID in (" + session.getAttribute("searchIds").toString() + ")");
            }
         }
      } catch (Exception var41) {
         session.setAttribute("FWExtension", "");
         LogManager.log("comming to catch: ", (Throwable)var41);
      }

      sql = dataLogic.buildSQLString(sessionId, "SelectAllEmployees");
      Connection conn = dataLogic.openConnection();
      dataLogic.startTransaction(conn);

      try {
         results = dataLogic.execute(conn, sql);
         dataLogic.commitTransaction(conn);
      } catch (EuropaException var40) {
         dataLogic.rollbackTransaction(conn);
         throw var40;
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var39) {
               LogManager.log("Unable to Close connection.", (Throwable)var39);
            }

            conn = null;
         }

      }

      SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy");
      StringBuffer activeEmployees = new StringBuffer();
      StringBuffer termedEmployees = new StringBuffer();

      for(int i = 0; i < results.size(); ++i) {
         Vector row = (Vector)results.get(i);
         String employeeId = row.get(0).toString();
         LogManager.log("employeeID.... " + employeeId);
         Date startDate = (Date)row.get(1);
         Date termDate = (Date)row.get(2);
         LogManager.log("tremDate.... " + termDate);
         String firstName = (String)row.get(3);
         String middleInitial = (String)row.get(4);
         String lastName = (String)row.get(5);
         String email = (String)row.get(6);
         String wAreaCode = (String)row.get(7);
         String wPrefix = (String)row.get(8);
         String wPostfix = (String)row.get(9);
         String workPhone = null;
         String extention = (String)row.get(10);
         LogManager.log("extention.... " + extention);
         String hAreaCode = (String)row.get(11);
         String hPrefix = (String)row.get(12);
         String hPostfix = (String)row.get(13);
         String homePhone = null;
         String cAreaCode = (String)row.get(14);
         String cPrefix = (String)row.get(15);
         String cPostfix = (String)row.get(16);
         String cellPhone = null;
         String fullName = null;
         if (wAreaCode != null) {
            if (wPrefix != null && wPostfix != null) {
               workPhone = wAreaCode + "-" + wPrefix + "-" + wPostfix;
            } else {
               workPhone = "";
            }
         } else if (wPrefix != null && wPostfix != null) {
            workPhone = wPrefix + "-" + wPostfix;
         } else {
            workPhone = "";
         }

         if (hAreaCode != null) {
            if (hPrefix != null && hPostfix != null) {
               homePhone = hAreaCode + "-" + hPrefix + "-" + hPostfix;
            } else {
               homePhone = "";
            }
         } else if (hPrefix != null && hPostfix != null) {
            homePhone = hPrefix + "-" + hPostfix;
         } else {
            homePhone = "";
         }

         if (cAreaCode != null) {
            if (cPrefix != null && cPostfix != null) {
               cellPhone = cAreaCode + "-" + cPrefix + "-" + cPostfix;
            } else {
               cellPhone = "";
            }
         } else if (cPrefix != null && cPostfix != null) {
            cellPhone = cPrefix + "-" + cPostfix;
         } else {
            cellPhone = "";
         }

         fullName = lastName + ", " + firstName + " " + middleInitial + ".";
         Date dt = new Date();
         LogManager.log("terDate Get Time:" + termDate.getTime() + " " + termDate.compareTo(dt) + " " + dt);
         if (termDate.getTime() > 0L && termDate.compareTo(dt) <= 0) {
            termedEmployees.append("<tr>").append("<td align=\"center\" valign=\"middle\">");
            if (SecurityManager.getUser(session.getUser().toString()).toString().equals("admin")) {
               termedEmployees.append("<a href=\"/jsp/TerminatedEmployee.jsp?sessionVal=" + sessionId + "&EmployeeName=" + lastName + "," + firstName + "&employeeIDVal=" + employeeId + "&FWModule=reportviewer&").append("\">");
            }

            termedEmployees.append(formatter.format(termDate));
            if (SecurityManager.getUser(session.getUser().toString()).toString().equals("admin")) {
               termedEmployees.append("</a>");
            }

            termedEmployees.append("</td>").append("<td align=\"left\" valign=\"middle\">").append("<a href=\"/servlets/ProxyServlet?FWModule=admin_employee&FWCurrentPage=2&").append("FWEmployeeID=").append(employeeId).append("\">").append(fullName).append("</a>").append("</td>").append("<td align=\"center\" valign=\"middle\">").append(workPhone).append("</td>").append("<td align=\"center\" valign=\"middle\">").append(extention).append("</td>").append("<td align=\"center\" valign=\"middle\">").append(homePhone).append("</td>").append("<td align=\"center\" valign=\"middle\">").append(cellPhone).append("</td>").append("<td align=\"left\" valign=\"middle\">").append("<a href=\"mailto:").append(email).append("\">").append(email).append("</a>").append("</td>").append("</tr>");
         } else if (SecurityManager.isMember("People Manager", session.getUser()) && this.isPeopleManagerUsers(sessionId, employeeId) || SecurityManager.isMember("TopLineManager", session.getUser()) && this.isTopLineManagerUsers(sessionId, employeeId)) {
            activeEmployees.append("<tr bgcolor=\"#D3D3D3\">").append("<td align=\"center\" valign=\"middle\">");
            if (SecurityManager.getUser(session.getUser().toString()).toString().equals("admin")) {
               activeEmployees.
                       append("<a class=\"link-primary link-offset-2 text-decoration-none pe-auto\" href=\"/jsp/TerminatedEmployee.jsp?sessionVal=" + sessionId + "&EmployeeName=" + lastName + "," + firstName + "&employeeIDVal=" + employeeId + "&FWModule=reportviewer&").append("\">");
            }

            activeEmployees.append(formatter.format(startDate));
            if (SecurityManager.getUser(session.getUser().toString()).toString().equals("admin")) {
               activeEmployees.append("</a>");
            }

            activeEmployees.
                    append("</td>")
                    .append("<td align=\"left\" valign=\"middle\">")
                    .append("<a href=\"/servlets/ProxyServlet?FWModule=admin_employee&FWCurrentPage=2&")
                    .append("FWEmployeeID=")
                    .append(employeeId).append("\">").append(fullName).append("</a>")
                    .append("</td>")
//                    .append("<td align=\"center\" valign=\"middle\">").append(workPhone).append("</td>")
//                    .append("<td align=\"center\" valign=\"middle\">").append(extention).append("</td>")
//                    .append("<td align=\"center\" valign=\"middle\">").append(homePhone).append("</td>")
//                    .append("<td align=\"center\" valign=\"middle\">").append(cellPhone).append("</td>")
                    .append("<td align=\"left\" valign=\"middle\">").append("<a href=\"mailto:").append(email).append("\">").append(email).append("</a>").append("</td>").append("</tr>");
         } else {
            activeEmployees.append("<tr>").append("<td align=\"center\" valign=\"middle\">");
            if (SecurityManager.getUser(session.getUser().toString()).toString().equals("admin")) {
               activeEmployees.append("<a class=\"link-primary link-offset-2 text-decoration-none pe-auto\" href=\"/jsp/TerminatedEmployee.jsp?sessionVal=" + sessionId + "&EmployeeName=" + lastName + "," + firstName + "&employeeIDVal=" + employeeId + "&FWModule=reportviewer&").append("\">");
            }

            activeEmployees.append(formatter.format(startDate));
            if (SecurityManager.getUser(session.getUser().toString()).toString().equals("admin")) {
               activeEmployees.append("</a>");
            }

            activeEmployees.append("</td>").
                    append("<td align=\"left\" valign=\"middle\">")
                    .append("<a class=\"link-primary link-offset-2 text-decoration-none pe-auto\" href=\"/servlets/ProxyServlet?FWModule=admin_employee&FWCurrentPage=2&").append("FWEmployeeID=").append(employeeId).append("\">").append(fullName).append("</a>").append("</td>")
//                    .append("<td align=\"center\" valign=\"middle\">").append(workPhone).append("</td>").
//                    append("<td align=\"center\" valign=\"middle\">").append(extention).append("</td>").
//                    append("<td align=\"center\" valign=\"middle\">").append(homePhone).append("</td>").
//                    append("<td align=\"center\" valign=\"middle\">").append(cellPhone).append("</td>").
                    .append("<td align=\"left\" valign=\"middle\">").append("<a class=\"link-primary link-offset-2 text-decoration-none pe-auto\" href=\"mailto:").append(email).append("\">").append(email).append("</a>").append("</td>").
                    append("</tr>");
         }
      }

      session.setAttribute("FWActiveEmployeeTable", activeEmployees.toString());
      if (SecurityManager.isMember("admin", session.getUser())) {
         session.setAttribute("FWTermedEmployeeTable", termedEmployees.toString());
         session.setAttribute("FWShowInActiveEmployeeStart", "");
         session.setAttribute("FWShowInActiveEmployeeEnd", "");
      } else {
         session.setAttribute("FWShowInActiveEmployeeStart", "<!--");
         session.setAttribute("FWShowInActiveEmployeeEnd", " -->");
         session.setAttribute("FWTermedEmployeeTable", "");
      }

   }

   private boolean buildNewEmpPage(String sessionId) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.buildNewEmpPage()");
      Session session = SessionManager.findSession(sessionId);
      boolean returnValue = false;
      session.setAttribute("FWCommentStart", "<!--");
      session.setAttribute("FWCommentEnd", "-->");
      session.setAttribute("FWShowFeaturePermStart", "<!--");
      session.setAttribute("FWShowFeaturePermEnd", "-->");
      session.setAttribute("FWShowReportPermStart", "<!--");
      session.setAttribute("FWShowReportPermEnd", "-->");
      session.setAttribute("FWShowAdminGroupStart", "<!--");
      session.setAttribute("FWShowAdminGroupEnd", "-->");
      session.setAttribute("FWShowEffectiveFromStart", "<!--");
      session.setAttribute("FWShowEffectiveFromEnd", "-->");
      session.setAttribute("FWEngineeringManagerID", "0");
      session.setAttribute("FWLeadEngineerID", "0");
      session.setAttribute("FWChargeabilityPercent", "95");
      session.setAttribute("FWVacationHours", "0.00");
      session.setAttribute("FWPBAHours", "0.00");
      session.setAttribute("FWCompTimeHours", "0.00");
      session.setAttribute("FWHolidayFloatingTimeHours", "0.00");
      session.setAttribute("FWEmployeeCost", "0.00");
      if (SecurityManager.isMember("admin", session.getUser())) {
         this.showCodeAccessButton(sessionId, false);
         this.showEmpSecureInfo(sessionId, false);
         this.showHRMInfo(sessionId, false);
         session.setAttribute("EditMode", "Insert");
         DataLogic dataLogic = this.module.getDataLogic();
         Connection conn = dataLogic.openConnection();
         SqlStatement sql = null;
         Vector results = null;

         try {
            session.setAttribute("FWDepartmentID", "0");
            sql = dataLogic.buildSQLString(sessionId, "SelectAllDepartments");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var48) {
               dataLogic.rollbackTransaction(conn);
               throw var48;
            }

            dataLogic.commitTransaction(conn);
            session.removeAttribute("FWDepartmentID");
            LogManager.log("FWDepartmentsList 2");
            session.setAttribute("FWDepartmentsList", this.buildList(sessionId, results, 0, "FWDepartment"));
            session.setAttribute("FWJobTitleID", "0");
            sql = dataLogic.buildSQLString(sessionId, "SelectAllJobTitles");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var47) {
               dataLogic.rollbackTransaction(conn);
               throw var47;
            }

            dataLogic.commitTransaction(conn);
            session.removeAttribute("FWJobTitleID");
            session.setAttribute("FWJobTitleList", this.buildList(sessionId, results, 0, "FWJobTitle"));
            session.setAttribute("FWJobLevelID", "0");
            sql = dataLogic.buildSQLString(sessionId, "SelectAllJobLevels");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var46) {
               dataLogic.rollbackTransaction(conn);
               throw var46;
            }

            dataLogic.commitTransaction(conn);
            session.removeAttribute("FWJobLevelID");
            session.setAttribute("FWJobLevelList", this.buildList(sessionId, results, 0, "FWJobLevel"));
            session.setAttribute("FWAccountID", "0");
            sql = dataLogic.buildSQLString(sessionId, "SelectAllAccounts");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var45) {
               dataLogic.rollbackTransaction(conn);
               throw var45;
            }

            dataLogic.commitTransaction(conn);
            session.removeAttribute("FWAccountID");
            session.setAttribute("FWAccountList", this.buildList(sessionId, results, 0, "FWAccount"));
            session.setAttribute("FWCompCenterId", "0");
            sql = dataLogic.buildSQLString(sessionId, "SelectAllCompetencyCenters");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var44) {
               dataLogic.rollbackTransaction(conn);
               throw var44;
            }

            dataLogic.commitTransaction(conn);
            session.removeAttribute("FWCompCenterId");
            session.setAttribute("FWCompCenterList", this.buildList(sessionId, results, 1, "FWCompCenter"));
            this.buildSubCompetencyList(sessionId, dataLogic, conn, "0");
            session.setAttribute("FWTermClassification", "0");
            sql = dataLogic.buildSQLString(sessionId, "SelectAllTermClassifications");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var43) {
               dataLogic.rollbackTransaction(conn);
               throw var43;
            }

            dataLogic.commitTransaction(conn);
            session.setAttribute("FWTermClassificationList", this.buildList(sessionId, results, 0, "FWTermClassification"));
            sql = dataLogic.buildSQLString(sessionId, "SelectAllTeams");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var42) {
               dataLogic.rollbackTransaction(conn);
               throw var42;
            }

            dataLogic.commitTransaction(conn);
            session.setAttribute("FWTeamsList", this.buildList(sessionId, results, 0, "FWTeam"));
            sql = dataLogic.buildSQLString(sessionId, "SelectAllPayRollDepts");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var41) {
               dataLogic.rollbackTransaction(conn);
               throw var41;
            }

            dataLogic.commitTransaction(conn);
            session.setAttribute("FWPayRollDeptList", this.buildList(sessionId, results, 0, "FWPayRollDept"));
            sql = dataLogic.buildSQLString(sessionId, "SelectAllSalaryPeriods");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var40) {
               dataLogic.rollbackTransaction(conn);
               throw var40;
            }

            dataLogic.commitTransaction(conn);
            session.setAttribute("FWSalaryPeriodsList", this.buildList(sessionId, results, 0, "FWSalaryPeriod"));
            sql = dataLogic.buildSQLString(sessionId, "SelectAllEmployeeTime");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var39) {
               dataLogic.rollbackTransaction(conn);
               throw var39;
            }

            dataLogic.commitTransaction(conn);
            session.setAttribute("FWEmployeeTimeList", this.buildList(sessionId, results, 0, "FWEmpWorkType"));
            session.setAttribute("FWEmployeeID", "0");
            session.setAttribute("FWPeopleManagerID", "0");
            sql = dataLogic.buildSQLString(sessionId, "SelectPeopleManager");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var38) {
               dataLogic.rollbackTransaction(conn);
               throw var38;
            }

            dataLogic.commitTransaction(conn);
            Vector peopleManager = new Vector();
            int k = 0;
            int length = results.size();
            LogManager.log(" Size  :" + results.size());
            Vector topLineManager;
            if (results.size() > 0) {
               do {
                  topLineManager = (Vector)results.get(k);
                  String emailPM = topLineManager.get(2).toString();

                  try {
                     if (SecurityManager.isMember("People Manager", SecurityManager.getUser(emailPM))) {
                        peopleManager.add(results.get(k));
                        LogManager.log("Register User True " + emailPM);
                     }
                  } catch (Exception var37) {
                     LogManager.log("Register User false " + emailPM);
                  }

                  ++k;
               } while(k < length);
            }

            session.setAttribute("FWPeopleManagerListValue", this.buildList(sessionId, peopleManager, 0, "FWPeopleManagerList"));
            session.setAttribute("FWEngineeringManagerList", this.buildEngMgrList(sessionId, dataLogic, conn));
            session.setAttribute("FWTopLineManagerId", "0");
            sql = dataLogic.buildSQLString(sessionId, "SelectTopLineManagers");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var36) {
               dataLogic.rollbackTransaction(conn);
               throw var36;
            }

            dataLogic.commitTransaction(conn);
            topLineManager = new Vector();
            k = 0;
            length = results.size();
            LogManager.log(" Size  :" + results.size());
            if (results.size() > 0) {
               do {
                  Vector temPMEmail = (Vector)results.get(k);
                  String emailPM = temPMEmail.get(2).toString();

                  try {
                     if (SecurityManager.isMember("TopLineManager", SecurityManager.getUser(emailPM))) {
                        topLineManager.add(results.get(k));
                        LogManager.log("Register User True " + emailPM);
                     }
                  } catch (Exception var35) {
                     LogManager.log("Register User false " + emailPM);
                  }

                  ++k;
               } while(k < length);
            }

            session.setAttribute("FWTopLineManagerListValue", this.buildList(sessionId, topLineManager, 0, "FWTopLineManagerIdList"));
            session.setAttribute("FWLeadEngineerList", this.buildLeadEngList(sessionId, dataLogic, conn));
            session.removeAttribute("FWEmployeeID");
            session.setAttribute("FWisPL", " ");
            session.setAttribute("FWisTL", " ");
            session.setAttribute("FWisTM", " ");
            session.setAttribute("FWisSE", " ");
            session.setAttribute("FWisSSE", " ");
            session.setAttribute("FWisAnv", " ");
            session.setAttribute("FWisElv", " ");
            session.setAttribute("FWisMfv", " ");
            session.setAttribute("FWisJpr", " ");
            session.setAttribute("FWSsn", "0");
            session.setAttribute("FWSalaryAmount", "0.00");
            session.setAttribute("FWBurdenFactor", "0.00");
            session.setAttribute("FWSalaryPeriodID", "0");
            session.setAttribute("FWEmpWorkType", "-1");
            session.setAttribute("FWEmpIdWage", "");
            session.setAttribute("FWComments", "");
            session.setAttribute("FWConcost", "");
            session.setAttribute("FWContractorAmount", "0");
            session.setAttribute("FWContractorOTCost", "0");
            session.setAttribute("FWisChk", " ");
            session.setAttribute("FWCollege", " ");
            session.setAttribute("FWRelocate", "0");
            session.setAttribute("FWYesRelocate", " ");
            session.setAttribute("FWNoRelocate", "checked");
            session.setAttribute("FWEvaluations", " ");
            session.setAttribute("FWSpotBonuses", "0");
            session.setAttribute("FWCurPage", session.getAttribute("FWCurrentPage"));
            returnValue = true;
         } finally {
            if (conn != null) {
               try {
                  conn.close();
               } catch (SQLException var34) {
                  LogManager.log("Unable to Close connection.", (Throwable)var34);
               }

               conn = null;
            }

         }
      }

      this.DataFeedFunction("Insert", sessionId);
      this.CollectAllEmployeeWageId(sessionId, (DataLogic)null, (Connection)null, "Insert");
      return returnValue;
   }

   private boolean buildEditEmpPage(String sessionId) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.buildEditEmpPage()");
      Session session = SessionManager.findSession(sessionId);
      session.setAttribute("FWCommentStart", "<!--");
      session.setAttribute("FWCommentEnd", "-->");
      boolean returnValue = false;
      String selectedUserId = (String)session.getAttribute("FWEmployeeID");
      Vector eViewUser = new Vector();
      Vector eSecureInfoAccess = new Vector();
      Connection conn = null;

      try {
         String employeeViewUsers = System.getProperty("europa.employeeviewpermittedusers");
         LogManager.log("EmployeeViewUsers:" + employeeViewUsers);
         StringTokenizer st1 = new StringTokenizer(employeeViewUsers, ",");

         String empViewUsersActivitycodes;
         while(st1.hasMoreTokens()) {
            empViewUsersActivitycodes = st1.nextToken().toString();
            eViewUser.add(empViewUsersActivitycodes);
         }

         empViewUsersActivitycodes = System.getProperty("europa.employeeactivitycodes.editaccess");
         LogManager.log("EmployeeViewUsers:" + empViewUsersActivitycodes);
         StringTokenizer st2 = new StringTokenizer(empViewUsersActivitycodes, ",");
         Vector empEditUsers = new Vector();

         String empSecureInfoUsers;
         while(st2.hasMoreTokens()) {
            empSecureInfoUsers = st2.nextToken().toString();
            empEditUsers.add(empSecureInfoUsers);
         }

         LogManager.log("eViewUser:" + eViewUser);
         empSecureInfoUsers = System.getProperty("europa.employeesecureinfousers.viewaccess");
         StringTokenizer st3 = new StringTokenizer(empSecureInfoUsers, ",");

         while(st3.hasMoreTokens()) {
            String username = st3.nextToken().toString();
            eSecureInfoAccess.add(username);
         }

         boolean isHireMgrForTheUser = this.isPeopleManagerUsers(sessionId, selectedUserId);
         boolean isTopLineManagerUser = this.isTopLineManagerUsers(sessionId, selectedUserId);
         if (!SecurityManager.isMember("admin", session.getUser()) && !session.getUser().getEmployeeId().toString().equals(selectedUserId) && !eViewUser.contains(session.getUser().toString()) && !eSecureInfoAccess.contains(session.getUser().toString()) && !empEditUsers.contains(session.getUser().toString()) && !this.hrmEditUsers.contains(session.getUser().toString()) && !this.hrmDataEntryUsers.contains(session.getUser().toString()) && !isHireMgrForTheUser && !isTopLineManagerUser) {
            returnValue = false;
         } else {
            if (!SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") && !this.hrmEditUsers.contains(session.getUser().toString()) && !isHireMgrForTheUser && !isTopLineManagerUser) {
               if (eViewUser.contains(session.getUser().toString())) {
                  this.showEmployeeData(sessionId, true);
                  session.setAttribute("FWisCompanyFunctionDisplayed", "true");
                  this.showCodeAccessButton(sessionId, true);
                  if (!session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString()) && !this.hrmDataEntryUsers.contains(session.getUser().toString())) {
                     this.showHRMInfo(sessionId, false);
                  } else {
                     this.showHRMInfo(sessionId, true);
                  }

                  if (!eSecureInfoAccess.contains(session.getUser().toString()) && !isHireMgrForTheUser && !isTopLineManagerUser) {
                     this.showEmpSecureInfo(sessionId, false);
                  } else {
                     this.showEmpSecureInfo(sessionId, true);
                     if (session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString())) {
                        session.setAttribute("FWEmployeeViewStart", "");
                        session.setAttribute("FWEmployeeViewEnd", "");
                     } else {
                        session.setAttribute("FWEmployeeViewStart", "<!--");
                        session.setAttribute("FWEmployeeViewEnd", "-->");
                     }

                     session.setAttribute("FWSecureInfoEditStart", "<!--");
                     session.setAttribute("FWSecureInfoEditEnd", "-->");
                  }

                  if (session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString())) {
                     session.setAttribute("FWEmployeeViewStart", "");
                     session.setAttribute("FWEmployeeViewEnd", "");
                  } else {
                     session.setAttribute("FWEmployeeViewStart", "<!--");
                     session.setAttribute("FWEmployeeViewEnd", "-->");
                  }

                  if (!session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString()) || SecurityManager.isMember("admin", session.getUser()) && !eViewUser.contains(session.getUser().toString())) {
                     this.showEmployeeData(sessionId, true);
                  } else {
                     this.showEmployeeData(sessionId, false);
                  }
               } else if (empEditUsers.contains(session.getUser().toString())) {
                  this.showEmployeeData(sessionId, true);
                  session.setAttribute("FWisCompanyFunctionDisplayed", "true");
                  this.showCodeAccessButton(sessionId, true);
                  this.showEmpSecureInfo(sessionId, false);
                  if (!session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString()) && !this.hrmDataEntryUsers.contains(session.getUser().toString())) {
                     this.showHRMInfo(sessionId, false);
                  } else {
                     this.showHRMInfo(sessionId, true);
                  }

                  if (!eSecureInfoAccess.contains(session.getUser().toString()) && !isHireMgrForTheUser && !isTopLineManagerUser) {
                     this.showEmpSecureInfo(sessionId, false);
                  } else {
                     this.showEmpSecureInfo(sessionId, true);
                     if (session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString())) {
                        session.setAttribute("FWEmployeeViewStart", "");
                        session.setAttribute("FWEmployeeViewEnd", "");
                     } else {
                        session.setAttribute("FWEmployeeViewStart", "<!--");
                        session.setAttribute("FWEmployeeViewEnd", "-->");
                     }

                     session.setAttribute("FWSecureInfoEditStart", "<!--");
                     session.setAttribute("FWSecureInfoEditEnd", "-->");
                  }

                  session.setAttribute("FWEmployeeViewStart", "<!--");
                  session.setAttribute("FWEmployeeViewEnd", "-->");
               } else {
                  session.setAttribute("FWisCompanyFunctionDisplayed", "false");
                  if (this.hrmDataEntryUsers.contains(session.getUser().toString())) {
                     this.showEmployeeData(sessionId, true);
                  } else {
                     this.showEmployeeData(sessionId, false);
                  }

                  this.showCodeAccessButton(sessionId, false);
                  if (!session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString()) && !this.hrmDataEntryUsers.contains(session.getUser().toString())) {
                     this.showHRMInfo(sessionId, false);
                  } else {
                     this.showHRMInfo(sessionId, true);
                  }

                  if (!eSecureInfoAccess.contains(session.getUser().toString()) && !isHireMgrForTheUser && !isTopLineManagerUser) {
                     this.showEmpSecureInfo(sessionId, false);
                  } else {
                     this.showEmpSecureInfo(sessionId, true);
                     if (session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString())) {
                        session.setAttribute("FWEmployeeViewStart", "");
                        session.setAttribute("FWEmployeeViewEnd", "");
                     } else {
                        session.setAttribute("FWEmployeeViewStart", "<!--");
                        session.setAttribute("FWEmployeeViewEnd", "-->");
                     }
                  }
               }
            } else if (SecurityManager.getUser(session.getUser().toString()).toString().equals("admin")) {
               this.showEmployeeData(sessionId, true);
               session.setAttribute("FWisCompanyFunctionDisplayed", "true");
               this.showCodeAccessButton(sessionId, true);
               this.showEmpSecureInfo(sessionId, true);
               this.showHRMInfo(sessionId, true);
               session.setAttribute("FWEmployeeViewStart", "");
               session.setAttribute("FWEmployeeViewEnd", "");
            } else {
               this.showEmployeeData(sessionId, true);
               session.setAttribute("FWisCompanyFunctionDisplayed", "true");
               if (empEditUsers.contains(session.getUser().toString())) {
                  this.showCodeAccessButton(sessionId, true);
               } else {
                  this.showCodeAccessButton(sessionId, false);
               }

               if (!eSecureInfoAccess.contains(session.getUser().toString()) && !isHireMgrForTheUser && !isTopLineManagerUser) {
                  this.showEmpSecureInfo(sessionId, false);
               } else {
                  this.showEmpSecureInfo(sessionId, true);
               }

               this.showHRMInfo(sessionId, true);
               if (session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString())) {
                  session.setAttribute("FWEmployeeViewStart", "");
                  session.setAttribute("FWEmployeeViewEnd", "");
               } else {
                  session.setAttribute("FWEmployeeViewStart", "<!--");
                  session.setAttribute("FWEmployeeViewEnd", "-->");
               }
            }

            if (SecurityManager.isMember("admin", session.getUser()) && !this.notAllowed.contains(session.getUser().toString())) {
               this.showCodeAccessButton(sessionId, true);
               this.showEmployeeData(sessionId, true);
               session.setAttribute("FWEmployeeViewStart", "");
               session.setAttribute("FWEmployeeViewEnd", "");
            }

            String feature_perms_access_users = System.getProperty("europa.employees.grant_feature_perms_access");
            StringTokenizer st0 = new StringTokenizer("");
            if (feature_perms_access_users != null) {
               st0 = new StringTokenizer(feature_perms_access_users, ",");
            }

            Vector featurePermsAccessUsers = new Vector();

            while(st0.hasMoreTokens()) {
               featurePermsAccessUsers.add(st0.nextElement().toString());
            }

            String sessionUserEmpId = session.getUser().getEmployeeId().toString();
            if ((featurePermsAccessUsers.contains(session.getUser().toString()) || session.getUser().toString().equals("admin")) && !selectedUserId.equals(sessionUserEmpId)) {
               session.setAttribute("FWShowFeaturePermStart", "");
               session.setAttribute("FWShowFeaturePermEnd", "");
            } else {
               session.setAttribute("FWShowFeaturePermStart", "<!--");
               session.setAttribute("FWShowFeaturePermEnd", "-->");
            }

            String grant_report_perms_access = System.getProperty("europa.employees.grant_report_perms_access");
            StringTokenizer st = new StringTokenizer("");
            if (grant_report_perms_access != null) {
               st = new StringTokenizer(grant_report_perms_access, ",");
            }

            Vector reportPermsAccessUsers = new Vector();

            while(st.hasMoreTokens()) {
               reportPermsAccessUsers.add(st.nextElement().toString());
            }

            if ((reportPermsAccessUsers.contains(session.getUser().toString()) || session.getUser().toString().equals("admin")) && !selectedUserId.equals(sessionUserEmpId)) {
               session.setAttribute("FWShowReportPermStart", "");
               session.setAttribute("FWShowReportPermEnd", "");
            } else {
               session.setAttribute("FWShowReportPermStart", "<!--");
               session.setAttribute("FWShowReportPermEnd", "-->");
            }

            session.setAttribute("EditMode", "Update");
            DataLogic dataLogic = this.module.getDataLogic();
            conn = dataLogic.openConnection();
            SqlStatement sql = null;
            Vector results = null;
            sql = dataLogic.buildSQLString(sessionId, "SelectEmployee");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var224) {
               dataLogic.rollbackTransaction(conn);
               throw var224;
            }

            dataLogic.commitTransaction(conn);
            LogManager.log("Line 537 ");
            SimpleDateFormat date = new SimpleDateFormat("MM/dd/yy");
            if (results.size() > 1) {
               throw new EuropaException("Duplicate EmployeeID");
            }

            Vector employee = (Vector)results.get(0);
            LogManager.log("Line 544 ");
            String employeeNum = employee.get(0).toString();
            String ssn = employee.get(1).toString();
            String firstName = employee.get(2).toString();
            String middleInitial = employee.get(3).toString();
            String lastName = employee.get(4).toString();
            String email = employee.get(5).toString();
            String departmentId = employee.get(6).toString();
            String title = employee.get(7).toString();
            String address_1 = employee.get(8).toString();
            LogManager.log("Line 554 ");
            String address_2 = employee.get(9).toString();
            String city = employee.get(10).toString();
            String state = employee.get(11).toString();
            String zip = employee.get(12).toString();
            String hAreaCode = (String)employee.get(13);
            String hPrefix = (String)employee.get(14);
            String hPostfix = (String)employee.get(15);
            String wAreaCode = (String)employee.get(16);
            String wPrefix = (String)employee.get(17);
            String wPostfix = (String)employee.get(18);
            LogManager.log("Line 564 ");
            String extention = employee.get(19).toString();
            String fAreaCode = (String)employee.get(20);
            String fPrefix = (String)employee.get(21);
            String fPostfix = (String)employee.get(22);
            String cAreaCode = (String)employee.get(23);
            String cPrefix = (String)employee.get(24);
            String cPostfix = (String)employee.get(25);
            String pAreaCode = (String)employee.get(26);
            String pPrefix = (String)employee.get(27);
            String pPostfix = (String)employee.get(28);
            String salaryPeriodId = employee.get(29).toString();
            String salaryAmount = employee.get(30).toString();
            DecimalFormat burdendecimal = new DecimalFormat("#,##0.00##");
            String burdenFactor = burdendecimal.format(Double.parseDouble(employee.get(31).toString()));
            session.setAttribute("FWOldSalaryPeriodId", salaryPeriodId);
            session.setAttribute("FWOldSalaryAmount", salaryAmount);
            session.setAttribute("FWOldBurdenFactor", burdenFactor);
            boolean checkemail = false;
            String periodName = "";
            if (salaryPeriodId.equals("1")) {
               periodName = "Annual";
            }

            if (salaryPeriodId.equals("2")) {
               periodName = "Hourly";
            }

            if (salaryPeriodId.equals("3")) {
               periodName = "1099";
            }

            session.setAttribute("FWPeriodName", periodName);
            LogManager.log("Line 577 ");
            Date hireDate = (Date)employee.get(32);
            LogManager.log("Line 581 ");
            Date termDate = (Date)employee.get(33);
            LogManager.log("Line 583 ");
            String teamId = employee.get(34).toString();
            LogManager.log("Line 585 ");
            String isProjectLeader = employee.get(35).toString();
            LogManager.log("Line 587 ");
            String isAccountManager = employee.get(36).toString();
            LogManager.log("Line 589 ");
            String isDirectRep = employee.get(37).toString();
            LogManager.log("Line 591 ");
            LogManager.log("Line 593 ");
            LogManager.log("Line 595 ");
            String isSalesEngineer = employee.get(38).toString();
            String isSrSalesEngineer = employee.get(39).toString();
            String isVertical = employee.get(40).toString();
            String isJrPartner = employee.get(41).toString();
            String isAnVertical = employee.get(42).toString();
            String isElVertical = employee.get(43).toString();
            String isMfVertical = employee.get(44).toString();
            String employeeTime = employee.get(46).toString();
            String isChecker = employee.get(47).toString();
            Date lastUpdated = (Date)employee.get(48);
            String isEngAdmin = employee.get(49).toString();
            String employeeId_wage = employee.get(50).toString();
            String isPeopleManager = employee.get(51).toString();
            String peoplemanagerId = employee.get(52).toString();
            String datafeed = employee.get(53).toString();
            String jobTitleId = employee.get(54).toString();
            String accountId = employee.get(55).toString();
            String availBillHrs = employee.get(56).toString();
            String isSpecialUser = employee.get(57).toString();
            Date effectiveFrom = (Date)employee.get(58);
            String isRehire = employee.get(60).toString();
            Date oldTermDate = (Date)employee.get(61);
            Date rehireDate = (Date)employee.get(62);
            String compCenterId = employee.get(63).toString();
            String engineeringManagerID = employee.get(64).toString();
            String chargeabilityTargetPercent = employee.get(65).toString();
            String subCompCenterId = employee.get(66).toString();
            String isEngManager = employee.get(67).toString();
            String isLeadEng = employee.get(68).toString();
            String leadEngId = employee.get(69).toString();
            String isPowerSystem = employee.get(70).toString();
            String comments = employee.get(71).toString();
            String termClassificationId = employee.get(72).toString();
            String payRollDeptId = employee.get(73).toString();
            session.setAttribute("FWOldPayRollDeptId", payRollDeptId);
            String assignConCost = employee.get(74).toString();
            String contractorAmt = employee.get(75).toString();
            String contractorOTCost = employee.get(76).toString();
            String vacationHr = employee.get(77).toString();
            String PBAHr = employee.get(78).toString();
            String CompTimeHr = employee.get(79).toString();
            String isconToDirect = employee.get(80).toString();
            Date conDirectHireDate = (Date)employee.get(81);
            String Dell_Employee_Id = employee.get(82).toString();
            Date serviceDate = (Date)employee.get(83);
            String isDesignAndAnalysisCompEast = employee.get(84).toString();
            String isDesignAndAnalysisCompWest = employee.get(85).toString();
            String isTopLineManager = employee.get(86).toString();
            String topLineManagerId = employee.get(87).toString();
            String isResourceManager = employee.get(88).toString();
            String employeeCost = employee.get(89).toString();
            Date employeeCostDate = (Date)employee.get(90);
            String isOffshoreManager = employee.get(91).toString();
            String isElectricalSystemsIntegration = employee.get(92).toString();
            String jobLevelId = employee.get(93).toString();
            String HolidayFloatingTimeHr = employee.get(94).toString();
            LogManager.log(" employeeCost " + employeeCost);
            LogManager.log(" employeeCostDate " + employeeCostDate);
            session.setAttribute("FWOld_EmployeeCost", employeeCost);
            session.setAttribute("FWOldEmployeeCostDate", employeeCostDate);
            LogManager.log("topLineManagerId =" + topLineManagerId);
            session.setAttribute("FWisDesignAndAnalysCompEast", isDesignAndAnalysisCompEast);
            session.setAttribute("FWisDesignAndAnalysCompWest", isDesignAndAnalysisCompWest);
            session.setAttribute("FWisTopLineManager", isTopLineManager);
            session.setAttribute("FWTopLineManagerId", topLineManagerId);
            session.setAttribute("FWOldMI", middleInitial);
            session.setAttribute("FWOldAddress1", address_1);
            session.setAttribute("FWOldAddress2", address_2);
            session.setAttribute("FWOldCity", city);
            session.setAttribute("FWOldState", state);
            session.setAttribute("FWOldZip", zip);
            session.setAttribute("FWOldHAreaCode", hAreaCode);
            session.setAttribute("FWOldHPrefix", hPrefix);
            session.setAttribute("FWOldHPostfix", hPostfix);
            session.setAttribute("FWOldWAreaCode", wAreaCode);
            session.setAttribute("FWOldWPrefix", wPrefix);
            session.setAttribute("FWOldWPostfix", wPostfix);
            session.setAttribute("FWOldextention", extention);
            session.setAttribute("FWOldFAreacode", fAreaCode);
            session.setAttribute("FWOldFPrefix", fPrefix);
            session.setAttribute("FWOldFPostfix", fPostfix);
            session.setAttribute("FWOldCAreaCode", cAreaCode);
            session.setAttribute("FWOldCPrefix", cPrefix);
            session.setAttribute("FWOldCPostfix", cPostfix);
            session.setAttribute("FWOldPAreaCode", pAreaCode);
            session.setAttribute("FWOldPPrefix", pPrefix);
            session.setAttribute("FWOldPPostfix", pPostfix);
            session.setAttribute("FWChargeabilityPercent", chargeabilityTargetPercent);
            session.setAttribute("FWEngineeringManagerID", engineeringManagerID);
            session.setAttribute("FWLeadEngineerID", leadEngId);
            session.setAttribute("FWisEngManager", isEngManager);
            session.setAttribute("FWisLeadEng", isLeadEng);
            session.setAttribute("FWisPowerSystem", isPowerSystem);
            session.setAttribute("FWTermClassification", termClassificationId);
            session.setAttribute("FWPayRollDept", payRollDeptId);
            session.setAttribute("FWContractorAmount", contractorAmt);
            session.setAttribute("FWContractorOTCost", contractorOTCost);
            session.setAttribute("FWisResourceManager", isResourceManager);
            session.setAttribute("FWisOffshoreManager", isOffshoreManager);
            session.setAttribute("FWisElectricalSystemsIntegration", isElectricalSystemsIntegration);
            session.setAttribute("FWJobLevelID", jobLevelId);
            LogManager.log("assignConCost =====" + assignConCost);
            if (assignConCost.equals("1")) {
               session.setAttribute("FWConcost", "checked");
               session.setAttribute("FWContractorcostvalue", "1");
            } else {
               session.setAttribute("FWConcost", "");
               session.setAttribute("FWContractorcostvalue", "0");
            }

            if (isRehire.equals("1")) {
               session.setAttribute("FWIsRehire", "checked");
            } else {
               session.setAttribute("FWIsRehire", " ");
            }

            if (oldTermDate.getTime() < 0L) {
               session.setAttribute("FWOldTermDate", "");
            } else {
               session.setAttribute("FWOldTermDate", date.format(oldTermDate));
            }

            if (rehireDate.getTime() < 0L) {
               session.setAttribute("FWRehireDate", "");
            } else {
               session.setAttribute("FWRehireDate", date.format(rehireDate));
            }

            if (isconToDirect.equals("1")) {
               session.setAttribute("FWIsConDirect", "checked");
            } else {
               session.setAttribute("FWIsConDirect", " ");
            }

            if (conDirectHireDate.getTime() < 0L) {
               session.setAttribute("FWConDirectHireDate", "");
            } else {
               session.setAttribute("FWConDirectHireDate", date.format(conDirectHireDate));
            }

            if (serviceDate.getTime() < 0L) {
               session.setAttribute("FWServiceDate", "");
            } else {
               session.setAttribute("FWServiceDate", date.format(serviceDate));
               LogManager.log("FWServiceDate value" + serviceDate);
            }

            session.setAttribute("FWJobTitleID", jobTitleId);
            session.setAttribute("FWOldJobTitleID", jobTitleId);
            session.setAttribute("FWAccountID", accountId);
            session.setAttribute("FWAvailBillHours", availBillHrs);
            session.setAttribute("FWVacationHours", vacationHr);
            session.setAttribute("FWPBAHours", PBAHr);
            session.setAttribute("FWCompTimeHours", CompTimeHr);
            session.setAttribute("FWHolidayFloatingTimeHours", HolidayFloatingTimeHr);
            session.setAttribute("FWDellEmployeeId", Dell_Employee_Id);
            if (isSpecialUser.equals("1")) {
               session.setAttribute("FWisSpecialEmployeeCheck", "checked");
            } else {
               session.setAttribute("FWisSpecialEmployeeCheck", "");
            }

            if (effectiveFrom.getTime() < 0L) {
               session.setAttribute("FWEffectiveFrom", "");
            } else {
               session.setAttribute("FWEffectiveFrom", date.format(effectiveFrom));
            }

            session.setAttribute("FWEmployeeCost", employeeCost);
            session.setAttribute("FWCostEffectiveFrom", date.format(employeeCostDate));
            LogManager.log("FWEmployeeCost " + employeeCost + " FWCostEffectiveFrom " + employeeCostDate);
            session.setAttribute("FWCompCenterId", compCenterId);
            sql = dataLogic.buildSQLString(sessionId, "SelectLastAuditDate");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var223) {
               dataLogic.rollbackTransaction(conn);
               throw var223;
            }

            dataLogic.commitTransaction(conn);
            Vector resultVector = (Vector)results.get(0);
            String lastAuditDate = resultVector.get(0).toString();
            session.setAttribute("FWLastAuditDate", lastAuditDate);
            session.setAttribute("FWServerDate", employee.get(59));
            LogManager.log(email);
            LogManager.log("" + session.getUser().toString().equals("admin"));

            try {
               if (SecurityManager.getUser(email) == null) {
                  checkemail = true;
               }
            } catch (Exception var222) {
               checkemail = false;
               LogManager.log("exception: ", (Throwable)var222);
            }

            LogManager.log("Flag Check" + checkemail);
            if (!session.getUser().toString().equals("admin") && checkemail) {
               session.setAttribute("FWValidInfoUser", "true");
            } else {
               session.setAttribute("FWValidInfoUser", "false");
            }

            LogManager.log("Info Validation" + session.getAttribute("FWValidInfoUser"));
            session.setAttribute("FWOldEmail", email);
            session.setAttribute("FWOldFirstName", firstName);
            session.setAttribute("FWOldLastName", lastName);
            session.setAttribute("FWOldDumHireDate", hireDate);
            session.setAttribute("FWOldDumLastUpdated", lastUpdated);
            session.setAttribute("FWEmployeeNum", employeeNum);
            session.setAttribute("FWEmpIdWage", employeeId_wage);
            session.setAttribute("FWComments", comments);
            session.setAttribute("FWSsn", ssn);
            session.setAttribute("FWFirstName", firstName);
            session.setAttribute("FWMiddleInitial", middleInitial);
            session.setAttribute("FWLastName", lastName);
            session.setAttribute("FWEmail", email);
            session.setAttribute("FWDepartmentID", departmentId);
            session.setAttribute("FWTitle", title);
            session.setAttribute("FWAddress_1", address_1);
            session.setAttribute("FWAddress_2", address_2);
            session.setAttribute("FWCity", city);
            session.setAttribute("FWState", state);
            session.setAttribute("FWZip", zip);
            LogManager.log("Line 609 ");
            if (hAreaCode != null) {
               session.setAttribute("FWHomePhoneAreaCode", hAreaCode);
            } else {
               session.setAttribute("FWHomePhoneAreaCode", "");
            }

            if (hPrefix != null) {
               session.setAttribute("FWHomePhonePrefix", hPrefix);
            } else {
               session.setAttribute("FWHomePhonePrefix", "");
            }

            if (hPostfix != null) {
               session.setAttribute("FWHomePhonePostfix", hPostfix);
            } else {
               session.setAttribute("FWHomePhonePostfix", "");
            }

            if (wAreaCode != null) {
               session.setAttribute("FWWorkPhoneAreaCode", wAreaCode);
            } else {
               session.setAttribute("FWWorkPhoneAreaCode", "");
            }

            if (wPrefix != null) {
               session.setAttribute("FWWorkPhonePrefix", wPrefix);
            } else {
               session.setAttribute("FWWorkPhonePrefix", "");
            }

            if (wPostfix != null) {
               session.setAttribute("FWWorkPhonePostfix", wPostfix);
            } else {
               session.setAttribute("FWWorkPhonePostfix", "");
            }

            session.setAttribute("FWExtention", extention);
            LogManager.log(extention);
            if (fAreaCode != null) {
               session.setAttribute("FWFaxAreaCode", fAreaCode);
            } else {
               session.setAttribute("FWFaxAreaCode", "");
            }

            if (fPrefix != null) {
               session.setAttribute("FWFaxPrefix", fPrefix);
            } else {
               session.setAttribute("FWFaxPrefix", "");
            }

            if (fPostfix != null) {
               session.setAttribute("FWFaxPostfix", fPostfix);
            } else {
               session.setAttribute("FWFaxPostfix", "");
            }

            if (cAreaCode != null) {
               session.setAttribute("FWCellAreaCode", cAreaCode);
            } else {
               session.setAttribute("FWCellAreaCode", "");
            }

            if (cPrefix != null) {
               session.setAttribute("FWCellPrefix", cPrefix);
            } else {
               session.setAttribute("FWCellPrefix", "");
            }

            if (cPostfix != null) {
               session.setAttribute("FWCellPostfix", cPostfix);
            } else {
               session.setAttribute("FWCellPostfix", "");
            }

            if (pAreaCode != null) {
               session.setAttribute("FWPagerAreaCode", pAreaCode);
            } else {
               session.setAttribute("FWPagerAreaCode", "");
            }

            if (pPrefix != null) {
               session.setAttribute("FWPagerPrefix", pPrefix);
            } else {
               session.setAttribute("FWPagerPrefix", "");
            }

            if (pPostfix != null) {
               session.setAttribute("FWPagerPostfix", pPostfix);
            } else {
               session.setAttribute("FWPagerPostfix", "");
            }

            session.setAttribute("FWSalaryPeriodID", salaryPeriodId);
            session.setAttribute("FWEmpWorkType", employeeTime);
            session.setAttribute("FWOldEmployeeTime", employeeTime);
            session.setAttribute("FWSalaryAmount", salaryAmount);
            session.setAttribute("FWBurdenFactor", burdenFactor);
            LogManager.log("Old Employee Id is here:" + session.getAttribute("FWOldEmployeeTime"));
            this.CollectAllEmployeeWageId(sessionId, dataLogic, conn, "Edit");
            String AuditTableValue;
            int accountid;
            if (hireDate.getTime() < 0L) {
               session.setAttribute("FWHireDate", "");
            } else {
               session.setAttribute("FWHireDate", date.format(hireDate));
               AuditTableValue = (String)session.getAttribute("FWHireDate");
               String dateYearStr = "";
               String yearHire = "";
               String normYear = "";
               accountid = Integer.parseInt(AuditTableValue.substring(6, 8));
               String day = AuditTableValue.substring(3, 5);
               String mon = AuditTableValue.substring(0, 2);
               if (accountid >= 80) {
                  dateYearStr = "19" + accountid;
                  yearHire = mon + "/" + day + "/" + dateYearStr;
               } else {
                  if (accountid <= 9) {
                     normYear = "200" + String.valueOf(accountid);
                  } else {
                     normYear = "20" + String.valueOf(accountid);
                  }

                  yearHire = mon + "/" + day + "/" + normYear;
               }

               LogManager.log("Formatted Hiredate FWFormattedHireDate" + yearHire);
               session.removeAttribute("FWFormattedHireDate");
               session.setAttribute("FWFormattedHireDate", yearHire);
            }

            if (termDate.getTime() < 0L) {
               session.setAttribute("FWTermDate", "");
            } else {
               session.setAttribute("FWTermDate", date.format(termDate));
            }

            session.setAttribute("FWPreTermDateValue", session.getAttribute("FWTermDate").toString());
            session.setAttribute("FWEmpIdWageExisting", employeeId_wage);
            session.setAttribute("FWTeamID", teamId);
            session.setAttribute("FWisProjectLeader", isProjectLeader);
            session.setAttribute("FWisAccountManager", isAccountManager);
            session.setAttribute("FWisDirectRep", isDirectRep);
            session.setAttribute("FWisSalesEngineer", isSalesEngineer);
            session.setAttribute("FWisSrSalesEngineer", isSrSalesEngineer);
            session.setAttribute("FWisVertical", isVertical);
            session.setAttribute("FWisJrPartner", isJrPartner);
            session.setAttribute("FWAnalysisVertical", isAnVertical);
            session.setAttribute("FWElecVertical", isElVertical);
            session.setAttribute("FWManFactVertical", isMfVertical);
            session.setAttribute("FWisChecker", isChecker);
            session.setAttribute("FWisEngAdmin", isEngAdmin);
            session.setAttribute("FWisPM", isPeopleManager);
            session.setAttribute("FWDataFeedValue", datafeed);
            if (peoplemanagerId == null) {
               session.setAttribute("FWPeopleManagerID", "0");
            } else {
               session.setAttribute("FWPeopleManagerID", peoplemanagerId);
            }

            try {
               if (SecurityManager.getUser(session.getUser().toString()).toString().equals("admin")) {
                  if (SecurityManager.isMember("admin", SecurityManager.getUser(email))) {
                     session.setAttribute("FWisAdminGP", "Checked");
                     session.setAttribute("FWOldisAdminGroup", "1");
                  } else {
                     session.setAttribute("FWisAdminGP", " ");
                     session.setAttribute("FWOldisAdminGroup", "0");
                  }
               } else {
                  session.setAttribute("FWisAdminGP", " ");
                  session.setAttribute("FWShowAdminGroupStart", "<!--");
                  session.setAttribute("FWShowAdminGroupEnd", "-->");
               }
            } catch (Exception var221) {
               session.setAttribute("FWisAdminGP", " ");
               session.setAttribute("FWShowAdminGroupStart", "<!--");
               session.setAttribute("FWShowAdminGroupEnd", "-->");
            }

            AuditTableValue = null;
            sql = dataLogic.buildSQLString(sessionId, "SelectAuditTableValue");
            dataLogic.startTransaction(conn);

            Vector AuditTableValue1;
            try {
               AuditTableValue1 = dataLogic.execute(conn, sql);
            } catch (EuropaException var220) {
               dataLogic.rollbackTransaction(conn);
               throw var220;
            }

            dataLogic.commitTransaction(conn);
            session.setAttribute("FWAuditValueFlag", String.valueOf(AuditTableValue1.size()));
            AuditTableValue1 = null;
            sql = dataLogic.buildSQLString(sessionId, "SelectPrevAuditTableValue");
            dataLogic.startTransaction(conn);

            try {
               AuditTableValue1 = dataLogic.execute(conn, sql);
            } catch (EuropaException var219) {
               dataLogic.rollbackTransaction(conn);
               throw var219;
            }

            dataLogic.commitTransaction(conn);
            Vector deptids;
            if (AuditTableValue1.isEmpty()) {
               session.setAttribute("FWPreAuditValueFlag", "0");
            } else {
               deptids = (Vector)AuditTableValue1.get(0);
               session.setAttribute("FWPreAuditValueFlag", deptids.get(0).toString());
            }

            int i;
            if (eSecureInfoAccess.contains(session.getUser().toString())) {
               session.setAttribute("FWSEmployeeID", session.getUser().getEmployeeId().toString());
               sql = dataLogic.buildSQLString(sessionId, "SelectEmpDeptAccess");
               dataLogic.startTransaction(conn);

               try {
                  results = dataLogic.execute(conn, sql);
               } catch (EuropaException var218) {
                  dataLogic.rollbackTransaction(conn);
                  throw var218;
               }

               dataLogic.commitTransaction(conn);
               deptids = new Vector();
               if (!results.isEmpty()) {
                  for(i = 0; i < results.size(); ++i) {
                     Vector v = (Vector)results.get(i);
                     String deptid = v.get(1).toString();
                     deptids.add(deptid);
                  }
               }

               LogManager.log("deptids:" + deptids);
               if (results.isEmpty()) {
                  this.showEmpSecureInfo(sessionId, true);
               } else if (!deptids.contains(departmentId)) {
                  this.showEmpSecureInfo(sessionId, false);
               }
            }

            sql = dataLogic.buildSQLString(sessionId, "SelectAllDepartments");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var217) {
               dataLogic.rollbackTransaction(conn);
               throw var217;
            }

            dataLogic.commitTransaction(conn);
            int dept = Integer.parseInt((String)session.getAttribute("FWDepartmentID"));
            LogManager.log("department check2");
            session.setAttribute("FWDepartmentsList", this.buildList(sessionId, results, dept, "FWDepartment"));
            i = Integer.parseInt((String)session.getAttribute("FWJobTitleID"));
            sql = dataLogic.buildSQLString(sessionId, "SelectAllJobTitles");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var216) {
               dataLogic.rollbackTransaction(conn);
               throw var216;
            }

            dataLogic.commitTransaction(conn);
            session.setAttribute("FWJobTitleList", this.buildList(sessionId, results, i, "FWJobTitle"));
            int jolevelbid = Integer.parseInt((String)session.getAttribute("FWJobLevelID"));
            sql = dataLogic.buildSQLString(sessionId, "SelectAllJobLevels");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var215) {
               dataLogic.rollbackTransaction(conn);
               throw var215;
            }

            dataLogic.commitTransaction(conn);
            session.setAttribute("FWJobLevelList", this.buildList(sessionId, results, jolevelbid, "FWJobLevel"));
            accountid = Integer.parseInt((String)session.getAttribute("FWAccountID"));
            sql = dataLogic.buildSQLString(sessionId, "SelectAllAccounts");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var214) {
               dataLogic.rollbackTransaction(conn);
               throw var214;
            }

            dataLogic.commitTransaction(conn);
            session.setAttribute("FWAccountList", this.buildList(sessionId, results, accountid, "FWAccount"));
            int currentCompCenterId = Integer.parseInt((String)session.getAttribute("FWCompCenterId"));
            sql = dataLogic.buildSQLString(sessionId, "SelectAllCompetencyCenters");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var213) {
               dataLogic.rollbackTransaction(conn);
               throw var213;
            }

            dataLogic.commitTransaction(conn);
            session.setAttribute("FWCompCenterList", this.buildList(sessionId, results, currentCompCenterId, "FWCompCenter"));
            this.buildSubCompetencyList(sessionId, dataLogic, conn, subCompCenterId);
            int currenttermClassificationId = Integer.parseInt((String)session.getAttribute("FWTermClassification"));
            sql = dataLogic.buildSQLString(sessionId, "SelectAllTermClassifications");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var212) {
               dataLogic.rollbackTransaction(conn);
               throw var212;
            }

            dataLogic.commitTransaction(conn);
            session.setAttribute("FWTermClassificationList", this.buildList(sessionId, results, currenttermClassificationId, "FWTermClassification"));
            LogManager.log("Employeeid :" + session.getAttribute("FWEmployeeID"));
            sql = dataLogic.buildSQLString(sessionId, "SelectPeopleManager");
            LogManager.log("FWPeopleManagerID :" + session.getAttribute("FWPeopleManagerID"));
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var211) {
               dataLogic.rollbackTransaction(conn);
               throw var211;
            }

            dataLogic.commitTransaction(conn);
            LogManager.log(" results  :" + results);
            LogManager.log("FWPeopleManagerID :" + session.getAttribute("FWPeopleManagerID"));
            int people = Integer.parseInt((String)session.getAttribute("FWPeopleManagerID"));
            Vector peopleManager = new Vector();
            int k = 0;
            int length = results.size();
            LogManager.log(" Size  :" + results.size());
            Vector temVector;
            if (results.size() > 0) {
               do {
                  temVector = (Vector)results.get(k);
                  String emailPM = temVector.get(2).toString();

                  try {
                     if (SecurityManager.isMember("People Manager", SecurityManager.getUser(emailPM))) {
                        peopleManager.add(results.get(k));
                        LogManager.log("Register User True " + emailPM);
                     }
                  } catch (Exception var210) {
                     LogManager.log("Register User false " + emailPM);
                  }

                  ++k;
               } while(k < length);
            }

            String isPL;
            String isSE;
            int payroll;
            try {
               results.clear();
               sql = dataLogic.buildSQLString(sessionId, "SelectedPeoplemanager");
               dataLogic.startTransaction(conn);

               try {
                  results = dataLogic.execute(conn, sql);
               } catch (EuropaException var209) {
                  dataLogic.rollbackTransaction(conn);
                  throw var209;
               }

               dataLogic.commitTransaction(conn);
               temVector = new Vector();
               if (!results.isEmpty()) {
                  for(payroll = 0; payroll < results.size(); ++payroll) {
                     Vector v = (Vector)results.get(payroll);
                     String empId = v.get(0).toString();
                     isPL = v.get(1).toString();
                     isSE = v.get(2).toString();
                     temVector.add(empId);
                     temVector.add(isPL);
                     temVector.add(isSE);
                  }

                  peopleManager.add(temVector);
               }
            } catch (Exception var225) {
            }

            LogManager.log(" Final results  :" + peopleManager);
            session.setAttribute("FWPeopleManagerListValue", this.buildList(sessionId, peopleManager, people, "FWPeopleManagerList"));
            session.setAttribute("FWEngineeringManagerList", this.buildEngMgrList(sessionId, dataLogic, conn));
            session.setAttribute("FWLeadEngineerList", this.buildLeadEngList(sessionId, dataLogic, conn));
            session.setAttribute("FWTopLineManagerListValue", this.buildTopLineMgrList(sessionId, dataLogic, conn));
            sql = dataLogic.buildSQLString(sessionId, "SelectAllTeams");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var208) {
               dataLogic.rollbackTransaction(conn);
               throw var208;
            }

            dataLogic.commitTransaction(conn);
            int team = Integer.parseInt((String)session.getAttribute("FWTeamID"));
            session.setAttribute("FWTeamsList", this.buildList(sessionId, results, team, "FWTeamID"));
            sql = dataLogic.buildSQLString(sessionId, "SelectAllPayRollDepts");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var207) {
               dataLogic.rollbackTransaction(conn);
               throw var207;
            }

            dataLogic.commitTransaction(conn);
            payroll = Integer.parseInt((String)session.getAttribute("FWPayRollDept"));
            session.setAttribute("FWPayRollDeptList", this.buildList(sessionId, results, payroll, "FWPayRollDept"));
            sql = dataLogic.buildSQLString(sessionId, "SelectAllSalaryPeriods");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var206) {
               dataLogic.rollbackTransaction(conn);
               throw var206;
            }

            dataLogic.commitTransaction(conn);
            int period = Integer.parseInt((String)session.getAttribute("FWSalaryPeriodID"));
            session.setAttribute("FWSalaryPeriodeForAudit", String.valueOf(period));
            session.setAttribute("FWSalaryPeriodsList", this.buildList(sessionId, results, period, "FWSalaryPeriod"));
            sql = dataLogic.buildSQLString(sessionId, "SelectAllEmployeeTime");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var205) {
               dataLogic.rollbackTransaction(conn);
               throw var205;
            }

            dataLogic.commitTransaction(conn);
            int empTimeId = Integer.parseInt((String)session.getAttribute("FWEmpWorkType"));
            LogManager.log("Employee time is given here:" + results + "   " + empTimeId);
            if (session.getAttribute("FWEmpWorkType").equals("1")) {
               session.setAttribute("FWWorkType", "Part Time");
            } else if (session.getAttribute("FWEmpWorkType").equals("2")) {
               session.setAttribute("FWWorkType", "Full Time");
            }

            session.setAttribute("FWEmpTimeIdForSecureInfoaudit", String.valueOf(empTimeId));
            session.setAttribute("FWEmployeeTimeList", this.buildList(sessionId, results, empTimeId, "FWEmpWorkType"));
            isPL = (String)session.getAttribute("FWisProjectLeader");
            if (isPL.equals("1")) {
               session.setAttribute("FWisPL", "checked");
            } else {
               session.setAttribute("FWisPL", " ");
            }

            isSE = (String)session.getAttribute("FWisSalesEngineer");
            if (isSE.equals("1")) {
               session.setAttribute("FWisSE", "checked");
            } else {
               session.setAttribute("FWisSE", " ");
            }

            String isAM = (String)session.getAttribute("FWisAccountManager");
            if (isAM.equals("1")) {
               session.setAttribute("FWisAM", "checked");
            } else {
               session.setAttribute("FWisAM", " ");
            }

            String isCOMPEAST = (String)session.getAttribute("FWisDesignAndAnalysCompEast");
            if (isCOMPEAST.equals("1")) {
               session.setAttribute("FWisCOMPEAST", "checked");
            } else {
               session.setAttribute("FWisCOMPEAST", " ");
            }

            String isCOMPWEST = (String)session.getAttribute("FWisDesignAndAnalysCompWest");
            if (isCOMPWEST.equals("1")) {
               session.setAttribute("FWisCOMPWEST", "checked");
            } else {
               session.setAttribute("FWisCOMPWEST", " ");
            }

            String isTOPLINEMANAGER = (String)session.getAttribute("FWisTopLineManager");
            if (isTOPLINEMANAGER.equals("1")) {
               session.setAttribute("FWisTLManager", "checked");
            } else {
               session.setAttribute("FWisTLManager", " ");
            }

            String isDR = (String)session.getAttribute("FWisDirectRep");
            if (isDR.equals("1")) {
               session.setAttribute("FWisDR", "checked");
            } else {
               session.setAttribute("FWisDR", " ");
            }

            String isSSE = (String)session.getAttribute("FWisSrSalesEngineer");
            String sseId;
            String isAnv;
            String jpId;
            if (isSSE.equals("1")) {
               sql = dataLogic.buildSQLString(sessionId, "SelectSSEDetails");
               dataLogic.startTransaction(conn);

               try {
                  results = dataLogic.execute(conn, sql);
               } catch (EuropaException var204) {
                  dataLogic.rollbackTransaction(conn);
                  throw var204;
               }

               dataLogic.commitTransaction(conn);
               LogManager.log("Results are:" + results);
               if (!results.isEmpty()) {
                  Vector sseResults = (Vector)results.get(0);
                  sseId = sseResults.get(0).toString();
                  isAnv = sseResults.get(1).toString();
                  jpId = sseResults.get(2).toString();
                  session.setAttribute("FWSseID", sseId);
                  session.setAttribute("FWSSEAllocatedHours", isAnv);
                  if (jpId.equals("1")) {
                     session.setAttribute("FWisSSEAutoAllocation1", "checked");
                  } else {
                     session.setAttribute("FWisSSEAutoAllocation1", " ");
                  }
               } else {
                  session.setAttribute("FWJpID", "");
                  session.setAttribute("FWSSEAllocatedHours", "");
                  session.setAttribute("FWisSSEAutoAllocation1", " ");
               }

               session.setAttribute("FWisSSE", "checked");
            } else {
               session.setAttribute("FWisSSE", " ");
            }

            String isVer = (String)session.getAttribute("FWisVertical");
            if (isVer.equals("1")) {
               session.setAttribute("FWisVer", "checked");
            } else {
               session.setAttribute("FWisVer", " ");
            }

            sseId = (String)session.getAttribute("FWisJrPartner");
            String isMfv;
            String isChk;
            if (sseId.equals("1")) {
               sql = dataLogic.buildSQLString(sessionId, "SelectJpDetails");
               dataLogic.startTransaction(conn);

               try {
                  results = dataLogic.execute(conn, sql);
               } catch (EuropaException var203) {
                  dataLogic.rollbackTransaction(conn);
                  throw var203;
               }

               dataLogic.commitTransaction(conn);
               LogManager.log("Results are:" + results);
               if (!results.isEmpty()) {
                  Vector jpResults = (Vector)results.get(0);
                  jpId = jpResults.get(0).toString();
                  isMfv = jpResults.get(1).toString();
                  isChk = jpResults.get(2).toString();
                  session.setAttribute("FWJpID", jpId);
                  session.setAttribute("FWJpAllocatedHours", isMfv);
                  if (isChk.equals("1")) {
                     session.setAttribute("FWisAutoAllocation1", "checked");
                  } else {
                     session.setAttribute("FWisAutoAllocation1", " ");
                  }
               } else {
                  session.setAttribute("FWJpID", "");
                  session.setAttribute("FWJpAllocatedHours", "");
                  session.setAttribute("FWisAutoAllocation1", " ");
               }

               session.setAttribute("FWisJpr", "checked");
            } else {
               session.setAttribute("FWisJpr", " ");
            }

            isAnv = (String)session.getAttribute("FWAnalysisVertical");
            if (isAnv.equals("1")) {
               session.setAttribute("FWisAnv", "checked");
            } else {
               session.setAttribute("FWisAnv", " ");
            }

            jpId = (String)session.getAttribute("FWElecVertical");
            if (jpId.equals("1")) {
               session.setAttribute("FWisElv", "checked");
            } else {
               session.setAttribute("FWisElv", " ");
            }

            isMfv = (String)session.getAttribute("FWManFactVertical");
            if (isMfv.equals("1")) {
               session.setAttribute("FWisMfv", "checked");
            } else {
               session.setAttribute("FWisMfv", " ");
            }

            isChk = (String)session.getAttribute("FWisChecker");
            if (isChk.equals("1")) {
               session.setAttribute("FWisChk", "checked");
            } else {
               session.setAttribute("FWisChk", " ");
            }

            String isPSystem = (String)session.getAttribute("FWisEngAdmin");
            if (isPSystem.equals("1")) {
               session.setAttribute("FWisEAdmn", "checked");
            } else {
               session.setAttribute("FWisEAdmn", " ");
            }

            String isEAdmn = (String)session.getAttribute("FWisPowerSystem");
            if (isEAdmn.equals("1")) {
               session.setAttribute("FWisPSystem", "checked");
            } else {
               session.setAttribute("FWisPSystem", " ");
            }

            String isOSMgr = (String)session.getAttribute("FWisOffshoreManager");
            if (isOSMgr.equals("1")) {
               session.setAttribute("FWisOffShoerMgr", "checked");
            } else {
               session.setAttribute("FWisOffShoerMgr", " ");
            }

            String isElectrical = (String)session.getAttribute("FWisElectricalSystemsIntegration");
            if (isElectrical.equals("1")) {
               session.setAttribute("FWisElectricalSysIntegration", "checked");
            } else {
               session.setAttribute("FWisElectricalSysIntegration", " ");
            }

            String isLEng = (String)session.getAttribute("FWisLeadEng");
            if (isLEng.equals("1")) {
               session.setAttribute("FWisLEng", "checked");
            } else {
               session.setAttribute("FWisLEng", " ");
            }

            String isTLMgrid = (String)session.getAttribute("FWisTopLineManager");
            if (isTLMgrid.equals("1")) {
               session.setAttribute("FWtlmIdList", "checked");
            } else {
               session.setAttribute("FWtlmIdList", " ");
            }

            String isEMgr = (String)session.getAttribute("FWisEngManager");
            if (isEMgr.equals("1")) {
               session.setAttribute("FWisEMgr", "checked");
            } else {
               session.setAttribute("FWisEMgr", " ");
            }

            String isPManager = (String)session.getAttribute("FWisPM");
            if (isPManager.equals("1")) {
               session.setAttribute("FWisPManager", "checked");
            } else {
               session.setAttribute("FWisPManager", " ");
            }

            String isRMGR = (String)session.getAttribute("FWisResourceManager");
            LogManager.log("isRMGR" + isRMGR);
            if (isRMGR.equals("1")) {
               session.setAttribute("FWisRManager", "checked");
            } else {
               session.setAttribute("FWisRManager", " ");
            }

            String isOffShoreMGR = (String)session.getAttribute("FWisOffshoreManager");
            LogManager.log("isOffShoreMGR" + isOffShoreMGR);
            if (isOffShoreMGR.equals("1")) {
               session.setAttribute("FWisOffshoreMgr", "checked");
            } else {
               session.setAttribute("FWisOffshoreMgr", " ");
            }

            String isElectricalSysIntMGR = (String)session.getAttribute("FWisElectricalSystemsIntegration");
            LogManager.log("isElectricalSysIntMGR" + isElectricalSysIntMGR);
            if (isElectricalSysIntMGR.equals("1")) {
               session.setAttribute("FWisElectricalSysIntMGR", "checked");
            } else {
               session.setAttribute("FWisElectricalSysIntMGR", " ");
            }

            try {
               String var167 = session.getAttribute("FWComment").toString();
            } catch (Exception var202) {
               session.setAttribute("FWComment", "on");
               LogManager.log("exception: ", (Throwable)var202);
            }

            LogManager.log("Details View in Emp");
            this.SetEmployeeInfoDetails(sessionId);
            session.setAttribute("FWPrevDetails", this.empSalaryAudit(sessionId));
            session.setAttribute("FWSpotBounsAudit", this.spotBonusCommisionAudit(sessionId));
            returnValue = true;
            if (this.hrmDataEntryUsers.contains(session.getUser().toString()) && !SecurityManager.isMember("admin", session.getUser())) {
               try {
                  if (SecurityManager.getUser(session.getAttribute("FWEmail").toString()).toString().equals(session.getUser().toString())) {
                     session.setAttribute("FWEmployeeViewStart", "");
                     session.setAttribute("FWEmployeeViewEnd", "");
                  } else {
                     session.setAttribute("FWEmployeeViewStart", "<!--");
                     session.setAttribute("FWEmployeeViewEnd", "-->");
                  }
               } catch (Exception var201) {
                  session.setAttribute("FWEmployeeViewStart", "<!--");
                  session.setAttribute("FWEmployeeViewEnd", "-->");
                  LogManager.log("exception: ", (Throwable)var201);
               }
            } else {
               try {
                  if (SecurityManager.getUser(session.getAttribute("FWEmail").toString()).toString().equals(session.getUser().toString())) {
                     session.setAttribute("FWEmployeeViewStart", "");
                     session.setAttribute("FWEmployeeViewEnd", "");
                  } else if (this.notAllowed.contains(session.getUser().toString())) {
                     session.setAttribute("FWEmployeeViewStart", "<!--");
                     session.setAttribute("FWEmployeeViewEnd", "-->");
                  } else {
                     session.setAttribute("FWEmployeeViewStart", "");
                     session.setAttribute("FWEmployeeViewEnd", "");
                  }
               } catch (Exception var200) {
                  LogManager.log("exception: ", (Throwable)var200);
               }
            }
         }
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var199) {
               LogManager.log("Unable to Close connection.", (Throwable)var199);
            }

            conn = null;
         }

      }

      return returnValue;
   }

   public void SetEmployeeInfoDetails(String sessionId) throws EuropaException {
      Session session = SessionManager.findSession(sessionId);
      SqlStatement sql = null;
      Vector results = null;
      DataLogic dataLogic = this.module.getDataLogic();
      Connection conn = dataLogic.openConnection();

      try {
         sql = dataLogic.buildSQLString(sessionId, "SelectEmpInfoDetails");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var17) {
            dataLogic.rollbackTransaction(conn);
            throw var17;
         }

         dataLogic.commitTransaction(conn);
         SimpleDateFormat date = new SimpleDateFormat("MM/dd/yy");
         Vector rowvalue = (Vector)results.get(0);
         Date lastUpdated = (Date)rowvalue.get(0);
         session.setAttribute("FWLastUpdated", date.format(lastUpdated));
         session.setAttribute("FWDumLastUpdated", date.format(lastUpdated));
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var16) {
               LogManager.log("Unable to Close connection.", (Throwable)var16);
            }

            conn = null;
         }

      }

   }

   private String BuildAuditCurrentSalary(String sessionId, StringBuffer auditBuffer) throws EuropaException {
      LogManager.log("BuildAuditCurrentSalary()");
      auditBuffer.append("<tr bgcolor=\"#7983FD\" ><td width=\"25%\" align=\"center\" ><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Period</b></font></td><td width=\"25%\" align=\"center\" ><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Time</b></font></td><td width=\"25%\" align=\"center\" ><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Salary</b></font></td><td width=\"25%\" align=\"center\" ><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Burden</b></font></td><td width=\"25%\" align=\"center\" ><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Date</b></font></td></tr>");
      return auditBuffer.toString();
   }

   public String empSalaryAudit(String sessionId) throws EuropaException {
      Session session = SessionManager.findSession(sessionId);
      DataLogic dataLogic = this.module.getDataLogic();
      Connection conn = dataLogic.openConnection();
      SqlStatement sql = null;
      Vector results = null;
      StringBuffer auditBuffer = null;

      try {
         SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy");
         SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
         Vector AuditTableValue = null;
         sql = dataLogic.buildSQLString(sessionId, "SelectAuditTableValue");
         dataLogic.startTransaction(conn);

         try {
            AuditTableValue = dataLogic.execute(conn, sql);
         } catch (EuropaException var69) {
            dataLogic.rollbackTransaction(conn);
            throw var69;
         }

         dataLogic.commitTransaction(conn);
         session.setAttribute("FWAuditValueFlag", String.valueOf(AuditTableValue.size()));
         AuditTableValue = null;
         sql = dataLogic.buildSQLString(sessionId, "SelectBeforeAuditTableValue");
         dataLogic.startTransaction(conn);

         try {
            AuditTableValue = dataLogic.execute(conn, sql);
         } catch (EuropaException var68) {
            dataLogic.rollbackTransaction(conn);
            throw var68;
         }

         dataLogic.commitTransaction(conn);
         Vector DumEffectivedate;
         if (AuditTableValue.isEmpty()) {
            session.setAttribute("FWBeforeAuditValueFlag", "0");
         } else {
            DumEffectivedate = (Vector)AuditTableValue.get(0);
            session.setAttribute("FWBeforeAuditValueFlag", DumEffectivedate.get(0).toString());
         }

         session.setAttribute("FWEffectiveStartDate", "");
         session.setAttribute("FWEffectiveEndDate", "");
         sql = dataLogic.buildSQLString(sessionId, "SelectEmpsalAuditDetails");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var67) {
            dataLogic.rollbackTransaction(conn);
            throw var67;
         }

         dataLogic.commitTransaction(conn);
         DumEffectivedate = null;
         sql = dataLogic.buildSQLString(sessionId, "SelectPrevAuditTableValue");
         dataLogic.startTransaction(conn);

         try {
            DumEffectivedate = dataLogic.execute(conn, sql);
         } catch (EuropaException var66) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var66);
            throw var66;
         }

         dataLogic.commitTransaction(conn);
         if (DumEffectivedate.isEmpty()) {
            session.setAttribute("FWPreAuditValueFlag", "0");
         } else {
            Vector row = (Vector)DumEffectivedate.get(0);
            session.setAttribute("FWPreAuditValueFlag", row.get(0).toString());
         }

         LogManager.log("PerAuditValueFlag" + session.getAttribute("FWPreAuditValueFlag"));
         LogManager.log("results;------------------------->" + results.size());
         LogManager.log("results;------------------------->" + results);
         auditBuffer = new StringBuffer();
         boolean flag = true;
         auditBuffer.append("<table  border=\"1\" width=\"100%\" cellspacing=\"0\" cellpadding=\"5\" bordercolor=\"#FFFFFF\" style=\"border-collapse: collapse\"><tr><td colspan=\"5\"><p align=\"left\"><font face=\"Tahoma\"><B><span style=\"font-size: 9pt\">Wage History   </span></B></font>");
         auditBuffer.append("<span id = \"disp\" ><font face=\"Tahoma\"><span style=\"font-size: 9pt\"><font size=\"2\"><a href=\"/servlets/ProxyServlet?FWModule=admin_employee&FWCurrentPage=8&FWArch=1&FWGoToSecure=1\">Archive</a></font></span></font></span></tr>");
         if (!results.isEmpty()) {
            this.BuildAuditCurrentSalary(sessionId, auditBuffer);
            session.setAttribute("FWNoRec", "0");
         } else {
            auditBuffer.append("<table border=\"1\" width=\"100%\" cellspacing=\"0\" cellpadding=\"5\" bordercolor=\"#FFFFFF\" style=\"border-collapse: collapse\"><tr bgcolor=\"#C4C8FB\"><td colspan=\"4\"><p align=\"left\"><font face=\"Tahoma\"><B><span style=\"font-size: 9pt\">No Wage History is available for the this year.</span></font></td>");
            session.setAttribute("FWNoRec", "1");
         }

         String CurrentSalaryPeriod = (String)session.getAttribute("FWSalaryPeriodID");
         String CurrentWorkType = (String)session.getAttribute("FWEmpWorkType");
         String CurrentSalaryAmount = (String)session.getAttribute("FWSalaryAmount");
         String CurrentBurden = (String)session.getAttribute("FWBurdenFactor");
         String dateEntered = null;
         LogManager.log("Crrent value" + CurrentSalaryPeriod + ":Work" + CurrentWorkType + ":CurrentSalaryAmount" + CurrentSalaryAmount + ":CurrentBurden" + CurrentBurden);
         boolean isSameid = true;
         String oldSno = "0";
         String auditPeriod = "As Above";
         String salary = "As Above";
         String burden = "As Above";
         String empTime = "As Above";
         String new_auditPeriod = "";
         String new_salary = "";
         String new_burden = "";
         String new_empTime = "";
         String dateEntered1 = null;
         String disableStr = "";
         int is_firstEntry = 0;
         String empSecureInfoEditUsers = System.getProperty("europa.employees.secureinfo.editaccess");
         ArrayList secureInfoEditUsers = new ArrayList();
         StringTokenizer secureInfoEditUserTokens = new StringTokenizer(empSecureInfoEditUsers, ",");

         while(secureInfoEditUserTokens.hasMoreTokens()) {
            secureInfoEditUsers.add(secureInfoEditUserTokens.nextToken().toString());
         }

         if (!secureInfoEditUsers.contains(session.getUser().toString()) && !session.getUser().getEmployeeId().toString().equals("82")) {
            disableStr = " disabled ";
         } else {
            disableStr = "";
         }

         StringBuffer snoList = new StringBuffer();
         int temp = results.size();
         String tempdateentered = null;
         String sno = null;
         String final_count = "0";

         for(int a = 0; a < results.size(); ++a) {
            Vector auditResults = (Vector)results.get(a);
            sno = auditResults.get(0).toString();
            String colName = auditResults.get(1).toString();
            String colData = auditResults.get(2).toString();
            dateEntered = auditResults.get(3).toString();
            LogManager.log("><><><><>" + CurrentSalaryPeriod + "<><>< Sno  :" + sno + " OldSno:: " + oldSno + " colname:: " + colName + " coldata:: " + colData + " date:: " + dateEntered);
            if (a != 0 && !oldSno.equals(sno)) {
               snoList.append(oldSno).append(",");
               if (new_auditPeriod.equals("")) {
                  auditPeriod = "As Above";
               } else if (new_auditPeriod.equals("1")) {
                  auditPeriod = "Annual";
               } else if (new_auditPeriod.equals("2")) {
                  auditPeriod = "Hourly";
               } else if (new_auditPeriod.equals("3")) {
                  auditPeriod = "1099";
               }

               if (new_empTime.equals("")) {
                  empTime = "As Above";
               } else if (new_empTime.equals("1")) {
                  empTime = "Part Time";
               } else if (new_empTime.equals("2")) {
                  empTime = "Full Time";
               }

               if (new_salary.equals("")) {
                  salary = "As Above";
               } else {
                  salary = new_salary;
               }

               if (new_burden.equals("")) {
                  burden = "As Above";
               } else {
                  burden = new_burden;
               }

               LogManager.log("Count Value" + a);
               LogManager.log("Period = " + new_auditPeriod + ": Time =" + new_empTime + " :new_salary = " + new_salary + " : new_burden = " + new_burden);
               auditBuffer.append("<tr bgcolor=\"#C4C8FB\" >");
               auditBuffer.append("</font></td><td width=\"24%\" height=\"34\"><p align=\"left\"><font face=\"Tahoma\" style=\"font-size: 9pt\">").append(auditPeriod);
               auditBuffer.append("<td width=\"21%\" height=\"34\"><font face=\"Tahoma\" style=\"font-size: 9pt\">").append(empTime);
               auditBuffer.append("</font></td><td width=\"24%\" height=\"34\"><p align=\"right\"><font face=\"Tahoma\" style=\"font-size: 9pt\">").append(salary);
               auditBuffer.append("</font></td><td width=\"24%\" height=\"34\"><p align=\"right\"><font face=\"Tahoma\" style=\"font-size: 9pt\">").append(burden);
               auditBuffer.append("</font></td><td width=\"24%\" height=\"34\"><p align=\"center\"><font face=\"Tahoma\" style=\"font-size: 9pt\">");

               try {
                  auditBuffer.append("<input type=\"text\" name=\"DateEntered" + oldSno + "\" maxlength=\"8\" size=\"9\" " + disableStr + " value=\"" + formatter.format(parser.parse(tempdateentered)) + "\" onfocus=\"enableSaveButton(this.name, document.getElementById('updateWages" + oldSno + "').name)\" class = \"style1\">").append("<input type=\"hidden\" name=\"hiddenSno" + a + "\" value=\"" + oldSno + "\">").append("<input type=\"hidden\" name=\"hiddenDateEntered" + oldSno + "\" id=\"hiddenDateEntered" + oldSno + "\" value=\"" + formatter.format(parser.parse(tempdateentered)) + "\">").append("<FIELD=\"FWSecureInfoEditStart\">").append("<input type=\"button\" name=\"updateWages" + oldSno + "\" disabled value=\"Save\" onclick=\"updateWagesDate(document.getElementById('DateEntered" + oldSno + "').value,document.getElementById('hiddenSno" + a + "').value," + a + ")\" class = \"style1\" >").append("<FIELD=\"FWSecureInfoEditEnd\">");
               } catch (Exception var65) {
                  LogManager.log("Error in date format in build emp audit: ", (Throwable)var65);
               }

               auditBuffer.append("</font></td></tr>");
               new_salary = "";
               new_burden = "";
               new_empTime = "";
               new_auditPeriod = "";
            }

            LogManager.log("Before  = " + new_auditPeriod + ": Time =" + new_empTime + " :new_salary = " + new_salary + " : new_burden = " + new_burden);
            if (colName.equals("SALARY")) {
               new_salary = colData;
            } else if (colName.equals("BURDEN")) {
               new_burden = colData;
            } else if (colName.equals("EMPTIME")) {
               new_empTime = colData;
            } else if (colName.equals("PERIOD")) {
               new_auditPeriod = colData;
            }

            LogManager.log("After  = " + new_auditPeriod + ": Time =" + new_empTime + " :new_salary = " + new_salary + " : new_burden = " + new_burden);

            try {
               tempdateentered = dateEntered;
               formatter.format(parser.parse(dateEntered));
            } catch (Exception var64) {
               LogManager.log("exception: ", (Throwable)var64);
            }

            LogManager.log("salary.." + salary);
            if (!sno.equals(sno) && a != 0) {
               oldSno = sno;
            } else {
               oldSno = sno;
            }
         }

         if (!results.isEmpty()) {
            if (new_auditPeriod.equals("")) {
               auditPeriod = "As Above";
            } else if (new_auditPeriod.equals("1")) {
               auditPeriod = "Annual";
            } else if (new_auditPeriod.equals("2")) {
               auditPeriod = "Hourly";
            } else if (new_auditPeriod.equals("3")) {
               auditPeriod = "1099";
            }

            if (new_empTime.equals("")) {
               empTime = "As Above";
            } else if (new_empTime.equals("1")) {
               empTime = "Part Time";
            } else if (new_empTime.equals("2")) {
               empTime = "Full Time";
            }

            if (new_salary.equals("")) {
               salary = "As Above";
            } else {
               salary = new_salary;
            }

            if (new_burden.equals("")) {
               burden = "As Above";
            } else {
               burden = new_burden;
            }

            final_count = String.valueOf(results.size());
            auditBuffer.append("<tr bgcolor=\"#C4C8FB\" >");
            auditBuffer.append("</font></td><td width=\"24%\" height=\"34\"><p align=\"left\"><font face=\"Tahoma\" style=\"font-size: 9pt\">").append(auditPeriod);
            auditBuffer.append("<td width=\"21%\" height=\"34\"><font face=\"Tahoma\" style=\"font-size: 9pt\">").append(empTime);
            auditBuffer.append("</font></td><td width=\"24%\" height=\"34\"><p align=\"right\"><font face=\"Tahoma\" style=\"font-size: 9pt\">").append(salary);
            auditBuffer.append("</font></td><td width=\"24%\" height=\"34\"><p align=\"right\"><font face=\"Tahoma\" style=\"font-size: 9pt\">").append(burden);
            auditBuffer.append("</font></td><td width=\"24%\" height=\"34\"><p align=\"center\"><font face=\"Tahoma\" style=\"font-size: 9pt\">");

            try {
               auditBuffer.append("<input type=\"text\" name=\"DateEntered" + sno + "\" maxlength=\"8\" size=\"9\" " + disableStr + " value=\"" + formatter.format(parser.parse(tempdateentered)) + "\" onfocus=\"enableSaveButton(this.name, document.getElementById('updateWages" + sno + "').name)\" class = \"style1\">").append("<input type=\"hidden\" name=\"hiddenSno" + final_count + "\" value=\"" + sno + "\">").append("<input type=\"hidden\" name=\"hiddenDateEntered" + sno + "\" id=\"hiddenDateEntered" + sno + "\" value=\"" + formatter.format(parser.parse(tempdateentered)) + "\">").append("<FIELD=\"FWSecureInfoEditStart\">").append("<input type=\"button\" name=\"updateWages" + sno + "\" disabled value=\"Save\" onclick=\"updateWagesDate(document.getElementById('DateEntered" + sno + "').value,document.getElementById('hiddenSno" + final_count + "').value," + final_count + ")\" class = \"style1\" >").append("<FIELD=\"FWSecureInfoEditEnd\">");
            } catch (Exception var63) {
               LogManager.log("exception: ", (Throwable)var63);
            }

            auditBuffer.append("</font></td></tr>");
            snoList.append(sno);
         }

         session.setAttribute("FWWageDateLength", String.valueOf(temp));

         try {
            session.setAttribute("FWSno", snoList.toString());
         } catch (Exception var62) {
            session.setAttribute("FWSno", "0");
         }

         auditBuffer.append("</table>");
         session.setAttribute("FWCurPage", session.getAttribute("FWCurrentPage"));
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var61) {
               LogManager.log("Unable to Close connection.", (Throwable)var61);
            }

            conn = null;
         }

      }

      return auditBuffer.toString();
   }

   public String spotBonusCommisionAudit(String sessionId) throws EuropaException {
      Session session = SessionManager.findSession(sessionId);
      DataLogic dataLogic = this.module.getDataLogic();
      Connection conn = dataLogic.openConnection();
      LogManager.log("AdminEmployeeBusinessLogic.spotBonusCommisionAudit");
      SqlStatement sql = null;
      Vector results = null;
      StringBuffer auditBuffer1 = null;

      try {
         SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy");
         SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
         DecimalFormat decimal = new DecimalFormat("#,##0.00");
         sql = dataLogic.buildSQLString(sessionId, "SelectSpotBonusAuditDetails");
         LogManager.log("Comming to there" + sql);
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var33) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var33);
            throw var33;
         }

         dataLogic.commitTransaction(conn);
         auditBuffer1 = new StringBuffer();
         boolean isSameid = true;
         String disableStr = "";
         if (!results.isEmpty()) {
            auditBuffer1.append("&nbsp;<div align=\"left\">");
            auditBuffer1.append("<table border=\"1\" width=\"100%\" bgcolor=\"#C4C8FB\" cellpadding=\"5\" bordercolorlight=\"#FFFFFF\" style=\"border-collapse: collapse\">").append("<tr><td align=\"center\" colspan=\"2\"><p align=\"left\"><font face=\"Tahoma\"><b><span style=\"font-size: 9pt\">Spot Bonus History</span></b></font></td><tr><td width=\"25%\" align=\"right\" ><p align=\"center\"><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Amount</b></font></td><td width=\"25%\" align=\"center\" ><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Date</b></font></td></tr>");
            String empSecureInfoEditUsers = System.getProperty("europa.employees.secureinfo.editaccess");
            ArrayList secureInfoEditUsers = new ArrayList();
            StringTokenizer secureInfoEditUserTokens = new StringTokenizer(empSecureInfoEditUsers, ",");

            while(secureInfoEditUserTokens.hasMoreTokens()) {
               String username = secureInfoEditUserTokens.nextToken().toString();
               secureInfoEditUsers.add(username);
            }

            if (!secureInfoEditUsers.contains(session.getUser().toString()) && !session.getUser().getEmployeeId().toString().equals("82")) {
               disableStr = " disabled ";
            } else {
               disableStr = "";
            }

            for(int a = 0; a < results.size(); ++a) {
               Vector auditResults = (Vector)results.get(a);
               String sno = auditResults.get(0).toString();
               String amount = auditResults.get(1).toString();
               String date = auditResults.get(2).toString();
               LogManager.log("Amount:" + amount + "date :" + date);

               try {
                  try {
                     auditBuffer1.append("<tr><td width=\"23%\" align=\"right\"><font face=\"Tahoma\" style=\"font-size: 9pt\">");
                     auditBuffer1.append(decimal.format(Double.parseDouble(amount)));
                  } catch (Exception var31) {
                     auditBuffer1.append(amount);
                     LogManager.log("exception: ", (Throwable)var31);
                  }

                  auditBuffer1.append("</font></td><td width=\"24%\" align=\"center\"><font face=\"Tahoma\" style=\"font-size: 9pt\">").append("<input type=\"text\" name=\"spotBonusDate" + sno + "\" maxlength=\"8\" size=\"9\" " + disableStr + " value=\"" + formatter.format(parser.parse(date)) + "\" onfocus=\"enableSaveButton(this.name, document.getElementById('updateSpotBonus" + sno + "').name)\" class = \"style1\">").append("<input type=\"hidden\" name=\"hiddenSpotSno" + a + "\" value=\"" + sno + "\">").append("<FIELD=\"FWSecureInfoEditStart\">").append("<input type=\"button\" name=\"updateSpotBonus" + sno + "\" disabled value=\"Save\" onclick=\"updateSpotBonusDate(document.getElementById('spotBonusDate" + sno + "').value,document.getElementById('hiddenSpotSno" + a + "').value)\" class = \"style1\" >").append("<FIELD=\"FWSecureInfoEditEnd\">").append("</font></td></tr>");
               } catch (Exception var32) {
                  LogManager.log("exception: ", (Throwable)var32);
               }
            }

            auditBuffer1.append("</table>");
         } else {
            auditBuffer1.append("<table border=\"1\" width=\"100%\" cellpadding=\"5\" bgcolor=\"#C4C8FB\" bordercolorlight=\"#FFFFFF\" style=\"border-collapse: collapse\">").append("<tr><td align=\"center\" colspan=\"2\"><p align=\"left\"><font face=\"Tahoma\"><b><span style=\"font-size: 9pt\">No Spot Bonus History Information Available</span></b></font></td>").append("</table>");
         }
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var30) {
               LogManager.log("Unable to Close connection.", (Throwable)var30);
            }

            conn = null;
         }

      }

      return auditBuffer1.toString();
   }

   private void showNewEmployeeButton(String sessionId, boolean showButton) {
      Session session = SessionManager.findSession(sessionId);
      if (!showButton) {
         session.setAttribute("FWShowNewEmployeeButtonStart", "<!--");
         session.setAttribute("FWShowNewEmployeeButtonEnd", "-->");
      } else {
         session.removeAttribute("FWShowNewEmployeeButtonStart");
         session.removeAttribute("FWShowNewEmployeeButtonEnd");
      }

   }

   private void showHrmWagesPermissionButton(String sessionId, boolean showButton) {
      Session session = SessionManager.findSession(sessionId);
      if (!showButton) {
         session.setAttribute("FWShowHrmWagesPermissionButtonStart", "<!--");
         session.setAttribute("FWShowHrmWagesPermissionButtonEnd", "-->");
      } else {
         session.removeAttribute("FWShowHrmWagesPermissionButtonStart");
         session.removeAttribute("FWShowHrmWagesPermissionButtonEnd");
      }

   }

   private void showEmployeeSearchButton(String sessionId, boolean showButton) {
      Session session = SessionManager.findSession(sessionId);
      if (!showButton) {
         session.setAttribute("FWShowEmployeeSearchButtonStart", "<!--");
         session.setAttribute("FWShowEmployeeSearchButtonEnd", "-->");
      } else {
         session.removeAttribute("FWShowEmployeeSearchButtonStart");
         session.removeAttribute("FWShowEmployeeSearchButtonEnd");
      }

   }

   public void showHRMMasterScreenButton(String sessionId, boolean showButton) {
      Session session = SessionManager.findSession(sessionId);
      if (!showButton) {
         session.setAttribute("FWShowHRMMasterScreenButtonStart", "<!--");
         session.setAttribute("FWShowHRMMasterScreenButtonEnd", "-->");
      } else {
         session.removeAttribute("FWShowHRMMasterScreenButtonStart");
         session.removeAttribute("FWShowHRMMasterScreenButtonEnd");
      }

   }

   private void showHrmButton(String sessionId, boolean showButton) {
      Session session = SessionManager.findSession(sessionId);
      if (!showButton) {
         session.setAttribute("FWShowHRMInfoStart", "<!--");
         session.setAttribute("FWShowHRMInfoEnd", "-->");
      } else {
         session.removeAttribute("FWShowHRMInfoStart");
         session.removeAttribute("FWShowHRMInfoEnd");
      }

   }

   private void showEmployeeData(String sessionId, boolean showData) {
      Session session = SessionManager.findSession(sessionId);
      if (!showData) {
         session.setAttribute("FWShowEmployeeDataStart", "<!--");
         session.setAttribute("FWShowEmployeeDataEnd", "-->");
         session.setAttribute("FWShowEmployeeDataStart1", "/*");
         session.setAttribute("FWShowEmployeeDataEnd1", "*/");
         session.setAttribute("FWJobAccountAvailHrs", "disabled");
         session.setAttribute("FWCompCenterStatus", "disabled");
      } else {
         session.removeAttribute("FWShowEmployeeDataStart");
         session.removeAttribute("FWShowEmployeeDataEnd");
         session.removeAttribute("FWShowEmployeeDataStart1");
         session.removeAttribute("FWShowEmployeeDataEnd1");
         session.removeAttribute("FWJobAccountAvailHrs");
         session.removeAttribute("FWCompCenterStatus");
      }

   }

   private String buildList(String sessionId, Vector results, int selectedIndex, String name) {
      Session session = SessionManager.findSession(sessionId);
      StringBuffer list = new StringBuffer();
      StringBuffer sbAvailBillHrs = new StringBuffer();
      StringBuffer sbDepartment = new StringBuffer();

      for(int i = 0; i < results.size(); ++i) {
         Vector row = (Vector)results.get(i);
         String value = row.get(0).toString();
         String data = row.get(1).toString();
         String selectedIndexString = Integer.toString(selectedIndex);
         String AvailBillHrs;
         if (name.equals("FWHRMWorkLocationID")) {
            AvailBillHrs = row.get(2).toString();
            if (value.equals(selectedIndexString)) {
               list.append("<option value=\"").append(value).append("#" + AvailBillHrs).append("\" selected>");
            } else {
               list.append("<option value=\"").append(value).append("#" + AvailBillHrs).append("\">");
            }

            list.append(data);
            list.append("</option>");
         } else if (!name.equals("FWSalaryPeriod") || !data.equals("1099")) {
            if (name.equals("FWDepartment")) {
               if (i == 0) {
                  sbAvailBillHrs.append("[");
                  sbDepartment.append("[");
               }

               AvailBillHrs = row.get(2).toString();
               sbDepartment.append(value);
               sbAvailBillHrs.append(AvailBillHrs);
               if (i == results.size() - 1) {
                  sbDepartment.append("]");
                  sbAvailBillHrs.append("]");
               } else {
                  sbDepartment.append(",");
                  sbAvailBillHrs.append(",");
               }

               session.setAttribute("FWDepartmentArray", sbDepartment.toString());
               session.setAttribute("FWAvailBillHrsArray", sbAvailBillHrs.toString());
            }

            if (value.equals(selectedIndexString)) {
               list.append("<option value=\"").append(value).append("\" selected>");
            } else {
               list.append("<option value=\"").append(value).append("\">");
            }

            list.append(data);
            list.append("</option>");
            if (name.equals("FWPeopleManagerList") && value.equals(selectedIndexString)) {
               session.setAttribute("FWAssociatedPeopleManager", data);
            }
         }
      }

      return list.toString();
   }

   private String buildHelmetAwardsDetails(String sessionId) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.BuildHelmetAwardsDetails()");
      Session session = SessionManager.findSession(sessionId);
      DecimalFormat decimal = new DecimalFormat("#,###0.00");
      Connection conn = null;
      StringBuffer helmetAudit = null;

      try {
         DataLogic dataLogic = this.module.getDataLogic();
         SqlStatement sql = null;
         Vector results = null;
         conn = dataLogic.openConnection();
         boolean isPageFive = true;
         String queryName = "";
         if (!session.getAttribute("FWCurrentPage").equals("5") && !session.getAttribute("FWCurrentPage").equals("6") && !session.getAttribute("FWCurrentPage").equals("4")) {
            queryName = "SelectAwardedHelmetsArchive";
            isPageFive = false;
         } else {
            queryName = "SelectAwardedHelmets";
         }

         sql = dataLogic.buildSQLString(sessionId, queryName);
         LogManager.log("Comming to there" + sql);
         dataLogic.startTransaction(conn);

         try {
            LogManager.log("Comming here------Current Page;" + session.getAttribute("FWCurrentPage"));
            results = dataLogic.execute(conn, sql);
            LogManager.log("Comming here executed0" + results);
         } catch (EuropaException var41) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var41);
            throw var41;
         }

         dataLogic.commitTransaction(conn);
         helmetAudit = new StringBuffer();
         LogManager.log("results is" + results);
         this.oldHelmetResults = results;
         int a;
         Vector auditResults;
         String sno;
         String amount;
         String expDate;
         String claimAmt;
         Vector claimResults;
         Vector additionalResults;
         String lastUpdated;
         String claimDate;
         Vector clRow;
         double availAmt;
         if (results.isEmpty()) {
            if (!session.getAttribute("FWCurrentPage").equals("5")) {
               helmetAudit.append("<table border=\"1\" width=\"100%\" cellpadding=\"5\" bordercolorlight=\"#FFFFFF\" style=\"border-collapse: collapse\">").append("<tr><td align=\"center\" colspan=\"2\"><p align=\"left\"><font face=\"Tahoma\"><b><span style=\"font-size: 9pt\">No Helmet Awards History Information Available</span></b></font></td>").append("</table>");
            }
         } else {
            String page = "";
            if (isPageFive) {
               page = "present";
            } else {
               page = "none";
            }

            helmetAudit.append(this.buildHelmetHeadder(page, sessionId));

            for(a = 0; a < results.size(); ++a) {
               auditResults = (Vector)results.get(a);
               sno = auditResults.get(0).toString();
               amount = auditResults.get(1).toString();
               expDate = auditResults.get(2).toString();
               session.setAttribute("FWRef_Sno", sno);
               claimAmt = "0";
               sql = dataLogic.buildSQLString(sessionId, "SelectClaimAmount");
               LogManager.log("amount ===" + amount + ".....");
               new Vector();
               dataLogic.startTransaction(conn);

               try {
                  claimResults = dataLogic.execute(conn, sql);
               } catch (EuropaException var40) {
                  dataLogic.rollbackTransaction(conn);
                  LogManager.log("Exception e is:" + var40);
                  throw var40;
               }

               dataLogic.commitTransaction(conn);
               if (!claimResults.isEmpty()) {
                  additionalResults = (Vector)claimResults.get(0);
                  claimAmt = additionalResults.get(0).toString();
               } else {
                  claimAmt = "0";
               }

               sql = dataLogic.buildSQLString(sessionId, "SelectClaimData");
               LogManager.log("amount ===" + amount + ".....");
               new Vector();
               dataLogic.startTransaction(conn);

               try {
                  additionalResults = dataLogic.execute(conn, sql);
               } catch (EuropaException var39) {
                  dataLogic.rollbackTransaction(conn);
                  LogManager.log("Exception e is:" + var39);
                  throw var39;
               }

               dataLogic.commitTransaction(conn);
               lastUpdated = "";
               claimDate = "";
               if (!additionalResults.isEmpty()) {
                  clRow = (Vector)additionalResults.get(0);
                  if (clRow.get(0) != null) {
                     lastUpdated = clRow.get(0).toString();
                  }

                  if (clRow.get(1) != null) {
                     claimDate = clRow.get(1).toString();
                  }
               }

               availAmt = Double.parseDouble(amount) - Double.parseDouble(claimAmt);
               LogManager.log("avail Amount" + availAmt + "claim amt " + claimAmt);
               LogManager.log("Hrm Helmet users:" + this.hrmHelmetAwardEditUsers + "Present..." + page);
               if (page.equals("present") && (SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") || this.hrmDataEntryUsers.contains(session.getUser().toString()) || this.hrmEditUsers.contains(session.getUser().toString()) || this.hrmHelmetAwardEditUsers.contains(session.getUser().toString())) && !session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString())) {
                  helmetAudit.append("<tr>");
                  helmetAudit.append("<td width=\"105\" align=\"center\"><input type=\"checkbox\" name=\"FWChk" + a + "\"  value=\"" + a + "\" onClick =\"validate(this);\"></td>" + "<td align=\"center\">" + "<input type=\"text\" name=\"FWAmt" + a + "\" readonly size=\"18\" value = \"" + amount + "\" class=\"style1\"></td>" + "<td align=\"center\">" + "<input type=\"text\" name=\"FWDtExp" + a + "\" readonly size=\"18\" value = \"" + expDate + "\" class=\"style1\"></td>" + "<td align=\"center\">" + "<input type=\"text\" name=\"FWAvailAmt" + a + "\" size=\"18\" readonly value = \"" + decimal.format(availAmt) + "\" class=\"style1\"></td>" + "<td align=\"center\">" + "<input type=\"text\" name=\"FWClAmt" + a + "\" size=\"18\"  value = \"" + "\" class=\"style1\"></td>" + "<td align=\"center\">" + "<input type=\"text\" name=\"FWClDate" + a + "\" size=\"18\"  value = \"" + "\" class=\"style1\"></td>");
                  if (Double.parseDouble(claimAmt) == 0.0) {
                     helmetAudit.append("<td align=\"center\"><a href=\"/servlets/ProxyServlet?FWModule=admin_employee&FWHelmet_Sno=" + sno + "\" onclick = \"return (confirm('Are you sure?'))\" ><img src = \"/assets/images/recyclebin_hrm.gif\" border =0 ></img></a></td>" + "</tr>");
                  } else {
                     helmetAudit.append("<td align=\"center\"></td></tr>");
                  }

                  helmetAudit.append("<input type = hidden name = \"FWHelmetSno" + a + "\" value =\"" + sno + "\" >");
               } else {
                  helmetAudit.append("<tr  ><td align=\"center\" class=\"style1\" >" + lastUpdated + "</td>" + "<td align=\"center\" class=\"style1\" >" + amount + "</td>" + "<td align=\"center\" class=\"style1\" >" + expDate + "</td>" + "<td align=\"center\" class=\"style1\" >" + decimal.format(availAmt) + "</td>" + "<td align=\"center\" class=\"style1\" >" + (Double.parseDouble(amount) - availAmt) + "</td>" + "<td align=\"center\" class=\"style1\" >" + claimDate + "</td>" + "</tr>");
               }
            }

            helmetAudit.append("</table>");
            helmetAudit.append("<input type = hidden name = \"FWDtExpCount\" value = \"" + results.size() + "\">");
         }

         if (session.getAttribute("FWCurrentPage").equals("5") || session.getAttribute("FWCurrentPage").equals("4")) {
            sql = dataLogic.buildSQLString(sessionId, "SelectAwardedHelmetsHistory");
            LogManager.log("Comming to there" + sql);
            dataLogic.startTransaction(conn);

            try {
               LogManager.log("Comming here");
               results = dataLogic.execute(conn, sql);
               LogManager.log("Comming here executed0" + results);
            } catch (EuropaException var38) {
               dataLogic.rollbackTransaction(conn);
               LogManager.log("Exception e is:" + var38);
               throw var38;
            }

            dataLogic.commitTransaction(conn);
            StringBuffer helmetHistory = new StringBuffer();
            if (results.isEmpty()) {
               helmetHistory.append("<table border=\"1\" width=\"100%\" cellpadding=\"5\" bordercolorlight=\"#FFFFFF\" style=\"border-collapse: collapse\">").append("<tr><td align=\"center\" colspan=\"2\"><p align=\"left\"><font face=\"Tahoma\"><b><span style=\"font-size: 9pt\">No Helmet Awards History Information Available</span></b></font></td>").append("</table>");
            } else {
               helmetHistory.append(this.buildHelmetHeadder("history", sessionId));

               for(a = 0; a < results.size(); ++a) {
                  auditResults = (Vector)results.get(a);
                  sno = auditResults.get(0).toString();
                  amount = auditResults.get(1).toString();
                  expDate = auditResults.get(2).toString();
                  session.setAttribute("FWRef_Sno", sno);
                  claimAmt = "0";
                  sql = dataLogic.buildSQLString(sessionId, "SelectClaimAmount");
                  LogManager.log("amount ===" + amount + ".....");
                  new Vector();
                  dataLogic.startTransaction(conn);

                  try {
                     claimResults = dataLogic.execute(conn, sql);
                  } catch (EuropaException var37) {
                     dataLogic.rollbackTransaction(conn);
                     LogManager.log("Exception e is:" + var37);
                     throw var37;
                  }

                  dataLogic.commitTransaction(conn);
                  if (!claimResults.isEmpty()) {
                     additionalResults = (Vector)claimResults.get(0);
                     claimAmt = additionalResults.get(0).toString();
                  } else {
                     claimAmt = "0";
                  }

                  sql = dataLogic.buildSQLString(sessionId, "SelectClaimData");
                  LogManager.log("amount ===" + amount + ".....");
                  new Vector();
                  dataLogic.startTransaction(conn);

                  try {
                     additionalResults = dataLogic.execute(conn, sql);
                  } catch (EuropaException var36) {
                     dataLogic.rollbackTransaction(conn);
                     LogManager.log("Exception e is:" + var36);
                     throw var36;
                  }

                  dataLogic.commitTransaction(conn);
                  lastUpdated = "";
                  claimDate = "";
                  if (!additionalResults.isEmpty()) {
                     clRow = (Vector)additionalResults.get(0);
                     if (clRow.get(0) != null) {
                        lastUpdated = clRow.get(0).toString();
                     }

                     if (clRow.get(1) != null) {
                        claimDate = clRow.get(1).toString();
                     }
                  }

                  LogManager.log("Results for History Displayed is:" + claimResults);
                  LogManager.log("Claim Amt " + claimAmt + "Amount is:" + amount);
                  availAmt = Double.parseDouble(amount) - Double.parseDouble(claimAmt);
                  helmetHistory.append("<tr><td align=\"center\" class=\"style1\" >" + lastUpdated + "</td>" + "<td align=\"center\" class=\"style1\" >" + amount + "</td>" + "<td align=\"center\" class=\"style1\" >" + expDate + "</td>" + "<td align=\"center\" class=\"style1\" >" + availAmt + "</td>" + "<td align=\"center\" class=\"style1\" >" + (Double.parseDouble(amount) - availAmt) + "</td>" + "<td align=\"center\" class=\"style1\" >" + claimDate + "</td>" + "</tr>");
               }

               helmetHistory.append("</table>");
            }

            session.setAttribute("FWHelmetHistory", helmetHistory.toString());
         }
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var35) {
               LogManager.log("Unable to Close connection.", (Throwable)var35);
            }

            conn = null;
         }

      }

      return helmetAudit.toString();
   }

   public String buildFlexTimeHours(String sessionId) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.buildFlexTimeHours()");
      Session session = SessionManager.findSession(sessionId);
      SimpleDateFormat parser = new SimpleDateFormat("MM/dd/yy");
      DecimalFormat decimal = new DecimalFormat("#,##0.00");
      Connection conn = null;
      StringBuffer sb = null;

      try {
         DataLogic dataLogic = this.module.getDataLogic();
         SqlStatement sql = null;
         Vector results = null;
         conn = dataLogic.openConnection();
         long weeks = 1L;
         sb = new StringBuffer();
         sql = dataLogic.buildSQLString(sessionId, "SelectPresentWeek");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var63) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var63);
            throw var63;
         }

         dataLogic.commitTransaction(conn);
         Vector week = (Vector)results.get(0);
         String weekNo = week.get(0).toString();
         sql = dataLogic.buildSQLString(sessionId, "SelectFlexTimeHours");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var62) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var62);
            throw var62;
         }

         dataLogic.commitTransaction(conn);
         Vector flexTime = (Vector)results.get(0);
         String flexTimeHours = flexTime.get(0).toString();
         sql = dataLogic.buildSQLString(sessionId, "SelectEmploeeFlextime");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var61) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var61);
            throw var61;
         }

         dataLogic.commitTransaction(conn);
         String allocatedHours = "0";
         if (!results.isEmpty()) {
            Vector empFlexTime = (Vector)results.get(0);
            allocatedHours = empFlexTime.get(0).toString();
         }

         Date dt = new Date();
         int a;
         if (session.getAttribute("FWYearSelected") == null) {
            a = Integer.parseInt(String.valueOf(dt.getYear())) + 1900;
            session.setAttribute("FWYearSelected", String.valueOf(a));
         } else {
            a = Integer.parseInt(session.getAttribute("FWYearSelected").toString());
            --a;
            session.setAttribute("FWYearSelected", String.valueOf(a));
         }

         new Vector();
         sql = dataLogic.buildSQLString(sessionId, "SelectEmploeeFlextime");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
            LogManager.log("comming to flex time:" + results);
         } catch (EuropaException var60) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var60);
            throw var60;
         }

         dataLogic.commitTransaction(conn);
         String oldAllocatedHours = "0";
         String oldusedHours = "0";
         if (!results.isEmpty()) {
            LogManager.log("comming to flex time:" + results);
            Vector oldEmpFlexTime = (Vector)results.get(0);
            oldAllocatedHours = oldEmpFlexTime.get(0).toString();
            oldusedHours = oldEmpFlexTime.get(1).toString();
         }

         String flexTimecarryoveHours = null;
         LogManager.log("YEar For Fix Carry over" + (String)session.getAttribute("FWYearSelected"));
         if (session.getAttribute("FWYearSelected").equals("2007")) {
            sql = dataLogic.buildSQLString(sessionId, "SelectFlextTimeCarryOverHours");
            dataLogic.startTransaction(conn);

            try {
               results = dataLogic.execute(conn, sql);
            } catch (EuropaException var59) {
               dataLogic.rollbackTransaction(conn);
               LogManager.log("Exception e is:" + var59);
               throw var59;
            }

            dataLogic.commitTransaction(conn);
            if (!results.isEmpty()) {
               Vector flexTimecarryover = (Vector)results.get(0);
               flexTimecarryoveHours = flexTimecarryover.get(0).toString();
            }
         }


         if (session.getAttribute("FWYearSelected") == null) {
            a = Integer.parseInt(String.valueOf(dt.getYear())) + 1900;
            session.setAttribute("FWYearSelected", String.valueOf(a));
         } else {
            a = Integer.parseInt(session.getAttribute("FWYearSelected").toString());
            ++a;
            session.setAttribute("FWYearSelected", String.valueOf(a));
         }

         sql = dataLogic.buildSQLString(sessionId, "SelectPresentAllocation");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
            LogManager.log("comming to flex time:" + results);
         } catch (EuropaException var58) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var58);
            throw var58;
         }

         dataLogic.commitTransaction(conn);
         String presentAllocation = "0";
         String presentAdditionalEarned = "0";
         if (!results.isEmpty()) {
            Vector presentEmpFlexTime = (Vector)results.get(0);
            presentAllocation = presentEmpFlexTime.get(0).toString();
            presentAdditionalEarned = presentEmpFlexTime.get(1).toString();
         }

         double allocated = Double.parseDouble(allocatedHours);
         double oldAllocated = Double.parseDouble(oldAllocatedHours);
         double oldUsed = Double.parseDouble(oldusedHours);
         double timeEntryFlextime = Double.parseDouble(flexTimeHours);
         double presentWeekNo = Double.parseDouble(weekNo);
         if (!session.getAttribute("FWCurrentPage").equals("5")) {
            presentWeekNo = 52.0;
         }

         String hireDate = (String)session.getAttribute("FWHireDate");
         LogManager.log("Hire Date is........" + hireDate);

         try {
            Date hire_Date = null;
            if (hireDate != null && !hireDate.equals("")) {
               hire_Date = parser.parse(hireDate);
            }

            Date year_Start_Date = parser.parse("01/01/08");
            if (hire_Date.after(year_Start_Date)) {
               Calendar cl = new GregorianCalendar(hire_Date.getYear() - 100 + 2000, hire_Date.getMonth(), hire_Date.getDate());
               Date currDate = new Date();
               Calendar currentDate = new GregorianCalendar(currDate.getYear() - 100 + 2000, currDate.getMonth(), currDate.getDate());
               Calendar c2 = new GregorianCalendar(hire_Date.getYear() - 100 + 2000, 11, 31);
               long daysRemaining = (c2.getTimeInMillis() - cl.getTimeInMillis()) / 86400000L;
               long daysRemaining_week = (currentDate.getTimeInMillis() - cl.getTimeInMillis()) / 86400000L;
               weeks = daysRemaining / 7L;
               presentWeekNo = (double)(daysRemaining_week / 7L);
               LogManager.log("comming to else______weeks" + weeks + "present week no _________" + presentWeekNo + "daysRemaining_week" + daysRemaining_week);
            } else {
               LogManager.log("comming to if__________________ ");
               weeks = 52L;
            }
         } catch (Exception var57) {
            LogManager.log("Exception raised.............", (Throwable)var57);
         }

         session.setAttribute("FWFlexTimeHrs", String.valueOf(timeEntryFlextime));
         double accruedHours = Double.parseDouble(presentAllocation) / (double)weeks * presentWeekNo;
         LogManager.log("THis is hours comming:" + oldAllocated + "old Used:" + oldUsed + "AccruedHours" + accruedHours);
         double carryOver = 0.0;
         if (session.getAttribute("FWYearSelected").equals("2008")) {
            LogManager.log("YEar For Fix Carry over" + (String)session.getAttribute("FWYearSelected"));
            if (flexTimecarryoveHours != null) {
               carryOver = Double.parseDouble(flexTimecarryoveHours);
            }
         } else {
            carryOver = oldAllocated - oldUsed;
         }

         LogManager.log("usedFlexHours" + timeEntryFlextime);
         LogManager.log("allocated Hours:" + allocated);
         LogManager.log("carryOver:::::::::::" + carryOver + "old" + oldAllocated + "--" + oldUsed);
         double remainingCarryOverHrs = 0.0;
         if (carryOver == 0.0) {
            remainingCarryOverHrs = 0.0;
         } else if (carryOver > timeEntryFlextime) {
            remainingCarryOverHrs = carryOver - timeEntryFlextime;
         } else {
            LogManager.log("Remaining Carray Over Hours:" + remainingCarryOverHrs);
            remainingCarryOverHrs = carryOver - timeEntryFlextime;
            if (remainingCarryOverHrs <= 0.0) {
               remainingCarryOverHrs = 0.0;
            }
         }

         double balance = carryOver + Double.parseDouble(presentAdditionalEarned) + accruedHours - Double.parseDouble(flexTimeHours);
         session.setAttribute("FWFlexTimeBlockStart", "");
         session.setAttribute("FWFlexTimeBlockEnd", "");
         if (session.getAttribute("FWCurrentPage").equals("5") && (SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") || this.hrmEditUsers.contains(session.getUser().toString())) || this.hrmDataEntryUsers.contains(session.getUser().toString()) && !session.getAttribute("FWEmail").toString().toString().equals(session.getUser().toString())) {
            sb.append("<table width= \"100%\" style=\"border-collapse: collapse\" border=\"1\" bgcolor=\"#C4C8FB\" bordercolor=\"#FFFFFF\" cellpadding=\"4\"><tr  bgcolor = #606BFD ><td width=\"105\" align=\"center\" ><b><font style=\"font-size: 9pt\">Allocated Hours</font></b></td><td width=\"105\" align=\"center\" ><b><font style=\"font-size: 9pt\">Carry Over Hours</font></b></td><td width=\"105\" align=\"center\" ><b><font style=\"font-size: 9pt\">Additional Earned</font></b></td><td width=\"105\" align=\"center\" ><b><font style=\"font-size: 9pt\">Accrued Hours</font></b></td><td width=\"105\" align=\"center\" ><b><font style=\"font-size: 9pt\">Used Hours</font></b></td><td width=\"105\" align=\"center\" ><b><font style=\"font-size: 9pt\">Balance</font></b></td></tr>");
            sb.append("<tr  ><td width=\"105\" align=\"center\">");
            sb.append("<font size=\"2\"><input type=\"text\" name=\"FWAllocAmount\" value=\"" + presentAllocation + "\" size=\"12\" class=\"style1\"></font></td>" + "<td width=\"105\" align=\"center\">" + "<input type=\"text\" name=\"FWCarryOverHours\" readonly value=\"" + carryOver + "\" size=\"12\" class=\"style1\"></td>" + "<td width=\"105\" align=\"center\">" + "<font size=\"2\">" + "<input type=\"text\" name=\"FWAdditionalEarned\" value=\"" + presentAdditionalEarned + "\" size=\"12\" class=\"style1\"></font></td>" + "<td width=\"105\" align=\"center\">" + "<input type=\"text\" name=\"FWAccruedHours\" readonly value=\"" + decimal.format(accruedHours) + "\" size=\"12\" class=\"style1\"></td>" + "<td width=\"105\" align=\"center\">" + "<input type=\"text\" name=\"FWUsedHours\" readonly value=\"" + decimal.format(timeEntryFlextime) + "\" size=\"12\" class=\"style1\"></td>" + "<td width=\"105\" align=\"center\">" + "<input type=\"text\" name=\"FWBalanceHours\" readonly value=\"" + decimal.format(balance) + "\" size=\"12\" class=\"style1\"></td>" + "</tr>" + "</table>");
         } else if (!this.hrm_ViewAccess.contains(session.getUser().toString()) && !this.hrmDataEntryUsers.contains(session.getUser().toString()) && !SecurityManager.isMember("People Manager", session.getUser()) && !SecurityManager.isMember("TopLineManager", session.getUser())) {
            session.setAttribute("FWFlexTimeBlockStart", "<!--");
            session.setAttribute("FWFlexTimeBlockEnd", "-->");
         } else if (allocated == 0.0 && timeEntryFlextime == 0.0 && accruedHours == 0.0 && carryOver == 0.0 && remainingCarryOverHrs == 0.0) {
            sb.append("<table border=\"1\" width=\"100%\" cellpadding=\"5\" bgcolor=\"#C4C8FB\" bordercolor=\"#FFFFFF\" style=\"border-collapse: collapse\">").append("<tr><td align=\"center\" colspan=\"2\"><p align=\"left\"><font face=\"Tahoma\"><b><span style=\"font-size: 9pt\">No Flex time Archive Information Available</span></b></font></td>").append("</table>");
         } else {
            sb.append("<table width= \"100%\" style=\"border-collapse: collapse\" border=\"1\" bgcolor=\"#C4C8FB\" bordercolor=\"#FFFFFF\" cellpadding=\"4\"><tr bgcolor = #606BFD>");
            if (this.hrm_ViewAccess.contains(session.getUser().toString()) || this.hrmDataEntryUsers.contains(session.getUser().toString()) || SecurityManager.isMember("People Manager", session.getUser()) || SecurityManager.isMember("TopLineManager", session.getUser())) {
               sb.append("<td width=\"105\" align=\"center\" bgcolor=\"#606BFD\"><b><font style=\"font-size: 9pt\">Allocated Hours</font></b></td>");
            }

            sb.append("<td width=\"105\" align=\"center\" bgcolor=\"#606BFD\"><b><font style=\"font-size: 9pt\">Carry Over Hours</font></b></td><td width=\"105\" align=\"center\" bgcolor=\"#606BFD\"><b><font style=\"font-size: 9pt\">Additional Earned</font></b></td><td width=\"105\" align=\"center\" bgcolor=\"#606BFD\"><b><font style=\"font-size: 9pt\">Accrued Hours</font></b></td><td width=\"105\" align=\"center\" bgcolor=\"#606BFD\"><b><font style=\"font-size: 9pt\">Used Hours</font></b></td><td width=\"105\" align=\"center\" bgcolor=\"#606BFD\"><b><font style=\"font-size: 9pt\">Balance</font></b></td></tr>");
            sb.append("<tr>");
            if (this.hrm_ViewAccess.contains(session.getUser().toString()) || this.hrmDataEntryUsers.contains(session.getUser().toString()) || SecurityManager.isMember("People Manager", session.getUser()) || SecurityManager.isMember("TopLineManager", session.getUser())) {
               sb.append("<td width=\"105\" align=\"center\" style = \"1\" ><font size=\"2\">" + presentAllocation + "</font></td>");
            }

            sb.append("<td width=\"105\" align=\"center\" style = \"1\" ><font size=\"2\">" + carryOver + "</font></td>" + "<td width=\"105\" align=\"center\" style = \"1\" >" + "<font size=\"2\">" + presentAdditionalEarned + "</font></td>" + "<td width=\"105\" align=\"center\" style = \"1\"><font size=\"2\">" + decimal.format(accruedHours) + "</font></td>" + "<td width=\"105\" align=\"center\" style = \"1\" ><font size=\"2\">" + decimal.format(timeEntryFlextime) + "</font></td>" + "<td width=\"105\" align=\"center\" style = \"1\"><font size=\"2\">" + decimal.format(balance) + "</font></td>" + "</tr>" + "</table>");
         }
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var56) {
               LogManager.log("Unable to Close connection.", (Throwable)var56);
            }

            conn = null;
         }

      }

      return sb.toString();
   }

   public String buildBankTime(String sessionId) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.buildBankTimeDetails()");
      Session session = SessionManager.findSession(sessionId);
      DecimalFormat decimal = new DecimalFormat("#,##0.00");
      Connection conn = null;
      StringBuffer sb = null;

      try {
         DataLogic dataLogic = this.module.getDataLogic();
         conn = dataLogic.openConnection();
         Calendar c = Calendar.getInstance();
         int month = c.get(2) + 1;
         sb = new StringBuffer();
         sb.append("<table width=\"100%\">");
         int count = 0;
         if (month <= 3) {
            count = 1;
         } else if (month <= 6) {
            count = 2;
         } else if (month <= 9) {
            count = 3;
         } else if (month <= 12) {
            count = 4;
         }

         int yearList = 1;

         try {
            if (session.getAttribute("FWArch").equals("8")) {
               yearList = Integer.parseInt(session.getAttribute("FWYearLength").toString());
            } else {
               yearList = 1;
            }
         } catch (Exception var48) {
            LogManager.log("exception: ", (Throwable)var48);
         }

         String year = "";

         try {
            if (session.getAttribute("FWArch").equals("8")) {
               count = 4;
               year = String.valueOf(Integer.parseInt(session.getAttribute("FWYearSelected").toString()));
            } else {
               year = String.valueOf(c.get(1));
            }
         } catch (Exception var47) {
            year = String.valueOf(c.get(1));
            LogManager.log("exception: ", (Throwable)var47);
         }

         month = 1;
         if (yearList == 1) {
            String date1 = "";
            String date2 = "";
            double lastcumm = 0.0;

            for(int i = 1; i <= count; ++i) {
               if (month <= 3) {
                  date1 = year + "-01-01";
                  date2 = year + "-03-31";
               } else if (month <= 6) {
                  date1 = year + "-04-01";
                  date2 = year + "-06-30";
               } else if (month <= 9) {
                  date1 = year + "-07-01";
                  date2 = year + "-09-30";
               } else if (month <= 12) {
                  date1 = year + "-10-01";
                  date2 = year + "-12-31";
               }

               double additionalHours1 = 0.0;
               double usedHours1 = 0.0;
               double openingBalance = 0.0;
               String openBalance = "0";
               openBalance = this.getOpeningBalance(sessionId, conn, date1, month);
               StringTokenizer st1 = new StringTokenizer(openBalance, ",");
               additionalHours1 = Double.parseDouble(st1.nextToken().toString());
               usedHours1 = Double.parseDouble(st1.nextToken().toString());
               openingBalance = additionalHours1 - usedHours1;
               LogManager.log("quarter " + i + "opening Bal" + openBalance);
               LogManager.log("quarter " + i + "opening Bal" + additionalHours1);
               LogManager.log("First week of Jan " + openingBalance);
               String cummulative = this.getOpeningBalance(sessionId, conn, date2, month);
               LogManager.log("quarter " + i + "cumm Bal" + cummulative);
               StringTokenizer st = new StringTokenizer(cummulative, ",");
               double additionalHours = Double.parseDouble(st.nextToken().toString());
               double usedHours = Double.parseDouble(st.nextToken().toString());
               double actualUsedHours = usedHours - usedHours1;
               LogManager.log("LastCumm is :...." + lastcumm + "ActualHours" + actualUsedHours);
               double cummulativeBalance = additionalHours - usedHours;
               LogManager.log("March Quarter " + cummulativeBalance + "......additionalHours..." + additionalHours + ".....usedHours..." + usedHours);
               double totOb = 0.0;
               session.getAttribute("FWCurrentPage").equals("5");
               LogManager.log("Last Cummulative is" + lastcumm);
               LogManager.log("Opening Bal" + openingBalance);
               LogManager.log("Cummulative" + cummulativeBalance);
               LogManager.log("Actual Hours" + actualUsedHours);
               double openingBalanceDisplay = cummulativeBalance + actualUsedHours - openingBalance;
               LogManager.log("Additonal Hours " + openingBalanceDisplay);
               if ((!session.getAttribute("FWCurrentPage").equals("5") || !SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") && !this.hrmEditUsers.contains(session.getUser().toString())) && !this.hrmDataEntryUsers.contains(session.getUser().toString())) {
                  LogManager.log("Additonal Hours " + openingBalanceDisplay);
                  sb.append("<table width=\"100%\"><tr><td colspan=\"2\" align=\"left\"><b><font size=\"2\"> Quarter: " + i + "</font></b></td>" + "<td colspan=\"2\" align=\"center\">" + "<b><font size=\"2\">Opening Balance</font></b></td>" + "<td colspan=\"3\" align=\"left\"><font size=\"2\">" + openingBalance + "</font></td>" + "</tr>" + "<tr>" + "<td align=\"center\">" + "<font size=\"2\">Cumulative Balance</font></td>" + "<td align=\"center\">" + "<font face=\"Tahoma\"><span style=\"font-size: 9pt\">" + decimal.format(cummulativeBalance) + "</span></font></td>" + "<td align=\"center\">" + "<font size=\"2\">Additional Hours</font></td>" + "<td align=\"center\">" + "<font face=\"Tahoma\"><span style=\"font-size: 9pt\">" + decimal.format(openingBalanceDisplay) + "</span></font></td>" + "<td align=\"center\">" + "<font size=\"2\">Used Hours</font></td>" + "<td align=\"center\">" + "<font face=\"Tahoma\"><span style=\"font-size: 9pt\">" + decimal.format(actualUsedHours) + "</span></font></td>" + "</tr>");
               } else {
                  LogManager.log("Additonal Hours " + openingBalanceDisplay);
                  sb.append("<table width=\"100%\"><tr><td colspan=\"2\" align=\"left\"><b><font size=\"2\"> Quarter: " + i + "</font></b></td>" + "<td colspan=\"2\" align=\"center\">" + "<b><font size=\"2\">Opening Balance</font></b></td>" + "<td colspan=\"3\" align=\"left\"><font size=\"2\">" + openingBalance + "</font></td>" + "</tr>" + "<tr>" + "<td align=\"center\">" + "<font size=\"2\">Cumulative Balance</font></td>" + "<td align=\"center\">" + "<font face=\"Tahoma\"><span style=\"font-size: 9pt\">" + "<input type=\"text\" name=\"FWQ" + i + "Cb\" readonly size=\"15\" readonly value= \"" + decimal.format(cummulativeBalance) + "\"class=\"style1\"></span></font></td>" + "<td align=\"center\">" + "<font size=\"2\">Additional Hours</font></td>" + "<td align=\"center\">" + "<font face=\"Tahoma\"><span style=\"font-size: 9pt\">" + "<input type=\"text\" name=\"FWQ" + i + "Ah\" size=\"15\"  value= \"" + decimal.format(openingBalanceDisplay) + "\"class=\"style1\"></span></font></td>" + "<td align=\"center\">" + "<font size=\"2\">Used Hours</font></td>" + "<td align=\"center\">" + "<font face=\"Tahoma\"><span style=\"font-size: 9pt\">" + "<input type=\"text\" name=\"FWQ" + i + "Uh\" readonly size=\"15\"  readonly value= \"" + decimal.format(actualUsedHours) + "\"class=\"style1\"></span></font></td>" + "</tr>");
               }

               month += 3;
            }

            sb.append("</table>");
         } else {
            sb.append("<table border=\"1\" width=\"100%\" cellpadding=\"5\" bordercolorlight=\"#808080\" style=\"border-collapse: collapse\">").append("<tr><td align=\"center\" colspan=\"2\"><p align=\"left\"><font face=\"Tahoma\"><b><span style=\"font-size: 9pt\">No Bank time Archive Information Available</span></b></font></td>").append("</table>");
         }
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var46) {
               LogManager.log("Unable to Close connection.", (Throwable)var46);
            }

            conn = null;
         }

      }

      return sb.toString();
   }

   public String getOpeningBalance(String sessionId, Connection conn, String date2, int smonth) throws EuropaException {
      LogManager.log("AdminBLogic.getOpeningBalance");
      Session session = SessionManager.findSession(sessionId);
      SqlStatement sql = null;
      DataLogic dataLogic = this.module.getDataLogic();
      session.setAttribute("FWDate2", date2);
      double openbal = 0.0;
      sql = dataLogic.buildSQLString(sessionId, "SelectPreviousYearBankBalance");
      dataLogic.startTransaction(conn);
      Vector previousYearBankBalance = null;

      try {
         previousYearBankBalance = dataLogic.execute(conn, sql);
      } catch (EuropaException var21) {
         dataLogic.rollbackTransaction(conn);
         LogManager.log("Exception e is:" + var21);
         throw var21;
      }

      dataLogic.commitTransaction(conn);
      LogManager.log("Such element exists" + previousYearBankBalance);

      Vector additionalHours;
      try {
         if (previousYearBankBalance != null && !previousYearBankBalance.isEmpty()) {
            additionalHours = (Vector)previousYearBankBalance.get(0);
            LogManager.log("row is here" + additionalHours.get(0));
            openbal = ((BigDecimal)additionalHours.get(0)).doubleValue();
            LogManager.log("opening Balance" + openbal);
         }
      } catch (NoSuchElementException var20) {
         LogManager.log("row is here" + var20);
      }

      sql = dataLogic.buildSQLString(sessionId, "selectAdditionalHours");
      dataLogic.startTransaction(conn);
      additionalHours = null;

      try {
         additionalHours = dataLogic.execute(conn, sql);
      } catch (EuropaException var19) {
         dataLogic.rollbackTransaction(conn);
         LogManager.log("Exception e is:" + var19);
         throw var19;
      }

      dataLogic.commitTransaction(conn);
      double addHours = 0.0;
      LogManager.log("results....." + additionalHours);
      Vector usedBanktime;
      if (additionalHours != null && !additionalHours.isEmpty()) {
         usedBanktime = (Vector)additionalHours.get(0);
         addHours = Double.parseDouble(usedBanktime.get(0).toString());
      }

      LogManager.log("GetOpening Balance" + addHours);
      sql = dataLogic.buildSQLString(sessionId, "SelectBankTimeHours");
      dataLogic.startTransaction(conn);
      usedBanktime = null;

      try {
         usedBanktime = dataLogic.execute(conn, sql);
      } catch (EuropaException var18) {
         dataLogic.rollbackTransaction(conn);
         LogManager.log("Exception e is:" + var18);
         throw var18;
      }

      dataLogic.commitTransaction(conn);
      double usedHours = 0.0;
      if (usedBanktime != null && !usedBanktime.isEmpty()) {
         Vector v1 = (Vector)usedBanktime.get(0);
         usedHours = Double.parseDouble(v1.get(0).toString());
      }

      LogManager.log("results....999" + usedBanktime);
      return String.valueOf(addHours + openbal) + "," + usedHours;
   }

   public String buildHelmetHeadder(String headderType, String sessionId) throws EuropaException {
      Session session = SessionManager.findSession(sessionId);
      StringBuffer hBuffer = new StringBuffer();
      hBuffer.append("<table width=\"100%\" cellpadding=\"4\" border=\"1\" bgcolor=\"#FFFFFF\" style=\"border-collapse: collapse\" ><tr bgcolor=\"#606BFD\">");
      if (headderType.equals("present") && (SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") || this.hrmEditUsers.contains(session.getUser().toString()) || this.hrmDataEntryUsers.contains(session.getUser().toString()) || this.hrmHelmetAwardEditUsers.contains(session.getUser().toString())) && !session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString())) {
         hBuffer.append("<td width=\"105\" align=\"center\" ><b><font size=\"2\">Edit</font></b></td>");
      } else {
         hBuffer.append("<td width=\"105\" align=\"center\" ><b><font size=\"2\">Last Updated</font></b></td>");
      }

      hBuffer.append("<td align=\"center\"><b><font size=\"2\">Amount</font></b></td><td align=\"center\"><b><font size=\"2\">Date of Expiry</font></b></td><td align=\"center\"><b><font size=\"2\">Available Amount</font></b></td>");
      if (headderType.equals("present") && (SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") || this.hrmEditUsers.contains(session.getUser().toString()) || this.hrmDataEntryUsers.contains(session.getUser().toString()) || this.hrmHelmetAwardEditUsers.contains(session.getUser().toString())) && !session.getUser().getEmployeeId().toString().equals(session.getAttribute("FWEmployeeID").toString())) {
         hBuffer.append("<td align=\"center\"><b><font size=\"2\">Claim Amount</font></b></td><td align=\"center\"><b><font size=\"2\">Claim Date</font></b></td>");
         if (!headderType.equals("history") && (session.getAttribute("FWCurrentPage").equals("5") || session.getAttribute("FWCurrentPage").equals("6"))) {
            hBuffer.append("<td align=\"center\"><b><font size=\"2\">Delete</font></b></td>");
         }
      } else {
         hBuffer.append("<td align=\"center\"><b><font size=\"2\">Claim Amount</font></b></td><td align=\"center\"><b><font size=\"2\">Claim Date</font></b></td>");
      }

      hBuffer.append("</tr>");
      return hBuffer.toString();
   }

   private String buildTuitionAndReimbursementDetails(String sessionId) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.buildTuitionAndReimbursementDetails()");
      Session session = SessionManager.findSession(sessionId);
      SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy");
      SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
      DecimalFormat decimal = new DecimalFormat("#,###0.00");
      Connection conn = null;
      StringBuffer tutionAudit = null;

      try {
         DataLogic dataLogic = this.module.getDataLogic();
         SqlStatement sql = null;
         Vector results = null;
         conn = dataLogic.openConnection();
         sql = dataLogic.buildSQLString(sessionId, "SelectTutionAndReimbursement");
         LogManager.log("Comming to there" + sql);
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var28) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var28);
            throw var28;
         }

         dataLogic.commitTransaction(conn);
         tutionAudit = new StringBuffer();
         if (!results.isEmpty()) {
            tutionAudit.append("&nbsp;<div align=\"left\">");
            tutionAudit.append("<table border=\"1\" width=\"100%\" cellpadding=\"5\" bordercolorlight=\"#FFFFF\" style=\"border-collapse: collapse\">").append("<tr><td align=\"center\" colspan=\"4\" bgcolor=\"#C4C8FB\"><p align=\"left\"><font face=\"Tahoma\"><b><span style=\"font-size: 9pt\">Tuition / Scholarship Reimbursement</span></b></font></td><tr><td width=\"25%\" align=\"right\" bgcolor=\"#606BFD\"><p align=\"center\"><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Amount Used</b></font></td><td width=\"25%\" align=\"center\" bgcolor=\"#606BFD\"><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Date</b></font></td><td width=\"25%\" align=\"center\" bgcolor=\"#606BFD\"><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Purpose</b></font></td><td width=\"25%\" align=\"center\" bgcolor=\"#606BFD\"><font face=\"Tahoma\" style=\"font-size: 9pt\"><b></b></font></td></tr>");

            for(int a = 0; a < results.size(); ++a) {
               Vector auditResults = (Vector)results.get(a);
               String sno = auditResults.get(0).toString();
               String amount = auditResults.get(1).toString();
               String date = auditResults.get(2).toString();
               String purpose = auditResults.get(3).toString();

               try {
                  try {
                     tutionAudit.append("<tr><td width=\"23%\" align=\"right\" bgcolor=\"#C4C8FB\"><font face=\"Tahoma\" style=\"font-size: 9pt\">");
                     tutionAudit.append(decimal.format(Double.parseDouble(amount)));
                  } catch (Exception var27) {
                     tutionAudit.append(amount);
                     LogManager.log("exception: ", (Throwable)var27);
                  }

                  tutionAudit.append("</font></td><td width=\"24%\" align=\"center\" bgcolor=\"#C4C8FB\"><font face=\"Tahoma\" style=\"font-size: 9pt\">").append(String.valueOf(formatter.format(parser.parse(date)))).append("</font></td><td width=\"23%\" align=\"right\" bgcolor=\"#C4C8FB\"><font face=\"Tahoma\" style=\"font-size: 9pt\">" + purpose + "</font></td>");
                  if (!SecurityManager.getUser(session.getUser().toString()).toString().equals("admin") && !this.hrmEditUsers.contains(session.getUser().toString()) && !this.hrmDataEntryUsers.contains(session.getUser().toString())) {
                     tutionAudit.append("</tr>");
                  } else if (session.getAttribute("FWCurrentPage").equals("5")) {
                     tutionAudit.append("<td align=\"center\" bgcolor=\"#C4C8FB\"><a href=\"/servlets/ProxyServlet?FWModule=admin_employee&FWDelSno_Tuition=" + sno + "\" onclick = \"return (confirm('Are you sure?'))\" ><img src = \"/assets/images/recyclebin_hrm.gif\" border =0 ></img></a> </td>" + "</tr></tr>");
                  } else {
                     tutionAudit.append("</tr>");
                  }
               } catch (Exception var29) {
                  LogManager.log("exception: ", (Throwable)var29);
               }
            }

            tutionAudit.append("</table>");
         } else {
            tutionAudit.append("<table border=\"1\" width=\"100%\" cellpadding=\"5\" bordercolorlight=\"#808080\" style=\"border-collapse: collapse\">").append("<tr><td align=\"center\" colspan=\"2\"><p align=\"left\"><font face=\"Tahoma\"><b><span style=\"font-size: 9pt\">No Tuition / Scholarship Reimbursement History Information Available</span></b></font></td>").append("</table>");
         }
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var26) {
               LogManager.log("Unable to Close connection.", (Throwable)var26);
            }

            conn = null;
         }

      }

      return tutionAudit.toString();
   }

   private void flexTimeTransaction(String sessionId, Connection conn) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.FlexTimeTransaction()");
      DataLogic dataLogic = this.module.getDataLogic();
      SqlStatement sql = null;
      Vector results = null;
      sql = dataLogic.buildSQLString(sessionId, "SelectEntryFromFlexTime");
      dataLogic.startTransaction(conn);

      try {
         results = dataLogic.execute(conn, sql);
      } catch (EuropaException var11) {
         dataLogic.rollbackTransaction(conn);
         throw var11;
      }

      dataLogic.commitTransaction(conn);
      Vector entry = (Vector)results.get(0);
      int count = Integer.parseInt(entry.get(0).toString());
      String queryName = "";
      if (count <= 0) {
         queryName = "InsertFlexTime";
      } else {
         queryName = "UpdateFlexTime";
      }

      sql = dataLogic.buildSQLString(sessionId, queryName);
      dataLogic.startTransaction(conn);

      try {
         dataLogic.execute(conn, sql);
      } catch (EuropaException var10) {
         dataLogic.rollbackTransaction(conn);
         throw var10;
      }

      dataLogic.commitTransaction(conn);
   }

   private void bankTimeTransaction(String sessionId, Connection conn) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.bankTimeTransaction()");
      Session session = SessionManager.findSession(sessionId);
      DataLogic dataLogic = this.module.getDataLogic();
      SqlStatement sql = null;
      Vector results = null;
      Calendar c = Calendar.getInstance();
      int month = c.get(2) + 1;
      String quarterNumber = "";
      if (month <= 3) {
         quarterNumber = "1";
      } else if (month <= 6) {
         quarterNumber = "2";
      } else if (month <= 9) {
         quarterNumber = "3";
      } else if (month <= 12) {
         quarterNumber = "4";
      }

      session.setAttribute("FWQuarter", quarterNumber);
      sql = dataLogic.buildSQLString(sessionId, "SelectEntryFromBankTime");
      dataLogic.startTransaction(conn);

      try {
         results = dataLogic.execute(conn, sql);
      } catch (EuropaException var19) {
         dataLogic.rollbackTransaction(conn);
         throw var19;
      }

      dataLogic.commitTransaction(conn);
      LogManager.log("Comming here:-------" + results);
      Vector entry = (Vector)results.get(0);
      int count = Integer.parseInt(entry.get(0).toString());
      LogManager.log("Comming here:-------" + count);
      String queryName = "";
      if (count <= 0) {
         queryName = "InsertBankTime";
      } else {
         queryName = "UpdateBankTime";
      }

      LogManager.log("--------------:::------" + session.getAttribute("FWQ" + quarterNumber + "Ah").toString());
      session.setAttribute("FWAllocatedHours", session.getAttribute("FWQ" + quarterNumber + "Ah").toString());
      double addtionalVal = 0.0;

      try {
         addtionalVal = Double.parseDouble(session.getAttribute("FWQ" + quarterNumber + "Ah").toString());
      } catch (Exception var18) {
         LogManager.log("exception: ", (Throwable)var18);
      }

      if (queryName.equals("InsertBankTime")) {
         if (addtionalVal != 0.0) {
            sql = dataLogic.buildSQLString(sessionId, queryName);
            dataLogic.startTransaction(conn);

            try {
               dataLogic.execute(conn, sql);
            } catch (EuropaException var17) {
               dataLogic.rollbackTransaction(conn);
               throw var17;
            }
         }
      } else {
         sql = dataLogic.buildSQLString(sessionId, queryName);
         dataLogic.startTransaction(conn);

         try {
            dataLogic.execute(conn, sql);
         } catch (EuropaException var16) {
            dataLogic.rollbackTransaction(conn);
            throw var16;
         }
      }

   }

   private void helmetAwardsTransaction(String sessionId, Connection conn) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.HelmetAwardsTransaction()");
      Session session = SessionManager.findSession(sessionId);
      PreparedSqlStatement psActivityCodeData = new PreparedSqlStatement();
      PreparedStatement pquery1 = null;
      PreparedSqlStatement psInsertClaimAudit = new PreparedSqlStatement();
      PreparedStatement pquery2 = null;
      PreparedSqlStatement psInsertAmount = new PreparedSqlStatement();
      PreparedStatement pquery3 = null;

      try {
         DataLogic dataLogic = this.module.getDataLogic();
         SqlStatement sql = null;
         Vector results = null;
         sql = dataLogic.buildSQLString(sessionId, "UpdateHelmetData");
         Vector paramVector = new Vector();
         pquery1 = psActivityCodeData.createPreparedStatement(conn, sql.toString());
         sql = dataLogic.buildSQLString(sessionId, "InsertClaimAudit");
         pquery2 = psInsertClaimAudit.createPreparedStatement(conn, sql.toString());
         sql = dataLogic.buildSQLString(sessionId, "InsertAmountAudit");
         pquery3 = psInsertAmount.createPreparedStatement(conn, sql.toString());
         LogManager.log("XOssfjaflk;adjfdkals;fjdalsfjlasd;fj sec" + this.oldHelmetResults);

         try {
            dataLogic.startTransaction(conn);
         } catch (Exception var33) {
            LogManager.log("Exception e", (Throwable)var33);
         }

         for(int a = 0; a < this.oldHelmetResults.size(); ++a) {
            Vector auditResults = (Vector)this.oldHelmetResults.get(a);
            String sno = auditResults.get(0).toString();
            String amount = auditResults.get(1).toString();
            paramVector.clear();
            paramVector.add(0, session.getAttribute("FWAmt" + String.valueOf(a)).toString());
            paramVector.add(1, session.getAttribute("FWDtExp" + String.valueOf(a)).toString());
            paramVector.add(2, session.getAttribute("FWHelmetSno" + String.valueOf(a)).toString());
            LogManager.log("Paramter Vector: 1 " + paramVector);
            results = psActivityCodeData.executePreparedStatement(pquery1, paramVector);
            LogManager.log("Executed" + results);
            paramVector.clear();
            LogManager.log("Clearing vector1");
            LogManager.log("session1" + session.getAttribute("FWClAmt" + String.valueOf(a)).toString().length());
            if (session.getAttribute("FWClAmt" + String.valueOf(a)).toString().length() != 0) {
               paramVector.add(0, session.getAttribute("FWEmployeeId").toString());
               paramVector.add(1, session.getAttribute("FWClAmt" + String.valueOf(a)).toString());
               paramVector.add(2, session.getAttribute("FWClDate" + String.valueOf(a)).toString());
               paramVector.add(3, sno);
               LogManager.log("Paramter Vector: 2 " + paramVector);
               psInsertClaimAudit.executePreparedStatement(pquery2, paramVector);
            }

            paramVector.clear();
            LogManager.log("Clearing vector2");
            LogManager.log("session2" + session.getAttribute("FWAmt" + String.valueOf(a)).toString().length());
            if (!session.getAttribute("FWAmt" + String.valueOf(a)).toString().equals(amount) && session.getAttribute("FWAmt" + String.valueOf(a)).toString().length() != 0) {
               paramVector.add(0, session.getAttribute("FWEmployeeId").toString());
               paramVector.add(1, amount);
               paramVector.add(2, session.getAttribute("FWDtExp" + String.valueOf(a)).toString());
               paramVector.add(3, sno);
               LogManager.log("Paramter Vector: 3 " + paramVector);
               psInsertAmount.executePreparedStatement(pquery3, paramVector);
            }
         }

         if (session.getAttribute("FWHAmount").toString().length() != 0 && session.getAttribute("FWHDate").toString().length() != 0) {
            sql = dataLogic.buildSQLString(sessionId, "InsertHelmetAwards");
            dataLogic.startTransaction(conn);

            try {
               dataLogic.execute(conn, sql);
            } catch (EuropaException var32) {
               dataLogic.rollbackTransaction(conn);
               LogManager.log("Exception e is:" + var32);
               throw var32;
            }

            dataLogic.commitTransaction(conn);
            session.removeAttribute("FWHAmount");
            session.removeAttribute("FWHDate");
         }

         dataLogic.commitTransaction(conn);
      } finally {
         if (pquery1 != null) {
            try {
               pquery1.close();
            } catch (SQLException var31) {
               LogManager.log("Unable to Close statement.", (Throwable)var31);
            }
         }

         if (pquery2 != null) {
            try {
               pquery2.close();
            } catch (SQLException var30) {
               LogManager.log("Unable to Close statement.", (Throwable)var30);
            }
         }

         if (pquery3 != null) {
            try {
               pquery3.close();
            } catch (SQLException var29) {
               LogManager.log("Unable to Close statement.", (Throwable)var29);
            }
         }

      }

   }

   private String buildIvpAreaDetails(String sessionId, boolean ivpFlag) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.BuildIvpAreaDetails()");
      Session session = SessionManager.findSession(sessionId);
      DataLogic dataLogic = this.module.getDataLogic();
      Connection conn = dataLogic.openConnection();
      SqlStatement sql = null;
      Vector results = null;

      try {
         sql = dataLogic.buildSQLString(sessionId, "SelectIvpArea");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
            dataLogic.commitTransaction(conn);
         } catch (EuropaException var24) {
            dataLogic.rollbackTransaction(conn);
            throw var24;
         }
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var21) {
               LogManager.log("Unable to Close connection.", (Throwable)var21);
            }

            conn = null;
         }

      }

      if (conn != null) {
         try {
            conn.close();
         } catch (SQLException var23) {
            LogManager.log("Unable to Close connection.", (Throwable)var23);
         }

         conn = null;
      }

      String empTraining = "";
      String empBounsEmails = "";
      String empGoals = "";
      LogManager.log("abcd results:" + results);
      LogManager.log("abcd results size :" + results.size());
      Vector ivpAreaArchive = null;
      if (!results.isEmpty()) {
         Vector ivpArea = null;
         if (!ivpFlag) {
            ivpArea = (Vector)results.get(results.size() - 1);
         } else if (results.size() > 1) {
            Date archDate = new Date();
            String var14 = archDate.toString().substring(archDate.toString().length() - 4, archDate.toString().length());
         } else {
            empTraining = "";
            empBounsEmails = "";
            empGoals = "";
         }

         if (!ivpFlag) {
            empTraining = (String)ivpArea.get(1) + (String)ivpArea.get(2) + (String)ivpArea.get(3) + (String)ivpArea.get(4) + (String)ivpArea.get(5) + (String)ivpArea.get(6);
            empBounsEmails = (String)ivpArea.get(7) + (String)ivpArea.get(8) + (String)ivpArea.get(9) + (String)ivpArea.get(10) + (String)ivpArea.get(11) + (String)ivpArea.get(12);
            empGoals = (String)ivpArea.get(13) + (String)ivpArea.get(14) + (String)ivpArea.get(15) + (String)ivpArea.get(16) + (String)ivpArea.get(17) + (String)ivpArea.get(18);
         }
      } else {
         empTraining = "";
         empBounsEmails = "";
         empGoals = "";
      }

      LogManager.log("this is results" + empTraining + "----" + empBounsEmails + "-----" + empGoals);
      StringBuffer ivpBuffer = new StringBuffer();
      int arch = 0;

      try {
         if (session.getAttribute("FWArch").equals("6")) {
            arch = 1;
         }
      } catch (Exception var22) {
         LogManager.log("exception: ", (Throwable)var22);
      }

      LogManager.log(" Archive :" + arch);
      int i;
      if (!session.getAttribute("FWCurrentPage").equals("5") && !session.getAttribute("FWCurrentPage").equals("8")) {
         if (!results.isEmpty()) {
            ivpBuffer.append("*The history below displays from top, the latest to the oldest changes for the year chosen");
            ivpBuffer.append("<table border=\"1\" width=\"100%\" cellspacing=\"0\" cellpadding=\"10\" bordercolor=\"#C0C0C0\" style=\"border-collapse: collapse\"><tr bgcolor=\"#606BFD\"><td valign=\"top\" height=\"42\"><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Goals/ Training</b> </font> </td><td width=\"265\" valign=\"top\" height=\"42\"><span style=\"font-size: 9pt\"><b>Key Accomplishments/ Bonus</b></span></td><td width=\"251\" valign=\"top\" height=\"42\"><font face=\"Tahoma\"><span style=\"font-size: 9pt\"><b>Areas to Improve</b></span></font></td></tr>");

            for(i = results.size() - 1; i >= 0; --i) {
               ivpAreaArchive = (Vector)results.get(i);
               empTraining = (String)ivpAreaArchive.get(1) + (String)ivpAreaArchive.get(2) + (String)ivpAreaArchive.get(3) + (String)ivpAreaArchive.get(4) + (String)ivpAreaArchive.get(5) + (String)ivpAreaArchive.get(6);
               empBounsEmails = (String)ivpAreaArchive.get(7) + (String)ivpAreaArchive.get(8) + (String)ivpAreaArchive.get(9) + (String)ivpAreaArchive.get(10) + (String)ivpAreaArchive.get(11) + (String)ivpAreaArchive.get(12);
               empGoals = (String)ivpAreaArchive.get(13) + (String)ivpAreaArchive.get(14) + (String)ivpAreaArchive.get(15) + (String)ivpAreaArchive.get(16) + (String)ivpAreaArchive.get(17) + (String)ivpAreaArchive.get(18);
               ivpBuffer.append("<tr><td valign=\"top\"><font face=\"Tahoma\"><span style=\"font-size: 9pt\">" + empTraining + "</span></font></td>" + "<td width=\"265\" valign=\"top\">" + "<font face=\"Tahoma\"><span style=\"font-size: 9pt\">" + empBounsEmails + "</span></font></td>" + "<td width=\"251\" valign=\"top\">" + "<font face=\"Tahoma\"><span style=\"font-size: 9pt\">" + empGoals + "</span></font></td>" + "</tr>");
            }

            ivpBuffer.append("</table>");
         } else {
            ivpBuffer.append("<table border=\"1\" width=\"100%\" cellpadding=\"5\" bordercolorlight=\"#808080\" style=\"border-collapse: collapse\">").append("<tr><td align=\"center\" colspan=\"2\"><p align=\"left\"><font face=\"Tahoma\"><b><span style=\"font-size: 9pt\">No Ivp History Information Available</span></b></font></td>").append("</table>");
         }
      } else {
         ivpBuffer.append("<table border=\"1\" width=\"100%\" cellspacing=\"0\" cellpadding=\"10\" bordercolor=\"#C0C0C0\" style=\"border-collapse: collapse\">");
         if (!results.isEmpty() || session.getAttribute("FWCurrentPage").equals("5")) {
            if (session.getAttribute("FWCurrentPage").equals("8")) {
               ivpBuffer.append("*The history below displays from top, the latest to the oldest changes for the year chosen");
            }

            ivpBuffer.append("<tr bgcolor=\"#606BFD\" > <td valign=\"top\" height=\"42\"><font face=\"Tahoma\" style=\"font-size: 9pt\"><b>Goals/ Training</b> </font> </td><td width=\"265\" valign=\"top\" height=\"42\"><span style=\"font-size: 9pt\"><b>Key Accomplishments/ Bonus</b></span></td><td width=\"251\" valign=\"top\" height=\"42\"><font face=\"Tahoma\"><span style=\"font-size: 9pt\"><b>Areas to Improve</b></span></font></td></tr>");
         }

         if (arch == 0) {
            ivpBuffer.append("<tr><td valign=\"top\"><font face=\"Tahoma\"><span style=\"font-size: 9pt\"><textarea cols=\"25\" name=\"FWGoals\" onblur = \"calcLength(this)\" rows=\"5\" onblur = \"stripSpaces(this.value);\" wrap=\"vertical\" class= \"style1\">" + empGoals + "</textarea></span></font></td>" + "<td width=\"265\" valign=\"top\">" + "<font face=\"Tahoma\"><span style=\"font-size: 9pt\">" + "<textarea cols=\"25\" name=\"FWBonusEmail\" onblur = \"calcLength(this)\" rows=\"5\" onblur = \"stripSpaces(this.value);\" wrap=\"vertical\" class= \"style1\" >" + empBounsEmails + "</textarea></span></font></td>" + "<td width=\"251\" valign=\"top\">" + "<font face=\"Tahoma\"><span style=\"font-size: 9pt\">" + "<textarea cols=\"25\" name=\"FWTrainingScholarship\" onblur = \"calcLength(this)\" rows=\"5\" onblur = \"stripSpaces(this.value);\" wrap=\"vertical\" class=\"style1\" >" + empTraining + "</textarea></span></font></td>" + "</tr>" + "</table>");
         } else if (!results.isEmpty()) {
            for(i = results.size() - 1; i >= 0; --i) {
               ivpAreaArchive = (Vector)results.get(i);
               empTraining = (String)ivpAreaArchive.get(1) + (String)ivpAreaArchive.get(2) + (String)ivpAreaArchive.get(3) + (String)ivpAreaArchive.get(4) + (String)ivpAreaArchive.get(5) + (String)ivpAreaArchive.get(6);
               empBounsEmails = (String)ivpAreaArchive.get(7) + (String)ivpAreaArchive.get(8) + (String)ivpAreaArchive.get(9) + (String)ivpAreaArchive.get(10) + (String)ivpAreaArchive.get(11) + (String)ivpAreaArchive.get(12);
               empGoals = (String)ivpAreaArchive.get(13) + (String)ivpAreaArchive.get(14) + (String)ivpAreaArchive.get(15) + (String)ivpAreaArchive.get(16) + (String)ivpAreaArchive.get(17) + (String)ivpAreaArchive.get(18);
               ivpBuffer.append("<tr><td valign=\"top\"><font face=\"Tahoma\"><span style=\"font-size: 9pt\">" + empGoals + "</span></font></td>" + "<td width=\"265\" valign=\"top\">" + "<font face=\"Tahoma\"><span style=\"font-size: 9pt\">" + empBounsEmails + "</span></font></td>" + "<td width=\"251\" valign=\"top\">" + "<font face=\"Tahoma\"><span style=\"font-size: 9pt\">" + empTraining + "</span></font></td>" + "</tr>");
            }

            ivpBuffer.append("</table>");
         } else {
            ivpBuffer.append("<table border=\"1\" width=\"100%\" cellpadding=\"5\" bordercolorlight=\"#808080\" style=\"border-collapse: collapse\">").append("<tr><td align=\"center\" colspan=\"2\"><p align=\"left\"><font face=\"Tahoma\"><b><span style=\"font-size: 9pt\">No IVP History Information Available</span></b></font></td>").append("</table>");
         }
      }

      session.setAttribute("FWOldGoals", empGoals);
      session.setAttribute("FWOldBonusEmail", empBounsEmails);
      session.setAttribute("FWOldTrainingScholarship", empTraining);
      return ivpBuffer.toString();
   }

   private void updateIvpArea(String sessionId, Connection conn) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.updateIvpArea()");
      Session session = SessionManager.findSession(sessionId);
      String empOldGoals = (String)session.getAttribute("FWOldGoals");
      String empOldBonusMails = (String)session.getAttribute("FWOldBonusEmail");
      String empOldTraining = (String)session.getAttribute("FWOldTrainingScholarship");
      String empGoals = (String)session.getAttribute("FWGoals");
      String empBonusMails = (String)session.getAttribute("FWBonusEmail");
      String empTraining = (String)session.getAttribute("FWTrainingScholarship");
      DataLogic dataLogic = this.module.getDataLogic();
      SqlStatement sql = null;
      Vector results = null;
      sql = dataLogic.buildSQLString(sessionId, "SelectIvpArea");
      dataLogic.startTransaction(conn);

      try {
         results = dataLogic.execute(conn, sql);
      } catch (EuropaException var59) {
         dataLogic.rollbackTransaction(conn);
         throw var59;
      }

      dataLogic.commitTransaction(conn);
      Connection connClob;
      PreparedStatement pstmtClobInsert;
      String training;
      String bonusEmail;
      String goals;
      CLOB newClob;
      CLOB newClob1;
      CLOB newClob2;
      if (results.isEmpty()) {
         if (!empOldGoals.equals(empGoals) || !empOldBonusMails.equals(empBonusMails) || !empOldTraining.equals(empTraining)) {
            sql = dataLogic.buildSQLString(sessionId, "InsertIvpArea");
            connClob = dataLogic.specificConnection();
            pstmtClobInsert = null;

            try {
               connClob.setAutoCommit(false);
               training = "";
               bonusEmail = "";
               goals = "";
               if (session.getAttribute("FWTrainingScholarship") == null) {
                  training = "";
               } else {
                  training = session.getAttribute("FWTrainingScholarship").toString();
               }

               if (session.getAttribute("FWBonusEmail") == null) {
                  bonusEmail = "";
               } else {
                  bonusEmail = session.getAttribute("FWBonusEmail").toString();
               }

               if (session.getAttribute("FWGoals") == null) {
                  goals = "";
               } else {
                  goals = session.getAttribute("FWGoals").toString();
               }

               pstmtClobInsert = connClob.prepareStatement(sql.toString());
               newClob = CLOB.createTemporary(connClob, false, 10);
               newClob.putString(1L, training);
               pstmtClobInsert.setClob(1, newClob);
               newClob1 = CLOB.createTemporary(connClob, false, 10);
               newClob1.putString(1L, bonusEmail);
               pstmtClobInsert.setClob(2, newClob1);
               newClob2 = CLOB.createTemporary(connClob, false, 10);
               newClob2.putString(1L, goals);
               pstmtClobInsert.setClob(3, newClob2);
               pstmtClobInsert.executeUpdate();
               dataLogic.commitTransaction(connClob);
            } catch (Exception var58) {
               LogManager.log("Exception e:" + var58);
            } finally {
               if (pstmtClobInsert != null) {
                  try {
                     pstmtClobInsert.close();
                  } catch (SQLException var54) {
                     LogManager.log("Unable to Close statement.", (Throwable)var54);
                  }
               }

               if (connClob != null) {
                  try {
                     connClob.close();
                  } catch (SQLException var53) {
                     LogManager.log("Unable to Close connection.", (Throwable)var53);
                  }

                  connClob = null;
               }

            }
         }
      } else if (!empOldGoals.equals(empGoals) || !empOldBonusMails.equals(empBonusMails) || !empOldTraining.equals(empTraining)) {
         sql = dataLogic.buildSQLString(sessionId, "InsertIvpArea");
         connClob = dataLogic.specificConnection();
         pstmtClobInsert = null;

         try {
            connClob.setAutoCommit(false);
            training = "";
            bonusEmail = "";
            goals = "";
            if (session.getAttribute("FWTrainingScholarship") == null) {
               training = "";
            } else {
               training = session.getAttribute("FWTrainingScholarship").toString();
            }

            if (session.getAttribute("FWBonusEmail") == null) {
               bonusEmail = "";
            } else {
               bonusEmail = session.getAttribute("FWBonusEmail").toString();
            }

            if (session.getAttribute("FWGoals") == null) {
               goals = "";
            } else {
               goals = session.getAttribute("FWGoals").toString();
            }

            pstmtClobInsert = connClob.prepareStatement(sql.toString());
            newClob = CLOB.createTemporary(connClob, false, 10);
            newClob.putString(1L, training);
            pstmtClobInsert.setClob(1, newClob);
            newClob1 = CLOB.createTemporary(connClob, false, 10);
            newClob1.putString(1L, bonusEmail);
            pstmtClobInsert.setClob(2, newClob1);
            newClob2 = CLOB.createTemporary(connClob, false, 10);
            newClob2.putString(1L, goals);
            pstmtClobInsert.setClob(3, newClob2);
            pstmtClobInsert.executeUpdate();
            dataLogic.commitTransaction(connClob);
         } catch (Exception var57) {
            LogManager.log("Exception e:", (Throwable)var57);
         } finally {
            if (pstmtClobInsert != null) {
               try {
                  pstmtClobInsert.close();
               } catch (SQLException var56) {
                  LogManager.log("Unable to Close statement.", (Throwable)var56);
               }
            }

            if (connClob != null) {
               try {
                  connClob.close();
               } catch (SQLException var55) {
                  LogManager.log("Unable to Close connection.", (Throwable)var55);
               }

               connClob = null;
            }

         }
      }

   }

   private void tutionAndReimbursement(String sessionId, Connection conn) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.tutionAndReimbursement()");
      Session session = SessionManager.findSession(sessionId);
      DataLogic dataLogic = this.module.getDataLogic();
      SqlStatement sql = null;
      LogManager.log("--------------->" + session.getAttribute("FWAmountUsed"));
      if (!session.getAttribute("FWAmountUsed").equals("0")) {
         sql = dataLogic.buildSQLString(sessionId, "InsertTuitionAndReimbursement");
         LogManager.log("Comming to there" + sql);
         dataLogic.startTransaction(conn);

         try {
            dataLogic.execute(conn, sql);
         } catch (EuropaException var7) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var7);
            throw var7;
         }

         dataLogic.commitTransaction(conn);
         session.removeAttribute("FWAmountUsed");
         session.removeAttribute("FWTuitionDate");
         session.removeAttribute("FWPurpose");
      }

   }

   private void deleteSelectedEntry(String sessionId) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.deleteSelectedEntry()");
      Session session = SessionManager.findSession(sessionId);
      DataLogic dataLogic = this.module.getDataLogic();
      Connection conn = null;

      try {
         conn = dataLogic.openConnection();
         SqlStatement sql = null;
         Vector results = null;
         sql = dataLogic.buildSQLString(sessionId, "SelectDelRecord");
         LogManager.log("Comming to there" + sql);
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var61) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var61);
            throw var61;
         }

         dataLogic.commitTransaction(conn);
         String awardedHelmet = "0";
         if (!results.isEmpty()) {
            Vector row = (Vector)results.get(0);
            awardedHelmet = row.get(2).toString();
         }

         CallableStatement cs = null;

         try {
            cs = conn.prepareCall("{call SP_AUDIT_HelmetAW_MASTINSERT(?,?,?)}");
            cs.setString(1, session.getAttribute("FWEmployeeID").toString());
            cs.setString(2, session.getUser().getEmployeeId().toString());
            cs.setString(3, "Delete");
            cs.execute();
         } catch (Exception var60) {
            LogManager.log("Exception raised: SP", (Throwable)var60);
         } finally {
            if (cs != null) {
               try {
                  cs.close();
               } catch (SQLException var56) {
                  LogManager.log("Unable to Close statement.", (Throwable)var56);
               }
            }

         }

         LogManager.log("tot hel:" + session.getAttribute("FWTotHelmets") + "hel amt:" + session.getAttribute("FWHelmetAmount") + session.getAttribute("FWAllocationDate"));

         try {
            cs = conn.prepareCall("{call SP_AUDIT_HELMETAWARDS(?,?,?,?,?,?,?,?,?,?,?,?,?)}");
            cs.setString(1, String.valueOf(0));
            cs.setString(2, String.valueOf(session.getAttribute("FWTotHelmets").toString()));
            cs.setString(3, String.valueOf(0));
            cs.setString(4, String.valueOf(session.getAttribute("FWHelmetAmount").toString()));
            cs.setString(5, String.valueOf(session.getAttribute("FWAllocationDate").toString()));
            cs.setString(6, String.valueOf(session.getAttribute("FWAllocationDate").toString()));
            cs.setString(7, String.valueOf(0));
            cs.setString(8, String.valueOf(awardedHelmet));
            cs.setString(9, String.valueOf(1));
            cs.setString(10, String.valueOf(1));
            cs.setString(11, String.valueOf(1));
            cs.setString(12, String.valueOf(1));
            cs.setString(13, String.valueOf(session.getUser().getEmployeeId().toString()));
            LogManager.log(" exec SP_AUDIT_HELMETAWARDS(0," + session.getAttribute("FWTotHelmets").toString() + ",0," + session.getAttribute("FWHelmetAmount").toString() + "," + session.getAttribute("FWAllocationDate").toString() + "," + session.getAttribute("FWAllocationDate").toString() + ",0," + awardedHelmet + ",1,1,1,1," + session.getUser().getEmployeeId().toString());
            cs.execute();
         } catch (Exception var59) {
            LogManager.log("Exception raised: SP", (Throwable)var59);
         } finally {
            if (cs != null) {
               try {
                  cs.close();
               } catch (SQLException var57) {
                  LogManager.log("Unable to Close statement.", (Throwable)var57);
               }
            }

         }

         sql = dataLogic.buildSQLString(sessionId, "DeleteRecord");
         LogManager.log("Comming to there" + sql);
         dataLogic.startTransaction(conn);

         try {
            dataLogic.execute(conn, sql);
         } catch (EuropaException var58) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var58);
            throw var58;
         }

         dataLogic.commitTransaction(conn);
         session.removeAttribute("FWDelSno");
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var55) {
               LogManager.log("Unable to Close connection.", (Throwable)var55);
            }

            conn = null;
         }

      }

   }

   private void deletedSelectedTutionEntry(String sessionId) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.deletedSelectedTutionEntry()");
      Session session = SessionManager.findSession(sessionId);
      DataLogic dataLogic = this.module.getDataLogic();
      Connection conn = dataLogic.openConnection();

      try {
         SqlStatement sql = null;
         sql = dataLogic.buildSQLString(sessionId, "UpdateStatusOfTutionRec");
         LogManager.log("Comming to there" + sql);
         dataLogic.startTransaction(conn);

         try {
            dataLogic.execute(conn, sql);
         } catch (EuropaException var14) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var14);
            throw var14;
         }

         dataLogic.commitTransaction(conn);
         session.removeAttribute("FWDelSno_Tuition");
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var13) {
               LogManager.log("Unable to Close connection.", (Throwable)var13);
            }

            conn = null;
         }

      }

   }

   private void deleteSelectEvaluation(String sessionId) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.deleteSelectEvaluation()");
      Session session = SessionManager.findSession(sessionId);
      DataLogic dataLogic = this.module.getDataLogic();
      Connection conn = dataLogic.openConnection();
      SqlStatement sql = null;

      try {
         sql = dataLogic.buildSQLString(sessionId, "UpdateStatusOfEvaluation");
         LogManager.log("Comming to there" + sql);
         dataLogic.startTransaction(conn);

         try {
            dataLogic.execute(conn, sql);
         } catch (EuropaException var14) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var14);
            throw var14;
         }

         dataLogic.commitTransaction(conn);
         session.removeAttribute("FWEvaluation_Sno");
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var13) {
               LogManager.log("Unable to Close connection.", (Throwable)var13);
            }

            conn = null;
         }

      }

   }

   private void deleteSelectHelmetAwards(String sessionId) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.deleteSelectHelmetAwards()");
      Session session = SessionManager.findSession(sessionId);
      DataLogic dataLogic = this.module.getDataLogic();
      Connection conn = dataLogic.openConnection();
      SqlStatement sql = null;

      try {
         sql = dataLogic.buildSQLString(sessionId, "UpdateStatusOfHelmet");
         LogManager.log("Comming to there" + sql);
         dataLogic.startTransaction(conn);

         try {
            dataLogic.execute(conn, sql);
         } catch (EuropaException var14) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var14);
            throw var14;
         }

         dataLogic.commitTransaction(conn);
         session.removeAttribute("FWEvaluation_Sno");
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var13) {
               LogManager.log("Unable to Close connection.", (Throwable)var13);
            }

            conn = null;
         }

      }

   }

   private String buildYearList(String sessionId, String queryName, String selectedYear) throws EuropaException {
      LogManager.log("AdminEmployeeBusinessLogic.BuildHelmetAwardsDetails().YearList()");
      Session session = SessionManager.findSession(sessionId);
      DataLogic dataLogic = this.module.getDataLogic();
      Connection conn = dataLogic.openConnection();
      Vector results = null;

      try {
         SqlStatement sql = null;
         Date dt = new Date();
         int presentYear = Integer.parseInt(String.valueOf(dt.getYear())) + 1900;
         sql = dataLogic.buildSQLString(sessionId, queryName);
         LogManager.log("Comming to there" + sql);
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var24) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var24);
            throw var24;
         }

         dataLogic.commitTransaction(conn);
         Vector DumEffectivedate;
         if (session.getAttribute("FWYearSelected").equals(String.valueOf(presentYear))) {
            if (!results.isEmpty()) {
               DumEffectivedate = (Vector)results.get(0);
               String year = DumEffectivedate.get(0).toString();
               session.setAttribute("FWYearSelected", year);
            } else {
               session.setAttribute("FWYearSelected", String.valueOf(presentYear - 1));
            }
         }

         DumEffectivedate = null;
         sql = dataLogic.buildSQLString(sessionId, "SelectPrevAuditTableValue");
         dataLogic.startTransaction(conn);

         try {
            DumEffectivedate = dataLogic.execute(conn, sql);
         } catch (EuropaException var23) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var23);
            throw var23;
         }

         dataLogic.commitTransaction(conn);
         Vector row;
         if (DumEffectivedate.isEmpty()) {
            session.setAttribute("FWPreAuditValueFlag", "0");
         } else {
            row = (Vector)DumEffectivedate.get(0);
            session.setAttribute("FWPreAuditValueFlag", row.get(0).toString());
         }

         LogManager.log("PerAuditValueFlag" + session.getAttribute("FWPreAuditValueFlag"));
         DumEffectivedate = null;
         sql = dataLogic.buildSQLString(sessionId, "SelectCurrentEffectiveDate");
         dataLogic.startTransaction(conn);

         try {
            DumEffectivedate = dataLogic.execute(conn, sql);
         } catch (EuropaException var22) {
            dataLogic.rollbackTransaction(conn);
            LogManager.log("Exception e is:" + var22);
            throw var22;
         }

         dataLogic.commitTransaction(conn);
         if (DumEffectivedate.isEmpty()) {
            session.setAttribute("FWCurrentEffectiveDateDum", "0");
         } else {
            row = (Vector)DumEffectivedate.get(0);
            session.setAttribute("FWCurrentEffectiveDateDum", row.get(0).toString());
         }

         LogManager.log("CurrentEffectiveDateDum" + session.getAttribute("FWCurrentEffectiveDateDum"));
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var21) {
               LogManager.log("Unable to Close connection.", (Throwable)var21);
            }

            conn = null;
         }

      }

      return this.buildList(sessionId, results, Integer.parseInt(selectedYear), "FWYearList");
   }

   private void clearEmpInfo(String sessionId) {
      LogManager.log("AdminEmployeeBusinessLogic.clearEmpInfo()");
      Session session = SessionManager.findSession(sessionId);
      session.removeAttribute("FWEmployeeID");
      session.removeAttribute("FWEmployeeNum");
      session.removeAttribute("FWSsn");
      session.removeAttribute("FWFirstName");
      session.removeAttribute("FWMiddleInitial");
      session.removeAttribute("FWLastName");
      session.removeAttribute("FWEmail");
      session.removeAttribute("FWDepartmentID");
      session.removeAttribute("FWTitle");
      session.removeAttribute("FWAddress_1");
      session.removeAttribute("FWAddress_2");
      session.removeAttribute("FWCity");
      session.removeAttribute("FWState");
      session.removeAttribute("FWZip");
      session.removeAttribute("FWHomePhoneAreaCode");
      session.removeAttribute("FWHomePhonePrefix");
      session.removeAttribute("FWHomePhonePostfix");
      session.removeAttribute("FWWorkPhoneAreaCode");
      session.removeAttribute("FWWorkPhonePrefix");
      session.removeAttribute("FWWorkPhonePostfix");
      session.removeAttribute("FWExtention");
      session.removeAttribute("FWFaxAreaCode");
      session.removeAttribute("FWFaxPrefix");
      session.removeAttribute("FWFaxPostfix");
      session.removeAttribute("FWCellAreaCode");
      session.removeAttribute("FWCellPrefix");
      session.removeAttribute("FWCellPostfix");
      session.removeAttribute("FWPagerAreaCode");
      session.removeAttribute("FWPagerPrefix");
      session.removeAttribute("FWPagerPostfix");
      session.removeAttribute("FWSalaryPeriodID");
      session.removeAttribute("FWSalaryAmount");
      session.removeAttribute("FWBurdenFactor");
      session.removeAttribute("FWHireDate");
      session.removeAttribute("FWTermDate");
      session.removeAttribute("FWTeamID");
      session.removeAttribute("FWisProjectManager");
      session.removeAttribute("FWisAccountManager");
      session.removeAttribute("FWisDesignAndAnalysCompEast");
      session.removeAttribute("FWisDesignAndAnalysCompWest");
      session.removeAttribute("FWisDirectRep");
      session.removeAttribute("FWisTeamLeader");
      session.removeAttribute("FWisTeamMember");
      session.removeAttribute("FWLastUpdated");
      session.removeAttribute("FWisPM");
      session.removeAttribute("FWisAM");
      session.removeAttribute("FWisCOMPEAST");
      session.removeAttribute("FWisCOMPWEST");
      session.removeAttribute("FWisTLManager");
      session.removeAttribute("FWisDR");
      session.removeAttribute("FWisTL");
      session.removeAttribute("FWisChk");
      session.removeAttribute("FWisTM");
      session.removeAttribute("FWActID");
      session.removeAttribute("FWactCode");
      session.removeAttribute("FWDepartmentsList");
      session.removeAttribute("FWTeamsList");
      session.removeAttribute("FWdesc");
      session.removeAttribute("FWsubCode");
      session.removeAttribute("FWisPL");
      session.removeAttribute("FWisProjectLeader");
      session.removeAttribute("FWisSalesEngineer");
      session.removeAttribute("FWisSrSalesEngineer");
      session.removeAttribute("FWisVertical");
      session.removeAttribute("FWisJrPartner");
      session.removeAttribute("FWAnalysisVertical");
      session.removeAttribute("FWElecVertical");
      session.removeAttribute("FWManFactVertical");
      session.removeAttribute("FWSecureInfoEditStart");
      session.removeAttribute("FWSecureInfoEditEnd");
      session.removeAttribute("FWWageDateLength");
      session.removeAttribute("FWSno");
      session.removeAttribute("FWSalaryPeriodeForAudit");
      session.removeAttribute("FWEmpTimeIdForSecureInfoaudit");
      session.removeAttribute("FWPeopleManagerListValue");
      session.removeAttribute("FWTopLineManagerListValue");
      session.removeAttribute("FWisPM");
      session.removeAttribute("FWisPManager");
      session.removeAttribute("FWisPeopleManager");
      session.removeAttribute("FWPeopleManagerID");
      session.removeAttribute("FWisAdminGroup1");
      session.removeAttribute("FWShowAdminGroupStart");
      session.removeAttribute("FWShowAdminGroupEnd");
      session.removeAttribute("FWAllEmpWageId");
      session.removeAttribute("FWAssociatedPeopleManager");
      session.removeAttribute("FWisJpr");
      session.removeAttribute("FWisAutoAllocation1");
      session.removeAttribute("FWJpAllocatedHours");
      session.removeAttribute("FWisSSE");
      session.removeAttribute("FWisSSEAutoAllocation1");
      session.removeAttribute("FWSSEAllocatedHours");
      session.removeAttribute("FWisSE");
      session.removeAttribute("FWisVer");
      session.removeAttribute("FWisAnv");
      session.removeAttribute("FWisElv");
      session.removeAttribute("FWisMfv");
      session.removeAttribute("FWisEAdmn");
      session.removeAttribute("FWisPManager");
      session.removeAttribute("FWisAdminGP");
      session.removeAttribute("FWEngineeringManager");
      session.removeAttribute("FWPayRollDept");
      session.removeAttribute("FWNonCompanyfunctions");
      session.removeAttribute("FWNoncompanyfunMode");
      session.removeAttribute("FWisResourceManager");
      session.removeAttribute("FWisOffshoreManager");
      session.removeAttribute("FWisElectricalSystemsIntegration");
      session.removeAttribute("FWJobLevelID");
   }

   private String BuildNoArchiveData(String archiveName) {
      StringBuffer sb = new StringBuffer();
      if (archiveName.equals("7")) {
         sb.append("<table border=\"1\" width=\"100%\" cellpadding=\"5\" bgcolor=\"#C4C8FB\" bordercolor=\"#FFFFFF\" style=\"border-collapse: collapse\">").append("<tr><td align=\"center\" colspan=\"2\"><p align=\"left\"><font face=\"Tahoma\"><b><span style=\"font-size: 9pt\">No Flex time Archive Information Available</span></b></font></td>").append("</table>");
      } else {
         sb.append("<table border=\"1\" width=\"100%\" cellpadding=\"5\" bgcolor=\"#C4C8FB\" bordercolor=\"#FFFFFF\" style=\"border-collapse: collapse\">").append("<tr><td align=\"center\" colspan=\"2\"><p align=\"left\"><font face=\"Tahoma\"><b><span style=\"font-size: 9pt\">No Bank time Archive Information Available</span></b></font></td>").append("</table>");
      }

      return sb.toString();
   }

   private void DataFeedFunction(String Mode, String sessionId) {
      LogManager.log("AdminEmployeeBusinessLogic.DataFeedFunction()");
      Session session = SessionManager.findSession(sessionId);
      if (Mode.equals("Insert")) {
         if (session.getUser().getEmployeeId().toString().equals("82")) {
            session.setAttribute("FWDataFeed", "checked");
            session.setAttribute("FWDataFeedValue", "1");
         } else {
            session.setAttribute("FWDataFeed", "checked disabled");
            session.setAttribute("FWDataFeedValue", "1");
         }
      } else {
         String feedvalue = session.getAttribute("FWDataFeedValue").toString();
         if (session.getUser().getEmployeeId().toString().equals("82")) {
            if (feedvalue.equals("1")) {
               session.setAttribute("FWDataFeed", "checked");
            } else {
               session.setAttribute("FWDataFeed", "");
            }
         } else if (feedvalue.equals("1")) {
            session.setAttribute("FWDataFeed", "checked disabled");
         } else {
            session.setAttribute("FWDataFeed", "disabled");
         }
      }

   }

   private void CollectAllEmployeeWageId(String sessionId, DataLogic dataLogic, Connection conn, String Type) throws EuropaException {
      Session session = SessionManager.findSession(sessionId);
      String AllEmployee = "0";
      int subsum = 0;
      Vector results1 = null;
      SqlStatement sql = null;
      if (!Type.equals("Edit")) {
         dataLogic = this.module.getDataLogic();
         conn = dataLogic.openConnection();
         session.setAttribute("FWEmployeeID", "0");
      }

      sql = dataLogic.buildSQLString(sessionId, "getAllEmployeeIdWage");
      dataLogic.startTransaction(conn);

      try {
         results1 = dataLogic.execute(conn, sql);
      } catch (EuropaException var12) {
         dataLogic.rollbackTransaction(conn);
         throw var12;
      }

      dataLogic.commitTransaction(conn);
      if (!Type.equals("Edit")) {
         dataLogic.closeConnection(conn);
      }

      if (results1 != null && results1.size() != 0) {
         for(int i = 0; i < results1.size(); ++i) {
            if (i == 0) {
               AllEmployee = "";
            }

            Vector row = (Vector)results1.get(i);
            AllEmployee = AllEmployee + row.get(0).toString() + ",";
         }
      }

      if (!AllEmployee.equals("0")) {
         AllEmployee = AllEmployee.substring(0, AllEmployee.length() - 1);
      }

      session.setAttribute("FWAllEmpWageId", AllEmployee);
   }

   private boolean isPeopleManagerUsers(String sessionId, String Employeeid) throws EuropaException {
      Session session = SessionManager.findSession(sessionId);
      boolean flage = false;
      Connection conn = null;

      try {
         DataLogic dataLogic = this.module.getDataLogic();
         conn = dataLogic.openConnection();
         SqlStatement sql = null;
         Vector results = null;
         LogManager.log("Employeee  :" + Employeeid);
         session.setAttribute("FWPeopleManagerUser", session.getUser().getEmployeeId().toString());
         LogManager.log("FWPeopleManagerUser :" + session.getUser().getEmployeeId().toString());
         sql = dataLogic.buildSQLString(sessionId, "SelectedPeoplemanagerUser");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var19) {
            dataLogic.rollbackTransaction(conn);
            throw var19;
         }

         LogManager.log(" Result  :" + results);
         if (!results.isEmpty()) {
            for(int i = 0; i < results.size(); ++i) {
               Vector temp = (Vector)results.get(i);
               String empID = temp.get(0).toString();
               if (Employeeid.equals(empID)) {
                  flage = true;
               }
            }
         }

         session.removeAttribute("FWPeopleManagerUser");
         LogManager.log("flage : " + flage);
         dataLogic.commitTransaction(conn);
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var18) {
               LogManager.log("Unable to Close connection.", (Throwable)var18);
            }

            conn = null;
         }

      }

      return flage;
   }

   private boolean isTopLineManagerUsers(String sessionId, String Employeeid) throws EuropaException {
      Session session = SessionManager.findSession(sessionId);
      boolean flage = false;
      Connection conn = null;

      try {
         DataLogic dataLogic = this.module.getDataLogic();
         conn = dataLogic.openConnection();
         SqlStatement sql = null;
         Vector results = null;
         LogManager.log("Employeee  :" + Employeeid);
         session.setAttribute("FWTopLineManagerUser", session.getUser().getEmployeeId().toString());
         LogManager.log("FWTopLineManagerUser :" + session.getAttribute("FWTopLineManagerUser"));
         sql = dataLogic.buildSQLString(sessionId, "SelectedTopLineManagerUser");
         LogManager.log("" + sql);
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var19) {
            dataLogic.rollbackTransaction(conn);
            throw var19;
         }

         LogManager.log(" Result  :" + results);
         if (!results.isEmpty()) {
            for(int i = 0; i < results.size(); ++i) {
               Vector temp = (Vector)results.get(i);
               String empID = temp.get(0).toString();
               if (Employeeid.equals(empID)) {
                  flage = true;
               }
            }
         }

         session.removeAttribute("FWTopLineManagerUser");
         LogManager.log("flage : " + flage);
         dataLogic.commitTransaction(conn);
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var18) {
               LogManager.log("Unable to Close connection.", (Throwable)var18);
            }

            conn = null;
         }

      }

      return flage;
   }

   private boolean Checkvalidemail(String sessionId, SqlStatement sqlvalid, DataLogic dataLogic, Connection conn, String mode) throws EuropaException {
      Session session = SessionManager.findSession(sessionId);
      LogManager.log("Checkvalidemail");
      Vector userValidationRslt = null;
      boolean userValid = true;
      User checkUser = null;
      String eMail = "";
      String empEmail = null;
      String empid = null;
      sqlvalid = dataLogic.buildSQLString(sessionId, "SelectAllEmployees");
      dataLogic.startTransaction(conn);
      userValidationRslt = dataLogic.execute(conn, sqlvalid);
      dataLogic.commitTransaction(conn);
      int i;
      Vector row;
      if (mode.equals("Insert")) {
         for(i = 0; i < userValidationRslt.size(); ++i) {
            row = (Vector)userValidationRslt.get(i);
            empEmail = row.get(6).toString();
            empid = row.get(0).toString();
            if (empEmail.equals(session.getAttribute("FWEmail").toString())) {
               userValid = false;
            }
         }

         try {
            eMail = session.getAttribute("FWEmail").toString();
            checkUser = SecurityManager.getUser(eMail);
         } catch (Exception var16) {
         }
      } else {
         for(i = 0; i < userValidationRslt.size(); ++i) {
            row = (Vector)userValidationRslt.get(i);
            empEmail = row.get(6).toString();
            empid = row.get(0).toString();
            if (empEmail.equals(session.getAttribute("FWEmail").toString()) && !session.getAttribute("FWEmployeeID").toString().equals(empid)) {
               userValid = false;
            }
         }

         try {
            eMail = session.getAttribute("FWEmail").toString();
            if (SecurityManager.getUser(eMail).getEmployeeId().equals(session.getAttribute("FWEmployeeID").toString())) {
               checkUser = SecurityManager.getUser(eMail);
            }
         } catch (Exception var15) {
         }
      }

      return checkUser == null && userValid;
   }

   private Date DateFormatFunction(String sessionId, String DateType) throws ParseException {
      LogManager.log("DateFormatFunction");
      Date date = null;

      try {
         SimpleDateFormat expformatter = new SimpleDateFormat("MM/dd/yyyy");
         String dateyearString = "";
         String yearhire = "";
         String normyear = "";
         int Yearexp = Integer.parseInt(DateType.substring(6, 8));
         String dayexp = DateType.substring(3, 5);
         String monexp = DateType.substring(0, 2);
         if (Yearexp >= 80) {
            dateyearString = "19" + Yearexp;
            yearhire = monexp + "/" + dayexp + "/" + dateyearString;
         } else {
            if (Yearexp <= 9) {
               normyear = "200" + String.valueOf(Yearexp);
            } else {
               normyear = "20" + String.valueOf(Yearexp);
            }

            yearhire = monexp + "/" + dayexp + "/" + normyear;
         }

         date = expformatter.parse(yearhire);
         return date;
      } catch (ParseException var11) {
         LogManager.log("DateFormatFunction Exception e:" + var11);
         throw var11;
      }
   }

   private void setDisplayLabel(String sessionId) throws EuropaException {
      Session session = SessionManager.findSession(sessionId);
      session.setAttribute("FW_SRSALESENGINEER_GROUPLABEL", System.getProperty("europa.group.desc.srsalesengineer"));
      session.setAttribute("FW_VERTICAL_GROUPLABEL", System.getProperty("europa.group.desc.vertical"));
      session.setAttribute("FW_PROJECTLEADER_GROUPLABEL", System.getProperty("europa.group.desc.projectleader"));
      session.setAttribute("FW_PROJECTMNGRS_GROUPLABEL", System.getProperty("europa.group.desc.projectmngrs"));
      session.setAttribute("FW_ACCOUNTEXEC_GROUPLABEL", System.getProperty("europa.group.desc.accountexec"));
      session.setAttribute("FW_TOPLINEMANAGER_GROUPLABEL", System.getProperty("europa.group.desc.topLineManager"));
      session.setAttribute("FW_EMPLOYEES_GROUPLABEL", System.getProperty("europa.group.desc.employees"));
      session.setAttribute("FW_TEAMLEADERS_GROUPLABEL", System.getProperty("europa.group.desc.teamleaders"));
      session.setAttribute("FW_ADMIN_GROUPLABEL", System.getProperty("europa.group.desc.admin"));
      session.setAttribute("FW_SALESENGINEER_GROUPLABEL", System.getProperty("europa.group.desc.salesengineer"));
      session.setAttribute("FW_EVERYONE_GROUPLABEL", System.getProperty("europa.group.desc.everyone"));
      session.setAttribute("FW_JRPARTNER_GROUPLABEL", System.getProperty("europa.group.desc.jrpartner"));
      session.setAttribute("FW_ANALYSISVERTICAL_GROUPLABEL", System.getProperty("europa.group.desc.analysisvertical"));
      session.setAttribute("FW_ELECTRICALVERTICAL_GROUPLABEL", System.getProperty("europa.group.desc.electricalvertical"));
      session.setAttribute("FW_MANUFACTURINGVERTICAL_GROUPLABEL", System.getProperty("europa.group.desc.manufacturingvertical"));
      session.setAttribute("FW_CHECKERS_GROUPLABEL", System.getProperty("europa.group.desc.checkers"));
      session.setAttribute("FW_SU_GROUPLABEL", System.getProperty("europa.group.desc.su"));
      session.setAttribute("FW_ENGADMIN_GROUPLABEL", System.getProperty("europa.group.desc.engadmin"));
      session.setAttribute("FW_PEOPLEMANAGER_GROUPLABEL", System.getProperty("europa.group.desc.peoplemanager"));
      session.setAttribute("FW_DIRECTREP_GROUPLABEL", System.getProperty("europa.group.desc.directrep"));
      session.setAttribute("FW_ACTIVITY_DISPLAY_LABEL", System.getProperty("europa.activity.display.label"));
      session.setAttribute("FW_CHARGENUMBER_DISPLAY_LABEL", System.getProperty("europa.chargenumber.display.label"));
      session.setAttribute("FW_POWERSYSTEM_GROUPLABEL", System.getProperty("europa.group.desc.powersystemcompetency"));
      session.setAttribute("FW_ENGMANAGER_GROUPLABEL", System.getProperty("europa.group.desc.engineeringmanager"));
      session.setAttribute("FW_LEADENGINEER_GROUPLABEL", System.getProperty("europa.group.desc.leadengineer"));
      session.setAttribute("FW_IsTOPLINEMANAGER_GROUPLABEL", System.getProperty("europa.group.desc.topLineManager"));
      session.setAttribute("FW_TOPLINEMANAGERID_GROUPLABEL", System.getProperty("europa.group.desc.topLineManager"));
      session.setAttribute("FW_ANALYSISVERTICALEAST_GROUPLABEL", System.getProperty("europa.group.desc.designandanalysiscompeast"));
      session.setAttribute("FW_ANALYSISVERTICALWEST_GROUPLABEL", System.getProperty("europa.group.desc.designandanalysiscompwest"));
      session.setAttribute("FW_RESOURCEMANAGER_GROUPLABEL", System.getProperty("europa.group.desc.resourcemanager"));
      session.setAttribute("FW_OFFSHOREMANAGER_GROUPLABEL", System.getProperty("europa.group.desc.offshoreManager"));
      session.setAttribute("FW_ELECTRICALSYSTEMINTEGRATION_GROUPLABEL", System.getProperty("europa.group.desc.ElectricalSystemsIntegration"));
   }

   private boolean UpdateTerminationonJasper(String sessionId) {
      Session session = SessionManager.findSession(sessionId);
      String jasperEmail = (String)session.getAttribute("FWOldEmail");
      boolean isterminated = false;
      LogManager.log("Email....." + jasperEmail);
      SimpleDateFormat parser12 = new SimpleDateFormat("MM/dd/yy");
      LogManager.log("Email.....2" + jasperEmail);
      if (jasperEmail != null) {
         JasperserverConnector jsc = new JasperserverConnector();

         try {
            String termDate1 = (String)session.getAttribute("FWTermDate");
            LogManager.log("Inner Jastper");
            if (SecurityManager.getUser(jasperEmail) != null) {
               User jasperuser = SecurityManager.getUser(jasperEmail);
               if (!termDate1.equals("")) {
                  Date newterm = parser12.parse(termDate1);
                  Date ds = new Date();
                  String s = parser12.format(ds);
                  Date today = parser12.parse(s);
                  LogManager.log(" dates " + newterm + " today " + today + " EmpId" + jasperEmail);
                  if (!today.after(newterm) && !today.equals(newterm)) {
                     if (today.before(newterm)) {
                        jsc.updateJSUserTerminate(jasperuser, false);
                     }
                  } else {
                     jsc.updateJSUserTerminate(jasperuser, true);
                     isterminated = true;
                  }
               } else if (termDate1 == null || termDate1.equals("")) {
                  LogManager.log("To activate err in null" + termDate1);
                  jsc.updateJSUserTerminate(jasperuser, false);
               }
            }
         } catch (Exception var13) {
            LogManager.log(" dates error " + var13);
         }
      }

      return isterminated;
   }

   private String buildEngMgrList(String sessionId, DataLogic dataLogic, Connection conn) throws EuropaException {
      Session session = SessionManager.findSession(sessionId);
      Vector results = null;
      int em = Integer.parseInt((String)session.getAttribute("FWEngineeringManagerID"));
      Vector engineeringManagers = new Vector();
      SqlStatement sql = dataLogic.buildSQLString(sessionId, "SelectEngineeringManager");
      dataLogic.startTransaction(conn);

      try {
         results = dataLogic.execute(conn, sql);
      } catch (EuropaException var14) {
         dataLogic.rollbackTransaction(conn);
         throw var14;
      }

      Vector temVector = null;
      if (results != null && !results.isEmpty()) {
         for(int i = 0; i < results.size(); ++i) {
            Vector v = (Vector)results.get(i);
            temVector = new Vector();
            String empId = v.get(0).toString();
            String empname = v.get(1).toString();
            temVector.add(empId);
            temVector.add(empname);
            engineeringManagers.add(temVector);
         }
      }

      LogManager.log(" EngineeringManagers results  :" + engineeringManagers);
      return this.buildList(sessionId, engineeringManagers, em, "FWEngineeringManager");
   }

   private String buildLeadEngList(String sessionId, DataLogic dataLogic, Connection conn) throws EuropaException {
      Session session = SessionManager.findSession(sessionId);
      Vector results = null;
      int le = Integer.parseInt((String)session.getAttribute("FWLeadEngineerID"));
      Vector leadEngineers = new Vector();
      SqlStatement sql = dataLogic.buildSQLString(sessionId, "SelectLeadEngineer");
      dataLogic.startTransaction(conn);

      try {
         results = dataLogic.execute(conn, sql);
      } catch (EuropaException var14) {
         dataLogic.rollbackTransaction(conn);
         throw var14;
      }

      Vector temVector = null;
      if (results != null && !results.isEmpty()) {
         for(int i = 0; i < results.size(); ++i) {
            Vector v = (Vector)results.get(i);
            temVector = new Vector();
            String empId = v.get(0).toString();
            String empname = v.get(1).toString();
            temVector.add(empId);
            temVector.add(empname);
            leadEngineers.add(temVector);
         }
      }

      LogManager.log(" EngineeringManagers results  :" + leadEngineers);
      return this.buildList(sessionId, leadEngineers, le, "FWLeadEng");
   }

   private String buildTopLineMgrList(String sessionId, DataLogic dataLogic, Connection conn) throws EuropaException {
      LogManager.log("buildTopLineMgrList");
      Session session = SessionManager.findSession(sessionId);
      Vector topLineManager = new Vector();
      new Vector();
      LogManager.log("Employeeid :" + session.getAttribute("FWEmployeeID"));
      SqlStatement sql = dataLogic.buildSQLString(sessionId, "SelectTopLineManagers");
      LogManager.log("FWTopLineManagerId :" + session.getAttribute("FWTopLineManagerId"));
      dataLogic.startTransaction(conn);

      Vector results;
      try {
         results = dataLogic.execute(conn, sql);
      } catch (EuropaException var19) {
         dataLogic.rollbackTransaction(conn);
         throw var19;
      }

      dataLogic.commitTransaction(conn);
      LogManager.log(" results  :" + results);
      int tl = Integer.parseInt((String)session.getAttribute("FWTopLineManagerId"));
      int k = 0;
      int length = results.size();
      LogManager.log(" Size  :" + results.size());
      Vector temVector;
      if (results.size() > 0) {
         do {
            temVector = (Vector)results.get(k);
            String emailPM = temVector.get(2).toString();

            try {
               if (SecurityManager.isMember("TopLineManager", SecurityManager.getUser(emailPM))) {
                  topLineManager.add(results.get(k));
                  LogManager.log("Register User True " + emailPM);
               }
            } catch (Exception var18) {
               LogManager.log("Register User false " + emailPM);
            }

            ++k;
         } while(k < length);
      }

      try {
         results.clear();
         sql = dataLogic.buildSQLString(sessionId, "SelectedTopLineManager");
         dataLogic.startTransaction(conn);

         try {
            results = dataLogic.execute(conn, sql);
         } catch (EuropaException var17) {
            dataLogic.rollbackTransaction(conn);
            throw var17;
         }

         dataLogic.commitTransaction(conn);
         temVector = new Vector();
         if (!results.isEmpty()) {
            for(int i = 0; i < results.size(); ++i) {
               Vector v = (Vector)results.get(i);
               String empId = v.get(0).toString();
               String empname = v.get(1).toString();
               String empEmail = v.get(2).toString();
               temVector.add(empId);
               temVector.add(empname);
               temVector.add(empEmail);
            }

            topLineManager.add(temVector);
         }
      } catch (Exception var20) {
      }

      return this.buildList(sessionId, topLineManager, tl, "FWTopLineManager");
   }

   private void buildSubCompetencyList(String sessionId, DataLogic dataLogic, Connection conn, String curSubCompCenterId) throws EuropaException {
      Session session = SessionManager.findSession(sessionId);
      session.setAttribute("FWSubCompCenterId", curSubCompCenterId);
      Vector results = null;
      SqlStatement sql = dataLogic.buildSQLString(sessionId, "SelectAllSubCompetencyCenters");

      try {
         results = dataLogic.execute(conn, sql);
      } catch (EuropaException var14) {
         dataLogic.rollbackTransaction(conn);
         throw var14;
      }

      StringBuffer sbSubComp = new StringBuffer();
      StringBuffer sbCompId = new StringBuffer();

      for(int i = 0; i < results.size(); ++i) {
         Vector row = (Vector)results.get(i);
         String compId = row.get(2).toString();
         String subComp = "'" + row.get(0).toString() + "#" + row.get(1).toString() + "#" + compId + "'";
         if (i == 0) {
            sbSubComp.append("[");
            sbCompId.append("[");
         }

         sbSubComp.append(subComp);
         sbCompId.append(compId);
         if (i == results.size() - 1) {
            sbSubComp.append("]");
            sbCompId.append("]");
         } else {
            sbSubComp.append(",");
            sbCompId.append(",");
         }

         session.setAttribute("FWSubCompArray", sbSubComp.toString());
         session.setAttribute("FWCompIdArray", sbCompId.toString());
      }

   }

   public void CheckEmpinfodifferences(String sessionId) throws EuropaException {
      LogManager.log("CheckEmpinfodifferences");
      Vector modifiedInfo = new Vector();
      Session session = SessionManager.findSession(sessionId);
      String oldFirstName = session.getAttribute("FWOldFirstName").toString().trim();
      String oldLastName = session.getAttribute("FWOldLastName").toString().trim();
      String oldEmail = session.getAttribute("FWOldEmail").toString().trim();
      String oldmiddleInitial = session.getAttribute("FWOldMI").toString().trim();
      String oldaddress_1 = session.getAttribute("FWOldAddress1").toString().trim();
      String oldaddress_2 = session.getAttribute("FWOldAddress2").toString().trim();
      String oldcity = session.getAttribute("FWOldCity").toString().trim();
      String oldstate = session.getAttribute("FWOldState").toString().trim();
      String oldzip = session.getAttribute("FWOldZip").toString().trim();
      String oldhAreaCode = session.getAttribute("FWOldHAreaCode").toString().trim();
      String oldhPrefix = session.getAttribute("FWOldHPrefix").toString().trim();
      String oldhPostfix = session.getAttribute("FWOldHPostfix").toString().trim();
      String oldwAreaCode = session.getAttribute("FWOldWAreaCode").toString().trim();
      String oldwPrefix = session.getAttribute("FWOldWPrefix").toString().trim();
      String oldwPostfix = session.getAttribute("FWOldWPostfix").toString().trim();
      String oldextention = session.getAttribute("FWOldextention").toString().trim();
      String oldfAreaCode = session.getAttribute("FWOldFAreacode").toString().trim();
      String oldfPrefix = session.getAttribute("FWOldFPrefix").toString().trim();
      String oldfPostfix = session.getAttribute("FWOldFPostfix").toString().trim();
      String oldcAreaCode = session.getAttribute("FWOldCAreaCode").toString().trim();
      String oldcPrefix = session.getAttribute("FWOldCPrefix").toString().trim();
      String oldcPostfix = session.getAttribute("FWOldCPostfix").toString().trim();
      String oldpAreaCode = session.getAttribute("FWOldPAreaCode").toString().trim();
      String oldpPrefix = session.getAttribute("FWOldPPrefix").toString().trim();
      String oldpPostfix = session.getAttribute("FWOldPPostfix").toString().trim();
      String newFirstName = session.getAttribute("FWFirstName").toString().trim();
      String newLastName = session.getAttribute("FWLastName").toString().trim();
      String newEmail = session.getAttribute("FWEmail").toString().trim();
      String newmiddleInitial = session.getAttribute("FWMiddleInitial").toString().trim();
      String newaddress_1 = session.getAttribute("FWAddress_1").toString().trim();
      String newaddress_2 = session.getAttribute("FWAddress_2").toString().trim();
      String newcity = session.getAttribute("FWCity").toString().trim();
      String newstate = session.getAttribute("FWState").toString().trim();
      String newzip = session.getAttribute("FWZip").toString().trim();
      String newhAreaCode = session.getAttribute("FWHomePhoneAreaCode").toString().trim();
      String newhPrefix = session.getAttribute("FWHomePhonePrefix").toString().trim();
      String newhPostfix = session.getAttribute("FWHomePhonePostfix").toString().trim();
      String newwAreaCode = session.getAttribute("FWWorkPhoneAreaCode").toString().trim();
      String newwPrefix = session.getAttribute("FWWorkPhonePrefix").toString().trim();
      String newwPostfix = session.getAttribute("FWWorkPhonePostfix").toString().trim();
      String newextention = session.getAttribute("FWExtention").toString().trim();
      String newfAreaCode = session.getAttribute("FWFaxAreaCode").toString().trim();
      String newfPrefix = session.getAttribute("FWFaxPrefix").toString().trim();
      String newfPostfix = session.getAttribute("FWFaxPostfix").toString().trim();
      String newcAreaCode = session.getAttribute("FWCellAreaCode").toString().trim();
      String newcPrefix = session.getAttribute("FWCellPrefix").toString().trim();
      String newcPostfix = session.getAttribute("FWCellPostfix").toString().trim();
      String newpAreaCode = session.getAttribute("FWPagerAreaCode").toString().trim();
      String newpPrefix = session.getAttribute("FWPagerPrefix").toString().trim();
      String newpPostfix = session.getAttribute("FWPagerPostfix").toString().trim();
      Vector innerVect;
      if (!oldFirstName.equals(newFirstName)) {
         innerVect = new Vector();
         innerVect.add("First Name");
         innerVect.add(oldFirstName);
         innerVect.add(newFirstName);
         modifiedInfo.add(innerVect);
      }

      if (!oldLastName.equals(newLastName)) {
         innerVect = new Vector();
         innerVect.add("LastName");
         innerVect.add(oldLastName);
         innerVect.add(newLastName);
         modifiedInfo.add(innerVect);
      }

      if (!oldmiddleInitial.equals(newmiddleInitial)) {
         innerVect = new Vector();
         innerVect.add("MI");
         innerVect.add(oldmiddleInitial);
         innerVect.add(newmiddleInitial);
         modifiedInfo.add(innerVect);
      }

      if (!oldEmail.equals(newEmail)) {
         innerVect = new Vector();
         innerVect.add("Email");
         innerVect.add(oldEmail);
         innerVect.add(newEmail);
         modifiedInfo.add(innerVect);
      }

      if (!oldaddress_1.equals(newaddress_1)) {
         innerVect = new Vector();
         innerVect.add("Address Line 1");
         innerVect.add(oldaddress_1);
         innerVect.add(newaddress_1);
         modifiedInfo.add(innerVect);
      }

      if (!oldaddress_2.equals(newaddress_2)) {
         innerVect = new Vector();
         innerVect.add("Address Line 2");
         innerVect.add(oldaddress_2);
         innerVect.add(newaddress_2);
         modifiedInfo.add(innerVect);
      }

      if (!oldcity.equals(newcity)) {
         innerVect = new Vector();
         innerVect.add("City");
         innerVect.add(oldcity);
         innerVect.add(newcity);
         modifiedInfo.add(innerVect);
      }

      if (!oldstate.equals(newstate)) {
         innerVect = new Vector();
         innerVect.add("State");
         innerVect.add(oldstate);
         innerVect.add(newstate);
         modifiedInfo.add(innerVect);
      }

      if (!oldzip.equals(newzip)) {
         innerVect = new Vector();
         innerVect.add("Zip");
         innerVect.add(oldzip);
         innerVect.add(newzip);
         modifiedInfo.add(innerVect);
      }

      if (!oldhAreaCode.equals(newhAreaCode)) {
         innerVect = new Vector();
         innerVect.add("Home Phone Area Code");
         innerVect.add(oldhAreaCode);
         innerVect.add(newhAreaCode);
         modifiedInfo.add(innerVect);
      }

      if (!oldhPrefix.equals(newhPrefix)) {
         innerVect = new Vector();
         innerVect.add("Home Phone Prefix");
         innerVect.add(oldhPrefix);
         innerVect.add(newhPrefix);
         modifiedInfo.add(innerVect);
      }

      if (!oldhPostfix.equals(newhPostfix)) {
         innerVect = new Vector();
         innerVect.add("Home Phone Postfix");
         innerVect.add(oldhPostfix);
         innerVect.add(newhPostfix);
         modifiedInfo.add(innerVect);
      }

      if (!oldwAreaCode.equals(newwAreaCode)) {
         innerVect = new Vector();
         innerVect.add("Work Phone Area Code");
         innerVect.add(oldwAreaCode);
         innerVect.add(newwAreaCode);
         modifiedInfo.add(innerVect);
      }

      if (!oldwPrefix.equals(newwPrefix)) {
         innerVect = new Vector();
         innerVect.add("Work Phone Prefix");
         innerVect.add(oldwPrefix);
         innerVect.add(newwPrefix);
         modifiedInfo.add(innerVect);
      }

      if (!oldwPostfix.equals(newwPostfix)) {
         innerVect = new Vector();
         innerVect.add("Work Phone Postfix");
         innerVect.add(oldwPostfix);
         innerVect.add(newwPostfix);
         modifiedInfo.add(innerVect);
      }

      if (!oldextention.equals(newextention)) {
         innerVect = new Vector();
         innerVect.add("Work Phone Extn");
         innerVect.add(oldextention);
         innerVect.add(newextention);
         modifiedInfo.add(innerVect);
      }

      if (!oldfAreaCode.equals(newfAreaCode)) {
         innerVect = new Vector();
         innerVect.add("Fax Area Code");
         innerVect.add(oldfAreaCode);
         innerVect.add(newfAreaCode);
         modifiedInfo.add(innerVect);
      }

      if (!oldfPrefix.equals(oldfPrefix)) {
         innerVect = new Vector();
         innerVect.add("Fax Number Prefix");
         innerVect.add(oldfPrefix);
         innerVect.add(oldfPrefix);
         modifiedInfo.add(innerVect);
      }

      if (!oldfPostfix.equals(newfPostfix)) {
         innerVect = new Vector();
         innerVect.add("Fax Number Postfix");
         innerVect.add(oldfPostfix);
         innerVect.add(newfPostfix);
         modifiedInfo.add(innerVect);
      }

      if (!oldcAreaCode.equals(newcAreaCode)) {
         innerVect = new Vector();
         innerVect.add("Cell Phone Area Code");
         innerVect.add(oldcAreaCode);
         innerVect.add(newcAreaCode);
         modifiedInfo.add(innerVect);
      }

      if (!oldcPrefix.equals(newcPrefix)) {
         innerVect = new Vector();
         innerVect.add("Cell Phone Number Prefix");
         innerVect.add(oldcPrefix);
         innerVect.add(newcPrefix);
         modifiedInfo.add(innerVect);
      }

      if (!oldcPostfix.equals(newcPostfix)) {
         innerVect = new Vector();
         innerVect.add("Cell Phone Number Postfix");
         innerVect.add(oldcPostfix);
         innerVect.add(newcPostfix);
         modifiedInfo.add(innerVect);
      }

      if (!oldpAreaCode.equals(newpAreaCode)) {
         innerVect = new Vector();
         innerVect.add("Pager Number Area Code");
         innerVect.add(oldpAreaCode);
         innerVect.add(newpAreaCode);
         modifiedInfo.add(innerVect);
      }

      if (!oldpPrefix.equals(newpPrefix)) {
         innerVect = new Vector();
         innerVect.add("Pager Number Prefix");
         innerVect.add(oldpPrefix);
         innerVect.add(newpPrefix);
         modifiedInfo.add(innerVect);
      }

      if (!oldpPostfix.equals(newpPostfix)) {
         innerVect = new Vector();
         innerVect.add("Pager Number Postfix");
         innerVect.add(oldpPostfix);
         innerVect.add(newpPostfix);
         modifiedInfo.add(innerVect);
      }

      LogManager.log("modifiedInfo " + modifiedInfo);
      StringBuffer buffer = new StringBuffer();
      buffer.append("<HTML>").append("<head>").append("<link type=\"text/css\" rel=\"stylesheet\" href=\"assets/css/styles.css\">").append("</head>").append("<body>").append("The following information(s) is(are) modified for <b>" + newLastName + "," + newFirstName + "  </b>").append("<br><br>").append("<table class=\"align_center employee_info_table\">").append("<thead>").append("<tr>").append("<th class=\"table_header_color align_center td_width5\">Name Of Information</th>").append("<th class=\"table_header_color align_center td_width5\">Old Information</th>").append("<th class=\"table_header_color align_center td_width5\">Modified Information</th>").append("</tr>").append("</thead>").append("<tbody>");
      if (modifiedInfo.size() > 0) {
         for(int i = 0; i < modifiedInfo.size(); ++i) {
            innerVect = (Vector)modifiedInfo.get(i);
            buffer.append("<tr>").append("<td>").append(innerVect.get(0)).append("</td>").append("<td>").append(innerVect.get(1)).append("</td>").append("<td>").append(innerVect.get(2)).append("</td>").append("</tr>");
         }
      }

      buffer.append("</tbody>").append("</table>").append("</body>").append("</HTML>");
      if (modifiedInfo.size() > 0) {
         try {
            this.sendExport(newLastName + "," + newFirstName, buffer.toString());
         } catch (Exception var57) {
            LogManager.log("Exception in Sending Mail" + var57);
         }
      }

      LogManager.log("Buffer " + buffer.toString());
   }

   public void sendExport(String empName, String bodyText) throws Exception {
      try {
         Properties props = new Properties(System.getProperties());
         javax.mail.Session mailSession = javax.mail.Session.getDefaultInstance(props, (Authenticator)null);
         Message message = new MimeMessage(mailSession);
         String sender = System.getProperty("europa.mail.sender");
         Address fromAddr = new InternetAddress(sender);
         message.setFrom(fromAddr);
         String toEmail = System.getProperty("europa.EmpinfochangeAlertReceivers");
         StringTokenizer st = new StringTokenizer(toEmail, ",");
         Address[] toAddrs = new InternetAddress[st.countTokens()];

         for(int i = 0; st.hasMoreTokens(); toAddrs[i++] = new InternetAddress(st.nextToken())) {
         }

         message.setRecipients(RecipientType.TO, toAddrs);
         StringBuffer subject = new StringBuffer("Employee Information Modification Alert -" + empName);
         message.setSubject(subject.toString());
         message.setSentDate(new Date());
         Multipart mp = new MimeMultipart();
         mp.addBodyPart(this.getBodyContent(bodyText));
         message.setContent(mp);
         Transport.send(message);
      } catch (AddressException var14) {
         throw new Exception(var14.toString());
      } catch (MessagingException var15) {
         throw new Exception(var15.toString());
      }
   }

   protected BodyPart getBodyContent(String bodyText) throws MessagingException {
      MimeBodyPart part = new MimeBodyPart();
      part.setContent(bodyText + this.getConfidentialMsgHTML(), "text/html");
      return part;
   }

   protected String getConfidentialMsgHTML() {
      String confid_notice = System.getProperty("europa.confidentiality.notice");
      return "<br><br><br><br><br><br> <p style='font-size:x-small; font-family: Tahoma;' align='justify'> <font color='#0000ff'><b>" + confid_notice + "</p>";
   }
}
