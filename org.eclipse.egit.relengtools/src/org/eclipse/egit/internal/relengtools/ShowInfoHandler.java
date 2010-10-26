/*******************************************************************************
 * Copyright (C) 2011, Chris Aniszczyk <caniszczyk@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Chris Aniszczyk - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.internal.relengtools;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.util.Util;

public class ShowInfoHandler extends AbstractHandler implements
		IExecutableExtension {
	static final String OLD_TAG = "v20100723-1115";

	static final String NEW_TAG = "v20101018-1500";

	private boolean runTest = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.
	 * ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (runTest) {
			try {
				runTest();
			} catch (final Exception e) {
				throw new ExecutionException(e.getMessage(), e);
			}
			return null;
		}
		// show info on an existing project
		final ISelection s = HandlerUtil.getCurrentSelection(event);
		if (s instanceof IStructuredSelection) {
			final Object obj = ((IStructuredSelection) s).getFirstElement();
			final IProject proj = (IProject) Util.getAdapter(obj,
					IProject.class);
			if (proj != null) {
				try {
					showInfo(proj);
				} catch (final Exception e) {
					throw new ExecutionException(e.getMessage(), e);
				}
			}
		}
		return null;
	}

	private void runTest() throws Exception {
		// first test project
		deeplinkTest();

		// second test project
		handlerTest();
	}

	private void deeplinkTest() throws Exception {
		final IProject deeplink = ResourcesPlugin.getWorkspace().getRoot()
				.getProject("org.eclipse.e4.core.deeplink");
		System.out.println("\nTest for project: " + deeplink);
		final RepositoryMapping rm = RepositoryMapping.getMapping(deeplink);
		final Repository repo = rm.getRepository();
		final RevCommit expectedLastCommit = getCommit(repo,
				"d660f0a07c8ff16c9b46b3d69bc1c271b2cf4aba");
		final RevCommit latestCommit = getLatestCommitFor(rm, repo, deeplink);
		System.out.println(expectedLastCommit.equals(latestCommit)
				+ " Latest commit: expected: " + expectedLastCommit
				+ "\n\tgot: " + latestCommit);

		final Map<String, Ref> tags = repo.getTags();
		final Set<Ref> expectedTags = new HashSet<Ref>(Arrays.asList(tags
				.get(NEW_TAG)));
		final Set<Ref> twoTags = getTagsForCommit(repo, expectedLastCommit);
		System.out.println(expectedTags.equals(twoTags)
				+ " Found tags: expected: " + expectedTags + "\n\tgot: "
				+ twoTags);

		final Set<Ref> tagsContainingCommit = getTagsContainingCommit(repo,
				expectedLastCommit);
		System.out.println(expectedTags.equals(tagsContainingCommit)
				+ " Found tags containing commit " + expectedLastCommit
				+ ":\n\texpected: " + expectedTags + "\n\tgot: "
				+ tagsContainingCommit);
	}

	private void handlerTest() throws Exception {
		final IProject handler = ResourcesPlugin.getWorkspace().getRoot()
				.getProject("org.eclipse.e4.core.deeplink.handler");
		System.out.println("\nTest for project: " + handler);
		final RepositoryMapping rm = RepositoryMapping.getMapping(handler);
		final Repository repo = rm.getRepository();
		final RevCommit expectedLastCommit = getCommit(repo,
				"b42ad39c90fc57b679154ca33dfb306004dc08a3");
		final RevCommit latestCommit = getLatestCommitFor(rm, repo, handler);
		System.out.println(expectedLastCommit.equals(latestCommit)
				+ " Latest commit: expected: " + expectedLastCommit
				+ "\n\tgot: " + latestCommit);

		final Set<Ref> expectedTags = Collections.EMPTY_SET;
		final Set<Ref> noTags = getTagsForCommit(repo, expectedLastCommit);
		System.out.println(expectedTags.equals(noTags)
				+ " Found tags: expected: " + expectedTags + "\n\tgot: "
				+ noTags);

		final Map<String, Ref> tags = repo.getTags();
		final Set<Ref> expectedContainingTags = new HashSet<Ref>(Arrays.asList(
				tags.get("R0_10"), tags.get("v20100722-1700"),
				tags.get("v20100723-1115"), tags.get("v20101018-1500")));
		final Set<Ref> tagsContainingCommit = getTagsContainingCommit(repo,
				expectedLastCommit);
		System.out.println(expectedContainingTags.equals(tagsContainingCommit)
				+ " Found tags containing commit " + expectedLastCommit
				+ ":\n\texpected: " + expectedContainingTags + "\n\tgot: "
				+ tagsContainingCommit);
	}

	/**
	 * @param proj
	 * @throws Exception
	 */
	private void showInfo(IProject proj) throws Exception {
		final RepositoryMapping rm = RepositoryMapping.getMapping(proj);
		final Repository repo = rm.getRepository();

		// final RevCommit latestCommit = getLatestCommitFor(rm, repo, proj);
		final RevCommit latestCommit = getLatestCommitFor(rm, repo, proj);
		System.out
				.println(latestCommit + ": " + latestCommit.getShortMessage());

		final RevCommit oldCommit = getCommitForTag(repo, OLD_TAG);

		final RevCommit newCommit = getCommitForTag(repo, NEW_TAG);

		System.out.println(getTagsForCommit(repo, newCommit));
		System.out.println(getTagsForCommit(repo, newCommit));
		System.out.println(getTagsForCommit(repo,
				getCommit(repo, "b42ad39c90fc57b679154ca33dfb306004dc08a3")));

		showLogBetween(repo, oldCommit, newCommit);
	}

	public static void showLogBetween(final Repository repo,
			final RevCommit oldCommit, final RevCommit newCommit)
			throws MissingObjectException, IncorrectObjectTypeException,
			NoHeadException, Exception {
		final Git git = new Git(repo);
		final LogCommand command = git.log();
		command.addRange(oldCommit, newCommit);
		System.out.println("\nCommits:");
		for (final RevCommit rc : command.call()) {
			System.out.println(rc);
			System.out.print("Tags: ");
			System.out.println(getTagsForCommit(repo, rc));
			System.out.println(rc.getShortMessage());
		}
	}

	public static RevCommit getLatestCommitFor(RepositoryMapping mapping,
			Repository repo, IProject project) throws Exception {
		final RevWalk walk = new RevWalk(repo);
		walk.reset();
		walk.sort(RevSort.TOPO, true);
		walk.sort(RevSort.COMMIT_TIME_DESC, true);
		walk.setTreeFilter(AndTreeFilter.create(TreeFilter.ANY_DIFF,
				PathFilter.create(mapping.getRepoRelativePath(project))));
		final ObjectId start = repo.resolve(Constants.HEAD);
		walk.markStart(walk.parseCommit(start));

		// I should only be able to see commits that contain files
		// where project is a path prefix
		final RevCommit commit = walk.next();
		return commit;
	}

	public static RevCommit getCommitForTag(Repository repo, String name)
			throws Exception {
		final Ref ref = repo.getTags().get(name);
		final RevWalk walk = new RevWalk(repo);
		final RevObject obj = walk.parseAny(ref.getObjectId());
		final RevCommit tagCommit;
		if (obj instanceof RevCommit) {
			tagCommit = (RevCommit) obj;
		} else {
			tagCommit = walk.parseCommit(((RevTag) obj).getObject());
		}
		return tagCommit;
	}

	public static Set<Ref> getTagsForCommit(Repository repo, RevCommit c)
			throws Exception {
		initializeTagMap(repo);
		Set<Ref> s = commitToTagRef.get(c);
		if (s == null) {
			s = Collections.EMPTY_SET;
		}
		return s;
	}

	private static HashMap<RevCommit, Set<Ref>> commitToTagRef = null;

	private static void initializeTagMap(Repository repo) throws Exception {
		if (commitToTagRef == null) {
			final RevWalk walk = new RevWalk(repo);
			commitToTagRef = new HashMap<RevCommit, Set<Ref>>();
			for (final Ref ref : repo.getTags().values()) {
				final RevObject obj = walk.parseAny(ref.getObjectId());
				RevCommit commit = null;
				if (obj instanceof RevCommit) {
					commit = (RevCommit) obj;
				} else if (obj instanceof RevTag) {
					commit = walk.parseCommit(((RevTag) obj).getObject());
				}

				Set<Ref> tags = commitToTagRef.get(commit);
				if (tags == null) {
					tags = new HashSet<Ref>();
					commitToTagRef.put(commit, tags);
				}
				tags.add(ref);
			}
		}
	}

	// mimic git tag --contains <commit>
	private static Set<Ref> getTagsContainingCommit(Repository repo,
			RevCommit commit) throws Exception {
		final Set<Ref> tags = new HashSet<Ref>();
		final RevWalk walk = new RevWalk(repo);
		commit = walk.parseCommit(commit);
		for (final Ref ref : repo.getTags().values()) {
			final RevCommit tagCommit;
			try {
				tagCommit = walk.parseCommit(ref.getObjectId());
			} catch (final IncorrectObjectTypeException notCommit) {
				continue;
			}
			if (walk.isMergedInto(commit, tagCommit)) {
				tags.add(ref);
			}
		}
		return tags;
	}

	public static RevCommit getCommit(Repository repo, String name)
			throws Exception {
		final RevWalk walk = new RevWalk(repo);
		final ObjectId id = repo.resolve(name);
		return walk.parseCommit(id);
	}

	@Override
	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		if ("test".equals(data)) {
			runTest = true;
		}
	}
}
