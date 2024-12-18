package com.ltts.llc.dps.eserv.module;

import com.ltts.llc.dps.eserv.europa.exceptions.EuropaException;
import com.ltts.llc.dps.eserv.europa.log.LogManager;
import com.ltts.llc.dps.eserv.europa.logic.DataLogic;
import com.ltts.llc.dps.eserv.europa.module.Module;
import com.ltts.llc.dps.eserv.europa.security.SecurityManager;
import java.io.FileInputStream;
import java.sql.Connection;
import java.util.Properties;
import java.util.Vector;

public class ManageDepartmentDisplayLogic {
   private Properties sqlProps = null;
   private String NotAssociated = null;
   private String FWDepartMentList = null;
   private String DepIdList = null;

   public ManageDepartmentDisplayLogic() {
      this.sqlProps = new Properties();

      try {
         this.sqlProps.load(new FileInputStream(System.getProperty("europa.database.sqlpath") + "sql_departments.properties"));
      } catch (Exception var2) {
         LogManager.log(" Properties Error :" + var2);
      }

   }

   public String getNotAssociated() {
      return this.NotAssociated;
   }

   private void setNotAssociated(String notAssociated) {
      this.NotAssociated = notAssociated;
   }

   public String getFWDepartMentList() {
      return this.FWDepartMentList;
   }

   private void setFWDepartMentList(String departMentList) {
      this.FWDepartMentList = departMentList;
   }

   public String getDepIdList() {
      return this.DepIdList;
   }

   private void setDepIdList(String depIdList) {
      this.DepIdList = depIdList;
   }

   public String buildAllDepartment() {
      Connection conn = driverConnection();
      new Vector();
      StringBuffer departmentNameList = new StringBuffer();
      boolean shadeFlage = false;
      StringBuffer departList = new StringBuffer();
      StringBuffer depIdList = new StringBuffer();
      StringBuffer depCode = new StringBuffer();

      try {
         String sql = this.sqlProps.getProperty("SelectAllDepartments");
         Vector result = SecurityManager.executeQuery(conn, sql);
         departList.append("<table class=contacts>").append("<tr><td class=contactDept> Location Name </td>").append("<td class=contactDept>Item Code  </td>").append("<td class=contactDept>Avail Billable Hours  </td>").append("<td class=contactDept> Status                        </td>").append("<td class=contactDept> Save Changes </td></tr>");

         for(int i = 0; i < result.size(); ++i) {
            Vector row = (Vector)result.get(i);
            String depId = row.get(0).toString();
            String depName = row.get(1).toString();
            int isAtcive = Integer.parseInt(row.get(2).toString());
            String code = row.get(3).toString();
            String availBillHrs = row.get(4).toString();
            departmentNameList.append(depName).append("#");
            depCode.append(depId + "#").append(code).append(":");
            LogManager.log("Department Name :" + depName + "code:" + code);
            depIdList.append(depId).append(":");
            departList.append("<tr>").append("<td class=\"style1\">").append("<input type=\"text\" id=\"" + depId + "\" name=\"txt" + depId + "\"  class=\"form-control\" value=\"" + depName.toString() + "\" size=25 maxlength=\"49\">").append("</td>");
            departList.append("<td class=\"style1\">").append("<input type=\"text\" id=\"cd" + depId + "\" name=\"cd" + depId + "\" class=\"form-control\" value=\"" + code + "\" size=9 maxlength=\"9\">").append("</td>");
            departList.append("<td class=\"style1\">").append("<input type=\"text\" id=\"hrs" + depId + "\" name=\"hrs" + depId + "\" class=\"form-control\" value=\"" + availBillHrs + "\" size=9 maxlength=\"5\">").append("</td>");

            if (isAtcive == 1) {
               departList.append("<td class=\"contact" + (shadeFlage = !shadeFlage ? false : true) + "\">")
               .append("<div class=\"form-check form-check-inline\">")
                       .append("<INPUT class=\"mx-2 form-check form-check-inline form-check-input\" type=radio value=\"1\"  name=\"rd" + depId + "\" id=\"" + depId + "\" checked >Active |").
                       append("<INPUT class=\"form-check form-check-inline form-check-input\" type=radio value=\"0\"  name=\"rd" + depId + "\" id=\"" + depId + "\">In Active |");
            } else {
               departList.append("<td class=\"contact" + (shadeFlage = !shadeFlage ? false : true) + "\">").append("<INPUT class=\"mx-2 form-check form-check-inline form-check-input\" type=radio value=\"1\"  name=\"rd" + depId + "\" id=\"" + depId + "\"  >Active |").append("<INPUT class=\"form-check form-check-inline form-check-input\" type=radio value=\"0\"  name=\"rd" + depId + "\" id=\"" + depId + "\" checked>In Active |");
            }

            departList.append("<INPUT class=\"form-check form-check-inline form-check-input\" type=radio value=\"2\"  name=\"rd" + depId + "\" id=\"rd" + depId + "\">Delete ").
                    append("</td>");
            departList.append("<td class=\"contact" + (shadeFlage = shadeFlage == false ? false : true) + "\">").append("<input type=\"button\" id=" + depId + " name=\"sav" + depId + "\" class=\"btn btn-primary\" value=\"Submit\" size=\"9\"  onClick=\"UpdateLocation(" + depId + ");\">").append("</td> </tr>");
            shadeFlage = false;
            departList.append("<tr class=\"blankhead\"><td colspan=6 ><div id=\"rem" + depId + "\"><br></div><font color=red><div align=\"Center\" id=\"disp" + depId + "\" name=\"disp" + depId + "\"></div></font></td></tr>");
            shadeFlage = !shadeFlage;
         }

         String sql1 = this.sqlProps.getProperty("SelectDepartmentNotAssigned");
         result = SecurityManager.executeQuery(conn, sql1);
         StringBuffer notAssociateId = new StringBuffer();
         LogManager.log("Not Associated :" + result);

         for(int i = 0; i < result.size(); ++i) {
            Vector row = (Vector)result.get(i);
            String depId = row.get(0).toString();
            notAssociateId.append(depId).append(":");
         }

         LogManager.log(" Final String  :" + notAssociateId.toString());
         this.setDepIdList(depIdList.toString());
         this.setFWDepartMentList(departmentNameList.toString());
         this.setNotAssociated(notAssociateId.toString());
         conn.close();
      } catch (Exception var16) {
         LogManager.log("Sql Propertiy reader :" + var16);
      }

      return departList.toString();
   }

   private static Connection driverConnection() {
      Connection conn = null;

      try {
         DataLogic datalogic = null;
         Module module = new Module("departments");
         Departmentbusinesslogic aeb = new Departmentbusinesslogic();
         DepartmentDatalogic aed = new DepartmentDatalogic();
         DepartmentDisplaylogic aedi = new DepartmentDisplaylogic();
         module.setLogic(aeb, aed, aedi);
         datalogic = module.getDataLogic();
         conn = datalogic.openConnection();
      } catch (EuropaException var6) {
      }

      return conn;
   }
}
