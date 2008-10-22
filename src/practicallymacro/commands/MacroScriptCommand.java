package practicallymacro.commands;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import practicallymacro.model.MacroManager;
import practicallymacro.util.MacroConsole;
import practicallymacro.util.Utilities;


public class MacroScriptCommand implements IMacroCommand {

	private String mScript;
	private IMacroScriptSupport mScriptLanguageProxy;
	public static final String XML_Script_Tag="script";
	public static final String XML_ScriptType_Attr="scriptType";
	public static final String XML_MacroScriptType="MacroScriptCommand";
	
	public MacroScriptCommand(IMacroScriptSupport supportObject, String script)
	{
		mScript=script;
		mScriptLanguageProxy=supportObject;
	}
	
	public MacroScriptCommand()
	{
		mScript=null;
	}

	public void configure(Shell shell)
	{
		mScript=mScriptLanguageProxy.editScript(mScript, shell);
//		ScriptConfigureDialog dlg=new ScriptConfigureDialog(shell, mScript);
//		if (dlg.open()==Dialog.OK)
//		{
//			mScript=dlg.getScript();
//		}
	}

	public IMacroCommand copy()
	{
		IMacroCommand command=new MacroScriptCommand(mScriptLanguageProxy, mScript);
		return command;
	}

	public void dump()
	{
		System.out.println("Macro Script("+mScriptLanguageProxy.getIDDisplayString()+"): "+mScript);
	}

	public boolean execute(IEditorPart target)
	{
		// run script using the language script
		final Thread mainThread=Thread.currentThread();
		Thread scriptTimeoutThread=new Thread(new Runnable()
		{
			public void run()
			{
				try {
					Thread.sleep(8000);
				}
				catch (InterruptedException e)
				{
					return;
				}
				
				MacroConsole.getConsole().writeln("Script took too long to run; attempting to kill");
				mainThread.interrupt();
//				try {
//					mainThread.join(2000);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				mainThread.stop();
				
			}
		});
		
		scriptTimeoutThread.start();
		
		try
		{
			boolean returnVal=mScriptLanguageProxy.evaluate(mScript, target);
			return returnVal;
		}
		catch (Exception e)
		{
			if (e instanceof InterruptedException)
			{
				MacroConsole.getConsole().writeln("Script took too much time");
			}
			else
			{
				MacroConsole.getConsole().write(e);
			}
			return false;
		}
		finally
		{
			//join on timeout thread here too just to make sure that we don't leave it hanging around 
			try {
				scriptTimeoutThread.interrupt();
				scriptTimeoutThread.join();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public String getCategory()
	{
		return MacroManager.MacroCommandCategory;
	}

	public String getDescription()
	{
		StringBuffer buffer=new StringBuffer();
		buffer.append("Script of editor functionality");
		if (mScript!=null)
		{
			buffer.append(": \n"+mScript);
		}
		else
		{
			buffer.append(": No script provided");
		}
		return buffer.toString();
	}

	public String getName()
	{
		return "Editor Macro script ("+mScriptLanguageProxy.getIDDisplayString()+"):"+(mScript==null? "no script" : "script provided");
	}

	public boolean isConfigurable()
	{
		return true;
	}

	public void persist(Document doc, Element commandElement)
	{
		Map<String, String> dataMap=new HashMap<String, String>();
		if (mScript!=null)
			dataMap.put(XML_Script_Tag, mScript);
		Map<String, String> attrMap=new HashMap<String, String>();
		attrMap.put(XML_ScriptType_Attr, mScriptLanguageProxy.getID());
		Utilities.persistCommand(doc, commandElement, XML_MacroScriptType, attrMap, dataMap);
	}

	public IMacroCommand createFrom(Element commandElement)
	{
		Map<String, String> attrMap=new HashMap<String, String>();
		Set<String> attrKeys=new HashSet<String>();
		attrKeys.add(XML_ScriptType_Attr);

		Map<String, String> dataMap=new HashMap<String, String>();
		Set<String> dataKeys=new HashSet<String>();
		dataKeys.add(XML_Script_Tag);
		
		Utilities.getCommandData(commandElement, attrKeys, dataKeys, attrMap, dataMap);
		
		String scriptType=attrMap.get(XML_ScriptType_Attr);
		String script=dataMap.get(XML_Script_Tag);
		
		if (scriptType!=null)
		{
			IMacroScriptSupport proxy=MacroManager.getManager().getScriptProxy(scriptType);
			if (proxy!=null)
			{
				return new MacroScriptCommand(proxy, script);
			}
		}
		
		return null;
		
		
//		NodeList children=commandElement.getChildNodes();
//		for (int i=0;i<children.getLength();i++)
//		{
//			if (children.item(i).getNodeName().equals(XML_Script_Tag))
//			{
//				String data=children.item(i).getTextContent();
//				IMacroCommand newCommand=new MacroScriptCommand(data);
//				return newCommand;
//			}			
//		}
//		
//		return new MacroScriptCommand();
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
