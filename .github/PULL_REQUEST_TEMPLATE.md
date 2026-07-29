## What & why

<!-- Briefly describe the change and the problem it solves. -->

## Checklist

- [ ] Targets `dev` (not `main`)
- [ ] Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)
- [ ] `./gradlew testDebugUnitTest` passes locally
- [ ] `./gradlew detekt` is clean locally
- [ ] `CHANGELOG.md` updated under `[Unreleased]` (if user-facing)
- [ ] Domain layer changes remain Android-free; no business logic added to `ui/`
