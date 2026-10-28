# Auralis — Design System

**Status:** Approved visual specification; shared tokens and contract tests implemented  
**Platform:** Android / Jetpack Compose  
**Theme:** Dark only  
**Product:** Persian-first player for AI-generated podcasts

This document is the implementation reference for Auralis UI. If a screen conflicts with these tokens, update the screen to use the tokens rather than introducing a one-off value.

## Brand and product language

- App name: **Auralis**
- Persian descriptor: **پادکست‌های هوش مصنوعی**
- Auralis plays AI-generated podcast content; do not imply that the Android app itself generates episodes.
- Visual direction: premium, modern, calm, audio-first, with restrained blue-to-violet gradients.
- Poweramp is a reference for playback ergonomics only. Do not copy its branding, assets, or screen layouts.
- Use the approved Auralis logo asset when available; do not replace it with an invented mark.

## Color tokens

| Token | Hex | Usage |
|---|---|---|
| Background Deep | `#070A12` | Main app/window background |
| Surface | `#0F172A` | Cards, mini-player, secondary surfaces |
| Surface Elevated | `#1E293B` | Raised controls and emphasized surfaces |
| Primary Blue | `#4F8CFF` | Primary action gradient start |
| Primary Violet | `#9B5CFF` | Primary action gradient end |
| Accent Cyan | `#22D3EE` | AI-generated labels and secondary highlights |
| Text Primary | `#E8F1FF` | Headings and primary content |
| Text Secondary | `#94A3B8` | Descriptions and metadata |
| Text Tertiary | `#64748B` | Low-emphasis metadata |
| Success | `#34D399` | Completed downloads and success state |
| Error | `#F87171` | Errors and destructive feedback |
| Border Subtle | `#1E293B` | Dividers and low-emphasis outlines |

The main action gradient runs from Primary Blue to Primary Violet. Reserve glow and gradient treatment for important interactive elements; do not apply them to every card or label. Ensure text and controls remain legible without relying on color alone.

## Typography

Preferred Persian typeface: **Vazirmatn** or **IRANSansX**. Complementary Latin typeface: **Inter** or the platform system sans-serif. A font asset must be legally distributable and included in the app before selecting it explicitly in code. Until then, use the Compose/system fallback.

| Role | Size | Weight | Typical use |
|---|---:|---|---|
| Display / Hero | 28–32sp | Bold | Now Playing title |
| Title Large | 22–24sp | SemiBold | Screen titles |
| Title Medium | 18–20sp | SemiBold | Episode/podcast titles |
| Body Large | 16sp | Regular | Descriptions and primary body |
| Body Medium | 14sp | Regular | General text |
| Caption | 12sp | Regular | Duration and metadata |
| Overline | 11sp | Medium | Categories and compact labels |

The shared token file defines type sizes and line heights; the Material typography maps semantic styles to those values. Prefer semantic Material typography styles. Support system font scaling; do not hard-code text into bitmap artwork.

## Spacing and shape

- Base unit: **4dp**.
- Standard spacing: **8, 12, 16, 24, 32dp**.
- Episode/podcast card: **16dp** corner radius and **16dp** internal padding.
- Buttons: **12–16dp** corner radius.
- Artwork: **12–16dp** corner radius with a restrained shadow.
- Circular icon controls: fully rounded.
- Mini-player: **64–72dp** high; artwork **48dp**.
- Primary Now Playing play control: **72–80dp**.
- Waveform visualization: **64–80dp** high.
- Progress indicators should be visually thin and use the primary gradient where supported.

Use shared spacing and shape tokens rather than repeating numeric literals across components.

## Core components

1. **Primary button:** blue-to-violet gradient, white high-contrast label, restrained glow, clear enabled/disabled states.
2. **Secondary button:** Surface background with subtle outline.
3. **Now Playing control:** 72–80dp circular gradient play/pause control with soft elevation.
4. **Mini-player:** Surface background, 48dp artwork, episode title, playback action; target total height 64–72dp.
5. **Episode card:** Surface background, 16dp radius and padding, clear title/metadata hierarchy.
6. **AI Generated label:** translucent cyan-tinted background with cyan text and adequate contrast.
7. **Progress bar:** slim, with clear played/buffered/unplayed states.
8. **Waveform:** 64–80dp visual area, blue/violet palette; waveform must not be the only way to inspect or control progress.
9. **Icon button:** outline/linear icon, approximately 1.5–2px visual stroke; secondary text color by default and high-emphasis color when active.

All playback controls must reflect the real Media3 player state; never display “playing” merely because a button was tapped.

## Motion and interaction

- Standard transitions: **200–300ms**, short and purposeful.
- Respect Android system animation preferences where applicable.
- Avoid motion, blur, glow, or shadows that reduce readability, accessibility, responsiveness, or battery life.
- Provide touch targets appropriate for Android accessibility; keep important playback controls easy to reach.
- Communicate loading, playing, paused, completed, and error states distinctly.

## RTL, accessibility, and content

- Persian is first-class. Apply RTL-aware layout direction and use start/end semantics rather than left/right assumptions.
- Mixed Persian/English titles, numbers, durations, and file names must remain readable.
- Test narrow screens, large system font sizes, TalkBack, contrast, and long episode titles.
- Do not encode meaning by color alone; include text or accessible state descriptions.
- Artwork must preserve its aspect ratio, use rounded corners, and avoid stretching.
- Use stable placeholders when artwork is absent.

## Engineering rules

- Store colors, spacing, shapes, typography, line heights, and motion durations in shared Kotlin tokens.
- Use a single app theme entry point for Compose screens.
- Components should consume tokens and semantic Material styles, not define their own competing palettes.
- Keep playback/service logic independent of visual styling.
- Add new components incrementally and run the Android CI build after each coherent phase.
- This phase establishes tokens only; it does not claim that every screen or component has already been migrated.

## Implementation status

- [x] Final color and layout specification documented.
- [x] Shared Compose tokens implemented; token contract tests added (CI validation pending for this commit).
- [ ] Existing screen migrated to theme tokens.
- [ ] Reusable buttons, cards, labels, mini-player, and player controls implemented.
- [ ] RTL/accessibility checks completed on a real device.
