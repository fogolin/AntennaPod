package de.danoeh.antennapod.ui.screen.queue;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.StringRes;
import androidx.core.util.Consumer;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.queue.Queue;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.LongList;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class QueuePickerDialog {
    private static final String TAG = "QueuePickerDialog";

    private final Activity activity;
    @StringRes private final int title;
    private final long[] itemIds;
    private final Consumer<Queue> onQueueChosen;

    public QueuePickerDialog(Activity activity, @StringRes int title, long[] itemIds,
                             Consumer<Queue> onQueueChosen) {
        this.activity = activity;
        this.title = title;
        this.itemIds = itemIds;
        this.onQueueChosen = onQueueChosen;
    }

    public void show() {
        Observable.fromCallable(() -> {
            List<Queue> queues = DBReader.getQueues();
            LongList currentQueueIds = DBReader.getQueueIdsOfItems(itemIds);
            if (currentQueueIds.size() == 1) {
                for (int i = 0; i < queues.size(); i++) {
                    if (queues.get(i).id == currentQueueIds.get(0)) {
                        queues.remove(i);
                        break;
                    }
                }
            }
            return queues;
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showQueues, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void showQueues(List<Queue> queues) {
        if (activity.isFinishing() || activity.isDestroyed() || queues.isEmpty()) {
            return;
        }
        String[] names = new String[queues.size()];
        for (int i = 0; i < queues.size(); i++) {
            names[i] = queues.get(i).title != null ? queues.get(i).title : activity.getString(R.string.queue_label);
        }
        new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setItems(names, (dialog, which) -> onQueueChosen.accept(queues.get(which)))
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }
}
