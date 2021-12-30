package com.example.practice3_wheatherforecast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.practice3_wheatherforecast.entity.City;

import java.util.ArrayList;
import java.util.Stack;

public class SelectCityActivity extends AppCompatActivity {
    private final String TAG="SelectCityActivityDebug";

    ArrayList<City> cities=new ArrayList<>();

    Stack<ArrayList<City>> citiesStack=new Stack<>();
    Stack<String> titleStack=new Stack<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_city);
        ListView listView=findViewById(R.id.listView_selectCity);
        ActionBar actionBar=getSupportActionBar();
        actionBar.hide();

        TextView textView_title=(TextView) findViewById(R.id.text_selectCityBar_title);
        textView_title.setText("省份");

        this.initCities("0", 0);
        CityListAdapter adapter=new CityListAdapter(SelectCityActivity.this, R.layout.item_city, cities);
        listView.setAdapter(adapter);

        ImageButton back=findViewById(R.id.ImgButton_selectCityBar_back);
        back.setVisibility(View.INVISIBLE);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!citiesStack.isEmpty()){
                    cloneCities(citiesStack.pop());
                    textView_title.setText(titleStack.pop());
                }else{
                    Toast.makeText(SelectCityActivity.this, "已经到顶了！",Toast.LENGTH_SHORT).show();
                }
                if(citiesStack.isEmpty()){
                    back.setVisibility(View.INVISIBLE);
                }
                adapter.notifyDataSetChanged();
                Log.d(TAG, "onClick: stackSize= "+citiesStack.size());
            }
        });
        EditText sText=(EditText) findViewById(R.id.edit_selectCityBar_searchBox);
        ImageButton searchBt=(ImageButton) findViewById(R.id.ImgButton_selectCityBar_search);
        searchBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                citiesStack.push((ArrayList<City>) cities.clone());
                titleStack.push(textView_title.getText().toString());
                textView_title.setText("搜索结果");
                String id=sText.getText().toString();
                Log.d(TAG, "onClick: search id="+id);
                initCities(id, 1);
                adapter.notifyDataSetChanged();
                back.setVisibility(View.VISIBLE);
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                City city=cities.get(position);
                if(city.getType()==3){
                    Toast.makeText(SelectCityActivity.this, "没有下一级了！", Toast.LENGTH_SHORT).show();
                }else{
                    citiesStack.push((ArrayList<City>) cities.clone());
                    titleStack.push(textView_title.getText().toString());
                    initCities(city.getId(), 0);
                    adapter.notifyDataSetChanged();
                    back.setVisibility(View.VISIBLE);
                    textView_title.setText(city.getCity_name());
                }
                Log.d(TAG, "onItemClick: stackSize= "+citiesStack.size());
            }
        });

    }

    @SuppressLint("Range")
    private void initCities(String fpid, int itype){
        SQLiteOpenHelper sqLiteOpenHelper=new SQLiteOpenHelper(SelectCityActivity.this, "cities", null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {

            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            }
        };
        SQLiteDatabase db=sqLiteOpenHelper.getReadableDatabase();
        Cursor cursor;
        if(itype==0){
            cursor=db.query("all_cities", null, "pid=?", new String[]{fpid}, null, null, null);
        }else{
            cursor=db.query("all_cities", null, "city_code=?", new String[]{fpid}, null, null, null);
            if(!cursor.moveToFirst()){
                Toast.makeText(SelectCityActivity.this, "不存在输入ID的城市！请检查，城市ID为9位数字！", Toast.LENGTH_SHORT).show();
            }
        }
        if(cursor.moveToFirst()){
            ArrayList<City> cCities=new ArrayList<>();
            City city;
            do{
                String id=cursor.getString(cursor.getColumnIndex("id"));
                String pid=cursor.getString(cursor.getColumnIndex("pid"));
                String city_code=cursor.getString(cursor.getColumnIndex("city_code"));
                String city_name=cursor.getString(cursor.getColumnIndex("city_name"));
                String area_code=cursor.getString(cursor.getColumnIndex("area_code"));
                int type=cursor.getInt(cursor.getColumnIndex("type"));
                city=new City(id, pid, city_code, city_name, area_code, type);
                cCities.add(city);
//                Log.d(TAG, "initCities: add city: "+city.toString());
            }while (cursor.moveToNext());
            cloneCities(cCities);
        }
    }

    private void cloneCities(ArrayList<City> cCities){
        Log.d(TAG, "cloneCities: size="+cCities.size());
        cities.clear();
        for(City c:cCities){
            cities.add(c);
        }
    }

    protected void backResult(String city_code){
        Intent intent=new Intent();
        intent.putExtra("target_city_code", city_code);
        setResult(RESULT_OK, intent);
        finish();
    }
}