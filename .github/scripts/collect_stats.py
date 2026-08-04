#!/usr/bin/env python3
"""Snapshot the usage signals GitHub already keeps about this repository.

This exists because those signals are perishable. A release asset's
``download_count`` is cumulative and never says *when* the downloads happened,
and the traffic endpoints retain only fourteen days. Recording them daily is the
only way the shape of early growth survives at all.

Nothing here observes a user. Every figure comes from GitHub's own counters,
read from CI, so the app keeps making exactly one kind of network request and
SECURITY.md's "no analytics, and no telemetry" stays literally true.

Stdlib only, and no ``gh`` CLI, so the same file runs in Actions and on a laptop
with no setup. ``GITHUB_TOKEN`` is optional: without it the release and repo
numbers still collect and the traffic endpoints, which require push access, are
skipped with a warning rather than failing the run.
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import sys
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone
from pathlib import Path

API_ROOT = "https://api.github.com"
USER_AGENT = "ocho-stats-collector"
TIMEOUT_SECONDS = 30

# GitHub's traffic endpoints return a rolling fourteen-day window. Re-reading the
# whole window on every run is deliberate: the most recent day is always partial,
# so later runs must be able to correct rows they wrote earlier.
TRAFFIC_WINDOW_DAYS = 14

DOWNLOAD_FIELDS = ["date", "tag", "prerelease", "asset", "download_count"]
TRAFFIC_FIELDS = ["date", "views", "unique_views", "clones", "unique_clones"]
REPO_FIELDS = ["date", "stars", "forks", "watchers", "open_issues"]


def request_json(path: str, token: str | None) -> tuple[object, dict[str, str]]:
    """Fetch one API page, returning its parsed body and headers."""
    request = urllib.request.Request(f"{API_ROOT}{path}")
    request.add_header("Accept", "application/vnd.github+json")
    request.add_header("X-GitHub-Api-Version", "2022-11-28")
    request.add_header("User-Agent", USER_AGENT)
    if token:
        request.add_header("Authorization", f"Bearer {token}")

    with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
        body = json.loads(response.read().decode("utf-8"))
        return body, dict(response.headers)


def next_page(headers: dict[str, str]) -> str | None:
    """Extract the ``rel="next"`` target from a Link header, if there is one."""
    link = headers.get("Link") or headers.get("link")
    if not link:
        return None
    for part in link.split(","):
        segments = part.split(";")
        if len(segments) < 2:
            continue
        if 'rel="next"' in "".join(segments[1:]):
            url = segments[0].strip().strip("<>")
            return url[len(API_ROOT):] if url.startswith(API_ROOT) else url
    return None


def fetch_all(path: str, token: str | None) -> list:
    """Follow Link pagination to the end and concatenate the pages."""
    items: list = []
    cursor: str | None = path
    while cursor:
        page, headers = request_json(cursor, token)
        if not isinstance(page, list):
            raise TypeError(f"Expected a list from {cursor}, got {type(page).__name__}")
        items.extend(page)
        cursor = next_page(headers)
    return items


def fetch_optional(path: str, token: str | None) -> object | None:
    """Fetch an endpoint whose absence is tolerable, returning None on refusal.

    The traffic endpoints need push access. Rather than failing a scheduled run
    when the token cannot reach them, degrade to collecting everything else and
    say so on stderr.
    """
    try:
        body, _ = request_json(path, token)
        return body
    except urllib.error.HTTPError as error:
        if error.code in (401, 403, 404):
            print(
                f"warning: {path} returned {error.code}; skipping. "
                "Traffic data needs a token with push access "
                "(fine-grained PAT with Administration: read).",
                file=sys.stderr,
            )
            return None
        raise


def collect_downloads(repo: str, token: str | None, run_date: str) -> list[dict]:
    """Snapshot every release asset's cumulative download count.

    The counts matter more here than they would for most projects: the in-app
    updater hands ``browser_download_url`` to DownloadManager, and that URL is
    the endpoint GitHub counts. A stable release's APK count is therefore an
    estimate of how many live installs took that update, not just how many
    strangers clicked a link.
    """
    releases = fetch_all(f"/repos/{repo}/releases?per_page=100", token)
    rows = []
    for release in releases:
        for asset in release.get("assets", []):
            rows.append(
                {
                    "date": run_date,
                    "tag": release["tag_name"],
                    "prerelease": str(bool(release.get("prerelease"))).lower(),
                    "asset": asset["name"],
                    "download_count": asset.get("download_count", 0),
                }
            )
    return rows


def collect_traffic(repo: str, token: str | None, run_date: str) -> list[dict]:
    """Snapshot the fourteen-day view and clone window, zero-filling quiet days.

    GitHub omits days with no activity. Emitting an explicit zero keeps "nobody
    visited" distinguishable from "the collector did not run".
    """
    views = fetch_optional(f"/repos/{repo}/traffic/views", token)
    clones = fetch_optional(f"/repos/{repo}/traffic/clones", token)
    if views is None and clones is None:
        return []

    def by_day(payload: object) -> dict[str, dict]:
        if not isinstance(payload, dict):
            return {}
        return {entry["timestamp"][:10]: entry for entry in payload.get("views", payload.get("clones", []))}

    view_days = by_day(views)
    clone_days = by_day(clones)

    end = datetime.strptime(run_date, "%Y-%m-%d").date()
    rows = []
    for offset in range(TRAFFIC_WINDOW_DAYS - 1, -1, -1):
        day = (end - timedelta(days=offset)).isoformat()
        view = view_days.get(day, {})
        clone = clone_days.get(day, {})
        rows.append(
            {
                "date": day,
                "views": view.get("count", 0),
                "unique_views": view.get("uniques", 0),
                "clones": clone.get("count", 0),
                "unique_clones": clone.get("uniques", 0),
            }
        )
    return rows


def collect_repo(repo: str, token: str | None, run_date: str) -> list[dict]:
    """Snapshot the headline repository counters."""
    body, _ = request_json(f"/repos/{repo}", token)
    if not isinstance(body, dict):
        raise TypeError("Expected an object from the repository endpoint")
    return [
        {
            "date": run_date,
            "stars": body.get("stargazers_count", 0),
            "forks": body.get("forks_count", 0),
            "watchers": body.get("subscribers_count", 0),
            "open_issues": body.get("open_issues_count", 0),
        }
    ]


def upsert(path: Path, fields: list[str], key: list[str], rows: list[dict]) -> None:
    """Merge rows into a CSV by primary key, correcting rather than appending.

    Blind appending would bake in the partial counts that a same-day re-run is
    meant to fix, and would duplicate every row on a manual re-run.
    """
    if not rows:
        return

    existing: list[dict] = []
    if path.exists():
        with path.open(newline="", encoding="utf-8") as handle:
            existing = list(csv.DictReader(handle))

    merged: dict[tuple, dict] = {}
    for row in existing:
        normalised = {field: row.get(field, "") for field in fields}
        merged[tuple(normalised[k] for k in key)] = normalised
    for row in rows:
        normalised = {field: str(row[field]) for field in fields}
        merged[tuple(normalised[k] for k in key)] = normalised

    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, lineterminator="\n")
        writer.writeheader()
        for identity in sorted(merged):
            writer.writerow(merged[identity])


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--out",
        type=Path,
        default=Path("stats"),
        help="Directory to write the CSVs into (default: stats)",
    )
    parser.add_argument(
        "--repo",
        default=os.environ.get("GITHUB_REPOSITORY", "daniel-kindl/ocho"),
        help="owner/name to collect (default: $GITHUB_REPOSITORY)",
    )
    parser.add_argument(
        "--date",
        default=datetime.now(timezone.utc).strftime("%Y-%m-%d"),
        help="UTC date to record the snapshot under (default: today)",
    )
    args = parser.parse_args()

    token = os.environ.get("GITHUB_TOKEN") or None
    if not token:
        print("warning: no GITHUB_TOKEN; traffic data will be skipped.", file=sys.stderr)

    downloads = collect_downloads(args.repo, token, args.date)
    traffic = collect_traffic(args.repo, token, args.date)
    repo = collect_repo(args.repo, token, args.date)

    upsert(args.out / "downloads.csv", DOWNLOAD_FIELDS, ["date", "tag", "asset"], downloads)
    upsert(args.out / "traffic.csv", TRAFFIC_FIELDS, ["date"], traffic)
    upsert(args.out / "repo.csv", REPO_FIELDS, ["date"], repo)

    total = sum(int(row["download_count"]) for row in downloads)
    print(
        f"{args.repo} {args.date}: {len(downloads)} assets, {total} downloads to date, "
        f"{len(traffic)} traffic days"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
