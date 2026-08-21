# Sprint deliverables submission guide

This document covers **what to submit, where, and by when**. What goes
_inside_ each deliverable is in the brief and is not repeated here.

## Sprint schedule

| Sprint | Start      | End        | Deadline         | Deliverables                |
| ------ | ---------- | ---------- | ---------------- | --------------------------- |
| 1      | 2026-08-13 | 2026-08-19 | 2026-08-19 23:59 | D01, D02 v1, D03 v1, D04 v1 |
| 2      | 2026-08-20 | 2026-08-25 | 2026-08-25 23:59 | D02 v2, D03 v2, D04 v2      |
| 3      | 2026-08-26 | 2026-09-04 | 2026-09-04 23:59 | D02 v3, D03 v3, D04 v3      |
| 4      | 2026-09-07 | 2026-09-11 | 2026-09-11 23:59 | D05                         |

## Per-sprint gates

Missing any gate costs marks — the exact caps are in the rubric your instructor
publishes. A gate is not paperwork: it is dated evidence of what the team did
and who did it, and that is the one thing you cannot reconstruct the night
before the defence.

### G1 — Backlog freeze

Freeze the backlog **before** the sprint starts, keep it current as the sprint
runs, and submit **one** file at the end: `backlog_sprint<N>.xlsx`.

Plan in `backlog_template.xlsx` — sheet `Backlog` for the user stories, sheet
`WBS` for the tasks, `Sprint Summary` for the per-sprint and per-member roll-up,
and `Master Data` for the allowed values behind every dropdown. At the end of
the sprint save a copy of the whole workbook under the name above; that copy is
the sprint's frozen record, so do not edit it afterwards.

- Every task has a `Task ID` and the `Story ID` it belongs to — `T-011` is task 1
  of `US-01`. The G4 record and your commit messages refer to those ids.
- Every task has **exactly one** Assignee and an Estimate. A task nobody owns is
  a task nobody planned, and it counts as missing.
- Tasks added after the freeze are allowed, but must carry
  `Added <YYYY-MM-DD>` in the `Note` column and appear in the G4 record as a
  scope change.
- The `Status` column accepts only, in lowercase: `todo`, `in-progress`, `done`,
  `pending`.

### G2 — Code tag

Create an annotated tag on the default branch and push it before the
deadline:

```bash
git tag -a sprint-<N> -m "Sprint <N>: <scope summary>"
git push origin sprint-<N>
```

- **The tag date is the proof you delivered on time.** A tag created after the
  deadline is late, however long ago the code was written.
- Do not upload source code to Drive. The tag is the code submission.
- The repository must be reachable by the instructor's account when it is
  marked.

### G3 — Submission folder

Submit to this Google Drive path, exactly:

`<CLASS>/<TEAM>/Sprint <N>/`

Contents, with these filenames:

| File                          | From gate |
| ----------------------------- | --------- |
| `backlog_sprint<N>.xlsx`      | G1        |
| `sprint<N>_review.md`         | G4        |
| That sprint's deliverables    | G5        |

**The Drive upload time is the submission time.** Files edited after the
deadline are not read — what gets marked is what the folder held at 23:59
on the sprint's last day.

### G4 — Sprint review record

Write `sprint<N>_review.md` **during the sprint review**, not after.
Use `sprint_review_template.md` for the shape. The record needs a date and every
member's name.

The per-member section is the primary evidence for the individual mark. Name
task ids and the actual code — "helped the team" cannot be graded.

### G5 — Sprint deliverables

The deliverables that sprint owns, per the sprint schedule above. Each
deliverable's own rules — diagram-as-code, the `docs/` layout, submission format
— are in the brief's Deliverables section and in `deliverables.md` beside this
file, and apply here unchanged.

## Per-sprint checklist

### Sprint 1 — due 2026-08-19 23:59

- [ ] **G1** Backlog frozen before the sprint started
- [ ] **G1** `backlog_sprint1.xlsx` saved at the end of the sprint
- [ ] **G1** Every task has an Assignee and an Estimate
- [ ] **G1** Post-freeze tasks carry `Added <date>` in the Note column
- [ ] **G2** `git tag -a sprint-1` created on the default branch
- [ ] **G2** `git push origin sprint-1` done, and the tag shows on the repository page
- [ ] **G2** The instructor can open the repository
- [ ] **G3** Folder `<CLASS>/<TEAM>/Sprint 1/` created
- [ ] **G3** Every file in the table above is there, named exactly
- [ ] **G3** No source code in the Drive folder
- [ ] **G4** `sprint1_review.md` has a date and every member's name
- [ ] **G4** The done list reconciles with `backlog_sprint1.xlsx`
- [ ] **G4** Scope changes are written out, or "None" is stated
- [ ] **G4** Every member has a contribution line naming task ids
- [ ] **G5** D01 Project Proposal submitted
- [ ] **G5** D02 v1 Product Backlog and WBS submitted
- [ ] **G5** D03 v1 Requirement and Design Documents submitted
- [ ] **G5** D04 v1 Source Code submitted

### Sprint 2 — due 2026-08-25 23:59

- [ ] **G1** Backlog frozen before the sprint started
- [ ] **G1** `backlog_sprint2.xlsx` saved at the end of the sprint
- [ ] **G1** Every task has an Assignee and an Estimate
- [ ] **G1** Post-freeze tasks carry `Added <date>` in the Note column
- [ ] **G2** `git tag -a sprint-2` created on the default branch
- [ ] **G2** `git push origin sprint-2` done, and the tag shows on the repository page
- [ ] **G2** The instructor can open the repository
- [ ] **G3** Folder `<CLASS>/<TEAM>/Sprint 2/` created
- [ ] **G3** Every file in the table above is there, named exactly
- [ ] **G3** No source code in the Drive folder
- [ ] **G4** `sprint2_review.md` has a date and every member's name
- [ ] **G4** The done list reconciles with `backlog_sprint2.xlsx`
- [ ] **G4** Scope changes are written out, or "None" is stated
- [ ] **G4** Every member has a contribution line naming task ids
- [ ] **G5** D02 v2 Product Backlog and WBS submitted
- [ ] **G5** D03 v2 Requirement and Design Documents submitted
- [ ] **G5** D04 v2 Source Code submitted

### Sprint 3 — due 2026-09-04 23:59

- [ ] **G1** Backlog frozen before the sprint started
- [ ] **G1** `backlog_sprint3.xlsx` saved at the end of the sprint
- [ ] **G1** Every task has an Assignee and an Estimate
- [ ] **G1** Post-freeze tasks carry `Added <date>` in the Note column
- [ ] **G2** `git tag -a sprint-3` created on the default branch
- [ ] **G2** `git push origin sprint-3` done, and the tag shows on the repository page
- [ ] **G2** The instructor can open the repository
- [ ] **G3** Folder `<CLASS>/<TEAM>/Sprint 3/` created
- [ ] **G3** Every file in the table above is there, named exactly
- [ ] **G3** No source code in the Drive folder
- [ ] **G4** `sprint3_review.md` has a date and every member's name
- [ ] **G4** The done list reconciles with `backlog_sprint3.xlsx`
- [ ] **G4** Scope changes are written out, or "None" is stated
- [ ] **G4** Every member has a contribution line naming task ids
- [ ] **G5** D02 v3 Product Backlog and WBS submitted
- [ ] **G5** D03 v3 Requirement and Design Documents submitted
- [ ] **G5** D04 v3 Source Code submitted

### Sprint 4 — due 2026-09-11 23:59

- [ ] **G1** Backlog frozen before the sprint started
- [ ] **G1** `backlog_sprint4.xlsx` saved at the end of the sprint
- [ ] **G1** Every task has an Assignee and an Estimate
- [ ] **G1** Post-freeze tasks carry `Added <date>` in the Note column
- [ ] **G2** `git tag -a sprint-4` created on the default branch
- [ ] **G2** `git push origin sprint-4` done, and the tag shows on the repository page
- [ ] **G2** The instructor can open the repository
- [ ] **G3** Folder `<CLASS>/<TEAM>/Sprint 4/` created
- [ ] **G3** Every file in the table above is there, named exactly
- [ ] **G3** No source code in the Drive folder
- [ ] **G4** `sprint4_review.md` has a date and every member's name
- [ ] **G4** The done list reconciles with `backlog_sprint4.xlsx`
- [ ] **G4** Scope changes are written out, or "None" is stated
- [ ] **G4** Every member has a contribution line naming task ids
- [ ] **G5** D05 Final Presentation and Demo submitted
