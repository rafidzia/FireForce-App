package com.example.fireforce;
import android.content.Context;
import java.io.File;

public class AppUtils {
    public static void delete(File file, boolean deleteDir) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    delete(f, true);
                }
            }

            if (deleteDir) {
                file.delete();
            }
        } else {
            file.delete();
        }
    }

    public static void clearData(Context context) {
        File files = context.getDir("tmp", Context.MODE_PRIVATE);
        delete(files.getParentFile(), false);
    }

}
