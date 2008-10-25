package practicallymacro.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import practicallymacro.commands.EclipseCommand;
import practicallymacro.commands.IMacroCommand;
import practicallymacro.editormacros.Activator;
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
	}
	
	public void setContributed(boolean contributed)
	{
		mIsContributed=contributed;
	}
	
	public boolean isContributed()
	{
		return mIsContributed;
	}
	
	public void run(final IEditorPart editor)
	{
		if (editor==null)
		{
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Execute macro", "Cannot execute macro: the view with focus is not a text editor.");
			return;
		}
//		StyledText widget=Utilities.getStyledText(editor);
//		widget.removeListener(MacroManager.Macro_Event, MacroManager.getManager().getMacroRunListener());
//		widget.addListener(MacroManager.Macro_Event, MacroManager.getManager().getMacroRunListener());
//		MacroManager.getManager().setMark(widget.getCaretOffset());

		Activator.getDefault().setLastMacro(this);
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
			
			for (Object object : flattenedList)
			{
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
						MacroConsole.getConsole().writeln("--Executing command: "+command.getName(), MacroConsole.Type_PlayingCommand);
						canContinue[0]=command.execute(editor);
						if (!canContinue[0])
						{
							//TODO: error message?
							break;
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
}
