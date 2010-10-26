/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.internal.relengtools;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

public class ReleaseWizard extends Wizard {

	private Dialog parentDialog;

	private ArrayList<IProject> preSelectedProjects;

	private final IDialogSettings section = new DialogSettings("Section");

	private MapProject mapProject;

	private MapProjectSelectionPage mapSelectionPage;

	private ProjectSelectionPage projectSelectionPage;

	private IProject[] selectedProjects;

	private TagPage tagPage;

	private BuildNotesPage buildNotesPage;

	@Override
	public boolean performFinish() {
		if (!isProjectSelected())
			return false;
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Processing", 1 + selectedProjects.length);
					monitor.worked(1);
					final String tag = tagPage.getTagString();
					System.out.println("Processing tag: " + tag);
					for (final IProject proj : selectedProjects) {
						monitor.worked(1);
						System.out.println("processing: " + proj);
						try {
							mapProject.updateFile(proj, tag);
						} catch (final CoreException e) {
							e.printStackTrace();
						}
					}
					monitor.done();
				}
			});
			return true;
		} catch (final InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	private boolean isProjectSelected() {
		if (selectedProjects == null || selectedProjects.length == 0) {
			return false;
		}
		return true;
	}

	public void setPreSelection(ArrayList<IProject> selection) {
		preSelectedProjects = selection;
	}

	public boolean execute(Shell shell) {
		setNeedsProgressMonitor(true);
		final WizardDialog dialog = new WizardDialog(shell, this);
		setParentDialog(dialog);
		return (dialog.open() == Window.OK);
	}

	public void setParentDialog(Dialog p) {
		this.parentDialog = p;
	}

	@Override
	public void addPages() {
		addMapSelectionPage();
	}

	private void addMapSelectionPage() {
		mapSelectionPage = new MapProjectSelectionPage(
				"MapProjectSelectionPage", "Map Project Selection", section,
				null);
		mapSelectionPage.setDescription("Map Project Selection"); //$NON-NLS-1$
		addPage(mapSelectionPage);

		projectSelectionPage = new ProjectSelectionPage("ProjectSelectionPage", //$NON-NLS-1$
				Messages.getString("ReleaseWizard.6"), //$NON-NLS-1$
				section, null);
		projectSelectionPage.setDescription(Messages
				.getString("ReleaseWizard.7")); //$NON-NLS-1$
		addPage(projectSelectionPage);

		tagPage = new TagPage("TagPage", //$NON-NLS-1$
				Messages.getString("ReleaseWizard.9"), //$NON-NLS-1$
				section, null);
		tagPage.setDescription(Messages.getString("ReleaseWizard.10")); //$NON-NLS-1$
		addPage(tagPage);

		buildNotesPage = new BuildNotesPage("Build Notes Page", //$NON-NLS-1$
				Messages.getString("ReleaseWizard.1"), //$NON-NLS-1$
				section, null);
		buildNotesPage.setDescription(Messages.getString("ReleaseWizard.0")); //$NON-NLS-1$
		addPage(buildNotesPage);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == mapSelectionPage) {
			// if (selectedProjects == null && preSelectedProjectsArr != null) {
			// projectSelectionPage.setSelection(preSelectedProjectsARrr);
			// selectedProjects= preSelectedProjects;
			// }
			return projectSelectionPage;
		}
		if (page == projectSelectionPage) {
			final IProject[] projects = projectSelectionPage
					.getCheckedProjects();
			if (projects != null && projects.length > 0) {
				selectedProjects = projects;
			}

			if (projectSelectionPage.isCompareButtonChecked()) {
				return buildNotesPage;
			} else
				return tagPage;
		}
		if (page == buildNotesPage) {
			return tagPage;
		}
		// if (page == tagPage) {
		// if (tagPage.compareButtonSelected()){
		// mapComparePage.setTag(tagPage.getTagString());
		// return mapComparePage;
		// }
		// if (tagPage.commitButtonSelected())
		// return commentPage;
		// }
		return null;
	}

	public MapProject getMapProject() {
		return mapProject;
	}

	public void broadcastMapProjectChange(MapProject m) {
		mapProject = m;
		projectSelectionPage.updateMapProject(m);
		// projectComparePage.updateMapProject(m);
		// mapComparePage.updateMapProject(m);
	}

	public IProject[] getPreSelectedProjects() {
		return preSelectedProjects.toArray(new IProject[preSelectedProjects
				.size()]);
	}

	// the update will happen when (1)from project selection page to compare
	// project page or (2)from
	// project selection page to Enter Tag page. It calls shouldRemove() to
	// determine the projects to keep
	public void updateSelectedProject() {
		selectedProjects = projectSelectionPage.getCheckedProjects();
		// selectedProjects = performPrompting(selectedProjects);
		projectSelectionPage.setSelection(selectedProjects);
	}

	public void setSelectedProjects(IResource[] projects) {
		if (projects == null)
			selectedProjects = null;
		else {
			selectedProjects = new IProject[projects.length];
			for (int i = 0; i < projects.length; i++) {
				selectedProjects[i] = (IProject) projects[i];
			}
		}
	}

	protected ProjectSelectionPage getProjectSelectionPage() {
		return projectSelectionPage;
	}

	public IProject[] getSelectedProjects() {
		return selectedProjects;
	}
}
