# Phase 5: a queue per podcast

- **Branch:** `mq/phase-5-routing`, cut from `multiple-queues` (`d9be176`) after the upstream sync check. The fork was level with upstream `develop`, so there was nothing to merge. Its PR goes into `multiple-queues`.
- **Date:** 2026-09-27
- **Decision:** [ADR-0010](../decisions/ADR-0010-podcast-queue.md)

## Why

Routing podcasts to queues was the most requested follow-up in the backlog. For example, news podcasts can go to a "News" queue and long shows to a "Running" queue, without switching queues first.

## Decisions (the user's choices)

- **Per podcast** (not per tag).
- **Automatic adds only.** The "Add to queue" new-episodes action and "Enqueue downloaded" follow the podcast's queue. Manual adds keep using the active queue.

## What changed

| File | Change |
|---|---|
| `model/…/queue/Queue.java` | `ACTIVE_QUEUE_ID = -1`: "whichever queue is active". |
| `model/…/feed/FeedPreferences.java` | A `queueId` field (default `-1`) with a getter and setter. The constructors are unchanged. |
| `storage/database/…/PodDBAdapter.java` | The `feed_queue` column: schema, `KEYS_FEED`, `setFeedPreferences`. `removeQueue` resets podcasts that pointed to the deleted queue. |
| `storage/database/…/DBUpgrader.java` | Guarded `ALTER TABLE Feeds ADD COLUMN feed_queue` in `upgradeMultipleQueues`. |
| `storage/database/…/mapper/FeedPreferencesCursor.java` | Reads the column. |
| `storage/database/…/DBWriter.java` | `addQueueItem(context, queueId, items)`: picks the target queue on the DB thread (falling back to the active queue) and posts no `QueueEvent` when the target isn't active. The old overload delegates to it. |
| `storage/database/…/FeedDatabaseWriter.java` | The "Add to queue" new-episodes action uses the podcast's queue. |
| `net/download/service/…/DownloadServiceInterfaceImpl.java` | "Enqueue downloaded" uses the podcast's queue. |
| `app/…/feed/preferences/FeedSettingsPreferenceFragment.java`, `res/xml/feed_settings.xml` | "Queue for new episodes" under Automation, hidden with only one queue. |
| `ui/i18n/…/values/strings.xml` | `feed_queue_title`, `feed_queue_active`. |
| Tests | `DbMultipleQueuesTest`: four new tests. `DBUpgraderMultipleQueuesTest`: a `Feeds` table in the fixture and one new test. |
| `docs/…` | ADR-0010, this log, reviews, the feature spec, fixtures, the backlog and the index. |

## Reuse check

Before writing code, these were checked and reused (details in [perf/phase-5.md](../perf/phase-5.md)):

- the two automatic paths (`FeedDatabaseWriter.updateFeed` and `DownloadServiceInterfaceImpl.getRequest`);
- how podcast settings are stored (`Feeds` columns, `FeedPreferencesCursor`, `setFeedPreferences`) and shown (`MaterialListPreference` in `feed_settings.xml`);
- the phase 1 helpers (`getQueues`, `queueExists`, `upgradeMultipleQueues`);
- the existing enqueue logic in `DBWriter.addQueueItem`.

Findings:

- **Automatic download** doesn't enqueue by itself. With automatic download on, new episodes go to the inbox, and "Enqueue downloaded" queues them once downloaded. Routing that one path covers it.
- **The download cancel path** removes the episode from whichever queue holds it (`removeQueueItem` is queue-agnostic since phase 1), so no change was needed there.
- **"Queued"** (`FeedItem.TAG_QUEUE`) spans all queues, so "Automatically download queued episodes" also covers routed episodes.

## Security and performance

- [security/phase-5.md](../security/phase-5.md): no SQL injection surface, no orphaned queue rows, no wrong list on the Queue screen, null-safe in the download path.
- [perf/phase-5.md](../perf/phase-5.md): one primary-key lookup per add, and fewer UI reloads when adding to a queue that isn't active.

## Local checks

| Check | Result |
|---|---|
| checkstyle 10.12.0 on the changed Java files | clean (the existing `DBUpgraderMultipleQueuesTest` class-name warning from phase 1 isn't counted; it's outside this change) |
| android-xml-formatter on `feed_settings.xml` | no changes |

The workspace can't reach Google Maven, so building and the unit tests run in CI on the PR.

## How to test (debug APK from the PR, or the release after merging)

1. **Setup:** have at least two queues, for example "Queue" and "News". Make "Queue" the active one.
2. **The setting:** open a podcast → ⋮ → Podcast settings. Under Automation, below "New episodes action", there's **"Queue for new episodes"** showing "Active queue".
   - With only one queue, the setting isn't shown.
3. **New episodes:** set the podcast's "New episodes action" to "Add to queue" and "Queue for new episodes" to "News". Pull to refresh when the podcast has a new episode. The episode lands in "News". "Queue" stays as it was, and its list doesn't jump.
   - Tip: a podcast that publishes daily gives you a new episode fastest. Step 4 tests the same routing without waiting.
4. **Downloads:** with Settings → Playback → "Enqueue downloaded" on (it's on by default), download an unqueued episode of that podcast. It goes to "News".
5. **Manual adds are unchanged:** "Add to queue" on an episode of that podcast adds it to the active queue.
6. **Delete the target queue:** delete "News". The podcast's setting is back to "Active queue".
7. **Back to default:** set it to "Active queue", and new episodes follow the active queue again.

## Results

- **CI on the PR:** all 12 checks green, including the unit tests and emulator tests on API 23, 30 and 36.
- **Merged** by the user through PR #7 (merge commit `0ee0c8b`). Released as [v3.12.1-mq.4](https://github.com/fogolin/AntennaPod/releases/tag/v3.12.1-mq.4).
