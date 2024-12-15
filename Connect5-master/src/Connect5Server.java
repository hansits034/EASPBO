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
     * isWinner() - States the situations where a player has won.
     * @return The current state of the board, determines if a player has won
     */
    public boolean isWinner() {

        // 5 Accross - HorizontalCheck
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 5; col++) {  // 5-in-a-row
                if (board[row * 9 + col] != null &&
                        board[row * 9 + col] == board[row * 9 + col + 1] &&
                        board[row * 9 + col] == board[row * 9 + col + 2] &&
                        board[row * 9 + col] == board[row * 9 + col + 3] &&
                        board[row * 9 + col] == board[row * 9 + col + 4]) {
                    return true;
                }
            }
        }

        // Vertical check (down the columns)
        for (int col = 0; col < 9; col++) {
            for (int row = 0; row < 5; row++) {  // 5-in-a-column
                if (board[row * 9 + col] != null &&
                        board[row * 9 + col] == board[(row + 1) * 9 + col] &&
                        board[row * 9 + col] == board[(row + 2) * 9 + col] &&
                        board[row * 9 + col] == board[(row + 3) * 9 + col] &&
                        board[row * 9 + col] == board[(row + 4) * 9 + col]) {
                    return true;
                }
            }
        }

        // Ascending diagonal check (top-left to bottom-right)
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

        // Descending diagonal check (top-right to bottom-left)
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
     * boardFilledUp() - Checks if the board squares are
     * full
     * @return If there is or isnt empty squares.
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
     * moveCheck() - method is called by each player thread when
     * they make a move. This method checks to see if the move is legal
     * and if so updates on both clients GUIs.
     */
    public synchronized int moveCheck(int location, Player player) {
        // Directly check if the chosen location is empty
        if (board[location] == null) {
            board[location] = player;  // Place the player's move
            currentPlayer = currentPlayer.opponent; // Switch to the opponent
            currentPlayer.opponentMoves(location); // Inform opponent of the move
            return location;  // Return the location of the move
        }
        return -1; // Invalid move if the spot is not empty
    }


    /**
     * Player class - extends helper threads for this multithreaded
     * server game. Allows for text (read & write) communication with
     * the client. Consists of a socket with I/O streams.
     */
    class Player extends Thread {
        String mark;//A Player is identified by a character mark which is either '1' or '2'.
        Player opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;

        /**
         * Player constructor - this constructs a handler thread for a given socket
         * and mark. It then initializes the stream fields & displays the initial messages for the
         * players determining the game state.
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

        /**
         * Sets opponent Player.
         */
        public void setOpponent(Player opponent) {
            this.opponent = opponent;

        }

        /**
         * opponentMoves() - Handles messages from the
         * opponent & Checks if the opponent player has won or not
         * and also checks if the board is full.
         */
        public void opponentMoves(int location) {
            output.println("OPPONENT_MOVED " + location);
            output.println(
                    isWinner() ? "DEFEAT" : boardFilledUp() ? "TIE" : "");
        }

        /**
         * run() - Run method of this thread only starts once the two
         * Client players have connected.
         */
        public void run() {
            try {
                output.println("MESSAGE All players connected");

                if (mark.equals("RED")) {//Tells the 1st connected Client that it is their move.
                    output.println("MESSAGE Your move");
                }


                while (true) {// Repeatedly get Client commands & Processes them.
                    String command = input.readLine();
                    if (command.startsWith("MOVE")) {
                        int location = Integer.parseInt(command.substring(5));
                        int validlocation = moveCheck(location, this);
                        if (validlocation!= -1) {
                            output.println("VALID_MOVE"+validlocation);
                            output.println(isWinner() ? "VICTORY"
                                    : boardFilledUp() ? "TIE"
                                    : "");
                        } else {
                            output.println("MESSAGE Wait your Turn");
                        }
                    } else if (command.startsWith("QUIT")) {
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