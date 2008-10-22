package practicallymacro.commands;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import practicallymacro.model.MacroManager;
import practicallymacro.util.Utilities;


public class InsertStringCommand implements IMacroCommand
{
	public static final String XML_Data_Tag="data";
	public static final String XML_InsertStringType="InsertStringCommand";
	
	private String mData;
	public InsertStringCommand(String data)
	{
		mData=data;
	}
	
	//for serialization only
	public InsertStringCommand()
	{
		
	}
	
	public void dump()
	{
		System.out.println("Insert string: "+mData);
	}

	public boolean execute(final IEditorPart target)
	{
		if (mData==null)
		{
			//prompt for data to be added?
			configure(target.getSite().getShell());
//			InputDialog dlg=new InputDialog(target.getSite().getShell(), "Insert String", "Enter string to insert", mData, null);
//			if (dlg.open()==Dialog.OK)
//			{
//				data=dlg.getValue();
//			}
		}
		
		if (mData!=null)
		{
			StyledText widget=Utilities.getStyledText(target);
			if (widget!=null)
			{
				int caretPos=widget.getCaretOffset();
				int selSize=widget.getSelectionCount();
				widget.insert(mData);
				widget.setCaretOffset(caretPos+mData.length()-selSize);
				return true;
			}
		}
		
		return false;
	}

	public void persist(Document doc, Element commandElement)
	{
		Map<String, String> dataMap=new HashMap<String, String>();
		if (mData!=null)
			dataMap.put(XML_Data_Tag, mData);
		Utilities.persistCommand(doc, commandElement, XML_InsertStringType, null, dataMap);
//		if (mData!=null)
//		{
//			Element child = doc.createElement(XML_Data_Tag);
//			child.setTextContent(mData);
//			commandElement.appendChild(child);
//		}
//		commandElement.setAttribute(MacroManager.XML_CommandType_ATTR, XML_InsertStringType);
	}

	public IMacroCommand createFrom(Element commandElement)
	{
		NodeList children=commandElement.getChildNodes();
		for (int i=0;i<children.getLength();i++)
		{
			if (children.item(i).getNodeName().equals(XML_Data_Tag))
			{
				String data=children.item(i).getTextContent();
				InsertStringCommand newCommand=new InsertStringCommand(data);
				return newCommand;
			}			
		}
		
		return new InsertStringCommand();
	}

	public String getDescription()
	{
		return "Insert the string in to the document at the current caret location";
	}

	public String getName()
	{
		return "Insert string: "+mData;
	}

	public String getCategory()
	{
		return MacroManager.MacroCommandCategory;
	}

	public void configure(Shell shell)
	{
		InputDialog dlg=new InputDialog(shell, "Insert String", "Enter string to insert", mData, null);
		if (dlg.open()==Dialog.OK)
		{
			mData=dlg.getValue();
		}
	}

	public boolean isConfigurable()
	{
		return true;
	}
	
	public IMacroCommand copy()
	{
		return new InsertStringCommand(mData);
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
