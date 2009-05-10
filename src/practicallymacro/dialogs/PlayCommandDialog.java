package practicallymacro.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import practicallymacro.commands.EclipseCommand;
import practicallymacro.commands.IMacroCommand;
import practicallymacro.commands.TemporaryMacroCommand;
import practicallymacro.editormacros.Activator;
import practicallymacro.model.EditorMacro;
import practicallymacro.model.MacroManager;
import practicallymacro.util.MacroConsole;
import practicallymacro.util.Utilities;


public class PlayCommandDialog extends Dialog
{
	private Table mCommandTable;
	private Text mDescriptionText;
//	private List<IMacroCommand> mCommands;
	private Button mFilterNonEditorCommands;
	private Button mFilterNonMacroCommands;
	private TableColumn mNameColumn;
	private Spinner mCountSpinner;
	
	public PlayCommandDialog(Shell shell)
	{
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText("Play command");
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
				updateCommandTable();
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
				updateCommandTable();
			}
		});

		
		mCommandTable=new Table(comp, SWT.BORDER | SWT.SINGLE| SWT.FULL_SELECTION);
		mCommandTable.setHeaderVisible(true);
		GridData tableData=new GridData(GridData.FILL_HORIZONTAL);
		GC gc=new GC(mCommandTable);
		tableData.heightHint=gc.getFontMetrics().getHeight()*25;
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
		mNameColumn=new TableColumn(mCommandTable, SWT.None);
		mNameColumn.setText("Command Name");
		mNameColumn.setWidth(200);
		final TableColumn categoryColumn=new TableColumn(mCommandTable, SWT.None);
		categoryColumn.setText("Category");
		categoryColumn.setWidth(200);
		
		mCommandTable.setSortDirection(SWT.UP);
		mCommandTable.setSortColumn(mNameColumn);
		
		mNameColumn.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				mCommandTable.setSortColumn(mNameColumn);
				updateCommandTable();
			}
			
		});
		categoryColumn.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				mCommandTable.setSortColumn(categoryColumn);
				updateCommandTable();
			}
			
		});
		
		
		mDescriptionText=new Text(comp, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL);
		GridData descData=new GridData(GridData.FILL_HORIZONTAL);
		gc=new GC(mDescriptionText);
		descData.heightHint=gc.getFontMetrics().getHeight()*5;
		mDescriptionText.setLayoutData(descData);
		
		Composite spinComp=new Composite(comp, SWT.None);
		spinComp.setLayout(new GridLayout(2, false));
		spinComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Label l=new Label(spinComp, SWT.None);
		l.setText("Number of times to execute command");
		mCountSpinner=new Spinner(spinComp, SWT.BORDER);
		mCountSpinner.setMinimum(1);
		mCountSpinner.setSelection(0);
		
//		mCommands=new ArrayList<IMacroCommand>();
		
		updateCommandTable();
		
		return comp;
	}

	@Override
	protected void okPressed()
	{
		TableItem[] selItems=mCommandTable.getSelection();
		if (selItems.length>0)
		{
			//perform selected command
			final IMacroCommand command=(IMacroCommand)selItems[0].getData();
			final int timesToExecute=mCountSpinner.getSelection();
			
			// TODO Auto-generated method stub
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					//turn off recording while this command is played so that we can only record the command itself, not its components
					if (MacroManager.getManager().getMacroState()==MacroManager.State_Recording)
					{
						//turn off command recording
						MacroManager.getManager().getRecorder().pauseRecording();
					}
					
					IEditorPart editor=Utilities.getActiveEditor();
					for (int i=0;i<timesToExecute;i++)
					{
						boolean success=command.execute(Utilities.getActiveEditor());
						if (!success)
						{
							MacroConsole.getConsole().writeln("Stopped executing command after "+timesToExecute+" iterations because last execution failed.", MacroConsole.Type_Standard);
							break;
						}
					}
					if (MacroManager.getManager().getMacroState()==MacroManager.State_Recording)
					{
						//turn command recording back on
						MacroManager.getManager().getRecorder().resumeRecording();
						MacroManager.getManager().getRecorder().recordCommand(command);
					}
					
//					widget.removeExtendedModifyListener(MacroManager.getManager().getMarkUpdater());
//					if (Activator.getDefault().getMacroState()==Activator.State_Idle)
//						document.removeDocumentListener(MacroManager.getManager().getMarkUpdater());
					
					Map<String, String> audit=new HashMap<String, String>();
					audit.put(Activator.Audit_Operation, Activator.Audit_Operation_PlayAny);
					audit.put(Activator.Audit_FileExtension, Activator.getExtension(editor));
					Activator.logStatistics(audit);
				}
				
			});
			
		}
		
		super.okPressed();
	}
	
	private void updateDescription()
	{
		mDescriptionText.setText("");
		TableItem[] selItems=mCommandTable.getSelection();
		if (selItems.length>0)
		{
			String desc=((IMacroCommand)selItems[0].getData()).getDescription();
			if (desc!=null)
				mDescriptionText.setText(desc);
		}		
	}

	private void updateButtons()
	{
		mFilterNonMacroCommands.setEnabled(mFilterNonEditorCommands.getSelection());
	}

	private void updateCommandTable()
	{
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
		
		List<EditorMacro> tempMacros=MacroManager.getManager().getUsedTempMacros(-1);
		for (EditorMacro editorMacro : tempMacros) {
			allItems.add(new TemporaryMacroCommand(editorMacro));
		}
		
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
				TableColumn column=mCommandTable.getSortColumn();
				if (column==mNameColumn)
				{
					return o1.getName().compareToIgnoreCase(o2.getName());
				}
				
				return o1.getCategory().compareToIgnoreCase(o2.getCategory());
			}
		});
		
		mCommandTable.removeAll();
		for (IMacroCommand command : filteredCommands)
		{
			TableItem item=new TableItem(mCommandTable, SWT.None);
			item.setText(0, command.getName());
			item.setText(1, command.getCategory());
			item.setData(command);
		}
		
		updateButtons();
	}

}
