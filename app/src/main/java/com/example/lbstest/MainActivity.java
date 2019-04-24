package com.example.lbstest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolygonOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    public LocationClient mLocationClient;

    private TextView positionText;

    private MapView mapView;

    private BaiduMap baiduMap;

    private boolean isFirstLocate = true;

    private static boolean isLogin = false;//登录成功标志位

    private char traceSwitch = 0;//追踪轨迹按键

    private boolean isTrace = false;//追踪轨迹标志位

    List<LatLng> points ;//追踪轨迹-坐标队列

    private Button loginButton;
    private List<String> permissionList;
    private Button inforButton;
    private Button mapTypeButton;
    private Button traceButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
    }

    //画线
    private void setLine(List<LatLng> points) {

        OverlayOptions mOverlayOptions = new PolylineOptions()
                .width(10)
                .color(0xAAFF0000)
                .points(points);
        Overlay overlay = baiduMap.addOverlay(mOverlayOptions);
    }



    private void initView() {
        points=new ArrayList<>();
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.bmapView);
        baiduMap = mapView.getMap();
        baiduMap.setMyLocationEnabled(true);
        positionText = (TextView) findViewById(R.id.position_text_view);

        permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()){
            String [] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        }else {
            requrestLocation();
        }

        //登录
        loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLogin == false){
                    Intent intent = new Intent(MainActivity.this,LoginActivity.class);
                    startActivity(intent);
                }
            }
        });

        //经纬度显示切换
        inforButton = (Button) findViewById(R.id.information_button);
        inforButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (positionText.getVisibility() == View.INVISIBLE){
                    positionText.setVisibility(View.VISIBLE);
                }else {
                    positionText.setVisibility(View.INVISIBLE);
                }
            }
        });

        //地图类型切换
        mapTypeButton = (Button) findViewById(R.id.mapType_button_);
        mapTypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (baiduMap.getMapType() == BaiduMap.MAP_TYPE_NORMAL){
                    baiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                }else {
                    baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                }
            }
        });

        //轨迹追踪按键
        traceButton = (Button) findViewById(R.id.trace_button);
        traceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (traceSwitch){
                    case 0:
                        //开始追踪
                        isTrace = true;
                        traceSwitch = 1;
                        traceButton.setText("结束追踪");
                        break;
                    case 1:
                        //结束追踪
                        isTrace = false;
                        traceSwitch = 2;
                        traceButton.setText("清除轨迹");
                        setLine(points);
                        break;
                    case 2:
                        //清楚轨迹
                        traceSwitch = 0;
                        traceButton.setText("开始追踪");
                        break;

                }
            }
        });
    }

    public  static void setIsLogin(){
        if (isLogin){
            isLogin = false;
        }else {
            isLogin = true;
        }
    }

    public static boolean getIsLogin(){
        return isLogin;
    }

    private void navigateTo(BDLocation location){
        if (isFirstLocate) {
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
            baiduMap.animateMapStatus(update);
            update = MapStatusUpdateFactory.zoomTo(16f);
            baiduMap.animateMapStatus(update);
            isFirstLocate = false;
        }
        MyLocationData locData = new MyLocationData.Builder()
                .accuracy(location.getRadius())
                // 此处设置开发者获取到的方向信息，顺时针0-360
                .direction(location.getDirection()).latitude(location.getLatitude())
                .longitude(location.getLongitude()).build();
        baiduMap.setMyLocationData(locData);

    }

    private void requrestLocation(){
        initLocation();
        mLocationClient.start();
    }

    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//定位模式
        option.setOpenGps(true);//GPS开关
        option.setScanSpan(5000);//扫描间隔
        option.setIsNeedAddress(true);//地理信息开关
        mLocationClient.setLocOption(option);//设置
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        if (isLogin){
            loginButton.setText("已登录");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length > 0){
                    for (int result : grantResults){
                        if (result != PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this, "必须同意所有权限才能使用本程序", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requrestLocation();
                }else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    public class MyLocationListener extends BDAbstractLocationListener {

        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            Log.d("监听器打印","监听开始");


             if (isTrace) {
                 double latitude = bdLocation.getLatitude();
                 double longitude = bdLocation.getLongitude();
                 LatLng latLng = new LatLng(latitude, longitude);
                 points.add(latLng);
                 Log.d("监听器打印", "精度" + latitude);
                 Log.d("监听器打印", "纬度" + longitude);
             }


            StringBuilder currentPosition = new StringBuilder();
                currentPosition.append("纬度: ").append(bdLocation.getLatitude()).append("\n");
                currentPosition.append("经线: ").append(bdLocation.getLongitude()).append("\n");
                currentPosition.append("国家: ").append(bdLocation.getCountry()).append("\n");
                currentPosition.append("省: ").append(bdLocation.getProvince()).append("\n");
                currentPosition.append("市: ").append(bdLocation.getCity()).append("\n");
                currentPosition.append("区: ").append(bdLocation.getDistrict()).append("\n");
                currentPosition.append("街道: ").append(bdLocation.getStreet()).append("\n");
                currentPosition.append("定位方式: ");
                if (bdLocation.getLocType() == BDLocation.TypeGpsLocation) {
                    currentPosition.append("GPS");
                } else if (bdLocation.getLocType() == BDLocation.TypeNetWorkLocation) {
                    currentPosition.append("网络");
                }
                positionText.setText(currentPosition);

                if (bdLocation.getLocType() == BDLocation.TypeGpsLocation || bdLocation.getLocType() == BDLocation.TypeNetWorkLocation) {
                    navigateTo(bdLocation);
                } else {
                    Toast.makeText(getApplicationContext(), "网络通信出错", Toast.LENGTH_SHORT).show();
                }



        }
    }

}
