package be.aga.dominionSimulator.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

public class ButtonSelector extends JDialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -3675279357665261098L;
    private final ArrayList<String> myChooseFrom;
    private final String myButtonMessage;
    private int myChosenButton;

    public ButtonSelector(Component aComponent, String aTitle, ArrayList<String> chooseFrom, String aButtonMessage) {
        setModal(true);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                JOptionPane.showMessageDialog(null, "sorry, can't close");
            }
        });
        myChosenButton = 0;
        myChooseFrom = chooseFrom;
        myButtonMessage = aButtonMessage;
        buildGUI();
        setTitle(aTitle);
        pack();
        setLocationRelativeTo(aComponent);
        setVisible(true);
    }

    private void buildGUI() {
        setLayout(new BorderLayout());
        add(new JScrollPane(getChoicePanel()), BorderLayout.CENTER);
        add(getButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel getChoicePanel() {
        JPanel thePanel = new JPanel();
        thePanel.setLayout(new GridBagLayout());
        final GridBagConstraints theCons = DomGui.getGridBagConstraints(2);
        theCons.fill = GridBagConstraints.BOTH;
        theCons.anchor = GridBagConstraints.CENTER;
        for (String theOption : myChooseFrom) {
            JButton theButton = new JButton(theOption);
            theButton.addActionListener(this);
            theButton.setActionCommand(theOption);
            thePanel.add(theButton, theCons);
            theCons.gridy++;
        }
        return thePanel;
    }

    private JPanel getButtonPanel() {
        final JPanel thePanel = new JPanel();
        thePanel.setLayout(new GridBagLayout());
        final GridBagConstraints theCons = DomGui.getGridBagConstraints(2);
        theCons.fill = GridBagConstraints.NONE;
        theCons.anchor = GridBagConstraints.CENTER;
        //Clear button
        JButton theBTN = new JButton(myButtonMessage);
        theBTN.addActionListener(this);
        thePanel.add(theBTN, theCons);
        return thePanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (String theOption:myChooseFrom) {
            if (e.getActionCommand().equals(theOption)) {
                myChosenButton = myChooseFrom.indexOf(theOption);
                dispose();
                return;
            }
        }
        if (myButtonMessage.equals("Mandatory!"))
            return;
        myChosenButton=-1;
        dispose();
    }

    public int getChosenOption() {
        return myChosenButton;
    }
}