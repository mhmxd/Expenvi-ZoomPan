package tool;

import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import java.awt.*;
import java.io.*;

import static tool.Constants.*;

public class MoSVG {
    static final TaggedLogger conLog = Logger.tag(MoSVG.class.getSimpleName());

    public static void genCircleGrid(String svgFileName, int nRows, int diam, int gutter, Color color) {
        // Init calc
        int width = nRows * (diam + gutter) - gutter;
        String hex = COLORS.getHex(color);
        int rad = diam / 2;

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
                    final String id = String.format("r%d_c%d", j, i);
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

    public static void genRectGrid(String svgFileName, int nRows, int size, int rx, int gutter, Color color) {
        // Consts
        final String width = String.format("%d", nRows * (size + gutter) - gutter);
        final String widthStr = String.format("\"%s\"", width);
        final String viewBoxStr = String.format("\"0 0 %s %s\"", width, width);
        final String noneStr = String.format("\"%s\"", "none");
        final String xmlnsStr = String.format("\"%s\"", "http://www.w3.org/2000/svg");

        final String hexStr = String.format("\"%s\"", COLORS.getHex(color));
        final String wStr = String.format("\"%d\"", size);
        final String rxStr = String.format("\"%d\"", rx);

        // Open the file and write line by line
        try {
            final PrintWriter fileWriter = new PrintWriter(new FileOutputStream(svgFileName));

            // Write the svg tag (w = h)
            fileWriter.println("<svg" +
                    " width=" + widthStr + " height=" + widthStr +
                    " viewBox=" + viewBoxStr +
                    " fill=" + noneStr +
                    " xmlns= " + xmlnsStr +
                    " >");

            // Write the rectangls
            for (int i = 0; i < nRows; i++) {
                for (int j = 0; j < nRows; j++) {
                    // Calculate the x and y coordinates of the center of the circle
                    int x = i * (size + gutter);
                    int y = j * (size + gutter);

                    // Write the rectangle
                    final String idStr = String.format("\"r%d_c%d\"", j, i);
                    final String xStr = String.format("\"%d\"", x);
                    final String yStr = String.format("\"%d\"", y);

                    fileWriter.println("<rect" +
                            " id=" + idStr +
                            " x=" + xStr + " y=" + yStr +
                            " width=" + wStr + " height= " + wStr +
                            " rx=" + rxStr + " ry= " + rxStr +
                            " fill=" + hexStr +
                            " />");
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
