package practicallymacro.dialogs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import practicallymacro.model.EditorMacro;
import practicallymacro.model.MacroManager;


public class ExportMacroDialog extends Dialog
{
	private List<EditorMacro> mMacros=new ArrayList<EditorMacro>();
	private Text mFileText;
	private Text mExportText;
	
	public ExportMacroDialog(Shell shell, List<EditorMacro>macros)
	{
		super(shell);
		mMacros=macros;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}
	
	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText("Export Macros");
		Composite comp=new Composite(parent, SWT.None);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite fileComp=new Composite(comp, SWT.None);
		fileComp.setLayout(new GridLayout(3, false));
		fileComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label l=new Label(fileComp, SWT.None);
		l.setText("Export file name:");
		
		mFileText=new Text(fileComp, SWT.BORDER | SWT.SINGLE);
		GridData gd=new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint=300;
		mFileText.setLayoutData(gd);
		
		Button browse=new Button(fileComp, SWT.PUSH);
		browse.setText("Browse...");
		browse.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				FileDialog fd=new FileDialog(getShell());
				String filePath=fd.open();
				if (filePath!=null)
				{
					mFileText.setText(filePath);
				}
			}
		});
		
		mExportText=new Text(comp, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL);
		gd=new GridData(GridData.FILL_BOTH);
		gd.heightHint=400;
		gd.widthHint=300;
		mExportText.setLayoutData(gd);
		
		String macroXML=MacroManager.persistMacros(mMacros);
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		try
		{
	        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
	        Document doc = docBuilder.newDocument();
	        Element masterElement=doc.createElement("macroDefinition");
	        doc.appendChild(masterElement);
	        CDATASection cdataContent = doc.createCDATASection(macroXML);
			masterElement.appendChild(cdataContent);
			String xmlData=MacroManager.outputXML(doc);
			mExportText.setText(xmlData);
   
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{}
		
		return comp;
	}

	@Override
	protected void okPressed()
	{
		Writer w=null;
		try
		{
			String filePath=mFileText.getText();
			File outputFile=new File(filePath);
			w=new FileWriter(outputFile);
			String data=MacroManager.persistMacros(mMacros);
			w.write(data);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (w!=null)
			{
				try
				{
					w.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		
		super.okPressed();
	}

}
