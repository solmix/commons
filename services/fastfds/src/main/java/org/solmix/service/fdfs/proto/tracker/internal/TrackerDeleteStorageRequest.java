package org.solmix.service.fdfs.proto.tracker.internal;

import org.apache.commons.lang3.Validate;
import org.solmix.service.fdfs.proto.CmdConstants;
import org.solmix.service.fdfs.proto.FdfsRequest;
import org.solmix.service.fdfs.proto.OtherConstants;
import org.solmix.service.fdfs.proto.ProtoHead;
import org.solmix.service.fdfs.proto.mapper.FdfsColumn;

/**
 * 移除存储服务器
 * 
 * @author tobato
 *
 */
public class TrackerDeleteStorageRequest extends FdfsRequest {

    /** 组名 */
    @FdfsColumn(index = 0, max = OtherConstants.FDFS_GROUP_NAME_MAX_LEN)
    private String groupName;
    /** 存储ip */
    @FdfsColumn(index = 1, max = OtherConstants.FDFS_IPADDR_SIZE - 1)
    private String storageIpAddr;

    /**
     * 获取文件源服务器
     * 
     * @param groupName
     * @param path
     */
    public TrackerDeleteStorageRequest(String groupName, String storageIpAddr) {
        Validate.notBlank(groupName, "分组不能为空");
        Validate.notBlank(storageIpAddr, "文件路径不能为空");
        this.groupName = groupName;
        this.storageIpAddr = storageIpAddr;
        head = new ProtoHead(CmdConstants.TRACKER_PROTO_CMD_SERVER_DELETE_STORAGE);
    }

    public String getGroupName() {
        return groupName;
    }

    public String getStorageIpAddr() {
        return storageIpAddr;
    }

}
