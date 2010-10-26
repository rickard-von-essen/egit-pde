/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.internal.relengtools;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class BuildNotesPage extends WizardPage {

	private static final String FOLDER_BIN = "bin"; //$NON-NLS-1$

	private static final String EXT_HTML = "html"; //$NON-NLS-1$

	private static final String BUILD_NOTES_HTML = "/build_notes.html"; //$NON-NLS-1$

	private final String FILE_PATH_KEY = "BuildNotesPage.filePath"; //$NON-NLS-1$

	private final String UPDATE_FILE_KEY = "BuildNotesPage.updateNotesButton"; //$NON-NLS-1$

	private Button updateNotesButton;

	private boolean updateNotesButtonChecked;

	private final IDialogSettings settings;

	private Text reportText;

	private Map bugSummaryMap;

	private boolean validPath;

	private Text filePath;

	private Button browse;

	private IFile iFile;

	protected BuildNotesPage(String pageName, String title,
			IDialogSettings settings, ImageDescriptor image) {
		super(pageName, title, image);
		this.settings = settings;
	}

	@Override
	public void createControl(Composite parent) {
		GridData data = new GridData(GridData.FILL_BOTH);
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(3, false));
		composite.setLayoutData(data);

		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		updateNotesButton = new Button(composite, SWT.CHECK);
		updateNotesButton.setText(Messages.getString("BuildNotesPage.2")); //$NON-NLS-1$
		updateNotesButton.setLayoutData(data);
		updateNotesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateNotesButtonChecked = updateNotesButton.getSelection();
				if (updateNotesButtonChecked) {
					filePath.setEnabled(true);
					filePath.setText(filePath.getText());
					browse.setEnabled(true);
				} else {
					filePath.setEnabled(false);
					setErrorMessage(null);
					browse.setEnabled(false);
				}
				updateButtons();
			}
		});

		final Label label = new Label(composite, SWT.LEFT);
		label.setText(Messages.getString("BuildNotesPage.3")); //$NON-NLS-1$

		data = new GridData(GridData.FILL_HORIZONTAL);
		filePath = new Text(composite, SWT.BORDER);
		filePath.setLayoutData(data);
		filePath.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				final Path path = new Path(filePath.getText());
				validPath = false;
				if (!path.isEmpty()) {
					final IFile file = ResourcesPlugin.getWorkspace().getRoot()
							.getFile(path);
					if (path.isValidPath(filePath.getText())
							&& file.getParent().exists()) {
						if (path.getFileExtension().equals(EXT_HTML)) {
							setErrorMessage(null);
							validPath = true;
							iFile = file;
						} else {
							setErrorMessage(Messages
									.getString("BuildNotesPage.5")); //$NON-NLS-1$
						}
					} else {
						setErrorMessage(Messages.getString("BuildNotesPage.6")); //$NON-NLS-1$
					}
				} else {
					// path is empty
					setErrorMessage(Messages.getString("BuildNotesPage.7")); //$NON-NLS-1$
				}
				updateButtons();
			}
		});

		browse = new Button(composite, SWT.PUSH);
		browse.setText(Messages.getString("BuildNotesPage.8")); //$NON-NLS-1$
		browse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final IResource iResource = buildNotesFileDialog();
				if (iResource instanceof IFile) {
					final IFile iFile = (IFile) iResource;
					filePath.setText(iFile.getFullPath().toString());
				} else if (iResource instanceof IFolder) {
					final IFolder iFolder = (IFolder) iResource;
					filePath.setText(iFolder.getFullPath().toString()
							+ BUILD_NOTES_HTML);
				} else if (iResource instanceof IProject) {
					final IProject iProject = (IProject) iResource;
					filePath.setText(iProject.getFullPath().toString()
							+ BUILD_NOTES_HTML);
				}
			}
		});
		// SWTUtil.setButtonDimensionHint(browse);

		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 3;
		reportText = new Text(composite, SWT.READ_ONLY | SWT.MULTI | SWT.BORDER
				| SWT.WRAP | SWT.V_SCROLL);
		reportText.setLayoutData(data);

		initialize();
		setControl(composite);
	}

	private void initialize() {
		initSelections();
	}

	/*
	 * initialize the controls on the page
	 */
	private void initSelections() {
		if (settings == null || settings.get(UPDATE_FILE_KEY) == null
				|| settings.get(FILE_PATH_KEY) == null) {
			updateNotesButton.setSelection(false);
			updateNotesButtonChecked = false;
			browse.setEnabled(false);
			filePath.setEnabled(false);
			return;
		} else {
			final boolean b = settings.getBoolean(UPDATE_FILE_KEY);
			updateNotesButton.setSelection(b);
			updateNotesButtonChecked = b;
			filePath.setText(settings.get(FILE_PATH_KEY));
			browse.setEnabled(true);
			filePath.setEnabled(true);
		}
	}

	/*
	 * enable or disable wizard buttons
	 */
	public void updateButtons() {
		if (isUpdateNotesButtonChecked() && !getValidPath()) {
			setPageComplete(false);
		} else {
			setPageComplete(true);
		}
	}

	/*
	 * if file doesn't already exist, prepare new one otherwise call method to
	 * write to existing file
	 */
	public void updateNotesFile() {
		if (bugSummaryMap != null && !filePath.isDisposed()) {
			final Path path = new Path(filePath.getText());
			final IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
					.getRoot();
			final IFile file = root.getFile(path);
			if (file.exists()) {
				writeUpdate(file);
			} else {
				if (file.getParent().exists()) {
					try {
						getContainer().run(true, true,
								new IRunnableWithProgress() {
									@Override
									public void run(IProgressMonitor monitor)
											throws InvocationTargetException,
											InterruptedException {
										monitor.beginTask(
												Messages.getString("BuildNotesPage.11"), //$NON-NLS-1$
												100);
										final StringBuffer buffer = new StringBuffer();
										buffer.append("<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">\n");
										buffer.append("<html>\n\n");
										buffer.append("<head>\n");
										buffer.append("   <meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">\n");
										buffer.append("   <meta name=\"Build\" content=\"Build\">\n");
										buffer.append("   <title>Eclipse Platform Release Notes (3.3) - JFace and Workbench</title>\n");
										buffer.append("</head>\n\n");
										buffer.append("<body>\n\n");
										buffer.append("<h1>Eclipse Platform Build Notes (3.3)<br>\n");
										buffer.append("JFace and Workbench</h1>");

										final ByteArrayInputStream c = new ByteArrayInputStream(
												buffer.toString().getBytes());
										try {
											file.create(c, true, monitor);
										} catch (final CoreException e) {
											e.printStackTrace();
										}

										try {
											c.close();
										} catch (final IOException e) {
											e.printStackTrace();
										}
										monitor.done();
									}
								});
					} catch (final InvocationTargetException e) {
						e.printStackTrace();
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
					writeUpdate(file);
				}
			}
		}
	}

	/*
	 * write update to build notes file
	 */
	public void writeUpdate(final IFile file) {
		BufferedInputStream originalContents = null;
		try {
			originalContents = new BufferedInputStream(file.getContents());
			final StringBuffer buffer = new StringBuffer();

			int character;
			while ((character = originalContents.read()) != -1) {
				buffer.append((char) character);
			}

			final String marker = "</h1>";
			final SimpleDateFormat formatter = new SimpleDateFormat(
					"MMMM dd, yyyy, h:mm a");
			final Date currentTime = new Date();
			String dateString = formatter.format(currentTime);
			dateString = dateString.replaceAll("AM", "a.m.");
			dateString = dateString.replaceAll("PM", "p.m.");

			final int index = buffer.indexOf(marker) + marker.length();
			if (index != -1) {
				final StringBuffer insertBuffer = new StringBuffer();
				insertBuffer.append("\n<p>Integration Build (" + dateString
						+ ")</p>\n");
				insertBuffer.append("  <p>Problem reports updated</p>\n");
				insertBuffer.append("  <p>\n");

				final Iterator i = bugSummaryMap.entrySet().iterator();
				while (i.hasNext()) {
					final Map.Entry entry = (Map.Entry) i.next();
					final Integer bug = (Integer) entry.getKey();
					final String summary = (String) entry.getValue();
					insertBuffer
							.append("<a href=\"https://bugs.eclipse.org/bugs/show_bug.cgi?id=");
					insertBuffer.append(bug);
					insertBuffer.append("\">Bug ");
					insertBuffer.append(bug);
					insertBuffer.append("</a>. ");
					insertBuffer.append(summary + "<br>\n");
				}
				insertBuffer.append("  </p>");
				buffer.insert(index, "\n" + insertBuffer.toString());

				try {
					getContainer().run(true, true, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							monitor.beginTask(
									Messages.getString("BuildNotesPage.38"), 100); //$NON-NLS-1$
							final ByteArrayInputStream c = new ByteArrayInputStream(
									buffer.toString().getBytes());
							try {
								file.setContents(c, true, true, monitor);
								c.close();
							} catch (final CoreException e) {
								e.printStackTrace();
							} catch (final IOException e) {
								e.printStackTrace();
							}
							monitor.done();
						}
					});
				} catch (final InvocationTargetException e) {
					e.printStackTrace();
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			reportText.setText("");
			// TODO this is where we need to scan the logs and get the bugs.
			final GetBugsOperation getBugsOperation = new GetBugsOperation(
					(ReleaseWizard) getWizard(), null);
			getBugsOperation.run(this);
			if (bugSummaryMap != null) {
				final String tempText = outputReport();
				if (tempText != null) {
					reportText.setText(tempText);
				}
			}
		}
	}

	public IResource buildNotesFileDialog() {
		final ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
				getShell(), new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());

		// filter for .html files only and exclude bin folders
		dialog.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				if (element instanceof IFile) {
					final IFile file = (IFile) element;
					final IPath path = file.getFullPath();
					if (path.getFileExtension().equals(EXT_HTML)) {
						return true;
					}
				} else if (element instanceof IFolder) {
					final IFolder folder = (IFolder) element;
					if (folder.getName().equals(FOLDER_BIN)) {
						return false;
					}
					return true;
				} else if (element instanceof IProject) {
					return true;
				}
				return false;
			}
		});

		dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
		dialog.setAllowMultiple(false);
		dialog.setTitle(Messages.getString("BuildNotesPage.42")); //$NON-NLS-1$
		dialog.setMessage(Messages.getString("BuildNotesPage.43")); //$NON-NLS-1$
		if (dialog.open() == Window.OK) {
			final Object[] elements = dialog.getResult();
			if (elements != null && elements.length > 0) {
				if (elements[0] instanceof IFile) {
					final IFile iFile = (IFile) elements[0];
					return iFile;
				} else if (elements[0] instanceof IFolder) {
					final IFolder iFolder = (IFolder) elements[0];
					return iFolder;
				} else if (elements[0] instanceof IProject) {
					final IProject iProject = (IProject) elements[0];
					return iProject;
				}
			}
		}
		return null;
	}

	public boolean isUpdateNotesButtonChecked() {
		return updateNotesButtonChecked;
	}

	/**
	 * return string of report
	 */
	public String outputReport() {
		final StringBuffer buffer = new StringBuffer();
		if (bugSummaryMap.size() < 1) {
			buffer.append("The map file has been updated.\n");
		} else {
			buffer.append("The map file has been updated for the following Bug changes:\n");
			final Iterator i = bugSummaryMap.entrySet().iterator();

			while (i.hasNext()) {
				final Map.Entry entry = (Map.Entry) i.next();
				final Integer bug = (Integer) entry.getKey();
				final String summary = (String) entry.getValue();
				buffer.append("+ Bug " + bug + ". " + summary + "\n");
			}
		}
		buffer.append("\nThe following projects have changed:\n");
		final IProject[] iProjects = ((ReleaseWizard) getWizard())
				.getSelectedProjects();
		for (int j = 0; j < iProjects.length; j++) {
			buffer.append(iProjects[j].getName() + "\n");
		}
		return buffer.toString();
	}

	public void setMap(Map map) {
		this.bugSummaryMap = map;
	}

	public boolean getValidPath() {
		return validPath;
	}

	public IFile getIFile() {
		return iFile;
	}

	public void saveSettings() {
		settings.put(UPDATE_FILE_KEY, updateNotesButtonChecked);
		settings.put(FILE_PATH_KEY, filePath.getText());
	}
}
