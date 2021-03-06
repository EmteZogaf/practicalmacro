package practicallymacro.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import practicallymacro.commands.EclipseCommand;
import practicallymacro.commands.FindCommand;
import practicallymacro.commands.IMacroCommand;
import practicallymacro.commands.InsertStringCommand;
import practicallymacro.editormacros.Activator;
import practicallymacro.model.MacroManager;


public class Utilities {
	private static Map<Integer, Command> mFillInCommands=new HashMap<Integer, Command>();
	public static final String FillInPrefix="practicallyMacro.styledTextCommand";
	
	private static Set<String> mEditCategories;
	
	static
	{
		mEditCategories=new HashSet<String>();
		mEditCategories.add(MacroManager.UserMacroCategoryID);
		mEditCategories.add("org.eclipse.ui.category.edit");
		mEditCategories.add("org.eclipse.ui.category.textEditor");
		mEditCategories.add("org.eclipse.jdt.ui.category.source");
		mEditCategories.add("org.eclipse.jdt.ui.category.refactoring");
		mEditCategories.add(MacroManager.MacroCommandCategoryID);
		mEditCategories.add("eclipse.ui.category.navigate");
		mEditCategories.add(FillInPrefix);
	}
	
	public static IEditorPart getActiveEditor() {
		
		IEditorPart editor = null;
		
		// find the active part
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window!=null)
		{
			IPartService partService = window.getPartService();
			IWorkbenchPart part = partService.getActivePart();
			
			// Is the part an editor?
			if (part instanceof IEditorPart) {
				
				editor = (IEditorPart) part;
			}
		}
		
		return editor;
	}
	
	public static ISourceViewer getSourceViewer(IEditorPart editor) {
		
		if (editor==null)
			return null;
		
		ISourceViewer viewer = (ISourceViewer)editor.getAdapter(ITextOperationTarget.class);
		return viewer;
	}
	
//	public static ITextSelection getTextSelection(ISourceViewer viewer)
//	{
//		return (ITextSelection)viewer.getSelectionProvider().getSelection();
//	}
	
	public static String getSelectedText(IEditorPart editor)
	{
		Point viewerSelection=getUndirectedSelection(editor);
		IDocument doc=getIDocumentForEditor(editor);
		try {
			return doc.get(viewerSelection.x, viewerSelection.y-viewerSelection.x);
		} catch (BadLocationException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static int getSelectionAnchor(IEditorPart editor)
	{
		Point viewerSelection=getUndirectedSelection(editor);
		int caretPos=getCaretPos(editor);
		
		if (caretPos==viewerSelection.x)
			return viewerSelection.y;
		else
			return viewerSelection.x;
		
//		Point viewerSelection=getUndirectedSelection(editor);
//		if (sourceViewer instanceof ITextViewerExtension5) {
//			ITextViewerExtension5 extension= (ITextViewerExtension5)sourceViewer;
//			caret= extension.widgetOffset2ModelOffset(styledText.getCaretOffset());
//		} 
		
	}
	
	public static int getCaretPos(IEditorPart editor)
	{
		//use the styled texxt to determine which end of the selection is the caret,
		//then get the actual text position (may be different because of folding) from
		//the viewer selection
		Point viewerSelection=getUndirectedSelection(editor);
		StyledText styled=getStyledText(editor);
		int caretPos=styled.getCaretOffset();
		Point styledSel=styled.getSelection();
		if (caretPos==styledSel.x)
			return viewerSelection.x;
		else
			return viewerSelection.y;
	}
	
	public static Point getUndirectedSelection(ISourceViewer viewer)
	{
		Point result=viewer.getSelectedRange();
		if (result.y>=0)
			return new Point(result.x, result.x+result.y);
		else
			return new Point(result.x+result.y, result.x);
	}
	
	public static Point getUndirectedSelection(IEditorPart editor)
	{
		ISourceViewer viewer=getSourceViewer(editor);
		return getUndirectedSelection(viewer);
	}

	public static StyledText getStyledText(IEditorPart editor) {
		
		if (editor==null)
			return null;
		StyledText styledText = null;
		
		ISourceViewer viewer = (ISourceViewer)
			editor.getAdapter(ITextOperationTarget.class);
		if (viewer != null) {
			styledText= viewer.getTextWidget();
		}
		
		return styledText;
	}
	
//	public static List<Command> getFillInCommands()
//	{
//		if (mFillInCommands.size()>0)
//	}
	
	private static Command getStyledTextCommand(int styledTextCode)
	{
		if (mFillInCommands.size()==0)
			createStyledTextCommands();
		
		return mFillInCommands.get(styledTextCode);
	}
	
	public static List<IMacroCommand> getMacroSupportCommands()
	{
		List<IMacroCommand> commands=new ArrayList<IMacroCommand>();
		commands.add(new InsertStringCommand(null));
		commands.addAll(MacroManager.getManager().getCanonicalScriptCommands());
		commands.add(new FindCommand(""));
		return commands;
	}

	public static void createStyledTextCommands()
	{
		if (mFillInCommands.size()>0)
			return;
		
		//create styled text commands and fill in map
		int[] styledTextConstants={ST.DELETE_PREVIOUS, ST.LINE_DOWN, ST.SELECT_WORD_PREVIOUS, ST.WORD_PREVIOUS, ST.SELECT_COLUMN_PREVIOUS, ST.COLUMN_PREVIOUS, ST.SELECT_WORD_NEXT
				,ST.WORD_NEXT,ST.SELECT_COLUMN_NEXT,ST.COLUMN_NEXT,ST.LINE_UP,ST.SELECT_TEXT_START,ST.TEXT_START
				,ST.SELECT_LINE_START,ST.LINE_START,ST.SELECT_TEXT_END,ST.TEXT_END,ST.SELECT_LINE_END,ST.LINE_END
				,ST.SELECT_WINDOW_END,ST.WINDOW_END,ST.SELECT_PAGE_DOWN,ST.PAGE_DOWN,ST.SELECT_WINDOW_START
				,ST.WINDOW_START,ST.SELECT_PAGE_UP,ST.PAGE_UP,ST.DELETE_WORD_NEXT,ST.CUT,ST.DELETE_NEXT,ST.COPY
				,ST.PASTE,ST.TOGGLE_OVERWRITE,ST.SELECT_LINE_UP, ST.SELECT_LINE_DOWN, ST.DELETE_WORD_PREVIOUS};

		ICommandService cs = MacroManager.getOldCommandService();
		Category cat=cs.getCategory(FillInPrefix);
		cat.define("Styled Text Commands", "");
		for (int i = 0; i < styledTextConstants.length; i++)
		{
			int constant = styledTextConstants[i];
			Command newCommand=cs.getCommand(FillInPrefix+"."+Integer.toString(constant));
			newCommand.define(getNameForStyledTextConstant(constant), "", cat);
			newCommand.setHandler(new StyledTextHandler(constant));
			mFillInCommands.put(constant, newCommand);
		}
	}

	public static IMacroCommand getCommandForKeyEvent(Event event)
	{
		final int key = (SWT.KEY_MASK & event.keyCode);
		if ((key != 0 && !Character.isISOControl(event.character)))
		{
			return new InsertStringCommand(new String(new char[]{event.character}));
		}
		
		boolean isCtrl=((event.stateMask & SWT.MOD1) > 0); //ctrl
		boolean isShift=((event.stateMask & SWT.MOD2) > 0);  //shift
		
		switch (key)
		{
			case SWT.BS:
				if (isCtrl)
					return new EclipseCommand(getStyledTextCommand(ST.DELETE_WORD_PREVIOUS).getId());
				else
					return new EclipseCommand(getStyledTextCommand(ST.DELETE_PREVIOUS).getId());
			case SWT.ARROW_DOWN:
				if (isShift && !isCtrl)
					return new EclipseCommand(getStyledTextCommand(ST.SELECT_LINE_DOWN).getId());
				else if (!isCtrl && !isShift)
					return new EclipseCommand(getStyledTextCommand(ST.LINE_DOWN).getId());
			case SWT.ARROW_LEFT:
				if (isCtrl && isShift)
					return new EclipseCommand(getStyledTextCommand(ST.SELECT_WORD_PREVIOUS).getId());
				else if (isCtrl)
					return new EclipseCommand(getStyledTextCommand(ST.WORD_PREVIOUS).getId());
				else if (isShift)
					return new EclipseCommand(getStyledTextCommand(ST.SELECT_COLUMN_PREVIOUS).getId());
				return new EclipseCommand(getStyledTextCommand(ST.COLUMN_PREVIOUS).getId());
			case SWT.ARROW_RIGHT: 
				if (isCtrl && isShift)
					return new EclipseCommand(getStyledTextCommand(ST.SELECT_WORD_NEXT).getId());
				else if (isCtrl)
					return new EclipseCommand(getStyledTextCommand(ST.WORD_NEXT).getId());
				else if (isShift)
					return new EclipseCommand(getStyledTextCommand(ST.SELECT_COLUMN_NEXT).getId());
				return new EclipseCommand(getStyledTextCommand(ST.COLUMN_NEXT).getId());
			case SWT.ARROW_UP: 
				if (isShift && !isCtrl)
					return new EclipseCommand(getStyledTextCommand(ST.SELECT_LINE_UP).getId());
				else if (!isCtrl && !isShift)
					return new EclipseCommand(getStyledTextCommand(ST.LINE_UP).getId());
			case SWT.HOME:
				if (isCtrl && isShift)
					return new EclipseCommand(getStyledTextCommand(ST.SELECT_TEXT_START).getId());
				if (isCtrl)
					return new EclipseCommand(getStyledTextCommand(ST.TEXT_START).getId());
				if (isShift)
					return new EclipseCommand(getStyledTextCommand(ST.SELECT_LINE_START).getId());
				return new EclipseCommand(getStyledTextCommand(ST.LINE_START).getId());
			case SWT.END: 
				if (isCtrl && isShift)
					return new EclipseCommand(getStyledTextCommand(ST.SELECT_TEXT_END).getId());
				if (isCtrl)
					return new EclipseCommand(getStyledTextCommand(ST.TEXT_END).getId());
				if (isShift)
					return new EclipseCommand(getStyledTextCommand(ST.SELECT_LINE_END).getId());
				return new EclipseCommand(getStyledTextCommand(ST.LINE_END).getId());
			case SWT.PAGE_DOWN: 
				if (isCtrl && isShift)
					return new EclipseCommand(getStyledTextCommand(ST.SELECT_WINDOW_END).getId());
				if (isCtrl)
					return new EclipseCommand(getStyledTextCommand(ST.WINDOW_END).getId());
				if (isShift)
					return new EclipseCommand(getStyledTextCommand(ST.SELECT_PAGE_DOWN).getId());
				return new EclipseCommand(getStyledTextCommand(ST.PAGE_DOWN).getId());
			case SWT.PAGE_UP: 
				if (isCtrl && isShift)
					return new EclipseCommand(getStyledTextCommand(ST.SELECT_WINDOW_START).getId());
				if (isCtrl)
					return new EclipseCommand(getStyledTextCommand(ST.WINDOW_START).getId());
				if (isShift)
					return new EclipseCommand(getStyledTextCommand(ST.SELECT_PAGE_UP).getId());
				return new EclipseCommand(getStyledTextCommand(ST.PAGE_UP).getId());
			case SWT.DEL:
				if (isCtrl)
					return new EclipseCommand(getStyledTextCommand(ST.DELETE_WORD_NEXT).getId());
				if (isShift)
					return new EclipseCommand(getStyledTextCommand(ST.CUT).getId());
				return new EclipseCommand(getStyledTextCommand(ST.DELETE_NEXT).getId());
			case SWT.INSERT: 
				if (isCtrl)
					return new EclipseCommand(getStyledTextCommand(ST.COPY).getId());
				if (isShift)
					return new EclipseCommand(getStyledTextCommand(ST.PASTE).getId());
				return new EclipseCommand(getStyledTextCommand(ST.TOGGLE_OVERWRITE).getId());
		}
		
		if (key == SWT.CR || key==SWT.LF)
		{
//			return new EclipseCommand("org.eclipse.ui.edit.text.smartEnter");
//			try
//			{
//				String delimiter=Utilities.getStyledText(Utilities.getActiveEditor()).getLineDelimiter();
//				return new InsertStringCommand(delimiter);
//			}
//			catch (Exception e)
			{
				return new InsertStringCommand(new String(new char[]{(char)key}));	
			}
			
		}
		
		if (key == SWT.TAB)
		{
			return new InsertStringCommand(new String(new char[]{(char)key}));
		}
		
		//skip if this doesn't seem to correspond to anything we understand
		return null;
	}
	
	public static String getNameForStyledTextConstant(int constant)
	{
		switch (constant)
		{
		case ST.DELETE_PREVIOUS: return "Delete previous character";
		case ST.LINE_DOWN: return "Move cursor down";
		case ST.SELECT_WORD_PREVIOUS: return "Select previous word";
		case ST.WORD_PREVIOUS: return "Move cursor to previous word";
		case ST.SELECT_COLUMN_PREVIOUS: return "Select previous character";
		case ST.COLUMN_PREVIOUS: return "Move cursor left";
		case ST.SELECT_WORD_NEXT: return "Select next word";
		case ST.WORD_NEXT: return "Move cursor to next word";
		case ST.SELECT_COLUMN_NEXT: return "Select next character";
		case ST.COLUMN_NEXT: return "Move cursor right";
		case ST.LINE_UP: return "Move cursor up";
		case ST.SELECT_TEXT_START: return "Select to start of document";
		case ST.TEXT_START: return "Move cursor to start of document";
		case ST.SELECT_LINE_START: return "Select to line start";
		case ST.LINE_START: return "Move cursor to line start";
		case ST.SELECT_TEXT_END: return "Select to end of document";
		case ST.TEXT_END: return "Move cursor to end of document";
		case ST.SELECT_LINE_END: return "Select to end of line";
		case ST.LINE_END: return "Move cursor to end of line";
		case ST.SELECT_WINDOW_END: return "Select to window end";
		case ST.WINDOW_END: return "Move cursor to window end";
		case ST.SELECT_PAGE_DOWN: return "Select page down";
		case ST.PAGE_DOWN: return "Move cursor down a page";
		case ST.SELECT_WINDOW_START: return "Select to window start";
		case ST.WINDOW_START: return "Move cursor to window start";
		case ST.SELECT_PAGE_UP: return "Select page up";
		case ST.PAGE_UP: return "Move cursor up a page";
		case ST.DELETE_WORD_NEXT: return "Delete next word";
		case ST.DELETE_WORD_PREVIOUS: return "Delete previous word";
		case ST.CUT: return "Cut";
		case ST.DELETE_NEXT: return "Delete next character";
		case ST.COPY: return "Copy";
		case ST.PASTE: return "Paste";
		case ST.TOGGLE_OVERWRITE: return "Toggle overwrite mode";
		case ST.SELECT_LINE_DOWN: return "Select line down";
		case ST.SELECT_LINE_UP: return "Select line up";
		}
		
		return "Unknown constant";
	}

	static class StyledTextHandler implements IHandler
	{
		private int mStyledTextAction;
		public StyledTextHandler(int styledTextConstant)
		{
			mStyledTextAction=styledTextConstant;
		}
		
		public void addHandlerListener(IHandlerListener handlerListener) {
			
		}

		public void dispose() {
			
		}

		public Object execute(ExecutionEvent event) throws ExecutionException
		{
			IEditorPart target=getActiveEditor();
			Utilities.getStyledText(target).invokeAction(mStyledTextAction);
			return null;
		}

		public boolean isEnabled()
		{
			return true;
		}

		public boolean isHandled()
		{
			return true;
		}

		public void removeHandlerListener(IHandlerListener handlerListener) {
		}
		
	}

	public static boolean isEditCategory(String categoryID)
	{
		System.out.println("category test:"+categoryID);
		return mEditCategories.contains(categoryID);
	}
	
	public static IFindReplaceTarget getFindReplaceTarget(IEditorPart editor)
	{
		IFindReplaceTarget target=(IFindReplaceTarget) editor.getAdapter(IFindReplaceTarget.class);
		return target;
	}
	
	public static void persistCommand(Document doc, Element commandElement, String type, Map<String, String> attrs, Map<String, String> data)
	{
		commandElement.setAttribute(MacroManager.XML_CommandType_ATTR, type);
		if (attrs!=null)
		{
			for (String attr : attrs.keySet())
			{
				commandElement.setAttribute(attr, attrs.get(attr));
			}
		}
		
		if (data!=null)
		{
			for (String dataTag : data.keySet())
			{
				String dataContent=data.get(dataTag);
				Element dataChild = doc.createElement(dataTag);
				CDATASection cdataContent = doc.createCDATASection(dataContent);
//				child.setTextContent(dataContent);
				commandElement.appendChild(dataChild);
				dataChild.appendChild(cdataContent);
			}
		}
	}

	public static void getCommandData(Element commandElement, Set<String> attrKeys, Set<String> dataKeys, Map<String, String> attrMap, Map<String, String> dataMap)
	{
		if (attrMap!=null)
		{
			for (String attrName : attrKeys)
			{
				String attrValue=commandElement.getAttribute(attrName);
				attrMap.put(attrName, attrValue);
			}
		}
		
		if (dataMap!=null)
		{
			NodeList children=commandElement.getChildNodes();
			for (int i=0;i<children.getLength();i++)
			{
				String tagName=children.item(i).getNodeName();
				if (dataKeys.contains(tagName))
				{
					String data=children.item(i).getTextContent();
					dataMap.put(tagName, data);
				}
			}
		}
	}

	public static IDocument getIDocumentForEditor(IEditorPart editor)
	{
		TextFileDocumentProvider textFileDocumentProvider = new TextFileDocumentProvider();
		try {
			textFileDocumentProvider.connect(editor.getEditorInput());
		} catch (CoreException coreException) {
			coreException.printStackTrace();
		}

		IDocument document = textFileDocumentProvider.getDocument(editor.getEditorInput());
		textFileDocumentProvider.disconnect(editor.getEditorInput());
		return document;
	}

	public static boolean isSupportCategory(String categoryID)
	{
		if (categoryID.equals(MacroManager.MacroCommandCategoryID))
			return true;
		return false;
	}

	public static boolean isUserMacroCategory(String categoryID)
	{
		if (categoryID.equals(MacroManager.UserMacroCategoryID))
			return true;
		
		return false;
	}
	
	public static boolean isUserMacro(IMacroCommand macroCommand)
	{
		return isUserMacroCategory(macroCommand.getCategoryID());
	}
	
	public static IPreferenceStore getMainPreferenceStore()
	{
		return Activator.getDefault().getPreferenceStore();
	}
}
