package be.aga.dominionSimulator.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import be.aga.dominionSimulator.DomCard;
import be.aga.dominionSimulator.gui.util.CardRenderer;

public class OneCardSelectorWithDomCard extends JDialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -2763092423371435334L;
    private final ArrayList<DomCard> myChooseFrom;
    private final String myButtonMessage;
    private DomCard myChosenCard;
    private JList<DomCard> myChooseFromList;

    public OneCardSelectorWithDomCard(Component aComponent, String aTitle, ArrayList<DomCard> chooseFrom, String aButtonMessage) {
        setModal(true);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                JOptionPane.showMessageDialog(null,"sorry, can't close");
            }
        });
        myChosenCard = null;
        myChooseFrom = chooseFrom;
        Collections.sort(myChooseFrom,DomCard.SORT_BY_NAME);
        myButtonMessage = aButtonMessage;
        buildGUI();
        setTitle(aTitle);
        pack();
        setLocationRelativeTo(aComponent);
        setVisible(true);
    }

    private void buildGUI() {
        setLayout( new BorderLayout() );
        add(getChoicePanel(),BorderLayout.CENTER);
        add(getButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel getChoicePanel() {
        JPanel thePanel = new JPanel();
        thePanel.setLayout( new GridBagLayout() );
        final GridBagConstraints theCons = DomGui.getGridBagConstraints( 2 );
        theCons.fill=GridBagConstraints.BOTH;
        theCons.anchor=GridBagConstraints.CENTER;
        thePanel.add(getChooseFromPanel(),theCons);
        return thePanel;
    }

    private JPanel getButtonPanel() {
        final JPanel thePanel = new JPanel();
        thePanel.setLayout( new GridBagLayout() );
        final GridBagConstraints theCons = DomGui.getGridBagConstraints( 2 );
        theCons.fill=GridBagConstraints.NONE;
        theCons.anchor=GridBagConstraints.CENTER;
        //Clear button
        JButton theBTN = new JButton(myButtonMessage);
        theBTN.addActionListener(this);
        thePanel.add(theBTN,theCons);
        return thePanel;
    }

    private JPanel getChooseFromPanel() {
        final JPanel thePanel = new JPanel();
        thePanel.setLayout( new GridBagLayout() );
        final GridBagConstraints theCons = DomGui.getGridBagConstraints( 2 );
        JScrollPane theChooseFromScroller = new JScrollPane(getChooseFromList(), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        theChooseFromScroller.setBorder(new TitledBorder( "Choose a card" ));
        theChooseFromScroller.setPreferredSize(new Dimension(150,200));
        thePanel.add(theChooseFromScroller, theCons);
        return thePanel;
    }

    private JList<DomCard> getChooseFromList() {
        myChooseFromList = new JList<DomCard>();
        //    myChooseFromList.setPreferredSize(new Dimension(100,200));
        myChooseFromList.setCellRenderer(new CardRenderer<DomCard>());
        myChooseFromList.setModel(new DefaultListModel<DomCard>());
        for (DomCard theCard:myChooseFrom)
            ((DefaultListModel<DomCard>)myChooseFromList.getModel()).addElement(theCard);
        myChooseFromList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton()==MouseEvent.BUTTON1) {
                    int index = myChooseFromList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        DomCard theChosenCard = myChooseFromList.getModel().getElementAt(index);
                        for (DomCard theCard:myChooseFrom) {
                            if (theCard==theChosenCard) {
                                myChosenCard = theCard;
                                dispose();
                                break;
                            }
                        }
                    }
                }
                if (e.getButton()==MouseEvent.BUTTON3) {
                    int index = myChooseFromList.locationToIndex(e.getPoint());
                    if (index >= 0)
                        DomGameFrame.showWiki( myChooseFromList.getModel().getElementAt(index));
                }
            }
        });
        return myChooseFromList;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (myButtonMessage.equals("Mandatory!"))
            return;
        dispose();
    }

    public DomCard getChosenCard() {
        return myChosenCard;
    }
}