package kr.pe.silent.websequencediagrams.editors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;

public class WSDEditor extends TextEditor {

	private Combo cmbSkin;
	private ScrolledComposite sc;
	private Label lblImage;

	private String skin;
	
	private boolean imageUpdating = false;
	
	@Override
	public void createPartControl(Composite parent) {
		SashForm form = new SashForm(parent, SWT.HORIZONTAL);

		// left
		super.createPartControl(form);

		// right
		Composite right = new Composite(form, SWT.NONE);
		right.setBackground(getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		GridLayout layout = new GridLayout(1, true);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 0;
		right.setLayout(layout);
		
		cmbSkin = new Combo(right, SWT.FLAT | SWT.READ_ONLY);
		cmbSkin.setItems(WSDUtil.skinLabels);
		cmbSkin.setText(WSDUtil.DEFAULT_SKIN);
		cmbSkin.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		cmbSkin.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				skin = WSDUtil.skins[cmbSkin.getSelectionIndex()];
				updateImage();
			}
		});
		
		sc = new ScrolledComposite(right, SWT.H_SCROLL | SWT.V_SCROLL);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		lblImage = new Label(sc, SWT.NONE);
		lblImage.setBackground(getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		sc.setContent(lblImage);
		sc.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				sc.setMinSize(lblImage.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});
		
		form.setWeights(new int[] {2, 3});
		
		
		// listener
		getSourceViewer().getTextWidget().addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateImage();
			}
		});
		
		skin = WSDUtil.skins[cmbSkin.getSelectionIndex()];
		updateImage();
	}
	
	@Override
	public void doSave(IProgressMonitor progressMonitor) {
		updateImage();
		super.doSave(progressMonitor);
	}
	
	@Override
	public void doSaveAs() {
		updateImage();
		super.doSaveAs();
	}

	private void updateImage() {
		if (imageUpdating) {
			return;
		}
		
		imageUpdating = true;

		final String text = getSourceViewer().getTextWidget().getText();
		final String outFile = ((FileEditorInput)getEditorInput()).getPath().removeFileExtension() + ".png";
		final String style = skin;

		new Thread() {
			public void run() {
				WSDUtil.getSequenceDiagram(text, outFile, style);
				
				getEditorSite().getShell().getDisplay().asyncExec(new Runnable() {
					public void run() {
						if (lblImage.getImage() != null)
							lblImage.getImage().dispose();
						lblImage.setImage(new Image(null, outFile));
						
						sc.setMinSize(lblImage.computeSize(SWT.DEFAULT, SWT.DEFAULT));
						
						imageUpdating = false;
						
						try {
							((IFileEditorInput) getEditorInput()).getFile().getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
						} catch (CoreException e1) {
						}
					}
				});
			};
		}.start();
	}
	
}
