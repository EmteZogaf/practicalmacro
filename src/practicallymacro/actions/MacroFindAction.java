package practicallymacro.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;

import practicallymacro.commands.FindCommand;
import practicallymacro.model.MacroRecorder;

public class MacroFindAction extends Action
{
	private MacroRecorder mRecorder;
	public MacroFindAction(MacroRecorder recorder)
	{
		mRecorder=recorder;
	}

	@Override
	public void run()
	{
		FindCommand fc=new FindCommand();
		fc.configureNew(Display.getDefault().getActiveShell());
		mRecorder.recordCommand(fc);
		fc.execute(mRecorder.getEditor());
		System.out.println("running find action");
	}
	
}
