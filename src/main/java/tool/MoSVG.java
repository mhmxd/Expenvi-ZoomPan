package tool;

import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import java.awt.*;
import java.io.*;

public class MoSVG {
    static final TaggedLogger conLog = Logger.tag(MoSVG.class.getSimpleName());

    public static void genCircleGrid(String svgFileName, int nRows, int diam, int gutter, Color color) {

        // Init calc
        int width = nRows * (diam + gutter) - gutter;
        String hex = "#" + Integer.toHexString(color.getRGB()).substring(2);
        int rad = diam / 2;
        conLog.info("Hex: {}", hex);

        // Open the file and write line by line
        try {
            final PrintWriter fileWriter = new PrintWriter(new FileOutputStream(svgFileName));

            // Write the svg tag (w = h)
            fileWriter.println("<svg " +
                    "width=\"" + width + "\" height=\"" + width + "\" " +
                    "viewBox=\"0 0 " + width + " " + width + "\" fill=\"none\" " +
                    "xmlns=\"http://www.w3.org/2000/svg\">");

            // Write the circles
            for (int i = 0; i < nRows; i++) {
                for (int j = 0; j < nRows; j++) {
                    // Calculate the x and y coordinates of the center of the circle
                    int cx = i * (diam + gutter) + rad;
                    int cy = j * (diam + gutter) + rad;

                    // Write the circle
                    final String id = String.format("r%d_c%d", j+1, i+1);
                    fileWriter.println("<circle " +
                            "id=\"" + id +
                            "\" cx=\"" + cx + "\" cy=\"" + cy + "\" " +
                            "r=\"" + rad + "\" " +
                            "fill=\"" + hex + "\" " +
                            "/>");
                }
            }

            // Write the closing tag
            fileWriter.println("</svg>");

            // Close the file
            fileWriter.flush();
            fileWriter.close();

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
