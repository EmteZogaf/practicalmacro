package practicallymacro.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.jface.text.source.ISourceViewer;

import practicallymacro.model.EditorMacro;
import practicallymacro.model.MacroManager;
import practicallymacro.util.Utilities;

public class MoveCursorToMark implements IHandler {

	public void addHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

	public void dispose() {
		// TODO Auto-generated method stub

	}
	
	protected int mMarkIndex=0;

	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		ISourceViewer viewer=Utilities.getSourceViewer(Utilities.getActiveEditor());
		if (viewer!=null)
		{
			EditorMacro macro=MacroManager.getManager().getCurrentMacro();
			int markPos=0;
			if (macro!=null)
			{
				markPos=macro.getMark(mMarkIndex);
			}
			else
			{
				markPos=MacroManager.getManager().getRecordingMark(mMarkIndex);
			}
			viewer.setSelectedRange(markPos, 0);
		}
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
