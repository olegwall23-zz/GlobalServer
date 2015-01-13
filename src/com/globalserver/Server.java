package com.globalserver;

/**
 * Created by Sohan on 08.01.2015.
 */


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class Server implements Runnable {

    public static LinkedList<Server> waitForConnectionToGlobalServer = new LinkedList<>();
    public LinkedList<Socket> waitForUserResponse = new LinkedList<>();
    public static LinkedList<Server> clientsList = new LinkedList<>();
    public static LinkedList<Server> robotsList = new LinkedList<>();

    protected Server robotConnected = null;
    protected byte robotConnectedStatus = 0; // -1 wait, 1 connected

    public void setRobotConnectedStatus(byte status){
        robotConnectedStatus = status;
    }

    public byte getRobotConnectedStatus(){
        return robotConnectedStatus;
    }

    public String getRobotConnectedIP() {
        if(robotConnected != null){
            return robotConnected.clientSocket.getRemoteSocketAddress().toString();
        } else {
            return null;
        }
    }

    public void setRobotConnected(Server robotConnected) {
        this.robotConnected = robotConnected;
    }

    Socket clientSocket;

    Server(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    protected void sendMessage(Socket socket, String message) {
        try{
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void showAllClientsList(){
        String showList = "";
        for(int i = 0; i < clientsList.size(); i++){
            showList += i + 1 + ") Kind: " + "   IP: " + clientsList.get(i).clientSocket.getRemoteSocketAddress() + "\n";
        }
        try{
            DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream());
            outputStream.writeUTF(showList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws Exception {
        ServerSocket serverSocket = new ServerSocket(6789);
        System.out.println("Listening");
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Connected: " + socket.getRemoteSocketAddress());
            Server server = new Server(socket);
            waitForConnectionToGlobalServer.add(server);
            new Thread(server).start();
        }
    }

    public void run() {
        try {

            DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream());

            String message = "";
            while(true){
                char x = (char) inputStream.read();
                while (x != '\n') {
                    message += x;
                    x = (char) inputStream.read();
                }
                message = message.substring(2, message.length()); // delete bomb in UTF
                System.out.println(clientSocket.getRemoteSocketAddress()+ ": " + message);
                new Command(message);
                message = "";
                outputStream.flush(); // заставляем поток закончить передачу данных.
            }
        } catch(Exception x) {
            x.printStackTrace();
            int index = clientsList.indexOf(Server.this);
            if(index > -1){
                clientsList.remove(index);
            } else {
                index = robotsList.indexOf(Server.this);
                if(index > -1){
                    robotsList.remove(index);
                } else {
                    index = waitForConnectionToGlobalServer.indexOf(Server.this);
                    if(index > -1){
                        waitForConnectionToGlobalServer.remove(index);
                    }
                }
            }
            System.out.println("Socket '" + clientSocket.getRemoteSocketAddress() + "' was closed.");
        }
    }

    class Command {

        String message;
        String messageCopy = message;

        public Command(String message){
            this.message = message;
            parseMessage();
        }

        protected String makeNextCommand(String command){
            if(command.indexOf(' ') >= 0) {
                message = command.substring(command.indexOf(' ') + 1, command.length());
                command = command.substring(0, command.indexOf(' '));
            }
            System.out.println(command + "  " + command.length());
            return command;
        }

        protected void showInfo(String message){
            String response = "";
            switch (message){
                case "robot" :
                    if( robotsList.size() > 0){
                        response += "\nRobot info:\n";
                        for(int i = 0; i < robotsList.size(); i++){
                            response += i + 1 + ") IP: " + robotsList.get(i).clientSocket.getRemoteSocketAddress() + " ; Connected: " + robotsList.get(i).getRobotConnectedIP();
                        }
                        sendMessage(response, clientSocket);
                    }  else {
                        sendMessage("Empty set", clientSocket);
                    }
                    break;
                case "user" :
                    if(clientsList.size() > 0){
                        response += "\nUser info:\n";
                        for(int i = 0; i < clientsList.size(); i++){
                            response += i + 1 + ") IP: " + clientsList.get(i).clientSocket.getRemoteSocketAddress() + " ; Connected: " + clientsList.get(i).getRobotConnectedIP();
                        }
                        sendMessage(response, clientSocket);
                    }  else {
                        sendMessage("Empty set", clientSocket);
                    }
                    break;
                case "waiting" :
                    if(waitForConnectionToGlobalServer.size() > 0){
                        response += "\nWaiting clients info:\n";
                        for(int i = 0; i < waitForConnectionToGlobalServer.size(); i++){
                            response += i + 1 + ") IP: " + waitForConnectionToGlobalServer.get(i).clientSocket.getRemoteSocketAddress();
                        }
                        sendMessage(response, clientSocket);
                    }  else {
                        sendMessage("Empty set", clientSocket);
                    }
                    break;
                case "all" :
                    if(robotsList.size() > 0){
                        response += "\nRobot info:\n";
                        for(int i = 0; i < robotsList.size(); i++){
                            response += 1 + ") IP: " + robotsList.get(i).clientSocket.getRemoteSocketAddress() + " ; Connected: " + robotsList.get(i).getRobotConnectedIP() + "\n";
                        }
                    }

                    if(clientsList.size() > 0){
                        response += "\nUser info:\n";
                        for(int i = 0; i < clientsList.size(); i++){
                            response += 1 + ") IP: " + clientsList.get(i).clientSocket.getRemoteSocketAddress() + " ; Connected: " + clientsList.get(i).getRobotConnectedIP() + "\n";
                        }
                    }

                    if(waitForConnectionToGlobalServer.size() > 0){
                        response += "\nWaiting clients info:\n";
                        for(int i = 0; i < waitForConnectionToGlobalServer.size(); i++){
                            response += i + ") IP: " + waitForConnectionToGlobalServer.get(i).clientSocket.getRemoteSocketAddress() + "\n";
                        }
                    }

                    sendMessage(response, clientSocket);

                    break;
                case "id" :
                    response += "\nInfo ID: \n";
                    LinkedList list = parseIds(makeNextCommand(message));
                    for(int i = 0; i < list.size(); i++){
                        for(int j = 0; j < clientsList.size(); j++){
                            if(clientsList.get(j).clientSocket.getRemoteSocketAddress().toString().equals(list.get(i))){
                                response += i + 1 + ") List: Client; Connected: " + clientsList.get(j).getRobotConnectedIP();
                            }
                        }
                        for(int j = 0; j < robotsList.size(); j++){
                            if(robotsList.get(j).clientSocket.getRemoteSocketAddress().toString().equals(list.get(i))){
                                response += i + 1 + ") List: Robot; Connected: " + robotsList.get(j).getRobotConnectedIP();
                            }
                        }
                        for(int j = 0; j < waitForConnectionToGlobalServer.size(); j++){
                            if(waitForConnectionToGlobalServer.get(j).clientSocket.getRemoteSocketAddress().toString().equals(list.get(i))){
                                response += i + 1 + ") List: waitForUserResponse";
                            }
                        }
                    }
                    sendMessage(response, clientSocket);
                    break;
                case "my" :
                    sendMessage("ID: " + clientSocket.getRemoteSocketAddress() + " ; Connected: " + getRobotConnectedIP(), clientSocket);
            }
        }

        protected void parseMessage(){
            switch (makeNextCommand(message)) {
                case "show" :
                    switch (makeNextCommand(message)) {
                        case "info" :
                            switch (makeNextCommand(message)){
                                case "robot" : showInfo("robot");
                                    break;
                                case "user" : showInfo("user");
                                    break;
                                case "waiting" : showInfo("waiting");
                                    break;
                                case "all" : showInfo("all");
                                    break;
                                case "id" : showInfo("id");
                                    break;
                                case "my" : showInfo("my");
                                    break;
                                default:
                                    sendMessage("Bad request '" + messageCopy + "' at " + message, clientSocket);
                            }
                            break;
                        default:
                            sendMessage("Bad request '" + messageCopy + "' at " + message, clientSocket);
                    }
                    break;

                case "connect" :
                    switch (makeNextCommand(message)){
                        case "user" : requestConnectToUser(makeNextCommand(message));
                            break;
                        case "robot": connectToRobot(makeNextCommand(message));
                            break;
                        case "server":
                            connectToGlobalServer(makeNextCommand(message));
                            break;
                        default:
                            sendMessage("Bad request '" + messageCopy + "' at " + message, clientSocket);
                    }
                    break;

                case "disconnect" : //todo
                    break;

                case "send" : //todo
                    switch (makeNextCommand(message)){
                        case "message" : //todo
                            break;
                        case "image" ://todo
                            break;
                        case "video" : //todo
                            break;
                        case "echo" : //todo
                            break;
                        default:
                            sendMessage("Bad request '" + messageCopy + "' at " + message, clientSocket);
                    }
                    break;

                case "RTC" :
                    switch (makeNextCommand(message)){
                        case "accelerometer" : updateAccelerometerData(makeNextCommand(message));
                            break;
                        case "motors" : updateMotorsData(makeNextCommand(message));
                            break;
                        default:
                            sendMessage("Bad request '" + messageCopy + "' at " + message, clientSocket);
                    }
                    break;


                case "CTR" :
                    switch (makeNextCommand(message)){
                        case "motion" : robotMotion(makeNextCommand(message));
                            break;
                        default:
                            sendMessage("Bad request '" + messageCopy + "' at " + message, clientSocket);
                    }
                    break;

                default:
                    sendMessage("Bad request '" + messageCopy + "' at " + message, clientSocket);
            }
        }

        protected void robotMotion(String command){
            //todo
        }

        protected void updateAccelerometerData(String data){
            //todo
        }

        protected void updateMotorsData(String data){
            //todo
        }

        protected void connectToRobot(String robotIP){
            for(int i = 0; i < robotsList.size(); i++){
                if(robotsList.get(i).clientSocket.getRemoteSocketAddress().toString().equals(robotIP)){
                    sendMessage("request connect", robotsList.get(i).clientSocket);
                    robotConnected = robotsList.get(i);
                } else {
                    sendMessage("ERROR", clientSocket);
                }
            }
        }

        protected void connectToGlobalServer(String msg){
            if(msg.equals("client")){
                for(int i = 0; i < waitForConnectionToGlobalServer.size(); i++){
                    if(waitForConnectionToGlobalServer.get(i).clientSocket.getRemoteSocketAddress().toString().equals(clientSocket.getRemoteSocketAddress().toString())){
                        clientsList.add(waitForConnectionToGlobalServer.get(i));
                        waitForConnectionToGlobalServer.remove(i);
                        return;
                    }
                }
            } else if(msg.equals("robot")){
                for(int i = 0; i < waitForConnectionToGlobalServer.size(); i++){
                    if(waitForConnectionToGlobalServer.get(i).clientSocket.getRemoteSocketAddress().toString().equals(clientSocket.getRemoteSocketAddress().toString())){
                        robotsList.add(waitForConnectionToGlobalServer.get(i));
                        waitForConnectionToGlobalServer.remove(i);
                        return;
                    }
                }
            }
        }

        protected LinkedList parseIds(String command){
            LinkedList<String> list = new LinkedList<>();
            System.out.println("command__ "+command);
            command = command.substring(command.indexOf('[') + 1, command.indexOf(']'));
            while (true) {
                int index = command.indexOf(',');
                if(index >= 0){
                    list.add(command.substring(0, index));
                    command = command.substring(index + 1, command.length());
                } else {
                    list.add(command);
                    break;
                }
            }
            return list;
        }

        protected void requestConnectToUser(String command){
            LinkedList list = parseIds(command);
            for(int i = 0; i < list.size(); i++){
                for(int j = 0; j < clientsList.size(); j++){
                    if(clientsList.get(j).clientSocket.getRemoteSocketAddress().toString().equals(list.get(i))){
                        sendMessage("^connect request " + clientSocket.getRemoteSocketAddress() + "^", clientsList.get(j).clientSocket);
                        waitForUserResponse.add(clientsList.get(j).clientSocket);
                    }
                }
            }
        }

        protected void connectRequest(String command){
            LinkedList<String> list = new LinkedList<>();
            System.out.println("command__ "+command);
            command = command.substring(command.indexOf('[') + 1, command.indexOf(']'));
            while (true) {
                int index = command.indexOf(',');
                if(index >= 0){
                    list.add(command.substring(0, index));
                    command = command.substring(index + 1, command.length());
                } else {
                    list.add(command);
                    break;
                }
            }
            for(int i = 0; i < list.size(); i++){
                for(int j = 0; j < clientsList.size(); j++){
                    if(clientsList.get(j).clientSocket.getRemoteSocketAddress().toString().equals(list.get(i))){
                        sendMessage("^connect request " + clientSocket.getRemoteSocketAddress() + "^", clientsList.get(j).clientSocket);
                    }
                }
            }
        }

        protected LinkedList getIdsFromMessage(String command){
            LinkedList<String> list = new LinkedList<>();
            String message = command.substring(command.indexOf(']') + 1, command.length());
            command = command.substring(command.indexOf('[') + 1, command.indexOf(']'));
            while (true) {
                int index = command.indexOf(',');
                if(index >= 0){
                    list.add(command.substring(0, index));
                    command = command.substring(index + 1, command.length());
                } else {
                    list.add(command);
                    break;
                }
            }
            return list;
        }

        protected void sendMessage(String message, Socket clientSocket){
            try{
                DataOutputStream outputStream = new DataOutputStream(clientSocket.getOutputStream());
                outputStream.writeUTF(message);
            } catch (Exception x){ x.printStackTrace(); }
        }

    }
}