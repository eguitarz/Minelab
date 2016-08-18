package minelab.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import minelab.model.Dungeon;
import minelab.model.WidePathDungeon;


public class Main extends JavaPlugin implements Listener {
	private static World world = null;
	public static int CHUNK_WIDTH = 16;
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase("dungeon")){
			if (sender instanceof Player) {
	            Player player = (Player)sender;
	            player.teleport(getWorld().getSpawnLocation());
	        } else {
	            sender.sendMessage("I don't know who you are!");
	        }
			return true;
		}
		
		if(cmd.getName().equalsIgnoreCase("regenerate")){
			ChunkGenerator generator = world.getGenerator();
			if (generator instanceof DungeonChunkGenerator) {
				Dungeon dungeon = new WidePathDungeon(55, 55);
				((DungeonChunkGenerator) generator).setDungeon(dungeon.generate());
			}
			regenerateLoadedChunks(world);
			return true;
		}
		
		return false;
	}
	
	public static Dungeon getDungeon() {
		return (new WidePathDungeon(65, 65)).generate();
	}
	
	public static World getWorld() {
        if (world == null) {
        	WorldCreator wc = new WorldCreator("dungeon");
        	wc.generator(new DungeonChunkGenerator(getDungeon()));
        	world = Bukkit.getServer().createWorld(wc);
        }

        return world;
    }
	
	public void onEnable()
    {
		getLogger().info("[MinelabPlugin] enabled");  
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
    }

    public void onDisable()
    {
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id)
    {
        return new DungeonChunkGenerator(getDungeon());
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
    	Player player = event.getPlayer();
    	World world = getWorld();
    	Location spawn = world.getSpawnLocation();
    	
    	// regenerate
    	regenerateLoadedChunks(world);
    	
    	getLogger().info("teleporting " + player.getDisplayName());
    	player.teleport(spawn);
    }
    
    private void regenerateLoadedChunks(World world) {
    	Chunk[] chunks = world.getLoadedChunks();
    	for(int i=0; i<chunks.length; i++) {
    		Chunk chunk = chunks[i];
    		world.regenerateChunk(chunk.getX(), chunk.getZ());
    	}
    }
}
