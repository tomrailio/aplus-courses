package fi.aalto.cs.apluscourses.ui.ideactivities;

import com.intellij.util.ui.JBUI;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import fi.aalto.cs.apluscourses.ui.GuiObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BalloonPopup extends JPanel {
  private final @NotNull Component anchorComponent;

  @GuiObject
  private final JPanel titlePanel;

  @GuiObject
  private final BalloonLabel titleLabel;

  @GuiObject
  private final BalloonLabel messageLabel;

  private float transparencyCoefficient;

  /**
   * Creates a popup with the given text. The popup is permanently attached to the specified
   * component. Optionally, an icon can be provided which will be displayed to the left of
   * the popup's title.
   */
  public BalloonPopup(@NotNull Component anchorComponent, @NotNull String title,
                      @NotNull String message, @Nullable Icon icon) {
    this.anchorComponent = anchorComponent;

    setOpaque(false);
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(new EmptyBorder(JBUI.insets(0, 5, 10, 5)));

    // introduce a limit to the popup's width (so it doesn't take the entire screen width)
    setMaximumSize(new Dimension(500, 0));

    titleLabel = new BalloonLabel("<html><h1>" + title + "</h1></html>");
    titleLabel.setIcon(icon);

    titlePanel = new JPanel();
    titlePanel.setOpaque(false);
    titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));
    titlePanel.setAlignmentX(LEFT_ALIGNMENT);
    titlePanel.add(titleLabel);
    add(titlePanel);

    messageLabel = new BalloonLabel("<html>" + message + "</html>");
    messageLabel.setAlignmentX(LEFT_ALIGNMENT);
    add(messageLabel);

    setTransparencyCoefficient(0.3f);
    recalculateBounds();
  }

  @Override
  public boolean isVisible() {
    return anchorComponent.isShowing();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    Graphics g2 = g.create();

    var bgColor = getBackground();
    var newColor = new Color(
        bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(),
        (int) (transparencyCoefficient * 255));

    g2.setColor(newColor);
    Rectangle drawBounds = g2.getClipBounds();
    g2.fillRect(drawBounds.x, drawBounds.y, drawBounds.width, drawBounds.height);

    g2.dispose();
  }

  /**
   * Sets the popup transparency level.
   * @param coefficient The transparency coefficient - a value between 0.0f and 1.0f.
   *                    0.0f means completely transparent, 1.0f means completely opaque.
   */
  public void setTransparencyCoefficient(float coefficient) {
    transparencyCoefficient = coefficient;
    titleLabel.setTransparencyCoefficient(coefficient);
    messageLabel.setTransparencyCoefficient(coefficient);
  }

  /**
   * Recomputes the popups bounds and triggers a reposition if needed. Should ideally be called
   * every time anything changes in the parent frame.
   */
  public void recalculateBounds() {
    // the origin of the component that this popup is attached to must be converted to the
    // overlay pane's coordinate system, because that overlay uses a null layout and requires
    // that this popup specify its bounds
    var componentWindowPos = SwingUtilities.convertPoint(anchorComponent, 0, 0, getParent());

    var minSize = getMinimumSize();

    var windowSize = JOptionPane.getRootFrame().getSize();
    var componentSize = anchorComponent.getSize();

    int popupWidth = minSize.width + 20;
    int popupHeight = minSize.height;

    int availableSizeLeft = componentWindowPos.x;
    int availableSizeRight = windowSize.width - (componentWindowPos.x + componentSize.width);
    int availableSizeTop = componentWindowPos.y;
    int availableSizeBottom = windowSize.height - (componentWindowPos.y + componentSize.height);

    int mostHorizontalSpace = Integer.max(availableSizeLeft, availableSizeRight);
    int mostVerticalSpace = Integer.max(availableSizeTop, availableSizeBottom);

    int popupX;
    int popupY;

    if (mostHorizontalSpace < popupWidth && mostVerticalSpace < popupHeight) {
      popupX = componentWindowPos.x + componentSize.width - popupWidth - 20;
      popupY = componentWindowPos.y + 20;

      setTransparencyCoefficient(0.4f);
    } else {
      boolean positionHorizontally = mostHorizontalSpace > mostVerticalSpace;

      if (positionHorizontally) {
        if (availableSizeRight > availableSizeLeft) {
          popupX = componentWindowPos.x + anchorComponent.getWidth() + 5;
        } else {
          popupX = componentWindowPos.x - popupWidth - 5;
        }

        popupY = componentWindowPos.y + (anchorComponent.getHeight() - popupHeight) / 2;
      } else {
        if (availableSizeBottom > availableSizeTop) {
          popupY = componentWindowPos.y + anchorComponent.getHeight() + 5;
        } else {
          popupY = componentWindowPos.y - popupHeight - 5;
        }

        popupX = componentWindowPos.x + (anchorComponent.getWidth() - popupWidth) / 2;
      }

      setTransparencyCoefficient(1.0f);
    }

    setBounds(popupX, popupY, popupWidth, popupHeight);
  }
}
