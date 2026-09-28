# Gap review: requests and maintainer statements vs. the fork

- **Date:** 2026-09-28
- **Fork state:** phases 1–6 merged (release `v3.12.1-mq.5`)
- **Why:** the user collected every upstream thread about multiple queues. This checks what the fork covers, what it leaves out, and where it goes against what maintainers said.

## Sources read (in addition to [upstream-research.md](upstream-research.md))

| Source | Content |
|---|---|
| Issue #1611 (closed, duplicate) | Themed collections (history series vs news). Suggests tags that podcasts can have, and queues built from tags. mfietz: duplicate of #900 (playlists) / #851 (tags). |
| Issue #2908 (closed, duplicate of #2648) | Named lists, sorting per list, new episodes automatically in the assigned lists, downloads regardless of list. |
| Issue #4739 (closed, duplicate) | Genre playlists (humour, science…) through tags or multiple queues from the context menu. keunes points to #2648 and tags (#1711). |
| Issue #5095 (open, related to #2648) | Two queues with priority: play the high-priority queue (news) until it's empty, then continue with the low-priority one (history backlog). Each podcast picks its queue. |
| Issue #2648 (locked by keunes: "needs and interest are clear") | Re-read for the latest comments. It adds nothing new beyond the 2022 and Oct 2025 statements already in the plan. User use cases: a kids' queue at a different speed and language (mhellwig), a priority queue that jumps ahead (wesnm), four queues with progression between them (kjetilk). |
| PR #3221 (2019), re-read | ByteHamster: a play button on a queue is unclear ("not clear which item will be played"). A feed's items should be in one queue only. "Changing the queue should move existing items to new queue". "Most queue settings should probably be adjustable per queue". "'Move to queue x' menu entries … should only be shown if the user has multiple queues". |
| PR #8066, PR #8070, re-read | Nothing new beyond the plan: AI-looking structure and code without human review are rejected. |
| Forum 2670 (#1–#15) | Igor's 2023 impact map, keunes' 2023 answers, and ByteHamster's reply of 2026-09-28 to the user (below). |
| Forum 3211 | Named queues, and moving items between queues. Some users worry that playlists replace the main queue ("If playlists don't cancel the main queue, that calms me down"). |
| Forum 8635 | Sorting one queue by tag priority. |
| Forum 415 | 2020 question, pointing to #2648. |

## ByteHamster, forum 2670 #15, 2026-09-28 (the latest statement)

> The important part here is not writing the code, but doing usability testing and interaction design. How we indicate what the next episode will be, to what playlist something gets added, how to switch between playlists, etc., are quite subtle decisions that can make the app much harder to use when done wrong. […] the first result to discuss should be UX design and a couple of sentences of architecture (not a huge wall of AI generated plans that nobody wants to read). Once we have the design, we need to find some users and do something like a think-aloud study.

## What the fork covers

| Request | Where |
|---|---|
| Named manual queues: create, rename, delete, switch in place, remembered | Phases 1–2 |
| Nothing changes with one queue (except the "New queue" menu item) | Phases 2, 5, 6 |
| "Move to queue x" only with multiple queues (#3221) | Phase 6 |
| Moving items between queues (forum 3211) | Phase 6 |
| A podcast's new episodes go to a chosen queue (#2908, #5095, keunes 2023/2025) | Phase 5, automatic adds only |
| Downloads regardless of queue (#2908) | "Automatically download queued episodes" already covers every queue |
| Episode actions (played, position, delete) are global, not per queue (keunes 2023) | Played state and position belong to the episode |
| Swipe actions global, no queue in the notification (keunes 2023) | Unchanged |
| No drawer management (ByteHamster 2022) | Queue screen only |
| The main queue stays the default (forum 3211) | Default queue `0`, can't be deleted |
| Sync (keunes' question, Oct 2025) | Nothing to decide: gpodder.net and Nextcloud sync episode actions, not the queue. An episode played elsewhere is removed from whichever queue holds it. |

## Gaps

### A. The questions maintainers care about most (UX)

| # | Gap | Asked by |
|---|---|---|
| A1 | **Nothing shows which queue is playing or what plays next.** Playing an episode that isn't in the active queue silently stops after it (ADR-0005). The player screen and episode screen don't name a queue. | ByteHamster 2019 (#3221), 2022, Oct 2025, 2026-09-28; Igor 2023; Matth7878 |
| A2 | **Nothing tells you where an add went.** The "added to queue" message and the in-queue icon don't name the queue. This matters more with phase 5, where automatic adds can go to a queue other than the active one. | ByteHamster 2026-09-28 ("to what playlist something gets added"), Igor 2023 |
| A3 | **No UX write-up and no usability test.** The team wants a short design proposal and a think-aloud study before code. | ByteHamster 2026-09-28 |

### B. Requested behavior the fork doesn't have

| # | Gap | Asked by | Note |
|---|---|---|---|
| B1 | Settings per queue: sort order and keep-sorted, lock, enqueue location | ByteHamster 2019, keunes 2023 and Oct 2025, #2908, Igor 2023 | Backlog. v1 keeps them global (ADR-0006). |
| B2 | Rename the default queue | keunes 2023, Igor 2023 | Backlog (ADR-0003). |
| B3 | Queue priority: continue with the next queue when one is empty | #5095, wesnm and kjetilk on #2648 | Backlog ("queue rotation"). |
| B4 | Changing a podcast's queue doesn't move its already-queued episodes | ByteHamster 2019 (#3221) | Phase 5 only affects future automatic adds. |
| B5 | Deleting a queue can't move its episodes to another queue. They're unqueued. | Igor 2023 | Small, and fits the phase 6 picker. |
| B6 | Playlists that keep played episodes | AndreLevy (forum 2670), gomezz (forum 3211), #900 | Every fork queue behaves like the queue: played episodes leave it. |
| B7 | Several queues per episode, or per podcast | #2908, Matth7878, keunes Oct 2025 | Not done on purpose (ADR-0004). |
| B8 | Queues built from tags | #1611, #4739, ByteHamster 2022 ("prefer tags") | The user chose per podcast (ADR-0010). Tags stay in the backlog. |
| B9 | Speed per queue (a kids' queue at 1×) | mhellwig on #2648 | Speed per podcast already exists upstream, which covers most of it. |
| B10 | Android Auto and Wear: browse all queues | keunes: deferred until the UX is clear | Backlog. |
| B11 | Translations: the fork's new texts are English only | the user, 2026-09-28 | Upstream translates through Weblate, and `AGENTS.md` allows only the English file. |

### C. Where the fork goes against a maintainer statement

| # | Statement | Fork |
|---|---|---|
| C1 | "The items of a feed should only be in one queue" (ByteHamster 2019) | Manual adds and the picker let one podcast's episodes sit in several queues. Later statements (Nov 2025: add to the active queue) imply the same, so the 2019 rule looks superseded, but it's never been said. |
| C2 | Manual MVP only, nothing automatic (Oct 2025) | Phase 5 routing is automation, and phase 6 is extra scope. Fine for the fork. Not part of an upstream MVP. |
| C3 | Design and user testing before code (2026-09-28) | The fork went code-first. It can serve as the prototype for the study. |

## Suggested order

1. **A1 + A2:** show the active queue where playback and adds happen. For example: name the queue in the "Added to …" message, show "Playing from: X" or "Up next" somewhere, and decide what happens after an episode that isn't in the active queue. This is ByteHamster's first question and the most visible hole.
2. **A3:** a short UX proposal for the forum (screens, rules, two sentences of architecture), then a small think-aloud study with testers using the fork's release APK.
3. **B5, B4:** small, and they make phases 5 and 6 more predictable.
4. **B1, B2, B3:** larger. They wait for the UX discussion.
