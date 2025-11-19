# Backend Implementation Guide for FR 3 (Book Loans)

This document outlines all the PHP backend files you need to create to support the Book Loans functionality.

## Database Schema Reference

Based on your schema:
- **BOOK_LOANS**: `Loan_id` (PK), `Isbn` (FK), `Card_id` (FK), `Date_out`, `Due_date`, `Date_in`
- **BORROWER**: `Card_id` (PK), `Ssn`, `Bname`, `Address`, `Phone`
- **BOOK**: `Isbn` (PK), `Title`
- **FINES**: `Loan_id` (PK/FK), `Fine_amt`, `Paid`

---

## File 1: `checkoutBook.php`

**Purpose**: Handle book checkout with all validations

**Location**: `http://cm8tes.com/CS4347_Project_Folder/checkoutBook.php`

**Request Method**: POST

**Parameters**:
- `isbn` - Book ISBN
- `card_id` - Borrower Card_id

**Validations Required**:
1. ✅ Check if book is already checked out (`Date_in IS NULL`)
2. ✅ Check if borrower has unpaid fines (`Paid = 0`)
3. ✅ Check if borrower already has 3 active loans (`Date_in IS NULL`)
4. ✅ Set due date to exactly 14 days from checkout date

**Response Format**:
```json
{
  "status": "success" | "error",
  "message": "Descriptive message"
}
```

**PHP Implementation**:

```php
<?php
header('Content-Type: application/json');
require_once 'db_connection.php'; // Your existing DB connection file

$isbn = $_POST['isbn'] ?? '';
$card_id = $_POST['card_id'] ?? '';

if (empty($isbn) || empty($card_id)) {
    echo json_encode(['status' => 'error', 'message' => 'ISBN and Card ID are required']);
    exit;
}

try {
    $conn = getConnection(); // Your existing connection function
    
    // 1. Check if book exists
    $stmt = $conn->prepare("SELECT Isbn, Title FROM BOOK WHERE Isbn = ?");
    $stmt->bind_param("s", $isbn);
    $stmt->execute();
    $result = $stmt->get_result();
    if ($result->num_rows == 0) {
        echo json_encode(['status' => 'error', 'message' => 'Book not found']);
        exit;
    }
    $book = $result->fetch_assoc();
    $stmt->close();
    
    // 2. Check if borrower exists
    $stmt = $conn->prepare("SELECT Card_id, Bname FROM BORROWER WHERE Card_id = ?");
    $stmt->bind_param("s", $card_id);
    $stmt->execute();
    $result = $stmt->get_result();
    if ($result->num_rows == 0) {
        echo json_encode(['status' => 'error', 'message' => 'Borrower not found']);
        exit;
    }
    $borrower = $result->fetch_assoc();
    $stmt->close();
    
    // 3. Check if book is already checked out (Date_in IS NULL means still out)
    $stmt = $conn->prepare("
        SELECT COUNT(*) as count 
        FROM BOOK_LOANS 
        WHERE Isbn = ? AND Date_in IS NULL
    ");
    $stmt->bind_param("s", $isbn);
    $stmt->execute();
    $result = $stmt->get_result();
    $row = $result->fetch_assoc();
    
    if ($row['count'] > 0) {
        echo json_encode([
            'status' => 'error', 
            'message' => 'Book "' . $book['Title'] . '" is already checked out and not available.'
        ]);
        exit;
    }
    $stmt->close();
    
    // 4. Check if borrower has unpaid fines
    $stmt = $conn->prepare("
        SELECT SUM(Fine_amt) as total_fines 
        FROM FINES 
        WHERE Loan_id IN (
            SELECT Loan_id FROM BOOK_LOANS WHERE Card_id = ?
        ) AND Paid = 0
    ");
    $stmt->bind_param("s", $card_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $row = $result->fetch_assoc();
    
    if ($row['total_fines'] > 0) {
        echo json_encode([
            'status' => 'error', 
            'message' => 'Borrower "' . $borrower['Bname'] . '" has unpaid fines of $' . 
                        number_format($row['total_fines'], 2) . '. Cannot checkout books.'
        ]);
        exit;
    }
    $stmt->close();
    
    // 5. Check if borrower already has 3 active loans
    $stmt = $conn->prepare("
        SELECT COUNT(*) as count 
        FROM BOOK_LOANS 
        WHERE Card_id = ? AND Date_in IS NULL
    ");
    $stmt->bind_param("s", $card_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $row = $result->fetch_assoc();
    
    if ($row['count'] >= 3) {
        echo json_encode([
            'status' => 'error', 
            'message' => 'Borrower "' . $borrower['Bname'] . '" already has 3 active loans. Maximum limit reached.'
        ]);
        exit;
    }
    $stmt->close();
    
    // 6. Create checkout - Due date is exactly 14 days from today
    $date_out = date('Y-m-d');
    $due_date = date('Y-m-d', strtotime('+14 days'));
    
    $stmt = $conn->prepare("
        INSERT INTO BOOK_LOANS (Isbn, Card_id, Date_out, Due_date) 
        VALUES (?, ?, ?, ?)
    ");
    $stmt->bind_param("ssss", $isbn, $card_id, $date_out, $due_date);
    
    if ($stmt->execute()) {
        echo json_encode([
            'status' => 'success', 
            'message' => 'Book "' . $book['Title'] . '" checked out successfully to ' . 
                        $borrower['Bname'] . '. Due date: ' . $due_date
        ]);
    } else {
        echo json_encode(['status' => 'error', 'message' => 'Failed to checkout book: ' . $stmt->error]);
    }
    
    $stmt->close();
    $conn->close();
    
} catch (Exception $e) {
    echo json_encode(['status' => 'error', 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
```

---

## File 2: `searchLoans.php`

**Purpose**: Search for active loans by ISBN, Card_id, or Borrower name (substring matching)

**Location**: `http://cm8tes.com/CS4347_Project_Folder/searchLoans.php`

**Request Method**: GET

**Parameters**:
- `query` - Search string (ISBN, Card_id, or Borrower name substring)

**Requirements**:
- Case-insensitive substring matching
- Only return loans where `Date_in IS NULL` (active loans)
- Search across: `BOOK.Isbn`, `BORROWER.Card_id`, `BORROWER.Bname`

**Response Format**:
```json
{
  "status": "success",
  "loans": [
    {
      "Loan_id": "123",
      "Isbn": "0923398364",
      "Title": "Book Title",
      "Card_id": "B001",
      "Bname": "John Doe",
      "Date_out": "2024-01-15",
      "Due_date": "2024-01-29"
    }
  ]
}
```

**PHP Implementation**:

```php
<?php
header('Content-Type: application/json');
require_once 'db_connection.php';

$query = $_GET['query'] ?? '';

if (empty($query)) {
    echo json_encode(['status' => 'error', 'message' => 'Search query required']);
    exit;
}

try {
    $conn = getConnection();
    $searchPattern = "%" . $query . "%";
    
    // Search across ISBN, Card_id, and Borrower name
    // Only return active loans (Date_in IS NULL)
    $stmt = $conn->prepare("
        SELECT 
            bl.Loan_id,
            bl.Isbn,
            b.Title,
            bl.Card_id,
            br.Bname,
            bl.Date_out,
            bl.Due_date
        FROM BOOK_LOANS bl
        JOIN BOOK b ON bl.Isbn = b.Isbn
        JOIN BORROWER br ON bl.Card_id = br.Card_id
        WHERE bl.Date_in IS NULL
          AND (
            LOWER(bl.Isbn) LIKE LOWER(?)
            OR LOWER(bl.Card_id) LIKE LOWER(?)
            OR LOWER(br.Bname) LIKE LOWER(?)
          )
        ORDER BY bl.Due_date ASC
    ");
    $stmt->bind_param("sss", $searchPattern, $searchPattern, $searchPattern);
    $stmt->execute();
    $result = $stmt->get_result();
    
    $loans = [];
    while ($row = $result->fetch_assoc()) {
        $loans[] = [
            'Loan_id' => $row['Loan_id'],
            'Isbn' => $row['Isbn'],
            'Title' => $row['Title'],
            'Card_id' => $row['Card_id'],
            'Bname' => $row['Bname'],
            'Date_out' => $row['Date_out'],
            'Due_date' => $row['Due_date']
        ];
    }
    
    echo json_encode(['status' => 'success', 'loans' => $loans]);
    
    $stmt->close();
    $conn->close();
    
} catch (Exception $e) {
    echo json_encode(['status' => 'error', 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
```

---

## File 3: `checkinBooks.php`

**Purpose**: Check in 1-3 selected loans

**Location**: `http://cm8tes.com/CS4347_Project_Folder/checkinBooks.php`

**Request Method**: POST

**Parameters**:
- `loan_ids` - Comma-separated list of Loan_id values (1-3 loans)

**Requirements**:
- Validate that 1-3 loans are provided
- Update `Date_in` to current date for each loan
- Only update loans that are currently active (`Date_in IS NULL`)
- Calculate fines if overdue (optional - can be done separately)

**Response Format**:
```json
{
  "status": "success" | "error",
  "message": "Successfully checked in X book(s)"
}
```

**PHP Implementation**:

```php
<?php
header('Content-Type: application/json');
require_once 'db_connection.php';

$loanIdsStr = $_POST['loan_ids'] ?? '';

if (empty($loanIdsStr)) {
    echo json_encode(['status' => 'error', 'message' => 'Loan IDs required']);
    exit;
}

$loanIds = explode(',', $loanIdsStr);
$loanIds = array_map('trim', $loanIds);
$loanIds = array_filter($loanIds); // Remove empty values

if (count($loanIds) == 0 || count($loanIds) > 3) {
    echo json_encode(['status' => 'error', 'message' => 'Must select 1-3 loans to check in']);
    exit;
}

try {
    $conn = getConnection();
    $returnDate = date('Y-m-d');
    $successCount = 0;
    $errors = [];
    
    foreach ($loanIds as $loanId) {
        // Validate loan ID is numeric
        if (!is_numeric($loanId)) {
            $errors[] = "Invalid Loan ID: $loanId";
            continue;
        }
        
        // Check if loan exists and is active
        $stmt = $conn->prepare("
            SELECT Loan_id, Isbn, Card_id, Due_date 
            FROM BOOK_LOANS 
            WHERE Loan_id = ? AND Date_in IS NULL
        ");
        $stmt->bind_param("i", $loanId);
        $stmt->execute();
        $result = $stmt->get_result();
        
        if ($result->num_rows == 0) {
            $errors[] = "Loan ID $loanId not found or already checked in";
            $stmt->close();
            continue;
        }
        
        $loan = $result->fetch_assoc();
        $stmt->close();
        
        // Update Date_in
        $stmt = $conn->prepare("
            UPDATE BOOK_LOANS 
            SET Date_in = ? 
            WHERE Loan_id = ?
        ");
        $stmt->bind_param("si", $returnDate, $loanId);
        
        if ($stmt->execute()) {
            $successCount++;
            
            // Optional: Calculate fine if overdue
            // You can implement fine calculation here if needed
            $dueDate = strtotime($loan['Due_date']);
            $returnDateTimestamp = strtotime($returnDate);
            if ($returnDateTimestamp > $dueDate) {
                $daysOverdue = floor(($returnDateTimestamp - $dueDate) / (60 * 60 * 24));
                $fineAmount = $daysOverdue * 0.25; // $0.25 per day (adjust as needed)
                
                // Check if fine already exists
                $checkFine = $conn->prepare("SELECT Loan_id FROM FINES WHERE Loan_id = ?");
                $checkFine->bind_param("i", $loanId);
                $checkFine->execute();
                $fineResult = $checkFine->get_result();
                
                if ($fineResult->num_rows == 0) {
                    // Create fine record
                    $createFine = $conn->prepare("INSERT INTO FINES (Loan_id, Fine_amt, Paid) VALUES (?, ?, 0)");
                    $createFine->bind_param("id", $loanId, $fineAmount);
                    $createFine->execute();
                    $createFine->close();
                }
                $checkFine->close();
            }
        } else {
            $errors[] = "Failed to check in Loan ID $loanId: " . $stmt->error;
        }
        
        $stmt->close();
    }
    
    if ($successCount > 0) {
        $message = "Successfully checked in $successCount book(s)";
        if (!empty($errors)) {
            $message .= ". Errors: " . implode(", ", $errors);
        }
        echo json_encode(['status' => 'success', 'message' => $message]);
    } else {
        echo json_encode([
            'status' => 'error', 
            'message' => 'Failed to check in any books. ' . implode(", ", $errors)
        ]);
    }
    
    $conn->close();
    
} catch (Exception $e) {
    echo json_encode(['status' => 'error', 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
```

---

## Summary Checklist

### Files to Create:
- [ ] `checkoutBook.php` - Handles book checkout
- [ ] `searchLoans.php` - Searches active loans
- [ ] `checkinBooks.php` - Checks in selected loans

### Validations to Implement:
- [x] Book availability check (not already checked out)
- [x] Borrower unpaid fines check
- [x] Maximum 3 active loans per borrower
- [x] Due date = 14 days from checkout
- [x] Search loans by ISBN, Card_id, or Borrower name
- [x] Select 1-3 loans for check-in

### Database Operations:
- [x] INSERT into BOOK_LOANS (checkout)
- [x] UPDATE BOOK_LOANS.Date_in (checkin)
- [x] SELECT with JOINs (search loans)
- [x] COUNT queries (validation checks)
- [x] SUM queries (fine calculations)

---

## Testing Checklist

### Test Checkout:
1. ✅ Normal checkout (book available, borrower eligible)
2. ✅ Book already checked out (should fail)
3. ✅ Borrower has 3 active loans (should fail)
4. ✅ Borrower has unpaid fines (should fail)
5. ✅ Invalid ISBN (should fail)
6. ✅ Invalid Card_id (should fail)

### Test Checkin:
1. ✅ Check in 1 loan (should succeed)
2. ✅ Check in 2-3 loans (should succeed)
3. ✅ Try to check in 0 loans (should fail)
4. ✅ Try to check in 4+ loans (should fail)
5. ✅ Check in already returned loan (should fail)
6. ✅ Invalid Loan_id (should fail)

### Test Search:
1. ✅ Search by ISBN (full and partial)
2. ✅ Search by Card_id
3. ✅ Search by Borrower name (substring)
4. ✅ Case-insensitive search
5. ✅ Empty search (should return error)
6. ✅ No results found (should return empty array)

---

## Notes

1. **Database Connection**: Make sure your `db_connection.php` file has a `getConnection()` function that returns a MySQLi connection.

2. **Error Handling**: All files return JSON with `status` and `message` fields for consistent error handling.

3. **Date Format**: Uses MySQL DATE format (`YYYY-MM-DD`).

4. **Fine Calculation**: The checkin file includes optional fine calculation. You can remove or modify this based on your requirements.

5. **Security**: Consider adding:
   - Input sanitization
   - SQL injection prevention (using prepared statements - already done)
   - Rate limiting
   - Authentication/authorization checks

6. **Testing**: Test each endpoint thoroughly before deploying to production.

