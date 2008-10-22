package practicallymacro.commands;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension;
import org.eclipse.jface.text.IFindReplaceTargetExtension3;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.custom.StyledText;
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
		FindConfigureDialog dlg=new FindConfigureDialog(shell, this);
		dlg.open();
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
		IFindReplaceTarget findTarget=Utilities.getFindReplaceTarget(target);
		if (findTarget!=null)
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
						
						int boundaryStart=0;
						int boundaryEnd=widget.getCharCount()-1;
						if (mScopeIsSelection)
						{
							Point sel=widget.getSelectionRange();
							boundaryStart=sel.x;
							boundaryEnd=sel.x+sel.y;
							findTarget1.setScope(new Region(sel.x, sel.y));
						}
						else
						{
							findTarget1.setScope(null);
						}
						
						//if we have a replace string
						if (mReplaceString!=null && mReplaceAll)
						{
							//by convention, replaceall always returns true
							doReplaceAll(findTarget3, widget);
							return true;
						}
							
						String searchString=mSearchString;
						if (searchString==null)
						{
							searchString=widget.getSelectionText();
						}
						if (searchString.length()==0)
							return false;

						int startPos=widget.getCaretOffset()-1;
						if (mScopeIsSelection)
						{
							if (mSearchForward)
								startPos=boundaryStart;
							else
								startPos=boundaryEnd;
						}
						int findPos=((IFindReplaceTargetExtension3)findTarget).findAndSelect(startPos, searchString, mSearchForward, mCaseSensitive, mMatchWholeWord, mRegExpMode);
						if (!isInRange(searchString, boundaryStart, boundaryEnd, findPos) && mWrapSearch)
						{
							if (!mScopeIsSelection)
							{
								if (mSearchForward)
									startPos=boundaryStart;
								else
									startPos=boundaryEnd;
								
								findPos=((IFindReplaceTargetExtension3)findTarget).findAndSelect(startPos, searchString, mSearchForward, mCaseSensitive, mMatchWholeWord, mRegExpMode);
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

	private void doReplaceAll(IFindReplaceTargetExtension3 findTarget, StyledText widget)
	{
		int docPos=0;
		int endBoundary=0;
		if (mScopeIsSelection)
		{
			docPos=widget.getSelection().x;
			endBoundary=widget.getSelection().y;
		}
		
		while (true)
		{
			//perform a find
			int foundPos=findTarget.findAndSelect(docPos, mSearchString, mSearchForward, mCaseSensitive, mMatchWholeWord, mRegExpMode);
			if (foundPos<0 || (mScopeIsSelection && foundPos>=endBoundary))
				break;
				
			//replace the text
			int oldDocLength=widget.getCharCount();
			int searchStringSize=widget.getSelectionCount(); //accounts for regular expressions
			findTarget.replaceSelection(mReplaceString, mRegExpMode);
			int newDocLength=widget.getCharCount();
			int amtAdded=newDocLength-oldDocLength;
			docPos=foundPos+searchStringSize+amtAdded;
			endBoundary+=amtAdded;
			
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
		return "Find "+mSearchString;
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
