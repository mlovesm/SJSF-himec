package com.creative.himec.app.common;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.creative.himec.app.R;
import com.creative.himec.app.adaptor.BoardAdapter;
import com.creative.himec.app.retrofit.Datas;
import com.creative.himec.app.retrofit.RetrofitService;
import com.creative.himec.app.util.UtilClass;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MonthlyFragment extends Fragment {
    private static final String TAG = "MonthlyFragment";
    private RetrofitService service;
    private String title;

    private ArrayList<HashMap<String,String>> arrayList;
    private BoardAdapter mAdapter;
    @Bind(R.id.listView1) ListView listView;
    @Bind(R.id.top_title) TextView textTitle;
    @Bind(R.id.textButton1) TextView tv_button1;
    @Bind(R.id.textButton2) TextView tv_button2;

    private boolean isSdate=false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.monthly_list, container, false);
        ButterKnife.bind(this, view);
        service= RetrofitService.rest_api.create(RetrofitService.class);

        title= getArguments().getString("title");
        textTitle.setText(title);
        view.findViewById(R.id.top_write).setVisibility(View.VISIBLE);

        tv_button1.setText(UtilClass.getCurrentDate(3, "."));
        tv_button2.setText(UtilClass.getCurrentDate(3, "."));

        async_progress_dialog();

        listView.setOnItemClickListener(new ListViewItemClickListener());

        return view;
    }//onCreateView

    public void async_progress_dialog(){
        final ProgressDialog pDlalog = new ProgressDialog(getActivity());
        UtilClass.showProcessingDialog(pDlalog);

        Call<Datas> call = service.listData("Common","monthlyList",tv_button1.getText().toString(), tv_button2.getText().toString());
        call.enqueue(new Callback<Datas>() {
            @Override
            public void onResponse(Call<Datas> call, Response<Datas> response) {
                UtilClass.logD(TAG, "response="+response);
                if (response.isSuccessful()) {
                    UtilClass.logD(TAG, "isSuccessful="+response.body().toString());
                    String status= response.body().getStatus();
                    try {
                        if(response.body().getCount()==0){
                            Toast.makeText(getActivity(), "데이터가 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                        arrayList = new ArrayList<>();
                        arrayList.clear();
                        for(int i=0; i<response.body().getList().size();i++){
                            UtilClass.dataNullCheckZero(response.body().getList().get(i));

                            HashMap<String,String> hashMap = new HashMap<>();
                            hashMap.put("key",response.body().getList().get(i).get("idx"));
                            hashMap.put("data1",response.body().getList().get(i).get("plan_sdate")
                                    +"  "+response.body().getList().get(i).get("plan_stime")+"   ~ ");
                            hashMap.put("data2",response.body().getList().get(i).get("plan_edate")
                                    +"  "+response.body().getList().get(i).get("plan_etime"));
                            hashMap.put("data3",response.body().getList().get(i).get("plan_title"));
                            hashMap.put("data4",response.body().getList().get(i).get("plan_locate"));
                            arrayList.add(hashMap);
                        }

                        mAdapter = new BoardAdapter(getActivity(), arrayList, "Monthly");
                        listView.setAdapter(mAdapter);
                    } catch ( Exception e ) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "에러코드 Monthly 1", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(getActivity(), "response isFailed", Toast.LENGTH_SHORT).show();
                }
                if(pDlalog!=null) pDlalog.dismiss();
            }

            @Override
            public void onFailure(Call<Datas> call, Throwable t) {
                if(pDlalog!=null) pDlalog.dismiss();
                UtilClass.logD(TAG, "onFailure="+call.toString()+", "+t);
                Toast.makeText(getActivity(), "onFailure Board",Toast.LENGTH_SHORT).show();
            }
        });
    }

    @OnClick(R.id.top_home)
    public void goHome() {
        UtilClass.goHome(getActivity());
    }

    @OnClick(R.id.top_write)
    public void getWriteBoard() {
        Fragment frag = new MonthlyWriteFragment();
        Bundle bundle = new Bundle();

        bundle.putString("mode","insert");
        frag.setArguments(bundle);

        FragmentManager fm = getFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentReplace, frag);
        fragmentTransaction.addToBackStack(title+"작성");
        fragmentTransaction.commit();
    }


    //날짜 필드
    private static final String DATE_PICKER = "mDatePicker";
    private static final String DAY_FIELD = "day";
    private static final String ID = "id";
    private static final String ANDROID = "android";
    private Context mContext;
    private DatePickerDialog mDatePickerDialog;
    private TextView tv_date;

    //날짜설정
    @OnClick({R.id.textButton1, R.id.textButton2})
    public void getDateDialog(View view) {
        mContext = getActivity();
        int year = 0, month = 0;
        UtilClass.logD(TAG, "button="+view.getId());

        if (view.getId() == R.id.textButton1) {
            tv_date= tv_button1;
        }else{
            tv_date= tv_button2;
        }
        if(tv_date.length()>0){
            String date= tv_date.getText().toString();
            int point= date.indexOf(".");
            year= Integer.parseInt(date.substring(0,point));
            month= Integer.parseInt(date.substring(point+1));

        }

        if (Build.VERSION.SDK_INT == 24) {    // Android 7.0 Nougat, API 24
            final Context contextThemeWrapper = new ContextThemeWrapper(mContext, android.R.style.Theme_Holo_Light_Dialog);
            try {
                mDatePickerDialog = new FixedHoloDatePickerDialog(contextThemeWrapper, date_listener, year, month, -1);

            } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException |
                    InvocationTargetException | InstantiationException e) {
                e.printStackTrace();
            } catch (java.lang.InstantiationException e) {
                e.printStackTrace();
            }
        } else {
            mDatePickerDialog= dialogDatePicker(year, month);
        }
        mDatePickerDialog.show();

    }


    //24 제외 버전
    public DatePickerDialog dialogDatePicker(int year, int month) {
        mDatePickerDialog = new DatePickerDialog(getActivity(), AlertDialog.THEME_HOLO_LIGHT, date_listener, year, month, -1);
        try {
            Field[] datePickerDialogFields = mDatePickerDialog.getClass().getDeclaredFields();
            for (Field datePickerDialogField : datePickerDialogFields) {
                if (datePickerDialogField.getName().equals(DATE_PICKER)) {
                    datePickerDialogField.setAccessible(true);
                    DatePicker datePicker =
                            (DatePicker) datePickerDialogField.get(mDatePickerDialog);
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        customDatePicker(datePicker);
                    } else {
                        if ("mDaySpinner".equals(datePickerDialogField.getName())
                                || "mDaySpinner".equals(datePickerDialogField
                                .getName())
//                                    || "mMonthPicker".equals(datePickerField.getName())
//                                    || "mMonthSpinner".equals(datePickerField.getName())
                                ) {
                            datePickerDialogField.setAccessible(true);
                            Object dayPicker = new Object();
                            dayPicker = datePickerDialogField.get(datePicker);
                            ((View) dayPicker).setVisibility(View.GONE);
                        }
                    }
                }
            }
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException: ", e);
        }

        return mDatePickerDialog;
    }

    //24버전
    DatePicker mDatePicker;
    private final class FixedHoloDatePickerDialog extends DatePickerDialog {
        private FixedHoloDatePickerDialog(Context context, OnDateSetListener callBack, int year,
                                          int monthOfYear, int dayOfMonth)
                throws ClassNotFoundException, IllegalAccessException, NoSuchMethodException,
                InvocationTargetException, InstantiationException, java.lang.InstantiationException {
            super(context, callBack, year, monthOfYear, dayOfMonth);

            final Field field =
                    this.findField(DatePickerDialog.class, DatePicker.class, DATE_PICKER);
            assert field != null;
            try {
                mDatePicker = (DatePicker) field.get(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            final Class<?> delegateClass =
                    Class.forName("android.widget.DatePicker$DatePickerDelegate");
            final Field delegateField =
                    this.findField(DatePicker.class, delegateClass, "mDelegate");
            assert delegateField != null;
            final Object delegate = delegateField.get(mDatePicker);
            final Class<?> spinnerDelegateClass =
                    Class.forName("android.widget.DatePickerSpinnerDelegate");
            if (delegate.getClass() != spinnerDelegateClass) {
                delegateField.set(mDatePicker, null);
                mDatePicker.removeAllViews();
                final Constructor spinnerDelegateConstructor =
                        spinnerDelegateClass.getDeclaredConstructor(DatePicker.class, Context.class,
                                AttributeSet.class, int.class, int.class);
                spinnerDelegateConstructor.setAccessible(true);
                final Object spinnerDelegate =
                        spinnerDelegateConstructor.newInstance(mDatePicker, context, null,
                                android.R.attr.datePickerStyle, 0);
                delegateField.set(mDatePicker, spinnerDelegate);
                mDatePicker.init(year, monthOfYear, dayOfMonth, this);
                customDatePicker(mDatePicker);
                mDatePicker.setCalendarViewShown(false);
                mDatePicker.setSpinnersShown(true);
            }
        }

        private Field findField(Class objectClass, Class fieldClass, String expectedName) {
            try {
                final Field field = objectClass.getDeclaredField(expectedName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
            for (final Field field : objectClass.getDeclaredFields()) {
                if (field.getType() == fieldClass) {
                    field.setAccessible(true);
                    return field;
                }
            }
            return null;
        }
    }

    private void customDatePicker(DatePicker datePicker) {
        int daySpinnerId = Resources.getSystem().getIdentifier(DAY_FIELD, ID, ANDROID);
        if (daySpinnerId != 0) {
            View daySpinner = datePicker.findViewById(daySpinnerId);
            if (daySpinner != null) {
                daySpinner.setVisibility(View.GONE);
            }
        }
    }

    private DatePickerDialog.OnDateSetListener date_listener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            Toast.makeText(getActivity(), year + "년" + (monthOfYear+1) + "월", Toast.LENGTH_SHORT).show();
            String month= UtilClass.addZero(monthOfYear+1);

            tv_date.setText(year+"."+month);
            async_progress_dialog();
        }
    };


    //날짜설정
//    @OnClick(R.id.textButton1)
//    public void getDateDialog() {
//        getDialog("SD");
//        isSdate=true;
//    }
//    @OnClick(R.id.textButton2)
//    public void getDateDialog2() {
//        getDialog("ED");
//        isSdate=false;
//    }

//    public void getDialog(String gubun) {
//        int year, month, day;
//
//        GregorianCalendar calendar = new GregorianCalendar();
//        year = calendar.get(Calendar.YEAR);
//        month = calendar.get(Calendar.MONTH);
//        day= calendar.get(Calendar.DAY_OF_MONTH);
//
//        if(gubun.equals("SD")){
//            DatePickerDialog dialog = new DatePickerDialog(getActivity(), date_listener, year, month, 1);
//            dialog.show();
//        }else{
//            DatePickerDialog dialog = new DatePickerDialog(getActivity(), date_listener, year, month, day);
//            dialog.show();
//        }
//
//    }

//    private DatePickerDialog.OnDateSetListener date_listener = new DatePickerDialog.OnDateSetListener() {
//        @Override
//        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
//            String month= UtilClass.addZero(monthOfYear+1);
//            String day= UtilClass.addZero(dayOfMonth);
//            String date= year+"."+month+"."+day;
//
//            if(isSdate){
//                tv_button1.setText(date);
//            }else{
//                tv_button2.setText(date);
//            }
//            async_progress_dialog();
//
//        }
//    };

    //ListView의 item (상세)
    private class ListViewItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Fragment frag = null;
            Bundle bundle = new Bundle();

            FragmentManager fm = getFragmentManager();
            FragmentTransaction fragmentTransaction = fm.beginTransaction();
            fragmentTransaction.replace(R.id.fragmentReplace, frag = new MonthlyWriteFragment());
            bundle.putString("title",title+"상세");
            String key= arrayList.get(position).get("key");
            bundle.putString("idx", key);
            bundle.putString("mode", "update");

            frag.setArguments(bundle);
            fragmentTransaction.addToBackStack(title+"상세");
            fragmentTransaction.commit();
        }
    }

}
