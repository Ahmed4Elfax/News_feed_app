package com.example.android.newsfeedapp;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public final class QueryUtils {

    public static final String LOG_TAG = MainActivity.class.getName();
    /**
     * Create a private constructor because no one should ever create a {@link QueryUtils} object.
     * This class is only meant to hold static variables and methods, which can be accessed
     * directly from the class name QueryUtils (and an object instance of QueryUtils is not needed).
     */
    private QueryUtils() {
    }

    /**
     * Return a list of {@link News} objects that has been built up from
     * parsing the given JSON response.
     */
    private static List<News> extractFeatureFromJson(String newsJSON) {

        // If the JSON string is empty or null, then return early.
        if (TextUtils.isEmpty(newsJSON)) {
            return null;
        }

        // Create an empty ArrayList that we can start adding earthquakes to
        List<News> news = new ArrayList<>();

        // Try to parse the JSON response string. If there's a problem with the way the JSON
        // is formatted, a JSONException exception object will be thrown.
        // Catch the exception so the app doesn't crash, and print the error message to the logs.
        try {

            // Create a JSONObject from the JSON response string
            JSONObject baseJsonResponse = new JSONObject(newsJSON);

            // Extract the JSONArray associated with the key called "features",
            // which represents a list of features (or earthquakes).
            JSONArray newsArray = baseJsonResponse.optJSONObject("response").getJSONArray("results");

            // For each earthquake in the earthquakeArray, create an {@link Earthquake} object
            for (int i = 0; i < newsArray.length(); i++) {

                // Get a single earthquake at position i within the list of earthquakes
                JSONObject currentNewsItem = newsArray.getJSONObject(i);

                String title = currentNewsItem.getString("webTitle");
                int index = title.indexOf("|");
                if (index != -1) {
                    title = title.substring(0, index - 1);
                }
                // extracting section name

                String section = currentNewsItem.getString("sectionName");
                // sorting out the date and time
                String date = currentNewsItem.getString("webPublicationDate");
                index = date.indexOf("Z");
                date = date.substring(0, index);
                date = timeConversion(date);

                // getting image thumbnail

                Bitmap image = null;
                String imageUrl = null;
                if (currentNewsItem.optJSONObject("fields") != null) {
                    imageUrl = currentNewsItem.optJSONObject("fields").optString("thumbnail", "empty");
                    if (!imageUrl.equals("empty")) {
                        image = srcImage(imageUrl);
                    } else {
                        image = null;
                    }
                }
                // trying to get contributor image if news item has no thumbnail

                if (image == null) {
                    if (currentNewsItem.optJSONArray("tags") != null && currentNewsItem.optJSONArray( "tags").length() == 1
                            && !currentNewsItem.optJSONArray("tags").getJSONObject(0).optString("bylineImageUrl", "none").equals("none")) {
                        image = srcImage(currentNewsItem.optJSONArray("tags").getJSONObject(0).optString("bylineImageUrl", "none"));
                    }
                }

                // getting link to article
                String link = currentNewsItem.getString("webUrl");

                // extracting the name of the author if there is one
                String author;
                try {
                    author = currentNewsItem.getJSONArray("tags").getJSONObject(0).getString("webTitle");
                } catch (JSONException e) {
                    author = "not known";
                }
                // creating new NewsObject object
                News item = new News(title, author, section, date, image, link);
                news.add(item);
            }


        } catch (JSONException e) {
            // If an error is thrown when executing any of the above statements in the "try" block,
            // catch the exception here, so the app doesn't crash. Print a log message
            // with the message from the exception.
            Log.e("QueryUtils", "Problem parsing the earthquake JSON results", e);
        }

        // Return the list of earthquakes
        return news;
    }

    /**
     * Query the USGS dataset and return a list of {@link News} objects.
     */
    public static List<News> fetchEarthquakeData(String requestUrl) {



        // Create URL object
        URL url = createUrl(requestUrl);

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem making the HTTP request.", e);
        }

        // Extract relevant fields from the JSON response and create a list of {@link Earthquake}s
        List<News> news = extractFeatureFromJson(jsonResponse);

        // Return the list of {@link Earthquake}s
        return news;
    }
    /**
     * Returns new URL object from the given string URL.
     */
    private static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Problem building the URL ", e);
        }
        return url;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     */
    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem retrieving the earthquake JSON results.", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                // Closing the input stream could throw an IOException, which is why
                // the makeHttpRequest(URL url) method signature specifies than an IOException
                // could be thrown.
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Convert the {@link InputStream} into a String which contains the
     * whole JSON response from the server.
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }


    private static Bitmap srcImage(String url) {
        if (url == null) {
            return null;
        }
        URL srcUrl = createUrl(url);
        try {
            assert srcUrl != null;
            return BitmapFactory.decodeStream(srcUrl.openConnection().getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // method to generate date string to add to url string to get today's news from Guardian
    public static String getDate() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        Date today = Calendar.getInstance().getTime();
        return dateFormatter.format(today);
    }

    // method to convert Guardian's time stamp to format suitable for UI
    private static String timeConversion(String jsonTime) {
        long milliSeconds;
        @SuppressLint("SimpleDateFormat") SimpleDateFormat guardianTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        try {
            Date date = guardianTime.parse(jsonTime);
            milliSeconds = date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateConverter = new SimpleDateFormat("MMM d yyyy");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat timeConverter = new SimpleDateFormat("h:mm a");
        String articleDate = dateConverter.format(milliSeconds);
        String articleTime = timeConverter.format(milliSeconds);
        return articleDate + "\n" + articleTime;
    }

}
