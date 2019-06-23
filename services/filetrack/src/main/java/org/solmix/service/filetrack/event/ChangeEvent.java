package org.solmix.service.filetrack.event;


public class ChangeEvent
{
	/** Event type */
	private EventActionsEnum type;
	
	/** Full path to changed file/directory */
	private String fullPath;
	
	/** Old path to renamed file/directory */
	private String oldFullPath;
	
	/** True message is baseline */
	private boolean isBaseline;
	
	/** Buffer */
	private byte[] fileBuf;
	
	/** Depth of directory tree*/
	private int treeDepth;
	
	/** Revision id in version control **/
	private String revisionId;

	/** Old revision id in version control - incase of rename **/
	private String oldRevisionId;

	/** Diff of change. Populated for modify only **/
	private String diff;
	
	private Long timestamp;
	
	/**
	 * Creates a new event message
	 * @param type, Type of change event
	 * @param fullPath, Full path to changed file/directory
	 */
	public ChangeEvent(EventActionsEnum type, String fullPath, Long timestamp){
		this(type, fullPath, false, timestamp);
	}
	
	/**
	 * Creates a new event message
	 * @param type, Type of change event
	 * @param fullPath, Full path to changed file/directory
	 * @param isBaseline, Is file sent as baseline
	 */
	public ChangeEvent(EventActionsEnum type, String fullPath, boolean isBaseline, Long timestamp) {
		this(type, fullPath, null, timestamp);
		this.isBaseline = isBaseline;
	}
	
	/**
	 * Creates a new event message
	 * @param type, Type of change event
	 * @param fullPath, Full path to changed file/directory
	 * @param oldFullPath, Old path to renamed file/directory
	 */
	public ChangeEvent(EventActionsEnum type, String fullPath, String oldFullPath, Long timestamp) {
        this.type = type;
        this.fullPath = fullPath;
        this.oldFullPath = oldFullPath;
        this.isBaseline = false;
        this.fileBuf = null;
        this.treeDepth = -1;
        this.timestamp = timestamp;
	}
	
	/**
	 * Creates a new event message
	 * @param type, Type of change event
	 * @param fullPath, Full path to changed file/directory
	 * @param buf, File buffer
	 */
	public ChangeEvent(EventActionsEnum type, String fullPath, Long timestamp, byte[] buf){
		this(type, fullPath, timestamp);
		this.fileBuf = buf;
	}
	
	/**
	 * Creates a new event message
	 * @param type, Type of change event
	 * @param fullPath, Full path to changed file/directory
	 * @param treeDepth, Subtree depth. The value 0 is reserved to indicate all subdirectories are to be sent.
	 */
	public ChangeEvent(EventActionsEnum type, String fullPath, Long timestamp, int treeDepth) {
		this(type, fullPath, timestamp);
        this.treeDepth = treeDepth;
	}
	
    public ChangeEvent(ChangeEvent event) {
		this(event.type, event.fullPath, event.oldFullPath, event.timestamp);
	}

	public EventActionsEnum getType() {
		return type;
	}

	public void setType(EventActionsEnum type) {
		this.type = type;
	}

	public String getFullPath() {
		return fullPath;
	}

	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}

	public String getOldFullPath() {
		return oldFullPath;
	}

	public void setOldFullPath(String oldFullPath) {
		this.oldFullPath = oldFullPath;
	}

	public boolean isBaseline() {
		return isBaseline;
	}

	public void setBaseline(boolean isBaseline) {
		this.isBaseline = isBaseline;
	}

	public byte[] getFileBuf() {
		return fileBuf;
	}

	public void setFileBuf(byte[] fileBuf) {
		this.fileBuf = fileBuf;
	}

	public int getTreeDepth() {
		return treeDepth;
	}

	public void setTreeDepth(int treeDepth) {
		this.treeDepth = treeDepth;
	}

	public String getRevisionId() {
		return revisionId;
	}

	public void setRevisionId(String versionControlRevisionId) {
		this.revisionId = versionControlRevisionId;
	}

	public String getOldRevisionId() {
		return oldRevisionId;
	}

	public void setOldRevisionId(String oldRevisionId) {
		this.oldRevisionId = oldRevisionId;
	}

	public String getDiff() {
		return diff;
	}

	public void setDiff(String diff) {
		this.diff = diff;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	
	public boolean equals(ChangeEvent other) {
		if (null == this.oldFullPath )
	        return (this.type.equals(other.type) &&
	                this.fullPath.equals(other.fullPath));
		else
			
	        return (null == other.oldFullPath) ? false 
	                						: (this.type.equals(other.type) &&
		                							 this.fullPath.equals(other.fullPath) &&
		                							 this.oldFullPath.equals(other.oldFullPath));

	}
}
