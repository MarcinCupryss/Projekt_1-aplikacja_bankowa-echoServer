package echoserver;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class EchoServer {

    public static void main(String args[]) {
        CopyOnWriteArrayList<ArrayList<String>> logins = new CopyOnWriteArrayList<>();
        ServerSocket serverSocket = null; // Do akceptowania połączeń i przekazywania ich dalej
        Socket socket = null; // Nawiązywanie połączenia z konkretnym użytkownikiem
        try {
            serverSocket = new ServerSocket(997);
        } catch (IOException e) {
            System.out.println(
                    "Błąd przy tworzeniu gniazda serwerowego " + e);
            System.exit(-1);
        }
        System.out.println("Inicjalizacja gniazda zakończona...");
        System.out.println("Parametry gniazda: " + serverSocket);
        while (true) {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                System.out.println("Błąd wejścia-wyjścia: " + e);
            }
            System.out.println("Nadeszło połączenie...");
            System.out.println("Parametry połączenia: " + socket);
            new Thread(new EchoServerThread(socket, logins)).start();
        }
    }
}