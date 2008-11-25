package practicallymacro.dialogs;

import org.eclipse.swt.graphics.Point;

import practicallymacro.commands.IMacroCommand;

public class MacroCommandDebugInfo
{
	private Point mCursorPos;
	private Point mSelEnd;
	private IMacroCommand mCommand;
	
	public MacroCommandDebugInfo(IMacroCommand command)
	{
		mCommand=command;
	}
	
	public MacroCommandDebugInfo(IMacroCommand command, Point cursorPos, Point selEnd)
	{
		mCursorPos=cursorPos;
		mSelEnd=selEnd;
		mCommand=command;
	}

	public Point getCursorPos() {
		return mCursorPos;
	}

	public Point getSelEnd() {
		return mSelEnd;
	}

	public IMacroCommand getCommand() {
		return mCommand;
	}
}
