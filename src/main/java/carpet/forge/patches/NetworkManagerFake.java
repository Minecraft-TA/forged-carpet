package carpet.forge.patches;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class NetworkManagerFake extends NetworkManager
{
    private final Channel channel;

    public NetworkManagerFake(EnumPacketDirection p)
    {
        super(p);
        this.channel = new ChannelFake(null);
    }

    @Override
    public void sendPacket(Packet<?> packetIn) {
    }

    @Override
    public void sendPacket(Packet<?> packetIn, GenericFutureListener<? extends Future<? super Void>> listener, GenericFutureListener<? extends Future<? super Void>>... listeners) {
    }

    @Override
    public void disableAutoRead()
    {
    }

    @Override
    public void handleDisconnection()
    {
    }

    @Override
    public Channel channel()
    {
        return this.channel;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return new InetSocketAddress("localhost", 1337);
    }
}
