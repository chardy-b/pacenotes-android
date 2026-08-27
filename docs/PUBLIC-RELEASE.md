# Public release checklist

Complete this checklist before making the repository public and before every phone-test APK release.

## Repository visibility

1. Review the public branch history for credentials, private data, and unlicensed material.
2. Confirm that `LICENSE` and `NOTICE` remain accurate.
3. Change repository visibility only after the public-release pull request is merged.
4. Immediately configure the `phone-test-release` GitHub Environment with the approved maintainer as a required reviewer. The private-repository plan rejects required-reviewer rules; GitHub enables them after the repository becomes public.
5. Read the environment back through GitHub settings or the API. Confirm the required reviewer and `prevent self-review` are enabled.

## Phone-test APK release

1. Confirm the release workflow runs only from an approved actor and targets `phone-test-release`.
2. Confirm an approved reviewer has authorized the environment deployment.
3. Generate a version-pinned third-party notice bundle from the exact resolved runtime dependency graph.
4. Review the notice bundle, attach it to the GitHub Release with the unchanged approved APK and scenario screenshots, and link the release evidence in the pull request and Linear.
5. Do not publish if the environment rules or third-party notice bundle are absent.