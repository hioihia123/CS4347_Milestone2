<?php
/**
 * FR5: Pay Fines
 * Marks fines as paid for a specific borrower
 * 
 * Request: POST
 * Parameters:
 *   - card_id: Borrower's card ID
 *   - loan_ids: Comma-separated list of Loan_ids (e.g., "1,2,3")
 * 
 * Response: JSON with status and message
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

// Get parameters
$cardId = isset($_POST['card_id']) ? $_POST['card_id'] : '';
$loanIdsStr = isset($_POST['loan_ids']) ? $_POST['loan_ids'] : '';

// Validate inputs
if (empty($cardId) || empty($loanIdsStr)) {
    echo json_encode([
        "status" => "error",
        "message" => "Missing required parameters: card_id and loan_ids"
    ]);
    $conn->close();
    exit;
}

// Explode loan_ids into array
$loanIds = explode(',', $loanIdsStr);
$loanIds = array_map('trim', $loanIds); // Remove whitespace
$loanIds = array_filter($loanIds); // Remove empty values

if (empty($loanIds)) {
    echo json_encode([
        "status" => "error",
        "message" => "Invalid loan_ids parameter"
    ]);
    $conn->close();
    exit;
}

try {
    // Validate: Check if all books are returned
    $placeholders = implode(',', array_fill(0, count($loanIds), '?'));
    $validateSql = "SELECT COUNT(*) AS count_not_returned
                    FROM BOOK_LOANS 
                    WHERE Loan_id IN ($placeholders)
                      AND Card_id = ?
                      AND Date_in IS NULL";
    
    $stmt = $conn->prepare($validateSql);
    if (!$stmt) {
        throw new Exception("SQL prepare failed: " . $conn->error);
    }
    
    // Bind parameters: loan_ids + card_id
    $types = str_repeat('i', count($loanIds)) . 's';
    $params = array_merge($loanIds, [$cardId]);
    $stmt->bind_param($types, ...$params);
    
    if (!$stmt->execute()) {
        throw new Exception("Validation query failed: " . $stmt->error);
    }
    
    $result = $stmt->get_result();
    $row = $result->fetch_assoc();
    $stmt->close();
    
    if ($row['count_not_returned'] > 0) {
        echo json_encode([
            "status" => "error",
            "message" => "Cannot pay fines for books that are not yet returned."
        ]);
        $conn->close();
        exit;
    }
    
    // Validate: Check if all loans belong to this card_id
    $validateOwnershipSql = "SELECT COUNT(*) AS count_mismatch
                             FROM BOOK_LOANS 
                             WHERE Loan_id IN ($placeholders)
                               AND Card_id != ?";
    
    $stmt = $conn->prepare($validateOwnershipSql);
    if (!$stmt) {
        throw new Exception("SQL prepare failed: " . $conn->error);
    }
    
    $stmt->bind_param($types, ...$params);
    
    if (!$stmt->execute()) {
        throw new Exception("Ownership validation failed: " . $stmt->error);
    }
    
    $result = $stmt->get_result();
    $row = $result->fetch_assoc();
    $stmt->close();
    
    if ($row['count_mismatch'] > 0) {
        echo json_encode([
            "status" => "error",
            "message" => "Some loans do not belong to this borrower."
        ]);
        $conn->close();
        exit;
    }
    
    // Get borrower name for response message
    $nameSql = "SELECT Bname FROM BORROWER WHERE Card_id = ?";
    $stmt = $conn->prepare($nameSql);
    $stmt->bind_param("s", $cardId);
    $stmt->execute();
    $nameResult = $stmt->get_result();
    $borrowerName = "borrower";
    if ($nameRow = $nameResult->fetch_assoc()) {
        $borrowerName = $nameRow['Bname'];
    }
    $stmt->close();
    
    // Calculate total fine amount before payment
    $totalSql = "SELECT SUM(f.Fine_amt) AS total
                 FROM FINES f
                 JOIN BOOK_LOANS bl ON f.Loan_id = bl.Loan_id
                 WHERE bl.Card_id = ?
                   AND f.Loan_id IN ($placeholders)
                   AND f.Paid = 0";
    
    $stmt = $conn->prepare($totalSql);
    $stmt->bind_param($types, ...$params);
    $stmt->execute();
    $totalResult = $stmt->get_result();
    $totalRow = $totalResult->fetch_assoc();
    $totalFine = $totalRow['total'] ?? 0;
    $stmt->close();
    
    // Update fines: Set Paid = 1
    $updateSql = "UPDATE FINES f
                  JOIN BOOK_LOANS bl ON f.Loan_id = bl.Loan_id
                  SET f.Paid = 1
                  WHERE bl.Card_id = ?
                    AND f.Loan_id IN ($placeholders)
                    AND bl.Date_in IS NOT NULL
                    AND f.Paid = 0";
    
    $stmt = $conn->prepare($updateSql);
    if (!$stmt) {
        throw new Exception("Update SQL prepare failed: " . $conn->error);
    }
    
    $stmt->bind_param($types, ...$params);
    
    if (!$stmt->execute()) {
        throw new Exception("Update failed: " . $stmt->error);
    }
    
    $affectedRows = $stmt->affected_rows;
    $stmt->close();
    
    if ($affectedRows == 0) {
        echo json_encode([
            "status" => "error",
            "message" => "No unpaid fines found to pay, or fines are already paid."
        ]);
    } else {
        $formattedTotal = number_format((float)$totalFine, 2, '.', '');
        echo json_encode([
            "status" => "success",
            "message" => "Fines paid successfully for borrower " . $borrowerName . ". Total: $" . $formattedTotal
        ]);
    }
    
} catch (Exception $e) {
    echo json_encode([
        "status" => "error",
        "message" => "Error processing payment: " . $e->getMessage()
    ]);
}

$conn->close();
?>

