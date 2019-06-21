package org.solmix.service.versioncontrol;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jgit.revwalk.RevCommit;

public class Revision
{
	private RevCommit revCommit;

	private Collection<FileDiffer> diffs;

	public Revision(RevCommit c) {
		this.revCommit = c;
	}

	public RevCommit getRevCommit() {
		return revCommit;
	}

	public void setRevCommit(RevCommit revCommit) {
		this.revCommit = revCommit;
	}

	public Collection<FileDiffer> getDiffs() {
		return diffs;
	}

	public void setDiffs(Collection<FileDiffer> diffs) {
		this.diffs = diffs;
	}

	public void addDiff(FileDiffer diff) {
		if (diffs == null)
			diffs = new ArrayList<FileDiffer>();
		diffs.add(diff);
	}
}
