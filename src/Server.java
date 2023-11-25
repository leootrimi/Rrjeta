
import java.io.*;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class Server {

    public static void main(String[] args) throws IOException, InterruptedException {

        InetAddress address = InetAddress.getByName("localhost");
        ServerSocket server = new ServerSocket(3000,3, address);
        System.out.println("Server Started at port : " + address);
        while(true) {

            Socket newClient = server.accept();

            ServerWork serverWorker = new ServerWork(newClient);
            serverWorker.start();

        }
    }
}

class ServerWork extends Thread {

    private Socket clientSocket;
    private ArrayList<Socket> readOnlyUsers = new ArrayList<>();
    private ArrayList<Socket> readWriteUsers = new ArrayList<>();
    private boolean isFirstClient = true;

    public ServerWork(Socket clientSocket) {
        this.clientSocket = clientSocket;
        System.out.println("Client connected...");
        if (isFirstClient) {
            readWriteUsers.add(clientSocket);
            isFirstClient = false;
        } else {
            readOnlyUsers.add(clientSocket);
        }

        for (Socket users : readWriteUsers){
            System.out.println(users);
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

    void startWorker() throws IOException {


        // Getting the input/output streams of the client
        BufferedReader clientInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter clientOutput = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

        // Getting the keyboard input stream
        BufferedReader keyboardInput = new BufferedReader(new InputStreamReader(System.in));


        Thread messagePrinter = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    if(clientSocket.isClosed()) {
                        Thread.interrupted();
                        break;
                    }
                    try {
                        if(clientInput == null) {
                            break;
                        }

                        String messageFromTheClient = clientInput.readLine();
                        if (messageFromTheClient != null) {
                            if (isInReadOnlyUsers(clientSocket)) {
                                System.out.println("Read-only user sent a message: " + messageFromTheClient);
                            }
                            if (isInReadAndWriteUsers(clientSocket)) {
                                System.out.println("Message from client Read N Write: " + messageFromTheClient);
                            }
                            if (messageFromTheClient.equals("exit")) {
                                clientSocket.close();
                                break;
                            }
                        }

                        if(messageFromTheClient.equals("exit")) {
                            clientSocket.close();
                            break;
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        });

        messagePrinter.start();

        while(true) {
            if(clientSocket.isClosed()) {
                break;
            }
            String messageBack = keyboardInput.readLine();

            if(messageBack.equals("exit")) {
                Thread.interrupted();
            }
            clientOutput.println(messageBack);
            clientOutput.flush();

        }

        clientSocket.close();

    }

    private boolean isInReadOnlyUsers(Socket clientSocket) {
        for (Socket socket : readOnlyUsers) {
            if (socket.getPort() == clientSocket.getPort()) {
                return true;
            }
        }
        return false;
    }

    private boolean isInReadAndWriteUsers(Socket clientSocket) {
        for (Socket socket : readWriteUsers) {
            if (socket.getPort() == clientSocket.getPort()) {
                return true;
            }
        }
        return false;
    }

}