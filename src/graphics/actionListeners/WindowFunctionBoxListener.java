package graphics.actionListeners;

import processing.RecognitionMachine;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WindowFunctionBoxListener implements ActionListener
{
    private RecognitionMachine recogMachine;

    public WindowFunctionBoxListener(RecognitionMachine recogMachine)
    {
        this.recogMachine = recogMachine;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        String selectedItem = (String) ((JComboBox)e.getSource()).getSelectedItem();
        recogMachine.setWindow(selectedItem);
    }
}
