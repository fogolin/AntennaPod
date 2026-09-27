package de.danoeh.antennapod.model.queue;

import androidx.annotation.Nullable;

public class Queue {

    public static final long DEFAULT_QUEUE_ID = 0;

    public final long id;
    @Nullable public final String title;

    public Queue(long id, @Nullable String title) {
        this.id = id;
        this.title = title;
    }
}
