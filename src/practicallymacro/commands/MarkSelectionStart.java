package practicallymacro.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.swt.custom.StyledText;

import practicallymacro.model.EditorMacro;
import practicallymacro.model.MacroManager;
import practicallymacro.util.Utilities;

public class MarkSelectionStart implements IHandler {

	public void addHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		StyledText styled=Utilities.getStyledText(Utilities.getActiveEditor());
		if (styled!=null)
		{
			int markPos=styled.getSelection().x;
			EditorMacro macro=MacroManager.getManager().getCurrentMacro();
			if (macro!=null)
			{
				macro.setMark(markPos);
			}
			else
			{
				MacroManager.getManager().setRecordingMark(markPos);
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
