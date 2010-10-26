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

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class RepoCheck {

	private File repoDir;

	private FileRepository repo;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("USAGE: " + RepoCheck.class.getName()
					+ " /path/to/repo");
			return;
		}
		try {
			RepoCheck check = new RepoCheck(args[0]);
			check.build();
			check.displayInfo();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void displayInfo() throws IOException {
		System.out.println("current branch: " + repo.getBranch());
	}

	public RepoCheck(String repoDirectory) {
		repoDir = new File(repoDirectory);
	}

	public void build() throws IOException {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		builder.setWorkTree(repoDir);
		repo = builder.readEnvironment().findGitDir(repoDir).build();
	}
}
