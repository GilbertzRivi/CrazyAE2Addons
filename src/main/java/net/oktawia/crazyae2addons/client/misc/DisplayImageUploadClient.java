package net.oktawia.crazyae2addons.client.misc;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.lwjgl.util.tinyfd.TinyFileDialogs;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.logic.display.DisplayImageLimits;
import net.oktawia.crazyae2addons.logic.display.DisplayImageTransfer;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.UploadDisplayImageStreamPacket;

@OnlyIn(Dist.CLIENT)
public final class DisplayImageUploadClient {

    public record Result(Component message, int color, boolean success) {
    }

    private static final int MAX_SOURCE_NAME_BYTES = 64;

    private DisplayImageUploadClient() {
    }

    public static Result pickAndUpload(DisplayImageLimits limits) {
        String selected = TinyFileDialogs.tinyfd_openFileDialog(
                Component.translatable(LangDefs.PICK_FILE.getTranslationKey()).getString(),
                "",
                null,
                null,
                false);

        if (selected == null || selected.isBlank()) {
            return new Result(
                    Component.translatable(LangDefs.IMAGE_UPLOAD_CANCELLED.getTranslationKey()),
                    0xFFAAAAAA,
                    false);
        }

        try {
            return uploadPath(Path.of(stripQuotes(selected.trim())), limits);
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("invalid display image path from file dialog", e);
            return new Result(
                    Component.translatable(LangDefs.IMAGE_UPLOAD_INVALID_PATH.getTranslationKey()),
                    0xFFFF5555,
                    false);
        }
    }

    public static Result pasteAndUpload(DisplayImageLimits limits) {
        try {
            System.setProperty("java.awt.headless", "false");
            var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            var transferable = clipboard.getContents(null);

            if (transferable == null) {
                return new Result(
                        Component.translatable(LangDefs.IMAGE_UPLOAD_CLIPBOARD_EMPTY.getTranslationKey()),
                        0xFFFF5555,
                        false);
            }

            if (transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                Object data = transferable.getTransferData(DataFlavor.imageFlavor);
                if (data instanceof Image image) {
                    return uploadBufferedImage(toBufferedImage(image), "clipboard_image.png", limits);
                }
            }

            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                Object data = transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (data instanceof List<?> list && !list.isEmpty()) {
                    Object first = list.get(0);
                    if (first instanceof File file) {
                        return uploadPath(file.toPath(), limits);
                    }
                }
            }

            if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                Object data = transferable.getTransferData(DataFlavor.stringFlavor);
                if (data instanceof String s && !s.isBlank()) {
                    try {
                        return uploadPath(Path.of(stripQuotes(s.trim())), limits);
                    } catch (Throwable e) {
                        CrazyAddons.LOGGER.debug("invalid display image path from clipboard string", e);
                        return new Result(
                                Component.translatable(LangDefs.IMAGE_UPLOAD_INVALID_PATH.getTranslationKey()),
                                0xFFFF5555,
                                false);
                    }
                }
            }
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to read display image from clipboard", e);
            return new Result(
                    Component.translatable(LangDefs.IMAGE_UPLOAD_FAILED.getTranslationKey()),
                    0xFFFF5555,
                    false);
        }

        return new Result(
                Component.translatable(LangDefs.IMAGE_UPLOAD_CLIPBOARD_EMPTY.getTranslationKey()),
                0xFFFF5555,
                false);
    }

    public static Result uploadDroppedFiles(List<Path> paths, DisplayImageLimits limits) {
        if (paths == null || paths.isEmpty()) {
            return new Result(
                    Component.translatable(LangDefs.IMAGE_UPLOAD_INVALID_PATH.getTranslationKey()),
                    0xFFFF5555,
                    false);
        }

        for (Path path : paths) {
            if (path != null && Files.isRegularFile(path)) {
                return uploadPath(path, limits);
            }
        }

        return new Result(
                Component.translatable(LangDefs.IMAGE_UPLOAD_INVALID_PATH.getTranslationKey()),
                0xFFFF5555,
                false);
    }

    public static Result uploadPath(Path path, DisplayImageLimits limits) {
        if (path == null || !Files.isRegularFile(path)) {
            return new Result(
                    Component.translatable(LangDefs.IMAGE_UPLOAD_INVALID_PATH.getTranslationKey()),
                    0xFFFF5555,
                    false);
        }

        try {
            DisplayImageGif.Animation animation = DisplayImageGif.read(
                    path.toFile(),
                    CrazyConfig.COMMON.DISPLAY_IMAGE_MAX_FRAMES.get());

            Path animationName = path.getFileName();

            if (animation != null) {
                return uploadAnimation(
                        animation,
                        animationName == null ? "image.gif" : animationName.toString(),
                        limits);
            }

            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                return new Result(
                        Component.translatable(LangDefs.IMAGE_UPLOAD_INVALID_IMAGE.getTranslationKey()),
                        0xFFFF5555,
                        false);
            }

            Path fileName = path.getFileName();
            return uploadBufferedImage(image, fileName == null ? "image.png" : fileName.toString(), limits);
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to upload display image from path", e);
            return new Result(
                    Component.translatable(LangDefs.IMAGE_UPLOAD_FAILED.getTranslationKey()),
                    0xFFFF5555,
                    false);
        }
    }

    private static Result uploadBufferedImage(BufferedImage original, String sourceName, DisplayImageLimits rawLimits) {
        DisplayImageLimits limits = rawLimits == null ? DisplayImageLimits.fromConfig() : rawLimits.sanitized();

        try {
            BufferedImage img = ensureArgb(original);

            if (img.getWidth() <= 0 || img.getHeight() <= 0) {
                return new Result(
                        Component.translatable(LangDefs.IMAGE_UPLOAD_INVALID_IMAGE.getTranslationKey()),
                        0xFFFF5555,
                        false);
            }

            if (img.getWidth() > limits.maxDimension() || img.getHeight() > limits.maxDimension()) {
                img = resizeToFit(img, limits.maxDimension(), limits.maxDimension());
            }

            String safeName = sanitizeSourceName(sourceName);
            byte[] pngBytes = encodePng(img);

            while (pngBytes.length > limits.maxBytes()) {
                int oldW = img.getWidth();
                int oldH = img.getHeight();

                if (oldW <= 1 && oldH <= 1) {
                    break;
                }

                int nextW = oldW > 1 ? Math.max(1, (int) Math.floor(oldW * 0.85)) : 1;
                int nextH = oldH > 1 ? Math.max(1, (int) Math.floor(oldH * 0.85)) : 1;

                if (nextW == oldW && nextH == oldH) {
                    break;
                }

                img = resizeExact(img, nextW, nextH);
                pngBytes = encodePng(img);
            }

            if (pngBytes.length > limits.maxBytes()) {
                return new Result(
                        Component.translatable(
                                LangDefs.IMAGE_UPLOAD_TOO_LARGE.getTranslationKey(),
                                limits.maxBytes() / 1024),
                        0xFFFF5555,
                        false);
            }

            sendStream(safeName, pngBytes);

            return new Result(
                    Component.translatable(
                            LangDefs.IMAGE_UPLOAD_SENDING.getTranslationKey(),
                            img.getWidth(),
                            img.getHeight()),
                    0xFFFFFF55,
                    true);
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to upload buffered display image", e);
            return new Result(
                    Component.translatable(LangDefs.IMAGE_UPLOAD_FAILED.getTranslationKey()),
                    0xFFFF5555,
                    false);
        }
    }

    private static Result uploadAnimation(
            DisplayImageGif.Animation animation,
            String sourceName,
            DisplayImageLimits rawLimits) {
        DisplayImageLimits limits = rawLimits == null ? DisplayImageLimits.fromConfig() : rawLimits.sanitized();

        try {
            List<BufferedImage> frames = animation.frames();
            BufferedImage first = frames.get(0);

            if (first.getWidth() <= 0 || first.getHeight() <= 0) {
                return new Result(
                        Component.translatable(LangDefs.IMAGE_UPLOAD_INVALID_IMAGE.getTranslationKey()),
                        0xFFFF5555,
                        false);
            }

            int columns = (int) Math.ceil(Math.sqrt(frames.size()));
            int rows = (frames.size() + columns - 1) / columns;

            float fit = Math.min(
                    limits.maxDimension() / (float) (columns * first.getWidth()),
                    limits.maxDimension() / (float) (rows * first.getHeight()));

            int cellWidth = Math.max(1, (int) Math.floor(first.getWidth() * Math.min(1f, fit)));
            int cellHeight = Math.max(1, (int) Math.floor(first.getHeight() * Math.min(1f, fit)));

            byte[] pngBytes = encodePng(buildAtlas(frames, columns, rows, cellWidth, cellHeight));

            while (pngBytes.length > limits.maxBytes() && (cellWidth > 1 || cellHeight > 1)) {
                int nextWidth = cellWidth > 1 ? Math.max(1, (int) Math.floor(cellWidth * 0.85)) : 1;
                int nextHeight = cellHeight > 1 ? Math.max(1, (int) Math.floor(cellHeight * 0.85)) : 1;

                if (nextWidth == cellWidth && nextHeight == cellHeight) {
                    break;
                }

                cellWidth = nextWidth;
                cellHeight = nextHeight;
                pngBytes = encodePng(buildAtlas(frames, columns, rows, cellWidth, cellHeight));
            }

            if (pngBytes.length > limits.maxBytes()) {
                return new Result(
                        Component.translatable(
                                LangDefs.IMAGE_UPLOAD_TOO_LARGE.getTranslationKey(),
                                limits.maxBytes() / 1024),
                        0xFFFF5555,
                        false);
            }

            sendStream(
                    sanitizeSourceName(sourceName),
                    pngBytes,
                    frames.size(),
                    columns,
                    animation.frameDurationMs());

            return new Result(
                    Component.translatable(
                            LangDefs.IMAGE_UPLOAD_SENDING_ANIMATED.getTranslationKey(),
                            cellWidth,
                            cellHeight,
                            frames.size()),
                    0xFFFFFF55,
                    true);
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to upload animated display image", e);
            return new Result(
                    Component.translatable(LangDefs.IMAGE_UPLOAD_FAILED.getTranslationKey()),
                    0xFFFF5555,
                    false);
        }
    }

    private static BufferedImage buildAtlas(
            List<BufferedImage> frames,
            int columns,
            int rows,
            int cellWidth,
            int cellHeight) {
        BufferedImage atlas = new BufferedImage(
                columns * cellWidth,
                rows * cellHeight,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = atlas.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            for (int i = 0; i < frames.size(); i++) {
                int column = i % columns;
                int row = i / columns;
                graphics.drawImage(
                        frames.get(i),
                        column * cellWidth,
                        row * cellHeight,
                        cellWidth,
                        cellHeight,
                        null);
            }
        } finally {
            graphics.dispose();
        }

        return atlas;
    }

    private static void sendStream(String sourceName, byte[] pngBytes) {
        sendStream(sourceName, pngBytes, 1, 1, 0);
    }

    private static void sendStream(
            String sourceName,
            byte[] pngBytes,
            int frameCount,
            int columns,
            int frameDurationMs) {
        NetworkHandler.sendToServer(UploadDisplayImageStreamPacket.begin(
                sourceName,
                frameCount,
                columns,
                frameDurationMs));

        for (int offset = 0; offset < pngBytes.length; offset += DisplayImageTransfer.CHUNK_BYTES) {
            int end = Math.min(pngBytes.length, offset + DisplayImageTransfer.CHUNK_BYTES);
            NetworkHandler.sendToServer(UploadDisplayImageStreamPacket.data(
                    Arrays.copyOfRange(pngBytes, offset, end)));
        }

        NetworkHandler.sendToServer(UploadDisplayImageStreamPacket.end());
    }

    private static String sanitizeSourceName(String sourceName) {
        String name = sourceName == null || sourceName.isBlank() ? "image.png" : sourceName.trim();

        name = stripQuotes(name);
        name = name.replace('\\', '/');

        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = slash + 1 < name.length() ? name.substring(slash + 1) : "image.png";
        }

        if (name.isBlank()) {
            name = "image.png";
        }

        name = trimUtf8ToMaxBytes(name, MAX_SOURCE_NAME_BYTES);

        if (name.isBlank()) {
            name = "image.png";
        }

        return name;
    }

    private static String trimUtf8ToMaxBytes(String s, int maxBytes) {
        if (s.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
            return s;
        }

        int dot = s.lastIndexOf('.');
        String ext = "";

        if (dot > 0 && dot + 1 < s.length() && s.length() - dot <= 12) {
            ext = s.substring(dot);
        }

        if (!ext.isEmpty()) {
            int extBytes = ext.getBytes(StandardCharsets.UTF_8).length;
            int baseMaxBytes = Math.max(1, maxBytes - extBytes);
            String base = s.substring(0, dot);
            String trimmedBase = trimUtf8Prefix(base, baseMaxBytes);
            String out = trimmedBase + ext;

            if (!trimmedBase.isBlank() && out.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
                return out;
            }
        }

        return trimUtf8Prefix(s, maxBytes);
    }

    private static String trimUtf8Prefix(String s, int maxBytes) {
        StringBuilder out = new StringBuilder();
        int used = 0;

        for (int i = 0; i < s.length();) {
            int cp = s.codePointAt(i);
            String part = new String(Character.toChars(cp));
            int partBytes = part.getBytes(StandardCharsets.UTF_8).length;

            if (used + partBytes > maxBytes) {
                break;
            }

            out.appendCodePoint(cp);
            used += partBytes;
            i += Character.charCount(cp);
        }

        return out.toString();
    }

    private static BufferedImage resizeToFit(BufferedImage src, int maxW, int maxH) {
        double scale = Math.min(maxW / (double) src.getWidth(), maxH / (double) src.getHeight());
        if (scale >= 1.0) {
            return src;
        }

        int newW = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int newH = Math.max(1, (int) Math.round(src.getHeight() * scale));
        return resizeExact(src, newW, newH);
    }

    private static BufferedImage resizeExact(BufferedImage src, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.drawImage(src.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static BufferedImage ensureArgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) {
            return src;
        }

        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.drawImage(src, 0, 0, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage buffered && buffered.getType() == BufferedImage.TYPE_INT_ARGB) {
            return buffered;
        }

        int w = Math.max(1, image.getWidth(null));
        int h = Math.max(1, image.getHeight(null));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.drawImage(image, 0, 0, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static byte[] encodePng(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private static String stripQuotes(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
