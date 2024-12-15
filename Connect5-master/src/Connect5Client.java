import java.awt.Color;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Scanner;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.NumberFormat.Style;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.Timer;

public class Connect5Client {

    private JFrame frame = new JFrame("Welcome to Connect 5");
    private JLabel messageLabel = new JLabel("...");

    private Square[] board = new Square[81]; //9 columns x 9 rows
    private Square square;
    private ImageIcon disc;
    private ImageIcon opponentDisc;

    private Socket socket;
    //private Scanner in;
    private BufferedReader in;
    //private PrintWriter out;
    private PrintWriter out;
    private String name;
    private boolean isPlayerTurn = false;
    String response;
    private Timer timer;
    private int timeLeft = 15;
    private JLabel timerLabel = new JLabel("Time left: 15s");


    /**
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(String name){
        this.name = name;
    }


    public Connect5Client(String serverAddress) throws Exception {

        socket = new Socket(serverAddress, 5000);
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        messageLabel.setBackground(Color.lightGray);
        frame.getContentPane().add(messageLabel, BorderLayout.SOUTH);

        JPanel boardPanel = new JPanel();
        boardPanel.setBackground(Color.black);

        boardPanel.setLayout(new GridLayout(9, 9, 3, 3)); //9 columns x 6 Rows
        for (int i = 0; i < board.length; i++) {
            final int j = i;
            board[i] = new Square();
            board[i].addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if(isPlayerTurn){
                        square = board[j];
                        out.println("MOVE " + j);
                        System.out.println("Sent to server: MOVE "+j);
                    }
                }
            });
            boardPanel.add(board[i]);
        }
        frame.getContentPane().add(boardPanel, BorderLayout.CENTER);
        frame.getContentPane().add(timerLabel, BorderLayout.NORTH);
    }

    /**
     * play() - Client main thread play() listens for messages from the server
     * & displays moves to each of the players during the game.
     *
     */
    public void play() throws Exception {
        try {
            String response;
            response = in.readLine();
            System.out.println(response);
            if (response.startsWith("WELCOME")) {
                String mark = response.substring(8);
                if (mark.equals("RED")) {
                    //Sets colour disc for Player1.
                    disc = new ImageIcon(getClass().getResource("/resources/redness.png"));
                    opponentDisc = new ImageIcon(getClass().getResource("/resources/Bluey.png"));
                } else {
                    //Sets colour disc for Player2.
                    disc = new ImageIcon(getClass().getResource("/resources/Bluey.png"));
                    opponentDisc = new ImageIcon(getClass().getResource("/resources/redness.png"));
                }

                frame.setTitle("Connect 5: " + name +" is the colour " + mark + ".");
            }

            while (true) {
                response = in.readLine();
                System.out.println(response);
                if (response.startsWith("VALID_MOVE")) {
                    messageLabel.setText("Valid Move, Opponents Turn, Please Wait...");
                    int chosenBoard = Integer.parseInt(response.substring(10));
                    // if(chosenBoard == 100){
                    //     isPlayerTurn = false;
                    //     System.out.println("Time's up");
                    //     continue;
                    // }
                    square = board[chosenBoard];
                    square.setIcon(disc);
                    square.repaint();
                    System.out.println("Valid move made.");
                    isPlayerTurn = false;
                    if (timer != null) {
                        timer.stop();
                    }
                } else if (response.startsWith("OPPONENT_MOVED")) {
                    int loc = Integer.parseInt(response.substring(15));
                    board[loc].setIcon(opponentDisc);
                    board[loc].repaint();
                    messageLabel.setText("Opponent Moved. Your turn Again!");
                    System.out.println("Opponent Moved.");
                    isPlayerTurn = true;
                    timeLeft = 15;
                    timerLabel.setText("Time left: " + timeLeft + "s");
                    timer = new Timer(1000, e -> {
                        timeLeft--;
                        timerLabel.setText("Time left: " + timeLeft + "s");
                        if (timeLeft <= 0) {
                            timer.stop();
                            JOptionPane.showMessageDialog(frame, "Time's up! Your turn is over.");
                            out.println("TIME_UP");
                            isPlayerTurn = false;
                        }
                    });
                    timer.start();
                }else if (response.startsWith("VICTORY")) {
                    JOptionPane.showMessageDialog(frame, "Congratulations you WON!!!");
                    System.out.println("Player Won.");
                    break;

                } else if (response.startsWith("DEFEAT")) {
                    JOptionPane.showMessageDialog(frame, "Sorry, You LOST!!");
                    System.out.println("Player Lost.");
                    break;

                } else if (response.startsWith("TIE")) {
                    JOptionPane.showMessageDialog(frame, "You TIED with your Opponent!!");
                    System.out.println("Players Tied.");
                    break;

                }else if (response.startsWith("MESSAGE")) {
                    String messageFill = response.substring(8);
                    messageLabel.setText(messageFill);
                    if(messageFill.startsWith("Your move")){
                        System.out.println("Player Turn");
                        isPlayerTurn = true;
                    }
                    System.out.println("Message");
                }
                else if (response.startsWith("OTHER_PLAYER_LEFT")) {
                    JOptionPane.showMessageDialog(frame, "Other Player left");
                    System.out.println("Other Player Exited.");
                    break;
                } else if (response.startsWith("TIME_UP")){
                    messageLabel.setText("Your time is up!");
                } else if(response.startsWith("OPPONENT_TIME_UP")){
                    isPlayerTurn = true;
                    timeLeft = 15;
                    timerLabel.setText("Time left: " + timeLeft + "s");
                    timer = new Timer(1000, e -> {
                        timeLeft--;
                        timerLabel.setText("Time left: " + timeLeft + "s");
                        if (timeLeft <= 0) {
                            timer.stop();
                            JOptionPane.showMessageDialog(frame, "Time's up! Your turn is over.");
                            out.println("TIME_UP");
                            isPlayerTurn = false;
                        }
                    });
                    timer.start();
                }
            }
            out.println("QUIT");


        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            socket.close();
            frame.dispose();
        }
    }



    /**
     * playAgain() -
     * Gives the Players the oppourtunity for a re-match.
     */
    private boolean playAgain() {

        int response = JOptionPane.showConfirmDialog(frame,
                "Want to play again?",
                "GAME OVER" ,
                JOptionPane.YES_NO_OPTION);
        frame.dispose();
        return response == JOptionPane.YES_OPTION;

    }

    /**
     * Square class -
     * Sets up settings for each square in the JFrame incl. text & Icon used.
     */
    static class Square extends JPanel {
        JLabel label = new JLabel((Icon)null);

        public Square() {
            setBackground(Color.white);
            add(label);
        }

        public void setText(char text) {
            label.setText(text + "");
        }

        public void setIcon(Icon icon) {
            label.setIcon(icon);
        }
    }
    /**
     * Main() - Runs the client side application allowing the client to pairs up
     * with the specified server & take part in a game of Connect5 with a second player.
     */
    public static void main(String[] args) throws Exception {



        while (true) {

            Connect5Client c = new Connect5Client("127.0.0.1");//Localhost passed in as serveraddress
            // Scanner s = new Scanner(System.in);
            String playerName = JOptionPane.showInputDialog(c.frame, "Enter your name:");
            if (playerName != null && !playerName.trim().isEmpty()) {
                c.setName(playerName);
            } else {
                // Optional: Handle case if user cancels or enters an empty name
                JOptionPane.showMessageDialog(c.frame, "Name cannot be empty!");
                return; // Exit or prompt for name again
            }

            // System.out.println("Please enter your name: ");
            // c.setName(s.nextLine());
            System.out.println("--- Game Command Log --- ");
            c.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            c.frame.setSize(720, 720);
            c.frame.setVisible(true);
            c.frame.setResizable(false);
            c.play();

            if (!c.playAgain()) {
                break;
            }

        }
    }
}