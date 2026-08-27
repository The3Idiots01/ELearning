# Sprint 2 Review 

> **Date:** 2026-08-27
> **Present:** TienPQ9, DuongNT182, TungNT188

## Done

All 8 stories frozen into Sprint 2 (44 points, 21 tasks) shipped — every task in the WBS is `done`.

- **US-04** — View/edit profile (name, avatar, info) 
- **US-05** — Instructor course CRUD: create/edit course, price, draft ↔ published state, owner-only edit
- **US-06** — Multi-format lessons (video/text/file) stored by `storage_key`, presigned upload, section–lesson tree with drag-to-reorder
- **US-08** — Instructor "My Courses": list own courses with status + enrollment count, unpublish/republish
- **US-10** — Course search & filter (keyword, category, price) with paginated list screen
- **US-11** — Course landing page (curriculum, price, rating) + free-preview lessons
- **US-16** — Enrolled-learner curriculum API + player screen layout (protected video delivery / non-enrolled blocking ships with US-15 in Sprint 3)
- **US-19** — "My Courses" (active enrollments + resume target) — API and screen done; will show real data once US-14 (payment/enrollment) lands in Sprint 3

## Not done, and why

None .

## Scope change

Dropped Certificate feature and Admin management module from scope .

## Per-member contribution

- **TienPQ9** — T-051, T-052, T-053, T-061, T-062, T-063, T-064, T-083 — course CRUD API + create/edit screen + draft/published state (US-05); lesson CRUD API with storage_key, presigned upload, content tree with drag-to-reorder (US-06); instructor "My Courses" screen (US-08)
- **DuongNT182** — T-041, T-042, T-081, T-082, T-191 — profile API + change password, profile view/edit screen with avatar (US-04); instructor course-list API (status + enrollment count) and unpublish/republish API (US-08); active-enrollments API for "My Courses" (US-19)
- **TungNT188** — T-101, T-102, T-111, T-112, T-113, T-161, T-162, T-192 — search API + filtered list screen (US-10); course detail API + free-preview + landing page screen (US-11); enrolled-learner curriculum API + player screen layout (US-16); "My Courses" screen (US-19)

## Commitment for next sprint

- US-02
- US-07
- US-13
- US-14
- US-15
- US-17
- US-20