package net.oktawia.crazyae2addons.client.misc;

import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.network.packets.UploadDisplayImagePacket;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class DisplayImageUploadClient {

    public record Result(Component message, int color, boolean success) {}

    private static final int MAX_DIM = UploadDisplayImagePacket.MAX_IMAGE_DIM;
    private static final int MAX_BYTES = UploadDisplayImagePacket.MAX_IMAGE_BYTES;

    private DisplayImageUploadClient() {
    }

    public static Result pickAndUpload() {
        String selected = TinyFileDialogs.tinyfd_openFileDialog(
                Component.translatable(LangDefs.PICK_FILE.getTranslationKey()).getString(),
                "",
                null,
                null,
                false
        );

        if (selected == null || selected.isBlank()) {
            return new Result(
                    Component.translatable(LangDefs.IMAGE_UPLOAD_CANCELLED.getTranslationKey()),
                    0xFFAAAAAA,
                    false
            );
        }

        try {
            return uploadPath(Path.of(stripQuotes(selected.trim())));
        } catch (Throwable ignored) {
            return new Result(
                    Component.translatable(LangDefs.IMAGE_UPLOAD_INVALID_PATH.getTranslationKey()),
                    0xFFFF5555,
                    false
            );
        }
    }

    public static Result pasteAndUpload() {
        try {
            var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            var transferable = clipboard.getContents(null);

            if (transferable == null) {
                return new Result(
                        Component.translatable(LangDefs.IMAGE_UPLOAD_CLIPBOARD_EMPTY.getTranslationKey()),
                        0xFFFF5555,
                        false
                );
            }

            if (transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                Object data = transferable.getTransferData(DataFlavor.imageFlavor);
                if (data instanceof java.awt.Image image) {
                    return uploadBufferedImage(toBufferedImage(image), "clipboard_image.png");
                }
            }

            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                Object data = transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (data instanceof List<?> list && !list.isEmpty()) {
                    Object first = list.getFirst();
                    if (first instanceof java.io.File file) {
                        return uploadPath(file.toPath());
                    }
                }
            }

            if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                Object data = transferable.getTransferData(DataFlavor.stringFlavor);
                if (data instanceof String s && !s.isBlank()) {
                    try {
                        return uploadPath(Path.of(stripQuotes(s.trim())));
                    } catch (Throwable ignored) {
                        return new Result(
                                Component.translatable(LangDefs.IMAGE_UPLOAD_INVALID_PATH.getTranslationKey()),
                                0xFFFF5555,
                                false
                        );
                    }
                }
            }
        } catch (Throwable ignored) {
            return new Result(
                    Component.translatable(LangDefs.IMAGE_UPLOAD_FAILED.getTranslationKey()),
                    0xFFFF5555,
                    false
            );
        }

        return new Result(
                Component.translatable(LangDefs.IMAGE_UPLOAD_CLIPBOARD_EMPTY.getTranslationKey()),
                0xFFFF5555,
                false
        );
    }

    public static Result uploadDroppedFiles(List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            return new Result(
                    Component.translatable(LangDefs.IMAGE_UPLOAD_INVALID_PATH.getTranslationKey()),
                    0xFFFF5555,
                    false
            );
        }

        for (Path path : paths) {
            if (path != null && Files.isRegularFile(path)) {
                return uploadPath(path);
            }
        }

        return new Result(
                Component.translatable(LangDefs.IMAGE_UPLOAD_INVALID_PATH.getTranslationKey()),
                0xFFFF5555,
                false
        );
    }

    public static Result uploadPath(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return new Result(
                    Component.translatable(LangDefs.IMAGE_UPLOAD_INVALID_PATH.getTranslationKey()),
                    0xFFFF5555,
                    false
            );
        }

        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                return new Result(
                        Component.translatable(LangDefs.IMAGE_UPLOAD_INVALID_IMAGE.getTranslationKey()),
                        0xFFFF5555,
                        false
                );
            }

            return uploadBufferedImage(image, path.getFileName().toString());
        } catch (Throwable ignored) {
            return new Result(
                    Component.translatable(LangDefs.IMAGE_UPLOAD_FAILED.getTranslationKey()),
                    0xFFFF5555,
                    false
            );
        }
    }

    private static Result uploadBufferedImage(BufferedImage original, String sourceName) {
        try {
            BufferedImage img = ensureArgb(original);

            if (img.getWidth() <= 0 || img.getHeight() <= 0) {
                return new Result(
                        Component.translatable(LangDefs.IMAGE_UPLOAD_INVALID_IMAGE.getTranslationKey()),
                        0xFFFF5555,
                        false
                );
            }

            if (img.getWidth() > MAX_DIM || img.getHeight() > MAX_DIM) {
                img = resizeToFit(img, MAX_DIM, MAX_DIM);
            }

            byte[] pngBytes = encodePng(img);

            while (pngBytes.length > MAX_BYTES && img.getWidth() > 1 && img.getHeight() > 1) {
                int nextW = Math.max(1, (int) Math.floor(img.getWidth() * 0.85));
                int nextH = Math.max(1, (int) Math.floor(img.getHeight() * 0.85));
                if (nextW == img.getWidth() && nextH == img.getHeight()) {
                    break;
                }
                img = resizeExact(img, nextW, nextH);
                pngBytes = encodePng(img);
            }

            if (pngBytes.length > MAX_BYTES) {
                return new Result(
                        Component.translatable(
                                LangDefs.IMAGE_UPLOAD_TOO_LARGE.getTranslationKey(),
                                pngBytes.length
                        ),
                        0xFFFF5555,
                        false
                );
            }

            PacketDistributor.sendToServer(new UploadDisplayImagePacket(
                    sourceName == null || sourceName.isBlank() ? "image.png" : sourceName,
                    pngBytes,
                    img.getWidth(),
                    img.getHeight()
            ));

            return new Result(
                    Component.translatable(
                            LangDefs.IMAGE_UPLOAD_OK.getTranslationKey(),
                            img.getWidth(),
                            img.getHeight()
                    ),
                    0xFF55FF55,
                    true
            );
        } catch (Throwable ignored) {
            return new Result(
                    Component.translatable(LangDefs.IMAGE_UPLOAD_FAILED.getTranslationKey()),
                    0xFFFF5555,
                    false
            );
        }
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
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
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