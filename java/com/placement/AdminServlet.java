package com.placement;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/AdminServlet")
public class AdminServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !"admin".equals(session.getAttribute("role"))) {
            response.sendRedirect("index.html");
            return;
        }

        String action = request.getParameter("action");
        Connection conn = DBConnection.getConnection();

        try {
            if ("set_criteria".equals(action)) {
                int    companyId  = Integer.parseInt(request.getParameter("company_id"));
                double minCgpa    = Double.parseDouble(request.getParameter("min_cgpa"));
                int    maxBacklogs = Integer.parseInt(request.getParameter("max_backlogs"));
                String[] branches = request.getParameterValues("branches");

                // Upsert eligibility criteria
                String upsert = "INSERT INTO eligibility_criteria (company_id, min_cgpa, max_backlogs) " +
                                "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE min_cgpa=?, max_backlogs=?";
                PreparedStatement psUps = conn.prepareStatement(upsert);
                psUps.setInt(1, companyId);
                psUps.setDouble(2, minCgpa);
                psUps.setInt(3, maxBacklogs);
                psUps.setDouble(4, minCgpa);
                psUps.setInt(5, maxBacklogs);
                psUps.executeUpdate();

                // Replace allowed branches
                PreparedStatement psDel = conn.prepareStatement(
                    "DELETE FROM allowed_branches WHERE company_id = ?");
                psDel.setInt(1, companyId);
                psDel.executeUpdate();

                if (branches != null) {
                    PreparedStatement psIns = conn.prepareStatement(
                        "INSERT IGNORE INTO allowed_branches (company_id, branch) VALUES (?, ?)");
                    for (String branch : branches) {
                        psIns.setInt(1, companyId);
                        psIns.setString(2, branch);
                        psIns.executeUpdate();
                    }
                }

                response.sendRedirect("index.html?login_success=true&role=admin&msg=criteria_updated");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("index.html?login_success=true&role=admin&error=db_error");
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }
    }
}
