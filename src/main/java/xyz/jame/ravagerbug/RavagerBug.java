package xyz.jame.ravagerbug;

import java.lang.reflect.*;
import net.minecraft.server.v1_14_R1.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.craftbukkit.v1_14_R1.entity.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.plugin.java.*;
import org.bukkit.scheduler.*;
import org.bukkit.scoreboard.*;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.*;

public class RavagerBug extends JavaPlugin implements Listener
{
    public Ravager testRavager;
    public Llama testLlama;
    public Scoreboard board;
    public Field bz, bA, bB;

    public static String toString( Vector v, String format )
    {
        return String.format( format + ", " + format + ", " + format, v.getX(), v.getY(), v.getZ() );
    }

    @Override
    public boolean onCommand( CommandSender sender, Command command, String label, String[] args )
    {
        if ( label.equalsIgnoreCase( "rav" ) )
        {
            if ( !( sender instanceof Player ) )
                return true;
            Player p = (Player) sender;
            if ( args.length >= 1 && args[0].equalsIgnoreCase( "pos" ) )
            {
                testLlama.teleport( new Location( testRavager.getWorld(), testRavager.getLocation().getX(), p.getLocation().getY(), testRavager.getLocation().getZ() ) );
                return true;
            }
            if ( args.length >= 1 && args[0].equalsIgnoreCase( "apos" ) )
            {
                testLlama.teleport( new Location( testRavager.getWorld(), testRavager.getLocation().getX() + 0.0001f, p.getLocation().getY(), testRavager.getLocation().getZ() + 0.0001f ) );
                return true;
            }
            CraftPlayer cPlayer = (CraftPlayer) p;
            if ( testRavager != null && !testRavager.isDead() )
                testRavager.setHealth( 0.0D );
            if ( testLlama != null && !testLlama.isDead() )
                testLlama.setHealth( 0.0D );
            testRavager = (Ravager) p.getWorld().spawnEntity( p.getLocation(), EntityType.RAVAGER );
            testRavager.setCustomName( "Bob" );
            testLlama = (Llama) p.getWorld().spawnEntity( p.getLocation(), EntityType.LLAMA );
            testLlama.setTamed( true );
            testLlama.setAdult();
            testLlama.setCustomName( "yes" );
            p.sendMessage( "spawned a rav" );
            return true;
        }
        return false;
    }

    public void sendActionbar( Player p, String message )
    {
        IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a( "{\"text\": \"" + message + "\"}" );
        PacketPlayOutChat bar = new PacketPlayOutChat( icbc, ChatMessageType.GAME_INFO );
        ( (CraftPlayer) p ).getHandle().playerConnection.sendPacket( bar );
    }

    @Override
    public void onEnable()
    {
        board = getServer().getScoreboardManager().getNewScoreboard();
        board.registerNewObjective( "ravtest_obj", "dummy", "Ravager Info" ).setDisplaySlot( DisplaySlot.SIDEBAR );
        for ( Player p : getServer().getOnlinePlayers() )
            p.setScoreboard( board );
        try
        {
            bz = EntityRavager.class.getDeclaredField( "bz" );
            bA = EntityRavager.class.getDeclaredField( "bA" );
            bB = EntityRavager.class.getDeclaredField( "bB" );
            bz.setAccessible( true );
            bA.setAccessible( true );
            bB.setAccessible( true );
        }
        catch ( NoSuchFieldException e )
        {
            e.printStackTrace();
        }
        getServer().getPluginManager().registerEvents( this, this );
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if ( ( testRavager == null || testRavager.isDead() ) || ( testLlama == null || testLlama.isDead() ) )
                    return;
                EntityRavager rav = ( (CraftRavager) testRavager ).getHandle();
                EntityLlama llama = ( (CraftLlama) testLlama ).getHandle();
                double d0 = llama.locX - rav.locX;
                double d1 = llama.locZ - rav.locZ;
                double d2 = d0 * d0 + d1 * d1;
                double velX = d0 / d2 * 4.0D;
                double velZ = d1 / d2 * 4.0D;
                for ( Player p : getServer().getOnlinePlayers() )
                    sendActionbar( p, String.format( "X Diff: %.4f | Z Diff: %.4f | Expect %.4fsq, %.4fsq", d0, d1, velX, velZ ) );
                Objective o = board.getObjective( "ravtest_obj" );
                try
                {
                    int bbVal = bB.getInt( rav );
                    o.getScore( "bz (AttackTick)" ).setScore( bz.getInt( rav ) );
                    o.getScore( "bA (StunTick)" ).setScore( bA.getInt( rav ) );
                    o.getScore( "bB (RoarTick)" ).setScore( bbVal );
                    if ( bbVal <= 10 && bbVal > 0 )
                    {
                        getServer().broadcastMessage( "Llama velocity: " + RavagerBug.toString( testLlama.getVelocity(), "%.4f" ) );
                    }
                }
                catch ( IllegalAccessException e )
                {
                    e.printStackTrace();
                }
            }
        }.runTaskTimer( this, 0L, 1L );
    }

    @EventHandler
    public void onDamage( EntityDamageByEntityEvent e )
    {

    }
}
