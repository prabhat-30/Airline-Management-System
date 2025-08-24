import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import org.mindrot.jbcrypt.BCrypt;

public class User {
    private int id;
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String phoneNo;
    private String role;

    // Constructors
    public User() {}

    public User(int id, String username, String firstName, String lastName, String phoneNo, String role) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNo = phoneNo;
        this.role = role;
    }

    // Getters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getRole() { return role; }

    /**
     * Handles the user registration process for a specific role.
     * @param role The role to assign to the new user (e.g., "passenger").
     */
    public static void registerUser(String role) {
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.print("\t\t\t\tEnter a username: ");
            String username = scanner.nextLine();

            // Use the corrected checkUsername method
            if (!checkUsername(username)) {
                AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\t!! Username is already taken !!" + AirlinesReservationSystem.RESET);
                return; // Exit if username is taken
            }

            System.out.print("\t\t\t\tEnter a password (min 8 characters): ");
            String password = scanner.nextLine();
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            System.out.print("\t\t\t\tEnter your first name: ");
            String firstName = scanner.nextLine();
            System.out.print("\t\t\t\tEnter your last name: ");
            String lastName = scanner.nextLine();
            System.out.print("\t\t\t\tEnter your phone number: ");
            String phoneNo = scanner.nextLine();

            Database.databaseQuery(
                    "INSERT INTO users (username, password, first_name, last_name, phone_no, role) VALUES (?, ?, ?, ?, ?, ?);",
                    username, hashedPassword, firstName, lastName, phoneNo, role
            );

            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.GREEN + "\tRegistration successful! Please log in." + AirlinesReservationSystem.RESET);

        } catch (Exception e) {
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tAn error occurred during registration." + AirlinesReservationSystem.RESET);
            e.printStackTrace();
        }
    }

    /**
     * Handles the user login process and verifies their role.
     * @param expectedRole The role the user is expected to have (e.g., "admin", "passenger").
     * @return A User object if login is successful and the role matches, null otherwise.
     */
    public static User userLogin(String expectedRole) {
        Scanner scanner = new Scanner(System.in);
        try {
            System.out.print("\t\t\t\tEnter your username: ");
            String username = scanner.nextLine();
            System.out.print("\t\t\t\tEnter your password: ");
            String password = scanner.nextLine();

            ResultSet rs = (ResultSet) Database.databaseQuery("SELECT * FROM users WHERE username = ?;", username);

            if (rs != null && rs.next()) {
                String storedHash = rs.getString("password");
                if (BCrypt.checkpw(password, storedHash)) {
                    String actualRole = rs.getString("role");
                    // Check if the user's actual role matches the expected role
                    if (expectedRole.equalsIgnoreCase(actualRole)) {
                        return new User(
                                rs.getInt("id"),
                                rs.getString("username"),
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                rs.getString("phone_no"),
                                actualRole
                        );
                    } else {
                        // Password is correct, but role is wrong
                        AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tLogin failed. Access denied for this role." + AirlinesReservationSystem.RESET);
                        return null;
                    }
                }
            }
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tInvalid username or password." + AirlinesReservationSystem.RESET);
            return null;
        } catch (SQLException e) {
            AirlinesReservationSystem.setDisplayMessage(AirlinesReservationSystem.RED + "\tDatabase error during login." + AirlinesReservationSystem.RESET);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * MODIFIED: Checks if a username is available. Now public.
     * @param username The username to check.
     * @return true if the username is available, false otherwise.
     */
    public static boolean checkUsername(String username) throws SQLException {
        ResultSet rs = (ResultSet) Database.databaseQuery("SELECT username FROM users WHERE username = ?;", username);
        if (rs != null && rs.next()) {
            return false; // Username is taken
        }
        return true; // Username is available
    }
}
