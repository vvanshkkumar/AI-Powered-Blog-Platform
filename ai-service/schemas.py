from __future__ import annotations

import operator
from typing import Annotated, List, Literal, Optional, TypedDict

from pydantic import BaseModel, Field


# ── Pydantic Models ──────────────────────────────────────────────────────────


class Task(BaseModel):
    id: int
    title: str
    goal: str
    bullets: List[str] = Field(..., min_length=3, max_length=6)
    target_words: int
    tags: List[str]
    requires_research: bool
    requires_citations: bool
    requires_code: bool


class Plan(BaseModel):
    blog_title: str
    audience: str
    tone: str
    blog_kind: Literal[
        "explainer", "tutorial", "news_roundup", "comparison", "system_design"
    ] = "explainer"
    constraints: List[str]
    tasks: List[Task]


class EvidenceItem(BaseModel):
    title: str
    url: str
    published_at: Optional[str] = None
    snippet: Optional[str] = None
    source: Optional[str] = None


class RouterDecision(BaseModel):
    needs_research: bool
    mode: Literal["closed_book", "hybrid", "open_book"]
    reason: str
    queries: List[str]
    max_results_per_query: int = 5


class EvidencePack(BaseModel):
    evidence: List[EvidenceItem]


# ── API Request / Response ───────────────────────────────────────────────────


class GenerateRequest(BaseModel):
    topic: str


class GenerateResponse(BaseModel):
    title: str
    content: str
    suggested_tags: List[str]


# ── LangGraph State ─────────────────────────────────────────────────────────


class State(TypedDict):
    topic: str
    mode: str
    needs_research: bool
    queries: List[str]
    evidence: List[EvidenceItem]
    plan: Optional[Plan]
    as_of: str
    recency_days: int
    sections: Annotated[List[tuple[int, str]], operator.add]
    merged_md: str
    final: str
