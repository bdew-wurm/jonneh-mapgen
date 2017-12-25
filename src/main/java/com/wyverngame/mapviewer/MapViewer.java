package com.wyverngame.mapviewer;

import com.wyverngame.mapviewer.zone.Deed;
import com.wyverngame.mapviewer.zone.FocusZone;
import com.wyverngame.mapviewer.zone.ZoneInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MapViewer {
    private static final Logger LOGGER;
    private static final String[] RESOURCES;

    public static void main(final String[] args) throws IOException, SQLException {
        MapViewer.LOGGER.log(Level.INFO, "Loading config");
        Config config;
        if (args.length > 0) {
            config = Config.load(Paths.get(args[0]));
        } else {
            config = Config.load(Paths.get("mapviewer.cfg"));
        }
        final Renderer renderer = new Renderer(config);
        if (Files.notExists(config.getOutputDirectory())) {
            Files.createDirectory(config.getOutputDirectory(), (FileAttribute<?>[]) new FileAttribute[0]);
        }
        copyResources(config);
        MapViewer.LOGGER.log(Level.INFO, "Loading map data");
        final ZoneInfo zoneInfo = ZoneInfo.load(config);
        final int size = renderer.load(zoneInfo.getDeeds(), zoneInfo.getBridgeParts(), zoneInfo.getFocusZones(), zoneInfo.getHwNodes()) / config.getSectionSize();
        saveConfig(config.getOutputDirectory(), zoneInfo.getDeeds(), zoneInfo.getFocusZones(), config, size);
        MapViewer.LOGGER.log(Level.INFO, "Saving 3d map");
        renderer.save(config.getOutputDirectory().resolve("sections-3d"), Renderer.Type.NORMAL);
        MapViewer.LOGGER.log(Level.INFO, "Saving flat map");
        renderer.save(config.getOutputDirectory().resolve("sections-flat"), Renderer.Type.FLAT);
        MapViewer.LOGGER.log(Level.INFO, "Saving road map");
        renderer.save(config.getOutputDirectory().resolve("sections-roads"), Renderer.Type.ROADS);
        MapViewer.LOGGER.log(Level.INFO, "Done");
    }

    private static void copyResources(final Config config) throws IOException {
        String[] resources;
        for (int length = (resources = MapViewer.RESOURCES).length, i = 0; i < length; ++i) {
            final String res = resources[i];
            final Throwable t = null;
            try (final InputStream in = MapViewer.class.getResourceAsStream("/res/" + res)) {
                final Path path = config.getOutputDirectory().resolve(res);
                if (!Files.exists(path)) {
                    MapViewer.LOGGER.log(Level.INFO, "Copying " + res + " to " + path);
                    Files.copy(in, path);
                }
            }
        }
    }

    private static void saveConfig(final Path path, final List<Deed> deeds, final List<FocusZone> focusZones, final Config config, final int size) throws IOException {
        MapViewer.LOGGER.log(Level.INFO, "Saving config file");
        int x;
        int y = x = size * config.getSectionSize() / 2;
        for (final Deed deed : deeds) {
            if (deed.isPermanent()) {
                x = deed.getX();
                y = deed.getY();
                break;
            }
        }
        if (Files.notExists(path)) {
            Files.createDirectory(path, (FileAttribute<?>[]) new FileAttribute[0]);
        }
        try (final OutputStream out = Files.newOutputStream(path.resolve("config.js"))) {
            out.write("function Config() {}\n".getBytes(StandardCharsets.UTF_8));
            out.write("var config = new Config();\n".getBytes(StandardCharsets.UTF_8));
            out.write(("config.size = " + size + ";\n").getBytes(StandardCharsets.UTF_8));
            out.write(("config.step = " + config.getSectionSize() + ";\n").getBytes(StandardCharsets.UTF_8));
            out.write(("config.x = " + x + ";\n").getBytes(StandardCharsets.UTF_8));
            out.write(("config.y = " + y + ";\n").getBytes(StandardCharsets.UTF_8));
            out.write("config.mode = \"3d\";\n".getBytes(StandardCharsets.UTF_8));
            out.write(("config.showDeedBordersIn3dMode = " + config.showDeedBorders3d() + ";\n").getBytes(StandardCharsets.UTF_8));
            out.write(("config.showDeedBordersInFlatMode = " + config.showDeedBordersFlat() + ";\n").getBytes(StandardCharsets.UTF_8));
            out.write("\n".getBytes(StandardCharsets.UTF_8));
            out.write("function Deed(name, x, y, height, permanent, sx, sy, ex, ey) {\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.name = name;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.x = x;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.y = y;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.sx = sx;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.sy = sy;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.ex = ex;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.ey = ey;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.height = height;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.permanent = permanent;\n".getBytes(StandardCharsets.UTF_8));
            out.write("}\n".getBytes(StandardCharsets.UTF_8));
            out.write("\n".getBytes(StandardCharsets.UTF_8));
            out.write("function FocusZone(name, x, y, height, type, sx, sy, ex, ey) {\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.name = name;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.x = x;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.y = y;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.sx = sx;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.sy = sy;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.ex = ex;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.ey = ey;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.height = height;\n".getBytes(StandardCharsets.UTF_8));
            out.write("\tthis.type = type;\n".getBytes(StandardCharsets.UTF_8));
            out.write("}\n".getBytes(StandardCharsets.UTF_8));
            out.write("\n".getBytes(StandardCharsets.UTF_8));
            out.write("var deeds = [];\n".getBytes(StandardCharsets.UTF_8));
            out.write("var focusZones = [];\n".getBytes(StandardCharsets.UTF_8));
            if (config.showDeeds()) {
                for (final Deed deed2 : deeds) {
                    out.write(("deeds.push(new Deed('" + deed2.getName().replace("'", "\\'") + "', " + deed2.getX() + ", " + deed2.getY() + ", " + deed2.getHeight() + ", " + deed2.isPermanent() + ", " + deed2.getSx() + ", " + deed2.getSy() + ", " + deed2.getEx() + ", " + deed2.getEy() + "));\n").getBytes(StandardCharsets.UTF_8));
                }
                for (final FocusZone focusZone : focusZones) {
                    out.write(("focusZones.push(new FocusZone('" + focusZone.getName().replace("'", "\\'") + "', " + focusZone.getX() + ", " + focusZone.getY() + ", " + focusZone.getHeight() + ", " + focusZone.getType() + ", " + focusZone.getSx() + ", " + focusZone.getSy() + ", " + focusZone.getEx() + ", " + focusZone.getEy() + "));\n").getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    static {
        LOGGER = Logger.getLogger(MapViewer.class.getName());
        RESOURCES = new String[]{"index.html", "map.js", "main.css", "search.png", "bg.png"};
    }
}
