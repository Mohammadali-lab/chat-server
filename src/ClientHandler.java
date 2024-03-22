import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClientHandler implements Runnable{

    private static List<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;

    public ClientHandler(Socket socket, BufferedWriter bufferedWriter, BufferedReader bufferedReader, String username) {

        this.socket = socket;
        this.bufferedReader = bufferedReader;
        this.bufferedWriter = bufferedWriter;
        clientUsername = username;
        clientHandlers.add(this);
        System.out.println(clientUsername + " has joint the group");
    }

    @Override
    public void run() {

        String serverMessage = "سرور:" + clientUsername + " به گروه پیوست";
        broadcastMessage(serverMessage);
        if(clientHandlers.size()>1)
            sendMembers();

        String messageFromClient;

        try {
            while(socket.isConnected()) {
                messageFromClient = bufferedReader.readLine();
                String[] destAndMsg = messageFromClient.split(":");
                String destination = destAndMsg[0];
                if(destination.equals("گروه")){
                    messageFromClient = "گروه:"+clientUsername+":"+destAndMsg[1];
                    broadcastMessage(messageFromClient);
                }else{
                    messageFromClient = clientUsername+":"+destAndMsg[1];
                    Optional<ClientHandler> dest = clientHandlers.stream().filter(clientHandler ->
                            clientHandler.clientUsername.equals(destination)).findFirst();
                    if(dest.isPresent()){
                        dest.get().bufferedWriter.write(messageFromClient);
                        dest.get().bufferedWriter.newLine();
                        dest.get().bufferedWriter.flush();
                    } else {
                        System.out.println("Error to find user: " + destination);
                        this.bufferedWriter.write(destination + ":این پیام از سمت سرور است. کاربر " + destination + " دیگر وجود ندارد");
                        this.bufferedWriter.newLine();
                        this.bufferedWriter.flush();
                    }
                }
            }
        } catch (IOException e) {
            closeEverything(socket, bufferedWriter, bufferedReader);
        }
    }

    private void sendMembers(){
        String thisMember = "membersList:"+clientUsername;
        String members = "membersList:";
        for(ClientHandler clientHandler:clientHandlers){
            if(!clientHandler.clientUsername.equals(clientUsername)){
                members += clientHandler.clientUsername + ",";
                try{
                    clientHandler.bufferedWriter.write(thisMember);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }catch (IOException e){
                    closeEverything(clientHandler.socket, clientHandler.bufferedWriter, clientHandler.bufferedReader);
                }
            }
        }
        try {
            this.bufferedWriter.write(members);
            this.bufferedWriter.newLine();
            this.bufferedWriter.flush();
        } catch (IOException e) {
            closeEverything(socket, bufferedWriter, bufferedReader);
        }
    }

    private void broadcastMessage(String message) {
        for(ClientHandler clientHandler: clientHandlers) {
            if(!clientHandler.clientUsername.equals(clientUsername)) {
                try {
                    clientHandler.bufferedWriter.write(message);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                } catch (IOException e) {
                    closeEverything(socket, bufferedWriter, bufferedReader);
                }
            }
        }
    }

    private void closeEverything(Socket socket, BufferedWriter bufferedWriter, BufferedReader bufferedReader) {
        clientHandlers.remove(this);
        System.out.println(clientUsername + " left the group");
        String serverMessage = "سرور:" + clientUsername + " گروه را ترک کرد";
        broadcastMessage(serverMessage);
        try {
            if (bufferedReader != null)
                bufferedReader.close();
            if (bufferedWriter != null)
                bufferedWriter.close();
            if (socket != null)
                socket.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean usernameExistence(String username) {
        Optional<ClientHandler> client = clientHandlers.stream().filter(clientHandler ->
                clientHandler.clientUsername.equals(username)).findFirst();
        return client.isPresent();
    }
}
