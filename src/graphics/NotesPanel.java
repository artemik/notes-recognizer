package graphics;

import javax.swing.*;
import java.awt.*;

public class NotesPanel extends JPanel
{
    private JLabel infoLabel;
    private JLabel notesLabel;

    public NotesPanel()
    {
        //Create the "cards".
        infoLabel = new JLabel("Notes currently being played:");
        JPanel card1 = new JPanel();
        card1.add(infoLabel);

        notesLabel = new JLabel("...");
        notesLabel.setFont(new Font("Tahoma", Font.PLAIN, 30));
        notesLabel.setForeground(Color.BLUE);
        //notesLabel.setPreferredSize(new Dimension(300, 40));

        JPanel card2 = new JPanel();
        card2.setPreferredSize(new Dimension(card2.getWidth(), 40));
        card2.setBorder(BorderFactory.createLineBorder(Color.black));
        card2.add(notesLabel);

        //Create the panel that contains the "cards".
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(card1);
        add(card2);
    }

    public void setNotesLabel(String text){ notesLabel.setText(text); }
}
