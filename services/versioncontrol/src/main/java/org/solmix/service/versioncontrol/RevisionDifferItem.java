package org.solmix.service.versioncontrol;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;

public class RevisionDifferItem
{
	private Date commitTime;
	private String path;
	private String id;
	/** version id of the previous version of the file **/
	private String prevVersionId;
	private String revisionId;
	private ChangedType changeType;
	private String comment;
	/** file mode bits of the previous version of the file **/
	private Integer fileModeBits;
	private Integer prevVersionFileModeBits;
//    private static Logger log = LoggerFactory.getLogger(RevisionDiffItem.class);
	
	public final static String DATE_FORMAT = "MM/dd/yyyy hh:mm:ss";
	
	public RevisionDifferItem() {
	}

	public RevisionDifferItem(FileDiffer fileDiff){
/*		final String commitMsg = fileDiff.getCommit().getFullMessage();
		if (log.isDebugEnabled())
			log.debug("---BENCHMARK--- RevisionDiffItem after get message");
		final String[] parsedMsg = commitMsg.split(";");
		if (parsedMsg!=null && parsedMsg.length > 0){
			try{
				final DateFormat df = new SimpleDateFormat(DATE_FORMAT);
				commitTime = df.parse(parsedMsg[0]);
			} catch (Exception e) {
				//e.printStackTrace();
			}
		} 
*/
//		if (commitTime == null){
			final Calendar c = Calendar.getInstance();
			c.setTimeInMillis(fileDiff.getCommit().getCommitTime());
			commitTime = c.getTime();
//		}
		path = fileDiff.getPath();
		final ObjectId[] objIds = fileDiff.getBlobs();
		if (objIds != null && objIds.length > 0)
			id = ObjectId.toString(fileDiff.getBlobs()[objIds.length-1]);//   
		if (objIds != null && objIds.length > 1)
			prevVersionId = ObjectId.toString(fileDiff.getBlobs()[0]);//   
		revisionId = ObjectId.toString(fileDiff.getCommit().getId());
/*		boolean isBaseline = commitMsg != null && commitMsg.contains("is_baseline");
*/
		changeType = ChangedType.fromChangeType(fileDiff.getChange(), false);

/*
		setComment(commitMsg);
*/		final FileMode[] fileModes = fileDiff.getModes();
		if (fileModes != null && fileModes.length > 0)		
			fileModeBits = fileDiff.getModes()[fileModes.length-1].getBits();
		if (fileModes != null && fileModes.length > 1)		
			prevVersionFileModeBits = fileDiff.getModes()[0].getBits();
	}
	
	public Date getCommitTime() {
		return commitTime;
	}
	public void setCommitTime(Date date) {
		this.commitTime = date;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public ChangedType getChangeType() {
		return changeType;
	}
	public void setChangeType(ChangedType changeType) {
		this.changeType = changeType;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getComment() {
		return comment;
	}

	public void setRevisionId(String revisionId) {
		this.revisionId = revisionId;
	}

	public String getRevisionId() {
		return revisionId;
	}

	public void setFileModeBits(Integer fileModeBits) {
		this.fileModeBits = fileModeBits;
	}

	public Integer getFileModeBits() {
		return fileModeBits;
	}

	public String getPrevVersionId() {
		return prevVersionId;
	}

	public void setPrevVersionId(String prevVersionId) {
		this.prevVersionId = prevVersionId;
	}

	public Integer getPrevVersionFileModeBits() {
		return prevVersionFileModeBits;
	}

	public void setPrevVersionFileModeBits(Integer prevFileModeBits) {
		this.prevVersionFileModeBits = prevFileModeBits;
	}
}
