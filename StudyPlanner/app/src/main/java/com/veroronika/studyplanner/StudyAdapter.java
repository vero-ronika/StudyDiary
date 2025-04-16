package com.veroronika.studyplanner;

import static com.veroronika.studyplanner.DiariesAdapter.getLineCount;

import android.app.AlertDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

public class StudyAdapter extends RecyclerView.Adapter<StudyAdapter.ViewHolder> {
    private final List<String[]> studyList;

    private final int[] tags;

    public StudyAdapter(List<String[]> studyList, int[] tags) {
        this.studyList = studyList;
        this.tags = tags;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_study, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String[] studyEntry = studyList.get(position);
        Log.d("StudyText", "Time: " + studyEntry[4]);
        Log.d("StudyText", "Text: " + studyEntry[2]);

        if (studyEntry.length >= 5) {
            holder.dateTextView.setText(studyEntry[0].length() > 6 ? studyEntry[0].substring(0, 6) : studyEntry[0]);
            holder.stopwatchTextView.setText(studyEntry[4]);

            holder.timeTextView.setText("");

            String text = studyEntry[2]; 

            if (text != null) {
                StringBuilder modifiedText = new StringBuilder();
                int lineCount = getLineCount(text, modifiedText, 14);
                text = modifiedText.toString().trim();

            }

            holder.textTextView.setText(text);

            try {
                int tagIndex = Integer.parseInt(studyEntry[3]);
                if (tagIndex >= 0 && tagIndex < tags.length) {
                    String subjectName = studyEntry[1];
                    updateTagImageView(holder.tagImageView, tagIndex, subjectName);
                } else {
                    holder.tagImageView.setImageResource(tags[0]);
                    holder.tagImageView.setContentDescription("Default tag");
                }
            } catch (NumberFormatException e) {
                holder.tagImageView.setImageResource(tags[0]);
                holder.tagImageView.setContentDescription("Default tag");
            }
        } else {
            holder.dateTextView.setText("No Date");
            holder.stopwatchTextView.setText("No Time");
            holder.textTextView.setText("No Text");
            holder.timeTextView.setText("Load Time: Not Available");  
            holder.tagImageView.setImageResource(tags[0]);
            holder.tagImageView.setContentDescription("Default tag");
        }
        holder.itemView.setOnLongClickListener(v -> {
            Context context = holder.itemView.getContext();
            if (context instanceof StudyActivity) {
                showDeleteSessionDialog((StudyActivity) context, studyEntry);
            }
            return true;
        });
    }

    private void showDeleteSessionDialog(StudyActivity context, String[] session) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Study Session")
                .setMessage("Are you sure you want to delete this study session?")
                .setPositiveButton("Yes", (dialog, which) -> confirmAndDeleteStudySession(context, session))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void confirmAndDeleteStudySession(StudyActivity context, String[] session) {
        boolean isFirebase = FirebaseAuth.getInstance().getCurrentUser() != null;
        fetchStudySessionAndDelete(context, session, isFirebase);
    }

    private void fetchStudySessionAndDelete(Context context, String[] session, boolean isFirebase) {
        String date = session[0];
        String time = session[5]; // Assuming session[1] == time saved in Firebase under "time"

        Log.d("SessionCheck", "Looking for session with date: " + date + ", time: " + time);

        if (isFirebase) {
            String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
            DatabaseReference database = FirebaseDatabase.getInstance().getReference("study_sessions").child(userId);

            database.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    boolean deleted = false;
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String fetchedDate = snapshot.child("date").getValue(String.class);
                        String fetchedTime = snapshot.child("time").getValue(String.class);

                        Log.d("SessionCheck", "Fetched date: " + fetchedDate + ", time: " + fetchedTime);

                        if (date.equals(fetchedDate) && time.equals(fetchedTime)) {
                            snapshot.getRef().removeValue()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Session deleted", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show());
                            deleted = true;
                            break;
                        }
                    }
                    if (!deleted) {
                        Toast.makeText(context, "Session not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.w("Firebase", "Failed to delete session", databaseError.toException());
                }
            });
        } else {
            // SQLite deletion stays the same
            DatabaseHelper databaseHelper = new DatabaseHelper(context);
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            int deletedRows = db.delete(DatabaseHelper.TABLE_STUDY_SESSION,
                    DatabaseHelper.COLUMN_DATE + " = ? AND " + DatabaseHelper.COLUMN_TIME + " = ?",
                    new String[]{date, time});

            if (deletedRows > 0) {
                Toast.makeText(context, "Session deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show();
            }
            db.close();
        }
    }


    private void updateTagImageView(ImageView imageView, int tagIndex, String subjectName) {
        if (tagIndex >= 0 && tagIndex < tags.length) {
            imageView.setImageResource(tags[tagIndex]);
            imageView.setContentDescription(subjectName + " tag");
        }
    }


    @Override
    public int getItemCount() {
        return studyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView dateTextView;
        final TextView stopwatchTextView;
        final TextView textTextView;
        final TextView timeTextView;
        final ImageView tagImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            stopwatchTextView = itemView.findViewById(R.id.stopwatchTextView);
            textTextView = itemView.findViewById(R.id.textTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            tagImageView = itemView.findViewById(R.id.tagImageView);
        }
    }
}
