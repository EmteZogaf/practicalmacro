package practicallymacro.actions;

import org.eclipse.core.commands.Command;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

import practicallymacro.commands.EclipseCommand;
import practicallymacro.dialogs.QuickExecuteDialog;
import practicallymacro.util.Utilities;

public class QuickExecuteAction implements IWorkbenchWindowActionDelegate {

	IWorkbenchWindow mWindow;
	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IWorkbenchWindow window)
	{
		mWindow=window;
	}

	@Override
	public void run(IAction action) {
		Shell shell=null;
		if (mWindow!=null)
			shell=mWindow.getShell();
		else
			shell=Display.getDefault().getActiveShell();
		QuickExecuteDialog dlg=new QuickExecuteDialog(shell);
		if (dlg.open()==Dialog.OK)
		{
			String commandID=dlg.getCommandID();
			if (commandID!=null)
			{
				EclipseCommand command=new EclipseCommand(commandID);
				command.execute(Utilities.getActiveEditor());
			}			
		}

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

}
