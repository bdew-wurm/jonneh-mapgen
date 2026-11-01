package com.wyverngame.mapviewer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wyverngame.mapviewer.zone.Deed;
import com.wyverngame.mapviewer.zone.FocusZone;
import com.wyverngame.mapviewer.zone.ZoneInfo;

import java.io.IOException;
import java.io.InputStream;
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
        saveData(config.getOutputDirectory(), zoneInfo.getDeeds(), zoneInfo.getFocusZones(), config, size);
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

    private static void saveData(final Path path, final List<Deed> deeds, final List<FocusZone> focusZones, final Config config, final int size) throws IOException {
        MapViewer.LOGGER.log(Level.INFO, "Saving map data");
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
        final JsonObject data = new JsonObject();
        final JsonObject mapConfig = new JsonObject();
        mapConfig.addProperty("size", size);
        mapConfig.addProperty("step", config.getSectionSize());
        mapConfig.addProperty("x", x);
        mapConfig.addProperty("y", y);
        mapConfig.addProperty("mode", "3d");
        mapConfig.addProperty("showDeedBordersIn3dMode", config.showDeedBorders3d());
        mapConfig.addProperty("showDeedBordersInFlatMode", config.showDeedBordersFlat());
        data.add("config", mapConfig);

        final JsonArray deedData = new JsonArray();
        final JsonArray focusZoneData = new JsonArray();
        if (config.showDeeds()) {
            deeds.stream().map(MapViewer::buildDeed).forEach(deedData::add);
            focusZones.stream().map(MapViewer::buildFocusZone).forEach(focusZoneData::add);
        }
        data.add("deeds", deedData);
        data.add("focusZones", focusZoneData);
        final byte[] json = new Gson().toJson(data).getBytes(StandardCharsets.UTF_8);
        Files.write(path.resolve("config.json"), json);
    }

    private static JsonObject buildDeed(Deed deed) {
        final JsonObject deedObject = new JsonObject();
        deedObject.addProperty("name", deed.getName());
        deedObject.addProperty("x", deed.getX());
        deedObject.addProperty("y", deed.getY());
        deedObject.addProperty("height", deed.getHeight());
        deedObject.addProperty("permanent", deed.isPermanent());
        deedObject.addProperty("sx", deed.getSx());
        deedObject.addProperty("sy", deed.getSy());
        deedObject.addProperty("ex", deed.getEx());
        deedObject.addProperty("ey", deed.getEy());
        return deedObject;
    }

    private static JsonObject buildFocusZone(FocusZone focusZone) {
        final JsonObject focusZoneObject = new JsonObject();
        focusZoneObject.addProperty("name", focusZone.getName());
        focusZoneObject.addProperty("x", focusZone.getX());
        focusZoneObject.addProperty("y", focusZone.getY());
        focusZoneObject.addProperty("height", focusZone.getHeight());
        focusZoneObject.addProperty("type", focusZone.getType());
        focusZoneObject.addProperty("sx", focusZone.getSx());
        focusZoneObject.addProperty("sy", focusZone.getSy());
        focusZoneObject.addProperty("ex", focusZone.getEx());
        focusZoneObject.addProperty("ey", focusZone.getEy());
        return focusZoneObject;
    }

    static {
        LOGGER = Logger.getLogger(MapViewer.class.getName());
        RESOURCES = new String[]{"index.html", "map.js", "main.css", "search.png", "bg.png"};
    }
}
