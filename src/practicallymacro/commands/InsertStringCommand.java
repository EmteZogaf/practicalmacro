package practicallymacro.commands;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.custom.TextChangeListener;
import org.eclipse.swt.custom.TextChangedEvent;
import org.eclipse.swt.custom.TextChangingEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import practicallymacro.dialogs.InsertStringConfigureDlg;
import practicallymacro.model.MacroManager;
import practicallymacro.util.MacroConsole;
import practicallymacro.util.Utilities;


public class InsertStringCommand implements IMacroCommand
{
	public static final String XML_Data_Tag="data";
	public static final String XML_InsertStringType="InsertStringCommand";
	
	private String mData;
//	private MyTextChangeListener mTextChangeListener=new MyTextChangeListener();
	private MyExtendedModifyListener mExtendedModifyListener=new MyExtendedModifyListener();
	
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
		System.out.println(getName());
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
				//I've had to hack to prevent problems with auto-edits.  The problem with just inserting
				//into the styled text widget is that a string with more than one character is treated
				//like a "paste", and in the Java editor, for instance, the paste will adjust the
				//leading whitespace on the current line.  Therefore the amount of text actually inserted
				//will not match the original string and setting the caret offset will be off.  Also,
				//I might have compressed the string, so the result from inserting individual chars 
				//(which is what the user actually typed) wouldn't match the results from a string
				//insertion.  Also, there is a flag in the styled widget that would keep the caret updated
				//but it's not publicly available in the api.  The fix seems to be to insert >1 length
				//strings directly into the styledTextContent, which bypasses any auto
				//indent strategy that might be set on the viewer.  I don't think I want to always do
				//this because I *do* want to get autoindent behavior for \n insertions.
				int caretPos=widget.getCaretOffset();
				int selSize=widget.getSelectionCount();
				StyledTextContent content=widget.getContent();
				if (content!=null && (mData.length()>1))// || (mData.length()==1 && mData.charAt(0)!='\r')))
				{
					try
					{
//						content.addTextChangeListener(mTextChangeListener);
						content.replaceTextRange(caretPos, selSize, mData);
						widget.setCaretOffset(caretPos+mData.length()-selSize);
//						widget.setCaretOffset(mTextChangeListener.getCaretOffset());
					}
					finally
					{
//						content.removeTextChangeListener(mTextChangeListener);
					}
				}
				else
				{
					//want to allow single characters through so that we get good behavior from \r, '{', etc.
					try
					{
						widget.addExtendedModifyListener(mExtendedModifyListener);
						mExtendedModifyListener.clearCaret();
						widget.insert(mData);
//						widget.setCaretOffset(caretPos+mData.length()-selSize);
						if (mExtendedModifyListener.getCaretOffset()>=0)
							widget.setCaretOffset(mExtendedModifyListener.getCaretOffset());
					}
					finally
					{
						widget.removeExtendedModifyListener(mExtendedModifyListener);
					}
				}
				return true;
			}
		}
		
		return false;
	}
	
//	private static class MyTextChangeListener implements TextChangeListener
//	{
//		private StyledText mWidget;
//		private int mCaretPos;
//		public void setWidget(StyledText widget)
//		{
//			mWidget=widget;
//		}
//		public void textChanged(TextChangedEvent event)
//		{
////			mCaretPos=event.mWidget.getCaretOffset();
//		}
//
//		public void textChanging(TextChangingEvent event)
//		{
//			//nothing to do
//		}
//
//		public void textSet(TextChangedEvent event)
//		{
//			
//		}
//		
//		public int getCaretOffset()
//		{
//			return mCaretPos;
//		}
//	}
	
	private static class MyExtendedModifyListener implements ExtendedModifyListener
	{
		private int mCaretPos;
		public void modifyText(ExtendedModifyEvent event)
		{
			mCaretPos=event.start+event.length;
		}
		
		public void clearCaret()
		{
			mCaretPos=(-1);
		}

		public int getCaretOffset()
		{
			return mCaretPos;
		}
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
		StringBuffer buffer=new StringBuffer();
		buffer.append("Insert the string into the document at the current caret location");
		if (mData!=null)
		{
			buffer.append("\n");
			buffer.append(getName());
		}
		return buffer.toString();
	}

	public String getName()
	{
		String printableData=mData;
		if (printableData!=null)
		{
			printableData=printableData.replace("\r", "<return>");
			printableData=printableData.replace("\n", "<return>");
			printableData=printableData.replace("\t", "<tab>");
		}
		return "Insert string: "+printableData;
	}

	public String getCategory()
	{
		return MacroManager.MacroCommandCategory;
	}

	public void configure(Shell shell)
	{
		InsertStringConfigureDlg dlg=new InsertStringConfigureDlg(shell, mData);
		if (dlg.open()==Dialog.OK)
		{
			mData=dlg.getText();
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

	public boolean combineWith(InsertStringCommand anotherCommand)
	{
		if (mData.indexOf('\n')>=0 || anotherCommand.mData.indexOf('\n')>=0 || mData.indexOf('\r')>=0 || anotherCommand.mData.indexOf('\r')>=0)
			return false;
		
		//just concatenate the 2 strings
		mData+=anotherCommand.mData;
		return true;
	}
	
}
