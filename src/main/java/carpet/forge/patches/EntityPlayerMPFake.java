package carpet.forge.patches;

import carpet.forge.fakes.IEntityPlayerMP;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketEntityHeadLook;
import net.minecraft.network.play.server.SPacketEntityTeleport;
import net.minecraft.network.play.server.SPacketPlayerListItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.server.management.PlayerList;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;

import javax.annotation.Nonnull;

public class EntityPlayerMPFake extends EntityPlayerMP {

    private static final Container DUMMY_CONTAINER = new Container() {
        @Override
        public void detectAndSendChanges() {
            //NO OP - Performance
        }

        @Override
        public boolean canInteractWith(EntityPlayer playerIn) {
            return true;
        }
    };

    private double lastReportedPosX;
    private double lastReportedPosY;
    private double lastReportedPosZ;

    private double setX;
    private double setY;
    private double setZ;
    private float setYaw;
    private float setPitch;

    public static EntityPlayerMPFake createFake(String username, MinecraftServer server, double x, double y, double z, double yaw, double pitch, int dimension, int gamemode) {
        WorldServer worldIn = server.getWorld(dimension);
        PlayerInteractionManager interactionManagerIn = new PlayerInteractionManager(worldIn);
        GameProfile gameprofile = server.getPlayerProfileCache().getGameProfileForUsername(username);
        gameprofile = fixSkin(gameprofile);
        EntityPlayerMPFake instance = new EntityPlayerMPFake(server, worldIn, gameprofile, interactionManagerIn);
        instance.setSetPosition(x, y, z, (float) yaw, (float) pitch);
        PlayerList playerList = server.getPlayerList();
        NetworkManager manager = new NetworkManagerFake(EnumPacketDirection.CLIENTBOUND);
        manager.channel().attr(NetworkDispatcher.FML_DISPATCHER).getAndSet(new NetworkDispatcher(manager, playerList));
        playerList.initializeConnectionToPlayer(manager, instance, instance.connection);
        if (instance.dimension != dimension) //player was logged in in a different dimension
        {
            WorldServer old_world = server.getWorld(instance.dimension);
            instance.dimension = dimension;
            old_world.removeEntityDangerously(instance);
            instance.isDead = false;
            worldIn.spawnEntity(instance);
            instance.setWorld(worldIn);
            server.getPlayerList().preparePlayer(instance, old_world);
            instance.connection.setPlayerLocation(x, y, z, (float) yaw, (float) pitch);
            instance.interactionManager.setWorld(worldIn);
        }
        instance.setHealth(20.0F);
        instance.isDead = false;
        instance.stepHeight = 0.6F;
        interactionManagerIn.setGameType(GameType.getByID(gamemode));
        server.getPlayerList().sendPacketToAllPlayersInDimension(new SPacketEntityHeadLook(instance, (byte) (instance.rotationYawHead * 256 / 360)), instance.dimension);
        server.getPlayerList().sendPacketToAllPlayersInDimension(new SPacketEntityTeleport(instance), instance.dimension);
        server.getPlayerList().serverUpdateMovingPlayer(instance);
        instance.dataManager.set(PLAYER_MODEL_FLAG, (byte) 0x7f); // show all model layers (incl. capes)
        return instance;
    }

    public static EntityPlayerMPFake createShadow(MinecraftServer server, EntityPlayerMP player) {
        player.getServer().getPlayerList().playerLoggedOut(player);
        player.connection.disconnect(new TextComponentTranslation("multiplayer.disconnect.duplicate_login"));
        WorldServer worldIn = server.getWorld(player.dimension);
        PlayerInteractionManager interactionManagerIn = new PlayerInteractionManager(worldIn);
        GameProfile gameprofile = player.getGameProfile();
        gameprofile = fixSkin(gameprofile);
        EntityPlayerMPFake playerShadow = new EntityPlayerMPFake(server, worldIn, gameprofile, interactionManagerIn);
        playerShadow.setSetPosition(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);
        PlayerList playerList = server.getPlayerList();
        NetworkManager manager = new NetworkManagerFake(EnumPacketDirection.CLIENTBOUND);
        manager.channel().attr(NetworkDispatcher.FML_DISPATCHER).getAndSet(new NetworkDispatcher(manager, playerList));
        playerList.initializeConnectionToPlayer(manager, playerShadow, playerShadow.connection);

        playerShadow.setHealth(player.getHealth());
        playerShadow.connection.setPlayerLocation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);
        interactionManagerIn.setGameType(player.interactionManager.getGameType());
        //noinspection ConstantConditions
        ((IEntityPlayerMP) playerShadow).getActionPack().copyFrom(((IEntityPlayerMP) player).getActionPack());
        playerShadow.stepHeight = 0.6F;

        server.getPlayerList().sendPacketToAllPlayersInDimension(new SPacketEntityHeadLook(playerShadow, (byte) (player.rotationYawHead * 256 / 360)), playerShadow.dimension);
        server.getPlayerList().sendPacketToAllPlayers(new SPacketPlayerListItem(SPacketPlayerListItem.Action.ADD_PLAYER, playerShadow));
        server.getPlayerList().serverUpdateMovingPlayer(playerShadow);
        return playerShadow;
    }

    private EntityPlayerMPFake(MinecraftServer server, WorldServer worldIn, GameProfile profile, PlayerInteractionManager interactionManagerIn) {
        super(server, worldIn, profile, interactionManagerIn);
    }

    private static GameProfile fixSkin(GameProfile gameProfile) {
        if (!gameProfile.getProperties().containsKey("texture"))
            return TileEntitySkull.updateGameProfile(gameProfile);
        else
            return gameProfile;
    }

    @Override
    public void awardKillScore(Entity p_191956_1_, int p_191956_2_, DamageSource p_191956_3_) {
        //NO OP - Performance
    }

    @Override
    public void onKillCommand() {
        logout();
    }

    @Override
    public void onUpdate() {
        this.openContainer = DUMMY_CONTAINER;
        super.onUpdate();
        this.onUpdateEntity();
        this.playerMoved();
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        logout();
    }

    private void logout() {
        this.dismountRidingEntity();
        getServer().getPlayerList().playerLoggedOut(this);
    }

    private void playerMoved() {
        if (posX != lastReportedPosX || posY != lastReportedPosY || posZ != lastReportedPosZ) {
            server.getPlayerList().serverUpdateMovingPlayer(this);
            lastReportedPosX = posX;
            lastReportedPosY = posY;
            lastReportedPosZ = posZ;
        }
    }

    public void setSetPosition(double x, double y, double z, float yaw, float pitch) {
        this.setX = x;
        this.setY = y;
        this.setZ = z;
        this.setYaw = yaw;
        this.setPitch = pitch;
    }

    public void resetToSetPosition() {
        setLocationAndAngles(setX, setY, setZ, setYaw, setPitch);
    }

    @Nonnull
    @Override
    public String getPlayerIP() {
        return "1.3.3.7";
    }
}
