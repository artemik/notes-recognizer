package graphics.actionListeners;

import graphics.GraphicPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.locks.ReentrantLock;

public class GraphicTypeBoxListener implements ActionListener
{
    private GraphicPanel graphicPanel;
    private ReentrantLock recogParamChangesLock;

    public GraphicTypeBoxListener(GraphicPanel graphicPanel, ReentrantLock recogParamChangesLock)
    {
        this.graphicPanel = graphicPanel;
        this.recogParamChangesLock = recogParamChangesLock;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        final String selectedItem = (String) ((JComboBox)e.getSource()).getSelectedItem();
        new Thread(new Runnable() {
            @Override
            public void run() {
                recogParamChangesLock.lock();
                if (selectedItem == "dB")
                    graphicPanel.setDecibelView(true);
                else
                    graphicPanel.setDecibelView(false);
                recogParamChangesLock.unlock();
            }
        }).start();
    }
}
