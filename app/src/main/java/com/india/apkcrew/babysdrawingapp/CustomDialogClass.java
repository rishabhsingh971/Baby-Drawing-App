package com.india.apkcrew.babysdrawingapp;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * Created by lenovo on 26-03-2017.
 */

public class CustomDialogClass extends Dialog implements DialogInterface{

    private ImageButton yes, no;
    private TextView tv;
    private LayoutInflater layoutInflater;

    public CustomDialogClass(Context context) {
        super(context);
        layoutInflater = LayoutInflater.from(context);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = layoutInflater.inflate(R.layout.custom_dialog, null);
        this.setContentView(dialogView);
        yes = (ImageButton) dialogView.findViewById(R.id.btn_yes);
        no = (ImageButton) dialogView.findViewById(R.id.btn_no);
        tv = (TextView) dialogView.findViewById(R.id.dialog_text);
    }

    public void setMessage(CharSequence message)
    {
        tv.setText(message);
    }
    public void setPositiveButton(final View.OnClickListener listener)
    {
        yes.setOnClickListener(listener);
    }
    public void setNegativeButton(final View.OnClickListener listener)
    {
        no.setOnClickListener(listener);
    }

}