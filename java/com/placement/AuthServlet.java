package com.placement;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/AuthServlet")
public class AuthServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        Connection conn = DBConnection.getConnection();

        if (conn == null) {
            response.sendRedirect("index.html?error=db_error");
            return;
        }

        try {
            if ("register".equals(action)) {
                String username = request.getParameter("username").trim();
                String fullName = request.getParameter("full_name").trim();
                String branch   = request.getParameter("branch");
                double cgpa     = Double.parseDouble(request.getParameter("cgpa"));
                int    backlogs = Integer.parseInt(request.getParameter("backlogs"));
                String password = request.getParameter("password");

                // Check duplicate username
                PreparedStatement psCheck = conn.prepareStatement(
                    "SELECT student_id FROM student WHERE username = ?");
                psCheck.setString(1, username);
                if (psCheck.executeQuery().next()) {
                    response.sendRedirect("index.html?error=username_taken&page=register");
                    return;
                }

                String query = "INSERT INTO student (full_name, branch, cgpa, backlogs, username, password) " +
                               "VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setString(1, fullName);
                ps.setString(2, branch);
                ps.setDouble(3, cgpa);
                ps.setInt(4, backlogs);
                ps.setString(5, username);
                ps.setString(6, password);
                ps.executeUpdate();
                response.sendRedirect("index.html?msg=reg_success");

            } else if ("student_login".equals(action)) {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM student WHERE username = ? AND password = ?");
                ps.setString(1, request.getParameter("username"));
                ps.setString(2, request.getParameter("password"));
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    HttpSession session = request.getSession(true);
                    session.setAttribute("user_id",  rs.getInt("student_id"));
                    session.setAttribute("user_name",rs.getString("full_name"));
                    session.setAttribute("branch",   rs.getString("branch"));
                    session.setAttribute("cgpa",     rs.getDouble("cgpa"));
                    session.setAttribute("backlogs", rs.getInt("backlogs"));
                    session.setAttribute("role",     "student");
                    response.sendRedirect("index.html?login_success=true&role=student");
                } else {
                    response.sendRedirect("index.html?error=invalid&page=student-login");
                }

            } else if ("admin_login".equals(action)) {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM admins WHERE username = ? AND password = ?");
                ps.setString(1, request.getParameter("username"));
                ps.setString(2, request.getParameter("password"));
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    HttpSession session = request.getSession(true);
                    session.setAttribute("role", "admin");
                    response.sendRedirect("index.html?login_success=true&role=admin");
                } else {
                    response.sendRedirect("index.html?error=invalid&page=admin-login");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("index.html?error=server_error");
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }
    }
}
