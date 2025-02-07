package fi.aalto.cs.apluscourses.ui.ideactivities;

import com.intellij.util.concurrency.annotations.RequiresEdt;
import fi.aalto.cs.apluscourses.utils.Event;
import icons.PluginIcons;
import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import org.jetbrains.annotations.NotNull;

public class OverlayPane extends JPanel implements AWTEventListener {
  // A high value (> 500) allows us to place the overlay pretty much above every other component
  private static final int PANE_Z_ORDER = 20000;

  private final JRootPane associatedRootPane;
  private final Set<GenericHighlighter> highlighters = new HashSet<>();
  private final Set<BalloonPopup> balloonPopups = new HashSet<>();

  private static OverlayPane overlayInstance;

  public final Event clickEvent = new Event();

  private void revalidatePane() {
    getRootPane().revalidate();
    getRootPane().repaint();
  }

  @RequiresEdt
  private Area getDimmedArea() {
    var overlayArea = new Area(new Rectangle(0, 0, getWidth(), getHeight()));

    for (var c : highlighters) {
      var posDiff = SwingUtilities.convertPoint(c.getComponent(), 0, 0, this);
      var translation = AffineTransform.getTranslateInstance(posDiff.x, posDiff.y);

      if (c.getComponent().isShowing()) {
        for (var rectangle : c.getArea()) {
          var rectangleArea = new Area(rectangle);
          rectangleArea.transform(translation);

          overlayArea.subtract(rectangleArea);
        }
      }
    }

    for (var c : balloonPopups) {
      // popups are already places in the overlay's coordinate system
      if (c.isVisible()) {
        final double borderWidth = BalloonPopup.BORDER_WIDTH * 2.0;
        final double borderHeight = BalloonPopup.BORDER_WIDTH * 2.0;

        var componentRect = new RoundRectangle2D.Double(
            c.getX() + (double) BalloonPopup.BORDER_WIDTH,
            c.getY() + (double) BalloonPopup.BORDER_WIDTH,
            c.getWidth() - borderWidth, c.getHeight() - borderHeight, 6, 6
        );

        overlayArea.subtract(new Area(componentRect));
      }
    }

    return overlayArea;
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);

    for (var c : balloonPopups) {
      c.recalculateBounds();
    }

    // use Graphics's copy so as not to influence subsequent calls using the same Graphics object
    Graphics2D g = (Graphics2D) graphics.create();
    g.setComposite(AlphaComposite.SrcOver.derive(0.7f));
    g.setColor(Color.BLACK); // using JBColor.BLACK is wrong here
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.fill(getDimmedArea());
  }

  @Override
  public JRootPane getRootPane() {
    return associatedRootPane;
  }

  @Override
  public int getWidth() {
    return associatedRootPane.getLayeredPane().getWidth();
  }

  @Override
  public int getHeight() {
    return associatedRootPane.getLayeredPane().getHeight();
  }

  @Override
  public Dimension getSize() {
    return new Dimension(getWidth(), getHeight());
  }

  @Override
  public Dimension getPreferredSize() {
    return getSize();
  }

  @Override
  public Dimension getMinimumSize() {
    return getSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return getSize();
  }

  @Override
  public boolean isOpaque() {
    return false;
  }

  @Override
  public boolean isVisible() {
    return true;
  }

  /**
   * Installs the overlay.
   */
  @RequiresEdt
  public static OverlayPane installOverlay() {
    if (overlayInstance != null) {
      return overlayInstance;
    }

    var overlay = new OverlayPane();
    overlay.getRootPane().getLayeredPane().add(overlay, PANE_Z_ORDER);
    overlay.revalidatePane();

    Toolkit.getDefaultToolkit().addAWTEventListener(overlay,
        AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK);

    overlayInstance = overlay;
    return overlay;
  }

  /**
   * Removes the overlay.
   */
  @RequiresEdt
  public void remove() {
    Toolkit.getDefaultToolkit().removeAWTEventListener(this);

    this.getRootPane().getLayeredPane().remove(this);
    for (var c : this.balloonPopups) {
      this.getRootPane().getLayeredPane().remove(c);
    }

    this.revalidatePane();
    overlayInstance = null;
  }

  /**
   * Marks a component not to be dimmed.
   */
  public void addHighlighter(@NotNull GenericHighlighter highlighter) {
    this.highlighters.add(highlighter);
    this.revalidatePane();
  }

  public boolean hasHighlighterForComponent(@NotNull Component component) {
    return this.highlighters.stream().anyMatch(highlighter -> highlighter.getComponent().equals(component));
  }

  /**
   * Adds a popup to a specified component.
   */
  @RequiresEdt
  public void addPopup(@NotNull Component c, @NotNull String title,
                       @NotNull String message, @NotNull Action @NotNull [] actions, boolean isAlreadyCompleted) {
    var popup = new BalloonPopup(c, title, message,
        isAlreadyCompleted ? PluginIcons.A_PLUS_FULL_POINTS : PluginIcons.A_PLUS_OPTIONAL_PRACTICE, actions);
    this.balloonPopups.add(popup);
    this.getRootPane().getLayeredPane().add(popup, PANE_Z_ORDER + 1);
    this.revalidatePane();
  }

  /**
   * Resets the overlay to its original state, i.e. removes all popups and dims all components.
   */
  @RequiresEdt
  public void reset() {
    this.highlighters.clear();
    for (var c : this.balloonPopups) {
      this.getRootPane().getLayeredPane().remove(c);
    }
    this.balloonPopups.clear();
    this.revalidatePane();
  }

  private OverlayPane() {
    associatedRootPane = ((JFrame) JOptionPane.getRootFrame()).getRootPane();
    setLayout(null);
  }

  @Override
  public void eventDispatched(AWTEvent event) {
    var mouseEvent = (MouseEvent) event;
    var source = (Component) mouseEvent.getSource();
    if (!getRootPane().getContentPane().isAncestorOf(source)) {
      // don't process events from context menus, pop-up windows etc.
      // these are not subject to dimming in the first place - only the main content pane is dimmed
      return;
    }

    var windowEventPos = SwingUtilities.convertPoint(source, mouseEvent.getX(), mouseEvent.getY(), this);
    if (getDimmedArea().contains(windowEventPos)) {
      mouseEvent.consume();
      if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
        clickEvent.trigger();
      }
    }
  }
}
