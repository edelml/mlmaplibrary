package com.ml.map;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationAnnotationActivity extends AppCompatActivity {

    //Called from the 'annotate' button in the Notification
    //This in turn captures user input and sends the input to the GPS Logging Service

    private static final Logger LOG = LoggerFactory.getLogger(NotificationAnnotationActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new MaterialDialog.Builder(this)
                .title(R.string.add_description)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .negativeText(R.string.cancel)
                .autoDismiss(true)
                .canceledOnTouchOutside(true)
                .cancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        finish();
                    }
                })
                .input(getString(R.string.letters_numbers), "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog materialDialog, @NonNull CharSequence input) {

                        LOG.info("Annotation from notification: " + input.toString());
                        Intent serviceIntent = new Intent(getApplicationContext(), GoblobLocationService.class);
                        serviceIntent.putExtra(IntentConstants.SET_DESCRIPTION, input.toString());
                        getApplicationContext().startService(serviceIntent);
                        materialDialog.dismiss();
                        finish();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        materialDialog.hide();
                        materialDialog.dismiss();
                        finish();
                    }
                })
                .show();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            super.finish();
        }

        return super.onKeyDown(keyCode, event);
    }
}
