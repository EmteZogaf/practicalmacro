package practicallymacro.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import practicallymacro.model.EditorMacro;
import practicallymacro.model.MacroManager;
import practicallymacro.util.Utilities;


/**
 * I don't think this is used anymore.
 * @deprecated
 *
 */
public class SetCursorToMarkAction extends Action implements
		IWorkbenchWindowActionDelegate {

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub

	}

	public void run(IAction action) {
		run();
	}
	
	@Override
	public void run()
	{
		ISourceViewer viewer=Utilities.getSourceViewer(Utilities.getActiveEditor());
		if (viewer!=null)
		{
			EditorMacro macro=MacroManager.getManager().getCurrentMacro();
			if (macro!=null)
			{
				int markPos=macro.getMark(0);
				viewer.setSelectedRange(markPos, 0);
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

}
