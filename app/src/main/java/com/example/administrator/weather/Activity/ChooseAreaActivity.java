package com.example.administrator.weather.Activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.weather.CONSTS;
import com.example.administrator.weather.Model.City;
import com.example.administrator.weather.Model.County;
import com.example.administrator.weather.Model.Province;
import com.example.administrator.weather.R;
import com.example.administrator.weather.Util.HttpCallbackListener;
import com.example.administrator.weather.Util.HttpUtil;
import com.example.administrator.weather.Util.Utility;
import com.example.administrator.weather.db.CoolWeatherDB;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/2/20.
 */

public class ChooseAreaActivity extends Activity {

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private CoolWeatherDB coolWeatherDB;
    private List<String> dataList = new ArrayList<String>();

    //省列表
    private List<Province> provinceList;
    //市列表
    private List<City> cityList;
    //县列表
    private List<County> countyList;
    //选中的省份
    private Province seletedProvince;
    //选中的城市
    private City seletedCity;
    //当前选中的级别
    private int currentLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);

        listView = (ListView) findViewById(R.id.list_view);
        titleText = (TextView) findViewById(R.id.tv_title);

        adapter = new ArrayAdapter<String>(ChooseAreaActivity.this
                , android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);

        coolWeatherDB = CoolWeatherDB.getInstance(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    seletedProvince = provinceList.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    seletedCity = cityList.get(position);
                    queryCounties();
                }
            }
        });
        queryProvinces();
    }

    //查询全国所有省，优先从数据库，没有再到服务器上查询
    private void queryProvinces() {
        provinceList = coolWeatherDB.loadProvinces();
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText("中国");
            currentLevel = LEVEL_PROVINCE;
        } else {
            queryFromServer(null, "province");
        }
    }

    //查询全国所有市，优先从数据库，没有再到服务器上查询
    private void queryCities() {
        cityList = coolWeatherDB.loadCities(seletedProvince.getId());
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(seletedProvince.getProvinceName());
            currentLevel = LEVEL_CITY;
        } else {
            queryFromServer(seletedProvince.getProvinceCode(), "city");
        }
    }

    //查询全国所有县，优先从数据库，没有再到服务器上查询
    private void queryCounties() {
        countyList = coolWeatherDB.loadCounties(seletedCity.getId());
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(seletedCity.getCityName());
            currentLevel = LEVEL_COUNTY;
        } else {
            queryFromServer(seletedCity.getCityCode(), "county");
        }
    }

    //根据传入代号和类型从服务器上查询省市县数据
    private void queryFromServer(final String code, final String type) {

        String address;
        if (TextUtils.isEmpty(code)) {
            address = "http://www.weather.com.cn/data/list3/city"+code+".xml";
        } else {
            address = "http://www.weather.com.cn/data/list3/city.xml";
        }
        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvincesResponse(coolWeatherDB
                            , response);
                } else if ("city".equals(type)) {
                    result = Utility.handleCitiesResponse(coolWeatherDB
                            , response, seletedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(coolWeatherDB
                            , response, seletedCity.getId());
                }
                if (result) {
                    //通过runOnUIThread（）方法回到主线程处理逻辑
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                //通过runOnUIThread（）方法回到主线程逻辑
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });

    }

    //显示进度对话框
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    //关闭进度对话框
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    //捕获Back按键，根据当前级别判断，此时返回市、省列表或直接退出
    @Override
    public void onBackPressed() {
        if (currentLevel == LEVEL_COUNTY) {
            queryCities();
        } else if (currentLevel == LEVEL_CITY) {
            queryProvinces();
        } else {
            finish();
        }
    }
}
