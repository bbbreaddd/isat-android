package com.cafeed28.omori;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {
    private final List<Achievement> mAchievements;

    public AchievementAdapter(List<Achievement> achievements) {
        mAchievements = achievements;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_achievement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Achievement achievement = mAchievements.get(position);

        boolean showContent = !achievement.isHidden || achievement.isUnlocked;

        if (showContent) {
            holder.title.setText(achievement.title);
            holder.description.setText(achievement.description);
            Glide.with(holder.itemView.getContext())
                    .load(achievement.isUnlocked ? achievement.iconUrl : achievement.iconGrayUrl)
                    .into(holder.icon);
        } else {
            holder.title.setText("Hidden Achievement");
            holder.description.setText("Explore to unlock this hidden achievement.");
            Glide.with(holder.itemView.getContext())
                    .load(achievement.iconGrayUrl)
                    .into(holder.icon);
        }

        if (!achievement.isUnlocked) {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);
            holder.icon.setColorFilter(new ColorMatrixColorFilter(matrix));
            holder.itemView.setAlpha(0.7f);
        } else {
            holder.icon.clearColorFilter();
            holder.itemView.setAlpha(1.0f);
        }
    }

    @Override
    public int getItemCount() {
        return mAchievements.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView description;

        ViewHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.achievement_icon);
            title = view.findViewById(R.id.achievement_title);
            description = view.findViewById(R.id.achievement_description);
        }
    }
}
