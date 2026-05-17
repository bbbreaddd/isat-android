package com.bread.isat;

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

public class AchievementAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final List<Achievement> mAchievements;
    private boolean mShowHidden = false;

    public AchievementAdapter(List<Achievement> achievements) {
        mAchievements = achievements;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_achievement_header, parent,
                    false);
            return new HeaderViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_achievement, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.btnToggle.setText(mShowHidden ? "Hide Hidden Achievements" : "Show Hidden Achievements");
            headerHolder.btnToggle.setOnClickListener(v -> {
                mShowHidden = !mShowHidden;
                headerHolder.btnToggle.setText(mShowHidden ? "Hide Hidden Achievements" : "Show Hidden Achievements");
                for (int i = 0; i < mAchievements.size(); i++) {
                    Achievement a = mAchievements.get(i);
                    if (a.isHidden && !a.isUnlocked) notifyItemChanged(i + 1);
                }
            });
            return;
        }

        ItemViewHolder itemHolder = (ItemViewHolder) holder;
        Achievement achievement = mAchievements.get(position - 1);

        boolean showDesc = !achievement.isHidden || achievement.isUnlocked || mShowHidden;

        itemHolder.title.setText(achievement.title);

        if (showDesc) {
            itemHolder.description.setText(achievement.description);
        } else {
            itemHolder.description.setText("Hidden Achievement");
        }

        Glide.with(itemHolder.itemView.getContext())
                .load(achievement.isUnlocked ? achievement.iconUrl : achievement.iconGrayUrl)
                .into(itemHolder.icon);

        if (!achievement.isUnlocked) {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);
            itemHolder.icon.setColorFilter(new ColorMatrixColorFilter(matrix));
            itemHolder.itemView.setAlpha(0.7f);
        } else {
            itemHolder.icon.clearColorFilter();
            itemHolder.itemView.setAlpha(1.0f);
        }
    }

    @Override
    public int getItemCount() {
        return mAchievements.size() + 1;
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        android.widget.Button btnToggle;

        HeaderViewHolder(View view) {
            super(view);
            btnToggle = view.findViewById(R.id.btn_toggle_hidden);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView description;

        ItemViewHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.achievement_icon);
            title = view.findViewById(R.id.achievement_title);
            description = view.findViewById(R.id.achievement_description);
        }
    }
}
