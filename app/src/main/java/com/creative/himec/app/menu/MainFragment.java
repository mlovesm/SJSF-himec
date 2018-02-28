package com.creative.himec.app.menu;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.creative.himec.app.R;
import com.creative.himec.app.fragment.FragMenuActivity;
import com.creative.himec.app.government.PeoplesCardDialogActivity;
import com.creative.himec.app.retrofit.Datas;
import com.creative.himec.app.retrofit.RetrofitService;
import com.creative.himec.app.util.FileDownProgressTask;
import com.creative.himec.app.util.SettingPreference;
import com.creative.himec.app.util.UtilClass;
import com.creative.himec.app.work.WorkStandardMainFragment;
import com.google.firebase.iid.FirebaseInstanceId;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainFragment extends Fragment {

//    adb shell dumpsys activity activities | findstr "Run"
    private static final String TAG = "MainFragment";
    private RetrofitService service;
    public static String ipAddress= "http://119.202.60.145:8585";
//    public static String ipAddress= "http://192.168.0.22:9191";
    public static String contextPath= "/sjsf_himec";
    private ProgressDialog pDlalog = null;

    static final String TASK_FRAGMENT_TAG = "MainTask";
    private static final int DIALOG_REQUEST_CODE = 1234;
    private String fileDir= Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator  + "Download" + File.separator;
    private String fileNm;

    //FCM 관련
    private String token=null;
    private String phone_num=null;
    public static boolean onAppCheck= false;
    public static String pendingPath= "";
    public static String pendingPathKey= "";

    private PermissionListener permissionlistener;

    private SettingPreference pref;
    public static String loginSabun;
    public static String loginName;
    public static String userSosok;
    public static String jPos;
    public static String part1_cd;
    public static String part2_cd;
    public static String latestAppVer;

//    @Override public void onAttach(Context context) {
//        //This method avoid to call super.onAttach(context) if I'm not using api 23 or more
//        if (Build.VERSION.SDK_INT >= 23) {
//            super.onAttach(context);
//            onAttachToContext(context);
//        }
//    }
//
//    /*
//     * Deprecated on API 23
//     * Use onAttachToContext instead
//     */
//    @SuppressWarnings("deprecation")
//    @Override public void onAttach(Activity activity) {
//        super.onAttach(activity);
//        if (Build.VERSION.SDK_INT < 23) {
//            onAttachToContext(activity);
//        }
//    }

//    protected void onAttachToContext(Context context) {
//        this.mActivity = (Activity) context;
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main, container, false);
        ButterKnife.bind(this, view);
        service= RetrofitService.rest_api.create(RetrofitService.class);

        view.findViewById(R.id.top_home).setVisibility(View.GONE);
        pref = new SettingPreference("loginData",getActivity());

        loginSabun = pref.getValue("sabun_no","").trim();
        loginName = pref.getValue("user_nm","").trim();
        userSosok = pref.getValue("user_sosok","").trim();
        jPos = pref.getValue("j_pos","").trim();
        part1_cd = pref.getValue("part1_cd","");
        part2_cd = pref.getValue("part2_cd","");
        latestAppVer = pref.getValue("LATEST_APP_VER","");

        String currentAppVer= getAppVersion(getActivity());
        UtilClass.logD(TAG, "currentAppVer="+currentAppVer+", latestAppVer="+latestAppVer);
        if(!currentAppVer.equals(latestAppVer)){
            //최신버전이 아닐때
            fileNm= "sjsf_himec_"+latestAppVer+"-debug.apk";
            alertDialog();
        }

        token = FirebaseInstanceId.getInstance().getToken();
        UtilClass.logD(TAG, "Refreshed token: " + token);

        String mode= getArguments().getString("mode");
        if(mode.equals("first")){
            permissionlistener = new PermissionListener() {
                @Override
                public void onPermissionGranted() {
//                Toast.makeText(getApplicationContext(), "권한 허가", Toast.LENGTH_SHORT).show();
                    phone_num= getPhoneNumber();
                    postData();
                }

                @Override
                public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                    Toast.makeText(getActivity(), "권한 거부 목록\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
                    phone_num="";
                    postData();
                }
            };
            new TedPermission(getActivity())
                    .setPermissionListener(permissionlistener)
                    .setRationaleMessage("전화번호 정보를 가져오기 위해선 권한이 필요합니다.")
                    .setDeniedMessage("권한을 확인하세요.\n\n [설정] > [애플리케이션] [해당앱] > [권한]")
                    .setGotoSettingButtonText("권한확인")
                    .setPermissions(Manifest.permission.CALL_PHONE)
                    .check();
        }
        onAppCheck= true;

        return view;
    }

    public static String getAppVersion(Context context) {
        // application version
        String versionName = "";
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            UtilClass.logD(TAG, "getAppVersion Error");
        }

        return versionName;
    }

    @OnClick(R.id.imageView1)
    public void getMenu1() {
        Intent intent = new Intent(getActivity(), PeoplesCardDialogActivity.class);
        intent.putExtra("sabun_no", MainFragment.loginSabun);
        startActivity(intent);
    }

    @OnClick(R.id.imageView2)
    public void getMenu2() {
        Intent intent = new Intent(getActivity(),FragMenuActivity.class);
        intent.putExtra("title", "월중행사계획표");
        startActivity(intent);
    }

    @OnClick(R.id.imageView3)
    public void getMenu3() {
        Intent intent = new Intent(getActivity(),FragMenuActivity.class);
        intent.putExtra("title", "공지사항");
        startActivity(intent);
    }

    @OnClick(R.id.imageView4)
    public void getMenu4() {
        Intent intent = new Intent(getActivity(),FragMenuActivity.class);
        intent.putExtra("title", "사람찾기");
        startActivity(intent);
    }

    @OnClick(R.id.imageView5)
    public void getMenu5() {
        Intent intent = new Intent(getActivity(),FragMenuActivity.class);
        intent.putExtra("title", "근태/급여");
        startActivity(intent);
    }

    @OnClick(R.id.imageView6)
    public void getMenu6() {
        Intent intent = new Intent(getActivity(),FragMenuActivity.class);
        intent.putExtra("title", "작업자위치");
        startActivity(intent);
    }

    @OnClick(R.id.imageView7)
    public void getMenu7() {
        Intent intent = new Intent(getActivity(),FragMenuActivity.class);
        intent.putExtra("title", "W/O정보");
        startActivity(intent);
    }

    @OnClick(R.id.imageView8)
    public void getMenu8() {
        Intent intent = new Intent(getActivity(),FragMenuActivity.class);
        intent.putExtra("title", "작업기준");
        startActivity(intent);
    }

    //푸시 데이터 전송
    public void postData() {
        String android_id = "" + android.provider.Settings.Secure.getString(getActivity().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        Map<String, Object> map = new HashMap();
        map.put("Token",token);
        map.put("phone_num",phone_num);
        map.put("sabun_no",loginSabun);
        map.put("and_id",android_id);

        pDlalog = new ProgressDialog(getActivity());
        UtilClass.showProcessingDialog(pDlalog);

        RetrofitService service = RetrofitService.rest_api.create(RetrofitService.class);
        Call<Datas> call = service.sendData("Board","fcmTokenData",map);

        call.enqueue(new Callback<Datas>() {
            @Override
            public void onResponse(Call<Datas> call, Response<Datas> response) {
                UtilClass.logD(TAG, "response="+response);
                if (response.isSuccessful()) {
                    UtilClass.logD(TAG, "isSuccessful="+response.body().toString());
                    String status= response.body().getStatus();

                }else{
                    Toast.makeText(getActivity(), "handleResponse Main",Toast.LENGTH_SHORT).show();
                }
                if(pDlalog!=null) pDlalog.dismiss();
            }

            @Override
            public void onFailure(Call<Datas> call, Throwable t) {
                if(pDlalog!=null) pDlalog.dismiss();
                UtilClass.logD(TAG, "onFailure="+call.toString()+", "+t);
                Toast.makeText(getActivity(), "토큰 생성에 실패하였습니다.",Toast.LENGTH_SHORT).show();
            }
        });

    }

    // 단말기 핸드폰번호 얻어오기
    public String getPhoneNumber() {
        String num = null;
        try {
            TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            num = tm.getLine1Number();
            if(num!=null&&num.startsWith("+82")){
                num = num.replace("+82", "0");
            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), "오류가 발생 하였습니다!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        return num;
    }

    public void alertDialog(){
        final android.app.AlertDialog.Builder alertDlg = new android.app.AlertDialog.Builder(getActivity());
        alertDlg.setTitle("알림");
        alertDlg.setMessage("현재 앱의 버전보다 높은 최신 버전이 있습니다.");

        // '예' 버튼이 클릭되면
        alertDlg.setPositiveButton("지금 설치", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                installAPK();
            }
        });
        // '아니오' 버튼이 클릭되면
        alertDlg.setNegativeButton("다음에 설치", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDlg.show();
    }

    public void installAPK() {
        UtilClass.logD("InstallApk", "Start");
        File apkFile = new File(fileDir + fileNm);
        if(apkFile.exists()) {
            try {
                Intent webLinkIntent = new Intent(Intent.ACTION_VIEW);
                Uri uri = null;

                // So you have to use Provider
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    uri = FileProvider.getUriForFile(getActivity(), getActivity().getPackageName() + ".provider", apkFile);

                    // Add in case of if We get Uri from fileProvider.
                    webLinkIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    webLinkIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }else{
                    uri = Uri.fromFile(apkFile);
                }

                webLinkIntent.setDataAndType(uri, "application/vnd.android.package-archive");

                startActivity(webLinkIntent);
            } catch (ActivityNotFoundException ex){
                ex.printStackTrace();
                Toast.makeText(getActivity(), "설치에 실패 하였습니다.", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(getActivity(), "최신버전 파일을 다운로드 합니다.", Toast.LENGTH_SHORT).show();
            try {
                downloadFile("http://w-cms.co.kr:9090/app/apkDown.do?appGubun="+fileNm);
            }catch (Exception e){

            }
        }

    }

    //파일 다운로드
    public void downloadFile(String fileUrl) {
        final ProgressDialog pDlalog = new ProgressDialog(getActivity());
        UtilClass.showProcessingDialog(pDlalog);

        Call<ResponseBody> call = service.downloadFile(fileUrl);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    UtilClass.logD(TAG, "isSuccessful="+response.body().toString());
                    //새로운 TaskFragment 생성
                    WorkStandardMainFragment.TaskFragment taskFragment = new WorkStandardMainFragment.TaskFragment();

                    //Task를 생성하여 taskFragment의 setTask메소드에서 Task 인스턴스를 저장하고, Task에게 taskFragment 인스턴스를 넘겨준다.
                    taskFragment.setTask(new FileDownProgressTask(response.body(),fileDir, fileNm));

                    //taskFragment에서 getTargetFragment().onActivityResult를 호출하면 MainFragment의 onActivityResult가 호출되도록 설정
                    taskFragment.setTargetFragment( MainFragment.this, DIALOG_REQUEST_CODE );

                    //프레그먼트를 보여준다.태그는 나중에 프레그먼트를 찾기 위해 사용 된다.
                    taskFragment.show(getFragmentManager(), TASK_FRAGMENT_TAG);

                }else{
                    UtilClass.logD(TAG, "response isFailed="+response);
                    Toast.makeText(getActivity(), "response isFailed", Toast.LENGTH_SHORT).show();
                }
                if(pDlalog!=null) pDlalog.dismiss();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if(pDlalog!=null) pDlalog.dismiss();
                UtilClass.logD(TAG, "onFailure="+call.toString()+", "+t);
                Toast.makeText(getActivity(), "onFailure downloadFile",Toast.LENGTH_LONG).show();
            }
        });

    }

    //taskFragment에서 getTargetFragment().onActivityResult를 호출하면 이 메소드가 호출된다.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        UtilClass.logD(TAG, "onActivityResult="+ resultCode);
        if (requestCode == DIALOG_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Boolean downloadResult = data.getExtras().getBoolean("downloadResult");
            UtilClass.logD(TAG, "onActivityResult="+downloadResult);
            if ( downloadResult) {
                Toast.makeText(getActivity(), "다운로드 완료", Toast.LENGTH_SHORT).show();
                installAPK();
            }else {
                Toast.makeText(getActivity(), "다운로드 실패", Toast.LENGTH_SHORT).show();
            }

        }else if (requestCode == DIALOG_REQUEST_CODE && resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(getActivity(), "다운로드가 중단되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
