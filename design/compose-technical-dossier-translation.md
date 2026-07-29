# Rally Technical Dossier — Compose UI Translation

## Decision

Use the Evangelion-inspired web libraries as **reference material only**. Do not import their React/Tailwind code, NERV brand treatment, fictional-organization wording, proprietary visual identity, screen layouts, or animation sequences.

The Compose implementation is an original `RallyTechnicalTheme` built on Material 3 accessibility behavior and a small set of app-owned navigation components.

### Reference materials reviewed

| Reference | May borrow | Do not borrow |
|---|---|---|
| [`mdrbx/nerv-ui`](https://github.com/mdrbx/nerv-ui) — MIT React component library | Operational hierarchy, technical rails, state density, component taxonomy | Component code, NERV branding, literal CRT/military-screen treatment |
| [`TheGreatGildo/nerv-ui`](https://github.com/TheGreatGildo/nerv-ui) — MIT web/skill library | Command-surface posture, clear alert and system-state treatment | Logos, named fictional systems, copied layouts, any branded content |

MIT covers those repositories’ code, not the rights to the underlying franchise identifiers or artwork. The product therefore uses the original name **Rally Technical Dossier** and original Compose components.

---

## Compose architecture

```text
RallyTechnicalTheme
├── RallyTechnicalColors
├── RallyTechnicalTypography
├── RallyTechnicalMetrics
└── App-owned primitives
    ├── TechnicalRule
    ├── SystemLabel
    ├── TechnicalPanel
    └── SignalBadge

Navigation screen
├── NavigationStatusBar
├── Mapbox map viewport
├── PacenoteCard                 ← driving-critical component
├── ConfidenceGateDebugOverlay   ← debug/replay only
└── NavigationActionBar

Replay screen
├── RouteEventTimeline
├── PacenoteEventRow
└── ClassifierEvidencePanel
```

### Dependency policy

Use:

- Jetpack Compose and Material 3 for semantics, focus behavior, dark/light theming, and baseline accessible interaction.
- Compose `Canvas` / `drawBehind` for original technical rules, route ticks, and small diagram marks.
- `AnimatedVisibility` or state transitions only for non-driving/replay states.

Do not add a dependency merely to reproduce an aesthetic. No React bridges, Tailwind ports, web canvas packages, CRT filter packages, or anime-specific asset package belongs in the Android app.

---

## Semantic tokens

Implement names by **meaning**, rather than by their hex color. This preserves a usable daylight mode and prevents red from being abused as decoration.

```kotlin
val RallyVoid = Color(0xFF0B0D0E)
val RallyGraphite = Color(0xFF151A1B)
val RallyGrid = Color(0xFF3F4A4C)
val RallyPaper = Color(0xFFF1F4ED)
val RallySignalRed = Color(0xFFE2493D)
val RallyTelemetryCyan = Color(0xFF63D4D0)
val RallyCautionAmber = Color(0xFFF4BE4F)
val RallyReadyGreen = Color(0xFF75D38A)

@Immutable
data class RallyTechnicalColors(
    val surface: Color,
    val surfaceElevated: Color,
    val onSurface: Color,
    val mutedLabel: Color,
    val technicalRule: Color,
    val routeProgress: Color,
    val activeSignal: Color,
    val caution: Color,
    val ready: Color,
)
```

### Token rules

| Semantic token | Default dark value | Rule |
|---|---:|---|
| `surface` | `RallyVoid` | App canvas and driving card base |
| `surfaceElevated` | `RallyGraphite` | Adjacent/non-driving panels only |
| `onSurface` | `RallyPaper` | All driving-critical words/numerals |
| `routeProgress` | `RallyTelemetryCyan` | Position, route continuity, selected route state |
| `activeSignal` | `RallySignalRed` | Severity numeral, destructive action, primary active state; sparse use |
| `caution` | `RallyCautionAmber` | Low-confidence/suppressed evidence, never the only cue |
| `ready` | `RallyReadyGreen` | Matched/healthy state only |

### Typography

| Role | Compose role | Intended quality |
|---|---|---|
| Pacenote severity | `PacenoteDisplay` | Condensed, forceful numeral; target 64 sp minimum in active drive mode |
| Direction / distance | `PacenoteSupporting` | Clear sans; not all caps when a full word is used |
| System label | `SystemLabel` | Monospace, 10–12 sp, short uppercase phrases only |
| Body / settings | Material body styles | System sans, readable with font scaling |

Bundle only fonts whose license is included with the app source. Candidate display fonts: Rajdhani or Barlow Condensed; candidate mono: JetBrains Mono. Validate final files and licenses before adding them to `res/font/`.

---

## Component specifications

## 1. `PacenoteCard`

**Purpose:** The sole driving-critical surface. It should be glanceable before it is stylish.

```kotlin
@Composable
fun PacenoteCard(
    note: UpcomingPacenote,
    modifier: Modifier = Modifier,
)
```

**Must display:**
- left/right arrow plus a text alternative exposed to accessibility services;
- large severity value (`L4`, `R2` or an equivalent split display);
- distance to event;
- an optional modifier such as `TIGHTENS → L2`;
- no debug confidence value during normal driving.

**Visual rules:**
- `surface` background;
- 2 dp `routeProgress` top rule;
- 24 dp inner padding on normal phones;
- square or 2–4 dp corners, no large Material card roundness;
- severity may use `activeSignal`, but the arrow and accessible text must remain `onSurface`;
- no animated texture, grain, or scanning effects inside the card.

**Accessibility semantics:**

```kotlin
Modifier.semantics {
    contentDescription = "In 240 meters, left four, tightens to left two"
    liveRegion = LiveRegionMode.Polite
}
```

The visual instruction must not be repeatedly announced every recomposition. The speech scheduler remains the authoritative voice channel.

## 2. `NavigationStatusBar`

**Purpose:** Communicate trip health in one line without turning the navigation screen into a dashboard.

```kotlin
@Composable
fun NavigationStatusBar(
    navigationState: NavigationState,
    modifier: Modifier = Modifier,
)
```

**States:** `Matched`, `Rerouting`, `GpsWeak`, `Offline`, `Paused`.

**Design:** one compact mono label, a non-color state icon/shape, and a subtle technical rule. Use `ready` for matched and `caution` for GPS/rerouting—but always show readable state text.

## 3. `TechnicalPanel`

**Purpose:** An app-owned base panel for setup, replay, and post-drive screens.

It provides a thin top rail and optional corner clipping. It must **not** become a generic card wrapper around every element.

```kotlin
@Composable
fun TechnicalPanel(
    modifier: Modifier = Modifier,
    signal: Color = RallyTelemetryCyan,
    content: @Composable ColumnScope.() -> Unit,
)
```

Use it for real grouping: an event inspector, route-replay controls, or a single safety acknowledgement. Do not wrap every setting row in a panel.

## 4. `TechnicalActionButton`

**Purpose:** Primary/secondary actions outside active driving.

- 48 dp minimum touch target.
- Uses an angular/cut-corner outline drawn by shape or `drawBehind`.
- Normal action: cyan outline and paper text.
- Destructive/end-navigation action: red outline and text.
- Must retain visible pressed, disabled, and focused states.

## 5. `ConfidenceGateDebugOverlay`

**Purpose:** Explain why the classifier emitted or suppressed a pacenote during debug/replay.

This is **not present on the ordinary active-driving screen**. It may show confidence, intersection proximity, sample density, radius trend, and route revision in a replay or developer build.

## 6. `RouteEventTimeline`

**Purpose:** Replay and post-drive inspection, not live driving.

A Compose Canvas draws ordered event markers on a distance axis. Use cyan for selected position, red for significant pacenotes, and neutral grid marks for distance. Every colored marker needs a companion text/shape legend.

---

## Screen composition rules

### Active navigation — `Monitor` surface

```text
┌ NAV ACTIVE · MATCHED ───────────────────────────────┐
│                                                      │
│                  LIVE MAP VIEWPORT                   │
│                                                      │
├──────────────────────────────────────────────────────┤
│ NEXT CURVE                              0.24 KM      │
│ ←  L4                                                   │
│ TIGHTENS → L2                                         │
└──────────────────────────────────────────────────────┘
```

- Map: approximately 65–70% of usable height.
- PacenoteCard: attached to bottom edge; one major instruction only.
- System state: top bar only.
- Settings, debug controls, classifier data, animated textures, and dense telemetry: excluded.

### Route replay — `Inspect` surface

- The pace-note event timeline and evidence panel can be visible.
- Technical grid and restrained line animation are allowed.
- This is the appropriate place for richer “analog racing log” atmosphere.

### Onboarding / safety — `Configure` surface

- Technical dossier framing may be stronger.
- Use explicit safety copy, readable body text, normal Material controls, and no action ambiguity.

---

## Motion policy

| Context | Allowed | Forbidden |
|---|---|---|
| Active navigation | 150–200 ms opacity/position state transitions | scanning loops, parallax, animated noise, motion that competes with map/voice |
| Replay | route trace, event focus movement, measured timeline motion | rapid flashing or seizure-risk effects |
| Settings/onboarding | normal Material interaction feedback | decorative transitions that delay acknowledgement |

Read Android animation-scale settings. If animations are disabled, all state changes must remain clear without motion.

---

## Build order once the Android app exists

1. `RallyTechnicalTheme` + token unit/screenshot tests.
2. `SystemLabel`, `TechnicalRule`, and `TechnicalPanel`.
3. `PacenoteCard` for static states: left, right, tightens, and no modifier.
4. Semantics and large-font Compose tests for PacenoteCard.
5. `NavigationStatusBar` and state tests.
6. Integrate the Mapbox map viewport behind the static components.
7. Add replay-only `ConfidenceGateDebugOverlay` and `RouteEventTimeline`.

Each production component must follow the project’s TDD sequence: write a focused Compose test first, observe its failure, implement the minimum component behavior, and then run the full UI suite.

---

## Review checklist

- [ ] A user can identify direction, severity, and distance without interpreting color.
- [ ] Pacenote text and controls survive 200% font scaling without clipping.
- [ ] Active navigation remains visually quieter than replay/onboarding.
- [ ] No component uses franchise naming, imagery, logos, copied layouts, or recognizable fictional-system wording.
- [ ] All non-decorative UI has semantics, focus behavior, and sufficient contrast.
- [ ] The Mapbox map remains unobscured where route reading matters.
- [ ] Animation has no role in communicating a time-sensitive callout.
