package graphics.actionListeners;

import graphics.ControlPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StopBtnListener implements ActionListener
{
    private ControlPanel controlPanel;

    public StopBtnListener(ControlPanel controlPanel)
    {
        this.controlPanel = controlPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Thread recogMachineThread = controlPanel.getRecogMachineThread();
        if (recogMachineThread != null)
        {
            recogMachineThread.interrupt();
            try {recogMachineThread.join();}
            catch (InterruptedException e1){e1.printStackTrace();}
        }
    }
}
