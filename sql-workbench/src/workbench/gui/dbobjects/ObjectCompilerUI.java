/*
 * ObjectCompilerUI.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.WindowListener;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import workbench.db.DbObject;
import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;
import workbench.db.oracle.OracleObjectCompiler;
import workbench.gui.WbSwingUtilities;
import workbench.gui.sql.EditorPanel;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author  Thomas Kellerer
 */
public class ObjectCompilerUI
	extends JPanel
	implements Runnable, WindowListener
{
	private Thread worker;
	protected EditorPanel log;
	private JFrame window;
	private List<DbObject> objects;
	private OracleObjectCompiler compiler;
	private WbConnection dbConnection;

	public ObjectCompilerUI(List<DbObject> objectList, WbConnection conn)
		throws SQLException
	{
		super();
		this.objects = objectList;
		this.dbConnection = conn;
		this.compiler = new OracleObjectCompiler(conn);
		this.setLayout(new BorderLayout());
		this.log = EditorPanel.createTextEditor();
		this.add(this.log, BorderLayout.CENTER);
	}

	private void startCompile()
	{
		this.worker = new WbThread(this, "ObjectCompiler Thread");
		this.worker.start();
	}

	@Override
	public void run()
	{
		String msg = ResourceMgr.getString("TxtCompilingObject");
		this.log.setText("");
		int count = this.objects.size();

		try
		{
			this.dbConnection.setBusy(true);
			for (int i = 0; i < count; i++)
			{
				DbObject o = this.objects.get(i);
				if (i > 0)
				{
					appendLog("\n");
				}

				if (o instanceof ProcedureDefinition && ((ProcedureDefinition)o).isOraclePackage())
				{
					appendLog(msg + " " + o.getCatalog() + "... ");
				}
				else
				{
					appendLog(msg + " " + o.getObjectName() + "... ");
				}

				String error = this.compiler.compileObject(o);
				if (error == null)
				{
					appendLog(ResourceMgr.getString("TxtOK"));
				}
				else
				{
					appendLog(ResourceMgr.getString("TxtError") + "\n  " + error.replaceAll(StringUtil.REGEX_CRLF, "\n    "));
				}
			}
		}
		finally
		{
			this.dbConnection.setBusy(false);
		}
		this.log.setCaretPosition(0);
	}

	private void appendLog(final String msg)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				log.appendLine(msg);
			}
		});
	}

	public void show(Window aParent)
	{
		if (this.window == null)
		{
			this.window = new JFrame(ResourceMgr.getString("TxtWindowTitleCompileStatus"));
			this.window.getContentPane().setLayout(new BorderLayout());
			this.window.getContentPane().add(this, BorderLayout.CENTER);

			ResourceMgr.setWindowIcons(window, "compile");

			if (!Settings.getInstance().restoreWindowSize(this.window, ObjectCompilerUI.class.getName()))
			{
				this.window.setSize(500, 400);
			}

			if (!Settings.getInstance().restoreWindowPosition(this.window, ObjectCompilerUI.class.getName()))
			{
				WbSwingUtilities.center(this.window, aParent);
			}
			this.window.addWindowListener(this);
		}
		this.window.setVisible(true);
		this.startCompile();
	}

	@Override
	public void windowActivated(java.awt.event.WindowEvent e)
	{
	}

	@Override
	public void windowClosed(java.awt.event.WindowEvent e)
	{
	}

	@Override
	public void windowClosing(java.awt.event.WindowEvent e)
	{
		if (this.worker != null)
		{
			this.worker.interrupt();
			this.compiler = null;
			this.worker = null;
		}
		Settings.getInstance().storeWindowPosition(this.window, ObjectCompilerUI.class.getName());
		Settings.getInstance().storeWindowSize(this.window, ObjectCompilerUI.class.getName());
		this.window.setVisible(false);
		this.window.dispose();
	}

	@Override
	public void windowDeactivated(java.awt.event.WindowEvent e)
	{
	}

	@Override
	public void windowDeiconified(java.awt.event.WindowEvent e)
	{
	}

	@Override
	public void windowIconified(java.awt.event.WindowEvent e)
	{
	}

	@Override
	public void windowOpened(java.awt.event.WindowEvent e)
	{
	}
}
