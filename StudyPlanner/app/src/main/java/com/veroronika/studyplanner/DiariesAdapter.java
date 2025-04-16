package com.veroronika.studyplanner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DiariesAdapter extends RecyclerView.Adapter<DiariesAdapter.ViewHolder> {
    private final List<String[]> diaryList;

    public DiariesAdapter(List<String[]> diaryList) {
        this.diaryList = diaryList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_diary, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String[] diary = diaryList.get(position);

        String fullDate = diary[0];
        String[] dateParts = fullDate.split("\\.");
        if (dateParts.length == 3) {
            holder.dateTextView.setText(dateParts[0] + "." + dateParts[1]);
        } else {
            holder.dateTextView.setText(fullDate);
        }
        String diaryText = diary[1];

        StringBuilder modifiedText = new StringBuilder();
        int lineCount = getLineCount(diaryText, modifiedText, 21);


        diaryText = modifiedText.toString();

        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        if (lineCount > 3) {
            params.height += (int) (13 * holder.itemView.getContext().getResources().getDisplayMetrics().density) * (lineCount - 2);
            holder.itemView.setLayoutParams(params);
        }

        holder.textTextView.setText(diaryText);
        holder.timeTextView.setText(diary[2]);
        holder.moodTextView.setText(diary[3]);
        holder.moodTextView.setBackgroundColor(Integer.parseInt(diary[5]));

        holder.ratingTextView.setText(diary[4]);
        holder.ratingTextView.setBackgroundColor(Integer.parseInt(diary[6]));
    }

    public static int getLineCount(String diaryText, StringBuilder modifiedText, int maxLength) {
        int currentIndex = 0;
        int lineCount = 0;

        while (currentIndex < diaryText.length()) {
            int nextCutOff = Math.min(currentIndex + maxLength, diaryText.length());

            
            int spaceBeforeCutOff = diaryText.lastIndexOf(' ', nextCutOff);
            int safeCutOff = (spaceBeforeCutOff != -1 && spaceBeforeCutOff >= currentIndex + maxLength - 10)
                    ? spaceBeforeCutOff
                    : currentIndex + maxLength;

            
            safeCutOff = Math.min(safeCutOff, diaryText.length());

            modifiedText.append(diaryText, currentIndex, safeCutOff).append("\n");
            currentIndex = safeCutOff;

            
            while (currentIndex < diaryText.length() && diaryText.charAt(currentIndex) == ' ') {
                currentIndex++;
            }

            lineCount++;
        }

        return lineCount;
    }




    @Override
    public int getItemCount() {
        return diaryList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView dateTextView;
        final TextView textTextView;
        final TextView timeTextView;
        final TextView moodTextView;
        final TextView ratingTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            textTextView = itemView.findViewById(R.id.textTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            moodTextView = itemView.findViewById(R.id.moodTextView);
            ratingTextView = itemView.findViewById(R.id.ratingTextView);
        }
    }
}
