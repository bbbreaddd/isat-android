package com.cafeed28.omori;

public class Achievement {
    public String id;
    public String title;
    public String description;
    public String iconUrl;
    public String iconGrayUrl;
    public boolean isHidden;
    public boolean isUnlocked;

    public Achievement(String id, String title, String description, String iconUrl, String iconGrayUrl, boolean isHidden, boolean isUnlocked) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconUrl = iconUrl;
        this.iconGrayUrl = iconGrayUrl;
        this.isHidden = isHidden;
        this.isUnlocked = isUnlocked;
    }
}
