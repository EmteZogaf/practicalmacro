package practicallymacro.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.Command;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import practicallymacro.commands.EclipseCommand;
import practicallymacro.dialogs.EditMacroDialog;
import practicallymacro.dialogs.MacroDefinitionsPage;
import practicallymacro.model.EditorMacro;
import practicallymacro.model.MacroManager;
import practicallymacro.util.Utilities;


public class PlayDropdownAction implements IWorkbenchWindowPulldownDelegate 
{

	private Menu fMenu;
	private IAction mPlayAnyCommandAction=new PlayCommandAction();
	
	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub

	}

	public void run(IAction action) {
		EditorMacro macro=MacroManager.getManager().getLastMacro();
		if (macro!=null)
		{
			IEditorPart editor=Utilities.getActiveEditor();
			macro.run(editor);
		}
		else
		{
			MessageDialog.openInformation(null, "Execute last macro", "No recently recorded/used macro");
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	public Menu getMenu(Control parent) {
		setMenu(new Menu(parent));
		fillMenu(fMenu);
		initMenu();
		return fMenu;
	}

	protected void fillMenu(Menu menu)
	{
		//make "run last", "execute any command..." (not just macros)
		ActionContributionItem item = new ActionContributionItem(mPlayAnyCommandAction);
		item.fill(menu, -1);
		
		List<EditorMacro> tempMacros=MacroManager.getManager().getUsedTempMacros(15);
		if (tempMacros.size()>0)
		{
			MenuItem cascadeMenu=new MenuItem(menu, SWT.CASCADE);
			cascadeMenu.setText("Temporary macros");
			Menu tempMRUMenu=new Menu(cascadeMenu);
			cascadeMenu.setMenu(tempMRUMenu);
			for (EditorMacro macro : tempMacros)
			{
				Action action=new MacroAction(macro);
				item = new ActionContributionItem(action);
				item.fill(tempMRUMenu, -1);
			}
		}
		
		//add the mru list of macros
		List<EditorMacro> macros=MacroManager.getManager().getUsedMacros(15);
		if (macros.size()>0)
		{
			MenuItem cascadeMenu=new MenuItem(menu, SWT.CASCADE);
			cascadeMenu.setText("Saved Macros");
			Menu mruMenu=new Menu(cascadeMenu);
			cascadeMenu.setMenu(mruMenu);
			for (EditorMacro editorMacro : macros)
			{
				Action action=new MacroAction(editorMacro);
				item = new ActionContributionItem(action);
				item.fill(mruMenu, -1);
			}
		}
		
		new MenuItem(menu, SWT.SEPARATOR);
		
		//add the mru list of macro/temp macros that can be edited (or viewed?)
		List<EditorMacro> allMacros=new ArrayList<EditorMacro>();
		allMacros.addAll(macros);
		allMacros.addAll(tempMacros);
		Collections.sort(allMacros, new Comparator<EditorMacro>()
		{
			public int compare(EditorMacro o1, EditorMacro o2)
			{
				return (o1.getLastUse()>o2.getLastUse())? -1 : 1;
			}
		});
		if (allMacros.size()>0)
		{
			MenuItem cascadeMenu=new MenuItem(menu, SWT.CASCADE);
			cascadeMenu.setText("Edit macro...");
			Menu editMenu=new Menu(cascadeMenu);
			cascadeMenu.setMenu(editMenu);
			for (int i=0;i<Math.min(10, allMacros.size());i++)
			{
				EditorMacro macro=allMacros.get(i);
				Action action=new EditMacroAction(macro);
				item = new ActionContributionItem(action);
				item.fill(editMenu, -1);
			}
		}
		
		MenuItem debugItem=new MenuItem(menu, SWT.CHECK);
		debugItem.setText("Macro debug mode");
		debugItem.setSelection(MacroManager.getManager().isMacroDebugMode());
		debugItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				MacroManager.getManager().setMacroDebugMode(!MacroManager.getManager().isMacroDebugMode());
			}
		});
	}
	
	private void initMenu() {
		// Add listener to repopulate the menu each time
		// it is shown because of dynamic history list
		fMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {
				Menu m = (Menu) e.widget;
				MenuItem[] items = m.getItems();
				for (int i = 0; i < items.length; i++) {
					items[i].dispose();
				}
				fillMenu(m);
			}
		});
	}

	private void setMenu(Menu menu) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = menu;
	}

	protected void addToMenu(Menu menu, IAction action, int accelerator) {
		StringBuffer label = new StringBuffer();
		if (accelerator >= 0 && accelerator < 10) {
			// add the numerical accelerator
			label.append('&');
			label.append(accelerator);
			label.append(' ');
		}
		label.append(action.getText());
		action.setText(label.toString());
		ActionContributionItem item = new ActionContributionItem(action);
		item.fill(menu, -1);
	}
	
	private static class MacroAction extends Action
	{
		private EditorMacro mMacro;
		public MacroAction(EditorMacro macro)
		{
			mMacro=macro;
		}

		@Override
		public String getText()
		{
			return mMacro.getName();
		}

		@Override
		public void run()
		{
			ICommandService cs = MacroManager.getOldCommandService();
			Command systemCommand=null;
			if (mMacro.getID().length()>0)
				systemCommand=cs.getCommand(mMacro.getID());
			
			//turn macro recording off so that only the top-level command will be registered IF this macro
			//actually corresponds to a command.  Otherwise, let the commands be recorded
			if (systemCommand!=null && MacroManager.getManager().getMacroState()==MacroManager.State_Recording)
			{
				//turn off command recording
				MacroManager.getManager().getRecorder().pauseRecording();
			}
			IEditorPart editor=Utilities.getActiveEditor();
			mMacro.run(editor);
			if (systemCommand!=null && MacroManager.getManager().getMacroState()==MacroManager.State_Recording)
			{
				//turn command recording back on
				MacroManager.getManager().getRecorder().resumeRecording();
				MacroManager.getManager().getRecorder().recordCommand(new EclipseCommand(mMacro.getID()));
			}
		}
	}
	
	private static class EditMacroAction extends Action
	{
		private EditorMacro mMacro;
		public EditMacroAction(EditorMacro macro)
		{
			mMacro=macro;
		}

		@Override
		public String getText()
		{
			return mMacro.getName();
		}
		
		@SuppressWarnings("unchecked")
		protected Set<String> getIDSet()
		{
			Map<Integer, EditorMacro> allMacros=MacroManager.getManager().getUniqueMacroMap();
			Set<String> currentIDs=new HashSet<String>();
			for (EditorMacro macro: allMacros.values())
			{
				if (macro.getID().length()>0)
				{
					currentIDs.add(macro.getID());
				}
			}
			ICommandService cs = MacroManager.getOldCommandService();
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

		@Override
		public void run()
		{
			Set<String> idSet=getIDSet();
			Map<Integer, EditorMacro> allMacros=MacroManager.getManager().getUniqueMacroMap();
			//prevent ID from being changed? only if it's already being used by another macro?
			EditMacroDialog dlg=new EditMacroDialog(Display.getDefault().getActiveShell(), mMacro, idSet, (MacroDefinitionsPage.getUsageOfMacro(allMacros, mMacro).size()==0), mMacro.isContributed());
			if (dlg.open()==Dialog.OK)
			{
				allMacros.put(mMacro.getSessionID(), dlg.getMacro());
				MacroManager.getManager().replaceDefinedMacros(allMacros);
			}
		}
	}
	
}
