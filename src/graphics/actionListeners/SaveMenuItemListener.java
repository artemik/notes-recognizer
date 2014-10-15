package graphics.actionListeners;

import graphics.ControlPanel;
import processing.RecognitionMachine;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class SaveMenuItemListener implements ActionListener
{
    private RecognitionMachine recogMachine;
    private ControlPanel controlPanel;

    public SaveMenuItemListener(RecognitionMachine recogMachine, ControlPanel controlPanel)
    {
        this.recogMachine = recogMachine;
        this.controlPanel = controlPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (!recogMachine.hasInputFile())
        {
            JOptionPane.showMessageDialog(null, "No file was opened. There is nothing to save.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        else
        {
            Thread recogMachineThread = controlPanel.getRecogMachineThread();
            if (recogMachineThread != null)
            {
                recogMachineThread.interrupt();
                try {recogMachineThread.join();}
                catch (InterruptedException e1){e1.printStackTrace();}
            }

            //Create a file chooser
            final JFileChooser fc = new JFileChooser();
            fc.setMultiSelectionEnabled(false);
            fc.setCurrentDirectory(new File("../"));
            fc.setFileFilter(new FileNameExtensionFilter("MIDI files (*.midi)", "midi"));

            int returnVal = fc.showSaveDialog(controlPanel);
            if (returnVal == JFileChooser.APPROVE_OPTION)
            {
                final StringBuilder outputFileFullPath = new StringBuilder(fc.getSelectedFile().getAbsolutePath());
                if (!outputFileFullPath.substring(outputFileFullPath.length() - ".midi".length(), outputFileFullPath.length()).equals(".midi"))
                    outputFileFullPath.append(".midi");

                final ProgressMonitor pg = new ProgressMonitor(null, "Saving to " + outputFileFullPath, "", 0, 100);
                pg.setProgress(0);
                new Thread(new Runnable() {
                    @Override
                    public void run() { recogMachine.startSaving2(outputFileFullPath.toString(), pg); }
                }).start();
            }
        }
    }
}

