# Living sidecar protocol (spec: living-skills/1.1)

This folder augments the skill WITHOUT modifying it. The original SKILL.md is
never edited; delete `.living/` and the skill is exactly what its author shipped.

## On every use of this skill

1. **Integrity check**: sha256 of `../SKILL.md` must match `ORIGINAL.sha256`.
   On mismatch: the skill was updated or edited — inform the user; offer to
   re-baseline the hash and re-review KNOWLEDGE.md against the new text.
   If sources.md has an UPSTREAM entry, occasionally check it for a newer
   version of the skill; if found, notify the user and offer a diff.
   NEVER auto-apply upstream changes.
2. **Staleness gate**: read KNOWLEDGE.md frontmatter. If `last_updated` is
   older than `review_interval_days`, run the Refresh Protocol below first.
   The user may say "skip refresh" — then proceed but state the knowledge age.
3. **Use KNOWLEDGE.md** for current domain facts. Cite the relevant section
   when it drives a decision.
4. **Conflict handling — mode: advisory**: if KNOWLEDGE.md contradicts
   something stated in SKILL.md, DO NOT silently override the author. Surface
   the conflict: quote both, recommend which to trust and why, let the user
   decide. The author's judgment stands until a human says otherwise.

## Refresh Protocol

1. Read sources.md. Web-search each entry for changes since `last_updated`.
2. PRIMARY-source findings: apply directly. SECONDARY-source findings: apply
   only if corroborated by a PRIMARY source; otherwise log in CHANGELOG.md as
   "UNCONFIRMED, monitoring".
3. Rewrite only the affected sections of KNOWLEDGE.md; preserve structure.
4. Bump `last_updated` to today.
5. Append every change to CHANGELOG.md with rationale + source URL; log
   "No changes found" on a null refresh.
6. Writes are allowed ONLY inside `.living/`, and only to KNOWLEDGE.md and
   CHANGELOG.md. Never SKILL.md, never sources.md, never this file.
7. Summarize what changed in 2-3 lines before proceeding with the user's task.
