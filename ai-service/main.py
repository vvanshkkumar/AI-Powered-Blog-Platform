from __future__ import annotations

import logging
from datetime import date

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

import agents
from schemas import GenerateRequest, GenerateResponse

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="AI Blog Generator", version="1.0.0")

# ── CORS ─────────────────────────────────────────────────────────────────────

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://localhost:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ── Endpoints ────────────────────────────────────────────────────────────────


@app.post("/generate", response_model=GenerateResponse)
async def generate(request: GenerateRequest) -> GenerateResponse:
    """Generate a full blog post from a topic."""
    try:
        as_of = date.today().isoformat()

        initial_state = {
            "topic": request.topic,
            "as_of": as_of,
            "sections": [],
            "evidence": [],
            "queries": [],
            "needs_research": False,
            "mode": "",
            "recency_days": 0,
            "plan": None,
            "merged_md": "",
            "final": "",
        }

        result = agents.app.invoke(initial_state)

        # Extract title from plan
        title = result["plan"].blog_title

        # Collect unique suggested tags from all tasks
        all_tags: list[str] = []
        for task in result["plan"].tasks:
            for tag in task.tags:
                if tag not in all_tags:
                    all_tags.append(tag)

        return GenerateResponse(
            title=title,
            content=result["final"],
            suggested_tags=all_tags,
        )

    except Exception as exc:
        logger.exception("Blog generation failed")
        raise HTTPException(status_code=500, detail=str(exc)) from exc
