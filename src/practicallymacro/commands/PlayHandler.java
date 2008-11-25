package practicallymacro.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;

import practicallymacro.model.EditorMacro;
import practicallymacro.model.MacroManager;
import practicallymacro.util.Utilities;

public class PlayHandler implements IHandler
{
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
		EditorMacro macro=MacroManager.getManager().getLastMacro();
		if (macro!=null)
		{
			macro.run(Utilities.getActiveEditor());
		}
		else
		{
			MessageDialog.openInformation(null, "Warning", "No recently run/recorded macro");
		}
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

	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	
	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub

	}

}
