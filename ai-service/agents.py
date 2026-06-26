from __future__ import annotations

import logging
import os
from datetime import datetime, timedelta

import markdown as md_lib
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI
from langgraph.constants import Send
from langgraph.graph import END, START, StateGraph

from schemas import (
    EvidenceItem,
    EvidencePack,
    Plan,
    RouterDecision,
    State,
)

load_dotenv()

logger = logging.getLogger(__name__)

# ── Shared LLM instance ─────────────────────────────────────────────────────

llm = ChatOpenAI(model="gpt-4.1-mini")


# ── 1. Router Node ──────────────────────────────────────────────────────────


def router_node(state: State) -> dict:
    """Decide whether the topic needs external research."""

    structured_llm = llm.with_structured_output(RouterDecision)

    decision: RouterDecision = structured_llm.invoke(
        [
            {
                "role": "system",
                "content": (
                    "You are a research-routing assistant. Given a blog topic, decide "
                    "whether the writer needs live web research to produce a high-quality "
                    "article.\n\n"
                    "Modes:\n"
                    "  • open_book  – topic is very recent or fast-moving; research required.\n"
                    "  • hybrid     – topic benefits from some external evidence.\n"
                    "  • closed_book – the LLM's training data is sufficient.\n\n"
                    "Return a RouterDecision with:\n"
                    "  - needs_research: true/false\n"
                    "  - mode: one of the three modes above\n"
                    "  - reason: short justification\n"
                    "  - queries: list of search queries (may be empty for closed_book)\n"
                    "  - max_results_per_query: how many results per query (default 5)"
                ),
            },
            {
                "role": "user",
                "content": f"Topic: {state['topic']}\nToday's date: {state['as_of']}",
            },
        ]
    )

    recency_map = {"open_book": 7, "hybrid": 45, "closed_book": 3650}

    return {
        "mode": decision.mode,
        "needs_research": decision.needs_research,
        "queries": decision.queries,
        "recency_days": recency_map.get(decision.mode, 3650),
    }


# ── 2. Research Node ────────────────────────────────────────────────────────


def research_node(state: State) -> dict:
    """Fetch evidence from Tavily for each query, then structure via LLM."""

    tavily_api_key = os.getenv("TAVILY_API_KEY")
    if not tavily_api_key:
        logger.warning("TAVILY_API_KEY not set – skipping research.")
        return {"evidence": []}

    try:
        from langchain_community.tools.tavily_search import TavilySearchResults
    except ImportError:
        logger.warning("tavily-python not installed – skipping research.")
        return {"evidence": []}

    search_tool = TavilySearchResults(max_results=6)

    raw_results: list[dict] = []
    for query in state["queries"][:10]:
        try:
            results = search_tool.invoke({"query": query})
            if isinstance(results, list):
                raw_results.extend(results)
        except Exception as exc:
            logger.warning("Tavily query failed for %r: %s", query, exc)

    if not raw_results:
        return {"evidence": []}

    # Ask LLM to structure raw results into EvidencePack
    structured_llm = llm.with_structured_output(EvidencePack)
    pack: EvidencePack = structured_llm.invoke(
        [
            {
                "role": "system",
                "content": (
                    "You receive raw search results. Extract structured evidence items. "
                    "Each item must have title, url, and optionally published_at "
                    "(ISO date string), snippet, and source."
                ),
            },
            {
                "role": "user",
                "content": str(raw_results),
            },
        ]
    )

    # Deduplicate by URL
    seen_urls: set[str] = set()
    deduped: list[EvidenceItem] = []
    for item in pack.evidence:
        if item.url not in seen_urls:
            seen_urls.add(item.url)
            deduped.append(item)

    # Filter by recency for open_book mode
    if state.get("mode") == "open_book":
        cutoff = datetime.now() - timedelta(days=state.get("recency_days", 7))
        filtered: list[EvidenceItem] = []
        for item in deduped:
            if item.published_at:
                try:
                    pub_date = datetime.fromisoformat(item.published_at)
                    if pub_date >= cutoff:
                        filtered.append(item)
                    continue
                except ValueError:
                    pass
            # Keep items with no parseable date
            filtered.append(item)
        deduped = filtered

    return {"evidence": deduped}


# ── 3. Orchestrator Node ────────────────────────────────────────────────────


def orchestrator_node(state: State) -> dict:
    """Plan the blog post: produce 5-9 tasks (sections)."""

    evidence_summary = ""
    if state.get("evidence"):
        items = state["evidence"]
        evidence_summary = "\n".join(
            f"- [{e.title}]({e.url}): {e.snippet or 'No snippet'}" for e in items
        )

    structured_llm = llm.with_structured_output(Plan)

    plan: Plan = structured_llm.invoke(
        [
            {
                "role": "system",
                "content": (
                    "You are a senior technical writer and blog architect.\n\n"
                    "Given a topic (and optional evidence), produce a detailed blog Plan "
                    "with 5 to 9 tasks. Each task represents one section of the blog.\n\n"
                    "Rules:\n"
                    "- Assign sequential integer IDs starting from 1.\n"
                    "- Each task must have 3-6 bullet points describing what to cover.\n"
                    "- Set target_words per section (150-600 words).\n"
                    "- Assign descriptive tags to each task.\n"
                    "- Mark requires_research, requires_citations, requires_code as "
                    "appropriate.\n"
                    "- Choose an appropriate blog_kind.\n"
                    "- Identify audience and tone.\n"
                    "- List any constraints."
                ),
            },
            {
                "role": "user",
                "content": (
                    f"Topic: {state['topic']}\n"
                    f"Date: {state['as_of']}\n"
                    f"Research mode: {state.get('mode', 'closed_book')}\n\n"
                    f"Evidence:\n{evidence_summary or 'None available.'}"
                ),
            },
        ]
    )

    return {"plan": plan}


# ── 4. Worker Node ──────────────────────────────────────────────────────────


def worker_node(payload: dict) -> dict:
    """Write a single markdown section for one task."""

    task_data = payload["task"]
    plan_data = payload["plan"]
    evidence = payload.get("evidence", [])

    evidence_text = ""
    if evidence:
        evidence_text = "\n".join(
            f"- [{e['title']}]({e['url']}): {e.get('snippet', '')}"
            if isinstance(e, dict)
            else f"- [{e.title}]({e.url}): {e.snippet or ''}"
            for e in evidence
        )

    citation_instruction = ""
    if task_data.get("requires_citations"):
        citation_instruction = (
            "You MUST include inline citations as markdown links to evidence URLs."
        )

    code_instruction = ""
    if task_data.get("requires_code"):
        code_instruction = (
            "You MUST include relevant code examples in fenced code blocks."
        )

    messages = [
        {
            "role": "system",
            "content": (
                "You are a skilled technical blog writer.\n\n"
                "Write ONE markdown section for a blog post.\n\n"
                f"Blog title: {plan_data.get('blog_title', 'Untitled')}\n"
                f"Audience: {plan_data.get('audience', 'developers')}\n"
                f"Tone: {plan_data.get('tone', 'professional')}\n\n"
                "Rules:\n"
                f"- Target approximately {task_data.get('target_words', 300)} words.\n"
                f"- Cover ALL of these bullet points: {task_data.get('bullets', [])}\n"
                f"{citation_instruction}\n"
                f"{code_instruction}\n"
                "- Use ## for the section heading.\n"
                "- Do NOT include the blog title (# heading) — only the section.\n"
                "- Write in markdown."
            ),
        },
        {
            "role": "user",
            "content": (
                f"Section title: {task_data.get('title', 'Section')}\n"
                f"Goal: {task_data.get('goal', '')}\n\n"
                f"Evidence:\n{evidence_text or 'None.'}"
            ),
        },
    ]

    response = llm.invoke(messages)
    section_md = response.content

    return {"sections": [(task_data["id"], section_md)]}


# ── 5. Merge Content ────────────────────────────────────────────────────────


def merge_content(state: State) -> dict:
    """Sort sections, merge into full markdown, convert to HTML."""

    plan = state["plan"]
    sorted_sections = sorted(state["sections"], key=lambda x: x[0])

    merged_md = f"# {plan.blog_title}\n\n"
    merged_md += "\n\n".join(section for _, section in sorted_sections)

    html_content = md_lib.markdown(
        merged_md,
        extensions=["extra", "codehilite", "tables"],
    )

    return {
        "merged_md": merged_md,
        "final": html_content,
    }


# ── Routing functions ───────────────────────────────────────────────────────


def route_next(state: State) -> str:
    """After router, go to research if needed, otherwise straight to orchestrator."""
    if state.get("needs_research"):
        return "research"
    return "orchestrator"


def fanout_tasks(state: State) -> list[Send]:
    """Fan out to worker nodes — one Send per task."""
    plan = state["plan"]
    evidence = [e.model_dump() for e in state.get("evidence", [])]

    sends = []
    for task in plan.tasks:
        sends.append(
            Send(
                "worker",
                {
                    "task": task.model_dump(),
                    "plan": plan.model_dump(),
                    "evidence": evidence,
                },
            )
        )
    return sends


# ── Graph wiring ─────────────────────────────────────────────────────────────

g = StateGraph(State)

g.add_node("router", router_node)
g.add_node("research", research_node)
g.add_node("orchestrator", orchestrator_node)
g.add_node("worker", worker_node)
g.add_node("merge_content", merge_content)

g.add_edge(START, "router")
g.add_conditional_edges("router", route_next, {"research": "research", "orchestrator": "orchestrator"})
g.add_edge("research", "orchestrator")
g.add_conditional_edges("orchestrator", fanout_tasks, ["worker"])
g.add_edge("worker", "merge_content")
g.add_edge("merge_content", END)

app = g.compile()
