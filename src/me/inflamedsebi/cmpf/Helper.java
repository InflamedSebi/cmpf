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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.inflamedsebi.cmpf.IniFile.IniSection;

public class Helper {
	
	public static Helper h = null;
	
	public Helper() {
		h = this;
	}
	
	public static Helper get() {
		if(h == null) new Helper();
		return h;
	}
	

	public void deleteFolderContent(File folder) {
	    File[] files = folder.listFiles();
	    if(files!=null) {
	        for(File f: files) {
	            if(f.isDirectory()) {
	            	deleteFolderContent(f);
	            }
	            f.delete();
	
	        }
	    }
	}
	
	Object logger;
	Method log;
	
	public void registerLogger(Object logger, String method, Class<?> ... args) {
		this.logger = logger;
		try {
			this.log = logger.getClass().getDeclaredMethod(method, args);
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
	}
	
	public void log(Object ... args) {
		try {
			log.invoke(logger, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void logException(Exception e) {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter( writer );
		e.printStackTrace( printWriter );
		printWriter.flush();

		h.log( writer.toString());
		printWriter.close();
	}
	
	public String requestURL(String url) {
		String result = null;
	    try (Scanner scanner = new Scanner(new URL(url).openStream(), StandardCharsets.UTF_8.toString()))
	    {
	        scanner.useDelimiter("\\A");
	        result = scanner.hasNext() ? scanner.next() : "";
	    } catch (Exception e) {
			e.printStackTrace();
		}
	    return result;
	}
	
	public int getMaxPages(String pageContent){
	    // use any page to get max number of pages
	    Matcher m = Pattern.compile("(?<=href=\\\"[^\\\"]{0,150}?\\?page=)(\\d{1,4})").matcher(pageContent);
	    ArrayList<String> matches= new ArrayList<String>();
	    while (m.find()) {
	        matches.add(m.group());
	    }
	    int max = 0;
	    for(String match : matches){
	        try{
	        int i = Integer.parseInt(match);
	        if(max<i) max = i;
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	    return max;
	}
	

	
	public ArrayList<String> getDependents(String mod, Boolean canceling) throws IOException {
		
	    String url="https://minecraft.curseforge.com/projects/"+mod+"/relations/dependents?page=";
	    int page = 1;
	    String pageContent = h.requestURL(url+page);
	    int max = h.getMaxPages(pageContent);
	    if(max == 0){
	    	h.log("[ERR] Invalid mod '"+mod+"'.\n");
	        return null;
	    }
	    
	    // clear cache if max pages are not consistent anymore
	    
	    File file = new File("cache/"+mod+"/"+max+".cache");
	    ArrayList<String> matches= new ArrayList<String>();
	    // read cached file from lokal FS
	    if(file.exists()) {
	    	h.log("[...] Loading cache for '"+mod+"'.\n");
	        BufferedReader br = new BufferedReader(new FileReader(file));
	        for(String line = br.readLine(); line != null; line = br.readLine()){
	           if(!line.isEmpty()) matches.add(line); 
	        }
	        br.close();
	        h.log("[OK] Loaded cache for '"+mod+"'.\n");
	        return matches;
	    }
	    // no valid file so get it online
	    file.getParentFile().mkdirs();
	    h.deleteFolderContent(file.getParentFile());
	    h.log("[...] Downloading lists for'"+mod+"'.\n");
	    do {
	        
	    	h.log("[...] "+page+"/"+max+"'.\n");
	        // get page x contents
	        if(page > 1) pageContent = h.requestURL(url+page);
	        // get all links to project sites except the current one and extract project names
	        Matcher m = Pattern.compile("(?<=href=\\\"\\/projects\\/)(?!"+mod+")([a-z-]*?)(?=\\\">)").matcher(pageContent);
	        while (m.find()) {
	            matches.add(m.group());
	        }
	    } while(page++<max && !canceling);
	    if(canceling) {
	    	h.log("[OK] Cancelled by user.");
	    	return null;
	    }
	    // now chache the results
	    BufferedWriter br = new BufferedWriter(new FileWriter(file));
	    for(String line : matches){
	        br.write(line);
	        br.newLine();
	    }
	    br.close();
	    if(matches.isEmpty()) {
	    	h.log("[ERR] No occurences found for '"+mod+"'.\n");
	        return null;
	    } else h.log("[OK] Downloaded and cache created for '"+mod+"' ( "+matches.size()+" occurences).\n");
	    return matches;
	}
	
	public void exportResult(ArrayList<String> matches) {
		String url = "https://minecraft.curseforge.com/projects/";
	    File file = new File("results.rtf");
		BufferedWriter br;
		try {
			br = new BufferedWriter(new FileWriter(file));
		    for(String line : matches){
		        br.write(url+line);
		        br.newLine();
		    }
		        br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addMeta(IniSection section){
		String pageContent = h.requestURL(section.getString("link"));
        Matcher m = Pattern.compile("(?<=href=\\\"[^\\\"]{0,128}?\\\">\\s{0,32}?Minecraft\\s)(\\d\\.\\d{1,2})").matcher(pageContent);
        ArrayList<String> tempResult = new ArrayList<String>();
        while (m.find()) {
        	tempResult.add(m.group());
        }
        section.put("versions", String.join(";", tempResult));
        
        m = Pattern.compile("(?<=data-epoch=\\\")(\\d{8,16})(?=\\\")").matcher(pageContent);
        tempResult.clear();
        while (m.find()) {
        	tempResult.add(m.group());
        }
        
	    long max = 0;
	    for(String match : tempResult){
	        try{
	        long i = Long.parseLong(match);
	        if(max<i) max = i;
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	    Date last = new Date(max * 1000);
	    DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
	    String reportDate = df.format(last);
	    
        section.put("updated", reportDate);
	}
	
}
