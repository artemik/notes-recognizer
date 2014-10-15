package graphics;

import graphics.actionListeners.OpenMenuItemListener;
import graphics.actionListeners.SaveMenuItemListener;
import processing.NotesProvider;
import processing.RecognitionMachine;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.locks.ReentrantLock;

public class MainFrame extends JFrame
{
    private JMenuBar                menuBar;
    private JMenu                   fileMenu;
    private JMenuItem               openMenuItem;
    private JMenuItem               saveMenuItem;

    private ControlPanel            controlPanel;
    private GraphicPanel            graphicPanel;
    private GraphicTypePanel        graphicTypePanel;
    private NotesPanel              notesPanel;
    private RecognitionMachine      recogMachine;
    private final ReentrantLock     recogParamChangesLock;

    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;

    public MainFrame()
    {
        // General settings
        setTitle("Notes recognition");
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        setSize(WIDTH, HEIGHT);
        setLocation((int) dim.getWidth() / 2 - WIDTH / 2, (int) dim.getHeight() / 2 - HEIGHT / 2);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // Inner panels and main algorithm machine
        recogParamChangesLock = new ReentrantLock();
        graphicPanel = new GraphicPanel();
        notesPanel = new NotesPanel();
        graphicTypePanel = new GraphicTypePanel(graphicPanel, recogParamChangesLock);
        recogMachine = new RecognitionMachine(graphicPanel, notesPanel, recogParamChangesLock);
        controlPanel = new ControlPanel(recogMachine);
        NotesProvider.init();

        // Setting menu bar
        saveMenuItem = new JMenuItem("Save-to-midi");
        saveMenuItem.addActionListener(new SaveMenuItemListener(recogMachine, controlPanel));
        saveMenuItem.setEnabled(false);
        openMenuItem = new JMenuItem("Open");
        openMenuItem.addActionListener(new OpenMenuItemListener(recogMachine, controlPanel, saveMenuItem));
        fileMenu = new JMenu("File");
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Setting layout
        SpringLayout layout = new SpringLayout();
        Container contentPane = getContentPane();
        setLayout(layout);
        layout.putConstraint(SpringLayout.WEST, controlPanel, 5, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, controlPanel, 5, SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.EAST, controlPanel, 150, SpringLayout.WEST, contentPane);
        layout.putConstraint(SpringLayout.SOUTH, controlPanel, 300, SpringLayout.NORTH, contentPane);

        layout.putConstraint(SpringLayout.WEST, graphicPanel, 0, SpringLayout.EAST, controlPanel);
        layout.putConstraint(SpringLayout.SOUTH, graphicPanel, 0, SpringLayout.NORTH, graphicTypePanel);
        layout.putConstraint(SpringLayout.NORTH, graphicPanel, 0, SpringLayout.NORTH, contentPane);
        layout.putConstraint(SpringLayout.EAST, graphicPanel, 0, SpringLayout.EAST, contentPane);

        layout.putConstraint(SpringLayout.WEST, graphicTypePanel, 0, SpringLayout.EAST, controlPanel);
        layout.putConstraint(SpringLayout.EAST, graphicTypePanel, -5, SpringLayout.EAST, contentPane);
        layout.putConstraint(SpringLayout.NORTH, graphicTypePanel, -20, SpringLayout.NORTH, notesPanel);
        layout.putConstraint(SpringLayout.SOUTH, graphicTypePanel, 0, SpringLayout.NORTH, notesPanel);

        layout.putConstraint(SpringLayout.WEST, notesPanel, 40, SpringLayout.EAST, controlPanel);
        layout.putConstraint(SpringLayout.NORTH, notesPanel, -90, SpringLayout.SOUTH, contentPane);
        layout.putConstraint(SpringLayout.EAST, notesPanel, -40, SpringLayout.EAST, contentPane);
        layout.putConstraint(SpringLayout.SOUTH, notesPanel, -10, SpringLayout.SOUTH, contentPane);

        add(controlPanel);
        add(graphicPanel);
        add(graphicTypePanel);
        add(notesPanel);
    }
}
