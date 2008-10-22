package practicallymacro.model;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;

public class MarkUpdater implements IDocumentListener
{
	private boolean mRecordMode;
	public MarkUpdater(boolean recordMode)
	{
		mRecordMode=recordMode;
	}
//	@Override
//	public void modifyText(ExtendedModifyEvent event)
//	{
//		if (event.replacedText.length()>0)
//		{
//			MacroManager.getManager().moveMarkOnDelete(event.start, event.replacedText.length());
//		}
//		if (event.length>0)
//		{
//			MacroManager.getManager().moveMarkOnInsert(event.start, event.length);
//		}
//	}

	public void documentAboutToBeChanged(DocumentEvent event)
	{
		//nothing to do
	}

	public void documentChanged(DocumentEvent event)
	{
		if (event.getLength()>0)
		{
			MacroManager.getManager().moveMarkOnDelete(mRecordMode, event.getOffset(), event.getLength());
		}
		
		if (event.getText().length()>0)
		{
			MacroManager.getManager().moveMarkOnInsert(mRecordMode, event.getOffset(), event.getText().length());
		}
	}
}
