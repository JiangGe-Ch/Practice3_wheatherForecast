package com.example.practice3_wheatherforecast.entity;

public class City {
    private String id;
    private String pid;
    private String city_code;
    private String city_name;
    private String area_code;
    private String ctime;
    private int type;

    public City(){

    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public City(String id, String pid, String city_code, String city_name, String area_code, String ctime) {
        this.id = id;
        this.pid = pid;
        this.city_code = city_code;
        this.city_name = city_name;
        this.area_code = area_code;
        this.ctime = ctime;
    }

    public City(String id, String pid, String city_code, String city_name, String area_code, int type) {
        this.id = id;
        this.pid = pid;
        this.city_code = city_code;
        this.city_name = city_name;
        this.area_code = area_code;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getCity_code() {
        return city_code;
    }

    public void setCity_code(String city_code) {
        this.city_code = city_code;
    }

    public String getCity_name() {
        return city_name;
    }

    public void setCity_name(String city_name) {
        this.city_name = city_name;
    }

    public String getArea_code() {
        return area_code;
    }

    public void setArea_code(String area_code) {
        this.area_code = area_code;
    }

    public String getCtime() {
        return ctime;
    }

    public void setCtime(String ctime) {
        this.ctime = ctime;
    }

    @Override
    public String toString() {
        return "City{" +
                "id='" + id + '\'' +
                ", pid='" + pid + '\'' +
                ", city_code='" + city_code + '\'' +
                ", city_name='" + city_name + '\'' +
                ", area_code='" + area_code + '\'' +
                ", ctime='" + ctime + '\'' +
                ", type=" + type +
                '}';
    }
}
