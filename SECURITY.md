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
([PRIVACY.md](PRIVACY.md) covers the same ground from the user's side; this section
is about what an attacker would go after.)

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

## Verifying a release download

Every release and dev prerelease publishes two things next to the APK: a
`.sha256` file, and a GitHub build provenance attestation. Both are for a human
checking a manual download. Neither changes how the app updates itself, which
still relies on Android's signature check as described above.

### Checksum

The SHA-256 digest is attached as `<apk-name>.sha256`, quoted in the release
notes, and printed in the log of the workflow run that produced the build. To
check a download:

```
sha256sum -c ocho-1.2.3.apk.sha256
```

Or compare by eye against the digest in the release notes:

```
sha256sum ocho-1.2.3.apk
```

On Windows: `certutil -hashfile ocho-1.2.3.apk SHA256`.

What this proves is limited. It detects a corrupted download and an asset that
does not match what the release notes claim. It does not defend against someone
who has compromised the repository, because that person can replace the APK and
the `.sha256` file and the release notes in one move. A checksum published by
the same party as the artifact only ever confirms internal consistency.

The workflow log is a slightly better witness than the release assets, since
release assets can be re-uploaded after the fact while a finished run's log
cannot be edited through the normal release UI. It is a second place to look,
not a proof.

### Build provenance attestation

This is the check worth actually running. Each APK is attested with
[actions/attest-build-provenance](https://github.com/actions/attest-build-provenance),
which produces a signed statement binding the artifact's digest to the workflow
run, commit, and repository that built it. The signature comes from GitHub's
signing infrastructure using a short-lived OIDC identity for that specific run,
so it is not something a repository write token can forge after the fact.

Verify with the GitHub CLI:

```
gh attestation verify ocho-1.2.3.apk --repo daniel-kindl/ocho
```

A successful result means that exact file was built by a run of this
repository's own workflow. A file that was modified, rebuilt elsewhere, or
served by a third party fails, even if its accompanying checksum matches.

### Signature check

Android's signature check remains the guarantee that matters for installation
itself. An APK not signed with the release key cannot upgrade an existing
install, regardless of what any checksum or attestation says. The checks above
are for inspecting a file before you install it, and for detecting tampering on
a machine that has never had Ocho installed. To read the signing certificate of
a downloaded APK directly:

```
apksigner verify --print-certs ocho-1.2.3.apk
```

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
