# Public release checklist

Complete the applicable section before making the repository public or publishing an APK.

## Repository visibility

1. Review the public branch history for credentials, private data, and unlicensed material.
2. Confirm that `LICENSE` and `NOTICE` remain accurate.
3. Change repository visibility only after the public-release pull request is merged.
4. Immediately configure the `phone-test-release` GitHub Environment with the approved maintainer as a required reviewer. The private-repository plan rejects required-reviewer rules; GitHub enables them after the repository becomes public.
5. Read the environment back through GitHub settings or the API. Confirm the required reviewer and `prevent self-review` are enabled.

## Automated main-build prerelease

1. The Android baseline publishes a prerelease only after its `main` push build and unit tests succeed; pull requests cannot enter the publish job.
2. The release downloads the unchanged baseline artifact rather than rebuilding the APK.
3. Attach `app-debug.apk`, its SHA-256 checksum, `apk-provenance.json`, the resolved `debugRuntimeClasspath` third-party notice bundle, `LICENSE`, and `NOTICE`.
4. An existing release tag must resolve to the exact merge commit; retries may replace the same release assets but never move a tag.
5. Treat it as an automated debug build, not a phone-tested, device-gated, or human-reviewed release.
6. Do not use it as evidence of device readiness or distribute it as an approved phone-test build.

## Phone-test APK release

1. Confirm the release workflow runs only from an approved actor and targets `phone-test-release`.
2. Confirm an approved reviewer has authorized the environment deployment.
3. Generate a version-pinned third-party notice bundle from the exact resolved runtime dependency graph.
4. Review the notice bundle, attach it to the GitHub Release with the unchanged approved APK and scenario screenshots, and link the release evidence in the pull request and Linear.
5. Do not publish if the environment rules or third-party notice bundle are absent.