package mg.mgmap.application;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.TreeSet;

public class BaseConfig {

    public enum Mode { NORMAL, UNIT_TEST, INSTRUMENTATION_TEST, SYSTEM_TEST }

    private final String appDirName;
    private final String preferencesName;
    private final SharedPreferences sharedPreferences;
    private final Mode mode;

    public BaseConfig(String appDirName, String preferencesName, SharedPreferences sharedPreferences, Mode mode) {
        this.appDirName = appDirName;
        this.preferencesName = preferencesName;
        this.sharedPreferences = sharedPreferences;
        this.mode = mode;
    }

    public String getAppDirName() {
        return appDirName;
    }

    public String getPreferencesName() {
        return preferencesName;
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public Mode getMode() {
        return mode;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("BaseConfig{" +
                "appDirName='" + appDirName + '\'' +
                ", preferencesName='" + preferencesName + '\'' +
                ", #sharedPreferences='" + sharedPreferences.getAll().size() + '\'' +
                ", mode=" + mode);
        Map<String, ?> sp = sharedPreferences.getAll();
        for (String key :new TreeSet<>( sp.keySet())){
            res.append("\n        ").append(key).append("='").append(sp.get(key)).append("'");
        }
        res.append(" }");
        return res.toString();
    }
}
