# Metrics

How Ocho's usage is measured, what the numbers mean, and what they cannot tell you.

The app collects nothing. There is no SDK, no event tracking, no identifier, and no
server of ours for anything to be sent to. Every figure here comes from counters
GitHub already keeps about a public repository, read from CI once a day. That is the
whole system, and it is why [SECURITY.md](../SECURITY.md)'s "no accounts, no backend,
no analytics, and no telemetry" is still literally true.

## Why bother collecting at all

Because the numbers expire. A release asset's `download_count` is cumulative and never
says *when* the downloads happened, and GitHub's traffic endpoints retain only fourteen
days. Neither can be reconstructed later. Snapshotting daily costs nothing and is the
only way the shape of early growth survives; skipping it destroys data permanently.

## How it works

[`.github/workflows/stats.yml`](../.github/workflows/stats.yml) runs
[`.github/scripts/collect_stats.py`](../.github/scripts/collect_stats.py) on a daily
schedule and commits three CSVs to the orphan
[`stats`](https://github.com/daniel-kindl/ocho/tree/stats) branch.

The branch is orphaned deliberately. `release.yml` derives the required SemVer bump
from the Conventional Commits since the last tag, and `CONTRIBUTING.md` has all work
branching off `dev`; a daily bot commit in that history would be swept into both the
bump analysis and every release diff.

| File | Key | Contents |
|------|-----|----------|
| `downloads.csv` | `date, tag, asset` | Cumulative download count per release asset |
| `traffic.csv` | `date` | Views, unique visitors, clones, unique cloners |
| `repo.csv` | `date` | Stars, forks, watchers, open issues |

`downloads.csv` is a full daily snapshot, so a release's downloads *on* a given day are
the difference between consecutive rows for that asset. Rows are upserted by key rather
than appended: the most recent traffic day is always partial, so a later run has to be
able to correct a row it wrote earlier. Re-running the collector any number of times on
the same day converges rather than duplicating.

Days with no traffic are written as explicit zeros, which keeps "nobody visited"
distinguishable from "the collector did not run".

## Reading the numbers

**Stable APK downloads are the closest thing to an active-install count**, and that is
specific to how Ocho updates. `UpdateRepositoryImpl.kt` takes the release asset's
`browser_download_url` and `UpdateDownloader.kt` hands it to `DownloadManager`. That URL
is the endpoint GitHub counts. Only a device that already has Ocho installed, launched
it, and accepted the update ever requests it — so the count of `app-release.apk` for a
given release approximates the number of live installs that took that update, not the
number of strangers who clicked a link.

Comparing consecutive stable releases therefore gives a crude active-install curve, and
a release's count plateauing says its rollout has finished.

**Unique visitors and unique cloners** measure discovery, not use. They are the signal
for whether anything you did — a post, a link, a search ranking — actually reached
anyone.

## What these numbers are not

Be careful here, because the download figure is easy to over-read in both directions.

It is **inflated** by crawlers, mirrors and archivers that pull release assets; by your
own manual downloads and any CI fetch; and by one device reinstalling or moving between
channels, which counts twice.

It is **deflated**, more severely, by Ocho's own design. An available update is only
visible if the user opens Settings — there is no notification and no badge
(`UpdateViewModel` seeds its state from `UpdateCheckCache`, and only `SettingsScreen`
renders it). A user who never opens Settings never updates, and so never appears again
after their first download. Treat stable APK downloads as a **lower bound on active
installs**, and trust the trend far more than the absolute value.

And it is silent on everything below the install:

- Daily or monthly actives
- Retention, or whether anyone came back after week one
- Which of EMOM, Tabata and AMRAP people actually use
- Whether workouts get finished or abandoned
- Session length, or time of day

None of that is observable without instrumenting the app. That is the trade, and for
now it is the right side of it.

## If an active-user count is ever needed

Not yet. As of the first snapshot the repository had one star, nine downloads of stable
APKs across twelve stable releases, and eleven downloads across all release assets. The
answer to "how many users" is countable by hand, and no measurement system would change
it.

Build this only when a concrete decision depends on it — raising `minSdk` above 26,
retiring a mode, or deciding whether maintenance is still worth it. A rough proxy for
"hand-counting has stopped working" is a sustained hundred-plus downloads per stable
release.

The design, if that day comes:

Once per calendar day, a release build fetches a few-byte `pulse.txt` attached by
`release.yml` to the release matching its own version. GitHub increments that asset's
public `download_count`; the daily delta is daily actives, already split by version.

- **No backend.** GitHub stays the only counterpart, so nothing new sits on the update
  path and SECURITY.md's trust model is untouched.
- **No identifier.** No UUID, no advertising ID, no fingerprint. State is a single
  epoch-day `Long` in the existing DataStore, used only to rate-limit. It identifies
  nothing and cannot be correlated across days.
- **No new party and no new permission.** Same host, same `INTERNET`, about forty bytes
  a day.
- **Publicly auditable.** Anyone can read the same counter the maintainer can.

Shape of the work: a `PulseReporter` in `data/update/` called from `OchoApp` beside
`checkForUpdateOnStart()`, gated on `UpdateChannel.Stable` and on a new opt-in
`pulseEnabled` setting threaded through the usual five files (`UserSettings` →
`SettingsRepository` → `SettingsRepositoryImpl` → `SettingsViewModel` →
`SettingsScreen`). Failures silent, matching the update check.

What it would cost, stated plainly:

- SECURITY.md's "no analytics, and no telemetry" becomes false and has to be rewritten.
  A once-daily unidentified fetch is about as mild as telemetry gets, but calling it
  something else would be spin.
- Opt-in and default-off means a privacy-minded audience opts in at a low rate, so the
  absolute number is meaningless and only the trend is readable.
- It counts launches that crossed a day boundary. It still says nothing about whether
  anyone finishes a workout.
- The usage figures become public to everyone, not just the maintainer.

Given all four, deciding never to build it is a legitimate outcome — and one that turns
a promise already being kept into something worth advertising.

## Running the collector by hand

Stdlib only, no `gh` CLI, no install step:

```
python3 .github/scripts/collect_stats.py --out /tmp/ocho-stats
```

Without a token the release and repository numbers still collect; the traffic endpoints
need push access and are skipped with a warning. In CI the workflow's `GITHUB_TOKEN` is
used. If it turns out not to reach the traffic endpoints, the fallback is a fine-grained
PAT with *Administration: read* in a repository secret — the script reads whichever
token it is given.
