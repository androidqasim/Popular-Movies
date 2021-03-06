package com.franktan.popularmovies.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.franktan.popularmovies.R;
import com.franktan.popularmovies.data.base.BaseContentProvider;
import com.franktan.popularmovies.data.favorite.FavoriteColumns;
import com.franktan.popularmovies.data.genre.GenreColumns;
import com.franktan.popularmovies.data.movie.MovieColumns;
import com.franktan.popularmovies.data.movie.MovieCursor;
import com.franktan.popularmovies.model.MovieGroup;
import com.franktan.popularmovies.ui.activities.MovieDetailActivity;
import com.franktan.popularmovies.ui.adapters.MovieGridAdapter;
import com.franktan.popularmovies.util.Constants;

/**
 * A fragment showing a group of movies in a grid layout
 */
public class MoviesGridFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String[] MOVIE_COLUMNS = {
            MovieColumns.TABLE_NAME + "." + MovieColumns._ID,
            MovieColumns.TABLE_NAME + "." + MovieColumns.MOVIE_MOVIEDB_ID,
            MovieColumns.TABLE_NAME + "." + MovieColumns.POSTER_PATH,
            MovieColumns.TABLE_NAME + "." + MovieColumns.TITLE,
            FavoriteColumns.TABLE_NAME + "." + FavoriteColumns._ID,
            FavoriteColumns.TABLE_NAME + "." + FavoriteColumns.CREATED,
            "group_concat(" + GenreColumns.TABLE_NAME + "." + GenreColumns.NAME + ", ', ') as name"
    };

    private static final int MOVIE_LOADER_ID = 0;

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String SELECTION = "selection";
    private static final String GROUP = "group";

    private int mSelection;
    private MovieGroup mMovieGroup;

    private OnFragmentInteractionListener mListener;
    private MovieGridAdapter mMovieGridAdapter;
    private GridView mGridView;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param selection the active selection.
     * @return A new instance of fragment MoviesGridFragment.
     */
    public static MoviesGridFragment newInstance(int selection, MovieGroup movieGroup) {
        MoviesGridFragment fragment = new MoviesGridFragment();
        Bundle args = new Bundle();
        args.putInt(SELECTION, selection);
        args.putString(GROUP, movieGroup.toString());
        fragment.setArguments(args);
        return fragment;
    }

    public MoviesGridFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int savedPosition;
        String movieGroupString;

        if(savedInstanceState != null && savedInstanceState.containsKey(SELECTION)) {
            savedPosition = savedInstanceState.getInt(SELECTION);
            movieGroupString = savedInstanceState.getString(GROUP);

        } else {
            Bundle arguments = getArguments();
            if(arguments != null) {
                savedPosition = arguments.getInt(SELECTION);
                movieGroupString = arguments.getString(GROUP);
            } else {
                savedPosition = -1;
                movieGroupString = null;
            }
        }

        if(savedPosition >= 0)
            mSelection = savedPosition;

        if(movieGroupString != null && movieGroupString.length() > 0)
            mMovieGroup = MovieGroup.valueOf(movieGroupString);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(MOVIE_LOADER_ID, null, this);

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movies_grid, container, false);

        mGridView = (GridView) view.findViewById(R.id.moviesgridview);
        mMovieGridAdapter = new MovieGridAdapter(getActivity(), null, 0);

        mGridView.setAdapter(mMovieGridAdapter);

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                mSelection = position;
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                if (cursor == null) {
                    Log.i(Constants.APP_NAME, "item clicked, but cursor is null");
                    return;
                }
                MovieCursor movieCursor = new MovieCursor(cursor);
                long movieDBId = movieCursor.getMovieMoviedbId();
                Log.i(Constants.APP_NAME, "movieDBId onclick: " + movieDBId);
                if (mListener.isInTwoPaneMode()) {
                    mListener.onMovieItemSelected(movieDBId);
                } else {
                    Activity activity = getActivity();
                    Intent intent = new Intent(activity, MovieDetailActivity.class);
                    intent.putExtra(Constants.MOVIEDB_ID, movieDBId);

                    if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ActivityOptionsCompat options = ActivityOptionsCompat.
                                makeSceneTransitionAnimation(activity, v, activity.getString(R.string.poster_transition_element));
                        activity.startActivity(intent, options.toBundle());
                    } else {
                        activity.startActivity(intent);
                    }
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mGridView.setNestedScrollingEnabled(true);
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new ClassCastException("activity must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * Save the selection and movie group
     * @param outState
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        // save the current active selection
        if(mSelection != GridView.INVALID_POSITION) {
            outState.putInt(SELECTION, mSelection);
        }
        outState.putString(GROUP, mMovieGroup.toString());
        super.onSaveInstanceState(outState);
    }

    /**
     * Run different queries depending on the group of movies user is after
     * @param id
     * @param args
     * @return
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String groupBy = MovieColumns.TABLE_NAME + "." + MovieColumns._ID;
        Uri movieUri = BaseContentProvider.groupBy(Uri.withAppendedPath(MovieColumns.CONTENT_URI, "with_favorite"),groupBy);
        String sortOrder;

        if(mMovieGroup == MovieGroup.POPULAR) {
            sortOrder = MovieColumns.POPULARITY + " DESC";

            return new CursorLoader(
                    getActivity(),
                    movieUri,
                    MOVIE_COLUMNS,
                    null,
                    null,
                    sortOrder
            );
        } else if(mMovieGroup == MovieGroup.TOP_RATED) {
            sortOrder = MovieColumns.VOTE_AVERAGE + " DESC";

            return new CursorLoader(
                    getActivity(),
                    movieUri,
                    MOVIE_COLUMNS,
                    null,
                    null,
                    sortOrder
            );
        } else {
            sortOrder = FavoriteColumns.TABLE_NAME + "." + FavoriteColumns.CREATED + " DESC";

            return new CursorLoader(
                    getActivity(),
                    movieUri,
                    MOVIE_COLUMNS,
                    FavoriteColumns.TABLE_NAME + "." + FavoriteColumns._ID + " is not null",
                    null,
                    sortOrder
            );
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mMovieGridAdapter.swapCursor(cursor);

        //keep scrolling position, add code here
        if(mListener.isActiveFragment(this)) {
            goToSelectedMovie(mSelection);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mMovieGridAdapter.swapCursor(null);
    }

    /**
     * Scroll to the selected movie. In two pane mode, also show the selected movie in the detail view
     * @param selection
     */
    private void goToSelectedMovie (int selection) {
        mSelection = selection;
        if(mSelection != GridView.INVALID_POSITION) {
            mGridView.setSelection(mSelection);
            mGridView.setItemChecked(mSelection, true);
            if(mListener.isInTwoPaneMode()) {
                int movieDBId = (int) mMovieGridAdapter.getItemId(mSelection);
                mListener.onMovieItemSelected(movieDBId);
            }

        }
    }

    public boolean checkFragmentActive() {
        if(mListener.isActiveFragment(this)) {
            goToSelectedMovie(mSelection);
            return true;
        }
        return false;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        boolean isInTwoPaneMode();

        void onMovieItemSelected(long movieDBId);

        boolean isActiveFragment(MoviesGridFragment fragment);
    }

    public MovieGroup getMovieGroup() {
        return mMovieGroup;
    }
}
