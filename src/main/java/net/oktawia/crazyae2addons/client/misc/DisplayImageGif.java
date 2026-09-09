package net.oktawia.crazyae2addons.client.misc;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;

import org.w3c.dom.Node;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.oktawia.crazyae2addons.CrazyAddons;

@OnlyIn(Dist.CLIENT)
public final class DisplayImageGif {

    private static final int MAX_KEPT_FRAME_SIZE = 512;

    private static final int MIN_FRAME_MS = 20;
    private static final int MAX_FRAME_MS = 2000;
    private static final int DEFAULT_FRAME_MS = 100;

    public record Animation(List<BufferedImage> frames, int frameDurationMs) {
    }

    private record FrameInfo(int left, int top, int delayMs, String disposal) {
    }

    private DisplayImageGif() {
    }

    @Nullable
    public static Animation read(File file, int maxFrames) {
        ImageReader reader = null;

        try (ImageInputStream stream = ImageIO.createImageInputStream(file)) {
            if (stream == null) {
                return null;
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                return null;
            }

            reader = readers.next();
            reader.setInput(stream);

            int sourceFrames = reader.getNumImages(true);
            if (sourceFrames <= 1) {
                return null;
            }

            return composite(reader, sourceFrames, Math.max(1, maxFrames));
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to read animated image", e);
            return null;
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

    private static Animation composite(ImageReader reader, int sourceFrames, int maxFrames) throws Exception {
        int canvasWidth = reader.getWidth(0);
        int canvasHeight = reader.getHeight(0);

        for (int i = 1; i < sourceFrames; i++) {
            FrameInfo info = readFrameInfo(reader, i);
            canvasWidth = Math.max(canvasWidth, info.left() + reader.getWidth(i));
            canvasHeight = Math.max(canvasHeight, info.top() + reader.getHeight(i));
        }

        int keptFrames = Math.min(sourceFrames, maxFrames);
        List<BufferedImage> frames = new ArrayList<>(keptFrames);

        BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
        long totalDelayMs = 0L;

        for (int i = 0; i < sourceFrames; i++) {
            FrameInfo info = readFrameInfo(reader, i);
            BufferedImage source = reader.read(i);

            BufferedImage restorePoint = "restoreToPrevious".equals(info.disposal()) ? copyOf(canvas) : null;

            Graphics2D graphics = canvas.createGraphics();
            try {
                graphics.drawImage(source, info.left(), info.top(), null);
            } finally {
                graphics.dispose();
            }

            totalDelayMs += info.delayMs();

            if (isKept(i, sourceFrames, keptFrames)) {
                frames.add(scaledCopyOf(canvas));
            }

            if (restorePoint != null) {
                canvas = restorePoint;
            } else if ("restoreToBackgroundColor".equals(info.disposal())) {
                clearRect(canvas, info.left(), info.top(), source.getWidth(), source.getHeight());
            }
        }

        if (frames.size() <= 1) {
            return null;
        }

        int frameDurationMs = (int) Math.round(totalDelayMs / (double) frames.size());
        return new Animation(frames, Math.max(MIN_FRAME_MS, Math.min(MAX_FRAME_MS, frameDurationMs)));
    }

    private static boolean isKept(int index, int sourceFrames, int keptFrames) {
        if (keptFrames >= sourceFrames) {
            return true;
        }

        int slot = (int) ((long) index * keptFrames / sourceFrames);
        int previousSlot = index == 0 ? -1 : (int) ((long) (index - 1) * keptFrames / sourceFrames);
        return slot != previousSlot;
    }

    private static FrameInfo readFrameInfo(ImageReader reader, int index) throws Exception {
        IIOMetadata metadata = reader.getImageMetadata(index);
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_gif_image_1.0");

        int left = 0;
        int top = 0;
        int delayMs = DEFAULT_FRAME_MS;
        String disposal = "none";

        IIOMetadataNode descriptor = firstChild(root, "ImageDescriptor");
        if (descriptor != null) {
            left = parseInt(descriptor.getAttribute("imageLeftPosition"), 0);
            top = parseInt(descriptor.getAttribute("imageTopPosition"), 0);
        }

        IIOMetadataNode control = firstChild(root, "GraphicControlExtension");
        if (control != null) {
            int hundredths = parseInt(control.getAttribute("delayTime"), 10);
            delayMs = hundredths <= 1 ? DEFAULT_FRAME_MS : hundredths * 10;

            String method = control.getAttribute("disposalMethod");
            if (method != null && !method.isBlank()) {
                disposal = method;
            }
        }

        return new FrameInfo(left, top, delayMs, disposal);
    }

    @Nullable
    private static IIOMetadataNode firstChild(IIOMetadataNode root, String name) {
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof IIOMetadataNode node && name.equals(node.getNodeName())) {
                return node;
            }
        }
        return null;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static BufferedImage scaledCopyOf(BufferedImage source) {
        int longestSide = Math.max(source.getWidth(), source.getHeight());

        if (longestSide <= MAX_KEPT_FRAME_SIZE) {
            return copyOf(source);
        }

        double scale = MAX_KEPT_FRAME_SIZE / (double) longestSide;
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));

        BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private static BufferedImage copyOf(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private static void clearRect(BufferedImage canvas, int x, int y, int width, int height) {
        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(x, y, width, height);
        } finally {
            graphics.dispose();
        }
    }
}
