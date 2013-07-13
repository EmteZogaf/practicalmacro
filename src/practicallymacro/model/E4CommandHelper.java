package practicallymacro.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.core.commands.Command;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.ui.PlatformUI;

public class E4CommandHelper {
	public static void addMCommandToSystem(Command newCommand)
	{
		try
		{
			IEclipseContext context=(IEclipseContext)PlatformUI.getWorkbench().getAdapter(IEclipseContext.class);
			Class commandsFactory=Class.forName("org.eclipse.e4.ui.model.application.commands.impl.CommandsFactoryImpl");
			Field instanceField=commandsFactory.getField("INSTANCE");
			Method createCommand=commandsFactory.getMethod("createCommand");
			Object implInstance=instanceField.get(null);
			MCommand anMCommand=(MCommand)createCommand.invoke(implInstance);
//			MCommand anMCommand=CommandsFactoryImpl.INSTANCE.createCommand();
			anMCommand.setElementId(newCommand.getId());
			try
			{
				anMCommand.setCommandName(newCommand.getName());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			MApplication app=context.get(MApplication.class);
			List<MCommand> allCommands=app.getCommands();
			boolean commandExists=false;
			for (MCommand mcommand : allCommands)
			{
				if (mcommand.getElementId().equals(newCommand.getId()))
				{
					commandExists=true;
					break;
				}
			}
			if (!commandExists)
				allCommands.add(anMCommand);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	
}
