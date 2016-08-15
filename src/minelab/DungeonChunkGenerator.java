package minelab;

import java.awt.Rectangle;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.material.Door;

import model.Cell;
import model.Dungeon;

public class DungeonChunkGenerator extends ChunkGenerator {
	
	public static final int MAX_SPACE_HEIGHT = 256;
	public static final int CHUNK_WIDTH = 16;
	public boolean hasGeneratedSpawnChunk = false;
	
	private Dungeon dungeon;
	
	public DungeonChunkGenerator(Dungeon dungeon) {
		this.dungeon = dungeon;
	}
	
	@Override
	public ChunkData generateChunkData(World world, Random random, int cx, int cz, BiomeGrid biome) {
		ChunkData chunk = createChunkData(world);
		
		// spawn chunk
		Location spawn = getFixedSpawnLocation(world, random);
		int chunkX = (int) (spawn.getX() / CHUNK_WIDTH);
		int chunkZ = (int) (spawn.getZ() / CHUNK_WIDTH);
		
		// floor
		for (int x=0; x<16; x++ ) {
			for (int z=0; z<16; z++) {
				chunk.setBlock(x, 0, z, Material.BEDROCK);
				
				if (random.nextDouble() < 0.85) {
					chunk.setBlock(x, 1, z, Material.COBBLESTONE);
					chunk.setBlock(x, 2, z, Material.COBBLESTONE);
					chunk.setBlock(x, 3, z, Material.COBBLESTONE);
				} else {
					Material[] alternative = {
						Material.STONE, 
						Material.MOSSY_COBBLESTONE,
						Material.GRAVEL
					};
					int idx = random.nextInt(alternative.length);
					chunk.setBlock(x, 1, z, alternative[idx]);
					chunk.setBlock(x, 2, z, alternative[idx]);
					chunk.setBlock(x, 3, z, alternative[idx]);
				}
				
				
				if (random.nextDouble() < 0.02) {
					chunk.setBlock(x, 3, z, Material.GLOWSTONE);	
				}
			}
		}
		
		//generateRoom(cx, cz, chunk);
		if (chunkX == cx && chunkZ == cz) {
			generateSpawn(chunk);
		}
		
		// build cells
		Rectangle currentChunkRectangle = new Rectangle(cx * CHUNK_WIDTH, cz * CHUNK_WIDTH, CHUNK_WIDTH, CHUNK_WIDTH);
		Rectangle dungeonRectangle = dungeon.getRectangle();
		if (currentChunkRectangle.intersects(dungeonRectangle)) {
			build(currentChunkRectangle, dungeonRectangle, chunk);
		}

		
		return chunk;
		
	}
	
	private void build(Rectangle currentChunk, Rectangle dungeonRectangle, ChunkData chunk) {
		Rectangle toBeFilledIn = currentChunk.intersection(dungeonRectangle);
		fillInRectangle(chunk, toBeFilledIn);
	}
	
	private void fillInRectangle(ChunkData chunk, Rectangle rectangle) {
		Cell[][] source = this.dungeon.getCells();
		int x1 = (int) rectangle.getMinX();
		int y1 = (int) rectangle.getMinY();
		int x2 = (int) rectangle.getMaxX();
		int y2 = (int) rectangle.getMaxY();
		final int HEIGHT = 4;
		
		for (int x=x1; x<x2; x++) {
			for (int z=y1; z<y2; z++) {
				for (int y=1; y<HEIGHT; y++) {
					if (source[x][z] != null) {
						Cell cell = source[x][z];
						
						if (cell.getMaterial() == Material.DARK_OAK_DOOR) {
							Material material = cell.getMaterial();
							
							if (y == 1) {
								Door door = new Door(material);
								chunk.setBlock(x % CHUNK_WIDTH, y, z % CHUNK_WIDTH, door);
							} else if (y==2) {
								Door door = new Door(material);
								door.setTopHalf(true);
								chunk.setBlock(x % CHUNK_WIDTH, y, z % CHUNK_WIDTH, door);	
							} else if (y == HEIGHT - 1) {
								chunk.setBlock(x % CHUNK_WIDTH, y, z % CHUNK_WIDTH, Material.GLOWSTONE);
							}
						} else {
							chunk.setBlock(x % CHUNK_WIDTH, y, z % CHUNK_WIDTH, cell.getMaterial());
						}
					}
				}
			}
		}
		
	}
	
	private void generateSpawn(ChunkData chunk) {
		for (int x=0; x<16; x++) {
			for (int z=0; z<16; z++) {
				chunk.setBlock(x, 1, z, Material.GRASS);
			}
		}
	}
	
	@Override
	public Location getFixedSpawnLocation(World world, Random random) {
		Location spawn = new Location(world, 7, 5, 7);
		return spawn;
	}
	
}
