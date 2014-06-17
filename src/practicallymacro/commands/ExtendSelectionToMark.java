package practicallymacro.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.jface.text.source.ISourceViewer;

import practicallymacro.model.EditorMacro;
import practicallymacro.model.MacroManager;
import practicallymacro.util.Utilities;

public class ExtendSelectionToMark implements IHandler
{

	public void addHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		ISourceViewer viewer=Utilities.getSourceViewer(Utilities.getActiveEditor());
		if (viewer!=null)
		{
			int markPos=(-1);
			EditorMacro macro=MacroManager.getManager().getCurrentMacro();
			int caretPos=Utilities.getCaretPos(Utilities.getActiveEditor());
			if (macro!=null)
			{
				markPos=macro.getMark();
			}
			else
			{
				markPos=MacroManager.getManager().getRecordingMark();
			}
			
			if (markPos>=0)
			{
				viewer.setSelectedRange(caretPos, markPos-caretPos);
			}			
		}
		
		return null;
	}

	public boolean isEnabled() {
		return true;
	}

	public boolean isHandled() {
		return true;
	}

	public void removeHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

}
