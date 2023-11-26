import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Server {

    private static final int PORT = 3000;
    private static final InetAddress ADDRESS = InetAddress.getLoopbackAddress();
    static final Map<String, String> userCredentials = new HashMap<>();

    static {
        userCredentials.put("Leotrim", "passw");
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT, 3, ADDRESS);
        System.out.println("Server Started at port : " + ADDRESS + ":" + PORT);

        Scanner input = new Scanner(System.in);
        System.out.println("Enter the name of the file to store client information: ");
        String filePath = input.next();
        System.out.println("File: " + filePath + " will be created");

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            while (true) {
                Socket newClient = serverSocket.accept();
                ServerWork serverWorker = new ServerWork(newClient, fos);
                serverWorker.start();
            }
        }
    }
}

class ServerWork extends Thread {

    private Socket clientSocket;
    private boolean isReadWriteOnly;
    private FileOutputStream fos;
    private FileInputStream fis;
    public ServerWork(Socket clientSocket, FileOutputStream fos) {
        this.clientSocket = clientSocket;
        this.fos = fos;
        System.out.println("Client connected...");


        if (authenticateClient()) {
            isReadWriteOnly = !isFirstClient();
            System.out.println("User authenticated. Read-Write-only: " + isReadWriteOnly);
        } else {
            System.out.println("Authentication failed.");
        }
    }

    @Override
    public void run() {
        try {
            startWorker();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isFirstClient() {
        // Implement logic to determine if this is the first client
        return false;
    }


    private boolean authenticateClient() {
        try {
            BufferedReader clientInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter clientOutput = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);

            // Ask for username
            clientOutput.println("Enter your username:");
            String username = clientInput.readLine();

            // Ask for password
            clientOutput.println("Enter your password:");
            String password = clientInput.readLine();

            // Check credentials
            return Server.userCredentials.containsKey(username) && Server.userCredentials.get(username).equals(password);

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void startWorker() throws IOException {
        BufferedReader clientInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter clientOutput = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

        Thread messagePrinter = new Thread(() -> {
            while (true) {
                if (clientSocket.isClosed()) {
                    Thread.interrupted();
                    break;
                }
                try {
                    String messageFromTheClient = clientInput.readLine();
                    if (messageFromTheClient != null) {
                        if (isReadWriteOnly) {
                            System.out.println("Message from client Read N Write:" + messageFromTheClient);
                            fos.write((messageFromTheClient + System.lineSeparator()).getBytes());
                            fos.flush();
                        } else {

                        }
                        if (messageFromTheClient.equals("exit")) {
                            clientSocket.close();
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        messagePrinter.start();

        BufferedReader keyboardInput = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            if (clientSocket.isClosed()) {
                break;
            }
            String messageBack = keyboardInput.readLine();

            if (messageBack.equals("exit")) {
                Thread.interrupted();
            }
            clientOutput.println(messageBack);
            clientOutput.flush();
        }

        clientSocket.close();
    }

    private boolean isValidCredentials(String username, String password) {
        return Server.userCredentials.containsKey(username) &&
                Server.userCredentials.get(username).equals(password);
    }

    private void sendFileDataToClient() {
        try {
            BufferedReader fileReader = new BufferedReader(new InputStreamReader(fis));
            PrintWriter clientOutput = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            String line;
            while ((line = fileReader.readLine()) != null) {
                clientOutput.println(line);
                clientOutput.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}