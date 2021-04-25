package com.tech.stockspickertracker.UI;

import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.widget.SearchView;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;

import android.widget.TextView;

import com.tech.stockspickertracker.Adapter.StockRecAdapter;
import com.tech.stockspickertracker.Helper.AlarmHelper;
import com.tech.stockspickertracker.Model.TickerDatabase;
import com.tech.stockspickertracker.Model.TickerModel;
import com.tech.stockspickertracker.Network.APIService;
import com.tech.stockspickertracker.R;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.core.*;

public class MainActivity extends AppCompatActivity implements StockRecAdapter.DeleteInterface {
    private static final String TAG = "MainActivity";
    Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    SearchView mSearchView;
    private TextView mTextView;

    private Handler handler = new Handler();
    long DELAY = 500;
    long last_edit = 0;
    String searchText;

    private CompositeDisposable disposable = new CompositeDisposable();
    private StockRecAdapter adapter = new StockRecAdapter(this, MainActivity.this);
    private APIService apiService = new APIService();
    private TickerDatabase mTickerDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adapter.setTickers();
        initViews();

        getSupportActionBar().hide();
        mTickerDatabase = TickerDatabase.getInstance(this);

        if(mTickerDatabase.tickerDao().getAllTickers().size()!=0){
            mTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_tootlbar,menu);
        mSearchView = (SearchView) menu.findItem(R.id.searchTicker).getActionView();
        Log.d(TAG, "onCreateOptionsMenu: Im searching in ");
        searchCode();
        Log.d(TAG, "onCreateOptionsMenu: Im searching out ");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.clear();
    }

    @Override
    public void onBackPressed() {
        if (!mSearchView.isIconified()) {
            mSearchView.setIconified(true);
        } else {
            super.onBackPressed();
        }
    }

    private Runnable input_check = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() > (last_edit + DELAY)) {
                apiSearch();
            }
        }
    };
    private void searchCode() {

        Log.d(TAG, "searchCode: Im searchCode in ");
        mSearchView.setQueryHint("Search a Symbol: TSLA");
        //Setting search text threshold
        int autoCompleteID = getResources().getIdentifier("search_src_text", "id", getPackageName());
        AutoCompleteTextView autoCompleteTextView = mSearchView.findViewById(autoCompleteID);
        autoCompleteTextView.setThreshold(1);

        SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(
                MainActivity.this,
                R.layout.search_list_item,
                null, new String[] {SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2},
                new int[] {R.id.search_symbol, R.id.search_name},
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        mSearchView.setSuggestionsAdapter(cursorAdapter);

        Log.d(TAG, "searchCode: search view"+mSearchView);
        mSearchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit: searchText"+searchText);
                searchText = query;
                apiSearch();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                if (newText.length() > 0) {
                    searchText = newText;
                    last_edit = System.currentTimeMillis();

                    handler.postDelayed(input_check,DELAY);
                }
                else {
                    mSearchView.getSuggestionsAdapter().changeCursor(null);
                }

                return true;
            }
        });

        // Listening to user selecting from suggestions list
        mSearchView.setOnSuggestionListener(new androidx.appcompat.widget.SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                Cursor cursor = (Cursor) mSearchView.getSuggestionsAdapter().getItem(position);
                String symbol = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));

                Intent intent = new Intent(MainActivity.this, StockDetailActivity.class);
                intent.putExtra(StockDetailActivity.SYMBOL_KEY, symbol);

                TickerModel tickerCheck = mTickerDatabase.tickerDao().getsingleTickerByName(symbol);

                if (tickerCheck != null) {
                    Log.d(TAG, "onSuggestionSelect: symbol is " + symbol);
                    Log.d(TAG, "onSuggestionSelect: tickerCheck symbol is " + tickerCheck.getSymbol());


                    if (tickerCheck.getSymbol().equals(symbol)) {
                        intent.putExtra(StockDetailActivity.TICKER_ID, tickerCheck.getId());
                        intent.putExtra(StockDetailActivity.IS_EXIST_KEY, true);
                    }
                }
                startActivity(intent);
                cursor.close();
                mSearchView.setIconified(true);
                return true;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                return onSuggestionSelect(position);
            }
        });
    }

    private void apiSearch() {
        Log.d(TAG, "apiSearch: +in");
        apiService.getSymbols(searchText)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Cursor>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onNext(@NonNull Cursor cursor) {
                        Log.d(TAG, "onNext: suggestion cursor");
                        mSearchView.getSuggestionsAdapter().changeCursor(cursor);
                        mSearchView.getSuggestionsAdapter().notifyDataSetChanged();
                        Log.d(TAG, "onNext: suggestion cursor out ");

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void initViews() {
        mToolbar = findViewById(R.id.toolBar);
        mRecyclerView= findViewById(R.id.mainRecView);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        mRecyclerView.setAdapter(adapter);
        mTextView = findViewById(R.id.txtEmptyMessage);

    }


    @Override
    public void showEmptyText() {
        if (mTickerDatabase.tickerDao().getAllTickers().size() == 0) {
            mTextView.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void cancelAlarm(int id) {
        AlarmHelper.cancelAlarm(getBaseContext(), id);
    }



}