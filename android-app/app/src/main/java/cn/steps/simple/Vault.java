package cn.steps.simple;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

final class Vault {
    private final SharedPreferences prefs;
    private static final String ALIAS = "step_submit_credentials_v1";
    Vault(Context c) { this(c,"private_vault"); }
    Vault(Context c,String name) { prefs = c.getSharedPreferences(name, Context.MODE_PRIVATE); }
    private SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if (ks.containsAlias(ALIAS)) return (SecretKey)ks.getKey(ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
    void save(String account, String password) throws Exception {
        JSONObject data = new JSONObject(); data.put("account", account); data.put("password", password);
        saveObject(data);
    }
    void saveObject(JSONObject data) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted = c.doFinal(data.toString().getBytes("UTF-8"));
        if (!prefs.edit().putString("iv", Base64.encodeToString(c.getIV(), Base64.NO_WRAP))
            .putString("data", Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit())
            throw new java.io.IOException("无法保存账号");
    }
    JSONObject load() throws Exception {
        String data = prefs.getString("data", ""); if (data.isEmpty()) return null;
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(prefs.getString("iv", ""), Base64.NO_WRAP)));
        return new JSONObject(new String(c.doFinal(Base64.decode(data, Base64.NO_WRAP)), "UTF-8"));
    }
    void clear() { prefs.edit().clear().commit(); }
}
