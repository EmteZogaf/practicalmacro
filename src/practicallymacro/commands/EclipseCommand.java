package practicallymacro.commands;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IHandler2;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import practicallymacro.model.MacroManager;
import practicallymacro.util.MacroConsole;
import practicallymacro.util.Utilities;


public class EclipseCommand implements IMacroCommand
{
	public static final String XML_ID_ATTR="commandID";
	public static final String XML_EclipseCommandType="EclipseCommand";
	
	private String mCommandId; 
	public EclipseCommand(String commandId)
	{
		mCommandId=commandId;
	}
	
	public EclipseCommand()
	{
		//nothing to do here
	}
	
	@SuppressWarnings("deprecation")
	public boolean execute(IEditorPart target)
	{
		ICommandService cs = MacroManager.getOldCommandService();
		Command command=cs.getCommand(mCommandId);
		if (command!=null)
		{
			try {
				IHandlerService hs = MacroManager.getOldHandlerService();
				ExecutionEvent exEvent=hs.createExecutionEvent(command, null);
				
				//This check is necessary because certain commands (like copy) may be disabled until there
				//is a selection.  However, the keystrokes that set the selection kick off an event
				//that is not guaranteed to be received in time to run the copy command.  The 'new'
				//way is to implement IHandler2, which explicitly runs the code to check the enablement,
				//so for commands that don't implement that, I've gone back to the deprecated "execute"
				//method that doesn't set (or check) the possibly out-of-date enable flag at all.
				if (command.getHandler() instanceof IHandler2)
					command.executeWithChecks(exEvent);
				else
					command.execute(exEvent);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				MacroConsole.getConsole().write(e, MacroConsole.Type_Error);
			}
		}

		return false;
	}

	public void dump() {
		System.out.println("Eclipse Command: "+mCommandId);
	}

	public void persist(Document doc, Element commandElement)
	{
		Map<String, String> attrMap=new HashMap<String, String>();
		attrMap.put(XML_ID_ATTR, mCommandId);
		Utilities.persistCommand(doc, commandElement, XML_EclipseCommandType, attrMap, null);
//		
//		commandElement.setAttribute(XML_ID_ATTR, mCommandId);
//		commandElement.setAttribute(MacroManager.XML_CommandType_ATTR, XML_EclipseCommandType);
	}

	public IMacroCommand createFrom(Element commandElement)
	{
		String commandID=commandElement.getAttribute(XML_ID_ATTR);
		EclipseCommand newCommand=new EclipseCommand(commandID);
		return newCommand;
	}

	public String getDescription()
	{
		ICommandService cs = MacroManager.getOldCommandService();
		Command command=cs.getCommand(mCommandId);
		try {
			return command.getDescription();
		} catch (NotDefinedException e) {
			e.printStackTrace();
		}
		
		return "";
	}

	public String getName()
	{
		ICommandService cs = MacroManager.getOldCommandService();
		Command command=cs.getCommand(mCommandId);
		try {
			return command.getName();
		} catch (NotDefinedException e)
		{
			//e.printStackTrace();
		}
		
		return mCommandId;
	}
	
	public String getCategory()
	{
		ICommandService cs = MacroManager.getOldCommandService();
		Command command=cs.getCommand(mCommandId);
		try {
			Category cat=command.getCategory();
			if (cat!=null)
				return cat.getName();
		} catch (NotDefinedException e) {
//			e.printStackTrace();
		}
		
		return "";
	}

	public String getCategoryID()
	{
		ICommandService cs = MacroManager.getOldCommandService();
		Command command=cs.getCommand(mCommandId);
		try {
			Category cat=command.getCategory();
			if (cat!=null)
				return cat.getId();
		} catch (NotDefinedException e) {
//			e.printStackTrace();
		}
		
		return "";
	}

	public void configure(Shell shell) {
		
	}

	public boolean isConfigurable() {
		return false;
	}

	public IMacroCommand copy()
	{
		//immutable command, but return a new copy in case an == comparison is done
		return new EclipseCommand(mCommandId);
	}

	public String getCommandID()
	{
		return mCommandId;
	}

	public boolean requiresPost()
	{
//		//need to search recursively if this is actually a macro
//		EditorMacro macro=MacroManager.getManager().getMacro(mCommandId);
//		if (macro!=null)
//			return macro.requiresPost();
		return false;
	}
}
