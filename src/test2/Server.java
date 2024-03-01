package test2;

import java.io.*;
import java.net.*;
import java.sql.*;

public class Server {
    private static final String URL = "jdbc:mysql://localhost:3306/grading_sys";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    public static void main(String[] args) {

        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server started. Listening for connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket);

                //Handle client communication in a separate thread
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting the server: " + e.getMessage());
        }
    }

    static class ClientHandler extends Thread {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
            ) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Received from client: " + inputLine);
                    String[] tokens = inputLine.split(":");
                    if (tokens.length < 3) {
                        out.println("Invalid command format.");
                        continue;
                    }
                    String username = tokens[0];
                    String password = tokens[1];
                    String role = authenticateUser(username, password);
                    if (role == null) {
                        out.println("Authentication failed.");
                        continue;
                    }
                    if (role.equals("Admin")) {
                        if (tokens[2].equals("addStudent")) {
                            // Add student user details
                            if (tokens.length != 5) {
                                out.println("Invalid command format for adding student.");
                            } else {
                                String studentUsername = tokens[3];
                                String studentPassword = tokens[4];
                                String studentFullName = tokens[5];
                                addUser(studentUsername, studentPassword, studentFullName,"Student");
                                out.println("Student added successfully.");
                            }
                        } else if (tokens[2].equals("addInstructor")) {
                            // Add instructor details
                            if (tokens.length != 5) {
                                out.println("Invalid command format for adding instructor.");
                            } else {
                                String instructorUsername = tokens[3];
                                String instructorPassword = tokens[4];
                                String instructorFullName = tokens[5];
                                addUser(instructorUsername, instructorPassword, instructorFullName,"Instructor");
                                out.println("Instructor added successfully.");
                            }
                        } else if (tokens[2].equals("addCourse")) {
                            // Add course details
                            if (tokens.length != 4) {
                                out.println("Invalid command format for adding course.");
                            } else {
                                String courseName = tokens[3];
                                String instructorUsername = tokens[4];
                                addCourse(courseName, instructorUsername);
                                out.println("Course added successfully.");
                            }
                        }else if (tokens[2].equals("assignInstructorToCourse")) {
                            if (tokens.length != 5) {
                                out.println("Invalid command format for assigning instructor to course.");
                            } else {
                                String instructorUsername = tokens[3];
                                String courseName = tokens[4];
                                assignInstructorToCourse(instructorUsername, courseName);
                            }
                        }else  if (tokens[2].equals("addStudentToCourse")) {
                            if (tokens.length != 5) {
                                out.println("Invalid command format for adding student to course.");
                            } else {
                                String studentUsername = tokens[3];
                                String courseName = tokens[4];
                                addStudentToCourse(studentUsername, courseName);
                            }
                        }  else {
                            out.println("Invalid command.");
                        }
                    } else if (role.equals("Instructor")) {
                        // Handle instructor actions
                        if (tokens[2].equals("updateGrade")) {
                            if (tokens.length != 6) {
                                out.println("Invalid command format.");
                                continue;
                            }
                            String studentUsername = tokens[3];
                            String courseName = tokens[4];
                            float grade = Float.parseFloat(tokens[5]);
                            updateGrade(studentUsername, courseName, grade);
                            out.println("Grade updated successfully.");
                        }else if(tokens[2].equals("addGrade")){
                            if (tokens.length != 6) {
                                out.println("Invalid command format for adding grade.");
                            } else {
                                String studentUsername = tokens[3];
                                String courseName = tokens[4];
                                float grade = Float.parseFloat(tokens[5]);
                                addGrade(studentUsername, courseName, grade);
                            }

                        }
                        else {
                            out.println("Invalid command.");
                        }
                    }else if (role.equals("Student")) {
                        if (tokens[2].equals("viewGrades")) {
                            if (tokens.length != 3) {
                                out.println("Invalid command format.");
                                continue;
                            }
                            String studentUsername = tokens[0]; // Username of the student
                            String grades = viewGrades(studentUsername);
                            out.println("Grades: " + grades);
                        } else {
                            out.println("Invalid command.");
                        }
                    } else {
                        out.println("Invalid role.");
                    }
                }
            } catch (IOException e) {
                System.err.println("Error handling client connection: " + e.getMessage());
            }
        }

    }

    private static String authenticateUser(String username, String password) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT role FROM users WHERE username = ? AND password = ?")) {
            statement.setString(1, username);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("role");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void updateGrade(String studentUsername, String courseName, float grade) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE grades " +
                             "SET grade = ? " +
                             "WHERE student_id = (SELECT user_id FROM users WHERE username = ?) " +
                             "AND course_id = (SELECT course_id FROM courses WHERE course_name = ?)")) {
            statement.setFloat(1, grade);
            statement.setString(2, studentUsername);
            statement.setString(3, courseName);
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Grade updated successfully.");
            } else {
                System.out.println("Failed to update grade.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static String viewGrades(String studentUsername) {
        // Retrieve grades from the database for the given student
        StringBuilder grades = new StringBuilder();
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT course_name, grade FROM grades " +
                             "JOIN courses USING (course_id) " +
                             "JOIN users ON grades.student_id = users.user_id " +
                             "WHERE users.username = ?")) {
            statement.setString(1, studentUsername);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String courseName = resultSet.getString("course_name");
                float grade = resultSet.getFloat("grade");
                grades.append(courseName).append(": ").append(grade).append("\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return grades.toString();
    }
    private static void addCourse(String courseName, String instructorUsername) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO courses (course_name, instructor_id) VALUES (?, (SELECT user_id FROM users WHERE username = ?))")) {
            statement.setString(1, courseName);
            statement.setString(2, instructorUsername);
            statement.executeUpdate();
            System.out.println("Course added successfully.");
        } catch (SQLException e) {
            System.out.println("Failed to add course: " + e.getMessage());
        }
    }
    private static void assignInstructorToCourse(String instructorUsername, String courseName) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO instructor_course (instructor_id, course_id) " +
                             "SELECT u.user_id, c.course_id " +
                             "FROM users u JOIN courses c ON u.username = ? AND c.course_name = ?")) {
            statement.setString(1, instructorUsername);
            statement.setString(2, courseName);
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Instructor assigned to course successfully.");
            } else {
                System.out.println("Failed to assign instructor to course.");
            }
        } catch (SQLException e) {
            System.out.println("Failed to assign instructor to course: " + e.getMessage());
        }
    }
    private static void addStudentToCourse(String studentUsername, String courseName) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO student_course (student_id, course_id) " +
                             "SELECT u.user_id, c.course_id " +
                             "FROM users u JOIN courses c ON u.username = ? AND c.course_name = ?")) {
            statement.setString(1, studentUsername);
            statement.setString(2, courseName);
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Student added to course successfully.");
            } else {
                System.out.println("Failed to add student to course.");
            }
        } catch (SQLException e) {
            System.out.println("Failed to add student to course: " + e.getMessage());
        }
    }

    private static void addGrade(String studentUsername, String courseName, float grade) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO grades (student_id, course_id, grade) " +
                             "VALUES ((SELECT user_id FROM users WHERE username = ?), " +
                             "(SELECT course_id FROM courses WHERE course_name = ?), ?)")) {
            statement.setString(1, studentUsername);
            statement.setString(2, courseName);
            statement.setFloat(3, grade);
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Grade added successfully.");
            } else {
                System.out.println("Failed to add grade.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




    private static void addUser(String username, String password, String fullName, String role) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);

             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, fullName);
            statement.setString(3, role);
            statement.executeUpdate();
            System.out.println(""+role+" added successfully.");

        } catch (SQLException e) {
            System.out.println("Failed to add user: " + e.getMessage());
        }
    }

}
