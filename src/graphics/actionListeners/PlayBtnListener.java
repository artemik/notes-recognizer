package graphics.actionListeners;

import graphics.ControlPanel;
import processing.RecognitionMachine;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PlayBtnListener implements ActionListener
{
    private RecognitionMachine recogMachine;
    private ControlPanel controlPanel;

    public PlayBtnListener(RecognitionMachine recogMachine, ControlPanel controlPanel)
    {
        this.recogMachine = recogMachine;
        this.controlPanel = controlPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Thread recogMachineThread = controlPanel.getRecogMachineThread();
        if ((recogMachineThread == null) || (!recogMachineThread.isAlive()))
        {
            controlPanel.setRecogMachineThread(new Thread(new Runnable() {
                @Override
                public void run() {
                    recogMachine.startCalculation2();
                }
            }));
            controlPanel.getRecogMachineThread().start();
        }
    }

}
