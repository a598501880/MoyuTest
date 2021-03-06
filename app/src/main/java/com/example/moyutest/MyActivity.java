package com.example.moyutest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.moyutest.db.MoyuUser;
import com.example.moyutest.util.Api;
import com.example.moyutest.util.BaseActivity;
import com.example.moyutest.util.GlideImageLoader;
import com.example.moyutest.util.HandleResponse;
import com.example.moyutest.util.RetrofitProvider;
import com.example.moyutest.util.SharedPreferencesUtil;
import com.google.gson.JsonObject;
import com.yancy.gallerypick.config.GalleryConfig;
import com.yancy.gallerypick.config.GalleryPick;
import com.yancy.gallerypick.inter.IHandlerCallBack;

import org.litepal.crud.DataSupport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;


public class MyActivity extends BaseActivity implements View.OnClickListener {
    private static TextView follow;
    private static TextView fans;
    private static TextView nickname;
    private static ImageView myphoto;
    private static TextView introduce;
    private RelativeLayout settingll, personalll;
    private LinearLayout followsll, followerll;
    private GalleryConfig galleryConfig;
    protected static final int CHOOSE_PICTURE = 0;
    protected static final int TAKE_PICTURE = 1;
    private static Context mContext;
    private static Activity mActivity;
    private final int PERMISSIONS_REQUEST_READ_CONTACTS = 8;
    private List<String> path = new ArrayList<>();
    private String TAG = "Phone";
    private IHandlerCallBack iHandlerCallBack;
    private String obj;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        mContext = this;
        mActivity = this;
        findById();
        setText();
        settingll.setOnClickListener(this);
        myphoto.setOnClickListener(this);
        personalll.setOnClickListener(this);
        followsll.setOnClickListener(this);
        followerll.setOnClickListener(this);
        initGallery();
        init();

    }

    private static void setText() {
        MoyuUser user = DataSupport.findFirst(MoyuUser.class);
        nickname.setText(user.getNickname());
        follow.setText(String.valueOf(user.getFollow()));
        fans.setText(String.valueOf(user.getFollower()));
        introduce.setText(user.getIntroduction());
        RequestOptions requestOptions = new RequestOptions()
                .placeholder(R.drawable.account);
        Glide.with(mContext)
                .load("http://120.79.42.49:8080/images/avatar/" + user.getAvatar())
                .apply(requestOptions)
                .into(myphoto);
    }

    private void findById() {
        follow = (TextView) findViewById(R.id.my_follow);
        fans = (TextView) findViewById(R.id.my_fans);
        nickname = (TextView) findViewById(R.id.my_nickname);
        settingll = (RelativeLayout) findViewById(R.id.setting);
        personalll = (RelativeLayout) findViewById(R.id.personal_data);
        myphoto = (ImageView) findViewById(R.id.my_photo);
        followerll = (LinearLayout) findViewById(R.id.btn_followers);
        followsll = (LinearLayout) findViewById(R.id.btn_follows);
        introduce = (TextView) findViewById(R.id.profile_introduce);
    }

    private void init() {
        galleryConfig = new GalleryConfig.Builder()
                .imageLoader(new GlideImageLoader())    // ImageLoader 加载框架（必填）
                .iHandlerCallBack(iHandlerCallBack)     // 监听接口（必填）
                .provider("com.example.moyutest.fileprovider")   // provider(必填)
                .pathList(path)                         // 记录已选的图片
                .multiSelect(false)                      // 是否多选   默认：false
                .crop(true)                             // 快捷开启裁剪功能，仅当单选 或直接开启相机时有效
                .crop(true, 1, 1, 500, 500)
                // 配置裁剪功能的参数，   默认裁剪比例 1:1
                .isShowCamera(true)                     // 是否现实相机按钮  默认：false
                .filePath("/Gallery/Pictures")          // 图片存放路径
                .build();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.setting:
                Intent setIntent = new Intent(MyActivity.this, SettingActivity.class);
                startActivity(setIntent);
                break;
            case R.id.my_photo:
                ItemsDialogFragment itemsDialogFragment = new ItemsDialogFragment();
                String[] items = {"从相册选择", "拍照"};
                itemsDialogFragment.show("更换头像", items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case CHOOSE_PICTURE: // 选择本地照片
                                galleryConfig.getBuilder().isOpenCamera(false).build();
                                initPermissions();
                                break;
                            case TAKE_PICTURE: // 拍照
                                GalleryPick.getInstance().setGalleryConfig(galleryConfig).openCamera(mActivity);
                                break;
                        }
                    }
                }, getFragmentManager());
                break;
            case R.id.personal_data:
                Intent personalIntent = new Intent(MyActivity.this, PersonalDataActivity.class);
                startActivityForResult(personalIntent, 5);
                break;
            case R.id.btn_follows:
                Intent followsintent = new Intent(MyActivity.this, FollowActivity.class);
                startActivity(followsintent);
                break;
            case R.id.btn_followers:
                Intent followersintent = new Intent(MyActivity.this, FollowerActivity.class);
                startActivity(followersintent);
                break;
            default:
                break;
        }
    }

    // 授权管理
    private void initPermissions() {
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "需要授权 ");
            if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.i(TAG, "拒绝过了");
                Toast.makeText(mContext, "请在 设置-应用管理 中开启此应用的储存授权。", Toast.LENGTH_SHORT).show();
            } else {
                Log.i(TAG, "进行授权");
                ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        } else {
            Log.i(TAG, "不需要授权 ");
            GalleryPick.getInstance().setGalleryConfig(galleryConfig).open(MyActivity.this);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "同意授权");
                GalleryPick.getInstance().setGalleryConfig(galleryConfig).open(MyActivity.this);
            } else {
                Log.i(TAG, "拒绝授权");
            }
        }
    }

    private void initGallery() {
        iHandlerCallBack = new IHandlerCallBack() {
            @Override
            public void onStart() {
                Log.i(TAG, "onStart: 开启");
            }

            @Override
            public void onSuccess(List<String> photoList) {
                Log.i(TAG, "onSuccess: 返回数据");
                path.clear();
                for (String s : photoList) {
                    Log.i(TAG, "图片路径" + s);
                    path.add(s);
                }
                String id_token = SharedPreferencesUtil.getIdTokenFromXml(MyActivity.this);
                File file = new File(path.get(0));
                Log.d(TAG, "图片名" + file.getName());
                RequestBody requestFile = RequestBody.create(MediaType.parse("application/octet-stream"), file);
                MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
                final Api api = RetrofitProvider.create().create(Api.class);
                api.upload(body, id_token)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<JsonObject>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(JsonObject JsonObject) {
                                String responeseBody = JsonObject.toString();
                                obj = HandleResponse.handlerupload(responeseBody);
                                Log.d("Phone", "上传头像" + responeseBody);
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.d(TAG, "上传失败" + e);
                            }

                            @Override
                            public void onComplete() {
                                Glide.with(MyActivity.this)
                                        .load(path.get(0))
                                        .into(myphoto);
                                MoyuUser user = new MoyuUser();
                                user.setAvatar(obj);
                                String id = SharedPreferencesUtil.getIdFromDB();
                                user.updateAll("userid = ?", id);
                            }
                        });
            }

            @Override
            public void onCancel() {
                Log.i(TAG, "onCancel: 取消");
            }

            @Override
            public void onFinish() {
                Log.i(TAG, "onFinish: 结束");
            }

            @Override
            public void onError() {
                Log.i(TAG, "onError: 出错");
            }
        };

    }

    public static void change() {
        setText();
    }
}
