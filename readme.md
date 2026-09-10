# 🤖 AI Resume ATS Analyzer — Telegram Bot

Build a simple **AI-powered Resume ATS Analyzer Telegram Bot**.

The chatbot takes two inputs:

1. **Resume** — PDF/DOCX
2. **Job Description** — text

It then produces a single useful analysis containing:

* ATS score
* Score breakdown
* Matching skills
* Missing skills
* Keyword analysis
* Resume corrections
* Skill-gap analysis
* Recommended courses
* Overall improvement suggestions

This is a **placement project MVP**. The goal is to demonstrate a practical AI + Java backend application, not to build a production-scale SaaS platform.

Keep the implementation **simple, clean, and easy to demonstrate**.

---

# 1. Core User Flow

The complete application should work like this:

```text
User
 │
 │ /start
 ▼
Telegram Bot
 │
 │ Upload Resume
 ▼
Resume Parser
 │
 ▼
Extract Resume Text
 │
 │ Send Job Description
 ▼
JD Analyzer
 │
 ▼
ATS Analysis
 │
 ├── Keyword Matching
 ├── Skill Matching
 ├── Experience Matching
 ├── Resume Structure
 │
 ▼
ATS Score
 │
 ├── Missing Skills
 ├── Corrections
 ├── Course Recommendations
 └── Analytics
 │
 ▼
Telegram
 │
 ▼
Final Report
```

---

# 2. Technology Stack

Use only the following technologies unless there is a strong reason to add something else.

### Backend

* Java
* Spring Boot
* Maven
* Spring Web

### Database

For this MVP, **PostgreSQL is optional**.

Prefer keeping the first version stateless/in-memory where possible.

If persistence is required, use:

* PostgreSQL
* Spring Data JPA

Do not introduce Redis.

### Resume Parsing

PDF:

* Apache PDFBox

DOCX:

* Apache POI

### AI

Use an LLM API for:

* Resume extraction
* Job description extraction
* Semantic matching
* Resume corrections
* Course recommendations

### Telegram

* Telegram Bot API

### Deployment

* Docker

Docker is useful but should be implemented only after the application works locally.

---

# 3. Simple Architecture

Use this architecture:

```text
                 ┌─────────────────┐
                 │    Telegram     │
                 │      Bot        │
                 └────────┬────────┘
                          │
                          ▼
                 ┌─────────────────┐
                 │   Spring Boot   │
                 │    Backend      │
                 └────────┬────────┘
                          │
          ┌───────────────┼────────────────┐
          ▼               ▼                ▼
   Resume Parser     JD Analyzer       ATS Engine
          │               │                │
          └───────────────┼────────────────┘
                          ▼
                    LLM Service
                          │
                          ▼
              Course Recommendation
                          │
                          ▼
                  Final Analysis
                          │
                          ▼
                     Telegram
```

---

# 4. Important Architecture Rule

Do NOT make the LLM responsible for calculating the entire ATS score.

Use:

```text
Deterministic Logic
        +
      LLM
        ↓
Final Analysis
```

### Deterministic logic

Calculate:

* Keyword score
* Skill score
* Experience score
* Structure score
* Overall ATS score

### LLM

Generate:

* Semantic understanding
* Corrections
* Explanations
* Skill-gap reasoning
* Course recommendations

This makes the ATS score more consistent and explainable.

---

# 5. Project Structure

Keep the project small.

```text
resume-ats-bot/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com.example.resumeats/
│   │   │       │
│   │   │       ├── ResumeAtsApplication.java
│   │   │       │
│   │   │       ├── controller/
│   │   │       │   └── TelegramWebhookController.java
│   │   │       │
│   │   │       ├── service/
│   │   │       │   ├── TelegramService.java
│   │   │       │   ├── ResumeParserService.java
│   │   │       │   ├── ResumeAnalysisService.java
│   │   │       │   ├── JobDescriptionService.java
│   │   │       │   ├── AtsScoringService.java
│   │   │       │   ├── LlmService.java
│   │   │       │   └── CourseRecommendationService.java
│   │   │       │
│   │   │       ├── dto/
│   │   │       │   ├── ResumeData.java
│   │   │       │   ├── JobDescriptionData.java
│   │   │       │   ├── AtsResult.java
│   │   │       │   ├── Correction.java
│   │   │       │   └── CourseRecommendation.java
│   │   │       │
│   │   │       ├── config/
│   │   │       │   ├── TelegramConfig.java
│   │   │       │   └── LlmConfig.java
│   │   │       │
│   │   │       └── exception/
│   │   │           └── GlobalExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       └── prompts/
│   │           ├── resume-analysis.txt
│   │           ├── jd-analysis.txt
│   │           └── corrections.txt
│   │
├── pom.xml
├── Dockerfile
├── docker-compose.yml
└── README.md
```

Do not create unnecessary layers or classes.

---

# 6. Phase 1 — Telegram Bot + Resume/JD Input

## Goal

Make the chatbot accept:

* Resume
* Job Description

### Tasks

1. Create Spring Boot project.
2. Configure Telegram Bot API.
3. Implement `/start`.
4. Handle resume PDF/DOCX upload.
5. Download the Telegram file.
6. Extract text from the resume.
7. Ask user for Job Description.
8. Receive Job Description.
9. Keep the resume and JD associated with the current Telegram user/session.

### Conversation

```text
User:
/start

Bot:
Welcome to AI Resume ATS Analyzer 🚀

Upload your resume in PDF or DOCX format.
```

After upload:

```text
Bot:
Resume received successfully ✅

Now send the Job Description.
```

After JD:

```text
Bot:
Job Description received ✅

Analyzing your resume...
```

### Deliverable

At the end of this phase:

```text
Telegram
   ↓
Resume
   +
Job Description
   ↓
Spring Boot
```

Both inputs should be available to the analysis service.

---

# 7. Phase 2 — Resume + JD AI Analysis

## Goal

Convert both inputs into structured information.

### Resume

Extract:

```text
Name
Summary
Skills
Experience
Education
Projects
Certifications
```

Example:

```json
{
  "name": "John Doe",
  "summary": "Java backend developer...",
  "skills": [
    "Java",
    "Spring Boot",
    "Docker",
    "MySQL"
  ],
  "experience": [],
  "education": [],
  "projects": [],
  "certifications": []
}
```

### Job Description

Extract:

```text
Job Title
Required Skills
Preferred Skills
Experience Requirement
Education Requirement
Important Keywords
Responsibilities
```

Example:

```json
{
  "jobTitle": "Java Backend Developer",
  "requiredSkills": [
    "Java",
    "Spring Boot",
    "Kafka",
    "AWS",
    "Docker"
  ],
  "preferredSkills": [
    "Kubernetes"
  ],
  "experienceRequired": "2+ years",
  "keywords": [
    "REST API",
    "Microservices",
    "Kafka",
    "AWS"
  ]
}
```

### LLM Requirements

The LLM must return valid structured JSON.

Never rely on free-form AI output for internal processing.

The LLM must NOT invent information.

For missing resume information return:

```json
null
```

or:

```json
[]
```

---

# 8. Phase 3 — ATS Scoring Engine

## Goal

Calculate an explainable ATS score.

Use the following initial weighting:

```text
Skill Match          30%
Keyword Match        30%
Experience Match     15%
Resume Structure     15%
Education Match      10%

Total                100%
```

Keep the formula simple.

---

## 8.1 Skill Matching

Compare:

```text
JD required skills
        vs
Resume skills
```

Example:

```text
JD:

Java          ✅
Spring Boot   ✅
Kafka         ❌
AWS           ❌
Docker        ✅
```

Calculate:

```text
3 / 5 = 60%
```

---

## 8.2 Keyword Matching

Extract important JD keywords.

Check whether they appear meaningfully in the resume.

Normalize:

```text
Java
JAVA
java
```

as the same keyword.

Avoid blindly matching every word.

Focus on:

* Technologies
* Tools
* Frameworks
* Job-specific terminology
* Important responsibilities

---

## 8.3 Experience Matching

Example:

```text
JD:
2+ years

Resume:
1 year
```

The experience score should be reduced.

If experience is not specified by the JD, do not penalize the resume.

---

## 8.4 Education Matching

Compare education requirements.

If JD doesn't specify education:

```text
Education Score = 100
```

Do not unnecessarily penalize the user.

---

## 8.5 Resume Structure

Check whether the resume contains common sections:

```text
Summary
Skills
Experience
Projects
Education
Certifications
```

Do not require every section.

Only evaluate whether the resume has a reasonable structure.

---

# 9. ATS Result

Create:

```java
AtsResult
```

Example:

```json
{
  "overallScore": 72,
  "skillScore": 75,
  "keywordScore": 68,
  "experienceScore": 80,
  "educationScore": 100,
  "structureScore": 85
}
```

The backend calculates this score.

The LLM does not decide the final number.

---

# 10. Phase 4 — Corrections + Skill Gaps + Courses

This phase uses the ATS result and LLM.

---

## 10.1 Missing Skills

Example:

```text
Required:
Java
Spring Boot
Kafka
AWS
Docker

Resume:
Java
Spring Boot
Docker
```

Output:

```text
Missing Skills:

🔴 Kafka
🔴 AWS
```

Classify:

```text
HIGH
MEDIUM
LOW
```

Required skills should have higher priority than preferred skills.

---

# 11. Resume Corrections

The bot should give **specific corrections**, not generic advice.

Bad:

```text
Improve your resume.
```

Good:

```text
Summary

Current:
Java developer with backend experience.

Suggested:
Backend developer experienced in Java and Spring Boot
for building REST APIs and backend applications.

Reason:
Makes relevant technologies clearer for the target role.
```

Another example:

```text
Skills Section

Missing:
Kafka

Recommendation:
Add Kafka only if you actually have experience with it.
Otherwise, consider learning it rather than claiming it.
```

---

# 12. Critical AI Rule

The AI must NEVER fabricate:

```text
Experience
Skills
Achievements
Metrics
Companies
Certifications
Projects
Technologies
```

For example, never generate:

```text
Improved API performance by 40%.
```

unless the resume actually provides evidence for that number.

Instead:

```text
Consider adding a measurable performance improvement
if you have one.
```

---

# 13. Course Recommendations

Use the missing skills to recommend learning resources.

Example:

```text
Missing:
Kafka
AWS
Microservices
```

Output:

```text
🎓 Recommended Courses

1. Apache Kafka Fundamentals
   Skill: Kafka
   Priority: HIGH

2. AWS Fundamentals
   Skill: AWS
   Priority: HIGH

3. Microservices with Spring Boot
   Skill: Microservices
   Priority: HIGH
```

Each recommendation should contain:

```text
title
provider
skill
level
reason
URL
```

Do not fabricate URLs.

For the MVP, course data can be stored in a simple JSON/configuration file.

Example:

```json
[
  {
    "skill": "Kafka",
    "title": "Apache Kafka Fundamentals",
    "provider": "Example Provider",
    "level": "Beginner",
    "url": "VALID_URL"
  }
]
```

Do not create fake providers or fake links.

If real course data is unavailable, provide course/topic recommendations without pretending that a specific course exists.

---

# 14. Phase 5 — Final Telegram Report

This is the most important phase because the evaluator will primarily see this output.

The final Telegram message should be concise but informative.

Example:

```text
━━━━━━━━━━━━━━━━━━━━
🎯 RESUME ATS ANALYSIS
━━━━━━━━━━━━━━━━━━━━

ATS Score: 72/100

📊 Score Breakdown

Skills Match       75%
Keywords Match     68%
Experience Match   80%
Education Match    100%
Structure           85%

━━━━━━━━━━━━━━━━━━━━
✅ STRONG MATCHES
━━━━━━━━━━━━━━━━━━━━

• Java
• Spring Boot
• Docker
• REST API

━━━━━━━━━━━━━━━━━━━━
❌ MISSING SKILLS
━━━━━━━━━━━━━━━━━━━━

🔴 Kafka
🔴 AWS
🟡 Microservices

━━━━━━━━━━━━━━━━━━━━
⚠️ KEY IMPROVEMENTS
━━━━━━━━━━━━━━━━━━━━

1. Improve professional summary.
2. Highlight REST API experience.
3. Improve project descriptions.
4. Add relevant keywords where truthful.

━━━━━━━━━━━━━━━━━━━━
🎓 RECOMMENDED LEARNING
━━━━━━━━━━━━━━━━━━━━

1. Apache Kafka
2. AWS Fundamentals
3. Microservices

━━━━━━━━━━━━━━━━━━━━
📈 OVERALL ASSESSMENT
━━━━━━━━━━━━━━━━━━━━

Your resume has a good match for the role,
but Kafka, AWS and Microservices are major gaps.

Focus on these skills and improve the relevant
resume sections to increase compatibility.
```

---

# 15. Optional Telegram Buttons

After sending the main report, provide buttons:

```text
[📊 Detailed Analytics]
[✏️ Corrections]
[🎓 Courses]
[❌ Missing Skills]
```

When the user clicks a button, send the corresponding detailed information.

Do not create a complicated UI.

---

# 16. LLM Service

Create a single abstraction:

```java
public interface LlmService {

    ResumeData analyzeResume(String resumeText);

    JobDescriptionData analyzeJobDescription(String jobDescription);

    CorrectionResult generateCorrections(
        ResumeData resume,
        JobDescriptionData jobDescription,
        AtsResult atsResult
    );
}
```

The implementation:

```text
LlmService
     ↓
LlmServiceImpl
     ↓
LLM API
```

Do not scatter LLM API calls across multiple classes.

---

# 17. Analysis Flow

The main service should orchestrate the complete process:

```java
public AnalysisResult analyze(
    String resumeText,
    String jobDescription
) {
    // 1. Extract resume information
    ResumeData resume =
        llmService.analyzeResume(resumeText);

    // 2. Extract JD information
    JobDescriptionData jd =
        llmService.analyzeJobDescription(jobDescription);

    // 3. Calculate deterministic ATS score
    AtsResult ats =
        atsScoringService.calculate(resume, jd);

    // 4. Generate corrections
    CorrectionResult corrections =
        llmService.generateCorrections(
            resume,
            jd,
            ats
        );

    // 5. Find missing skills
    // 6. Recommend courses
    // 7. Build final result

    return result;
}
```

Keep this orchestration clean.

---

# 18. Error Handling

Handle at minimum:

### Invalid file

```text
❌ Unsupported file.

Please upload a PDF or DOCX resume.
```

### Resume parsing failure

```text
❌ I couldn't read this resume.

Please upload another PDF/DOCX file.
```

### Empty JD

```text
❌ Job Description cannot be empty.
```

### LLM failure

```text
❌ Resume analysis is temporarily unavailable.

Please try again.
```

Do not expose stack traces or internal errors to Telegram users.

---

# 19. Security

Even though this is an MVP:

### Never hardcode

```text
TELEGRAM_BOT_TOKEN
LLM_API_KEY
```

Use environment variables.

Example:

```yaml
telegram:
  bot-token: ${TELEGRAM_BOT_TOKEN}

llm:
  api-key: ${LLM_API_KEY}
```

Do not commit `.env` files containing secrets.

---

# 20. Validation

Implement:

### Resume

Allowed:

```text
PDF
DOCX
```

Set a reasonable maximum file size.

### Job Description

Set a reasonable maximum text length.

Do not allow extremely large inputs to be sent to the LLM unnecessarily.

---

# 21. Testing

Do not build a huge testing suite.

Focus on the important logic.

Test:

### Skill matching

```text
JD: Java, Spring Boot, Kafka
Resume: Java, Spring Boot

Expected:
2 matched
1 missing
```

### Keyword matching

Check case-insensitivity.

```text
Java
JAVA
java
```

should match.

### Experience

```text
Required: 2 years
Resume: 1 year

Expected:
Reduced score
```

### Education

No JD education requirement:

```text
Expected:
No penalty
```

### ATS calculation

Verify the weighted score mathematically.

### File validation

Test:

```text
PDF → accepted
DOCX → accepted
TXT → rejected
```

---

# 22. Do NOT Implement

The following are intentionally excluded from the MVP:

```text
❌ Redis
❌ Kafka
❌ RabbitMQ
❌ Elasticsearch
❌ Vector database
❌ Kubernetes
❌ Authentication
❌ User accounts
❌ Resume history
❌ Resume version tracking
❌ Web dashboard
❌ Payment system
❌ Subscription system
❌ Microservices architecture
❌ Complex event-driven architecture
❌ Multiple backend services
❌ Discord integration
```

Do not add these unless specifically requested later.

---

# 23. Optional Docker Phase

After everything works locally, add:

```text
Dockerfile
```

The application should be runnable using:

```text
docker build
docker run
```

If PostgreSQL is not used, there is no need for a complex Docker Compose setup.

---

# 24. Final MVP Architecture

The final project should remain approximately this simple:

```text
                   TELEGRAM
                       │
                       ▼
             ┌──────────────────┐
             │ Telegram Service │
             └────────┬─────────┘
                      │
                      ▼
             ┌──────────────────┐
             │ Analysis Service │
             └────────┬─────────┘
                      │
          ┌───────────┼────────────┐
          ▼           ▼            ▼
       Resume         JD       ATS Engine
       Parser       Analyzer       │
          │           │            │
          └───────────┼────────────┘
                      ▼
                 LLM Service
                      │
            ┌─────────┼─────────┐
            ▼         ▼         ▼
       Corrections  Gaps     Courses
            │         │         │
            └─────────┼─────────┘
                      ▼
                Final Report
                      │
                      ▼
                  TELEGRAM
```

---

# 25. Expected Final Demonstration

The complete demo should take approximately:

```text
1. User starts bot
        ↓
2. Uploads resume
        ↓
3. Sends JD
        ↓
4. Bot analyzes
        ↓
5. Bot returns:
```

```text
ATS SCORE
    ↓
Score Breakdown
    ↓
Matching Skills
    ↓
Missing Skills
    ↓
Corrections
    ↓
Course Recommendations
    ↓
Overall Assessment
```

The entire process should be easy to demonstrate in a placement interview.

---

# 26. Implementation Protocol for AI Coding Agent

Implement the project **phase-by-phase**.

There are only **5 main phases**:

```text
Phase 1 → Telegram + Resume/JD Input
Phase 2 → Resume/JD AI Analysis
Phase 3 → ATS Scoring Engine
Phase 4 → Corrections + Skill Gaps + Courses
Phase 5 → Final Telegram Report
```

Optional:

```text
Phase 6 → Docker
```

---

## Rules

### Rule 1

Implement ONLY the current phase.

Do not implement future phases early.

### Rule 2

After each phase:

1. Compile the project.
2. Run tests.
3. Fix errors.
4. Verify the current functionality.
5. Show what was implemented.
6. Stop.

### Rule 3

Wait for the user to say:

```text
next
```

before starting the next phase.

### Rule 4

Do not introduce unnecessary dependencies.

### Rule 5

Do not over-engineer the project.

### Rule 6

Do not fabricate resume information.

### Rule 7

Do not fabricate course providers or URLs.

### Rule 8

Do not hardcode API keys.

### Rule 9

Keep ATS scoring deterministic and explainable.

### Rule 10

Keep Telegram-specific logic separate from ATS business logic.

---

# 27. Success Criteria

The project is considered complete when a user can:

```text
✅ Open Telegram bot
✅ Upload PDF/DOCX resume
✅ Send Job Description
✅ Receive ATS score
✅ See score breakdown
✅ See matching skills
✅ See missing skills
✅ See keyword gaps
✅ Receive resume corrections
✅ Receive course recommendations
✅ Receive overall resume assessment
```

The final product should prioritize:

```text
Simplicity
    +
Correctness
    +
Good AI output
    +
Clean Java/Spring Boot code
    +
Good Telegram user experience
```

Do not sacrifice these qualities by adding unnecessary architecture.
