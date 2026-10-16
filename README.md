# PlacementPredictor – Setup & Run Guide

## Project Structure
```
PlacementPredictor/
├── database_setup.sql                  ← Run this FIRST in MySQL
├── src/main/
│   ├── java/com/placement/
│   │   ├── DBConnection.java           ← DB credentials (edit PASS here)
│   │   ├── AuthServlet.java            ← Login / Register
│   │   ├── AdminServlet.java           ← Set eligibility criteria
│   │   ├── CompanyServlet.java         ← List / Add / Delete companies
│   │   └── EligibilityServlet.java     ← Student eligibility check (JSON)
│   └── webapp/
│       ├── index.html                  ← Full SPA frontend
│       └── WEB-INF/
│           ├── web.xml
│           └── lib/
│               ├── gson-2.10.1.jar
│               └── mysql-connector-j-9.6.0.jar
```

## Prerequisites
- Java JDK 11+
- Apache Tomcat 9 or 10
- MySQL 8.x
- Eclipse IDE for Enterprise Java (or IntelliJ with Tomcat plugin)

---

## Step 1 – Set Up the Database

```bash
mysql -u root -p < database_setup.sql
```

Or paste the contents of `database_setup.sql` into MySQL Workbench and run it.

This creates **placement_db** with tables:
- `admins`
- `student`
- `companies`
- `eligibility_criteria`
- `allowed_branches`

And seeds sample data (7 companies + admin account: `admin` / `admin123`).

---

## Step 2 – Update DB Password

Open `src/main/java/com/placement/DBConnection.java` and change:

```java
private static final String PASS = "root"; // ← your MySQL root password
```

---

## Step 3 – Import into Eclipse

1. File → Import → Existing Projects into Workspace
2. Select `PlacementPredictor` folder → Finish
3. Right-click project → Properties → Java Build Path → Libraries
   - Confirm `gson-2.10.1.jar` and `mysql-connector-j-9.6.0.jar` are listed under WEB-INF/lib
4. Right-click project → Run As → Run on Server → Select Tomcat

---

## Step 4 – Run

Visit: `http://localhost:8080/PlacementPredictor/`

### Default Credentials
| Role  | Username | Password |
|-------|----------|----------|
| Admin | admin    | admin123 |

---

## Feature Summary

### Student Features
- Register with branch, CGPA, backlogs
- Login and view personalised eligibility report
- Filter by Eligible / Not Eligible
- See reason for ineligibility per company

### Admin Features
- View all companies + registered student count
- **Add Company** with name, role, package, visit date, min CGPA, max backlogs, allowed branches
- **Delete Company** (cascade deletes criteria + branches)
- **Set/Update Criteria** for existing companies
- DB status indicator

---

## Database Tables

```sql
admins              (admin_id, username, password)
student             (student_id, full_name, branch, cgpa, backlogs, username, password)
companies           (company_id, company_name, role, package_lpa, visit_date)
eligibility_criteria(criteria_id, company_id, min_cgpa, max_backlogs)
allowed_branches    (id, company_id, branch)
```
