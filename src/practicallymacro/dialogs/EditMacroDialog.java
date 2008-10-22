package practicallymacro.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.Command;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import practicallymacro.commands.EclipseCommand;
import practicallymacro.commands.IMacroCommand;
import practicallymacro.editormacros.Activator;
import practicallymacro.model.EditorMacro;
import practicallymacro.util.Utilities;


public class EditMacroDialog extends TitleAreaDialog
{
	protected static final String Category_Undefined = "Undefined";
	private EditorMacro mExistingMacro;
	private EditorMacro mResultMacro;
	
	private Text mCommandDescriptionText;
	private Button mAddButton;
	private Button mDeleteButton;
	private Button mEditButton;
	private Button mCopyButton;
	private Button mMoveUpButton;
	private Button mMoveDownButton;
	private Table mCommandTable;
	
	private Text mMacroDescriptionText;
	private Text mIDText;
	private Text mNameText;
	
	private Button mFilterNonEditorCommands;
	private Button mFilterNonMacroCommands;
	
	List<IMacroCommand> mCommands;
	
	private TableColumn mAvailNameColumn;
	private TableColumn mAvailcategoryColumn;
	
	private Table mAvailableCommandTable;
	
	private Text mAvailableDescriptionText;
	
	private Set<String> mUsedIDs;
	
	public EditMacroDialog(Shell shell, EditorMacro existingMacro, Set<String> usedIDs)
	{
		super(shell);
		mExistingMacro=existingMacro;
		mUsedIDs=usedIDs;
		mCommands=new ArrayList<IMacroCommand>();
		if (mExistingMacro!=null)
		{
			//may need to copy?
			mCommands.addAll(mExistingMacro.getCommands());
		}
		
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}
	
	@Override
	protected Control createDialogArea(Composite parent)
	{
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getShell(), Activator.PLUGIN_ID+".editDialogHelp");
		getShell().setText("Edit Macro");
		
		Composite comp=new Composite(parent, SWT.None);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite commandComp=new Composite(comp, SWT.None);
		commandComp.setLayout(new GridLayout(4, false));
		commandComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		
		Composite availableComp=new Composite(commandComp, SWT.None);
		availableComp.setLayout(new GridLayout());
		availableComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		mFilterNonEditorCommands=new Button(availableComp, SWT.CHECK);
		mFilterNonEditorCommands.setText("Filter out non-editor commands");
		mFilterNonEditorCommands.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updateAvailableCommandTable();
			}
		});
		mFilterNonEditorCommands.setSelection(true);
		
		mFilterNonMacroCommands=new Button(availableComp, SWT.CHECK);
		GridData gd=new GridData();
		gd.horizontalIndent=15;
		mFilterNonMacroCommands.setLayoutData(gd);
		mFilterNonMacroCommands.setText("Show only user defined macros and macro support commands");
		mFilterNonMacroCommands.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updateAvailableCommandTable();
			}
		});
		
		Label avail=new Label(availableComp, SWT.None);
		avail.setText("Available Commands");
		
		mAvailableCommandTable=new Table(availableComp, SWT.BORDER| SWT.FULL_SELECTION);
		GridData tableData=new GridData(GridData.FILL_HORIZONTAL);
		GC gc=new GC(mAvailableCommandTable);
		tableData.heightHint=gc.getFontMetrics().getHeight()*25;
		tableData.widthHint=gc.getFontMetrics().getAverageCharWidth()*50;
		mAvailableCommandTable.setLayoutData(tableData);
		mAvailNameColumn=new TableColumn(mAvailableCommandTable, SWT.None);
		mAvailNameColumn.setText("Name");
		mAvailNameColumn.setWidth(150);
		mAvailcategoryColumn=new TableColumn(mAvailableCommandTable, SWT.None);
		mAvailcategoryColumn.setText("Category");
		mAvailcategoryColumn.setWidth(150);
		mAvailableCommandTable.setHeaderVisible(true);
		mAvailableCommandTable.setSortDirection(SWT.UP);
		mAvailableCommandTable.setSortColumn(mAvailNameColumn);
		mAvailNameColumn.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				mAvailableCommandTable.setSortColumn(mAvailNameColumn);
				updateAvailableCommandTable();
			}
			
		});
		mAvailcategoryColumn.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				mAvailableCommandTable.setSortColumn(mAvailcategoryColumn);
				updateAvailableCommandTable();
			}
			
		});
		
		mAvailableCommandTable.addMouseListener(new MouseListener()
		{

			public void mouseDoubleClick(MouseEvent e)
			{
				TableItem[] selItems=mAvailableCommandTable.getSelection();
				for (int i = 0; i < selItems.length; i++) {
					TableItem item = selItems[i];
					IMacroCommand command=(IMacroCommand)item.getData();
					addCommandToMacroList(command);
				}
				
				//regenerate command table with new items
				updateCommandTable();
			}

			public void mouseDown(MouseEvent e) {
				// nothing
			}

			public void mouseUp(MouseEvent e) {
				// nothing
			}
			
		});
		mAvailableCommandTable.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updateAvailDescription();
			}
		});
		
		updateAvailableCommandTable();
		
		mAvailableDescriptionText=new Text(availableComp, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
		gd=new GridData(GridData.FILL_BOTH);
		gc=new GC(mAvailableDescriptionText);
		gd.heightHint=gc.getFontMetrics().getHeight()*3;
		mAvailableDescriptionText.setLayoutData(gd);
		
		mAddButton=new Button(commandComp, SWT.PUSH);
		mAddButton.setText("Add->");
		mAddButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				TableItem[] selItems=mAvailableCommandTable.getSelection();
				for (int i = 0; i < selItems.length; i++) {
					TableItem item = selItems[i];
					IMacroCommand command=(IMacroCommand)item.getData();
					addCommandToMacroList(command);
				}
				
				//regenerate command table with new items
				updateCommandTable();
			}
		});
		
		Composite tableComp=new Composite(commandComp, SWT.None);
		tableComp.setLayout(new GridLayout());
		tableComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label current=new Label(tableComp, SWT.None);
		current.setText("Current Commands");
		
		mCommandTable=new Table(tableComp, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		tableData=new GridData(GridData.FILL_HORIZONTAL);
		gc=new GC(mCommandTable);
		tableData.heightHint=gc.getFontMetrics().getHeight()*25;
		tableData.widthHint=gc.getFontMetrics().getAverageCharWidth()*50;
		mCommandTable.setLayoutData(tableData);
		mCommandTable.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updateDescription();
				updateButtons();
			}
		});
		TableColumn nameColumn=new TableColumn(mCommandTable, SWT.None);
		nameColumn.setText("Name");
		nameColumn.setWidth(150);
		TableColumn categoryColumn=new TableColumn(mCommandTable, SWT.None);
		categoryColumn.setText("Category");
		categoryColumn.setWidth(150);
		mCommandTable.setHeaderVisible(true);

		updateCommandTable();
		if (mCommandTable.getItemCount()>0)
			mCommandTable.setSelection(0);
		
		mCommandDescriptionText=new Text(tableComp, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData descData=new GridData(GridData.FILL_BOTH);
		gc=new GC(mCommandDescriptionText);
		descData.heightHint=gc.getFontMetrics().getHeight()*3;
		mCommandDescriptionText.setLayoutData(descData);
		
		Composite buttonComp=new Composite(commandComp, SWT.None);
		buttonComp.setLayout(new GridLayout());
		
		mDeleteButton=new Button(buttonComp, SWT.PUSH);
		mDeleteButton.setText("Delete");
		mDeleteButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				int[] items=mCommandTable.getSelectionIndices();
				Arrays.sort(items);
				int largestIndex=items[items.length-1];
				for (int i = items.length-1; i >=0 ; i--)
				{
					mCommands.remove(items[i]);
				}
				
				updateCommandTable();
				int newSelIndex=Math.min(Math.max(0, largestIndex-items.length+1), mCommandTable.getItemCount()-1);
				if (newSelIndex<mCommandTable.getItemCount())
					mCommandTable.setSelection(newSelIndex);
				updateButtons();
			}
		});
		
		mEditButton=new Button(buttonComp, SWT.PUSH);
		mEditButton.setText("Edit...");
		mEditButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				TableItem[] items=mCommandTable.getSelection();
				if (items.length==1)
				{
					IMacroCommand command=(IMacroCommand)items[0].getData();
					command.configure(getShell());
				}
			}
		});
		
		mCopyButton=new Button(buttonComp, SWT.PUSH);
		mCopyButton.setText("Copy");
		mCopyButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				TableItem[] items=mCommandTable.getSelection();
				for (TableItem tableItem : items)
				{
					IMacroCommand command=(IMacroCommand)tableItem.getData();
					mCommands.add(command.copy());
				}
				updateCommandTable();
				updateButtons();
			}
		});
		
		mMoveUpButton=new Button(buttonComp, SWT.PUSH);
		mMoveUpButton.setText("Move Up");
		mMoveUpButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				int selIndex=mCommandTable.getSelectionIndex();
				Collections.swap(mCommands, selIndex, selIndex-1);
				updateCommandTable();
				mCommandTable.setSelection(selIndex-1);
				updateButtons();
			}
		});
		
		mMoveDownButton=new Button(buttonComp, SWT.PUSH);
		mMoveDownButton.setText("Move Down");
		mMoveDownButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				int selIndex=mCommandTable.getSelectionIndex();
				Collections.swap(mCommands, selIndex, selIndex+1);
				updateCommandTable();
				mCommandTable.setSelection(selIndex+1);
				updateButtons();
			}
		});
		
		Group idComp=new Group(comp, SWT.None);
		idComp.setLayout(new GridLayout(2, false));
		idComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		idComp.setText("Macro info");
		
		Label l=new Label(idComp, SWT.None);
		l.setText("Name: ");
		mNameText=new Text(idComp, SWT.BORDER | SWT.SINGLE);
		mNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if (mExistingMacro!=null)
			mNameText.setText(mExistingMacro.getName());
		mNameText.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				updateButtons();
			}
		});
		
		l=new Label(idComp, SWT.None);
		l.setText("id: ");
		mIDText=new Text(idComp, SWT.BORDER | SWT.SINGLE);
		mIDText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if (mExistingMacro!=null)
			mIDText.setText(mExistingMacro.getID());
		mIDText.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				String currentText=mIDText.getText();
				boolean isError=false;
				if (currentText.length()>0)
				{
					if (mUsedIDs.contains(currentText) && ((mExistingMacro==null) || !(currentText.equals(mExistingMacro.getID()))))
					{
						isError=true;
					}
				}
				
				if (isError)
				{
					setMessage("ID conflicts with another command", IMessageProvider.ERROR);
					getButton(IDialogConstants.OK_ID).setEnabled(false);
				}
				else
				{
					setMessage(null);
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
			}
		});
		
		l=new Label(idComp, SWT.None);
		l.setText("Description: ");
		mMacroDescriptionText=new Text(idComp, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		descData=new GridData(GridData.FILL_HORIZONTAL);
		gc=new GC(mMacroDescriptionText);
		descData.heightHint=gc.getFontMetrics().getHeight()*5;
		mMacroDescriptionText.setLayoutData(descData);
		if (mExistingMacro!=null)
			mMacroDescriptionText.setText(mExistingMacro.getDescription());
		
		return comp;
	}
	
	private void updateAvailableCommandTable()
	{
		//add all available commands to table; need to add synthethic commands too (or add them as real commands at startup)
		Utilities.createStyledTextCommands();
		ICommandService cs = (ICommandService) PlatformUI.getWorkbench().getAdapter(ICommandService.class);
		Command[] allCommands=cs.getDefinedCommands();
		List<IMacroCommand> allItems=new ArrayList<IMacroCommand>();
		for (int i = 0; i < allCommands.length; i++) {
			Command command = allCommands[i];
			if (command.isDefined())
			{
				allItems.add(new EclipseCommand(command.getId()));
			}
		}
		allItems.addAll(Utilities.getMacroSupportCommands());
		
		List<IMacroCommand> filteredCommands=new ArrayList<IMacroCommand>();
		if (mFilterNonEditorCommands.getSelection())
		{
			for (IMacroCommand macroCommand : allItems)
			{
				if (Utilities.isEditCategory(macroCommand.getCategoryID()))
				{
					if (!mFilterNonMacroCommands.getSelection())
						filteredCommands.add(macroCommand);
					else if (Utilities.isUserMacro(macroCommand) || Utilities.isSupportCategory(macroCommand.getCategoryID()))
						filteredCommands.add(macroCommand);
				}
			}
		}
		else
		{
			filteredCommands=allItems;
		}
		
		
		Collections.sort(filteredCommands, new Comparator<IMacroCommand>()
		{
			public int compare(IMacroCommand o1, IMacroCommand o2)
			{
				TableColumn column=mAvailableCommandTable.getSortColumn();
				if (column==mAvailNameColumn)
				{
					return o1.getName().compareToIgnoreCase(o2.getName());
				}
				
				return o1.getCategory().compareToIgnoreCase(o2.getCategory());
			}
		});
		
		mAvailableCommandTable.removeAll();
		for (IMacroCommand command : filteredCommands) {
			TableItem t=new TableItem(mAvailableCommandTable, SWT.None);
			t.setText(0, command.getName());
			t.setText(1, command.getCategory());
			t.setData(command);
		}
	}

	protected void addCommandToMacroList(IMacroCommand command)
	{
		//see if it's a configurable command; if so, give them a chance to configure it
		IMacroCommand newCommand=command;
		if (newCommand.isConfigurable())
		{
			newCommand=command.copy();
			newCommand.configure(getShell());
		}
		
		//otherwise, it's just a wrapped eclipse command
		mCommands.add(newCommand);
	}

	private void updateCommandTable()
	{
		mCommandTable.removeAll();
		for (IMacroCommand command : mCommands)
		{
			TableItem item=new TableItem(mCommandTable, SWT.None);
			item.setText(0, command.getName());
			item.setText(1, command.getCategory());
			item.setData(command);
		}
		
	}

	private void updateDescription()
	{
		mCommandDescriptionText.setText("");
		TableItem[] selItems=mCommandTable.getSelection();
		if (selItems.length>0)
		{
			StringBuffer buffer=new StringBuffer();
			for (int i = 0; i < selItems.length; i++) {
				TableItem tableItem = selItems[i];
				buffer.append(((IMacroCommand)tableItem.getData()).getDescription());
				if (i+1<selItems.length)
					buffer.append("\n");
			}
			mCommandDescriptionText.setText(buffer.toString());
		}		
	}

	private void updateAvailDescription() {
		mAvailableDescriptionText.setText("");
		TableItem[] selItems=mAvailableCommandTable.getSelection();
		if (selItems.length>0)
		{
			StringBuffer buffer=new StringBuffer();
			for (int i = 0; i < selItems.length; i++) {
				TableItem tableItem = selItems[i];
				String desc=((IMacroCommand)tableItem.getData()).getDescription();
				if (desc!=null)
				{
					buffer.append(desc);
					if (i+1<selItems.length)
						buffer.append("\n");
				}
			}
			mAvailableDescriptionText.setText(buffer.toString());
		}		
	}
	
	private void updateButtons()
	{
		int selCount=mCommandTable.getSelectionCount();
		int selIndex=mCommandTable.getSelectionIndex();
		TableItem[] selItems=mCommandTable.getSelection();
		boolean isConfigurable=false;
		if (selItems.length==1)
		{
			if (((IMacroCommand)selItems[0].getData()).isConfigurable())
			{
				isConfigurable=true;
			}
		}
		mEditButton.setEnabled(isConfigurable);
		mDeleteButton.setEnabled(selCount>0);
		mMoveUpButton.setEnabled(selCount==1 && selIndex>0);
		mMoveDownButton.setEnabled(selCount==1 && selIndex<mCommandTable.getItemCount()-1);
		mCopyButton.setEnabled(selCount>0);
		
		mFilterNonMacroCommands.setEnabled(mFilterNonEditorCommands.getSelection());
		
		getButton(IDialogConstants.OK_ID).setEnabled(mNameText.getText().length()>0);
	}

//	@Override
//	protected boolean isResizable()
//	{
//		return true;
//	}
	
	@Override
	protected void okPressed()
	{
		//build result macro
		mResultMacro=new EditorMacro(mCommands, mIDText.getText(), mNameText.getText(), mMacroDescriptionText.getText());
		super.okPressed();
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
		updateButtons();
	}
	
}
