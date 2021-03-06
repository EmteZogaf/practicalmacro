package practicallymacro.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.jface.text.source.ISourceViewer;

import practicallymacro.model.EditorMacro;
import practicallymacro.model.MacroManager;
import practicallymacro.util.Utilities;

public class MarkSelectionEnd implements IHandler {

	public void addHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

	public void dispose() {
		// TODO Auto-generated method stub

	}
	
	protected int mMarkIndex=0;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISourceViewer viewer=Utilities.getSourceViewer(Utilities.getActiveEditor());
		if (viewer!=null)
		{
			int markPos=Utilities.getUndirectedSelection(Utilities.getActiveEditor()).y;
			EditorMacro macro=MacroManager.getManager().getCurrentMacro();
			if (macro!=null)
			{
				macro.setMark(markPos, mMarkIndex);
			}
			else
			{
				MacroManager.getManager().setRecordingMark(markPos, mMarkIndex);
			}
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
