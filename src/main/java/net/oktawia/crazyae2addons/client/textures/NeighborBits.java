package net.oktawia.crazyae2addons.client.textures;

record NeighborBits(
    boolean up, boolean right, boolean down, boolean left,
    boolean upRight, boolean upLeft, boolean downRight, boolean downLeft
) {}

enum QuarterType {
    OUTER(0),
    FULL(1),
    VERTICAL(2),
    HORIZONTAL(3),
    INNER(4);

    final int tile;
    QuarterType(int tile) { this.tile = tile; }
}

record FaceQuarters(
    QuarterType topLeft,
    QuarterType topRight,
    QuarterType bottomLeft,
    QuarterType bottomRight
) {}