package practicallymacro.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import practicallymacro.commands.EclipseCommand;
import practicallymacro.commands.IMacroCommand;
import practicallymacro.commands.InsertStringCommand;
import practicallymacro.dialogs.MacroCommandDebugInfo;
import practicallymacro.dialogs.MacroDebugDialog;
import practicallymacro.editormacros.Activator;
import practicallymacro.preferences.Initializer;
import practicallymacro.util.MacroConsole;
import practicallymacro.util.Utilities;


public class EditorMacro {
	private List<IMacroCommand> mCommands;
	private String mName;
	private String mDescription;
	private String mID;
	private long mLastUse;
	private int mMark=0;
	private boolean mIsContributed;
	private int mSessionID=0;
	private boolean mRunAsCompoundEvent;
	
	private static int mMaxMacroSize=1000;
	
	public EditorMacro(List<IMacroCommand> commands, String id, String name, String desc)
	{
		mCommands=new ArrayList<IMacroCommand>();
		mCommands.addAll(commands);
		mDescription=desc;
		mName=name;
		mID=id;
		mLastUse=System.currentTimeMillis();
		mIsContributed=false;
		mRunAsCompoundEvent=true;
	}
	
	public void setContributed(boolean contributed)
	{
		mIsContributed=contributed;
	}
	
	public boolean isContributed()
	{
		return mIsContributed;
	}
	
	public boolean isRunAsCompoundEvent() {
		return mRunAsCompoundEvent;
	}

	public void setRunAsCompoundEvent(boolean runAsCompoundEvent) {
		mRunAsCompoundEvent = runAsCompoundEvent;
	}

	public void run(final IEditorPart editor)
	{
		if (editor==null)
		{
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Execute macro", "Cannot execute macro: the view with focus is not a text editor.");
			return;
		}
		
		boolean debugMode=MacroManager.getManager().isMacroDebugMode();
		boolean saveWriteMode=Activator.getDefault().getPreferenceStore().getBoolean(Initializer.Pref_WriteToMacroConsole);
		if (debugMode)
		{
			Activator.getDefault().getPreferenceStore().setValue(Initializer.Pref_WriteToMacroConsole, true);
		}
		
		boolean atomicExecution=!debugMode && isRunAsCompoundEvent(); //Activator.getDefault().getPreferenceStore().getBoolean(Initializer.Pref_ExecuteMacrosAtomically);
		ITextViewer viewer =(ITextViewer)editor.getAdapter(ITextOperationTarget.class);
		ITextViewerExtension viewExtension=null;
		if (viewer instanceof ITextViewerExtension)
		{
			viewExtension=(ITextViewerExtension)viewer;
		}
		  
		try
		{
			if (atomicExecution && viewExtension!=null)
			{
				viewExtension.getRewriteTarget().beginCompoundChange();
			}
			
	//		StyledText widget=Utilities.getStyledText(editor);
	//		widget.removeListener(MacroManager.Macro_Event, MacroManager.getManager().getMacroRunListener());
	//		widget.addListener(MacroManager.Macro_Event, MacroManager.getManager().getMacroRunListener());
	//		MacroManager.getManager().setMark(widget.getCaretOffset());
	
			MacroManager.getManager().setLastMacro(this);
			mLastUse=System.currentTimeMillis();
			
			List<Object> flattenedList=new ArrayList<Object>();
			flattenMacro(flattenedList);
			
			MacroConsole.getConsole().writeln("Executing macro: "+getID(), MacroConsole.Type_Standard);
			
			if (flattenedList.size()>mMaxMacroSize)
			{
				//TODO: error dialog
				return;
			}
			
			try
			{
				final MarkUpdater markUpdater=new MarkUpdater(false);
				final IDocument document=Utilities.getIDocumentForEditor(Utilities.getActiveEditor());
				document.addDocumentListener(markUpdater);		
				
				final boolean[] canContinue=new boolean[]{true};
				boolean mustPost=requiresPost(flattenedList);
				
				List<MacroCommandDebugInfo> collectedDebugInfo=new ArrayList<MacroCommandDebugInfo>();
				
				for (int i=0;i<flattenedList.size();i++)
				{
					Object object = flattenedList.get(i);
					if (object instanceof FlattenWrapper)
					{
						final FlattenWrapper wrapper=(FlattenWrapper)object;
						if (mustPost)
						{
							Display.getCurrent().asyncExec(new Runnable()
							{
								public void run()
								{
									updateMacroStack(wrapper);
								}
								
							});
						}
						else
						{
							updateMacroStack(wrapper);
						}
					}
					else
					{
						final IMacroCommand command=(IMacroCommand)object;
						if (mustPost)
						{
							Display.getCurrent().asyncExec(new Runnable()
							{
								public void run()
								{
									if (!canContinue[0])
										return;
									MacroConsole.getConsole().writeln("--Executing command: "+command.getName(), MacroConsole.Type_PlayingCommand);
									boolean succeeded=command.execute(editor);
									if (!succeeded)
									{
										//TODO: error message?
										canContinue[0]=false;
									}
								}
								
							});
						}
						else
						{
							if (debugMode)
							{
								List<MacroCommandDebugInfo> combinedDebugInfo=new ArrayList<MacroCommandDebugInfo>();
								combinedDebugInfo.addAll(collectedDebugInfo);
								for (int k=i;k<flattenedList.size();k++)
								{
									Object obj=flattenedList.get(k);
									if (obj instanceof IMacroCommand)
									{
										combinedDebugInfo.add(new MacroCommandDebugInfo((IMacroCommand)obj));
									}
								}
								MacroDebugDialog dlg=new MacroDebugDialog(Display.getCurrent().getActiveShell(), combinedDebugInfo);
								dlg.open();
								int selectedAction=dlg.getSelectedAction();
								if (selectedAction==MacroDebugDialog.ACTION_CANCELEXECUTION)
								{
									break; //just kick out
								}
								else if (selectedAction==MacroDebugDialog.ACTION_CONTINUETOEND)
								{
									debugMode=false;
								}
							}
							MacroConsole.getConsole().writeln("--Executing command: "+command.getName(), MacroConsole.Type_PlayingCommand);
							canContinue[0]=command.execute(editor);
							if (!canContinue[0])
							{
								//TODO: error message?
								if (debugMode)
								{}
								break;
							}
							
							if (debugMode)
							{
								//patch my list of commands with the proper info
								ISourceViewer sourceViewer=Utilities.getSourceViewer(editor);
								Point selRange=sourceViewer.getSelectedRange();
								int caretPos=Utilities.getCaretPos(editor);
								Point cursorPos=new Point(0,0);
								Point selEndPos=new Point(0,0);
								if (selRange.x<selRange.y) //if there is a selection
								{
									if (caretPos==selRange.x)
									{
										translate(sourceViewer, selRange.y, selEndPos);
									}
									else
									{
										translate(sourceViewer, selRange.x, selEndPos);
									}
								}
								else
								{
									selEndPos=null;
								}
								translate(sourceViewer, caretPos, cursorPos);
								MacroCommandDebugInfo newInfo=new MacroCommandDebugInfo(command, cursorPos, selEndPos);
								collectedDebugInfo.add(newInfo);
								MacroConsole.getConsole().writeln("--After command, cursor at: Line "+cursorPos.x+", Column "+cursorPos.y, MacroConsole.Type_DebugInfo);
								if (selEndPos==null)
								{
									MacroConsole.getConsole().writeln("--After command, there is no selection", MacroConsole.Type_DebugInfo);
								}
								else
								{
									MacroConsole.getConsole().writeln("--After command, the selection pivot point is at: Line "+selEndPos.x+", Column "+selEndPos.y, MacroConsole.Type_DebugInfo);
								}
							}
						}
					}
				}
				
				if (mustPost)
				{
					Display.getCurrent().asyncExec(new Runnable()
					{
						public void run()
						{
							MacroManager.getManager().clearMacroStack();
							document.removeDocumentListener(markUpdater);
							MacroConsole.getConsole().writeln("--Finished executing macro: "+getID(), MacroConsole.Type_PlayingCommand);
						}
					});
				}
				else
				{
					MacroManager.getManager().clearMacroStack();
					document.removeDocumentListener(markUpdater);
					MacroConsole.getConsole().writeln("--Finished executing macro: "+getID(), MacroConsole.Type_PlayingCommand);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				MacroConsole.getConsole().write("Failed to run command with the following exception: ", MacroConsole.Type_Error);
				MacroConsole.getConsole().write(e, MacroConsole.Type_Error);
			}
		}
		finally
		{
			Activator.getDefault().getPreferenceStore().setValue(Initializer.Pref_WriteToMacroConsole, saveWriteMode);
			
			if (atomicExecution && viewExtension!=null)
			{
				viewExtension.getRewriteTarget().endCompoundChange();
			}
		}
		
	}

	private void translate(ISourceViewer viewer, int offset, Point textPos)
	{
		try
		{
			int lineAtOffset=viewer.getDocument().getLineOfOffset(offset);
			int column=offset-viewer.getDocument().getLineOffset(lineAtOffset);
			textPos.x=lineAtOffset+1;
			textPos.y=column+1;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void updateMacroStack(FlattenWrapper wrapper)
	{
		if (wrapper.isPush())
		{
			MacroManager.getManager().pushMacro(wrapper.getMacro());
		}
		else
		{
			MacroManager.getManager().popMacro();
		}
	}
	
	public void flattenMacro(List<Object> flattenedList)
	{
		if (flattenedList.size()>mMaxMacroSize)
			return;
		flattenedList.add(new FlattenWrapper(this, true));
		for (IMacroCommand command : mCommands)
		{
			if (command instanceof EclipseCommand)
			{
				EditorMacro macro=MacroManager.getManager().getMacro(((EclipseCommand)command).getCommandID());
				if (macro!=null)
				{
					macro.flattenMacro(flattenedList);
					if (flattenedList.size()>mMaxMacroSize)
						return;
				}
				else
				{
					flattenedList.add(command);
				}
			}
			else
			{
				flattenedList.add(command);
			}
		}		
		flattenedList.add(new FlattenWrapper(this, false));
	}
	
	
	public void addCommand(IMacroCommand command)
	{
		mCommands.add(command);
	}

	public boolean hasEvents() {
		return mCommands.size()>0;
	}
	
	public String getName()
	{
		return mName;
	}
	
	public String getDescription()
	{
		return mDescription;
	}

	public void dump()
	{
		for (IMacroCommand command : mCommands) {
			command.dump();
		}
	}

	public void setName(String name) {
		mName = name;
	}

	public void setDescription(String description) {
		mDescription = description;
	}

	public String getID() {
		return mID;
	}

	public void setID(String id) {
		mID = id;
	}

	public void persist(Document doc, Element macroElement)
	{
		for (IMacroCommand command : mCommands)
		{
            Element child = doc.createElement(MacroManager.XML_Command_Tag);
            command.persist(doc, child);
            macroElement.appendChild(child);
		}
	}

	public long getLastUse() {
		return mLastUse;
	}
	
	public void setLastUse(long time)
	{
		mLastUse=time;
	}

	public List<IMacroCommand> getCommands()
	{
		return mCommands;
	}

	public void copyFrom(EditorMacro existingMacro)
	{
		mID=existingMacro.mID;
		mName=existingMacro.mName;
		mDescription=existingMacro.mDescription;
		mCommands.clear();
		mCommands.addAll(existingMacro.mCommands);
		mLastUse=existingMacro.mLastUse;
		mIsContributed=existingMacro.mIsContributed;
		mRunAsCompoundEvent=existingMacro.mRunAsCompoundEvent;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof EditorMacro))
			return false;
		
		if (getID()==null || ((EditorMacro)obj).getID()==null)
			return false;
		return (((EditorMacro)obj).getID().equals(getID()));
	}

	@Override
	public int hashCode()
	{
		if (getID()==null)
			return getName().hashCode();
		return getID().hashCode();
	}
	
	public void setMark(int caretOffset)
	{
		mMark=caretOffset;
	}

	public void moveMarkOnInsert(int loc, int length)
	{
		mMark=moveMarkOnInsert(mMark, loc, length);
	}
	
	public static int moveMarkOnInsert(int initialMark, int loc, int length)
	{
		if (loc<=initialMark)
			return initialMark+length;
		return initialMark;
	}
	
	public void moveMarkOnDelete(int start, int length)
	{
		mMark=moveMarkOnDelete(mMark, start, length);
	}

	public static int moveMarkOnDelete(int mark, int start, int length)
	{
		if (start+length<=mark)
			return mark-length;
		else if (start<=mark && start+length>mark)
			return start;
		return mark;
	}
	
	public int getMark()
	{
		return mMark;
	}

	static class FlattenWrapper
	{
		private EditorMacro mMacro;
		private boolean mPush;
		public FlattenWrapper(EditorMacro macro, boolean push)
		{
			mMacro=macro;
			mPush=push;
		}
		public EditorMacro getMacro() {
			return mMacro;
		}
		public boolean isPush() {
			return mPush;
		}
	}

	public boolean requiresPost(List<Object> flattenedList)
	{
		for (Object object : flattenedList)
		{
			if (object instanceof IMacroCommand)
			{
				if (((IMacroCommand)object).requiresPost())
				{
					return true;
				}
			}
		}
		
		return false;
	}

	public int getSessionID() {
		return mSessionID;
	}

	public void setSessionID(int sessionID)
	{
		if (mSessionID==0)
			mSessionID = sessionID;
		else
		{
			throw new RuntimeException("Session ID set twice. Macro: "+toString());
		}
	}
	
	@Override
	public String toString()
	{
		StringBuffer buffer=new StringBuffer();
		buffer.append("ID: ");
		if (getID()==null)
			buffer.append("<none>");
		else
			buffer.append(getID());
		return buffer.toString();
	}

	public static List<IMacroCommand> compressStringInsertions(List<IMacroCommand> commands)
	{
		List<IMacroCommand> newCommands=new ArrayList<IMacroCommand>();
		InsertStringCommand lastCommand=null;
		for (IMacroCommand macroCommand : commands)
		{
			if (macroCommand instanceof InsertStringCommand)
			{
				InsertStringCommand anotherCommand=(InsertStringCommand)macroCommand;
				if (lastCommand!=null)
				{
					boolean success=lastCommand.combineWith(anotherCommand);
					if (!success)
					{
						lastCommand=anotherCommand;
					}
					else
					{
						//we've combined the data for this command, so this is the only
						//case where we don't add the command to the list we're building.
						continue;
					}
				}
				else
				{
					lastCommand=anotherCommand;
				}
			}
			else
			{
				if (lastCommand!=null)
				{
					lastCommand=null;
				}
			}
			
			newCommands.add(macroCommand);
		}
		
		return newCommands;
	}
}
