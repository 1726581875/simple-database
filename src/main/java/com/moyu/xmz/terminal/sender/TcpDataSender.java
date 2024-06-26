package com.moyu.xmz.terminal.sender;

import com.moyu.xmz.net.constant.CmdTypeConstant;
import com.moyu.xmz.net.model.BaseResultDto;
import com.moyu.xmz.net.model.terminal.DatabaseInfo;
import com.moyu.xmz.net.model.terminal.QueryResultDto;
import com.moyu.xmz.net.packet.ErrPacket;
import com.moyu.xmz.net.packet.OkPacket;
import com.moyu.xmz.net.packet.Packet;
import com.moyu.xmz.net.util.ReadWriteUtil;
import com.moyu.xmz.common.exception.DbException;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * @author xiaomingzhang
 * @date 2023/7/6
 */
public class TcpDataSender {

    private String ipAddress;

    private Integer port;


    public TcpDataSender(String ipAddress, Integer port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public DatabaseInfo getDatabaseInfo(String databaseName) {
        // 创建Socket对象，并指定服务端IP地址和端口号
        try (Socket socket = new Socket(ipAddress, port);
             // 获取输入流和输出流
             InputStream inputStream = socket.getInputStream();
             OutputStream outputStream = socket.getOutputStream();
             DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
             DataInputStream dataInputStream = new DataInputStream(inputStream)) {
            // 命令类型
            dataOutputStream.writeByte(CmdTypeConstant.DB_INFO);
            // 数据库名称
            ReadWriteUtil.writeString(dataOutputStream, databaseName);
            // 获取结果
            Packet packet = readPacket(dataInputStream);
            if (packet.getPacketType() == Packet.PACKET_TYPE_OK) {
                OkPacket okPacket = (OkPacket) packet;
                return (DatabaseInfo) okPacket.getContent();
            } else if (packet.getPacketType() == Packet.PACKET_TYPE_ERR) {
                ErrPacket errPacket = (ErrPacket) packet;
                System.out.println("获取数据库信息失败, 错误码: " + errPacket.getErrCode() + "，错误信息: " + errPacket.getErrMsg());
            } else {
                System.out.println("不支持的packet type" + packet.getPacketType());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new DbException("获取数据库信息失败");
    }

    public QueryResultDto execQueryCommand(Integer databaseId, String sql) {
        return execQueryCommand(databaseId, sql, CmdTypeConstant.DB_QUERY);
    }


    public QueryResultDto execQueryCommand(Integer databaseId, String sql, int commandType) {
        try (Socket socket = new Socket(ipAddress, port);
             // 获取输入流和输出流
             InputStream inputStream = socket.getInputStream();
             OutputStream outputStream = socket.getOutputStream();
             DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
             DataInputStream dataInputStream = new DataInputStream(inputStream)) {
            // 命令类型
            dataOutputStream.writeByte(commandType);
            // 数据库id
            dataOutputStream.writeInt(databaseId);
            // SQL
            ReadWriteUtil.writeString(dataOutputStream, sql);
            // 获取结果
            Packet packet = readPacket(dataInputStream);
            if (packet.getPacketType() == Packet.PACKET_TYPE_OK) {
                OkPacket okPacket = (OkPacket) packet;
                System.out.println("sql执行成功，结果:");
                BaseResultDto content = okPacket.getContent();
                return (QueryResultDto) content;
            } else if (packet.getPacketType() == Packet.PACKET_TYPE_ERR) {
                ErrPacket errPacket = (ErrPacket) packet;
                System.out.println("sql执行失败,错误码: " + errPacket.getErrCode() + "，错误信息: " + errPacket.getErrMsg());
            } else {
                System.out.println("不支持的packet type" + packet.getPacketType());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



    public static Packet readPacket(DataInputStream dataInputStream) throws IOException {

        Packet packet = null;

        int packetLen = dataInputStream.readInt();
        byte packetType = dataInputStream.readByte();

        ByteBuffer byteBuffer = ByteBuffer.allocate(packetLen);
        byte[] bytes = new byte[1024];
        while (byteBuffer.remaining() > 0) {
            int read = dataInputStream.read(bytes);
            if (read == -1) {
                throw new EOFException();
            }
            byteBuffer.put(bytes, 0, read);
        }
        byteBuffer.flip();

        if (packetType == Packet.PACKET_TYPE_OK) {
            packet = new OkPacket(byteBuffer);
        } else if (packetType == Packet.PACKET_TYPE_ERR) {
            packet = new ErrPacket(byteBuffer);
        } else {
            throw new DbException("不支持的packet type " + packetType);
        }


        return packet;
    }


}
