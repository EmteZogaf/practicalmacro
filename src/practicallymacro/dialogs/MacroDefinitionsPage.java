package practicallymacro.dialogs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.keys.IBindingService;

import practicallymacro.commands.EclipseCommand;
import practicallymacro.commands.IMacroCommand;
import practicallymacro.editormacros.Activator;
import practicallymacro.model.EditorMacro;
import practicallymacro.model.MacroManager;


public class MacroDefinitionsPage extends PreferencePage implements
		IWorkbenchPreferencePage {

	protected static final String KEY_SESSIONID = "SessionID";
	private Table mMacroTable;
	private Text mDescriptionText;
	private Button mNewButton;
	private Button mDeleteButton;
	private Button mEditButton;
	private Button mCopyButton;
	private Button mExportButton;
	private Button mImportButton;
	
	private Map<Integer, EditorMacro> mAllMacros;
	private int mNextUniqueID=999999999;
	
	public MacroDefinitionsPage() {
		// TODO Auto-generated constructor stub
	}

	public MacroDefinitionsPage(String title) {
		super(title);
		// TODO Auto-generated constructor stub
	}

	public MacroDefinitionsPage(String title, ImageDescriptor image) {
		super(title, image);
		// TODO Auto-generated constructor stub
	}
	
	private int getNextUniqueID()
	{
		return mNextUniqueID++;
	}

	@Override
	protected Control createContents(Composite parent)
	{
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getShell(), Activator.PLUGIN_ID+".managingMacrosHelp");
		getShell().setText("Editor Macros");
		Composite comp=new Composite(parent, SWT.None);
		comp.setLayout(new GridLayout(2, false));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite tableComp=new Composite(comp, SWT.None);
		tableComp.setLayout(new GridLayout());
		tableComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		mMacroTable=new Table(tableComp, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		mMacroTable.setHeaderVisible(true);
		GridData tableData=new GridData(GridData.FILL_HORIZONTAL);
		GC gc=new GC(mMacroTable);
		tableData.heightHint=gc.getFontMetrics().getHeight()*25;
		mMacroTable.setLayoutData(tableData);
		mMacroTable.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updateDescription();
				updateButtons();
			}
		});
		TableColumn nameColumn=new TableColumn(mMacroTable, SWT.None);
		nameColumn.setText("Command Name");
		nameColumn.setWidth(200);
		TableColumn idColumn=new TableColumn(mMacroTable, SWT.None);
		idColumn.setText("Command ID");
		idColumn.setWidth(200);
		TableColumn contributedColumn=new TableColumn(mMacroTable, SWT.None);
		contributedColumn.setText("From Plug-in");
		contributedColumn.setWidth(200);
		
		mDescriptionText=new Text(tableComp, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.WRAP);
		GridData descData=new GridData(GridData.FILL_HORIZONTAL);
		gc=new GC(mDescriptionText);
		descData.heightHint=gc.getFontMetrics().getHeight()*5;
		mDescriptionText.setLayoutData(descData);

		mAllMacros=MacroManager.getManager().getUniqueMacroMap();
		
		rebuildMacroTable();
		
		Composite buttonComp=new Composite(comp, SWT.None);
		buttonComp.setLayout(new GridLayout());
		
		mNewButton=new Button(buttonComp, SWT.PUSH);
		mNewButton.setText("New...");
		mNewButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e) {
				EditMacroDialog dlg=new EditMacroDialog(getShell(), null, getIDSet(), true, false);
				if (dlg.open()==Dialog.OK)
				{
					EditorMacro newMacro=dlg.getMacro();
					mAllMacros.put(getNextUniqueID(), newMacro);
					rebuildMacroTable();
				}
			}
		});
		
		mDeleteButton=new Button(buttonComp, SWT.PUSH);
		mDeleteButton.setText("Delete");
		mDeleteButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				List<EditorMacro> deletedItems=new ArrayList<EditorMacro>();
				List<EditorMacro> failedToDeleteForKeyBindings=new ArrayList<EditorMacro>();
				List<EditorMacro> failedToDeletedContributed=new ArrayList<EditorMacro>();
				Map<MacroHolder, Set<MacroHolder>> inUseMap=new HashMap<MacroHolder, Set<MacroHolder>>();
				TableItem[] selItems=mMacroTable.getSelection();
				for (int i = 0; i < selItems.length; i++)
				{
					TableItem tableItem = selItems[i];
					
					//see if macro is mapped as a command (can't delete) or is used as part of another command (warning)
					EditorMacro macro=(EditorMacro)tableItem.getData();
					
					boolean hasBinding=hasBinding(macro.getID());
					Set<MacroHolder> uses=getUsageOfMacro(mAllMacros, macro);
					if (macro.isContributed())
					{
						failedToDeletedContributed.add(macro);
					}
					else if (hasBinding)
					{
						failedToDeleteForKeyBindings.add(macro);
					}
					else if (uses.size()>0)
					{
						inUseMap.put(new MacroHolder((Integer)tableItem.getData(KEY_SESSIONID), macro), uses);
					}
					else
					{
						mAllMacros.remove(tableItem.getData(KEY_SESSIONID));
						deletedItems.add(macro);
					}
				}
				
				if (failedToDeletedContributed.size()>0)
				{
					StringBuffer buffer=new StringBuffer();
					boolean first=true;
					for (EditorMacro editorMacro : failedToDeletedContributed)
					{
						if (!first)
						{
							buffer.append(", ");
						}
						first=false;
						buffer.append(editorMacro.getName());
					}
					MessageDialog.openError(getShell(), "Failed to delete some macros", "The following macros cannot be deleted because they are contributed by a plug-in.\n"+buffer.toString());
				}
				
				if (failedToDeleteForKeyBindings.size()>0)
				{
					StringBuffer buffer=new StringBuffer();
					boolean first=true;
					for (EditorMacro editorMacro : failedToDeleteForKeyBindings)
					{
						if (!first)
						{
							buffer.append(", ");
						}
						first=false;
						buffer.append(editorMacro.getName());
					}
					MessageDialog.openError(getShell(), "Failed to delete some macros", "The following macros cannot be deleted because they have keys mapped to them.  You must first remove the key mappings.\n"+buffer.toString());
				}
				
				Map<MacroHolder, Set<MacroHolder>> stillInUse=new HashMap<MacroHolder, Set<MacroHolder>>();
				
				boolean madeChange=true;
				while (madeChange)
				{
					madeChange=false;
					for (Map.Entry<MacroHolder, Set<MacroHolder>> entry: inUseMap.entrySet())
					{
						MacroHolder holder=entry.getKey();
						Set<MacroHolder> inUseSet=inUseMap.get(holder);
						
						//remove any deleted items from this set
						for (EditorMacro deletedMacro : deletedItems) {
							inUseSet.remove(deletedMacro);
						}
						
						if (inUseSet.size()>0)
						{
							stillInUse.put(holder, inUseSet);
						}
						else
						{
							//since all uses were removed, I can delete it now
							deletedItems.add(holder.mMacro);
							madeChange=true;
							mAllMacros.remove(holder.mSessionMacroID);
						}
					}
					inUseMap=stillInUse;
				}
				
				if (inUseMap.size()>0)
				{
					StringBuffer buffer=new StringBuffer();
					buffer.append("Some macros are used by other macros.  Continue with delete?\n");
					for (Map.Entry<MacroHolder, Set<MacroHolder>> entry: inUseMap.entrySet()) {
						EditorMacro macro=entry.getKey().mMacro;
						buffer.append(macro.getName());
						buffer.append(" is used by: ");
						Set<MacroHolder> usingMacros=entry.getValue();
						boolean first=true;
						for (MacroHolder usingMacro : usingMacros) {
							if (!first)
							{
								buffer.append(", ");
							}
							first=false;
							buffer.append(usingMacro.mMacro.getName());
						}
						buffer.append("\n");
					}
					boolean okayToDelete=MessageDialog.openQuestion(getShell(), "Macros in use", buffer.toString());
					if (okayToDelete)
					{
						for (MacroHolder holder : inUseMap.keySet())
						{
							mAllMacros.remove(holder.mSessionMacroID);
						}
					}
				}
				
				rebuildMacroTable();
			}
		});
		
		mEditButton=new Button(buttonComp, SWT.PUSH);
		mEditButton.setText("Edit...");
		mEditButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem[] selItems=mMacroTable.getSelection();
				if (selItems.length>0)
				{
					EditorMacro macro=(EditorMacro)selItems[0].getData();
					EditMacroDialog dlg=new EditMacroDialog(getShell(), macro, getIDSet(), (getUsageOfMacro(mAllMacros, macro).size()==0), macro.isContributed());
					if (dlg.open()==Dialog.OK)
					{
						Integer sessionID=(Integer)selItems[0].getData(KEY_SESSIONID);
						macro=dlg.getMacro();
						mAllMacros.put(sessionID, macro);
//						mAllMacros.remove(selItems[0].getData());
//						mAllMacros.add(macro);
						rebuildMacroTable();
					}
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
				TableItem selItems[]=mMacroTable.getSelection();
				if (selItems.length>0)
				{
//					List<EditorMacro> macros=new ArrayList<EditorMacro>();
					for (int i = 0; i < selItems.length; i++)
					{
						TableItem tableItem = selItems[i];
						EditorMacro oldMacro=(EditorMacro)tableItem.getData();
						List<IMacroCommand> oldCommands=oldMacro.getCommands();
						List<IMacroCommand> newCommands=new ArrayList<IMacroCommand>();
						for (IMacroCommand macroCommand : oldCommands)
						{
							newCommands.add(macroCommand.copy());
						}
						EditorMacro copiedMacro=new EditorMacro(newCommands, oldMacro.getID()+".copy", oldMacro.getName()+" (copy)", oldMacro.getDescription());
						mAllMacros.put(getNextUniqueID(), copiedMacro);
					}
					rebuildMacroTable();
				}
			}
		});
		
		mExportButton=new Button(buttonComp, SWT.PUSH);
		mExportButton.setText("Export...");
		mExportButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				TableItem selItems[]=mMacroTable.getSelection();
				if (selItems.length>0)
				{
					List<EditorMacro> macros=new ArrayList<EditorMacro>();
					for (int i = 0; i < selItems.length; i++) {
						TableItem tableItem = selItems[i];
						macros.add((EditorMacro)tableItem.getData());
					}
					ExportMacroDialog dlg=new ExportMacroDialog(getShell(), macros);
					dlg.open();
				}
			}
		});
		
		mImportButton=new Button(buttonComp, SWT.PUSH);
		mImportButton.setText("Import...");
		mImportButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				ImportMacroDialog dlg=new ImportMacroDialog(getShell());
				if (dlg.open()==Dialog.OK)
				{
					Map<String, EditorMacro> idMap=new HashMap<String, EditorMacro>();
					for (EditorMacro existingMacro : mAllMacros.values())
					{
						idMap.put(existingMacro.getID(), existingMacro);
					}
					Collection<EditorMacro> macros=dlg.getMacros();
					for (EditorMacro newMacro : macros)
					{
						if (!idMap.containsKey(newMacro.getID()))
						{
							mAllMacros.put(getNextUniqueID(), newMacro);
						}
						else
						{
							//TODO: give them a choice to create new IDs and add these commands or override the existing macros
							MessageBox mb=new MessageBox(getShell(), SWT.OK);
							mb.setText("Import macro");
							mb.setMessage("Skipping macro because the id is already in use: "+newMacro.getID());
							mb.open();
						}
					}
					rebuildMacroTable();
				}
			}
		});
		
		updateButtons();
		return comp;
	}

	@SuppressWarnings("unchecked")
	protected Set<String> getIDSet()
	{
		Set<String> currentIDs=new HashSet<String>();
		for (EditorMacro macro: mAllMacros.values())
		{
			if (macro.getID().length()>0)
			{
				currentIDs.add(macro.getID());
			}
		}
		ICommandService cs = (ICommandService) PlatformUI.getWorkbench().getAdapter(ICommandService.class);
		Collection<String> allCommands=cs.getDefinedCommandIds();
		for (String commandID : allCommands)
		{
			if (commandID.length()>0)
			{
				currentIDs.add(commandID);
			}
		}
		return currentIDs;
	}

	protected boolean hasBinding(String commandId)
	{
		IBindingService bindingService = (IBindingService)PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		final Binding[] bindings = bindingService.getBindings();
		for (int i = 0; i < bindings.length; i++) {

			Binding binding = bindings[i];
			ParameterizedCommand command=binding.getParameterizedCommand();
			if (command==null)
				continue;
			
			String id=command.getId();
			if (commandId.equals(id))
				return true;
		}
		
		return false;
	}

	public static Set<MacroHolder> getUsageOfMacro(Map<Integer, EditorMacro> allMacros, EditorMacro macro)
	{
		Set<MacroHolder> usingMacros=new HashSet<MacroHolder>();
		for (Map.Entry<Integer, EditorMacro> entry : allMacros.entrySet())
		{
			EditorMacro editorMacro=entry.getValue();
			List<IMacroCommand> commands=editorMacro.getCommands();
			for (IMacroCommand macroCommand : commands)
			{
				//I think that the EclipseCommand is the only one that it makes sense to check
				if (macroCommand instanceof EclipseCommand)
				{
					if (((EclipseCommand)macroCommand).getCommandID().equals(macro.getID()))
					{
						usingMacros.add(new MacroHolder(entry.getKey(), editorMacro));
						break;
					}
				}
			}
		}
		return usingMacros;
	}

	protected void rebuildMacroTable()
	{
		mMacroTable.removeAll();
		List<MacroHolder> allMacros=new ArrayList<MacroHolder>();
		for (Map.Entry<Integer, EditorMacro> entry : mAllMacros.entrySet()) {
			MacroHolder holder=new MacroHolder(entry.getKey(), entry.getValue());
			allMacros.add(holder);
		}
		Collections.sort(allMacros, new Comparator<MacroHolder>()
		{
			public int compare(MacroHolder o1, MacroHolder o2)
			{
				return o1.mMacro.getName().compareTo(o2.mMacro.getName());
			}
		});
		
		for (MacroHolder holder: allMacros)
		{
			TableItem t=new TableItem(mMacroTable, SWT.None);
			EditorMacro macro=holder.mMacro;
			t.setText(0, macro.getName());
			t.setText(1, macro.getID());
			if (macro.isContributed())
			{
				t.setText(2, "true");
			}
			t.setData(macro);
			t.setData(KEY_SESSIONID, holder.mSessionMacroID);
		}
	}
	
	private static class MacroHolder
	{
		Integer mSessionMacroID;
		EditorMacro mMacro;
		public MacroHolder(Integer id, EditorMacro macro)
		{
			mSessionMacroID=id;
			mMacro=macro;
		}
		
		@Override
		public boolean equals(Object arg0)
		{
			return mSessionMacroID.equals(arg0);
		}
		
		@Override
		public int hashCode()
		{
			return mSessionMacroID.hashCode();
		}
		
		
	}

	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub

	}

	private void updateDescription()
	{
		mDescriptionText.setText("");
		TableItem[] selItems=mMacroTable.getSelection();
		if (selItems.length>0)
		{
			StringBuffer buffer=new StringBuffer();
			for (int i = 0; i < selItems.length; i++) {
				buffer.append(((EditorMacro)selItems[i].getData()).getDescription());
				if (i+1<selItems.length)
					buffer.append("\n");
			}
			mDescriptionText.setText(buffer.toString());
		}		
	}
	
	private void updateButtons()
	{
		int selCount=mMacroTable.getSelectionCount();
//		TableItem[] selItems=mMacroTable.getSelection();
		mEditButton.setEnabled(selCount==1);
		mDeleteButton.setEnabled(selCount>0);
		mExportButton.setEnabled(selCount>0);
//		mCopyButton.setEnabled(selCount>0);
	}

	@Override
	public boolean performOk()
	{
		//save the current state of the commands into the macro manager
		MacroManager.getManager().replaceDefinedMacros(mAllMacros);
		
		return super.performOk();
	}

	
}
