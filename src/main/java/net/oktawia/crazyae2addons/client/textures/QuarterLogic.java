package net.oktawia.crazyae2addons.client.textures;

final class QuarterLogic {
    private QuarterLogic() {}

    static FaceQuarters resolve(NeighborBits b) {
        return new FaceQuarters(
                topLeft(b.up(), b.left(), b.upLeft()),
                topRight(b.up(), b.right(), b.upRight()),
                bottomLeft(b.down(), b.left(), b.downLeft()),
                bottomRight(b.down(), b.right(), b.downRight())
        );
    }

    private static QuarterType topLeft(boolean up, boolean left, boolean corner) {
        if (!up && !left) return QuarterType.OUTER;
        if ( up && !left) return QuarterType.VERTICAL;
        if (!up &&  left) return QuarterType.HORIZONTAL;
        if (!corner) return QuarterType.INNER;
        return QuarterType.FULL;
    }

    private static QuarterType topRight(boolean up, boolean right, boolean corner) {
        if (!up && !right) return QuarterType.OUTER;
        if ( up && !right) return QuarterType.VERTICAL;
        if (!up &&  right) return QuarterType.HORIZONTAL;
        if (!corner) return QuarterType.INNER;
        return QuarterType.FULL;
    }

    private static QuarterType bottomLeft(boolean down, boolean left, boolean corner) {
        if (!down && !left) return QuarterType.OUTER;
        if ( down && !left) return QuarterType.VERTICAL;
        if (!down &&  left) return QuarterType.HORIZONTAL;
        if (!corner) return QuarterType.INNER;
        return QuarterType.FULL;
    }

    private static QuarterType bottomRight(boolean down, boolean right, boolean corner) {
        if (!down && !right) return QuarterType.OUTER;
        if ( down && !right) return QuarterType.VERTICAL;
        if (!down &&  right) return QuarterType.HORIZONTAL;
        if (!corner) return QuarterType.INNER;
        return QuarterType.FULL;
    }
}