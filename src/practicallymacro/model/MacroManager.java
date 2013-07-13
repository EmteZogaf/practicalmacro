package practicallymacro.model;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.Command;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.keys.IBindingService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import practicallymacro.actions.RecordCommandAction;
import practicallymacro.commands.IMacroCommand;
import practicallymacro.commands.IMacroScriptSupport;
import practicallymacro.commands.MacroHandler;
import practicallymacro.commands.MacroScriptCommand;
import practicallymacro.editormacros.Activator;
import practicallymacro.preferences.Initializer;
import practicallymacro.util.Utilities;


public class MacroManager
{
	public static final String MacroCommandCategory="PracticallyMacro utility command";
	public static final String MacroCommandCategoryID="practicallymacro.category.utility.command";
	public static final String UserMacroCategoryID = "practicallymacro.category.usermacros";
	public static final String UserMacroCategoryName = "User defined editor macros";
	private static MacroManager mManager;
//	private Map<String, Long> mUsedMacroTimeStamps;
	private Map<String, EditorMacro> mMacroMap;
	private List<EditorMacro> mTemporaryMacros;
//	private int mTempMacroCount=20; //throw away more than 20
	private Map<String, IMacroCommand> mXMLCommandHandlerMap=new HashMap<String, IMacroCommand>();
//	private MacroRunListener mRunListener=new MacroRunListener();
	private List<EditorMacro> mMacroStack;
	private int mCommandsExecuted;
	private int mRecordingMark;
	private String mCurrentMacroState;
	
	private static int mUniqueSessionMacroID=1;
	
	private boolean mMacroDebugMode;
	
	private Map<String, IMacroScriptSupport> mScriptSupportMap; 
	
	private static int mNextTempMacroIndex=1;
	private int mLastUsedMacroSessionID;
	
	public static final String State_Idle="MACROSTATE_IDLE";
	public static final String State_Recording="MACROSTATE_RECORDING";
	public static final String State_Playing="MACROSTATE_PLAYING";
	
	public static final String XML_Macros_Tag="EditorMacros";
	public static final String XML_Macro_Tag="Macro";
	public static final String XML_Name_Tag="name";
	public static final String XML_LastUsed_Attr="lastUsed";
	public static final String XML_Compound_Attr="runAsCompound";
	public static final String XML_ID_Tag="id";
	public static final String XML_Description_Tag="description";
	public static final String XML_Command_Tag="Command";
	public static final String XML_CommandType_ATTR="type";
	public static final String PREF_USER_MACRO_DEFINITIONS="Preference_UserMacroDefinitions";
	
	public static final int Macro_Event=2345; //some number that doesn't conflict with SWT event constants
	
	private MacroManager()
	{
//		mUsedMacroTimeStamps=new HashMap<String, Long>();
		mMacroMap=new HashMap<String, EditorMacro>(); //load from preferences
		mTemporaryMacros=new ArrayList<EditorMacro>();
		mMacroStack=new ArrayList<EditorMacro>();
		mScriptSupportMap=new HashMap<String, IMacroScriptSupport>();
		mLastUsedMacroSessionID=0;
		mCurrentMacroState=State_Idle;
		mMacroDebugMode=false;
	}
	
	public static boolean isNull()
	{
		return mManager==null;
	}
	
	public int getNextMacroSessionID()
	{
		int id=mUniqueSessionMacroID;
		mUniqueSessionMacroID++;
		return id;
	}
	
	public static MacroManager getManager()
	{
		if (mManager==null)
		{
			mManager=new MacroManager();
			
			//macro script plugins
		    IExtensionPoint point=Platform.getExtensionRegistry().getExtensionPoint(Activator.PLUGIN_ID, "scriptingSupport");
			if (point!=null)
			{
				IExtension[] extensions=point.getExtensions();
				for (int i = 0; i < extensions.length; i++)
				{
					IExtension extension = extensions[i];
					IConfigurationElement[] elements=extension.getConfigurationElements();
					for (int j = 0; j < elements.length; j++)
					{
						IConfigurationElement configurationElement = elements[j];
						if (configurationElement.getName().equals("scriptSupport"))
						{
							String supportClass=configurationElement.getAttribute("class");
							if (supportClass!=null)
							{
								try {
									IMacroScriptSupport scriptSupport=(IMacroScriptSupport)configurationElement.createExecutableExtension("class");
									if (scriptSupport!=null)
									{
										mManager.mScriptSupportMap.put(scriptSupport.getID(), scriptSupport);
									}
								} catch (CoreException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}
				}
			}			
			
			//register xml handlers
		    point=Platform.getExtensionRegistry().getExtensionPoint(Activator.PLUGIN_ID, "xmlCommandHandlers");
			if (point!=null)
			{
				IExtension[] extensions=point.getExtensions();
				for (int i = 0; i < extensions.length; i++)
				{
					IExtension extension = extensions[i];
					IConfigurationElement[] elements=extension.getConfigurationElements();
					for (int j = 0; j < elements.length; j++)
					{
						IConfigurationElement configurationElement = elements[j];
						if (configurationElement.getName().equals("XMLCommandHandler"))
						{
							String handlerType=configurationElement.getAttribute("type");
							String handlerClass=configurationElement.getAttribute("class");
							if (handlerClass!=null && handlerType!=null)
							{
								try {
									IMacroCommand macroCommand=(IMacroCommand)configurationElement.createExecutableExtension("class");
									if (macroCommand!=null)
									{
										mManager.mXMLCommandHandlerMap.put(handlerType, macroCommand);
									}
								} catch (CoreException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
			
			mManager.loadMacros();
		}
		return mManager;
	}
	
//	public void markMacroUsed(EditorMacro macro)
//	{
//		mUsedMacroTimeStamps.put(macro.getID(), new Long(System.currentTimeMillis()));
//	}
	
	private static class AgeSorter implements Comparator<EditorMacro>
	{
		public int compare(EditorMacro o1, EditorMacro o2)
		{
			//sort with latest timestamps at top
			long t1=o1.getLastUse();
			long t2=o2.getLastUse();
			if (t1<t2)
				return 1;
			else if (t1>t2)
				return -1;
			return 0;
		}
	}
	
	public List<EditorMacro> getUsedMacros(int count)
	{
		List<EditorMacro> list=new ArrayList<EditorMacro>();
//		List<String> commandIDs=new ArrayList<String>();
		list.addAll(mMacroMap.values());
		Collections.sort(list, new AgeSorter());
		if (count<0)
			return list;
		return list.subList(0, Math.min(count, list.size()));
//		for (String commandID : commandIDs)
//		{
//			if (list.size()>=count)
//				break;
//			
//			EditorMacro macro=mMacroMap.get(commandID);
//			if (macro!=null)
//			{
//				list.add(macro);
//			}
//		}
//		
//		return list;
	}
	
	protected Set<String> getMacroNames()
	{
		Set<String> results=new HashSet<String>();
		for (EditorMacro macro : mMacroMap.values()) {
			results.add(macro.getName());
		}
		for (EditorMacro macro : mTemporaryMacros) {
			results.add(macro.getName());
		}
		return results;
	}
	
	public String getUniqueMacroName()
	{
		Set<String> macroNames=MacroManager.getManager().getMacroNames();
		int num=mNextTempMacroIndex;
		while (true)
		{
			String name="User-defined macro "+num;
			if (!macroNames.contains(name))
			{
				mNextTempMacroIndex=num+1;
				return name;
			}
			num++;
		}
	}
	
	
	public EditorMacro getMacro(String id)
	{
		return mMacroMap.get(id);
	}
	
	public void addMacro(EditorMacro macro)
	{
		if (macro.getSessionID()<=0)
			macro.setSessionID(getNextMacroSessionID());
		if (macro.getID().length()>0)
		{
			mMacroMap.put(macro.getID(), macro);
			
			ICommandService cs = MacroManager.getOldCommandService();
			Category category=cs.getCategory(MacroManager.UserMacroCategoryID);
			Command newCommand=cs.getCommand(macro.getID());
			newCommand.define(macro.getName(), macro.getDescription(), category);
			MacroHandler handler=new MacroHandler(macro.getID());
			IHandlerService hs=MacroManager.getOldHandlerService();
			if (hs!=null)
				hs.activateHandler(newCommand.getId(), handler);
//			else
				newCommand.setHandler(handler);
			
			try
			{
				Class e4Helper=Class.forName("practicallymacro.model.E4CommandHelper");
				Method helperMethod=e4Helper.getMethod("addMCommandToSystem", Command.class);
				helperMethod.invoke(e4Helper, newCommand);
			}
			catch (Throwable t)
			{
				t.printStackTrace();
			}
			
			
//			IEclipseContext context=(IEclipseContext)PlatformUI.getWorkbench().getAdapter(IEclipseContext.class);
//			try
//			{
//				Class commandsFactory=Class.forName("org.eclipse.e4.ui.model.application.commands.impl.CommandsFactoryImpl");
//				Field instanceField=commandsFactory.getField("INSTANCE");
//				Method createCommand=commandsFactory.getMethod("createCommand");
//				Object implInstance=instanceField.get(null);
//				MCommand anMCommand=(MCommand)createCommand.invoke(implInstance);
//	//			MCommand anMCommand=CommandsFactoryImpl.INSTANCE.createCommand();
//				anMCommand.setElementId(newCommand.getId());
//				try
//				{
//					anMCommand.setCommandName(newCommand.getName());
//				}
//				catch (Exception e)
//				{
//					e.printStackTrace();
//				}
//				MApplication app=context.get(MApplication.class);
//				List<MCommand> allCommands=app.getCommands();
//				allCommands.add(anMCommand);
//			}
//			catch (Exception e)
//			{
//				e.printStackTrace();
//			}
			
//            IBindingService bindingService=context.get(IBindingService.class);
//            TriggerSequence[] allKeys=bindingService.getActiveBindingsFor(newCommand.getId());
//            System.out.println("nothing");
			
		}
		else
		{
			int maxTempCount=Activator.getDefault().getPreferenceStore().getInt(Initializer.Pref_MaximumTempMacroCount);
			if (mTemporaryMacros.size()>=maxTempCount)
			{
				//sort and throw away extras; maybe prompt for save here?
				Collections.sort(mTemporaryMacros, new AgeSorter());
				mTemporaryMacros.subList(maxTempCount-1, mTemporaryMacros.size()).clear();
			}
			mTemporaryMacros.add(macro);
		}
	
	}
	
//	public static ECommandService getCommandService()
//	{
//		IEclipseContext context=(IEclipseContext)PlatformUI.getWorkbench().getAdapter(IEclipseContext.class);
//		if (context!=null)
//		{
//			return (ECommandService)context.get(ECommandService.class);
//		}
//		return null;
//	}
	
	public static ICommandService getOldCommandService()
	{
		ICommandService cs = (ICommandService) PlatformUI.getWorkbench().getAdapter(ICommandService.class);
		return cs;
	}
	
//	public static EHandlerService getHandlerService()
//	{
//		IEclipseContext context=(IEclipseContext)PlatformUI.getWorkbench().getAdapter(IEclipseContext.class);
//		if (context!=null)
//		{
//			return (EHandlerService)context.get(EHandlerService.class);
//		}
//		return null;
//	}
	
	public static IHandlerService getOldHandlerService()
	{
		IHandlerService hs = (IHandlerService) PlatformUI.getWorkbench().getAdapter(IHandlerService.class);
		return hs;
	}
	
	public void deleteMacro(EditorMacro macro)
	{
		mMacroMap.remove(macro.getID());

		ICommandService cs = MacroManager.getOldCommandService();
		Command c=cs.getCommand(macro.getID());
		if (c!=null && c.isDefined())
			c.undefine();
	}

	
	public EditorMacro getCurrentMacro()
	{
		if (mMacroStack.size()==0)
			return null;
		
		return mMacroStack.get(mMacroStack.size()-1);
	}
	
	public boolean incrementCommandExecutedCount()
	{
		mCommandsExecuted++;
		if (mCommandsExecuted<1000)
			return true;
		return false;
	}
	
	public void pushMacro(EditorMacro currentMacro)
	{
		mMacroStack.add(currentMacro);
	}
	
	public void popMacro()
	{
		if (mMacroStack.size()>0)
		{
			mMacroStack.remove(mMacroStack.size()-1);
		}
		
		if (mMacroStack.size()==0)
			mCommandsExecuted=0;
	}
	
	public static Document createDocument(Collection<EditorMacro> macros)
	{
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		try
		{
	        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	        Document doc = docBuilder.newDocument();
	        
	        //create the root element and add it to the document
            Element root = doc.createElement(XML_Macros_Tag);
            doc.appendChild(root);
            
            for (EditorMacro macro : macros)
            {
                Element child = doc.createElement(XML_Macro_Tag);
                child.setAttribute(XML_Name_Tag, macro.getName());
                child.setAttribute(XML_ID_Tag, macro.getID());
                child.setAttribute(XML_LastUsed_Attr, Long.toString(macro.getLastUse()));
                child.setAttribute(XML_Compound_Attr, Boolean.toString(macro.isRunAsCompoundEvent()));
                Element descNode = doc.createElement(XML_Description_Tag);
                child.appendChild(descNode);
                descNode.setTextContent(macro.getDescription());
                macro.persist(doc, child);
                root.appendChild(child);
			}
            
            return doc;
	        
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{}
		return null;
	}
	
	public static String outputXML(Document doc)
	{
		try
		{
            //set up a transformer
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");

            //create string from xml tree
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(doc);
            trans.transform(source, result);
            String xmlString = sw.toString();
            return xmlString;
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{}
		return null;
	}
	
	public static String persistMacros(Collection<EditorMacro> macros)
	{
        Document doc=createDocument(macros);
        return outputXML(doc);
//		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
//		try
//		{
//	        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
//	        Document doc = docBuilder.newDocument();
//	        
//	        //create the root element and add it to the document
//            Element root = doc.createElement(XML_Macros_Tag);
//            doc.appendChild(root);
//            
//            for (EditorMacro macro : macros)
//            {
//                Element child = doc.createElement(XML_Macro_Tag);
//                child.setAttribute(XML_Name_Tag, macro.getName());
//                child.setAttribute(XML_ID_Tag, macro.getID());
//                child.setAttribute(XML_LastUsed_Attr, Long.toString(macro.getLastUse()));
//                Element descNode = doc.createElement(XML_Description_Tag);
//                child.appendChild(descNode);
//                descNode.setTextContent(macro.getDescription());
//                macro.persist(doc, child);
//                root.appendChild(child);
//			}
//            
//            
//            //set up a transformer
//            TransformerFactory transfac = TransformerFactory.newInstance();
//            Transformer trans = transfac.newTransformer();
//            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
//            trans.setOutputProperty(OutputKeys.INDENT, "yes");
//
//            //create string from xml tree
//            StringWriter sw = new StringWriter();
//            StreamResult result = new StreamResult(sw);
//            DOMSource source = new DOMSource(doc);
//            trans.transform(source, result);
//            String xmlString = sw.toString();
//            return xmlString;
//	        
////		}
////		catch (ParserConfigurationException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
//		} catch (TransformerConfigurationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (TransformerException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		finally{}
//		return null;
	}
	
	public void saveMacros()
	{
		List<EditorMacro> filteredMacros=new ArrayList<EditorMacro>();
		for (EditorMacro macro : mMacroMap.values()) {
			if (!macro.isContributed())
				filteredMacros.add(macro);
		}
		String xmlString=persistMacros(filteredMacros);
		if (xmlString!=null)
		{
    		IPreferenceStore prefStore=Activator.getDefault().getPreferenceStore();
            prefStore.setValue(PREF_USER_MACRO_DEFINITIONS, xmlString);
            System.out.println("Macro defs:"+xmlString);
		}
		
//		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
//		try
//		{
//	        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
//	        Document doc = docBuilder.newDocument();
//	        
//	        //create the root element and add it to the document
//            Element root = doc.createElement(XML_Macros_Tag);
//            doc.appendChild(root);
//            
//            for (EditorMacro macro : mMacroMap.values())
//            {
//                Element child = doc.createElement(XML_Macro_Tag);
//                child.setAttribute(XML_Name_Tag, macro.getName());
//                child.setAttribute(XML_ID_Tag, macro.getID());
//                child.setAttribute(XML_LastUsed_Attr, Long.toString(macro.getLastUse()));
//                Element descNode = doc.createElement(XML_Description_Tag);
//                child.appendChild(descNode);
//                descNode.setTextContent(macro.getDescription());
//                macro.persist(doc, child);
//                root.appendChild(child);
//			}
//            
//            
//            //set up a transformer
//            TransformerFactory transfac = TransformerFactory.newInstance();
//            Transformer trans = transfac.newTransformer();
//            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
//            trans.setOutputProperty(OutputKeys.INDENT, "yes");
//
//            //create string from xml tree
//            StringWriter sw = new StringWriter();
//            StreamResult result = new StreamResult(sw);
//            DOMSource source = new DOMSource(doc);
//            trans.transform(source, result);
//            String xmlString = sw.toString();
//    		IPreferenceStore prefStore=Activator.getDefault().getPreferenceStore();
//            prefStore.setValue(PREF_USER_MACRO_DEFINITIONS, xmlString);
//            System.out.println("Macro defs:"+xmlString);
//	        
//		} catch (ParserConfigurationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (TransformerConfigurationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (TransformerException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		finally{}
	}
	
//	public static void registerXMLHandler(String name, IMacroCommand handler)
//	{
//		mXMLCommandHandlerMap.put(name, handler);
//	}
	
	public Collection<EditorMacro> readMacros(Reader xmlReader)
	{
		Collection<EditorMacro> results=new ArrayList<EditorMacro>();
		
		Utilities.createStyledTextCommands();
    	try
    	{
        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        	DocumentBuilder loader = factory.newDocumentBuilder();
        	Document document = loader.parse(new InputSource(xmlReader));
        	Element macros = document.getDocumentElement();
        	NodeList macroList=macros.getChildNodes();
        	for (int i=0;i<macroList.getLength();i++)
        	{
        		Node macroNode=macroList.item(i);
        		if (!(macroNode instanceof Element))
        			continue;
        		
        		Element macroEl=(Element)macroNode;
        		String name=macroEl.getAttribute(XML_Name_Tag);
        		String id=macroEl.getAttribute(XML_ID_Tag);
        		boolean runAsCompound=Boolean.parseBoolean(macroEl.getAttribute(XML_Compound_Attr));
        		String timeString=macroEl.getAttribute(XML_LastUsed_Attr);
        		String desc="";
        		NodeList macroChildren=macroNode.getChildNodes();
        		for (int k=0;k<macroChildren.getLength();k++)
        		{
        			Node testNode=macroChildren.item(k);
        			if (testNode.getNodeName().equals(XML_Description_Tag))
        			{
        				desc=testNode.getTextContent();
        				break;
        			}
        		}
        		
        		if (id.length()>0 && name.length()>0)
        		{
        			//construct macro
        			List<IMacroCommand> commands=new ArrayList<IMacroCommand>();
        			
	        		for (int k=0;k<macroChildren.getLength();k++)
	        		{
	        			Node commandNode=macroChildren.item(k);
	        			if (commandNode.getNodeName().equals(XML_Command_Tag))
	        			{
	        				NamedNodeMap commandAttrs=commandNode.getAttributes();
		        			Node typeNode=commandAttrs.getNamedItem(XML_CommandType_ATTR);
		        			if (typeNode!=null)
		        			{
		        				String typeString=typeNode.getNodeValue();
		        				IMacroCommand macroCommand=mXMLCommandHandlerMap.get(typeString);
		        				if (macroCommand!=null)
		        				{
		        					IMacroCommand newCommand=macroCommand.createFrom((Element)commandNode);
		        					if (newCommand!=null)
		        						commands.add(newCommand);
		        				}
		        			}
	        				
	        			}
	        		}
	        		
        			EditorMacro newMacro=new EditorMacro(commands, id, name, desc);
        			newMacro.setRunAsCompoundEvent(runAsCompound);
        			if (timeString.length()>0)
        			{
        				try
        				{
        					newMacro.setLastUse(Long.parseLong(timeString));
        				}
        				catch (NumberFormatException e)
        				{
        					e.printStackTrace();
        				}
        			}
	        		
        			results.add(newMacro);
        		}
        		else
        		{
        			continue;
        		}
        		
        		
        	}
        	
    	} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	finally{}
    	
    	return results;
    }
	
	public void loadMacros()
	{
		IPreferenceStore prefStore=Activator.getDefault().getPreferenceStore();
        String xmlString=prefStore.getString(PREF_USER_MACRO_DEFINITIONS);
        if (xmlString!=null && xmlString.length()>0)
        {
            Collection<EditorMacro> readCommands=readMacros(new StringReader(xmlString));
            for (EditorMacro editorMacro : readCommands)
            {
				addMacro(editorMacro);
			}
            
//            IEclipseContext context=(IEclipseContext)PlatformUI.getWorkbench().getAdapter(IEclipseContext.class);
//            final IBindingService bindingService=context.get(IBindingService.class);
            final IBindingService bindingService = (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
            if (bindingService!=null)
            {
				Display.getDefault().asyncExec(new Runnable() {
		               public void run() {
		            	   try {
		            		   bindingService.savePreferences(bindingService.getActiveScheme(), bindingService.getBindings());
		            	   } catch (IOException e) {
		            		   // TODO Auto-generated catch block
		            		   e.printStackTrace();
		            	   }
		               }
		            });
            }
        }
        
        //get macros from extension point
	    IExtensionPoint point=Platform.getExtensionRegistry().getExtensionPoint(Activator.PLUGIN_ID, "defineMacro");
		if (point!=null)
		{
			IExtension[] extensions=point.getExtensions();
			for (int i = 0; i < extensions.length; i++)
			{
				IExtension extension = extensions[i];
				IConfigurationElement[] elements=extension.getConfigurationElements();
				for (int j = 0; j < elements.length; j++)
				{
					IConfigurationElement configurationElement = elements[j];
					if (configurationElement.getName().equals("macroDefinition"))
					{
						String rawXML=configurationElement.getValue();
						if (rawXML!=null)
						{
				            Collection<EditorMacro> readCommands=readMacros(new StringReader(rawXML));
				            for (EditorMacro editorMacro : readCommands)
				            {
				            	if (!mMacroMap.containsKey(editorMacro.getID()))
				            	{
				            		editorMacro.setContributed(true);
				            		addMacro(editorMacro);
				            	}
				            	else
				            	{
				            		System.out.println("Skipping duplicate macro id: "+editorMacro.getID());
				            	}
							}
						}
					}
				}
			}
		}
        
	}
	
	public Map<Integer, EditorMacro> getUniqueMacroMap()
	{
		Map<Integer, EditorMacro> map=new HashMap<Integer, EditorMacro>();
		for (EditorMacro macro : mMacroMap.values())
		{
			map.put(macro.getSessionID(), macro);
		}
		
		for (EditorMacro macro : mTemporaryMacros) {
			map.put(macro.getSessionID(), macro);
		}
		
		return map;
	}

	public List<EditorMacro> getAllMacros()
	{
		List<EditorMacro> allMacros=new ArrayList<EditorMacro>();
		allMacros.addAll(mMacroMap.values());
		return allMacros;
	}

	public List<EditorMacro> getUsedTempMacros(int count)
	{
		List<EditorMacro> list=new ArrayList<EditorMacro>();
		list.addAll(mTemporaryMacros);
		Collections.sort(list, new AgeSorter());
		if (count<0)
			return list;
		
		return list.subList(0, Math.min(count, list.size()));
	}

	public void replaceDefinedMacros(Map<Integer, EditorMacro> updatedMacros)
	{
		//determine new/deleted/changed commands
		//new- add them
		//deleted- undefine them, possibly also attempting to remove key bindings
		//changed - just define them again.
		
		//collect the keys for the current set of macros
		Set<EditorMacro> currentKeys=new HashSet<EditorMacro>();
		currentKeys.addAll(mMacroMap.values());
		
		List<EditorMacro> newTempMacros=new ArrayList<EditorMacro>();
		
		//look for added or changed macros
		Map<String, EditorMacro> newMacroMap=new HashMap<String, EditorMacro>();
		for (Map.Entry<Integer, EditorMacro> entry: updatedMacros.entrySet())
		{
			EditorMacro newMacro=entry.getValue();
			
			if (newMacro.getID().length()==0)
			{
				//doesn't matter if macro existed before or not
				newTempMacros.add(newMacro);
			}
			else
			{
				//just redefine the macro, which will redefine the command for the id
				addMacro(newMacro);
				newMacroMap.put(newMacro.getID(), newMacro);
			}
		}
		
		//now, look for deleted macros
		ICommandService cs = MacroManager.getOldCommandService();
		for (EditorMacro existingMacro : currentKeys)
		{
			//see if current map contains the command from the new set.  If not, then we need to delete the command
			EditorMacro newMacro=newMacroMap.get(existingMacro.getID());
			if (newMacro==null)
			{
				//this macro was in the old map but not in the new one
				try
				{
					//undefine the command
					Command commandToDelete=cs.getCommand(existingMacro.getID());
					commandToDelete.undefine();
					mMacroMap.remove(existingMacro.getID());
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
			}
		}
		
		//The session id shouldn't have changed.  Will either be valid or invalid, which 
		//should be okay either way and appropriate if the last used command was deleted.
//		EditorMacro lastMacro=Activator.getDefault().getLastMacro();
//		if (lastMacro!=null)
//		{
//			EditorMacro updatedVersion=getMacro(lastMacro.getID());
//			if (updatedVersion!=null)
//			{
//				Activator.getDefault().setLastMacro(updatedVersion);
//			}
//		}

		mTemporaryMacros=newTempMacros;
	}
	
	public void setLastMacro(EditorMacro macro)
	{
		if (macro.getSessionID()>0)
			mLastUsedMacroSessionID=macro.getSessionID();
	}
	
	public EditorMacro getLastMacro()
	{
		if (mLastUsedMacroSessionID>0)
		{
			Map<Integer, EditorMacro> map=getUniqueMacroMap();
			return map.get(mLastUsedMacroSessionID);
		}
		
		return null;
	}

//	public Listener getMacroRunListener()
//	{
//		return mRunListener;
//	}
	
//	static class MacroRunListener implements Listener
//	{
//		@Override
//		public void handleEvent(Event event)
//		{
//			if (event.data instanceof IMacroCommand)
//			{
//				IMacroCommand command=(IMacroCommand)event.data;
//				command.execute(null);
//			}
//		}
//	}
	
	public boolean isRecordingRawKeystrokes()
	{
		return Activator.getDefault().getPreferenceStore().getBoolean(Initializer.Pref_RecordRawCharacterKeys);
	}
	
	public MacroRecorder getRecorder()
	{
		return RecordCommandAction.getRecorder();
	}

	public void clearMacroStack()
	{
		mMacroStack.clear();
		mCommandsExecuted=0;
	}

	public int getRecordingMark()
	{
		return mRecordingMark;
	}
	
	public void setRecordingMark(int mark)
	{
		mRecordingMark=mark;
	}

	public void moveMarkOnDelete(boolean recordMode, int offset, int length)
	{
		if (recordMode)
		{
			mRecordingMark=EditorMacro.moveMarkOnDelete(mRecordingMark, offset, length);
		}
		else
		{
			for (EditorMacro macro : mMacroStack) {
				macro.moveMarkOnDelete(offset, length);
			}
		}
		
	}

	public void moveMarkOnInsert(boolean recordMode, int offset, int length)
	{
		if (recordMode)
		{
			mRecordingMark=EditorMacro.moveMarkOnInsert(mRecordingMark, offset, length);
		}
		else
		{
			for (EditorMacro macro : mMacroStack)
			{
				macro.moveMarkOnInsert(offset, length);
			}
		}
	}
	
	/**
	 * @return new copies of standard script commands
	 */
	public List<IMacroCommand> getCanonicalScriptCommands()
	{
		List<IMacroCommand> results=new ArrayList<IMacroCommand>();
		
		for (IMacroScriptSupport supportObj : mScriptSupportMap.values()) {
			MacroScriptCommand command=new MacroScriptCommand(supportObj, null);
			results.add(command);
		}
		
		return results;
	}
	
	public IMacroScriptSupport getScriptProxy(String proxyID)
	{
		return mScriptSupportMap.get(proxyID);
	}
	
	public String getMacroState()
	{
		return mCurrentMacroState;
	}

	public void setMacroState(String newState)
	{
		mCurrentMacroState=newState;
	}

	public boolean isMacroDebugMode() {
		return mMacroDebugMode;
	}

	public void setMacroDebugMode(boolean macroDebugMode) {
		mMacroDebugMode = macroDebugMode;
	}
	
}
