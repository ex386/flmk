package com.lab.fuumod;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** 模块自身的设置界面：编辑伪装档案 / 按 seed 生成 / 查看 trace 路径。 */
public class SettingsActivity extends AppCompatActivity {

    private final Map<String, EditText> fields = new LinkedHashMap<>();
    private DeviceProfile profile = new DeviceProfile();

    private static final String[][] LABELS = {
            {"manufacturer", "厂商"}, {"brand", "品牌"}, {"model", "机型"},
            {"device", "device"}, {"product", "product"}, {"board", "board"},
            {"hardware", "hardware"}, {"id", "build id"}, {"fingerprint", "fingerprint"},
            {"serial", "序列号"}, {"androidId", "ANDROID_ID"}, {"imei", "IMEI"},
            {"mac", "MAC"}, {"radio", "radio"}, {"bootloader", "bootloader"}
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        profile = DeviceProfile.from(CfgStore.readOwn(this));

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 60, 40, 120);
        sv.addView(root);

        for (String[] d : LABELS) {
            EditText et = new EditText(this);
            et.setHint(d[0] + " (" + d[1] + ")");
            et.setText(profile.get(d[0]));
            fields.put(d[0], et);
            root.addView(et);
        }

        final EditText seed = new EditText(this);
        seed.setHint("seed：同 seed 复现同一指纹");

        Button gen = new Button(this);
        gen.setText("按 seed 生成");

        Button save = new Button(this);
        save.setText("保存（重启目标 App 生效）");

        TextView info = new TextView(this);
        info.setText("trace: " + Trace.PATH_HINT + "\ncfg 回退: /sdcard/Download/FuuMod/cfg.json");

        root.addView(seed);
        root.addView(gen);
        root.addView(save);
        root.addView(info);
        setContentView(sv);

        gen.setOnClickListener(v -> {
            String s = seed.getText().toString();
            if (s.isEmpty()) {
                Toast.makeText(this, "先填 seed", Toast.LENGTH_SHORT).show();
                return;
            }
            profile = genBySeed(s);
            for (Map.Entry<String, EditText> e : fields.entrySet()) {
                e.getValue().setText(profile.get(e.getKey()));
            }
        });

        save.setOnClickListener(v -> {
            for (Map.Entry<String, EditText> e : fields.entrySet()) {
                profile.set(e.getKey(), e.getValue().getText().toString());
            }
            if (profile.fingerprint.isEmpty()) profile.fingerprint = profile.buildFingerprint();
            CfgStore.writeOwn(this, profile);
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        });
    }

    /** 由 seed 派生一套稳定的伪机型；MAC 首字节固定 02（本地管理位）。 */
    static DeviceProfile genBySeed(String seed) {
        String h = sha256(seed);
        String[] makers = {"Xiaomi", "Redmi", "OPPO", "vivo", "HONOR", "OnePlus"};
        String[] devices = {"alioth", "venus", "lisa", "mars", "curtis", "lemonade"};
        DeviceProfile p = new DeviceProfile();
        int i = Integer.parseInt(h.substring(0, 2), 16) % makers.length;
        p.manufacturer = makers[i];
        p.brand = makers[i].equals("Redmi") ? "Redmi" : makers[i];
        p.device = devices[Integer.parseInt(h.substring(2, 4), 16) % devices.length];
        p.product = p.device;
        p.board = p.device;
        p.hardware = i < 2 ? "qcom" : "mt6893";
        p.model = String.format(Locale.US, "%s-%s", p.brand, h.substring(4, 8).toUpperCase(Locale.US));
        p.id = "RKQ1." + h.substring(8, 14).toUpperCase(Locale.US);
        p.serial = h.substring(14, 26).toUpperCase(Locale.US);
        p.androidId = h.substring(16, 32);
        p.imei = "86" + digits(h, 26, 13);
        p.mac = "02:" + h.substring(0, 2) + ":" + h.substring(2, 4) + ":"
                + h.substring(4, 6) + ":" + h.substring(6, 8) + ":" + h.substring(8, 10);
        p.bootloader = "unknown";
        p.fingerprint = p.buildFingerprint();
        return p;
    }

    static String digits(String h, int from, int len) {
        StringBuilder s = new StringBuilder();
        int i = from;
        while (s.length() < len) {
            s.append((char) ('0' + (Character.digit(h.charAt(i % h.length()), 16) % 10)));
            i++;
        }
        return s.toString();
    }

    static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"));
            StringBuilder b = new StringBuilder();
            for (byte x : d) b.append(String.format(Locale.US, "%02x", x));
            return b.toString();
        } catch (Throwable t) {
            return "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        }
    }
}