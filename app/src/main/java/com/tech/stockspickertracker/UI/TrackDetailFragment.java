package com.tech.stockspickertracker.UI;


import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.tech.stockspickertracker.Helper.AlarmHelper;
import com.tech.stockspickertracker.Model.TickerDatabase;
import com.tech.stockspickertracker.Model.TickerModel;
import com.tech.stockspickertracker.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class TrackDetailFragment extends Fragment {

    private static final String TAG = "TrackDetailFragment";
    private Spinner spinnerType, spinnerInterval;
    private TextView txtLblLow, txtLblHigh, txtDateSelected;
    private EditText etLow, etHigh, etInterval;
    private Button btnStartDate, btnFinish;
    private ScrollView layout;
    private Context context;
    private TickerDatabase db;
    private Long refreshDate, refreshInterval;
    private String symbol, lastRefresh, stockName;
    private Boolean isExist;
    private Double currentPrice;
    private int id;

    private Calendar startDate = Calendar.getInstance();

    public TrackDetailFragment(Context context) {
        this.context = context;
    }

    public TrackDetailFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        db = TickerDatabase.getInstance(context);

        currentPrice = getArguments().getDouble("currentPrice");
        isExist = getArguments().getBoolean("isExist");
        lastRefresh = getArguments().getString("lastRefresh");
        stockName = getArguments().getString("stockName");
        id = getArguments().getInt("id");

        layout = (ScrollView) inflater.inflate(R.layout.fragment_track_detail, container, false);
        initViews();

        final View dialogView = View.inflate(context, R.layout.date_time_picker, null);
        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();

        if (isExist) {
            TickerModel existingTicker = db.tickerDao().getSingleTicker(id);

            if (existingTicker.getLowThreshold() != null && existingTicker.getHighThershold() != null) {
                spinnerType.setSelection(2);
                etLow.setText(existingTicker.getLowThreshold().toString());
                etHigh.setText(existingTicker.getHighThershold().toString());
            }
            else if (existingTicker.getLowThreshold() != null) {
                spinnerType.setSelection(0);
                etLow.setText(existingTicker.getLowThreshold().toString());
            }
            else if (existingTicker.getHighThershold() != null) {
                spinnerType.setSelection(1);
                etHigh.setText(existingTicker.getHighThershold().toString());
            }

            Long interval = (long) 0;
            switch(existingTicker.getIntervaltype()) {
                case "Minute(s)":
                    spinnerInterval.setSelection(0);
                    interval = Long.valueOf(existingTicker.getRepeatInterval() / 60000);
                    break;
                case "Hour(s)":
                    spinnerInterval.setSelection(1);
                    interval = Long.valueOf(existingTicker.getRepeatInterval() / 3600000);
                    break;
                case "Day(s)":
                    spinnerInterval.setSelection(2);
                    interval = Long.valueOf(existingTicker.getRepeatInterval() / 86400000);
                    break;
                case "Week(s)":
                    spinnerInterval.setSelection(3);
                    interval = Long.valueOf(existingTicker.getRepeatInterval() / 604800000);
                    break;
                default:
                    break;
            }

            etInterval.setText(interval.toString());
        }


        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch(spinnerType.getSelectedItem().toString()) {
                    case "Low":
                        Log.d(TAG, "onCreateView: selected low");
                        txtLblLow.setVisibility(View.VISIBLE);
                        etLow.setVisibility(View.VISIBLE);

                        txtLblHigh.setVisibility(View.GONE);
                        etHigh.setVisibility(View.GONE);
                        break;
                    case "High":
                        Log.d(TAG, "onCreateView: selected High");
                        txtLblHigh.setVisibility(View.VISIBLE);
                        etHigh.setVisibility(View.VISIBLE);

                        txtLblLow.setVisibility(View.GONE);
                        etLow.setVisibility(View.GONE);
                        break;
                    case "Low and High":
                        Log.d(TAG, "onCreateView: selected Low and High");
                        txtLblLow.setVisibility(View.VISIBLE);
                        etLow.setVisibility(View.VISIBLE);
                        txtLblHigh.setVisibility(View.VISIBLE);
                        etHigh.setVisibility(View.VISIBLE);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //Opening calendar and time picker
        btnStartDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: open a calendar dialog
                alertDialog.setView(dialogView);
                alertDialog.show();

                // When user clicks SET button on calendar dialog
                dialogView.findViewById(R.id.btnPickerSet).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        DatePicker datePicker = dialogView.findViewById(R.id.datePicker);
                        TimePicker timePicker = dialogView.findViewById(R.id.timePicker);

                        startDate.set(Calendar.YEAR, datePicker.getYear());
                        startDate.set(Calendar.MONTH, datePicker.getMonth());
                        startDate.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());

                        startDate.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                        startDate.set(Calendar.MINUTE, timePicker.getMinute());

                        String dateSet = new SimpleDateFormat("MMM dd, yyyy h:mm a").format(startDate.getTime());

                        if (Calendar.getInstance().getTimeInMillis() < startDate.getTimeInMillis()) {
                            txtDateSelected.setTextColor(ContextCompat.getColor(context, R.color.dateSet));
                            txtDateSelected.setTypeface(null, Typeface.NORMAL);
                            txtDateSelected.setText(dateSet);
                        }
                        else {
                            txtDateSelected.setTextColor(ContextCompat.getColor(context, R.color.error));
                            txtDateSelected.setTypeface(null, Typeface.BOLD);
                            txtDateSelected.setText("Please select a future Date.");
                        }

                        alertDialog.dismiss();

                    }
                });
            }
        });



        return layout;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: create a method that checks for all fields filled. Will need to create custom shape for red border

                //TODO: Create a new TickerModel object and send it to database. Need to set different repeatInterval depending on type selection
                Log.d(TAG, "onClick: symbol is " + symbol);
                TickerModel ticker;
                if (isExist) {
                    ticker = db.tickerDao().getSingleTicker(id);
                }
                else {
                    ticker = new TickerModel();
                }

                switch(spinnerType.getSelectedItem().toString()) {
                    case "Low":

                        ticker.setLowThreshold(Double.valueOf(etLow.getText().toString()));
                        break;
                    case "High":
                        ticker.setHighThershold(Double.valueOf(etHigh.getText().toString()));
                        break;
                    case "Low and High":
                        ticker.setLowThreshold(Double.valueOf(etLow.getText().toString()));
                        ticker.setHighThershold(Double.valueOf(etHigh.getText().toString()));

                        break;
                    default:
                        break;
                }

                refreshInterval = Long.valueOf(etInterval.getText().toString());


                switch(spinnerInterval.getSelectedItem().toString()) {
                    case "Minute(s)":
                        // 60000 milliseconds in 1 minute
                        refreshInterval *= 60000;
                        break;
                    case "Hour(s)":
                        // 3,600,000 milliseconds in 1 hour
                        refreshInterval *= 3600000;
                        break;
                    case "Day(s)":
                        // 86,400,000 milliseconds in 1 day
                        refreshInterval *= 86400000;
                        break;
                    case "Week(s)":
                        // 604,800,000 milliseconds in 1 week
                        refreshInterval *= 604800000;
                        break;
                    default:
                        break;
                }

                ticker.setSymbol(symbol);
                ticker.setIntervaltype(spinnerInterval.getSelectedItem().toString());
                ticker.setCurrentPrice(currentPrice);
                ticker.setLastRefreshDate(lastRefresh);
                ticker.setStartDate(startDate.getTimeInMillis());
                ticker.setRepeatInterval(refreshInterval);
                ticker.setStockName(stockName);

                // TODO: initialize worker thread
                if (isExist) {
                    db.tickerDao().updateSingleTicker(ticker);

                    AlarmHelper.initAlarm(getActivity(), id, isExist, startDate.getTimeInMillis(), refreshInterval);

                }
                else {
                    db.tickerDao().insertSingleTicker(ticker);
                    TickerModel newTicker = db.tickerDao().getsingleTickerByName(symbol);
                    int newId = newTicker.getId();
                    AlarmHelper.initAlarm(getActivity(), newId, isExist, startDate.getTimeInMillis(), refreshInterval);
                }

                Intent intent = new Intent(context, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initViews() {
        spinnerType = layout.findViewById(R.id.spinnerType);
        spinnerInterval = layout.findViewById(R.id.spinnerInterval);
        spinnerInterval.setSelection(3);
        txtLblLow = layout.findViewById(R.id.txtLblLow);
        txtLblHigh = layout.findViewById(R.id.txtLblHigh);
        txtDateSelected = layout.findViewById(R.id.txtDateSelected);
        etLow = layout.findViewById(R.id.etLow);
        etHigh = layout.findViewById(R.id.etHigh);
        etInterval = layout.findViewById(R.id.etInterval);
        btnStartDate = layout.findViewById(R.id.btnStartDate);
        btnFinish = layout.findViewById(R.id.btnFinish);

        symbol = getArguments().getString("symbol");

    }


}
