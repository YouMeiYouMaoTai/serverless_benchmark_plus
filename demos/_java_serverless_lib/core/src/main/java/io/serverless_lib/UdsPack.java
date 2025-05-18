package io.serverless_lib;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.annotation.PostConstruct;
import process_rpc_proto.ProcessRpcProto.AppStarted;
import process_rpc_proto.ProcessRpcProto.FuncCallReq;
import process_rpc_proto.ProcessRpcProto.UpdateCheckpoint;
import process_rpc_proto.ProcessRpcProto.FuncCallResp;
import process_rpc_proto.ProcessRpcProto.KvRequest;
import process_rpc_proto.ProcessRpcProto.KvResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.channel.unix.UnixChannel;
import io.netty.buffer.Unpooled;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import com.google.protobuf.Message;
import java.nio.ByteBuffer;

public class UdsPack{
    public Message pack;
    public int id;
    public int taskId;
    private String app;
    private String func;

    
    public UdsPack(Message inner,int taskId, int idOpt){
        pack=inner;
        this.taskId=taskId;

        if(idOpt>=0){
            id=idOpt;
            return;
        }

        if (inner instanceof FuncCallReq){
            id=2;
        }else if(inner instanceof FuncCallResp){
            id=3;
        }else if(inner instanceof UpdateCheckpoint){
            id=4; 
        }else if(inner instanceof KvRequest){
            id=5;
        }else if(inner instanceof KvResponse){
            id=6;
        }else{
            throw new IllegalArgumentException("Unknown pack type: " + inner.getClass().getName());
        }
    }

    public static UdsPack decodeRecv(int packId, int taskId, ByteBuf buffer_){
        ByteBuffer buffer = buffer_.nioBuffer();
        try{
            switch(packId){
                case 2:
                    return new UdsPack(FuncCallReq.parseFrom(buffer), taskId,2);
                case 6:
                    return new UdsPack(KvResponse.parseFrom(buffer), taskId,6);
                default:
                    throw new IllegalArgumentException("Unsupported recv pack type: " + packId);
            }
        }catch(Exception e){
            e.printStackTrace();
            throw new IllegalArgumentException("Failed to decode recv pack: " + e.getMessage());
        }
    }


    
    byte[] staticEncode(){
        switch(id){
            case 3:
                return ((FuncCallResp)pack).toByteArray();
            case 4:
                return ((UpdateCheckpoint)pack).toByteArray();
            case 5:
                return ((KvRequest)pack).toByteArray();
            case 6:
                return ((KvResponse)pack).toByteArray();
            default:
                throw new IllegalArgumentException("Unknown pack type");
        }
    }

    public ByteBuf encode(){
        byte[] data = staticEncode();
        ByteBuf buffer = Unpooled.buffer(9 + data.length);
        buffer.writeInt(data.length); // length
        byte[] msgId = {(byte)id}; 
        buffer.writeBytes(msgId);// pack type
        buffer.writeInt(taskId);// dummy task id
        buffer.writeBytes(data);// data

        return buffer;
    }

    /**
     * Determines if a message ID represents an RPC response
     * @param messageId The message type ID to check
     * @return true if the message is an RPC response, false otherwise
     */
    public boolean isRpcResponse() {
        // kv response
        return this.id == 6;
    }

    public void setRpcCtx(String app, String func){
        this.app=app;
        this.func=func;
    }
}