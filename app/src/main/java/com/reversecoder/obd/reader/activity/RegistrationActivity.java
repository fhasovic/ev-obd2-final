package com.reversecoder.obd.reader.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.reversecoder.library.storage.SessionManager;
import com.reversecoder.obd.reader.R;
import com.reversecoder.obd.reader.sqlite.table.User;
import com.reversecoder.sqlite.crud.DataSupport;

import java.util.List;

import static com.reversecoder.obd.reader.util.AllConstants.REGISTERED_EMAIL;
import static com.reversecoder.obd.reader.util.AllConstants.REGISTERED_PASSWORD;
import static com.reversecoder.obd.reader.util.AllConstants.REGISTERED_USER_NAME;

public class RegistrationActivity extends AppCompatActivity {

    Button btnLogin, btnRegistration;
    EditText edtName, edtEmail, edtPassword;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        initRegistrationUI();
        initRegistrationAction();
    }

    private void initRegistrationUI() {
        btnRegistration = (Button) findViewById(R.id.btn_registration);
        btnLogin = (Button) findViewById(R.id.btn_login);
        edtName = (EditText) findViewById(R.id.edt_name);
        edtEmail = (EditText) findViewById(R.id.edt_email);
        edtPassword = (EditText) findViewById(R.id.edt_password);

        edtName.setText(SessionManager.getStringSetting(RegistrationActivity.this, REGISTERED_USER_NAME));
        edtEmail.setText(SessionManager.getStringSetting(RegistrationActivity.this, REGISTERED_EMAIL));

//        edtName.setText("rashed");
//        edtEmail.setText("rashed.droid@gmail.com");
//        edtPassword.setText("123456");
    }

    private void initRegistrationAction() {
        btnRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edtName.getText().toString().equalsIgnoreCase("")) {
                    Toast.makeText(RegistrationActivity.this, getResources().getString(R.string.toast_empty_name_field), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (edtEmail.getText().toString().equalsIgnoreCase("")) {
                    Toast.makeText(RegistrationActivity.this, getResources().getString(R.string.toast_empty_email_field), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (edtPassword.getText().toString().equalsIgnoreCase("")) {
                    Toast.makeText(RegistrationActivity.this, getResources().getString(R.string.toast_empty_password_field), Toast.LENGTH_SHORT).show();
                    return;
                }

                List<User> users = DataSupport.where("email = ?", edtEmail.getText().toString()).find(User.class);
                if (users.size() > 0 && users.get(0).getEmail().equalsIgnoreCase(edtEmail.getText().toString())) {
                    Toast.makeText(RegistrationActivity.this, getResources().getString(R.string.toast_already_have_an_account), Toast.LENGTH_SHORT).show();
                } else {
                    User user = new User(edtName.getText().toString(), edtEmail.getText().toString(), edtPassword.getText().toString());
                    user.save();
                    SessionManager.setStringSetting(RegistrationActivity.this, REGISTERED_USER_NAME, edtName.getText().toString());
                    SessionManager.setStringSetting(RegistrationActivity.this, REGISTERED_EMAIL, edtEmail.getText().toString());
                    SessionManager.setStringSetting(RegistrationActivity.this, REGISTERED_PASSWORD, edtPassword.getText().toString());
                    Toast.makeText(RegistrationActivity.this, getResources().getString(R.string.toast_successfully_registered), Toast.LENGTH_SHORT).show();
                }

            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });


    }
}
