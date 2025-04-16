package com.veroronika.studyplanner;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.veroronika.studyplanner.database.DatabaseHelper;

import java.util.List;
import java.util.Objects;

public class RemindersAdapter extends RecyclerView.Adapter<RemindersAdapter.ViewHolder> {
    private final List<String[]> reminderList;
    private final int[] tags;

    public RemindersAdapter(List<String[]> reminderList, int[] tags) {
        this.reminderList = reminderList;
        this.tags = tags;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String[] reminder = reminderList.get(position);


        holder.dateTextView.setText(reminder[0]);
        String reminderText = reminder[1];
        if (reminderText != null) {
            if (reminderText.length() >= 12) {
                if (reminderText.contains(" ")) {
                    int spaceIndex = reminderText.indexOf(" ");
                    reminderText = reminderText.substring(0, spaceIndex);
                } else if (reminderText.length() > 16) {
                    reminderText = reminderText.substring(0, 12) + "...";
                } else {
                    holder.textTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                }
            }
        }

        holder.textTextView.setText(reminderText);

        if (reminder.length > 4) {
            int tagIndex;
            try {
                tagIndex = Integer.parseInt(reminder[4]);
                if (tagIndex >= 0 && tagIndex < tags.length) {
                    String subjectName = reminder[3];
                    updateReminderImageView(holder.reminderImageView, tagIndex, subjectName);
                } else {
                    holder.reminderImageView.setImageResource(tags[0]);
                }
            } catch (NumberFormatException e) {
                holder.reminderImageView.setImageResource(tags[0]);
            }
        } else {
            holder.reminderImageView.setImageResource(tags[0]);
        }
        holder.itemView.setOnClickListener(v -> {
            int reminderIndex = Integer.parseInt(reminder[4]);
            Context context = holder.itemView.getContext();
            ((AllRemindersActivity) context).loadFullReminderDetails(reminderIndex);
        });

        holder.itemView.setOnLongClickListener(v -> {
            Context context = holder.itemView.getContext();
            if (context instanceof AllRemindersActivity) {
                showDeleteConfirmationDialog((AllRemindersActivity) context, reminder);
            }
            return true;
        });
    }

    private void showDeleteConfirmationDialog(AllRemindersActivity context, String[] reminder) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Reminder")
                .setMessage("Are you sure you want to delete this reminder?")
                .setPositiveButton("Yes", (dialog, which) -> confirmAndDeleteReminder(context, reminder))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void confirmAndDeleteReminder(AllRemindersActivity context, String[] reminder) {
        boolean isFirebase = FirebaseAuth.getInstance().getCurrentUser() != null;
        fetchReminderDetailsAndDelete(context, reminder, isFirebase);
    }

    private void fetchReminderDetailsAndDelete(Context context, String[] reminder, boolean isFirebase) {
        String dateReminder = reminder[0];
        String timeReminder = reminder[2];

        if (isFirebase) {
            String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
            DatabaseReference database = FirebaseDatabase.getInstance().getReference("study_entries").child(userId);

            database.orderByChild("dateReminder").equalTo(dateReminder)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                String fetchedTime = snapshot.child("timeReminder").getValue(String.class);
                                if (fetchedTime != null && fetchedTime.equals(timeReminder)) {
                                    snapshot.getRef().removeValue()
                                            .addOnSuccessListener(aVoid -> Toast.makeText(context, "Reminder deleted", Toast.LENGTH_SHORT).show())
                                            .addOnFailureListener(e -> Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show());
                                    return;
                                }
                            }
                            Toast.makeText(context, "Reminder not found", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.w("Firebase", "Failed to delete reminder", databaseError.toException());
                        }
                    });

        } else {
            DatabaseHelper databaseHelper = new DatabaseHelper(context);
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            int deletedRows = db.delete(DatabaseHelper.TABLE_STUDY,
                    DatabaseHelper.COLUMN_DATE_REMINDER + " = ? AND " + DatabaseHelper.COLUMN_TIME_REMINDER + " = ?",
                    new String[]{dateReminder, timeReminder});

            if (deletedRows > 0) {
                Toast.makeText(context, "Reminder deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show();
            }
            db.close();
        }
    }

    @Override
    public int getItemCount() {
        return reminderList.size();
    }

    private void updateReminderImageView(ImageView imageView, int tagIndex, String subjectName) {
        if (tagIndex >= 0 && tagIndex < tags.length) {
            imageView.setImageResource(tags[tagIndex]);
            imageView.setContentDescription(subjectName + " tag");
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView dateTextView;
        final TextView textTextView;
        final ImageView reminderImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            textTextView = itemView.findViewById(R.id.textTextView);
            reminderImageView = itemView.findViewById(R.id.reminderImageView1);
        }
    }
}
