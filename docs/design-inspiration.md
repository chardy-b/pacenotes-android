# Design Inspiration Sources

This document records external references used to inform Rally Pacenotes’ original **Rally Technical Dossier** visual direction.

## Policy

These projects are **reference material only**. Rally Pacenotes does not import their React/Tailwind implementation, use their names/branding, reproduce their screen layouts, or imply affiliation with their creators or the underlying media franchise.

The application’s visual system remains original and Compose-native. We may borrow only broad, non-proprietary interaction and visual principles:

- operational information hierarchy;
- concise state labels and clear alert semantics;
- thin technical rails, ticks, and structured panel grouping;
- dense-but-legible command-surface posture;
- restrained use of accent color to communicate state.

Do not borrow or reproduce franchise identifiers, logos, fictional organization labels, characters, screenshots, vehicle likenesses, bespoke typography treatments, or recognizable screen compositions.

## References

### `mdrbx/nerv-ui`

- **Repository:** <https://github.com/mdrbx/nerv-ui>
- **Documentation/demo:** <https://mdrbx.github.io/nerv-ui/docs/getting-started/installation/>
- **Observed license:** MIT
- **Captured:** 2026-07-25
- **Why it is relevant:** A React component library that presents a dense operational/CRT command-surface aesthetic.
- **Principles to study:** Component taxonomy for status surfaces, hierarchy under urgency, narrow mono labels, strong rails/dividers, and the treatment of warning versus normal states.
- **Not for reuse:** React source, Tailwind structure, Framer Motion sequences, package name, NERV/MAGI terminology, and any literal branded visual treatment.

### `TheGreatGildo/nerv-ui`

- **Repository:** <https://github.com/TheGreatGildo/nerv-ui>
- **Observed license:** MIT
- **Captured:** 2026-07-25
- **Why it is relevant:** A skill/component collection framing an operations-console visual posture.
- **Principles to study:** Command-surface framing, explicit system state, alert restraint, and visual density that serves monitoring rather than marketing.
- **Not for reuse:** Project/brand identity, web implementation, generated branded copy, named fictional systems, or exact component/screen arrangements.

## Compose translation

The implementation guidance derived from these references is maintained in:

- [`design/compose-technical-dossier-translation.md`](../design/compose-technical-dossier-translation.md)
- [`design/rally-technical-dossier-inspo.html`](../design/rally-technical-dossier-inspo.html)

When implementing a component, begin with the app-owned Compose contract and accessibility requirements in the Compose translation document—not with source code from either reference repository.
