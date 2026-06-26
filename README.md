# AI-Powered Blog Platform

A full-stack blog platform with AI-powered blog generation using LangGraph agents.

## Architecture

```
┌─────────────┐     ┌─────────────────┐     ┌──────────────────┐
│   React +   │────▶│  Spring Boot    │────▶│  Python FastAPI   │
│   NextUI    │     │  REST API       │     │  + LangGraph      │
│  (port 5173)│◀────│  (port 8080)    │◀────│  (port 8000)      │
└─────────────┘     └─────────────────┘     └──────────────────┘
                           │                         │
                           ▼                         ▼
                     ┌───────────┐            ┌─────────────┐
                     │PostgreSQL │            │ OpenAI API  │
                     │  (5432)   │            │ Tavily API  │
                     └───────────┘            └─────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, Vite, NextUI, TipTap Editor, TypeScript |
| Backend | Spring Boot 3.4, Java 21, Spring Security (JWT) |
| AI Service | Python, FastAPI, LangGraph, LangChain, OpenAI |
| Database | PostgreSQL |
| Research | Tavily Search API (optional) |

## Features

- **User Authentication** — JWT-based registration, login, and profile
- **Blog CRUD** — Create, read, update, delete posts with rich text (TipTap)
- **Categories & Tags** — Organize content with categories and tags
- **AI Blog Generation** — Enter a topic → LangGraph agents research, plan, and write a full blog post
- **Ownership Checks** — Users can only edit/delete their own posts
- **Draft System** — Save posts as drafts before publishing

## AI Pipeline (LangGraph)

```
START → Router → Research? → Orchestrator → Workers (parallel) → Merge → END
```

1. **Router** — Decides if web research is needed (closed_book / hybrid / open_book)
2. **Research** — Fetches evidence via Tavily search (if needed)
3. **Orchestrator** — Plans 5-9 sections with goals, bullets, word targets
4. **Workers** — Write each section in parallel
5. **Merge** — Combines sections, converts Markdown → HTML

## Getting Started

### Prerequisites

- Java 21+
- Node.js 18+
- Python 3.10+
- Docker (for PostgreSQL)

### 1. Database

```bash
cd backend
docker-compose up -d
```

### 2. Python AI Service

```bash
cd ai-service
python -m venv venv
source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt

# Create .env file
echo "OPENAI_API_KEY=sk-your-key-here" > .env
echo "TAVILY_API_KEY=tvly-your-key-here" >> .env  # Optional

uvicorn main:app --port 8000 --reload
```

### 3. Spring Boot Backend

```bash
cd backend
./mvnw spring-boot:run
```

### 4. React Frontend

```bash
cd frontend
npm install
npm run dev
```

The app will be available at **http://localhost:5173**

## API Endpoints

### Auth
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/auth/register` | Public | Register new user |
| POST | `/api/v1/auth/login` | Public | Login, returns JWT |
| GET | `/api/v1/auth/me` | Required | Get current user profile |

### Posts
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/posts` | Public | List all published posts |
| GET | `/api/v1/posts/:id` | Public | Get single post |
| POST | `/api/v1/posts` | Required | Create post |
| PUT | `/api/v1/posts/:id` | Required | Update post (owner only) |
| DELETE | `/api/v1/posts/:id` | Required | Delete post (owner only) |
| GET | `/api/v1/posts/drafts` | Required | List user's drafts |
| POST | `/api/v1/posts/generate` | Required | Generate post with AI |

### Categories & Tags
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/categories` | Public | List categories |
| POST | `/api/v1/categories` | Required | Create category |
| PUT | `/api/v1/categories/:id` | Required | Update category |
| DELETE | `/api/v1/categories/:id` | Required | Delete category |
| GET | `/api/v1/tags` | Public | List tags |
| POST | `/api/v1/tags` | Required | Create tags |
| DELETE | `/api/v1/tags/:id` | Required | Delete tag |

## Project Structure

```
├── ai-service/            # Python FastAPI + LangGraph
│   ├── agents.py          # LangGraph pipeline (5 nodes)
│   ├── schemas.py         # Pydantic models + State
│   ├── main.py            # FastAPI app
│   └── requirements.txt
├── backend/               # Spring Boot
│   └── src/main/java/com/vvanshkkumar/blog/
│       ├── controllers/   # REST controllers
│       ├── services/      # Business logic
│       ├── domain/        # Entities + DTOs
│       ├── repositories/  # JPA repositories
│       ├── security/      # JWT auth
│       └── config/        # App + Security config
└── frontend/              # React + Vite
    └── src/
        ├── pages/         # Route pages
        ├── components/    # Reusable components
        └── services/      # API client
```

## License

MIT
