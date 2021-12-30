package com.example.practice3_wheatherforecast;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.practice3_wheatherforecast.entity.City;

import java.util.ArrayList;

public class CityListAdapter extends ArrayAdapter<City> {
    private final String TAG="CityListAdapterDebug";

    private int resourceId;

    public CityListAdapter(@NonNull Context context, int textViewResourceId, @NonNull ArrayList<City> objects) {
        super(context, textViewResourceId, objects);
        resourceId=textViewResourceId;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        City item= getItem(position);
        View view= LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
//        ImageView imageView_return=(ImageView) view.findViewById(R.id.imageView_return_cityItem);
        TextView textView_name=(TextView) view.findViewById(R.id.textView_cityName_cityItem);
        TextView textView_cityCode=(TextView) view.findViewById(R.id.textView_cityCode_cityItem);
        Button button_select=(Button) view.findViewById(R.id.button_select_cityItem);
        ImageView imageView_next=(ImageView) view.findViewById(R.id.imageView_next_cityItem);
//        if(item.getType()>1){
//            imageView_return.setImageResource(R.mipmap.pre_level);
//        }
        textView_name.setText(item.getCity_name());
        textView_cityCode.setText(item.getCity_code());
        button_select.setTag(item);
        SelectCityActivity activity=(SelectCityActivity) getContext();
        button_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.backResult(item.getCity_code());
            }
        });
        if(item.getCity_code().equals("")){
            button_select.setVisibility(View.INVISIBLE);
        }
        if(item.getType()<3){
            imageView_next.setImageResource(R.mipmap.next_level);
        }
        return view;
    }
}
