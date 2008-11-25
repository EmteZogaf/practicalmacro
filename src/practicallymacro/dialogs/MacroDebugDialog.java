package practicallymacro.dialogs;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import practicallymacro.editormacros.Activator;

public class MacroDebugDialog extends Dialog
{
	public static final int ACTION_NEXT=4;
	public static final int ACTION_CONTINUETOEND=5;
	public static final int ACTION_CANCELEXECUTION=6;
	private List<MacroCommandDebugInfo> mCommands;
	private int mSelectedAction;
//	private StyledText mFocusEditor;
	public MacroDebugDialog(Shell shell, List<MacroCommandDebugInfo> commands, StyledText focusEditor)
	{
		super(shell);
		mCommands=commands;
//		mFocusEditor=focusEditor;
		setShellStyle(SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
	}
	
	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText("Macro debug console");
		Composite comp=new Composite(parent, SWT.None);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Table t=new Table(comp, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData gd=new GridData(GridData.FILL_BOTH);
		gd.widthHint=600;
		gd.heightHint=200;
		t.setLayoutData(gd);
		t.setHeaderVisible(true);
		
		TableColumn commandCol=new TableColumn(t, SWT.None);
		commandCol.setText("Command");
		commandCol.setWidth(300);
		
		TableColumn cursorCol=new TableColumn(t, SWT.None);
		cursorCol.setText("Cursor Position");
		cursorCol.setWidth(100);
		
		TableColumn selectionCol=new TableColumn(t, SWT.None);
		selectionCol.setText("Selection end");
		selectionCol.setWidth(200);
		
		int nextCommandIndex=0;
		for (MacroCommandDebugInfo commandInfo : mCommands)
		{
			TableItem item=new TableItem(t, SWT.None);
			item.setText(0, commandInfo.getCommand().getName());
			Point cursorPos=commandInfo.getCursorPos();
			if (cursorPos!=null)
			{
				nextCommandIndex++;
				item.setText(1, "Line: "+cursorPos.x+" Col: "+cursorPos.y);
				Point selEndPos=commandInfo.getSelEnd();
				if (selEndPos==null)
					item.setText(2, "No selection");
				else
				{
					item.setText(2, "Selection from Line: "+selEndPos.x+" Col: "+selEndPos.y);
				}
			}
			else
			{
				item.setText(1, "Not run yet");
			}
		}
		
		if (nextCommandIndex<t.getItemCount())
			t.select(nextCommandIndex);
		t.showSelection();
		
		final Table finalT=t;
		final int selIndex=nextCommandIndex;
		t.addControlListener(new ControlAdapter()
		{
			@Override
			public void controlResized(ControlEvent e)
			{
				int itemsPerScreen=(finalT.getBounds().height/finalT.getItemHeight())-1;
				finalT.setTopIndex(Math.max(0, selIndex-(itemsPerScreen-1)));
			}
		});
		
		return comp;
	}

	@Override
	protected void buttonPressed(int buttonId)
	{
		mSelectedAction=buttonId;
		super.okPressed();
	}
	
	public int getSelectedAction()
	{
		return mSelectedAction;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{
		createButton(parent, ACTION_NEXT, "Next", true);
		createButton(parent, ACTION_CONTINUETOEND, "Continue w/o stepping", false);
		createButton(parent, ACTION_CANCELEXECUTION, "Cancel execution", false);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings()
	{
		return Activator.getDefault().getDialogSettings();
	}

	
}
