import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.io.*;
import java.net.*;

class Player {
	public int playerID;
	public boolean isBot;
	public boolean online;
	public Socket connection;
	public BufferedReader inFromClient;
	public DataOutputStream outToClient;
	public ArrayList<Card> hand;
	public boolean uno;
	public Player(int playerID, ArrayList<Card> hand, boolean isBot) {
		this.playerID = playerID; this.hand = hand; this.isBot = isBot; this.online = false;
	}
	public Player(int playerID, boolean isBot, Socket connection, BufferedReader inFromClient, DataOutputStream outToClient) {
		this.playerID = playerID; this.isBot = isBot; this.online = true;
		this.connection = connection; this.inFromClient = inFromClient; this.outToClient = outToClient; 
	}
}

public class Server {
	public static String[] CARDCOLOR = new String[] {"\u001B[101m\033[30m\u001B[1m", //RED BACKGROUND, BLACK TEXT, BOLD
									 				 "\u001B[102m\033[30m\u001B[1m", //GREEN BACKGROUND, BLACK TEXT, BOLD
													 "\u001B[103m\033[30m\u001B[1m", //YELLOW BACKGROUND, BLACK TEXT, BOLD
													 "\u001B[106m\033[30m\u001B[1m", //BLUE BACKGROUND, BLACK TEXT, BOLD
													 "\u001B[47m\033[30m\u001B[1m"}; //WILD: GRAY BACKGROUND, BLACK TEXT, BOLD
													 // {"RED_", "GREEN_", "YELLOW_", "BLUE_", "WILD_"}; //CODE FOR WINDOWS COMMAND PROMPT
	public static String RESET = "\u001B[0m"; // RESET TO NORMAL
	public Card PlayedCard;
	public ArrayList<Card> deck;
	public ArrayList<Player> players;
	public Random rnd;
	public int index = 0; //The starting order isn't randomized right now, but could be in the future


	String[] coloredcards = new String[] {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
											   "1", "2", "3", "4", "5", "6", "7", "8", "9", 
											   		  "[</>]", "[(X)]", "[+2]",
											   		  "[</>]", "[(X)]", "[+2]"}; //REVERSE, SKIP, +2
	String[] wildcards = new String[] {"(?)", "+4"}; // WILD, WILD+4

	//Creates and shuffles a deck
	private void createDeck() {
		deck = new ArrayList<Card>();
		for(int i=0; i<4; i++) {
			for(int j=0; j<coloredcards.length; j++) {
				deck.add(new Card(i, coloredcards[j]));
			}			
		}
		for(int i=0; i<4; i++) {
			deck.add(new Card(Card.WILD, wildcards[0]));
			deck.add(new Card(Card.WILD, wildcards[1]));
		}
		rnd = ThreadLocalRandom.current();
		for(int i=deck.size()-1; i>0; i--) {
			int index = rnd.nextInt(i+1);
			Card a = deck.get(index); deck.set(index, deck.get(i)); deck.set(i, a); // SWAP
		}
	}

	private void printHand(ArrayList<Card> hand) {
		for(int i=0; i<hand.size(); i++) {
			System.out.print(printCard(hand.get(i)));
		}
	}
	private String printCard(Card aCard) {
		return "\t"+CARDCOLOR[aCard.color] + " " + aCard.value + " " + RESET;
	}

	private void sortHand(ArrayList<Card> hand) {
		boolean swapped = true; int j=0;
		while(swapped) {
			swapped = false; j++;
			for(int i=0; i<hand.size()-j; i++) {
				if(hand.get(i+1).color < hand.get(i).color || (hand.get(i+1).color == hand.get(i).color && (hand.get(i+1).value.compareTo(hand.get(i).value)<0))) { // if(hand[i+1] < hand[i])
					Card a = hand.get(i); hand.set(i, hand.get(i+1)); hand.set(i+1, a); swapped = true; // SWAP
				}
			}			
		}
	}

	public ArrayList<Card> drawCard(ArrayList<Card> hand) {
		hand.add(deck.remove(0));
		sortHand(hand);
		players.get(index).uno=false;
		return hand; 
	}

	public Card playerPlay(ArrayList<Card> hand) {
		boolean viableChoice=false;
		Card lastCardPlayed = null;
		BufferedReader br = null;
		String input = "";
		boolean uno = false;

		if(canPlay(hand)) {
			System.out.print("Your current hand:    "); printHand(hand); 
			System.out.print("\nSelect cards to play: ");
			for(int i=0; i<hand.size(); i++) {System.out.print("\t["+i+"]");} System.out.println();
			List<Integer> cardChoices = new ArrayList<Integer>();
			boolean validCardChoices = false;
			String[] splitInput;
			while(!validCardChoices) { //Continue until player selected valid card to play
				try {
					br = new BufferedReader(new InputStreamReader(System.in));
					input=br.readLine();
					splitInput = input.split(",");
					for(int i=0; i<splitInput.length; i++) {
						cardChoices.add(Integer.parseInt(splitInput[i].trim()));
					}
				} catch (NumberFormatException e) {
					//The array element was not an integer... so we assume it was a string with the text "uno" instead
					uno = true; //Ugly hack... It's 3:20 AM... fix later...
				} catch (Exception e) {}
				for(int i=0; i<cardChoices.size(); i++) {
					if(!(validCardChoices=((viableChoice(hand.get(cardChoices.get(0))) && 
						(hand.get(cardChoices.get(0)).value.compareTo(hand.get(cardChoices.get(i)).value)==0))))) { //Is the first selected card valid, is the other cards of same value as first?
						System.out.println("You have selected an invalid card to play, try again");
						cardChoices.clear();
						break;
					}
				}
			}

			lastCardPlayed = hand.get(cardChoices.get(cardChoices.size()-1));
			if(lastCardPlayed.color==Card.WILD) {
				System.out.println("Choose color: \t" + CARDCOLOR[0] + " 0 " + RESET + "\t" +
													    CARDCOLOR[1] + " 1 " + RESET + "\t" +
													    CARDCOLOR[2] + " 2 " + RESET + "\t" +
													    CARDCOLOR[3] + " 3 " + RESET + "\t");
				try {
					input=br.readLine();
					lastCardPlayed.color = Integer.parseInt(input);
				} catch(Exception e){}
			}			
			String PlayedString = "";
			for(int i=0; i<cardChoices.size(); i++) {
				PlayedString = PlayedString + printCard(hand.get((int )cardChoices.get(i)));
			}
			Collections.sort(cardChoices);
			for(int i=cardChoices.size()-1; i>=0; i--) {
				hand.remove((int )cardChoices.get(i));
			}
			System.out.println("Player has played the following cards: " + PlayedString);
		} else {
			return playerPlay(drawCard(hand));
		}
		if(uno){lastCardPlayed.uno = true;}
		return lastCardPlayed;
	}

	public Card botPlay(ArrayList<Card> hand) {
		for (int i=0; i<hand.size(); i++) {
			if(hand.get(i).value.compareTo(PlayedCard.value)==0 || PlayedCard.color == hand.get(i).color || hand.get(i).color == Card.WILD) { //Same value, same color, or wildcard
				if(hand.get(i).color == Card.WILD) {hand.get(i).color = rnd.nextInt(4);}//Just set random color for now, could probably be improved
				Card cardToPlay = hand.remove(i);
				if(hand.size() == 1) {
					cardToPlay.uno = true;
				}
				return cardToPlay;
			}
		}
		if(deck.size() > 0) {
			Card cardToPlay = botPlay(drawCard(hand)); //recursive draw card and play until deck is empty
			if(cardToPlay.color == Card.WILD) {
				cardToPlay.color = rnd.nextInt(4); //Just set random color for now, could probably be improved
			}
			return cardToPlay;
		} 
		return null;
	}

	public boolean canPlay(ArrayList<Card> hand) {
		for (int i=0; i<hand.size(); i++) {
			if(hand.get(i).value.compareTo(PlayedCard.value)==0 || PlayedCard.color == hand.get(i).color || hand.get(i).color == Card.WILD) { //Same value, same color, or wildcard
				return true;
			}
		}
		if(deck.size() > 0) {return canPlay(drawCard(hand));} //recursive draw card until there is a card the player can play
		return false;
	}

	public boolean viableChoice(Card aCard) {
		if(aCard.value.compareTo(PlayedCard.value)==0 || PlayedCard.color == aCard.color || aCard.color == Card.WILD) { //Same value, same color, or wildcard
			return true;
		}
		return false;		
	}

	public Server(int numberOfOnlineClients) {
		createDeck();
		players = new ArrayList<Player>();
		players.add(new Player(0, new ArrayList<Card>(), false)); // Player 0
		players.add(new Player(1, new ArrayList<Card>(), true)); // Bot 1
		for(int i=0; i<7; i++) { //Deal 7 cards to the Player and the Non-online bot
			players.get(0).hand.add(deck.remove(0)); // Player 0
			players.get(1).hand.add(deck.remove(0)); // Bot 1
		}
		sortHand(players.get(0).hand); //Sort the hand for Player 0

		//Server stuffs
		try {
			ServerSocket aSocket = new ServerSocket(2048);
			for(int onlineClient=0; onlineClient<numberOfOnlineClients; onlineClient++) {
				Socket connectionSocket = aSocket.accept();
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				boolean isBot = Boolean.parseBoolean(inFromClient.readLine());
				String handString = "";
				for(int i=0; i<7; i++) { //Deal 7 cards to the online Player or Bot
					handString = ((handString.compareTo("")==0)?"":(handString+";")) + deck.remove(0).toString(); //Create String of Cards, separated by ;
				}
				outToClient.writeBytes(handString+"\n");
				players.add(new Player(onlineClient+2, isBot, connectionSocket, inFromClient, outToClient));
				System.out.println("Connected to " + (isBot?"Bot":"Player") + " ID: " + (onlineClient+2));
			}
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}

		PlayedCard = deck.remove(0);
		System.out.println("Starting card: " + printCard(PlayedCard)); 
		for(int i=0; i<players.size(); i++) {
			if(players.get(i).online) {
				try {
					players.get(i).outToClient.writeBytes("Starting card:" + PlayedCard.toString()+"\n"); //make sure all clients know the starting card
				} catch(Exception e) {}
			}
		}

		Card playCard = null;
		boolean clockwise = true;
		while(true) {
			boolean skip = false;
			Player current = players.get(index);
			int drawCards = 0;
			if(PlayedCard.value.compareTo("[+2]")==0) {drawCards=2;}
			if(PlayedCard.value.compareTo("+4")==0) {drawCards=4;}
			if(current.online) {
				try {
					if(drawCards>0) { //deal with +2 / +4 card draw for online clients
						for(int i=0; i<drawCards; i++) {current.outToClient.writeBytes("CARD:"+deck.remove(0).toString()+"\n");}
					}
					current.outToClient.writeBytes("Your turn:" + PlayedCard.toString()+"\n");
					String response="";
					while(!((response=current.inFromClient.readLine()).compareTo("DONE:")==0)) {
						String[] parsedResponse = response.split(":");
						if(parsedResponse[0].compareTo("DRAW")==0) {
							current.outToClient.writeBytes(deck.remove(0).toString()+"\n");
							current.uno=false;
						} else if(parsedResponse[0].compareTo("CARD")==0) {
							String[] cardResponse = parsedResponse[1].split(",");
							PlayedCard = new Card(Integer.parseInt(cardResponse[0]), cardResponse[1]);

							if(PlayedCard.value.compareTo("[</>]")==0) { clockwise = !clockwise; } //Reverse: [</>]
							if(PlayedCard.value.compareTo("[(X)]")==0) { skip=true; } // Skip: [(X)]
							PlayedCard.uno = (parsedResponse.length==3?true:false);
							String writeText = (current.isBot?"Bot":"Player") + index + " played:";
							System.out.println(writeText + " " + printCard(PlayedCard) + (PlayedCard.uno?" and has said uno":""));
							for(int i=0;i<players.size();i++) {
								if(players.get(i).online){players.get(i).outToClient.writeBytes(writeText+PlayedCard.toString()+(PlayedCard.uno?":UNO":"")+"\n");}
							}
						}
					}					
				} catch(Exception e){}
			} else {
				if(drawCards>0) {
					for(int i=0; i<drawCards; i++) {drawCard(current.hand);} //Draw Cards for Player 0 or Bot 1
				}
				if(current.isBot) {
					PlayedCard = ((playCard=botPlay(current.hand))!=null?playCard:PlayedCard);
				} else {
					if(current.hand.size() == 1 && !current.uno) {drawCard(current.hand);} //The player forgot to say UNO with only one card left, draw card.
					PlayedCard = ((playCard=playerPlay(current.hand))!=null?playCard:PlayedCard);
				}
				if(PlayedCard.value.compareTo("[</>]")==0) { clockwise = !clockwise; } //Reverse: [</>]
				if(PlayedCard.value.compareTo("[(X)]")==0) { skip=true; } // Skip: [(X)]
				String writeText = (current.isBot?"Bot":"Player") + index + " played:";
				System.out.println(writeText + " " + printCard(PlayedCard) + (PlayedCard.uno?" and has said uno":""));
				for(int i=0;i<players.size();i++) {
					try {
						if(players.get(i).online){players.get(i).outToClient.writeBytes(writeText+PlayedCard.toString()+(PlayedCard.uno?":UNO":"")+"\n");}
					} catch(Exception e){}
				}
			}

			if(current.uno) {
				String writeText = (current.isBot?"Bot":"Player") + index + " has Won the game";
				System.out.println(writeText);
				for(int i=0;i<players.size();i++) {
					try {
						if(players.get(i).online){players.get(i).outToClient.writeBytes("WIN:"+writeText+"\n");}
					} catch(Exception e){}
				}
				System.exit(0);
			}
			if(PlayedCard.uno) {
				current.uno = true;
			} else {current.uno = false;}

			if(clockwise) {
				if(skip){index=index+2;} else {index=index+1;}
			} else {
				if(skip){index=index-2;} else {index=index-1;}
			}
			if(index<0) {
				index = players.size() + index;
			} else if(index>(players.size()-1)) {
				index = 0 + (index - players.size());
			}
		}
	}

	public static void main (String argv[]) {
		Server server;
		if(argv.length == 0) {
			server = new Server(0);
		} else {
			try {
				int numberOfOnlineClients = Integer.parseInt(argv[0]);
				server = new Server(numberOfOnlineClients);
			} catch(Exception e) {}
		}
	}
}
