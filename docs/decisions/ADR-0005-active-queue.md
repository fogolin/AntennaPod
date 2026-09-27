# ADR-0005: The active queue drives playback and every enqueue

- **Status:** accepted, 2026-09-26. This is ByteHamster's #8070 direction (Nov 19, 2025) plus plan decision D2. Amended by [ADR-0010](ADR-0010-podcast-queue.md): automatic adds follow the podcast's queue when one is set.

## Decision

- One preference, `prefActiveQueue` (a long, default `0`), names the active queue.
- The Queue screen, manual and automatic adds, continuous playback, Android Auto, Wear, the home section and the drawer badge all use it.
- **Continuous playback:** `DBReader.getNextInQueue(item)` looks only in the active queue. If the finished episode isn't in the active queue (for example, it was played from the episode list while it sits in another queue), there is no next episode and playback stops. That is the same thing `getNextInQueue` returning `null` does today.
- Switching queues while something is playing doesn't interrupt it. What follows comes from the newly active queue, by the rule above.
- `DBWriter.switchQueue(id)` is the only writer of the preference apart from `deleteQueue`. Both run on `dbExec`, so a switch can't interleave with a queue write (see ADR-0001).

## Rejected alternative

Continue in the finished episode's own queue. It avoids the stop, but then the queue that keeps playing isn't the one the user selected, and v1 has no player-screen indicator to explain that.
