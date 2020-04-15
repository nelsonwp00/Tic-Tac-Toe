import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;

public class Server implements Runnable{

    private static ServerSocket serverSocket ;
    private static Socket socket;
    private static ArrayList<String> players = new ArrayList<String>(0);
    private static int nameCount = 0;
    private static int turnNumber = 1;
    private static String[] turnList = new String[9];
    private static int[] move = new int[9];
    private static String[] gameBoard = new String[9];
    private static boolean gameEnded;
    private static int disconnectClient = 0;
    private static boolean lock_thread;

    public static void main(String[] args) {
        Server server = new Server();
        try {
            serverSocket = new ServerSocket(5000);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        Thread clientA = new Thread(server);
        Thread clientB = new Thread(server);
        clientA.setName("Thread A");
        clientB.setName("Thread B");
        clientA.start();
        clientB.start();
    }


    @Override
    public void run() {

        String threadName = Thread.currentThread().getName();
        try {

            while(true) {
                gameEnded = false;
                System.out.println();
                System.out.println(threadName + " waiting for client connection...");
                socket = serverSocket.accept();
                players.add(threadName);
                System.out.println(threadName + " connected to client");
                System.out.println("Number of Players : " + players.size());

                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                isTwoClients(writer);
                for (int i = 0; i < 2; i++) {
                    if (players.get(i) == threadName) {
                        writer.println("Player " + (i+1));
                    }
                }

                //System.out.println(threadName + " waiting for client name...");
                System.out.println(threadName + " " + reader.readLine());
                nameCount++;
                System.out.println("nameCount = " + nameCount);
                nameRegistration(nameCount);
                initializeGame();

                if (!(threadName.equals(turnList[0]))) {
                    //System.out.println(threadName + " pending to takeTurn()...");
                    lockThread();
                }

                while (turnNumber != 10 && !gameEnded) {
                    int temp = turnNumber;
                    takeTurn(writer, reader);

                    if (determineWinner(writer)) {
                        gameEnded = true;
                        System.out.println("Game ended --- Status : Win/Lose");
                        System.out.println("////////////////");
                        if (threadName.equals(turnList[turnNumber-2])) {
                            writer.println(move[turnNumber-1]);
                        } else {
                            unlockThread();
                        }
                        break;
                    }

                    if (turnNumber == temp) {
                        if (turnNumber == 1) {
                            try {
                                // In case another thread has not been locked and this thread finished turn 1
                                Thread.sleep(200);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
                        turnNumber++;
                        unlockThread();
                    }
                }

                draw(writer);


            }
        } catch (IOException ex) {
            System.out.println(threadName + " client disconnected - Exception Caught");
            players.remove(threadName);
            System.out.println("Number of players : " + players.size());
            run();
        }
    }

    public synchronized void isTwoClients(PrintWriter writer) {

        String threadName = Thread.currentThread().getName();

        if (players.size() != 2) {
            writer.println("Please wait for another player");
            try {
                System.out.println(threadName + " : waiting for another player");
                wait();
            } catch (InterruptedException ex) {
                System.out.println("InterruptedException - isGameStart()");
            }

        }
        else {
            writer.println("Game start");
            System.out.println("Starting game...");
            for (int i = 0; i < 2; i++) {
                System.out.println("Player " + i + " : " + players.get(i));
            }
            notify();
        }

    }

    public synchronized void nameRegistration(int nameCount) {

        String threadName = Thread.currentThread().getName();
        if (nameCount == 2) {
            notify();
            //System.out.println("Finish Name Registration");
            //System.out.println("Initializing turnlist...");
        }
        else {
            try {
                //System.out.println(threadName + " is waiting for another thread nameRegistration()");
                wait();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    public synchronized void initializeGame() {

        String threadName = Thread.currentThread().getName();

        // Initialize turnList
        if (players.get(0).equals(threadName)) {
            System.out.println("----------------------------");
            //System.out.println(threadName + " plays first");
            for(int i = 0; i < 9; i+=2) {
                turnList[i] = threadName;
            }
        } else {
            for(int i = 1; i < 9; i+=2) {
                turnList[i] = threadName;
            }
        }
        for (int i = 0; i < 2; i++) {
            if (turnList[i] == null) {
                System.out.println(threadName + " waiting turnList to be completed...");
                lockThread();
                //System.out.println(threadName + " Game starts. 1");
                break;
            }
            else if (i == 1){
                // Initialize gameBoard
                for(int j = 0; j < 9; j++) {
                    gameBoard[j] = String.valueOf(j);
                }
                unlockThread();
                //System.out.println(threadName + " Game starts. 0");
            }
        }
    }

    public synchronized void takeTurn(PrintWriter writer, BufferedReader reader) {
        String threadName = Thread.currentThread().getName();
        String request = "";

        if (turnList[(turnNumber-1)].equals(threadName) && !gameEnded) {
            try {
                System.out.println("----------------------------");
                System.out.println("Turn : " + turnNumber);
                System.out.println("It is " + threadName + "'s turn");

                if (turnNumber > 1) {
                    writer.println("Your opponent has moved, now is your turn.");
                    writer.println(move[turnNumber-2]);
                }

                request = reader.readLine();
                System.out.println(threadName + " : " + request);

                if (!request.equals("quit the game")) {
                    int buttonClicked = Integer.valueOf(request.substring(request.indexOf(":") + 2));
                    String buttonText = request.substring(0, 1);
                    gameBoard[buttonClicked] = buttonText;
                    move[turnNumber - 1] = buttonClicked;
                }
                else {
                    disconnectClient++;
                    gameEnded = true;

                }

            } catch (IOException ex) {
                System.out.println(threadName + " - IOException takeTurn()");
            }

        } else if (!gameEnded){
            try {
                writer.println("Valid move, wait for your opponent.");
                System.out.println(threadName + " in takeTurn() waiting...");
                wait();

            } catch (InterruptedException ex) {
                System.out.println(threadName + "in takeTurn() InterruptedException");
            }
        }
    }

    public synchronized boolean determineWinner(PrintWriter writer) {

        int h = 0;
        int temp = 0;
        boolean determineWin = false;
        for (int i = 0; i < 3; i++) {

            // Horizontal
            h = temp + i;
            if (gameBoard[h].equals(gameBoard[h+1]) && gameBoard[h+1].equals(gameBoard[h+2])) {
                writer.println(gameBoard[h]);
                System.out.println(gameBoard[h] + " wins - Horizontal");
                determineWin = true;
                return true;
            }
            temp += 2;

            // Vertical
            if (gameBoard[i].equals(gameBoard[i+3]) && gameBoard[i+3].equals(gameBoard[i+6])) {
                writer.println(gameBoard[i]);
                System.out.println(gameBoard[i] + " wins - Vertical");
                determineWin = true;
                return true;
            }
        }

        // Cross
        if (!determineWin) {
            if (gameBoard[0].equals(gameBoard[4]) && gameBoard[4].equals(gameBoard[8])) {
                writer.println(gameBoard[0]);
                System.out.println(gameBoard[0] + " wins - Cross");
                return true;
            }
            else if (gameBoard[2].equals(gameBoard[4]) && gameBoard[4].equals(gameBoard[6])) {
                writer.println(gameBoard[2]);
                System.out.println(gameBoard[2] + " wins - Cross");
                return true;
            }
        }


        return false;
    }

    public synchronized void draw(PrintWriter writer) {

        String threadName = Thread.currentThread().getName();

        if (turnNumber == 10 && !gameEnded) {
            //System.out.println("Game ended --- Status : Draw");
            writer.println("Draw.");
            if (threadName.equals(turnList[7])) {
                writer.println(move[turnNumber-2]);
            }
            disconnectClient++;
            if (disconnectClient == 1) {
                //System.out.println(threadName + " waiting for game end...");
                lockThread();
            }
            else if (disconnectClient == 2){
                System.out.println("Game ended --- Status : Draw");
                System.out.println("////////////////");
                //System.out.println(threadName + " releaseLock()");
                unlockThread();
                gameEnded = true;
            }
        }
    }

    public synchronized void lockThread() {
        try {
            //System.out.println(threadName + " is locked");
            lock_thread = true;
            wait();
            //System.out.println(threadName + " is unlocked");
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void unlockThread() {
        lock_thread = false;
        notify();
    }




}
