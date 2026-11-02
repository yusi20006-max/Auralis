# Auralis — Catalog API v1 and Infrastructure Decision

**Status:** design proposal; no provider selected, no production service created, no deployment authorized.  
**Issue:** #14  
**Revision date:** 2026-11-02

## 1. Decision boundary

Issue #14 defines a provider-neutral API contract and compares infrastructure options. It does not authorize account creation, paid-plan activation, domain purchase, production bucket/database creation, migration of real content, or deployment.

The current app remains offline-first. Local import, Library, playback and private downloads must work without the Catalog. No server credential, signing secret or administrative token may be shipped in the Android APK.

**Provisional architecture recommendation:** keep the public Catalog as versioned, cacheable JSON documents and serve audio as immutable HTTPS objects from object storage. Do not introduce a database, authenticated API or always-on application server until a concrete product requirement proves one is needed. This is a design recommendation, not a provider selection.

## 2. Existing implementation facts

Verified against the current `main` commit `37893a28835aa4ed879641254520b994b3a363c5`:

- `CatalogConfig.SOURCE_ID` is `catalog:auralis`.
- `CatalogConfig.catalogUrl` currently points to the repository's raw `docs/catalog.json` sample document.
- The current sample document uses top-level `version`, `sourceId`, `updatedAt`, `podcasts[]` and `episodes[]`.
- Podcast fields currently include `id`, `title`, `description`, `coverUrl`, `isAiGenerated`, `episodeCount` and `category`.
- Episode fields currently include `id`, `podcastId`, `title`, `description`, `audioUrl`, `durationMs`, `publishedAtEpochMs` and `isAiGenerated`.
- The parser skips malformed individual rows, but does not currently enforce the root `version` or `sourceId`.
- `CatalogRepository` fetches one JSON document, retains its previous in-memory lists on refresh failure and uses a 12-second connect/read timeout by default.
- The app's durable remote identity is `(sourceId, remoteId)`; the current Catalog source is `catalog:auralis`. Audio URLs are locations, not identity.
- Current sample audio URLs are external test/demo media. They are not approved production content and must not be treated as production assets.

The v1 proposal below is intentionally explicit about fields and validation. Implementing a new parser/client contract belongs to the later implementation scope, not this design-only document.

## 3. API contract

### 3.1 Versioning and identity

- API major version is in the path: `/v1/`.
- The JSON document carries `schemaVersion: 1` and a monotonically increasing `catalogRevision`.
- `sourceId` is the immutable value `catalog:auralis`.
- Podcast `id` and episode `id` are stable, opaque strings. IDs are never recycled or reassigned to different content.
- Episode remote identity remains `(sourceId, episode.id)`. Never derive identity from title, audio URL, filename or artwork URL.
- Additive optional fields are allowed within v1. Changing meaning, removing/renaming required fields, or changing identity semantics requires `/v2/`.
- Unknown optional fields must be ignored by clients. Unknown `schemaVersion` or an incompatible major version must fail closed while preserving the last known-good Catalog.

### 3.2 Read-only endpoints

| Method and path | Purpose | Response |
|---|---|---|
| `GET /v1/catalog.json` | Catalog index and podcast summaries | `CatalogIndex` JSON |
| `GET /v1/podcasts/{podcastId}.json` | One podcast and its published episodes | `PodcastDetail` JSON |
| `GET {media.url}` | Stream or download one immutable audio object | Audio bytes; HTTP Range supported |

The index is the initial required endpoint. Podcast detail is a separate resource to avoid forcing clients to download every episode description as the catalog grows. A small initial catalog may be delivered as a single static JSON file; the logical resource contract remains the same.

No search endpoint, user account, write API, analytics API, payment endpoint or admin API is required for v1. Search can be local over the downloaded index until measured catalog size or UX latency justifies server-side search.

### 3.3 Catalog index example

```json
{
  "schemaVersion": 1,
  "sourceId": "catalog:auralis",
  "catalogRevision": 12,
  "generatedAt": "2026-11-02T08:00:00Z",
  "podcasts": [
    {
      "id": "ai-philosophy",
      "title": "فلسفه با هوش مصنوعی",
      "description": "گفت‌وگوهایی درباره معنا، اخلاق و آگاهی.",
      "artworkUrl": null,
      "category": "فلسفه",
      "isAiGenerated": true,
      "episodeCount": 2,
      "detailUrl": "https://catalog.example.invalid/v1/podcasts/ai-philosophy.json"
    }
  ]
}
```

### 3.4 Podcast detail example

```json
{
  "schemaVersion": 1,
  "sourceId": "catalog:auralis",
  "catalogRevision": 12,
  "podcast": {
    "id": "ai-philosophy",
    "title": "فلسفه با هوش مصنوعی",
    "description": "گفت‌وگوهایی درباره معنا، اخلاق و آگاهی.",
    "artworkUrl": null,
    "category": "فلسفه",
    "isAiGenerated": true
  },
  "episodes": [
    {
      "id": "ep-philo-1",
      "podcastId": "ai-philosophy",
      "title": "آیا ماشین می‌تواند بفهمد؟",
      "description": "اپیزود نمونه.",
      "publishedAt": "2026-11-01T10:00:00Z",
      "durationMs": 1800000,
      "isAiGenerated": true,
      "media": {
        "url": "https://media.example.invalid/audio/ep-philo-1/sha256-<digest>.mp3",
        "mimeType": "audio/mpeg",
        "sizeBytes": 14400000,
        "sha256": "<64-character-lowercase-hex-digest>"
      }
    }
  ]
}
```

The `.invalid` hostnames and digest above are placeholders, not real service URLs or deployed endpoints.

### 3.5 Field rules

| Field | Rule |
|---|---|
| `schemaVersion` | Required positive integer; v1 value is 1. |
| `sourceId` | Required; must equal `catalog:auralis`. |
| `catalogRevision` | Required non-negative integer; strictly increases on each published catalog change. |
| `generatedAt` | Required RFC 3339 UTC timestamp. |
| Podcast `id` | Required, non-empty, stable and unique within the source. |
| Podcast `title` | Required, non-empty plain text. |
| `artworkUrl` | Optional nullable HTTPS URL; invalid or absent artwork uses a client placeholder. |
| Episode `id` | Required, non-empty and unique within the source. |
| `podcastId` | Required and must reference a podcast in the index/detail relationship. |
| `publishedAt` | Required RFC 3339 UTC timestamp for published episodes. |
| `durationMs` | Required positive integer when known; never infer it from a URL or filename. |
| `media.url` | Required HTTPS URL to an immutable media object. |
| `media.mimeType` | Required supported audio MIME type, e.g. `audio/mpeg` or `audio/mp4`. |
| `media.sizeBytes` | Required positive integer for complete downloadable media. |
| `media.sha256` | Required lowercase SHA-256 hex digest for integrity verification. |

The index's `episodeCount` is a convenience summary and must equal the number of published episodes returned by the detail resource. It is not the source of truth for playback or download state.

### 3.6 HTTP and cache behavior

- All API and media URLs use HTTPS. HTTP URLs, credentials embedded in URLs and unsupported schemes are rejected.
- Successful JSON responses use `Content-Type: application/json; charset=utf-8`.
- Catalog responses provide a strong `ETag` and `Last-Modified`; clients send `If-None-Match` first and accept `304 Not Modified`.
- Initial cache proposal: `Cache-Control: public, max-age=300, stale-if-error=86400`. Revisit after measuring update frequency and freshness needs.
- JSON should be gzip/Brotli compressed where supported.
- Media objects use stable, content-addressed or revisioned paths. Never overwrite bytes behind an already published URL.
- Media responses provide correct `Content-Type`, `Content-Length`, `ETag`, `Last-Modified` and byte-range support (`Accept-Ranges: bytes`, `206 Partial Content`, valid `Content-Range`) for seeking and resumable transfer.
- Redirects must remain HTTPS and resolve to an approved media host. The client must not forward credentials to redirected hosts.
- `429` responses include `Retry-After`. Transient `5xx` and network failures preserve last-known-good Catalog data.
- A malformed or incompatible response must not replace a valid cached Catalog.

### 3.7 Error model

For an API error, return a stable machine-readable code and a safe human-readable message. Do not expose stack traces, storage keys, internal paths, signed credentials or provider error bodies.

| HTTP status | Meaning | Client behavior |
|---|---|---|
| 400 | Invalid request/path | Do not retry automatically. |
| 404 | Unknown podcast/resource | Show unavailable state; retain local media/library. |
| 410 | Resource intentionally withdrawn | Stop offering remote stream; do not silently delete local/downloaded media. |
| 429 | Rate limited | Respect `Retry-After`; keep cached data. |
| 5xx / timeout / offline | Temporary service failure | Keep last-known-good data and local playback; offer retry. |
| 304 | Catalog unchanged | Keep cached representation. |

### 3.8 Removal and publication semantics

- Only published podcasts/episodes appear in the public index/detail responses.
- Unpublishing removes an item from the current public Catalog. A future optional tombstone list may communicate removals to clients that have been offline for a long time.
- Catalog removal never implicitly deletes a Room row, Library membership or private downloaded file.
- Deleting a downloaded file remains an explicit user action under the existing local ownership rules.
- Audio object deletion follows a separate retention/recovery policy; do not immediately hard-delete an object when merely unpublishing it.

## 4. Security, privacy and operational controls

- V1 is a public, read-only Catalog. No authentication is proposed because no private content or per-user entitlement requirement is currently established.
- No secret is required by the Android client. Object storage write credentials and publishing credentials remain server-side/CI-side with least privilege.
- Separate public read access from upload/delete permissions. The mobile app receives no write key, admin token or bucket credential.
- Validate IDs, JSON size, text lengths, MIME type, audio size, checksums and HTTPS host allowlists during publication and client parsing.
- Reject arbitrary URL schemes and unexpected media hosts. Never accept a server-provided local file URI.
- Do not log user listening history or stable user identifiers in the public Catalog service. Keep operational logs minimal and define retention before production.
- Use immutable object keys and verify SHA-256 before accepting a completed download. Range downloads must validate the final complete file before promotion from partial storage.
- Prefer no account system, no personal data collection and no analytics in this phase.

## 5. Backup, retention and recovery proposal

- Git remains the reviewable source of truth for Catalog metadata and schema changes.
- Published audio objects use immutable/versioned keys; never mutate a published object in place.
- Keep at least the last three published Catalog revisions readily recoverable.
- Keep an independent copy of source audio and published metadata outside the primary object bucket before public release.
- Proposed unpublish recovery window: 30 days before permanent deletion, unless rights/privacy removal requires immediate purge.
- Define backup frequency, restore owner, recovery-point objective (RPO) and recovery-time objective (RTO) before production. These are operational decisions, not assumed provider guarantees.
- Test restore and republish in a non-production environment before Issue #16 is considered complete.

## 6. Infrastructure comparison and indicative cost model

Prices below are public list prices checked on 2026-11-02. They are estimates, not quotes. Region, tax, exchange rate, billing unit rounding, account eligibility, support, domain, CDN configuration, backup copies and operational labor can change the total. No account or service has been created.

### 6.1 Candidates

| Option | Appropriate use | Published price / limit | Assessment |
|---|---|---|---|
| Existing GitHub Pages | Small static JSON index and documentation only | 1 GB published-site limit; 100 GB/month soft bandwidth limit | Keep for sample/test metadata if useful. Not the audio origin and not a production media platform. |
| Cloudflare R2 Standard | Public audio object storage | First 10 GB-month, 1M Class A and 10M Class B requests/month free; then $0.015/GB-month, $4.50/M Class A, $0.36/M Class B; internet egress free | Strong candidate when audio streaming/download egress is the dominant uncertainty. |
| Backblaze B2 | Public audio object storage / archive source | First 10 GB free; $6.95/TB-month; egress free up to 3× average monthly stored data, then $0.01/GB; most common transaction classes free | Strong low-cost alternative at modest egress; overage and CDN route need modeling. |
| Supabase Storage + database | Integrated database, auth and storage when those capabilities are genuinely needed | Pro starts at $25/month; includes 100 GB file storage and 250 GB egress, then published overage applies | Overbuilt for a public read-only JSON catalog; baseline subscription and egress make it less attractive for media-only delivery. |

GitHub Pages must not be treated as a production audio CDN. Its published bandwidth limit is soft and GitHub may recommend moving high-traffic content to a CDN or other hosting service.

### 6.2 Audio-size assumptions

Approximate uncompressed MP3 payload size (decimal units, before container/metadata overhead):

`size_MB ≈ bitrate_kbps × duration_seconds ÷ 8 ÷ 1000`

| Audio profile | Approx. size for 30 minutes |
|---|---:|
| 64 kbps | 14.4 MB |
| 96 kbps | 21.6 MB |
| 128 kbps | 28.8 MB |

### 6.3 Illustrative monthly scenarios

These scenarios assume every play streams the full 30-minute episode once. Real usage may be lower due to partial listening, or higher due to repeated plays, downloads, retries, range requests and multiple device caches.

| Scenario | Stored library | Monthly full plays | Approx. stored bytes | Approx. media egress |
|---|---:|---:|---:|---:|
| Early | 100 × 30 min @ 64 kbps | 500 | 1.44 GB | 7.2 GB |
| Growth | 1,000 × 30 min @ 96 kbps | 2,000 | 21.6 GB | 43.2 GB |
| High | 2,000 × 30 min @ 128 kbps | 10,000 | 57.6 GB | 288 GB |

Indicative object-storage-only costs using the listed free allowances:

| Scenario | Cloudflare R2 Standard | Backblaze B2 |
|---|---:|---:|
| Early | $0.00 storage/egress at stated usage | $0.00 storage; egress may exceed 3× stored data, about $0.03 |
| Growth | About $0.17 storage; egress free | About $0.08 storage; egress within 3× stored data |
| High | About $0.71 storage; egress free | About $0.33 storage + $1.15 egress overage = $1.48 |

Calculations use 10 GB free storage for each provider, R2's 10 GB-month free tier and free internet egress, and B2's 10 GB free storage plus 3× average-storage free egress. They exclude request charges where the scenario remains within published free allowances, taxes, domain, backup copy, CDN/compute outside the stated partner arrangements, support and labor. B2's egress threshold uses average monthly storage, not simply the end-of-month object count.

The estimates show that raw storage cost is not the principal early-stage risk. Egress policy, reliability, geographic performance, restore process, account/payment availability and operator workload matter more. A provider's free tier is not a guarantee of permanent zero-cost production operation.

### 6.4 Cost-control requirements

Before any production provider is selected, the owner must supply or approve:
- expected number of published episodes and average duration/bitrate;
- monthly active listeners, average listening minutes and download-to-stream ratio;
- target countries/regions and likely egress path;
- required backup copies and retention;
- monthly spending ceiling and alert threshold.

Every provider proposal must show base cost, storage, egress, request costs, backups, domain/CDN, taxes/FX exposure and an explicit overage scenario. Add budget alerts and a documented shutdown/rollback path before enabling production traffic.

## 7. Provisional recommendation — not a provider approval

1. Keep the current repository-hosted JSON as a test fixture only; do not promote the raw GitHub URL to a production API.
2. Use a static, versioned JSON Catalog for v1; avoid a database or always-on backend until search, private access or editorial concurrency actually requires one.
3. Use object storage for immutable audio objects, with HTTP Range support and checksums.
4. Keep Cloudflare R2 and Backblaze B2 as the leading storage candidates for a later owner decision. R2 is simpler to model for high egress because internet egress is free; B2 can be cheaper at low egress and has a 3× storage egress allowance. Validate performance and account availability from the target audience's networks before selection.
5. Keep Supabase as a later option only if a real database/auth requirement appears.
6. Do not use GitHub Pages for audio delivery.

No provider is selected by this document. No production account, bucket, database, DNS record, payment method, paid plan or deployment may be created under Issue #14 without the owner's explicit approval of the final design and cost.

## 8. Open decisions requiring owner approval

- Approve public read-only Catalog for v1, or identify a concrete private-content requirement.
- Approve the proposed versioned JSON + immutable object-storage architecture.
- Approve a provider after region/network checks and a finalized usage estimate.
- Approve a monthly cost ceiling, alert threshold and backup retention.
- Approve whether a custom domain is required now or can wait.

Until those decisions are explicitly approved, this remains a design artifact only.

## 9. Public pricing and platform references

- Cloudflare R2 pricing: https://developers.cloudflare.com/r2/pricing/
- Cloudflare plans and Workers pricing: https://www.cloudflare.com/plans/
- Backblaze B2 pricing: https://www.backblaze.com/cloud-storage/pricing
- Supabase pricing: https://supabase.com/pricing
- GitHub Pages limits: https://docs.github.com/en/pages/getting-started-with-github-pages/github-pages-limits

Last checked: 2026-11-02.
