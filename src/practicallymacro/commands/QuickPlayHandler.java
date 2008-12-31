package practicallymacro.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;

import practicallymacro.util.Utilities;

public class QuickPlayHandler implements IHandler {

	public void addHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		return null;
	}

	public boolean isEnabled() 
	{
		return (Utilities.getActiveEditor()!=null);
	}

	public boolean isHandled()
	{
		return true;
	}

	public void removeHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

}
