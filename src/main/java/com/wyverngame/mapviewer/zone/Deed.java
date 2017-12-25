package com.wyverngame.mapviewer.zone;

public final class Deed {
    private final String name;
    private final int x;
    private final int y;
    private final int sx;
    private final int sy;
    private final int ex;
    private final int ey;
    private final boolean permanent;
    private int height;

    public Deed(final String name, final int x, final int y, final boolean permanent, final int sx, final int sy, final int ex, final int ey) {
        this.height = 0;
        this.name = name;
        this.x = x;
        this.y = y;
        this.permanent = permanent;
        this.sx = sx;
        this.sy = sy;
        this.ex = ex;
        this.ey = ey;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(final int height) {
        this.height = height;
    }

    public String getName() {
        return this.name;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public boolean isPermanent() {
        return this.permanent;
    }

    public int getSx() {
        return this.sx;
    }

    public int getSy() {
        return this.sy;
    }

    public int getEx() {
        return this.ex;
    }

    public int getEy() {
        return this.ey;
    }
}
