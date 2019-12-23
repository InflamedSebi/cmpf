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

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JTextArea;

import me.inflamedsebi.cmpf.IniFile.IniSection;

public class Eventlistener implements ActionListener {

    private Helper _h = Helper.get();
    private int[] _state = { 0 };
    private boolean[] _canceling = { false };

    private IniFile _resultIni = new IniFile();
    private IniFile _cacheIni = new IniFile();

    private HashMap<String, JComponent> _components = new HashMap<String, JComponent>();

    private JComponent getComponent(String cmp) {
        return _components.get(cmp);
    }

    private String getComponent(JComponent cmp) {
        for (Entry<String, JComponent> entry : _components.entrySet()) {
            if (cmp == entry.getValue())
                return entry.getKey();
        }
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        JComponent src = (JComponent) evt.getSource();
        try {
            // JOptionPane.showMessageDialog(null, components.get(src));
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

    public void initialize() {
        _h.registerLogger(getComponent("output"), "append", String.class);
        _h.updateList(getComponent("list"));
    }

    public void registerComponentEvent(JComponent cmp, String name) {
        _components.put(name, cmp);
    }

    public void next() {
        if (_canceling[0])
            return;

        switch (_state[0]) {
            case 0:
                downloadAndMatch();
                updateState();
                break;
            case 2:
                fetchMeta();
                updateState();
                break;
            case 1:
            case 3:
                _h.log("[...] Canceling.\n");
                _canceling[0] = true;
                break;
            default:
                _h.log("[ERR] No action assigned.\n");
                break;
        }
    }

    /*
     * 0 idle fresh start/reseted
     * 1 (down)loading lists and join lists
     * 2 stage 1 done (idle)
     * 3 (down)loading meta
     * 4 idle meta done
     */
    public void updateState() {
        if (_canceling[0]) {
            _canceling[0] = false;
            switch (_state[0]) {
                case 1:
                    ((JMenuItem) getComponent("next")).setText("Start");
                    _state[0] = 0;
                    break;
                case 3:
                    ((JMenuItem) getComponent("next")).setText("Fetch Meta");
                    _state[0] = 2;
                    break;
                default:
                    _h.log("[ERR] Unsupported state. [c=true|" + _state[0] + "]\n");
                    break;
            }
            _h.log("[OK] Canceled.");
        } else {
            switch (_state[0]) {
                case 1:
                case 3:
                    ((JMenuItem) getComponent("next")).setText("Cancel");
                    break;
                case 2:
                    ((JMenuItem) getComponent("next")).setText("Fetch Meta");
                    break;
                case 4:
                    ((JMenuItem) getComponent("next")).setText("Restart");
                    _state[0] = 0;
                    _resultIni = new IniFile();
                    break;
                default:
                    _h.log("[ERR] Unsupported state. [c=false|" + _state[0] + "]\n");
                    break;
            }
        }
    }

    public void fetchMeta() {
        _state[0] = 3;
        updateState();
        _h.log("[...] Fetch Meta from cache or download.\n");
        File file = new File("cache/cache.ini");
        file.getParentFile().mkdirs();
        if (file.exists())
            try {
                _cacheIni.load(file);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        int i = 1;
        int max = _resultIni.getSections().size();
        for (String section : _resultIni.getSections()) {
            if (_cacheIni.containsKey(section, "versions") && _cacheIni.containsKey(section, "updated")) {
                _h.log("[...] " + i + "/" + max + " Cache '" + section + "'.\n");
                _resultIni.putSection(_cacheIni.getSection(section).copy());
            } else {
                _h.log("[...] " + i + "/" + max + " Download '" + section + "'.\n");
                IniSection is = _resultIni.getSection(section);
                _h.addMeta(is);
                _cacheIni.putSection(is.copy());
            }
            if (_canceling[0]) {
                try {
                    _cacheIni.save(file);
                } catch (IOException e) {
                    _h.logException(e);
                }
                _h.log("[OK] Meta saved to cache.\n");
                return;
            }
            i++;
        }
        try {
            _cacheIni.save(file);
        } catch (IOException e) {
            _h.logException(e);
        }
        _h.log("[OK] Meta saved to cache.\n");

        IniFile old = _resultIni;
        _resultIni = new IniFile();
        for (String section : old.getSections()) {
            IniSection is = old.getSection(section);
            String[] versions = is.getString("versions").split(";");
            if (versions != null && versions.length > 0)
                for (String version : versions)
                    if (!version.isEmpty())
                        _resultIni.put(version, section, is.getString("updated") + "|" + is.getString("downloads") + "|" + is.getString("link"));
        }
        _h.log("[OK] Rebuilt results.\n");

        file = new File("results.ini");
        try {
            _resultIni.save(file);
        } catch (IOException e) {
            _h.logException(e);
        }
        _h.log("[OK] Meta exported to results.ini.\n");

        _state[0] = 4;
    }

    public void downloadAndMatch() {
        _state[0] = 1;
        updateState();

        ArrayList<String> modlist = new ArrayList<String>();
        JTextArea area = (JTextArea) getComponent("modlist");
        String text = area.getText();
        for (String line : text.split("\n")) {
            String mod = "";
            Matcher m = Pattern.compile("(?:.*?\\/)([a-z-]+)(?!.*\\/.*)").matcher(line);
            if (!m.matches()) {
                m = Pattern.compile("([a-z-]*)").matcher(line);
                if (!m.matches()) {
                    _h.log("[ERR] Skipping: '" + line + "' wrong format\n");
                    continue;
                } else
                    mod = m.group(1);
            } else
                mod = m.group(1);

            _h.log("[OK] looking for: '" + mod + "'\n");
            modlist.add(mod);
        }

        ArrayList<String> consistencies = null;
        if (modlist.size() > 0)
            for (String mod : modlist) {
                ArrayList<String> dependents = null;
                try {
                    dependents = _h.getDependents(mod, _canceling);
                    _h.updateList(getComponent("list"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (_canceling[0])
                    return;

                if (consistencies == null)
                    consistencies = dependents;
                else {
                    ArrayList<String> newConsistencies = new ArrayList<String>();
                    for (String item : consistencies) {
                        if (dependents.contains(item))
                            newConsistencies.add(item);
                    }
                    consistencies = newConsistencies;
                }
            }
        else {
            _h.log("[ERR] Too few entries.\n");
            _canceling[0] = true;
            return;
        }

        if (consistencies != null && !consistencies.isEmpty()) {
            _h.log("[...] We got matches:\n");
            String url = "https://www.curseforge.com/minecraft/mc-mods/";
            for (String con : consistencies) {
                _h.log("[...] " + con + "\n");
                _resultIni.put(con, "link", url + con);
            }
            _h.log("[OK] Thats it.\n");
            File file = new File("results.ini");
            try {
                _resultIni.save(file);
            } catch (IOException e) {
                _h.logException(e);
            }
            _h.log("[OK] Exported to results.ini.\n");
        } else {
            _h.log("[ERR] No matches :(\n");
            _canceling[0] = true;
            return;
        }
        _state[0] = 2;
    }

    @SuppressWarnings("unchecked")
    public void mouseClicked(MouseEvent evt) {
        JList<String> jl = (JList<String>) getComponent("list");
        if (evt.getSource() != getComponent("list") || evt.getClickCount() != 2)
            return;
        ((JTextArea) getComponent("modlist")).append(jl.getSelectedValue().split("&")[0] + "\n");
    }
}
