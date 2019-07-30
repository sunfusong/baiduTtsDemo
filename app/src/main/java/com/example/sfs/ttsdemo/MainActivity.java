package com.example.sfs.ttsdemo;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.TtsMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * @author sfs
 * @create 19-7-29
 * @Describe 实现简单的中英TTS
 */


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private String apiId = "16890536";
    private String apiKey = "eFYLvEQGp5zvQdbM1976g3VM";
    private String secretKey = "ve79Oown5TprQA4MbEGROAugFD4761Ps";

    private EditText et_input;
    private Button bt_start;
    private Button bt_pause;
    private Button bt_resume;

    // assets目录下bd_etts_common_speech_m15_mand_eng_high_am-mix_v3.0.0_20170505.dat为离线男声模型；
    // assets目录下bd_etts_common_speech_f7_mand_eng_high_am-mix_v3.0.0_20170512.dat为离线女声模型
    private SpeechSynthesizer mSpeechSynthesizer;
    private static final String ENGLISH_SPEECH_FEMALE_MODEL_NAME = "bd_etts_common_speech_f7_mand_eng_high_am-mix_v3.0.0_20170512.dat";
    private static final String ENGLISH_TEXT_MODEL_NAME = "bd_etts_text.dat";
    private static final String SAMPLE_DIR_NAME = "baiduTTS";
    private String mSampleDirPath;

    private AlertDialog mPermissionDialog;
    private String mPackName = "com.example.sfs.ttsdemo";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermission();
    }


    private void isFirstRun() {
        SharedPreferences sp = getSharedPreferences("ttsFlag", MODE_PRIVATE);
        boolean firstFlag = sp.getBoolean("firstFlag", true);
        SharedPreferences.Editor edit = sp.edit();
        Log.i("msg", "isFirstRun  firstFlag: " + firstFlag);
        if (firstFlag) {//第一次启动app,判断是否联网
            final int netFlag = NetUtil.getNetWorkState(MainActivity.this);
            Log.i("msg", "isFirstRun  netFlag: " + netFlag);
            if (netFlag == 0 || netFlag == 2) {//移动或者无线网络
                edit.putBoolean("firstFlag", false);
                edit.apply();

                initialEnv();
                initTts();
                initView();
            } else {//没有网络
                Toast.makeText(this, "没有网络或者检查网络是否可用", Toast.LENGTH_LONG).show();
                MainActivity.this.finish();
            }
        } else {//非第一次启动app
            initialEnv();
            initTts();
            initView();
        }

    }

    private void initPermission() {
        String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };
        ArrayList<String> mPermissionList = new ArrayList<String>();
        mPermissionList.clear();
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) !=
                    PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);//添加还未授予的权限到mPermissionList中
            }
        }
        //申请权限
        if (mPermissionList.size() > 0) {
            ActivityCompat.requestPermissions(this, permissions, 100);
        } else {
            //权限都已通过,进行初始化
            isFirstRun();

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermissionDismiss = false;//权限是否都已通过的标记
        if (requestCode == 100) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == -1) {
                    hasPermissionDismiss = true;
                    break;
                }
            }
        }
        if (hasPermissionDismiss) {//有未被允许的权限
            showPermissionDialog();
        } else {
            //初始化
            isFirstRun();

        }
    }


    /**
     * 手动设置权限
     */
    private void showPermissionDialog() {
        if (mPermissionDialog == null) {
            mPermissionDialog = new AlertDialog.Builder(this)
                    .setMessage("已禁用权限，请手动授予")
                    .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mPermissionDialog.cancel();
                            Uri packageURI = Uri.parse("package:" + mPackName);
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                            startActivity(intent);//打开应用设置
                            MainActivity.this.finish();
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mPermissionDialog.cancel();
                            MainActivity.this.finish();
                        }
                    })
                    .create();
        }
        mPermissionDialog.show();
    }


    /**
     * 初始化Tts
     */
    private void initTts() {
        //获取实例
        mSpeechSynthesizer = SpeechSynthesizer.getInstance();
        mSpeechSynthesizer.setContext(this);
        mSpeechSynthesizer.setAppId(apiId);
        mSpeechSynthesizer.setApiKey(apiKey, secretKey);


        //文本模型文件路径 (离线引擎使用)
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, mSampleDirPath + "/"
                + ENGLISH_TEXT_MODEL_NAME);
        //声学模型文件路径 (离线引擎使用)
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, mSampleDirPath + "/"
                + ENGLISH_SPEECH_FEMALE_MODEL_NAME);
        Log.i("msg", "initTts param: " + mSampleDirPath + "/" + ENGLISH_TEXT_MODEL_NAME);
        Log.i("msg", "initTts param: " + mSampleDirPath + "/" + ENGLISH_SPEECH_FEMALE_MODEL_NAME);

        //模式:离在线混合
        mSpeechSynthesizer.auth(TtsMode.MIX);
        //对语音合成进行监听
        mSpeechSynthesizer.setSpeechSynthesizerListener(new listener());

        //设置参数
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");//标准女声
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "5");//音量 范围["0" - "15"], 不支持小数。 "0" 最轻，"15" 最响。
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5");//语速 范围["0" - "15"], 不支持小数。 "0" 最慢，"15" 最快
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5");//语调 范围["0" - "15"], 不支持小数。 "0" 最慢，"15" 最快
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_HIGH_SPEED_NETWORK);//WIFI,4G,3G 使用在线合成，其他使用离线合成 6s超时
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, mSampleDirPath + "/" + ENGLISH_TEXT_MODEL_NAME);//文本模型文件路径
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, mSampleDirPath + "/" + ENGLISH_SPEECH_FEMALE_MODEL_NAME);//声学模型文件路径
        mSpeechSynthesizer.initTts(TtsMode.MIX);
    }


    /**
     * 初始化离线资源
     */
    private void initialEnv() {
        if (mSampleDirPath == null) {
            String sdcardPath = Environment.getExternalStorageDirectory().toString();
            mSampleDirPath = sdcardPath + "/" + SAMPLE_DIR_NAME;
            Log.i("msg", "initialEnv mSampleDirPath: " + mSampleDirPath);// /storage/emulated/0/baiduTTS
        }
        File file = new File(mSampleDirPath);
        if (!file.exists()) {
            file.mkdirs();
        }

        copyFromAssetsToSdcard(false, ENGLISH_SPEECH_FEMALE_MODEL_NAME, mSampleDirPath + "/"
                + ENGLISH_SPEECH_FEMALE_MODEL_NAME);
        copyFromAssetsToSdcard(false, ENGLISH_TEXT_MODEL_NAME, mSampleDirPath + "/"
                + ENGLISH_TEXT_MODEL_NAME);


    }

    /**
     * 将离线资源文件拷贝到SD卡中（授权文件为临时授权文件，请注册正式授权）
     *
     * @param isCover 是否覆盖已存在的目标文件
     * @param source  dat文件
     * @param dest    保存文件路径
     */
    public void copyFromAssetsToSdcard(boolean isCover, String source, String dest) {
        File file = new File(dest);
        if (isCover || (!isCover && !file.exists())) {
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                is = getResources().getAssets().open(source);
                String path = dest;
                fos = new FileOutputStream(path);
                byte[] buffer = new byte[1024];
                int size = 0;
                while ((size = is.read(buffer, 0, 1024)) >= 0) {
                    fos.write(buffer, 0, size);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 初始化控件
     */
    private void initView() {
        et_input = findViewById(R.id.et_input);
        bt_start = findViewById(R.id.bt_start);
        bt_pause = findViewById(R.id.bt_pause);
        bt_resume = findViewById(R.id.bt_resume);
        bt_start.setOnClickListener(MainActivity.this);
        bt_pause.setOnClickListener(MainActivity.this);
        bt_resume.setOnClickListener(MainActivity.this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_start:
                Log.i("msg", "onClick text: " + et_input.getText().toString());
                mSpeechSynthesizer.speak(et_input.getText().toString());
                break;
            case R.id.bt_pause:
                mSpeechSynthesizer.pause();
                break;
            case R.id.bt_resume:
                mSpeechSynthesizer.resume();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        //释放tts资源
        if (mSpeechSynthesizer != null) {
            mSpeechSynthesizer.stop();
            mSpeechSynthesizer.release();
            mSpeechSynthesizer = null;
        }
        super.onDestroy();
    }
}
