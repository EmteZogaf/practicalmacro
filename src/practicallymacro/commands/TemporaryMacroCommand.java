package practicallymacro.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import practicallymacro.model.EditorMacro;
import practicallymacro.model.MacroManager;

public class TemporaryMacroCommand implements IMacroCommand {

	private EditorMacro mTempMacro;
	
	public TemporaryMacroCommand(EditorMacro tempMacro)
	{
		mTempMacro=tempMacro;
	}
	
	public void configure(Shell shell)
	{
		//can't be configured
	}

	public IMacroCommand copy()
	{
		return this;
	}

	public IMacroCommand createFrom(Element commandElement)
	{
		//can't be serialized
		return null;
	}

	public void dump()
	{
		mTempMacro.dump();
	}

	public boolean execute(IEditorPart target)
	{
		mTempMacro.run(target);
		return true;
	}

	public String getCategory() {
		return MacroManager.UserMacroCategoryName;
	}

	public String getCategoryID()
	{
		return MacroManager.UserMacroCategoryID;
	}

	public String getDescription()
	{
		return mTempMacro.getDescription();
	}

	public String getName()
	{
		return mTempMacro.getName();
	}

	public boolean isConfigurable()
	{
		return false;
	}

	public void persist(Document doc, Element commandElement) {
		////can't be serialized
	}

	public boolean requiresPost()
	{
		List<Object> flattenedList=new ArrayList<Object>();
		mTempMacro.flattenMacro(flattenedList);
		return mTempMacro.requiresPost(flattenedList);
	}

}
