package practicallymacro.dialogs;


import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.commands.ICommandService;

import practicallymacro.editormacros.Activator;
import practicallymacro.model.MacroManager;
import practicallymacro.preferences.Initializer;

public class QuickExecuteDialog extends Dialog
{
	private String mCommandID;
	private Table mCommandTable;
	private Button mAddButton;
	private Button mRemoveButton;
	private Button mMoveUpButton;
	private Button mMoveDownButton;
	private List<Command> mQuickCommands;
	private Text mDescriptionText;
	
	public QuickExecuteDialog(Shell parentShell)
	{
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText("Quick Execute Command");
		
		Label l=new Label(parent, SWT.None);
		l.setText("Choose the command to execute");
		
		Composite comp=new Composite(parent, SWT.None);
		comp.setLayout(new GridLayout(2, false));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite tableComp=new Composite(comp, SWT.None);
		tableComp.setLayout(new GridLayout());
		tableComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		mCommandTable=new Table(tableComp, SWT.BORDER | SWT.SINGLE);
		GridData gd=new GridData(GridData.FILL_BOTH);
		gd.widthHint=200;
		gd.heightHint=200;
		mCommandTable.setLayoutData(gd);
		mCommandTable.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				TableItem[] selItems=mCommandTable.getSelection();
				mDescriptionText.setText("");
				if (selItems!=null && selItems.length>0)
				{
					TableItem selItem=selItems[0];
					mCommandID=((Command)selItem.getData()).getId();
				}
				else
				{
					mCommandID=null;
				}
				enableWidgets();
			}
		});
		
		mQuickCommands=new ArrayList<Command>();
		String commandIDString=Activator.getDefault().getPreferenceStore().getString(Initializer.Pref_QuickPlayCommands);
		String[] commandIDs=commandIDString.split("\n");
		final ICommandService cs = MacroManager.getOldCommandService();
		for (String id : commandIDs)
		{
			if (id==null || id.length()==0)
				continue;
			
			Command command=cs.getCommand(id);
			if (command!=null && command.isDefined())
			{
				mQuickCommands.add(command);
			}
		}
		
		Composite actionComp=new Composite(comp, SWT.None);
		actionComp.setLayout(new GridLayout());
		actionComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		
		mAddButton=new Button(actionComp, SWT.PUSH);
		mAddButton.setText("Add command...");
		mAddButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				ChooseCommandsDlg dlg=new ChooseCommandsDlg(getShell(), mQuickCommands);
				if (dlg.open()==Dialog.OK)
				{
					List<String> ids=dlg.getCommands();
					for (String id : ids)
					{
						Command c=cs.getCommand(id);
						if (c.isDefined())
						{
							mQuickCommands.add(c);
						}
					}
					populateCommandList();
				}
			}
		});
		
		mRemoveButton=new Button(actionComp, SWT.PUSH);
		mRemoveButton.setText("Remove command");
		mRemoveButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				int selIndex=mCommandTable.getSelectionIndex();
				if (selIndex>=0)
				{
					mQuickCommands.remove(selIndex);
					populateCommandList();
					if (selIndex>=mQuickCommands.size())
						selIndex=mQuickCommands.size()-1;
					if (selIndex>=0)
						mCommandTable.setSelection(selIndex);
					enableWidgets();
				}
			}
		});
		
		mMoveUpButton=new Button(actionComp, SWT.PUSH);
		mMoveUpButton.setText("Move up");
		mMoveUpButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				int selIndex=mCommandTable.getSelectionIndex();
				if (selIndex>=0)
				{
					Command c=mQuickCommands.get(selIndex);
					mQuickCommands.set(selIndex, mQuickCommands.get(selIndex-1));
					mQuickCommands.set(selIndex-1, c);
					populateCommandList();
					mCommandTable.setSelection(selIndex-1);
					enableWidgets();
				}
			}
		});
		
		mMoveDownButton=new Button(actionComp, SWT.PUSH);
		mMoveDownButton.setText("Move down");
		mMoveDownButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				int selIndex=mCommandTable.getSelectionIndex();
				if (selIndex>=0)
				{
					Command c=mQuickCommands.get(selIndex);
					mQuickCommands.set(selIndex, mQuickCommands.get(selIndex+1));
					mQuickCommands.set(selIndex+1, c);
					populateCommandList();
					mCommandTable.setSelection(selIndex+1);
					enableWidgets();
				}
			}
		});
		
		Composite descComp=new Composite(parent, SWT.None);
		descComp.setLayout(new GridLayout(1, false));
		descComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		l=new Label(descComp, SWT.None);
		l.setText("Description:");
		
		mDescriptionText=new Text(descComp, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		gd=new GridData(GridData.FILL_BOTH);
		gd.heightHint=40;
		mDescriptionText.setLayoutData(gd);
		
		
		populateCommandList();
		if (mCommandTable.getItemCount()>0)
			mCommandTable.select(0);
		enableWidgets();
		
		return comp;
	}
	
	private void populateCommandList()
	{
		mCommandTable.removeAll();
		for (Command c : mQuickCommands)
		{
			TableItem t=new TableItem(mCommandTable, SWT.None);
			try {
				t.setText(c.getName());
			} catch (NotDefinedException e) {
				//we should have filtered out 'bad' commands above
				e.printStackTrace();
			}
			t.setData(c);
		}
		
		enableWidgets();
	}

	private void enableWidgets()
	{
		int selIndex=mCommandTable.getSelectionIndex();
		mRemoveButton.setEnabled(selIndex>=0);
		mMoveDownButton.setEnabled(selIndex>=0 && selIndex+1<mCommandTable.getItemCount());
		mMoveUpButton.setEnabled(selIndex>=0 && selIndex>0);

		mDescriptionText.setText("");
		if (selIndex>=0)
		{
			Command selItem=mQuickCommands.get(selIndex);
			try {
				mDescriptionText.setText(selItem.getDescription());
			} catch (NotDefinedException e1) {
				e1.printStackTrace();
			}
		}		
	}

	public String getCommandID()
	{
		return mCommandID;
	}

	@Override
	protected void okPressed()
	{
		mCommandID=null;
		TableItem[] selItems=mCommandTable.getSelection();
		if (selItems.length>0)
		{
			mCommandID=((Command)selItems[0].getData()).getId();
		}
		
		//save command list to prefstore
		StringBuffer buffer=new StringBuffer();
		for (Command c : mQuickCommands) {
			buffer.append(c.getId());
			buffer.append('\n');
		}
		Activator.getDefault().getPreferenceStore().setValue(Initializer.Pref_QuickPlayCommands, buffer.toString());
		
		super.okPressed();
	}
	
	
}
