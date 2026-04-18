package net.oktawia.crazyae2addons.client.textures;

public final class ConnectedTextureRules {
    private ConnectedTextureRules() {}

    public static final ConnectedTextureRule SAME_BLOCK =
            (level, selfPos, selfState, otherPos, otherState, face) ->
                    selfState.getBlock() == otherState.getBlock();

    public static final ConnectedTextureRule SAME_BLOCK_AND_STATE =
            (level, selfPos, selfState, otherPos, otherState, face) ->
                    selfState == otherState;
}