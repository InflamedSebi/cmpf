package me.inflamedsebi.cmpf;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JList;

import org.jdesktop.xswingx.PromptSupport;

import com.jgoodies.forms.layout.FormLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.DefaultCaret;

public class Mainframe {
	@SuppressWarnings("serial")
	class MyListRenderer extends DefaultListCellRenderer
	  {
	    public Component getListCellRendererComponent(JList<?> list,Object value,
	                        int index,boolean isSelected,boolean cellHasFocus)
	    {
	    	String[] text = value.toString().split("&");
	    	Color c = Color.BLACK;
	    	if(text.length > 1) {
	    		value = text[0];
	    		//JOptionPane.showMessageDialog(null, text[0]+":"+text[1]);
	    		if (text[1].equals("O")) c = Color.blue.darker();
	    		if (text[1].equals("X")) c = Color.red;
	    	}
	      JLabel lbl = (JLabel)super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
	      lbl.setForeground(c);
	      return lbl;
	    }
	  }

	private JFrame frame;

	/**
	 * Create the application.
	 */
	public Mainframe(Eventlistener evt) {
		initialize(evt);
		evt.initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 * @param evt 
	 */
	private void initialize(Eventlistener evt) {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 493);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new FormLayout("fill:d:grow(0.80), 2px, fill:d:grow(0.2)","12px, 2px, fill:d:grow(0.20), 2px, fill:d:grow(0.80)"));
		
		JLabel jlModlist = new JLabel("Modlist:");
		frame.getContentPane().add(jlModlist, "1, 1, fill, fill");
		
		JTextArea jtaModlist = new JTextArea();
		evt.registerComponentEvent(jtaModlist, "modlist");
		PromptSupport.setPrompt("https://minecraft.curseforge.com/projects/thaumcraft\nthaumcraft", jtaModlist);
        PromptSupport.setFocusBehavior(PromptSupport.FocusBehavior.SHOW_PROMPT, jtaModlist);
		frame.getContentPane().add(jtaModlist, "1, 3, fill, fill");
		
		JScrollPane spOutput = new JScrollPane();
		spOutput.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		spOutput.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		JTextArea jtaOutput = new JTextArea(0,0);
		jtaOutput.setWrapStyleWord(true);
		jtaOutput.setLineWrap(true);
		jtaOutput.setBackground(Color.LIGHT_GRAY);
		jtaOutput.setEditable(false);
		//jtaOutput.setPreferredSize(new Dimension());
		DefaultCaret caret = (DefaultCaret)jtaOutput.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		evt.registerComponentEvent(jtaOutput, "output");
		spOutput.setViewportView(jtaOutput);
		frame.getContentPane().add(spOutput, "1, 5, fill, fill");
		

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		JList<String> jlCache = new JList<String>(new DefaultListModel<String>());
		jlCache.setCellRenderer(new MyListRenderer());
		jlCache.addMouseListener(new MouseAdapter() {
		    public void mouseClicked(MouseEvent e) {
		    	evt.mouseClicked(e);
		    }
		});
		//jtaOutput.setPreferredSize(new Dimension());
		evt.registerComponentEvent(jlCache, "list");
		scrollPane.setViewportView(jlCache);
		frame.getContentPane().add(scrollPane, "3, 3, 1, 3, fill, fill");
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		JMenuItem jmiStart = new JMenuItem("Start");
		jmiStart.addActionListener(evt);
		evt.registerComponentEvent(jmiStart, "next");
		menuBar.add(jmiStart);
		
		
		frame.setVisible(true);
		frame.requestFocusInWindow();
	}

}
