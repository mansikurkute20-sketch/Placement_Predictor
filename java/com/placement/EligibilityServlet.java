package com.placement;

import com.google.gson.Gson;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;

@WebServlet("/EligibilityServlet")
public class EligibilityServlet extends HttpServlet {

    // Called by student dashboard JS: GET /EligibilityServlet?action=check_all
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().print("[]");
            return;
        }

        String action = request.getParameter("action");
        if ("check_all".equals(action)) {
            handleCheckAll(request, response);
        } else if ("student_info".equals(action)) {
            handleStudentInfo(request, response);
        }
    }

    private void handleStudentInfo(HttpServletRequest request,
                                   HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> info = new HashMap<>();
        info.put("name",     session.getAttribute("user_name"));
        info.put("branch",   session.getAttribute("branch"));
        info.put("cgpa",     session.getAttribute("cgpa"));
        info.put("backlogs", session.getAttribute("backlogs"));
        response.getWriter().print(new Gson().toJson(info));
    }

    private void handleCheckAll(HttpServletRequest request,
                                HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        int    studentId = (int)    session.getAttribute("user_id");
        double sCgpa     = (double) session.getAttribute("cgpa");
        int    sBacklogs = (int)    session.getAttribute("backlogs");
        String sBranch   = (String) session.getAttribute("branch");

        List<Map<String, Object>> results = new ArrayList<>();

        Connection conn = DBConnection.getConnection();
        try {
            String sql = "SELECT c.company_id, c.company_name, c.role, c.package_lpa, " +
                         "       e.min_cgpa, e.max_backlogs " +
                         "FROM companies c " +
                         "JOIN eligibility_criteria e ON c.company_id = e.company_id " +
                         "ORDER BY c.visit_date";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    int    compId  = rs.getInt("company_id");
                    double minCgpa = rs.getDouble("min_cgpa");
                    int    maxBack = rs.getInt("max_backlogs");

                    // Check branch
                    PreparedStatement psBranch = conn.prepareStatement(
                        "SELECT 1 FROM allowed_branches WHERE company_id = ? AND branch = ?");
                    psBranch.setInt(1, compId);
                    psBranch.setString(2, sBranch);
                    boolean branchOk = psBranch.executeQuery().next();

                    boolean eligible = (sCgpa >= minCgpa) && (sBacklogs <= maxBack) && branchOk;

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("companyName", rs.getString("company_name"));
                    item.put("role",        rs.getString("role"));
                    item.put("package",     rs.getDouble("package_lpa"));
                    item.put("minCgpa",     minCgpa);
                    item.put("maxBacklogs", maxBack);
                    item.put("eligible",    eligible);
                    item.put("reason",      getFailReason(sCgpa, minCgpa, sBacklogs, maxBack, branchOk, sBranch));
                    results.add(item);
                }
            }

            out.print(new Gson().toJson(results));

        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]");
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
            out.flush();
        }
    }

    private String getFailReason(double cgpa, double minCgpa,
                                  int backlogs, int maxBack,
                                  boolean branchOk, String branch) {
        if (cgpa < minCgpa) return "CGPA " + cgpa + " < " + minCgpa + " required";
        if (backlogs > maxBack) return backlogs + " backlogs (max " + maxBack + " allowed)";
        if (!branchOk) return "Branch " + branch + " not eligible";
        return "";
    }
}
