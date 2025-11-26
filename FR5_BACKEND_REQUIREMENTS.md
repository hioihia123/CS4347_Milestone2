# FR5: Fines Management - Backend Requirements

This document outlines the PHP endpoints that need to be implemented on the backend to support FR5 (Fines Management).

## Database Schema

The FINES table should have the following structure:
```sql
FINES (
    Loan_id VARCHAR/INT PRIMARY KEY,
    Fine_amt DECIMAL(10,2) NOT NULL,  -- Fixed decimal with 2 decimal places
    Paid TINYINT(1) DEFAULT 0          -- Boolean: 0 = unpaid, 1 = paid
)
```

**Important:** 
- `Fine_amt` must be DECIMAL(10,2), NOT FLOAT or REAL
- `Paid` should be TINYINT(1) or BOOLEAN (0/1)

## Required PHP Endpoints

### 1. `getFines.php`
**Purpose:** Retrieve fines grouped by borrower (card_no)

**Request Method:** POST

**Parameters:**
- `include_paid` (string): "1" to include paid fines, "0" to exclude them

**Response Format:**
```json
{
    "status": "success",
    "fines": [
        {
            "Card_id": "12345",
            "Bname": "John Doe",
            "Total_fine": "12.50",
            "Paid": "0",
            "Loan_ids": "1,2,3"
        },
        {
            "Card_id": "67890",
            "Bname": "Jane Smith",
            "Total_fine": "5.25",
            "Paid": "1",
            "Loan_ids": "4,5"
        }
    ]
}
```

**SQL Logic:**
- Group by `Card_id` (from BOOK_LOANS)
- SUM(`Fine_amt`) as `Total_fine`
- Get borrower name from BORROWER table
- Concatenate Loan_ids for that borrower
- Filter by `Paid` status if `include_paid = "0"`
- Join: FINES → BOOK_LOANS → BORROWER

**Example Query:**
```sql
SELECT 
    bl.Card_id,
    b.Bname,
    SUM(f.Fine_amt) AS Total_fine,
    MAX(f.Paid) AS Paid,  -- If any fine is unpaid, show as unpaid
    GROUP_CONCAT(f.Loan_id) AS Loan_ids
FROM FINES f
JOIN BOOK_LOANS bl ON f.Loan_id = bl.Loan_id
JOIN BORROWER b ON bl.Card_id = b.Card_id
WHERE (? = '1' OR f.Paid = 0)
GROUP BY bl.Card_id, b.Bname
ORDER BY b.Bname;
```

---

### 2. `updateFines.php`
**Purpose:** Calculate and update fines for all late loans (simulates daily cron job)

**Request Method:** POST

**Parameters:** None (processes all loans)

**Response Format:**
```json
{
    "status": "success",
    "message": "Fines updated successfully. 15 fines calculated.",
    "updated_count": 15
}
```

**Business Logic:**

1. **For returned books (Date_in is NOT NULL):**
   - Calculate: `days_late = DATEDIFF(Date_in, Due_date)`
   - If `days_late > 0`: `fine_amt = days_late * 0.25`
   - If `days_late <= 0`: No fine

2. **For books still out (Date_in IS NULL):**
   - Calculate: `days_late = DATEDIFF(CURDATE(), Due_date)`
   - If `days_late > 0`: `fine_amt = days_late * 0.25`
   - If `days_late <= 0`: No fine

3. **Update Rules:**
   - If FINES row exists for Loan_id:
     - If `Paid = 0` (unpaid): Update `Fine_amt` if different from calculated value
     - If `Paid = 1` (paid): Do nothing (skip)
   - If FINES row does NOT exist:
     - Create new row with calculated `Fine_amt` and `Paid = 0`

**Example SQL Logic:**
```sql
-- For returned books
INSERT INTO FINES (Loan_id, Fine_amt, Paid)
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
        WHEN Paid = 0 THEN VALUES(Fine_amt)
        ELSE Fine_amt
    END;

-- For books still out
INSERT INTO FINES (Loan_id, Fine_amt, Paid)
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
        WHEN Paid = 0 THEN VALUES(Fine_amt)
        ELSE Fine_amt
    END;
```

**Note:** Use MySQL's `ON DUPLICATE KEY UPDATE` or check existence first, then INSERT or UPDATE accordingly.

---

### 3. `payFines.php`
**Purpose:** Mark fines as paid for a specific borrower

**Request Method:** POST

**Parameters:**
- `card_id` (string): Borrower's card ID
- `loan_ids` (string): Comma-separated list of Loan_ids (e.g., "1,2,3")

**Response Format:**
```json
{
    "status": "success",
    "message": "Fines paid successfully for borrower John Doe. Total: $12.50"
}
```

**Business Logic:**

1. **Validation:**
   - Verify all loans in `loan_ids` belong to the `card_id`
   - Verify all loans have `Date_in IS NOT NULL` (book must be returned)
   - If any loan is not returned, return error:
     ```json
     {
         "status": "error",
         "message": "Cannot pay fines for books that are not yet returned."
     }
     ```

2. **Payment:**
   - Update all FINES records for the given Loan_ids
   - Set `Paid = 1`
   - Only update if `Paid = 0` (prevent re-paying)

3. **Error Cases:**
   - Loan not found
   - Loan doesn't belong to card_id
   - Book not returned (Date_in is NULL)
   - Fine already paid

**Example SQL:**
```sql
-- Validate all books are returned
SELECT COUNT(*) 
FROM BOOK_LOANS 
WHERE Loan_id IN (1,2,3) 
  AND Card_id = '12345'
  AND Date_in IS NULL;

-- If count > 0, return error

-- Update fines
UPDATE FINES f
JOIN BOOK_LOANS bl ON f.Loan_id = bl.Loan_id
SET f.Paid = 1
WHERE bl.Card_id = ?
  AND f.Loan_id IN (?)  -- Use FIND_IN_SET or explode loan_ids
  AND bl.Date_in IS NOT NULL
  AND f.Paid = 0;
```

**PHP Implementation Notes:**
```php
// Explode loan_ids
$loanIds = explode(',', $_POST['loan_ids']);

// Validate
foreach ($loanIds as $loanId) {
    // Check if loan exists and belongs to card_id
    // Check if Date_in is NOT NULL
}

// Update fines
$placeholders = implode(',', array_fill(0, count($loanIds), '?'));
$sql = "UPDATE FINES f
        JOIN BOOK_LOANS bl ON f.Loan_id = bl.Loan_id
        SET f.Paid = 1
        WHERE bl.Card_id = ?
          AND f.Loan_id IN ($placeholders)
          AND bl.Date_in IS NOT NULL
          AND f.Paid = 0";
```

---

## Summary

You need to create **3 PHP endpoints**:

1. **`getFines.php`** - Get fines grouped by borrower
2. **`updateFines.php`** - Calculate/update fines (daily cron job simulation)
3. **`payFines.php`** - Mark fines as paid

All endpoints should:
- Return JSON responses
- Handle errors gracefully
- Use prepared statements to prevent SQL injection
- Use DECIMAL(10,2) for fine amounts (not FLOAT)
- Use TINYINT(1) or BOOLEAN for Paid field

## Testing Checklist

- [ ] `getFines.php` returns fines grouped by card_no
- [ ] `getFines.php` filters paid/unpaid correctly
- [ ] `updateFines.php` calculates fines correctly for returned books
- [ ] `updateFines.php` calculates fines correctly for books still out
- [ ] `updateFines.php` doesn't update paid fines
- [ ] `payFines.php` validates books are returned
- [ ] `payFines.php` prevents paying fines for unreturned books
- [ ] `payFines.php` prevents partial payments
- [ ] Fine amounts are stored with 2 decimal places
- [ ] All SQL uses prepared statements

