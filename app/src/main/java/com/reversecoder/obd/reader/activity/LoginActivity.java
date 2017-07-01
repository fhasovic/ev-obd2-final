package com.reversecoder.obd.reader.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.reversecoder.library.storage.SessionManager;
import com.reversecoder.obd.reader.R;
import com.reversecoder.obd.reader.sqlite.table.User;
import com.reversecoder.sqlite.crud.DataSupport;

import java.util.List;

import static com.reversecoder.obd.reader.util.AllConstants.IS_REMEMBER_ME;
import static com.reversecoder.obd.reader.util.AllConstants.IS_USER_LOGGED_IN;
import static com.reversecoder.obd.reader.util.AllConstants.REMEMBERED_EMAIL;
import static com.reversecoder.obd.reader.util.AllConstants.REMEMBERED_PASSWORD;

public class LoginActivity extends AppCompatActivity {

    Button btnLogin, btnRegistration;
    EditText edtEmail, edtPassword;
    CheckBox cBoxRememberMe;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initLoginUI();
        initLoginAction();
    }

    private void initLoginUI() {
        btnLogin = (Button) findViewById(R.id.btn_login);
        btnRegistration = (Button) findViewById(R.id.btn_registration);
        edtEmail = (EditText) findViewById(R.id.edt_email);
        edtPassword = (EditText) findViewById(R.id.edt_password);
        cBoxRememberMe = (CheckBox) findViewById(R.id.cbox_remember_me);

        cBoxRememberMe.setChecked(SessionManager.getBooleanSetting(LoginActivity.this, IS_REMEMBER_ME, false));
        if (SessionManager.getBooleanSetting(LoginActivity.this, IS_REMEMBER_ME, false)) {
            edtEmail.setText(SessionManager.getStringSetting(LoginActivity.this, REMEMBERED_EMAIL));
        }

//        edtEmail.setText("rashed.droid@gmail.com");
//        edtPassword.setText("123456");
    }

    private void initLoginAction() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edtEmail.getText().toString().equalsIgnoreCase("")) {
                    Toast.makeText(LoginActivity.this, getResources().getString(R.string.toast_empty_email_field), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (edtPassword.getText().toString().equalsIgnoreCase("")) {
                    Toast.makeText(LoginActivity.this, getResources().getString(R.string.toast_empty_password_field), Toast.LENGTH_SHORT).show();
                    return;
                }

                List<User> users = DataSupport.where("email = ?", edtEmail.getText().toString()).find(User.class);
                if (users.size() > 0) {
                    if (users.get(0).getEmail().equalsIgnoreCase(edtEmail.getText().toString()) && users.get(0).getPassword().equalsIgnoreCase(edtPassword.getText().toString())) {
                        SessionManager.setBooleanSetting(LoginActivity.this, IS_USER_LOGGED_IN, true);
                        SessionManager.setStringSetting(LoginActivity.this, REMEMBERED_EMAIL, edtEmail.getText().toString());
                        SessionManager.setStringSetting(LoginActivity.this, REMEMBERED_PASSWORD, edtPassword.getText().toString());
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, getResources().getString(R.string.toast_did_not_match_credential), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, getResources().getString(R.string.toast_user_not_found), Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
                finish();
            }
        });

        cBoxRememberMe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SessionManager.setBooleanSetting(LoginActivity.this, IS_REMEMBER_ME, isChecked);
            }
        });
    }
}
