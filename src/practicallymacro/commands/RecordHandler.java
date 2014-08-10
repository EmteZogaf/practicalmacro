package practicallymacro.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import practicallymacro.model.MacroManager;

public class RecordHandler implements IHandler {

	public RecordHandler()
	{
//		System.out.println("hit constructor");
	}
	
	public void addHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		// TODO Auto-generated method stub
		MacroManager.getManager().clearMarks();
		return null;
	}

	public boolean isEnabled()
	{
		IWorkbenchWindow activeWindow=PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWindow!=null)
		{
			IWorkbenchPage activePage=activeWindow.getActivePage();
			if (activePage!=null)
			{
				if (activePage.getActiveEditor()!=null)
					return true;
			}
		}
		
		return false;
	}

	public boolean isHandled() {
		// TODO Auto-generated method stub
		return true;
	}

	public void removeHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

}
