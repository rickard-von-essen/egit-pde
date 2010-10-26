/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.GitTag;

/**
 * This class provides access to information stored in RelEng map files
 */
public class MapEntry {
	public static final GitTag DEFAULT = new GitTag("master");

	private static final String HEAD = "HEAD";

	private static final String KEY_TAG = "tag"; //$NON-NLS-1$

	private static final String KEY_PATH = "path"; //$NON-NLS-1$

	private static final String REPO = "repo";

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private boolean valid = false;

	private String type = EMPTY_STRING;

	private String id = EMPTY_STRING;

	private OrderedMap arguments = new OrderedMap();

	private boolean legacy = false;

	private String version;

	public static void main(String[] args) {
		// For testing only

		final String[] strings = {
				"",
				" ",
				"type",
				"type@",
				"type@id",
				"type@id=",
				"type@id=tag,",
				"type@id=tag, connectString",
				"type@id=tag, connectString,",
				"type@id=tag, connectString,password",
				"type@id=tag, connectString,password,",
				"type@id=tag, connectString,password,moduleName",
				"type@id=tag, connectString,,moduleName",
				"!***************  FEATURE CONTRIBUTION  ******************************************************",
				"@",
				"=",
				",,,",
				"@=,,,,",
				"type@id,version=CVS,tag=myTag,cvsRoot=myCvsRoot,password=password,path=myPath", };

		for (int i = 0; i < strings.length; i++) {
			final String string = strings[i];
			final MapEntry anEntry = new MapEntry(string);

			System.out
					.println("-----------------------------------------------");
			System.out.println("input: " + string);
			System.out.println("map string: " + anEntry.getMapString());
			// System.out.println(anEntry.getReferenceString());
			anEntry.display();
		}

	}

	private void display() {
		// For testing only
		System.out.println("Is Valid: " + isValid());
		System.out.println("Type: " + getType());
		System.out.println("Project Name: " + getId());
		System.out.println("Tag: " + getTagName());
		if (version != null)
			System.out.println("Version: " + version);
		System.out.println("Connect: " + getRepo());
		System.out.println("Path: " + getPath());
	}

	public MapEntry(String entryLine) {
		init(entryLine);
	}

	/**
	 * Parse a map file entry line
	 * 
	 * @param entryLine
	 */
	private void init(String entryLine) {
		valid = false;

		// check for commented out entry
		if (entryLine.startsWith("#") || entryLine.startsWith("!"))
			return;

		// Type
		int start = 0;
		int end = entryLine.indexOf('@');
		if (end == -1)
			return;
		type = entryLine.substring(start, end).trim();

		// Project Name
		start = end + 1;
		end = entryLine.indexOf('=', start);
		if (end == -1)
			return;
		id = entryLine.substring(start, end).trim();
		// we have a version that we have to strip off
		final int comma = id.indexOf(',');
		if (comma != -1) {
			version = id.substring(comma + 1);
			id = id.substring(0, comma);
		}

		final String[] args = getArrayFromStringWithBlank(
				entryLine.substring(end + 1), ",");
		this.arguments = populate(args);
		final String tag = (String) arguments.get(KEY_TAG);
		final String repo = (String) arguments.get(REPO);
		if (tag == null || tag.length() == 0 || repo == null
				|| repo.length() == 0)
			return;
		valid = true;
	}

	/*
	 * Build a table from the given array. In the new format,the array contains
	 * key=value elements. Otherwise we fill in the key based on the old format.
	 */
	private OrderedMap populate(String[] entries) {
		final OrderedMap result = new OrderedMap();
		for (int i = 0; i < entries.length; i++) {
			final String entry = entries[i];
			final int index = entry.indexOf('=');
			if (index == -1) {
				// we only handle CVS entries
				if (i == 0 && "GIT".equalsIgnoreCase(entry))
					continue;
				// legacy story...
				return legacyPopulate(entries);
			}
			final String key = entry.substring(0, index);
			final String value = entry.substring(index + 1);
			result.put(key, value);
		}
		result.toString();
		return result;
	}

	private OrderedMap legacyPopulate(String[] entries) {
		legacy = true;
		final OrderedMap result = new OrderedMap();
		// must have at least tag and connect string
		if (entries.length >= 2) {
			// Version
			result.put(KEY_TAG, entries[0]);
			// Repo Connect String
			result.put(REPO, entries[1]);

			// Optional CVS Module Name
			if (entries.length >= 3)
				result.put(KEY_PATH, entries[3]);
		}
		return result;
	}

	/**
	 * Convert a list of tokens into an array. The list separator has to be
	 * specified. The specificity of this method is that it returns an empty
	 * element when to same separators are following each others. For example
	 * the string a,,b returns the following array [a, ,b]
	 * 
	 */
	public static String[] getArrayFromStringWithBlank(String list,
			String separator) {
		if (list == null || list.trim().length() == 0)
			return new String[0];
		final List result = new ArrayList();
		boolean previousWasSeparator = true;
		for (final StringTokenizer tokens = new StringTokenizer(list,
				separator, true); tokens.hasMoreTokens();) {
			final String token = tokens.nextToken().trim();
			if (token.equals(separator)) {
				if (previousWasSeparator)
					result.add(""); //$NON-NLS-1$
				previousWasSeparator = true;
			} else {
				result.add(token);
				previousWasSeparator = false;
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	public String getTagName() {
		final String value = (String) arguments.get(KEY_TAG);
		return value == null || HEAD.equals(value) ? EMPTY_STRING : value;
	}

	public GitTag getTag() {
		if (getTagName().equals(HEAD) || getTagName().equals(""))
			return DEFAULT;
		return new GitTag(getTagName());
	}

	public String getId() {
		return id;
	}

	private String internalGetCVSModule() {
		final String module = (String) arguments.get(KEY_PATH);
		return module == null ? id : module;
	}

	public String getPath() {
		final String value = (String) arguments.get(KEY_PATH);
		return value == null ? EMPTY_STRING : value;
	}

	public String getRepo() {
		final String value = (String) arguments.get(REPO);
		return value == null ? EMPTY_STRING : value;
	}

	public String getType() {
		return type;
	}

	public boolean isValid() {
		return valid;
	}

	public String getReferenceString() {
		if (!isValid())
			return null;
		// This is the format used by the CVS IProjectSerializer
		final String projectName = new Path(internalGetCVSModule())
				.lastSegment();
		return "1.0," + getRepo() + "," + internalGetCVSModule() + ","
				+ projectName + "," + getTagName();
	}

	public String getMapString() {
		final StringBuffer result = new StringBuffer();
		if (legacy) {
			result.append(getType());
			result.append('@');
			result.append(getId());
			if (version != null) {
				result.append(',');
				result.append(version);
			}
			result.append('=');
			result.append(getTagName());
			result.append(',');
			result.append(getRepo());
			result.append(',');
			result.append(getPath());
			return result.toString();
		}
		result.append(getType());
		result.append('@');
		result.append(getId());
		if (version != null) {
			result.append(',');
			result.append(version);
		}
		result.append('=');
		result.append("GIT");
		for (final Iterator iter = arguments.keys().iterator(); iter.hasNext();) {
			final String key = (String) iter.next();
			final String value = (String) arguments.get(key);
			if (value != null && value.length() > 0)
				result.append(',' + key + '=' + value);
		}
		return result.toString();
	}

	/*
	 * Return the version specified for this entry. Can be null.
	 */
	public String getVersion() {
		return version;
	}

	public void setId(String projectID) {
		this.id = projectID;
	}

	public void setCVSModule(String path) {
		arguments.put(KEY_PATH, path);
	}

	public void setRepo(String repo) {
		arguments.put(REPO, repo);
	}

	public void setTagName(String tagName) {
		arguments.put(KEY_TAG, tagName);
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	@Override
	public String toString() {
		return "Entry: " + getMapString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MapEntry) {
			return ((MapEntry) obj).getMapString().equals(getMapString());
		}
		return super.equals(obj);
	}

	/**
	 * Return <code>true</code> if the entry is mapped to the given project and
	 * <code>false</code> otherwise.
	 */
	public boolean isMappedTo(IProject project) {
		// RepositoryProvider provider =
		// RepositoryProvider.getProvider(project);
		if (id.equals(project.getName())) {
			return true;
		}
		return false;
	}

}
