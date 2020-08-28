package com.yad.rpc.handler;

import com.yad.rpc.protocol.YBody;
import com.yad.rpc.protocol.YHeader;
import com.yad.rpc.protocol.YPack;
import com.yad.rpc.util.SerializeUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.List;

public class YDecoder  extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        while (byteBuf.readableBytes()>=98){
            byte[] bytes = new byte[98];
            byteBuf.getBytes(byteBuf.readerIndex(),bytes);//这个不会移动读指针
            //反序列化
            ByteArrayInputStream bain = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bain);
            YHeader header  = (YHeader) ois.readObject();

            if (byteBuf.readableBytes()>= header.getDataLen()+98){
                byteBuf.readBytes(98);//移动读指针
                byte[] data = new byte[(int) header.getDataLen()];
                byteBuf.readBytes(data);
                ByteArrayInputStream ins  = new ByteArrayInputStream(data);
                ois = new ObjectInputStream(ins);
                YBody body = (YBody) ois.readObject();
                //根据协议头 flag 来区分协议意义
                if (header.getFlag()==0x14141414){
                    YPack pack = new YPack(header,body);
                    list.add(pack);
                }else if (header.getFlag()==0x14141412){
                    YPack pack = new YPack(header,body);
                    list.add(pack);
                }
            }else {
                break;//说明不够长度不是一个完整的包和下个包拼接起来重新读取
            }
        }
    }
}
