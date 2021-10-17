package carpet.forge.patches;

import io.netty.channel.Channel;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;

public class NetworkManagerFake extends NetworkManager
{
    private final Channel channel;
    
    public NetworkManagerFake(EnumPacketDirection p)
    {
        super(p);
        this.channel = new ChannelFake(null);
    }

    @Override
    public void sendPacket(Packet<?> packetIn) 
    {
    }

    @Override
    public void sendPacket(Packet<?> packetIn, GenericFutureListener<? extends Future<? super Void>> listener, GenericFutureListener<? extends Future<? super Void>>... listeners) 
    {
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
}
