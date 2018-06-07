package me.inflamedsebi.cmpf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IniFile {
    class IniSection {
        private HashMap< String, String > _entries = new HashMap<>();
        private String _section = null;
        
        public IniSection(String sectionName){
            this(sectionName, null);
        }
        
        public IniSection(String sectionName, IniSection section){
            if (sectionName == null) throw new NullPointerException();
            if (sectionName.isEmpty()) throw new IllegalArgumentException();
            _section = sectionName;
            if(section != null) for(Entry<String, String > entry : section.entrySet())
                _entries.put(entry.getKey(), entry.getValue());
        }
        
        public String getName () {
            return _section;
        }
        
        public IniSection copy() {
        	return new IniSection(_section, this);
        }
        
        public void put (String key, String value){
            if (key == null) throw new NullPointerException();
            if (key == "") throw new IllegalArgumentException();
            if (value == null) _entries.remove(key);
            else _entries.put( key, value );
        }
        
        public void remove (String key){
            put (key, null);
        }
        
        public String getString (String key) {
            return _entries.get(key);
        }
        
        public boolean containsKey(String key) {
            return _entries.containsKey(key);
        }
        
        public Set<Entry<String, String>> entrySet() {
            return _entries.entrySet();
        }
        
        public Set<String> keySet() {
            return _entries.keySet();
        }
    }

    private HashMap< String, IniSection> _entries  = new HashMap<>();

    public IniFile( File path ) throws IOException {
        load( path );
    }
    
    public IniFile() {
    }

    public void load( File path ) throws IOException {
        Pattern  _section  = Pattern.compile( "\\s*\\[([^]]+)\\]\\s*" );
        Pattern  _keyValue = Pattern.compile( "\\s*([^=]+)=(.+)" );
        try( BufferedReader br = new BufferedReader( new FileReader( path ))) {
            String line;
            String section = null;
            while(( line = br.readLine()) != null ) {
                Matcher m = _section.matcher( line );
                if( m.matches()) section = m.group( 1 ).trim();
                else if( section != null ) {
                    m = _keyValue.matcher( line );
                    if( m.matches()) {
                        String key   = m.group( 1 ).trim();
                        String value = m.group( 2 ).trim();
                        IniSection s = _entries.get( section );
                        if(!key.isEmpty() && !value.isEmpty()) {
	                        if( s == null ) {
	                            _entries.put( section, s = new IniSection(section));   
	                        }
	                        s.put( key, value );
                        }
                    }
                }
            }
        }
    }
    
    public void save( File file ) throws IOException {
        try( BufferedWriter br = new BufferedWriter( new FileWriter( file ))) {
            for(IniSection section : _entries.values()){
                if(section == null) continue;
                br.write("["+section.getName()+"]");
                br.newLine();
                for(Entry<String, String > entry : section.entrySet()){
                    br.write(entry.getKey()+"="+entry.getValue());
                    br.newLine();
                }
            }
        }
    }

    public void putSection (IniSection section){
        if (section == null) throw new NullPointerException();
        else _entries.put( section.getName(), section );
    }
    
    public void removeSection (String section){
        _entries.remove( section );
    }
    
    public IniSection getSection (String section){
        return _entries.get( section );
    }

    public Set<String> getSections (){
        return _entries.keySet();
    }
    

    public boolean containsSection(String section) {
        return _entries.containsKey(section);
    }
    
    public void put (String section, String key, String value){
        IniSection s = _entries.get( section );
        if( s == null ) _entries.put( section, s = new IniSection( section ));   
        s.put( key, value );
    }
    
    public void remove (String section, String key){
        put ( section, key, null);
    }
    
    public String getString (String section, String key) {
        return getString(section, key, null);
    }
    
    public String getString( String section, String key, String defaultvalue ) {
        IniSection s = _entries.get( section );
        if( s == null || !s.containsKey(key)) {
            return defaultvalue;
        }
        return s.getString( key );
    }
    
    public boolean containsKey(String section, String key) {
        return getString(section, key)  != null;
    }
    
}