package com.qaut.qautjail.utils;

import com.qaut.qautjail.QauTJail;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogManager {

    private final QauTJail plugin;
    private final File logFolder;

    public LogManager(QauTJail plugin) {
        this.plugin = plugin;
        this.logFolder = new File(plugin.getDataFolder(), "logs");

        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }
    }

    /**
     * 📜 تسجيل حدث داخل ملف اليوم الحالي
     *
     * @param type نوع الحدث (مثلاً: JAIL / UNJAIL / DELETE)
     * @param message نص الحدث
     */
    public void logEvent(String type, String message) {
        try {
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            File logFile = new File(logFolder, date + ".log");

            try (FileWriter fw = new FileWriter(logFile, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {

                String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                out.println("[" + time + "] [" + type + "] " + message);
            }

        } catch (IOException e) {
            plugin.getLogger().severe("❌ Failed to write to log file: " + e.getMessage());
        }
    }
}
