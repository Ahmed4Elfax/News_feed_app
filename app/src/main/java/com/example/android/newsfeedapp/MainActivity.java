package com.example.android.newsfeedapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LoaderCallbacks<List<News>> {

    private static final int NEWS_LOADER_ID = 1;
    private NewsAdapter mAdapter;
    private TextView mEmptyStateTextView;
    SharedPreferences sharedPrefs;
    ProgressBar loadingIndicator;
    SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        loadingIndicator = findViewById(R.id.progressBar);

        ListView newsListView = (ListView) findViewById(R.id.list);

        // Create a new adapter that takes an empty list of earthquakes as input
        mAdapter = new NewsAdapter(this, new ArrayList<News>());

        // Set the adapter on the {@link ListView}
        // so the list can be populated in the user interface
        newsListView.setAdapter(mAdapter);

        mEmptyStateTextView = (TextView) findViewById(R.id.noDataLoaded);
        newsListView.setEmptyView(mEmptyStateTextView);


        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get details on the currently active default data network
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            // Get a reference to the LoaderManager, in order to interact with loaders.
            LoaderManager loaderManager = getLoaderManager();

            // Initialize the loader. Pass in the int ID constant defined above and pass in null for
            // the bundle. Pass in this activity for the LoaderCallbacks parameter (which is valid
            // because this activity implements the LoaderCallbacks interface).
            loaderManager.initLoader(NEWS_LOADER_ID, null, this);
        } else {
            // Otherwise, display error
            // First, hide loading indicator so error message will be visible
            loadingIndicator.setVisibility(View.GONE);

            // Update empty state with no connection error message
            mEmptyStateTextView.setText(R.string.no_internet);
        }

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        getSupportLoaderManager().destroyLoader(NEWS_LOADER_ID);
                        ConnectivityManager connMgr2 = (ConnectivityManager)
                                getSystemService(Context.CONNECTIVITY_SERVICE);

                        // Get details on the currently active default data network
                        NetworkInfo networkInfo2 = connMgr2.getActiveNetworkInfo();
                        if (networkInfo2 != null && networkInfo2.isConnected()) {
                            LoaderManager loaderManager2 = getLoaderManager();
                            loaderManager2.initLoader(NEWS_LOADER_ID, null, MainActivity.this);
                        } else {
                            loadingIndicator.setVisibility(View.GONE);
                            mEmptyStateTextView.setText(R.string.no_internet);
                            swipeRefresh.setRefreshing(false);
                        }
                    }
                }
        );

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_icon, menu);
        return true;
    }

    // overriding onOptionsItemSelected to take user to settings activity
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<List<News>> onCreateLoader(int id, Bundle args) {
        String newUrl = getURL(sharedPrefs);
        return new NewsLoader(this, newUrl);
    }

    @Override
    public void onLoadFinished(Loader<List<News>> loader, List<News> data) {
        swipeRefresh.setRefreshing(false);
        if (data != null) {
            updateUi(data);
            loadingIndicator.setVisibility(View.GONE);
        } else {
            loadingIndicator.setVisibility(View.GONE);
            mEmptyStateTextView.setText(R.string.no_server);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<News>> loader) {
        mAdapter.clear();
    }

    public void updateUi(List<News> news) {

        ListView list = findViewById(R.id.list);
        mAdapter = new NewsAdapter(this, news);
        list.setAdapter(mAdapter);

        AdapterView.OnItemClickListener itemListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                News currentItem = mAdapter.getItem(position);
                assert currentItem != null;
                String link = currentItem.getLink();

                Uri webPage = Uri.parse(link);
                Intent intent = new Intent(Intent.ACTION_VIEW, webPage);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        };
        list.setOnItemClickListener(itemListener);
        if(news.size() == 0){
            searchFailed();

        }
    }

    private void searchFailed() {
        loadingIndicator.setVisibility(View.GONE);
        mEmptyStateTextView.setText(getString(R.string.your_search_for) + "\n'" + sharedPrefs.getString(getString(R.string.search_key), "") + "'\n" + getString(R.string.returned_no_results));
    }

    public String getURL(SharedPreferences sharedPrefs) {

        String section = sharedPrefs.getString(getString(R.string.sections_key), getString(R.string.all));
        String keyWords = "";

        if (!sharedPrefs.getString(getString(R.string.search_key), getString(R.string.none)).equals(getString(R.string.none))) {
            keyWords = sharedPrefs.getString(getString(R.string.search_key), getString(R.string.none));
        }
        if (!keyWords.equals("")) {
            keyWords = searchStringFormatter(keyWords);
        }
        /// Uri builder
        Uri baseUri = Uri.parse("https://content.guardianapis.com/search?");

        Uri.Builder builder = baseUri.buildUpon();

        if (!section.equals(getString(R.string.all))) {
            builder.appendQueryParameter(getString(R.string.section), section);

        }
        builder.appendQueryParameter(getString(R.string.production_office), getString(R.string.uk));

        if (section.equals(getString(R.string.news)) && keyWords.equals("")) {
            builder.appendQueryParameter(getString(R.string.order_by), getString(R.string.relevance));
        }

        builder.appendQueryParameter(getString(R.string.show_fields), getString(R.string.thumbnail));
        builder.appendQueryParameter(getString(R.string.page_size), sharedPrefs.getString(getString(R.string.requests_key), getString(R.string.default_request_number)));

        builder.appendQueryParameter(getString(R.string.show_tags), getString(R.string.contributor));

        if (!keyWords.equals("")) {
            builder.appendQueryParameter(getString(R.string.q), keyWords);
        }
        builder.appendQueryParameter(getString(R.string.api_key), "test");
        Log.i("ONCREATE:" ,builder.toString());
        return builder.toString();/*Uri.parse("https://content.guardianapis.com/search?api-key=test&format=json&q=vegan&page=1&page-size=20&order-by=newest&show-tags=contributor").toString();*/

    }

    private String searchStringFormatter(String keyWords) {
        while (keyWords.contains("  ")) {
            keyWords = keyWords.replace("  ", " ");
        }
        while (keyWords.startsWith(" ")) {
            keyWords = keyWords.substring(1);
        }
        while (keyWords.endsWith(" ")) {
            keyWords = keyWords.substring(0, keyWords.length() - 1);

        }
        if (keyWords.equals(" ")) {
            keyWords = "";
        }

        if (keyWords.contains(" ")) {
            keyWords = keyWords.replace(" ", " " + getString(R.string.AND) + " ");
        }

        return keyWords;
    }
}