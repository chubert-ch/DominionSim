package be.aga.dominionSimulator;

import be.aga.dominionSimulator.enums.DomBotType;
import be.aga.dominionSimulator.enums.DomCardName;
import be.aga.dominionSimulator.enums.DomCardType;
import be.aga.dominionSimulator.enums.DomSet;
import be.aga.dominionSimulator.gui.DomBarChart;
import be.aga.dominionSimulator.gui.DomGameFrame;
import be.aga.dominionSimulator.gui.DomGui;
import be.aga.dominionSimulator.gui.DomLineChart;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Engine for a simulation of Dominion game.
 * <p>
 * Contains the {@code main} method that launches the simulator GUI.
 */
public class DomEngine {
    public static boolean showColoredLog = true;
    public static boolean hideGraphs = false;
    public static boolean developmentMode=false;

    public static double NUMBER_OF_GAMES = 1000;
	public static File BOT_FILE = new File(System.getProperty("user.home") + "/.domsim/userbots.xml");

    public static boolean haveToLog;
    public static StringBuilder myLog=new StringBuilder();
    public static int logIndentation=0;
    public static int logPlayerIndentation=0;
    private static final Logger LOGGER = Logger.getLogger( DomEngine.class );
    public static final Level LEVEL = Level.INFO;
    public static final boolean addAppender = true;
    static {
      LOGGER.setLevel( DomEngine.LEVEL );
      LOGGER.removeAllAppenders();
      if (addAppender)
         LOGGER.addAppender(new ConsoleAppender(new SimpleLayout()) );
    }

    public static DomPlayer currentPlayer;

    private ArrayList< DomPlayer > players = new ArrayList< DomPlayer >();
	private long findWinnerTime=0;
	
	/**
	 * The bots that will play the game.
	 */
	private ArrayList<DomPlayer> bots;
	
    private long boardResetTime=0;
    private long checkGameFinishTime=0;
    private long playerTurnTime=0;
    private DomGui myGui;
	private String myLastFile;
	private double myTotalTime;
	private int emptyPilesEndingCount=0;
	private static DomGameFrame myGameFrame;
	private DomGame currentGame;
    private String myStatus;

	/**
	 * @param aString
	 */
	public static void addToLog( String aString ) {
		StringBuilder theBuilder = new StringBuilder();
		for (int i=0;i<logPlayerIndentation;i++){
			theBuilder.append("&nbsp;&nbsp;&nbsp;");
		}
		for (int i=0;i<logIndentation;i++){
			theBuilder.append("...&nbsp;");
		}
		theBuilder.append(aString).append("<BR>");
		if (myGameFrame!=null) {
			myGameFrame.addToLog(theBuilder.toString());
		}
		myLog.append(theBuilder);
	}

	public DomEngine () {
		loadSystemBots();
		createSimpleCardStrategiesBots();
		loadCurrentUserBots();
		myGui = new DomGui( this );
		myGui.setVisible(true);
    }
    
    private void createSimpleCardStrategiesBots() {
        for (DomCardName theCard : DomCardName.getSafeValues()) {
           if (theCard.hasCardType( DomCardType.Kingdom )
                   && !theCard.hasCardType(DomCardType.Ruins)){
             DomPlayer theBot = getBot( "Big Money Ultimate" ).getCopy( theCard.toString());
             if (getBot(theBot.name)!=null)
            	 continue;
             theBot.setDescription("This bot has been generated by the computer without any optimization. " +
             		"XXXXIt just buys a single Action card and money");
             theBot.setAuthor("Computer");
             theBot.addBuyRuleFor(theCard);
             theBot.getTypes().remove(DomBotType.Optimized);
             theBot.setComputerGenerated();
             bots.add( theBot );
           }
        }
    }

    public void loadSystemBots() {
		try {
            InputSource src = new InputSource(getClass().getResourceAsStream("DomBots.xml"));
            // InputSource src = new InputSource(new FileInputStream(new File("..."));
			XMLHandler saxHandler = new XMLHandler();
			XMLReader rdr = XMLReaderFactory.createXMLReader();
			rdr.setContentHandler(saxHandler);
			rdr.parse(src);
			bots = saxHandler.getBots();
		} catch (Exception e) {
			// TODO: Update this message since this requires Java 1.8.
			JOptionPane.showMessageDialog(myGui, "You'll need to download Java 1.6 at www.java.com to runSimulation this program!!!");
		}
		Collections.sort( bots );
	}

    /**
     * Loads user-defined bots from file.
     * @return true iff the bots were successfully loaded
     */
	public boolean loadCurrentUserBots() {
		LOGGER.info("loading from: " + BOT_FILE);
		if (BOT_FILE.exists() && BOT_FILE.isFile()) {
			Reader input = null;
			try {
				input = new BufferedReader(new FileReader(BOT_FILE));
				loadUserBotsFromXML(new InputSource(input));
				input.close();
			} catch (IOException e1) {
				LOGGER.error("failed to load current user bots", e1);
				JOptionPane.showMessageDialog(myGui,
						"Error Reading File", "error",
						JOptionPane.ERROR_MESSAGE);
			}
			return false;
		}
		return true;
	}

	public void saveCurrentUserBots() {
		LOGGER.info("saving to: " + BOT_FILE);
		Writer output = null;
		try {
			BOT_FILE.getParentFile().mkdirs();
			BOT_FILE.createNewFile();
			output = new BufferedWriter(new FileWriter(BOT_FILE));
			output.write(getXMLForAllUserBots());
			output.close();
		} catch (IOException e1) {
			LOGGER.error("failed to save current user bots", e1);
			JOptionPane.showMessageDialog(myGui,
					"Error Writing File", "error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

    public DomPlayer loadUserBotsFromXML(InputSource anXMLSource) {
		try {
			XMLHandler saxHandler = new XMLHandler();
			XMLReader rdr = XMLReaderFactory.createXMLReader();
			rdr.setContentHandler(saxHandler);
			rdr.parse(anXMLSource);
			ArrayList<DomPlayer> theNewPlayers = saxHandler.getBots();
			for (DomPlayer thePlayer : theNewPlayers) {
			  thePlayer.addType(DomBotType.UserCreated);
			  addUserBot(thePlayer);
			}
			return bots.get(0);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(myGui, "Bot creation failed! Make sure you have a valid XML in your clipboard", "", JOptionPane.ERROR_MESSAGE);
		}
		return null;
	}

	private void showCharts() {
      myGui.setBarChart(new DomBarChart(players));
      myGui.setVPLineChart(new DomLineChart(players, "VP"));
      myGui.setMoneyLineChart(new DomLineChart(players, "Money"));
      myGui.validate();
	}

    private void printResults() {
        int theTotalTies = 0;
        for (DomPlayer thePlayer : players) {
            theTotalTies += thePlayer.getTies();
        }
        LOGGER.info("=============================");
        LOGGER.info("Games");
        LOGGER.info("=============================");

        double theAverageTurns = ((int) (players.get(0).getSumTurns() * 10 / NUMBER_OF_GAMES)) / 10.0;
        LOGGER.info("Average turns = " + theAverageTurns);

        showRunTimes();

        if (NUMBER_OF_GAMES > 1) {
            for (DomPlayer thePlayer : players) {
//                myGui.showWinPercentage(thePlayer, thePlayer.getWins()*100/(theTotalWins +theTotalTies/2));
                myGui.showWinPercentage(thePlayer, (int) (thePlayer.getWins() * 100 / NUMBER_OF_GAMES));
//                myGui.showTiePercentage(thePlayer.getTies()*100/(theTotalWins +theTotalTies/2));
                myGui.showAverageTurns(theAverageTurns);
                myGui.show3EmptyPilesEndings(emptyPilesEndingCount / NUMBER_OF_GAMES * 100);
                myGui.showTime(myTotalTime);

                LOGGER.info(thePlayer + " has " + thePlayer.getWins() * 100 / NUMBER_OF_GAMES + "% wins ("
                        + thePlayer.getWins() + ")"
                        + " and " + thePlayer.getTies() * 100 / NUMBER_OF_GAMES + "% ties ("
                        + thePlayer.getTies() + ")");
            }
            myGui.showTiePercentage((int) (theTotalTies*100/NUMBER_OF_GAMES));
            LOGGER.info("Empty Piles Endings : " + emptyPilesEndingCount / NUMBER_OF_GAMES * 100 + "%");
        }
    }

    private void showRunTimes() {
        long theTotalActionTime = 0;
        long theTotalBuyTime = 0;
        long theTotalCountVPTime = 0;
        for (DomPlayer thePlayer : players) {
          theTotalActionTime+= thePlayer.actionTime;
          theTotalBuyTime+=thePlayer.buyTime;
          theTotalCountVPTime+=thePlayer.countVPTime;
        }
        LOGGER.info( "Action time : " + theTotalActionTime);
        LOGGER.info( "Buy time: " + theTotalBuyTime);
        LOGGER.info( "count VPs time: " + theTotalCountVPTime);
        LOGGER.info( "player turn time: " + playerTurnTime);
        LOGGER.info( "find winner time: " + findWinnerTime);
        LOGGER.info( "board reset time: " + boardResetTime);
        LOGGER.info( "check game finish time: " + checkGameFinishTime);
    }

    /**
     * Returns {@code DomPlayer} corresponding to {@code aString}.
     * <p>
     * Returns {@code null} if no corresponding bot exists.
     * @param aString String identifying the bot or player
     * @return DomPlayer bot or player 
     */
    private DomPlayer getBot( String aString ) {
        for (DomPlayer thePlayer : bots){
            if (thePlayer.toString().equals( aString ))
                return thePlayer;
        }
        return null;
    }

    public static void main( String[] args )  {
       new DomEngine();
    }

    public Object[] getBotArray() {
      return bots.toArray();
    }

	public DomGame getCurrentGame() {
		return currentGame;
	}

	/**
     * @param thePlayers
     * @param keepOrder 
     * @param aNumber 
     * @param aShowLog 
     */
    public void startSimulation( ArrayList<DomPlayer> thePlayers, boolean keepOrder, int aNumber, boolean aShowLog ) {
        emptyPilesEndingCount=0;
        NUMBER_OF_GAMES = aNumber;
     	myLog = new StringBuilder();
		myLog.append("<BR><HR><B>Game Log</B><BR>");
        long theStartTime = System.currentTimeMillis();
        players.clear();
        players.addAll(thePlayers);
        DomBoard theBoard = null;
        playerTurnTime=0;
        checkGameFinishTime=0;
        findWinnerTime=0;
        boardResetTime=0;

        for (int i=0;i<NUMBER_OF_GAMES;i++) {
            if (!keepOrder) {
              Collections.shuffle(players);
            }
            haveToLog=false;
            currentGame = new DomGame(theBoard, players, this);
            haveToLog=aShowLog;
            currentGame.runSimulation();
            if (DomEngine.haveToLog) {
              writeEndOfGameLog(currentGame);
            }
            playerTurnTime+=currentGame.playerTurnTime;
            checkGameFinishTime+=currentGame.checkGameFinishTime;
            emptyPilesEndingCount+=currentGame.emptyPilesEnding ? 1 : 0;
            long theTime = System.currentTimeMillis();
            currentGame.determineWinners();
            findWinnerTime += System.currentTimeMillis()-theTime;
            theBoard=currentGame.getBoard();
            theTime = System.currentTimeMillis();
//            LOGGER.info("Game : "+ i);
//            LOGGER.info("--------------");
//            LOGGER.info("Board: "+ theBoard);
//            LOGGER.info(players.get(0) + " : "+ players.get(0).getDeck());
//            LOGGER.info(players.get(1) + " : "+ players.get(1).getDeck());
            theBoard.reset();
            boardResetTime += System.currentTimeMillis()-theTime;
        }
        //restoring the player order:
        players.clear();
        players.addAll( thePlayers );
        
        myTotalTime = ((System.currentTimeMillis()-theStartTime)/100)/10.0;
        LOGGER.info("Board after all games: "+ theBoard);
        LOGGER.info( "Totale runSimulation tijd : " + myTotalTime );

        printResults();
        if (!haveToLog) 
          showCharts();
    }

	private void writeEndOfGameLog(DomGame theGame) {
		if (getCurrentGame().getBoard().countEmptyPiles() >= 3) {
			DomEngine.addToLog("");
			DomEngine.addToLog("Three piles depleted!");
		}
		DomEngine.addToLog("</i>");
	  DomEngine.addToLog("!!!!!!!Game ends!!!!!!!!");
	  DomEngine.addToLog("");
	  DomEngine.addToStartOfLog("the Empty Piles : " + theGame.getEmptyPiles());
	  if (!theGame.getTrashedCards().isEmpty())
	    DomEngine.addToStartOfLog("the Trashed Cards : " + theGame.getTrashedCards());
	  DomEngine.addToStartOfLog("");
	  String theEmbargoedStuff = theGame.getBoard().getEmbargoInfo();
	  if (theEmbargoedStuff!=null) {
	      DomEngine.addToLog("");
	      DomEngine.addToLog("Embargo Tokens on: " + theEmbargoedStuff);
	  }
	}

    public static void addToStartOfLog(String string) {
        StringBuilder theBuilder = new StringBuilder();
        theBuilder.append(string).append("<BR>").append(myLog);
        myLog=theBuilder;
	}

	/**
     * @return
     */
    public ArrayList< DomPlayer > getPlayers() {
        return players;
    }
    

	public void addUserBot(DomPlayer theNewPlayer) {
		for (DomPlayer theBot : bots) {
			if (theBot.name.equals(theNewPlayer.name)) {
		       bots.remove(theBot);		
		       break;
			}
		}
		bots.add(0,theNewPlayer);
		if (myGui != null) {
			myGui.refreshBotSelectors(theNewPlayer);
		}
	}

	public void deleteBot(DomPlayer selectedItem) {
        bots.remove(selectedItem);		
		myGui.refreshBotSelectors(null);
	}
	
	public void saveUserBots() {
		JFileChooser fileChooser = new JFileChooser(myLastFile);
		fileChooser.setFileFilter(getFileFilter());
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (fileChooser.showSaveDialog(myGui) == JFileChooser.APPROVE_OPTION) {
			Writer output = null;
			try {
				String theWriteFile = fileChooser.getCurrentDirectory()
						.getAbsolutePath()
						+ "\\";
				theWriteFile += fileChooser.getSelectedFile().getName();
				if (!theWriteFile.endsWith(".xml")) {
					theWriteFile+=".xml";
				}
				myLastFile=theWriteFile.replaceAll("\\\\", "/");;
				output = new BufferedWriter(new FileWriter(myLastFile));
				output.write(getXMLForAllUserBots());
				output.close();
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(myGui,
						"Error Writing File", "error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private String getXMLForAllUserBots() {
        String newline = System.getProperty( "line.separator" );
		StringBuilder theXML = new StringBuilder();
		theXML.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(newline);
		theXML.append("<playerCollection>").append(newline);
		for (DomPlayer thePlayer : bots) {
	      if (thePlayer.isUserCreated()){
	    	theXML.append(thePlayer.getXML()).append(newline);
	      }
		}
		theXML.append("</playerCollection>");
		return theXML.toString();
	}

	public void loadUserBots() {
		JFileChooser fileChooser = new JFileChooser(myLastFile);
		fileChooser.setFileFilter(getFileFilter());
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (fileChooser.showOpenDialog(myGui) == JFileChooser.APPROVE_OPTION) {
			Reader input = null;
			try {
				String theReadFile = fileChooser.getCurrentDirectory()
						.getAbsolutePath()
						+ "\\";
				theReadFile += fileChooser.getSelectedFile().getName();
				myLastFile=theReadFile.replaceAll("\\\\", "/");
				LOGGER.info(myLastFile);
				input = new BufferedReader(new FileReader(myLastFile));
				loadUserBotsFromXML(new InputSource(input));
				input.close();
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(myGui,
						"Error Reading File", "error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private FileFilter getFileFilter() {
		return new FileFilter() {
			@Override
			public String getDescription() {
				return "*.xml";
			}
			
			@Override
			public boolean accept(File f) {
				return f.getName().endsWith(".xml") || f.isDirectory();
			}
		};
	}

	public void orderBots() {
		Collections.sort(bots);
	}

	public Object[] getBots(Object[] domBotTypes, String[] keywords) {
		ArrayList<DomPlayer> theBots = new ArrayList<DomPlayer>();
		player:
		for (DomPlayer player : bots) {
			for (Object type : domBotTypes) {
				if (!player.hasType(type)) {
					continue player;
			    }
		    }
		    if (keywords != null) {
				keyword:
				for (String searchKeyword : keywords) {
					for (String playerKeyword : player.getKeywords()) {
						if (playerKeyword.startsWith(searchKeyword)) {
							continue keyword;
						}
					}
					continue player;
				}
			}
			theBots.add(player);
		}
		Collections.sort(theBots);
		return theBots.toArray();
	}

	public void setSelectedBot(Object selectedValue) {
	  myGui.refreshBotSelectors((DomPlayer) selectedValue);	
	}

	public boolean doesBotExist(DomPlayer domPlayer) {
		for (DomPlayer player : bots){
			if (domPlayer==player)
				return true;
		}
		return false;
	}

	public ArrayList<DomCardName> getBoardCards() {
		ArrayList<DomCardName> theCards = new ArrayList<DomCardName>();
		theCards.addAll(DomSet.Common.getCards());
		theCards.addAll(DomBoard.getRandomBoard());
		return theCards;
	}

	public void setGameFrame(DomGameFrame domGameFrame) {
		myGameFrame = domGameFrame;
	}

	public DomGui getGui() {
		return myGui;
	}

	public void setSelectedBoard(Object[] selectedValues) {
		// TODO Auto-generated method stub
		
	}

	public void startHumanGame(DomPlayer theHumanPlayer, String delay) {
    	myLog=new StringBuilder();
    	logPlayerIndentation=0;
    	logIndentation=0;
		ArrayList<DomPlayer> thePlayers = myGui.initPlayers();
		theHumanPlayer.setBuyRules((ArrayList<DomBuyRule>) thePlayers.get(0).getBuyRules().clone());
		theHumanPlayer.setStartState(thePlayers.get(0).getStartState());
		theHumanPlayer.setShelters(thePlayers.get(0).getShelters());
		thePlayers.add(0,theHumanPlayer);
		if (!myGui.getOrderBoxSelected())
			Collections.shuffle(thePlayers);
		emptyPilesEndingCount=0;
		players.clear();
		players.addAll(thePlayers);
		DomBoard theBoard = null;
		haveToLog=false;
		currentGame = new DomGame(theBoard, players, this);
		haveToLog=true;
        setGameFrame(new DomGameFrame(this, delay));
		myGameFrame.setVisible(true);
		currentGame.startUpHumanGame();
	}

	public void doEndOfHumanGameStuff() {
		writeEndOfGameLog(currentGame);
		emptyPilesEndingCount+=currentGame.emptyPilesEnding ? 1 : 0;
		currentGame.determineWinners();
//		printResults();
		myGui.showSampleGame();
	}

	public void setStatus(String status) {
		myStatus = status;
	}

	public DomGameFrame getGameFrame() {
		return myGameFrame;
	}

    public String getStatus() {
        return myStatus;
    }
}