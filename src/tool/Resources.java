package tool;

import org.tinylog.Logger;
import org.tinylog.TaggedLogger;
import ui.TrialPanel;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

public class Resources {

    public static class SVG {
        private static final TaggedLogger conLog = Logger.tag(SVG.class.getSimpleName());

        public static URI ZOOM_IN_URI = URI.create("");
        public static URI ZOOM_OUT_URI = URI.create("");

        static {
            try {
                // Load the SVG resources
                ZOOM_IN_URI = Objects.requireNonNull(SVG.class.getResource("/zoom_in.svg")).toURI();
                ZOOM_OUT_URI = Objects.requireNonNull(SVG.class.getResource("/zoom_out.svg")).toURI();
            } catch(URISyntaxException ignored) {
                conLog.error("Could not load the SVGs");
            }
        }
    }
}
