CS4347 Project - Team Iridium
Library Management System (BookBuddy)
Github link: https://github.com/hioihia123/CS4347_Milestone2

1. Project Overview
  This is a Java-based Library Management System designed to handle librarian tasks such as managing books, borrowers, loans, and fines. The application features a modern Swing GUI and communicates with a hosted remote MySQL database via PHP API endpoints.
  
  Note: The application requires an active internet connection to run, as it fetches data from a live server (cm8tes.com).

2. Project Structure
  The repository is organized as follows:
  
  src/com/checkmates/main/: Contains the entry point of the application (Login.java).
  
  src/com/checkmates/ui/: Contains the UI Dashboards (Books, Borrowers, Fines, etc.) and the Sign-Up form.
  
  src/com/checkmates/model/: Contains data models (Librarian, Professor, etc.).
  
  PHP Folder/: Contains the server-side PHP scripts used to bridge the Java application with the hosted MySQL database.
  
  Note: You do not need to run these PHP files locally. They are hosted on our live server. They are included here for code review purposes.

3. Prerequisites & Dependencies
  To run this project, ensure you have the following installed:
  
  Java Development Kit (JDK): Version 8 or higher.
  
  IDE: NetBeans, IntelliJ IDEA, or Eclipse.

  External Libraries (JARs): The project relies on the following external libraries. Ensure these are added to your project's classpath/dependencies:
  
  org.json: For parsing JSON responses from the PHP API.
  
  FlatLaf: For the modern UI look and feel.
  
  ***Our Recommendation: Please CLONE our entire github repo for the full and automatated libraries importation.

4. How to Run the Project
Step 1: Clone the Repository
  Github repo link: https://github.com/hioihia123/CS4347_Milestone2

  Clone this repository to your local machine using your preferred IDE or command line.

Step 2: Build the Project

  Open the project in your IDE. Ensure all dependencies (listed in Section 3) are resolved. Clean and Build the project to ensure there are no compilation errors.

Step 3: Launch the Application

  Navigate to the package com.checkmates.main.
  
  Locate Login.java.
  
  Right-click Login.java and select Run File.

5. Usage Instructions (Walkthrough)
Since this connects to a live database, you will need to create your own credentials to test the system.

A. Creating an Account

  When the Login screen appears, you will not have an account yet.
  
  Click the text label that says "Sign up here!".
  
  Enter your Name, a valid Email, and a Password.
  
  Accept the "Terms and Conditions" and click Sign Up.
  
  Once successful, the window will close, or you will be redirected to the Login screen.

B. Logging In

  Enter the Email and Password you just registered.

  Click LOGIN.

  Upon successful authentication, you will be directed to the main Dashboard.

C. Dashboard Features

  From the main Dashboard, you can access the following modules:

  Manage (Book Catalog): View all books, add new books, edit details, and delete books. You can also manually checkout items here.

  Borrowers: View a list of registered borrowers/students.


D. Sub-Dashboards

  Loans: Inside "Manage," you can view the Loan History, check books in, and see due dates.

  Fines: Manage overdue fines, view paid/unpaid status, and process payments for returned books.

6. Troubleshooting
  "Connection Error" or Data not loading:

  Please check your internet connection. The app creates HTTP requests to http://cm8tes.com/.... If the server is down or you are offline, the app cannot fetch data.

UI Looks different:

  If the application throws an error regarding FlatLightLaf, ensure the FlatLaf JAR is properly added to your libraries.

CS4347 - Team Iridium
