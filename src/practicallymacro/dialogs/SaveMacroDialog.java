package practicallymacro.dialogs;

import java.util.List;

import org.eclipse.core.commands.Command;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import practicallymacro.commands.IMacroCommand;
import practicallymacro.editormacros.Activator;
import practicallymacro.model.EditorMacro;
import practicallymacro.model.MacroManager;
import practicallymacro.preferences.Initializer;


public class SaveMacroDialog extends TitleAreaDialog
{
	private List<IMacroCommand> mCommands;
	private Text mName;
	private Text mID;
	private Text mDescription;
	private Button mSaveCheck;
	private EditorMacro mResultMacro;
	private Button mShowDialogCheck;
	public SaveMacroDialog(Shell shell, List<IMacroCommand> commands)
	{
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		mCommands=commands;
	}
	
	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText("Save recorded macro");
		Composite comp=new Composite(parent, SWT.None);
		comp.setLayout(new GridLayout(2, false));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		SelectionListener updateStatusListener=new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updateStatus();
			}
		};
		
		ModifyListener updateModifyListener=new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				updateStatus();
			}
			
		};
		
		Composite nameHalf=new Composite(comp, SWT.None);
		nameHalf.setLayout(new GridLayout());
		nameHalf.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite nameField=new Composite(nameHalf, SWT.None);
		nameField.setLayout(new GridLayout(2, false));
		nameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label l=new Label(nameField, SWT.None);
		l.setText("Name for macro: ");
		mName=new Text(nameField, SWT.SINGLE | SWT.BORDER);
		mName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mName.addModifyListener(updateModifyListener);
		
		//generate a unique name
		mName.setText(MacroManager.getManager().getUniqueMacroName());
		
		l=new Label(nameField, SWT.None);
		l.setText("Description for macro: ");
		mDescription=new Text(nameField, SWT.SINGLE | SWT.BORDER);
		mDescription.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
		
		mSaveCheck=new Button(nameHalf, SWT.CHECK);
		mSaveCheck.setText("Save macro (for permanent use)");
		mSaveCheck.setSelection(false);
		mSaveCheck.addSelectionListener(updateStatusListener);
		
		Composite dataComp=new Composite(nameHalf, SWT.None);
		dataComp.setLayout(new GridLayout(2, false));
		dataComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		l=new Label(dataComp, SWT.None);
		l.setText("ID for macro: ");
		mID=new Text(dataComp, SWT.SINGLE | SWT.BORDER);
		mID.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mID.addModifyListener(updateModifyListener);
		
		mShowDialogCheck=new Button(nameHalf, SWT.CHECK);
		mShowDialogCheck.setText("Show this dialog after recording each macro");
		mShowDialogCheck.setSelection(Activator.getDefault().getPreferenceStore().getBoolean(Initializer.Pref_ShowSaveDialogAfterRecording));
		
		Composite commandHalf=new Composite(comp, SWT.None);
		commandHalf.setLayout(new GridLayout());
		commandHalf.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Table commandTable=new Table(commandHalf, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
		GridData gd=new GridData(GridData.FILL_BOTH);
		gd.heightHint=250;
		gd.widthHint=200;
		commandTable.setLayoutData(gd);
		for (IMacroCommand command : mCommands) {
			TableItem ti=new TableItem(commandTable, SWT.None);
			ti.setText(command.getName());
		}
		
		mName.setFocus();
		return comp;
	}
	
	private void updateStatus()
	{
//		mDescription.setEnabled(mSaveCheck.getSelection());
		if (mID==null || mSaveCheck==null || mName==null)
			return;
		
		mID.setEnabled(mSaveCheck.getSelection());
		if (mSaveCheck.getSelection())
		{
			//TODO: validate name
			if (mName.getText().length()==0)
			{
				setMessage("Name must be provided", IMessageProvider.ERROR);
				getButton(IDialogConstants.OK_ID).setEnabled(false);
				return;
			}
			
			//TODO: validate id
			ICommandService cs = MacroManager.getOldCommandService();
			if (mID.getText().length()==0)
			{
				setMessage("You must specify an ID for the command", IMessageProvider.ERROR);
				getButton(IDialogConstants.OK_ID).setEnabled(false);
				return;
			}
			
			Command command=cs.getCommand(mID.getText());
			if (command.isDefined())
			{
				setMessage("A command with that id is already defined", IMessageProvider.ERROR);
				getButton(IDialogConstants.OK_ID).setEnabled(false);
				return;
			}
			
		}
		
		getButton(IDialogConstants.OK_ID).setEnabled(mName.getText().length()>0);
		if (mName.getText().length()==0)
		{
			setMessage("Name must be provided to keep macro", IMessageProvider.ERROR);
			return;
		}
		setMessage(null);
	}

	@Override
	protected void okPressed()
	{
		if (mSaveCheck.getSelection())
		{
			mResultMacro=new EditorMacro(mCommands, mID.getText(), mName.getText(), mDescription.getText());
			System.out.println("Save macro as :"+mName.getText());
		}
		else
		{
			mResultMacro=new EditorMacro(mCommands, "", mName.getText(), "");
			System.out.println("Save temp macro as :"+mName.getText());
		}
		
		mResultMacro.setRunAsCompoundEvent(Activator.getDefault().getPreferenceStore().getBoolean(Initializer.Pref_ExecuteMacrosAtomically));
		Activator.getDefault().getPreferenceStore().setValue(Initializer.Pref_ShowSaveDialogAfterRecording, mShowDialogCheck.getSelection());
		super.okPressed();
	}
	
	
	
	@Override
	protected void cancelPressed()
	{
//		//save setting on cancel too? 
//		Activator.getDefault().getPreferenceStore().setValue(Initializer.Pref_ShowSaveDialogAfterRecording, mShowDialogCheck.getSelection());
		super.cancelPressed();
	}

	public EditorMacro getMacro()
	{
		return mResultMacro;
	}
	
	/*
	 * (non-Javadoc) Method declared on Dialog.
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		updateStatus();
	}

}
