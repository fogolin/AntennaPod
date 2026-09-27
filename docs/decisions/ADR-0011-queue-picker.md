# ADR-0011: "Add to queue…" and "Move to queue…" with a queue picker

- **Status:** accepted, 2026-09-27

## Context

- Until now, putting an episode into a queue that isn't active meant switching to that queue first. Moving an episode between queues meant removing it, switching, and adding it again.
- Upstream rejected a queue picker as the **default** "Add to queue" flow (see [upstream research](../findings/upstream-research.md)), but not as an extra action.
- The user added a requirement: people who only use the default queue must see **no change at all**, as maintainers asked.

## Decision

1. **Two extra actions,** next to the existing ones. "Add to queue" and "Remove from queue" are unchanged.
   - **"Add to queue…"** is for episodes that aren't queued. It opens a picker and adds the episodes to the chosen queue.
   - **"Move to queue…"** is for queued episodes. It takes them out of their queue and adds them to the chosen one.
2. **Everywhere episodes have actions,** because the three menus are shared: the long-press menu in every episode list (including the Queue screen and Home), the episode screen and player menu, and multi-select.
3. **Shown only while custom queues exist.** Menus are prepared on the main thread without database access, so the visibility comes from an in-memory flag:
   - `PodDBAdapter.hasCustomQueues`, read through `DBReader.hasCustomQueues()`;
   - computed when the database opens;
   - updated by `insertQueue` and `removeQueue`, the only writers of `Queues`, and reset by `deleteDatabase`.

   A database import force-restarts the app, so the flag is recalculated. The XML items default to `visible="false"`.
4. **The picker** lists the queues in creation order. When every chosen episode sits in the same queue, that queue is left out. It's the first entry that isn't an episode action, so it loads off the main thread (`QueuePickerDialog`).
5. **Moving:**
   - episodes already in the target queue are left where they are;
   - the others are removed from their queue and added with the normal add logic (enqueue location, keep sorted, single membership);
   - both steps run back to back on the DB thread (`DBWriter.moveToQueue`).

   Unqueued episodes in a mixed selection are ignored by "Move to queue…"; "Add to queue…" handles them.

## Rejected alternatives

- **Showing the actions always,** with "New queue…" in the picker. It's simpler, but it changes the menus for single-queue users. The user turned it down.
- **A cached flag in `SharedPreferences`.** It survives a database import unchanged, so it could be wrong until the Queue screen is opened. It would also need a writer on the read path.
- **A "Change" action on the "Added to queue" snackbar.** It doesn't touch the menus, but it's less discoverable, and it offers no way to move an episode that's already queued.

## Consequences

- If the playing episode is moved out of the active queue, playback stops after it, as for any episode outside the active queue ([ADR-0005](ADR-0005-active-queue.md)).
- Before the database has opened after an app start, the flag is `false`. Lists load from the database first, so in practice no episode menu can appear that early.
- A read-only first open (security review phase 5, H10) sets the flag to `false`, which hides the actions until the next writable launch.
