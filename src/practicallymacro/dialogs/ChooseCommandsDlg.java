package practicallymacro.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import practicallymacro.model.MacroManager;
import practicallymacro.util.Utilities;

public class ChooseCommandsDlg extends Dialog
{
	private Button mFilterNonEditorCommands;
	private Button mFilterNonMacroCommands;
	private Table mCommandTable;
	private TableColumn mAvailNameColumn;
	private TableColumn mAvailcategoryColumn;
	private Text mDescription;
	
	private List<Command> mFilteredCommands;
	private List<Command> mAvailableCommands;
	
	private List<String> mOutputSelectedCommands;
	
	protected ChooseCommandsDlg(Shell parentShell, List<Command> filteredCommands)
	{
		super(parentShell);
		mFilteredCommands=filteredCommands;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}
	
	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText("Add command(s) to the Quick Execute list");
		Composite comp=new Composite(parent, SWT.None);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

		mFilterNonEditorCommands=new Button(comp, SWT.CHECK);
		mFilterNonEditorCommands.setText("Filter out non-editor commands");
		mFilterNonEditorCommands.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				populateCommandTable();
			}
		});
		mFilterNonEditorCommands.setSelection(true);
		
		mFilterNonMacroCommands=new Button(comp, SWT.CHECK);
		GridData gd=new GridData();
		gd.horizontalIndent=15;
		mFilterNonMacroCommands.setLayoutData(gd);
		mFilterNonMacroCommands.setText("Show only user defined macros and macro support commands");
		mFilterNonMacroCommands.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				populateCommandTable();
			}
		});
		
		mCommandTable=new Table(comp, SWT.BORDER | SWT.MULTI);
		gd=new GridData(GridData.FILL_BOTH);
		gd.widthHint=300;
		gd.heightHint=300;
		mCommandTable.setLayoutData(gd);
		mAvailableCommands=new ArrayList<Command>();
		mAvailNameColumn=new TableColumn(mCommandTable, SWT.None);
		mAvailNameColumn.setText("Name");
		mAvailNameColumn.setWidth(150);
		mAvailcategoryColumn=new TableColumn(mCommandTable, SWT.None);
		mAvailcategoryColumn.setText("Category");
		mAvailcategoryColumn.setWidth(150);
		mCommandTable.setHeaderVisible(true);
		mCommandTable.setSortDirection(SWT.UP);
		mCommandTable.setSortColumn(mAvailNameColumn);
		mCommandTable.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				mDescription.setText("");
				TableItem[] selItems=mCommandTable.getSelection();
				StringBuffer buffer=new StringBuffer();
				for (TableItem tableItem : selItems)
				{
					Command c=(Command)tableItem.getData();
					try {
						buffer.append(c.getDescription());
					} catch (NotDefinedException e1) {
						e1.printStackTrace();
					}
					buffer.append('\n');
				}
				mDescription.setText(buffer.toString());
			}
		});
		mAvailNameColumn.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				mCommandTable.setSortColumn(mAvailNameColumn);
				populateCommandTable();
			}
			
		});
		mAvailcategoryColumn.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				mCommandTable.setSortColumn(mAvailcategoryColumn);
				populateCommandTable();
			}
			
		});
		
		
		final ICommandService cs = MacroManager.getOldCommandService();
		Command[] allCommands=cs.getDefinedCommands();
		Set<String> filteredIDs=new HashSet<String>();
		for (Command c : mFilteredCommands)
		{
			filteredIDs.add(c.getId());
		}
		
		for (Command command : allCommands)
		{
			if (!filteredIDs.contains(command.getId()))
			{
				mAvailableCommands.add(command);
			}
		}
		
		Label l=new Label(comp, SWT.None);
		l.setText("Description:");
		
		mDescription=new Text(comp, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		gd=new GridData(GridData.FILL_BOTH);
		gd.widthHint=300;
		gd.heightHint=50;
		mDescription.setLayoutData(gd);
		
		populateCommandTable();
		return comp;
	}
	
	private void populateCommandTable()
	{
		mCommandTable.removeAll();
		
		List<Command> filteredCommands=new ArrayList<Command>();
		if (mFilterNonEditorCommands.getSelection())
		{
			for (Command command : mAvailableCommands)
			{
				String catID="";
				try {
					catID=command.getCategory().getId();
				} catch (NotDefinedException e) {
					e.printStackTrace();
				}
				if (Utilities.isEditCategory(catID))
				{
					if (!mFilterNonMacroCommands.getSelection())
						filteredCommands.add(command);
					else if (Utilities.isUserMacroCategory(catID) || Utilities.isSupportCategory(catID))
						filteredCommands.add(command);
				}
			}
		}
		else
		{
			filteredCommands=mAvailableCommands;
		}
		
		
		Collections.sort(filteredCommands, new Comparator<Command>()
		{
			public int compare(Command o1, Command o2)
			{
				TableColumn column=mCommandTable.getSortColumn();
				if (column==mAvailNameColumn)
				{
					return getName(o1).compareToIgnoreCase(getName(o2));
				}
				
				return getCategory(o1).compareToIgnoreCase(getCategory(o2));
			}
		});
		
		mCommandTable.removeAll();
		for (Command command : filteredCommands) {
			TableItem t=new TableItem(mCommandTable, SWT.None);
			t.setText(0, getName(command));
			t.setText(1, getCategory(command));
			t.setData(command);
		}
		
	}
	
	private static String getCategory(Command c)
	{
		try
		{
			return c.getCategory().getName();
		}
		catch (NotDefinedException e)
		{
			e.printStackTrace();
			return "";
		}
	}
	
	private static String getName(Command c)
	{
		try {
			return c.getName();
		}
		catch (NotDefinedException e)
		{
			e.printStackTrace();
			return "";
		}
	}
	
	public List<String> getCommands()
	{
		return mOutputSelectedCommands;
	}

	@Override
	protected void okPressed()
	{
		mOutputSelectedCommands=new ArrayList<String>();
		TableItem[] selItems=mCommandTable.getSelection();
		for (TableItem tableItem : selItems)
		{
			Command c=(Command)tableItem.getData();
			mOutputSelectedCommands.add(c.getId());
		}
		super.okPressed();
	}
	
	
}
