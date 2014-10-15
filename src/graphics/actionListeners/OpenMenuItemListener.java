package graphics.actionListeners;

import graphics.ControlPanel;
import processing.RecognitionMachine;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

public class OpenMenuItemListener implements ActionListener
{
    private RecognitionMachine recogMachine;
    private ControlPanel       controlPanel;
    private JMenuItem          saveMenuItem;

    public OpenMenuItemListener(RecognitionMachine recogMachine, ControlPanel controlPanel, JMenuItem saveMenuItem)
    {
        this.recogMachine = recogMachine;
        this.controlPanel = controlPanel;
        this.saveMenuItem = saveMenuItem;
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

        try
        {
            //Create a file chooser
            final JFileChooser fc = new JFileChooser();
            fc.setMultiSelectionEnabled(false);
            fc.setCurrentDirectory(new File("../"));
            fc.setFileFilter(new FileNameExtensionFilter("WAV Audio files (*.wav)", "wav"));

            int returnVal = fc.showOpenDialog(controlPanel);
            if (returnVal == JFileChooser.APPROVE_OPTION)
            {
                recogMachine.setFilePath(fc.getSelectedFile().getAbsolutePath());
                controlPanel.setFileNameText(fc.getSelectedFile().getName());
                saveMenuItem.setEnabled(true);
            }
        }
        catch (UnsupportedAudioFileException e1)
        {
            JOptionPane.showMessageDialog(null, "You've chosen an unsupported audio file.\nOnly WAV is appropriate.", "Error", JOptionPane.ERROR_MESSAGE);
            e1.printStackTrace();
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null, "File not found", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
