package org.solmix.service.versioncontrol;

import org.eclipse.jgit.diff.DiffEntry.ChangeType;

public enum ChangedType {
	/** Add a new file to the project */
	ADD,

	/** Modify an existing file in the project (content and/or mode) */
	MODIFY,

	/** Delete an existing file from the project */
	DELETE,

	/** Rename an existing file to a new location */
	RENAME,

	/** Copy an existing file to a new location, keeping the original */
	COPY,
	
	/** Register basline for initialization **/
	REGISTER;
	
	public static ChangedType fromChangeType(final ChangeType ct, boolean isBaseline){
		switch (ct) {
		case ADD:
			if (isBaseline)
				return ChangedType.REGISTER;
			return ChangedType.ADD;
		case MODIFY:
			return ChangedType.MODIFY;
		case DELETE:
			return ChangedType.DELETE;
		case RENAME:
			return ChangedType.RENAME;
		case COPY:			
			return ChangedType.COPY;
		default:
			return null;
		}
	}
}
