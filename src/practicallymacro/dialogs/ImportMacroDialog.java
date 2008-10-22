package practicallymacro.dialogs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import practicallymacro.model.EditorMacro;
import practicallymacro.model.MacroManager;


public class ImportMacroDialog extends Dialog {
	
	private Text mFileText;
	private Table mMacroTable;
	private Text mDescription;
	
	private Collection<EditorMacro> mMacros;
	
	public ImportMacroDialog(Shell shell)
	{
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText("Import Macros");
		Composite comp=new Composite(parent, SWT.None);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite fileComp=new Composite(comp, SWT.None);
		fileComp.setLayout(new GridLayout(3, false));
		fileComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label l=new Label(fileComp, SWT.None);
		l.setText("Import file name:");
		
		mFileText=new Text(fileComp, SWT.BORDER | SWT.SINGLE);
		GridData gd=new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint=300;
		mFileText.setLayoutData(gd);
		mFileText.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				rereadMacroFile();
				populateTable();
			}
		});
		
		Button browse=new Button(fileComp, SWT.PUSH);
		browse.setText("Browse...");
		browse.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				FileDialog fd=new FileDialog(getShell());
				String filePath=fd.open();
				if (filePath!=null)
				{
					mFileText.setText(filePath);
//					rereadMacroFile();
//					populateTable();
				}
			}
		});
		
		mMacroTable=new Table(comp, SWT.BORDER| SWT.FULL_SELECTION);
		gd=new GridData(GridData.FILL_BOTH);
		gd.heightHint=150;
		mMacroTable.setLayoutData(gd);
		TableColumn nameCol=new TableColumn(mMacroTable, SWT.None);
		nameCol.setText("Name");
		nameCol.setWidth(150);
		TableColumn idCol=new TableColumn(mMacroTable, SWT.None);
		idCol.setText("Command id");
		idCol.setWidth(150);
		mMacroTable.setHeaderVisible(true);
		
		mDescription=new Text(comp, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL);
		mDescription.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		return comp;
	}
	
	protected void rereadMacroFile()
	{
		mMacros=new ArrayList<EditorMacro>();
		String filePath=mFileText.getText();
		InputStreamReader r=null;
		try
		{
			File f=new File(filePath);
			if (!f.exists() || f.isDirectory())
				return;
			r=new InputStreamReader(new FileInputStream(f));
			mMacros=MacroManager.getManager().readMacros(r);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (r!=null)
					r.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	protected void populateTable()
	{
		mMacroTable.removeAll();
		for (EditorMacro macro : mMacros)
		{
			TableItem item=new TableItem(mMacroTable, SWT.None);
			item.setText(0, macro.getName());
			item.setText(1, macro.getID());
			item.setData(macro);
		}
	}

	@Override
	protected void okPressed()
	{
		super.okPressed();
	}
	
	public Collection<EditorMacro> getMacros()
	{
		return mMacros;
	}
	

}
