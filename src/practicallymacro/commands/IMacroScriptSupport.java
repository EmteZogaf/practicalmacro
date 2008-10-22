package practicallymacro.commands;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;

public interface IMacroScriptSupport
{
	/**
	 * @param script the script to evaluate.  May be null.
	 * @param target the editor that the script is evaluated against
	 * @return true if successfully executed, false otherwise
	 */
	public boolean evaluate(String script, IEditorPart target);
	
	/**
	 * @return the ID of the support class; should uniquely identify the support plugin
	 */
	public String getID();  
	
	/**
	 * @return the ID of the support class as a display string
	 */
	public String getIDDisplayString();
	
	/**
	 * @param script the current script, null if no current script (the initial case)
	 * @return the script after editing
	 */
	public String editScript(String script, Shell shell); 
}
