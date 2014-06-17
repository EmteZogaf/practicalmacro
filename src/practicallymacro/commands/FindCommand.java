package practicallymacro.commands;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension;
import org.eclipse.jface.text.IFindReplaceTargetExtension3;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import practicallymacro.dialogs.FindConfigureDialog;
import practicallymacro.model.MacroManager;
import practicallymacro.util.Utilities;


public class FindCommand implements IMacroCommand {

	private boolean mReplaceAll;
	private boolean mScopeIsSelection;
//	private boolean mFindAfterReplace;
	private boolean mWrapSearch;
	private boolean mSearchForward;
	private boolean mCaseSensitive;
	private boolean mMatchWholeWord;
	private boolean mRegExpMode;
	private String mSearchString;
	private String mReplaceString;

	public static final String XML_ReplaceAll_Attr="replaceAll";
	public static final String XML_SelectionScope_Attr="selectionScope";
//	public static final String XML_FindAfterReplace_Attr="findAfterReplace";
	public static final String XML_WrapSearch_Attr="wrapSearch";
	public static final String XML_Forward_Attr="forward";
	public static final String XML_CaseSensitive_Attr="caseSensitive";
	public static final String XML_MatchWord_Attr="matchWord";
	public static final String XML_RegExp_Attr="regexp";
	public static final String XML_SearchString_Tag="searchString";
	public static final String XML_ReplaceString_Tag="replaceString";
	public static final String XML_MacroFindType="MacroFindCommand";
	
	public FindCommand()
	{
		mSearchString=null;
		mSearchForward=true;
		mReplaceAll=false;
		mCaseSensitive=false;
		mMatchWholeWord=false;
		mRegExpMode=false;
		mReplaceString=null;
		mWrapSearch=false;
		mScopeIsSelection=false;
//		mFindAfterReplace=false;
	}
	
	public FindCommand(String findString)
	{
		this();
		mSearchString=findString;
	}
	
	public void configure(Shell shell)
	{
		configureWithSearchTerm(shell, null);
	}
	
	private void configureWithSearchTerm(Shell shell, String initialSearchString)
	{
		FindConfigureDialog dlg=new FindConfigureDialog(shell, this, initialSearchString);
		dlg.open();
	}
	
	public void configureNew(Shell shell)
	{
		String initialSearchString=null;
		IEditorPart editor=Utilities.getActiveEditor();
		if (editor!=null)
		{
			initialSearchString=Utilities.getSelectedText(editor);
			if (initialSearchString.length()==0)
				initialSearchString=null;
		}
		configureWithSearchTerm(shell, initialSearchString);
	}

	public IMacroCommand copy()
	{
		FindCommand newCopy=new FindCommand(mSearchString);
		newCopy.mCaseSensitive=mCaseSensitive;
		newCopy.mMatchWholeWord=mMatchWholeWord;
		newCopy.mRegExpMode=mRegExpMode;
		newCopy.mReplaceAll=mReplaceAll;
		newCopy.mReplaceString=mReplaceString;
		newCopy.mSearchForward=mSearchForward;
//		newCopy.mFindAfterReplace=mFindAfterReplace;
		newCopy.mScopeIsSelection=mScopeIsSelection;
		newCopy.mWrapSearch=mWrapSearch;
		return newCopy;
	}


	public void dump()
	{
		System.out.println(getDescription());
	}

	public boolean execute(IEditorPart target)
	{
		ISourceViewer viewer = Utilities.getSourceViewer(target);
		IFindReplaceTarget findTarget=Utilities.getFindReplaceTarget(target);
		if (findTarget!=null && viewer!=null)
		{
			if (findTarget instanceof IFindReplaceTargetExtension)
			{
				IFindReplaceTargetExtension findTarget1=(IFindReplaceTargetExtension)findTarget;
				findTarget1.beginSession();
				try
				{
					StyledText widget=Utilities.getStyledText(target);
					if (findTarget instanceof IFindReplaceTargetExtension3)
					{
						IFindReplaceTargetExtension3 findTarget3=(IFindReplaceTargetExtension3)findTarget;
						
						int widgetBoundaryStart=0;
						int widgetBoundaryEnd=widget.getCharCount()-1;
						int boundaryStart=0;
						int boundaryEnd=viewer.getDocument().getLength()-1;
						if (mScopeIsSelection)
						{
							Point sel=Utilities.getUndirectedSelection(target);
							boundaryStart=sel.x;
							boundaryEnd=sel.y;
							Point widgetSel=widget.getSelectionRange();
							widgetBoundaryStart=widgetSel.x;
							widgetBoundaryEnd=widgetSel.x+widgetSel.y;
//							findTarget1.setScope(new Region(widgetSel.x, widgetSel.y));
							findTarget1.setScope(new Region(boundaryStart, boundaryEnd-boundaryStart));
						}
						else
						{
							findTarget1.setScope(null);
						}
						
						//if we have a replace string
						if (mReplaceString!=null && mReplaceAll)
						{
							//by convention, replaceall always returns true
							doReplaceAll(findTarget3, viewer, target);
							return true;
						}
							
						String searchString=mSearchString;
						if (searchString==null)
						{
							//try getting search text from clipboard, if not clipboard text, then use current selection
							try
							{
								Clipboard cb = new Clipboard(widget.getDisplay());
								TextTransfer transfer = TextTransfer.getInstance();
						        searchString = (String) cb.getContents(transfer);
							}
							catch (IllegalArgumentException e)
							{
								//no text on clipboard; don't print error
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
							
							//backup strategy is to use the selection
							if (searchString==null)
								searchString=Utilities.getSelectedText(target);
						}
						if (searchString==null || searchString.length()==0)
							return false;

						int startPos=widget.getCaretOffset()-1; //for some reason, has to be widget offsets, not sourceViewer
//						int startPos=Utilities.getCaretPos(target)-1;
						if (mScopeIsSelection)
						{
							if (mSearchForward)
								startPos=widgetBoundaryStart;
							else
								startPos=widgetBoundaryEnd;
						}
						int findPos=((IFindReplaceTargetExtension3)findTarget).findAndSelect(startPos, searchString, mSearchForward, mCaseSensitive, mMatchWholeWord, mRegExpMode);
						if (findPos>=0)
							findPos=Utilities.getUndirectedSelection(target).x;  //convert back to global coordinates
						if (!isInRange(searchString, boundaryStart, boundaryEnd, findPos) && mWrapSearch)
						{
							if (!mScopeIsSelection)
							{
								if (mSearchForward)
									startPos=widgetBoundaryStart;
								else
									startPos=widgetBoundaryEnd;
								
								findPos=((IFindReplaceTargetExtension3)findTarget).findAndSelect(startPos, searchString, mSearchForward, mCaseSensitive, mMatchWholeWord, mRegExpMode);
								if (findPos>=0)
									findPos=Utilities.getUndirectedSelection(target).x;  //convert back to global coordinates
							}
						}
							
						boolean inRange=(isInRange(searchString, boundaryStart, boundaryEnd, findPos));
						if (inRange && mReplaceString!=null)
						{
							findTarget3.replaceSelection(mReplaceString, mRegExpMode);
						}
						return inRange;
					}
				}
				finally
				{
					((IFindReplaceTargetExtension)findTarget).endSession();
				}
			}
		}

		return false;
	}

	private boolean isInRange(String searchString, int boundaryStart, int boundaryEnd, int foundPos)
	{
		if (foundPos>=boundaryStart && foundPos+searchString.length()<=boundaryEnd)
			return true;
		return false;
	}

	private void doReplaceAll(IFindReplaceTargetExtension3 findTarget, ISourceViewer sourceViewer, IEditorPart target)
	{
		int widgetPos=0;
		int endBoundary=0;
		if (mScopeIsSelection)
		{
			Point currentSel=Utilities.getUndirectedSelection(sourceViewer);
			endBoundary=currentSel.y;
			Point widgetSel=sourceViewer.getTextWidget().getSelectionRange();
			widgetPos=widgetSel.x;
		}
		
		while (true)
		{
			//perform a find
			int foundPos=findTarget.findAndSelect(widgetPos, mSearchString, mSearchForward, mCaseSensitive, mMatchWholeWord, mRegExpMode);
			int docPos=foundPos;
			if (docPos>=0)
				docPos=Utilities.getUndirectedSelection(target).x;  //convert back to global coordinates
			if (docPos<0 || (mScopeIsSelection && docPos>=endBoundary))
				break;
				
			//replace the text
			int oldDocLength=sourceViewer.getDocument().getLength();
			int foundStringSize=sourceViewer.getSelectedRange().y; //accounts for regular expressions by just using whatever the selected text is
			findTarget.replaceSelection(mReplaceString, mRegExpMode);
			int newDocLength=sourceViewer.getDocument().getLength();
			int amtAdded=newDocLength-oldDocLength;
			widgetPos=foundPos+foundStringSize+amtAdded;  //push past end of replacement string
			endBoundary+=amtAdded; //extend end character boundary in document 
		}
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
		StringBuffer buffer=new StringBuffer();
		buffer.append("Perform Find with the following parameters:\n");
		buffer.append("Search String: "+mSearchString+"\n");
		buffer.append("Case Sensitive: "+mCaseSensitive+"\n");
		buffer.append("Search Forward: "+mSearchForward+"\n");
		buffer.append("Match whole word: "+mMatchWholeWord+"\n");
		buffer.append("Selection scope: "+mScopeIsSelection+"\n");
		buffer.append("Wrap Search: "+mWrapSearch+"\n");
//		buffer.append("FindAfterReplace: "+mFindAfterReplace+"\n");
		buffer.append("Reg Exp Mode: "+mRegExpMode+"\n");
		buffer.append("Replace All: "+mReplaceAll+"\n");
		buffer.append("Replace String: "+mReplaceString+"\n");
		return buffer.toString();
	}

	public String getName()
	{
		return "Find "+((mSearchString!=null) ? mSearchString : "<clipboard/selection>");
	}

	public boolean isConfigurable()
	{
		return true;
	}

	public void persist(Document doc, Element commandElement)
	{
		Map<String, String> attrMap=new HashMap<String, String>();
		attrMap.put(XML_Forward_Attr, Boolean.toString(mSearchForward));
		attrMap.put(XML_CaseSensitive_Attr, Boolean.toString(mCaseSensitive));
		attrMap.put(XML_RegExp_Attr, Boolean.toString(mRegExpMode));
		attrMap.put(XML_MatchWord_Attr, Boolean.toString(mMatchWholeWord));
		attrMap.put(XML_ReplaceAll_Attr, Boolean.toString(mReplaceAll));
//		attrMap.put(XML_FindAfterReplace_Attr, Boolean.toString(mFindAfterReplace));
		attrMap.put(XML_SelectionScope_Attr, Boolean.toString(mScopeIsSelection));
		attrMap.put(XML_WrapSearch_Attr, Boolean.toString(mWrapSearch));
		
		Map<String, String> dataMap=new HashMap<String, String>();
		if (mSearchString!=null)
			dataMap.put(XML_SearchString_Tag, mSearchString);
		if (mReplaceString!=null)
			dataMap.put(XML_ReplaceString_Tag, mReplaceString);
		
		Utilities.persistCommand(doc, commandElement, XML_MacroFindType, attrMap, dataMap);
	}

	public IMacroCommand createFrom(Element commandElement)
	{
		Map<String, String> attrMap=new HashMap<String, String>();
		Set<String> attrKeys=new HashSet<String>();
		attrKeys.add(XML_Forward_Attr);
		attrKeys.add(XML_CaseSensitive_Attr);
		attrKeys.add(XML_RegExp_Attr);
		attrKeys.add(XML_MatchWord_Attr);
		attrKeys.add(XML_ReplaceAll_Attr);
		attrKeys.add(XML_WrapSearch_Attr);
		attrKeys.add(XML_SelectionScope_Attr);
//		attrKeys.add(XML_FindAfterReplace_Attr);

		Map<String, String> dataMap=new HashMap<String, String>();
		Set<String> dataKeys=new HashSet<String>();
		dataKeys.add(XML_SearchString_Tag);
		dataKeys.add(XML_ReplaceString_Tag);
		
		Utilities.getCommandData(commandElement, attrKeys, dataKeys, attrMap, dataMap);
		
		FindCommand newCommand=new FindCommand(dataMap.get(XML_SearchString_Tag));
		String value=attrMap.get(XML_Forward_Attr);
		if (value!=null)
		{
			newCommand.mSearchForward=Boolean.parseBoolean(value);
		}
		value=attrMap.get(XML_CaseSensitive_Attr);
		if (value!=null)
		{
			newCommand.mCaseSensitive=Boolean.parseBoolean(value);
		}
		value=attrMap.get(XML_MatchWord_Attr);
		if (value!=null)
		{
			newCommand.mMatchWholeWord=Boolean.parseBoolean(value);
		}
		value=attrMap.get(XML_RegExp_Attr);
		if (value!=null)
		{
			newCommand.mRegExpMode=Boolean.parseBoolean(value);
		}
		value=attrMap.get(XML_ReplaceAll_Attr);
		if (value!=null)
		{
			newCommand.mReplaceAll=Boolean.parseBoolean(value);
		}
		newCommand.mReplaceString=dataMap.get(XML_ReplaceString_Tag);
//		value=attrMap.get(XML_FindAfterReplace_Attr);
//		if (value!=null)
//		{
//			newCommand.mFindAfterReplace=Boolean.parseBoolean(value);
//		}
		value=attrMap.get(XML_SelectionScope_Attr);
		if (value!=null)
		{
			newCommand.mScopeIsSelection=Boolean.parseBoolean(value);
		}
		value=attrMap.get(XML_WrapSearch_Attr);
		if (value!=null)
		{
			newCommand.mWrapSearch=Boolean.parseBoolean(value);
		}
		
		return newCommand;
	}

	public void setSearchForward(boolean forward)
	{
		mSearchForward=forward;
	}

	public boolean isReplaceAll() {
		return mReplaceAll;
	}

	public void setReplaceAll(boolean replaceAll) {
		mReplaceAll = replaceAll;
	}

	public boolean isCaseSensitive() {
		return mCaseSensitive;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		mCaseSensitive = caseSensitive;
	}

	public boolean isMatchWholeWord() {
		return mMatchWholeWord;
	}

	public void setMatchWholeWord(boolean matchWholeWord) {
		mMatchWholeWord = matchWholeWord;
	}

	public boolean isRegExpMode() {
		return mRegExpMode;
	}

	public void setRegExpMode(boolean regExpMode) {
		mRegExpMode = regExpMode;
	}

	public String getSearchString() {
		return mSearchString;
	}

	public void setSearchString(String searchString) {
		mSearchString = searchString;
	}

	public String getReplaceString() {
		return mReplaceString;
	}

	public void setReplaceString(String replaceString) {
		mReplaceString = replaceString;
	}

	public boolean isSearchForward() {
		return mSearchForward;
	}

	public boolean isScopeIsSelection() {
		return mScopeIsSelection;
	}

	public void setScopeIsSelection(boolean scopeIsSelection) {
		mScopeIsSelection = scopeIsSelection;
	}

//	public boolean isFindAfterReplace() {
//		return mFindAfterReplace;
//	}
//
//	public void setFindAfterReplace(boolean findAfterReplace) {
//		mFindAfterReplace = findAfterReplace;
//	}

	public boolean isWrapSearch() {
		return mWrapSearch;
	}

	public void setWrapSearch(boolean wrapSearch) {
		mWrapSearch = wrapSearch;
	}

	public boolean requiresPost()
	{
		return false;
	}

}
