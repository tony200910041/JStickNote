package myjava.gui;

import java.awt.*;
import javax.swing.*;
import myjava.gui.common.Resources;

public class MyLabel extends JLabel implements Resources
{
	public MyLabel()
	{
		super();
		this.setFont(f13);
		this.setForeground(Color.BLACK);
	}
	
	public MyLabel(String str)
	{
		this();
		this.setText(str);
	}
}
