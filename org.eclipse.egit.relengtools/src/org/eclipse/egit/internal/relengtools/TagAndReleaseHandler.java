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

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.util.Util;

public class TagAndReleaseHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);
		ReleaseWizard wizard = new ReleaseWizard();
		ArrayList<IProject> selection = new ArrayList<IProject>();
		ISelection s = HandlerUtil.getCurrentSelection(event);
		if (s instanceof IStructuredSelection) {
			Iterator i = ((IStructuredSelection) s).iterator();
			while (i.hasNext()) {
				Object obj = i.next();
				IProject proj = (IProject) Util.getAdapter(obj, IProject.class);
				if (proj != null) {
					selection.add(proj);
				}
			}
		}
		if (!s.isEmpty()) {
			wizard.setPreSelection(selection);
		}
		wizard.execute(shell);
		return null;
	}

}
