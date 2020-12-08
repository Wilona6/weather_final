package com.coolweather.android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.db.County;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    public DrawerLayout drawerLayout;
    private Button navButton;
    private Button Refresh;
    private Button followButton;
    private Button followListButton;

    public SwipeRefreshLayout swipeRefreshLayout;
    private String mWeatherId;

    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView bingPicImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);

        //初始化各控件
        followButton = (Button) findViewById(R.id.follow_button);
        followListButton = (Button) findViewById(R.id.followList_button);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton = (Button) findViewById(R.id.nav_button);

        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img);

        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);

        titleCity = (TextView) findViewById(R.id.title_city);

        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.design_default_color_primary);
        Refresh = (Button) findViewById(R.id.refresh);

        //manageCityBtn.setOnClickListener(new View.OnClickListener() {
        //@Override
        //public void onClick(View v) {
        //  Intent intent = new Intent(WeatherActivity.this, ChooseAreaFragment.class);
        //    startActivityForResult(intent, CHOOSEAREAACTIVITY_RETURN);

        //  }
        //});
        followButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //favorite2.setVisibility(View.VISIBLE);
                //favorite.setVisibility(View.INVISIBLE);
                County county = DataSupport.where("weatherid=?", mWeatherId).find(County.class).get(0);
                county.toggleFollwed();
                county.save();
                //布局中fragment对应的id
                Fragment followListFragment = getFragmentManager().findFragmentById(R.id.follow_area_fragment);
                ((FollowAreaFragment) followListFragment).update();
                if (county.isFollowed()) {
                    Toast.makeText(WeatherActivity.this, "关注成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(WeatherActivity.this, "取消关注", Toast.LENGTH_SHORT).show();
                }
            }
        });
        followListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        //定义缓存对象
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        /*editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
        //缓存计数器
        if (prefs.getString("count", null)==null) {
            editor.putString("count", 0+"");
            editor.apply();
        }
        //读取最近3次的天气数据和城市ID缓存
        String cache[] = new String[3];
        for (int i = 0; i < 3; i++) {
            cache[i] = prefs.getString((i)+"", null);
        }
        String _cache[] = new String[3];
        for (int i = 0; i < 3; i++) {
            _cache[i] = prefs.getString(String.valueOf(i)+"id", null);
        }
        */

        String weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            // 有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;//用于记录城市的天气id
            showWeatherInfo(weather);
        } else {
            // 无缓存时去服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId, true);
        }

        //bing图片
        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }

        //手动下拉刷新
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId,true);
            }
        });
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        //手动更新天气
        Refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestWeather(mWeatherId, true);
            }
        });
    }


    //根据天气id请求城市天气信息
    public void requestWeather(final String weatherId, boolean isFresh) {
        //作者的
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=bc0418b57b2d4918819d3974ac1285d9";
        //自己的
        //String weatherUrl = "https://devapi.qweather.com/v7/weather/7d?locationID=" + weatherId + "&key=ff2268f056a5459d96b25a37d3fc880b";
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weather_history = prefs.getString(weatherId, null);
        if (weather_history != null && isFresh == false) {
            Weather weather = Utility.handleWeatherResponse(weather_history);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
            Toast.makeText(WeatherActivity.this, "缓存导入成功", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
        } else {
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_LONG).show();
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String responseText = response.body().string();
                    final Weather weather = Utility.handleWeatherResponse(responseText);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (weather != null && "ok".equals(weather.status)) {
                                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                                editor.putString("weather", responseText);
                                editor.putString(weather.basic.weatherId, responseText);//缓存
                                editor.apply();
                                mWeatherId = weather.basic.weatherId;
                                showWeatherInfo(weather);
                            } else {
                                Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                            }
                            swipeRefreshLayout.setRefreshing(false);
                            //最近三次数据缓存
                        /*String COUNT=prefs.getString("count",null);
                        editor.putString(Integer.parseInt(COUNT) % 3+"id", mWeatherId);
                        editor.putString(Integer.parseInt(COUNT) % 3 + "", responseText);

                        int i = Integer.parseInt(COUNT) + 1;
                        if (i == 2000) {
                            i = 2;
                        }
                        editor.putString("count", i + "");
                        editor.apply();

                         */
                        }
                    });
                }
            });
            loadBingPic();
        }
    }


        //加载必应每日一图
        private void loadBingPic() {
            String requestBingPic = "http://guolin.tech/api/bing_pic";
            HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String bingPic = response.body().string();
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                    editor.putString("bing_pic", bingPic);
                    editor.apply();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                        }
                    });
                }
            });
        }
        //处理并展示weather实体类中的数据
        private void showWeatherInfo (Weather weather){
            String cityName = weather.basic.cityName;
            String updateTime = weather.basic.update.updateTime.split(" ")[1];
            String degree = weather.now.temperature + "℃";
            String weatherInfo = weather.now.more.info;
            titleCity.setText(cityName);
            titleUpdateTime.setText(updateTime);
            degreeText.setText(degree);
            weatherInfoText.setText(weatherInfo);
            forecastLayout.removeAllViews();
            for (Forecast forecast : weather.forecastList) {
                View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
                TextView dateText = (TextView) view.findViewById(R.id.date_text);
                TextView infoText = (TextView) view.findViewById(R.id.info_text);
                TextView maxText = (TextView) view.findViewById(R.id.max_text);
                TextView minText = (TextView) view.findViewById(R.id.min_text);
                dateText.setText(forecast.date);
                infoText.setText(forecast.more.info);
                maxText.setText(forecast.temperature.max);
                minText.setText(forecast.temperature.min);
                forecastLayout.addView(view);
            }
            if (weather.aqi != null) {
                aqiText.setText(weather.aqi.city.aqi);
                pm25Text.setText(weather.aqi.city.pm25);
            }
            String comfort = "舒适度：" + weather.suggestion.comfort.info;
            String carWash = "洗车指数：" + weather.suggestion.carWash.info;
            String sport = "出行建议：" + weather.suggestion.sport.info;
            comfortText.setText(comfort);
            carWashText.setText(carWash);
            sportText.setText(sport);
            weatherLayout.setVisibility(View.VISIBLE);
        }
    }