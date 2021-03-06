package com.example.moyutest;

import android.app.Activity;
import android.app.LocalActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.moyutest.adapter.MainFragmentPagerAdapter;
import com.example.moyutest.adapter.MyViewPagerAdapter;
import com.example.moyutest.util.Api;
import com.example.moyutest.util.BaseActivity;
import com.example.moyutest.util.BottomNavigationViewHelper;
import com.example.moyutest.util.HandleResponse;
import com.example.moyutest.util.RetrofitProvider;
import com.example.moyutest.util.SharedPreferencesUtil;
import com.google.gson.JsonObject;

import org.java_websocket.WebSocket;
import org.litepal.tablemanager.Connector;


import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import pl.com.salsoft.sqlitestudioremote.SQLiteStudioService;
import rx.functions.Action1;
import ua.naiksoftware.stomp.LifecycleEvent;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.client.StompClient;
import ua.naiksoftware.stomp.client.StompMessage;

import static android.content.ContentValues.TAG;

public class MainActivity extends BaseActivity {

    private ViewPager mViewPager;
    private MainFragment mainfragment = new MainFragment();
    private MenuItem menuItem;
    private LocalActivityManager manager;
    private MyViewPagerAdapter viewPageAdapter;
    private BottomNavigationView bottomNavigationView;
    public static Activity mMainActivity = null;
    private long exitfirstTime = 0;
    private StompClient mStompClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //连接SQLiteStudio
        mMainActivity = this;
        SQLiteStudioService.instance().start(this);
        Connector.getDatabase();
        createStompClient();
        registerStompTopic();
//        checklogin();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //取消动画
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);
        manager = new LocalActivityManager(this, true);
        manager.dispatchCreate(savedInstanceState);
        mViewPager = (ViewPager) findViewById(R.id.mainviewPager);
        initViews();
    }

    private void initViews() {
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.main:
                                mViewPager.setCurrentItem(0);
                                break;
                            case R.id.message:
                                mViewPager.setCurrentItem(1);
                                break;
                            case R.id.fl_post:
                                mViewPager.setCurrentItem(2);
                                break;
                            case R.id.discovery:
                                mViewPager.setCurrentItem(3);
                                break;
                            case R.id.my:
                                mViewPager.setCurrentItem(4);
                                break;
                        }
                        return false;
                    }
                });
        InitPager();
    }


    ///user/lincoln/queue/notifications
    private void registerStompTopic() {
        String url = "/user/" + SharedPreferencesUtil.getIdFromDB() + "/message";
        mStompClient.topic(url).subscribe(new Action1<StompMessage>() {
            @Override
            public void call(StompMessage stompMessage) {
                Log.d("Phone", stompMessage.getPayload());
                String responseText = stompMessage.getPayload();
                String newcomment = HandleResponse.handleNewComment(responseText);
                NotificationCompat
                        .Builder builder = new NotificationCompat.Builder(MainActivity.this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("有新评论")
                        .setTicker("有新评论")
                        .setAutoCancel(true)
                        .setWhen(System.currentTimeMillis())
                        .setContentText(newcomment);
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(1, builder.build());
            }
        });
    }


    private void createStompClient() {
        mStompClient = Stomp.over(WebSocket.class, "ws://120.79.42.49:8080/hello/websocket");
        mStompClient.connect();
        mStompClient.lifecycle().subscribe(new Action1<LifecycleEvent>() {
            @Override
            public void call(LifecycleEvent lifecycleEvent) {
                switch (lifecycleEvent.getType()) {
                    case OPENED:
                        Log.d("Phone", "Stomp connection opened");
                        break;
                    case ERROR:
                        Log.e("Phone", "Stomp Error", lifecycleEvent.getException());
                        break;
                    case CLOSED:
                        Log.d("Phone", "Stomp connection closed");
                        break;
                }
            }
        });
    }

    private void InitPager() {
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (menuItem != null) {
                    menuItem.setChecked(false);
                } else {
                    bottomNavigationView.getMenu().getItem(0).setChecked(false);
                }
                menuItem = bottomNavigationView.getMenu().getItem(position);
                menuItem.setChecked(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
//禁止ViewPager滑动
        mViewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        AddActivitiesToViewPager();
        mViewPager.setCurrentItem(0);
    }

    private void AddActivitiesToViewPager() {
        List<View> mViews = new ArrayList<View>();
        Intent intent = new Intent();

        intent.setClass(this, MoyuActivity.class);
        intent.putExtra("id", 1);
        mViews.add(getView("MoyuActivity", intent));

        intent.setClass(this, MessageActivity.class);
        intent.putExtra("id", 2);
        mViews.add(getView("Message", intent));

        intent.setClass(this, PostActivity.class);
        intent.putExtra("id", 3);
        mViews.add(getView("Edit", intent));

        intent.setClass(this, SearchActivity.class);
        intent.putExtra("id", 4);
        mViews.add(getView("Search", intent));

        intent.setClass(this, MyActivity.class);
        intent.putExtra("id", 5);
        mViews.add(getView("MyActivity", intent));

        viewPageAdapter = new MyViewPagerAdapter(mViews);
        mViewPager.setAdapter(viewPageAdapter);

    }

    private View getView(String id, Intent intent) {
        return manager.startActivity(id, intent).getDecorView();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (System.currentTimeMillis() - exitfirstTime > 2000) {
                Toast.makeText(MainActivity.this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                exitfirstTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String id_token = SharedPreferencesUtil.getIdTokenFromXml(MainActivity.this);
        Api api = RetrofitProvider.create().create(Api.class);
        api.profile(id_token).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<JsonObject>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(JsonObject jsonObject) {
                        String responstText = jsonObject.toString();
                        HandleResponse.handleProfile(responstText);
                        MyActivity.change();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }
}