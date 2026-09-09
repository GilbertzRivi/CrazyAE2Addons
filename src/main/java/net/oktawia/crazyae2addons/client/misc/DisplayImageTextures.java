package net.oktawia.crazyae2addons.client.misc;

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.oktawia.crazyae2addons.CrazyAddons;

@OnlyIn(Dist.CLIENT)
public final class DisplayImageTextures {

    private static final int MAX_TEXTURES = 32;
    private static final long MAX_TEXTURE_PIXELS = 32L * 1024 * 1024;

    private static long cachedPixels = 0L;

    private static final LinkedHashMap<String, Entry> TEXTURES = new LinkedHashMap<>(16, 0.75f, true);

    public record Entry(ResourceLocation location, int width, int height) {
    }

    private DisplayImageTextures() {
    }

    @Nullable
    public static Entry get(String imageId, byte[] pngBytes) {
        if (imageId == null || imageId.isEmpty() || pngBytes == null || pngBytes.length == 0) {
            return null;
        }

        Entry cached = TEXTURES.get(imageId);

        if (cached != null) {
            return cached;
        }

        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(pngBytes));
            if (image == null) {
                return null;
            }

            DynamicTexture texture = new DynamicTexture(image);
            ResourceLocation location = Minecraft.getInstance().getTextureManager().register(
                    "crazyae2addons_display_image_" + imageId,
                    texture);

            Entry created = new Entry(location, image.getWidth(), image.getHeight());
            TEXTURES.put(imageId, created);
            cachedPixels += pixelsOf(created);
            evictOldest();
            return created;
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to create display image texture", e);
            return null;
        }
    }

    public static void clear() {
        for (Entry entry : TEXTURES.values()) {
            release(entry);
        }
        TEXTURES.clear();
        cachedPixels = 0L;
    }

    private static void evictOldest() {
        Iterator<Map.Entry<String, Entry>> oldest = TEXTURES.entrySet().iterator();

        while (oldest.hasNext() && (TEXTURES.size() > MAX_TEXTURES || cachedPixels > MAX_TEXTURE_PIXELS)) {
            Entry evicted = oldest.next().getValue();

            if (TEXTURES.size() <= 1) {
                return;
            }

            release(evicted);
            cachedPixels -= pixelsOf(evicted);
            oldest.remove();
        }
    }

    private static long pixelsOf(Entry entry) {
        return (long) entry.width() * entry.height();
    }

    private static void release(Entry entry) {
        try {
            Minecraft.getInstance().getTextureManager().release(entry.location());
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to release display image texture", e);
        }
    }
}
