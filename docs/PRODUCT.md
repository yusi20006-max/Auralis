# Auralis — Product Scope

## Vision

A focused Android listening app for AI-generated podcasts, initially usable entirely offline and later connected to a hosted catalog controlled by the project owner.

## In scope for the offline MVP

- Import audio files using Android's system file picker.
- Browse a local podcast/episode library.
- Show episode title, creator/source when known, duration, artwork when available, and useful metadata.
- Play, pause, seek, and move between episodes where a queue exists.
- Persist playback position and recently played episodes.
- Support background playback and Android media controls.
- Support playback from the notification, lock screen, Bluetooth/headset controls, and audio-focus changes.
- Provide a Persian-first, RTL-aware interface.
- Remain useful with airplane mode enabled.

## Explicitly out of scope for the first MVP

- User accounts and sign-in.
- Online podcast discovery.
- Generating podcasts inside the app.
- General music-player features such as a full parametric equalizer.
- A server, admin dashboard, or paid hosting dependency.
- Analytics or telemetry that is not necessary for the app.

## Future hosted-catalog phase

The app may later fetch a catalog of published episodes from a hosted service. The catalog should expose stable episode IDs and metadata, with audio and artwork served as files. The Android app should support streaming and optional downloads while preserving local playback when offline.

Authentication, multi-user permissions, publishing workflows, and server-side generation are future decisions—not assumptions for the first server version.

## UX principles

- Playback controls must be easy to reach and clearly communicate state.
- Use motion sparingly and avoid effects that harm responsiveness, readability, battery life, or accessibility.
- Preserve playback state across navigation and app restarts.
- Design for small screens, font scaling, TalkBack, and both Persian and mixed-language episode titles.
- Show actionable errors for missing, unreadable, or unsupported audio files.

## MVP acceptance criteria

- A user can import a supported local audio file and play it without network access.
- Playback can continue when the app is backgrounded and the screen is off.
- Playback position and recent history persist after restarting the app.
- Basic media controls work through the Android media session.
- The library and player remain usable with Persian RTL text and larger system fonts.
- Automated build/tests pass, and critical playback flows are manually checked on a real Android device.
