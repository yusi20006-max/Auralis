# Auralis Roadmap

Last updated against `main` (download cancel / error mapper / private storage).

## Product capability (code)

- [x] Offline library (Room), import, Media3 session, Now Playing, Mini Player, speed
- [x] Brand icon, RTL, Back navigation, Library filters (دانلود شده‌ها)
- [x] Sample catalog stream + private offline download (app sandbox)
- [x] Download cancel, concurrent guard, space check, file validation, progress UI
- [x] LocalUriAccess + PlaybackErrorMapper (Persian errors)
- [x] Position persistence tests; repository dedupe (Issue #11 work)

## Formal issues

| Issue | Focus | Status |
|-------|--------|--------|
| #11 Persistence | Tests, URI safety, resume | Code done — device evidence / close with owner |
| #12 Playback hardening | System controls, error paths | Mapper + checklist — device evidence open |
| #14 API / hosting | Contract + provider | Waiting on product/host |
| #15 Catalog download | Cancel/retry/space | Client hardening on main; API later |
| #7 Accessibility | TalkBack / semantics | Open |
| #13 Release | Keystore, license, versioning | Open |

## Next recommended order

1. Owner device pass for #12 checklist + #15 cancel/retry
2. Issue #7 accessibility pass on core screens
3. Issue #13 release readiness when ready to distribute
4. #14 then real CDN for catalog

See also: [HOSTING.md](HOSTING.md), [ISSUE-12-DEVICE-CHECKLIST.md](ISSUE-12-DEVICE-CHECKLIST.md)
