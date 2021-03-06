package com.franktan.popularmovies.util;

import android.content.ContentValues;

import com.franktan.popularmovies.data.genre.GenreColumns;
import com.franktan.popularmovies.data.movie.MovieColumns;
import com.franktan.popularmovies.data.moviegenre.MovieGenreColumns;
import com.franktan.popularmovies.model.Genre;
import com.franktan.popularmovies.model.Movie;
import com.franktan.popularmovies.model.MovieGenre;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * A helper class for conversions
 * Created by tan on 16/08/2015.
 */
public class Parser {

    /**
     * Convert the movieDB movie list json string to a list of Movies
     * @param movieJsonString
     * @return
     * @throws JSONException
     * @throws ParseException
     */
    public static List<Movie> jsonToMovieList (String movieJsonString)
            throws JSONException, ParseException {

        JSONObject forecastJson = new JSONObject(movieJsonString);
        JSONArray movieArray = forecastJson.getJSONArray("results");
        int length = movieArray.length();

        List<Movie> movieList = new ArrayList<>(length);

        for(int i = 0; i < length; i++) {
            JSONObject movieJsonObj = movieArray.getJSONObject(i);
            String backdropPath = movieJsonObj.getString("backdrop_path");
            int movieDbId = movieJsonObj.getInt("id");
            String originalLan = movieJsonObj.getString("original_language");
            String originalTitle = movieJsonObj.getString("original_title");
            String overview = movieJsonObj.getString("overview");
            String releaseDateString = movieJsonObj.getString("release_date");
            String posterPath = movieJsonObj.getString("poster_path");
            String popularityString = movieJsonObj.getString("popularity");
            String title = movieJsonObj.getString("title");
            double voteAverage = movieJsonObj.getDouble("vote_average");
            int voteCount = movieJsonObj.getInt("vote_count");

            // convert date to epoch
            Long releaseDate = epochFromMovieDbDateString(releaseDateString);

            // convert popularity string to double with two decimal places
            double rawPopularity = Double.parseDouble(popularityString);
            DecimalFormat doubleFormat = new DecimalFormat("#.00");
            double popularity = Double.parseDouble(doubleFormat.format(rawPopularity));

            Movie movie = new Movie();
            movie.setBackdropPath(backdropPath);
            movie.setId(movieDbId);
            movie.setOriginalLanguage(originalLan);
            movie.setOriginalTitle(originalTitle);
            movie.setOverview(overview);
            movie.setReleaseDate(releaseDate);
            movie.setPosterPath(posterPath);
            movie.setPopularity(popularity);
            movie.setTitle(title);
            movie.setVoteAverage(voteAverage);
            movie.setVoteCount(voteCount);

            movieList.add(movie);
        }

        return movieList;
    }

    /**
     * Convert movie list json string to a list of movie genres
     * @param movieJsonString
     * @return
     * @throws JSONException
     * @throws ParseException
     */
    public static List<MovieGenre> jsonToMovieGenreList (String movieJsonString)
            throws JSONException, ParseException {

        JSONObject forecastJson = new JSONObject(movieJsonString);
        JSONArray movieArray = forecastJson.getJSONArray("results");
        int length = movieArray.length();

        List<MovieGenre> movieGenres = new ArrayList<>();

        for (int i = 0; i < length; i++) {
            JSONObject movieJsonObj = movieArray.getJSONObject(i);
            JSONArray genreArray = movieJsonObj.getJSONArray("genre_ids");
            for(int j = 0; j < genreArray.length(); j++) {
                MovieGenre movieGenre = new MovieGenre(movieJsonObj.getLong("id"),genreArray.getLong(j));
                movieGenres.add(movieGenre);
            }
        }
        return movieGenres;
    }

    /**
     * Convert a List of MovieGenre objects to an array of ContentValues
     * @param movieGenreList
     * @return
     */
    public static ContentValues[] contentValuesFromMovieGenreList (List<MovieGenre> movieGenreList) {
        ContentValues[] movieGenreContentValues = new ContentValues[movieGenreList.size()];

        for (int i = 0; i < movieGenreList.size(); i++) {
            MovieGenre movieGenre = movieGenreList.get(i);
            ContentValues movieGenreContentValue = new ContentValues();
            movieGenreContentValue.put(MovieGenreColumns.MOVIE_ID, movieGenre.getMovieId());
            movieGenreContentValue.put(MovieGenreColumns.GENRE_ID, movieGenre.getGenreId());
            movieGenreContentValues[i] = movieGenreContentValue;
        }

        return movieGenreContentValues;
    }

    /**
     * Convert a list of Movie objects to an array of content values
     * @param movieList
     * @return
     */
    public static ContentValues[] contentValuesFromMovieList(List<Movie> movieList) {
        ContentValues[] movieContentValues = new ContentValues[movieList.size()];

        for (int i = 0; i < movieList.size(); i++) {
            Movie movie = movieList.get(i);
            ContentValues movieContentValue = new ContentValues();
            movieContentValue.put(MovieColumns.BACKDROP_PATH,    movie.getBackdropPath());
            movieContentValue.put(MovieColumns.MOVIE_MOVIEDB_ID, movie.getId());
            movieContentValue.put(MovieColumns.ORIGINAL_LAN,     movie.getOriginalLanguage());
            movieContentValue.put(MovieColumns.ORIGINAL_TITLE,   movie.getOriginalTitle());
            movieContentValue.put(MovieColumns.OVERVIEW,         movie.getOverview());
            movieContentValue.put(MovieColumns.RELEASE_DATE,     movie.getReleaseDate());
            movieContentValue.put(MovieColumns.POSTER_PATH,      movie.getPosterPath());
            movieContentValue.put(MovieColumns.POPULARITY,       movie.getPopularity());
            movieContentValue.put(MovieColumns.TITLE,            movie.getTitle());
            movieContentValue.put(MovieColumns.VOTE_AVERAGE,     movie.getVoteAverage());
            movieContentValue.put(MovieColumns.VOTE_COUNT,       movie.getVoteCount());
            movieContentValues[i] = movieContentValue;
        }
        return movieContentValues;
    }

    /**
     * Convert epoch milliseconds to a user friendly string
     * @param milliseconds
     * @return
     */
    public static String humanDateStringFromMilliseconds(long milliseconds) {
        Date date = new Date(milliseconds);
        DateFormat format = new SimpleDateFormat("d MMM yyyy");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(date);
    }

    /**
     * Convert epoch milliseconds to the date strings that movieDB uses
     * @param milliseconds
     * @return
     */
    public static String movieDbDateStringFromMilliseconds(long milliseconds) {
        Date date = new Date(milliseconds);
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(date);
    }

    /**
     * Convert movieDB date string to epoch milliseconds
     * @param movieDbDate
     * @return
     * @throws ParseException
     */
    public static long epochFromMovieDbDateString (String movieDbDate) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = dateFormat.parse(movieDbDate);
        return date.getTime();
    }

    /**
     * Convert List of Genre objects to an array of content values
     * @param genres
     * @return
     */
    public static ContentValues[] contentValuesFromGenreList(List<Genre> genres) {
        ContentValues[] genreContentValues = new ContentValues[genres.size()];

        for (int i = 0; i < genres.size(); i++) {
            Genre genre = genres.get(i);
            ContentValues genreContentValue = new ContentValues();
            genreContentValue.put(GenreColumns.GENRE_MOVIEDB_ID,genre.getId());
            genreContentValue.put(GenreColumns.NAME,genre.getName());
            genreContentValues[i] = genreContentValue;
        }

        return genreContentValues;
    }
}
