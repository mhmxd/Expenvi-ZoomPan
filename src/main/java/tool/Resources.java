package tool;

import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class Resources {

    public static class SVG {
        private static final TaggedLogger conLog = Logger.tag(SVG.class.getSimpleName());

        public static URI ZOOM_IN_URI = URI.create("");
        public static URI ZOOM_OUT_URI = URI.create("");

        public static URI PAN_LVL1_URI = URI.create("");
        public static URI PAN_LVL2_URI = URI.create("");
        public static URI PAN_LVL3_URI = URI.create("");

        static {
            try {
                // Load the SVG resources
                ZOOM_IN_URI = Objects.requireNonNull(SVG.class.getResource("/zoom_in.svg")).toURI();
                ZOOM_OUT_URI = Objects.requireNonNull(SVG.class.getResource("/zoom_out.svg")).toURI();

                PAN_LVL1_URI = Objects.requireNonNull(SVG.class.getResource("/curve1.svg")).toURI();
                PAN_LVL2_URI = Objects.requireNonNull(SVG.class.getResource("/curve2.svg")).toURI();
                PAN_LVL3_URI = Objects.requireNonNull(SVG.class.getResource("/curve3.svg")).toURI();

            } catch(URISyntaxException ignored) {
                conLog.error("Could not load the SVGs");
            }
        }
    }

}
