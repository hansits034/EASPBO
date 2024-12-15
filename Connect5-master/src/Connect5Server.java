import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Connect5Server {

    public static void main(String[] args) throws Exception {

        try (ServerSocket listener = new ServerSocket(5000)) {


            System.out.println("Connect 5 Game Server is Running");
            System.out.println("Listening on IP Address: "+ listener.getInetAddress());
            System.out.println("Listening on Port: "+ listener.getLocalSocketAddress());
            System.out.println("Waiting on Connections... ");

            while (true) {
                Connect5Game game = new Connect5Game();
                Connect5Game.Player player1 = game.new Player(listener.accept(), "RED");
                System.out.println("Player1 has connected.");
                Connect5Game.Player player2 = game.new Player(listener.accept(), "BLUE");
                System.out.println("Player2 has connected.");
                player1.setOpponent(player2);
                player2.setOpponent(player1);
                game.currentPlayer = player1;
                player1.start();
                player2.start();
            }
        }
    }
}


class Connect5Game {

    private Player[] board = {
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null };

    Player currentPlayer;

    /**
     * isWinner() - return true if there is a winner
     */
    public boolean isWinner() {
        // Check for horizontal
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 5; col++) {
                if (board[row * 9 + col] != null &&
                        board[row * 9 + col] == board[row * 9 + col + 1] &&
                        board[row * 9 + col] == board[row * 9 + col + 2] &&
                        board[row * 9 + col] == board[row * 9 + col + 3] &&
                        board[row * 9 + col] == board[row * 9 + col + 4]) {
                    return true;
                }
            }
        }

        // Check for vertical
        for (int col = 0; col < 9; col++) {
            for (int row = 0; row < 5; row++) {  
                if (board[row * 9 + col] != null &&
                        board[row * 9 + col] == board[(row + 1) * 9 + col] &&
                        board[row * 9 + col] == board[(row + 2) * 9 + col] &&
                        board[row * 9 + col] == board[(row + 3) * 9 + col] &&
                        board[row * 9 + col] == board[(row + 4) * 9 + col]) {
                    return true;
                }
            }
        }

        // Check diagonal ascending
        for (int row = 4; row < 9; row++) {
            for (int col = 0; col < 5; col++) {
                if (board[row * 9 + col] != null &&
                        board[row * 9 + col] == board[(row - 1) * 9 + col + 1] &&
                        board[row * 9 + col] == board[(row - 2) * 9 + col + 2] &&
                        board[row * 9 + col] == board[(row - 3) * 9 + col + 3] &&
                        board[row * 9 + col] == board[(row - 4) * 9 + col + 4]) {
                    return true;
                }
            }
        }

        // Check diagonal descending
        for (int row = 4; row < 9; row++) {
            for (int col = 4; col < 9; col++) {
                if (board[row * 9 + col] != null &&
                        board[row * 9 + col] == board[(row - 1) * 9 + col - 1] &&
                        board[row * 9 + col] == board[(row - 2) * 9 + col - 2] &&
                        board[row * 9 + col] == board[(row - 3) * 9 + col - 3] &&
                        board[row * 9 + col] == board[(row - 4) * 9 + col - 4]) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * return true if there is no space
     */
    public boolean boardFilledUp() {
        for (int i = 0; i < board.length; i++) {
            if (board[i] == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * function to check if every player move valid and switch turn if valid
     */
    public synchronized int moveCheck(int location, Player player) {
        if (board[location] == null) {
            board[location] = player;
            currentPlayer = currentPlayer.opponent; // Switch to the opponent
            currentPlayer.opponentMoves(location); // Give info to opponent
            return location;  // return the location if valid
        }
        return -1; // return if move invalid
    }


    /**
     * Player class - extends thread for multithreading server game. 
     * To communicate to player.
     */
    class Player extends Thread {
        String mark;
        Player opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;

        /**
         * constructor function for player - contruct the player and setup socket to communicate.
         */
        public Player(Socket socket, String mark) {
            this.socket = socket;
            this.mark = mark;
            try {
                input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println("WELCOME " + mark);
                output.println("MESSAGE Waiting for your opponent to Connect!");

            } catch (IOException e) {
                System.out.println("Player Left: " + e);

            }
        }

        // setter for opponent 
        public void setOpponent(Player opponent) {
            this.opponent = opponent;

        }

        /**
         * opponentMoves() - Handles messages to inform oppenent moves
         */
        public void opponentMoves(int location) {
            output.println("OPPONENT_MOVED " + location);
            // check if game done
            if(isWinner()){
                output.println("DEFEAT");
            }else{
                if(boardFilledUp()){
                    output.println("TIE");
                }else{
                    output.println("");
                }
            }
        }

        /**
         * function run() to show player when the game started
         */
        public void run() {
            try {
                output.println("MESSAGE All players connected");

                if (mark.equals("RED")) { // send initiate moves
                    output.println("MESSAGE Your move");
                }

                // loop to get player command and process it
                while (true) {
                    String command = input.readLine();
                    if(command == null){
                        System.out.println("Null command");
                        continue;
                    }
                    if (command.startsWith("MOVE")) {
                        int location = Integer.parseInt(command.substring(5));
                        int validlocation = moveCheck(location, this);
                        if (validlocation!= -1) {
                            output.println("VALID_MOVE"+validlocation);
                            if(isWinner()){
                                output.println("VICTORY");
                            }else{
                                if(boardFilledUp()){
                                    output.println("TIE");
                                }else{
                                    output.println("");
                                }
                            };
                        } else {
                            output.println("MESSAGE Wait your Turn");
                        }
                    } else if (command.startsWith("TIME_UP")){
                        System.out.println("TIME UP. Change Turn");
                        opponent.output.println("OPPONENT_TIME_UP ");
                        currentPlayer = currentPlayer.opponent;
                    }else if (command.startsWith("QUIT")) {
                        System.out.println("Player Exited. Game Over.");
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println("Player left: " + e);

            } finally {

                if (opponent != null && opponent.output != null) {
                    opponent.output.println("OTHER_PLAYER_LEFT");
                }
                try {
                    socket.close();
                    System.out.println("Server Side Connection Closed. ");


                } catch (IOException e)
                {System.out.println("Player left: " + e);
                    System.exit(1);
                }

            }
        }
    }
}