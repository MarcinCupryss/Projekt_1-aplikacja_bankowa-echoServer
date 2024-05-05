/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package echoserver;

/**
 *
 * @author dzelazny
 */
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EchoServer {

    public static void main(String args[]) {
        CopyOnWriteArrayList<ArrayList<String>> logins = new CopyOnWriteArrayList<ArrayList<String>>();
        ArrayList<String> user = new ArrayList<>(); // format "login pass nickname"
        user.add("adam123");
        user.add("pass123");
        user.add("Adam");
        logins.add(user);
        ArrayList<String> user2 = new ArrayList<>(); // format "login pass nickname"
 
        user2.add("dama123");
        user2.add("pass123");
        user2.add("Dama");
        logins.add(user2);
//        ArrayList<ArrayList<String>> logins = new ArrayList<>();
        ServerSocket serverSocket = null;
        Socket socket = null;
        try {
            serverSocket = new ServerSocket(6666);
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
