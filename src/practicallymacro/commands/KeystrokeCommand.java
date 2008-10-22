package practicallymacro.commands;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import practicallymacro.model.MacroManager;
import practicallymacro.util.Utilities;

public class KeystrokeCommand implements IMacroCommand {

	private Event mKeyEvent;
	
	private static final String XML_ATTR_STATEMASK="stateMask";
	private static final String XML_ATTR_KEYCODE="keyCode";
	private static final String XML_ATTR_TYPE="eventType";
	private static final String XML_ATTR_CHARACTER="character";
	public static final String XML_Macro_Keystroke_Type="MacroKeystrokeCommand";
	
	public KeystrokeCommand(Event event)
	{
		mKeyEvent=event;
	}
	
	public KeystrokeCommand()
	{
		
	}
	
	public void configure(Shell shell) {
		// TODO Auto-generated method stub

	}

	public IMacroCommand copy()
	{
		// TODO Auto-generated method stub
		return null;
	}


	public void dump() {
		// TODO Auto-generated method stub

	}

	public boolean execute(IEditorPart target)
	{
		if (mKeyEvent!=null)
		{
			Display.getCurrent().post(mKeyEvent);
			return true;
		}
		
		return false;
	}

	public String getCategory()
	{
		return MacroManager.MacroCommandCategory;
	}

	public String getCategoryID()
	{
		return MacroManager.MacroCommandCategoryID;
	}

	public String getDescription()
	{
		return "Insert a single character into the document, as though by typing\n"+getName();
	}

	public String getName()
	{
		return (mKeyEvent.type==SWT.KeyDown? "Key down" : "Key up")+" ("+mKeyEvent.character+")";
	}

	public boolean isConfigurable()
	{
		return false;
	}

	public IMacroCommand createFrom(Element commandElement)
	{
		Map<String, String> attrMap=new HashMap<String, String>();
		Set<String> attrKeys=new HashSet<String>();
		attrKeys.add(XML_ATTR_KEYCODE);
		attrKeys.add(XML_ATTR_STATEMASK);
		attrKeys.add(XML_ATTR_TYPE);
		attrKeys.add(XML_ATTR_CHARACTER);

//		Map<String, String> dataMap=new HashMap<String, String>();
//		Set<String> dataKeys=new HashSet<String>();
//		dataKeys.add(XML_SearchString_Tag);
//		dataKeys.add(XML_ReplaceString_Tag);
		
		Utilities.getCommandData(commandElement, attrKeys, null, attrMap, null);
		
		Event event=new Event();
		KeystrokeCommand newCommand=new KeystrokeCommand(event);
		
		String value=attrMap.get(XML_ATTR_KEYCODE);
		if (value!=null && value.length()>0)
		{
			event.keyCode=Integer.parseInt(value);
		}
		value=attrMap.get(XML_ATTR_STATEMASK);
		if (value!=null && value.length()>0)
		{
			event.stateMask=Integer.parseInt(value);
		}
		value=attrMap.get(XML_ATTR_TYPE);
		if (value!=null && value.length()>0)
		{
			event.type=Integer.parseInt(value);
		}
		value=attrMap.get(XML_ATTR_CHARACTER);
		if (value!=null && value.length()>0)
		{
			event.character=(char)Integer.parseInt(value);
		}
		
		return newCommand;
	}
	
	public void persist(Document doc, Element commandElement)
	{
		Map<String, String> attrMap=new HashMap<String, String>();
		attrMap.put(XML_ATTR_KEYCODE, Integer.toString(mKeyEvent.keyCode));
		attrMap.put(XML_ATTR_STATEMASK, Integer.toString(mKeyEvent.stateMask));
		attrMap.put(XML_ATTR_TYPE, Integer.toString(mKeyEvent.type));
		attrMap.put(XML_ATTR_CHARACTER, Integer.toString(mKeyEvent.character));
		
//		Map<String, String> dataMap=new HashMap<String, String>();
//		if (mSearchString!=null)
//			dataMap.put(XML_SearchString_Tag, mSearchString);
//		if (mReplaceString!=null)
//			dataMap.put(XML_ReplaceString_Tag, mReplaceString);
		
		Utilities.persistCommand(doc, commandElement, XML_Macro_Keystroke_Type, attrMap, null);
	}

	public boolean requiresPost()
	{
		return true;
	}
	
}
