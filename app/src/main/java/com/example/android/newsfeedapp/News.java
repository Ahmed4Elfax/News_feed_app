package com.example.android.newsfeedapp;

import android.graphics.Bitmap;

public class News {
    private String mTitle;
    private String mAuthor;
    private String mSection;
    private String mDate;
    private Bitmap mImage;
    private String mLink;

    public News (String title, String author, String section, String date, Bitmap image, String link) {
        this.mTitle = title;
        this.mAuthor = author;
        this.mSection = section;
        this.mDate = date;
        this.mImage = image;
        this.mLink = link;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public String getSection() {
        return mSection;
    }

    public String getDate() {
        return mDate;
    }

    public Bitmap getImage() {
        return mImage;
    }

    public String getLink() {
        return mLink;
    }
}
