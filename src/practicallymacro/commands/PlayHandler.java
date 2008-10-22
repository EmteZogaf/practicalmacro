package practicallymacro.commands;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate;

import practicallymacro.actions.PlayCommandAction;
import practicallymacro.editormacros.Activator;
import practicallymacro.model.EditorMacro;
import practicallymacro.model.MacroManager;
import practicallymacro.util.Utilities;

public class PlayHandler implements IHandler, IWorkbenchWindowPulldownDelegate
{
	private Menu fMenu;
	private IAction mPlayAnyCommandAction=new PlayCommandAction();
	
	public PlayHandler()
	{
		
	}
	
	public void addHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public Object execute(ExecutionEvent event) throws ExecutionException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isEnabled()
	{
		return (Utilities.getActiveEditor()!=null);
	}

	public boolean isHandled() {
		// TODO Auto-generated method stub
		return true;
	}

	public void removeHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

	public void run(IAction action) {
		EditorMacro macro=Activator.getDefault().getLastMacro();
		if (macro!=null)
		{
			macro.run(Utilities.getActiveEditor());
		}
		else
		{
			MessageDialog.openInformation(null, "Warning", "No recently recorded macro");
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
		
		List<EditorMacro> tempMacros=MacroManager.getManager().getUsedTempMacros(-1);
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
		List<EditorMacro> macros=MacroManager.getManager().getUsedMacros(10);
		if (macros.size()>0)
		{
			MenuItem cascadeMenu=new MenuItem(menu, SWT.CASCADE);
			cascadeMenu.setText("Macros");
			Menu mruMenu=new Menu(cascadeMenu);
			cascadeMenu.setMenu(mruMenu);
			for (EditorMacro editorMacro : macros)
			{
				Action action=new MacroAction(editorMacro);
				item = new ActionContributionItem(action);
				item.fill(mruMenu, -1);
			}
		}
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
			mMacro.run(Utilities.getActiveEditor());
		}
	}
	
	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub

	}

}
