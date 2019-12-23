package me.inflamedsebi.cmpf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;

import me.inflamedsebi.cmpf.IniFile.IniSection;

public class Helper {

    private static Helper _h = null;

    public Helper() {
        _h = this;
    }

    public static Helper get() {
        if (_h == null)
            new Helper();
        return _h;
    }

    public void deleteFolderContent(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolderContent(f);
                }
                f.delete();

            }
        }
    }

    private Object _logger;
    private Method _log;

    public void registerLogger(Object logger, String method, Class<?>... args) {
        this._logger = logger;
        try {
            this._log = logger.getClass().getDeclaredMethod(method, args);
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
    }

    public void log(Object... args) {
        try {
            _log.invoke(_logger, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logException(Exception e) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        printWriter.flush();

        _h.log(writer.toString());
        printWriter.close();
    }

    public String requestURL(String url) {
        String result = null;
        try {
            HttpURLConnection con =  (HttpURLConnection) new URL(url).openConnection();
            con.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36");
            con.addRequestProperty("Upgrade-Insecure-Requests", "1");
            con.addRequestProperty("Sec-Fetch-User", "?1");
            con.addRequestProperty("Accept", "*/*");
            @SuppressWarnings("resource")
            Scanner scanner = new Scanner(con.getInputStream(), StandardCharsets.UTF_8.toString());
            scanner.useDelimiter("\\A");
            result = scanner.hasNext() ? scanner.next() : null;
        } catch (Exception e) {
            _h.log("[ERR] Could not download '" + url + ".\n");
            _h.log(e.getMessage()+"\n");
        }
        return result;
    }

    public int getMaxPages(String pageContent) {
        // use any page to get max number of pages
        Matcher m = Pattern.compile("(?:href=\\\"[^\\\"]*?\\?page=)(\\d{1,4})").matcher(pageContent);
        ArrayList<String> matches = new ArrayList<String>();
        while (m.find()) {
            matches.add(m.group(1));
        }
        int max = 0;
        for (String match : matches) {
            try {
                int i = Integer.parseInt(match);
                if (max < i)
                    max = i;
            } catch (Exception e) {
                _h.log("[ERR] Could not get page count.\n");
            }
        }
        return max;
    }

    public ArrayList<String> getDependents(String mod, boolean[] canceling) throws IOException {

        String url = "https://www.curseforge.com/minecraft/mc-mods/" + mod + "/relations/dependents?page=";
        int page = 1;
        String pageContent = _h.requestURL(url + page);
        if (pageContent == null) {
            _h.log("[ERR] Invalid mod '" + mod + "'?\n");
            canceling[0] = true;
            return null;
        }
        int max = _h.getMaxPages(pageContent);
        if (max == 0) {
            _h.log("[ERR] Failed parsing page count.\n");
            canceling[0] = true;
            return null;
        }

        // clear cache if max pages are not consistent anymore

        File file = new File("cache/" + mod + "/" + max + ".cache");
        ArrayList<String> matches = new ArrayList<String>();
        // read cached file from lokal FS
        if (file.exists()) {
            _h.log("[...] Loading cache for '" + mod + "'.\n");
            BufferedReader br = new BufferedReader(new FileReader(file));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (!line.isEmpty())
                    matches.add(line);
            }
            br.close();
            _h.log("[OK] Loaded cache for '" + mod + "'.\n");
            return matches;
        }
        // no valid file so get it online
        file.getParentFile().mkdirs();
        _h.deleteFolderContent(file.getParentFile());
        _h.log("[...] Downloading lists for' " + mod + "'.\n");
        do {
            _h.log("[...] " + page + "/" + max + "'.\n");
            // get page x contents
            if (page > 1)
                pageContent = _h.requestURL(url + page);
            // get all links to project sites except the current one and extract project names
            // <a href="https://www.curseforge.com/minecraft/modpacks/whatever"  class="bg-white">
            Matcher m = Pattern.compile("(?:href=\\\".*?\\/modpacks\\/)(?!" + mod + ")([a-z-]*?)(?:\\\" class=\\\"bg-white\\\">)").matcher(pageContent);
            while (m.find()) {
            	//TODO filter duplicates?
                matches.add(m.group(1));
            }
        } while (page++ < max && !canceling[0]);
        if (canceling[0]) {
            return null;
        }
        // now cache the results
        BufferedWriter br = new BufferedWriter(new FileWriter(file));
        for (String line : matches) {
            br.write(line);
            br.newLine();
        }
        br.close();
        if (matches.isEmpty()) {
            _h.log("[ERR] No occurences found for '" + mod + "'.\n");
            canceling[0] = true;
            return null;
        } else
            _h.log("[OK] Downloaded and cache created for '" + mod + "' ( " + matches.size() + " occurences).\n");
        return matches;
    }

    public void addMeta(IniSection section) {
        String pageContent = _h.requestURL(section.getString("link"));
        Matcher m = Pattern.compile("(?:href=\\\"[^\\\"]*?\\\">\\s*?Minecraft\\s)(\\d\\.\\d{1,2})").matcher(pageContent);
        ArrayList<String> tempResult = new ArrayList<String>();
        while (m.find()) {
            tempResult.add(m.group(1));
        }
        section.put("versions", String.join(";", tempResult));

        m = Pattern.compile("(?:data-epoch=\\\")(\\d+)(?:\\\")").matcher(pageContent);
        tempResult.clear();
        while (m.find()) {
            tempResult.add(m.group(1));
        }

        long max = 0;
        for (String match : tempResult) {
            try {
                long i = Long.parseLong(match);
                if (max < i)
                    max = i;
            } catch (Exception e) {
                _h.log("[ERR] Failed to find date.");
            }
        }
        Date last = new Date(max * 1000);
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        String reportDate = df.format(last);

        section.put("updated", reportDate);

        m = Pattern.compile("(?:Total\\sDownloads<\\/div>\\s*<div\\sclass=\\\"info-data\\\">)([^<]*)(?:<\\/div>)").matcher(pageContent);
        while (m.find()) {
            section.put("downloads", m.group(1));
        }
    }

    public void updateList(JComponent list) {
        @SuppressWarnings("unchecked")
        DefaultListModel<String> jlm = (DefaultListModel<String>) ((JList<String>) list).getModel();
        File f = new File("cache");
        jlm.clear();
        if (f.exists() && f.isDirectory()) {
            for (File dir : f.listFiles())
                if (dir.isDirectory())
                    if (dir.listFiles().length > 0)
                        jlm.addElement(dir.getName() + "&O");
                    else
                        jlm.addElement(dir.getName() + "&X");
        }
    }
}
