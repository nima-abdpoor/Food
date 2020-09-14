package com.chinachino.mvvm.Requests;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.chinachino.mvvm.AppExecutors;
import com.chinachino.mvvm.Filter;
import com.chinachino.mvvm.models.Details;
import com.chinachino.mvvm.models.Example;
import com.chinachino.mvvm.models.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;

import static com.chinachino.mvvm.Utils.Constants.API_KEY;
import static com.chinachino.mvvm.Utils.Constants.DEFAULT_ADULT;
import static com.chinachino.mvvm.Utils.Constants.DEFAULT_LANGUAGE;
import static com.chinachino.mvvm.Utils.Constants.TIME_OUT;

public class MovieAPIClient {

    Filter filter;
    private static MovieAPIClient instance;

    private MutableLiveData<List<Result>> listMutableLiveData;
    private MutableLiveData<Details> movieMutableLiveData;

    private RetrieveMovieDetailsRunnable movieDetailsRunnable;
    private RetrieveMoviesRunnable retrieveMoviesRunnable;


    public LiveData<List<Result>> getMovies() {
        return listMutableLiveData;
    }

    public LiveData<Details> getMovieDetails() {
        return movieMutableLiveData;
    }

    public static MovieAPIClient getInstance() {
        if (instance == null) {
            instance = new MovieAPIClient();
        }
        return instance;
    }

    private MovieAPIClient() {
        listMutableLiveData = new MutableLiveData<>();
        movieMutableLiveData = new MutableLiveData<>();
        filter = new Filter();
    }



    public void SearchMovieAPI(String query, int page) {
        if (retrieveMoviesRunnable != null) {
            retrieveMoviesRunnable = null;
        }
        retrieveMoviesRunnable = new RetrieveMoviesRunnable(query, page);

        final Future handler = AppExecutors.getInstance().getExecutorService().submit(retrieveMoviesRunnable);

        AppExecutors.getInstance().getExecutorService().schedule(() -> {
            handler.cancel(true);
        }, TIME_OUT, TimeUnit.SECONDS);
    }
    public void SearchMovieID(int movieID) {
        if (movieDetailsRunnable != null) {
            movieDetailsRunnable = null;
        }
        movieDetailsRunnable = new RetrieveMovieDetailsRunnable(movieID);

        final Future handler = AppExecutors.getInstance().getExecutorService().submit(movieDetailsRunnable);

        AppExecutors.getInstance().getExecutorService().schedule(() -> {
            handler.cancel(true);
        }, TIME_OUT, TimeUnit.SECONDS);
    }

    private class RetrieveMoviesRunnable implements Runnable {

        private String query;
        private int page;
        private boolean cancelRequest;
        private String TAG = "MovieAPIClient";

        public RetrieveMoviesRunnable(String query, int page) {
            this.query = query;
            this.page = page;
            cancelRequest = false;
        }

        @Override
        public void run() {
            try {
                Response response = getMovies(this.query, this.page).execute();
                if (cancelRequest) {
                    return;
                }
                if (response.code() == 200) {
                    List<Result> results = new ArrayList<>(((Example) response.body()).getResults());
                    listMutableLiveData.postValue(results);
                } else {
                    String error = response.errorBody().toString();
                    Log.d(TAG, "run: " + error);
                    listMutableLiveData.postValue(null);
                }
            } catch (IOException e) {
                Log.d(TAG, "run: " + e.getMessage());
                e.printStackTrace();
                listMutableLiveData.postValue(null);
            }
        }

        public Call<Example> getMovies(String query, int page) {
            return ServiceGenerator.GetMovies().SearchResponse(API_KEY, DEFAULT_LANGUAGE
                    , query, page, DEFAULT_ADULT);
        }

        public void cancelRequest() {
            cancelRequest = true;
        }
    }

    private class RetrieveMovieDetailsRunnable implements Runnable {
        private int movieID;
        private boolean cancelRequest;
        private String TAG = "MovieAPIClient";

        public RetrieveMovieDetailsRunnable(int movieID) {
            this.movieID= movieID;
            cancelRequest = false;
        }
        @Override
        public void run() {
            try {
                Response response = getMovieDetails(movieID).execute();
                if (cancelRequest) {
                    return;
                }
                if (response.code() == 200) {
                   Details details = (Details) response.body();
                    Log.d(TAG, "SubscribeOnObservers "+details.getOverview());
                   movieMutableLiveData.postValue(details);

                } else {
                    String error = response.errorBody().toString();
                    Log.d(TAG, "------run: " + response.code() + "|" + response.toString());
                    movieMutableLiveData.postValue(null);
                }
            } catch (IOException e) {
                Log.d(TAG, "run: exception"+e);
                e.printStackTrace();
                movieMutableLiveData.postValue(null);
            }
        }

        public Call<Details> getMovieDetails(int movieID) {
            return ServiceGenerator.GetMovies().getMovieDetails(movieID,API_KEY,DEFAULT_LANGUAGE);
        }

        public void cancelRequest() {
            cancelRequest = true;
        }
    }
    public void CancelRequest(){
        if (retrieveMoviesRunnable !=null){
            retrieveMoviesRunnable.cancelRequest();
        }
        if (movieDetailsRunnable != null){
            movieDetailsRunnable.cancelRequest();
        }
    }
}
