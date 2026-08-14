# Pacenotes GitHub Delivery

Use this procedure for every material Pacenotes change. Linear is the lifecycle source of truth; the pull request and exact GitHub Actions runs are the engineering evidence.

## 1. Classify the delivery gate

Record one classification in the Linear issue and PR:

- **Baseline-only:** documentation, pure Kotlin/domain logic, build configuration, or other work whose acceptance does not depend on Android runtime or rendered pixels.
- **Device gate required:** UI/Compose, permissions, location/GPS replay, foreground service, audio/TTS, MapLibre rendering, lifecycle, or any Android-runtime-specific behavior.
- **Release requested:** the user explicitly wants a phone-installable APK after successful device evidence is reviewed.

When uncertain, use **device gate required**. A screenshot filename, semantic assertion, or build success does not replace visual inspection when pixels are part of acceptance.

## 2. Prepare the pull request

1. Use the Linear issue identifier in the feature branch and PR title.
2. Reconcile the branch against current `origin/main`.
3. Run static checks that do not invoke Gradle locally, including workflow parsing and `git diff --check`.
4. Obtain an independent skeptical review of the exact diff before pushing.
5. Push only the reviewed feature branch; never push directly to `main`.
6. Open or update a PR targeting `main`. Do not enable auto-merge.

## 3. Verify baseline CI

The `Android baseline / build and unit tests` job is authoritative for the exact PR head. It must:

- run `:app:assembleDebug`;
- run `:app:testDebugUnitTest`, `:core-model:test`, and `:pacenotes:test`;
- upload `app-debug.apk` for 14 days with missing files treated as an error.

Read the exact run and artifact metadata. A prior SHA or branch-level green state does not satisfy this gate.

## 4. Run the device gate when required

After baseline CI passes, manually dispatch `Android device gate` from the feature-branch ref whose head is the exact candidate commit, with:

- that same full 40-character current PR-head SHA;
- the Linear issue identifier or evidence label.

The selected workflow ref and requested commit must resolve to the same SHA so the later release gate can verify both the workflow run and tested artifact.

The generic device gate verifies the APK, instrumentation suite, app launch, XML, logs, device state, and evidence manifest on API 35. Ticket-specific tests remain responsible for ticket-specific states and screenshots. Do not add one ticket's GPS route, camera mode, or screenshot assertions to the universal gate.

Download and inspect the exact-run artifact. Required conclusions are separate:

- **Automated:** build, installation, instrumentation, and process launch passed.
- **Device:** the requested runtime behavior executed on API 35.
- **Visual:** each ticket-required screenshot visibly proves its claim.

Link the exact run and artifact in both the PR and Linear issue.

## 5. Publish a phone-test prerelease only on request

After human review of successful device evidence, manually dispatch `Phone-test prerelease` with:

- the successful device-gate run ID;
- its exact 40-character commit SHA;
- a new release tag;
- a human-readable release name.

The release workflow must verify the source run, commit, workflow-ref SHA, artifact name, manifest, APK size, and SHA-256, then publish that unchanged APK. It must not rebuild. Release-tag concurrency is serialized; an existing tag is reusable only when it resolves to the same verified commit, so a failed or interrupted publication can be retried safely. Physical-device confirmation remains user evidence and is not implied by emulator success.

## 6. Merge and close

Prepare the PR for human review, but do not merge without explicit user instruction. After a human merge:

1. verify the merge commit on `main`;
2. verify the authoritative baseline check on the merged ref;
3. update Linear with the PR, exact runs, artifacts, and merge evidence;
4. move the issue to Done only when every required gate passed.

## Non-goals

Do not add CI matrices, OIDC/cloud deployment, canary deployment, self-hosted runners, automatic releases, mandatory emulator runs on every PR, direct `main` pushes, or automatic merge unless a separately approved issue requires them.
