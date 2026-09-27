package de.danoeh.antennapod.storage.database;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import de.danoeh.antennapod.model.queue.Queue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class DBUpgraderMultipleQueuesTest {
    private SQLiteDatabase db;

    @Before
    public void setUp() {
        db = SQLiteDatabase.create(null);
        db.execSQL("CREATE TABLE Queue(id INTEGER PRIMARY KEY,feeditem INTEGER,feed INTEGER)");
        db.execSQL("INSERT INTO Queue (id, feeditem, feed) VALUES (0, 10, 1)");
        db.execSQL("INSERT INTO Queue (id, feeditem, feed) VALUES (1, 11, 1)");
        db.execSQL("CREATE TABLE Feeds(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT)");
        db.execSQL("INSERT INTO Feeds (title) VALUES ('Podcast')");
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void testExistingQueueMovesToDefaultQueue() {
        DBUpgrader.upgradeMultipleQueues(db);

        try (Cursor cursor = db.rawQuery("SELECT feeditem, queue FROM Queue ORDER BY id", null)) {
            assertEquals(2, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(10, cursor.getLong(0));
            assertEquals(Queue.DEFAULT_QUEUE_ID, cursor.getLong(1));
            cursor.moveToNext();
            assertEquals(11, cursor.getLong(0));
            assertEquals(Queue.DEFAULT_QUEUE_ID, cursor.getLong(1));
        }
        assertEquals(0, DatabaseUtils.queryNumEntries(db, PodDBAdapter.TABLE_NAME_QUEUES));
    }

    @Test
    public void testUpgradeIsIdempotent() {
        DBUpgrader.upgradeMultipleQueues(db);
        db.execSQL("INSERT INTO Queues (title) VALUES ('Second')");
        DBUpgrader.upgradeMultipleQueues(db);
        assertEquals(1, DatabaseUtils.queryNumEntries(db, PodDBAdapter.TABLE_NAME_QUEUES));
    }

    @Test
    public void testExistingFeedsFollowActiveQueue() {
        DBUpgrader.upgradeMultipleQueues(db);
        db.execSQL("INSERT INTO Feeds (title) VALUES ('Second podcast')");
        try (Cursor cursor = db.rawQuery("SELECT feed_queue FROM Feeds", null)) {
            assertEquals(2, cursor.getCount());
            while (cursor.moveToNext()) {
                assertEquals(Queue.ACTIVE_QUEUE_ID, cursor.getLong(0));
            }
        }
    }

    @Test
    public void testRowsWrittenWithoutQueueColumnGoToDefaultQueue() {
        DBUpgrader.upgradeMultipleQueues(db);
        db.execSQL("INSERT INTO Queue (id, feeditem, feed) VALUES (5, 12, 1)");
        try (Cursor cursor = db.rawQuery("SELECT queue FROM Queue WHERE feeditem = 12", null)) {
            cursor.moveToFirst();
            assertEquals(Queue.DEFAULT_QUEUE_ID, cursor.getLong(0));
        }
    }
}
