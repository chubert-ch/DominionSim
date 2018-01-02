package be.aga.dominionSimulator.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import be.aga.dominionSimulator.DomCard;
import be.aga.dominionSimulator.DomEngine;
import be.aga.dominionSimulator.DomPlayer;
import be.aga.dominionSimulator.enums.DomCardName;
import be.aga.dominionSimulator.enums.DomCardType;
import be.aga.dominionSimulator.enums.DomPhase;
import be.aga.dominionSimulator.gui.util.CardRenderer;
import be.aga.dominionSimulator.gui.util.HandCardRenderer;
import be.aga.dominionSimulator.gui.util.TableCardRenderer;

public class DomGameFrame extends JFrame implements ActionListener, ListSelectionListener, Observer {
    /**
     * 
     */
    private static final long serialVersionUID = 3862051191311979736L;
    public static final int LIST_HEIGHT = 360;
    final private DomEngine myEngine;
    final private JLabel myActionsValue = new JLabel();
    final private JLabel myBuysValue = new JLabel();
    final private JTextPane myLogPane = new JTextPane();
    final private JList<DomCard> myInPlayList = new JList<DomCard>();
    final private DefaultListModel<DomCard> myInPlayModel = new DefaultListModel<DomCard>();
    final private JButton myEndTurnBTN = new JButton("End turn");
    final private JList<DomCard> myHandList = new JList<DomCard>();
    final private JTable myBoardTable;
    final private HTMLEditorKit editorKit = new HTMLEditorKit();
    final private HTMLDocument gameLog = (HTMLDocument) editorKit.createDefaultDocument();
    final private JButton myEndActions = new JButton("End Actions");
    final private StatusBar myStatusBar = new StatusBar();
    final private JScrollPane myLogScroll = new JScrollPane(myLogPane);
    final private JButton myPlayAllTreasurersBTN = new JButton("Play all treasures");
    final private JLabel myVPLabel = new JLabel();
    final private JLabel myOppsVPLabel = new JLabel();
    final private JLabel myDrawDeckLabel = new JLabel();
    final private JLabel myDiscardLabel = new JLabel();
    final private JButton myHintButton = new JButton("Hint!");
    final private ArrayList<String> logStack = new ArrayList<String>();
    final private int myDelay;
    final private JButton mySpendCoinTokensBTN = new JButton("$0");
    final private JButton myPayOffDebtBTN = new JButton("$0");

    public DomGameFrame(DomEngine anEngine, String delay) {
        myEngine = anEngine;
        myBoardTable = new JTable(new KingdomTableModel(myEngine));

        Integer intDelay;
        try {
            intDelay = Integer.valueOf(delay);
        } catch (NumberFormatException e) {
            intDelay = 300;
        }

        myDelay = intDelay;
        buildGUI();
        setTitle("Play Dominion");
        //     setPreferredSize(RefineryUtilities.getMaximumWindowBounds().getSize());
        setPreferredSize(new Dimension(850, 750));
        pack();
        setVisible(true);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (myEngine.getCurrentGame() != null && !myEngine.getCurrentGame().isGameFinished())
                    myEngine.doEndOfHumanGameStuff();
            }
        });
    }

    private void buildGUI() {
        setLayout(new BorderLayout());
        JSplitPane theSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, getTopSplit(), getBottomPanel());
        theSplit.setResizeWeight(0.5);
        theSplit.setDividerLocation(320);
        theSplit.setDividerSize(3);
        //    theSplit.resetToPreferredSizes();
        getContentPane().add(theSplit, BorderLayout.CENTER);
        getContentPane().add(getStatusBar(), BorderLayout.SOUTH);
    }

    public Component getStatusBar() {
        myStatusBar.setText("Alles goed");
        return myStatusBar;
    }

    private JSplitPane getTopSplit() {
        JSplitPane theSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, getLogPanel(), getInfoPanel());
        theSplit.setResizeWeight(1);
        theSplit.setDividerSize(1);
        //	theSplit.resetToPreferredSizes();
        return theSplit;
    }

    private JPanel getLogPanel() {
        JPanel theLogPanel = new JPanel();
        theLogPanel.setLayout(new BorderLayout());
        myLogPane.setPreferredSize(new Dimension(400, 300));
        myLogPane.setEditorKit(editorKit);
        myLogPane.setDocument(gameLog);
        myLogScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        //	theScrollPane.setPreferredSize(new Dimension(400,400));
        theLogPanel.add(myLogScroll, BorderLayout.CENTER);
        Font font = new Font("Times New Roman", Font.PLAIN, 14);
        String bodyRule = "body { font-family: " + font.getFamily() + "; " + "font-size: " + font.getSize() + "pt; }";
        ((HTMLDocument) myLogPane.getDocument()).getStyleSheet().addRule(bodyRule);//	myLogPane.revalidate();
        return theLogPanel;
    }

    private JPanel getBottomPanel() {
        JPanel thePanel = new JPanel();
        thePanel.setLayout(new GridBagLayout());
        GridBagConstraints theCons = DomGui.getGridBagConstraints(0);
        theCons.fill = GridBagConstraints.BOTH;

        //hand list
        myHandList.setFixedCellHeight(20);
        myHandList.setLayoutOrientation(JList.VERTICAL);
        myHandList.setPreferredSize(new Dimension(100, LIST_HEIGHT));
        myHandList.setVisibleRowCount(40);
        final DefaultListModel<DomCard> myHandModel = new DefaultListModel<DomCard>();
        myHandList.setModel(myHandModel);
        myHandList.setMinimumSize(new Dimension(60, 400));
        myHandList.setCellRenderer(new HandCardRenderer());
        myHandList.setFont(new Font("Arial", Font.PLAIN, 12));
        myHandList.addListSelectionListener(this);
        myHandList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    int index = myHandList.locationToIndex(e.getPoint());
                    if (index >= 0)
                        showWiki(myHandList.getModel().getElementAt(index).getName());
                }
                super.mouseClicked(e);
            }
        });
        JScrollPane theScrollPane = new JScrollPane(myHandList);
        theScrollPane.setBorder(new TitledBorder("Hand"));
        theScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        thePanel.add(theScrollPane, theCons);
        //in-play list
        myInPlayList.setModel(myInPlayModel);
        //	myInPlayList.setBorder(new TitledBorder("In play"));
        myInPlayList.setMinimumSize(new Dimension(60, 400));
        myInPlayList.setPreferredSize(new Dimension(100, LIST_HEIGHT));
        myInPlayList.setVisibleRowCount(40);
        myInPlayList.setCellRenderer(new CardRenderer<DomCard>());
        myInPlayList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    final Point point = e.getPoint();
                    int index = myInPlayList.locationToIndex(point);
                    if (index >= 0)
                        showWiki(myInPlayList.getModel().getElementAt(index));
                }
                super.mouseClicked(e);
            }
        });
        myInPlayModel.addElement(DomCard.NONEXISTANT_CARD);
        theScrollPane = new JScrollPane(myInPlayList);
        theScrollPane.setBorder(new TitledBorder("In play"));
        theScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        theScrollPane.setAutoscrolls(true);
        theCons.gridx++;
        thePanel.add(theScrollPane, theCons);
        //the Board
        myBoardTable.setModel(new KingdomTableModel(myEngine));
        myBoardTable.setDefaultRenderer(DomCardName.class, new TableCardRenderer(myEngine));
        myBoardTable.setTableHeader(null);
        myBoardTable.setShowHorizontalLines(false);
        myBoardTable.setShowVerticalLines(false);
        myBoardTable.setColumnSelectionAllowed(false);
        myBoardTable.setRowSelectionAllowed(false);
        myBoardTable.setRowHeight(20);
        myBoardTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final Point point = e.getPoint();
                if (e.getButton() == MouseEvent.BUTTON1) {
                    System.out.println(myBoardTable);
                    int row = myBoardTable.rowAtPoint(point);
                    int col = myBoardTable.columnAtPoint(point);
                    DomCardName theCardName = (DomCardName) myBoardTable.getModel().getValueAt(row, col);
                    ArrayList<DomCard> thePile = myEngine.getCurrentGame().getBoard().get(theCardName);
                    if (thePile.isEmpty() && !theCardName.hasCardType(DomCardType.Event))
                        return;
                    if (!theCardName.hasCardType(DomCardType.Event))
                        theCardName = thePile.get(0).getName();
                    tryToBuyOrGainFromSupply(theCardName);
                }
                if (e.getButton() == MouseEvent.BUTTON3) {
                    int row = myBoardTable.rowAtPoint(point);
                    int col = myBoardTable.columnAtPoint(point);
                    DomCardName theCardName = (DomCardName) myBoardTable.getModel().getValueAt(row, col);
                    // ArrayList<DomCard> thePile =
                    // myEngine.getCurrentGame().getBoard().get(theCardName);
                    // if (thePile.isEmpty() &&
                    // !theCardName.hasCardType(DomCardType.Event))
                    //                    return;
                    //                if (!theCardName.hasCardType(DomCardType.Event))
                    //                    theCardName=thePile.get(0).getName();
                    showWiki(theCardName);
                }
                super.mouseClicked(e);
            }
        });
        JScrollPane theBoardPanel = new JScrollPane(myBoardTable);
        theBoardPanel.setPreferredSize(new Dimension(300, LIST_HEIGHT));
        theBoardPanel.setBorder(new TitledBorder("Kingdom"));
        theCons.gridx++;
        thePanel.add(theBoardPanel, theCons);
        return thePanel;
    }

    public static void showWiki(Object valueAt) {
        DomCardName theCardName = (DomCardName) valueAt;
        String theString = theCardName.toString().replaceAll("\\s+", "_");
        try {
            Desktop.getDesktop().browse(new URI("http://wiki.dominionstrategy.com/index.php/"+theString));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Can not open web site!", "", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void tryToBuyOrGainFromSupply(Object aCardName) {
        DomCardName theCard = (DomCardName) aCardName;
        DomPlayer activePlayer = myEngine.getCurrentGame().getActivePlayer();
        activePlayer.attemptToBuyFromSupplyAsHuman(theCard);
    }

    private JPanel getInfoPanel() {
        JPanel thePanel = new JPanel();
        thePanel.setLayout(new GridBagLayout());
        GridBagConstraints theCons = DomGui.getGridBagConstraints(2);
        theCons.fill = GridBagConstraints.NONE;
        theCons.gridx++;
        thePanel.add(myDrawDeckLabel, theCons);
        theCons.gridx++;
        thePanel.add(myDiscardLabel, theCons);
        //Actions indicator
        JLabel theActionsLabel = new JLabel("Actions:");
        theCons.gridx++;
        thePanel.add(theActionsLabel, theCons);
        theCons.gridx++;
        thePanel.add(myActionsValue, theCons);
        //Buys indicator
        JLabel theBuysLabel = new JLabel("Buys:");
        theCons.gridx++;
        thePanel.add(theBuysLabel, theCons);
        theCons.gridx++;
        thePanel.add(myBuysValue, theCons);
        theCons.gridx++;
        thePanel.add(myVPLabel, theCons);
        theCons.gridx++;
        thePanel.add(myOppsVPLabel, theCons);
        theCons.gridx++;
        JButton theInfoButton = new JButton("Game Info");
        theInfoButton.setActionCommand("Game Info");
        theInfoButton.addActionListener(this);
        thePanel.add(theInfoButton, theCons);
        //    theCons.gridx++;
        //    myOppTextLabel = new JLabel();
        //    thePanel.add(myOppTextLabel, theCons);
        theCons.weightx = 100;
        theCons.gridx++;
        thePanel.add(new JLabel(), theCons);
        theCons.weightx = 1;
        theCons.gridx++;
        myHintButton.setActionCommand("Hint");
        myHintButton.addActionListener(this);
        myHintButton.setVisible(false);
        thePanel.add(myHintButton, theCons);
        theCons.gridx++;
        myPlayAllTreasurersBTN.setActionCommand("Play all treasures");
        myPlayAllTreasurersBTN.addActionListener(this);
        myPlayAllTreasurersBTN.setVisible(false);
        thePanel.add(myPlayAllTreasurersBTN, theCons);
        theCons.gridx++;
        mySpendCoinTokensBTN.setActionCommand("Spend Coin Tokens");
        mySpendCoinTokensBTN.setToolTipText("Spend Coin Tokens");
        mySpendCoinTokensBTN.addActionListener(this);
        mySpendCoinTokensBTN.setVisible(false);
        thePanel.add(mySpendCoinTokensBTN, theCons);
        theCons.gridx++;
        myPayOffDebtBTN.setForeground(Color.red);
        myPayOffDebtBTN.setActionCommand("Pay off debt");
        myPayOffDebtBTN.setToolTipText("Pay off debt");
        myPayOffDebtBTN.addActionListener(this);
        myPayOffDebtBTN.setVisible(false);
        thePanel.add(myPayOffDebtBTN, theCons);
        theCons.gridx++;
        myEndActions.setActionCommand("End Actions");
        myEndActions.addActionListener(this);
        thePanel.add(myEndActions, theCons);
        theCons.gridx++;
        myEndTurnBTN.setActionCommand("End turn");
        myEndTurnBTN.addActionListener(this);
        thePanel.add(myEndTurnBTN, theCons);
        return thePanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Cancel")) {
            dispose();
        }
        if (e.getActionCommand().equals("Game Info")) {
            StringBuilder theInfo = new StringBuilder("<html>");
            theInfo.append("Trash: ").append(myEngine.getCurrentGame().getBoard().getTrashedCardsString());
            if (!myEngine.getCurrentGame().getActivePlayer().getTavernMat().isEmpty()) {
                theInfo.append("<br>");
                theInfo.append("Tavern Mat: ")
                        .append(myEngine.getCurrentGame().getActivePlayer().getTavernMatAsString());
            }
            if (!myEngine.getCurrentGame().getActivePlayer().getNativeVillageMatToString().isEmpty()) {
                theInfo.append("<br>");
                theInfo.append("Native Village Mat: ")
                        .append(myEngine.getCurrentGame().getActivePlayer().getNativeVillageMatToString());
            }
            if (!myEngine.getCurrentGame().getActivePlayer().getIslandMatString().isEmpty()) {
                theInfo.append("<br>");
                theInfo.append("Island Mat:").append(myEngine.getCurrentGame().getActivePlayer().getIslandMatString());
            }
            if (myEngine.getCurrentGame().getActivePlayer().getPirateShipLevel() > 0) {
                theInfo.append("<br>");
                theInfo.append("Pirate Ship Level: $")
                        .append(myEngine.getCurrentGame().getActivePlayer().getPirateShipLevel());
            }
            theInfo.append("<br>");
            theInfo.append("Journey token: ");
            if (myEngine.getCurrentGame().getActivePlayer().isJourneyTokenFaceUp())
                theInfo.append("face up");
            else
                theInfo.append("face down");
            if (myEngine.getCurrentGame().getActivePlayer().isMinusOneCardToken()) {
                theInfo.append("<br>");
                theInfo.append("-1 Card Token!");
            }
            theInfo.append("<br>");
            theInfo.append("<br>");
            theInfo.append("Opponent's cards in play: ");
            for (DomPlayer theOpps : myEngine.getCurrentGame().getActivePlayer().getOpponents()) {
                theInfo.append(theOpps.getCardsInPlay());
            }
            theInfo.append("</html>");
            JOptionPane.showMessageDialog(this, theInfo.toString());
        }
        if (e.getActionCommand().equals("Play all treasures")) {
            myEngine.getCurrentGame().getActivePlayer().attemptToPlayAllTreasures();
        }
        if (e.getActionCommand().equals("Spend Coin Tokens")) {
            int i = 1;
            if (myEngine.getCurrentGame().getActivePlayer().getCoinTokens() > 4) {
                String theStr = JOptionPane.showInputDialog(this, "Spend how many coin tokens?");
                if (!theStr.equals("") && Integer.valueOf(theStr) > 0)
                    i = Integer.valueOf(theStr);
            }
            myEngine.getCurrentGame().getActivePlayer().spendCoinTokens(i);
            myEngine.getCurrentGame().getActivePlayer().addAvailableCoins(i);
            myEngine.getCurrentGame().getActivePlayer().setNeedsToUpdate();
        }
        if (e.getActionCommand().equals("Pay off debt")) {
            myEngine.getCurrentGame().getActivePlayer().payOffDebt(1);
            myEngine.getCurrentGame().getActivePlayer().addAvailableCoins(-1);
            myEngine.getCurrentGame().getActivePlayer().setNeedsToUpdate();
        }
        if (e.getActionCommand().equals("End Actions")) {
            myEngine.getCurrentGame().getActivePlayer().endActions();
        }
        if (e.getActionCommand().equals("End turn")) {
            myEngine.getCurrentGame().getActivePlayer().humanEndsTurn();
        }
        if (e.getActionCommand().equals("End Buy")) {
            myEngine.getCurrentGame().getActivePlayer().setPhase(DomPhase.Night);
            myEngine.getCurrentGame().getActivePlayer().setNeedsToUpdate();
        }
        if (e.getActionCommand().equals("Hint")) {
            if (myEngine.getCurrentGame().getActivePlayer().getPhase() == DomPhase.Action) {
                DomCard theCard = myEngine.getCurrentGame().getActivePlayer().getNextActionToPlay();
                if (theCard != null)
                    JOptionPane.showMessageDialog(null, "<html>Play " + theCard.getName().toHTML() + "</html>");
                else
                    JOptionPane.showMessageDialog(null, "<html>End Actions</html>");
            }
            if (myEngine.getCurrentGame().getActivePlayer().getPhase() == DomPhase.Buy) {
                DomCardName theCard = myEngine.getCurrentGame().getHumanPlayer()
                        .getDesiredCard(myEngine.getCurrentGame().getHumanPlayer().getTotalPotentialCurrency(), false);
                if (theCard != null)
                    JOptionPane.showMessageDialog(null, "<html>Buy " + theCard.toHTML() + "</html>");
                else
                    JOptionPane.showMessageDialog(null, "<html>Buy nothing</html>");
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting() && ((JList<DomCard>) e.getSource()).getSelectedValue()!=null) {
            myEngine.getCurrentGame().getActivePlayer().attemptToPlay((DomCard) ((JList<DomCard>) e.getSource()).getSelectedValue());
        }
    }

    public void addToLog(final String s) {
        if (logStack.isEmpty()) {
            Timer theTimer = new Timer(myDelay, getListener());
            theTimer.start();
        }
        logStack.add(s);
    }

    private ActionListener getListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent o) {
                try {
                    if (!logStack.isEmpty()) {
                        String t = logStack.remove(0);
                        if (!t.contains("cards in Hand:")) {
                            editorKit.insertHTML(gameLog, gameLog.getLength(), t, 0, 0, null);
                        }
                        ((Timer)o.getSource()).restart();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    public void update(Observable o, Object arg) {
        updateHandList();
        updateInPlayList();
        ((KingdomTableModel)myBoardTable.getModel()).update();
        myStatusBar.setText(myEngine.getStatus());
        DomPlayer theActivePlayer = myEngine.getCurrentGame().getActivePlayer();
        myActionsValue.setText("<html><FONT style=\"BACKGROUND-COLOR: #D9D9D9\">"+theActivePlayer.actionsLeft+"</font></html>");
        myActionsValue.setToolTipText(theActivePlayer.actionsLeft + " actions left");
        ImageIcon imageIcon = new ImageIcon(getClass().getResource("images/Randomizer.jpg"));
        Image image = imageIcon.getImage(); // transform it
        Image newimg = image.getScaledInstance(12, 18,  Image.SCALE_DEFAULT); // scale it the smooth way
        myDrawDeckLabel.setIcon(new ImageIcon(newimg));  // transform it back
        myDrawDeckLabel.setText(""+myEngine.getCurrentGame().getActivePlayer().getDrawDeckSize());
        imageIcon = new ImageIcon(getClass().getResource("images/openDominion.jpg"));
        image = imageIcon.getImage(); // transform it
        newimg = image.getScaledInstance(12, 18,  Image.SCALE_DEFAULT); // scale it the smooth way
        myDiscardLabel.setIcon(new ImageIcon(newimg));  // transform it back
        myDiscardLabel.setText(""+myEngine.getCurrentGame().getActivePlayer().getCardsFromDiscard().size());

        String text = "<html>" + theActivePlayer.getBuysLeft() + " $" + "<FONT style=\"BACKGROUND-COLOR: #F3F584\">"
                + theActivePlayer.getAvailableCoinsWithoutTokens() + "</font> ($<FONT style=\"BACKGROUND-COLOR: #F3F584\">" + theActivePlayer.getTotalPotentialCurrency().getCoins() + "</font>?)";
        if (theActivePlayer.getAvailableCoinsWithoutTokens()==theActivePlayer.getTotalPotentialCurrency().getCoins())
            text = "<html>" + theActivePlayer.getBuysLeft() + " $" + "<FONT style=\"BACKGROUND-COLOR: #F3F584\">"
                    + theActivePlayer.getAvailableCoinsWithoutTokens() + "</font>";
        myBuysValue.setText(text);
        myVPLabel.setText("<html>"+myEngine.getCurrentGame().getHumanPlayer().countVictoryPoints()+"&#x25BC;</font></html>");
        String theOppVPString = "(Opp.: ";
        for (DomPlayer thePlayer : myEngine.getPlayers()) {
            if (!thePlayer.isHuman())
                theOppVPString+=thePlayer.countVictoryPoints()+"&#x25BC; ";
        }
        myOppsVPLabel.setText("<html>"+theOppVPString+")</html>");
        myOppsVPLabel.setFont(new Font("", Font.PLAIN, 10));
        if (theActivePlayer.getPhase()==DomPhase.Action)
            myEndActions.setVisible(true);
        else
            myEndActions.setVisible(false);
        if (theActivePlayer.getPhase()==DomPhase.Buy && theActivePlayer.getBoughtCards().isEmpty() && !theActivePlayer.getCardsFromHand(DomCardType.Treasure).isEmpty())
            myPlayAllTreasurersBTN.setVisible(true);
        else
            myPlayAllTreasurersBTN.setVisible(false);
        if (theActivePlayer.getCoinTokens()>0) {
            mySpendCoinTokensBTN.setText("$"+theActivePlayer.getCoinTokens());
            mySpendCoinTokensBTN.setVisible(true);
            mySpendCoinTokensBTN.setEnabled(false);
        } else {
            mySpendCoinTokensBTN.setVisible(false);
        }
        if (theActivePlayer.getPhase()==DomPhase.Buy && theActivePlayer.getBoughtCards().isEmpty() && theActivePlayer.getCoinTokens()>0) {
            mySpendCoinTokensBTN.setEnabled(true);
        } else {
            mySpendCoinTokensBTN.setEnabled(false);
        }
        if (theActivePlayer.getDebt()>0) {
            myPayOffDebtBTN.setText("$"+theActivePlayer.getDebt());
            myPayOffDebtBTN.setVisible(true);
            myPayOffDebtBTN.setEnabled(false);
        } else {
            myPayOffDebtBTN.setVisible(false);
        }
        if (theActivePlayer.getPhase()==DomPhase.Buy && theActivePlayer.getDebt()>0 && theActivePlayer.getAvailableCoinsWithoutTokens()>0) {
            myPayOffDebtBTN.setEnabled(true);
        } else {
            myPayOffDebtBTN.setEnabled(false);
        }
        if (theActivePlayer.getPhase()==DomPhase.Action || theActivePlayer.getPhase()==DomPhase.Buy)
            myHintButton.setVisible(true);
        else
            myHintButton.setVisible(false);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                myLogScroll.getVerticalScrollBar().setValue(myLogScroll.getVerticalScrollBar().getMaximum());
            }
        });
        if (!myEngine.getCurrentGame().getActivePlayer().getCardsFromHand(DomCardType.Night).isEmpty() && myEngine.getCurrentGame().getActivePlayer().getPhase()!=DomPhase.Night) {
            myEndTurnBTN.setText("End Buy");
            myEndTurnBTN.setActionCommand("End Buy");
        }else {
            myEndTurnBTN.setText("End Turn");
            myEndTurnBTN.setActionCommand("End turn");
        }
    }

    private void updateInPlayList() {
        DomPlayer thePlayer;
        if (myEngine.getCurrentGame().getActivePlayer().isHumanOrPossessedByHuman()) {
            thePlayer = myEngine.getCurrentGame().getActivePlayer();
        } else {
            thePlayer = myEngine.getCurrentGame().getHumanPlayer();
        }
        myInPlayModel.removeAllElements();
        for (DomCard theCard : thePlayer.getCardsInPlay()) {
            myInPlayModel.addElement(theCard);
        }
        if (thePlayer.getCardsInPlay().isEmpty())
            myInPlayModel.addElement(DomCard.NONEXISTANT_CARD);
        Runnable doRun = new Runnable() {
            @Override
            public void run() {
                myInPlayList.ensureIndexIsVisible(myInPlayModel.size() - 1);
            }
        };
        SwingUtilities.invokeLater(doRun);
    }

    private void updateHandList() {
        DomPlayer thePlayer;
        if (myEngine.getCurrentGame().getActivePlayer().isHumanOrPossessedByHuman()) {
            thePlayer = myEngine.getCurrentGame().getActivePlayer();
        } else {
            thePlayer = myEngine.getCurrentGame().getHumanPlayer();
        }
        ((DefaultListModel<DomCard>) myHandList.getModel()).removeAllElements();
        for (DomCardName theCard : thePlayer.getUniqueCardNamesInHand()) {
            ((DefaultListModel<DomCard>) myHandList.getModel()).addElement(thePlayer.getCardsFromHand(theCard).get(0));
        }
        myHandList.setSelectedIndex(-1);

    }

    public boolean askPlayer(String question, String title) {
        return JOptionPane.showConfirmDialog(this, question, title, JOptionPane.YES_NO_OPTION)==0;
    }

    public void askToSelectCards(String s, ArrayList<DomCard> chooseFrom, ArrayList<DomCard> theChosenCards, int aNumber) {
        new CardSelector(myInPlayList,s, theChosenCards, chooseFrom, aNumber);
    }

    public DomCardName askToSelectOneCard(String title, ArrayList<DomCardName> cards, String buttonMessage) {
        OneCardSelector theSelector = new OneCardSelector(myInPlayList, title, cards, buttonMessage);
        return theSelector.getChosenCard();
    }

    public DomCard askToSelectOneCardWithDomCard(String title, ArrayList<DomCard> cards, String buttonMessage) {
        OneCardSelectorWithDomCard theSelector = new OneCardSelectorWithDomCard(myInPlayList, title, cards, buttonMessage);
        return theSelector.getChosenCard();
    }

    public int askToSelectOption(String title, ArrayList<String> options, String buttonMessage) {
        ButtonSelector theSelector = new ButtonSelector(myInPlayList, title, options, buttonMessage);
        return theSelector.getChosenOption();
    }

    public void hover(String s) {
        myLogPane.setToolTipText(s);
    }
}