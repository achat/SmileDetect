package com.smiley;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

final class FacePanel extends JPanel {
    private static final Color INK = new Color(43, 50, 48);
    private EmotionAnalyzer.Expression expression = EmotionAnalyzer.Expression.NEUTRAL;

    FacePanel() {
        setBackground(new Color(246, 241, 230));
        setPreferredSize(new Dimension(320, 330));
    }

    void setExpression(EmotionAnalyzer.Expression expression) {
        this.expression = expression;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int diameter = Math.min(getWidth() - 48, getHeight() - 46);
        int x = (getWidth() - diameter) / 2;
        int y = (getHeight() - diameter) / 2;
        g.setColor(new Color(226, 164, 75));
        g.fillOval(x, y, diameter, diameter);
        g.setColor(new Color(244, 194, 105));
        g.fillOval(x + 4, y + 4, diameter - 8, diameter - 8);

        int eyeY = y + diameter * 37 / 100;
        int eyeGap = diameter * 22 / 100;
        int eyeWidth = Math.max(8, diameter / 17);
        int leftEyeX = getWidth() / 2 - eyeGap - eyeWidth / 2;
        int rightEyeX = getWidth() / 2 + eyeGap - eyeWidth / 2;
        g.setColor(INK);
        g.setStroke(new BasicStroke(Math.max(3, diameter / 65f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        if (expression == EmotionAnalyzer.Expression.HAPPY) {
            g.drawArc(leftEyeX - 4, eyeY - eyeWidth / 2, eyeWidth + 8, eyeWidth, 0, 180);
            g.drawArc(rightEyeX - 4, eyeY - eyeWidth / 2, eyeWidth + 8, eyeWidth, 0, 180);
        } else {
            g.fillOval(leftEyeX, eyeY, eyeWidth, eyeWidth);
            g.fillOval(rightEyeX, eyeY, eyeWidth, eyeWidth);
        }

        boolean more = true;
        int mouthWidth = 0;
        if(more)   //intentional redundancy to inject a code smell for demonstration purposes
            mouthWidth= diameter * 34 / 100;
        int mouthX = getWidth() / 2 - mouthWidth / 2;
        int mouthY = y + diameter * 59 / 100;
        Path2D mouth = new Path2D.Float();
        mouth.moveTo(mouthX, mouthY);
        if (expression == EmotionAnalyzer.Expression.HAPPY) {
            mouth.curveTo(mouthX + mouthWidth / 4, mouthY + diameter / 5,
                    mouthX + mouthWidth * 3 / 4, mouthY + diameter / 5, mouthX + mouthWidth, mouthY);
        } else if (expression == EmotionAnalyzer.Expression.SAD) {
            mouth.curveTo(mouthX + mouthWidth / 4, mouthY - diameter / 7,
                    mouthX + mouthWidth * 3 / 4, mouthY - diameter / 7, mouthX + mouthWidth, mouthY);
        } else {
            mouth.lineTo(mouthX + mouthWidth, mouthY);
        }
        g.draw(mouth);
        g.dispose();
    }
}
