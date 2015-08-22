package com.franktan.popularmovies.sync;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.franktan.popularmovies.R;
import com.franktan.popularmovies.util.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by tan on 16/08/2015.
 */
public class MovieDbRESTAPIService {
    public static String getApiKey(Context context) {
        return context.getString(R.string.moviedb_api_key);
    }

    //TODO: use Enum for sort by parameter
    public String getMovieInfoFromAPI(Context context, String sortBy, long releaseDateFrom, int page) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        String formattedDateFrom = Parser.movieDbDateStringFromMiliseconds(releaseDateFrom);
        Log.i("popularmovies",formattedDateFrom);

        final String MOVIEDB_BASE_URL =
                "http://api.themoviedb.org/3/discover/movie?";
        final String API_KEY = "api_key";
        final String SORT_BY = "sort_by";
        final String PAGE = "page";
        final String RELEASE_BEFORE = "primary_release_date.gte";

        try {
            Uri builtUri = Uri.parse(MOVIEDB_BASE_URL).buildUpon()
                    .appendQueryParameter(API_KEY, MovieDbRESTAPIService.getApiKey(context))
                    .appendQueryParameter(SORT_BY, sortBy)
                    .appendQueryParameter(PAGE, String.valueOf(page))
                    .appendQueryParameter(RELEASE_BEFORE, formattedDateFrom)
                    .build();

            URL url = new URL(builtUri.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            return buffer.toString();

        } catch (IOException e) {
            return null;
        }

    }

    public static MovieDbRESTAPIService getDbSyncService() {
        return new MovieDbRESTAPIService();
    }
}