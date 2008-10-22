package practicallymacro.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import practicallymacro.model.EditorMacro;
import practicallymacro.model.MacroManager;
import practicallymacro.util.Utilities;


public class MarkSelectionEndAction extends Action implements
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
	public void run() {
		StyledText styled=Utilities.getStyledText(Utilities.getActiveEditor());
		if (styled!=null)
		{
			Point sel=styled.getSelection();
			EditorMacro macro=MacroManager.getManager().getCurrentMacro();
			if (macro!=null)
				macro.setMark(sel.x+sel.y);
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

}
