package graphics;

import graphics.actionListeners.GraphicTypeBoxListener;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.locks.ReentrantLock;

public class GraphicTypePanel extends JPanel
{
    private JComboBox graphicTypeBox;

    public GraphicTypePanel(GraphicPanel graphicPanel, ReentrantLock recogParamChangesLock)
    {
        // Graphic type box
        graphicTypeBox = new JComboBox(new String[]{"dB", "Real"});
        graphicTypeBox.addActionListener(new GraphicTypeBoxListener(graphicPanel, recogParamChangesLock));
        graphicTypeBox.setPreferredSize(new Dimension(60, 23));

        // Setting layout
        setLayout(new BorderLayout());
        add(graphicTypeBox, BorderLayout.LINE_END);
    }
}
