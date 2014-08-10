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
	protected int mMarkIndex=0;
	public void addHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		//NOTE: this selects from the current caret position to the mark.  Really, it should
		//select from the current selection anchor so that it would be an actual extension of
		//the selection.
		ISourceViewer viewer=Utilities.getSourceViewer(Utilities.getActiveEditor());
		if (viewer!=null)
		{
			int markPos=(-1);
			EditorMacro macro=MacroManager.getManager().getCurrentMacro();
			int anchorPos=Utilities.getSelectionAnchor(Utilities.getActiveEditor());
			if (macro!=null)
			{
				markPos=macro.getMark(mMarkIndex);
			}
			else
			{
				markPos=MacroManager.getManager().getRecordingMark(mMarkIndex);
			}
			
			if (markPos>=0)
			{
				viewer.setSelectedRange(anchorPos, markPos-anchorPos);
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
