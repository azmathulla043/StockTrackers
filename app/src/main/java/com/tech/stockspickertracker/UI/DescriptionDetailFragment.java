package com.tech.stockspickertracker.UI;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.tech.stockspickertracker.R;

public class DescriptionDetailFragment extends Fragment {

    TextView txtAddress, txtExchange, txtSector, txtIndustry, txtDescription;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ScrollView layout = (ScrollView) inflater.inflate(R.layout.fragment_description_details, container, false);

        initViews(layout);

        txtAddress.setText(getArguments().getString("address"));
        txtExchange.setText(getArguments().getString("exchange"));
        txtSector.setText(getArguments().getString("sector"));
        txtIndustry.setText(getArguments().getString("industry"));
        txtDescription.setText(getArguments().getString("description"));

        // Inflate the layout for this fragment
        return layout;
    }

    private void initViews(ScrollView layout) {
        txtAddress = layout.findViewById(R.id.txtAddress);
        txtExchange = layout.findViewById(R.id.txtExchange);
        txtSector = layout.findViewById(R.id.txtSector);
        txtIndustry = layout.findViewById(R.id.txtIndustry);
        txtDescription = layout.findViewById(R.id.txtDesc);
    }
}
