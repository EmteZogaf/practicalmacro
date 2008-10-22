package practicallymacro.commands;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import practicallymacro.model.MacroManager;
import practicallymacro.util.Utilities;


public class StyledTextCommand implements IMacroCommand {

	public static final String XML_StyledTextCommandID_Attr="styledTextCommandID";
	public static final String XML_StyledTextType="styledTextCommand";
	
	private int mStyledTextAction;
	private String mName;
	public StyledTextCommand(int styledTextActionConstant)
	{
		mStyledTextAction=styledTextActionConstant;
		mName=Utilities.getNameForStyledTextConstant(mStyledTextAction);
	}
	
	public StyledTextCommand()
	{
		
	}
	
	public void dump()
	{
		System.out.println("Styled text action: "+mStyledTextAction);
	}

	public boolean execute(IEditorPart target)
	{
		Utilities.getStyledText(target).invokeAction(mStyledTextAction);
		return true;
	}

	public void persist(Document doc, Element commandElement)
	{
		Map<String, String> attrMap=new HashMap<String, String>();
		attrMap.put(XML_StyledTextCommandID_Attr, Integer.toString(mStyledTextAction));
		Utilities.persistCommand(doc, commandElement, MacroManager.XML_CommandType_ATTR, attrMap, null);
		
//		commandElement.setAttribute(XML_StyledTextCommandID_Attr, Integer.toString(mStyledTextAction));
//		commandElement.setAttribute(MacroManager.XML_CommandType_ATTR, XML_StyledTextType);
	}

	public IMacroCommand createFrom(Element commandElement)
	{
		String codeString=commandElement.getAttribute(XML_StyledTextCommandID_Attr);
		StyledTextCommand newCommand=new StyledTextCommand(Integer.parseInt(codeString));
		return newCommand;
	}

	public String getDescription()
	{
		return "";
	}

	public String getName()
	{
		return mName;
	}

	public String getCategory()
	{
		return MacroManager.MacroCommandCategory;
	}

	public void configure(Shell shell) {
	}

	public boolean isConfigurable() {
		return false;
	}

	public IMacroCommand copy()
	{
		//immutable command, return a new copy in case == is needed
		return new StyledTextCommand(mStyledTextAction);
	}
	
	public String getCategoryID()
	{
		return MacroManager.MacroCommandCategoryID;
	}

	public boolean requiresPost()
	{
		return false;
	}
	
}
