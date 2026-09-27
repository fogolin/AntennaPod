package de.danoeh.antennapod.ui.screen.queue;

import android.app.Activity;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.ref.WeakReference;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.queue.Queue;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.ui.common.Keyboard;
import de.danoeh.antennapod.ui.common.databinding.EditTextDialogBinding;

public class QueueNameDialog {
    private static final int MAX_NAME_LENGTH = 30;

    private final WeakReference<Activity> activityRef;
    @Nullable private final Queue queue;

    public QueueNameDialog(Activity activity, @Nullable Queue queue) {
        this.activityRef = new WeakReference<>(activity);
        this.queue = queue;
    }

    public void show() {
        Activity activity = activityRef.get();
        if (activity == null) {
            return;
        }

        final EditTextDialogBinding binding = EditTextDialogBinding.inflate(LayoutInflater.from(activity));
        binding.textInput.setHint(R.string.queue_name_label);
        binding.textInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        binding.textInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_NAME_LENGTH)});
        if (queue != null) {
            binding.textInput.setText(queue.title);
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(binding.getRoot())
                .setTitle(queue == null ? R.string.new_queue_label : R.string.rename_queue_label)
                .setPositiveButton(R.string.confirm_label, null)
                .setNegativeButton(R.string.cancel_label, null)
                .show();
        binding.textInput.requestFocus();
        Keyboard.show(activity, binding.textInput);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = binding.textInput.getText().toString().trim();
            if (title.isEmpty()) {
                binding.textInputLayout.setError(activity.getString(R.string.queue_name_empty));
                return;
            }
            if (queue == null) {
                DBWriter.createQueue(title);
            } else {
                DBWriter.renameQueue(queue.id, title);
            }
            dialog.dismiss();
        });
    }
}
