# Codebase map: how the queue works in upstream `develop` (34de86f)

This map was made before changing anything, so every change can reuse what exists.

## Storage (`storage/database`)

**Table `Queue`:** `id INTEGER PRIMARY KEY, feeditem INTEGER, feed INTEGER`, with an index on `feeditem`.

- `id` doubles as the position. `PodDBAdapter.setQueue(List)` deletes every row and re-inserts with an explicit `id = i`.
- Every read orders by `Queue.id`.

**`PodDBAdapter`:**

| Method | What it does |
|---|---|
| `setQueue(List)` | Replaces the whole queue. |
| `clearQueue()` | Deletes every row. |
| `getQueueCursor()` | Items plus media, ordered by `id`. |
| `getQueueIDCursor()` | Item ids only. |
| `getNextInQueue(item)` | `id > (id of item)`, limit 1. |
| `getPausedQueueCursor(limit)` | Recently played first. |
| `getQueueSize()` | Row count. |

**`SELECT_KEY_IS_IN_QUEUE`:** every item select computes `id IN (SELECT feeditem FROM Queue)`. `FeedItemCursor` turns it into `FeedItem.TAG_QUEUE`.

**`FeedItemFilterQuery`:** the `QUEUED` and `NOT_QUEUED` filters use the same subselect.

**`getAutoDownloadCandidatesCursor(..., includeQueue)`:** uses `is_in_queue`.

**`removeFeedItems`** does **not** touch `Queue`. Queue rows of deleted items are removed by `DBWriter.deleteFeedItemsSynchronous` through `setQueue`.

**`DBReader`:** `getQueue()`, `getQueueIDList()`, `getRemainingQueueSize(id)`, `getNextInQueue(item)`, `getPausedQueue(limit)`, plus `getNavDrawerData()`, which uses `getQueueSize()`.

**`DBWriter`:**

- All writes run on a single-thread executor (`dbExec`, via `runOnDbThread`).
- Every queue mutation follows the same pattern: `DBReader.getQueue()`, modify the list in Java, `adapter.setQueue(list)`, post `QueueEvent`s.
- Mutations: `addQueueItemAt`, `addQueueItem` (uses `ItemEnqueuePositionCalculator` and `applySortOrder`), `clearQueue`, `removeQueueItem(Synchronous)`, `moveQueueItem`, `moveQueueItemsToTop/Bottom`, `reorderQueue`, `deleteFeedItemsSynchronous`.

**`DBUpgrader`:**

- Runs `if (oldVersion < X)` blocks from `PodDBHelper.onUpgrade`.
- The current `VERSION` is `3110000`, and the last schema block is `< 3080000`.
- `CREATE_*` constants are package-private in `PodDBAdapter` and reused by the upgrader (e.g. `CREATE_TABLE_FAVORITES`).

**`DatabaseExporter.importBackup`** refuses a database whose version is higher than the app's (`import_no_downgrade`).

## Preferences (`storage/preferences/UserPreferences`)

Global queue preferences: `prefQueueKeepSorted`, `prefQueueKeepSortedOrder`, `prefQueueLocked`, `prefEnqueueLocation`, `prefFollowQueue`, `prefEnqueueDownloaded`, `prefEnableAutoDlQueue`, `prefDeleteRemovesFromQueue`.

## Events (`event/QueueEvent`)

Actions: `ADDED`, `ADDED_ITEMS`, `SET_QUEUE`, `REMOVED`, `IRREVERSIBLE_REMOVED`, `CLEARED`, `DELETED_MEDIA`, `SORTED`, `MOVED`.

Subscribers:

| Subscriber | Reaction |
|---|---|
| `QueueFragment` | Applies each action to its list in place. `SET_QUEUE` replaces the list and is used by `moveQueueItemsSynchronous` right before `MOVED` events. |
| `NavDrawerFragment` | Reloads on everything except `DELETED_MEDIA`, `SORTED` and `MOVED`. |
| `QueueSection` (home) | Reloads on everything. |
| `FeedItemlistFragment` | Also subscribes. |

## Call sites outside the database module

About 40 call sites use the API above. Because they go through `DBReader` and `DBWriter`, scoping those methods to the active queue changes the behavior of all of them without editing them:

| Area | Calls |
|---|---|
| UI | `FeedItemMenuHandler`, `EpisodeMultiSelectActionHandler`, `EpisodeItemViewHolder` (in-queue icon), `QueueFragment`, `QueueRecyclerAdapter`, `QueueSection`, `SleepTimerDialog` (`getRemainingQueueSize`), `NavDrawerFragment`/`NavListAdapter` (badge) |
| Swipe actions | `AddToQueueSwipeAction`, `RemoveFromQueueSwipeAction` (undo uses the index from `getQueueIDList`), `MoveToTop/BottomSwipeAction` |
| Playback | The old `PlaybackService` and `Media3PlaybackService` both call `DBReader.getNextInQueue(item)` and auto-add a played item to the queue if it isn't tagged. `MediaLibrarySessionCallback` provides the Android Auto queue list via `getQueue()` and its count via the `FeedItemFilter.QUEUED` total. |
| Downloads | `DownloadServiceInterfaceImpl` (enqueue downloaded), `APCleanupAlgorithm` and `APQueueCleanupAlgorithm` (keep queued episodes), `AutomaticDownloadAlgorithm` (download queued episodes) |
| Other | `FeedDatabaseWriter` (new-episodes action "add to queue"), `SyncService` (remove episodes played elsewhere), `WearListenerService` (sends `getQueue()`), `Feed`/`FeedItemFilter` (tag checks) |

## UI patterns to reuse

**Subscriptions tag chips:**

- `SubscriptionTagAdapter` binds `item_tag_chip.xml` (a Material filter chip).
- `SubscriptionFragment` keeps the selected tag in fragment preferences (`PREF_LAST_TAG`).
- Long-press opens a context menu handled by `TagMenuHandler`, which offers rename (`RenameFeedDialog`) and delete (`ConfirmationDialog`).

**Text input dialogs:** `EditTextDialogBinding` with `MaterialAlertDialogBuilder`, as in `RenameFeedDialog`, `EditUrlSettingsDialog` and `AddFeedFragment`.

## Tests

- Robolectric tests for the queue live in `net/download/service/src/test/.../autodownload/`: `DbWriterTest` (add, remove, move, clear, delete-with-queue), `DbReaderTest` (`getQueue`, `getQueueIdList`, nav drawer queue size), `DbCleanupTests` and `DbQueueCleanupAlgorithmTest`.
- Test setup: `PodDBAdapter.init`, then `deleteDatabase()` (empties every table in `ALL_TABLES`), with `DbTestUtils.saveFeedlist` as the fixture builder.
- 12 test and `androidTest` lines call `adapter.setQueue(list)` directly.

## Upstream churn (last 12 months, for fork maintenance)

59 commits touched the files this feature changes:

| File | Commits |
|---|---|
| `MediaLibrarySessionCallback` | 24 |
| `PodDBAdapter` | 16 |
| `DBReader` | 10 |
| `DBWriter` | 8 |
| `ui/screen/queue` | 8 |
| `UserPreferences` | 5 |

Releases come every one to two months.
