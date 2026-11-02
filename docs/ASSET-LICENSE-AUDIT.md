# Asset and license audit

Audit date: 2026-11-02  
Scope: repository license declaration, launcher artwork, typography, Catalog sample audio and Android dependencies. This is a provenance/status audit, not a legal opinion.

## Findings

| Asset / component | Evidence in repository | Status | Required action before public distribution |
|---|---|---|---|
| Project source and authored documentation | LICENSE declares All Rights Reserved | **DECLARED PROPRIETARY** | Keep the notice; confirm copyright ownership for all contributed material. |
| In-repository vector launcher mark | app/src/main/res/drawable/ic_launcher_foreground.xml contains vector paths and comments describing the Auralis mark | **SOURCE PRESENT; AUTHORSHIP NOT FORMALLY RECORDED** | Record creator/assignment or owner approval. |
| PNG launcher artwork fallback | scripts/install-brand-icons.sh downloads https://litter.catbox.moe/5mamgu.png when no local icon asset is available. scripts/icon-assets/README.md calls it an owner-approved crop, but no license/permission record or pinned checksum is present in the repository. | **UNVERIFIED / RELEASE BLOCKER** | Replace with an owner-controlled, committed asset and record provenance/permission; or remove remote fallback. Do not treat the script comment as documentary proof of rights. |
| Vazirmatn typography | README describes Vazirmatn via Google Fonts; Gradle includes Compose Google Fonts support. The upstream Vazirmatn project identifies the font as SIL Open Font License 1.1. No bundled font file or license notice was verified in this repository. | **UPSTREAM LICENSE IDENTIFIED; PACKAGING NOTICE NOT VERIFIED** | Record the exact font version/source and include the applicable OFL notice if font software is bundled; verify the actual Google Fonts runtime/provider path used by the app. |
| Catalog sample audio | docs/catalog.json and CatalogRepository.kt reference ExoPlayer test media and SoundHelix MP3 URLs. | **RIGHTS / ATTRIBUTION NOT VERIFIED** | Obtain and record permission/license/attribution for each exact track before distribution, or replace with content whose rights are documented. |
| Sample Catalog metadata | Catalog entries mark sample tracks as AI-generated, while descriptions identify test/demo audio (Jazz in Paris and SoundHelix examples). | **METADATA / CONTENT MISMATCH** | Do not represent demo tracks as actual AI-generated podcast episodes. Replace the audio with matching cleared content or correct the metadata and product presentation. |
| Artwork in remote Catalog | Current docs/catalog.json entries have coverUrl null. | **NO REMOTE COVER ART IN CURRENT JSON** | Audit any artwork added in later Catalog revisions before release. |
| AndroidX, Kotlin, Compose, Media3, Room, Coil and other Gradle dependencies | Dependencies are declared in app/build.gradle.kts; this audit did not produce a complete transitive dependency/license inventory. | **THIRD-PARTY NOTICE INVENTORY INCOMPLETE** | Generate and review a dependency license inventory for the exact release build; include required notices and attribution. |

## External license references

- Vazirmatn upstream project and OFL-1.1 declaration: https://github.com/rastikerdar/vazirmatn
- SIL Open Font License 1.1 text and guidance: https://openfontlicense.org/

The upstream font license identification does not prove which exact font binary/version the app loads at runtime; preserve the version/source and required notices with the release materials.

## Catalog hosting distinction

CatalogConfig.catalogUrl currently points to the repository's raw GitHub JSON file at docs/catalog.json. This is a GitHub-hosted static test/sample catalog, not a production backend, managed publishing service, object store, CDN contract or guarantee of production availability. The audio URLs in that JSON are separately hosted external sample media. A successful GitHub Pages workflow does not establish a production Catalog service.

Production Catalog/API, hosting provider, paid infrastructure and publishing operations remain separate decisions under Issues #14–#16 and require owner approval.

## Release gate

The repository's All Rights Reserved notice applies to project-owned material; it does not grant rights to third-party fonts, audio, remote images or dependencies. Any item marked UNVERIFIED must remain visibly unresolved and must not be described as cleared for public distribution.

Before public distribution:
1. Pin and document the approved launcher asset; remove unverified remote asset fallback.
2. Verify font license and ship required notices.
3. Verify exact rights/attribution for every sample audio track and replace demo content that is mislabeled as AI-generated.
4. Generate a complete dependency license/notice inventory for the release dependency graph.
5. Record owner approval for any project-owned artwork or third-party material whose rights are not already documented.

No asset was downloaded, republished, relicensed or modified as part of this audit.
