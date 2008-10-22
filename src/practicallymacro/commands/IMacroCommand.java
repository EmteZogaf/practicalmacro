package practicallymacro.commands;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface IMacroCommand
{
	public boolean execute(IEditorPart target);
	public void dump();
	public void persist(Document doc, Element commandElement);
	public IMacroCommand createFrom(Element commandElement);
	public String getName();
	public String getDescription();
	public String getCategory();
	public String getCategoryID();
	public boolean isConfigurable();
	public void configure(Shell shell);
	public IMacroCommand copy();
	public boolean requiresPost(); //return true if this command must be posted to the async queue 
}
