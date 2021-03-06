package com.franktan.popularmovies.integration;

import android.content.res.Resources;
import android.database.Cursor;
import android.test.InstrumentationTestCase;

import com.franktan.popularmovies.data.movie.MovieColumns;
import com.franktan.popularmovies.data.movie.MovieCursor;
import com.franktan.popularmovies.data.movie.MovieSelection;
import com.franktan.popularmovies.model.SortCriterion;
import com.franktan.popularmovies.rest.MovieListAPIService;
import com.franktan.popularmovies.sync.MovieSyncAdapter;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.*;

/**
 * Created by tan on 16/08/2015.
 */
public class RetrieveAndSaveDataTest extends InstrumentationTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        System.setProperty(
                "dexmaker.dexcache",
                getInstrumentation().getTargetContext().getCacheDir().getPath());
    }

    // Test Retrieving data from Real Movie DB API
    public void testMovieDBAPIMovieListReturn() {
        MovieListAPIService movieListAPIService = MovieListAPIService.getDbSyncService();
        //retrieve movies released after 1/1/2015 order by popularity
        String jsonReturn = movieListAPIService.getMovieInfoFromAPI(getInstrumentation().getTargetContext(), SortCriterion.POPULARITY, 1420070400000L,1435708800000L,1);
        assertNotNull("API should return something", jsonReturn);
        assertFalse("Returned Json should not be an empty string", jsonReturn.equals(""));
    }

    // Test retrieving fake json from a mock service, parsing it and save to DB
    public void testMovieListParsingAndSaving() throws IOException {
        deleteAllRecordsFromProvider();

        MovieSyncAdapter.retrieveAndSaveGenres(getInstrumentation().getTargetContext());

        MovieListAPIService mockService = mock(MovieListAPIService.class);
        when(mockService.getMovieInfoFromAPI(getInstrumentation().getTargetContext(), SortCriterion.POPULARITY, 1420070400000L, 1435708800000L,1)).thenReturn(getTestingMovieJson());
        assertEquals("retrieveAndSaveMovieData should return 20", 20, MovieSyncAdapter.retrieveAndSaveMovieData(getInstrumentation().getTargetContext(), mockService, SortCriterion.POPULARITY, 1420070400000L, 1435708800000L, 1));

        MovieSelection where = new MovieSelection();
        MovieCursor movieCursor = where.query(getInstrumentation().getTargetContext().getContentResolver());
        assertEquals("Should be 20 movie record in database", 20, movieCursor.getCount());
        movieCursor.moveToFirst();
        assertEquals("First record should match the first movie in our list",
                102899, movieCursor.getMovieMoviedbId());

        movieCursor.moveToLast();
        assertEquals("Last record should match the last movie in our list",
                260346, movieCursor.getMovieMoviedbId());
        movieCursor.close();
    }

    public void deleteAllRecordsFromProvider() {
        getInstrumentation().getContext().getContentResolver().delete(
                MovieColumns.CONTENT_URI,
                null,
                null
        );

        Cursor cursor = getInstrumentation().getContext().getContentResolver().query(
                MovieColumns.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals("Should be 0 record in table after delete", 0, cursor.getCount());
        cursor.close();

    }

    public String getTestingMovieJson() throws IOException {
        Resources res = getInstrumentation().getContext().getResources();
        InputStream in = res.getAssets().open("mock_movies.json");
        byte[] b  = new byte[(int) in.available()];
        int len = b.length;
        int total = 0;

        while (total < len) {
            int result = in.read(b, total, len - total);
            if (result == -1) {
                break;
            }
            total += result;
        }
        return new String(b);
    }

}
