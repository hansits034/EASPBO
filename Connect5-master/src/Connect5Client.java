import java.awt.Color;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.Timer;
import java.awt.Font;
import javax.swing.BorderFactory;

public class Connect5Client {

    private JFrame frame = new JFrame("Welcome to Connect 5 Game");
    private JLabel messageLabel = new JLabel("...");

    private Square[] board = new Square[81]; //9 columns x 9 rows
    private Square square;
    private ImageIcon disc;
    private ImageIcon opponentDisc;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String name;
    private boolean isPlayerTurn = false;
    String response;
    private Timer timer;
    private int timeLeft = 15;
    private JLabel timerLabel = new JLabel("Time left: 15s", JLabel.CENTER);
    
    // getter and setter for user name
    public String getName() {
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    // constructor
    public Connect5Client(String serverAddress) throws Exception {

        socket = new Socket(serverAddress, 5000);
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        messageLabel.setBackground(Color.lightGray);
        messageLabel.setBackground(Color.lightGray);
        messageLabel.setFont(new Font("Arial", Font.BOLD, 24)); 
        messageLabel.setHorizontalAlignment(JLabel.CENTER);
        frame.getContentPane().add(messageLabel, BorderLayout.SOUTH);

        JPanel boardPanel = new JPanel();
        boardPanel.setBackground(Color.black);

        boardPanel.setLayout(new GridLayout(9, 9, 3, 3));
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
        // timer label
        timerLabel.setFont(new Font("Arial", Font.BOLD, 48));
        timerLabel.setForeground(Color.RED);
        timerLabel.setOpaque(true);
        timerLabel.setBackground(Color.BLACK);
        timerLabel.setHorizontalAlignment(JLabel.CENTER);
        timerLabel.setVerticalAlignment(JLabel.CENTER); 
        timerLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel timerPanel = new JPanel(new BorderLayout());
        timerPanel.setBackground(Color.BLACK);
        timerPanel.add(timerLabel, BorderLayout.CENTER);

        frame.getContentPane().add(timerPanel, BorderLayout.NORTH);
        frame.getContentPane().add(timerLabel, BorderLayout.NORTH);


    }

    /**
     * play() - main function for user to play, including listens messages from the server
     * & show GUI to player
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
                    // set icon to player red
                    disc = new ImageIcon(getClass().getResource("/resources/redness.png"));
                    opponentDisc = new ImageIcon(getClass().getResource("/resources/Bluey.png"));
                } else {
                    // set icon to player blue
                    disc = new ImageIcon(getClass().getResource("/resources/Bluey.png"));
                    opponentDisc = new ImageIcon(getClass().getResource("/resources/redness.png"));
                }

                frame.setTitle("Connect 5: " + name +" is the colour " + mark + ".");
            }

            while (true) {
                response = in.readLine();
                System.out.println(response);
                if (response.startsWith("VALID_MOVE")) {
                    messageLabel.setText(name+": Valid Move, Opponents Turn, Please Wait...");
                    int chosenBoard = Integer.parseInt(response.substring(10));
                    square = board[chosenBoard];
                    square.setIcon(disc);
                    square.repaint();
                    System.out.println("Valid move made.");
                    // change turn to opponent
                    isPlayerTurn = false;
                    if (timer != null) {
                        timer.stop();
                    }
                } else if (response.startsWith("OPPONENT_MOVED")) {
                    int loc = Integer.parseInt(response.substring(15));
                    board[loc].setIcon(opponentDisc);
                    board[loc].repaint();
                    messageLabel.setText(name+ ": Opponent Moved. Your turn Again!");
                    System.out.println("Opponent Moved.");
                    // change turn to player
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
                    messageLabel.setText(name+": "+messageFill);
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
                    messageLabel.setText(name + ": Your time is up!");
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
     * playAgain() - function to ask player if want to play again or not. If no, close the program. If yes, restart the program
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
     * Class to handle GUI
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
     * main function to start the program
     */
    public static void main(String[] args) throws Exception {
        while (true) {

            Connect5Client c = new Connect5Client("127.0.0.1");//Localhost passed in as serveraddress
            // Scanner s = new Scanner(System.in);
            String playerName = JOptionPane.showInputDialog(c.frame, "Enter your name:");
            if (playerName != null && !playerName.trim().isEmpty()) {
                c.setName(playerName);
            } else {
                JOptionPane.showMessageDialog(c.frame, "Name cannot be empty!");
                return; 
            }

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