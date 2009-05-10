package practicallymacro.actions;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import practicallymacro.commands.EclipseCommand;
import practicallymacro.dialogs.QuickExecuteDialog;
import practicallymacro.editormacros.Activator;
import practicallymacro.util.Utilities;

public class QuickExecuteAction implements IWorkbenchWindowActionDelegate {

	IWorkbenchWindow mWindow;
	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void init(IWorkbenchWindow window)
	{
		mWindow=window;
	}

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
				IEditorPart editor=Utilities.getActiveEditor();
				command.execute(editor);
				
				Map<String, String> audit=new HashMap<String, String>();
				audit.put(Activator.Audit_Operation, Activator.Audit_Operation_QuickPlay);
				audit.put(Activator.Audit_FileExtension, Activator.getExtension(editor));
				Activator.logStatistics(audit);
			}			
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

}
