package graphics;

import graphics.actionListeners.*;
import processing.RecognitionMachine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;

public class ControlPanel extends JPanel
{
    private final RecognitionMachine recogMachine;
    private Thread recogMachineThread;

    private JLabel fileNameLabel;
    private JLabel fileNameText;

    private JLabel              wSizeLabel;
    private JFormattedTextField wSizeText;

    private JLabel     thresholdLabel;
    private JFormattedTextField thresholdText;

    private final Font titleFont;
    private final Font itemFont;

    private JLabel    windowFunctionLabel;
    private JComboBox windowFunctionBox;

    private JButton   playBtn;
    private JButton   stopBtn;
    private ImageIcon btnPlayIcon;
    private ImageIcon btnStopIcon;

    public ControlPanel(RecognitionMachine rMachine)
    {
        // General settings
        this.recogMachine = rMachine;

        // Fonts
        titleFont = new Font("Tahoma", Font.BOLD, 12);
        itemFont = new Font("Tahoma", Font.PLAIN, 12);

        // File labels
        fileNameLabel = new JLabel("File name:");
        fileNameLabel.setFont(titleFont);
        fileNameText = new JLabel("...");
        fileNameText.setFont(itemFont);

        // Window functions labels
        windowFunctionLabel = new JLabel("Window function:");
        windowFunctionLabel.setFont(titleFont);
        windowFunctionBox = new JComboBox(new String[]{"Hamming", "Hann"});
        windowFunctionBox.addActionListener(new WindowFunctionBoxListener(recogMachine));
        windowFunctionBox.setPreferredSize(new Dimension(109, 23));

        // Window size labels
        wSizeLabel = new JLabel("Window size:");
        wSizeLabel.setFont(titleFont);
        wSizeText = new JFormattedTextField(NumberFormat.getInstance());
        wSizeText.setColumns(4);
        wSizeText.setValue(8192);
        WSizeTextFieldListener wSizeTextFieldListener = new WSizeTextFieldListener(recogMachine);
        wSizeText.addActionListener(wSizeTextFieldListener);
        wSizeText.addFocusListener(wSizeTextFieldListener);
        wSizeText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_ENTER) wSizeText.selectAll(); }
        });
        wSizeText.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { wSizeText.selectAll(); }
        });

        // Threshold labels
        thresholdLabel = new JLabel("Threshold:");
        thresholdLabel.setFont(titleFont);
        thresholdText = new JFormattedTextField(NumberFormat.getInstance());
        thresholdText.setColumns(4);
        thresholdText.setValue(2100);
        ThresholdTextFieldListener thresholdTextFieldListener = new ThresholdTextFieldListener(recogMachine);
        thresholdText.addActionListener(thresholdTextFieldListener);
        thresholdText.addFocusListener(thresholdTextFieldListener);
        thresholdText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {if (e.getKeyCode() == KeyEvent.VK_ENTER) thresholdText.selectAll(); }
        });
        thresholdText.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { thresholdText.selectAll(); }
        });

        // Buttons
        btnPlayIcon = new ImageIcon(getClass().getResource("/images/play.png"), "Play button");
        btnPlayIcon.setImage(btnPlayIcon.getImage().getScaledInstance(23, 23, 4));
        btnStopIcon = new ImageIcon(getClass().getResource("/images/stop.png"), "Stop button");
        btnStopIcon.setImage(btnStopIcon.getImage().getScaledInstance(23, 23, 4));
        playBtn = new JButton("", btnPlayIcon);
        playBtn.setPreferredSize(new Dimension(30, 30));
        playBtn.addActionListener(new PlayBtnListener(recogMachine, this));
        stopBtn = new JButton("", btnStopIcon);
        stopBtn.setPreferredSize(new Dimension(30, 30));
        stopBtn.addActionListener(new StopBtnListener(this));

        // Setting layout
        SpringLayout layout = new SpringLayout();
        Container contentPane = this;
        setLayout(layout);
        layout.putConstraint(SpringLayout.WEST, fileNameLabel, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, fileNameLabel, 5, SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.WEST, fileNameText, 15, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, fileNameText, 15, SpringLayout.NORTH, fileNameLabel);

        layout.putConstraint(SpringLayout.WEST, windowFunctionLabel, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, windowFunctionLabel, 20, SpringLayout.SOUTH, fileNameText);
        layout.putConstraint(SpringLayout.WEST, windowFunctionBox, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, windowFunctionBox, 4, SpringLayout.SOUTH, windowFunctionLabel);

        layout.putConstraint(SpringLayout.WEST, wSizeLabel, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, wSizeLabel, 20, SpringLayout.SOUTH, windowFunctionBox);
        layout.putConstraint(SpringLayout.WEST, wSizeText, 8, SpringLayout.EAST, wSizeLabel);
        layout.putConstraint(SpringLayout.NORTH, wSizeText, -3, SpringLayout.NORTH, wSizeLabel);

        layout.putConstraint(SpringLayout.WEST, thresholdLabel, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, thresholdLabel, 20, SpringLayout.SOUTH, wSizeText);
        layout.putConstraint(SpringLayout.WEST, thresholdText, 23, SpringLayout.EAST, thresholdLabel);
        layout.putConstraint(SpringLayout.NORTH, thresholdText, -3, SpringLayout.NORTH, thresholdLabel);

        layout.putConstraint(SpringLayout.WEST, playBtn, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, playBtn, 25, SpringLayout.SOUTH, thresholdText);
        layout.putConstraint(SpringLayout.WEST, stopBtn, 1, SpringLayout.EAST, playBtn);
        layout.putConstraint(SpringLayout.NORTH, stopBtn, 25, SpringLayout.SOUTH, thresholdText);

        add(fileNameLabel);
        add(fileNameText);
        add(windowFunctionLabel);
        add(windowFunctionBox);
        add(wSizeLabel);
        add(wSizeText);
        add(thresholdLabel);
        add(thresholdText);
        add(playBtn);
        add(stopBtn);
    }

    public Thread getRecogMachineThread()
    {
        return recogMachineThread;
    }

    public void setRecogMachineThread(Thread recogMachineThread)
    {
        this.recogMachineThread = recogMachineThread;
    }

    public void setFileNameText(String name){ fileNameText.setText(name); }

}
