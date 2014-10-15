package graphics.actionListeners;

import processing.RecognitionMachine;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.ParseException;

public class WSizeTextFieldListener implements ActionListener, FocusListener
{
    private RecognitionMachine recogMachine;

    public WSizeTextFieldListener(RecognitionMachine recogMachine)
    {
        this.recogMachine = recogMachine;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        final int value = ((Number)((JFormattedTextField)e.getSource()).getValue()).intValue();
        new Thread(new Runnable() {
            @Override
            public void run() {
                recogMachine.setWindowSize(value);
            }
        }).start();
    }

    @Override
    public void focusLost(FocusEvent e)
    {
        // In order that TAB focus loss takes effect too
        try { ((JFormattedTextField)e.getSource()).commitEdit(); }
        catch (ParseException e1) { e1.printStackTrace(); }

        actionPerformed(new ActionEvent(e.getSource(), 0, ""));
    }

    @Override
    public void focusGained(FocusEvent e) { }
}
