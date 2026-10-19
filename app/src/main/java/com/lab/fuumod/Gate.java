package com.lab.fuumod;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.security.MessageDigest;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** 密码门：首启设置 4-8 位 PIN，之后校验（PBKDF2 + salt）。 */
public class Gate extends AppCompatActivity {

    private static final String SALT = "FuuMod_v1";
    private static final int ITER = 120_000;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        final boolean firstRun = !CfgStore.gateHash(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(64, 160, 64, 64);

        TextView tip = new TextView(this);
        tip.setText(firstRun ? "首次使用：设置访问密码" : "输入访问密码");
        tip.setTextSize(16);

        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        et.setHint("PIN");

        final EditText et2 = new EditText(this);
        et2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        et2.setHint("再输一次");
        et2.setVisibility(firstRun ? LinearLayout.VISIBLE : LinearLayout.GONE);

        Button go = new Button(this);
        go.setText(firstRun ? "设置并进入" : "进入");

        root.addView(tip);
        root.addView(et);
        root.addView(et2);
        root.addView(go);
        setContentView(root);

        go.setOnClickListener(v -> {
            String pin = et.getText().toString();
            if (pin.length() < 4) {
                et.setError("至少 4 位");
                return;
            }
            if (firstRun) {
                if (!pin.equals(et2.getText().toString())) {
                    et2.setError("两次不一致");
                    return;
                }
                CfgStore.setGateHash(this, hash(pin));
            } else if (!hash(pin).equalsIgnoreCase(CfgStore.gateHashValue(this))) {
                et.setError("密码错误");
                return;
            }
            startActivity(new Intent(this, AppsActivity.class));
            finish();
        });
    }

    static String hash(String pin) {
        try {
            byte[] dk = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new PBEKeySpec(pin.toCharArray(), SALT.getBytes("UTF-8"), ITER, 256))
                    .getEncoded();
            byte[] h = MessageDigest.getInstance("SHA-256")
                    .digest((SALT + "|" + hex(dk)).getBytes("UTF-8"));
            return hex(h);
        } catch (Throwable t) {
            return "";
        }
    }

    static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (byte x : b) s.append(String.format(Locale.US, "%02x", x));
        return s.toString();
    }
}