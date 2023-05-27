package net.cavoj.servertick.paper;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class ServerTick extends JavaPlugin {

    private final Set<Player> listeners = new HashSet<>();
    private final Metrics metrics = new Metrics();

    @Override
    public void onEnable() {
        getLogger().info("ServerTick is starting up!");

        getServer().getMessenger().registerIncomingPluginChannel(this, Channels.PACKET_ENABLED, this::handleEnablePacket);

        getServer().getMessenger().registerOutgoingPluginChannel(this, Channels.PACKET_FULL_METRICS);
        getServer().getMessenger().registerOutgoingPluginChannel(this, Channels.PACKET_LAST_SAMPLE);

        getServer().getPluginManager().registerEvents(new DisconnectListener(this), this);

        (new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }).runTaskTimer(this, 0, 0);
    }

    private void handleEnablePacket(String channel, Player player, byte[] message) {
        boolean enabled = message[0] == 1;
        if (enabled) {
            // if (player.hasPermission("servertick.nometrics")) return; // i might as well to implement this later
            listeners.add(player);
            sendFullMetrics(player);
        } else {
            listeners.remove(player);
        }
    }

    public void tick() {
        this.metrics.update(getServer());
        long sample = this.metrics.getLastSample();
        ByteBuffer bb = ByteBuffer.allocate(Long.BYTES);
        bb.putLong(sample);
        for (Player player : listeners) {
            player.sendPluginMessage(this, Channels.PACKET_LAST_SAMPLE, bb.array());
        }
    }

    private void sendFullMetrics(Player player) {
        player.sendPluginMessage(this, Channels.PACKET_FULL_METRICS, metrics.serialize());
    }

    @Override
    public void onDisable() {

    }

    public void onDisconnected(Player player) {
        this.listeners.remove(player);
    }

}
