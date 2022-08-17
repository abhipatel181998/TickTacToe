package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Server {
    //Srver Configurations.
    private final int port = 9001;

    //ArrayList stores game players' thread objects.
    private ArrayList<HandlePlayers> playersThreadList = new ArrayList<>();

    //Stores palyers' moves.
    private ArrayList<Integer> player1Moves = new ArrayList<>();
    private ArrayList<Integer> player2Moves = new ArrayList<>();

    //Track number of moves made by the user.
    private int numberOfMovesMade = 0;

    //Main method.
    public static void main(String args[]) {
        new Server();
    }

    //Default Constructor that starts the server and runs
    //and manages the playes and game.
    public Server() {

        try {
            //Creating object of server socket.
            ServerSocket serverSocket = new ServerSocket(port);

            while (true) {
                //Accept client request.
                Socket socket = serverSocket.accept();

                //Allow ony two clients to connect.
                //And start thread for each client.
                if (playersThreadList.size() < 2) {
                    HandlePlayers handlePlayers = new HandlePlayers(socket);
                    playersThreadList.add(handlePlayers);
                    handlePlayers.start();
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Thread class to handle client response.
    class HandlePlayers extends Thread {

        //To store existing socket object.
        private Socket socket;

        private DataInputStream dataInputStreamFromPlayer = null;
        private DataOutputStream dataOutputStreamToPlayer = null;

        public HandlePlayers(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                //creating object of dataInputStream and dataOutputStream
                //to send and recieve messages to and from the playes.
                dataInputStreamFromPlayer = new DataInputStream(socket.getInputStream());
                dataOutputStreamToPlayer = new DataOutputStream(socket.getOutputStream());
                
                //Assigning tokens(X fror 1st player, O to 2nd Player).
                assignTokens(playersThreadList);
                while (true) {
                    String messageFromClient = dataInputStreamFromPlayer.readUTF();
                    sendToAllPlayes(playersThreadList, messageFromClient);

                }
            }
            //When one of the players get disconnected from the srever.
            catch (SocketException s_ex) {
                sendToAllPlayes(playersThreadList, "txtMessage:Player disconnected. Game Stopped.");
                for (Thread player : playersThreadList) {
                    player.stop();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        //Send message to all players.
        private void sendToAllPlayes(ArrayList<HandlePlayers> playersThreadList, String message) {
            String[] spiltMessage = message.split(":");

            try {
                //Increase number of moves made by client.
                numberOfMovesMade++;
                
                //Runs when Player1(X) has made the move.
                if (spiltMessage[0].equals("X")) {
                    player1Moves.add(Integer.parseInt(spiltMessage[1]));

                    playersThreadList.get(0).dataOutputStreamToPlayer.writeUTF("disableAll");
                    playersThreadList.get(0).dataOutputStreamToPlayer.writeUTF("txtMessage:Player 2's turn.");
                    playersThreadList.get(1).dataOutputStreamToPlayer.writeUTF("txtMessage:Your turn.");
                    playersThreadList.get(1).dataOutputStreamToPlayer.writeUTF("enable");

                }
                
                //Runs when player2(O) has made the move.
                if (spiltMessage[0].equals("O")) {
                    player2Moves.add(Integer.parseInt(spiltMessage[1]));

                    playersThreadList.get(1).dataOutputStreamToPlayer.writeUTF("disableAll");
                    playersThreadList.get(1).dataOutputStreamToPlayer.writeUTF("txtMessage:Player 1's turn.");
                    playersThreadList.get(0).dataOutputStreamToPlayer.writeUTF("txtMessage:Your turn.");
                    playersThreadList.get(0).dataOutputStreamToPlayer.writeUTF("enable");

                }

                //Sends message to both the players.
                for (int i = 0; i < playersThreadList.size(); i++) {
                    playersThreadList.get(i).dataOutputStreamToPlayer.writeUTF(message);

                }

                //Checks for the winners and stops the game for playes.
                if (checkWinner() > 0) {
                    message = "txtMessage:Player " + checkWinner() + " wins the game.";
                    for (int i = 0; i < playersThreadList.size(); i++) {
                        playersThreadList.get(i).dataOutputStreamToPlayer.writeUTF(message);
                    }

                    playersThreadList.get(1).dataOutputStreamToPlayer.writeUTF("disableAll");
                    playersThreadList.get(0).dataOutputStreamToPlayer.writeUTF("disableAll");
                }
                //When game is draw and no one wins.
                else if (checkWinner() == 0 && numberOfMovesMade == 9) {
                    for (int i = 0; i < playersThreadList.size(); i++) {
                        playersThreadList.get(i).dataOutputStreamToPlayer.writeUTF("txtMessage:Match Draw.");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Cheks who wins the game.
        private int checkWinner() {

            //Possible winning conditions.
            List topRow = Arrays.asList(1, 2, 3);
            List midRow = Arrays.asList(4, 5, 6);
            List botRow = Arrays.asList(7, 8, 9);
            List leftCol = Arrays.asList(1, 4, 7);
            List midCol = Arrays.asList(2, 5, 8);
            List rightRow = Arrays.asList(3, 6, 9);
            List cross1 = Arrays.asList(1, 5, 9);
            List cross2 = Arrays.asList(7, 5, 3);

            List<List> winningRules = new ArrayList<List>();

            winningRules.add(topRow);
            winningRules.add(midRow);
            winningRules.add(botRow);
            winningRules.add(leftCol);
            winningRules.add(midCol);
            winningRules.add(rightRow);
            winningRules.add(cross1);
            winningRules.add(cross2);

            //Checks for the wining condition for each move made by player.
            //And returns player number who won the game.
            for (List rule : winningRules) {
                if (player1Moves.containsAll(rule)) {
                    return 1;
                }
                if (player2Moves.containsAll(rule)) {
                    return 2;
                }
            }
            
            //If no one win the game.
            return 0;
        }

        //Assign tokens(X fror 1st player, O to 2nd Player)
        private void assignTokens(ArrayList<HandlePlayers> playersThreadList) {
            for (int i = 0; i < playersThreadList.size(); i++) {
                try {
                    //Assign token X to first player.
                    if (playersThreadList.size() == 1) {
                        playersThreadList.get(0).dataOutputStreamToPlayer.writeUTF("tocken:X");
                        playersThreadList.get(0).dataOutputStreamToPlayer.writeUTF("txtMessage:Waiting for player 2. Your token in X.");
                    }
                    //Assign token O to 2nd player.
                    if (playersThreadList.size() == 2) {
                        playersThreadList.get(0).dataOutputStreamToPlayer.writeUTF("txtMessage: Player 2 Connected. Start the game.");
                        playersThreadList.get(0).dataOutputStreamToPlayer.writeUTF("enableAll");
                        playersThreadList.get(1).dataOutputStreamToPlayer.writeUTF("tocken:O");
                        playersThreadList.get(1).dataOutputStreamToPlayer.writeUTF("txtMessage:Your tocken is O. Player 1 will play first.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
