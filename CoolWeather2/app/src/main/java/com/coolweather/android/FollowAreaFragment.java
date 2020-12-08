package com.coolweather.android;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;


import com.coolweather.android.db.County;
import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

public class FollowAreaFragment extends Fragment {
    private ProgressDialog progressDialog;

    private TextView titleText;

    private ListView listView;

    private Button backButton;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    //县列表
    private List<County> countyList;

    //选中的县
    private County selectedCounty;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String weatherId = countyList.get(position).getWeatherId();
                if (getActivity() instanceof MainActivity) {
                    Intent intent = new Intent(getActivity(), WeatherActivity.class);
                    intent.putExtra("weather_id", weatherId);
                    startActivity(intent);
                    getActivity().finish();
                } else if (getActivity() instanceof WeatherActivity) {
                    WeatherActivity activity = (WeatherActivity) getActivity();
                    activity.drawerLayout.closeDrawers();
                    activity.swipeRefreshLayout.setRefreshing(true);
                    activity.requestWeather(weatherId, false);
                }
            }
        });
        queryCounties();
    }

    public void update(){
        queryCounties();
    }

    //查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询。
    private void queryCounties() {
        backButton.setVisibility(View.GONE);
        titleText.setText("已关注");
        countyList = DataSupport.where("followed = ?", "1").find(County.class);
        dataList.clear();
        if (countyList.size() > 0) {

            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
        }
    }

}
