package myjava.gui;

/**
 * Requires myjava.gui.common.Resources to work
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import myjava.gui.common.Resources;

public class MyButton extends JButton implements ActionListener, MouseListener, Resources
{
	private static Color background = Color.WHITE;
	private static Color foreground = Color.BLACK;
	private static Dimension dimension = new Dimension(50,28);	
	public MyButton(String text)
	{
		super(text);
		this.setFont(f13);
		this.setBorder(bord1);
		this.setBackground(background);
		this.setForeground(foreground);
		this.setPreferredSize(dimension);
		this.setFocusPainted(false);
		this.addMouseListener(this);
		this.addActionListener(this);
	}
	
	public MyButton()
	{
		this("");
	}
	
	public static void setDefaultBackground(Color c)
	{
		MyButton.background = c;
	}
	
	public static void setDefaultForeground(Color c)
	{
		MyButton.foreground = c;
	}
	
	public static void setDefaultDimension(Dimension d)
	{
		MyButton.dimension = d;
	}
	
	@Override
	public void actionPerformed(ActionEvent ev)
	{
	}
	
	@Override
	public void mouseReleased(MouseEvent ev)
	{
	}
	
	@Override
	public void mousePressed(MouseEvent ev)
	{
	}
	
	@Override
	public void mouseClicked(MouseEvent ev)
	{
	}
	
	@Override
	public void mouseEntered(MouseEvent ev)
	{
		if (this.isEnabled()) this.setBorder(bord2);
	}
	
	@Override
	public void mouseExited(MouseEvent ev)
	{
		if (this.isEnabled()) this.setBorder(bord1);
	}
}
