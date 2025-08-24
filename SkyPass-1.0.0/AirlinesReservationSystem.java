import java.util.Scanner;

public class AirlinesReservationSystem {

    // ANSI color codes for console output
    public static final String RED = "\033[0;31m";
    public static final String CYAN = "\033[0;36m";
    public static final String RESET = "\033[0m";
    public static final String GREEN = "\033[0;32m";
    public static final String YELLOW = "\033[0;33m";

    private static String displayMessage = "";

    public static void setDisplayMessage(String message) {
        displayMessage = message;
    }

    public static void showDisplayMessage() {
        if (!displayMessage.isEmpty()) {
            System.out.println("\n");
            printCentered(displayMessage);
            displayMessage = "";
        } else {
            System.out.println("\n");
        }
    }

    public static void printCentered(String message) {
        int width = 120;
        int padding = (width - message.length()) / 2;
        System.out.printf("%" + (padding + message.length()) + "s\n", message);
    }

    public static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            System.out.println("Error clearing screen: " + e.getMessage());
        }
    }

    public static void showAppTitle() {
        clearScreen();
        System.out.println(CYAN + """

                \t\t\t\t███████ ██   ██ ██    ██ ██████   █████  ███████ ███████
                \t\t\t\t██      ██  ██   ██  ██  ██   ██ ██   ██ ██      ██
                \t\t\t\t███████ █████     ████   ██████  ███████ ███████ ███████
                \t\t\t\t     ██ ██  ██     ██    ██      ██   ██      ██      ██
                \t\t\t\t███████ ██   ██    ██    ██      ██   ██ ███████ ███████
                    """ + RESET);

        printCentered("╠═════════════ Airlines Reservation System ════════════╣");
    }

    public static void showStartMenu() {
        showDisplayMessage();
        printCentered("""

                \t\t\t\t╔══════════════════════════════════════════════════════╗
                \t\t\t\t║  1. ADMIN login                                      ║
                \t\t\t\t╟──────────────────────────────────────────────────────╢
                \t\t\t\t║  2. Passenger login                                  ║
                \t\t\t\t╟──────────────────────────────────────────────────────╢
                \t\t\t\t║  3. Register                                         ║
                \t\t\t\t╟──────────────────────────────────────────────────────╢
                \t\t\t\t║  4. Exit                                             ║
                \t\t\t\t╚══════════════════════════════════════════════════════╝
                        """);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int choice = 0;

        do {
            try {
                showAppTitle();
                showStartMenu();
                System.out.print("\t\t\t\tEnter your choice: ");
                choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1:
                        User loggedInAdmin = User.userLogin("admin");
                        if (loggedInAdmin != null) {
                            Admin admin = new Admin(loggedInAdmin);
                            admin.adminMenu();
                        }
                        break;
                    case 2:
                        User loggedInPassenger = User.userLogin("passenger");
                        if (loggedInPassenger != null) {
                            Passenger passenger = new Passenger(loggedInPassenger);
                            passenger.passengerMenu();
                        }
                        break;
                    case 3:
                        User.registerUser("passenger");
                        break;
                    case 4:
                        printCentered("Thank you for using SkyPass. Goodbye!");
                        System.exit(0);
                        break;
                    default:
                        setDisplayMessage(RED + "\t ERROR ! Please enter a valid option !" + RESET);
                }
            } catch (NumberFormatException e) {
                setDisplayMessage(RED + "\t ERROR ! Invalid input. Please enter a number." + RESET);
            } catch (Exception e) {
                setDisplayMessage(RED + "\t An unexpected error occurred: " + e.getMessage() + RESET);
                e.printStackTrace();
            }

        } while (choice != 4);
        scanner.close();
    }
}