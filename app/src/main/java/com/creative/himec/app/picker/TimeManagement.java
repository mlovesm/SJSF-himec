package com.creative.himec.app.picker;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;

/**
 * Created by GS on 2017-09-16.
 */

public interface TimeManagement {
    TimeManagement dialogDatePicker(DatePickerDialog.OnDateSetListener onDateSetListener);

    void showDatePickerDialog();

    TimeManagement dialogTimePicker(TimePickerDialog.OnTimeSetListener onTimeSetListener);

    void showTimePickerDialog();
}
