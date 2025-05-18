package io.serverless_lib;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;
import process_rpc_proto.ProcessRpcProto.AppStarted;
import process_rpc_proto.ProcessRpcProto.FuncCallReq;
import process_rpc_proto.ProcessRpcProto.FuncCallResp;
import process_rpc_proto.ProcessRpcProto.UpdateCheckpoint;
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
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.ProcessHandle;
import com.google.protobuf.Message;

public class UdsBackend
// DisposableBean
{

    Thread netty_thread = null;

    @Autowired
    RpcHandleOwner rpcHandleOwner;

    Channel channel = null;

    String agentSock="";

    String httpPort="";

    String appName="";


    private final ReentrantLock sendlock = new ReentrantLock();
    List<UdsPack> waitingPacks=new ArrayList<>();

    // Add an atomic counter for generating unique task IDs
    private final AtomicInteger taskIdCounter = new AtomicInteger(1);

    // Map to store pending RPC requests
    private final ConcurrentHashMap<Integer, CompletableFuture<Message>> pendingRpcRequests = new ConcurrentHashMap<>();


    @EventListener
    public void bootArgCheckOk(BootArgCheckOkEvent e) {
        this.agentSock = e.agentSock;
        this.httpPort = e.httpPort;
        this.appName = e.appName;
        start();
    }

    public void start(){
        netty_thread = new Thread(() -> {
            UnixChannelHandle.start(Paths.get(agentSock), httpPort, rpcHandleOwner, this);
        });
        netty_thread.start();
    }


    public void send(UdsPack pack){
        sendlock.lock();
        if(channel==null){
            // chennel 读到null后，还没连接{加入队列} else {也可能连接了，if {还没消费掉队列，} else {队列已经消费！！！泄露}}
            // 因此需要锁，保证channel 为null时，消息一定加到队列
            System.out.println("Channel is not ready, packs will be sent later.");
            waitingPacks.add(pack);

            sendlock.unlock();
            return;
        }
        sendlock.unlock();

        System.out.println("Sending pack, packid:"+pack.id+", taskid:"+pack.taskId);
        channel.writeAndFlush(pack.encode());
    }

    public void setUpChannel(Channel channel){
        sendlock.lock();
        this.channel=channel;
        for(UdsPack pack:waitingPacks){
            System.out.println("Sending pended pack, packid:"+pack.id+", taskid:"+pack.taskId);
            send(pack);
        }
        waitingPacks.clear();
        sendlock.unlock();
    }

    public void close(){
        try{

            sendlock.lock();
            channel.close().sync();
            netty_thread.join();
            channel=null;
            sendlock.unlock();
        }catch (Exception e){
            System.out.println("close uds with err");
            e.printStackTrace();
            sendlock.unlock();
        }
    }


    /**
     * Creates a new UdsPack with an automatically generated task ID
     * @param message The message to pack (FuncCallResp, UpdateCheckpoint, KvRequest, or KvResponse)
     * @return A new UdsPack instance with a unique task ID
     */
    public UdsPack newRpcReqUdsPack(Message message, String func) {
        // Generate a new unique task ID atomically
        int newTaskId = taskIdCounter.getAndIncrement();
        UdsPack pack = new UdsPack(message, newTaskId,-1);

        if (pack.id==5){
            KvRequest kvRequest = (KvRequest) pack.pack;
            // Use the protobuf setters according to the message structure
            if (kvRequest.hasGet()){
                KvRequest.KvGetRequest inner=kvRequest.getGet().toBuilder().setAppFn(appName+"/"+func).build();
                pack.pack=kvRequest.toBuilder().setGet(inner).build();
            }else if (kvRequest.hasSet()){
                KvRequest.KvPutRequest inner=kvRequest.getSet().toBuilder().setAppFn(appName+"/"+func).build();
                pack.pack=kvRequest.toBuilder().setSet(inner).build();
            }else if (kvRequest.hasDelete()){
                KvRequest.KvDeleteRequest inner=kvRequest.getDelete().toBuilder().setAppFn(appName+"/"+func).build();
                pack.pack=kvRequest.toBuilder().setDelete(inner).build();
            }else{
                throw new IllegalArgumentException("Invalid kv request without any oneof: "+pack.toString());
            }
        } else {
            throw new IllegalArgumentException("Unsupported rpc pack type: "+pack.toString());
        }

        pack.setRpcCtx(appName, func);

        return pack;
    }

    /**
     * Sends an RPC request and waits for a response
     * @param pack The UdsPack containing the request
     * @param timeout The maximum time to wait
     * @param unit The time unit of the timeout
     * @return The response ByteBuf
     * @throws Exception If an error occurs or the timeout is reached
     */
    public Message sendRpc(UdsPack pack, long timeout, TimeUnit unit) throws Exception {
        CompletableFuture<Message> responseFuture = new CompletableFuture<>();
        
        // Register this request in the pending requests map
        pendingRpcRequests.put(pack.taskId, responseFuture);
        
        // Send the pack
        send(pack);
        
        System.out.println("RPC request sent with taskId: " + pack.taskId);
        
        // Wait for the response with timeout
        try {
            return responseFuture.get(timeout, unit);
        } catch (Exception e) {
            // Make sure to remove the future from the map if there's an error
            pendingRpcRequests.remove(pack.taskId);
            throw e;
        }
    }

    /**
     * Called by the message handler when a response is received
     * @param taskId The task ID from the response
     * @param responseData The response data
     */
    public void handleRpcResponse(int taskId, Message responseData) {
        CompletableFuture<Message> future = pendingRpcRequests.remove(taskId);
        if (future != null) {
            future.complete(responseData);
            System.out.println("RPC response received and processed for taskId: " + taskId);
        } else {
            System.out.println("Received response for unknown taskId: " + taskId);
        }
    }
}

class ByteBufInputStream extends InputStream {
    private final ByteBuf buffer;

    public ByteBufInputStream(ByteBuf buffer) {
        this.buffer = buffer;
    }

    @Override
    public int read() throws IOException {
        if (!buffer.isReadable()) {
            return -1;
        }
        return buffer.readByte() & 0xFF;
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        int readableBytes = buffer.readableBytes();
        if (readableBytes == 0) {
            return -1;
        }
        len = Math.min(len, readableBytes);
        buffer.readBytes(bytes, off, len);
        return len;
    }

    @Override
    public int available() throws IOException {
        return buffer.readableBytes();
    }
}

// class RpcPack {
//     // public ByteBuf packData;
//     public UdsPack pack;

//     public RpcPack(int packId, int taskId, ByteBuf packData) {
//         // this.packData = packData;
//         this.pack = UdsPack.decodeRecv(packId, taskId, packData);
//     }
// }

class UnixChannelHandle {
    static void waitingForSockFile(Path sock_path) {
        System.out.println("Current directory: " + Paths.get(".").toAbsolutePath().toString());
        while (true) {
            if (Files.exists(sock_path)) {
                System.out.println("Socket file exists: " + sock_path);
                break;
            } else {
                System.out.println("Socket file not exists: " + sock_path);
            }
            try {
                // 等待 1 秒钟后再次检查
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // 处理中断异常
                System.err.println("Thread was interrupted while waiting for the socket file.");
                Thread.currentThread().interrupt(); // 重新设置中断状态
                return;
            }
        }
    }

    static void start(Path sock_path, String httpPort, RpcHandleOwner rpcHandleOwner, UdsBackend udsHandle) {
        io.netty.bootstrap.Bootstrap bootstrap = new io.netty.bootstrap.Bootstrap();
        final EpollEventLoopGroup epollEventLoopGroup = new EpollEventLoopGroup();
        String appName=udsHandle.appName;
        try {
            bootstrap.group(epollEventLoopGroup)
                    .channel(EpollDomainSocketChannel.class)
                    .handler(new ChannelInitializer<UnixChannel>() {
                        @Override
                        public void initChannel(UnixChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new ByteToMessageDecoder() {
                                        @Override
                                        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
                                                throws Exception {
                                            // 确保有足够的字节来读取长度字段
                                            if (in.readableBytes() < 4) {
                                                return;
                                            }

                                            // 标记当前的读索引
                                            in.markReaderIndex();

                                            // 读取长度字段
                                            int packId = in.readShort();
                                            int length = in.readInt();
                                            int taskId = in.readInt();
                                            System.out.println("Received message from server: " + length + " bytes, taskId: " + taskId);

                                            // 确保有足够的字节来读取数据
                                            if (in.readableBytes() < length) {
                                                // 重置读索引
                                                in.resetReaderIndex();
                                                return;
                                            }

                                            // 读取数据
                                            ByteBuf frame = in.readBytes(length);
                                            out.add(UdsPack.decodeRecv(packId, taskId, frame));
                                        }
                                    })
                                    .addLast(new SimpleChannelInboundHandler<UdsPack>() {
                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, UdsPack msg)
                                                throws Exception {
                                            System.out.println("Received message from server, id:" + msg.id);
                                            
                                            // // If this is a FuncCallResp (ID=3), it might be a response to our RPC
                                            // if (messageTypeId == 3) { // FuncCallResp ID
                                            //     // Parse the response
                                            //     FuncCallResp funcCallResp = FuncCallResp.parseFrom(stream);
                                            //     String responseData = funcCallResp.getRetStr();
                                                
                                            //     // Notify the waiting future
                                            //     udsHandle.handleRpcResponse(msg.taskId, responseData);
                                            // } 
                                            // If this is a FuncCallReq (ID=1), handle it as a request
                                            if (msg.id==2){ // FuncCallReq ID
                                                FuncCallReq funcCallReq = (FuncCallReq) msg.pack;
                                                
                                                String func = funcCallReq.getFunc();
                                                String argStr = funcCallReq.getArgStr();
                                                
                                                // Process the request
                                                try {
                                                    String resStr = rpcHandleOwner.rpcHandle.handleFuncRpc(funcCallReq.getSrcTaskId(),func, argStr);
                                                    FuncCallResp resp = FuncCallResp.newBuilder().setRetStr(resStr).build();
                                                    ctx.writeAndFlush(new UdsPack(resp, msg.taskId,3).encode());
                                                    System.out.println("Response sent.");
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            // rpc response
                                            else if (msg.isRpcResponse()) {
                                                System.out.println("Received rpc response, id:" + msg.id);
                                                // handle rpc response
                                                udsHandle.handleRpcResponse(msg.taskId, msg.pack);
                                            }
                                            // Other message types can be handled here
                                            else {
                                                System.out.println("unhandled message type, id:" + msg.id);
                                            }
                                        }

                                        @Override
                                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                            System.out.println("Channel is active");

                                            // Create AuthHeader message
                                            AppStarted commu = AppStarted.newBuilder().setAppid(appName)
                                                    .setHttpPort(httpPort).setPid((int)ProcessHandle.current().pid()).build();

                                            // Serialize the message
                                            byte[] data = commu.toByteArray();

                                            System.err.println("data length: " + data.length);
                                            int length = data.length;

                                            // Create a buffer to hold the length and the data
                                            ByteBuf buffer = Unpooled.buffer(4 + length);
                                            buffer.writeInt(length);
                                            buffer.writeBytes(data);

                                            // Send the buffer to the server
                                            ctx.writeAndFlush(buffer);
                                        }

                                        @Override
                                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                            cause.printStackTrace();
                                            ctx.close();
                                        }
                                    });
                        }
                    });
            waitingForSockFile(sock_path);
            // System.out.println("agent's sock is ready");
            Channel channel = bootstrap.connect(new DomainSocketAddress(sock_path.toString())).sync()
                    .channel();
            udsHandle.setUpChannel(channel);
            channel.closeFuture().sync();

            // final FullHttpRequest request = new
            // DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
            // "/services", Unpooled.EMPTY_BUFFER);
            // request.headers().set(HttpHeaderNames.HOST, "daemon");
            // channel.writeAndFlush(request);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            epollEventLoopGroup.shutdownGracefully();
        }
    }
}