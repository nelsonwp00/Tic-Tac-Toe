import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class Client {

    private static JFrame frame = new JFrame();

    private static JMenuBar menuBar = new JMenuBar();
    private static JMenu menu1 = new JMenu("Control");
    private static JMenu menu2 = new JMenu("Help");
    private static JMenuItem exit = new JMenuItem("Exit");
    private static JMenuItem rule = new JMenuItem("Instruction");

    private static JLabel info_label = new JLabel("Enter your player name...");

    private static JPanel boardPanel = new JPanel();
    private static ArrayList<JButton> boardBtn = new ArrayList<JButton>(9);
    private static String btnText = "";
    private static Color btnColor;

    private static JPanel name_panel = new JPanel();
    private static JTextField textField = new JTextField(20);
    private static JButton submit_btn = new JButton("Submit");

    private static Socket socket;
    private static PrintWriter writer;
    private static BufferedReader reader;

    private static boolean gameStart = false;
    private static boolean nameSubmitted = false;
    private static boolean isYourTurn = false;

    public static void main(String[] args) {

        Client game = new Client();
        game.go();
    }

    public void initiateGUI() {

        menu1.add(exit);
        menu2.add(rule);
        menuBar.add(menu1);
        menuBar.add(menu2);
        frame.setJMenuBar(menuBar);
        exit.addActionListener(new exitMenuListener());
        rule.addActionListener(new ruleMenuListener());

        JButton btn;
        for (int i = 0; i < 9; i++) {
            btn = new JButton("");
            btn.addActionListener(new gameBtnListener());
            boardBtn.add(btn);
            boardPanel.add(btn);
        }

        boardPanel.setLayout(new GridLayout(3,3));

        submit_btn.addActionListener(new submitBtnListener());
        name_panel.add(textField);
        name_panel.add(submit_btn);

        frame.add(info_label, BorderLayout.NORTH);
        frame.add(boardPanel, BorderLayout.CENTER);
        frame.add(name_panel, BorderLayout.SOUTH);

        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (socket != null ) {
                    String name = textField.getText();
                    writer.println("quit the game");
                    System.out.println("Disconnecting...");
                }
                System.exit(0);
            }
        });
        frame.setTitle("Tic Tac Toe");
        frame.setSize(400, 450);
        frame.setVisible(true);
    }

    public void go() {

        try {
            initiateGUI();
            JFrame DialogFrame = new JFrame();
            String message;
            socket = new Socket("127.0.0.1", 5000);
            System.out.println("Connected to Server");

            writer = new PrintWriter(socket.getOutputStream(),true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response;
            response = reader.readLine();
            System.out.println("Server response : " + response);

            response = reader.readLine();
            System.out.println("You are " + response);
            String role = determineRole(response);
            String opponent_role;
            Color opponent_color;
            if (role.substring(0,1).equals("X")) {
                opponent_role = "O";
                opponent_color = Color.GREEN;
            } else {
                opponent_role = "X";
                opponent_color = Color.RED;
            }
            System.out.println("Your role is " + role);
            gameStart = true;
            System.out.println("Please submit your name");

            while ((response = reader.readLine()) != null) {
                if (response.equals("Your opponent has moved, now is your turn.")) {
                    info_label.setText(response);
                    int opponent_move = Integer.valueOf(reader.readLine());
                    showOpponentMove(opponent_color, opponent_role, opponent_move);
                    isYourTurn = true;
                }

                else if (response.equals("Valid move, wait for your opponent.")) {
                    info_label.setText(response);
                    isYourTurn = false;
                }

                else if (response.equals("X") || response.equals("O")) {
                    if (response.equals(role.substring(0,1))) {
                        message = "Congratulation! You win.";
                        System.out.println(message);
                        JOptionPane.showMessageDialog(DialogFrame, message);
                        break;
                    }
                    else {
                        message = "You lose.";
                        System.out.println(message);
                        int opponent_move = Integer.valueOf(reader.readLine());
                        showOpponentMove(opponent_color, opponent_role, opponent_move);
                        JOptionPane.showMessageDialog(DialogFrame, message);
                        break;
                    }
                }

                else if (response.equals("Draw.")) {
                    System.out.println("Draw.");
                    if (!isYourTurn) {
                        int opponent_move = Integer.valueOf(reader.readLine());
                        showOpponentMove(opponent_color, opponent_role, opponent_move);
                    }
                    message = "Draw.";
                    JOptionPane.showMessageDialog(DialogFrame, message);
                    break;
                }
            }

            System.out.println("Game ended. Please start a new game.");




        } catch (IOException ex) {
            System.out.println("IOException Client");
        }
    }



    public String determineRole(String response) {

        String str;
        if (response.contains("1")) {
            btnColor = Color.RED;
            btnText = "X";
            str = " ...You start first";
            isYourTurn = true;
        }
        else {
            btnColor = Color.GREEN;
            btnText = "O";
            str = " ...Please wait for your opponent";
        }

        return btnText + str;
    }

    public void showOpponentMove(Color color, String role, int move) {

        JButton btn = boardBtn.get(move);
        btn.setFont(new Font("Arial", Font.BOLD, 40));
        btn.setForeground(color);
        btn.setText(role);
        btn.removeActionListener(btn.getActionListeners()[0]);
    }

    class gameBtnListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            String name = textField.getText();
            for (int i = 0; i < 9; i++) {
                if (e.getSource() == boardBtn.get(i) && nameSubmitted && isYourTurn) {
                    try {
                        writer.println(btnText + " clicked : " + i);
                        System.out.println("You clicked Button " + i);
                        boardBtn.get(i).setFont(new Font("Arial", Font.BOLD, 40));
                        boardBtn.get(i).setForeground(btnColor);
                        boardBtn.get(i).setText(btnText);
                        boardBtn.get(i).removeActionListener(this);
                        break;
                    } catch (Exception ex) {
                        System.out.println("Unable to send Button Clicked");
                    }
                }
            }
        }
    }

    class submitBtnListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameStart) {
                String name = textField.getText();
                try {
                    writer.println("Client name : " + name);
                    System.out.println("Request sent : Client name : " + name);

                    info_label.setText("WELCOME " + name);
                    frame.setTitle("Tic Tac Toe-Player : " + name);
                    textField.setEditable(false);
                    submit_btn.setEnabled(false);
                    nameSubmitted = true;

                } catch (Exception ex) {
                    System.out.println("Unable to send the name message");
                }
            }
        }
    }

    class exitMenuListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            if (socket != null ) {
                String name = textField.getText();
                writer.println("quit the game");
                System.out.println("Disconnecting...");
            }
            System.exit(0);
        }
    }

    class ruleMenuListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            JFrame DialogFrame = new JFrame();
            String message;
            message = "Some information about the game:\n" +
                    "Criteria for a valid move:\n" +
                    "- The move is not occupied by any mark.\n" +
                    "- The move is made in the player’s turn.\n" +
                    "- The move is made within the 3 x 3 board.\n" +
                    "The game would continue and switch among the opposite player " +
                    "until it reaches either one of the following conditions:\n" +
                    "- Player 1 wins.\n" +
                    "- Player 2 wins.\n" +
                    "- Draw.";
            JOptionPane.showMessageDialog(DialogFrame, message);
        }
    }


}
