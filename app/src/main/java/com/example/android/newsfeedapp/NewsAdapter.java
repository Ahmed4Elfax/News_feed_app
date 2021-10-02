package com.example.android.newsfeedapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.List;

public class NewsAdapter extends ArrayAdapter<News> {
    private Context context;
    public NewsAdapter(Context context, List<News> news) {
        super(context, 0, news);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Check if there is an existing list item view (called convertView) that we can reuse,
        // otherwise, if convertView is null, then inflate a new list item layout.
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.news_item, parent, false);
        }
        News news = getItem(position);
        TextView authorText = (TextView) listItemView.findViewById(R.id.author_text);
        if (!news.getAuthor().equals("not known")) {
            authorText.setText(news.getAuthor());
        }

        TextView titleText = listItemView.findViewById(R.id.title_text);
        titleText.setText(news.getTitle());

        TextView dateText = (TextView) listItemView.findViewById(R.id.date_text);
        dateText.setText(news.getDate());

        TextView sectionText = (TextView) listItemView.findViewById(R.id.section_text);
        sectionText.setText(news.getSection() + " /");

        ImageView newsImage = (ImageView) listItemView.findViewById(R.id.news_image);
        // checking for available image - if none found, using placeholder image
        if (news.getImage() != null) {
            newsImage.setImageBitmap(news.getImage());
        } else {
            newsImage.setImageResource(R.drawable.placeholder_500x300);
        }

        ImageView backGround1 = (ImageView) listItemView.findViewById(R.id.background_image1);
        ImageView backGround2 = (ImageView) listItemView.findViewById(R.id.background_image2);

        // setting colours
        int color = getCategoryColour(news.getSection(),listItemView);
        backGround1.setBackgroundColor(color);
        backGround2.setBackgroundColor(color);
        sectionText.setTextColor(color);
        authorText.setTextColor(color);


        return  listItemView ;
    }

    private int getCategoryColour(String category,View listItemView) {

        int categoryColourId;
        if (context.getString(R.string.news_colour_categories).contains(category)) {
            categoryColourId = R.color.news;
        } else if (context.getString(R.string.opinion_colour_categories).contains(category)) {
            categoryColourId = R.color.opinion;
        } else if (context.getString(R.string.sports_colour_categories).contains(category)) {
            categoryColourId = R.color.sports;
        } else if (context.getString(R.string.culture_colour_categories).contains(category)) {
            categoryColourId = R.color.culture;
        } else if (context.getString(R.string.lifestyle_colour_categories).contains(category)) {
            categoryColourId = R.color.lifestyle;
        } else {
            categoryColourId = R.color.unclassified;
            int index = category.indexOf(" ", category.indexOf(" ") + 1);

            if (index != -1) {
                category = category.substring(0, index);
                TextView sectionText = (TextView) listItemView.findViewById(R.id.section_text);
                sectionText.setText(category + " /");
            }
        }

        return ContextCompat.getColor(getContext(), categoryColourId);
    }
}
