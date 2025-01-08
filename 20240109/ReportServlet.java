package com.ltts.llc.dps.eserv.platform;

import com.ltts.llc.dps.eserv.reports.Report;
import com.ltts.llc.dps.eserv.europa.log.LogManager;
import  com.ltts.llc.dps.eserv.europa.session.Session;
import  com.ltts.llc.dps.eserv.europa.session.SessionManager;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ReportServlet extends HttpServlet {
   public void init(ServletConfig config) throws ServletException {
      super.init(config);
   }

   public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      this.doGet(request, response);
   }

   public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      LogManager.log("/servlets/ReportServlet.doGet()");
      String sessionId = request.getParameter("SessionID");
      Session session = SessionManager.findSession(sessionId);
      if (session != null) {
         session.parseQueryString(request);
         Report report = (Report)session.getAttribute("UserReport");
         response.setContentType("text/html");
         PrintWriter out = response.getWriter();
         if (request.getParameter("doTask") != null) {
            try {
               report.generate(sessionId);
            } catch (Exception var8) {
               report.setErrorMsg(var8.getMessage());
            }
         }

         out.println("<html>");
         if (report.isGenerating()) {
            out.println("<head>");
            StringBuffer url = new StringBuffer("http://" + System.getProperty("europa.app.host") + "/servlets/ReportServlet");
            url.append("?SessionID=" + sessionId);
            LogManager.log("ReportServlet : redirecting to : " + url);
            out.println("<meta http-equiv=\"refresh\" content=\"5;URL=" + url + "\">\n");
            out.println("</head><body bgcolor=\"#FFFFFF\" text=\"#000000\" link=\"#0000ff\" vlink=\"#800080\" alink=\"#ff0000\">");
            out.println("<br>Generating Report... Please Wait.");
            out.println("<br>");
         } else if (report.hasContent() && report.isScheduler()) {
            LogManager.log("hasContent Else IF");
            session.setAttribute("FWCommonOutputContent", report.getCommonOutputContent());
            session.setAttribute("FWFileSize", report.getContentSize());
            session.setAttribute("FWReportName", report.getReportName());
            response.sendRedirect("/jsp/reports/" + report.getOutputJSPFileName() + "?FWSessionId=" + sessionId);
         } else if (report.hasContent()) {
            LogManager.log("hasContent Else IF");
            out.println(report.getContent());
         } else if (report.hasError()) {
            out.println("<body bgcolor=\"#FFFFFF\" text=\"#000000\" link=\"#0000ff\" vlink=\"#800080\" alink=\"#ff0000\">");
            out.println("An unknown error occured...");
            out.println("<br><br>");
            out.println(report.getErrorMsg());
         } else {
            out.println("<body bgcolor=\"#FFFFFF\" text=\"#000000\" link=\"#0000ff\" vlink=\"#800080\" alink=\"#ff0000\">");
            out.println("An unknown error occured...");
         }

         out.println("</body></html>");
         out.close();
      } else {
         SessionManager.redirectToSessionExpiredPage(request, response);
      }

   }
}
