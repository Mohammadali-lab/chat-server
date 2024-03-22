import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private ServerSocket serverSocket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private Socket socket;

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void startServer() {
        while (!serverSocket.isClosed()) {

            try {
                socket = serverSocket.accept();
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String username = bufferedReader.readLine();
                if(ClientHandler.usernameExistence(username)){
                    listenForCorrectUsername();
                } else {
                    bufferedWriter.write("OK");
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    ClientHandler clientHandler = new ClientHandler(socket, bufferedWriter, bufferedReader, username);
                    Thread thread = new Thread(clientHandler);
                    thread.start();
                }
            } catch (IOException e) {
                closeEverything(bufferedWriter, bufferedReader, socket);
            }
        }
    }

    private void listenForCorrectUsername() {
        new Thread(() -> {
            try {
                bufferedWriter.write("error");
                bufferedWriter.newLine();
                bufferedWriter.flush();
                while (socket.isConnected()){
                    String newUsername = bufferedReader.readLine();
                    if(ClientHandler.usernameExistence(newUsername)){
                        bufferedWriter.write("error");
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                    } else {
                        bufferedWriter.write("OK");
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                        ClientHandler clientHandler = new ClientHandler(socket, bufferedWriter, bufferedReader, newUsername);
                        Thread thread = new Thread(clientHandler);
                        thread.start();
                        break;
                    }
                }
            } catch (IOException e){
                closeEverything(bufferedWriter, bufferedReader, socket);
            }
        }).start();
    }

    private void closeEverything(BufferedWriter bufferedWriter, BufferedReader bufferedReader, Socket socket){
        try {
            if(bufferedReader != null)
                bufferedReader.close();
            if(bufferedWriter != null)
                bufferedWriter.close();
            if(socket != null)
                socket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(1234);

        Server server = new Server(serverSocket);
        server.startServer();
    }
}
