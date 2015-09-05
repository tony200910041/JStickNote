import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.filechooser.*;
import java.util.*;
import java.io.*;
import java.nio.file.Files;
import myjava.gui.*;
import myjava.gui.common.*;

public class JStickNote extends JDialog implements ActionListener, Resources
{
	//constants
	private static final String VERSION_NO = "1.0";
	private static final String BETA_NO = "";
	private static final boolean isTraySupported = SystemTray.isSupported();
	//size constants
	private static final Rectangle env = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
	private static final Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();
	static final int winWidth = env.width;
	static final int winHeight = env.height;
	static final int scrWidth = scrSize.width;
	static final int scrHeight = scrSize.height;
	//lists
	static ArrayList<TextFrame> list = new ArrayList<TextFrame>(10);
	static ArrayList<SerializableTextFrame> disposed = new ArrayList<>(10);
	static JDialog w;	
	//settings
	static final Properties prop = new Properties();
	static final File defaultData = new File(getSettingFilePath(), "JSTICKNOTE.JN2");
	static final File settings = new File(getSettingFilePath(), "JSTICKNOTEPREF.PROPERTIES");
	static MyWhiteFileChooser chooser = MyWhiteFileChooser.getInstance();
	
	public static void main(final String[] args)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				JStickNote.setUI();
				chooser.setFileFilter(new FileNameExtensionFilter("JStickNote data", new String[]{"jn2","jsn"}));
				w = new JStickNote(args);
			}
		});
	}
	
	JStickNote(String[] args)
	{
		super((JFrame)null, "JStickNote " + VERSION_NO, false);
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent ev)
			{
				if (isTraySupported)
				{
					JStickNote.this.setVisible(false);
				}
				else
				{
					JStickNote.exit();
				}
			}
		});
		this.setLayout(new GridLayout(4,1,0,0));
		//
		JPanel P1 = new JPanel();
		P1.add(new MyDialogButton("Create New", 1));
		P1.add(new MyDialogButton("Hide All", 2));
		P1.add(new MyDialogButton("Show All", 3));
		JPanel P2 = new JPanel();
		P2.add(new MyDialogButton("Restore", 4));
		P2.add(new MyDialogButton("Save", 5));
		P2.add(new MyDialogButton("Hierarchy", 12));
		JPanel P3 = new JPanel();
		P3.add(new MyDialogButton("Import", 7));
		P3.add(new MyDialogButton("Export", 8));
		P3.add(new MyDialogButton("Option", 6));
		JPanel P4 = new JPanel();
		P4.add(new MyDialogButton("About", 9));
		P4.add(new MyDialogButton("Close dialog", 10));
		P4.add(new MyDialogButton("Close", 11));
		this.add(P1);
		this.add(P2);
		this.add(P3);
		this.add(P4);
		//
		this.pack();
		this.setLocationRelativeTo(null);
		this.setResizable(false);
		this.setAlwaysOnTop(true);
		this.createSystemTray();
		this.initialize(args);
		this.setIconImages(getIconImageList());
	}
	
	void createSystemTray()
	{
		//system tray:
		try
		{
			if (isTraySupported)
			{
				JPopupMenu trayMenu = new JPopupMenu();
				//add tray menu items:
				trayMenu.add(new MyTrayMenuItem("Show dialog", 0));
				trayMenu.add(new JPopupMenu.Separator());
				trayMenu.add(new MyTrayMenuItem("Create new note", "NEW", 1));
				trayMenu.add(new MyTrayMenuItem("Hide all notes", 2));
				trayMenu.add(new MyTrayMenuItem("Show all notes", 3));					
				trayMenu.add(new MyTrayMenuItem("Restore disposed notes", 4));
				trayMenu.add(new MyTrayMenuItem("Save notes", 5));
				trayMenu.add(new MyTrayMenuItem("Change hierarchy",12));
				trayMenu.add(new MyTrayMenuItem("Options", 6));
				trayMenu.add(new JPopupMenu.Separator());
				trayMenu.add(new MyTrayMenuItem("Import notes", 7));
				trayMenu.add(new MyTrayMenuItem("Export notes", 8));
				trayMenu.add(new MyTrayMenuItem("About JStickNote", "APPICON16", 9));
				trayMenu.add(new JPopupMenu.Separator());		
				trayMenu.add(new MyTrayMenuItem("Close JStickNote", 11));
				//create system tray:
				MyTrayIcon trayIcon = new MyTrayIcon(getIconImage(),"JStickNote " + VERSION_NO,trayMenu);
				trayIcon.setImageAutoSize(true);
				trayIcon.addMouseListener(new MouseAdapter()
				{
					@Override
					public void mouseReleased(MouseEvent ev)
					{
						if (!ev.isPopupTrigger())
						{
							if (isAllNotesShown())
							{
								hideAndSaveAllNotes();
							}
							else
							{
								showAllNotes();
							}
						}
					}
				});
				SystemTray.getSystemTray().add(trayIcon);
			}
		}
		catch (Exception ex)
		{
		}
	}
	
	void initialize(String[] args)
	{
		//args: include user settings input
		//read serialized frame:
		for (int i=0; i<args.length; i++)
		{
			args[i] = args[i].toLowerCase().replace("/","-");
		}
		ArrayList<String> parameters = new ArrayList<>(Arrays.asList(args));
		if (defaultData.exists())
		{
			if (!parameters.contains("-startup")||!isTraySupported)
			{
				SerializableTextFrame.readNotes(defaultData,true);
				this.setVisible(true);
			}
			else
			{
				SerializableTextFrame.readNotes(defaultData,false);
			}
			//backup:
			if (!parameters.contains("-nobackup"))
			{
				File dir = new File(settings.getParentFile(),"JStickNote_Backup");
				if (!dir.exists())
				{
					dir.mkdir();
				}
				int x = 1;
				while (new File(dir,"JSTICKNOTE_BACKUP" + x + ".JN2").exists())
				{
					x++;
				}
				File dest = new File(dir,"JSTICKNOTE_BACKUP" + x + ".JN2");
				/*
				 * copy file:
				 */
				JStickNote.copy(defaultData,dest);
			}
		}
		else //create file
		{
			File defaultDataOld = new File(defaultData.getParent(), "JSTICKNOTE.JSN");
			if (defaultDataOld.exists()&&(JOptionPane.showConfirmDialog(null, "Would you like to import the old notes (Notes created by version 1.0alpha)?", "Import reminder", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION))
			{
				SerializableTextFrame.readNotes(defaultDataOld,true);
				defaultDataOld.renameTo(new File(defaultDataOld.getParent(), "JSTICKNOTE_OLD_NOTES.JSN"));
			}
			else
			{
				//add "welcome" notes
				TextFrame welcome = new TextFrame("Welcome!");
				welcome.textArea.setText("Welcome to JStickNote " + VERSION_NO + BETA_NO + "!\n\nJStickNote is a cross-platform application that allows users to create unlimited desktop notes and show them neatly on the screen.\n\nEnjoy!");
				welcome.setVisible(true);
			}
			SerializableTextFrame.writeNotes(defaultData); //data file
			this.setVisible(true);
		}
		if (!settings.exists())
		{
			try (PrintWriter writer = new PrintWriter(settings,"UTF-8"))
			{
			}
			catch (IOException ex)
			{
			}
			writeConfig0("Color","random");
			saveConfig();
		}
		else loadConfig();
	}
	
	class MyTrayMenuItem extends JMenuItem implements Indexable
	{
		private int x;
		MyTrayMenuItem(String text, int x)
		{
			super(text);
			this.setFont(f13);
			this.addActionListener(JStickNote.this);
			this.x = x;
		}
		
		MyTrayMenuItem(String text, String iconName, int x)
		{
			this(text,x);
			try
			{
				this.setIcon(new ImageIcon(JStickNote.class.getResource("/SRC/" + iconName + ".PNG")));
			}
			catch (Exception ex)
			{
			}
		}
		
		@Override
		public int getIndex()
		{
			return this.x;
		}
	}
	
	class MyDialogButton extends MyButton implements Indexable
	{
		private int x;
		MyDialogButton(String str, int x)
		{
			super(str);
			this.setPreferredSize(new Dimension(80,28));
			this.addActionListener(JStickNote.this);
			this.x = x;
		}
		
		@Override
		public int getIndex()
		{
			return this.x;
		}
	}
	
	interface Indexable
	{
		int getIndex();
	}
		
	@Override
	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent ev)
	{
		outSwitch:
		switch (((Indexable)(ev.getSource())).getIndex())
		{
			case 0: //show dialog
			this.setVisible(true);
			break;
			
			case 1: //create new
			{
				(new TextFrame("Note "+TextFrame.count)).setVisible(true);					
			}
			break;
			
			case 2: //hide all notes
			JStickNote.hideAndSaveAllNotes();
			break;
			
			case 3: //show all notes
			JStickNote.showAllNotes();
			break;
			
			case 4: //restore
			{
				final JDialog dialog = new JDialog((Frame)null,"Disposed notes",false);
				dialog.setLayout(new BorderLayout());
				dialog.setAlwaysOnTop(true);
				dialog.setIconImages(getIconImageList());
				final DefaultListModel<SerializableTextFrame> lm = new DefaultListModel<>();
				final JList<SerializableTextFrame> l = new JList<>(lm);
				l.setFont(f13);
				l.setCellRenderer(new TextFrameCellRenderer());
				JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
				JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
				dialog.add(top, BorderLayout.PAGE_START);
				dialog.add(new JScrollPane(l), BorderLayout.CENTER);
				dialog.add(bottom, BorderLayout.PAGE_END);
				for (SerializableTextFrame frame: disposed)
				{
					lm.addElement(frame);
				}
				bottom.add(new MyButton("Restore")
				{
					@Override
					public void mouseReleased(MouseEvent ev)
					{
						java.util.List<SerializableTextFrame> frameList = l.getSelectedValuesList();
						for (SerializableTextFrame frame: frameList)
						{
							JStickNote.disposed.remove(frame);
							TextFrame textFrame = new TextFrame(frame);
							JStickNote.list.add(textFrame);
							lm.removeElement(frame);
							textFrame.setVisible(true);
						}
					}
				});
				bottom.add(new MyButton("Delete")
				{
					@Override
					public void mouseReleased(MouseEvent ev)
					{
						if (JOptionPane.showConfirmDialog(dialog,"This will delete the selected note(s) permanently.\nContinue?","Confirm", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
						{
							int i;
							while ((i=l.getSelectedIndex()) != -1)
							{
								lm.remove(i);
								disposed.remove(i);
							}
						}
					}
				});
				bottom.add(new MyButton("Refresh")
				{
					@Override
					public void mouseReleased(MouseEvent ev)
					{
						lm.removeAllElements();
						for (SerializableTextFrame frame: disposed)
						{
							lm.addElement(frame);
						}
					}
				});
				bottom.add(new MyButton("Cancel")
				{
					@Override
					public void mouseReleased(MouseEvent ev)
					{
						dialog.dispose();
					}
				});
				dialog.setSize(300,250);
				dialog.setResizable(false);
				dialog.setLocationRelativeTo(null);
				dialog.setVisible(true);
			}
			break;
			
			case 5: //save
			SerializableTextFrame.writeNotes(defaultData);
			break;
			
			case 6: //option
			{
				loadConfig();
				final JDialog dialog = new JDialog((Frame)null, "JStickNote options", true);
				dialog.setAlwaysOnTop(true);
				dialog.setLayout(new FlowLayout());
				dialog.getContentPane().setBackground(Color.WHITE);
				dialog.setIconImages(getIconImageList());
				boolean _isRandom = ("random").equals(getConfig0("Color"));
				final MyRadioButton isRandom = new MyRadioButton("Use random color", _isRandom);
				final MyRadioButton isDefined = new MyRadioButton("Use a color:", !_isRandom);
				final JPanel previewColor = new JPanel();
				ButtonGroup group = new ButtonGroup();
				group.add(isRandom);
				group.add(isDefined);
				dialog.add(isRandom);
				dialog.add(isDefined);
				dialog.add(previewColor);
				dialog.add(new MyButton("Choose")
				{
					{
						this.setPreferredSize(new Dimension(60,28));
					}
					@Override
					public void actionPerformed(ActionEvent ev)
					{
						final JColorChooser chooser = new JColorChooser(previewColor.getBackground());
						JDialog chooserDialog = null;
						ActionListener ok = new ActionListener()
						{
							@Override
							public void actionPerformed(ActionEvent ev)
							{
								previewColor.setBackground(chooser.getColor());
							}
						};
						ActionListener cancel = new ActionListener()
						{
							@Override
							public void actionPerformed(ActionEvent ev)
							{
							}
						};
						chooserDialog = JColorChooser.createDialog(dialog,"Choose a color:",true,chooser,ok,cancel);
						chooserDialog.pack();
						chooserDialog.setLocationRelativeTo(dialog);
						chooserDialog.setIconImages(getIconImageList());
						chooserDialog.setVisible(true);
					}
				});
				try
				{
					previewColor.setBackground(new Color(Integer.parseInt(getConfig0("Color"))));
				}
				catch (Exception ex)
				{
					previewColor.setBackground(Color.WHITE);
				}
				previewColor.setBorder(bord1);
				previewColor.setPreferredSize(new Dimension(40,28));
				dialog.pack();
				dialog.setLocationRelativeTo(null);
				dialog.setIconImages(getIconImageList());
				dialog.setResizable(false);
				dialog.setVisible(true);
				dialog.setVisible(false);
				if (isRandom.isSelected())
				{
					writeConfig0("Color","random");
				}
				else
				{
					Color color = previewColor.getBackground();
					writeConfig0("Color",color.getRGB()+"");
				}
				saveConfig();
			}
			break;
			
			case 7: //import
			{
				int option = chooser.showOpenDialog(w);
				if (option == JFileChooser.APPROVE_OPTION)
				{
					SerializableTextFrame.readNotes(chooser.getSelectedFile(),true);
				}
			}
			break;
			
			case 8: //export
			{
				boolean isOverride = false;
				File file;
				outDoWhile:
				do
				{
					int option = chooser.showSaveDialog(w);
					if (option == JFileChooser.APPROVE_OPTION)
					{
						file = chooser.getSelectedFile();
						String path = file.getPath().toLowerCase();
						if ((!path.endsWith(".jsn"))&&(!path.endsWith(".jn2")))
						{
							file = new File(file.getParent(), file.getName()+".jn2");
						}
						if (file.exists())
						{
							isOverride = (JOptionPane.showConfirmDialog(null, file.getPath() + " has already exists.\nOverride old file?", "Override", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION);
						}
						else break outDoWhile;
					}
					else return;
				} while (!isOverride);
				SerializableTextFrame.writeNotes(file);
				SerializableTextFrame.writeNotes(defaultData);
				file = null;
			}
			break;
			
			case 9: //about
			{
				JOptionPane pane = new JOptionPane("JStickNote " + VERSION_NO + BETA_NO + " -- a simple desktop notes manager written in Java.\nBy tony200910041.\nDistributed under MPL 2.0.", JOptionPane.INFORMATION_MESSAGE);
				pane.setIcon(JStickNote.getIcon());
				JDialog dialog = pane.createDialog(null, "About JStickNote");
				dialog.setAlwaysOnTop(true);
				dialog.setModal(true);
				dialog.setVisible(true);
			}
			break;
			
			case 10: //close dialog
			this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
			break;		
							
			case 11: //close
			JStickNote.exit();
			break;
			
			case 12: //hierarchy
			{
				final JDialog dialog = new JDialog((Dialog)null,"Hierarchy dialog",true);
				final DefaultListModel<TextFrame> lm = new DefaultListModel<TextFrame>();
				final JList<TextFrame> l = new JList<TextFrame>(lm);
				l.setFont(f13);
				l.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				ArrayList<TextFrame> cloned = (ArrayList<TextFrame>)(JStickNote.list.clone());
				Collections.reverse(cloned);
				for (TextFrame frame: cloned)
				{
					lm.addElement(frame); //1st: top
				}
				l.setCellRenderer(new TextFrameCellRenderer());
				dialog.add(new JScrollPane(l), BorderLayout.CENTER);
				JPanel P0 = new JPanel(new FlowLayout(FlowLayout.CENTER));
				P0.add(new MyButton("Up")
				{
					@Override
					public void mouseReleased(MouseEvent ev)
					{
						TextFrame frame = l.getSelectedValue();
						int index = l.getSelectedIndex();
						lm.set(index,lm.getElementAt(index-1));
						lm.set(index-1,frame);
						l.setSelectedIndex(index-1);
						l.ensureIndexIsVisible(index-1);
						
					}
				});
				P0.add(new MyButton("Down")
				{
					@Override
					public void mouseReleased(MouseEvent ev)
					{
						TextFrame frame = l.getSelectedValue();
						int index = l.getSelectedIndex();
						lm.set(index,lm.getElementAt(index+1));
						lm.set(index+1,frame);
						l.setSelectedIndex(index+1);
						l.ensureIndexIsVisible(index+1);
					}
				});
				P0.add(new MyButton("Done")
				{
					@Override
					public void mouseReleased(MouseEvent ev)
					{
						dialog.setVisible(false);
						Object o[] = lm.toArray();
						for (Object obj: o)
						{
							((TextFrame)obj).setVisible(false);
						}
						ArrayList<TextFrame> tmpList = new ArrayList<TextFrame>();
						for (int i=o.length-1; i>=0; i--)
						{
							TextFrame frame = (TextFrame)(o[i]);
							frame.setVisible(true);
							tmpList.add(frame);
						}
						list = tmpList;
					}
				});
				P0.add(new MyButton("Cancel")
				{
					@Override
					public void mouseReleased(MouseEvent ev)
					{
						dialog.setVisible(false);
					}
				});
				dialog.add(P0, BorderLayout.PAGE_END);
				dialog.setSize(new Dimension(300,300));
				dialog.setLocationRelativeTo(null);
				dialog.setAlwaysOnTop(true);
				dialog.setIconImages(getIconImageList());
				dialog.setVisible(true);
			}
			break;
		}
	}
	
	static void copy(File original, File dest)
	{
		try
		{
			Files.copy(original.toPath(), dest.toPath());
		}
		catch (Throwable ex)
		{
			FileInputStream input = null;
			FileOutputStream output = null;
			try
			{
				input = new FileInputStream(original);
				output = new FileOutputStream(dest);
				byte[] buffer = new byte[1024];
				int read;
				while ((read = input.read(buffer))>0)
				{
					output.write(buffer, 0, read);
				}
			}
			catch (Exception ex2)
			{
				exception(ex2);
			}
			finally
			{
				try
				{
					input.close();
				}
				catch (IOException ex3)
				{
					exception(ex3);
				}
				try
				{
					output.close();
				}
				catch (IOException ex4)
				{
					exception(ex4);
				}
			}
		}
	}
	
	static void exception(Exception ex)
	{
		JOptionPane.showMessageDialog(null, "Error!\nException type: " + ex.getClass().getName() + "\nException message: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
	}
	
	static String getSettingFilePath()
	{
		try
		{			
			return (new File(JStickNote.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())).getParentFile().getPath();
		}
		catch (Exception ex)
		{
			return null;
		}
	}
	
	static ImageIcon getIcon()
	{
		try
		{
			return new ImageIcon(JStickNote.class.getResource("/SRC/APPICON.PNG"));
		}
		catch (Exception ex)
		{			
			JOptionPane.showMessageDialog(null,"Failed to load the icon file.\nJStickNote executable may be corrupted.","Error",JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}
	
	static Image getIconImage()
	{
		return getIcon().getImage();
	}
	
	static ArrayList<Image> getIconImageList()
	{
		ArrayList<Image> iconList = new ArrayList<>(2);
		iconList.add(getIconImage());
		try
		{
			iconList.add(new ImageIcon(JStickNote.class.getResource("/SRC/APPICON16.PNG")).getImage());
		}
		catch (Exception ex)
		{
		}
		return iconList;
	}
	
	static void showAllNotes()
	{
		for (TextFrame frame: list)
		{
			frame.setVisible(true);
		}
	}
	
	static void hideAndSaveAllNotes()
	{
		for (TextFrame frame: list)
		{
			frame.setVisible(false);
		}
		SerializableTextFrame.writeNotes(defaultData);
	}
	
	static boolean isAllNotesShown()
	{
		for (TextFrame frame: list)
		{
			if (!frame.isVisible()) return false;
		}
		return true;
	}
	
	static void exit()
	{
		SerializableTextFrame.writeNotes(defaultData);
		System.exit(0);
	}
	
	static void setUI()
	{
		UIManager.put("OptionPane.buttonFont", f13);
		UIManager.put("OptionPane.messageFont", f13);
		UIManager.put("OptionPane.yesButtonText", "YES");
		UIManager.put("OptionPane.noButtonText", "NO");
		UIManager.put("OptionPane.okButtonText", "OK");
		UIManager.put("OptionPane.cancelButtonText", "Cancel");
		UIManager.put("PopupMenu.background",Color.WHITE);
		UIManager.put("Menu.background",Color.WHITE);
		UIManager.put("MenuItem.background",Color.WHITE);
		UIManager.put("RadioButtonMenuItem.background",Color.WHITE);
		UIManager.put("ToolTip.background",Color.WHITE);
		UIManager.put("Button.background",Color.WHITE);
		UIManager.put("Menu.font",f13);
		UIManager.put("MenuItem.font",f13);
		UIManager.put("RadioButtonMenuItem.font",f13);
		UIManager.put("ToolTip.font",f13);
		UIManager.put("TextField.font",f13);
		UIManager.put("TabbedPane.font", f13);
		UIManager.put("Button.font", f13);
		UIManager.put("Label.font", f13);
		UIManager.put("RadioButton.font", f13);
		UIManager.put("TitledBorder.font", f13);
		UIManager.put("FormattedTextField.font", f13);
	}
	
	static void loadConfig()
	{
		try
		{
			prop.load(new FileInputStream(settings));
			
		}
		catch (Exception ex)
		{
			JStickNote.exception(ex);
		}
	}
	
	static String getConfig0(String name)
	{
		return prop.getProperty(name);		
	}
	
	static void writeConfig0(String key, String value)
	{
		prop.setProperty(key, value);
	}
	
	static void saveConfig()
	{
		try
		{
			prop.store(new FileOutputStream(settings), null);
		}
		catch (Exception ex)
		{
		}
	}
	
	static class TextFrameCellRenderer extends DefaultListCellRenderer
	{
		TextFrameCellRenderer()
		{
			super();
		}
		
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			JLabel label = (JLabel)(super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus));
			if (value instanceof TextFrame)
			{
				TextFrame frame = (TextFrame)value;
				label.setText(frame.label.getText() + " -- " + frame.textArea.getText());
			}
			else if (value instanceof SerializableTextFrame)
			{
				SerializableTextFrame frame = (SerializableTextFrame)value;
				label.setText(frame.title);
			}
			return label;
		}
	}	
}

class TextFrame extends JFrame implements MouseListener, MouseMotionListener, Resources
{
	static final Clipboard clipBoard = Toolkit.getDefaultToolkit().getSystemClipboard();
	JTextArea textArea = new JTextArea();
	JScrollPane scrollPane = new JScrollPane(textArea);
	JPopupMenu textPopup = new JPopupMenu();
	JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT,3,2));
	SmallButton close = new SmallButton("\u00D7",1);
	SmallButton minimize = new SmallButton("-",2);
	MyLabel label = new MyLabel();
	JPanel topPanel = new JPanel(new BorderLayout()); //top
	JPopupMenu framePopup = new JPopupMenu();
	JPanel linePanel = new JPanel(new BorderLayout()); //top
	JPanel bottomPanel = new JPanel(new BorderLayout()); //bottom
	private ResizeButton top = new ResizeButton(1);
	private ResizeButton bottom = new ResizeButton(2);
	private ResizeButton left = new ResizeButton(3);
	private ResizeButton right = new ResizeButton(4);
	private ResizeButton topLeft = new ResizeButton(5);
	private ResizeButton topRight = new ResizeButton(6);
	private ResizeButton bottomLeft = new ResizeButton(7);
	private ResizeButton bottomRight = new ResizeButton(8);
	Dimension dimension;
	boolean isMinimized = false;
	int caret;
	// dragging
	private int startMouseX, startMouseY;
	private int startFrameX, startFrameY;
	private Point p;
	private Dimension d;
	// count
	static int count = 1;
	static final int topHeight = 25;
	TextFrame(String title)
	{
		this();
		this.label.setText(title);
		this.setSize(160,220);
		this.setLocation(48*(count%5),48*(count%5));
		Color background;
		String color = JStickNote.getConfig0("Color");
		if (("random").equals(color))
		{
			background = createRandomColor();
		}
		else
		{
			try
			{
				background = new Color(Integer.parseInt(color));
			}
			catch (Exception ex)
			{
				background = createRandomColor();
				JStickNote.writeConfig0("Color", "random");
			}
		}		
		this.textArea.setBackground(background);
		this.topPanel.setBackground(background);
		JStickNote.list.add(this);
		count++;
	}
	
	TextFrame(SerializableTextFrame w)
	{
		this();
		this.setSize(Math.min(w.d.width,JStickNote.winWidth),Math.min(w.d.height,JStickNote.winHeight));
		this.setLocation(Math.min(w.p.x,JStickNote.winWidth),Math.min(w.p.y,JStickNote.winHeight));
		this.label.setText(w.title);
		this.textArea.setText(w.text);
		this.textArea.setBackground(w.background);
		this.topPanel.setBackground(w.background);
		if (w.isMinimized)
		{
			this.minimize();
		}
		if (w instanceof SerializableTextFramev1)
		{
			this.caret = ((SerializableTextFramev1)w).caret;
		}
		else
		{
			this.caret = 0;
		}
	}
	
	private TextFrame()
	{
		super();
		this.setType(JFrame.Type.UTILITY);
		this.setUndecorated(true);
		try
		{
			this.setOpacity(0.95f);
		}
		catch (Exception ex)
		{
		}
		this.setAlwaysOnTop(true);
		this.setLayout(new BorderLayout());
		// top
		linePanel.setPreferredSize(new Dimension(0,2));
		linePanel.add(topLeft, BorderLayout.LINE_START);
		linePanel.add(top, BorderLayout.CENTER);
		linePanel.add(topRight, BorderLayout.LINE_END);
		topPanel.add(linePanel, BorderLayout.PAGE_START);
		topPanel.add(bar, BorderLayout.CENTER);
		bar.add(close);
		bar.add(minimize);
		bar.add(label);
		bar.setBorder(bord1);
		bar.setPreferredSize(new Dimension(0,topHeight));
		bar.setOpaque(false);
		textArea.setFont(f13);
		textArea.setDragEnabled(true);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.addMouseListener(this);
		this.add(topPanel, BorderLayout.PAGE_START);
		// center text area
		this.add(scrollPane, BorderLayout.CENTER);
		// bottom
		bottomPanel.add(bottomLeft, BorderLayout.LINE_START);
		bottomPanel.add(bottom, BorderLayout.CENTER);
		bottomPanel.add(bottomRight, BorderLayout.LINE_END);
		this.add(bottomPanel, BorderLayout.PAGE_END);
		this.add(left, BorderLayout.LINE_START);
		this.add(right, BorderLayout.LINE_END);
		// popup
		framePopup.add(new MyMenuItem("Minimize/Restore", 1));
		framePopup.add(new MyMenuItem("Dispose", 2));
		framePopup.add(new JPopupMenu.Separator());
		framePopup.add(new MyMenuItem("Set title", 3));
		framePopup.add(new MyMenuItem("Set background", 4));
		framePopup.add(new JPopupMenu.Separator());
		framePopup.add(new MyMenuItem("Stick to top", 5));
		framePopup.add(new MyMenuItem("Stick to bottom", 6));
		// popup
		textPopup.add(new MyMenuItem("Cut","CUT",-1));
		textPopup.add(new MyMenuItem("Copy","COPY",-2));
		textPopup.add(new MyMenuItem("Paste","PASTE",-3));
		textPopup.add(new MyMenuItem("Paste on next line",-4));
		textPopup.add(new JPopupMenu.Separator());
		textPopup.add(new MyMenuItem("Select all",-5));
		textPopup.add(new MyMenuItem("Delete",-6));
		// others
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.label.addMouseListener(this);
		this.label.addMouseMotionListener(this);
		this.bar.addMouseListener(this);
		this.bar.addMouseMotionListener(this);
	}
	
	@Override
	public void setVisible(boolean b)
	{
		super.setVisible(b);
		if (b)
		{
			//show:
			this.textArea.setCaretPosition(caret);
		}
		else
		{
			caret = this.textArea.getCaretPosition();
		}
	}
	
	@Override
	public void setSize(int x1, int y1)
	{
		super.setSize(x1,y1);
		this.dimension = new Dimension(x1,y1);
	}
	
	@Override
	public void setSize(Dimension x1)
	{
		super.setSize(x1);
		this.dimension = new Dimension(x1);
	}
	
	public void minimize()
	{
		try
		{
			this.bar.remove(close);
			this.bar.remove(minimize);
			this.remove(scrollPane);
			this.topPanel.remove(linePanel);
			this.remove(bottomPanel);
		}
		catch (Exception ex)
		{
		}
		super.setSize(this.getWidth(),topHeight);
		this.revalidate();
		this.repaint();
		this.isMinimized = true;
	}
	
	public void restoreSize()
	{
		this.setSize(this.dimension);
		this.add(scrollPane, BorderLayout.CENTER);
		this.topPanel.add(linePanel, BorderLayout.PAGE_START);
		this.bar.add(close,0);	
		this.bar.add(minimize,1);			
		this.minimize.setBorder(bord1);
		this.add(bottomPanel, BorderLayout.PAGE_END);		
		this.revalidate();
		this.repaint();
		this.isMinimized = false;
		Point p = this.getLocation();
		Dimension d = this.getSize();
		if (p.x+d.width>JStickNote.winWidth)
		{
			this.setLocation(JStickNote.winWidth-d.width,p.y);
		}
		if (p.y+d.height>JStickNote.winHeight)
		{
			this.setLocation(p.x,JStickNote.winHeight-d.height);
		}
	}
	
	@Override
	public void mousePressed(MouseEvent ev)
	{
		p = this.getLocation();
		d = this.getSize();
		startFrameX = (int)p.getX();
		startFrameY = (int)p.getY();
		if (ev.getSource() instanceof TextFrame||ev.getSource() instanceof MyLabel||ev.getSource() instanceof JPanel)
		{
			startMouseX = ev.getXOnScreen();
			startMouseY = ev.getYOnScreen();				
		}
		else if (ev.getSource() instanceof ResizeButton)
		{
			ResizeButton src = (ResizeButton)(ev.getSource());
			src.startMouseX = ev.getXOnScreen();
			src.startMouseY = ev.getYOnScreen();
		}
	}
	
	@Override
	public synchronized void mouseDragged(MouseEvent ev)
	{
		if (ev.getSource() instanceof TextFrame||ev.getSource() instanceof MyLabel||ev.getSource() instanceof JPanel)
		{
			int newX = startFrameX+ev.getXOnScreen()-startMouseX;
			if (newX<0) newX=0;
			if (newX+this.getWidth()>JStickNote.winWidth) newX = JStickNote.winWidth-this.getWidth();
			int newY = startFrameY+ev.getYOnScreen()-startMouseY;
			if (newY<0) newY=0;
			if (newY+this.getHeight()>JStickNote.winHeight) newY = JStickNote.winHeight-this.getHeight();
			this.setLocation(newX, newY);
		}
		else if (ev.getSource() instanceof ResizeButton)
		{
			if (this.isMinimized) return;
			ResizeButton src = (ResizeButton)(ev.getSource());
			int x1=0,y1=0,width=0,height=0;
			switch (src.x)
			{
				case 1: //top
				{
					int dy = ev.getYOnScreen()-p.y;
					x1 = p.x;
					width = d.width;
					if (d.height-dy>=100)
					{
						y1 = p.y+dy;
						height = d.height-dy;
					}
					else return;
				}
				break;
				
				case 2: //bottom
				{
					int dy = ev.getYOnScreen()-(p.y+d.height);
					x1 = p.x;
					width = d.width;
					if (d.height+dy>=100)
					{
						y1 = p.y;
						height = d.height+dy;
					}
					else return;
				}
				break;
				
				case 3: //left
				{
					int dx = ev.getXOnScreen()-(p.x);
					y1 = p.y;
					height = d.height;
					if (d.width-dx>=80)
					{
						x1 = p.x+dx;
						width = d.width-dx;
					}
					else return;
				}
				break;
				
				case 4: //right
				{
					int dx = ev.getXOnScreen()-(p.x+d.width);
					y1 = p.y;
					height = d.height;
					if (d.width+dx>=80)
					{
						x1 = p.x;
						width = d.width+dx;
					}
					else return;
				}
				break;
				
				case 5: //topLeft
				{
					int dx = ev.getXOnScreen()-p.x;
					int dy = ev.getYOnScreen()-p.y;
					if (d.width-dx>=80)
					{
						x1 = p.x+dx;
						width = d.width-dx;
					}
					else
					{
						x1 = (int)this.getLocation().getX();
						width = 80;
					}
					if (d.height-dy>=100)
					{
						y1 = p.y+dy;
						height = d.height-dy;
					}
					else
					{
						y1 = (int)this.getLocation().getY();
						height = 100;
					}
				}
				break;
				
				case 6: //topRight
				{
					int dx = ev.getXOnScreen()-(p.x+d.width);
					int dy = ev.getYOnScreen()-p.y;
					if (d.width+dx>=80)
					{
						x1 = p.x;
						width = d.width+dx;
					}
					else
					{
						x1 = (int)this.getLocation().getX();
						width = 80;
					}
					if (d.height-dy>=100)
					{
						y1 = p.y+dy;
						height = d.height-dy;
					}
					else
					{
						y1 = (int)this.getLocation().getY();
						height = 100;
					}
				}
				break;
				
				case 7: //bottomLeft
				{
					int dx = ev.getXOnScreen()-p.x;
					int dy = ev.getYOnScreen()-(p.y+d.height);
					if (d.width-dx>=80)
					{
						x1 = p.x+dx;
						width = d.width-dx;
					}
					else
					{
						x1 = (int)this.getLocation().getX();
						width = 80;
					}
					if (d.height+dy>=100)
					{
						y1 = p.y;
						height = d.height+dy;
					}
					else
					{
						y1 = (int)this.getLocation().getY();
						height = 100;
					}
				}
				break;
				
				case 8: //bottomRight
				{
					int dx = ev.getXOnScreen()-(p.x+d.width);
					int dy = ev.getYOnScreen()-(p.y+d.height);
					if (d.width+dx>=80)
					{
						x1 = p.x;
						width = d.width+dx;
					}
					else
					{
						x1 = (int)this.getLocation().getX();
						width = 80;
					}
					if (d.height+dy>=100)
					{
						y1 = p.y;
						height = d.height+dy;
					}
					else
					{
						y1 = (int)this.getLocation().getY();
						height = 100;
					}
				}
				break;
			}
			this.setSize(width,height);
			this.setLocation(x1,y1);
		}
	}
	
	@Override
	public void mouseReleased(MouseEvent ev)
	{
		if (ev.getSource() instanceof SmallButton)
		{
			SmallButton src = (SmallButton)(ev.getSource());
			switch (src.x)
			{
				case 1: //close
				JStickNote.list.remove(this);
				this.setVisible(false);
				JStickNote.disposed.add(new SerializableTextFrame(this));
				break;
				
				case 2: //minimize
				if (this.getSize().equals(dimension))
				{
					this.minimize();
				}
				else
				{
					this.restoreSize();
				}
				break;
			}
		}
		else if (ev.getSource() instanceof MyMenuItem)
		{
			//textarea
			int x = ((MyMenuItem)(ev.getSource())).x;
			switch (-x)
			{
				case 1: //cut
				clipBoard.setContents(new StringSelection(this.textArea.getSelectedText()), null);
				this.textArea.replaceSelection(null);
				break;
				
				case 2: //copy
				clipBoard.setContents(new StringSelection(this.textArea.getSelectedText()), null);
				break;
				
				case 3: //paste
				{
					String str = null;
					try
					{
						str = clipBoard.getData(DataFlavor.stringFlavor).toString();
					}
					catch (Exception ex)
					{
						JStickNote.exception(ex);
					}
					if (str != null)
					{
						this.textArea.insert(str, textArea.getCaretPosition());
					}
				}
				break;
				
				case 4: //paste on next line
				{
					String str = null;
					try
					{
						str = clipBoard.getData(DataFlavor.stringFlavor).toString();
					}
					catch (Exception ex)
					{
						JStickNote.exception(ex);
					}
					if (str != null)
					{
						this.textArea.insert("\n"+str, this.textArea.getCaretPosition());
					}
				}
				break;
				
				case 5: //select all
				this.textArea.selectAll();
				break;
				
				case 6: //delete
				this.textArea.replaceSelection(null);
				break;
			}
			//frame
			switch (x)
			{
				case 1: //minimize
				if (this.isMinimized)
				{
					this.restoreSize();
				}
				else
				{
					this.minimize();
				}
				break;
				
				case 2: //dispose
				JStickNote.list.remove(this);
				this.dispose();
				JStickNote.disposed.add(new SerializableTextFrame(this));
				break;
				
				case 3: //set title
				this.label.dispatchEvent(new MouseEvent(label,MouseEvent.MOUSE_CLICKED,0,0,0,0,2,false));
				break;
				
				case 4: //set background
				{
					final JColorChooser chooser = new JColorChooser(TextFrame.this.textArea.getBackground());
					JDialog chooserDialog = null;
					ActionListener ok = new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent ev)
						{
							Color c = chooser.getColor();
							TextFrame.this.topPanel.setBackground(c);
							TextFrame.this.textArea.setBackground(c);
						}
					};
					ActionListener cancel = new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent ev)
						{
						}
					};
					chooserDialog = JColorChooser.createDialog(TextFrame.this,"Choose a color:",true,chooser,ok,cancel);
					chooserDialog.pack();
					chooserDialog.setLocationRelativeTo(TextFrame.this);
					chooserDialog.setIconImages(JStickNote.getIconImageList());
					chooserDialog.setVisible(true);
				}
				break;
				
				case 5: //stick to top
				this.minimize();
				this.setLocation((int)this.getLocation().getX(),0);
				break;
				
				case 6: //stick to bottom
				this.minimize();
				this.setLocation((int)this.getLocation().getX(),JStickNote.winHeight-topHeight);
				break;
			}
		}
		else if (ev.getSource() instanceof JPanel||ev.getSource() instanceof MyLabel)
		{
			if (ev.isPopupTrigger())
			{
				framePopup.show((JComponent)ev.getSource(),ev.getX(),ev.getY());
			}
		}
		else if (ev.getSource() instanceof JTextArea)
		{
			if (ev.isPopupTrigger())
			{
				textPopup.show(textArea,ev.getX(),ev.getY());
			}
		}
	}
	
	@Override
	public void mouseClicked(MouseEvent ev)
	{
		if ((ev.getSource() instanceof MyLabel)&&(ev.getClickCount()==2))
		{
			JLabel label = (JLabel)(ev.getSource());
			Object str = JOptionPane.showInputDialog(this, "Enter a title:", "Title", JOptionPane.QUESTION_MESSAGE, null,null,label.getText());
			if (str != null)
			{
				if (str.toString().isEmpty())
				{
					label.setText("Note");
				}
				else
				{
					label.setText(str.toString());
				}
			}				
		}
		if ((ev.getSource() instanceof JPanel)&&(ev.getClickCount()==2))
		{
			if (this.isMinimized) this.restoreSize();
			else this.minimize();
		}
	}
	
	@Override
	public void mouseEntered(MouseEvent ev)
	{
		if (ev.getSource() instanceof SmallButton)
		{
			((SmallButton)(ev.getSource())).setBorder(bord2);
		}
	}
	
	@Override
	public void mouseExited(MouseEvent ev)
	{
		if (ev.getSource() instanceof SmallButton)
		{
			((SmallButton)(ev.getSource())).setBorder(bord1);
		}
	}
	
	@Override
	public void mouseMoved(MouseEvent ev)
	{
	}
	
	class MyMenuItem extends JMenuItem
	{
		int x;
		MyMenuItem(String text, int x)
		{
			super(text);
			this.setFont(f13);
			this.setBackground(Color.WHITE);
			this.x = x;
			this.addMouseListener(TextFrame.this);
		}
		
		MyMenuItem(String text, String iconName, int x)
		{
			this(text,x);
			try
			{
				this.setIcon(new ImageIcon(JStickNote.class.getResource("/SRC/" + iconName + ".PNG")));
			}
			catch (Exception ex)
			{
			}
		}
	}
	
	class SmallButton extends JButton
	{
		int x;
		public SmallButton(String text, int x)
		{
			super(text);
			this.setPreferredSize(new Dimension(20,20));
			this.setBackground(Color.WHITE);
			this.setBorder(bord1);
			this.setFocusPainted(false);
			this.addMouseListener(TextFrame.this);
			this.x = x;
		}
	}
	
	class ResizeButton extends JButton
	{
		int x;
		int startMouseX, startMouseY;
		public ResizeButton(int x)
		{
			this.x = x;
			this.setBackground(Color.WHITE);
			this.setFocusPainted(false);
			this.setPreferredSize(new Dimension(2,2));
			this.addMouseListener(TextFrame.this);
			this.addMouseMotionListener(TextFrame.this);
			int cursor = Cursor.DEFAULT_CURSOR;
			switch (x)
			{
				case 1:
				case 2:
				cursor = Cursor.N_RESIZE_CURSOR;
				break;
				
				case 3:
				case 4:
				cursor = Cursor.E_RESIZE_CURSOR;
				break;
				
				case 5:
				case 8:
				cursor = Cursor.NW_RESIZE_CURSOR;
				break;
				
				case 6:
				case 7:
				cursor = Cursor.NE_RESIZE_CURSOR;
				break;
			}
			this.setCursor(Cursor.getPredefinedCursor(cursor));
		}
	}
	
	static Color createRandomColor()
	{
		return new Color((int)(Math.random()*55+200),(int)(Math.random()*55+200),(int)(Math.random()*55+200));
	}
}

class SerializableTextFrame implements Serializable
{
	private static final long serialVersionUID = 4078318378884514704L;
	String title;
	String text;
	Color background;
	Point p;
	Dimension d;
	boolean isMinimized;
	SerializableTextFrame(TextFrame w)
	{
		this.title = w.label.getText();
		this.text = w.textArea.getText();
		this.background = w.textArea.getBackground();
		this.p = w.getLocation();
		this.d = w.dimension;
		this.isMinimized = w.isMinimized;
	}
	
	SerializableTextFrame()
	{
	}
	
	static void writeNotes(File file)
	{
		try
		{
			String path = file.getPath().toLowerCase();
			if (path.endsWith("jsn"))
			{
				ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
				ArrayList<SerializableTextFramev1> array = new ArrayList<SerializableTextFramev1>();
				for (TextFrame frame: JStickNote.list)
				{
					array.add(new SerializableTextFramev1(frame));
				}
				oos.writeObject(array);
				oos.writeObject(JStickNote.disposed);
				oos.close();
			}
			else //"jn2"
			{
				Properties p = new Properties();
				/*
				 * save frame list
				 */
				int frameSize = JStickNote.list.size();
				for (int i=0; i<frameSize; i++)
				{
					SerializableTextFramev1 serial = new SerializableTextFramev1(JStickNote.list.get(i));
					p.setProperty("frame."+i+".title", serial.title);
					p.setProperty("frame."+i+".text", serial.text);
					p.setProperty("frame."+i+".background", serial.background.getRGB()+"");
					p.setProperty("frame."+i+".p.x", serial.p.x+"");
					p.setProperty("frame."+i+".p.y", serial.p.y+"");
					p.setProperty("frame."+i+".d.width", serial.d.width+"");
					p.setProperty("frame."+i+".d.height", serial.d.height+"");
					p.setProperty("frame."+i+".isMinimized", serial.isMinimized+"");
					p.setProperty("frame."+i+".caret", serial.caret+"");
				}
				/*
				 * save disposed list
				 */
				int disposedSize = JStickNote.disposed.size();
				for (int i=0; i<disposedSize; i++)
				{
					SerializableTextFrame serial = JStickNote.disposed.get(i);
					p.setProperty("disposed."+i+".title", serial.title);
					p.setProperty("disposed."+i+".text", serial.text);
					p.setProperty("disposed."+i+".background", serial.background.getRGB()+"");
					p.setProperty("disposed."+i+".p.x", serial.p.x+"");
					p.setProperty("disposed."+i+".p.y", serial.p.y+"");
					p.setProperty("disposed."+i+".d.width", serial.d.width+"");
					p.setProperty("disposed."+i+".d.height", serial.d.height+"");
					p.setProperty("disposed."+i+".isMinimized", serial.isMinimized+"");
					if (serial instanceof SerializableTextFramev1)
					{
						p.setProperty("disposed."+i+".caret", ((SerializableTextFramev1)serial).caret+"");
					}
				}
				p.store(new BufferedOutputStream(new FileOutputStream(file)), "JStickNote data: DO NOT MODIFY");
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			JStickNote.exception(ex);
		}
	}
	
	@SuppressWarnings("unchecked")
	static void readNotes(File file, boolean visible)
	{
		try
		{
			String path = file.getPath().toLowerCase();
			if (path.endsWith("jsn"))
			{
				ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
				Object read = ois.readObject();
				ArrayList<SerializableTextFrame> array = (ArrayList<SerializableTextFrame>)read;
				for (SerializableTextFrame frame: array)
				{
					TextFrame fr = new TextFrame(frame);
					JStickNote.list.add(fr);
					TextFrame.count++;
					if (visible)
					{
						fr.setVisible(true);
					}
				}
				JStickNote.disposed = (ArrayList<SerializableTextFrame>)(ois.readObject());
				ois.close();
			}
			else
			{
				Properties p = new Properties();
				p.load(new BufferedInputStream(new FileInputStream(file)));
				/*
				 * read frame list
				 */
				int i=0;
				while (p.containsKey("frame."+i+".title"))
				{
					SerializableTextFramev1 serial = readFromProperties(p, "frame."+i);
					TextFrame tf = new TextFrame(serial);
					if (visible)
					{
						tf.setVisible(true);
					}
					JStickNote.list.add(tf);
					i++;
				}
				/*
				 * read disposed list
				 */
				i = 0;
				while (p.containsKey("disposed."+i+".title"))
				{
					SerializableTextFramev1 serial = readFromProperties(p, "disposed."+i);
					JStickNote.disposed.add(serial);
					i++;
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			JStickNote.exception(ex);
		}
	}
	
	static SerializableTextFramev1 readFromProperties(Properties p, String key)
	{
		SerializableTextFramev1 serial = new SerializableTextFramev1();
		serial.title = p.getProperty(key+".title");
		serial.text = p.getProperty(key+".text");
		try
		{
			serial.background = new Color(Integer.parseInt(p.getProperty(key+".background")));
		}
		catch (Exception ex)
		{
			serial.background = new Color(255,255,255);
		}
		try
		{
			int x = Integer.parseInt(p.getProperty(key+".p.x"));
			int y = Integer.parseInt(p.getProperty(key+".p.y"));
			serial.p = new Point(x,y);
		}
		catch (Exception ex)
		{
			serial.p = new Point(0,0);
		}
		try
		{
			int width = Integer.parseInt(p.getProperty(key+".d.width"));
			int height = Integer.parseInt(p.getProperty(key+".d.height"));
			serial.d = new Dimension(width,height);
		}
		catch (Exception ex)
		{
			serial.d = new Dimension(160,220);
		}
		serial.isMinimized = ("true").equals(p.getProperty(key+".isMinimized"));
		try
		{
			serial.caret = Integer.parseInt(p.getProperty(key+".caret"));
		}
		catch (Exception ex)
		{
			serial.caret = 0;
		}
		return serial;
	}
}

class SerializableTextFramev1 extends SerializableTextFrame
{
	/*
	 * New class: caret added
	 */
	private static final long serialVersionUID = 3019144428862973590L;
	int caret;
	SerializableTextFramev1(TextFrame w)
	{
		super(w);
		this.caret = w.textArea.getCaretPosition();
	}
	
	SerializableTextFramev1()
	{
		super();
	}
}
