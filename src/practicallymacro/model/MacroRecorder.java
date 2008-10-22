package practicallymacro.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;

import practicallymacro.commands.EclipseCommand;
import practicallymacro.commands.FindCommand;
import practicallymacro.commands.IMacroCommand;
import practicallymacro.commands.KeystrokeCommand;
import practicallymacro.util.MacroConsole;
import practicallymacro.util.Utilities;


public class MacroRecorder implements Listener, IExecutionListener, IDocumentListener
{
	private IEditorPart mEditor;
	private List<IMacroCommand> mCommands;
	private Set<String> mNonRecordableCommandIds;
	private boolean mCurrentlyExecutingCommand;
	private boolean mRecordCommands;
	private MarkUpdater mMarkUpdater=new MarkUpdater(true);
	
	public MacroRecorder(IEditorPart editor)
	{
		mEditor=editor;
		
		//TODO: get from extension point?
		mNonRecordableCommandIds=new HashSet<String>();
		mNonRecordableCommandIds.add("practicallymacro.actions.recordMacro");
		mNonRecordableCommandIds.add("practicallymacro.actions.playCommand");
		mNonRecordableCommandIds.add(IWorkbenchActionDefinitionIds.FIND_REPLACE);
	}
	
	public void start()
	{
		MacroConsole.getConsole().writeln("***Started macro recording", MacroConsole.Type_RecordingCommand);
		mCommands=new ArrayList<IMacroCommand>();
		mCurrentlyExecutingCommand=false;
		mRecordCommands=true;
	}
	
	public void stop()
	{
		updateIncrementalFindMode();
		MacroConsole.getConsole().writeln("***Finished recording macro", MacroConsole.Type_RecordingCommand);
	}

	public void handleEvent(Event event) {
		updateIncrementalFindMode();
		if (event.type==SWT.KeyDown)
		{
			IMacroCommand command=null;
			if (MacroManager.getManager().isRecordingRawKeystrokes())
			{
				command=new KeystrokeCommand(event);
			}
			else
			{
				command=Utilities.getCommandForKeyEvent(event);
			}
			
			if (!mIncrementalFindMode)
			{
				if (command!=null)
				{
					recordCommand(command);
//					mCommands.add(command);
				}
			}
//			System.out.println("event="+event);
		}
		else if (event.type==SWT.KeyUp)
		{
			if (MacroManager.getManager().isRecordingRawKeystrokes())
			{
				IMacroCommand command=new KeystrokeCommand(event);
				recordCommand(command);
//				mCommands.add(command);
//				System.out.println("event="+event);
			}
		}
	}

	public void notHandled(String commandId, NotHandledException exception) {
		// TODO Auto-generated method stub
		mCurrentlyExecutingCommand=false;
		endIncrementalFindMode();
		System.out.println("not handled: "+commandId);
	}

	public void postExecuteFailure(String commandId,
			ExecutionException exception) {
		// TODO Auto-generated method stub
		mCurrentlyExecutingCommand=false;
		endIncrementalFindMode();
		System.out.println("command failed: "+commandId);
		
	}

	public void postExecuteSuccess(String commandId, Object returnValue) {
		
		mCurrentlyExecutingCommand=false;
		endIncrementalFindMode();
		if (mNonRecordableCommandIds.contains(commandId))
		{
			//we *always" see the record command first, so don't log it
			if (!commandId.equals("practicallymacro.actions.recordMacro") && !commandId.equals(IWorkbenchActionDefinitionIds.FIND_REPLACE))
			{
				MacroConsole.getConsole().writeln("Not recording command (it's in the exclude list): "+commandId, MacroConsole.Type_Standard);
			}
			return;
		}
		
		if (commandId.equals(ITextEditorActionDefinitionIds.FIND_INCREMENTAL) || commandId.equals(ITextEditorActionDefinitionIds.FIND_INCREMENTAL_REVERSE))
		{
			if (commandId.equals(ITextEditorActionDefinitionIds.FIND_INCREMENTAL))
				mIncrementalFindForward=true;
			else
				mIncrementalFindForward=false;
			Listener[] currentListeners=Utilities.getStyledText(Utilities.getActiveEditor()).getListeners(SWT.MouseUp);
			mIncrementalListener=null;
			for (Listener listener : currentListeners) {
				boolean inCurrentList=false;
				for (Listener listenerBeforeFind : mPreexecuteListeners) {
					if (listenerBeforeFind==listener)
						inCurrentList=true;
						break;
				}
				if (!inCurrentList)
				{
					mIncrementalListener=listener;
				}
			}
		}
		
		updateIncrementalFindMode();
		if (!mIncrementalFindMode)
		{
			System.out.println("Command executed: "+commandId);
			recordCommand(new EclipseCommand(commandId));
		}
	}
	
	private boolean mIncrementalFindMode=false;
	private boolean mIncrementalFindForward=true;
	private Listener[] mPreexecuteListeners=null;
	private Listener mIncrementalListener=null;

	public void preExecute(String commandId, ExecutionEvent event) {
		mCurrentlyExecutingCommand=true;
		if (commandId.equals(ITextEditorActionDefinitionIds.FIND_INCREMENTAL) || commandId.equals(ITextEditorActionDefinitionIds.FIND_INCREMENTAL_REVERSE))
		{
			mIncrementalFindMode=true;
			StyledText widget=Utilities.getStyledText(Utilities.getActiveEditor());
			if (widget!=null)
			{
				mPreexecuteListeners=widget.getListeners(SWT.MouseUp);
			}
		}
		System.out.println("preexecute: "+commandId);
	}

	public List<IMacroCommand> getMacroCommands()
	{
		return mCommands;
	}
	
	public IEditorPart getEditor()
	{
		return mEditor;
	}
	
	private void updateIncrementalFindMode()
	{
		if (!mIncrementalFindMode)
			return;

		StyledText st=Utilities.getStyledText(Utilities.getActiveEditor());
		Listener[] currentListeners=st.getListeners(SWT.MouseUp);
		boolean stillInList=false;
		for (Listener listener : currentListeners)
		{
			if (listener==mIncrementalListener)
			{
				stillInList=true;
				break;
			}
		}
		
		if (!stillInList)
		{
			mIncrementalFindMode=false;
			
			//add find command representing whatever is currently selected
			String selectionText=st.getSelectionText();
			FindCommand findCommand=new FindCommand(selectionText);
			findCommand.setSearchForward(mIncrementalFindForward);
			recordCommand(findCommand);
			System.out.println("Incremental find string: "+selectionText);
		}
	}

//	@Override
//	public void modifyText(ExtendedModifyEvent event)
//	{
////		if (!mCurrentlyExecutingCommand)
////		{
////			System.out.println(event);
////		}
////		//the text modify event is used to handle character insert/delete events
////		if (event.replacedText.length()>0)
////		{
////			mCommands.add(new StyledTextCommand(ST.DELETE_NEXT));
////		}
////		
////		if ()
//	}

	public void documentAboutToBeChanged(DocumentEvent event)
	{
		// TODO Auto-generated method stub
		
	}

	public void documentChanged(DocumentEvent event)
	{
		// TODO Auto-generated method stub
		if (!mCurrentlyExecutingCommand)
		{
			endIncrementalFindMode();
			System.out.println(event);
		}
	}

	private void endIncrementalFindMode()
	{
		
	}
	
	public void pauseRecording()
	{
		mRecordCommands=false;
	}
	
	public void resumeRecording()
	{
		mRecordCommands=true;
	}
	
	public void recordCommand(IMacroCommand newCommand)
	{
		if (mRecordCommands)
		{
			MacroConsole.getConsole().writeln("*Command added to macro: "+newCommand.getName(), MacroConsole.Type_RecordingCommand);			
			mCommands.add(newCommand);
		}
	}

	public IDocumentListener getMarkUpdater() {
		return mMarkUpdater;
	}
}
