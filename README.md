# 🧠 RecallFlow — Forgetting-Based Revision System

> A full-stack intelligent learning platform that tells you **what to study next** based on memory science.

[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)](https://java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green?style=flat-square&logo=spring)](https://spring.io)
[![React](https://img.shields.io/badge/React-19-blue?style=flat-square&logo=react)](https://react.dev)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql)](https://mysql.com)
[![JWT](https://img.shields.io/badge/JWT-Auth-black?style=flat-square&logo=jsonwebtokens)](https://jwt.io)

---

## 📖 Problem Statement

Traditional study apps let you **manually** choose what to revise. But the human brain forgets information at a predictable rate — if you don't revise at the right time, you forget it.

**RecallFlow solves this by automatically calculating which topic you are most likely to forget TODAY and building your revision queue for you.**

---

## ✨ What Makes This Project Different?

Most study tools are just **CRUD apps** — add a topic, tick it off. RecallFlow is different.

The system doesn't just store your topics. It **continuously analyzes**:

```
Time since last revision
+
Confidence level (1–5)
+
Quiz performance (latest + average)
+
Number of mistakes
+
Topic difficulty
+
Revision interval exceeded?
──────────────────────────────
= Forgetting Risk Score (0–100)
= Next Revision Date
= Today's Revision Queue
```

This algorithm runs every time you complete a revision or quiz — so your revision queue is always up-to-date.

---

## 🎯 Core Features

| Feature | Description |
|---|---|
| **Revision Priority Engine** | Custom algorithm scoring each topic 0–100 |
| **Forgetting Risk Levels** | LOW / MEDIUM / HIGH / VERY HIGH |
| **Adaptive Revision Intervals** | Poor score → revise in 1 day. Excellent → 14 days |
| **Daily Revision Queue** | Auto-generated, ranked by forgetting risk |
| **Quiz System** | MCQ with scoring, explanations, and mistake tracking |
| **Mistake Tracker** | Every wrong answer recorded and weighted in priority |
| **Confidence Tracking** | 1–5 star self-assessment after every revision |
| **Weak Area Detection** | Automatic identification of struggling topics |
| **Smart Recommendations** | Data-driven personalized suggestions |
| **Analytics Dashboard** | Charts for performance, activity, risk distribution |
| **JWT Authentication** | Secure, per-user data isolation |

---

## 🏗️ Architecture

```
┌────────────────────────────────────────────────────────────┐
│                    FRONTEND (React + Vite)                  │
│  Dashboard  Topics  Revision  Quiz  Analytics  Mistakes     │
└──────────────────────────┬─────────────────────────────────┘
                           │ REST API (Axios + JWT)
┌──────────────────────────┴─────────────────────────────────┐
│                  BACKEND (Spring Boot)                       │
│                                                              │
│  AuthController  ──→  AuthService  ──→  UserRepository      │
│  TopicController ──→  TopicService ──→  TopicRepository      │
│  RevisionController──→RevisionService──→RevisionRepository   │
│  QuizController  ──→  QuizService  ──→  QuizAttemptRepo      │
│  AnalyticsController──→AnalyticsService──→MistakeRepo        │
│                          │                                   │
│              RevisionPriorityService (THE ENGINE)            │
│                          │                                   │
└──────────────────────────┴─────────────────────────────────┘
                           │ JPA/Hibernate
┌──────────────────────────┴─────────────────────────────────┐
│                      MySQL Database                          │
│  users  topics  revisions  questions  quiz_attempts          │
│  quiz_answers  mistakes                                      │
└────────────────────────────────────────────────────────────┘
```

---

## 🔬 Revision Priority Algorithm

The `RevisionPriorityService` computes a **priority score from 0–100**:

```
Score = w₁×TimeFactor
      + w₂×ConfidenceFactor
      + w₃×QuizFactor
      + w₄×MistakeFactor
      + w₅×DifficultyFactor
      + IntervalExceededBonus
```

### Weight Distribution

| Factor | Weight | How Calculated |
|---|---|---|
| Time since revision | **30%** | Square-root of days / 30 (Ebbinghaus curve) |
| Confidence | **20%** | Inverted: (5 - confidence) / 4 |
| Quiz performance | **25%** | Blended: 60% latest + 40% average, inverted |
| Mistakes | **15%** | mistakes / 10, capped at 1.0 |
| Difficulty | **10%** | EASY=0.2, MEDIUM=0.5, HARD=1.0 |
| Overdue bonus | **+0–10** | Days overdue × 1.5 |

### Example

```
Topic: Java Exception Handling
Days Since Revision: 8    → TimeFactor = 0.516
Confidence: 2/5           → ConfidenceFactor = 0.75
Latest Score: 55%         → QuizFactor = 0.47
Avg Score: 62%            
Mistakes: 6               → MistakeFactor = 0.60
Difficulty: MEDIUM        → DifficultyFactor = 0.50

Score = (0.30×0.516) + (0.20×0.75) + (0.25×0.47)
      + (0.15×0.60) + (0.10×0.50) × 100
     = 89.2 → HIGH RISK
```

---

## 🧬 Adaptive Revision Intervals

```
Quiz Score    Confidence    Next Revision
-----------------------------------------
< 50%         1–2           1 day
50–69%        2–3           2–3 days
70–79%        3             4 days
80–89%        4             7 days
90–100%       4–5           14 days
```

The interval is calculated dynamically in `RevisionPriorityService.calculateNextRevisionDays()`.

---

## 🗃️ Database Schema

```
users
  id, username, email, password, full_name, current_streak, longest_streak

topics
  id, name, subject, difficulty, status, date_learned, last_revised_date,
  next_revision_date, revision_count, confidence_level, latest_quiz_score,
  average_quiz_score, total_mistakes, consecutive_successful_revisions,
  forgetting_risk, revision_priority_score, user_id

revisions
  id, topic_id, user_id, revision_date, confidence_before, confidence_after,
  quiz_score, was_successful, next_revision_days, risk_at_revision

questions
  id, topic_id, question_text, option_a/b/c/d, correct_answer, explanation,
  difficulty, subject

quiz_attempts
  id, topic_id, user_id, total_questions, correct_answers, score_percentage,
  attempt_date

quiz_answers
  id, quiz_attempt_id, question_id, selected_answer, correct_answer, is_correct

mistakes
  id, user_id, topic_id, question_id, user_answer, correct_answer, mistake_date,
  attempt_number, reviewed
```

---

## 🚀 API Reference

```
Authentication
  POST  /api/auth/register
  POST  /api/auth/login
  GET   /api/auth/me

Topics
  GET   /api/topics
  POST  /api/topics
  GET   /api/topics/{id}
  PUT   /api/topics/{id}
  DELETE /api/topics/{id}
  POST  /api/topics/{id}/learn
  GET   /api/topics/revision/today

Revision
  GET   /api/revision/{topicId}
  POST  /api/revision/{topicId}/complete
  GET   /api/revision/{topicId}/history
  GET   /api/revision

Quiz
  GET   /api/quiz/{topicId}/questions?limit=10
  POST  /api/quiz/submit
  GET   /api/quiz/{topicId}/history
  POST  /api/quiz/{topicId}/questions

Analytics
  GET   /api/analytics
  GET   /api/analytics/subjects
  GET   /api/analytics/weak-topics
  GET   /api/analytics/recommendations
  GET   /api/analytics/quiz-performance?days=30
  GET   /api/analytics/revision-activity?days=30
  GET   /api/analytics/mistakes
  GET   /api/analytics/mistakes/summary
```

---

## 🛠️ Technology Stack

### Backend
- **Java 17** + **Spring Boot 3.2**
- **Spring Security** + **JWT (JJWT)**
- **Spring Data JPA** + **Hibernate**
- **MySQL 8.0**
- **Maven**
- **Lombok**

### Frontend
- **React 19** + **Vite 8**
- **React Router v6**
- **Axios**
- **Recharts** (analytics charts)
- **Lucide React** (icons)
- **Vanilla CSS** (custom design system)

---

## ⚙️ How to Run Locally

### Prerequisites
- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Node.js 18+

### 1. Database
```sql
CREATE DATABASE recallflow_db;
```

### 2. Backend
```bash
cd recallflow-backend
# Update src/main/resources/application.properties with your MySQL credentials
mvn spring-boot:run
```
Backend runs on: `http://localhost:8080`

### 3. Frontend
```bash
# In the project root
npm install
npm run dev
```
Frontend runs on: `http://localhost:5173`

---

## 🧪 Running Tests

```bash
cd recallflow-backend
mvn test
```

Tests cover:
- Priority score calculation
- Forgetting risk levels
- Next revision interval calculation
- Memory strength estimation
- Reason generation

---

## 📊 Sample User Flow

```
1. Register → Login
2. Create topic: "Java Collections" (HARD difficulty)
3. Mark as learned → Confidence: 3/5
4. System schedules first revision in 4 days
5. Start revision → Take quiz → Score: 55%
6. System records 3 mistakes, updates priority to 89
7. Next revision scheduled in 1 day
8. Revise again → Score: 88%
9. Priority drops to 32 (LOW)
10. Next revision: 7 days away
```

---

## 🔮 Future Enhancements

- AI-generated quiz questions from topic name
- Email reminders for due revisions
- Import topics from PDF/notes
- Mobile application
- Study groups and leaderboards
- More advanced SM-2 / FSRS spaced repetition
- Push notifications

---

## 📁 Project Structure

```
Recall_flow/
├── src/                          # React Frontend
│   ├── components/
│   │   └── common/               # Sidebar, UIComponents
│   ├── context/                  # AuthContext
│   ├── pages/                    # All page components
│   ├── services/                 # API service layer
│   └── App.jsx                   # Router + Layout
│
└── recallflow-backend/           # Spring Boot Backend
    └── src/main/java/com/recallflow/app/
        ├── controller/           # REST Controllers
        ├── service/              # Business Logic
        │   └── RevisionPriorityService.java  ← THE ENGINE
        ├── repository/           # JPA Repositories
        ├── entity/               # JPA Entities
        ├── dto/                  # Data Transfer Objects
        ├── security/             # JWT + Spring Security
        ├── config/               # SecurityConfig
        └── exception/            # Global error handling
```

---

*Built with ❤️ using Java, Spring Boot, React, and memory science.*
