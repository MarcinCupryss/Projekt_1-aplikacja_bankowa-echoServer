package echoserver;

import java.net.*;
import java.io.*;

/*
Autorzy:
Marcin Cupryś 89529
Piotr Solecki 88349
 */

public class EchoServer {

    public static void main(String[] args) {
        ServerSocket serverSocket = null; // Do akceptowania połączeń i przekazywania ich dalej
        Socket socket = null; // Nawiązywanie połączenia z konkretnym użytkownikiem
        try {
            serverSocket = new ServerSocket(997);
        } catch (IOException e) {
            System.out.println(
                    "Error creating server socket " + e);
            System.exit(-1);
        }
        System.out.println("Socket initialization completed...");
        System.out.println("Socket parameters: " + serverSocket);
        while (true) {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                System.out.println("Input/Output Error: " + e);
            }
            System.out.println("Connection received...");
            System.out.println("Connection parameters: " + socket);
            new Thread(new EchoServerThread(socket)).start();
        }
    }
}