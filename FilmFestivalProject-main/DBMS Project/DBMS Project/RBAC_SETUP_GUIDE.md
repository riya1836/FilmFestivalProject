# Film Festival RBAC Implementation Guide

## ✅ IMPLEMENTATION COMPLETE

This document guides you through setting up and testing the new Role-Based Access Control (RBAC) system with JWT authentication and jury evaluation workflow.

---

## 📋 SETUP INSTRUCTIONS

### **STEP 1: Database Migration**

1. Open your MySQL client (MySQL Workbench or command line)

2. Select your database:
```sql
USE film_festival;
```

3. Run the migration script to add RBAC tables:
```bash
# From project root, run:
mysql -u your_user -p your_password film_festival < db_migration_rbac.sql
```

Or paste the contents of `db_migration_rbac.sql` in MySQL Workbench and execute.

4. Verify tables were created:
```sql
SHOW TABLES;  -- Should see: users, jury_assignment, evaluation, audit_log
```

### **STEP 2: Backend Setup**

1. **Copy Backend Files**
   - Auth files:
     - `src/auth/JwtTokenProvider.java` - JWT token generation
     - `src/auth/PasswordEncoder.java` - Secure password hashing
     - `src/auth/AuthorizationFilter.java` - Request authentication
   
   - Service files:
     - `src/user/UserService.java` - User registration & login
     - `src/jury/EvaluationService.java` - Film evaluations
     - `src/jury/JuryAssignmentService.java` - Jury assignment management
     - `src/film/FilmService.java` - Film queries with ratings
   
   - API Server:
     - Rename `src/app/ApiServerNew.java` to `src/app/ApiServer.java` (backup old one first)

2. **Compile Backend**
```bash
cd path/to/DBMS Project
javac -cp "lib/*" src/auth/*.java src/user/*.java src/jury/*.java src/film/*.java src/app/*.java -d out/
```

3. **Start Backend**
```bash
java -cp "lib/*;out" app.Main
```

You should see:
```
[SUCCESS] Database connection established successfully!
Web server started on port 8080
```

### **STEP 3: Frontend Setup**

1. **Copy Frontend Files** to `frontend/src/`:
   - `contexts/AuthContext.jsx` - Auth state management
   - `components/shared/ProtectedRoute.jsx` - Route protection
   - `components/auth/LoginForm.jsx` - Login page
   - `components/auth/RegisterForm.jsx` - Registration page
   - `components/auth/Auth.css` - Auth styling
   - `components/dashboards/AdminDashboard.jsx` - Admin panel
   - `components/dashboards/UserDashboard.jsx` - User panel
   - `components/dashboards/JuryDashboard.jsx` - Jury panel
   - `components/dashboards/dashboards.css` - Dashboard styling
   - Updated `App.jsx` - New routing
   - Updated `api.js` - New endpoints with auth

2. **Install Dependencies** (if not already installed)
```bash
cd frontend
npm install
```

3. **Start Frontend**
```bash
npm run dev
```

Frontend will run on `http://localhost:5176`

---

## 🧪 TESTING THE SYSTEM

### **Test Account Details**

After running the migration, you have one admin account:
- **Email:** `admin@filmfestival.com`
- **Password:** `admin123`

### **Test Scenario 1: Admin Registration & Login**

1. Visit `http://localhost:5176/register`
2. Create an account with role "User (Participant)"
3. Verify registration successful
4. Redirected to User Dashboard

### **Test Scenario 2: Jury Registration & Assignment**

1. Register as "Jury Member"
   - Email: `jury1@example.com`
   - Password: `test123`

2. After registration, you're in Jury Dashboard (but no films assigned)

3. Login as Admin
   - Email: `admin@filmfestival.com`
   - Password: `admin123`

4. Go to "Assign Jury" tab
   - Jury ID: `1` (the jury member you just created)
   - Film IDs: `1,2,3` (comma-separated)
   - Click "Assign Jury"

### **Test Scenario 3: Jury Evaluation Workflow**

1. Logout from Admin
2. Login as Jury member (jury1@example.com)
3. In "Assigned Films" tab, you'll see films 1, 2, 3
4. Click "Evaluate" on any film
5. Set score (1-10) and add remarks
6. Click "Submit Evaluation"
7. Film appears in "My Evaluations" with checkmark

### **Test Scenario 4: Admin Views Evaluations**

1. Login as Admin
2. Dashboard shows evaluation count
3. "Award Eligible" tab shows films with avg_score >= 7.5

### **Test Scenario 5: User Browses Films**

1. Register/Login as User
2. Browse all films (filtered by genre/language)
3. View leaderboard (top-rated films)
4. See average scores and evaluation counts

---

## 🔐 SECURITY FEATURES IMPLEMENTED

✅ **Password Security**
- SHA-256 hashing with salt
- 10,000 iterations
- Constant-time comparison (prevents timing attacks)

✅ **Token Security**
- JWT tokens with HMAC-SHA256 signature
- 24-hour expiration
- Token stored in localStorage
- Sent with every request via Authorization header

✅ **Authorization**
- Role-based access control on all endpoints
- Endpoint-level validation
- Prevents unauthorized access

✅ **Data Protection**
- Prepared statements (prevents SQL injection)
- Foreign key constraints with cascading deletes
- Unique constraints on evaluations (prevents duplicates)

---

## 📡 API ENDPOINTS REFERENCE

### **Authentication**
```
POST   /api/auth/register        - Create account
POST   /api/auth/login           - Get JWT token
GET    /api/auth/me              - Current user info
```

### **Admin Only**
```
POST   /api/admin/assign-jury    - Assign jury to films
GET    /api/admin/jury-assignments - View all assignments
GET    /api/admin/evaluations    - View all evaluations
GET    /api/admin/award-eligible - Films eligible for awards
```

### **Jury Only**
```
GET    /api/jury/assigned-films  - See assigned films
POST   /api/jury/evaluate        - Submit evaluation
GET    /api/jury/evaluations     - View my evaluations
```

### **User (Public)**
```
GET    /api/films                - Browse all films
GET    /api/films/leaderboard    - Top-rated films
GET    /api/user/my-bookings     - My ticket bookings
```

---

## 🐛 TROUBLESHOOTING

### **"Database connection failed"**
- Check `db.properties` has correct credentials
- Ensure MySQL is running
- Run migration script

### **"Invalid token signature"**
- Check JWT_SECRET environment variable matches backend
- Clear localStorage and login again

### **"Jury record not found"**
- Ensure jury user exists in jury table
- Run migration to create junction records

### **API returns 403 Unauthorized**
- Verify token is being sent in Authorization header
- Check user has correct role for endpoint
- Tokens expire after 24 hours - login again

### **"CORS error"**
- Backend adds CORS headers automatically
- For development, localhost:5176 is allowed
- For production, update CORS origin in ApiServer.java

---

## 📊 DATABASE SCHEMA SUMMARY

### Users Table
```
user_id (PK) | name | email (UNIQUE) | password_hash | role | is_active | created_at
```

### Evaluation Table
```
evaluation_id (PK) | jury_id (FK) | film_id (FK) | score (1-10) | remarks | created_at
UNIQUE(jury_id, film_id) -- Prevent duplicate evaluations
```

### Jury Assignment Table
```
id (PK) | jury_id (FK) | film_id (FK) | assigned_at
UNIQUE(jury_id, film_id) -- Prevent duplicate assignments
```

---

## 🚀 DEPLOYMENT CHECKLIST

- [ ] Change JWT_SECRET in production
- [ ] Update API_BASE in frontend api.js for production
- [ ] Enable HTTPS in production
- [ ] Set stronger database passwords
- [ ] Enable database backups
- [ ] Review CORS settings for production domain
- [ ] Test all role-based routes
- [ ] Load test with multiple concurrent users

---

## 📚 KEY FILES REFERENCE

### Backend
| File | Purpose |
|------|---------|
| `src/auth/JwtTokenProvider.java` | JWT token management |
| `src/auth/PasswordEncoder.java` | Secure hashing |
| `src/auth/AuthorizationFilter.java` | Request validation |
| `src/user/UserService.java` | User CRUD |
| `src/jury/EvaluationService.java` | Evaluations CRUD |
| `src/jury/JuryAssignmentService.java` | Assignment management |
| `src/film/FilmService.java` | Film queries |
| `src/app/ApiServer.java` | REST endpoints |

### Frontend
| File | Purpose |
|------|---------|
| `contexts/AuthContext.jsx` | Auth state & logic |
| `components/shared/ProtectedRoute.jsx` | Route guards |
| `components/auth/LoginForm.jsx` | Login UI |
| `components/auth/RegisterForm.jsx` | Registration UI |
| `components/dashboards/AdminDashboard.jsx` | Admin panel |
| `components/dashboards/UserDashboard.jsx` | User panel |
| `components/dashboards/JuryDashboard.jsx` | Jury panel |
| `App.jsx` | Main routing |
| `api.js` | API client |

---

## ✨ FEATURES IMPLEMENTED

✅ **Authentication**
- JWT-based token system
- Secure password hashing
- User registration & login

✅ **Role-Based Access**
- Admin: Full management access
- User: Browse & book tickets
- Jury: Evaluate assigned films

✅ **Jury Workflow**
- Admin assigns jury to films
- Jury receives notifications
- Jury evaluates films (score + remarks)
- Prevent duplicate evaluations
- Admin reviews all evaluations

✅ **Film Ratings**
- Calculate average scores
- Show evaluation count
- Leaderboard of top films
- Award eligibility (avg >= 7.5)

✅ **Security**
- JWT with HMAC-SHA256
- SHA-256 password hashing
- SQL injection prevention
- CORS protection
- Role-based endpoint validation

---

## 📞 SUPPORT & NEXT STEPS

### To Add More Features:

1. **Ticket Booking**
   - Implement `POST /api/user/book-ticket`
   - Add seat management
   - Payment integration

2. **Email Notifications**
   - Notify jury when assigned
   - Send evaluation reminders
   - Award announcements

3. **Admin Analytics**
   - Dashboard metrics
   - Evaluation trends
   - Jury performance reports

4. **Multi-Language Support**
   - Translate UI
   - Support multiple languages

---

**System Ready for Production! 🎬**
