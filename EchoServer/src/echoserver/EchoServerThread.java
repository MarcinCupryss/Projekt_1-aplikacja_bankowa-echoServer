package echoserver;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class EchoServerThread implements Runnable {

    protected Socket socket;
    CopyOnWriteArrayList<ArrayList<String>> logins;
    private String login;
    private boolean isLoggedIn = false;

    public EchoServerThread(Socket clientSocket) {
        this.socket = clientSocket;
        this.logins = new CopyOnWriteArrayList<>();
    }

    public void run() {
        BufferedReader brinp = null;
        BufferedWriter writer = null;
        String threadName = Thread.currentThread().getName();

        try {
            brinp = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println(threadName + "| Błąd przy tworzeniu strumieni " + e);
            return;
        }
        String line = null;

        while (true) {
            try {
                line = brinp.readLine();
                System.out.println(threadName + "| Line read: " + line);

                if (isLoggedIn) {
                    if ("saldo".equals(line)) {
                        sendUserBalance(writer, threadName, login);
                    } else if ("wplata".equals(line)) {
                        handleDeposit(brinp, writer, threadName, login);
                    } else if ("wyplata".equals(line)) {
                        handleWithdrawal(brinp, writer, threadName, login);
                    } else if ("przelew".equals(line)) {
                        // Obsługa przelewu
                    } else if ("wyloguj".equals(line)) {
                        logout(writer, threadName);
                    } else if ("dane".equals(line)) {
                        showUserInfo(writer, threadName, login);
                    } else if ("haslo".equals(line)) {
                        changePassword(brinp, writer, threadName, login);
                    } else if ("komendy".equals(line)) {
                        getCommands(writer);
                        System.out.println(threadName + "| User " + login + " got commands list.");
                    } else {
                        writer.write("Niepoprawna komenda!" + System.lineSeparator());
                        writer.flush();
                        System.out.println(threadName + "| Invalid operation.");
                    }
                } else if ("zaloguj".equals(line)) {
                    handleLogin(brinp, writer, threadName);
                } else if ("rejestracja".equals(line)) {
                    handleRegistration(brinp, writer, threadName);
                } else if ("lista".equals(line)) {
                    sendUserList(writer);
                } else if ("komendy".equals(line)) {
                    getCommands(writer);
                    System.out.println(threadName + "| Sending commands list.");
                } else {
                    writer.write("Niepoprawna komenda!" + System.lineSeparator());
                    writer.flush();
                    System.out.println(threadName + "| Invalid command.");
                }

            } catch (IOException e) {
                System.out.println(threadName + "| Input/output error." + e);
                return;
            }
        }
    }

    private void getCommands(BufferedWriter writer) throws IOException {
        String commands = "";
        if (isLoggedIn) {
            commands = "Komendy do wyboru: saldo, wplata, wyplata, przelew, komendy, haslo, wyloguj";
        } else {
            commands = "Komendy do wyboru: zaloguj, rejestracja, lista, komendy";
        }
        writer.write(commands + System.lineSeparator());
        writer.flush();
    }

    private void handleLogin(BufferedReader brinp, BufferedWriter writer, String threadName) throws IOException {
        boolean logLoop = true;
        String login, password;
        writer.write("Podaj login (lub wpisz 'anuluj' aby przerwać): " + System.lineSeparator());
        writer.flush();

        while (logLoop) {

            login = brinp.readLine().trim();

            if ("anuluj".equalsIgnoreCase(login)) {
                writer.write("Logowanie anulowane." + System.lineSeparator());
                writer.flush();
                return;
            }

            writer.write("Podaj hasło: " + System.lineSeparator());
            writer.flush();
            password = brinp.readLine().trim();

            if ("anuluj".equalsIgnoreCase(password)) {
                writer.write("Logowanie anulowane." + System.lineSeparator());
                writer.flush();
                return;
            }

            boolean userExists = false;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
                String currentLine;
                while ((currentLine = br.readLine()) != null) {
                    String[] userData = currentLine.split(", ");
                    if (userData.length >= 6 && userData[1].equals(login) && userData[5].equals(password)) {
                        userExists = true;
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println(threadName + "| Error reading file: " + e.getMessage());
            }

            if (userExists) {
                writer.write("Zalogowano!" + System.lineSeparator());
                writer.flush();
                System.out.println(threadName + "| User " + login + " has logged in.");
                setLogin(login);
                isLoggedIn = true;
                logLoop = false;
            } else {
                writer.write("Niepoprawnie wpisano login/hasło. Podaj login." + System.lineSeparator());
                writer.flush();
                System.out.println(threadName + "| User has put incorrect login and/or password.");
            }
        }
    }

    private void handleRegistration(BufferedReader brinp, BufferedWriter writer, String threadName) throws IOException {
        boolean registerLoop = true;

        String login, name, lastname, pesel = "", password = "", confirmPassword, accountNumber;

        while (registerLoop) {
            writer.write("Podaj login: " + System.lineSeparator());
            writer.flush();
            login = brinp.readLine().trim();
            if (isLoginTaken(login)) {
                writer.write("Ten login jest już zajęty. Wybierz inny." + System.lineSeparator());
                writer.flush();
                continue;
            }
            writer.write("Podaj imię: " + System.lineSeparator());
            writer.flush();
            name = brinp.readLine().trim();

            writer.write("Podaj nazwisko: " + System.lineSeparator());
            writer.flush();
            lastname = brinp.readLine().trim();

            boolean isPESELGood = false;
            writer.write("Podaj PESEL: " + System.lineSeparator());
            writer.flush();
            while (!isPESELGood) {
                pesel = brinp.readLine().trim();
                if (!isValidPesel(pesel)) {
                    writer.write("Niepoprawny PESEL. PESEL powinien zawierać 11 cyfr." + System.lineSeparator());
                    writer.flush();
                    continue;
                }
                isPESELGood = true;
            }

            boolean doPasswordsMatch = false;
            writer.write("Podaj hasło: " + System.lineSeparator());
            writer.flush();
            while (!doPasswordsMatch) {
                password = brinp.readLine().trim();
                writer.write("Potwierdź hasło: " + System.lineSeparator());
                writer.flush();
                confirmPassword = brinp.readLine().trim();
                if (!password.equals(confirmPassword)) {
                    writer.write("Hasła nie są zgodne. Spróbuj ponownie podać hasło." + System.lineSeparator());
                    writer.flush();
                } else {
                    doPasswordsMatch = true; // Przerywaj pętlę, jeśli hasła są zgodne
                }
            }

            accountNumber = generateUniqueAccountNumber();

            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("users.txt", true), StandardCharsets.UTF_8))) {
                bw.write(accountNumber + ", " + login + ", " + name + ", " + lastname + ", " + pesel + ", " + password + ", 0.0" + System.lineSeparator());
            } catch (IOException e) {
                System.err.println(threadName + "| Error writing to file: " + e.getMessage());
                return;
            }

            writer.write("Użytkownik " + name + " " + lastname + " został zarejestrowany." + System.lineSeparator());
            writer.flush();
            System.out.println(threadName + "| User registered: " + login);
            registerLoop = false;
        }
    }

    private boolean isLoginTaken(String login) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length >= 2 && userData[1].equals(login)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return false;
    }

    private void sendUserList(BufferedWriter writer) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String currentLine;
            StringJoiner userList = new StringJoiner(", ");
            while ((currentLine = br.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length >= 2 && !userData[1].isEmpty()) {
                    userList.add(userData[1]);
                }
            }
            String userListString = userList.toString();
            if (!userListString.isEmpty()) {
                writer.write("Lista użytkowników: " + userListString + System.lineSeparator());
                writer.flush();
            } else {
                writer.write("Lista użytkowników jest pusta." + System.lineSeparator());
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private void sendUserBalance(BufferedWriter writer, String threadName, String login) throws IOException {
        String userSaldo = findUserSaldo(login);
        writer.write("Środki na koncie: " + userSaldo + " PLN" + System.lineSeparator());
        writer.flush();
        System.out.println(threadName + "| User " + login + " has checked account balance. Balance: " + userSaldo + " PLN");
    }

    private void handleDeposit(BufferedReader brinp, BufferedWriter writer, String threadName, String login) throws IOException {
        boolean validAmount = false;
        double depositAmount = 0;
        writer.write("Wprowadź wartość do wpłacenia:" + System.lineSeparator());
        writer.flush();
        while (!validAmount) {
            System.out.println(threadName + "| Message sent: Enter the amount to deposit:");
            String line = brinp.readLine().trim();

            try {
                depositAmount = Double.parseDouble(line);
                validAmount = true;
            } catch (NumberFormatException e) {
                writer.write("Niepoprawna kwota. Wprowadź liczbę." + System.lineSeparator());
                writer.flush();
            }
        }

        String userSaldo = findUserSaldo(login);
        double saldo = Double.parseDouble(userSaldo);
        saldo += depositAmount;

        updateUserSaldo(login, saldo);
        writer.write(String.format("Na twoje konto wpłynęło: %.2f PLN. Obecny stan konta: %.2f PLN.%n", depositAmount, saldo)); // IDE requires this way
        writer.flush();
    }

    private void handleWithdrawal(BufferedReader brinp, BufferedWriter writer, String threadName, String login) throws IOException {
        boolean validAmount = false;
        double withdrawAmount = 0;
        writer.write("Wprowadź wartość do wypłacenia:" + System.lineSeparator());
        writer.flush();
        while (!validAmount) {
            System.out.println(threadName + "| Message sent: Enter the amount to withdraw:");
            String line = brinp.readLine().trim();
            try {
                withdrawAmount = Double.parseDouble(line);
                validAmount = true;
            } catch (NumberFormatException e) {
                writer.write("Niepoprawna kwota. Wprowadź liczbę." + System.lineSeparator());
                writer.flush();
            }
        }

        String userSaldo = findUserSaldo(login);
        double saldo = Double.parseDouble(userSaldo);

        if (saldo >= withdrawAmount) {
            saldo -= withdrawAmount;
            updateUserSaldo(login, saldo);

            writer.write("Wypłacono: " + withdrawAmount + " PLN. Obecny stan konta: " + saldo + " PLN." + System.lineSeparator());
        } else {
            writer.write("Niewystarczające środki na koncie." + System.lineSeparator());
        }
        writer.flush();
    }

    private void changePassword(BufferedReader brinp, BufferedWriter writer, String threadName, String currentUserLogin) throws IOException {
        String currentPassword, newPassword, confirmNewPassword;
        List<String> lines = new ArrayList<>();

        // Poproś o obecne hasło
        writer.write("Podaj obecne hasło: " + System.lineSeparator());
        writer.flush();
        currentPassword = brinp.readLine().trim();

        boolean passwordCorrect = false;

        // Odczytaj wszystkie linie z pliku i sprawdź obecne hasło
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] userData = line.split(", ");
                if (userData.length >= 7 && userData[1].equals(currentUserLogin)) {
                    if (userData[5].equals(currentPassword)) {
                        passwordCorrect = true;
                    }
                }
                lines.add(line);
            }
        } catch (IOException e) {
            System.err.println(threadName + "| Error reading file: " + e.getMessage());
            return;
        }

        if (!passwordCorrect) {
            writer.write("Niepoprawne obecne hasło. Spróbuj ponownie." + System.lineSeparator());
            writer.flush();
            return;
        }

        // Poproś o nowe hasło i jego potwierdzenie
        writer.write("Podaj nowe hasło: " + System.lineSeparator());
        writer.flush();
        newPassword = brinp.readLine().trim();
        writer.write("Potwierdź nowe hasło: " + System.lineSeparator());
        writer.flush();
        confirmNewPassword = brinp.readLine().trim();

        // Sprawdź, czy nowe hasła są zgodne
        if (!newPassword.equals(confirmNewPassword)) {
            writer.write("Nowe hasła nie są zgodne. Spróbuj ponownie." + System.lineSeparator());
            writer.flush();
            return;
        }

        // Zaktualizuj hasło dla zalogowanego użytkownika
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("users.txt"), StandardCharsets.UTF_8))) {
            for (String line : lines) {
                String[] userData = line.split(", ");
                if (userData.length >= 7 && userData[1].equals(currentUserLogin)) {
                    userData[5] = newPassword; // Zaktualizuj hasło
                    line = String.join(", ", userData);
                }
                bw.write(line + System.lineSeparator());
            }
        } catch (IOException e) {
            System.err.println(threadName + "| Error writing to file: " + e.getMessage());
        }

        writer.write("Hasło zostało zmienione pomyślnie." + System.lineSeparator());
        writer.flush();
    }

    private void showUserInfo(BufferedWriter writer, String threadName, String currentUserLogin) throws IOException {
        String info = "";
        boolean userFound = false;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] userData = line.split(", ");
                if (userData.length >= 7 && userData[1].equals(currentUserLogin)) {
                    String accountNumber = userData[0];
                    String login = userData[1];
                    String name = userData[2];
                    String lastname = userData[3];
                    String pesel = userData[4];
                    String balance = userData[6];

                    info = String.format("Twoje dane (Numer konta - %s, Login - %s, Imię - %s, Nazwisko - %s, PESEL - %s, Saldo - %s PLN)",
                            accountNumber, login, name, lastname, pesel, balance);
                    userFound = true;
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println(threadName + "| Error reading file: " + e.getMessage());
        }

        if (userFound) {
            writer.write(info + System.lineSeparator());
        } else {
            writer.write("Nie znaleziono informacji o użytkowniku." + System.lineSeparator());
        }
        writer.flush();
    }

    private void logout(BufferedWriter writer, String threadName) throws IOException {
        writer.write("Wylogowano!" + System.lineSeparator());
        writer.flush();
        System.out.println(threadName + "| Logged out successfully.");
        login = null;
        isLoggedIn = false;
    }

    private String findUserSaldo(String login) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length >= 7 && userData[1].equals(login)) {
                    return userData[6];
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return "0.0";
    }

    private void updateUserSaldo(String login, double newSaldo) {
        try (BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String currentLine;
            while ((currentLine = file.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length >= 7 && userData[1].equals(login)) {
                    userData[6] = Double.toString(newSaldo);
                    currentLine = String.join(", ", userData);
                }
                lines.add(currentLine);
            }
            try (BufferedWriter fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("users.txt"), StandardCharsets.UTF_8))) {
                for (String writeLine : lines) {
                    fileWriter.write(writeLine + System.lineSeparator());
                }
            }
        } catch (IOException e) {
            System.err.println("Error updating saldo: " + e.getMessage());
        }
    }

    private boolean isValidPesel(String pesel) {
        return pesel.matches("\\d{11}");
    }

    private String generateUniqueAccountNumber() {
        Random rand = new Random();
        String accountNumber;
        Set<String> existingAccountNumbers = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length >= 1) {
                    existingAccountNumbers.add(userData[0]);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        do {
            accountNumber = String.format("%08d", rand.nextInt(100000000));
        } while (existingAccountNumbers.contains(accountNumber));

        return accountNumber;
    }

    private void setLogin(String login) {
        this.login = login;
    }
}