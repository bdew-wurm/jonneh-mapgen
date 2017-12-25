package com.wyverngame.mapviewer.zone;

import com.wyverngame.mapviewer.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ZoneInfo {
    private static final Logger LOGGER;
    private final List<Deed> deeds;
    private final List<FocusZone> focusZones;
    private final Map<Tile, BridgePart> bridgeParts;
    private final List<HighwayNode> hwNodes;

    private ZoneInfo() {
        this.deeds = new ArrayList<>();
        this.focusZones = new ArrayList<>();
        this.bridgeParts = new HashMap<>();
        this.hwNodes = new ArrayList<>();
    }

    public List<Deed> getDeeds() {
        return this.deeds;
    }

    public List<FocusZone> getFocusZones() {
        return this.focusZones;
    }

    public List<HighwayNode> getHwNodes() {
        return this.hwNodes;
    }

    public Map<Tile, BridgePart> getBridgeParts() {
        return this.bridgeParts;
    }

    public static ZoneInfo load(final Config config) throws SQLException, IOException {
        final Path temp = Paths.get("temp");
        if (Files.notExists(temp)) {
            Files.createDirectory(temp, (FileAttribute<?>[]) new FileAttribute[0]);
        }
        ZoneInfo.LOGGER.log(Level.INFO, "Copying zones database");
        final Path zonesTable = temp.resolve("wurmzones.db");
        Files.deleteIfExists(zonesTable);
        Files.copy(config.getMapDirectory().resolve("sqlite/wurmzones.db"), zonesTable);
        ZoneInfo.LOGGER.log(Level.INFO, "Copying items database");
        final Path itemsTable = temp.resolve("wurmitems.db");
        Files.deleteIfExists(itemsTable);
        Files.copy(config.getMapDirectory().resolve("sqlite/wurmitems.db"), itemsTable);
        final ZoneInfo info = new ZoneInfo();
        final Map<Tile, BridgePart> bridges = info.getBridgeParts();
        final List<Deed> deeds = info.getDeeds();
        final List<FocusZone> focusZones = info.getFocusZones();
        final List<HighwayNode> hwNodes = info.getHwNodes();
        final Connection zonesConnection = DriverManager.getConnection("jdbc:sqlite:temp/wurmzones.db");
        final Connection itemsConnection = DriverManager.getConnection("jdbc:sqlite:temp/wurmitems.db");
        ZoneInfo.LOGGER.log(Level.INFO, "Loading bridges");
        final PreparedStatement bridgeStatement = zonesConnection.prepareStatement("SELECT * FROM BRIDGEPARTS;");
        final ResultSet bridgeResultSet = bridgeStatement.executeQuery();
        while (bridgeResultSet.next()) {
            final int x = bridgeResultSet.getInt("TILEX");
            final int y = bridgeResultSet.getInt("TILEY");
            final int material = bridgeResultSet.getInt("MATERIAL");
            final int heightOffset = bridgeResultSet.getInt("HEIGHTOFFSET");
            final int slope = bridgeResultSet.getInt("SLOPE");
            final Tile tile = new Tile(x, y);
            final BridgePart bridgePart = new BridgePart(material, heightOffset, slope);
            bridges.put(tile, bridgePart);
        }
        ZoneInfo.LOGGER.log(Level.INFO, "Loading deeds and tokens");
        final PreparedStatement tokenStatement = itemsConnection.prepareStatement("SELECT * FROM ITEMS WHERE `WURMID` == ?;");
        final PreparedStatement deedStatement = zonesConnection.prepareStatement("SELECT * FROM VILLAGES WHERE `DISBANDED` == 0;");
        final ResultSet deedResultSet = deedStatement.executeQuery();
        while (deedResultSet.next()) {
            final String name = deedResultSet.getString("NAME");
            final int startX = deedResultSet.getInt("STARTX");
            final int startY = deedResultSet.getInt("STARTY");
            final int endX = deedResultSet.getInt("ENDX");
            final int endY = deedResultSet.getInt("ENDY");
            int x2 = (startX + endX) / 2;
            int y2 = (startY + endY) / 2;
            final long tokenId = deedResultSet.getLong("TOKEN");
            tokenStatement.setLong(1, tokenId);
            final ResultSet tokenResultSet = tokenStatement.executeQuery();
            while (tokenResultSet.next()) {
                x2 = (int) (tokenResultSet.getFloat("POSX") / 4.0f);
                y2 = (int) (tokenResultSet.getFloat("POSY") / 4.0f);
            }
            final boolean permanent = deedResultSet.getInt("PERMANENT") == 1;
            deeds.add(new Deed(name, x2, y2, permanent, startX, startY, endX, endY));
        }
        ZoneInfo.LOGGER.log(Level.INFO, "Loading focus zones");
        final PreparedStatement focusZoneStatement = zonesConnection.prepareStatement("SELECT * FROM FOCUSZONES;");
        final ResultSet focusZonesResultSet = focusZoneStatement.executeQuery();
        while (focusZonesResultSet.next()) {
            final String name2 = focusZonesResultSet.getString("NAME");
            final int startX2 = focusZonesResultSet.getInt("STARTX");
            final int startY2 = focusZonesResultSet.getInt("STARTY");
            final int endX2 = focusZonesResultSet.getInt("ENDX");
            final int endY2 = focusZonesResultSet.getInt("ENDY");
            final int type = focusZonesResultSet.getInt("TYPE");
            focusZones.add(new FocusZone(name2, startX2, startY2, endX2, endY2, type));
        }
        final PreparedStatement highwayItemsStatement = itemsConnection.prepareStatement("SELECT TEMPLATEID,POSX,POSY,AUXDATA FROM ITEMS WHERE TEMPLATEID IN (1114,1112);");
        final ResultSet highwayItemsResultSet = highwayItemsStatement.executeQuery();
        while (highwayItemsResultSet.next()) {
            final int t = highwayItemsResultSet.getInt("TEMPLATEID");
            final int x = highwayItemsResultSet.getInt("POSX");
            final int y = highwayItemsResultSet.getInt("POSY");
            final int a = highwayItemsResultSet.getInt("AUXDATA");
            if (x > 0 && y > 0 & a != 0) {
                hwNodes.add(new HighwayNode(x / 4, y / 4, t == 1112 ? HW_WAYSTONE : HW_NODE));
            }
        }

        itemsConnection.close();
        zonesConnection.close();
        return info;
    }

    private static final int
            HW_NODE = 0xFF0000,
            HW_WAYSTONE = 0xFFA500;


    static {
        LOGGER = Logger.getLogger(ZoneInfo.class.getName());
    }
}
