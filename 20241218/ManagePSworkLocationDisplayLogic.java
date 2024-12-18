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

public class ManagePSworkLocationDisplayLogic {
   private Properties sqlProps = null;
   private String PSLocCodeList = null;
   private String FWPSLocationList = null;
   private String LocIdList = null;

   public ManagePSworkLocationDisplayLogic() {
      this.sqlProps = new Properties();

      try {
         this.sqlProps.load(new FileInputStream(System.getProperty("europa.database.sqlpath") + "sql_PSworkLocations.properties"));
      } catch (Exception var2) {
         LogManager.log(" Properties Error :" + var2);
      }

   }

   public String getPSLocationsCode() {
      return this.PSLocCodeList;
   }

   private void setPSLocationsCode(String locCode) {
      this.PSLocCodeList = locCode;
   }

   public String getPSLocationsList() {
      return this.FWPSLocationList;
   }

   private void setPSLocationsList(String departMentList) {
      this.FWPSLocationList = departMentList;
   }

   public String getLocIdList() {
      return this.LocIdList;
   }

   private void setLocIdList(String locIdList) {
      this.LocIdList = locIdList;
   }

   public String buildAllLocations() {
      Connection conn = driverConnection();
      new Vector();
      StringBuffer locationNameList = new StringBuffer();
      new StringBuffer();
      StringBuffer psLocationList = new StringBuffer();
      int shadeFlage = 0;
      StringBuffer locationList = new StringBuffer();
      StringBuffer locIdList = new StringBuffer();
      StringBuffer locationCode = new StringBuffer();

      try {
         String sql = this.sqlProps.getProperty("SelectAllPSworkLocations");
         Vector result = SecurityManager.executeQuery(conn, sql);
         locationList.append("<table class=\"table table-bordered\"")
            .append("<thead class=\"thead-dark\">\n")
            .append("<tr>          <th>Location Name</th>\n").append("<th> PS Location Code  </th>")
            .append("<th >CP Location Code </th>").append("<th> Save Changes </th></tr></thead>")
                 .append("<tbody>");

         for(int i = 0; i < result.size(); ++i) {
            Vector row = (Vector)result.get(i);
            String locId = row.get(0).toString();
            String locName = row.get(1).toString();
            String code = row.get(2).toString();
            String psLocation = row.get(3).toString();
            locationNameList.append(locId).append("_").append(locName).append("#");
            locationCode.append(locId).append("_").append(code).append(":");
            psLocationList.append(locId).append("_").append(psLocation).append("#");
            LogManager.log("Location Name :" + locName + " Location code:" + code + "PS Location" + psLocation);
            locIdList.append(locId).append(":");
            locationList.append("<tr>").append("<td class=\"style1\">").append("<input type=\"text\" id=\"" + locId + "\" name=\"txt" + locId + "\"  class=\"form-control \" value=\"" + locName.toString() + "\" size=30 maxlength=\"49\" >").append("</td>");
            locationList.append("<td class=\"style1\">").append("<input type=\"text\" id=\"cd" + locId + "\" name=\"cd" + locId + "\" class=\"form-control \" value=\"" + code + "\" size=9 maxlength=\"9\" >").append("</td>");
            locationList.append("<td class=\"style1\">").append("<input type=\"text\" id=\"cp" + locId + "\" name=\"cp" + locId + "\" class=\"form-control \" value=\"" + psLocation + "\" size=40 maxlength=\"49\" >").append("</td>");

            locationList.append("<td class=\"contact" + (shadeFlage = shadeFlage == 0 ? 0 : 1) + "\">").append("<input type=\"button\" id=" + locId + " name=\"sav" + locId + "\" class=\"btn btn-primary p-2 m-2\" value=\"Submit\" size=\"18\" onClick=\"updateLocation(" + locId + ");\" >").append("</td> </tr>");
            if(shadeFlage == 0)
               shadeFlage = 1;
            locationList.append("<tr class=\"blankhead\"><td colspan=3 ><div id=\"rem" + locId + "\"><br></div><font color=red><div align=\"Center\" id=\"disp" + locId + "\" name=\"disp" + locId + "\"></div></font></td></tr>");
            shadeFlage = shadeFlage == 1 ? 0 : 1;
         }

         this.setLocIdList(locIdList.toString());
         this.setPSLocationsList(locationNameList.toString());
         this.setPSLocationsCode(locationCode.toString());
         LogManager.log("locIdList.toString()");
         LogManager.log("locationNameList.toString()");
         conn.close();
      } catch (Exception var17) {
         LogManager.log("Sql Propertiy reader :" + var17);
      }

      return locationList.toString();
   }

   private static Connection driverConnection() {
      Connection conn = null;

      try {
         DataLogic datalogic = null;
         Module module = new Module("PS_WORKLOCATIONS");
         PSWorkLocationBusinessLogic aeb = new PSWorkLocationBusinessLogic();
         PSWorkLocationDataLogic aed = new PSWorkLocationDataLogic();
         PSWorkLocationDisplayLogic aedi = new PSWorkLocationDisplayLogic();
         module.setLogic(aeb, aed, aedi);
         datalogic = module.getDataLogic();
         conn = datalogic.openConnection();
      } catch (EuropaException var6) {
      }

      return conn;
   }
}
