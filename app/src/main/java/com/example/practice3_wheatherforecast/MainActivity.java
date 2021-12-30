package com.example.practice3_wheatherforecast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.practice3_wheatherforecast.entity.City;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity  implements View.OnClickListener {
    private final String TAG="MainActivityDebug";

    private static final int SUCCESS=1;
    private static final int FAILD=0;

    private static String cCity_code="101010100";

    private Handler handler=new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case SUCCESS:
                    setUpUI();
                    break;
                case FAILD:
                    Toast.makeText(MainActivity.this, "请求错误！", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar=getSupportActionBar();
        actionBar.hide();

        ImageButton imageButton_add=(findViewById(R.id.main_imgBt_add));
        imageButton_add.setOnClickListener(this);
        ImageButton imageButton_reload=(findViewById(R.id.main_imgBt_reload));
        imageButton_reload.setOnClickListener(this);
        ImageButton imageButton_love=(ImageButton) findViewById(R.id.main_imgBt_love);
        imageButton_love.setOnClickListener(this);
        ImageButton imageButton_select=(ImageButton) findViewById(R.id.main_imgBt_select);
        imageButton_select.setOnClickListener(this);

        SharedPreferences pref=getSharedPreferences("appStatus", MODE_PRIVATE);
        if(pref.getBoolean("first", true)){
            Toast.makeText(MainActivity.this, "首次运行，正在初始化城市数据库......", Toast.LENGTH_SHORT).show();
            initData();
            SharedPreferences.Editor editor= pref.edit();
            editor.putBoolean("first", false);
            editor.apply();
        }else{
            SQLiteDatabase db=getLovedCitiesWritableDb();
            cCity_code=getFirst(db);
            request(cCity_code);
        }
        request(cCity_code);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "onActivityResult: result city_code=" + data.getStringExtra("target_city_code"));
                    cCity_code=data.getStringExtra("target_city_code");
                    request(data.getStringExtra("target_city_code"));
                }
                break;
            default:
                Toast.makeText(MainActivity.this, "参数返回错误，请选择一个城市，点击”选择“按钮", Toast.LENGTH_SHORT).show();
        }
    }

    private SQLiteDatabase getLovedCitiesWritableDb(){
        SQLiteOpenHelper sqLiteOpenHelper=new SQLiteOpenHelper(MainActivity.this, "cities", null, 1) {

            @Override
            public void onCreate(SQLiteDatabase db) {
//                db.execSQL(CREATE_ALLCITIES);
                Log.d(TAG, "onCreate: SQLiteDatabase");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            }
        };
        SQLiteDatabase db=sqLiteOpenHelper.getWritableDatabase();
        return db;
    }

    /**
     * 获取收藏列表的城市存储id
     * @return
     */
    @SuppressLint("Range")
    private ArrayList<Integer> getLovedIdList(SQLiteDatabase db){
        Cursor cursor=db.query("loved_cities", null, null, null, null, null, null);
        ArrayList<Integer> lovedName=new ArrayList<>();
        if(cursor.moveToFirst()){
            do{
                lovedName.add(cursor.getInt(cursor.getColumnIndex("id")));
            }while(cursor.moveToNext());
        }
        return lovedName;
    }

    /**
     * 根据收藏城市的存储id查询城市名称
     * @param id
     * @return
     */
    @SuppressLint("Range")
    private String getCityNameById(String id, SQLiteDatabase db){
        Cursor cursor=db.query("loved_cities", null, "id=?", new String[]{id}, null, null, null);
        if(cursor.moveToFirst()){
            return cursor.getString(cursor.getColumnIndex("city_name"));
        }else{
            return "-1";
        }
    }

    /**
     * 根据收藏城市的存储id查询城市id
     * @param id
     * @param db
     * @return
     */
    @SuppressLint("Range")
    private String getCityCodeById(String id, SQLiteDatabase db){
        Cursor cursor=db.query("loved_cities", null, "id=?", new String[]{id}, null, null, null);
        if(cursor.moveToFirst()){
            return cursor.getString(cursor.getColumnIndex("city_code"));
        }else{
            return "-1";
        }
    }

    @SuppressLint("Range")
    private int getIdByCityCode(String city_code, SQLiteDatabase db){
        Cursor cursor=db.query("loved_cities", null, "city_code=?", new String[]{city_code}, null, null, null);
        if(cursor.moveToFirst()){
            return cursor.getInt(cursor.getColumnIndex("id"));
        }else{
            return 0;
        }
    }

    /**
     * 向数据库添加收藏的城市
     * @param city_name
     * @param city_code
     * @param db
     */
    private void addLovedCity(String city_name, String city_code, SQLiteDatabase db){
        ContentValues values=new ContentValues();
        values.put("city_name", city_name);
        values.put("city_code", city_code);
        db.insert("loved_cities", null, values);
    }

    /**
     * 从数据库删除收藏的城市
     * @param city_code
     * @param db
     */
    private void removeLovedCity(String city_code, SQLiteDatabase db){
        db.delete("loved_cities", "city_code=?", new String[]{city_code});
    }

    /**
     * 判断数据库中是否有收藏特定城市的id
     * @param city_code
     * @param db
     * @return
     */
    private boolean isLoved(String city_code, SQLiteDatabase db){
        Cursor cursor=db.query("loved_cities", null, "city_code=?", new String[]{city_code}, null, null, null);
        if(cursor.getCount()==0){
            return false;
        }else {
            return true;
        }
    }

    @SuppressLint("Range")
    private String getFirst(SQLiteDatabase db){
        Cursor cursor=db.query("loved_cities", null, null, null, null, null, null);
        if(cursor.moveToFirst()){
            Log.d(TAG, "getFirst: return first");
            return cursor.getString(cursor.getColumnIndex("city_code"));
        }else{
            Log.d(TAG, "getFirst: return default");
            return "101010100";
        }
    }

    private void showChooseDialog(){
        Log.d(TAG, "showChooseDialog: ");
        AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this, R.style.Theme_Design_BottomSheetDialog);
        builder.setTitle("已收藏的城市");
        int temp_city_code;
        SQLiteDatabase db= getLovedCitiesWritableDb();
        ArrayList<Integer> cityIds=getLovedIdList(db);
        String[] items=new String[cityIds.size()];
        int i=0;
        int checkedNum=-1;
        for(int s:cityIds){
            items[i]=getCityNameById(String.valueOf(s), db);
            if(getCityCodeById(String.valueOf(s), db).equals(cCity_code)){
                checkedNum=i;
            }
            Log.d(TAG, "showChooseDialog: items["+i+"]: "+items[i]);
            i++;
        }
        Log.d(TAG, "showChooseDialog: checkedNum="+checkedNum);
        final int[] toCheckNum = new int[1];
        builder.setSingleChoiceItems(items, checkedNum, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                toCheckNum[0] =which;
                Toast.makeText(MainActivity.this, items[which], Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onClick: toCheckNum[0]="+toCheckNum[0]);
            }
        });
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cCity_code=getCityCodeById(String.valueOf(cityIds.get(toCheckNum[0])), db);
                request(cCity_code);
            }
        });
        AlertDialog alertDialog=builder.create();
        alertDialog.show();
    }

    private void setUpUI(){
        Log.d(TAG, "setUpUI: ");
        ImageButton main_ImgBt_love=(ImageButton) findViewById(R.id.main_imgBt_love);
        SQLiteDatabase ldb=getLovedCitiesWritableDb();
        if(isLoved(cCity_code, ldb)){
            main_ImgBt_love.setImageResource(R.mipmap.loved);
        }else{
            main_ImgBt_love.setImageResource(R.mipmap.to_love);
        }
        JSONObject jsonObject= getJsonObject();
        try {
            Log.d(TAG, "setUpUI: message: " + jsonObject.getString("message"));
        }catch (Exception e){
            e.printStackTrace();
        }
        if(jsonObject!=null){
            Log.d(TAG, "setUpUI: jsonArray length: "+jsonObject.length());
            if(jsonObject.length()<6){
                try{
                    if(jsonObject.getString("status").equals("403")){
                        Toast.makeText(MainActivity.this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
                        cCity_code=getFirst(ldb);
                        Message message=new Message();
                        message.what=SUCCESS;
                        handler.sendMessage(message);
                        return;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            TextView main_textView_title=(TextView) findViewById(R.id.main_textView_title);
            TextView today_updateTime=(TextView) findViewById(R.id.text_todyweather_updateTime);
            TextView today_temp=(TextView) findViewById(R.id.text_todyweather_temp);
            TextView today_humidity=(TextView) findViewById(R.id.text_todyweather_humidity);
            TextView today_pm25=(TextView) findViewById(R.id.text_todyweather_pm25);
            TextView today_quality=(TextView) findViewById(R.id.text_todyweather_quality);
            try{
                main_textView_title.setText(jsonObject.getJSONObject("cityInfo").getString("city"));
                today_updateTime.setText("数据更新时间："+jsonObject.getString("time"));
                JSONObject data=jsonObject.getJSONObject("data");
                today_temp.setText(data.getString("wendu")+"℃");
                today_humidity.setText("湿度："+data.getString("shidu"));
                today_pm25.setText("PM 2.5:"+data.getString("pm25"));
                today_quality.setText("空气质量："+data.getString("quality"));
                JSONArray more= data.getJSONArray("forecast");
                JSONObject more_1=more.getJSONObject(1);
                JSONObject more_2=more.getJSONObject(2);
                JSONObject more_3=more.getJSONObject(3);
                TextView more_1_date=(TextView) findViewById(R.id.text_moreWheather1_date);
                TextView more_1_type=(TextView) findViewById(R.id.text_moreWheather1_type);
                TextView more_1_tempRange=(TextView) findViewById(R.id.text_moreWheather1_tempRange);
                TextView more_2_date=(TextView) findViewById(R.id.text_moreWheather2_date);
                TextView more_2_type=(TextView) findViewById(R.id.text_moreWheather2_type);
                TextView more_2_tempRange=(TextView) findViewById(R.id.text_moreWheather2_tempRange);
                TextView more_3_date=(TextView) findViewById(R.id.text_moreWheather3_date);
                TextView more_3_type=(TextView) findViewById(R.id.text_moreWheather3_type);
                TextView more_3_tempRange=(TextView) findViewById(R.id.text_moreWheather3_tempRange);
                more_1_date.setText(more_1.getString("date")+"日");
                more_1_type.setText(more_1.getString("type"));
                more_1_tempRange.setText(more_1.getString("high").substring(2)+"~"+more_1.getString("low").substring(2));
                more_2_date.setText(more_2.getString("date")+"日");
                more_2_type.setText(more_2.getString("type"));
                more_2_tempRange.setText(more_2.getString("high").substring(2)+"~"+more_2.getString("low").substring(2));
                more_3_date.setText(more_3.getString("date")+"日");
                more_3_type.setText(more_3.getString("type"));
                more_3_tempRange.setText(more_3.getString("high").substring(2)+"~"+more_3.getString("low").substring(2));
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 程序首次运行时初始化城市数据库
     */
    private void initData(){
        new Thread(new Runnable() {
            @SuppressLint("Range")
            @Override
            public void run() {
                InputStream is=getResources().openRawResource(R.raw.city);
                BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(is));
                StringBuilder stringBuilder=new StringBuilder();
                String line=null;
                try {
                    while ((line=bufferedReader.readLine())!=null){
                        stringBuilder.append(line);
                    }
//            Log.d(TAG, "initData:  stringBuilder:  "+stringBuilder.toString());
                }catch (Exception e){
                    e.printStackTrace();
                }
                JSONObject jsonObject=null;
                City city;
                ArrayList<City> citys=new ArrayList<>();
                try{
                    JSONArray jsonArray=new JSONArray(stringBuilder.toString());
                    for(int i=0;i<jsonArray.length();i++){
                        jsonObject=jsonArray.getJSONObject(i);
                        String id=jsonObject.getString("id");
                        String pid=jsonObject.getString("pid");
                        String city_code=jsonObject.getString("city_code");
                        String city_name=jsonObject.getString("city_name");
                        String area_code=jsonObject.getString("area_code");
                        String ctime=jsonObject.getString("ctime");
                        city=new City(id, pid, city_code, city_name, area_code, ctime);
                        citys.add(city);
//                Log.d(TAG, "initData:  add  city: "+city.toString());
                    }
                    Log.d(TAG, "initData: init city list  success!,  length:  "+citys.size());
                }catch (Exception e){
                    e.printStackTrace();
                }
                SQLiteOpenHelper sqLiteOpenHelper=new SQLiteOpenHelper(MainActivity.this, "cities", null, 1) {
                    private String CREATE_ALLCITIES="create table all_cities(" +
                            "id text primary key," +
                            "pid text," +
                            "city_code text," +
                            "city_name text," +
                            "area_code text," +
                            "ctime text,"+
                            "type integer)";
                    private String CREATE_LOVED_CITIES="create table loved_cities(" +
                            "id integer primary key autoincrement,"+
                            "city_name text,"+
                            "city_code text)";

                    @Override
                    public void onCreate(SQLiteDatabase db) {
                        Log.d(TAG, "onCreate: database");
                        db.execSQL(CREATE_ALLCITIES);
                        db.execSQL(CREATE_LOVED_CITIES);
                    }

                    @Override
                    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

                    }
                };
                SQLiteDatabase db=sqLiteOpenHelper.getWritableDatabase();
                ContentValues values=new ContentValues();
                for(City c:citys){
                    values.put("id", c.getId());
                    values.put("pid", c.getPid());
                    values.put("city_code", c.getCity_code());
                    values.put("city_name", c.getCity_name());
                    values.put("area_code", c.getArea_code());
                    values.put("ctime", c.getCtime());
                    db.insert("all_cities", null, values);
//            Log.d(TAG, "initData:  insert  value: "+values.toString());
                    values.clear();
                }
                Log.d(TAG, "initData:  db  insert success!");
                Cursor cursor=db.query("all_cities", null, null, null, null, null, null);
                if(cursor.moveToFirst()){
                    do{
                        @SuppressLint("Range") String id=cursor.getString(cursor.getColumnIndex("id"));
                        @SuppressLint("Range") String pid=cursor.getString(cursor.getColumnIndex("pid"));
                        @SuppressLint("Range") String city_code=cursor.getString(cursor.getColumnIndex("city_code"));
                        if(pid.equals("0") && !city_code.equals("")){
                            db.execSQL("update all_cities set type='"+0+"'where id="+id);
                            Log.d(TAG, "run: "+cursor.getString(cursor.getColumnIndex("city_name"))+ "set type "+0);
                        }else{
                            if(pid.equals("0")){
//                            ContentValues values1=new ContentValues();
//                            values.put("type", 1);
//                            db.update("all_cities", values1, "id=?", new String[]{id});
                                db.execSQL("update all_cities set type='"+1+"' where id="+id);
                                Log.d(TAG, "run: "+cursor.getString(cursor.getColumnIndex("city_name"))+ "set type "+1);
                            }else if(db.query("all_cities", null, "pid=?", new String[]{id}, null, null, null).getCount()==0){
//                            ContentValues values1=new ContentValues();
//                            values1.put("type", 3);
//                            db.update("all_cities", values1, "id=?", new String[]{id});
                                db.execSQL("update all_cities set type='"+3+"' where id="+id);
                                Log.d(TAG, "run: "+cursor.getString(cursor.getColumnIndex("city_name"))+ "set type "+3);
                            }else{
//                            ContentValues values1=new ContentValues();
//                            values1.put("type", 2);
//                            db.update("all_cities", values1, "id=?", new String[]{id});
                                db.execSQL("update all_cities set type='"+2+"' where id="+id);
                                Log.d(TAG, "run: "+cursor.getString(cursor.getColumnIndex("city_name"))+ "set type "+2);
                            }
                        }
                    }while (cursor.moveToNext());
                }
            }
        }).start();

    }

    private JSONObject getJsonObject(){
        StringBuilder sb=new StringBuilder();
        String line=null;
        FileInputStream fis=null;
        BufferedReader br=null;
        try{
            fis=openFileInput(cCity_code+".json");
            br=new BufferedReader(new InputStreamReader(fis));
            while((line=br.readLine())!=null){
                sb.append(line);
                Log.d(TAG, "openJsonArray: append:"+line);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        Log.d(TAG, "openRaw: "+sb);
        Log.d(TAG, "openRaw: length:  "+sb.length());
        JSONObject jsonObject=null;
        try{
            jsonObject=new JSONObject(sb.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
        return jsonObject;
    }

    private void request(String city_code){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection=null;
                BufferedReader reader=null;
                FileOutputStream fos = null;
                BufferedWriter writer=null;
                try{
                    URL url=new URL("http://t.weather.itboy.net/api/weather/city/"+city_code);
                    connection=(HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    InputStream in=connection.getInputStream();
                    reader=new BufferedReader(new InputStreamReader(in));
//                    file=new File(Environment.getDataDirectory(), city_code+".json");
//                    fos=new FileOutputStream(file);
                    fos=openFileOutput(city_code+".json", Context.MODE_PRIVATE);
                    writer=new BufferedWriter(new OutputStreamWriter(fos));

                    StringBuilder respense=new StringBuilder();
                    String line;
                    while((line=reader.readLine())!=null){
                        respense.append(line);
//                        fos.write(line.getBytes());
                        writer.write(line);
                        writer.flush();
                        Log.d(TAG, "run: write byte:  "+line.getBytes().toString());
//                        fos.flush();
                    }
                    Log.d(TAG, "run: respense: "+respense.toString());
                }catch (Exception e){
                    Message message=new Message();
                    message.what=FAILD;
                    handler.sendMessage(message);
                    Log.d(TAG, "run try: "+e.getMessage());
                    e.printStackTrace();
                }finally {
                    if(reader!=null){
                        try{
                            reader.close();
                            fos.close();
                            writer.close();
                        }catch (IOException e){
                            Log.d(TAG, "run: finally: "+e.getMessage());
                        }
                    }
                    if(connection!=null){
                        connection.disconnect();
                    }
                }
                Message message=new Message();
                message.what=SUCCESS;
                handler.sendMessage(message);
            }
        }).start();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.main_imgBt_add:
                Log.d(TAG, "onClick: ");
//                request();
                Intent intent=new Intent(MainActivity.this, SelectCityActivity.class);
                startActivityForResult(intent, 1);
                break;
            case R.id.main_imgBt_reload:
                request(cCity_code);
                Toast.makeText(MainActivity.this, "已重新加载！", Toast.LENGTH_SHORT).show();
                break;
            case  R.id.main_imgBt_select:
                showChooseDialog();
                break;
            case R.id.main_imgBt_love:
                
                TextView title_city=(TextView) findViewById(R.id.main_textView_title);
                String city_name=title_city.getText().toString();
                SQLiteDatabase db= getLovedCitiesWritableDb();
                if(isLoved(cCity_code, db)){
                    removeLovedCity(cCity_code, db);
                    Toast.makeText(MainActivity.this, "已取消收藏”"+title_city.getText().toString()+"“", Toast.LENGTH_LONG).show();
                    cCity_code=getFirst(db);
                    request(cCity_code);
                }else{
                    addLovedCity(city_name, cCity_code, db);
                    setUpUI();
                    Log.d(TAG, "onClick: loved :"+city_name+", "+cCity_code);
                }
                break;
        }
    }
}