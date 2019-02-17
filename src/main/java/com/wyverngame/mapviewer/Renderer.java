package com.wyverngame.mapviewer;

import com.wurmonline.mesh.Tiles;
import com.wyverngame.mapviewer.zone.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Renderer {
    private static final Logger LOGGER;
    private static final int[] COLOURS;
    private final Config config;
    private final Map<Tile, BridgePart> bridgeParts;
    private int size;
    private int[][] typeData;
    private int[][] metaData;
    private int[][] heightData;
    private List<HighwayNode> hwNodes;

    public Renderer(final Config config) {
        this.bridgeParts = new HashMap<>();
        this.size = 4096;
        this.config = config;
    }

    public int load(final List<Deed> deeds, final Map<Tile, BridgePart> bridgeParts, final List<FocusZone> focusZones, final List<HighwayNode> hwNodes) throws IOException {
        this.bridgeParts.putAll(bridgeParts);
        final Path temp = Paths.get("temp");
        if (Files.notExists(temp)) {
            Files.createDirectory(temp, (FileAttribute<?>[]) new FileAttribute[0]);
        }
        final Path map = temp.resolve("top_layer.map");
        Files.deleteIfExists(map);
        Files.copy(this.config.getMapDirectory().resolve("top_layer.map"), map);
        final Throwable t = null;
        try (final DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(map), 65536))) {
            in.readLong();
            in.readByte();
            this.size = 1 << in.readByte();
            in.skipBytes(1014);
            this.typeData = new int[this.size][this.size];
            this.metaData = new int[this.size][this.size];
            this.heightData = new int[this.size][this.size];
            for (int y = 0; y < this.size; ++y) {
                for (int x = 0; x < this.size; ++x) {
                    final int tileType = in.read() & 0xFF;
                    final int meta = in.read() & 0xFF;
                    final int height = in.readShort();
                    this.typeData[x][y] = tileType;
                    this.metaData[x][y] = meta;
                    this.heightData[x][y] = height;
                }
            }
            for (final Deed deed : deeds) {
                deed.setHeight(this.heightData[deed.getX()][deed.getY()]);
            }
            for (final FocusZone focusZone : focusZones) {
                focusZone.setHeight(this.heightData[focusZone.getX()][focusZone.getY()]);
            }
            this.hwNodes = hwNodes;
            return this.size;
        }
    }

    public BufferedImage render(Type type) {
        BufferedImage img = new BufferedImage(this.size, this.size, 1);
        int[] pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

        Arrays.fill(pixels, 3686001);

        for (int x = 0; x < this.size - 1; x++) {
            if (x % 128 == 0) {
                LOGGER.log(Level.INFO, String.format("Rendered %.1f%%", 100F * x / this.size));
            }

            for (int y = 0; y < this.size - 1; y++) {
                int colour = COLOURS[this.typeData[x][y]];

                int r = colour >>> 16 & 0xFF;
                int g = colour >>> 8 & 0xFF;
                int b = colour & 0xFF;

                float tileHeight = this.heightData[x][y] / 40.0F;
                float tileHeightN = this.heightData[(x + 1)][(y + 1)] / 40.0F;
                float tileHeightB = this.heightData[x][(y + 1)] / 40.0F;

                float delta = tileHeightN - tileHeight;
                float factor = (float) (1.0D + Math.tanh(delta / 4.0F));

                r = (int) (factor * r);
                g = (int) (factor * g);
                b = (int) (factor * b);

                if (tileHeight < 0.0F) {
                    r = r / 5 + 41;
                    g = r / 5 + 51;
                    b = r / 5 + 102;
                }

                if (r < 0) r = 0;
                if (r > 255) r = 255;
                if (g < 0) g = 0;
                if (g > 255) g = 255;
                if (b < 0) b = 0;
                if (b > 255) b = 255;

                if (type.equals(Type.NORMAL)) {
                    int start = y - (int) tileHeight;
                    if (tileHeightB - 1.0F > tileHeight) {
                        start = y - (int) tileHeightB;
                    }

                    int threshold = 2147483647;
                    if ((tileHeight >= 0.0F) && (tileHeightB < 0.0F)) {
                        threshold = y + 1;
                    }

                    for (int qy = start; qy <= start + Math.abs(delta) + 9.0F; qy++)
                        if ((qy >= 0) && (qy < this.size)) {
                            if (qy == threshold) {
                                r = r / 5 + 41;
                                g = r / 5 + 51;
                                b = r / 5 + 102;
                            }

                            pixels[(qy * this.size + x)] = (r << 16 | g << 8 | b);
                        }
                } else {
                    pixels[(y * this.size + x)] = (r << 16 | g << 8 | b);
                }

                Tile tile = new Tile(x, y);

                BridgePart part = this.bridgeParts.get(tile);
                if (part != null) {
                    if (type.equals(Type.NORMAL)) {
                        int height = part.getHeightOffset();
                        if (part.getSlope() < 0) height += part.getSlope();
                        int h = y - height / 40;
                        int ha = y - part.getHeightOffset() / 40;
                        int hb = y - (part.getHeightOffset() + part.getSlope()) / 40;
                        if ((h >= 0) && (h < this.size - 1)) {
                            if (ha != hb) pixels[((h - 1) * this.size + x)] = part.getColour();
                            pixels[(h * this.size + x)] = part.getColour();
                            pixels[((h + 1) * this.size + x)] = part.getShadowColour();
                        }
                    } else {
                        pixels[(y * this.size + x)] = part.getColour();
                    }
                }
            }
        }

        if (type == Type.ROADS) {
            for (HighwayNode n : hwNodes) {
                pixels[(n.y * this.size + n.x)] = n.c;
            }
        }

        return img;
    }

    public void save(final Path path, final Type type) throws IOException {
        if (Files.notExists(path)) {
            Files.createDirectory(path, (FileAttribute<?>[]) new FileAttribute[0]);
        }
        final BufferedImage img = this.render(type);
        final BufferedImage[][] parts = split(img, this.config.getSectionSize());
        for (int s = parts.length, x = 0; x < s; ++x) {
            for (int y = 0; y < s; ++y) {
                overwriteIfChanged(parts[x][y], path.resolve(String.valueOf(x) + "." + y + ".png"), x, y);
            }
        }
    }

    private static void overwriteIfChanged(final BufferedImage img, final Path path, final int sx, final int sy) throws IOException {
        boolean changed = true;
        if (Files.exists(path)) {
            try {
                final Throwable t = null;
                try (final InputStream in = Files.newInputStream(path)) {
                    final BufferedImage ex = ImageIO.read(in);
                    Label_0132:
                    {
                        if (img.getWidth() != ex.getWidth() || img.getHeight() != ex.getHeight()) {
                            changed = true;
                        } else {
                            changed = false;
                            for (int x = 0; x < img.getWidth(); ++x) {
                                for (int y = 0; y < img.getHeight(); ++y) {
                                    if (img.getRGB(x, y) != ex.getRGB(x, y)) {
                                        changed = true;
                                        break Label_0132;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex2) {
                changed = true;
                ex2.printStackTrace();
            }
        }
        if (changed) {
            try (final OutputStream out = Files.newOutputStream(path)) {
                Renderer.LOGGER.log(Level.INFO, "[Over]writing section " + sx + ", " + sy);
                ImageIO.write(img, "PNG", out);
            }
        }
    }

    private static BufferedImage[][] split(final BufferedImage render, final int size) {
        final int count = render.getWidth() / size;
        final BufferedImage[][] sections = new BufferedImage[count][count];
        for (int sx = 0; sx < count; ++sx) {
            for (int sy = 0; sy < count; ++sy) {
                final int x = sx * size;
                final int y = sy * size;
                Renderer.LOGGER.log(Level.INFO, "Splitting section " + sx + ", " + sy);
                final BufferedImage section = render.getSubimage(x, y, size, size);
                sections[sx][sy] = section;
            }
        }
        return sections;
    }

    static {
        LOGGER = Logger.getLogger(Renderer.class.getName());
        COLOURS = new int[256];
        for (Tiles.Tile tile : Tiles.Tile.values()) {
            COLOURS[tile.id & 0xFF] = tile.getColor().getRGB();
        }
    }

    public enum Type {
        NORMAL, FLAT, ROADS;
    }
}
