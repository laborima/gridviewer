package io.github.laborima.gridviewer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.laborima.gridviewer.data.TestFiles;

class RenderCommandTest {

    @TempDir
    Path dir;

    @Test
    void rendersAPngReadingOnlyWhatTheImageNeeds() throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path nc = TestFiles.regular(dir);
        Path png = dir.resolve("out.png");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int code = RenderCommand.run(new String[] {nc.toString(), "-v", "temp", "-z", "2", "-W", "30", "--no-legend",
                "-o", png.toString()}, new PrintStream(buf, true, StandardCharsets.UTF_8));
        assertEquals(0, code);
        String out = buf.toString(StandardCharsets.UTF_8);
        assertTrue(out.contains("stride 2"), out);
        BufferedImage img = ImageIO.read(png.toFile());
        assertEquals(30, img.getWidth());
        assertEquals(20, img.getHeight());
    }

    @Test
    void infoListsVariables() throws Exception {
        Path nc = TestFiles.nemo(dir);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        assertEquals(0, InfoCommand.info(new String[] {nc.toString()}, new PrintStream(buf, true, StandardCharsets.UTF_8)));
        String out = buf.toString(StandardCharsets.UTF_8);
        assertTrue(out.contains("votemper"), out);
        assertTrue(out.contains("curvilinear grid"), out);
    }
}
