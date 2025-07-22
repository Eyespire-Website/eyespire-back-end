-- Update chatbox_sessions table to increase session_data column size
-- This fixes the truncation error when saving chat session data

-- Check if the table exists and alter the column
IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'chatbox_sessions')
BEGIN
    -- Alter the session_data column to TEXT (NVARCHAR(MAX) in SQL Server)
    ALTER TABLE chatbox_sessions 
    ALTER COLUMN session_data NVARCHAR(MAX);
    
    PRINT 'Updated chatbox_sessions.session_data column to NVARCHAR(MAX)';
END
ELSE
BEGIN
    PRINT 'Table chatbox_sessions does not exist';
END
