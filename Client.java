import java.util.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.ThreadLocalRandom;

public class Client {
	
	ArrayList<Card> hand = new ArrayList<Card>();
	Card PlayedCard;
	Random rnd;

	private String printCard(Card aCard) {
		return "\t"+Server.CARDCOLOR[aCard.color] + " " + aCard.value + " " + Server.RESET;
	}

	public boolean canPlay() {
		for (int i=0; i<hand.size(); i++) {
			if(hand.get(i).value.compareTo(PlayedCard.value)==0 || PlayedCard.color == hand.get(i).color || hand.get(i).color == Card.WILD) { //Same value, same color, or wildcard
				return true;
			}
		}
		return false;
	}
	
	public Card botPlay() {
		rnd = ThreadLocalRandom.current();
		Card cardToPlay = null;
		for (int i=0; i<hand.size(); i++) {
			if(hand.get(i).value.compareTo(PlayedCard.value)==0 || PlayedCard.color == hand.get(i).color || hand.get(i).color == Card.WILD) { //Same value, same color, or wildcard
				if(hand.get(i).color == Card.WILD) {hand.get(i).color = rnd.nextInt(4);}//Just set random color for now, could probably be improved
				cardToPlay = hand.remove(i);
				if(hand.size() == 1) {
					cardToPlay.uno = true;
				}
			}
		}
		return cardToPlay;
	}

	public Client() {
		//Server stuffs
		try {
			Socket aSocket = new Socket("localhost", 2048);
			DataOutputStream outToServer = new DataOutputStream(aSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(aSocket.getInputStream()));
			outToServer.writeBytes("true\n"); //Tell the server that I am a bot
			String[] cardsString = (inFromServer.readLine()).split(";");
			for(int i=0; i<cardsString.length; i++) {
				hand.add(new Card(cardsString[i]));
			}
			while(true) {
				String[] text = (inFromServer.readLine()).split(":");
				if(text[0].compareTo("Starting card")==0) {
					PlayedCard = new Card(text[1]);
					System.out.println("Game has started with " + printCard(PlayedCard));
				} else if(text[0].compareTo("CARD")==0) {
					hand.add(new Card(text[1]));
				} else if(text[0].compareTo("Your turn")==0) {
					if(!canPlay()) {
						while(!canPlay()) {
							outToServer.writeBytes("DRAW:\n");
							hand.add(new Card(inFromServer.readLine()));
						}
					}
					Card cardToPlay = botPlay();
					outToServer.writeBytes("CARD:"+cardToPlay.toString()+(cardToPlay.uno?":UNO\n":"\n"));
					outToServer.writeBytes("DONE:\n");
				} else if(text[0].endsWith(" played")) {
					PlayedCard = new Card(text[1]);
					System.out.println(text[0] + " " + printCard(PlayedCard) + ((text.length==3)?(" " + text[2]):""));
				} else if(text[0].compareTo("WIN")==0) {
					System.out.println(text[1]);
					System.exit(0);
				}
			}
		} catch(Exception e) {}
	}

	public static void main(String argv[]) {
		Client client = new Client();
	}
}
