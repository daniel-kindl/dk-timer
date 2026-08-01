# Security policy

## Supported versions

| Version | Supported |
|---------|-----------|
| Latest stable release | Yes |
| Older stable releases | No |
| `-dev.*` prereleases | No. Testing builds, with no guarantees of any kind. |

Fixes go into the next release. There are no backports.

## Reporting a vulnerability

Report privately through GitHub's
[Report a vulnerability](https://github.com/daniel-kindl/ocho/security/advisories/new)
form, under the repository's Security tab. Please don't open a public issue for
something exploitable.

This is a personal project maintained by one person, so expect a reply in days
rather than hours.

## What's actually worth attacking

Ocho is a workout timer with no accounts, no backend, no analytics, and no
telemetry. It makes exactly one kind of network request, and that request is also
its entire meaningful attack surface, so it's worth being specific about it.

The app updates itself by downloading and installing an APK. It holds `INTERNET`
and `REQUEST_INSTALL_PACKAGES`, polls the GitHub Releases API for
`daniel-kindl/ocho`, downloads a release asset via Android's `DownloadManager`, and
installs it through `PackageInstaller`. Anything that subverts that chain replaces
the app on the device.

The trust model:

- Transport is HTTPS to `api.github.com` and `objects.githubusercontent.com`. A
  network attacker cannot substitute an APK without breaking TLS.
- Integrity rests on Android's signature check, not on anything this app does.
  Release APKs are signed with a private key held only by the maintainer, and
  Android refuses to install an update whose signature does not match the installed
  app. A substituted or modified APK fails to install rather than silently
  replacing Ocho. That is the guarantee that matters, and it's why the app does not
  verify checksums itself: the platform check is both stronger and unavoidable.
- Channel separation means a stable install cannot be moved onto dev builds. Stable
  reads `releases/latest`, which GitHub defines as excluding prereleases, and the
  dev channel uses a separate `applicationId` and installs as a distinct app.

Findings in that flow are the ones most worth reporting. So is anything that allows
an unsigned or third-party APK to be installed, or lets a non-GitHub host serve the
update.

## Out of scope

- The app stores no credentials, tokens, or personal data. Presets and settings are
  non-sensitive local `DataStore` values.
- "Install unknown apps" must be granted by the user for updates to work. That
  prompt is Android's, and being asked for it is intended behaviour.
- Sideloading and rooted-device attacks. An attacker who can already install
  arbitrary packages does not need this app.
- Dependency versions are tracked by Dependabot. A known CVE in a transitive
  dependency is welcome as a normal issue rather than a private report, unless it
  is actually reachable from Ocho's code.
