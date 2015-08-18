/* Copyright (C) 2015 American Printing House for the Blind Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.aph.braillezephyr;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class BZMenu
{
	private final Shell shell;
	private String fileName;

	BZMenu(Shell shell)
	{
		this.shell = shell;

		Menu menuBar = new Menu(shell, SWT.BAR);
		shell.setMenuBar(menuBar);

		Menu menu;
		MenuItem item;

		//   file menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&File");
		item.setMenu(menu);

		new NewHandler().addMenuItemTo(menu, "&New", 0);
		new OpenHandler().addMenuItemTo(menu, "&Open\tCtrl+O", SWT.MOD1 | 'o');
		new SaveHandler().addMenuItemTo(menu, "&Save\tCtrl+S", SWT.MOD1 | 's');
		new SaveAsHandler().addMenuItemTo(menu, "Save As\tCtrl+Shift+O", SWT.MOD1 | SWT.MOD2 | 's');

		//   edit menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&Edit");
		item.setMenu(menu);

		//   view menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&View");
		item.setMenu(menu);

		new VisibleHandler(menu);
		new BrailleFontHandler().addMenuItemTo(menu, "Braille Font", 0);
		new AsciiFontHandler().addMenuItemTo(menu, "ASCII Font", 0);

		//   format menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("F&ormat");
		item.setMenu(menu);

		new LinesPerPageHandler(shell).addMenuItemTo(menu, "Lines Per Page", 0);
		new CharsPerLineHandler(shell).addMenuItemTo(menu, "Chars Per Line", 0);
		item = new BellLineMarginHandler(shell).addMenuItemTo(menu, "Bell Margin", 0);
		if(Main.bzStyledText.getBellLineMargin() == -1)
			item.setEnabled(false);
		item = new BellPageMarginHandler(shell).addMenuItemTo(menu, "Bell Page", 0);
		if(Main.bzStyledText.getBellPageMargin() == -1)
			item.setEnabled(false);
		new RewrapFromCursorHandler().addMenuItemTo(menu, "Rewrap From Cursor\tCtrl+F", SWT.MOD1 | 'F');

		//   help menu
		menu = new Menu(menuBar);
		item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&Help");
		item.setMenu(menu);
	}

	private class NewHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			Main.bzStyledText.setText("");
			fileName = null;
			shell.setText("BrailleZephyr");
		}
	}

	private class OpenHandler extends AbstractAction
	{
		@SuppressWarnings("CallToPrintStackTrace")
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			FileDialog dialog = new FileDialog(shell, SWT.OPEN);
			dialog.setFilterExtensions(new String[]{ "*.brf", "*.bzy", "*.brf;*.bzy", "*.*" });
			dialog.setFilterNames(new String[]{ "Braille Ready Format File", "BrailleZephyr File", "Braille Files", "All Files" });
			dialog.setFilterIndex(2);
			String _fileName = dialog.open();
			if(_fileName == null)
				return;

			try
			{
				FileReader fileReader = new FileReader(_fileName);
				if(_fileName.endsWith("bzy"))
					Main.bzStyledText.readBZY(fileReader);
				else
					Main.bzStyledText.readBRF(fileReader);
				fileReader.close();
				shell.setText(new File(_fileName).getName() + " - BrailleZephyr");
			}
			catch(IOException e)
			{
				//TODO:  do something when this happens (everywhere)
				e.printStackTrace();
			}
			finally
			{
				fileName = _fileName;
			}
		}
	}

	private class SaveHandler extends AbstractAction
	{
		@SuppressWarnings("CallToPrintStackTrace")
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			if(fileName == null)
			{
				//   save as handler never uses event object so just pass it this one
				new SaveAsHandler().widgetSelected(event);
				return;
			}
			try
			{
				OutputStreamWriter writer;
				if(fileName.endsWith("brf"))
				{
					writer = new OutputStreamWriter(new FileOutputStream(fileName), Charset.forName("US-ASCII"));
					Main.bzStyledText.writeBRF(writer);
				}
				else if(fileName.endsWith("bzy"))
				{
					writer = new OutputStreamWriter(new FileOutputStream(fileName));
					Main.bzStyledText.writeBZY(writer);
				}
				else
				{
					writer = new OutputStreamWriter(new FileOutputStream(fileName));
					Main.bzStyledText.writeBRF(writer);
				}
				writer.close();
				shell.setText(new File(fileName).getName() + " - BrailleZephyr");
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private class SaveAsHandler extends AbstractAction
	{
		@SuppressWarnings("CallToPrintStackTrace")
		@Override
		public void widgetSelected(SelectionEvent event)
		{
			FileDialog dialog = new FileDialog(shell, SWT.SAVE);
			dialog.setFileName(fileName);
			dialog.setFilterExtensions(new String[]{ "*.brf", "*.bzy", "*.brf;*.bzy", "*.*" });
			dialog.setFilterNames(new String[]{ "Braille Ready Format File", "BrailleZephyr File", "Braille Files", "All Files" });
			dialog.setFilterIndex(2);
			String _fileName = dialog.open();
			if(_fileName == null)
				return;

			try
			{
				OutputStreamWriter writer;
				if(_fileName.endsWith("brf"))
				{
					writer = new OutputStreamWriter(new FileOutputStream(_fileName), Charset.forName("US-ASCII"));
					Main.bzStyledText.writeBRF(writer);
				}
				else if(_fileName.endsWith("bzy"))
				{
					writer = new OutputStreamWriter(new FileOutputStream(_fileName));
					Main.bzStyledText.writeBZY(writer);
				}
				else
				{
					writer = new OutputStreamWriter(new FileOutputStream(_fileName));
					Main.bzStyledText.writeBRF(writer);
				}
				writer.close();
				shell.setText(new File(_fileName).getName() + " - BrailleZephyr");
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				fileName = _fileName;
			}
		}
	}

	private static class VisibleHandler extends SelectionAdapter
	{
		private final MenuItem braille;
		private final MenuItem ascii;

		private VisibleHandler(Menu menu)
		{
			braille = new MenuItem(menu, SWT.PUSH);
			braille.setText("Hide Braille");
			braille.addSelectionListener(this);

			ascii = new MenuItem(menu, SWT.PUSH);
			ascii.setText("Hide ASCII");
			ascii.addSelectionListener(this);
		}

		@Override
		public void widgetSelected(SelectionEvent event)
		{
			if(event.widget == braille)
			{
				if(Main.bzStyledText.getBrailleVisible())
				{
					Main.bzStyledText.setBrailleVisible(false);
					braille.setText("Show Braille");
//					if(!Main.bzStyledText.getAsciiVisible())
//					{
//						Main.bzStyledText.setAsciiVisible(true);
//						ascii.setText("Hide ASCII");
//					}
				}
				else
				{
					Main.bzStyledText.setBrailleVisible(true);
					braille.setText("Hide Braille");
				}
			}
			else
			{
				if(Main.bzStyledText.getAsciiVisible())
				{
					Main.bzStyledText.setAsciiVisible(false);
					ascii.setText("Show ASCII");
//					if(!Main.bzStyledText.getBrailleVisible())
//					{
//						Main.bzStyledText.setBrailleVisible(true);
//						braille.setText("Hide Braille");
//					}
				}
				else
				{
					Main.bzStyledText.setAsciiVisible(true);
					ascii.setText("Hide ASCII");
				}
			}
		}
	}

	private class BrailleFontHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent e)
		{
			FontDialog dialog = new FontDialog(shell, SWT.OPEN);
			dialog.setFontList(Main.bzStyledText.getBrailleFont().getFontData());
			FontData fontData = dialog.open();
			if(fontData == null)
				return;
			Main.bzStyledText.setBrailleFont(new Font(shell.getDisplay(), fontData));
		}
	}

	private class AsciiFontHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent e)
		{
			FontDialog dialog = new FontDialog(shell, SWT.OPEN);
			dialog.setFontList(Main.bzStyledText.getAsciiFont().getFontData());
			FontData fontData = dialog.open();
			if(fontData == null)
				return;
			Main.bzStyledText.setAsciiFont(new Font(shell.getDisplay(), fontData));
		}
	}

	private static class LinesPerPageHandler extends AbstractAction
	{
		private final Shell parent;

		private LinesPerPageHandler(Shell parent)
		{
			this.parent = parent;
		}

		@Override
		public void widgetSelected(SelectionEvent e)
		{
			new LinesPerPageDialog(parent);
		}
	}

	private static class LinesPerPageDialog implements SelectionListener, KeyListener
	{
		private final Shell shell;
		private final Button okButton;
		private final Button cancelButton;
		private final Spinner spinner;

		public LinesPerPageDialog(Shell parent)
		{
			shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			shell.setText("Lines per Page");
			shell.setLayout(new GridLayout(3, true));

			spinner = new Spinner(shell, 0);
			spinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			spinner.setValues(Main.bzStyledText.getLinesPerPage(), 0, 225, 0, 1, 10);
			spinner.addKeyListener(this);

			okButton = new Button(shell, SWT.PUSH);
			okButton.setText("OK");
			okButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			okButton.addSelectionListener(this);

			cancelButton = new Button(shell, SWT.PUSH);
			cancelButton.setText("Cancel");
			cancelButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			cancelButton.addSelectionListener(this);

			shell.pack();
			shell.open();
		}

		private void setLinesPerPage()
		{
			Main.bzStyledText.setLinesPerPage(spinner.getSelection());
			Main.bzStyledText.redraw();
		}

		@Override
		public void widgetSelected(SelectionEvent event)
		{
			if(event.widget == okButton)
				setLinesPerPage();
			shell.dispose();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent event){}

		@Override
		public void keyPressed(KeyEvent event)
		{
			if(event.keyCode == '\r' || event.keyCode == '\n')
			{
				setLinesPerPage();
				shell.dispose();
			}
		}

		@Override
		public void keyReleased(KeyEvent event){}
	}

	private static class CharsPerLineHandler extends AbstractAction
	{
		private final Shell parent;

		private CharsPerLineHandler(Shell parent)
		{
			this.parent = parent;
		}

		@Override
		public void widgetSelected(SelectionEvent e)
		{
			new CharsPerLineDialog(parent);
		}
	}

	private static class CharsPerLineDialog implements SelectionListener, KeyListener
	{
		private final Shell shell;
		private final Button okButton;
		private final Button cancelButton;
		private final Spinner spinner;

		public CharsPerLineDialog(Shell parent)
		{
			shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			shell.setText("Characters Per Line");
			shell.setLayout(new GridLayout(3, true));

			spinner = new Spinner(shell, 0);
			spinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			spinner.setValues(Main.bzStyledText.getCharsPerLine(), 0, 27720, 0, 1, 10);
			spinner.addKeyListener(this);

			okButton = new Button(shell, SWT.PUSH);
			okButton.setText("OK");
			okButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			okButton.addSelectionListener(this);

			cancelButton = new Button(shell, SWT.PUSH);
			cancelButton.setText("Cancel");
			cancelButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			cancelButton.addSelectionListener(this);

			shell.pack();
			shell.open();
		}

		private void setCharsPerLine()
		{
			Main.bzStyledText.setCharsPerLine(spinner.getSelection());
			Main.bzStyledText.redraw();
		}

		@Override
		public void widgetSelected(SelectionEvent event)
		{
			if(event.widget == okButton)
				setCharsPerLine();
			shell.dispose();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent event){}

		@Override
		public void keyPressed(KeyEvent event)
		{
			//TODO:  is the \r necessary?
			if(event.keyCode == '\r' || event.keyCode == '\n')
			{
				setCharsPerLine();
				shell.dispose();
			}
		}

		@Override
		public void keyReleased(KeyEvent event){}
	}

	private static class BellLineMarginHandler extends AbstractAction
	{
		private final Shell parent;

		private BellLineMarginHandler(Shell parent)
		{
			this.parent = parent;
		}

		@Override
		public void widgetSelected(SelectionEvent e)
		{
			new BellLineMarginDialog(parent);
		}
	}

	private static class BellLineMarginDialog implements SelectionListener, KeyListener
	{
		private final Shell shell;
		private final Button okButton;
		private final Button cancelButton;
		private final Spinner spinner;

		public BellLineMarginDialog(Shell parent)
		{
			shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			shell.setText("Bell Margin");
			shell.setLayout(new GridLayout(3, true));

			spinner = new Spinner(shell, 0);
			spinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			spinner.setValues(Main.bzStyledText.getBellLineMargin(), 0, 27720, 0, 1, 10);
			spinner.addKeyListener(this);

			okButton = new Button(shell, SWT.PUSH);
			okButton.setText("OK");
			okButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			okButton.addSelectionListener(this);

			cancelButton = new Button(shell, SWT.PUSH);
			cancelButton.setText("Cancel");
			cancelButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			cancelButton.addSelectionListener(this);

			shell.pack();
			shell.open();
		}

		private void setBellLineMargin()
		{
			Main.bzStyledText.setBellLineMargin(spinner.getSelection());
			Main.bzStyledText.redraw();
		}

		@Override
		public void widgetSelected(SelectionEvent event)
		{
			if(event.widget == okButton)
				setBellLineMargin();
			shell.dispose();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent event){}

		@Override
		public void keyPressed(KeyEvent event)
		{
			if(event.keyCode == '\r' || event.keyCode == '\n')
			{
				setBellLineMargin();
				shell.dispose();
			}
		}

		@Override
		public void keyReleased(KeyEvent event){}
	}

	private static class BellPageMarginHandler extends AbstractAction
	{
		private final Shell parent;

		private BellPageMarginHandler(Shell parent)
		{
			this.parent = parent;
		}

		@Override
		public void widgetSelected(SelectionEvent e)
		{
			new BellPageMarginDialog(parent);
		}
	}

	private static class BellPageMarginDialog implements SelectionListener, KeyListener
	{
		private final Shell shell;
		private final Button okButton;
		private final Button cancelButton;
		private final Spinner spinner;

		public BellPageMarginDialog(Shell parent)
		{
			shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			shell.setText("Bell Page");
			shell.setLayout(new GridLayout(3, true));

			spinner = new Spinner(shell, 0);
			spinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			spinner.setValues(Main.bzStyledText.getBellPageMargin(), 0, 27720, 0, 1, 10);
			spinner.addKeyListener(this);

			okButton = new Button(shell, SWT.PUSH);
			okButton.setText("OK");
			okButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			okButton.addSelectionListener(this);

			cancelButton = new Button(shell, SWT.PUSH);
			cancelButton.setText("Cancel");
			cancelButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
			cancelButton.addSelectionListener(this);

			shell.pack();
			shell.open();
		}

		private void setBellPageMargin()
		{
			Main.bzStyledText.setBellPageMargin(spinner.getSelection());
			Main.bzStyledText.redraw();
		}

		@Override
		public void widgetSelected(SelectionEvent event)
		{
			if(event.widget == okButton)
				setBellPageMargin();
			shell.dispose();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent event){}

		@Override
		public void keyPressed(KeyEvent event)
		{
			if(event.keyCode == '\r' || event.keyCode == '\n')
			{
				setBellPageMargin();
				shell.dispose();
			}
		}

		@Override
		public void keyReleased(KeyEvent event){}
	}

	private static class RewrapFromCursorHandler extends AbstractAction
	{
		@Override
		public void widgetSelected(SelectionEvent e)
		{
			Main.bzStyledText.rewrapFromCaret();
		}
	}
}

abstract class AbstractAction implements SelectionListener
{
	MenuItem addMenuItemTo(Menu menu,
	                       String tag,
	                       int accelerator)
	{
		MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText(tag);
		if(accelerator != 0)
			item.setAccelerator(accelerator);
		item.addSelectionListener(this);
		return item;
	}

	@Override
	public void widgetSelected(SelectionEvent event){}

	@Override
	public void widgetDefaultSelected(SelectionEvent event){}
}
