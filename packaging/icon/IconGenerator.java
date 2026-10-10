import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Draws the Grid Viewer icon: a gridded globe with sea-surface-temperature bands.
 *
 * <pre>java packaging/icon/IconGenerator.java OUTPUT_DIR</pre>
 */
public class IconGenerator {

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : ".");
        dir.mkdirs();
        for (int size : new int[] {16, 32, 48, 64, 128, 256, 512, 1024}) {
            ImageIO.write(draw(size), "png", new File(dir, "gridviewer-" + size + ".png"));
        }
    }

    static BufferedImage draw(int size) {
        // Draw large and downsample, so small sizes stay crisp.
        int big = Math.max(size, 512) * 2;
        BufferedImage img = new BufferedImage(big, big, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        double s = big;

        // Rounded tile.
        RoundRectangle2D tile = new RoundRectangle2D.Double(s * 0.04, s * 0.04, s * 0.92, s * 0.92, s * 0.22, s * 0.22);
        g.setPaint(new GradientPaint(0, (float) (s * 0.04), new Color(0x123247), 0, (float) (s * 0.96), new Color(0x081119)));
        g.fill(tile);

        // Globe with temperature bands: cold poles, warm equator.
        double r = s * 0.34;
        double cx = s * 0.5;
        double cy = s * 0.5;
        Ellipse2D globe = new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
        g.setClip(globe);
        float[] f = {0f, 0.16f, 0.32f, 0.44f, 0.5f, 0.56f, 0.68f, 0.84f, 1f};
        Color[] c = {new Color(0x0b1f3a), new Color(0x2c3d8f), new Color(0x7a3f9a), new Color(0xe0704f),
                new Color(0xf4c64e), new Color(0xe0704f), new Color(0x7a3f9a), new Color(0x2c3d8f), new Color(0x0b1f3a)};
        g.setPaint(new LinearGradientPaint(new Point2D.Double(cx, cy - r), new Point2D.Double(cx, cy + r), f, c));
        g.fill(globe);

        // Grid lines: parallels and meridians of a sphere seen slightly from above.
        g.setColor(new Color(255, 255, 255, 120));
        g.setStroke(new BasicStroke((float) (s * 0.008)));
        for (int k = -2; k <= 2; k++) {
            double y = cy + r * Math.sin(Math.toRadians(k * 30));
            double half = r * Math.cos(Math.toRadians(k * 30));
            g.draw(new Line2D.Double(cx - half, y, cx + half, y));
        }
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(15 + k * 30);
            double w = Math.abs(r * Math.cos(a));
            g.draw(new Ellipse2D.Double(cx - w, cy - r, 2 * w, 2 * r));
        }

        // Spherical shading.
        g.setPaint(new RadialGradientPaint(new Point2D.Double(cx - r * 0.35, cy - r * 0.4), (float) (r * 1.5),
                new float[] {0f, 0.55f, 1f},
                new Color[] {new Color(255, 255, 255, 70), new Color(255, 255, 255, 0), new Color(0, 0, 0, 150)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fill(globe);
        g.setClip(null);

        // Rim.
        g.setColor(new Color(0x4cc9f0));
        g.setStroke(new BasicStroke((float) (s * 0.014)));
        g.draw(globe);
        g.dispose();

        return downsample(img, size);
    }

    static BufferedImage downsample(BufferedImage src, int size) {
        BufferedImage cur = src;
        int w = src.getWidth();
        while (w / 2 >= size) {
            w /= 2;
            BufferedImage next = new BufferedImage(w, w, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = next.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setComposite(AlphaComposite.Src);
            g.drawImage(cur, 0, 0, w, w, null);
            g.dispose();
            cur = next;
        }
        if (w != size) {
            BufferedImage next = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = next.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(cur, 0, 0, size, size, null);
            g.dispose();
            cur = next;
        }
        return cur;
    }
}
