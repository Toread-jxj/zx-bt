package com.zx.bt.entity;

import com.zx.bt.config.Config;
import com.zx.bt.exception.BTException;
import com.zx.bt.util.BTUtil;
import com.zx.bt.util.CodeUtil;
import io.netty.util.CharsetUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018-02-14 19:39
 * 一个节点信息
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Node {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * 存储16进制形式的String, byte[20] 转的16进制String,长度固定为40
     */
    private String nodeId;

    private String ip;

    private Integer port;

    /**
     * 最后活动时间(收到请求或收到回复)
     */
    @Transient
    private Date lastActiveTime;

    /**
     * 权重
     */
    @Transient
    private Integer rank;

    /**
     * 与自己的nodeId的异或值
     */
    @Transient
    private byte[] xor;

    /**
     * 检查该节点信息是否完整
     */
    public void check() {
        if(StringUtils.isBlank(nodeId) || nodeId.length() != 40 ||
                StringUtils.isBlank(ip) || port == null || port < 1024 || port > 65535)
            throw new BTException("该节点信息有误:" + this);
    }

    /**
     * List<Node> 转 byte[]
     */
    public static byte[] toBytes(List<Node> nodes) {
        if(CollectionUtils.isEmpty(nodes))
            return new byte[0];
        byte[] result = new byte[nodes.size() * Config.NODE_BYTES_LEN];
        for (int i = 0; i + Config.NODE_BYTES_LEN <= result.length; i+=Config.NODE_BYTES_LEN) {
            System.arraycopy(nodes.get(i/Config.NODE_BYTES_LEN).toBytes(),0,result,i,Config.NODE_BYTES_LEN);
        }
        return result;
    }

    /**
     * Node 转 byte[]
     */
    public byte[] toBytes() {
        check();
        //nodeId
        byte[] nodeBytes = new byte[Config.NODE_BYTES_LEN];
        byte[] nodeIdBytes = CodeUtil.hexStr2Bytes(nodeId);
        System.arraycopy(nodeIdBytes, 0, nodeBytes, 0, 20);

        //ip
        String[] ips = StringUtils.split(ip, ".");
        if(ips.length != 4)
            throw new BTException("该节点IP有误,节点信息:" + this);
        byte[] ipBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            ipBytes[i] = (byte) Integer.parseInt(ips[i]);
        }
        System.arraycopy(ipBytes, 0, nodeBytes, 20, 4);

        //port
        byte[] portBytes = CodeUtil.int2TwoBytes(port);
        System.arraycopy(portBytes, 0, nodeBytes, 24, 2);

        return nodeBytes;
    }

    /**
     * byte[26] 转 Node
     */
    public Node(byte[] bytes) {
        if (bytes.length != Config.NODE_BYTES_LEN)
            throw new BTException("转换为Node需要bytes长度为26,当前为:" + bytes.length);
        //nodeId
        byte[] nodeIdBytes = ArrayUtils.subarray(bytes, 0, 20);
        nodeId = CodeUtil.bytes2HexStr(nodeIdBytes);

        //ip
        byte[] ipBytes = ArrayUtils.subarray(bytes, 20, 24);
        ip = String.join(".", Integer.toString(ipBytes[0] & 0xFF), Integer.toString(ipBytes[1] & 0xFF)
                , Integer.toString(ipBytes[2] & 0xFF), Integer.toString(ipBytes[3] & 0xFF));

        //port
        byte[] portBytes = ArrayUtils.subarray(bytes, 24, Config.NODE_BYTES_LEN);
        port = portBytes[1] & 0xFF | (portBytes[0] & 0xFF) << 8;
    }

    public Node(String nodeId, String ip, Integer port) {
        this.nodeId = nodeId;
        this.ip = ip;
        this.port = port;
    }
}