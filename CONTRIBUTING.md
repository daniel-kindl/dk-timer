# Contributing to Ocho

---

## Branch strategy

| Branch | Purpose |
|--------|---------|
| `main` | Stable production code. Protected, so no direct pushes. |
| `dev`  | Active development. All work goes here. |

All commits go to `dev`. `main` is only updated via tagged releases.

Every green push to `dev` publishes a dev-channel APK automatically. See
[Update channels](#update-channels) below.

---

## Contributor terms

Ocho is licensed under GPL-3.0, and Daniel Kindl is its sole copyright holder. That
second fact is deliberate, and it has a consequence worth stating plainly before you
write any code.

Copyright is automatic and attaches per author. If you send a patch, you own the
copyright in it, and merging does not transfer that. The project would only hold a
GPL licence to your work, and a GPL licence does not permit relicensing. One merged
contribution would therefore remove the ability to offer Ocho under any other terms
without tracking you down for permission.

So, to keep that option open:

> By submitting a contribution to this project, you agree that your contribution is
> licensed under GPL-3.0, and you grant Daniel Kindl a perpetual, irrevocable,
> worldwide, royalty-free licence to use, reproduce, modify, sublicense and
> relicense it, including under commercial terms.

You keep your copyright. Nothing is assigned. You are granting a licence broad
enough that the project can be dual-licensed later.

Qt, MySQL, GitLab and every project under the Apache ICLA use the same arrangement,
and it is stated up front precisely so nobody is surprised by it. If you would
rather not grant that, open an issue describing the change instead of a pull
request. A good bug report is worth as much as a patch.

Anything already released under GPL-3.0 stays GPL-3.0 permanently. Relicensing can
only ever apply going forward, so nothing you or anyone else has already received
can be withdrawn.

---

## Commit convention

Every commit must follow [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/).
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
the colon, or a `BREAKING CHANGE:` footer, marks a breaking change and
requires a MAJOR release (see below).

Examples:
```
feat(engine): add drift-free interval scheduling
fix(audio): prevent overlapping tones on rapid intervals
test(engine): cover non-divisible duration edge case
feat(setup)!: remove deprecated preset import format
```

---

## Development workflow

1. Branch off `dev`:
   ```bash
   git checkout dev
   git pull origin dev
   git checkout -b feat/my-feature
   ```

2. Implement your change.

3. Run checks locally:
   ```bash
   ./gradlew check      # tests + detekt + lint
   ```

   All three run with warnings-as-errors, and detekt requires KDoc on every
   public declaration in `src/main`. Tests are exempt.

4. Open a PR targeting `dev`. `commit-lint` CI validates every commit in
   the PR.

5. After your branch is merged into `dev`, delete it locally and on
   origin. Feature branches are disposable. `dev` and `main` are the only
   long-lived branches and are never deleted.

---

## Update channels

Ocho ships outside Google Play and updates itself from GitHub Releases. There are
two channels, and they are invisible to each other by construction:

| Channel | `applicationId` | Reads | Published by |
|---------|-----------------|-------|--------------|
| Stable | `dev.danielkindl.ocho` | `releases/latest` | `release.yml`, on a version tag |
| Dev | `dev.danielkindl.ocho.dev` | Newest prerelease | `dev-ci.yml`, on every push to `dev` |

`releases/latest` excludes prereleases by GitHub's own definition, so a stable
install can never be offered a dev build. The differing `applicationId` means both
apps can be installed at once, with separate presets and settings.

Dev builds are versioned `<versionName>-dev.<CI run number>`, signed with the
release key, and pruned to the newest five. The release key matters here: CI's debug
keystore is regenerated per run, so dev APKs signed with it would refuse to install
over each other.

Dev and debug builds also ship a handful of built-in presets, so a device check is one
tap rather than four picker wheels. They live in `DEVICE_CHECK_PRESETS`, are merged in
on read by `WorkoutPresetRepositoryImpl` and never written, and carry no delete control.
The EMOM entries are deliberately the awkward cases rather than realistic workouts: an
exact multiple of the interval, a remainder longer than the 3s lead-in, a remainder
shorter than it, and an interval that outlasts the whole workout. Those four are where
the lead-in and the final numeral are decided.

`di/AppModule` hands the repository an empty list on the stable channel, so the presets
never appear there. The definitions are still compiled into the stable APK as unused
data; keeping them in the shared source set is what lets them be unit-tested, and a few
hundred bytes is a fair price.

Two guards in `release.yml` exist because dev tags contain a hyphen. Don't remove
them:

- The job skips any ref matching `*-*`. `release.yml` triggers on `v*`, which dev
  tags also match, and GitHub tag filters can't express an exception. Without the
  guard, every push to `dev` would fail the release workflow.
- Both `git describe` calls pass `--exclude='*-*'`, or a dev prerelease would
  resolve as the previous tag and corrupt the version checks and release notes.

---

## Release process

Releases follow [Semantic Versioning 2.0.0](https://semver.org/)
(`MAJOR.MINOR.PATCH`):

- `MAJOR` for breaking changes: any commit with `!` or a `BREAKING CHANGE:` footer
- `MINOR` for new features (`feat:` commits), backwards compatible
- `PATCH` for bug fixes and everything else

Steps to release:

1. Ensure `dev` CI is green.
2. Update `CHANGELOG.md` with the new version section.
3. Bump `versionName` and `versionCode` in `app/build.gradle.kts`. The new
   `versionName` must exactly match the tag you'll push in step 5.
4. Merge `dev` into `main` via PR.
5. Tag the merge commit:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
6. The `release` workflow validates the release before building anything:
   - the tag is strict SemVer (`vMAJOR.MINOR.PATCH`),
   - it's strictly greater than the previous tag,
   - it matches `versionName` in `app/build.gradle.kts`,
   - its bump level is at least what the commits since the last tag require
     under Conventional Commits. A breaking commit needs a major release, a
     `feat:` needs at least a minor.

   If all of that passes, it builds a signed `assembleRelease` APK and
   publishes a GitHub Release automatically.

Required repository secrets (Settings, then Secrets and variables, then
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

## Code style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- `detekt` is enforced in CI, so run `./gradlew detekt` before pushing
- No business logic in the UI layer
- The domain layer must remain Android-free (pure Kotlin)
