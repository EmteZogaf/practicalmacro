package practicallymacro.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import practicallymacro.dialogs.PlayCommandDialog;

public class PlayCommand implements IHandler
{
	public void addHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		Shell shell = Display.getDefault().getActiveShell();
		PlayCommandDialog dlg=new PlayCommandDialog(shell);
		dlg.open();
		return null;
	}

	public boolean isEnabled()
	{
		return true;
	}

	public boolean isHandled()
	{
		return true;
	}

	public void removeHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

}
