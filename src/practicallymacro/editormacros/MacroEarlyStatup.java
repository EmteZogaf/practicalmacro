package practicallymacro.editormacros;
import org.eclipse.ui.IStartup;

import practicallymacro.model.MacroManager;


public class MacroEarlyStatup implements IStartup {

	public void earlyStartup()
	{
		//this just kicks the manager so that it will load previously defined macros
		//and register commands
		MacroManager.getManager();
	}

}
