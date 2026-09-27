package de.danoeh.antennapod.net.download.service.episode.autodownload;

import android.content.Context;
import android.database.Cursor;

import androidx.test.platform.app.InstrumentationRegistry;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.queue.Queue;
import de.danoeh.antennapod.net.download.serviceinterface.AutoDownloadManager;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterfaceStub;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueueStub;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.PodDBAdapter;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class DbMultipleQueuesTest {
    private static final long TIMEOUT = 5L;

    private Context context;
    private List<FeedItem> items;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        UserPreferences.init(context);
        PlaybackPreferences.init(context);
        DownloadServiceInterface.setImpl(new DownloadServiceInterfaceStub());
        AutoDownloadManager.setInstance(new AutoDownloadManagerImpl());
        SynchronizationQueue.setInstance(new SynchronizationQueueStub());

        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        items = DbTestUtils.saveFeedlist(1, 6, true).get(0).getItems();
    }

    @After
    public void tearDown() {
        PodDBAdapter.tearDownTests();
        DBWriter.tearDownTests();
    }

    @Test
    public void testDefaultQueueIsActiveByDefault() {
        assertEquals(Queue.DEFAULT_QUEUE_ID, DBReader.getActiveQueueId());
        List<Queue> queues = DBReader.getQueues();
        assertEquals(1, queues.size());
        assertEquals(Queue.DEFAULT_QUEUE_ID, queues.get(0).id);
        assertNull(queues.get(0).title);
    }

    @Test
    public void testCreateQueueActivatesIt() throws Exception {
        DBWriter.createQueue("Second").get(TIMEOUT, TimeUnit.SECONDS);
        List<Queue> queues = DBReader.getQueues();
        assertEquals(2, queues.size());
        assertEquals("Second", queues.get(1).title);
        assertEquals(queues.get(1).id, DBReader.getActiveQueueId());
    }

    @Test
    public void testRenameQueue() throws Exception {
        long queueId = insertQueue("Second");
        DBWriter.renameQueue(queueId, "Renamed").get(TIMEOUT, TimeUnit.SECONDS);
        assertEquals("Renamed", DBReader.getQueues().get(1).title);
    }

    @Test
    public void testAddQueueItemAddsToActiveQueue() throws Exception {
        long queueId = insertQueue("Second");
        DBWriter.switchQueue(queueId).get(TIMEOUT, TimeUnit.SECONDS);

        DBWriter.addQueueItem(context, items.get(0)).get(TIMEOUT, TimeUnit.SECONDS);

        assertQueue(queueId, items.get(0));
        assertQueue(Queue.DEFAULT_QUEUE_ID);
        assertEquals(1, DBReader.getQueue().size());
    }

    @Test
    public void testSwitchQueueChangesQueue() throws Exception {
        long queueId = insertQueue("Second");
        setQueue(Queue.DEFAULT_QUEUE_ID, items.get(0));
        setQueue(queueId, items.get(1));

        DBWriter.switchQueue(queueId).get(TIMEOUT, TimeUnit.SECONDS);
        assertEquals(items.get(1).getId(), DBReader.getQueue().get(0).getId());
        assertEquals(1, DBReader.getQueueIDList().size());

        DBWriter.switchQueue(Queue.DEFAULT_QUEUE_ID).get(TIMEOUT, TimeUnit.SECONDS);
        assertEquals(items.get(0).getId(), DBReader.getQueue().get(0).getId());
    }

    @Test
    public void testAddQueueItemSkipsItemInOtherQueue() throws Exception {
        long queueId = insertQueue("Second");
        setQueue(Queue.DEFAULT_QUEUE_ID, items.get(0));
        DBWriter.switchQueue(queueId).get(TIMEOUT, TimeUnit.SECONDS);

        DBWriter.addQueueItem(context, items.get(0), items.get(1)).get(TIMEOUT, TimeUnit.SECONDS);
        DBWriter.addQueueItemAt(context, items.get(0).getId(), 0).get(TIMEOUT, TimeUnit.SECONDS);

        assertQueue(queueId, items.get(1));
        assertQueue(Queue.DEFAULT_QUEUE_ID, items.get(0));
    }

    @Test
    public void testRemoveQueueItemFromNonActiveQueue() throws Exception {
        long queueId = insertQueue("Second");
        setQueue(Queue.DEFAULT_QUEUE_ID, items.get(0), items.get(1));
        setQueue(queueId, items.get(2), items.get(3), items.get(4));

        DBWriter.removeQueueItem(context, false, items.get(3)).get(TIMEOUT, TimeUnit.SECONDS);

        assertQueue(queueId, items.get(2), items.get(4));
        assertQueue(Queue.DEFAULT_QUEUE_ID, items.get(0), items.get(1));
    }

    @Test
    public void testDeleteFeedRemovesItemsFromAllQueues() throws Exception {
        long queueId = insertQueue("Second");
        setQueue(Queue.DEFAULT_QUEUE_ID, items.get(0), items.get(1));
        setQueue(queueId, items.get(2));

        DBWriter.deleteFeed(context, items.get(0).getFeed().getId()).get(TIMEOUT, TimeUnit.SECONDS);

        assertQueue(Queue.DEFAULT_QUEUE_ID);
        assertQueue(queueId);
    }

    @Test
    public void testGetNextInQueueUsesActiveQueue() throws Exception {
        long queueId = insertQueue("Second");
        setQueue(Queue.DEFAULT_QUEUE_ID, items.get(0), items.get(1));
        setQueue(queueId, items.get(2), items.get(3));

        assertEquals(items.get(1).getId(), DBReader.getNextInQueue(items.get(0)).getId());
        assertNull(DBReader.getNextInQueue(items.get(2)));

        DBWriter.switchQueue(queueId).get(TIMEOUT, TimeUnit.SECONDS);
        assertEquals(items.get(3).getId(), DBReader.getNextInQueue(items.get(2)).getId());
        assertNull(DBReader.getNextInQueue(items.get(0)));
        assertNull(DBReader.getNextInQueue(items.get(3)));
    }

    @Test
    public void testQueueOperationsDoNotTouchOtherQueues() throws Exception {
        long queueId = insertQueue("Second");
        setQueue(Queue.DEFAULT_QUEUE_ID, items.get(0), items.get(1), items.get(2));
        setQueue(queueId, items.get(3), items.get(4));
        DBWriter.switchQueue(queueId).get(TIMEOUT, TimeUnit.SECONDS);

        DBWriter.moveQueueItem(0, 1, false).get(TIMEOUT, TimeUnit.SECONDS);
        assertQueue(queueId, items.get(4), items.get(3));
        assertQueue(Queue.DEFAULT_QUEUE_ID, items.get(0), items.get(1), items.get(2));

        DBWriter.moveQueueItemsToTop(Arrays.asList(items.get(3))).get(TIMEOUT, TimeUnit.SECONDS);
        assertQueue(queueId, items.get(3), items.get(4));

        DBWriter.clearQueue().get(TIMEOUT, TimeUnit.SECONDS);
        assertQueue(queueId);
        assertQueue(Queue.DEFAULT_QUEUE_ID, items.get(0), items.get(1), items.get(2));
    }

    @Test
    public void testSetQueueKeepsOrderAcrossRewrites() {
        long queueId = insertQueue("Second");
        setQueue(Queue.DEFAULT_QUEUE_ID, items.get(0), items.get(1), items.get(2));
        setQueue(queueId, items.get(3), items.get(4));
        setQueue(Queue.DEFAULT_QUEUE_ID, items.get(2), items.get(0), items.get(1));

        assertQueue(Queue.DEFAULT_QUEUE_ID, items.get(2), items.get(0), items.get(1));
        assertQueue(queueId, items.get(3), items.get(4));
        assertEquals(3, getQueueSize(Queue.DEFAULT_QUEUE_ID));
        assertEquals(2, getQueueSize(queueId));
    }

    @Test
    public void testDeleteQueue() throws Exception {
        long queueId = insertQueue("Second");
        setQueue(Queue.DEFAULT_QUEUE_ID, items.get(0));
        setQueue(queueId, items.get(1));
        DBWriter.switchQueue(queueId).get(TIMEOUT, TimeUnit.SECONDS);

        DBWriter.deleteQueue(queueId).get(TIMEOUT, TimeUnit.SECONDS);

        assertEquals(Queue.DEFAULT_QUEUE_ID, DBReader.getActiveQueueId());
        assertEquals(1, DBReader.getQueues().size());
        assertFalse(DBReader.getQueuedItemIds(items.get(1).getId()).contains(items.get(1).getId()));
        assertQueue(Queue.DEFAULT_QUEUE_ID, items.get(0));
    }

    @Test
    public void testDeleteDefaultQueueIsIgnored() throws Exception {
        setQueue(Queue.DEFAULT_QUEUE_ID, items.get(0));
        DBWriter.deleteQueue(Queue.DEFAULT_QUEUE_ID).get(TIMEOUT, TimeUnit.SECONDS);
        assertQueue(Queue.DEFAULT_QUEUE_ID, items.get(0));
    }

    @Test
    public void testUnknownActiveQueueFallsBackToDefault() {
        UserPreferences.setActiveQueueId(12345);
        assertEquals(Queue.DEFAULT_QUEUE_ID, DBReader.getActiveQueueId());
    }

    @Test
    public void testIsInQueueTagCoversAllQueues() throws Exception {
        long queueId = insertQueue("Second");
        setQueue(queueId, items.get(0));
        FeedItem item = DBReader.getFeedItem(items.get(0).getId());
        assertTrue(item.isTagged(FeedItem.TAG_QUEUE));
        assertNotEquals(queueId, DBReader.getActiveQueueId());
    }

    private long insertQueue(String title) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        long queueId = adapter.insertQueue(title);
        adapter.close();
        return queueId;
    }

    private void setQueue(long queueId, FeedItem... queue) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setQueue(queueId, Arrays.asList(queue));
        adapter.close();
    }

    private int getQueueSize(long queueId) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        int size = adapter.getQueueSize(queueId);
        adapter.close();
        return size;
    }

    private void assertQueue(long queueId, FeedItem... expected) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        List<Long> actual = new ArrayList<>();
        try (Cursor cursor = adapter.getQueueIDCursor(queueId)) {
            while (cursor.moveToNext()) {
                actual.add(cursor.getLong(0));
            }
        }
        adapter.close();
        List<Long> expectedIds = new ArrayList<>();
        for (FeedItem item : expected) {
            expectedIds.add(item.getId());
        }
        assertEquals(expectedIds, actual);
    }
}
