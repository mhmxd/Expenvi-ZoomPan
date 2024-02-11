package tool;

import com.github.weisj.jsvg.attributes.paint.PaintParser;
import com.github.weisj.jsvg.attributes.paint.SVGPaint;
import com.github.weisj.jsvg.attributes.paint.SimplePaintSVGPaint;
import com.github.weisj.jsvg.parser.AttributeNode;
import com.github.weisj.jsvg.parser.DefaultParserProvider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;

import static tool.Constants.*;

public class ZoomParserProvider extends DefaultParserProvider {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    private final ArrayList<MoCoord> targetCoords;
    private final ArrayList<MoCoord> errorCoords;

    public ZoomParserProvider(ArrayList<MoCoord> targetCoords, ArrayList<MoCoord> errorCoords) {
        this.targetCoords = new ArrayList<>(targetCoords);
        this.errorCoords = new ArrayList<>(errorCoords);
    }

    @Override
    public @NotNull PaintParser createPaintParser() {
        return new ZoomPaintParser(super.createPaintParser(), targetCoords, errorCoords);
    }

}

class ZoomPaintParser implements PaintParser {
    private final TaggedLogger conLog = Logger.tag(getClass().getSimpleName());

    private final PaintParser delegate;
    private final ArrayList<MoCoord> targetCoords;
    private final ArrayList<MoCoord> errorCoords;

    private final SimplePaintSVGPaint TARGET_PAINT = new SimplePaintSVGPaint() {
        @Override
        public @NotNull Paint paint() {
            return COLORS.GREEN;
        }
    };

    private final SimplePaintSVGPaint ERROR_PAINT = new SimplePaintSVGPaint() {
        @Override
        public @NotNull Paint paint() {
            return COLORS.BLACK;
        }
    };

    public ZoomPaintParser(PaintParser delegate, ArrayList<MoCoord> targetCoords, ArrayList<MoCoord> errorCoords) {
        conLog.trace("Construct");
        this.delegate = delegate;
        this.targetCoords = new ArrayList<>(targetCoords);
        this.errorCoords = new ArrayList<>(errorCoords);
    }


    @Override
    public @Nullable Color parseColor(@NotNull String value, @NotNull AttributeNode attributeNode) {
        return delegate.parseColor(value, attributeNode);
    }

    @Override
    public @Nullable SVGPaint parsePaint(@Nullable String value, @NotNull AttributeNode attributeNode) {

        SVGPaint paint = delegate.parsePaint(value, attributeNode);

        // Target painting
        for (MoCoord coord : targetCoords) {
            if (Objects.equals(attributeNode.getValue("id"), coord.formatRowCol())) {
                paint = TARGET_PAINT;
            }
        }

        // Error painting
        for (MoCoord coord : errorCoords) {
            if (Objects.equals(attributeNode.getValue("id"), coord.formatRowCol())) {
                paint = ERROR_PAINT;
            }
        }

        return paint;
    }

}
