package com.lab.fuumod;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 模块自身的设置界面：编辑伪装档案 / 按 seed 生成 / 导入导出档案 / 查看 trace 路径。
 */
public class SettingsActivity extends AppCompatActivity {

    private final Map<String, EditText> fields = new LinkedHashMap<>();
    private DeviceProfile profile = new DeviceProfile();

    private ActivityResultLauncher<String> createDoc;
    private ActivityResultLauncher<String[]> openDoc;

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

        // 导出：系统「另存为」对话框，写到用户选的位置（SAF，不需要存储权限）
        createDoc = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                    if (uri == null) return;
                    writeProfile(uri);
                });

        // 导入：系统文件选择器
        openDoc = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), uri -> {
                    if (uri == null) return;
                    readProfile(uri);
                });

        profile = DeviceProfile.from(CfgStore.readOwn(this));

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 60, 40, 120);
        sv.addView(root);

        for (String[] d : LABELS) {
            EditText et = new EditText(this);
            et.setHint(d[0] + " (" + d[1] + ")");
            fields.put(d[0], et);
            root.addView(et);
        }
        fillFields();

        final EditText seed = new EditText(this);
        seed.setHint("seed：同 seed 复现同一指纹");

        Button gen = new Button(this);
        gen.setText("按 seed 生成");

        Button save = new Button(this);
        save.setText("保存（重启目标 App 生效）");

        Button export = new Button(this);
        export.setText("导出档案（JSON）");

        Button importBtn = new Button(this);
        importBtn.setText("导入档案（JSON）");

        TextView info = new TextView(this);
        info.setText("trace: " + Trace.PATH_HINT
                + "\ncfg 回退: /sdcard/Download/FuuMod/cfg.json"
                + "\n档案格式：与 cfg.json 相同的字段，可直接互相导入导出");

        root.addView(seed);
        root.addView(gen);
        root.addView(save);
        root.addView(export);
        root.addView(importBtn);
        root.addView(info);
        setContentView(sv);

        gen.setOnClickListener(v -> {
            String s = seed.getText().toString().trim();
            if (s.isEmpty()) {
                Toast.makeText(this, "先填 seed", Toast.LENGTH_SHORT).show();
                return;
            }
            profile = genBySeed(s);
            fillFields();
        });

        save.setOnClickListener(v -> {
            profile = collectProfile();
            CfgStore.writeOwn(this, profile);
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        });

        export.setOnClickListener(v -> createDoc.launch("fuumod-profile.json"));

        importBtn.setOnClickListener(v -> openDoc.launch(new String[]{"application/json", "text/plain", "*/*"}));
    }

    private void fillFields() {
        for (Map.Entry<String, EditText> e : fields.entrySet()) {
            e.getValue().setText(profile.get(e.getKey()));
        }
    }

    /** 把输入框里的当前值收成一个档案。 */
    private DeviceProfile collectProfile() {
        DeviceProfile p = new DeviceProfile();
        for (Map.Entry<String, EditText> e : fields.entrySet()) {
            p.set(e.getKey(), e.getValue().getText().toString().trim());
        }
        if (p.fingerprint == null || p.fingerprint.isEmpty()) p.fingerprint = p.buildFingerprint();
        return p;
    }

    private void writeProfile(Uri uri) {
        try (OutputStream os = getContentResolver().openOutputStream(uri, "wt")) {
            String json = collectProfile().toJson().toString(2);
            if (os == null) throw new IllegalStateException("openOutputStream 返回 null");
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.flush();
            Toast.makeText(this, "已导出", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(this, "导出失败: " + t, Toast.LENGTH_LONG).show();
        }
    }

    private void readProfile(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) throw new IllegalStateException("openInputStream 返回 null");
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = is.read(buf)) > 0) bo.write(buf, 0, n);
            String text = new String(bo.toByteArray(), StandardCharsets.UTF_8).trim();
            if (text.isEmpty()) throw new IllegalStateException("文件为空");
            profile = DeviceProfile.from(new JSONObject(text));
            fillFields();
            Toast.makeText(this, "已导入，点「保存」后生效", Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            Toast.makeText(this, "导入失败: " + t, Toast.LENGTH_LONG).show();
        }
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