package com.placement;

import com.google.gson.Gson;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;

class CompanyData {
    public int id;
    public String name;
    public String role;
    public double package_lpa;
    public String visit_date;
    public double min_cgpa;
    public int max_backlogs;
    public List<String> allowed_branches = new ArrayList<>();

    public CompanyData(int id, String name, String role,
                       double package_lpa, String visit_date) {
        this.id = id; this.name = name; this.role = role;
        this.package_lpa = package_lpa; this.visit_date = visit_date;
    }
}

class AdminResponse {
    public List<CompanyData> companies;
    public int studentCount;
}

@WebServlet("/CompanyServlet")
public class CompanyServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("list_json".equals(action)) {
            handleListJson(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Admin-only: add a new company
        HttpSession session = request.getSession(false);
        if (session == null || !"admin".equals(session.getAttribute("role"))) {
            response.sendRedirect("index.html?error=unauthorized");
            return;
        }

        String action = request.getParameter("action");
        if ("add_company".equals(action)) {
            handleAddCompany(request, response);
        } else if ("delete_company".equals(action)) {
            handleDeleteCompany(request, response);
        }
    }

    private void handleListJson(HttpServletRequest request,
                                HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        AdminResponse data = new AdminResponse();
        data.companies = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) throw new Exception("DB connection failed");

            // Companies + criteria (left join so companies without criteria still show)
            String sql = "SELECT c.company_id, c.company_name, c.role, c.package_lpa, c.visit_date, " +
                         "       COALESCE(e.min_cgpa, 0) AS min_cgpa, " +
                         "       COALESCE(e.max_backlogs, 0) AS max_backlogs " +
                         "FROM companies c " +
                         "LEFT JOIN eligibility_criteria e ON c.company_id = e.company_id " +
                         "ORDER BY c.visit_date";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    CompanyData cd = new CompanyData(
                        rs.getInt("company_id"),
                        rs.getString("company_name"),
                        rs.getString("role"),
                        rs.getDouble("package_lpa"),
                        String.valueOf(rs.getDate("visit_date"))
                    );
                    cd.min_cgpa = rs.getDouble("min_cgpa");
                    cd.max_backlogs = rs.getInt("max_backlogs");
                    data.companies.add(cd);
                }
            }

            // Fetch allowed branches for each company
            for (CompanyData cd : data.companies) {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT branch FROM allowed_branches WHERE company_id = ?");
                ps.setInt(1, cd.id);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) cd.allowed_branches.add(rs.getString("branch"));
            }

            // Student count
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM student")) {
                if (rs.next()) data.studentCount = rs.getInt(1);
            }

            out.print(new Gson().toJson(data));

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
            e.printStackTrace();
        } finally {
            out.flush();
        }
    }

    private void handleAddCompany(HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {
        Connection conn = DBConnection.getConnection();
        try {
            String name      = request.getParameter("company_name").trim();
            String role      = request.getParameter("role").trim();
            double pkg       = Double.parseDouble(request.getParameter("package_lpa"));
            String date      = request.getParameter("visit_date");
            double minCgpa   = Double.parseDouble(request.getParameter("min_cgpa"));
            int    maxBack   = Integer.parseInt(request.getParameter("max_backlogs"));
            String[] branches = request.getParameterValues("branches");

            // Insert company
            PreparedStatement psComp = conn.prepareStatement(
                "INSERT INTO companies (company_name, role, package_lpa, visit_date) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            psComp.setString(1, name);
            psComp.setString(2, role);
            psComp.setDouble(3, pkg);
            psComp.setString(4, date);
            psComp.executeUpdate();

            int companyId;
            try (ResultSet gk = psComp.getGeneratedKeys()) {
                gk.next();
                companyId = gk.getInt(1);
            }

            // Criteria
            PreparedStatement psCrit = conn.prepareStatement(
                "INSERT INTO eligibility_criteria (company_id, min_cgpa, max_backlogs) VALUES (?,?,?)");
            psCrit.setInt(1, companyId);
            psCrit.setDouble(2, minCgpa);
            psCrit.setInt(3, maxBack);
            psCrit.executeUpdate();

            // Branches
            if (branches != null) {
                PreparedStatement psBranch = conn.prepareStatement(
                    "INSERT IGNORE INTO allowed_branches (company_id, branch) VALUES (?,?)");
                for (String b : branches) {
                    psBranch.setInt(1, companyId);
                    psBranch.setString(2, b);
                    psBranch.executeUpdate();
                }
            }

            response.sendRedirect("index.html?login_success=true&role=admin&msg=company_added");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("index.html?login_success=true&role=admin&error=add_failed");
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }
    }

    private void handleDeleteCompany(HttpServletRequest request,
                                     HttpServletResponse response) throws IOException {
        Connection conn = DBConnection.getConnection();
        try {
            int companyId = Integer.parseInt(request.getParameter("company_id"));
            PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM companies WHERE company_id = ?");
            ps.setInt(1, companyId);
            ps.executeUpdate();
            response.sendRedirect("index.html?login_success=true&role=admin&msg=company_deleted");
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("index.html?login_success=true&role=admin&error=delete_failed");
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }
    }
}
