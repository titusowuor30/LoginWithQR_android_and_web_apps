package com.examples.backendless;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.HeadersManager;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.messaging.MessageStatus;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String PROPERTY_STORAGE = MainActivity.class.getName();

    private static final String USER_NAME_KEY = "userName";
    private static final String USER_PASSWORD_KEY = "userPassword";
    private static final String USER_TOKEN_KEY = "userToken";
    private static final int rc_ScanQR = 1;

    public static final Uri zxingUri = Uri.parse("https://play.google.com/store/apps/details?id=com.google.zxing.client.android");
    private static final String ZXING_PACKAGE = "com.google.zxing.client.android";

    private static final String appId = ;
    private static final String apiKey = ;


    private EditText editText_name;
    private EditText editText_password;
    private EditText editText_userInfo;
    private Button button_login_logout;
    private Button button_loginWithQR;

    private String channelName = null;
    private String userName = null;
    private String userPassword = null;
    private String userToken = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        Backendless.initApp(this.getApplicationContext(), appId, apiKey);

        if (!isPackageInstalled(ZXING_PACKAGE))
        {
            editText_name.setEnabled(false);
            editText_password.setEnabled(false);
            editText_userInfo.setEnabled(false);
            button_login_logout.setEnabled(false);
            button_loginWithQR.setEnabled(false);

            Handler handler = new Handler();
            handler.postDelayed(() ->
            {
                MainActivity.this.runOnUiThread(() ->
                {
                    if (getCurrentFocus() != null) {
                        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }

                    View view = findViewById(R.id.main_activity);
                    Snackbar snackbar = Snackbar.make(view, "Unable to find QR scanner app. Please make sure to install the 'Barcode Scanner' app by ZXing Team", Snackbar.LENGTH_INDEFINITE);
                    View snackbarView = snackbar.getView();
                    TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
                    textView.setMaxLines(5);
                    snackbar.setAction("Install", this::installZXing);
                    snackbar.show();
                });
            }, 1500);
        }
    }

    private void initUI() {
        editText_name = findViewById(R.id.editText_name);
        editText_password = findViewById(R.id.editText_password);
        editText_userInfo = findViewById(R.id.editText_userInfo);

        button_login_logout = findViewById(R.id.button_login);
        button_loginWithQR = findViewById(R.id.button_loginWithQR);
        button_loginWithQR.setOnClickListener(this::scanDataFromQRCode);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sharedPreferences = getSharedPreferences(PROPERTY_STORAGE, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(USER_NAME_KEY, this.userName);
        editor.putString(USER_PASSWORD_KEY, this.userPassword);
        editor.putString(USER_TOKEN_KEY, this.userToken);
        editor.apply();
        Log.i(MainActivity.class.getSimpleName(), "onPause: saved data successfully [userName=" + this.userName + ", userPassword=" + this.userPassword + ", userToken=" + this.userToken + "]");
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        SharedPreferences sharedPreferences = getSharedPreferences(PROPERTY_STORAGE, MODE_PRIVATE);
        this.userName = sharedPreferences.getString(USER_NAME_KEY, null);
        this.editText_name.setText(this.userName);
        this.userPassword = sharedPreferences.getString(USER_PASSWORD_KEY, null);
        this.editText_password.setText(this.userPassword);
        this.userToken = sharedPreferences.getString(USER_TOKEN_KEY, null);
        Log.i(MainActivity.class.getSimpleName(), "onPostResume: restore data successfully [userName=" + this.userName + ", userPassword=" + this.userPassword + ", userToken=" + this.userToken + "]");

        if (userToken != null) {
            button_loginWithQR.setVisibility(View.VISIBLE);
            button_login_logout.setText("Logout");
            button_login_logout.setOnClickListener(this::backendlessLogout);
        } else {
            button_login_logout.setOnClickListener(this::backendlessLogin);
        }

        if (channelName != null)
            loginWithQRCode(this.channelName);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(MainActivity.class.getSimpleName(), "onStart: ");
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.i(MainActivity.class.getSimpleName(), "onRestoreInstanceState: ");
    }

    private void backendlessLogin(View view) {
        this.userName = editText_name.getText().toString();
        this.userPassword = editText_password.getText().toString();

        Backendless.UserService.login(MainActivity.this.userName, MainActivity.this.userPassword, new AsyncCallback<BackendlessUser>() {
            @Override
            public void handleResponse(BackendlessUser response) {
                button_loginWithQR.setVisibility(View.VISIBLE);
                button_login_logout.setText("Logout");
                button_login_logout.setOnClickListener(MainActivity.this::backendlessLogout);

                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Object> property : response.getProperties().entrySet())
                    sb.append(property.getKey()).append(" : ").append(property.getValue()).append('\n');

                editText_userInfo.setText(sb.toString());
                userToken = HeadersManager.getInstance().getHeader(HeadersManager.HeadersEnum.USER_TOKEN_KEY);
                Log.i(MainActivity.class.getSimpleName(), "backendlessLogin [userToken=" + MainActivity.this.userToken + "]");
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                userToken = null;
                editText_userInfo.setText(fault.getCode() + '\n' + fault.getMessage() + '\n' + fault.getDetail());
            }
        });
    }

    private void backendlessLogout(View view) {
        Backendless.UserService.logout(new AsyncCallback<Void>() {
            @Override
            public void handleResponse(Void response) {
                userToken = null;
                button_loginWithQR.setVisibility(View.INVISIBLE);
                button_login_logout.setText("Login to Backendless");
                button_login_logout.setOnClickListener(MainActivity.this::backendlessLogin);
                editText_userInfo.setText("");
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                editText_userInfo.setText(fault.toString());
            }
        });
    }

    private void scanDataFromQRCode(View view) {
        Intent intent = new Intent(ZXING_PACKAGE + ".SCAN");
        intent.setPackage(ZXING_PACKAGE);
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        startActivityForResult(intent, rc_ScanQR);
    }

    private void loginWithQRCode(String channelName) {
        Log.i(MainActivity.class.getSimpleName(), "loginWithQRCode: start remote login process");
        if (userToken == null) {
            Log.i(TAG, "loginWithQRCode: userToken is null.");
            return;
        }

        Log.i(MainActivity.class.getSimpleName(), "loginWithQRCode [channelName=" + channelName + ", userToken=" + this.userToken + "]");
        Backendless.Messaging.publish(channelName, this.userToken, new AsyncCallback<MessageStatus>() {
            @Override
            public void handleResponse(MessageStatus response) {
                Log.i(MainActivity.class.getSimpleName(), "loginWithQRCode: sent token successfully");
            }

            @Override
            public void handleFault(BackendlessFault fault) {

            }
        });
        this.channelName = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            String contents = intent.getStringExtra("SCAN_RESULT");
            String format = intent.getStringExtra("SCAN_RESULT_FORMAT");

            switch (requestCode) {
                case rc_ScanQR:
                    this.channelName = contents;
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            // Handle cancel
        }
    }

    private boolean isPackageInstalled(String packageName) {
        PackageManager pm = this.getApplicationContext().getPackageManager();

        try {
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void installZXing(View view) {
        Intent googlePlayIntent = new Intent(Intent.ACTION_VIEW, zxingUri);
        googlePlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(googlePlayIntent);

    }
}
