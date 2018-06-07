package me.inflamedsebi.cmpf;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JTextArea;

import me.inflamedsebi.cmpf.IniFile.IniSection;

public class Eventlistener implements ActionListener {
	
	private Helper h = Helper.get();
	private Integer state = 0;
	private Boolean canceling = false;
	/*
	 * 0 idle fresh start/reseted
	 * 1 (down)loading stuff
	 * 2 matching done
	 * 3 idle waiting for meta
	 * 4 (down)loading meta
	 * 5 idle meta done
	 * 
	 */
	
	private IniFile resultIni = new IniFile();
	private IniFile cacheIni = new IniFile();
	
	private HashMap<String, JComponent> components = new HashMap<String, JComponent>();
	
	private JComponent getComponent(String cmp) {
		return components.get(cmp);
	}
	
	private String getComponent(JComponent cmp) {
		for(Entry<String, JComponent> entry : components.entrySet()) {
			if(cmp == entry.getValue()) return entry.getKey();
		}
		return null;
	}
	

	@Override
	public void actionPerformed(ActionEvent evt) {
		JComponent src = (JComponent) evt.getSource();
		try {
			//JOptionPane.showMessageDialog(null, components.get(src));
			Method method = this.getClass().getDeclaredMethod(getComponent(src));
			Object obj = this;
			Thread t = new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						method.invoke(obj);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						e.printStackTrace();
					}					
				}
			});
			t.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void initialize(){
		h.registerLogger(getComponent("output"), "append", String.class);
		DefaultListModel<String> jlm = (DefaultListModel<String>) ((JList<String>) getComponent("list")).getModel();
		File f = new File("cache");
		if(f.exists()) {
			for (File dir : f.listFiles())
				if(dir.isDirectory())
					if(dir.listFiles().length > 0) jlm.addElement(dir.getName()+"&O");
					else jlm.addElement(dir.getName()+"&X");
		}
	}
	
	public void registerComponentEvent(JComponent cmp, String name) {
		components.put(name, cmp);
	}

	public void reset() {
		
	}
	
	public void next() {
		switch(state) {
		case 0:
			((JMenuItem) getComponent("next")).setText("Cancel");
			downloadAndMatch();
			break;
		case 1:
			((JMenuItem) getComponent("next")).setText("Start");
			canceling = true;
			break;
		case 2:
			((JMenuItem) getComponent("next")).setText("Fetch Meta");
			state = 3;
			break;
		case 3:
			((JMenuItem) getComponent("next")).setText("Cancel");
			fetchMeta();
			break;
		case 4:
			((JMenuItem) getComponent("next")).setText("Fetch Meta");
			canceling = true;
			break;
		case 5:
			((JMenuItem) getComponent("next")).setText("Start");
			state = 0;
			resultIni = new IniFile();
			break;
		}
	}
	
	public void fetchMeta() {
		state = 4;
		h.log("[...] Fetch Meta from cache or download.\n");
	    File file = new File("cache/cache.ini");
	    file.getParentFile().mkdirs();
	    if(file.exists()) try {
			cacheIni.load(file);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		for(String section : resultIni.getSections()) {
			if(cacheIni.containsKey(section, "versions") && cacheIni.containsKey(section, "updated")) {
				h.log("[...] Cache '"+section+"'.\n");
				resultIni.putSection(cacheIni.getSection(section).copy());
			} else {
				h.log("[...] Download '"+section+"'.\n");
				IniSection is = resultIni.getSection(section);
				h.addMeta(is);
				cacheIni.putSection(is.copy());
			}
		    if(canceling) {
		    	h.log("[OK] Cancelled by user.");
				state = 3;
		    	canceling = false;
				try {
					cacheIni.save(file);
				} catch (IOException e) {
					h.logException(e);
				}
				h.log("[OK] Meta saved to cache.\n");
		    	return;
		    }
		}
		try {
			cacheIni.save(file);
		} catch (IOException e) {
			h.logException(e);
		}
		h.log("[OK] Meta saved to cache.\n");
		
		IniFile old = resultIni;
		resultIni = new IniFile();
		for(String section : old.getSections()) {
			IniSection is = old.getSection(section);
			String[] versions = is.getString("versions").split(";");
			if (versions != null && versions.length > 0) for(String version : versions)
				if (!version.isEmpty()) resultIni.put(version, section, is.getString("updated")+"-"+is.getString("link"));
		}
		h.log("[OK] Rebuilt results.\n");

	    file = new File("results.ini");
		try {
			resultIni.save(file);
		} catch (IOException e) {
			h.logException(e);
		}
		h.log("[OK] Meta exported to results.ini.\n");
		
		state = 5;
		next();
	}

	public void downloadAndMatch() {
    	state = 1;
		ArrayList<String> modlist = new ArrayList<String>();
		JTextArea area = (JTextArea) getComponent("modlist");
		String text = area.getText();
		for (String line : text.split("\n")) {
			String mod = "";
			Matcher m = Pattern.compile("(?:.*?\\/)([a-z-]*)").matcher(line);
			if(!m.matches()) {
				m = Pattern.compile("([a-z-]*)").matcher(line);
				if(!m.matches()) {
					h.log("[ERR] Skipping: '"+line+"' wrong format\n");
					continue;
				} else mod = m.group(1);
			} else mod = m.group(1);
			
			h.log("[OK] Found: '"+mod+"'\n");
			modlist.add(mod);
		}

		
		ArrayList<String> consistencies = null;
		if (modlist.size()>1) for(String mod : modlist){
		    ArrayList<String> dependents = null;
			try {
				dependents = h.getDependents(mod, canceling);
			} catch (IOException e) {
				e.printStackTrace();
			}
		    if(canceling) {
		    	state = 0;
		    	canceling = false;
		    	return;
		    }
		    if (consistencies == null) consistencies = dependents;
		    else {
		        ArrayList<String> newConsistencies = new ArrayList<String>();
		        for(String item : consistencies){
		            if(dependents.contains(item)) newConsistencies.add(item);
		        }
		        consistencies = newConsistencies;
		    }
		} else {
			h.log("[ERR] Too few entries.\n");
			state = 0;
			return;
		}
		
		if (consistencies != null && !consistencies.isEmpty()) {
			h.log("[...] We got matches:\n");
			String url = "https://minecraft.curseforge.com/projects/";
			for(String con : consistencies) {
				h.log("[...] "+con+"\n");
				resultIni.put(con, "link", url+con);
			}	
			h.log("[OK] Thats it.\n");
		    File file = new File("results.ini");
			try {
				resultIni.save(file);
			} catch (IOException e) {
				h.logException(e);
			}
			h.log("[OK] Exported to results.ini.\n");
		} else {
			h.log("[ERR] No matches :(\n");
			state = 0;
			return;
		}
		state = 2;
		next();
	}

	@SuppressWarnings("unchecked")
	public void mouseClicked(MouseEvent evt) {
		JList<String> jl = (JList<String>) getComponent("list");
		if(evt.getSource() != getComponent("list") || evt.getClickCount() != 2) return;
		((JTextArea) getComponent("modlist")).append(jl.getSelectedValue().split("&")[0]+"\n");
	}
}
