package org.solmix.service.fdfs.proto.tracker.internal;

import org.apache.commons.lang3.Validate;
import org.solmix.service.fdfs.proto.CmdConstants;
import org.solmix.service.fdfs.proto.FdfsRequest;
import org.solmix.service.fdfs.proto.OtherConstants;
import org.solmix.service.fdfs.proto.ProtoHead;
import org.solmix.service.fdfs.proto.mapper.FdfsColumn;

/**
 * 按分组获取存储节点
 * 
 * 
 *
 */
public class TrackerGetStoreStorageWithGroupRequest extends FdfsRequest {

    private static final byte withGroupCmd = CmdConstants.TRACKER_PROTO_CMD_SERVICE_QUERY_STORE_WITH_GROUP_ONE;

    /**
     * 分组定义
     */
    @FdfsColumn(index = 0, max = OtherConstants.FDFS_GROUP_NAME_MAX_LEN)
    private final String groupName;

    /**
     * 获取存储节点
     * 
     * @param groupName
     */
    public TrackerGetStoreStorageWithGroupRequest(String groupName) {
        Validate.notBlank(groupName, "分组不能为空");
        this.groupName = groupName;
        this.head = new ProtoHead(withGroupCmd);
    }

    public String getGroupName() {
        return groupName;
    }

}
