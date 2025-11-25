<?php
/**
 * FR5: Update Fines
 * Calculates and updates fines for all late loans
 * Simulates what a daily cron job would do
 * 
 * Request: POST
 * Parameters: None
 * 
 * Response: JSON with status, message, and updated_count
 */

header('Content-Type: application/json');

// Database connection - UPDATE THESE WITH YOUR ACTUAL CREDENTIALS
$servername = "localhost";
$username = "your_username";
$password = "your_password";
$dbname = "your_database";

// Create connection
$conn = new mysqli($servername, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
    echo json_encode([
        "status" => "error",
        "message" => "Database connection failed: " . $conn->connect_error
    ]);
    exit;
}

$updatedCount = 0;

try {
    // Process returned books (Date_in IS NOT NULL)
    $sqlReturned = "INSERT INTO FINES (Loan_id, Fine_amt, Paid)
                    SELECT 
                        bl.Loan_id,
                        GREATEST(0, DATEDIFF(bl.Date_in, bl.Due_date)) * 0.25 AS Fine_amt,
                        0 AS Paid
                    FROM BOOK_LOANS bl
                    WHERE bl.Date_in IS NOT NULL
                      AND DATEDIFF(bl.Date_in, bl.Due_date) > 0
                      AND NOT EXISTS (
                          SELECT 1 FROM FINES f WHERE f.Loan_id = bl.Loan_id
                      )
                    ON DUPLICATE KEY UPDATE
                        Fine_amt = CASE 
                            WHEN Paid = 0 THEN GREATEST(0, DATEDIFF(bl.Date_in, bl.Due_date)) * 0.25
                            ELSE Fine_amt
                        END";
    
    if ($conn->query($sqlReturned)) {
        $updatedCount += $conn->affected_rows;
    }
    
    // Process books still out (Date_in IS NULL)
    $sqlStillOut = "INSERT INTO FINES (Loan_id, Fine_amt, Paid)
                    SELECT 
                        bl.Loan_id,
                        GREATEST(0, DATEDIFF(CURDATE(), bl.Due_date)) * 0.25 AS Fine_amt,
                        0 AS Paid
                    FROM BOOK_LOANS bl
                    WHERE bl.Date_in IS NULL
                      AND DATEDIFF(CURDATE(), bl.Due_date) > 0
                      AND NOT EXISTS (
                          SELECT 1 FROM FINES f WHERE f.Loan_id = bl.Loan_id
                      )
                    ON DUPLICATE KEY UPDATE
                        Fine_amt = CASE 
                            WHEN Paid = 0 THEN GREATEST(0, DATEDIFF(CURDATE(), bl.Due_date)) * 0.25
                            ELSE Fine_amt
                        END";
    
    if ($conn->query($sqlStillOut)) {
        $updatedCount += $conn->affected_rows;
    }
    
    // Also update existing unpaid fines for books still out
    $sqlUpdateExisting = "UPDATE FINES f
                          JOIN BOOK_LOANS bl ON f.Loan_id = bl.Loan_id
                          SET f.Fine_amt = GREATEST(0, DATEDIFF(CURDATE(), bl.Due_date)) * 0.25
                          WHERE bl.Date_in IS NULL
                            AND DATEDIFF(CURDATE(), bl.Due_date) > 0
                            AND f.Paid = 0";
    
    if ($conn->query($sqlUpdateExisting)) {
        $updatedCount += $conn->affected_rows;
    }
    
    // Return success response
    echo json_encode([
        "status" => "success",
        "message" => "Fines updated successfully. " . $updatedCount . " fine(s) calculated/updated.",
        "updated_count" => $updatedCount
    ]);
    
} catch (Exception $e) {
    echo json_encode([
        "status" => "error",
        "message" => "Error updating fines: " . $e->getMessage()
    ]);
}

$conn->close();
?>

