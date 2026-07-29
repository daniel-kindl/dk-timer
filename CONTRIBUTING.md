# Contributing to EMOM Timer

Thank you for helping improve EMOM Timer!

---

## Branch Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Stable production code. **Protected — no direct pushes.** |
| `dev`  | Active development. All work goes here. |

**All commits go to `dev`.**
`main` is only updated via tagged releases.

---

## Commit Convention

Every commit **must** follow [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/).
This is enforced, not just a suggestion:

- A local `commit-msg` hook rejects non-conforming commits. Enable it once
  per clone:
  ```bash
  git config core.hooksPath .githooks
  ```
- `.github/workflows/commit-lint.yml` re-validates every commit in a PR in
  CI, since the local hook can be bypassed with `--no-verify`.

```
<type>(<scope>)!: <short description>

[optional body]

[optional footer(s)]
```

Allowed types: `feat`, `fix`, `build`, `chore`, `ci`, `docs`, `style`,
`refactor`, `perf`, `test`, `revert`. Scope is optional. A `!` right before
the colon (or a `BREAKING CHANGE:` footer) marks a breaking change and
requires a MAJOR release (see below).

Examples:
```
feat(engine): add drift-free interval scheduling
fix(audio): prevent overlapping tones on rapid intervals
test(engine): cover non-divisible duration edge case
feat(setup)!: remove deprecated preset import format
```

---

## Development Workflow

1. Branch off `dev`:
   ```bash
   git checkout dev
   git pull origin dev
   git checkout -b feat/my-feature
   ```

2. Implement your change.

3. Run checks locally:
   ```bash
   ./gradlew testDebugUnitTest detekt
   ```

4. Open a PR targeting `dev`. `commit-lint` CI validates every commit in
   the PR.

5. After your branch is merged into `dev`, delete it (locally and on
   origin). Feature branches are disposable; `dev` and `main` are the only
   long-lived branches and are never deleted.

---

## Release Process

Releases follow **Semantic Versioning 2.0.0** (`MAJOR.MINOR.PATCH`,
see [semver.org](https://semver.org/)):

- `MAJOR` — breaking changes (any commit with `!` or a `BREAKING CHANGE:` footer)
- `MINOR` — new features (`feat:` commits), backwards compatible
- `PATCH` — bug fixes and everything else

**Steps to release:**

1. Ensure `dev` CI is green.
2. Update `CHANGELOG.md` with the new version section.
3. Bump `versionName` / `versionCode` in `app/build.gradle.kts`. The new
   `versionName` must exactly match the tag you'll push in step 5.
4. Merge `dev` → `main` via PR.
5. Tag the merge commit:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
6. The `release` workflow validates the release before building anything:
   - the tag is strict SemVer (`vMAJOR.MINOR.PATCH`),
   - it's strictly greater than the previous tag,
   - it matches `versionName` in `app/build.gradle.kts`,
   - its bump level (major/minor/patch) is at least what the commits since
     the last tag require under Conventional Commits (a breaking commit
     needs a major release, a `feat:` needs at least a minor, etc.).

   If all of that passes, it builds a signed `assembleRelease` APK and
   publishes a GitHub Release automatically.

**Required repository secrets** (Settings → Secrets and variables →
Actions) for the release workflow to sign the APK:

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | Your release keystore file, base64-encoded (`base64 -w0 release.keystore`) |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Signing key alias |
| `KEY_PASSWORD` | Signing key password |

Without these, the release workflow fails clearly at the signing step
instead of silently shipping an unsigned or debug build.

---

## Code Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- `detekt` is enforced in CI — run `./gradlew detekt` before pushing
- No business logic in UI layer
- Domain layer must remain Android-free (pure Kotlin)
