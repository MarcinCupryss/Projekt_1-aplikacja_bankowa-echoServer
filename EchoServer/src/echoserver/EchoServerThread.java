package echoserver;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/*
Autorzy:
Marcin Cupryś 89529
Piotr Solecki 88349
 */

public class EchoServerThread implements Runnable {

    protected Socket socket;
    private String login;
    private boolean isLoggedIn = false;
    private boolean isAdmin = false;

    public EchoServerThread(Socket clientSocket) {
        this.socket = clientSocket;
    }

    public void run() {
        BufferedReader brinp = null;
        BufferedWriter writer = null;
        String threadName = Thread.currentThread().getName();
        String line = null;

        try {
            brinp = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.out.println(threadName + "| Błąd przy tworzeniu strumieni " + e);
            return;
        }

        while (true) {
            try {
                line = brinp.readLine();
                System.out.println(threadName + "| Line read: " + line);

                if (isLoggedIn && isAdmin) {
                    if ("zmien login".equals(line) || "zmień login".equals(line) || "change login".equals(line)) {
                        changeLogin(brinp, writer, threadName);
                    } else if ("zmien imie".equals(line) || "zmień imię".equals(line) || "change first name".equals(line)) {
                        changeFirstName(brinp, writer, threadName);
                    } else if ("zmien nazwisko".equals(line) || "zmień nazwisko".equals(line) || "change last name".equals(line)) {
                        changeLastName(brinp, writer, threadName);
                    } else if ("zmien pesel".equals(line) || "zmień pesel".equals(line) || "change id".equals(line)) {
                        changePesel(brinp, writer, threadName);
                    } else if ("zmien haslo".equals(line) || "zmień hasło".equals(line) || "change password".equals(line)) {
                        changePassword(brinp, writer, threadName);
                    } else if ("wyloguj".equals(line) || "logout".equals(line)) {
                        logout(writer, threadName);
                    } else if ("komendy".equals(line) || "commands".equals(line)) {
                        getCommands(writer);
                        System.out.println(threadName + "| User " + login + " got commands list.");
                    } else {
                        writer.write("Niepoprawna komenda!" + System.lineSeparator());
                        writer.flush();
                        System.out.println(threadName + "| Invalid operation.");
                    }
                } else if (isLoggedIn && !isAdmin) {
                    if ("saldo".equals(line) || "balance".equals(line)) {
                        sendUserBalance(writer, threadName, login);
                    } else if ("wplata".equals(line) || "wpłata".equals(line) || "deposit".equals(line)) {
                        handleDeposit(brinp, writer, threadName, login);
                    } else if ("wyplata".equals(line) || "wypłata".equals(line) || "withdraw".equals(line)) {
                        handleWithdrawal(brinp, writer, threadName, login);
                    } else if ("przelew".equals(line) || "transfer".equals(line)) {
                        transferMoney(brinp, writer, threadName, login);
                    } else if ("wyloguj".equals(line) || "logout".equals(line)) {
                        logout(writer, threadName);
                    } else if ("dane".equals(line) || "info".equals(line)) {
                        showUserInfo(writer, threadName, login);
                    } else if ("komendy".equals(line) || "commands".equals(line)) {
                        getCommands(writer);
                        System.out.println(threadName + "| User " + login + " got commands list.");
                    } else {
                        writer.write("Niepoprawna komenda!" + System.lineSeparator());
                        writer.flush();
                        System.out.println(threadName + "| Invalid operation.");
                    }
                }
                 else if ("zaloguj".equals(line) || "login".equals(line)) {
                    handleLogin(brinp, writer, threadName);
                } else if ("rejestracja".equals(line) || "register".equals(line)) {
                    handleRegistration(brinp, writer, threadName);
                } else if ("lista".equals(line) || "list".equals(line)) {
                    sendUserList(threadName, writer);
                } else if ("komendy".equals(line) || "commands".equals(line)) {
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
        String commands;
        if (isLoggedIn && isAdmin == true) {
            commands = "Komendy do wyboru: zmień login, " +
                       "zmień imię, zmień nazwisko, zmień pesel, zmień hasło, komendy, wyloguj";
        } else if (isLoggedIn && isAdmin == false){
            commands = "Komendy do wyboru: dane, saldo, wpłata, wypłata, przelew, komendy, wyloguj";
        } else {
            commands = "Komendy do wyboru: zaloguj, rejestracja, lista, komendy";
        }
        writer.write(commands + System.lineSeparator());
        writer.flush();
    }

    private void handleLogin(BufferedReader brinp, BufferedWriter writer, String threadName) throws IOException {
        System.out.println(threadName + "| User wants to log in.");
        boolean logLoop = true;
        String login, password;
        writer.write("Podaj login (lub wpisz 'anuluj' aby przerwać): " + System.lineSeparator());
        writer.flush();

        while (logLoop) {
            login = brinp.readLine().trim();
            if ("anuluj".equalsIgnoreCase(login)) {
                writer.write("Logowanie anulowane." + System.lineSeparator());
                writer.flush();
                System.out.println(threadName + "| User has cancelled logging in");
                return;
            }

            writer.write("Podaj hasło: " + System.lineSeparator());
            writer.flush();
            password = brinp.readLine().trim();
            if ("anuluj".equalsIgnoreCase(password)) {
                writer.write("Logowanie anulowane." + System.lineSeparator());
                writer.flush();
                System.out.println(threadName + "| User has cancelled logging in");
                return;
            }

            boolean userExists = false;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
                String currentLine;
                while ((currentLine = br.readLine()) != null) {
                    String[] userData = currentLine.split(", ");
                    if (userData.length == 9 && userData[1].equals(login) && userData[5].equals(password)) {
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


            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
                String currentLine;
                while ((currentLine = br.readLine()) != null) {
                    String[] userData = currentLine.split(", ");
                    if (userData.length == 9 && userData[1].equals(login) && userData[8].equals("true")) {
                        System.out.println(threadName + "| Logging in " + login + " is admin");
                        isAdmin = true;
                    }
                }
            } catch (IOException e) {
                System.err.println(threadName + "| Error reading file: " + e.getMessage());
            }
        }
    }

    private void handleRegistration(BufferedReader brinp, BufferedWriter writer, String threadName) throws IOException {
        System.out.println(threadName + "| User wants to register a new account");
        boolean registerLoop = true;

        String login, name, lastname, pesel = "", password = "", confirmPassword, accountNumber, pin = "";

        while (registerLoop) {
            writer.write("Podaj login: " + System.lineSeparator());
            writer.flush();
            login = brinp.readLine().trim();
            if (isLoginTaken(threadName, login)) {
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

            boolean isPeselValid = false;
            writer.write("Podaj PESEL: " + System.lineSeparator());
            writer.flush();
            while (!isPeselValid) {
                pesel = brinp.readLine().trim();
                if (!pesel.matches("\\d{11}")) {
                    writer.write("Niepoprawny PESEL. PESEL powinien zawierać 11 cyfr." + System.lineSeparator());
                    writer.flush();
                    continue;
                }
                isPeselValid = true;
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

            boolean isPinValid = false;
            writer.write("Podaj numer PIN: " + System.lineSeparator());
            writer.flush();
            while (!isPinValid) {
                pin = brinp.readLine().trim();
                if (!pin.matches("\\d{4}")) {
                    writer.write("Niepoprawny PIN. PIN powinien zawierać 4 cyfry." + System.lineSeparator());
                    writer.flush();
                    continue;
                }
                isPinValid = true;
            }

            accountNumber = generateUniqueAccountNumber();
            System.out.println(threadName + "| Completed collecting information for registration");

            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream("users.txt", true), StandardCharsets.UTF_8))) {
                bw.write(accountNumber + ", " + login + ", " + name + ", " + lastname + ", " + pesel +
                         ", " + password + ", 0.0, " + pin + ", false" + System.lineSeparator());
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

    private boolean isLoginTaken(String threadName, String login) {
        System.out.println(threadName + "| Checking if new user login is already taken");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length == 9 && userData[1].equals(login)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return false;
    }

    private void sendUserList(String threadName, BufferedWriter writer) throws IOException {
        System.out.println(threadName + "| Sending list of the accounts");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String currentLine;
            StringJoiner userList = new StringJoiner(", ");
            while ((currentLine = br.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length == 9 && !userData[1].isEmpty()) {
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
        System.out.println(threadName + "| " + login + " wants to check account balance");
        String userSaldo = findUserBalance(login);
        writer.write("Środki na koncie: " + userSaldo + " PLN" + System.lineSeparator());
        writer.flush();
        System.out.println(threadName + "| User " + login + " has checked account balance. Balance: " + userSaldo + " PLN");
    }

    private void handleDeposit(BufferedReader brinp, BufferedWriter writer, String threadName, String login) throws IOException {
        System.out.println(threadName + "| " + login + " wants to deposit money");
        boolean validAmount = false;
        double depositAmount = 0;
        writer.write("Wprowadź wartość do wpłacenia:" + System.lineSeparator());
        writer.flush();
        while (!validAmount) {
            System.out.println(threadName + "| Message sent: Enter the amount to deposit:");
            String line = brinp.readLine().trim();
            try {
                depositAmount = Double.parseDouble(line);
                if (depositAmount > 0) {
                    validAmount = true;
                } else {
                    writer.write("Wprowadź kwotę wyższą od 0!" + System.lineSeparator());
                    writer.flush();
                    System.out.println(threadName + "| User entered number lower or equal to 0:");
                }
            } catch (NumberFormatException e) {
                writer.write("Niepoprawna kwota. Wprowadź liczbę." + System.lineSeparator());
                writer.flush();
            }
        }

        String userSaldo = findUserBalance(login);
        assert userSaldo != null;
        double saldo = Double.parseDouble(userSaldo);
        saldo += depositAmount;

        updateUserBalance(login, saldo);
        writer.write(String.format("Na twoje konto wpłynęło: %.2f PLN. Obecny stan konta: %.2f PLN.%n", depositAmount, saldo)); // IDE requires this way
        writer.flush();
        System.out.println(threadName + "| " + login + " has deposited money");
    }

    private void handleWithdrawal(BufferedReader brinp, BufferedWriter writer, String threadName, String login) throws IOException {
        System.out.println(threadName + "| " + login + " wants to withdraw money");
        boolean validAmount = false;
        double withdrawAmount = 0;
        writer.write("Wprowadź wartość do wypłacenia:" + System.lineSeparator());
        writer.flush();
        while (!validAmount) {
            System.out.println(threadName + "| Message sent: Enter the amount to withdraw:");
            String line = brinp.readLine().trim();
            try {
                withdrawAmount = Double.parseDouble(line);
                if (withdrawAmount > 0) {
                    validAmount = true;
                } else {
                    writer.write("Wprowadź kwotę wyższą od 0!" + System.lineSeparator());
                    writer.flush();
                    System.out.println(threadName + "| User entered number lower or equal to 0:");
                }
            } catch (NumberFormatException e) {
                writer.write("Niepoprawna kwota." + System.lineSeparator());
                writer.flush();
            }
        }

        String userBalance = findUserBalance(login);
        assert userBalance != null;
        double balance = Double.parseDouble(userBalance);
        boolean passedPinRequirement = true;

        if (withdrawAmount >= 100) {
            System.out.println(threadName + "| " + login + " wants to withdraw >= 100 PLN. Asking for PIN.");
            passedPinRequirement = gotCorrectPin(brinp, writer, threadName, login, findUserPin(login));
        }

        if (balance >= withdrawAmount && passedPinRequirement) {
            balance -= withdrawAmount;
            updateUserBalance(login, balance);

            writer.write("Wypłacono: " + withdrawAmount + " PLN. Obecny stan konta: " + balance + " PLN." + System.lineSeparator());
            System.out.println(threadName + "| " + login + " has withdrawn money");
        } else if (!passedPinRequirement) {
            writer.write("Trzykrotnie wprowadzono niepoprawny PIN. Wypłata została anulowana. Wprowadź nową komendę." + System.lineSeparator());
        } else {
            writer.write("Niewystarczające środki na koncie." + System.lineSeparator());
        }
        writer.flush();
    }

    private void transferMoney(BufferedReader brinp, BufferedWriter writer, String threadName, String login) throws IOException {
        System.out.println(threadName + "| " + login + " wants to transfer money. Asking for transfer destination.");
        boolean isReceiverAccountNumbValid = false;
        String receiverAccountNumber = "";
        String receiverLogin = "";
        writer.write("Wprowadź numer konta bankowego, do którego chcesz zrobić przelew:" + System.lineSeparator());
        writer.flush();

        while (!isReceiverAccountNumbValid) {
            receiverAccountNumber = brinp.readLine().trim();
            if (!receiverAccountNumber.matches("\\d{8}")) {
                System.out.println(threadName + "| " + login + " provided number that doesn't have 8 digits");
                writer.write("Niepoprawny numer konta. Numer konta bankowego powinien zawierać 8 cyfr:" + System.lineSeparator());
                writer.flush();
                continue;
            }
            receiverLogin = findTransferReceiverLogin(receiverAccountNumber, login, threadName);
            if (receiverLogin == null) {
                System.out.println(threadName + "| " + login + " provided not existing destination.");
                writer.write("Nie znaleziono użytkownika z takim numerem konta. Wprowadź poprawnie numer konta." + System.lineSeparator());
                writer.flush();
                continue;
            }
            isReceiverAccountNumbValid = true;
        }

        boolean validAmount = false;
        double transferAmount = 0;
        writer.write("Wprowadź kwotę przelewu:" + System.lineSeparator());
        writer.flush();
        while (!validAmount) {
            System.out.println(threadName + "| Message sent: Enter the amount of money to transfer:");
            String line = brinp.readLine().trim();
            try {
                transferAmount = Double.parseDouble(line);
                if (transferAmount > 0) {
                    validAmount = true;
                } else {
                    writer.write("Wprowadź kwotę wyższą od 0!" + System.lineSeparator());
                    writer.flush();
                    System.out.println(threadName + "| User entered number lower or equal to 0:");
                }
            } catch (NumberFormatException e) {
                writer.write("Niepoprawna kwota. Wprowadź liczbę." + System.lineSeparator());
                writer.flush();
            }
        }

        String userSendingBalance = findUserBalance(login);
        assert userSendingBalance != null;
        double balanceSender = Double.parseDouble(userSendingBalance);

        String userReceivingBalance = findUserBalance(receiverLogin);
        assert userReceivingBalance != null;
        double balanceReceiver = Double.parseDouble(userReceivingBalance);

        System.out.println(threadName + "| " + login + " wants to transfer money. Asking for pin");
        boolean passedPinRequirement = gotCorrectPin(brinp, writer, threadName, login, findUserPin(login));


        if (balanceSender >= transferAmount && passedPinRequirement) {
            balanceSender -= transferAmount;
            balanceReceiver += transferAmount;
            updateUserBalance(login, balanceSender);
            updateUserBalance(receiverLogin, balanceReceiver);

            writer.write("Przelano: " + transferAmount + " PLN do " + receiverAccountNumber + ". Obecny stan konta: " +
                         balanceSender + " PLN." + System.lineSeparator());
            System.out.println(threadName + "| " + login + " has transfered money to " + receiverLogin);
        } else if (!passedPinRequirement) {
            writer.write("Trzykrotnie wprowadzono niepoprawny PIN. Przelew został anulowany. Wprowadź nową komendę." + System.lineSeparator());
        } else {
            writer.write("Niewystarczające środki na koncie." + System.lineSeparator());
        }
        writer.flush();
    }

    private void changePassword(BufferedReader brinp, BufferedWriter writer, String threadName) throws IOException {///\
        String changingUser = "";
        System.out.println(threadName + "| Admin " + login + " wants to change user's password");
        String currentPassword, newPassword, confirmNewPassword;
        List<String> lines = new ArrayList<>();

        System.out.println(threadName + "| Message sent: Enter login of user whose date you want to change");
        writer.write("Podaj login użytkownika, którego dane chcesz zmienić: " + System.lineSeparator());
        writer.flush();
        changingUser = brinp.readLine().trim();

        System.out.println(threadName + "| Asking for old password");
        writer.write("Podaj obecne hasło: " + System.lineSeparator());
        writer.flush();
        currentPassword = brinp.readLine().trim();

        boolean passwordCorrect = false;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] userData = line.split(", ");
                if (userData.length == 9 && userData[1].equals(changingUser)) {
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

        writer.write("Podaj nowe hasło: " + System.lineSeparator());
        writer.flush();
        newPassword = brinp.readLine().trim();
        writer.write("Potwierdź nowe hasło: " + System.lineSeparator());
        writer.flush();
        confirmNewPassword = brinp.readLine().trim();

        if (!newPassword.equals(confirmNewPassword)) {
            writer.write("Nowe hasła nie są zgodne. Spróbuj ponownie." + System.lineSeparator());
            writer.flush();
            return;
        }

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("users.txt"), StandardCharsets.UTF_8))) {
            for (String line : lines) {
                String[] userData = line.split(", ");
                if (userData.length == 9 && userData[1].equals(changingUser)) {
                    userData[5] = newPassword;
                    line = String.join(", ", userData);
                }
                bw.write(line + System.lineSeparator());
            }
        } catch (IOException e) {
            System.err.println(threadName + "| Error writing to file: " + e.getMessage());
        }

        writer.write("Hasło zostało zmienione pomyślnie." + System.lineSeparator());
        writer.flush();
        System.out.println(threadName + "| " + changingUser + " has set a new password");
    }

    private void showUserInfo(BufferedWriter writer, String threadName, String currentUserLogin) throws IOException {
        System.out.println(threadName + "| " + login + " wants to see his account information");
        String info = "";
        boolean userFound = false;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] userData = line.split(", ");
                if (userData.length == 9 && userData[1].equals(currentUserLogin)) {
                    String accountNumber = userData[0];
                    String login = userData[1];
                    String name = userData[2];
                    String lastname = userData[3];
                    String pesel = userData[4];
                    String balance = userData[6];
                    String pin = userData[7];

                    info = String.format("Twoje dane (Numer konta - %s, Login - %s, Imię - %s, Nazwisko - %s, PESEL - %s, Saldo - %s PLN), PIN - %s",
                            accountNumber, login, name, lastname, pesel, balance, pin);
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
        System.out.println(threadName + "| " + login + " got his account informations");
    }

    private void logout(BufferedWriter writer, String threadName) throws IOException {
        System.out.println(threadName + "| " + login + " wants to log out");
        writer.write("Wylogowano!" + System.lineSeparator());
        writer.flush();
        System.out.println(threadName + "| Logged out successfully.");
        login = null;
        isLoggedIn = false;
        isAdmin = false;
    }

    private String findUserBalance(String login) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length == 9 && userData[1].equals(login)) {
                    return userData[6];
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return null;
    }

    private String findUserPin(String login) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length == 9 && userData[1].equals(login)) {
                    return userData[7];
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return null;
    }

    private void changeLogin(BufferedReader brinp, BufferedWriter writer, String threadName) throws IOException {///\
        String changingUser = "";
        System.out.println(threadName + "| Admin " + login + " wants to change user's login");
        System.out.println(threadName + "| Message sent: Enter login of user whose date you want to change");
        writer.write("Podaj login użytkownika, którego dane chcesz zmienić: " + System.lineSeparator());
        writer.flush();
        changingUser = brinp.readLine().trim();

        System.out.println(threadName + "| Asking for new login");
        writer.write("Wprowadź nowy login!" + System.lineSeparator());
        writer.flush();
        String newLogin = brinp.readLine().trim();
        List<String> users = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length == 9 && userData[1].equals(changingUser)) {
                    userData[1] = newLogin;
                    setLogin(newLogin);
                    currentLine = String.join(", ", userData);
                    System.out.println(threadName + "| " + changingUser + " has new login - " + newLogin);
                }
                users.add(currentLine);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("users.txt"), StandardCharsets.UTF_8))) {
            for (String user : users) {
                bw.write(user + System.lineSeparator());
            }
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }
        writer.write("Zmieniono login na " + newLogin + ". Wprowadź kolejną komendę." + System.lineSeparator());
        writer.flush();
    }

    private void changeFirstName(BufferedReader brinp, BufferedWriter writer, String threadName) throws IOException {///\
        String changingUser = "";
        System.out.println(threadName + "| Admin " + login + " wants to change user's first name");
        System.out.println(threadName + "| Message sent: Enter login of user whose date you want to change");
        writer.write("Podaj login użytkownika, którego dane chcesz zmienić: " + System.lineSeparator());
        writer.flush();
        changingUser = brinp.readLine().trim();

        System.out.println(threadName + "| Asking for new first name");
        writer.write("Wprowadź nowe imię!" + System.lineSeparator());
        writer.flush();
        String newFirstName = brinp.readLine().trim();
        List<String> users = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length == 9 && userData[1].equals(changingUser)) {
                    userData[2] = newFirstName;
                    currentLine = String.join(", ", userData);
                    System.out.println(threadName + "| " + changingUser + " has new first name - " + newFirstName);
                }
                users.add(currentLine);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("users.txt"), StandardCharsets.UTF_8))) {
            for (String user : users) {
                bw.write(user + System.lineSeparator());
            }
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }

        writer.write("Zmieniono imię na " + newFirstName + ". Wprowadź kolejną komendę." + System.lineSeparator());
        writer.flush();
    }

    private void changeLastName(BufferedReader brinp, BufferedWriter writer, String threadName) throws IOException {///\
        String changingUser = "";
        System.out.println(threadName + "| Admin " + login + " wants to change user's last name");
        System.out.println(threadName + "| Message sent: Enter login of user whose date you want to change");
        writer.write("Podaj login użytkownika, którego dane chcesz zmienić: " + System.lineSeparator());
        writer.flush();
        changingUser = brinp.readLine().trim();

        System.out.println(threadName + "| Asking for new last name");
        writer.write("Wprowadź nowe nazwisko!" + System.lineSeparator());
        writer.flush();
        String newLastName = brinp.readLine().trim();
        List<String> users = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length == 9 && userData[1].equals(changingUser)) {
                    userData[3] = newLastName;
                    currentLine = String.join(", ", userData);
                    System.out.println(threadName + "| " + changingUser + " has new last name - " + newLastName);
                }
                users.add(currentLine);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("users.txt"), StandardCharsets.UTF_8))) {
            for (String user : users) {
                bw.write(user + System.lineSeparator());
            }
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }

        writer.write("Zmieniono nazwisko na " + newLastName + ". Wprowadź kolejną komendę." + System.lineSeparator());
        writer.flush();
    }

    private void changePesel(BufferedReader brinp, BufferedWriter writer, String threadName) throws IOException {///\
        String changingUser = "";
        System.out.println(threadName + "| Admin " + login + " wants to change user's PESEL");
        String newPesel = "";
        boolean isPeselValid = false;

        System.out.println(threadName + "| Message sent: Enter login of user whose date you want to change");
        writer.write("Podaj login użytkownika, którego dane chcesz zmienić: " + System.lineSeparator());
        writer.flush();
        changingUser = brinp.readLine().trim();

        System.out.println(threadName + "| Asking for new PESEL (ID number)");
        writer.write("Podaj nowy PESEL: " + System.lineSeparator());
        writer.flush();
        while (!isPeselValid) {
            newPesel = brinp.readLine().trim();
            if (!newPesel.matches("\\d{11}")) {
                writer.write("Niepoprawny nowy PESEL. PESEL powinien zawierać 11 cyfr." + System.lineSeparator());
                writer.flush();
                continue;
            }
            isPeselValid = true;
        }
        List<String> users = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length == 9 && userData[1].equals(changingUser)) {
                    userData[4] = newPesel;
                    currentLine = String.join(", ", userData);
                    System.out.println(threadName + "| " + changingUser + " has new last name - " + newPesel);
                }
                users.add(currentLine);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("users.txt"), StandardCharsets.UTF_8))) {
            for (String user : users) {
                bw.write(user + System.lineSeparator());
            }
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }

        writer.write("Zmieniono PESEL na " + newPesel + ". Wprowadź kolejną komendę." + System.lineSeparator());
        writer.flush();
    }

    private String findTransferReceiverLogin(String receiverAccountNumber, String login, String threadName) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length == 9 && userData[0].equals(receiverAccountNumber)) {
                    System.out.println(threadName + "| " + login + " provided correct destination.");
                    return userData[1];
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return null;
    }

    private boolean gotCorrectPin(BufferedReader brinp, BufferedWriter writer, String threadName, String login, String filePin) throws IOException {
        String pin;
        if (filePin == null) {
            writer.write("Nie znaleziono Twojego numeru PIN" + System.lineSeparator());
            writer.flush();
            return false;
        }

        writer.write("Podaj PIN: " + System.lineSeparator()); // Pierwszy raz pytamy
        writer.flush();
        pin = brinp.readLine().trim();
        if (pin.equals(filePin)) {
            return true;
        }
        System.out.println(threadName + "| User " + login + " failed to enter PIN. Fail counter: 1");
        int attempts = 1;
        while (attempts < 3) {
            writer.write("Niepoprawny PIN. Spróbuj ponownie." + System.lineSeparator());
            writer.flush();
            pin = brinp.readLine().trim();

            if (pin.equals(filePin)) {
                return true;
            } else {
                attempts++;
                System.out.println(threadName + "| User " + login + " failed to enter PIN. Fail counter: " + attempts);
            }
        }

        System.out.println(threadName + "| User " + login + " has exceeded 3 tries to input PIN");
        return false;
    }

    private void updateUserBalance(String login, double newSaldo) {
        try (BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream("users.txt"), StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String currentLine;
            while ((currentLine = file.readLine()) != null) {
                String[] userData = currentLine.split(", ");
                if (userData.length == 9 && userData[1].equals(login)) {
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