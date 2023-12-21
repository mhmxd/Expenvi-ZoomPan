package model;

import ui.TrialPanel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class PanTrial extends BaseTrial {
    public URI uri;
    public Integer rotation;

    public PanTrial(int level, Integer rotation) {
        super("Panning", level);

        try {
            switch (level) {
                case 1 -> this.uri = Objects.requireNonNull(TrialPanel.class.getResource("resources/curve1.svg")).toURI();
                case 2 -> this.uri = Objects.requireNonNull(TrialPanel.class.getResource("resources/curve2.svg")).toURI();
                case 3 -> this.uri = Objects.requireNonNull(TrialPanel.class.getResource("resources/curve3.svg")).toURI();
            }
        } catch (URISyntaxException ignored) {
        }

        this.rotation = rotation;
    }
}
