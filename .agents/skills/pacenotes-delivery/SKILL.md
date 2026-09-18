---
name: pacenotes-delivery
description: "Use when developing, reviewing, delivering, or releasing Pacenotes changes; applies the repository's Linear, CI-only Gradle, emulator-evidence, APK, and release policies."
version: 1.0.0
metadata:
  hermes:
    tags: [pacenotes, android, github-actions, delivery, release]
    related_skills: [brokered-git-delivery, android-application-qa, linear-project-management]
---

# Pacenotes Delivery

## Scope

This is the repository-local Pacenotes overlay. It does not replace generic GitHub delivery or official Google Android guidance. Read `../../../AGENTS.md`, the active Linear issue, and `../../../docs/PACENOTES-GITHUB-DELIVERY.md` first.

Use the scoped `androiddev` Hermes profile when official Google Android implementation guidance is needed. Do not copy or rewrite those upstream skills here.

## Workflow

1. Confirm the live Linear issue, acceptance criteria, dependencies, and gate classification. Do not fabricate a ticket.
2. Work from current `origin/main` on the active WIL issue branch. If no real Linear issue covers material work, stop and define one before implementation. Never push directly to `main`.
3. Do not run Gradle locally. Run only non-Gradle static checks on the host, including workflow parsing and `git diff --check`.
4. Use risk-appropriate exact-diff review. Independent review is mandatory for elevated-risk security, credential, public-exposure, destructive-migration, or concurrency changes.
5. Deliver through a PR and verify `Android baseline / build and unit tests` on the exact PR head. Verify the retained `app-debug.apk` artifact.
6. For device-required changes, dispatch `Android device gate` only after baseline passes. The feature workflow ref and supplied 40-character candidate SHA must resolve to the same commit.
7. Inspect the exact device artifact. Keep automated, runtime-device, visual, and physical-device conclusions separate. Required screenshots must visibly prove their claims.
8. Record exact run, artifact, checksum, and evidence links in the PR and Linear.
9. Publish a phone-test prerelease only when requested. It must consume the unchanged verified artifact, include required screenshots, and never rebuild.
10. Merge only under the repository policy in `AGENTS.md`; then verify merged `main`, its baseline run, and Linear completion evidence.

## Evidence invariants

- Prior-SHA, branch-level, skipped, stale, or cancelled checks do not satisfy a gate.
- Build success and semantic assertions do not prove rendered pixels.
- Instrumentation may remove the target package. Reinstall the already-built APK before independent launch verification.
- Universal device CI must not embed one ticket's GPS route, camera, or screenshot assertions.
- A phone release must verify source run, workflow-ref SHA, expected commit, artifact name, manifest, APK size, and SHA-256.
- Physical-device results are user evidence and remain separate from emulator evidence.

## Completion record

Report the Linear issue, branch/PR, exact candidate SHA, gate classification, baseline run and APK, device run/evidence when required, prerelease asset when requested, merge SHA, and merged-ref baseline. Name every unverified or skipped gate; never turn missing evidence into a pass.
