package practicallymacro.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import practicallymacro.dialogs.PlayCommandDialog;


public class PlayCommandAction extends Action implements IWorkbenchWindowActionDelegate {

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub

	}

	public void run(IAction action)
	{
		run();
	}
	
	@Override
	public void run() {
		Shell shell = Display.getDefault().getActiveShell();
		PlayCommandDialog dlg=new PlayCommandDialog(shell);
		dlg.open();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getDescription() {
		return "Play any command in the system";
	}

	@Override
	public String getId() {
		return "com.none.actions.playCommandAction";
	}

	@Override
	public String getText() {
		return "Play Command...";
	}

	
}
