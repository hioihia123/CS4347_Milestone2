<?php
/**
 * FR5: Get Fines Grouped by Borrower
 * Retrieves fines from the FINES table, grouped by card_no (borrower)
 * 
 * Request: POST
 * Parameters:
 *   - include_paid: "1" to include paid fines, "0" to exclude them
 * 
 * Response: JSON with status and fines array
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

// Get parameter
$includePaid = isset($_POST['include_paid']) ? $_POST['include_paid'] : '0';

// Build SQL query - Group fines by borrower
$sql = "SELECT 
            bl.Card_id,
            b.Bname,
            SUM(f.Fine_amt) AS Total_fine,
            MAX(f.Paid) AS Paid,
            GROUP_CONCAT(f.Loan_id ORDER BY f.Loan_id) AS Loan_ids
        FROM FINES f
        JOIN BOOK_LOANS bl ON f.Loan_id = bl.Loan_id
        JOIN BORROWER b ON bl.Card_id = b.Card_id
        WHERE (? = '1' OR f.Paid = 0)
        GROUP BY bl.Card_id, b.Bname
        ORDER BY b.Bname";

// Prepare statement
$stmt = $conn->prepare($sql);
if (!$stmt) {
    echo json_encode([
        "status" => "error",
        "message" => "SQL prepare failed: " . $conn->error
    ]);
    $conn->close();
    exit;
}

// Bind parameter
$stmt->bind_param("s", $includePaid);

// Execute query
if (!$stmt->execute()) {
    echo json_encode([
        "status" => "error",
        "message" => "Query execution failed: " . $stmt->error
    ]);
    $stmt->close();
    $conn->close();
    exit;
}

// Get result
$result = $stmt->get_result();

// Build response array
$fines = [];
while ($row = $result->fetch_assoc()) {
    $fines[] = [
        "Card_id" => $row['Card_id'],
        "Bname" => $row['Bname'],
        "Total_fine" => number_format((float)$row['Total_fine'], 2, '.', ''), // Ensure 2 decimal places
        "Paid" => (string)$row['Paid'], // Convert to string "0" or "1"
        "Loan_ids" => $row['Loan_ids']
    ];
}

// Return JSON response
echo json_encode([
    "status" => "success",
    "fines" => $fines
]);

// Close connections
$stmt->close();
$conn->close();
?>

